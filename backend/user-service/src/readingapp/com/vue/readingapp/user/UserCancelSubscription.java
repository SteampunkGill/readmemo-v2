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
public class UserCancelSubscription {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到取消订阅请求 ===");
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
    public static class CancelSubscriptionResponse {
        private boolean success;
        private String message;
        private UserGetSubscriptionInfo.SubscriptionData subscription;

        public CancelSubscriptionResponse(boolean success, String message, UserGetSubscriptionInfo.SubscriptionData subscription) {
            this.success = success;
            this.message = message;
            this.subscription = subscription;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UserGetSubscriptionInfo.SubscriptionData getSubscription() { return subscription; }
        public void setSubscription(UserGetSubscriptionInfo.SubscriptionData subscription) { this.subscription = subscription; }
    }

    @DeleteMapping("/subscription")
    public ResponseEntity<CancelSubscriptionResponse> cancelSubscription(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("Authorization: " + authHeader);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CancelSubscriptionResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CancelSubscriptionResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 查询用户当前订阅状态
            String userSql = "SELECT user_id, username, email, user_type, created_at FROM users WHERE user_id = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CancelSubscriptionResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = users.get(0);
            String currentUserType = user.get("user_type") != null ? (String) user.get("user_type") : "free";

            // 4. 检查用户是否已经是免费用户
            if ("free".equals(currentUserType)) {
                return ResponseEntity.badRequest().body(
                        new CancelSubscriptionResponse(false, "您当前已经是免费用户", null)
                );
            }

            // 5. 更新用户类型为免费用户
            String updateSql = "UPDATE users SET user_type = 'free' WHERE user_id = ?";
            int rowsUpdated = jdbcTemplate.update(updateSql, userId);

            printQueryResult("取消订阅行数: " + rowsUpdated);

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CancelSubscriptionResponse(false, "用户不存在", null)
                );
            }

            // 6. 查询更新后的用户信息
            List<Map<String, Object>> updatedUsers = jdbcTemplate.queryForList(userSql, userId);

            if (updatedUsers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CancelSubscriptionResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> updatedUser = updatedUsers.get(0);

            // 7. 构建订阅数据（现在是免费用户）
            UserGetSubscriptionInfo.SubscriptionData subscription = new UserGetSubscriptionInfo.SubscriptionData();

            // 设置订阅类型和状态
            subscription.setType("free");
            subscription.setStatus("inactive");

            // 设置日期信息
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 假设订阅开始时间是用户注册时间
            LocalDateTime startDate = (LocalDateTime) updatedUser.get("created_at");
            subscription.setStartDate(startDate.format(formatter));
            subscription.setEndDate("");
            subscription.setRenewalDate("");
            subscription.setAutoRenew(false);

            // 8. 构建计划数据
            UserGetSubscriptionInfo.PlanData plan = new UserGetSubscriptionInfo.PlanData(
                    "free",
                    "免费版",
                    0.00,
                    "CNY",
                    "none",
                    "基础功能，包括有限文档管理、基础词汇学习、基本学习统计"
            );
            subscription.setPlan(plan);

            // 9. 构建功能列表
            List<UserGetSubscriptionInfo.FeatureData> features = new java.util.ArrayList<>();
            features.add(new UserGetSubscriptionInfo.FeatureData("有限文档", "最多上传10个文档", true));
            features.add(new UserGetSubscriptionInfo.FeatureData("基础词汇管理", "基本的词汇添加和查看", true));
            features.add(new UserGetSubscriptionInfo.FeatureData("基本学习统计", "查看基础的学习数据", true));
            features.add(new UserGetSubscriptionInfo.FeatureData("离线同步", "基础数据同步", false));
            features.add(new UserGetSubscriptionInfo.FeatureData("标准支持", "获得标准技术支持", true));

            subscription.setFeatures(features);

            // 10. 构建账单数据
            UserGetSubscriptionInfo.BillingData billing = new UserGetSubscriptionInfo.BillingData(
                    "",
                    0.00,
                    "",
                    0.00,
                    ""
            );
            subscription.setBilling(billing);

            // 11. 准备响应数据
            CancelSubscriptionResponse response = new CancelSubscriptionResponse(
                    true, "订阅已成功取消，您现在是免费用户", subscription);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("取消订阅过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CancelSubscriptionResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
