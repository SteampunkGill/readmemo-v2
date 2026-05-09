
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineGetStorageStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取离线存储统计请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
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
    public static class StorageStatsResponse {
        private boolean success;
        private String message;
        private StorageStatsData data;

        public StorageStatsResponse(boolean success, String message, StorageStatsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public StorageStatsData getData() { return data; }
        public void setData(StorageStatsData data) { this.data = data; }
    }

    public static class StorageStatsData {
        private long totalSize;
        private int documentCount;
        private int vocabularyCount;
        private int reviewCount;
        private int noteCount;
        private int highlightCount;
        private StorageByType byType;
        private StorageByStatus byStatus;

        public StorageStatsData(long totalSize, int documentCount, int vocabularyCount,
                                int reviewCount, int noteCount, int highlightCount,
                                StorageByType byType, StorageByStatus byStatus) {
            this.totalSize = totalSize;
            this.documentCount = documentCount;
            this.vocabularyCount = vocabularyCount;
            this.reviewCount = reviewCount;
            this.noteCount = noteCount;
            this.highlightCount = highlightCount;
            this.byType = byType;
            this.byStatus = byStatus;
        }

        // Getters and Setters
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

        public int getDocumentCount() { return documentCount; }
        public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

        public int getVocabularyCount() { return vocabularyCount; }
        public void setVocabularyCount(int vocabularyCount) { this.vocabularyCount = vocabularyCount; }

        public int getReviewCount() { return reviewCount; }
        public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

        public int getNoteCount() { return noteCount; }
        public void setNoteCount(int noteCount) { this.noteCount = noteCount; }

        public int getHighlightCount() { return highlightCount; }
        public void setHighlightCount(int highlightCount) { this.highlightCount = highlightCount; }

        public StorageByType getByType() { return byType; }
        public void setByType(StorageByType byType) { this.byType = byType; }

        public StorageByStatus getByStatus() { return byStatus; }
        public void setByStatus(StorageByStatus byStatus) { this.byStatus = byStatus; }
    }

    public static class StorageByType {
        private long documents;
        private long vocabulary;
        private long reviews;
        private long notes;
        private long highlights;

        public StorageByType(long documents, long vocabulary, long reviews,
                             long notes, long highlights) {
            this.documents = documents;
            this.vocabulary = vocabulary;
            this.reviews = reviews;
            this.notes = notes;
            this.highlights = highlights;
        }

        // Getters and Setters
        public long getDocuments() { return documents; }
        public void setDocuments(long documents) { this.documents = documents; }

        public long getVocabulary() { return vocabulary; }
        public void setVocabulary(long vocabulary) { this.vocabulary = vocabulary; }

        public long getReviews() { return reviews; }
        public void setReviews(long reviews) { this.reviews = reviews; }

        public long getNotes() { return notes; }
        public void setNotes(long notes) { this.notes = notes; }

        public long getHighlights() { return highlights; }
        public void setHighlights(long highlights) { this.highlights = highlights; }
    }

    public static class StorageByStatus {
        private int synced;
        private int pending;
        private int failed;

        public StorageByStatus(int synced, int pending, int failed) {
            this.synced = synced;
            this.pending = pending;
            this.failed = failed;
        }

        // Getters and Setters
        public int getSynced() { return synced; }
        public void setSynced(int synced) { this.synced = synced; }

        public int getPending() { return pending; }
        public void setPending(int pending) { this.pending = pending; }

        public int getFailed() { return failed; }
        public void setFailed(int failed) { this.failed = failed; }
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

    @GetMapping("/stats")
    public ResponseEntity<StorageStatsResponse> getOfflineStorageStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new StorageStatsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new StorageStatsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 统计文档数据
            String docStatsSql = "SELECT COUNT(*) as count, SUM(file_size) as total_size, " +
                    "SUM(CASE WHEN is_synced THEN 1 ELSE 0 END) as synced_count " +
                    "FROM offline_documents WHERE user_id = ?";
            List<Map<String, Object>> docStats = jdbcTemplate.queryForList(docStatsSql, userId);

            int documentCount = 0;
            long docTotalSize = 0;
            int docSyncedCount = 0;

            if (!docStats.isEmpty()) {
                Map<String, Object> row = docStats.get(0);
                documentCount = row.get("count") != null ? ((Number) row.get("count")).intValue() : 0;
                docTotalSize = row.get("total_size") != null ? ((Number) row.get("total_size")).longValue() : 0;
                docSyncedCount = row.get("synced_count") != null ? ((Number) row.get("synced_count")).intValue() : 0;
            }

            // 3. 统计词汇数据（假设有offline_vocabulary表）
            String vocabStatsSql = "SELECT COUNT(*) as count FROM offline_vocabulary WHERE user_id = ?";
            List<Map<String, Object>> vocabStats = jdbcTemplate.queryForList(vocabStatsSql, userId);
            int vocabularyCount = vocabStats.isEmpty() ? 0 : ((Number) vocabStats.get(0).get("count")).intValue();

            // 4. 统计复习记录（假设有offline_reviews表）
            String reviewStatsSql = "SELECT COUNT(*) as count FROM offline_reviews WHERE user_id = ?";
            List<Map<String, Object>> reviewStats = jdbcTemplate.queryForList(reviewStatsSql, userId);
            int reviewCount = reviewStats.isEmpty() ? 0 : ((Number) reviewStats.get(0).get("count")).intValue();

            // 5. 统计笔记数据（假设有offline_notes表）
            String noteStatsSql = "SELECT COUNT(*) as count FROM offline_notes WHERE user_id = ?";
            List<Map<String, Object>> noteStats = jdbcTemplate.queryForList(noteStatsSql, userId);
            int noteCount = noteStats.isEmpty() ? 0 : ((Number) noteStats.get(0).get("count")).intValue();

            // 6. 统计高亮数据（假设有offline_highlights表）
            String highlightStatsSql = "SELECT COUNT(*) as count FROM offline_highlights WHERE user_id = ?";
            List<Map<String, Object>> highlightStats = jdbcTemplate.queryForList(highlightStatsSql, userId);
            int highlightCount = highlightStats.isEmpty() ? 0 : ((Number) highlightStats.get(0).get("count")).intValue();

            // 7. 计算总大小（估算）
            long totalSize = docTotalSize +
                    (vocabularyCount * 1024L) + // 假设每个词汇1KB
                    (reviewCount * 512L) +      // 假设每个复习记录512B
                    (noteCount * 2048L) +       // 假设每个笔记2KB
                    (highlightCount * 256L);    // 假设每个高亮256B

            // 8. 按类型统计
            StorageByType byType = new StorageByType(
                    docTotalSize,
                    vocabularyCount * 1024L,
                    reviewCount * 512L,
                    noteCount * 2048L,
                    highlightCount * 256L
            );

            // 9. 按状态统计
            int docPendingCount = documentCount - docSyncedCount;
            StorageByStatus byStatus = new StorageByStatus(
                    docSyncedCount,
                    docPendingCount,
                    0 // 假设没有失败
            );

            // 10. 准备响应数据
            StorageStatsData data = new StorageStatsData(
                    totalSize,
                    documentCount,
                    vocabularyCount,
                    reviewCount,
                    noteCount,
                    highlightCount,
                    byType,
                    byStatus
            );

            printQueryResult(data);

            StorageStatsResponse response = new StorageStatsResponse(true, "获取离线存储统计成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取离线存储统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new StorageStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}