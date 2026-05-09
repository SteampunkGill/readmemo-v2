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
@RequestMapping("/api/v1//feedback")
public class FeedbackAddComment {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到添加评论请求 ===");
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
    public static class AddCommentRequest {
        private String feedbackId;
        private String content;
        private Boolean isInternal;
        private List<Map<String, Object>> attachments;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Boolean getIsInternal() { return isInternal; }
        public void setIsInternal(Boolean isInternal) { this.isInternal = isInternal; }

        public List<Map<String, Object>> getAttachments() { return attachments; }
        public void setAttachments(List<Map<String, Object>> attachments) { this.attachments = attachments; }
    }

    // 响应DTO
    public static class AddCommentResponse {
        private boolean success;
        private String message;
        private CommentData data;

        public AddCommentResponse(boolean success, String message, CommentData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public CommentData getData() { return data; }
        public void setData(CommentData data) { this.data = data; }
    }

    public static class CommentData {
        private String id;
        private String feedbackId;
        private String content;
        private String userId;
        private String userName;
        private String userAvatar;
        private boolean isInternal;
        private List<Map<String, Object>> attachments;
        private String createdAt;
        private String updatedAt;
        private boolean edited;

        public CommentData(String id, String feedbackId, String content, String userId, String userName,
                           String userAvatar, boolean isInternal, List<Map<String, Object>> attachments,
                           String createdAt, String updatedAt, boolean edited) {
            this.id = id;
            this.feedbackId = feedbackId;
            this.content = content;
            this.userId = userId;
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.isInternal = isInternal;
            this.attachments = attachments;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.edited = edited;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

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

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public boolean isEdited() { return edited; }
        public void setEdited(boolean edited) { this.edited = edited; }
    }

    @PostMapping("/{feedbackId}/comments")
    public ResponseEntity<AddCommentResponse> addCommentToFeedback(
            @PathVariable String feedbackId,
            @RequestBody AddCommentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 设置feedbackId
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new AddCommentResponse(false, "反馈ID不能为空", null)
                );
            }

            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new AddCommentResponse(false, "评论内容不能为空", null)
                );
            }

            // 2. 验证用户身份
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new AddCommentResponse(false, "用户未登录", null)
                );
            }

            // 3. 检查反馈是否存在
            String checkSql = "SELECT feedback_id FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new AddCommentResponse(false, "未找到指定的反馈", null)
                );
            }

            // 4. 获取用户信息
            String userSql = "SELECT username, avatar_url FROM users WHERE user_id = ?";
            List<Map<String, Object>> userList = jdbcTemplate.queryForList(userSql, userId);

            if (userList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new AddCommentResponse(false, "用户不存在", null)
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

            // 6. 生成评论ID
            String commentId = "cmt_" + UUID.randomUUID().toString().substring(0, 8);

            // 7. 插入评论记录（使用feedback_replies表）
            String insertSql = "INSERT INTO feedback_replies (reply_id, feedback_id, user_id, message, is_internal, attachments, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

            boolean isInternal = request.getIsInternal() != null ? request.getIsInternal() : false;

            int rowsAffected = jdbcTemplate.update(insertSql,
                    commentId,
                    feedbackId,
                    userId,
                    request.getContent(),
                    isInternal,
                    attachmentsJson
            );

            printQueryResult("插入评论行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new AddCommentResponse(false, "添加评论失败", null)
                );
            }

            // 8. 更新反馈的评论数量
            String updateCommentCountSql = "UPDATE user_feedback SET comment_count = comment_count + 1, updated_at = NOW() WHERE feedback_id = ?";
            jdbcTemplate.update(updateCommentCountSql, feedbackId);

            // 9. 查询刚插入的评论
            String selectSql = "SELECT * FROM feedback_replies WHERE reply_id = ?";
            List<Map<String, Object>> commentList = jdbcTemplate.queryForList(selectSql, commentId);
            printQueryResult("评论详情: " + commentList);

            if (commentList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new AddCommentResponse(false, "无法获取评论信息", null)
                );
            }

            Map<String, Object> comment = commentList.get(0);

            // 10. 准备响应数据
            List<Map<String, Object>> responseAttachments = request.getAttachments();
            if (responseAttachments == null) {
                responseAttachments = new ArrayList<>();
            }

            CommentData commentData = new CommentData(
                    commentId,
                    feedbackId,
                    request.getContent(),
                    userId,
                    userName,
                    userAvatar,
                    isInternal,
                    responseAttachments,
                    comment.get("created_at").toString(),
                    comment.get("updated_at").toString(),
                    !comment.get("created_at").equals(comment.get("updated_at"))
            );

            AddCommentResponse response = new AddCommentResponse(
                    true, "添加评论成功", commentData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("添加评论过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new AddCommentResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}