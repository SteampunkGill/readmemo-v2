package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors; // 用于 Stream API

@RestController
@RequestMapping("/api/v1/review")
public class ReviewUpdatePlan {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // --- 辅助打印方法 ---
    private void printRequest(Object request) {
        System.out.println("=== 收到更新复习计划请求 ===");
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

    // --- 请求 DTO ---
    public static class UpdatePlanRequest {
        private Integer daily_target_words;
        private Integer weekly_target_days;
        private String preferred_time;
        private List<String> preferred_days;
        private String language_focus;
        private String difficulty_level;
        private Boolean is_active;

        public Integer getDaily_target_words() { return daily_target_words; }
        public void setDaily_target_words(Integer daily_target_words) { this.daily_target_words = daily_target_words; }

        public Integer getWeekly_target_days() { return weekly_target_days; }
        public void setWeekly_target_days(Integer weekly_target_days) { this.weekly_target_days = weekly_target_days; }

        public String getPreferred_time() { return preferred_time; }
        public void setPreferred_time(String preferred_time) { this.preferred_time = preferred_time; }

        public List<String> getPreferred_days() { return preferred_days; }
        public void setPreferred_days(List<String> preferred_days) { this.preferred_days = preferred_days; }

        public String getLanguage_focus() { return language_focus; }
        public void setLanguage_focus(String language_focus) { this.language_focus = language_focus; }

        public String getDifficulty_level() { return difficulty_level; }
        public void setDifficulty_level(String difficulty_level) { this.difficulty_level = difficulty_level; }

        public Boolean getIs_active() { return is_active; }
        public void setIs_active(Boolean is_active) { this.is_active = is_active; }

        @Override
        public String toString() {
            return "UpdatePlanRequest{" +
                    "daily_target_words=" + daily_target_words +
                    ", weekly_target_days=" + weekly_target_days +
                    ", preferred_time='" + preferred_time + '\'' +
                    ", preferred_days=" + preferred_days +
                    ", language_focus='" + language_focus + '\'' +
                    ", difficulty_level='" + difficulty_level + '\'' +
                    ", is_active=" + is_active +
                    '}';
        }
    }

    // --- 响应 DTO ---
    public static class UpdatePlanResponse {
        private boolean success;
        private String message;
        private ReviewPlanData data; // 使用内部定义的 ReviewPlanData

        public UpdatePlanResponse(boolean success, String message, ReviewPlanData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReviewPlanData getData() { return data; }
        public void setData(ReviewPlanData data) { this.data = data; }

        @Override
        public String toString() {
            return "UpdatePlanResponse{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }

    // --- 内部定义的 Response Data DTO ---
    public static class ReviewPlanData {
        private String id; // 计划的唯一标识，例如 "plan_" + userId
        private String userId;
        private String name; // 复习计划的名称
        private String description; // 复习计划的描述
        private Integer dailyTargetWords;
        private Integer weeklyTargetDays;
        private String preferredTime;
        private List<String> preferredDays;
        private String languageFocus;
        private String difficultyLevel;
        private Boolean isActive;
        private String createdAt; // ISO 8601 格式的日期时间字符串
        private String updatedAt; // ISO 8601 格式的日期时间字符串

        // 嵌套的进度统计类
        private PlanProgress progress;

        public ReviewPlanData(String id, String userId, String name, String description, Integer dailyTargetWords, Integer weeklyTargetDays, String preferredTime, List<String> preferredDays, String languageFocus, String difficultyLevel, Boolean isActive, String createdAt, String updatedAt, PlanProgress progress) {
            this.id = id;
            this.userId = userId;
            this.name = name;
            this.description = description;
            this.dailyTargetWords = dailyTargetWords;
            this.weeklyTargetDays = weeklyTargetDays;
            this.preferredTime = preferredTime;
            this.preferredDays = preferredDays;
            this.languageFocus = languageFocus;
            this.difficultyLevel = difficultyLevel;
            this.isActive = isActive;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.progress = progress;
        }

        // Getters
        public String getId() { return id; }
        public String getUserId() { return userId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Integer getDailyTargetWords() { return dailyTargetWords; }
        public Integer getWeeklyTargetDays() { return weeklyTargetDays; }
        public String getPreferredTime() { return preferredTime; }
        public List<String> getPreferredDays() { return preferredDays; }
        public String getLanguageFocus() { return languageFocus; }
        public String getDifficultyLevel() { return difficultyLevel; }
        public Boolean getIsActive() { return isActive; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public PlanProgress getProgress() { return progress; }

        // Setters (仅为满足JavaBean规范，实际逻辑中不需要频繁调用)
        public void setId(String id) { this.id = id; }
        public void setUserId(String userId) { this.userId = userId; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setDailyTargetWords(Integer dailyTargetWords) { this.dailyTargetWords = dailyTargetWords; }
        public void setWeeklyTargetDays(Integer weeklyTargetDays) { this.weeklyTargetDays = weeklyTargetDays; }
        public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }
        public void setPreferredDays(List<String> preferredDays) { this.preferredDays = preferredDays; }
        public void setLanguageFocus(String languageFocus) { this.languageFocus = languageFocus; }
        public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
        public void setIsActive(Boolean active) { isActive = active; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public void setProgress(PlanProgress progress) { this.progress = progress; }


        @Override
        public String toString() {
            return "ReviewPlanData{" +
                    "id='" + id + '\'' +
                    ", userId='" + userId + '\'' +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", dailyTargetWords=" + dailyTargetWords +
                    ", weeklyTargetDays=" + weeklyTargetDays +
                    ", preferredTime='" + preferredTime + '\'' +
                    ", preferredDays=" + preferredDays +
                    ", languageFocus='" + languageFocus + '\'' +
                    ", difficultyLevel='" + difficultyLevel + '\'' +
                    ", isActive=" + isActive +
                    ", createdAt='" + createdAt + '\'' +
                    ", updatedAt='" + updatedAt + '\'' +
                    ", progress=" + progress +
                    '}';
        }

        // Equals and HashCode (用于潜在的测试或比较，此处省略以保持简洁，但实际项目可能需要)
    }

    // --- 内部定义的 Progress DTO ---
    public static class PlanProgress {
        private Integer todayWordsCompleted;
        private Integer dailyTargetWords;
        private Double todayProgressPercentage;
        private Integer weekWordsCompleted;
        private Integer weeklyTargetTotalWords; // 假设是 dailyTargetWords * weeklyTargetDays
        private Double weekProgressPercentage;
        private Integer consecutiveDaysStreak;
        private Integer totalWordsReviewed;
        private Double averageAccuracy;

        public PlanProgress(Integer todayWordsCompleted, Integer dailyTargetWords, Double todayProgressPercentage, Integer weekWordsCompleted, Integer weeklyTargetTotalWords, Double weekProgressPercentage, Integer consecutiveDaysStreak, Integer totalWordsReviewed, Double averageAccuracy) {
            this.todayWordsCompleted = todayWordsCompleted;
            this.dailyTargetWords = dailyTargetWords;
            this.todayProgressPercentage = todayProgressPercentage;
            this.weekWordsCompleted = weekWordsCompleted;
            this.weeklyTargetTotalWords = weeklyTargetTotalWords;
            this.weekProgressPercentage = weekProgressPercentage;
            this.consecutiveDaysStreak = consecutiveDaysStreak;
            this.totalWordsReviewed = totalWordsReviewed;
            this.averageAccuracy = averageAccuracy;
        }

        // Getters
        public Integer getTodayWordsCompleted() { return todayWordsCompleted; }
        public Integer getDailyTargetWords() { return dailyTargetWords; }
        public Double getTodayProgressPercentage() { return todayProgressPercentage; }
        public Integer getWeekWordsCompleted() { return weekWordsCompleted; }
        public Integer getWeeklyTargetTotalWords() { return weeklyTargetTotalWords; }
        public Double getWeekProgressPercentage() { return weekProgressPercentage; }
        public Integer getConsecutiveDaysStreak() { return consecutiveDaysStreak; }
        public Integer getTotalWordsReviewed() { return totalWordsReviewed; }
        public Double getAverageAccuracy() { return averageAccuracy; }

        // Setters (仅为满足JavaBean规范)
        public void setTodayWordsCompleted(Integer todayWordsCompleted) { this.todayWordsCompleted = todayWordsCompleted; }
        public void setDailyTargetWords(Integer dailyTargetWords) { this.dailyTargetWords = dailyTargetWords; }
        public void setTodayProgressPercentage(Double todayProgressPercentage) { this.todayProgressPercentage = todayProgressPercentage; }
        public void setWeekWordsCompleted(Integer weekWordsCompleted) { this.weekWordsCompleted = weekWordsCompleted; }
        public void setWeeklyTargetTotalWords(Integer weeklyTargetTotalWords) { this.weeklyTargetTotalWords = weeklyTargetTotalWords; }
        public void setWeekProgressPercentage(Double weekProgressPercentage) { this.weekProgressPercentage = weekProgressPercentage; }
        public void setConsecutiveDaysStreak(Integer consecutiveDaysStreak) { this.consecutiveDaysStreak = consecutiveDaysStreak; }
        public void setTotalWordsReviewed(Integer totalWordsReviewed) { this.totalWordsReviewed = totalWordsReviewed; }
        public void setAverageAccuracy(Double averageAccuracy) { this.averageAccuracy = averageAccuracy; }

        @Override
        public String toString() {
            return "PlanProgress{" +
                    "todayWordsCompleted=" + todayWordsCompleted +
                    ", dailyTargetWords=" + dailyTargetWords +
                    ", todayProgressPercentage=" + todayProgressPercentage +
                    ", weekWordsCompleted=" + weekWordsCompleted +
                    ", weeklyTargetTotalWords=" + weeklyTargetTotalWords +
                    ", weekProgressPercentage=" + weekProgressPercentage +
                    ", consecutiveDaysStreak=" + consecutiveDaysStreak +
                    ", totalWordsReviewed=" + totalWordsReviewed +
                    ", averageAccuracy=" + averageAccuracy +
                    '}';
        }

        // Equals and HashCode (用于潜在的测试或比较)
    }

    // --- API Endpoint ---
    @PutMapping("/plan")
    public ResponseEntity<UpdatePlanResponse> updateReviewPlan(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdatePlanRequest request) {

        printRequest(request);

        try {
            // 1. 验证 token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePlanResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            // 注意：这里的 SQL 是为了演示，实际项目中不应直接硬编码 Token，应使用更安全的 Session/Token 管理机制
            String userSql = "SELECT u.user_id FROM users u JOIN user_sessions us ON u.user_id = us.user_id WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePlanResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 验证请求数据
            if (request.getDaily_target_words() != null && request.getDaily_target_words() < 1) {
                return ResponseEntity.badRequest().body(
                        new UpdatePlanResponse(false, "每日目标单词数必须大于0", null)
                );
            }

            if (request.getWeekly_target_days() != null &&
                    (request.getWeekly_target_days() < 1 || request.getWeekly_target_days() > 7)) {
                return ResponseEntity.badRequest().body(
                        new UpdatePlanResponse(false, "每周目标天数必须在1-7之间", null)
                );
            }

            // 3. 检查是否已有设置记录 (使用 review_settings 表)
            String checkSql = "SELECT COUNT(*) FROM review_settings WHERE user_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);

            LocalDateTime now = LocalDateTime.now();
            String nowIso = now.toString(); // for database insertion/update

            // --- 准备更新或插入数据 ---
            // 为方便代码逻辑，先将请求参数和默认值合并
            int dailyTargetWords = request.getDaily_target_words() != null ? request.getDaily_target_words() : 20;
            int weeklyTargetDays = request.getWeekly_target_days() != null ? request.getWeekly_target_days() : 5;
            String preferredTime = request.getPreferred_time() != null ? request.getPreferred_time() : "18:00";
            List<String> preferredDays = request.getPreferred_days() != null ? request.getPreferred_days() : Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
            String preferredDaysString = String.join(",", preferredDays); // 数据库存储格式
            String languageFocus = request.getLanguage_focus() != null ? request.getLanguage_focus() : "en";
            String difficultyLevel = request.getDifficulty_level() != null ? request.getDifficulty_level() : "medium";
            boolean isActive = request.getIs_active() != null ? request.getIs_active() : true;

            if (count > 0) {
                // 更新现有记录
                StringBuilder updateSql = new StringBuilder("UPDATE review_settings SET ");
                List<Object> params = new ArrayList<>();
                List<String> updates = new ArrayList<>();

                // 动态构建 SET 子句，只更新传入的字段
                if (request.getDaily_target_words() != null) {
                    updates.add("daily_target_words = ?");
                    params.add(dailyTargetWords);
                }
                if (request.getWeekly_target_days() != null) {
                    updates.add("weekly_target_days = ?");
                    params.add(weeklyTargetDays);
                }
                if (request.getPreferred_time() != null) {
                    updates.add("preferred_time = ?");
                    params.add(preferredTime);
                }
                if (request.getPreferred_days() != null) {
                    updates.add("preferred_days = ?");
                    params.add(preferredDaysString);
                }
                if (request.getLanguage_focus() != null) {
                    updates.add("language_focus = ?");
                    params.add(languageFocus);
                }
                if (request.getDifficulty_level() != null) {
                    updates.add("difficulty_level = ?");
                    params.add(difficultyLevel);
                }
                if (request.getIs_active() != null) {
                    updates.add("is_active = ?");
                    params.add(isActive);
                }

                // 无论是否更新，都更新 updated_at
                updates.add("updated_at = ?");
                params.add(now);

                updateSql.append(String.join(", ", updates));
                updateSql.append(" WHERE user_id = ?");
                params.add(userId);

                System.out.println("执行更新SQL: " + updateSql.toString());
                //printQueryResult("参数: " + params); // 打印参数可能过于冗长，仅打印SQL

                int updated = jdbcTemplate.update(updateSql.toString(), params.toArray());
                printQueryResult("更新 review_settings 记录数: " + updated);

            } else {
                // 创建新记录
                String insertSql = "INSERT INTO review_settings " +
                        "(user_id, daily_target_words, weekly_target_days, " +
                        "preferred_time, preferred_days, language_focus, " +
                        "difficulty_level, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                int inserted = jdbcTemplate.update(insertSql,
                        userId,
                        dailyTargetWords,
                        weeklyTargetDays,
                        preferredTime,
                        preferredDaysString,
                        languageFocus,
                        difficultyLevel,
                        isActive,
                        now, // created_at
                        now  // updated_at
                );
                printQueryResult("插入 review_settings 记录数: " + inserted);
            }

            // 4. 获取更新后的计划数据
            // 使用一个内部方法来模拟 'ReviewGetPlan' 的部分逻辑，以获取最新的计划和进度
            ReviewPlanData updatedPlanData = getUpdatedPlanData(userId, dailyTargetWords, weeklyTargetDays, preferredTime, preferredDays, languageFocus, difficultyLevel, isActive, now);

            UpdatePlanResponse response = new UpdatePlanResponse(true, "更新复习计划成功", updatedPlanData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新复习计划过程中发生错误: " + e.getMessage());
            e.printStackTrace(); // 打印详细堆栈信息
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdatePlanResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 模拟获取最新复习计划和进度数据的方法。
     * 实际上，这部分逻辑可能会被单独的 'ReviewGetPlanRequestHandler' 调用。
     * 这里为了 complete the UpdatePlanResponse，将相关逻辑合并。
     * @param userId 当前用户ID
     * @param dailyTargetWords 每日目标单词数
     * @param weeklyTargetDays 每周目标天数
     * @param preferredTime 偏好时间
     * @param preferredDays 偏好日期列表
     * @param languageFocus 语言焦点
     * @param difficultyLevel 难度级别
     * @param isActive 是否激活
     * @param now 当前时间，用于生成 updated_at 和计算进度
     * @return 构建好的 ReviewPlanData 对象
     */
    private ReviewPlanData getUpdatedPlanData(int userId,
                                              int dailyTargetWords, int weeklyTargetDays,
                                              String preferredTime, List<String> preferredDays,
                                              String languageFocus, String difficultyLevel,
                                              boolean isActive, LocalDateTime now) {

        // --- 获取统计数据 ---
        String today = now.toLocalDate().toString();

        // 获取今日已完成复习单词数
        String todayReviewsSql = "SELECT COALESCE(SUM(total_words), 0) as today_words FROM review_sessions WHERE user_id = ? AND DATE(completed_at) = ?";
        Integer todayCompleted = jdbcTemplate.queryForObject(todayReviewsSql, Integer.class, userId, today);
        if (todayCompleted == null) todayCompleted = 0; // 确保不为null

        // 获取本周已完成复习单词数
        // 计算本周的开始日期（假设周一为一周的开始）
        LocalDate weekStartDate = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1);
        String weekStartStr = weekStartDate.toString();

        String weekReviewsSql = "SELECT COALESCE(SUM(total_words), 0) as week_words FROM review_sessions WHERE user_id = ? AND DATE(completed_at) BETWEEN ? AND ?";
        Integer weekCompleted = jdbcTemplate.queryForObject(weekReviewsSql, Integer.class, userId, weekStartStr, today);
        if (weekCompleted == null) weekCompleted = 0; // 确保不为null

        // 获取连续学习天数 (调用内部方法)
        int streakDays = calculateStreakDays(userId, now.toLocalDate());

        // 获取总复习单词数和平均准确率
        String totalStatsSql = "SELECT COALESCE(SUM(total_words), 0) as total_words, COALESCE(AVG(accuracy), 0) as avg_accuracy FROM review_sessions WHERE user_id = ?";
        Map<String, Object> totalStats = jdbcTemplate.queryForMap(totalStatsSql, userId);

        int totalWordsReviewed = 0;
        double averageAccuracy = 0.0;
        if (totalStats != null) {
            totalWordsReviewed = ((Number) totalStats.get("total_words")).intValue();
            // SQLite 的 AVG 返回 Double，其他数据库可能不同，需要类型转换
            Object avgObj = totalStats.get("avg_accuracy");
            if (avgObj != null) {
                averageAccuracy = ((Number) avgObj).doubleValue();
            }
        }

        // --- 计算百分比 ---
        double todayPercentage = (dailyTargetWords > 0) ? ((double) todayCompleted / dailyTargetWords * 100) : 0;
        // 本周目标总单词数
        int weeklyTargetTotalWords = dailyTargetWords * weeklyTargetDays;
        double weekPercentage = (weeklyTargetTotalWords > 0) ? ((double) weekCompleted / weeklyTargetTotalWords * 100) : 0;

        // 格式化百分比，保留两位小数
        todayPercentage = Math.round(todayPercentage * 100.0) / 100.0;
        weekPercentage = Math.round(weekPercentage * 100.0) / 100.0;

        // --- 构建进度数据 ---
        PlanProgress progress = new PlanProgress(
                todayCompleted,
                dailyTargetWords,
                todayPercentage,
                weekCompleted,
                weeklyTargetTotalWords,
                weekPercentage,
                streakDays,
                totalWordsReviewed,
                Math.round(averageAccuracy * 100.0) / 100.0 // 格式化平均准确率
        );

        // --- 构建复习计划数据 ---
        // 假设 plan ID 为 "plan_" + userId
        // 假设 description 动态生成
        String description = String.format("每日复习 %d 个单词，每周学习 %d 天。偏好时间 %s，每周目标 %s。",
                dailyTargetWords,
                weeklyTargetDays,
                preferredTime,
                String.join(",", preferredDays));

        return new ReviewPlanData(
                "plan_" + userId,
                String.valueOf(userId),
                "用户自定义复习计划", // 计划名称
                description,
                dailyTargetWords,
                weeklyTargetDays,
                preferredTime,
                preferredDays,
                languageFocus,
                difficultyLevel,
                isActive,
                now.toString(), // Assuming created_at from the DB or a default if new
                now.toString(), // This is the updated_at time
                progress
        );
    }

    /**
     * 计算连续学习天数。
     * @param userId 用户ID
     * @param todayLocalDate 当前日期 (LocalDate)
     * @return 连续学习天数
     */
    private int calculateStreakDays(int userId, LocalDate todayLocalDate) {
        try {
            // 获取用户所有复习会话的日期，去重并排序
            String sql = "SELECT DISTINCT DATE(completed_at) as review_date FROM review_sessions WHERE user_id = ? ORDER BY review_date DESC";
            List<Map<String, Object>> dates = jdbcTemplate.queryForList(sql, userId);

            if (dates.isEmpty()) {
                return 0; // 没有复习记录
            }

            // 检查今天是否复习过
            boolean todayReviewed = false;
            LocalDate lastReviewDate = null;
            for (Map<String, Object> dateRow : dates) {
                // 数据库日期类型可能不同，这里尝试转换为 LocalDate
                Object dateObj = dateRow.get("review_date");
                LocalDate reviewDate = null;
                if (dateObj instanceof LocalDate) {
                    reviewDate = (LocalDate) dateObj;
                } else if (dateObj instanceof java.sql.Date) {
                    reviewDate = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof LocalDateTime) {
                    reviewDate = ((LocalDateTime) dateObj).toLocalDate();
                } else {
                    // 尝试解析字符串，如果日期格式固定
                    // reviewDate = LocalDate.parse(dateObj.toString());
                }

                if (reviewDate != null) {
                    if (reviewDate.equals(todayLocalDate)) {
                        todayReviewed = true;
                    }
                    // 记录最后一次复习日期
                    if (lastReviewDate == null || reviewDate.isAfter(lastReviewDate)) {
                        lastReviewDate = reviewDate;
                    }
                }
            }

            // 如果今天没有复习，则连续天数为0
            if (!todayReviewed) {
                return 0;
            }

            // 如果今天复习过，开始计算连续天数
            int streak = 1; // 今天算一天
            LocalDate expectedDate = todayLocalDate.minusDays(1); // 期望的昨天日期

            for (Map<String, Object> dateRow : dates) {
                Object dateObj = dateRow.get("review_date");
                LocalDate reviewDate = null;
                if (dateObj instanceof LocalDate) {
                    reviewDate = (LocalDate) dateObj;
                } else if (dateObj instanceof java.sql.Date) {
                    reviewDate = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof LocalDateTime) {
                    reviewDate = ((LocalDateTime) dateObj).toLocalDate();
                }

                if (reviewDate != null) {
                    if (reviewDate.equals(expectedDate)) {
                        streak++;
                        expectedDate = expectedDate.minusDays(1); // 期望再往前一天
                    } else if (reviewDate.isBefore(expectedDate)) {
                        // 如果找到的日期比期望的日期早，说明中间有断层，停止计算
                        // 但需要找到比 expectedDate 更早但日期有效的记录，更新 expectedDate
                        expectedDate = reviewDate.minusDays(1);
                        // 这里需要一个逻辑来跳过已经检查过的日期，避免重复计算
                        // 实际上，我们只需要检查 dates 中的日期是否连续
                    }
                }
            }
            // 重新梳理逻辑：
            // 1. 确保今天复习了
            // 2. 从昨天的日期开始，往前遍历 dates 列表
            // 3. 如果 dates 中的日期等于 expectedDate，则 streak++，expectedDate--
            // 4. 如果 dates 中的日期早于 expectedDate，说明有断层，停止
            // 5. 如果 dates 中的日期晚于 expectedDate (不可能，因为已排序)，忽略

            streak = 0; // 重置
            LocalDate currentDate = todayLocalDate;
            boolean foundToday = false;
            for(Map<String, Object> dateRow : dates) {
                Object dateObj = dateRow.get("review_date");
                LocalDate reviewDate = null;
                if (dateObj instanceof LocalDate) { reviewDate = (LocalDate) dateObj; }
                else if (dateObj instanceof java.sql.Date) { reviewDate = ((java.sql.Date) dateObj).toLocalDate(); }
                else if (dateObj instanceof LocalDateTime) { reviewDate = ((LocalDateTime) dateObj).toLocalDate(); }

                if (reviewDate != null) {
                    if (reviewDate.equals(currentDate)) {
                        foundToday = true;
                        streak++;
                        currentDate = currentDate.minusDays(1);
                    } else if (reviewDate.isBefore(currentDate)) {
                        // 如果日期是早于我们期望的 currentDate，说明中间断开了
                        break;
                    }
                    // 如果 reviewDate == currentDate, 并且streak已经计算过，则继续
                }
            }

            return foundToday ? streak : 0; // 必须今天有复习才算 streak

        } catch (Exception e) {
            System.err.println("计算连续学习天数失败: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}