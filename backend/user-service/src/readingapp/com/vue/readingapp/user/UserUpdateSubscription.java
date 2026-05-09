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
public class UserUpdateSubscription {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新订阅计划请求 ===");
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
    public static class UpdateSubscriptionRequest {
        private String plan_id;

        public String getPlan_id() { return plan_id; }
        public void setPlan_id(String plan_id) { this.plan_id = plan_id; }
    }

    // 响应DTO
    public static class UpdateSubscriptionResponse {
        private boolean success;
        private String message;
        private UserGetSubscriptionInfo.SubscriptionData subscription;

        public UpdateSubscriptionResponse(boolean success, String message, UserGetSubscriptionInfo.SubscriptionData subscription) {
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

    @PutMapping("/subscription")
    public ResponseEntity<UpdateSubscriptionResponse> updateSubscription(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateSubscriptionRequest request) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateSubscriptionResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateSubscriptionResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证请求数据
            if (request.getPlan_id() == null || request.getPlan_id().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateSubscriptionResponse(false, "计划ID不能为空", null)
                );
            }

            // 4. 验证计划ID是否有效
            String[] validPlanIds = {"free", "premium_monthly", "premium_yearly"};
            boolean validPlanId = false;
            for (String planId : validPlanIds) {
                if (planId.equals(request.getPlan_id())) {
                    validPlanId = true;
                    break;
                }
            }

            if (!validPlanId) {
                return ResponseEntity.badRequest().body(
                        new UpdateSubscriptionResponse(false, "无效的计划ID", null)
                );
            }

            // 5. 确定用户类型
            String userType;
            if (request.getPlan_id().startsWith("premium")) {
                userType = "premium";
            } else {
                userType = "free";
            }

            // 6. 更新用户类型（假设users表有user_type字段）
            String updateSql = "UPDATE users SET user_type = ? WHERE user_id = ?";
            int rowsUpdated = jdbcTemplate.update(updateSql, userType, userId);

            printQueryResult("更新订阅行数: " + rowsUpdated);

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateSubscriptionResponse(false, "用户不存在", null)
                );
            }

            // 7. 查询更新后的用户信息
            String userSql = "SELECT user_id, username, email, user_type, created_at FROM users WHERE user_id = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateSubscriptionResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = users.get(0);

            // 8. 构建订阅数据
            UserGetSubscriptionInfo.SubscriptionData subscription = new UserGetSubscriptionInfo.SubscriptionData();

            // 设置订阅类型和状态
            subscription.setType(userType);
            subscription.setStatus("active".equals(userType) ? "active" : "inactive");

            // 设置日期信息
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 假设订阅开始时间是用户注册时间
            LocalDateTime startDate = (LocalDateTime) user.get("created_at");
            subscription.setStartDate(startDate.format(formatter));

            // 如果是付费用户，设置结束时间和续订时间
            if ("premium".equals(userType)) {
                LocalDateTime endDate = startDate.plusMonths(1);
                subscription.setEndDate(endDate.format(formatter));
                subscription.setRenewalDate(endDate.format(formatter));
                subscription.setAutoRenew(true);
            } else {
                subscription.setEndDate("");
                subscription.setRenewalDate("");
                subscription.setAutoRenew(false);
            }

            // 9. 构建计划数据
            UserGetSubscriptionInfo.PlanData plan;
            if ("premium".equals(userType)) {
                if (request.getPlan_id().equals("premium_yearly")) {
                    plan = new UserGetSubscriptionInfo.PlanData(
                            "premium_yearly",
                            "高级会员（年付）",
                            299.99,
                            "CNY",
                            "yearly",
                            "享受所有高级功能，年付更优惠"
                    );
                } else {
                    plan = new UserGetSubscriptionInfo.PlanData(
                            "premium_monthly",
                            "高级会员（月付）",
                            29.99,
                            "CNY",
                            "monthly",
                            "享受所有高级功能，包括无限文档、高级词汇管理、个性化学习报告等"
                    );
                }
            } else {
                plan = new UserGetSubscriptionInfo.PlanData(
                        "free",
                        "免费版",
                        0.00,
                        "CNY",
                        "none",
                        "基础功能，包括有限文档管理、基础词汇学习、基本学习统计"
                );
            }
            subscription.setPlan(plan);

            // 10. 构建功能列表
            List<UserGetSubscriptionInfo.FeatureData> features = new java.util.ArrayList<>();

            if ("premium".equals(userType)) {
                features.add(new UserGetSubscriptionInfo.FeatureData("无限文档", "上传和管理无限数量的文档", true));
                features.add(new UserGetSubscriptionInfo.FeatureData("高级词汇管理", "使用智能分类和标签管理词汇", true));
                features.add(new UserGetSubscriptionInfo.FeatureData("个性化学习报告", "获取详细的学习分析和建议", true));
                features.add(new UserGetSubscriptionInfo.FeatureData("离线同步", "在所有设备上同步学习数据", true));
                features.add(new UserGetSubscriptionInfo.FeatureData("优先支持", "获得优先技术支持", true));
            } else {
                features.add(new UserGetSubscriptionInfo.FeatureData("有限文档", "最多上传10个文档", true));
                features.add(new UserGetSubscriptionInfo.FeatureData("基础词汇管理", "基本的词汇添加和查看", true));
                features.add(new UserGetSubscriptionInfo.FeatureData("基本学习统计", "查看基础的学习数据", true));
                features.add(new UserGetSubscriptionInfo.FeatureData("离线同步", "基础数据同步", false));
                features.add(new UserGetSubscriptionInfo.FeatureData("标准支持", "获得标准技术支持", true));
            }
            subscription.setFeatures(features);

            // 11. 构建账单数据
            UserGetSubscriptionInfo.BillingData billing;
            if ("premium".equals(userType)) {
                if (request.getPlan_id().equals("premium_yearly")) {
                    billing = new UserGetSubscriptionInfo.BillingData(
                            startDate.format(formatter),
                            299.99,
                            startDate.plusYears(1).format(formatter),
                            299.99,
                            "credit_card"
                    );
                } else {
                    billing = new UserGetSubscriptionInfo.BillingData(
                            startDate.format(formatter),
                            29.99,
                            startDate.plusMonths(1).format(formatter),
                            29.99,
                            "credit_card"
                    );
                }
            } else {
                billing = new UserGetSubscriptionInfo.BillingData(
                        "",
                        0.00,
                        "",
                        0.00,
                        ""
                );
            }
            subscription.setBilling(billing);

            // 12. 准备响应数据
            UpdateSubscriptionResponse response = new UpdateSubscriptionResponse(
                    true, "订阅计划更新成功", subscription);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新订阅计划过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateSubscriptionResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
