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
public class ReviewGetSession {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习会话详情请求 ===");
        System.out.println("请求参数: " + request);
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

    public static class ReviewSessionResponse {
        private boolean success;
        private String message;
        private ReviewSessionDetail data;

        public ReviewSessionResponse(boolean success, String message, ReviewSessionDetail data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReviewSessionDetail getData() { return data; }
        public void setData(ReviewSessionDetail data) { this.data = data; }
    }

    public static class ReviewSessionDetail {
        private String id;
        private String mode;
        private int total_words;
        private int correct_words;
        private double accuracy;
        private int duration;
        private String language;
        private String completed_at;
        private String created_at;
        private List<SessionWord> words;
        private Map<String, Object> stats;

        public ReviewSessionDetail(String id, String mode, int total_words, int correct_words,
                                   double accuracy, int duration, String language, String completed_at,
                                   String created_at, List<SessionWord> words, Map<String, Object> stats) {
            this.id = id;
            this.mode = mode;
            this.total_words = total_words;
            this.correct_words = correct_words;
            this.accuracy = accuracy;
            this.duration = duration;
            this.language = language;
            this.completed_at = completed_at;
            this.created_at = created_at;
            this.words = words;
            this.stats = stats;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public int getTotal_words() { return total_words; }
        public void setTotal_words(int total_words) { this.total_words = total_words; }

        public int getCorrect_words() { return correct_words; }
        public void setCorrect_words(int correct_words) { this.correct_words = correct_words; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getCompleted_at() { return completed_at; }
        public void setCompleted_at(String completed_at) { this.completed_at = completed_at; }

        public String getCreated_at() { return created_at; }
        public void setCreated_at(String created_at) { this.created_at = created_at; }

        public List<SessionWord> getWords() { return words; }
        public void setWords(List<SessionWord> words) { this.words = words; }

        public Map<String, Object> getStats() { return stats; }
        public void setStats(Map<String, Object> stats) { this.stats = stats; }
    }

    public static class SessionWord {
        private int id;
        private String word;
        private String language;
        private String definition;
        private String example;
        private String phonetic;
        private String part_of_speech;
        private boolean correct;
        private Integer response_time;
        private String review_type;
        private int mastery_level_before;
        private int mastery_level_after;

        public SessionWord(int id, String word, String language, String definition, String example,
                           String phonetic, String part_of_speech, boolean correct, Integer response_time,
                           String review_type, int mastery_level_before, int mastery_level_after) {
            this.id = id;
            this.word = word;
            this.language = language;
            this.definition = definition;
            this.example = example;
            this.phonetic = phonetic;
            this.part_of_speech = part_of_speech;
            this.correct = correct;
            this.response_time = response_time;
            this.review_type = review_type;
            this.mastery_level_before = mastery_level_before;
            this.mastery_level_after = mastery_level_after;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }

        public String getPhonetic() { return phonetic; }
        public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

        public String getPart_of_speech() { return part_of_speech; }
        public void setPart_of_speech(String part_of_speech) { this.part_of_speech = part_of_speech; }

        public boolean isCorrect() { return correct; }
        public void setCorrect(boolean correct) { this.correct = correct; }

        public Integer getResponse_time() { return response_time; }
        public void setResponse_time(Integer response_time) { this.response_time = response_time; }

        public String getReview_type() { return review_type; }
        public void setReview_type(String review_type) { this.review_type = review_type; }

        public int getMastery_level_before() { return mastery_level_before; }
        public void setMastery_level_before(int mastery_level_before) { this.mastery_level_before = mastery_level_before; }

        public int getMastery_level_after() { return mastery_level_after; }
        public void setMastery_level_after(int mastery_level_after) { this.mastery_level_after = mastery_level_after; }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ReviewSessionResponse> getReviewSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String sessionId) {

        printRequest("sessionId: " + sessionId);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewSessionResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewSessionResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 获取会话基本信息
            String sessionSql = "SELECT session_id, mode, total_words, correct_words, " +
                    "accuracy, duration, completed_at, created_at " +
                    "FROM review_sessions " +
                    "WHERE session_id = ? AND user_id = ?";

            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, sessionId, userId);
            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ReviewSessionResponse(false, "复习会话不存在", null)
                );
            }

            Map<String, Object> session = sessions.get(0);

