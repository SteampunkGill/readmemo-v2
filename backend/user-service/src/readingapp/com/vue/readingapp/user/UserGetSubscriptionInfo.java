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
public class UserGetSubscriptionInfo {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取订阅信息请求 ===");
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
    public static class SubscriptionInfoResponse {
        private boolean success;
        private String message;
        private SubscriptionData subscription;

        public SubscriptionInfoResponse(boolean success, String message, SubscriptionData subscription) {
            this.success = success;
            this.message = message;
            this.subscription = subscription;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SubscriptionData getSubscription() { return subscription; }
        public void setSubscription(SubscriptionData subscription) { this.subscription = subscription; }
    }

    public static class SubscriptionData {
        private String type;
        private String status;
        private String startDate;
        private String endDate;
        private String renewalDate;
        private boolean autoRenew;
        private PlanData plan;
        private List<FeatureData> features;
        private BillingData billing;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }

        public String getRenewalDate() { return renewalDate; }
        public void setRenewalDate(String renewalDate) { this.renewalDate = renewalDate; }

        public boolean isAutoRenew() { return autoRenew; }
        public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }

        public PlanData getPlan() { return plan; }
        public void setPlan(PlanData plan) { this.plan = plan; }

        public List<FeatureData> getFeatures() { return features; }
        public void setFeatures(List<FeatureData> features) { this.features = features; }

        public BillingData getBilling() { return billing; }
        public void setBilling(BillingData billing) { this.billing = billing; }
    }

    public static class PlanData {
        private String id;
        private String name;
        private double price;
        private String currency;
        private String billingCycle;
        private String description;

        public PlanData(String id, String name, double price, String currency, String billingCycle, String description) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.currency = currency;
            this.billingCycle = billingCycle;
            this.description = description;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getBillingCycle() { return billingCycle; }
        public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class FeatureData {
        private String name;
        private String description;
        private boolean enabled;

        public FeatureData(String name, String description, boolean enabled) {
            this.name = name;
            this.description = description;
            this.enabled = enabled;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class BillingData {
        private String lastPaymentDate;
        private double lastPaymentAmount;
        private String nextPaymentDate;
        private double nextPaymentAmount;
        private String paymentMethod;

        public BillingData(String lastPaymentDate, double lastPaymentAmount, String nextPaymentDate,
                           double nextPaymentAmount, String paymentMethod) {
            this.lastPaymentDate = lastPaymentDate;
            this.lastPaymentAmount = lastPaymentAmount;
            this.nextPaymentDate = nextPaymentDate;
            this.nextPaymentAmount = nextPaymentAmount;
            this.paymentMethod = paymentMethod;
        }

        public String getLastPaymentDate() { return lastPaymentDate; }
        public void setLastPaymentDate(String lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }

        public double getLastPaymentAmount() { return lastPaymentAmount; }
        public void setLastPaymentAmount(double lastPaymentAmount) { this.lastPaymentAmount = lastPaymentAmount; }

        public String getNextPaymentDate() { return nextPaymentDate; }
        public void setNextPaymentDate(String nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }

        public double getNextPaymentAmount() { return nextPaymentAmount; }
        public void setNextPaymentAmount(double nextPaymentAmount) { this.nextPaymentAmount = nextPaymentAmount; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        } else if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        }
        return null;
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionInfoResponse> getSubscriptionInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("Authorization: " + authHeader);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SubscriptionInfoResponse(false, "请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SubscriptionInfoResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 查询用户信息（假设用户类型存储在user_type字段）
            String userSql = "SELECT user_id, username, email, user_type, created_at FROM users WHERE user_id = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new SubscriptionInfoResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = users.get(0);
            String userType = user.get("user_type") != null ? (String) user.get("user_type") : "free";

            // 4. 构建订阅数据
            SubscriptionData subscription = new SubscriptionData();

            // 设置订阅类型和状态
            subscription.setType(userType);
            subscription.setStatus("active".equals(userType) ? "active" : "inactive");

            // 设置日期信息
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 假设订阅开始时间是用户注册时间
            LocalDateTime startDate = convertToLocalDateTime(user.get("created_at"));
            subscription.setStartDate(startDate != null ? startDate.format(formatter) : "");

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

            // 5. 构建计划数据
            PlanData plan;
            if ("premium".equals(userType)) {
                plan = new PlanData(
                        "premium_monthly",
                        "高级会员（月付）",
                        29.99,
                        "CNY",
                        "monthly",
                        "享受所有高级功能，包括无限文档、高级词汇管理、个性化学习报告等"
                );
            } else {
                plan = new PlanData(
                        "free",
                        "免费版",
                        0.00,
                        "CNY",
                        "none",
                        "基础功能，包括有限文档管理、基础词汇学习、基本学习统计"
                );
            }
            subscription.setPlan(plan);

            // 6. 构建功能列表
            List<FeatureData> features = new java.util.ArrayList<>();

            if ("premium".equals(userType)) {
                features.add(new FeatureData("无限文档", "上传和管理无限数量的文档", true));
                features.add(new FeatureData("高级词汇管理", "使用智能分类和标签管理词汇", true));
                features.add(new FeatureData("个性化学习报告", "获取详细的学习分析和建议", true));
                features.add(new FeatureData("离线同步", "在所有设备上同步学习数据", true));
                features.add(new FeatureData("优先支持", "获得优先技术支持", true));
            } else {
                features.add(new FeatureData("有限文档", "最多上传10个文档", true));
                features.add(new FeatureData("基础词汇管理", "基本的词汇添加和查看", true));
                features.add(new FeatureData("基本学习统计", "查看基础的学习数据", true));
                features.add(new FeatureData("离线同步", "基础数据同步", false));
                features.add(new FeatureData("标准支持", "获得标准技术支持", true));
            }
            subscription.setFeatures(features);

            // 7. 构建账单数据
            BillingData billing;
            if ("premium".equals(userType)) {
                billing = new BillingData(
                        startDate.format(formatter),
                        29.99,
                        startDate.plusMonths(1).format(formatter),
                        29.99,
                        "credit_card"
                );
            } else {
                billing = new BillingData(
                        "",
                        0.00,
                        "",
                        0.00,
                        ""
                );
            }
            subscription.setBilling(billing);

            printQueryResult(subscription);

            // 8. 准备响应数据
            SubscriptionInfoResponse response = new SubscriptionInfoResponse(
                    true, "获取订阅信息成功", subscription);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取订阅信息过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SubscriptionInfoResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
