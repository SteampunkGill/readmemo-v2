
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineSyncReview {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到同步复习记录请求 ===");
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
    public static class SyncReviewRequest {
        private ReviewData review;

        public ReviewData getReview() { return review; }
        public void setReview(ReviewData review) { this.review = review; }
    }

    public static class ReviewData {
        private String id;
        private String vocabularyId;
        private int difficulty;
        private String reviewTime;
        private int offlineVersion;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getVocabularyId() { return vocabularyId; }
        public void setVocabularyId(String vocabularyId) { this.vocabularyId = vocabularyId; }

        public int getDifficulty() { return difficulty; }
        public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

        public String getReviewTime() { return reviewTime; }
        public void setReviewTime(String reviewTime) { this.reviewTime = reviewTime; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 响应DTO
    public static class SyncReviewResponse {
        private boolean success;
        private String message;
        private SyncReviewData data;

        public SyncReviewResponse(boolean success, String message, SyncReviewData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SyncReviewData getData() { return data; }
        public void setData(SyncReviewData data) { this.data = data; }
    }

    public static class SyncReviewData {
        private String reviewId;
        private boolean synced;
        private String syncedAt;
        private int offlineVersion;

        public SyncReviewData(String reviewId, boolean synced, String syncedAt, int offlineVersion) {
            this.reviewId = reviewId;
            this.synced = synced;
            this.syncedAt = syncedAt;
            this.offlineVersion = offlineVersion;
        }

        // Getters and Setters
        public String getReviewId() { return reviewId; }
        public void setReviewId(String reviewId) { this.reviewId = reviewId; }

        public boolean isSynced() { return synced; }
        public void setSynced(boolean synced) { this.synced = synced; }

        public String getSyncedAt() { return syncedAt; }
        public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 创建 offline_reviews 表（如果不存在）
    private void createOfflineReviewsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_reviews (" +
                "offline_rev_id VARCHAR(50) PRIMARY KEY, " +
                "review_id VARCHAR(50) NOT NULL, " +
                "user_id INT NOT NULL, " +
                "vocabulary_id VARCHAR(50) NOT NULL, " +
                "difficulty INT DEFAULT 3, " +
                "review_time TIMESTAMP, " +
                "is_synced BOOLEAN DEFAULT FALSE, " +
                "offline_version INT DEFAULT 1, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_reviews 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_reviews 表失败: " + e.getMessage());
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

    @PostMapping("/sync/review")
    public ResponseEntity<SyncReviewResponse> syncReview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SyncReviewRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncReviewResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncReviewResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getReview() == null) {
                return ResponseEntity.badRequest().body(
                        new SyncReviewResponse(false, "复习记录数据不能为空", null)
                );
            }

            ReviewData reviewData = request.getReview();
            if (reviewData.getId() == null || reviewData.getId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SyncReviewResponse(false, "复习记录ID不能为空", null)
                );
            }

            // 3. 创建相关表
            createOfflineReviewsTableIfNotExists();

            // 4. 检查复习记录是否存在（假设有reviews表）
            String checkReviewSql = "SELECT review_id FROM reviews WHERE review_id = ?";
            List<Map<String, Object>> reviews = jdbcTemplate.queryForList(checkReviewSql, reviewData.getId());

            if (reviews.isEmpty()) {
                // 复习记录不存在，创建新记录
                String insertReviewSql = "INSERT INTO reviews (review_id, user_id, vocabulary_id, " +
                        "difficulty, review_time, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())";
                jdbcTemplate.update(insertReviewSql, reviewData.getId(), userId,
                        reviewData.getVocabularyId(), reviewData.getDifficulty(),
                        reviewData.getReviewTime());
            } else {
                // 复习记录存在，更新记录
                String updateReviewSql = "UPDATE reviews SET vocabulary_id = ?, difficulty = ?, " +
                        "review_time = ?, updated_at = NOW() " +
                        "WHERE review_id = ? AND user_id = ?";
                jdbcTemplate.update(updateReviewSql, reviewData.getVocabularyId(),
                        reviewData.getDifficulty(), reviewData.getReviewTime(),
                        reviewData.getId(), userId);
            }

            // 5. 检查是否已有离线复习记录
            String checkOfflineSql = "SELECT offline_rev_id, offline_version FROM offline_reviews " +
                    "WHERE review_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineReviews = jdbcTemplate.queryForList(checkOfflineSql,
                    reviewData.getId(), userId);

            if (offlineReviews.isEmpty()) {
                // 创建离线复习记录
                String offlineRevId = "offline_rev_" + UUID.randomUUID().toString();
                String insertOfflineSql = "INSERT INTO offline_reviews (offline_rev_id, review_id, user_id, " +
                        "vocabulary_id, difficulty, review_time, is_synced, offline_version, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, TRUE, ?, NOW())";
                jdbcTemplate.update(insertOfflineSql, offlineRevId, reviewData.getId(), userId,
                        reviewData.getVocabularyId(), reviewData.getDifficulty(),
                        reviewData.getReviewTime(), reviewData.getOfflineVersion());
            } else {
                // 更新离线复习记录
                Map<String, Object> offlineReview = offlineReviews.get(0);
                int currentVersion = offlineReview.get("offline_version") != null ?
                        ((Number) offlineReview.get("offline_version")).intValue() : 0;

                if (reviewData.getOfflineVersion() > currentVersion) {
                    // 离线版本较新，更新服务器数据
                    String updateOfflineSql = "UPDATE offline_reviews SET vocabulary_id = ?, difficulty = ?, " +
                            "review_time = ?, is_synced = TRUE, offline_version = ?, updated_at = NOW() " +
                            "WHERE review_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateOfflineSql, reviewData.getVocabularyId(),
                            reviewData.getDifficulty(), reviewData.getReviewTime(),
                            reviewData.getOfflineVersion(), reviewData.getId(), userId);
                } else {
                    // 服务器版本较新或相同，标记为已同步
                    String updateSyncSql = "UPDATE offline_reviews SET is_synced = TRUE, updated_at = NOW() " +
                            "WHERE review_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateSyncSql, reviewData.getId(), userId);
                }
            }

            // 6. 准备响应数据
            SyncReviewData data = new SyncReviewData(
                    reviewData.getId(),
                    true,
                    "刚刚同步",
                    reviewData.getOfflineVersion()
            );

            SyncReviewResponse response = new SyncReviewResponse(true, "复习记录同步成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("同步复习记录过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SyncReviewResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}