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
public class FeedbackUpdateStatus {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新反馈状态请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
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
    public static class UpdateStatusRequest {
        private String feedbackId;
        private String status;
        private String reason;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // 响应DTO
    public static class UpdateStatusResponse {
        private boolean success;
        private String message;
        private FeedbackStatusData data;

        public UpdateStatusResponse(boolean success, String message, FeedbackStatusData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public FeedbackStatusData getData() { return data; }
        public void setData(FeedbackStatusData data) { this.data = data; }
    }

    public static class FeedbackStatusData {
        private String id;
        private String status;
        private String updatedAt;

        public FeedbackStatusData(String id, String status, String updatedAt) {
            this.id = id;
            this.status = status;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/{feedbackId}/status")
    public ResponseEntity<UpdateStatusResponse> updateFeedbackStatus(
            @PathVariable String feedbackId,
            @RequestBody UpdateStatusRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 设置feedbackId
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateStatusResponse(false, "反馈ID不能为空", null)
                );
            }

            if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateStatusResponse(false, "状态不能为空", null)
                );
            }

            // 2. 验证状态值的合法性
            List<String> validStatuses = List.of("pending", "reviewing", "in_progress", "completed", "rejected", "duplicate");
            if (!validStatuses.contains(request.getStatus())) {
                return ResponseEntity.badRequest().body(
                        new UpdateStatusResponse(false, "无效的状态值", null)
                );
            }

            // 3. 检查反馈是否存在
            String checkSql = "SELECT feedback_id, status FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateStatusResponse(false, "未找到指定的反馈", null)
                );
            }

            Map<String, Object> feedback = feedbackList.get(0);
            String oldStatus = (String) feedback.get("status");

            // 4. 如果状态没有变化，直接返回成功
            if (oldStatus.equals(request.getStatus())) {
                FeedbackStatusData statusData = new FeedbackStatusData(
                        feedbackId,
                        request.getStatus(),
                        LocalDateTime.now().toString()
                );

                UpdateStatusResponse response = new UpdateStatusResponse(
                        true, "状态未发生变化", statusData
                );

                printResponse(response);
                return ResponseEntity.ok(response);
            }

            // 5. 更新反馈状态
            String updateSql = "UPDATE user_feedback SET status = ?, updated_at = NOW() WHERE feedback_id = ?";
            int rowsAffected = jdbcTemplate.update(updateSql, request.getStatus(), feedbackId);

            printQueryResult("更新行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateStatusResponse(false, "更新反馈状态失败", null)
                );
            }

            // 6. 记录变更日志
            if (userId != null) {
                String logSql = "INSERT INTO feedback_change_log (feedback_id, field, old_value, new_value, changed_by, reason, created_at) " +
                        "VALUES (?, 'status', ?, ?, ?, ?, NOW())";

                jdbcTemplate.update(logSql,
                        feedbackId,
                        oldStatus,
                        request.getStatus(),
                        userId,
                        request.getReason() != null ? request.getReason() : "状态更新"
                );
            }

            // 7. 如果状态是completed，设置完成时间
            if ("completed".equals(request.getStatus())) {
                String completeSql = "UPDATE user_feedback SET completed_at = NOW() WHERE feedback_id = ?";
                jdbcTemplate.update(completeSql, feedbackId);
            }

            // 8. 查询更新后的反馈信息
            String selectSql = "SELECT feedback_id, status, updated_at FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> updatedFeedback = jdbcTemplate.queryForList(selectSql, feedbackId);

            if (updatedFeedback.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateStatusResponse(false, "无法获取更新后的反馈信息", null)
                );
            }

            Map<String, Object> updated = updatedFeedback.get(0);

            // 9. 准备响应数据
            FeedbackStatusData statusData = new FeedbackStatusData(
                    feedbackId,
                    (String) updated.get("status"),
                    updated.get("updated_at").toString()
            );

            UpdateStatusResponse response = new UpdateStatusResponse(
                    true, "反馈状态更新成功", statusData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新反馈状态过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateStatusResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}