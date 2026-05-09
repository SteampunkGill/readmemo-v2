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
public class FeedbackReply {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到回复反馈请求 ===");
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
    public static class ReplyRequest {
        private String feedbackId;
        private String message;
        private Boolean isInternal;
        private List<Map<String, Object>> attachments;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Boolean getIsInternal() { return isInternal; }
        public void setIsInternal(Boolean isInternal) { this.isInternal = isInternal; }

        public List<Map<String, Object>> getAttachments() { return attachments; }
        public void setAttachments(List<Map<String, Object>> attachments) { this.attachments = attachments; }
    }

    // 响应DTO
    public static class ReplyResponse {
        private boolean success;
        private String message;
        private ReplyData data;

        public ReplyResponse(boolean success, String message, ReplyData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReplyData getData() { return data; }
        public void setData(ReplyData data) { this.data = data; }
    }

    public static class ReplyData {
        private String id;
        private String feedbackId;
        private String message;
        private String userId;
        private String userName;
        private String userAvatar;
        private boolean isInternal;
        private List<Map<String, Object>> attachments;
        private String createdAt;

        public ReplyData(String id, String feedbackId, String message, String userId, String userName,
                         String userAvatar, boolean isInternal, List<Map<String, Object>> attachments, String createdAt) {
            this.id = id;
            this.feedbackId = feedbackId;
            this.message = message;
            this.userId = userId;
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.isInternal = isInternal;
            this.attachments = attachments;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getUserAvatar() { return userAvatar; }
        public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

        public boolean isInternal() { return isInternal; }
        public void setInternal(boolean isInternal) { this.isInternal = isInternal; }

        public List<Map<String, Object>> getAttachments() { return attachments; }
        public void setAttachments(List<Map<String, Object>> attachments) { this.attachments = attachments; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    @PostMapping("/{feedbackId}/reply")
    public ResponseEntity<ReplyResponse> replyToFeedback(
            @PathVariable String feedbackId,
            @RequestBody ReplyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 设置feedbackId
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ReplyResponse(false, "反馈ID不能为空", null)
                );
            }

            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ReplyResponse(false, "回复内容不能为空", null)
                );
            }

            // 2. 验证用户身份
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReplyResponse(false, "用户未登录", null)
                );
            }

            // 3. 检查反馈是否存在
            String checkSql = "SELECT feedback_id FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ReplyResponse(false, "未找到指定的反馈", null)
                );
            }

            // 4. 获取用户信息
            String userSql = "SELECT username, avatar_url FROM users WHERE user_id = ?";
            List<Map<String, Object>> userList = jdbcTemplate.queryForList(userSql, userId);

            if (userList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReplyResponse(false, "用户不存在", null)
                );
            }

            Map<String, Object> user = userList.get(0);
            String userName = (String) user.get("username");
            String userAvatar = (String) user.get("avatar_url");

            // 5. 处理attachments
            String attachmentsJson = "[]";
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                attachmentsJson = objectMapper.writeValueAsString(request.getAttachments());
            }

            // 6. 生成回复ID
            String replyId = "reply_" + UUID.randomUUID().toString().substring(0, 8);

            // 7. 插入回复记录
            String insertSql = "INSERT INTO feedback_replies (reply_id, feedback_id, user_id, message, is_internal, attachments, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

            boolean isInternal = request.getIsInternal() != null ? request.getIsInternal() : false;

            int rowsAffected = jdbcTemplate.update(insertSql,
                    replyId,
                    feedbackId,
                    userId,
                    request.getMessage(),
                    isInternal,
                    attachmentsJson
            );

            printQueryResult("插入回复行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new ReplyResponse(false, "回复失败", null)
                );
            }

            // 8. 更新反馈的评论数量
            String updateCommentCountSql = "UPDATE user_feedback SET comment_count = comment_count + 1, updated_at = NOW() WHERE feedback_id = ?";
            jdbcTemplate.update(updateCommentCountSql, feedbackId);

            // 9. 查询刚插入的回复
            String selectSql = "SELECT * FROM feedback_replies WHERE reply_id = ?";
            List<Map<String, Object>> replyList = jdbcTemplate.queryForList(selectSql, replyId);
            printQueryResult("回复详情: " + replyList);

            if (replyList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new ReplyResponse(false, "无法获取回复信息", null)
                );
            }

            Map<String, Object> reply = replyList.get(0);

            // 10. 准备响应数据
            List<Map<String, Object>> responseAttachments = request.getAttachments();
            if (responseAttachments == null) {
                responseAttachments = new ArrayList<>();
            }

            ReplyData replyData = new ReplyData(
                    replyId,
                    feedbackId,
                    request.getMessage(),
                    userId,
                    userName,
                    userAvatar,
                    isInternal,
                    responseAttachments,
                    reply.get("created_at").toString()
            );

            ReplyResponse response = new ReplyResponse(true, "回复成功", replyData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("回复反馈过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReplyResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}