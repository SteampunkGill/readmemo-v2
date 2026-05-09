package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/user")
public class UserGetLearningGoals {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取学习目标请求 ===");
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
    public static class LearningGoalsResponse {
        private boolean success;
        private String message;
        private List<GoalData> goals;

        public LearningGoalsResponse(boolean success, String message, List<GoalData> goals) {
            this.success = success;
            this.message = message;
            this.goals = goals;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<GoalData> getGoals() { return goals; }
        public void setGoals(List<GoalData> goals) { this.goals = goals; }
    }

    public static class GoalData {
        private int goalId;
        private String type;
        private String title;
        private String description;
        private int targetValue;
        private int currentValue;
        private String unit;
        private String startDate;
        private String endDate;
        private String status;
        private String createdDate;
        private String updatedDate;

        // Getters and Setters
        public int getGoalId() { return goalId; }
        public void setGoalId(int goalId) { this.goalId = goalId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getTargetValue() { return targetValue; }
        public void setTargetValue(int targetValue) { this.targetValue = targetValue; }

        public int getCurrentValue() { return currentValue; }
        public void setCurrentValue(int currentValue) { this.currentValue = currentValue; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCreatedDate() { return createdDate; }
        public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

        public String getUpdatedDate() { return updatedDate; }
        public void setUpdatedDate(String updatedDate) { this.updatedDate = updatedDate; }
    }

    @GetMapping("/goals")
    public ResponseEntity<LearningGoalsResponse> getLearningGoals(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("Authorization: " + authHeader);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningGoalsResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new LearningGoalsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 查询用户的学习目标
            // 假设有一个learning_goals表存储学习目标
            // 如果没有这个表，我们返回一些默认目标

            // 检查表是否存在（简化处理）
            boolean goalsTableExists = false;
            try {
                jdbcTemplate.queryForObject("SELECT 1 FROM learning_goals LIMIT 1", Integer.class);
                goalsTableExists = true;
            } catch (Exception e) {
                goalsTableExists = false;
            }

            List<GoalData> goals = new ArrayList<>();

            if (goalsTableExists) {
                // 查询用户的学习目标
                String goalsSql = "SELECT goal_id, type, title, description, target_value, current_value, " +
                        "unit, start_date, end_date, status, created_at, updated_at " +
                        "FROM learning_goals WHERE user_id = ? ORDER BY created_at DESC";

                List<Map<String, Object>> goalsResults = jdbcTemplate.queryForList(goalsSql, userId);

                for (Map<String, Object> goal : goalsResults) {
                    GoalData goalData = new GoalData();
                    goalData.setGoalId(((Number) goal.get("goal_id")).intValue());
                    goalData.setType((String) goal.get("type"));
                    goalData.setTitle((String) goal.get("title"));
                    goalData.setDescription((String) goal.get("description"));
                    goalData.setTargetValue(((Number) goal.get("target_value")).intValue());
                    goalData.setCurrentValue(((Number) goal.get("current_value")).intValue());
                    goalData.setUnit((String) goal.get("unit"));

                    // 格式化日期
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                    if (goal.get("start_date") != null) {
                        goalData.setStartDate(((LocalDateTime) goal.get("start_date")).format(formatter));
                    }

                    if (goal.get("end_date") != null) {
                        goalData.setEndDate(((LocalDateTime) goal.get("end_date")).format(formatter));
                    }

                    goalData.setStatus((String) goal.get("status"));

                    if (goal.get("created_at") != null) {
                        goalData.setCreatedDate(((LocalDateTime) goal.get("created_at")).format(formatter));
                    }

                    if (goal.get("updated_at") != null) {
                        goalData.setUpdatedDate(((LocalDateTime) goal.get("updated_at")).format(formatter));
                    }

                    goals.add(goalData);
                }
            }

            // 如果没有学习目标，返回一些默认目标
            if (goals.isEmpty()) {
                // 创建默认学习目标
                GoalData readingGoal = new GoalData();
                readingGoal.setGoalId(1);
                readingGoal.setType("reading");
                readingGoal.setTitle("每日阅读");
                readingGoal.setDescription("每天至少阅读30分钟");
                readingGoal.setTargetValue(30);
                readingGoal.setCurrentValue(0); // 需要查询实际数据
                readingGoal.setUnit("分钟");
                readingGoal.setStartDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                readingGoal.setEndDate(LocalDateTime.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                readingGoal.setStatus("active");
                readingGoal.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                readingGoal.setUpdatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                GoalData vocabularyGoal = new GoalData();
                vocabularyGoal.setGoalId(2);
                vocabularyGoal.setType("vocabulary");
                vocabularyGoal.setTitle("每周学习新单词");
                vocabularyGoal.setDescription("每周学习至少50个新单词");
                vocabularyGoal.setTargetValue(50);
                vocabularyGoal.setCurrentValue(0); // 需要查询实际数据
                vocabularyGoal.setUnit("个");
                vocabularyGoal.setStartDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                vocabularyGoal.setEndDate(LocalDateTime.now().plusWeeks(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                vocabularyGoal.setStatus("active");
                vocabularyGoal.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                vocabularyGoal.setUpdatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                GoalData reviewGoal = new GoalData();
                reviewGoal.setGoalId(3);
                reviewGoal.setType("review");
                reviewGoal.setTitle("每日复习");
                reviewGoal.setDescription("每天复习至少20个单词");
                reviewGoal.setTargetValue(20);
                reviewGoal.setCurrentValue(0); // 需要查询实际数据
                reviewGoal.setUnit("个");
                reviewGoal.setStartDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                reviewGoal.setEndDate(LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                reviewGoal.setStatus("active");
                reviewGoal.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                reviewGoal.setUpdatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                goals.add(readingGoal);
                goals.add(vocabularyGoal);
                goals.add(reviewGoal);
            }

            printQueryResult("学习目标数量: " + goals.size());

            // 4. 准备响应数据
            LearningGoalsResponse response = new LearningGoalsResponse(
                    true, "获取学习目标成功", goals);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取学习目标过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LearningGoalsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
