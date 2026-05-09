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
public class SettingsUpdateReview {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到更新复习设置请求 ===");
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

    public static class UpdateReviewRequest {
        private Integer dailyGoal;
        private String reviewTime;
        private Boolean reminderEnabled;
        private String reminderTime;
        private String difficultyAlgorithm;
        private Integer newWordsPerDay;
        private Integer reviewWordsPerDay;
        private Boolean autoGenerateReviews;
        private ReviewIntervalRequest reviewInterval;
        private Boolean showExamples;
        private Boolean showImages;
        private Boolean autoPlayAudioInReview;
        private Boolean shuffleQuestions;
        private Boolean enableStreak;
        private Integer streakGoal;
        private Boolean enableNotifications;

        // Getters and Setters
        public Integer getDailyGoal() { return dailyGoal; }
        public void setDailyGoal(Integer dailyGoal) { this.dailyGoal = dailyGoal; }

        public String getReviewTime() { return reviewTime; }
        public void setReviewTime(String reviewTime) { this.reviewTime = reviewTime; }

        public Boolean getReminderEnabled() { return reminderEnabled; }
        public void setReminderEnabled(Boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

        public String getReminderTime() { return reminderTime; }
        public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

        public String getDifficultyAlgorithm() { return difficultyAlgorithm; }
        public void setDifficultyAlgorithm(String difficultyAlgorithm) { this.difficultyAlgorithm = difficultyAlgorithm; }

        public Integer getNewWordsPerDay() { return newWordsPerDay; }
        public void setNewWordsPerDay(Integer newWordsPerDay) { this.newWordsPerDay = newWordsPerDay; }

        public Integer getReviewWordsPerDay() { return reviewWordsPerDay; }
        public void setReviewWordsPerDay(Integer reviewWordsPerDay) { this.reviewWordsPerDay = reviewWordsPerDay; }

        public Boolean getAutoGenerateReviews() { return autoGenerateReviews; }
        public void setAutoGenerateReviews(Boolean autoGenerateReviews) { this.autoGenerateReviews = autoGenerateReviews; }

        public ReviewIntervalRequest getReviewInterval() { return reviewInterval; }
        public void setReviewInterval(ReviewIntervalRequest reviewInterval) { this.reviewInterval = reviewInterval; }

        public Boolean getShowExamples() { return showExamples; }
        public void setShowExamples(Boolean showExamples) { this.showExamples = showExamples; }

        public Boolean getShowImages() { return showImages; }
        public void setShowImages(Boolean showImages) { this.showImages = showImages; }

        public Boolean getAutoPlayAudioInReview() { return autoPlayAudioInReview; }
        public void setAutoPlayAudioInReview(Boolean autoPlayAudioInReview) { this.autoPlayAudioInReview = autoPlayAudioInReview; }

        public Boolean getShuffleQuestions() { return shuffleQuestions; }
        public void setShuffleQuestions(Boolean shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }

        public Boolean getEnableStreak() { return enableStreak; }
        public void setEnableStreak(Boolean enableStreak) { this.enableStreak = enableStreak; }

        public Integer getStreakGoal() { return streakGoal; }
        public void setStreakGoal(Integer streakGoal) { this.streakGoal = streakGoal; }

        public Boolean getEnableNotifications() { return enableNotifications; }
        public void setEnableNotifications(Boolean enableNotifications) { this.enableNotifications = enableNotifications; }
    }

    public static class ReviewIntervalRequest {
        private Integer again;
        private Integer hard;
        private Integer good;
        private Integer easy;

        public Integer getAgain() { return again; }
        public void setAgain(Integer again) { this.again = again; }

        public Integer getHard() { return hard; }
        public void setHard(Integer hard) { this.hard = hard; }

        public Integer getGood() { return good; }
        public void setGood(Integer good) { this.good = good; }

        public Integer getEasy() { return easy; }
        public void setEasy(Integer easy) { this.easy = easy; }
    }

    public static class UpdateReviewResponse {
        private boolean success;
        private String message;
        private SettingsGetReview.ReviewSettingsData data;

        public UpdateReviewResponse(boolean success, String message, SettingsGetReview.ReviewSettingsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SettingsGetReview.ReviewSettingsData getData() { return data; }
        public void setData(SettingsGetReview.ReviewSettingsData data) { this.data = data; }
    }

