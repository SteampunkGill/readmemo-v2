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
public class UserGetPointsAndLevel {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取积分等级请求 ===");
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
    public static class PointsAndLevelResponse {
        private boolean success;
        private String message;
        private PointsData points;

        public PointsAndLevelResponse(boolean success, String message, PointsData points) {
            this.success = success;
            this.message = message;
            this.points = points;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public PointsData getPoints() { return points; }
        public void setPoints(PointsData points) { this.points = points; }
    }

    public static class PointsData {
        private int totalPoints;
        private int currentLevel;
        private String levelName;
        private int nextLevelPoints;
        private int currentLevelPoints;
        private double progressToNextLevel;
        private String formattedProgress;
        private String levelIcon;
        private List<BadgeData> badges;
        private List<PointsHistoryData> pointsHistory;

        // Getters and Setters
        public int getTotalPoints() { return totalPoints; }
        public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

        public int getCurrentLevel() { return currentLevel; }
        public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }

        public String getLevelName() { return levelName; }
        public void setLevelName(String levelName) { this.levelName = levelName; }

        public int getNextLevelPoints() { return nextLevelPoints; }
        public void setNextLevelPoints(int nextLevelPoints) { this.nextLevelPoints = nextLevelPoints; }

        public int getCurrentLevelPoints() { return currentLevelPoints; }
        public void setCurrentLevelPoints(int currentLevelPoints) { this.currentLevelPoints = currentLevelPoints; }

        public double getProgressToNextLevel() { return progressToNextLevel; }
        public void setProgressToNextLevel(double progressToNextLevel) { this.progressToNextLevel = progressToNextLevel; }

        public String getFormattedProgress() { return formattedProgress; }
        public void setFormattedProgress(String formattedProgress) { this.formattedProgress = formattedProgress; }

        public String getLevelIcon() { return levelIcon; }
        public void setLevelIcon(String levelIcon) { this.levelIcon = levelIcon; }

        public List<BadgeData> getBadges() { return badges; }
        public void setBadges(List<BadgeData> badges) { this.badges = badges; }

        public List<PointsHistoryData> getPointsHistory() { return pointsHistory; }
        public void setPointsHistory(List<PointsHistoryData> pointsHistory) { this.pointsHistory = pointsHistory; }
    }

    public static class BadgeData {
        private int id;
        private String name;
        private String description;
        private String icon;
        private String earnedDate;

        public BadgeData(int id, String name, String description, String icon, String earnedDate) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.earnedDate = earnedDate;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getEarnedDate() { return earnedDate; }
        public void setEarnedDate(String earnedDate) { this.earnedDate = earnedDate; }
    }

    public static class PointsHistoryData {
        private String date;
        private int points;
        private String reason;

