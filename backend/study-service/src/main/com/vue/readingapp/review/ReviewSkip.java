package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewSkip {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到跳过复习请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("========================");
    }

    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    public static class SkipReviewRequest {
        private String skip_reason;
        private Integer skip_days;

        public String getSkip_reason() { return skip_reason; }
        public void setSkip_reason(String skip_reason) { this.skip_reason = skip_reason; }

        public Integer getSkip_days() { return skip_days; }
        public void setSkip_days(Integer skip_days) { this.skip_days = skip_days; }
    }

    public static class SkipReviewResponse {
        private boolean success;
        private String message;
        private SkipData data;

        public SkipReviewResponse(boolean success, String message, SkipData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SkipData getData() { return data; }
        public void setData(SkipData data) { this.data = data; }
    }

    public static class SkipData {
        private int user_vocab_id;
        private String word;
        private String new_next_review_date;
        private String skip_reason;

        public SkipData(int user_vocab_id, String word, String new_next_review_date, String skip_reason) {
            this.user_vocab_id = user_vocab_id;
            this.word = word;
            this.new_next_review_date = new_next_review_date;
            this.skip_reason = skip_reason;
        }

        public int getUser_vocab_id() { return user_vocab_id; }
        public void setUser_vocab_id(int user_vocab_id) { this.user_vocab_id = user_vocab_id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getNew_next_review_date() { return new_next_review_date; }
        public void setNew_next_review_date(String new_next_review_date) { this.new_next_review_date = new_next_review_date; }

        public String getSkip_reason() { return skip_reason; }
        public void setSkip_reason(String skip_reason) { this.skip_reason = skip_reason; }
    }

    @PostMapping("/skip/{userVocabId}")
    public ResponseEntity<SkipReviewResponse> skipReview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userVocabId,
            @RequestBody(required = false) SkipReviewRequest request) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("userVocabId", userVocabId);
        requestParams.put("request", request);
        printRequest(requestParams);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SkipReviewResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SkipReviewResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 验证用户词汇ID
            int vocabId;
            try {
                vocabId = Integer.parseInt(userVocabId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(
                        new SkipReviewResponse(false, "无效的用户词汇ID", null)
                );
            }

            // 3. 检查用户词汇是否存在且属于当前用户
            String checkSql = "SELECT uv.user_vocab_id, w.word, uv.next_review_date " +
                    "FROM user_vocabulary uv " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "WHERE uv.user_vocab_id = ? AND uv.user_id = ?";

            List<Map<String, Object>> vocabList = jdbcTemplate.queryForList(checkSql, vocabId, userId);
            if (vocabList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new SkipReviewResponse(false, "用户词汇不存在", null)
                );
            }

            Map<String, Object> vocab = vocabList.get(0);
            String word = (String) vocab.get("word");
            LocalDateTime currentNextReview = ((java.sql.Timestamp) vocab.get("next_review_date")).toLocalDateTime();

            // 4. 确定跳过的天数
            int skipDays = 1; // 默认跳过1天
            if (request != null && request.getSkip_days() != null) {
                skipDays = request.getSkip_days();
                if (skipDays < 1) skipDays = 1;
                if (skipDays > 30) skipDays = 30; // 限制最大跳过30天
            }

            // 5. 确定跳过原因
            String skipReason = "user_skipped";
            if (request != null && request.getSkip_reason() != null) {
                skipReason = request.getSkip_reason();
            }

            // 6. 计算新的复习日期
            LocalDateTime newNextReviewDate = currentNextReview.plusDays(skipDays);
            LocalDateTime now = LocalDateTime.now();

            // 确保新的复习日期至少是明天
            if (newNextReviewDate.isBefore(now.plusDays(1))) {
                newNextReviewDate = now.plusDays(1);
            }

            // 7. 更新数据库
            String updateSql = "UPDATE user_vocabulary SET next_review_date = ?, " +
                    "last_skipped_at = ?, skip_count = COALESCE(skip_count, 0) + 1 " +
                    "WHERE user_vocab_id = ? AND user_id = ?";

            int updated = jdbcTemplate.update(updateSql,
                    newNextReviewDate,
                    now,
                    vocabId,
                    userId
            );

            printQueryResult("更新记录数: " + updated);

            if (updated > 0) {
                // 8. 记录跳过历史（如果有相关表）
                recordSkipHistory(userId, vocabId, skipReason, skipDays, now);

                // 9. 构建响应数据
                SkipData data = new SkipData(
                        vocabId,
                        word,
                        newNextReviewDate.toString(),
                        skipReason
                );

                SkipReviewResponse response = new SkipReviewResponse(true, "跳过复习成功", data);
                printResponse(response);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new SkipReviewResponse(false, "跳过复习失败", null)
                );
            }

        } catch (Exception e) {
            System.err.println("跳过复习过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SkipReviewResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void recordSkipHistory(int userId, int userVocabId, String skipReason, int skipDays, LocalDateTime now) {
        try {
            // 检查是否有skip_history表，如果没有就不记录
            // 这里简化处理，只打印日志
            System.out.println("记录跳过历史: 用户ID=" + userId +
                    ", 词汇ID=" + userVocabId +
                    ", 原因=" + skipReason +
                    ", 天数=" + skipDays);
        } catch (Exception e) {
            System.err.println("记录跳过历史失败: " + e.getMessage());
        }
    }
}
