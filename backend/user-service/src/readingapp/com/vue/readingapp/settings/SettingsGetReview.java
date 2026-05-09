package com.vue.readingapp.settings;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user/settings/review")
public class SettingsGetReview {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习设置请求 ===");
        System.out.println("请求数据: " + request);
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

    public static class ReviewSettingsResponse {
        private boolean success;
        private String message;
        private ReviewSettingsData data;

        public ReviewSettingsResponse(boolean success, String message, ReviewSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReviewSettingsData getData() { return data; }
        public void setData(ReviewSettingsData data) { this.data = data; }
    }

    public static class ReviewSettingsData {
        private int dailyGoal;
        private String reviewTime;
        private boolean reminderEnabled;
        private String reminderTime;
        private String difficultyAlgorithm;
        private int newWordsPerDay;
        private int reviewWordsPerDay;
        private boolean autoGenerateReviews;
        private ReviewInterval reviewInterval;
        private boolean showExamples;
        private boolean showImages;
        private boolean autoPlayAudioInReview;
        private boolean shuffleQuestions;
        private boolean enableStreak;
        private int streakGoal;
        private boolean enableNotifications;

        public ReviewSettingsData() {
            // 设置默认值
            this.dailyGoal = 20;
            this.reviewTime = "09:00";
            this.reminderEnabled = true;
            this.reminderTime = "20:00";
            this.difficultyAlgorithm = "sm2";
            this.newWordsPerDay = 10;
            this.reviewWordsPerDay = 50;
            this.autoGenerateReviews = true;
            this.reviewInterval = new ReviewInterval(1, 10, 1, 3);
            this.showExamples = true;
            this.showImages = true;
            this.autoPlayAudioInReview = false;
            this.shuffleQuestions = true;
            this.enableStreak = true;
            this.streakGoal = 7;
            this.enableNotifications = true;
        }

        // Getters and Setters
        public int getDailyGoal() { return dailyGoal; }
        public void setDailyGoal(int dailyGoal) { this.dailyGoal = dailyGoal; }

        public String getReviewTime() { return reviewTime; }
        public void setReviewTime(String reviewTime) { this.reviewTime = reviewTime; }

        public boolean isReminderEnabled() { return reminderEnabled; }
        public void setReminderEnabled(boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

        public String getReminderTime() { return reminderTime; }
        public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

        public String getDifficultyAlgorithm() { return difficultyAlgorithm; }
        public void setDifficultyAlgorithm(String difficultyAlgorithm) { this.difficultyAlgorithm = difficultyAlgorithm; }

        public int getNewWordsPerDay() { return newWordsPerDay; }
        public void setNewWordsPerDay(int newWordsPerDay) { this.newWordsPerDay = newWordsPerDay; }

        public int getReviewWordsPerDay() { return reviewWordsPerDay; }
        public void setReviewWordsPerDay(int reviewWordsPerDay) { this.reviewWordsPerDay = reviewWordsPerDay; }

        public boolean isAutoGenerateReviews() { return autoGenerateReviews; }
        public void setAutoGenerateReviews(boolean autoGenerateReviews) { this.autoGenerateReviews = autoGenerateReviews; }

        public ReviewInterval getReviewInterval() { return reviewInterval; }
        public void setReviewInterval(ReviewInterval reviewInterval) { this.reviewInterval = reviewInterval; }

        public boolean isShowExamples() { return showExamples; }
        public void setShowExamples(boolean showExamples) { this.showExamples = showExamples; }

        public boolean isShowImages() { return showImages; }
        public void setShowImages(boolean showImages) { this.showImages = showImages; }

        public boolean isAutoPlayAudioInReview() { return autoPlayAudioInReview; }
        public void setAutoPlayAudioInReview(boolean autoPlayAudioInReview) { this.autoPlayAudioInReview = autoPlayAudioInReview; }

        public boolean isShuffleQuestions() { return shuffleQuestions; }
        public void setShuffleQuestions(boolean shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }

        public boolean isEnableStreak() { return enableStreak; }
        public void setEnableStreak(boolean enableStreak) { this.enableStreak = enableStreak; }

        public int getStreakGoal() { return streakGoal; }
        public void setStreakGoal(int streakGoal) { this.streakGoal = streakGoal; }

        public boolean isEnableNotifications() { return enableNotifications; }
        public void setEnableNotifications(boolean enableNotifications) { this.enableNotifications = enableNotifications; }
    }

