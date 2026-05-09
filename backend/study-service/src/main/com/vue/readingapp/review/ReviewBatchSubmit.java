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
public class ReviewBatchSubmit {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到批量提交复习结果请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("==========================");
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

    public static class BatchSubmitRequest {
        private List<BatchReviewResult> results;
        private String session_id;
        private Integer duration;
        private String mode;

        public List<BatchReviewResult> getResults() { return results; }
        public void setResults(List<BatchReviewResult> results) { this.results = results; }

        public String getSession_id() { return session_id; }
        public void setSession_id(String session_id) { this.session_id = session_id; }

        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
    }

    public static class BatchReviewResult {
        private String word_id;
        private boolean correct;
        private Integer response_time;
        private String review_type;

        public String getWord_id() { return word_id; }
        public void setWord_id(String word_id) { this.word_id = word_id; }

        public boolean isCorrect() { return correct; }
        public void setCorrect(boolean correct) { this.correct = correct; }

        public Integer getResponse_time() { return response_time; }
        public void setResponse_time(Integer response_time) { this.response_time = response_time; }

        public String getReview_type() { return review_type; }
        public void setReview_type(String review_type) { this.review_type = review_type; }
    }

    public static class BatchSubmitResponse {
        private boolean success;
        private String message;
        private BatchResultData data;

        public BatchSubmitResponse(boolean success, String message, BatchResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchResultData getData() { return data; }
        public void setData(BatchResultData data) { this.data = data; }
    }

    public static class BatchResultData {
        private String session_id;
        private int total_words;
        private int correct_words;
        private int incorrect_words;
        private double accuracy;
        private int duration;
        private double average_response_time;
        private int updated_words_count;
        private String timestamp;

        public BatchResultData(String session_id, int total_words, int correct_words,
                               int incorrect_words, double accuracy, int duration,
                               double average_response_time, int updated_words_count, String timestamp) {
            this.session_id = session_id;
            this.total_words = total_words;
            this.correct_words = correct_words;
            this.incorrect_words = incorrect_words;
            this.accuracy = accuracy;
            this.duration = duration;
            this.average_response_time = average_response_time;
            this.updated_words_count = updated_words_count;
            this.timestamp = timestamp;
        }

        public String getSession_id() { return session_id; }
        public void setSession_id(String session_id) { this.session_id = session_id; }

        public int getTotal_words() { return total_words; }
        public void setTotal_words(int total_words) { this.total_words = total_words; }

        public int getCorrect_words() { return correct_words; }
        public void setCorrect_words(int correct_words) { this.correct_words = correct_words; }

        public int getIncorrect_words() { return incorrect_words; }
        public void setIncorrect_words(int incorrect_words) { this.incorrect_words = incorrect_words; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public double getAverage_response_time() { return average_response_time; }
        public void setAverage_response_time(double average_response_time) { this.average_response_time = average_response_time; }

        public int getUpdated_words_count() { return updated_words_count; }
        public void setUpdated_words_count(int updated_words_count) { this.updated_words_count = updated_words_count; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    @PostMapping("/batch-submit")
    public ResponseEntity<BatchSubmitResponse> batchSubmitReview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BatchSubmitRequest request) {

        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchSubmitResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchSubmitResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 验证请求数据
            if (request.getResults() == null || request.getResults().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchSubmitResponse(false, "复习结果不能为空", null)
                );
            }

            if (request.getSession_id() == null || request.getSession_id().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchSubmitResponse(false, "会话ID不能为空", null)
                );
            }

            // 3. 处理批量复习结果
            int totalWords = request.getResults().size();
            int correctWords = 0;
            int totalResponseTime = 0;
            int responseTimeCount = 0;
            int updatedWordsCount = 0;
            LocalDateTime now = LocalDateTime.now();

            // 用于记录更新的单词
            List<Map<String, Object>> updatedWords = new ArrayList<>();

