
package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderGetDocumentPage {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取文档页面请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
    }

    // 打印查询结果
    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应DTO
    public static class PageResponse {
        private boolean success;
        private String message;
        private PageData data;

        public PageResponse(boolean success, String message, PageData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public PageData getData() { return data; }
        public void setData(PageData data) { this.data = data; }
    }

    public static class PageData {
        private String id;
        private int documentId;
        private int pageNumber;
        private String content;
        private String htmlContent;
        private int wordCount;
        private int characterCount;
        private boolean hasImages;
        private List<Map<String, Object>> images;
        private List<Map<String, Object>> highlights;
        private List<Map<String, Object>> notes;
        private Map<String, Object> metadata;
        private Integer nextPage;
        private Integer prevPage;

        public PageData() {
            this.images = new ArrayList<>();
            this.highlights = new ArrayList<>();
            this.notes = new ArrayList<>();
            this.metadata = new HashMap<>();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getHtmlContent() { return htmlContent; }
        public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }

        public int getWordCount() { return wordCount; }
        public void setWordCount(int wordCount) { this.wordCount = wordCount; }

        public int getCharacterCount() { return characterCount; }
        public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }

        public boolean isHasImages() { return hasImages; }
        public void setHasImages(boolean hasImages) { this.hasImages = hasImages; }

        public List<Map<String, Object>> getImages() { return images; }
        public void setImages(List<Map<String, Object>> images) { this.images = images; }

        public List<Map<String, Object>> getHighlights() { return highlights; }
        public void setHighlights(List<Map<String, Object>> highlights) { this.highlights = highlights; }

        public List<Map<String, Object>> getNotes() { return notes; }
        public void setNotes(List<Map<String, Object>> notes) { this.notes = notes; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        public Integer getNextPage() { return nextPage; }
        public void setNextPage(Integer nextPage) { this.nextPage = nextPage; }

        public Integer getPrevPage() { return prevPage; }
        public void setPrevPage(Integer prevPage) { this.prevPage = prevPage; }
    }

    @GetMapping("/documents/{documentId}/pages/{pageNumber}")
    public ResponseEntity<PageResponse> getDocumentPage(
            @PathVariable("documentId") int documentId,
            @PathVariable("pageNumber") int pageNumber,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("pageNumber", pageNumber);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new PageResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new PageResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT d.*, dp.total_pages FROM documents d " +
                    "LEFT JOIN (SELECT document_id, COUNT(*) as total_pages FROM document_pages GROUP BY document_id) dp " +
                    "ON d.document_id = dp.document_id " +
                    "WHERE d.document_id = ? AND (d.user_id = ? OR d.is_public = true)";

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);
            printQueryResult(documents);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new PageResponse(false, "没有权限查看此文档", null)
                );
            }

            Map<String, Object> document = documents.get(0);
            Integer totalPages = document.get("total_pages") != null ?
                    ((Number) document.get("total_pages")).intValue() : 0;

            // 3. 获取页面内容
            System.out.println("[DEBUG] 正在查询页面: documentId=" + documentId + ", pageNumber=" + pageNumber);
            
            // 额外检查：该文档总共有多少页
            String countSql = "SELECT COUNT(*) FROM document_pages WHERE document_id = ?";
            Integer actualPageCount = jdbcTemplate.queryForObject(countSql, Integer.class, documentId);
            System.out.println("[DEBUG] 文档 ID=" + documentId + " 在 document_pages 表中的实际记录数: " + actualPageCount);

            String pageSql = "SELECT * FROM document_pages WHERE document_id = ? AND page_number = ?";
            List<Map<String, Object>> pages = jdbcTemplate.queryForList(pageSql, documentId, pageNumber);

            if (pages.isEmpty()) {
                System.err.println("[ERROR] 404 诊断 - 文档ID: " + documentId + ", 请求页码: " + pageNumber);
                System.err.println("[ERROR] 诊断信息 - 文档处理状态: " + document.get("processing_status"));
                System.err.println("[ERROR] 诊断信息 - 文档处理错误: " + document.get("processing_error"));
                
                // 检查该文档是否有任何页面已入库
                List<Map<String, Object>> allPages = jdbcTemplate.queryForList(
                    "SELECT page_number FROM document_pages WHERE document_id = ?", documentId);
                System.err.println("[ERROR] 诊断信息 - 该文档已存在的页码列表: " + allPages);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new PageResponse(false, "页面不存在。文档状态: " + document.get("processing_status") +
                                        ". 已入库页数: " + allPages.size(), null)
                );
            }

            Map<String, Object> page = pages.get(0);

            // 4. 获取该页面的高亮
            String highlightSql = "SELECT * FROM document_highlights WHERE document_id = ? AND page = ? AND user_id = ?";
            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightSql, documentId, pageNumber, userId);

            // 5. 获取该页面的笔记（关联高亮信息）
            String noteSql = "SELECT dn.*, dh.text as highlight_text, dh.color as highlight_color " +
                    "FROM document_notes dn " +
                    "LEFT JOIN document_highlights dh ON dn.highlight_id = dh.highlight_id " +
                    "WHERE dn.document_id = ? AND dn.page = ? AND dn.user_id = ?";
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(noteSql, documentId, pageNumber, userId);

            // 6. 构建响应数据
            PageData pageData = new PageData();
            pageData.setId("page_" + documentId + "_" + pageNumber);
            pageData.setDocumentId(documentId);
            pageData.setPageNumber(pageNumber);
            pageData.setContent((String) page.get("content"));
            pageData.setHtmlContent((String) page.get("html_content"));
            pageData.setWordCount(page.get("word_count") != null ? ((Number) page.get("word_count")).intValue() : 0);
            pageData.setCharacterCount(page.get("character_count") != null ? ((Number) page.get("character_count")).intValue() : 0);
            pageData.setHasImages(page.get("has_images") != null && (boolean) page.get("has_images"));

            // 设置图片
            if (page.get("images") != null) {
                try {
                    String imagesJson = (String) page.get("images");
                    // 简单处理，实际应该解析JSON
                    if (imagesJson.startsWith("[") && imagesJson.endsWith("]")) {
                        // 这里简化处理，实际应该使用JSON解析器
                        Map<String, Object> image = new HashMap<>();
                        image.put("url", "default_image.jpg");
                        image.put("alt", "文档图片");
                        pageData.getImages().add(image);
                    }
                } catch (Exception e) {
                    System.err.println("解析图片数据失败: " + e.getMessage());
                }
            }

            // 设置高亮
            for (Map<String, Object> highlight : highlights) {
                Map<String, Object> hl = new HashMap<>();
                hl.put("id", highlight.get("highlight_id"));
                hl.put("documentId", highlight.get("document_id"));
                hl.put("text", highlight.get("text"));
                hl.put("page", highlight.get("page"));
                hl.put("color", highlight.get("color"));
                hl.put("note", highlight.get("note"));
                hl.put("createdAt", highlight.get("created_at"));
                hl.put("updatedAt", highlight.get("updated_at"));
                pageData.getHighlights().add(hl);
            }

            // 设置笔记
            for (Map<String, Object> note : notes) {
                Map<String, Object> nt = new HashMap<>();
                nt.put("id", note.get("note_id"));
                nt.put("documentId", note.get("document_id"));
                nt.put("content", note.get("content"));
                nt.put("page", note.get("page"));
                nt.put("highlightId", note.get("highlight_id"));
                nt.put("createdAt", note.get("created_at"));
                nt.put("updatedAt", note.get("updated_at"));
                pageData.getNotes().add(nt);
            }

            // 设置元数据
            pageData.getMetadata().put("title", document.get("title"));
            pageData.getMetadata().put("author", document.get("author"));
            pageData.getMetadata().put("language", document.get("language"));

            // 设置上下页
            if (pageNumber < totalPages) {
                pageData.setNextPage(pageNumber + 1);
            }
            if (pageNumber > 1) {
                pageData.setPrevPage(pageNumber - 1);
            }

            // 7. 记录阅读历史
            String historySql = "INSERT INTO reading_history (user_id, document_id, page, start_time) VALUES (?, ?, ?, NOW()) " +
                    "ON DUPLICATE KEY UPDATE page = ?, start_time = NOW()";
            jdbcTemplate.update(historySql, userId, documentId, pageNumber, pageNumber);

            // 8. 自动更新文档主表的最后阅读时间和初始进度（如果尚未开始阅读）
            // 这样可以确保“打开文档”动作能立即将文档状态变为“正在阅读”
            String updateDocSql = "UPDATE documents SET last_read_at = NOW(), " +
                    "reading_progress = CASE WHEN reading_progress = 0 THEN 0.1 ELSE reading_progress END " +
                    "WHERE document_id = ?";
            jdbcTemplate.update(updateDocSql, documentId);

            // 8. 准备响应
            PageResponse response = new PageResponse(true, "获取页面成功", pageData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取文档页面过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new PageResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}