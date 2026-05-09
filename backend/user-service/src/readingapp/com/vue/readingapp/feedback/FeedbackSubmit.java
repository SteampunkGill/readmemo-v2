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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackSubmit {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到提交反馈请求 ===");
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
    public static class SubmitRequest {
        private String title;
        private String content;
        private String type;
        private String priority;
        private List<Attachment> attachments;
        private Map<String, Object> metadata;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public List<Attachment> getAttachments() { return attachments; }
        public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        @Override
        public String toString() {
            return "SubmitRequest{" +
                    "title='" + title + '\'' +
                    ", content='" + content + '\'' +
                    ", type='" + type + '\'' +
                    ", priority='" + priority + '\'' +
                    '}';
        }
    }

    public static class Attachment {
        private String name;
        private String url;
        private long size;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
    }

    // 响应DTO
    public static class SubmitResponse {
        private boolean success;
        private String message;
        private FeedbackData data;

        public SubmitResponse(boolean success, String message, FeedbackData data) {
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

        @Override
        public String toString() {
            return "SubmitResponse{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
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
        private List<Attachment> attachments;
        private Map<String, Object> metadata;
        private String createdAt;

        public FeedbackData(String id, String type, String title, String content, String status,
                            String priority, int upvotes, int downvotes, String userId, String userName,
                            List<Attachment> attachments, Map<String, Object> metadata, String createdAt) {
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

        public List<Attachment> getAttachments() { return attachments; }
        public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmitResponse> submitFeedback(
            @RequestBody SubmitRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证用户身份
            String actualUserId = null;
            String userName = "匿名用户";

            if (userId != null && !userId.isEmpty()) {
                String userSql = "SELECT user_id, username FROM users WHERE user_id = ?";
                List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

                if (!users.isEmpty()) {
                    Map<String, Object> user = users.get(0);
                    actualUserId = String.valueOf(user.get("user_id"));
                    userName = (String) user.get("username");
                }
            }

            if (actualUserId == null) {
                // 如果没有获取到用户ID，尝试获取数据库中第一个用户作为兜底（课设简化处理）
                try {
                    List<Map<String, Object>> firstUser = jdbcTemplate.queryForList("SELECT user_id, username FROM users LIMIT 1");
                    if (!firstUser.isEmpty()) {
                        actualUserId = String.valueOf(firstUser.get(0).get("user_id"));
                        userName = (String) firstUser.get(0).get("username");
                    } else {
                        // 实在没有用户，返回错误
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                new SubmitResponse(false, "请先登录后再提交反馈", null)
                        );
                    }
                } catch (Exception e) {
                    actualUserId = "1"; // 最后的保底
                }
            }

            // 2. 验证请求数据
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SubmitResponse(false, "反馈标题不能为空", null)
                );
            }

            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SubmitResponse(false, "反馈内容不能为空", null)
                );
            }

            // 3. 验证type和priority的合法性
            String type = request.getType();
            if (type == null) {
                type = "other";
            } else {
                List<String> validTypes = List.of("bug", "feature", "improvement", "question", "other");
                if (!validTypes.contains(type)) {
                    type = "other";
                }
            }

            String priority = request.getPriority();
            if (priority == null) {
                priority = "medium";
            } else {
                List<String> validPriorities = List.of("low", "medium", "high", "critical");
                if (!validPriorities.contains(priority)) {
                    priority = "medium";
                }
            }

            // 4. 处理attachments和metadata
            String attachmentsJson = "[]";
            if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                attachmentsJson = objectMapper.writeValueAsString(request.getAttachments());
            }

            String metadataJson = "{}";
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            }

            // 5. 生成反馈ID
            String feedbackId = "fb_" + UUID.randomUUID().toString().substring(0, 8);

            // 6. 插入数据库
            String insertSql = "INSERT INTO user_feedback (feedback_id, user_id, title, content, type, status, priority, " +
                    "upvotes, downvotes, view_count, comment_count, attachments, metadata, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, 'pending', ?, 0, 0, 0, 0, ?, ?, NOW(), NOW())";

            int rowsAffected = jdbcTemplate.update(insertSql,
                    feedbackId,
                    Integer.parseInt(actualUserId),
                    request.getTitle(),
                    request.getContent(),
                    type,
                    priority,
                    attachmentsJson,
                    metadataJson
            );

            printQueryResult("插入行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new SubmitResponse(false, "提交反馈失败", null)
                );
            }

            // 7. 查询刚插入的反馈
            String selectSql = "SELECT * FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(selectSql, feedbackId);
            printQueryResult(feedbackList);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new SubmitResponse(false, "无法获取提交的反馈信息", null)
                );
            }

            Map<String, Object> feedback = feedbackList.get(0);

            // 8. 准备响应数据
            List<Attachment> responseAttachments = request.getAttachments();
            Map<String, Object> responseMetadata = request.getMetadata();

            FeedbackData feedbackData = new FeedbackData(
                    feedbackId,
                    type,
                    request.getTitle(),
                    request.getContent(),
                    "pending",
                    priority,
                    0,
                    0,
                    actualUserId,
                    userName,
                    responseAttachments,
                    responseMetadata,
                    feedback.get("created_at").toString()
            );

            SubmitResponse response = new SubmitResponse(true, "反馈提交成功", feedbackData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("提交反馈过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SubmitResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}