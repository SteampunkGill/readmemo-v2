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
public class ReviewResetProgress {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到重置复习进度请求 ===");
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

    public static class ResetProgressRequest {
        private String reset_reason;
        private Boolean reset_mastery;
        private Boolean reset_review_count;

        public String getReset_reason() { return reset_reason; }
        public void setReset_reason(String reset_reason) { this.reset_reason = reset_reason; }

        public Boolean getReset_mastery() { return reset_mastery; }
        public void setReset_mastery(Boolean reset_mastery) { this.reset_mastery = reset_mastery; }

        public Boolean getReset_review_count() { return reset_review_count; }
        public void setReset_review_count(Boolean reset_review_count) { this.reset_review_count = reset_review_count; }
    }

    public static class ResetProgressResponse {
        private boolean success;
        private String message;
        private ResetData data;

        public ResetProgressResponse(boolean success, String message, ResetData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ResetData getData() { return data; }
        public void setData(ResetData data) { this.data = data; }
    }

    public static class ResetData {
        private int user_vocab_id;
        private String word;
        private int new_mastery_level;
        private int new_review_count;
        private String new_next_review_date;
        private String reset_reason;

        public ResetData(int user_vocab_id, String word, int new_mastery_level,
                         int new_review_count, String new_next_review_date, String reset_reason) {
            this.user_vocab_id = user_vocab_id;
            this.word = word;
            this.new_mastery_level = new_mastery_level;
            this.new_review_count = new_review_count;
            this.new_next_review_date = new_next_review_date;
            this.reset_reason = reset_reason;
        }

        public int getUser_vocab_id() { return user_vocab_id; }
        public void setUser_vocab_id(int user_vocab_id) { this.user_vocab_id = user_vocab_id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public int getNew_mastery_level() { return new_mastery_level; }
        public void setNew_mastery_level(int new_mastery_level) { this.new_mastery_level = new_mastery_level; }

        public int getNew_review_count() { return new_review_count; }
        public void setNew_review_count(int new_review_count) { this.new_review_count = new_review_count; }

        public String getNew_next_review_date() { return new_next_review_date; }
        public void setNew_next_review_date(String new_next_review_date) { this.new_next_review_date = new_next_review_date; }

        public String getReset_reason() { return reset_reason; }
        public void setReset_reason(String reset_reason) { this.reset_reason = reset_reason; }
    }

    @PutMapping("/reset/{userVocabId}")
    public ResponseEntity<ResetProgressResponse> resetReviewProgress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userVocabId,
            @RequestBody(required = false) ResetProgressRequest request) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("userVocabId", userVocabId);
        requestParams.put("request", request);
        printRequest(requestParams);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ResetProgressResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ResetProgressResponse(false, "登录已过期，请重新登录", null)
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
                        new ResetProgressResponse(false, "无效的用户词汇ID", null)
                );
            }

            // 3. 检查用户词汇是否存在且属于当前用户
            String checkSql = "SELECT uv.user_vocab_id, w.word, uv.mastery_level, " +
                    "uv.review_count, uv.next_review_date " +
                    "FROM user_vocabulary uv " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "WHERE uv.user_vocab_id = ? AND uv.user_id = ?";

            List<Map<String, Object>> vocabList = jdbcTemplate.queryForList(checkSql, vocabId, userId);
            if (vocabList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ResetProgressResponse(false, "用户词汇不存在", null)
                );
            }

            Map<String, Object> vocab = vocabList.get(0);
            String word = (String) vocab.get("word");
            int currentMasteryLevel = ((Number) vocab.get("mastery_level")).intValue();
            int currentReviewCount = ((Number) vocab.get("review_count")).intValue();

            // 4. 确定重置参数
            boolean resetMastery = request != null && request.getReset_mastery() != null ?
                    request.getReset_mastery() : true;
            boolean resetReviewCount = request != null && request.getReset_review_count() != null ?
                    request.getReset_review_count() : false;
            String resetReason = request != null && request.getReset_reason() != null ?
                    request.getReset_reason() : "user_reset";

            // 5. 计算新的值
            int newMasteryLevel = resetMastery ? 0 : currentMasteryLevel;
            int newReviewCount = resetReviewCount ? 0 : currentReviewCount;

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime newNextReviewDate;

            if (resetMastery) {
                // 如果重置了掌握等级，下次复习时间设为明天
                newNextReviewDate = now.plusDays(1);
            } else {
                // 否则保持原样
                newNextReviewDate = ((java.sql.Timestamp) vocab.get("next_review_date")).toLocalDateTime();
            }

            // 6. 更新数据库
            StringBuilder updateSql = new StringBuilder("UPDATE user_vocabulary SET ");
            List<Object> params = new ArrayList<>();

            if (resetMastery) {
                updateSql.append("mastery_level = ?, ");
                params.add(newMasteryLevel);

                // 如果掌握等级重置为0，也重置已掌握状态
                updateSql.append("is_mastered = ?, ");
                params.add(0);
            }

            if (resetReviewCount) {
                updateSql.append("review_count = ?, ");
                params.add(newReviewCount);
            }

            if (resetMastery) {
                updateSql.append("next_review_date = ?, ");
                params.add(newNextReviewDate);
            }

            updateSql.append("last_reset_at = ?, reset_count = COALESCE(reset_count, 0) + 1 ");
            params.add(now);

            updateSql.append("WHERE user_vocab_id = ? AND user_id = ?");
            params.add(vocabId);
            params.add(userId);

            System.out.println("执行更新SQL: " + updateSql.toString());
            System.out.println("参数: " + params);

            int updated = jdbcTemplate.update(updateSql.toString(), params.toArray());

            printQueryResult("更新记录数: " + updated);

            if (updated > 0) {
                // 7. 记录重置历史
                recordResetHistory(userId, vocabId, resetReason, resetMastery, resetReviewCount, now);

                // 8. 构建响应数据
                ResetData data = new ResetData(
                        vocabId,
                        word,
                        newMasteryLevel,
                        newReviewCount,
                        newNextReviewDate.toString(),
                        resetReason
                );

                ResetProgressResponse response = new ResetProgressResponse(true, "重置复习进度成功", data);
                printResponse(response);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new ResetProgressResponse(false, "重置复习进度失败", null)
                );
            }

        } catch (Exception e) {
            System.err.println("重置复习进度过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResetProgressResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void recordResetHistory(int userId, int userVocabId, String resetReason,
                                    boolean resetMastery, boolean resetReviewCount, LocalDateTime now) {
        try {
            // 检查是否有reset_history表，如果没有就不记录
            // 这里简化处理，只打印日志
            System.out.println("记录重置历史: 用户ID=" + userId +
                    ", 词汇ID=" + userVocabId +
                    ", 原因=" + resetReason +
                    ", 重置掌握等级=" + resetMastery +
                    ", 重置复习次数=" + resetReviewCount);
        } catch (Exception e) {
            System.err.println("记录重置历史失败: " + e.getMessage());
        }
    }
}