    public static class ReviewInterval {
        private int again;
        private int hard;
        private int good;
        private int easy;

        public ReviewInterval() {}

        public ReviewInterval(int again, int hard, int good, int easy) {
            this.again = again;
            this.hard = hard;
            this.good = good;
            this.easy = easy;
        }

        public int getAgain() { return again; }
        public void setAgain(int again) { this.again = again; }

        public int getHard() { return hard; }
        public void setHard(int hard) { this.hard = hard; }

        public int getGood() { return good; }
        public void setGood(int good) { this.good = good; }

        public int getEasy() { return easy; }
        public void setEasy(int easy) { this.easy = easy; }
    }

    @GetMapping("")
    public ResponseEntity<ReviewSettingsResponse> getReviewSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewSettingsResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewSettingsResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = ((Number) sessions.get(0).get("user_id")).intValue();

            // 3. 查询复习设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'review'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 4. 构建响应数据
            ReviewSettingsData settingsData = new ReviewSettingsData();
            ReviewInterval interval = new ReviewInterval();

            for (Map<String, Object> setting : settingsList) {
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                switch (key) {
                    case "dailyGoal":
                        try {
                            settingsData.setDailyGoal(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setDailyGoal(20);
                        }
                        break;
                    case "reviewTime":
                        settingsData.setReviewTime(value);
                        break;
                    case "reminderEnabled":
                        settingsData.setReminderEnabled("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "reminderTime":
                        settingsData.setReminderTime(value);
                        break;
                    case "difficultyAlgorithm":
                        settingsData.setDifficultyAlgorithm(value);
                        break;
                    case "newWordsPerDay":
                        try {
                            settingsData.setNewWordsPerDay(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setNewWordsPerDay(10);
                        }
                        break;
                    case "reviewWordsPerDay":
                        try {
                            settingsData.setReviewWordsPerDay(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setReviewWordsPerDay(50);
                        }
                        break;
                    case "autoGenerateReviews":
                        settingsData.setAutoGenerateReviews("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "reviewInterval_again":
                        try {
                            interval.setAgain(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            interval.setAgain(1);
                        }
                        break;
                    case "reviewInterval_hard":
                        try {
                            interval.setHard(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            interval.setHard(10);
                        }
                        break;
                    case "reviewInterval_good":
                        try {
                            interval.setGood(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            interval.setGood(1);
                        }
                        break;
                    case "reviewInterval_easy":
                        try {
                            interval.setEasy(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            interval.setEasy(3);
                        }
                        break;
                    case "showExamples":
                        settingsData.setShowExamples("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "showImages":
                        settingsData.setShowImages("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "autoPlayAudioInReview":
                        settingsData.setAutoPlayAudioInReview("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "shuffleQuestions":
                        settingsData.setShuffleQuestions("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "enableStreak":
                        settingsData.setEnableStreak("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                    case "streakGoal":
                        try {
                            settingsData.setStreakGoal(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            settingsData.setStreakGoal(7);
                        }
                        break;
                    case "enableNotifications":
                        settingsData.setEnableNotifications("true".equalsIgnoreCase(value) || "1".equals(value));
                        break;
                }
            }

            settingsData.setReviewInterval(interval);

            // 5. 准备响应
            ReviewSettingsResponse response = new ReviewSettingsResponse(true, "获取复习设置成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReviewSettingsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}