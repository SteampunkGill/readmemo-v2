package com.vue.readingapp.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.vue.readingapp.ocr.OcrService; // 假设 OcrService 在这个包下
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Component
public class DocumentProcessingScheduler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OcrService ocrService;

    // 每30秒执行一次
    @Scheduled(fixedDelay = 30000)
    public void processDocumentQueue() {
        System.out.println("INFO: [" + LocalDateTime.now() + "] Checking for documents to process...");

        try {
            // 1. 确保必要的数据库表存在
            ensureTablesExist();

            // 2. 查询待处理的文档队列
            //    - 优先处理 priority 较高的任务
            //    - 对于相同 priority 的任务，优先处理创建时间较早的
            //    - 限制每次处理的数量，避免一次性加载过多任务
            //    - 注意：这里的 sql 语句假设 queue_id 是数字类型，但会增加类型转换的健壮性
            String sql = "SELECT queue_id, document_id FROM document_processing_queue WHERE status = 'pending' ORDER BY priority DESC, created_at ASC LIMIT 200";

            List<Map<String, Object>> pendingTasks = jdbcTemplate.queryForList(sql);

            if (pendingTasks.isEmpty()) {
                System.out.println("INFO: [" + LocalDateTime.now() + "] No pending documents to process.");
                return;
            }

            System.out.println("INFO: [" + LocalDateTime.now() + "] Found " + pendingTasks.size() + " pending documents to process.");

            for (Map<String, Object> task : pendingTasks) {
                // --- 健壮的 queue_id 类型处理 ---
                Object queueIdObject = task.get("queue_id");
                Integer queueId = null;

                if (queueIdObject instanceof Integer) {
                    queueId = (Integer) queueIdObject;
                    System.out.println("DEBUG: [" + LocalDateTime.now() + "] Got queue_id as Integer: " + queueId);
                } else if (queueIdObject instanceof Long) {
                    queueId = ((Long) queueIdObject).intValue(); // 尝试转换为 Integer
                    System.out.println("DEBUG: [" + LocalDateTime.now() + "] Got queue_id as Long, converted to Integer: " + queueId);
                } else if (queueIdObject instanceof String) {
                    try {
                        queueId = Integer.parseInt((String) queueIdObject); // 尝试从 String 解析
                        System.out.println("DEBUG: [" + LocalDateTime.now() + "] Got queue_id as String, parsed to Integer: " + queueId);
                    } catch (NumberFormatException e) {
                        System.err.println("ERROR: [" + LocalDateTime.now() + "] Failed to parse queue_id '" + queueIdObject + "' as Integer. Skipping task.");
                        continue; // 跳过此任务，继续下一个
                    }
                } else if (queueIdObject != null) {
                    // 如果是其他未预期的数字类型（如 BigInteger），尝试转换为 int
                    try {
                        queueId = ((Number) queueIdObject).intValue();
                        System.out.println("DEBUG: [" + LocalDateTime.now() + "] Got queue_id as unexpected Number type, converted to Integer: " + queueId);
                    } catch (Exception e) {
                        System.err.println("ERROR: [" + LocalDateTime.now() + "] Unexpected type for queue_id: " + queueIdObject.getClass().getName() + ". Skipping task.");
                        continue; // 跳过此任务
                    }
                }

                if (queueId == null) {
                    System.err.println("ERROR: [" + LocalDateTime.now() + "] queue_id is null or could not be determined. Skipping task.");
                    continue; // 如果 queueId 仍然是 null，跳过此任务
                }
                // --- queue_id 类型处理结束 ---

                // 明确将 document_id 作为字符串处理，以适配 VARCHAR 类型的数据库字段
                String documentId = task.get("document_id").toString();

                System.out.println("INFO: [" + LocalDateTime.now() + "] Processing document with document_id: '" + documentId + "' (queue_id: " + queueId + ")");

                try {
                    // 3. 更新状态为 'processing'，标记为正在处理
                    //    - 同时更新 updated_at 字段
                    String updateSql = "UPDATE document_processing_queue SET status = 'processing', updated_at = ? WHERE queue_id = ?";
                    jdbcTemplate.update(updateSql, LocalDateTime.now(), queueId);

                    // 4. 调用 OcrService 处理文档
                    //    - 假设 ocrService.processDocument(String documentId) 返回一个布尔值表示是否成功
                    boolean success = ocrService.processDocument(Integer.valueOf(documentId));

                    if (success) {
                        // 5. 如果处理成功，更新状态为 'completed'
                        String completeSql = "UPDATE document_processing_queue SET status = 'completed', updated_at = ? WHERE queue_id = ?";
                        jdbcTemplate.update(completeSql, LocalDateTime.now(), queueId);
                        System.out.println("INFO: [" + LocalDateTime.now() + "] Document '" + documentId + "' processed successfully.");
                    } else {
                        // 6. 如果 OCR 服务返回处理失败，更新状态为 'failed'
                        String failSql = "UPDATE document_processing_queue SET status = 'failed', error_message = ?, updated_at = ? WHERE queue_id = ?";
                        jdbcTemplate.update(failSql, "OCR processing returned false", LocalDateTime.now(), queueId);
                        System.out.println("ERROR: [" + LocalDateTime.now() + "] Document '" + documentId + "' processing returned false.");
                    }

                } catch (Exception e) {
                    // 7. 捕获处理过程中可能发生的任何异常
                    System.err.println("ERROR: [" + LocalDateTime.now() + "] Failed to process document '" + documentId + "' (queue_id: " + queueId + "): " + e.getMessage());
                    e.printStackTrace(); // 打印详细堆栈信息以便调试

                    // 更新状态为 'failed'，并记录错误信息
                    String failSql = "UPDATE document_processing_queue SET status = 'failed', error_message = ?, updated_at = ? WHERE queue_id = ?";
                    jdbcTemplate.update(failSql, e.getMessage(), LocalDateTime.now(), queueId);
                }
            }

        } catch (Exception e) {
            // 捕获查询待处理任务过程中可能发生的异常
            System.err.println("ERROR: [" + LocalDateTime.now() + "] Failed to fetch pending tasks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 确保 'document_processing_queue' 和 'ocr_tasks' 表存在。
     * 如果不存在，则创建它们。
     * 这有助于在首次启动应用或数据库为空时自动初始化表结构。
     */
    private void ensureTablesExist() {
        try {
            // 检查 document_processing_queue 表是否存在
            // 使用 information_schema.tables 是标准的 SQL 方式
            // 注意：DATABASE() 函数可能在某些数据库（如 PostgreSQL）中需要替换为 CURRENT_DATABASE() 或其他方式
            String checkQueueTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'document_processing_queue'";
            Integer queueTableCount = jdbcTemplate.queryForObject(checkQueueTableSql, Integer.class);

            if (queueTableCount == null || queueTableCount == 0) {
                System.out.println("INFO: Creating table 'document_processing_queue'...");
                String createQueueTableSql = "CREATE TABLE document_processing_queue (" +
                        "queue_id INT AUTO_INCREMENT PRIMARY KEY," +
                        "document_id VARCHAR(255) NOT NULL," +
                        "status VARCHAR(50) NOT NULL DEFAULT 'pending'," +
                        "priority INT DEFAULT 1," +
                        "error_message TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "INDEX idx_document_id (document_id)," +
                        "INDEX idx_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                jdbcTemplate.execute(createQueueTableSql);
                System.out.println("INFO: Table 'document_processing_queue' created successfully.");
            } else {
                // 检查并修复缺失的列 (使用更兼容的方式，不依赖 IF NOT EXISTS)
                try {
                    String checkColumnSql = "SELECT COLUMN_NAME FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'document_processing_queue'";
                    List<String> existingColumns = jdbcTemplate.queryForList(checkColumnSql, String.class);
                    
                    if (!existingColumns.contains("error_message")) {
                        System.out.println("INFO: Adding missing column 'error_message' to 'document_processing_queue'");
                        jdbcTemplate.execute("ALTER TABLE document_processing_queue ADD COLUMN error_message TEXT AFTER priority");
                    }
                    
                    if (!existingColumns.contains("updated_at")) {
                        System.out.println("INFO: Adding missing column 'updated_at' to 'document_processing_queue'");
                        jdbcTemplate.execute("ALTER TABLE document_processing_queue ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at");
                    }

                    // 确保 status 长度足够
                    jdbcTemplate.execute("ALTER TABLE document_processing_queue MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'pending'");
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to check or alter table columns: " + e.getMessage());
                }
            }

            // 检查 ocr_tasks 表是否存在 (假设 OcrService 使用这个表)
            String checkTaskTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'ocr_tasks'";
            Integer taskTableCount = jdbcTemplate.queryForObject(checkTaskTableSql, Integer.class);

            if (taskTableCount == null || taskTableCount == 0) {
                System.out.println("INFO: Creating table 'ocr_tasks'...");
                String createTaskTableSql = "CREATE TABLE ocr_tasks (" +
                        "task_id VARCHAR(50) PRIMARY KEY," + // OCR 任务的唯一ID
                        "document_id VARCHAR(255) NOT NULL," + // 关联的文档ID
                        "page_number INT," + // 文档页码
                        "status VARCHAR(20) NOT NULL," + // 任务状态 (e.g., PENDING, PROCESSING, COMPLETED, FAILED)
                        "progress INT DEFAULT 0," + // 进度百分比
                        "options_json TEXT," + // OCR 处理的选项，JSON格式
                        "result_json TEXT," + // OCR 处理结果，JSON格式
                        "error_message TEXT," + // 错误信息
                        "estimated_time INT," + // 预估处理时间 (秒)
                        "started_at TIMESTAMP," + // 开始处理时间
                        "completed_at TIMESTAMP," + // 完成处理时间
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," + // 任务创建时间
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," + // 最后更新时间
                        "INDEX idx_document_id_ocr (document_id)," + // 为 document_id 创建索引
                        "INDEX idx_status_ocr (status)" + // 为 status 创建索引
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                jdbcTemplate.execute(createTaskTableSql);
                System.out.println("INFO: Table 'ocr_tasks' created successfully.");
            } else {
                // System.out.println("INFO: Table 'ocr_tasks' already exists.");
            }

        } catch (Exception e) {
            System.err.println("ERROR: [" + LocalDateTime.now() + "] Failed to ensure tables exist: " + e.getMessage());
            e.printStackTrace();
            // 在这里可以考虑抛出更严重的异常，或者标记应用为不可用，因为数据库表是核心依赖
        }
    }
}