    @PutMapping("")
    public ResponseEntity<UpdateReviewResponse> updateReviewSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateReviewRequest request) {

        try {
            printRequest(request);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateReviewResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateReviewResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证复习设置数据
            if (request.getDifficultyAlgorithm() != null && !isValidAlgorithm(request.getDifficultyAlgorithm())) {
                return ResponseEntity.badRequest().body(
                        new UpdateReviewResponse(false, "无效的复习算法", null)
                );
            }

            if (request.getDailyGoal() != null && (request.getDailyGoal() < 1 || request.getDailyGoal() > 200)) {
                return ResponseEntity.badRequest().body(
                        new UpdateReviewResponse(false, "每日目标必须在1-200之间", null)
                );
            }

            if (request.getNewWordsPerDay() != null && (request.getNewWordsPerDay() < 0 || request.getNewWordsPerDay() > 100)) {
                return ResponseEntity.badRequest().body(
                        new UpdateReviewResponse(false, "每日新词数量必须在0-100之间", null)
                );
            }

            // 验证时间格式
            if (request.getReviewTime() != null && !isValidTimeFormat(request.getReviewTime())) {
                return ResponseEntity.badRequest().body(
                        new UpdateReviewResponse(false, "无效的复习时间格式，请使用HH:MM格式", null)
                );
            }

            if (request.getReminderTime() != null && !isValidTimeFormat(request.getReminderTime())) {
                return ResponseEntity.badRequest().body(
                        new UpdateReviewResponse(false, "无效的提醒时间格式，请使用HH:MM格式", null)
                );
            }

            // 4. 更新复习设置
            LocalDateTime now = LocalDateTime.now();

            // 更新基本设置
            updateReviewSetting(userId, "dailyGoal", request.getDailyGoal() != null ? request.getDailyGoal().toString() : null, now);
            updateReviewSetting(userId, "reviewTime", request.getReviewTime(), now);
            updateReviewSetting(userId, "reminderEnabled", request.getReminderEnabled() != null ? request.getReminderEnabled().toString() : null, now);
            updateReviewSetting(userId, "reminderTime", request.getReminderTime(), now);
            updateReviewSetting(userId, "difficultyAlgorithm", request.getDifficultyAlgorithm(), now);
            updateReviewSetting(userId, "newWordsPerDay", request.getNewWordsPerDay() != null ? request.getNewWordsPerDay().toString() : null, now);
            updateReviewSetting(userId, "reviewWordsPerDay", request.getReviewWordsPerDay() != null ? request.getReviewWordsPerDay().toString() : null, now);
            updateReviewSetting(userId, "autoGenerateReviews", request.getAutoGenerateReviews() != null ? request.getAutoGenerateReviews().toString() : null, now);
            updateReviewSetting(userId, "showExamples", request.getShowExamples() != null ? request.getShowExamples().toString() : null, now);
            updateReviewSetting(userId, "showImages", request.getShowImages() != null ? request.getShowImages().toString() : null, now);
            updateReviewSetting(userId, "autoPlayAudioInReview", request.getAutoPlayAudioInReview() != null ? request.getAutoPlayAudioInReview().toString() : null, now);
            updateReviewSetting(userId, "shuffleQuestions", request.getShuffleQuestions() != null ? request.getShuffleQuestions().toString() : null, now);
            updateReviewSetting(userId, "enableStreak", request.getEnableStreak() != null ? request.getEnableStreak().toString() : null, now);
            updateReviewSetting(userId, "streakGoal", request.getStreakGoal() != null ? request.getStreakGoal().toString() : null, now);
            updateReviewSetting(userId, "enableNotifications", request.getEnableNotifications() != null ? request.getEnableNotifications().toString() : null, now);

            // 更新复习间隔设置
            if (request.getReviewInterval() != null) {
                ReviewIntervalRequest interval = request.getReviewInterval();
                updateReviewSetting(userId, "reviewInterval_again", interval.getAgain() != null ? interval.getAgain().toString() : null, now);
                updateReviewSetting(userId, "reviewInterval_hard", interval.getHard() != null ? interval.getHard().toString() : null, now);
                updateReviewSetting(userId, "reviewInterval_good", interval.getGood() != null ? interval.getGood().toString() : null, now);
                updateReviewSetting(userId, "reviewInterval_easy", interval.getEasy() != null ? interval.getEasy().toString() : null, now);
            }

            // 5. 查询更新后的设置
            String settingsSql = "SELECT setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ? AND setting_type = 'review'";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 6. 构建响应数据
            SettingsGetReview.ReviewSettingsData settingsData = new SettingsGetReview.ReviewSettingsData();
            SettingsGetReview.ReviewInterval interval = new SettingsGetReview.ReviewInterval();

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

            // 7. 准备响应
            UpdateReviewResponse response = new UpdateReviewResponse(true, "复习设置更新成功", settingsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新复习设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateReviewResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void updateReviewSetting(Integer userId, String key, String value, LocalDateTime now) {
        if (value != null) {
            // 检查设置是否存在
            String checkSql = "SELECT setting_id FROM user_settings WHERE user_id = ? AND setting_type = 'review' AND setting_key = ?";
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, userId, key);

            if (existing.isEmpty()) {
                // 插入新设置
                String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, 'review', ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, userId, key, value, now, now);
            } else {
                // 更新现有设置
                String updateSql = "UPDATE user_settings SET setting_value = ?, updated_at = ? WHERE user_id = ? AND setting_type = 'review' AND setting_key = ?";
                jdbcTemplate.update(updateSql, value, now, userId, key);
            }
        }
    }

    private boolean isValidAlgorithm(String algorithm) {
        return algorithm.equals("sm2") || algorithm.equals("fsrs") || algorithm.equals("custom");
    }

    private boolean isValidTimeFormat(String time) {
        return time.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
    }
}
