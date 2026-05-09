
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

@RestController
@RequestMapping("/api/v1/user")
public class UserSetLearningGoal {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到设置学习目标请求 ===");
        System.out.println("请求数据: " + request);
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

    // 请求DTO
    public static class SetLearningGoalRequest {
        private String type;
        private String title;
        private String description;
        private int targetValue;
        private String unit;
        private String endDate;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getTargetValue() { return targetValue; }
        public void setTargetValue(int targetValue) { this.targetValue = targetValue; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    // 响应DTO
    public static class SetLearningGoalResponse {
        private boolean success;
        private String message;
        private UserGetLearningGoals.GoalData goal;

        public SetLearningGoalResponse(boolean success, String message, UserGetLearningGoals.GoalData goal) {
            this.success = success;
            this.message = message;
            this.goal = goal;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserGetLearningGoals.GoalData getGoal() { return goal; }
        public void setGoal(UserGetLearningGoals.GoalData goal) { this.goal = goal; }
    }

    @PostMapping("/goals")
    public ResponseEntity<SetLearningGoalResponse> setLearningGoal(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SetLearningGoalRequest request) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SetLearningGoalResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SetLearningGoalResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证请求数据
            if (request.getType() == null || request.getType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SetLearningGoalResponse(false, "目标类型不能为空", null)
                );
            }

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SetLearningGoalResponse(false, "目标标题不能为空", null)
                );
            }

            if (request.getTargetValue() <= 0) {
                return ResponseEntity.badRequest().body(
                        new SetLearningGoalResponse(false, "目标值必须大于0", null)
                );
            }

            // 4. 检查learning_goals表是否存在，如果不存在则创建
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name = 'learning_goals'";

            int tableExists = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableExists == 0) {
                // 创建learning_goals表
                String createTableSql = "CREATE TABLE learning_goals (" +
                        "goal_id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "user_id INT NOT NULL, " +
                        "type VARCHAR(50) NOT NULL, " +
                        "title VARCHAR(200) NOT NULL, " +
                        "description TEXT, " +
                        "target_value INT NOT NULL, " +
                        "current_value INT DEFAULT 0, " +
                        "unit VARCHAR(50), " +
                        "start_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "end_date DATETIME, " +
                        "status VARCHAR(20) DEFAULT 'active', " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                        ")";

                jdbcTemplate.execute(createTableSql);
                System.out.println("已创建learning_goals表");
            }

            // 5. 插入新的学习目标
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endDate = null;

            if (request.getEndDate() != null && !request.getEndDate().trim().isEmpty()) {
                endDate = LocalDateTime.parse(request.getEndDate() + "T00:00:00");
            }

            String insertSql = "INSERT INTO learning_goals (user_id, type, title, description, " +
                    "target_value, current_value, unit, start_date, end_date, status) " +
                    "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, 'active')";

            int rowsInserted = jdbcTemplate.update(insertSql,
                    userId,
                    request.getType(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getTargetValue(),
                    request.getUnit(),
                    now,
                    endDate
            );

            printQueryResult("插入学习目标行数: " + rowsInserted);

            if (rowsInserted == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new SetLearningGoalResponse(false, "创建学习目标失败", null)
                );
            }

            // 6. 获取新插入的目标ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as goal_id";
            Map<String, Object> lastIdResult = jdbcTemplate.queryForMap(lastIdSql);
            int goalId = ((Number) lastIdResult.get("goal_id")).intValue();

            // 7. 查询新创建的学习目标
            String selectSql = "SELECT goal_id, type, title, description, target_value, current_value, " +
                    "unit, start_date, end_date, status, created_at, updated_at " +
                    "FROM learning_goals WHERE goal_id = ?";

            List<Map<String, Object>> goalResults = jdbcTemplate.queryForList(selectSql, goalId);

            if (goalResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new SetLearningGoalResponse(false, "学习目标不存在", null)
                );
            }

            Map<String, Object> goal = goalResults.get(0);

            // 8. 构建目标数据
            UserGetLearningGoals.GoalData goalData = new UserGetLearningGoals.GoalData();
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

            // 9. 准备响应数据
            SetLearningGoalResponse response = new SetLearningGoalResponse(
                    true, "学习目标设置成功", goalData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("设置学习目标过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SetLearningGoalResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}