        public PointsHistoryData(String date, int points, String reason) {
            this.date = date;
            this.points = points;
            this.reason = reason;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    @GetMapping("/points-level")
    public ResponseEntity<PointsAndLevelResponse> getUserPointsAndLevel(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("Authorization: " + authHeader);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new PointsAndLevelResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new PointsAndLevelResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 查询用户总积分
            // 假设积分存储在user_achievements表中，通过achievement的points字段累加
            String totalPointsSql = "SELECT COALESCE(SUM(la.points), 0) as total_points " +
                    "FROM user_achievements ua " +
                    "JOIN learning_achievements la ON ua.achievement_id = la.achievement_id " +
                    "WHERE ua.user_id = ?";
            Map<String, Object> totalPointsResult = jdbcTemplate.queryForMap(totalPointsSql, userId);
            int totalPoints = ((Number) totalPointsResult.get("total_points")).intValue();

            // 4. 计算用户等级
            // 假设等级规则：每100积分升一级
            int currentLevel = Math.max(1, totalPoints / 100 + 1);
            String levelName = getLevelName(currentLevel);

            // 当前等级所需积分
            int currentLevelPoints = (currentLevel - 1) * 100;

            // 下一等级所需积分
            int nextLevelPoints = currentLevel * 100;

            // 当前等级已获得积分
            int pointsInCurrentLevel = totalPoints - currentLevelPoints;

            // 升级进度百分比
            double progressToNextLevel = 0;
            if (nextLevelPoints > currentLevelPoints) {
                progressToNextLevel = (double) pointsInCurrentLevel / (nextLevelPoints - currentLevelPoints) * 100;
            }

            String formattedProgress = String.format("%.1f%%", progressToNextLevel);

            // 5. 查询用户徽章（简化处理，使用成就作为徽章）
            List<BadgeData> badges = new ArrayList<>();
            String badgesSql = "SELECT la.achievement_id, la.name, la.description, la.icon_url, ua.unlocked_at " +
                    "FROM user_achievements ua " +
                    "JOIN learning_achievements la ON ua.achievement_id = la.achievement_id " +
                    "WHERE ua.user_id = ? AND la.points >= 50 " + // 只显示50分以上的成就作为徽章
                    "ORDER BY ua.unlocked_at DESC LIMIT 10";

            List<Map<String, Object>> badgesResults = jdbcTemplate.queryForList(badgesSql, userId);

            for (Map<String, Object> badge : badgesResults) {
                int badgeId = ((Number) badge.get("achievement_id")).intValue();
                String badgeName = (String) badge.get("name");
                String badgeDescription = (String) badge.get("description");
                String badgeIcon = badge.get("icon_url") != null ? (String) badge.get("icon_url") : "";
                String earnedDate = badge.get("unlocked_at") != null ? badge.get("unlocked_at").toString() : "";

                badges.add(new BadgeData(badgeId, badgeName, badgeDescription, badgeIcon, earnedDate));
            }

            // 6. 查询积分历史（简化处理，使用成就解锁记录）
            List<PointsHistoryData> pointsHistory = new ArrayList<>();
            String historySql = "SELECT DATE(ua.unlocked_at) as date, la.points, la.name as reason " +
                    "FROM user_achievements ua " +
                    "JOIN learning_achievements la ON ua.achievement_id = la.achievement_id " +
                    "WHERE ua.user_id = ? " +
                    "ORDER BY ua.unlocked_at DESC LIMIT 20";

            List<Map<String, Object>> historyResults = jdbcTemplate.queryForList(historySql, userId);

            for (Map<String, Object> history : historyResults) {
                String date = history.get("date") != null ? history.get("date").toString() : "";
                int points = ((Number) history.get("points")).intValue();
                String reason = (String) history.get("reason");

                pointsHistory.add(new PointsHistoryData(date, points, reason));
            }

            // 7. 构建积分等级数据
            PointsData pointsData = new PointsData();
            pointsData.setTotalPoints(totalPoints);
            pointsData.setCurrentLevel(currentLevel);
            pointsData.setLevelName(levelName);
            pointsData.setNextLevelPoints(nextLevelPoints);
            pointsData.setCurrentLevelPoints(currentLevelPoints);
            pointsData.setProgressToNextLevel(progressToNextLevel);
            pointsData.setFormattedProgress(formattedProgress);
            pointsData.setLevelIcon(getLevelIcon(currentLevel));
            pointsData.setBadges(badges);
            pointsData.setPointsHistory(pointsHistory);

            printQueryResult(pointsData);

            // 8. 准备响应数据
            PointsAndLevelResponse response = new PointsAndLevelResponse(true, "获取积分等级信息成功", pointsData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取用户积分等级过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new PointsAndLevelResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 根据等级获取等级名称
    private String getLevelName(int level) {
        if (level <= 5) {
            return "初学者";
        } else if (level <= 10) {
            return "学习者";
        } else if (level <= 20) {
            return "熟练者";
        } else if (level <= 30) {
            return "专家";
        } else if (level <= 40) {
            return "大师";
        } else if (level <= 50) {
            return "宗师";
        } else {
            return "传奇";
        }
    }

    // 根据等级获取等级图标（简化处理，返回固定字符串）
    private String getLevelIcon(int level) {
        if (level <= 5) {
            return "level_beginner";
        } else if (level <= 10) {
            return "level_learner";
        } else if (level <= 20) {
            return "level_proficient";
        } else if (level <= 30) {
            return "level_expert";
        } else if (level <= 40) {
            return "level_master";
        } else if (level <= 50) {
            return "level_grandmaster";
        } else {
            return "level_legend";
        }
    }
}
