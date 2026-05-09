package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyResetLearningStatus {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到重置单词学习状态请求 ===");
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
    public static class ResetLearningStatusResponse {
        private boolean success;
        private String message;
        private VocabularyItemData data;

        public ResetLearningStatusResponse(boolean success, String message, VocabularyItemData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public VocabularyItemData getData() { return data; }
        public void setData(VocabularyItemData data) { this.data = data; }
    }

    public static class VocabularyItemData {
        private Long id;
        private String word;
        private String language;
        private String status;
        private Integer masteryLevel;
        private Integer reviewCount;
        private String lastReviewedAt;
        private String nextReviewAt;
        private String updatedAt;

        public VocabularyItemData(Long id, String word, String language, String status,
                                  Integer masteryLevel, Integer reviewCount,
                                  String lastReviewedAt, String nextReviewAt, String updatedAt) {
            this.id = id;
            this.word = word;
            this.language = language;
            this.status = status;
            this.masteryLevel = masteryLevel;
            this.reviewCount = reviewCount;
            this.lastReviewedAt = lastReviewedAt;
            this.nextReviewAt = nextReviewAt;
            this.updatedAt = updatedAt;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(Integer masteryLevel) { this.masteryLevel = masteryLevel; }

        public Integer getReviewCount() { return reviewCount; }
        public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

        public String getLastReviewedAt() { return lastReviewedAt; }
        public void setLastReviewedAt(String lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }

        public String getNextReviewAt() { return nextReviewAt; }
        public void setNextReviewAt(String nextReviewAt) { this.nextReviewAt = nextReviewAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/{userVocabId}/reset")
    public ResponseEntity<ResetLearningStatusResponse> resetLearningStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userVocabId) {

        printRequest("userVocabId: " + userVocabId);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ResetLearningStatusResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ResetLearningStatusResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 检查生词是否存在且属于当前用户
            String checkSql = "SELECT user_vocab_id, word, language FROM user_vocabulary WHERE user_vocab_id = ? AND user_id = ?";
            List<Map<String, Object>> items = jdbcTemplate.queryForList(checkSql, userVocabId, userId);

            if (items.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ResetLearningStatusResponse(false, "生词不存在或没有权限", null)
                );
            }

            Map<String, Object> item = items.get(0);
            String word = (String) item.get("word");
            String language = (String) item.get("language");

            // 3. 重置学习状态
            String updateSql = "UPDATE user_vocabulary SET status = 'new', mastery_level = 0, review_count = 0, " +
                    "last_reviewed_at = NULL, next_review_at = NULL, updated_at = ? " +
                    "WHERE user_vocab_id = ? AND user_id = ?";

            LocalDateTime currentTime = LocalDateTime.now();
            int updatedRows = jdbcTemplate.update(updateSql, currentTime, userVocabId, userId);
            printQueryResult("更新行数: " + updatedRows);

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new ResetLearningStatusResponse(false, "重置失败", null)
                );
            }

            // 4. 查询更新后的数据
            String selectSql = "SELECT status, mastery_level, review_count, last_reviewed_at, next_review_at, updated_at " +
                    "FROM user_vocabulary WHERE user_vocab_id = ?";

            List<Map<String, Object>> updatedItems = jdbcTemplate.queryForList(selectSql, userVocabId);
            Map<String, Object> updatedItem = updatedItems.get(0);

            // 5. 组装响应数据
            VocabularyItemData itemData = new VocabularyItemData(
                    userVocabId,
                    word,
                    language,
                    (String) updatedItem.get("status"),
                    updatedItem.get("mastery_level") != null ? ((Number) updatedItem.get("mastery_level")).intValue() : 0,
                    updatedItem.get("review_count") != null ? ((Number) updatedItem.get("review_count")).intValue() : 0,
                    updatedItem.get("last_reviewed_at") != null ? updatedItem.get("last_reviewed_at").toString() : null,
                    updatedItem.get("next_review_at") != null ? updatedItem.get("next_review_at").toString() : null,
                    updatedItem.get("updated_at").toString()
            );

            ResetLearningStatusResponse response = new ResetLearningStatusResponse(true, "单词学习状态已重置", itemData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("重置单词学习状态过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResetLearningStatusResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}