            // 3. 获取会话中的单词详情
            List<SessionWord> sessionWords = getSessionWords(sessionId, userId);

            // 4. 计算统计信息
            Map<String, Object> stats = calculateSessionStats(sessionWords);

            // 5. 确定语言
            String language = determineSessionLanguage(sessionWords);

            // 6. 构建响应数据
            ReviewSessionDetail detail = new ReviewSessionDetail(
                    (String) session.get("session_id"),
                    (String) session.get("mode"),
                    ((Number) session.get("total_words")).intValue(),
                    ((Number) session.get("correct_words")).intValue(),
                    Math.round(((Number) session.get("accuracy")).doubleValue() * 100.0) / 100.0,
                    ((Number) session.get("duration")).intValue(),
                    language,
                    session.get("completed_at").toString(),
                    session.get("created_at").toString(),
                    sessionWords,
                    stats
            );

            ReviewSessionResponse response = new ReviewSessionResponse(true, "获取复习会话详情成功", detail);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习会话详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReviewSessionResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private List<SessionWord> getSessionWords(String sessionId, int userId) {
        try {
            // 这里简化处理，实际应该从review_items表获取
            // 由于数据库设计中没有review_items表，我们返回空列表
            System.out.println("注意：数据库中没有review_items表，返回模拟数据");

            // 模拟数据
            List<SessionWord> words = new ArrayList<>();

            // 模拟3个单词
            words.add(new SessionWord(
                    1, "example", "en", "示例", "This is an example.",
                    "/ɪɡˈzæmpəl/", "noun", true, 1500, "recognition", 3, 4
            ));

            words.add(new SessionWord(
                    2, "practice", "en", "练习", "Practice makes perfect.",
                    "/ˈpræktɪs/", "noun", false, 2000, "recognition", 5, 4
            ));

            words.add(new SessionWord(
                    3, "review", "en", "复习", "I need to review my notes.",
                    "/rɪˈvjuː/", "verb", true, 1200, "recognition", 2, 3
            ));

            return words;

        } catch (Exception e) {
            System.err.println("获取会话单词失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, Object> calculateSessionStats(List<SessionWord> words) {
        Map<String, Object> stats = new HashMap<>();

        if (words.isEmpty()) {
            stats.put("total_words", 0);
            stats.put("correct_words", 0);
            stats.put("incorrect_words", 0);
            stats.put("accuracy", 0);
            stats.put("average_response_time", 0);
            return stats;
        }

        int totalWords = words.size();
        int correctWords = 0;
        int totalResponseTime = 0;
        int responseTimeCount = 0;

        for (SessionWord word : words) {
            if (word.isCorrect()) {
                correctWords++;
            }

            if (word.getResponse_time() != null) {
                totalResponseTime += word.getResponse_time();
                responseTimeCount++;
            }
        }

        int incorrectWords = totalWords - correctWords;
        double accuracy = (double) correctWords / totalWords * 100;
        double averageResponseTime = responseTimeCount > 0 ?
                (double) totalResponseTime / responseTimeCount : 0;

        stats.put("total_words", totalWords);
        stats.put("correct_words", correctWords);
        stats.put("incorrect_words", incorrectWords);
        stats.put("accuracy", Math.round(accuracy * 100.0) / 100.0);
        stats.put("average_response_time", Math.round(averageResponseTime * 100.0) / 100.0);

        // 按语言统计
        Map<String, Integer> languageStats = new HashMap<>();
        for (SessionWord word : words) {
            String lang = word.getLanguage();
            languageStats.put(lang, languageStats.getOrDefault(lang, 0) + 1);
        }
        stats.put("by_language", languageStats);

        return stats;
    }

    private String determineSessionLanguage(List<SessionWord> words) {
        if (words.isEmpty()) {
            return "mixed";
        }

        // 统计语言出现次数
        Map<String, Integer> languageCount = new HashMap<>();
        for (SessionWord word : words) {
            String lang = word.getLanguage();
            languageCount.put(lang, languageCount.getOrDefault(lang, 0) + 1);
        }

        // 找到出现最多的语言
        String dominantLanguage = "mixed";
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : languageCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantLanguage = entry.getKey();
            }
        }

        // 如果所有单词都是同一种语言，返回该语言
        if (maxCount == words.size()) {
            return dominantLanguage;
        }

        return "mixed";
    }
}
