package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/user")
public class UserGetAchievements {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取用户成就请求 ===");
        System.out.println("请求信息");
        System.out.println("=================");
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
    public static class AchievementsResponse {
        private boolean success;
        private String message;
        private List<AchievementData> achievements;

        public AchievementsResponse(boolean success, String message, List<AchievementData> achievements) {
            this.success = success;
            this.message = message;
            this.achievements = achievements;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<AchievementData> getAchievements() { return achievements; }
        public void setAchievements(List<AchievementData> achievements) { this.achievements = achievements; }
    }

    public static class AchievementData {
        private int id;
        private String name;
        private String description;
        private String icon;
        private boolean unlocked;
        private String unlockedAt;
        private int progress;
        private int total;
        private String formattedProgress;
        private String category;
        private int points;
        private String rarity;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public boolean isUnlocked() { return unlocked; }
        public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

        public String getUnlockedAt() { return unlockedAt; }
        public void setUnlockedAt(String unlockedAt) { this.unlockedAt = unlockedAt; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public String getFormattedProgress() { return formattedProgress; }
        public void setFormattedProgress(String formattedProgress) { this.formattedProgress = formattedProgress; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }

        public String getRarity() { return rarity; }
        public void setRarity(String rarity) { this.rarity = rarity; }
    }

    @GetMapping("/achievements")
    public ResponseEntity<AchievementsResponse> getUserAchievements(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("Authorization: " + authHeader);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new AchievementsResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new AchievementsResponse(false, "登录已过期，请重新登录", null)
                );
            }
Map<String, Object> session = sessions.get(0);
int userId = ((Number) session.get("user_id")).intValue();

// 3. 查询所有成就
            // 3. 查询所有成就
            String allAchievementsSql = "SELECT achievement_id, name, description, icon_url, category, points, rarity, " +
                    "total_required FROM learning_achievements ORDER BY category, points DESC";
            List<Map<String, Object>> allAchievements = jdbcTemplate.queryForList(allAchievementsSql);

            // 4. 查询用户已解锁的成就
            String userAchievementsSql = "SELECT ua.achievement_id, ua.unlocked_at, la.name, la.description, la.icon_url, " +
                    "la.category, la.points, la.rarity, la.total_required " +
                    "FROM user_achievements ua " +
                    "JOIN learning_achievements la ON ua.achievement_id = la.achievement_id " +
                    "WHERE ua.user_id = ?";
            List<Map<String, Object>> userAchievements = jdbcTemplate.queryForList(userAchievementsSql, userId);

            // 5. 构建成就列表
            List<AchievementData> achievements = new ArrayList<>();

            // 5.1 处理已解锁的成就
            for (Map<String, Object> userAchievement : userAchievements) {
                AchievementData achievement = new AchievementData();
                achievement.setId(((Number) userAchievement.get("achievement_id")).intValue());
                achievement.setName((String) userAchievement.get("name"));
                achievement.setDescription((String) userAchievement.get("description"));
                achievement.setIcon(userAchievement.get("icon_url") != null ? (String) userAchievement.get("icon_url") : "");
                achievement.setUnlocked(true);
                achievement.setUnlockedAt(userAchievement.get("unlocked_at") != null ? userAchievement.get("unlocked_at").toString() : "");
                achievement.setProgress(((Number) userAchievement.get("total_required")).intValue());
                achievement.setTotal(((Number) userAchievement.get("total_required")).intValue());
                achievement.setFormattedProgress("100%");
                achievement.setCategory((String) userAchievement.get("category"));
                achievement.setPoints(((Number) userAchievement.get("points")).intValue());
                achievement.setRarity((String) userAchievement.get("rarity"));

                achievements.add(achievement);
            }

            // 5.2 处理未解锁的成就
            for (Map<String, Object> allAchievement : allAchievements) {
                int achievementId = ((Number) allAchievement.get("achievement_id")).intValue();

                // 检查是否已经处理过（已解锁）
                boolean alreadyProcessed = false;
                for (AchievementData processed : achievements) {
                    if (processed.getId() == achievementId) {
                        alreadyProcessed = true;
                        break;
                    }
                }

                if (!alreadyProcessed) {
                    AchievementData achievement = new AchievementData();
                    achievement.setId(achievementId);
                    achievement.setName((String) allAchievement.get("name"));
                    achievement.setDescription((String) allAchievement.get("description"));
                    achievement.setIcon(allAchievement.get("icon_url") != null ? (String) allAchievement.get("icon_url") : "");
                    achievement.setUnlocked(false);
                    achievement.setUnlockedAt("");

                    // 根据成就类型计算进度
                    int totalRequired = ((Number) allAchievement.get("total_required")).intValue();
                    int progress = 0;

                    // 这里需要根据成就的具体要求查询用户进度
                    // 例如：如果是阅读时间成就，查询用户总阅读时间
                    // 如果是单词数量成就，查询用户已学单词数量
                    // 这里简化处理，随机生成一个进度
                    String category = (String) allAchievement.get("category");
                    if ("reading".equals(category)) {
                        // 阅读相关成就
                        String readingProgressSql = "SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, start_time, end_time)), 0) as total_seconds " +
                                "FROM reading_history WHERE user_id = ?";
                        Map<String, Object> readingResult = jdbcTemplate.queryForMap(readingProgressSql, userId);
                        progress = ((Number) readingResult.get("total_seconds")).intValue() / 3600; // 转换为小时
                    } else if ("vocabulary".equals(category)) {
                        // 词汇相关成就
                        String vocabProgressSql = "SELECT COUNT(*) as count FROM user_vocabulary WHERE user_id = ?";
                        Map<String, Object> vocabResult = jdbcTemplate.queryForMap(vocabProgressSql, userId);
                        progress = ((Number) vocabResult.get("count")).intValue();
                    } else if ("review".equals(category)) {
                        // 复习相关成就
                        String reviewProgressSql = "SELECT COUNT(*) as count FROM review_sessions WHERE user_id = ? AND status = 'completed'";
                        Map<String, Object> reviewResult = jdbcTemplate.queryForMap(reviewProgressSql, userId);
                        progress = ((Number) reviewResult.get("count")).intValue();
                    }

                    // 确保进度不超过总数
                    if (progress > totalRequired) {
                        progress = totalRequired;
                    }

                    achievement.setProgress(progress);
                    achievement.setTotal(totalRequired);

                    // 格式化进度
                    if (totalRequired > 0) {
                        double percentage = (double) progress / totalRequired * 100;
                        achievement.setFormattedProgress(String.format("%.0f%%", percentage));
                    } else {
                        achievement.setFormattedProgress("0%");
                    }

                    achievement.setCategory(category);
                    achievement.setPoints(((Number) allAchievement.get("points")).intValue());
                    achievement.setRarity((String) allAchievement.get("rarity"));

                    achievements.add(achievement);
                }
            }

            printQueryResult("成就数量: " + achievements.size());

            // 6. 准备响应数据
            AchievementsResponse response = new AchievementsResponse(true, "获取用户成就成功", achievements);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取用户成就过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new AchievementsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
