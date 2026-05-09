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
public class FeedbackUpdate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新反馈请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
    public static class UpdateRequest {
        private String feedbackId;
        private String title;
        private String content;
        private String type;
        private String priority;
        private List<Map<String, Object>> attachments;
        private Map<String, Object> metadata;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public List<Map<String, Object>> getAttachments() { return attachments; }
        public void setAttachments(List<Map<String, Object>> attachments) { this.attachments = attachments; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    // 响应DTO
    public static class UpdateResponse {
        private boolean success;
        private String message;
        private FeedbackData data;

        public UpdateResponse(boolean success, String message, FeedbackData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public FeedbackData getData() { return data; }
        public void setData(FeedbackData data) { this.data = data; }
    }

    public static class FeedbackData {
        private String id;
        private String type;
        private String title;
        private String content;
        private String status;
        private String priority;
        private int upvotes;
        private int downvotes;
        private String userId;
        private String userName;
        private List<Map<String, Object>> attachments;
        private Map<String, Object> metadata;
        private String createdAt;
        private String updatedAt;
        private String assignedTo;
        private String assignedAt;
        private String completedAt;

        public FeedbackData(String id, String type, String title, String content, String status,
                            String priority, int upvotes, int downvotes, String userId, String userName,
                            List<Map<String, Object>> attachments, Map<String, Object> metadata,
                            String createdAt, String updatedAt, String assignedTo, String assignedAt,
                            String completedAt) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.content = content;
            this.status = status;
            this.priority = priority;
            this.upvotes = upvotes;
            this.downvotes = downvotes;
            this.userId = userId;
            this.userName = userName;
            this.attachments = attachments;
            this.metadata = metadata;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.assignedTo = assignedTo;
            this.assignedAt = assignedAt;
            this.completedAt = completedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public int getUpvotes() { return upvotes; }
        public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

        public int getDownvotes() { return downvotes; }
        public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public List<Map<String, Object>> getAttachments() { return attachments; }
        public void setAttachments(List<Map<String, Object>> attachments) { this.attachments = attachments; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

        public String getAssignedAt() { return assignedAt; }
        public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }

        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    }

    @PutMapping("/{feedbackId}")
    public ResponseEntity<UpdateResponse> updateFeedback(
            @PathVariable String feedbackId,
            @RequestBody UpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 设置feedbackId
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateResponse(false, "反馈ID不能为空", null)
                );
            }

            // 2. 验证用户身份
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateResponse(false, "用户未登录", null)
                );
            }

            // 3. 检查反馈是否存在
            String checkSql = "SELECT f.*, u.username as user_name FROM user_feedback f " +
                    "LEFT JOIN users u ON f.user_id = u.user_id " +
                    "WHERE f.feedback_id = ?";

            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);
            printQueryResult("原始反馈信息: " + feedbackList);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateResponse(false, "未找到指定的反馈", null)
                );
            }

            Map<String, Object> originalFeedback = feedbackList.get(0);
            String feedbackUserId = (String) originalFeedback.get("user_id");

            // 4. 检查权限（只有反馈创建者或管理员可以更新）
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
                        new UpdateResponse(false, "没有权限更新此反馈", null)
                );
            }

            // 5. 获取更新后的值
            String updateTitle = request.getTitle() != null ? request.getTitle() : (String) originalFeedback.get("title");
            String updateContent = request.getContent() != null ? request.getContent() : (String) originalFeedback.get("content");
            String updateType = request.getType() != null ? request.getType() : (String) originalFeedback.get("type");
            String updatePriority = request.getPriority() != null ? request.getPriority() : (String) originalFeedback.get("priority");

            // 验证type和priority的合法性
            List<String> validTypes = List.of("bug", "feature", "improvement", "question", "other");
            if (!validTypes.contains(updateType)) {
                updateType = "other";
            }

            List<String> validPriorities = List.of("low", "medium", "high", "critical");
            if (!validPriorities.contains(updatePriority)) {
                updatePriority = "medium";
            }

            // 6. 处理attachments
            String attachmentsJson = (String) originalFeedback.get("attachments");
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                attachmentsJson = objectMapper.writeValueAsString(request.getAttachments());
            }

            // 7. 处理metadata
            String metadataJson = (String) originalFeedback.get("metadata");
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            }

            // 8. 更新反馈记录
            String updateSql = "UPDATE user_feedback SET title = ?, content = ?, type = ?, priority = ?, " +
                    "attachments = ?, metadata = ?, updated_at = NOW() WHERE feedback_id = ?";

            int rowsAffected = jdbcTemplate.update(updateSql,
                    updateTitle,
                    updateContent,
                    updateType,
                    updatePriority,
                    attachmentsJson,
                    metadataJson,
                    feedbackId
            );

            printQueryResult("更新行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateResponse(false, "更新反馈失败", null)
                );
            }

            // 9. 记录变更日志（如果字段有变化）
            recordFieldChange(feedbackId, userId, "title",
                    (String) originalFeedback.get("title"), updateTitle, "更新标题");

            recordFieldChange(feedbackId, userId, "content",
                    (String) originalFeedback.get("content"), updateContent, "更新内容");

            recordFieldChange(feedbackId, userId, "type",
                    (String) originalFeedback.get("type"), updateType, "更新类型");

            recordFieldChange(feedbackId, userId, "priority",
                    (String) originalFeedback.get("priority"), updatePriority, "更新优先级");

            // 10. 查询更新后的反馈
            String selectSql = "SELECT f.*, u.username as user_name FROM user_feedback f " +
                    "LEFT JOIN users u ON f.user_id = u.user_id " +
                    "WHERE f.feedback_id = ?";

            List<Map<String, Object>> updatedFeedbackList = jdbcTemplate.queryForList(selectSql, feedbackId);
            printQueryResult("更新后的反馈信息: " + updatedFeedbackList);

            if (updatedFeedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateResponse(false, "无法获取更新后的反馈信息", null)
                );
            }

            Map<String, Object> updatedFeedback = updatedFeedbackList.get(0);

            // 11. 准备响应数据
            List<Map<String, Object>> responseAttachments = request.getAttachments();
            if (responseAttachments == null) {
                responseAttachments = new ArrayList<>();
                // 尝试从JSON解析
                String attJson = (String) updatedFeedback.get("attachments");
                if (attJson != null && !attJson.isEmpty() && !"[]".equals(attJson)) {
                    try {
                        responseAttachments = objectMapper.readValue(
                                attJson, new TypeReference<List<Map<String, Object>>>() {});
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }

            Map<String, Object> responseMetadata = request.getMetadata();
            if (responseMetadata == null) {
                responseMetadata = new HashMap<>();
                // 尝试从JSON解析
                String metaJson = (String) updatedFeedback.get("metadata");
                if (metaJson != null && !metaJson.isEmpty() && !"{}".equals(metaJson)) {
                    try {
                        responseMetadata = objectMapper.readValue(
                                metaJson, new TypeReference<Map<String, Object>>() {});
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }

            FeedbackData feedbackData = new FeedbackData(
                    feedbackId,
                    updateType,
                    updateTitle,
                    updateContent,
                    (String) updatedFeedback.get("status"),
                    updatePriority,
                    ((Number) updatedFeedback.get("upvotes")).intValue(),
                    ((Number) updatedFeedback.get("downvotes")).intValue(),
                    (String) updatedFeedback.get("user_id"),
                    (String) updatedFeedback.get("user_name"),
                    responseAttachments,
                    responseMetadata,
                    updatedFeedback.get("created_at").toString(),
                    updatedFeedback.get("updated_at").toString(),
                    (String) updatedFeedback.get("assigned_to"),
                    updatedFeedback.get("assigned_at") != null ? updatedFeedback.get("assigned_at").toString() : null,
                    updatedFeedback.get("completed_at") != null ? updatedFeedback.get("completed_at").toString() : null
            );

            UpdateResponse response = new UpdateResponse(
                    true, "更新反馈成功", feedbackData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新反馈过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private void recordFieldChange(String feedbackId, String userId, String field,
                                   String oldValue, String newValue, String reason) {
        try {
            if (oldValue == null) oldValue = "";
            if (newValue == null) newValue = "";

            // 只有当值发生变化时才记录
            if (!oldValue.equals(newValue)) {
                String logSql = "INSERT INTO feedback_change_log (feedback_id, field, old_value, new_value, " +
                        "changed_by, reason, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";

                jdbcTemplate.update(logSql,
                        feedbackId,
                        field,
                        oldValue,
                        newValue,
                        userId,
                        reason
                );
            }
        } catch (Exception e) {
            System.err.println("记录字段变更失败: " + e.getMessage());
        }
    }
}