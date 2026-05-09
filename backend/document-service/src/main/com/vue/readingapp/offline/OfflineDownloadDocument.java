
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineDownloadDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到下载文档到离线请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("==========================");
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

    // 请求DTO
    public static class DownloadDocumentRequest {
        private String documentId;

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
    }

    // 响应DTO
    public static class DownloadDocumentResponse {
        private boolean success;
        private String message;
        private DownloadDocumentData data;

        public DownloadDocumentResponse(boolean success, String message, DownloadDocumentData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DownloadDocumentData getData() { return data; }
        public void setData(DownloadDocumentData data) { this.data = data; }
    }

    public static class DownloadDocumentData {
        private String offlineDocId;
        private String downloadId;
        private String status;
        private int progress;

        public DownloadDocumentData(String offlineDocId, String downloadId, String status, int progress) {
            this.offlineDocId = offlineDocId;
            this.downloadId = downloadId;
            this.status = status;
            this.progress = progress;
        }

        public String getOfflineDocId() { return offlineDocId; }
        public void setOfflineDocId(String offlineDocId) { this.offlineDocId = offlineDocId; }

        public String getDownloadId() { return downloadId; }
        public void setDownloadId(String downloadId) { this.downloadId = downloadId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
    }

    // 创建 offline_downloads 表（如果不存在）
    private void createOfflineDownloadsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_downloads (" +
                "download_id VARCHAR(50) PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "document_id VARCHAR(50) NOT NULL, " +
                "offline_doc_id VARCHAR(50), " +
                "status VARCHAR(20) DEFAULT 'pending', " +
                "progress INT DEFAULT 0, " +
                "file_size BIGINT DEFAULT 0, " +
                "downloaded_size BIGINT DEFAULT 0, " +
                "speed VARCHAR(50), " +
                "eta VARCHAR(50), " +
                "error TEXT, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_downloads 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_downloads 表失败: " + e.getMessage());
        }
    }

    // 验证用户token
    private Integer validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);

            if (sessions.isEmpty()) {
                return null;
            }

            return (Integer) sessions.get(0).get("user_id");
        } catch (Exception e) {
            System.err.println("验证token失败: " + e.getMessage());
            return null;
        }
    }

    @PostMapping("/documents/{documentId}/download")
    public ResponseEntity<DownloadDocumentResponse> downloadDocumentOffline(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String documentId) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("documentId", documentId);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DownloadDocumentResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DownloadDocumentResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证文档是否存在
            String checkDocSql = "SELECT document_id, title, content FROM documents WHERE document_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkDocSql, documentId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DownloadDocumentResponse(false, "文档不存在", null)
                );
            }

            Map<String, Object> document = documents.get(0);
            String title = (String) document.get("title");
            String content = (String) document.get("content");

            // 3. 创建相关表
            createOfflineDownloadsTableIfNotExists();

            // 4. 检查是否已经离线下载过
            String checkOfflineSql = "SELECT offline_doc_id FROM offline_documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> existingOffline = jdbcTemplate.queryForList(checkOfflineSql, documentId, userId);

            String offlineDocId;
            if (!existingOffline.isEmpty()) {
                // 已经存在，更新版本
                offlineDocId = (String) existingOffline.get(0).get("offline_doc_id");
                String updateSql = "UPDATE offline_documents SET offline_version = offline_version + 1, " +
                        "updated_at = NOW() WHERE offline_doc_id = ?";
                jdbcTemplate.update(updateSql, offlineDocId);
            } else {
                // 创建新的离线文档记录
                offlineDocId = "offline_doc_" + UUID.randomUUID().toString();
                String insertSql = "INSERT INTO offline_documents (offline_doc_id, document_id, user_id, title, content, " +
                        "download_url, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
                jdbcTemplate.update(insertSql, offlineDocId, documentId, userId, title, content,
                        "/api/offline/documents/" + offlineDocId + "/download");
            }

            // 5. 创建下载任务
            String downloadId = "dl_" + UUID.randomUUID().toString();
            String insertDownloadSql = "INSERT INTO offline_downloads (download_id, user_id, document_id, " +
                    "offline_doc_id, status, progress, start_time) VALUES (?, ?, ?, ?, 'pending', 0, NOW())";
            jdbcTemplate.update(insertDownloadSql, downloadId, userId, documentId, offlineDocId);

            // 6. 模拟下载过程（实际项目中应该是异步任务）
            // 这里简单模拟，实际应该使用线程池或消息队列
            new Thread(() -> {
                try {
                    // 更新状态为下载中
                    String updateStatusSql = "UPDATE offline_downloads SET status = 'downloading', progress = 10 WHERE download_id = ?";
                    jdbcTemplate.update(updateStatusSql, downloadId);

                    Thread.sleep(1000); // 模拟下载时间

                    // 更新进度
                    updateStatusSql = "UPDATE offline_downloads SET progress = 50 WHERE download_id = ?";
                    jdbcTemplate.update(updateStatusSql, downloadId);

                    Thread.sleep(1000);

                    // 完成下载
                    updateStatusSql = "UPDATE offline_downloads SET status = 'completed', progress = 100, " +
                            "end_time = NOW() WHERE download_id = ?";
                    jdbcTemplate.update(updateStatusSql, downloadId);

                    // 更新离线文档状态
                    String updateOfflineSql = "UPDATE offline_documents SET is_synced = TRUE, updated_at = NOW() " +
                            "WHERE offline_doc_id = ?";
                    jdbcTemplate.update(updateOfflineSql, offlineDocId);

                } catch (InterruptedException e) {
                    // 下载失败
                    String updateStatusSql = "UPDATE offline_downloads SET status = 'failed', error = ?, " +
                            "end_time = NOW() WHERE download_id = ?";
                    jdbcTemplate.update(updateStatusSql, "下载中断", downloadId);
                }
            }).start();

            // 7. 准备响应数据
            DownloadDocumentData data = new DownloadDocumentData(offlineDocId, downloadId, "pending", 0);
            DownloadDocumentResponse response = new DownloadDocumentResponse(true, "文档下载任务已开始", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("下载文档到离线过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DownloadDocumentResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}