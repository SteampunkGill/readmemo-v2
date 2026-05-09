package com.vue.readingapp.feedback;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackMarkAsResolved {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到标记反馈为已处理请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("===========================");
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
    public static class MarkAsResolvedRequest {
        private String feedbackId;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    }

    // 响应DTO
    public static class MarkAsResolvedResponse {
        private boolean success;
        private String message;
        private MarkAsResolvedData data;

        public MarkAsResolvedResponse(boolean success, String message, MarkAsResolvedData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public MarkAsResolvedData getData() { return data; }
        public void setData(MarkAsResolvedData data) { this.data = data; }
    }

    public static class MarkAsResolvedData {
        private String feedbackId;
        private boolean resolved;
        private String resolvedAt;

        public MarkAsResolvedData(String feedbackId, boolean resolved, String resolvedAt) {
            this.feedbackId = feedbackId;
            this.resolved = resolved;
            this.resolvedAt = resolvedAt;
        }

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }

        public String getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
    }

    @PutMapping("/{feedbackId}/resolve")
    public ResponseEntity<MarkAsResolvedResponse> markFeedbackAsResolved(
            @PathVariable String feedbackId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 创建请求对象用于打印
        MarkAsResolvedRequest request = new MarkAsResolvedRequest();
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new MarkAsResolvedResponse(false, "反馈ID不能为空", null)
                );
            }

            // 2. 验证用户身份
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MarkAsResolvedResponse(false, "用户未登录", null)
                );
            }

            // 3. 检查反馈是否存在
            String checkSql = "SELECT feedback_id, user_id, status FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new MarkAsResolvedResponse(false, "未找到指定的反馈", null)
                );
            }

            Map<String, Object> feedback = feedbackList.get(0);
            String feedbackUserId = (String) feedback.get("user_id");
            String currentStatus = (String) feedback.get("status");

            // 4. 检查权限（只有反馈创建者或管理员可以标记为已处理）
            boolean hasPermission = false;

            if (userId.equals(feedbackUserId)) {
                hasPermission = true;
            } else {
                // 检查是否是管理员
                String adminCheckSql = "SELECT role FROM users WHERE user_id = ?";
                List<Map<String, Object>> userList = jdbcTemplate.queryForList(adminCheckSql, userId);

                if (!userList.isEmpty()) {
                    Map<String, Object> user = userList.get(0);
                    String role = (String) user.get("role");

                    if ("admin".equals(role) || "moderator".equals(role)) {
                        hasPermission = true;
                    }
                }
            }

            if (!hasPermission) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new MarkAsResolvedResponse(false, "没有权限标记此反馈", null)
                );
            }

            // 5. 检查是否已经是已处理状态
            List<String> resolvedStatuses = List.of("completed", "rejected", "duplicate");
            if (resolvedStatuses.contains(currentStatus)) {
                MarkAsResolvedData resolvedData = new MarkAsResolvedData(
                        feedbackId,
                        true,
                        LocalDateTime.now().toString()
                );

                MarkAsResolvedResponse response = new MarkAsResolvedResponse(
                        true, "反馈已经是已处理状态", resolvedData
                );

                printResponse(response);
                return ResponseEntity.ok(response);
            }

            // 6. 更新反馈状态为completed（已处理）
            String updateSql = "UPDATE user_feedback SET status = 'completed', completed_at = NOW(), updated_at = NOW() WHERE feedback_id = ?";
            int rowsAffected = jdbcTemplate.update(updateSql, feedbackId);

            printQueryResult("更新行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new MarkAsResolvedResponse(false, "标记为已处理失败", null)
                );
            }

            // 7. 记录变更日志
            String logSql = "INSERT INTO feedback_change_log (feedback_id, field, old_value, new_value, changed_by, reason, created_at) " +
                    "VALUES (?, 'status', ?, 'completed', ?, '标记为已处理', NOW())";

            jdbcTemplate.update(logSql,
                    feedbackId,
                    currentStatus,
                    userId
            );

            // 8. 准备响应数据
            MarkAsResolvedData resolvedData = new MarkAsResolvedData(
                    feedbackId,
                    true,
                    LocalDateTime.now().toString()
            );

            MarkAsResolvedResponse response = new MarkAsResolvedResponse(
                    true, "反馈已标记为已处理", resolvedData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("标记反馈为已处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new MarkAsResolvedResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}