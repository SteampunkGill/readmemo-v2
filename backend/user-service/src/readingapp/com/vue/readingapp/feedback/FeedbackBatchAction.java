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
public class FeedbackBatchAction {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量操作反馈请求 ===");
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
    public static class BatchActionRequest {
        private String action;
        private List<String> feedbackIds;
        private String status;
        private String reason;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public List<String> getFeedbackIds() { return feedbackIds; }
        public void setFeedbackIds(List<String> feedbackIds) { this.feedbackIds = feedbackIds; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // 响应DTO
    public static class BatchActionResponse {
        private boolean success;
        private String message;
        private BatchActionData data;

        public BatchActionResponse(boolean success, String message, BatchActionData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchActionData getData() { return data; }
        public void setData(BatchActionData data) { this.data = data; }
    }

    public static class BatchActionData {
        private int total;
        private int successCount;
        private int failCount;
        private List<BatchResult> results;

        public BatchActionData(int total, int successCount, int failCount, List<BatchResult> results) {
            this.total = total;
            this.successCount = successCount;
            this.failCount = failCount;
            this.results = results;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }

        public List<BatchResult> getResults() { return results; }
        public void setResults(List<BatchResult> results) { this.results = results; }
    }

    public static class BatchResult {
        private String feedbackId;
        private boolean success;
        private String message;

        public BatchResult(String feedbackId, boolean success, String message) {
            this.feedbackId = feedbackId;
            this.success = success;
            this.message = message;
        }

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchActionResponse> batchActionFeedback(
            @RequestBody BatchActionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (request.getAction() == null || request.getAction().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchActionResponse(false, "操作类型不能为空", null)
                );
            }

            if (request.getFeedbackIds() == null || request.getFeedbackIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchActionResponse(false, "反馈ID列表不能为空", null)
                );
            }

            // 2. 验证用户身份（需要管理员权限）
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchActionResponse(false, "用户未登录", null)
                );
            }

            // 检查是否是管理员
            String adminCheckSql = "SELECT role FROM users WHERE user_id = ?";
            List<Map<String, Object>> userList = jdbcTemplate.queryForList(adminCheckSql, userId);

