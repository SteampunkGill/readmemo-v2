package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderUpdateReadingProgress {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新阅读进度请求 ===");
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

    // 请求DTO
    public static class ProgressRequest {
        private int page;
        private Double percentage;
        private Integer readingTime;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }

        public Integer getReadingTime() { return readingTime; }
        public void setReadingTime(Integer readingTime) { this.readingTime = readingTime; }
    }

    // 响应DTO
    public static class ProgressResponse {
        private boolean success;
        private String message;
        private ProgressData data;

        public ProgressResponse(boolean success, String message, ProgressData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ProgressData getData() { return data; }
        public void setData(ProgressData data) { this.data = data; }
    }

    public static class ProgressData {
        private int documentId;
        private int currentPage;
        private double progressPercentage;
        private int totalReadingTime;
        private String lastReadAt;

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public double getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(double progressPercentage) { this.progressPercentage = progressPercentage; }

        public int getTotalReadingTime() { return totalReadingTime; }
        public void setTotalReadingTime(int totalReadingTime) { this.totalReadingTime = totalReadingTime; }

        public String getLastReadAt() { return lastReadAt; }
        public void setLastReadAt(String lastReadAt) { this.lastReadAt = lastReadAt; }
    }

    @PutMapping("/documents/{documentId}/reading-progress")
    public ResponseEntity<ProgressResponse> updateReadingProgress(
            @PathVariable("documentId") int documentId,
            @RequestBody ProgressRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("request", request);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ProgressResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ProgressResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new ProgressResponse(false, "没有权限更新此文档的进度", null)
                );
            }

            // 3. 获取文档总页数
            String pageCountSql = "SELECT COUNT(*) as total_pages FROM document_pages WHERE document_id = ?";
            List<Map<String, Object>> pageCounts = jdbcTemplate.queryForList(pageCountSql, documentId);
            int totalPages = pageCounts.isEmpty() ? 0 : ((Number) pageCounts.get(0).get("total_pages")).intValue();

            // 4. 计算进度百分比
            double progressPercentage;
            if (request.getPercentage() != null) {
                progressPercentage = request.getPercentage();
            } else {
                progressPercentage = totalPages > 0 ? (request.getPage() * 100.0 / totalPages) : 0;
                progressPercentage = Math.min(100.0, Math.max(0.0, progressPercentage));
            }

            // 5. 更新文档阅读进度
            // 修复：将 read_progress 改为 reading_progress，与 DocumentsGet.java 保持一致
            // 注意：documents 表中没有 current_page 列，已移除
            String updateDocSql = "UPDATE documents SET reading_progress = ?, last_read_at = NOW() " +
                    "WHERE document_id = ?";
            jdbcTemplate.update(updateDocSql, progressPercentage, documentId);

            // 6. 更新阅读历史
            String historySql = "UPDATE reading_history SET page = ?, end_time = NOW() " +
                    "WHERE user_id = ? AND document_id = ? AND end_time IS NULL";
            int updated = jdbcTemplate.update(historySql, request.getPage(), userId, documentId);

            if (updated == 0) {
                // 如果没有正在进行的阅读历史，创建新的
                String insertHistorySql = "INSERT INTO reading_history (user_id, document_id, page, start_time) " +
                        "VALUES (?, ?, ?, NOW())";
                jdbcTemplate.update(insertHistorySql, userId, documentId, request.getPage());
            }

            // 7. 更新每日学习统计
            LocalDateTime now = LocalDateTime.now();
            String dateStr = now.toLocalDate().toString();


            String statsSql = "INSERT INTO daily_learning_stats (user_id, date, documents_read, pages_read, reading_time) " +
                    "VALUES (?, ?, 1, 1, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "documents_read = documents_read + 1, " +
                    "pages_read = pages_read + 1, " +
                    "reading_time = reading_time + COALESCE(?, 0)";

            jdbcTemplate.update(statsSql, userId, dateStr,
                    request.getReadingTime() != null ? request.getReadingTime() : 0,
                    request.getReadingTime() != null ? request.getReadingTime() : 0);

            // 8. 构建响应数据
            ProgressData progressData = new ProgressData();
            progressData.setDocumentId(documentId);
            progressData.setCurrentPage(request.getPage());
            progressData.setProgressPercentage(progressPercentage);
            progressData.setTotalReadingTime(request.getReadingTime() != null ? request.getReadingTime() : 0);
            progressData.setLastReadAt(LocalDateTime.now().toString());

            ProgressResponse response = new ProgressResponse(true, "阅读进度已更新", progressData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新阅读进度过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ProgressResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
