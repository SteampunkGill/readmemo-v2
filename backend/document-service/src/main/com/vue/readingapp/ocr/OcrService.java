package com.vue.readingapp.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OcrService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean processDocument(Integer documentId) {
        // 假设我们只处理第一页作为示例
        Integer page = 1;
        String taskId = "ocr_task_" + UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime now = LocalDateTime.now();

        // 异步执行OCR处理
        new Thread(() -> {
            try {
                // 1. 检查并创建 ocr_tasks 表 (如果不存在)
                ensureOcrTasksTableExists();

                // 2. 插入 OCR 任务记录
                String insertTaskSql = "INSERT INTO ocr_tasks (task_id, document_id, page_number, status, progress, options_json, estimated_time, started_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(insertTaskSql, taskId, documentId, page, "processing", 0, "{}", 30, now, now, now);

                System.out.println("INFO: Started OCR processing for document_id: " + documentId);

                // 确保 documents 表包含必要的 OCR 列
                ensureDocumentsColumnsExist();

                // 3. 获取文件路径
                String queryFileSql = "SELECT file_path FROM documents WHERE document_id = ?";
                String filePath = jdbcTemplate.queryForObject(queryFileSql, String.class, documentId);
                System.out.println("DEBUG: Document file path from DB: " + filePath);
                
                File imageFile = new File(filePath);
                if (!imageFile.exists()) {
                    throw new Exception("文件不存在: " + filePath);
                }

                // 4. 执行真实 OCR 处理
                ITesseract tesseract = new Tesseract();
                
                // 设置训练数据路径
                // 解决中文路径兼容性问题：如果项目路径包含中文，Tesseract 原生库可能无法加载。
                // 方案：将训练数据复制到系统临时目录（通常不含中文）
                File sourceTessDataFolder = new File("src/main/resources/tessdata");
                if (!sourceTessDataFolder.exists()) {
                    throw new Exception("源 Tessdata 目录不存在: " + sourceTessDataFolder.getAbsolutePath());
                }

                // 创建临时目录
                File tempTessDataFolder = new File(System.getProperty("java.io.tmpdir"), "readingapp_tessdata");
                if (!tempTessDataFolder.exists()) {
                    tempTessDataFolder.mkdirs();
                }

                // 复制必要的训练数据文件
                String[] langFiles = {"chi_sim.traineddata", "eng.traineddata"};
                for (String langFile : langFiles) {
                    File srcFile = new File(sourceTessDataFolder, langFile);
                    File destFile = new File(tempTessDataFolder, langFile);
                    if (srcFile.exists() && (!destFile.exists() || srcFile.length() != destFile.length())) {
                        System.out.println("INFO: Copying " + langFile + " to temp directory...");
                        Files.copy(srcFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                String datapath = tempTessDataFolder.getAbsolutePath();
                System.out.println("INFO: Setting Tesseract datapath to temp directory: " + datapath);
                tesseract.setDatapath(datapath);
                
                // 设置语言 (确保 chi_sim.traineddata 和 eng.traineddata 存在)
                tesseract.setLanguage("chi_sim+eng");

                System.out.println("INFO: Executing Text Extraction/OCR for file: " + filePath);
                
                String resultText = "";
                String lowerPath = filePath.toLowerCase();
                int totalPages = 1;
                
                if (lowerPath.endsWith(".pdf")) {
                    System.out.println("DEBUG: Detected PDF file, converting to images for OCR...");
                    try (PDDocument document = PDDocument.load(imageFile)) {
                        totalPages = document.getNumberOfPages();
                        PDFRenderer pdfRenderer = new PDFRenderer(document);
                        StringBuilder sb = new StringBuilder();
                        int pagesToProcess = totalPages; // 处理所有页面
                        for (int i = 0; i < pagesToProcess; i++) {
                            int currentPageNum = i + 1;
                            // 检查文档是否仍然存在，防止处理已删除的文档
                            if (!checkDocumentExists(documentId)) {
                                throw new Exception("文档 ID " + documentId + " 已不存在，停止 OCR 处理。");
                            }
                            
                            System.out.println("DEBUG: Processing PDF page " + currentPageNum);
                            BufferedImage bim = pdfRenderer.renderImageWithDPI(i, 300, ImageType.RGB);
                            String pageText = tesseract.doOCR(bim);
                            sb.append(pageText).append("\n");
                            
                            // 保存每一页到 document_pages 表
                            savePageToDatabase(documentId, currentPageNum, pageText);
                        }
                        resultText = sb.toString();
                    }
                } else if (lowerPath.endsWith(".docx")) {
                    System.out.println("DEBUG: Detected DOCX file, extracting text...");
                    try (FileInputStream fis = new FileInputStream(imageFile);
                         XWPFDocument doc = new XWPFDocument(fis);
                         XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                        resultText = extractor.getText();
                    }
                } else if (lowerPath.endsWith(".doc")) {
                    System.out.println("DEBUG: Detected DOC file, extracting text...");
                    try (FileInputStream fis = new FileInputStream(imageFile);
                         HWPFDocument doc = new HWPFDocument(fis);
                         WordExtractor extractor = new WordExtractor(doc)) {
                        resultText = extractor.getText();
                    }
                } else if (lowerPath.endsWith(".txt") || lowerPath.endsWith(".html") || lowerPath.endsWith(".htm")) {
                    System.out.println("DEBUG: Detected Text/HTML file, reading content...");
                    resultText = new String(Files.readAllBytes(imageFile.toPath()), StandardCharsets.UTF_8);
                } else if (lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".bmp") || lowerPath.endsWith(".tiff")) {
                    System.out.println("DEBUG: Detected Image file, performing OCR...");
                    resultText = tesseract.doOCR(imageFile);
                } else {
                    System.out.println("DEBUG: Unknown format, attempting direct OCR...");
                    resultText = tesseract.doOCR(imageFile);
                }
                
                // 对于非 PDF 文件，保存第一页内容
                if (!lowerPath.endsWith(".pdf")) {
                    // 检查文档是否仍然存在
                    if (!checkDocumentExists(documentId)) {
                        throw new Exception("文档 ID " + documentId + " 已不存在，停止 OCR 处理。");
                    }
                    savePageToDatabase(documentId, 1, resultText);
                }
                
                System.out.println("DEBUG: OCR Result length: " + (resultText != null ? resultText.length() : 0));

                // 5. 生成 OCR 结果
                Map<String, Object> ocrResultMap = new HashMap<>();
                ocrResultMap.put("text", resultText);
                ocrResultMap.put("confidence", 80.0); // Tess4J 获取单字置信度较复杂，此处暂设固定值
                String resultJson = objectMapper.writeValueAsString(ocrResultMap);

                // 5. 更新任务状态为完成，并保存结果
                String completeTaskSql = "UPDATE ocr_tasks SET status = 'completed', progress = 100, result_json = ?, completed_at = ?, updated_at = ? WHERE task_id = ?";
                jdbcTemplate.update(completeTaskSql, resultJson, LocalDateTime.now(), LocalDateTime.now(), taskId);

                System.out.println("INFO: Completed OCR processing for document_id: " + documentId);

                // 6. 更新文档表的状态（包括页数）
                String updateDocSql = "UPDATE documents SET is_processed = 1, processing_status = 'completed', " +
                        "processing_progress = 100, processing_completed_at = ?, page_count = ? WHERE document_id = ?";
                jdbcTemplate.update(updateDocSql, LocalDateTime.now(), totalPages, documentId);

                // 7. (可选) 将结果保存到 document_ocr_results 表
                saveOcrResultToDatabase(documentId, 1, ocrResultMap);

            } catch (Throwable t) {
                String errorMsg = t.getClass().getSimpleName() + ": " + t.getMessage();
                System.err.println("FATAL ERROR: OCR processing crashed for document_id: " + documentId + " - " + errorMsg);
                t.printStackTrace();

                // 更新任务为失败状态
                try {
                    String failTaskSql = "UPDATE ocr_tasks SET status = 'failed', error_message = ?, updated_at = ? WHERE task_id = ?";
                    jdbcTemplate.update(failTaskSql, errorMsg, LocalDateTime.now(), taskId);

                    // 更新文档表的状态
                    String updateDocSql = "UPDATE documents SET processing_status = 'failed', processing_error = ? WHERE document_id = ?";
                    jdbcTemplate.update(updateDocSql, errorMsg, documentId);
                } catch (Exception dbEx) {
                    System.err.println("CRITICAL: Failed to update error status to DB: " + dbEx.getMessage());
                }
            }
        }).start();
        return false;
    }

    private void ensureDocumentsColumnsExist() {
        try {
            String[] columns = {
                "is_processed TINYINT(1) DEFAULT 0",
                "processing_status VARCHAR(50) DEFAULT 'pending'",
                "processing_progress INT(11) DEFAULT 0",
                "processing_error TEXT",
                "processing_started_at TIMESTAMP NULL DEFAULT NULL",
                "processing_completed_at TIMESTAMP NULL DEFAULT NULL"
            };

            for (String colDef : columns) {
                String colName = colDef.split(" ")[0];
                try {
                    String checkColSql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'documents' AND column_name = ?";
                    Integer count = jdbcTemplate.queryForObject(checkColSql, Integer.class, colName);
                    if (count == null || count == 0) {
                        String addColSql = "ALTER TABLE documents ADD COLUMN " + colDef;
                        jdbcTemplate.execute(addColSql);
                        System.out.println("INFO: Added missing column '" + colName + "' to 'documents' table.");
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to add column '" + colName + "': " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to ensure documents columns exist: " + e.getMessage());
        }
    }

    private void ensureOcrTasksTableExists() {
        try {
            String checkTaskTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'ocr_tasks'";
            Integer taskTableCount = jdbcTemplate.queryForObject(checkTaskTableSql, Integer.class);

            if (taskTableCount == null || taskTableCount == 0) {
                String createTaskTableSql = "CREATE TABLE ocr_tasks (" +
                        "task_id VARCHAR(50) PRIMARY KEY," +
                        "document_id INT NOT NULL," +
                        "page_number INT," +
                        "status VARCHAR(20) NOT NULL," +
                        "progress INT DEFAULT 0," +
                        "options_json TEXT," +
                        "result_json TEXT," +
                        "error_message TEXT," +
                        "estimated_time INT," +
                        "started_at TIMESTAMP," +
                        "completed_at TIMESTAMP," +
                        "created_at TIMESTAMP," +
                        "updated_at TIMESTAMP" +
                        ")";
                jdbcTemplate.execute(createTaskTableSql);
                System.out.println("INFO: Created 'ocr_tasks' table.");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to check/create ocr_tasks table: " + e.getMessage());
        }
    }

    private boolean checkDocumentExists(Integer documentId) {
        try {
            String sql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, documentId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void savePageToDatabase(Integer documentId, int pageNumber, String content) throws Exception {
        System.out.println("[DEBUG] 准备保存页面数据: documentId=" + documentId + ", pageNumber=" + pageNumber + ", content长度=" + (content != null ? content.length() : 0));
        try {
            String pageId = "page_" + documentId + "_" + pageNumber;
            int wordCount = content != null ? content.split("\\s+").length : 0;
            int charCount = content != null ? content.length() : 0;

            String sql = "INSERT INTO document_pages (page_id, document_id, page_number, content, word_count, character_count, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                    "ON DUPLICATE KEY UPDATE content = ?, word_count = ?, character_count = ?, updated_at = NOW()";

            int rows = jdbcTemplate.update(sql, pageId, documentId, pageNumber, content, wordCount, charCount, content, wordCount, charCount);
            System.out.println("INFO: 成功保存页面 " + pageNumber + " 到 document_pages，影响行数: " + rows);
        } catch (Exception e) {
            System.err.println("ERROR: 保存页面到 document_pages 失败: " + e.getMessage());
            throw new Exception("数据库写入失败 (可能文档已被删除): " + e.getMessage(), e);
        }
    }

    private void saveOcrResultToDatabase(Integer documentId, Integer page, Map<String, Object> ocrResultMap) {
        try {
            String ocrResultId = "ocr_" + UUID.randomUUID().toString().substring(0, 8);
            LocalDateTime now = LocalDateTime.now();

            // 检查表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                // 创建表
                String createTableSql = "CREATE TABLE document_ocr_results (" +
                        "ocr_id VARCHAR(50) PRIMARY KEY," +
                        "document_id INT NOT NULL," +
                        "page_number INT NOT NULL," +
                        "ocr_text TEXT," +
                        "confidence DOUBLE," +
                        "created_at TIMESTAMP," +
                        "updated_at TIMESTAMP" +
                        ")";
                jdbcTemplate.execute(createTableSql);
                System.out.println("INFO: Created 'document_ocr_results' table.");
            }

            String insertSql = "INSERT INTO document_ocr_results (ocr_id, document_id, page_number, ocr_text, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSql,
                    ocrResultId,
                    documentId,
                    page,
                    ocrResultMap.get("text"),
                    ocrResultMap.get("confidence"),
                    now,
                    now
            );
            System.out.println("INFO: OCR result saved to document_ocr_results for document_id: " + documentId);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to save OCR result to database for document_id: " + documentId + " - " + e.getMessage());
        }
    }
}