            for (BatchReviewResult result : request.getResults()) {
                // 验证单词ID
                if (result.getWord_id() == null || result.getWord_id().trim().isEmpty()) {
                    continue;
                }

                // 统计正确单词数
                if (result.isCorrect()) {
                    correctWords++;
                }

                // 统计响应时间
                if (result.getResponse_time() != null && result.getResponse_time() > 0) {
                    totalResponseTime += result.getResponse_time();
                    responseTimeCount++;
                }

                // 更新用户词汇状态
                try {
                    int userVocabId = Integer.parseInt(result.getWord_id());
                    Map<String, Object> updatedWord = updateUserVocabularyBatch(userId, userVocabId, result.isCorrect(), now);
                    if (updatedWord != null) {
                        updatedWords.add(updatedWord);
                        updatedWordsCount++;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("单词ID格式错误: " + result.getWord_id());
                }
            }

            // 4. 计算统计数据
            int incorrectWords = totalWords - correctWords;
            double accuracy = totalWords > 0 ? (correctWords * 100.0 / totalWords) : 0;
            double averageResponseTime = responseTimeCount > 0 ? (totalResponseTime * 1.0 / responseTimeCount) : 0;

            // 5. 创建复习会话记录
            createReviewSessionBatch(userId, request.getSession_id(), totalWords, correctWords,
                    accuracy, request.getDuration() != null ? request.getDuration() : 0,
                    request.getMode() != null ? request.getMode() : "review", now);

            // 6. 更新每日学习统计
            updateDailyLearningStatsBatch(userId, correctWords, incorrectWords, now);

            // 7. 构建响应数据
            BatchResultData data = new BatchResultData(
                    request.getSession_id(),
                    totalWords,
                    correctWords,
                    incorrectWords,
                    Math.round(accuracy * 100.0) / 100.0,
                    request.getDuration() != null ? request.getDuration() : 0,
                    Math.round(averageResponseTime * 100.0) / 100.0,
                    updatedWordsCount,
                    now.toString()
            );

            BatchSubmitResponse response = new BatchSubmitResponse(true, "批量提交复习结果成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量提交复习结果过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchSubmitResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private Map<String, Object> updateUserVocabularyBatch(int userId, int userVocabId, boolean correct, LocalDateTime now) {
        try {
            // 1. 获取当前词汇状态
            String selectSql = "SELECT uv.mastery_level, uv.review_count, w.word " +
                    "FROM user_vocabulary uv " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "WHERE uv.user_vocab_id = ? AND uv.user_id = ?";

            List<Map<String, Object>> vocabList = jdbcTemplate.queryForList(selectSql, userVocabId, userId);
            if (vocabList.isEmpty()) {
                return null;
            }

            Map<String, Object> vocab = vocabList.get(0);
            int currentMasteryLevel = ((Number) vocab.get("mastery_level")).intValue();
            int currentReviewCount = ((Number) vocab.get("review_count")).intValue();
            String word = (String) vocab.get("word");

            // 2. 计算新的掌握等级（简单算法）
            int newMasteryLevel = currentMasteryLevel;
            if (correct) {
                newMasteryLevel = Math.min(currentMasteryLevel + 1, 10); // 最大10级
            } else {
                newMasteryLevel = Math.max(currentMasteryLevel - 1, 0); // 最小0级
            }

            // 3. 计算下次复习时间（基于掌握等级的间隔重复算法）
            int daysToAdd = calculateNextReviewDaysBatch(newMasteryLevel);
            LocalDateTime nextReviewDate = now.plusDays(daysToAdd);

            // 4. 更新数据库
            String updateSql = "UPDATE user_vocabulary SET " +
                    "mastery_level = ?, " +
                    "review_count = review_count + 1, " +
                    "last_reviewed_at = ?, " +
                    "next_review_at = ? " +
                    "WHERE user_vocab_id = ? AND user_id = ?";

            int updated = jdbcTemplate.update(updateSql,
                    newMasteryLevel,
                    now,
                    nextReviewDate,
                    userVocabId,
                    userId
            );

            if (updated > 0) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", userVocabId);
                result.put("word", word);
                result.put("mastery_level", newMasteryLevel);
                result.put("review_count", currentReviewCount + 1);
                result.put("next_review_at", nextReviewDate.toString());
                return result;
            }

            return null;

        } catch (Exception e) {
            System.err.println("批量更新用户词汇状态失败: " + e.getMessage());
            return null;
        }
    }

    private int calculateNextReviewDaysBatch(int masteryLevel) {
        // 简单的间隔重复算法
        switch (masteryLevel) {
            case 0: return 1;   // 第1级：1天后复习
            case 1: return 2;   // 第2级：2天后复习
            case 2: return 4;   // 第3级：4天后复习
            case 3: return 7;   // 第4级：7天后复习
            case 4: return 14;  // 第5级：14天后复习
            case 5: return 30;  // 第6级：30天后复习
            case 6: return 60;  // 第7级：60天后复习
            case 7: return 90;  // 第8级：90天后复习
            case 8: return 180; // 第9级：180天后复习
            case 9: return 365; // 第10级：365天后复习
            default: return 30; // 默认30天
        }
    }

    private void createReviewSessionBatch(int userId, String sessionId, int totalWords,
                                          int correctWords, double accuracy, int duration,
                                          String mode, LocalDateTime now) {
        try {
            String sql = "INSERT INTO review_sessions " +
                    "(session_id, user_id, total_words, correct_words, accuracy, " +
                    "duration, mode, completed_at, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(sql,
                    sessionId,
                    userId,
                    totalWords,
                    correctWords,
                    accuracy,
                    duration,
                    mode,
                    now,
                    now
            );

            System.out.println("批量创建复习会话记录成功: " + sessionId);

        } catch (Exception e) {
            System.err.println("批量创建复习会话记录失败: " + e.getMessage());
        }
    }

    private void updateDailyLearningStatsBatch(int userId, int correctWords, int incorrectWords, LocalDateTime now) {
        try {
            String dateStr = now.toLocalDate().toString();

            // 检查是否已有今日记录
            String checkSql = "SELECT COUNT(*) FROM daily_learning_stats " +
                    "WHERE user_id = ? AND learning_date = ?";

            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, dateStr);

            if (count > 0) {
                // 更新现有记录
                String updateSql = "UPDATE daily_learning_stats SET " +
                        "words_reviewed = words_reviewed + ?, " +
                        "words_correct = words_correct + ?, " +
                        "words_incorrect = words_incorrect + ?, " +
                        "updated_at = ? " +
                        "WHERE user_id = ? AND learning_date = ?";

                jdbcTemplate.update(updateSql,
                        correctWords + incorrectWords,
                        correctWords,
                        incorrectWords,
                        now,
                        userId,
                        dateStr
                );
            } else {
                // 创建新记录
                String insertSql = "INSERT INTO daily_learning_stats " +
                        "(user_id, learning_date, words_reviewed, " +
                        "words_correct, words_incorrect, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(insertSql,
                        userId,
                        dateStr,
                        correctWords + incorrectWords,
                        correctWords,
                        incorrectWords,
                        now,
                        now
                );
            }

            System.out.println("批量更新每日学习统计成功");

        } catch (Exception e) {
            System.err.println("批量更新每日学习统计失败: " + e.getMessage());
        }
    }
}
