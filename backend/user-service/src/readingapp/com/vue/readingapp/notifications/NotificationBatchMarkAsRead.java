package com.vue.readingapp.notifications;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationBatchMarkAsRead {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量标记通知为已读请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("===============================");
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
    public static class BatchMarkAsReadRequest {
        private List<String> ids;

        public List<String> getIds() { return ids; }
        public void setIds(List<String> ids) { this.ids = ids; }
    }

    // 响应DTO
    public static class BatchMarkAsReadResponse {
        private boolean success;
        private String message;
        private int processedCount;
        private int failedCount;
        private List<FailedItem> failedItems;

        public BatchMarkAsReadResponse(boolean success, String message, int processedCount, int failedCount, List<FailedItem> failedItems) {
            this.success = success;
            this.message = message;
            this.processedCount = processedCount;
            this.failedCount = failedCount;
            this.failedItems = failedItems;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getProcessedCount() { return processedCount; }
        public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }

        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

        public List<FailedItem> getFailedItems() { return failedItems; }
        public void setFailedItems(List<FailedItem> failedItems) { this.failedItems = failedItems; }
    }

    public static class FailedItem {
        private String id;
        private String error;

        public FailedItem(String id, String error) {
            this.id = id;
            this.error = error;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    @PutMapping("/batch-read")
    public ResponseEntity<BatchMarkAsReadResponse> batchMarkAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BatchMarkAsReadRequest request) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证信息
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchMarkAsReadResponse(false, "请先登录", 0, 0, null)
                );
            }

            String token = authHeader.substring(7);

            // 2. 根据token获取用户ID
            String getUserSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(getUserSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchMarkAsReadResponse(false, "登录已过期，请重新登录", 0, 0, null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer userId = (Integer) session.get("user_id");

            // 3. 验证请求数据
            if (request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchMarkAsReadResponse(false, "请提供要标记为已读的通知ID列表", 0, 0, null)
                );
            }

            // 4. 转换ID列表为整数列表
            List<Integer> notificationIdInts = new ArrayList<>();
            List<FailedItem> failedItems = new ArrayList<>();

            for (String notificationId : request.getIds()) {
                try {
                    if (notificationId.startsWith("notif_")) {
                        notificationIdInts.add(Integer.parseInt(notificationId.substring(6)));
                    } else {
                        notificationIdInts.add(Integer.parseInt(notificationId));
                    }
                } catch (NumberFormatException e) {
                    failedItems.add(new FailedItem(notificationId, "通知ID格式错误"));
                }
            }

            if (notificationIdInts.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchMarkAsReadResponse(false, "没有有效的通知ID", 0, failedItems.size(), failedItems)
                );
            }

            // 5. 构建IN查询参数
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < notificationIdInts.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }

            // 6. 批量标记为已读
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            String updateSql = "UPDATE notifications SET is_read = true, read_at = ? WHERE notification_id IN (" + placeholders + ") AND user_id = ? AND is_read = false";
            List<Object> params = new ArrayList<>();
            params.add(now);
            params.addAll(notificationIdInts);
            params.add(userId);

            int updatedRows = jdbcTemplate.update(updateSql, params.toArray());

            printQueryResult("更新行数: " + updatedRows);

            // 7. 创建响应
            BatchMarkAsReadResponse response = new BatchMarkAsReadResponse(
                    true,
                    "批量标记通知为已读成功",
                    updatedRows,
                    failedItems.size(),
                    failedItems.isEmpty() ? null : failedItems
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量标记通知为已读过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchMarkAsReadResponse(false, "服务器内部错误: " + e.getMessage(), 0, 0, null)
            );
        }
    }
}