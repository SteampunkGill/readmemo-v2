package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewSubmit {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到提交复习结果请求 ===");
        System.out.println("请求数据: " + request);
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

    public static class SubmitReviewRequest {
        private List<ReviewResult> results;
        private String sessionId;
        private Integer duration;
        private String mode;

        public List<ReviewResult> getResults() { return results; }
        public void setResults(List<ReviewResult> results) { this.results = results; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
    }

    public static class ReviewResult {
        private String wordId;
        private boolean correct;
        private Integer responseTime;
        private String reviewType;

        public String getWordId() { return wordId; }
        public void setWordId(String wordId) { this.wordId = wordId; }

        public boolean isCorrect() { return correct; }
        public void setCorrect(boolean correct) { this.correct = correct; }

        public Integer getResponseTime() { return responseTime; }
        public void setResponseTime(Integer responseTime) { this.responseTime = responseTime; }

        public String getReviewType() { return reviewType; }
        public void setReviewType(String reviewType) { this.reviewType = reviewType; }
    }

    public static class SubmitReviewResponse {
        private boolean success;
        private String message;
        private ReviewResultData data;

        public SubmitReviewResponse(boolean success, String message, ReviewResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReviewResultData getData() { return data; }
        public void setData(ReviewResultData data) { this.data = data; }
    }

    public static class ReviewResultData {
        private String session_id;
        private int total_words;
        private int correct_words;
        private int incorrect_words;
        private double accuracy;
        private int duration;
        private double average_response_time;
        private List<UpdatedWord> updated_words;
        private String timestamp;

        public ReviewResultData(String session_id, int total_words, int correct_words,
                                int incorrect_words, double accuracy, int duration,
                                double average_response_time, List<UpdatedWord> updated_words,
                                String timestamp) {
            this.session_id = session_id;
            this.total_words = total_words;
            this.correct_words = correct_words;
            this.incorrect_words = incorrect_words;
            this.accuracy = accuracy;
            this.duration = duration;
            this.average_response_time = average_response_time;
            this.updated_words = updated_words;
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

        public List<UpdatedWord> getUpdated_words() { return updated_words; }
        public void setUpdated_words(List<UpdatedWord> updated_words) { this.updated_words = updated_words; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class UpdatedWord {
        private int id;
        private String word;
        private int mastery_level;
        private int review_count;
        private String next_review_at;

        public UpdatedWord(int id, String word, int mastery_level, int review_count, String next_review_at) {
            this.id = id;
            this.word = word;
            this.mastery_level = mastery_level;
            this.review_count = review_count;
            this.next_review_at = next_review_at;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public int getMastery_level() { return mastery_level; }
        public void setMastery_level(int mastery_level) { this.mastery_level = mastery_level; }

        public int getReview_count() { return review_count; }
        public void setReview_count(int review_count) { this.review_count = review_count; }

        public String getNext_review_at() { return next_review_at; }
        public void setNext_review_at(String next_review_at) { this.next_review_at = next_review_at; }
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmitReviewResponse> submitReview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SubmitReviewRequest request) {

        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SubmitReviewResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SubmitReviewResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 验证请求数据
            if (request.getResults() == null || request.getResults().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SubmitReviewResponse(false, "复习结果不能为空", null)
                );
            }

            if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SubmitReviewResponse(false, "会话ID不能为空", null)
                );
            }

            // 3. 处理每个复习结果
            int totalWords = request.getResults().size();
            int correctWords = 0;
            int totalResponseTime = 0;
            int responseTimeCount = 0;
            List<UpdatedWord> updatedWords = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (ReviewResult result : request.getResults()) {
                // 验证单词ID
                if (result.getWordId() == null || result.getWordId().trim().isEmpty()) {
                    continue;
                }

                // 统计正确单词数
                if (result.isCorrect()) {
                    correctWords++;
                }

                // 统计响应时间
                if (result.getResponseTime() != null && result.getResponseTime() > 0) {
                    totalResponseTime += result.getResponseTime();
                    responseTimeCount++;
                }

                // 更新用户词汇状态
                try {
                    int userVocabId = Integer.parseInt(result.getWordId());
                    UpdatedWord updatedWord = updateUserVocabulary(userId, userVocabId, result.isCorrect(), now);
                    if (updatedWord != null) {
                        updatedWords.add(updatedWord);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("单词ID格式错误: " + result.getWordId());
                }
            }

            // 4. 计算统计数据
            int incorrectWords = totalWords - correctWords;
            double accuracy = totalWords > 0 ? (correctWords * 100.0 / totalWords) : 0;
            double averageResponseTime = responseTimeCount > 0 ? (totalResponseTime * 1.0 / responseTimeCount) : 0;

            // 5. 创建复习会话记录
            createReviewSession(userId, request.getSessionId(), totalWords, correctWords,
                    accuracy, request.getDuration() != null ? request.getDuration() : 0,
                    request.getMode() != null ? request.getMode() : "review", now);

            // 6. 更新每日学习统计
            updateDailyLearningStats(userId, correctWords, incorrectWords, now);

            // 7. 构建响应数据
            ReviewResultData data = new ReviewResultData(
                    request.getSessionId(),
                    totalWords,
                    correctWords,
                    incorrectWords,
                    Math.round(accuracy * 100.0) / 100.0,
                    request.getDuration() != null ? request.getDuration() : 0,
                    Math.round(averageResponseTime * 100.0) / 100.0,
                    updatedWords,
                    now.toString()
            );

            SubmitReviewResponse response = new SubmitReviewResponse(true, "复习结果提交成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("提交复习结果过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SubmitReviewResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private UpdatedWord updateUserVocabulary(int userId, int userVocabId, boolean correct, LocalDateTime now) {
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
            int currentMasteryLevel = (int) vocab.get("mastery_level");
            int currentReviewCount = (int) vocab.get("review_count");
            String word = (String) vocab.get("word");

            // 2. 计算新的掌握等级（简单算法）
            int newMasteryLevel = currentMasteryLevel;
            if (correct) {
                newMasteryLevel = Math.min(currentMasteryLevel + 1, 10); // 最大10级
            } else {
                newMasteryLevel = Math.max(currentMasteryLevel - 1, 0); // 最小0级
            }

            // 3. 计算下次复习时间（基于掌握等级的间隔重复算法）
            int daysToAdd = calculateNextReviewDays(newMasteryLevel);
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
                return new UpdatedWord(
                        userVocabId,
                        word,
                        newMasteryLevel,
                        currentReviewCount + 1,
                        nextReviewDate.toString()
                );
            }

            return null;

        } catch (Exception e) {
            System.err.println("更新用户词汇状态失败: " + e.getMessage());
            return null;
        }
    }

    private int calculateNextReviewDays(int masteryLevel) {
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

    private void createReviewSession(int userId, String sessionId, int totalWords,
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

            System.out.println("创建复习会话记录成功: " + sessionId);

        } catch (Exception e) {
            System.err.println("创建复习会话记录失败: " + e.getMessage());
        }
    }

    private void updateDailyLearningStats(int userId, int correctWords, int incorrectWords, LocalDateTime now) {
        try {
            String dateStr = now.toLocalDate().toString();

            // 检查是否已有今日记录
            String checkSql = "SELECT COUNT(*) FROM daily_learning_stats " +
                    "WHERE user_id = ? AND date = ?";

            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, dateStr);

            if (count > 0) {
                // 更新现有记录
                String updateSql = "UPDATE daily_learning_stats SET " +
                        "words_reviewed = words_reviewed + ?, " +
                        "words_correct = words_correct + ?, " +
                        "words_incorrect = words_incorrect + ?, " +
                        "updated_at = ? " +
                        "WHERE user_id = ? AND date = ?";

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
                        "(user_id, date, words_reviewed, " +
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

            System.out.println("更新每日学习统计成功");

        } catch (Exception e) {
            System.err.println("更新每日学习统计失败: " + e.getMessage());
        }
    }
}