            boolean isAdmin = false;
            if (!userList.isEmpty()) {
                Map<String, Object> user = userList.get(0);
                String role = (String) user.get("role");
                isAdmin = "admin".equals(role) || "moderator".equals(role);
            }

            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new BatchActionResponse(false, "需要管理员权限", null)
                );
            }

            // 3. 根据操作类型执行批量操作
            String action = request.getAction();
            List<BatchResult> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            switch (action) {
                case "update_status":
                    results = batchUpdateStatus(request, userId);
                    break;
                case "delete":
                    results = batchDelete(request, userId);
                    break;
                case "mark_as_read":
                    results = batchMarkAsRead(request, userId);
                    break;
                case "mark_as_resolved":
                    results = batchMarkAsResolved(request, userId);
                    break;
                default:
                    return ResponseEntity.badRequest().body(
                            new BatchActionResponse(false, "不支持的操作类型: " + action, null)
                    );
            }

            // 4. 统计成功和失败数量
            for (BatchResult result : results) {
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            // 5. 准备响应数据
            BatchActionData actionData = new BatchActionData(
                    request.getFeedbackIds().size(),
                    successCount,
                    failCount,
                    results
            );

            BatchActionResponse response = new BatchActionResponse(
                    true,
                    String.format("批量操作完成，成功: %d, 失败: %d", successCount, failCount),
                    actionData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量操作反馈过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchActionResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private List<BatchResult> batchUpdateStatus(BatchActionRequest request, String userId) {
        List<BatchResult> results = new ArrayList<>();
        String newStatus = request.getStatus();
        String reason = request.getReason() != null ? request.getReason() : "批量更新状态";

        // 验证状态值
        List<String> validStatuses = List.of("pending", "reviewing", "in_progress", "completed", "rejected", "duplicate");
        if (newStatus == null || !validStatuses.contains(newStatus)) {
            for (String feedbackId : request.getFeedbackIds()) {
                results.add(new BatchResult(feedbackId, false, "无效的状态值"));
            }
            return results;
        }

        for (String feedbackId : request.getFeedbackIds()) {
            try {
                // 检查反馈是否存在
                String checkSql = "SELECT feedback_id, status FROM user_feedback WHERE feedback_id = ?";
                List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

                if (feedbackList.isEmpty()) {
                    results.add(new BatchResult(feedbackId, false, "反馈不存在"));
                    continue;
                }

                Map<String, Object> feedback = feedbackList.get(0);
                String oldStatus = (String) feedback.get("status");

                // 如果状态没有变化，直接记录成功
                if (oldStatus.equals(newStatus)) {
                    results.add(new BatchResult(feedbackId, true, "状态未发生变化"));
                    continue;
                }

                // 更新状态
                String updateSql = "UPDATE user_feedback SET status = ?, updated_at = NOW() WHERE feedback_id = ?";
                int rowsAffected = jdbcTemplate.update(updateSql, newStatus, feedbackId);

                if (rowsAffected > 0) {
                    // 记录变更日志
                    String logSql = "INSERT INTO feedback_change_log (feedback_id, field, old_value, new_value, changed_by, reason, created_at) " +
                            "VALUES (?, 'status', ?, ?, ?, ?, NOW())";

                    jdbcTemplate.update(logSql,
                            feedbackId,
                            oldStatus,
                            newStatus,
                            userId,
                            reason
                    );

                    // 如果新状态是completed，设置完成时间
                    if ("completed".equals(newStatus)) {
                        String completeSql = "UPDATE user_feedback SET completed_at = NOW() WHERE feedback_id = ?";
                        jdbcTemplate.update(completeSql, feedbackId);
                    }

                    results.add(new BatchResult(feedbackId, true, "状态更新成功"));
                } else {
                    results.add(new BatchResult(feedbackId, false, "状态更新失败"));
                }

            } catch (Exception e) {
                results.add(new BatchResult(feedbackId, false, "操作失败: " + e.getMessage()));
            }
        }

        return results;
    }

    private List<BatchResult> batchDelete(BatchActionRequest request, String userId) {
        List<BatchResult> results = new ArrayList<>();

        for (String feedbackId : request.getFeedbackIds()) {
            try {
                // 检查反馈是否存在
                String checkSql = "SELECT feedback_id FROM user_feedback WHERE feedback_id = ?";
                List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

                if (feedbackList.isEmpty()) {
                    results.add(new BatchResult(feedbackId, false, "反馈不存在"));
                    continue;
                }

                // 先删除相关的投票记录
                String deleteVotesSql = "DELETE FROM feedback_votes WHERE feedback_id = ?";
                jdbcTemplate.update(deleteVotesSql, feedbackId);

                // 先删除相关的回复记录
                String deleteRepliesSql = "DELETE FROM feedback_replies WHERE feedback_id = ?";
                jdbcTemplate.update(deleteRepliesSql, feedbackId);

                // 先删除相关的变更日志
                String deleteLogsSql = "DELETE FROM feedback_change_log WHERE feedback_id = ?";
                jdbcTemplate.update(deleteLogsSql, feedbackId);

                // 删除反馈主记录
                String deleteFeedbackSql = "DELETE FROM user_feedback WHERE feedback_id = ?";
                int rowsAffected = jdbcTemplate.update(deleteFeedbackSql, feedbackId);

                if (rowsAffected > 0) {
                    results.add(new BatchResult(feedbackId, true, "删除成功"));
                } else {
                    results.add(new BatchResult(feedbackId, false, "删除失败"));
                }

            } catch (Exception e) {
                results.add(new BatchResult(feedbackId, false, "删除失败: " + e.getMessage()));
            }
        }

        return results;
    }

    private List<BatchResult> batchMarkAsRead(BatchActionRequest request, String userId) {
        List<BatchResult> results = new ArrayList<>();

        for (String feedbackId : request.getFeedbackIds()) {
            try {
                // 检查反馈是否存在
                String checkSql = "SELECT feedback_id FROM user_feedback WHERE feedback_id = ?";
                List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

                if (feedbackList.isEmpty()) {
                    results.add(new BatchResult(feedbackId, false, "反馈不存在"));
                    continue;
                }

                // 更新view_count（增加一次查看）
                String updateSql = "UPDATE user_feedback SET view_count = view_count + 1, updated_at = NOW() WHERE feedback_id = ?";
                int rowsAffected = jdbcTemplate.update(updateSql, feedbackId);

                if (rowsAffected > 0) {
                    results.add(new BatchResult(feedbackId, true, "标记为已读成功"));
                } else {
                    results.add(new BatchResult(feedbackId, false, "标记为已读失败"));
                }

            } catch (Exception e) {
                results.add(new BatchResult(feedbackId, false, "操作失败: " + e.getMessage()));
            }
        }

        return results;
    }

    private List<BatchResult> batchMarkAsResolved(BatchActionRequest request, String userId) {
        List<BatchResult> results = new ArrayList<>();

        for (String feedbackId : request.getFeedbackIds()) {
            try {
                // 检查反馈是否存在
                String checkSql = "SELECT feedback_id, status FROM user_feedback WHERE feedback_id = ?";
                List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

                if (feedbackList.isEmpty()) {
                    results.add(new BatchResult(feedbackId, false, "反馈不存在"));
                    continue;
                }

                Map<String, Object> feedback = feedbackList.get(0);
                String currentStatus = (String) feedback.get("status");

                // 检查是否已经是已处理状态
                List<String> resolvedStatuses = List.of("completed", "rejected", "duplicate");
                if (resolvedStatuses.contains(currentStatus)) {
                    results.add(new BatchResult(feedbackId, true, "反馈已经是已处理状态"));
                    continue;
                }

                // 更新反馈状态为completed（已处理）
                String updateSql = "UPDATE user_feedback SET status = 'completed', completed_at = NOW(), updated_at = NOW() WHERE feedback_id = ?";
                int rowsAffected = jdbcTemplate.update(updateSql, feedbackId);

                if (rowsAffected > 0) {
                    // 记录变更日志
                    String logSql = "INSERT INTO feedback_change_log (feedback_id, field, old_value, new_value, changed_by, reason, created_at) " +
                            "VALUES (?, 'status', ?, 'completed', ?, '批量标记为已处理', NOW())";

                    jdbcTemplate.update(logSql,
                            feedbackId,
                            currentStatus,
                            userId
                    );

                    results.add(new BatchResult(feedbackId, true, "标记为已处理成功"));
                } else {
                    results.add(new BatchResult(feedbackId, false, "标记为已处理失败"));
                }

            } catch (Exception e) {
                results.add(new BatchResult(feedbackId, false, "操作失败: " + e.getMessage()));
            }
        }

        return results;
    }
}