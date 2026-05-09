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
public class FeedbackGetById {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取反馈详情请求 ===");
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
    public static class GetByIdRequest {
        private String feedbackId;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    }

    // 响应DTO
    public static class GetByIdResponse {
        private boolean success;
        private String message;
        private FeedbackDetailData data;

        public GetByIdResponse(boolean success, String message, FeedbackDetailData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public FeedbackDetailData getData() { return data; }
        public void setData(FeedbackDetailData data) { this.data = data; }
    }

    public static class FeedbackDetailData {
        private String id;
        private String type;
        private String title;
        private String content;
        private String status;
        private String priority;
        private int upvotes;
        private int downvotes;
        private List<String> upvotedBy;
        private List<String> downvotedBy;
        private String userId;
        private String userName;
        private String userEmail;
        private List<Attachment> attachments;
        private List<Comment> comments;
        private Map<String, Object> metadata;
        private int viewCount;
        private int commentCount;
        private List<String> relatedFeedback;
        private List<ChangeLog> changeLog;
        private String estimatedCompletion;
        private String createdAt;
        private String updatedAt;
        private String assignedTo;
        private String assignedAt;
        private String completedAt;

        // Getters and Setters
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

        public List<String> getUpvotedBy() { return upvotedBy; }
        public void setUpvotedBy(List<String> upvotedBy) { this.upvotedBy = upvotedBy; }

        public List<String> getDownvotedBy() { return downvotedBy; }
        public void setDownvotedBy(List<String> downvotedBy) { this.downvotedBy = downvotedBy; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

        public List<Attachment> getAttachments() { return attachments; }
        public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }

        public List<Comment> getComments() { return comments; }
        public void setComments(List<Comment> comments) { this.comments = comments; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        public int getViewCount() { return viewCount; }
        public void setViewCount(int viewCount) { this.viewCount = viewCount; }

        public int getCommentCount() { return commentCount; }
        public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

        public List<String> getRelatedFeedback() { return relatedFeedback; }
        public void setRelatedFeedback(List<String> relatedFeedback) { this.relatedFeedback = relatedFeedback; }

        public List<ChangeLog> getChangeLog() { return changeLog; }
        public void setChangeLog(List<ChangeLog> changeLog) { this.changeLog = changeLog; }

        public String getEstimatedCompletion() { return estimatedCompletion; }
        public void setEstimatedCompletion(String estimatedCompletion) { this.estimatedCompletion = estimatedCompletion; }

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

    public static class Attachment {
        private String id;
        private String name;
        private String url;
        private long size;
        private String type;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class Comment {
        private String id;
        private String content;
        private String userId;
        private String userName;
        private String userAvatar;
        private boolean isInternal;
        private List<Attachment> attachments;
        private String createdAt;
        private String updatedAt;
        private boolean edited;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

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

        public List<Attachment> getAttachments() { return attachments; }
        public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public boolean isEdited() { return edited; }
        public void setEdited(boolean edited) { this.edited = edited; }
    }

    public static class ChangeLog {
        private String id;
        private String field;
        private String oldValue;
        private String newValue;
        private String changedBy;
        private String changedByName;
        private String changedAt;
        private String reason;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }

        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }

        public String getChangedBy() { return changedBy; }
        public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

        public String getChangedByName() { return changedByName; }
        public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

        public String getChangedAt() { return changedAt; }
        public void setChangedAt(String changedAt) { this.changedAt = changedAt; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    @GetMapping("/{feedbackId}")
    public ResponseEntity<GetByIdResponse> getFeedbackById(
            @PathVariable String feedbackId) {

        // 创建请求对象用于打印
        GetByIdRequest request = new GetByIdRequest();
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new GetByIdResponse(false, "反馈ID不能为空", null)
                );
            }

            // 2. 查询反馈基本信息
            String feedbackSql = "SELECT f.*, u.username as user_name, u.email as user_email " +
                    "FROM user_feedback f " +
                    "LEFT JOIN users u ON f.user_id = u.user_id " +
                    "WHERE f.feedback_id = ?";

            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(feedbackSql, feedbackId);
            printQueryResult("反馈基本信息: " + feedbackList);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetByIdResponse(false, "未找到指定的反馈", null)
                );
            }

            Map<String, Object> feedback = feedbackList.get(0);

            // 3. 更新查看次数
            String updateViewSql = "UPDATE user_feedback SET view_count = view_count + 1 WHERE feedback_id = ?";
            jdbcTemplate.update(updateViewSql, feedbackId);

            // 4. 查询投票信息
            List<String> upvotedBy = getVotedUsers(feedbackId, "upvote");
            List<String> downvotedBy = getVotedUsers(feedbackId, "downvote");

            // 5. 查询回复/评论
            List<Comment> comments = getFeedbackComments(feedbackId);

            // 6. 查询变更日志
            List<ChangeLog> changeLog = getFeedbackChangeLog(feedbackId);

            // 7. 处理attachments JSON
            List<Attachment> attachments = new ArrayList<>();
            String attachmentsJson = (String) feedback.get("attachments");
            if (attachmentsJson != null && !attachmentsJson.isEmpty() && !"[]".equals(attachmentsJson)) {
                try {
                    List<Map<String, Object>> attachmentList = objectMapper.readValue(
                            attachmentsJson, new TypeReference<List<Map<String, Object>>>() {});

                    for (Map<String, Object> att : attachmentList) {
                        Attachment attachment = new Attachment();
                        attachment.setId("att_" + UUID.randomUUID().toString().substring(0, 8));
                        attachment.setName((String) att.get("name"));
                        attachment.setUrl((String) att.get("url"));
                        attachment.setSize(((Number) att.get("size")).longValue());

                        // 根据文件名猜测类型
                        String name = attachment.getName().toLowerCase();
                        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")) {
                            attachment.setType("image");
                        } else if (name.endsWith(".pdf")) {
                            attachment.setType("pdf");
                        } else if (name.endsWith(".doc") || name.endsWith(".docx")) {
                            attachment.setType("document");
                        } else {
                            attachment.setType("file");
                        }

                        attachments.add(attachment);
                    }
                } catch (Exception e) {
                    System.err.println("解析attachments失败: " + e.getMessage());
                }
            }

            // 8. 处理metadata JSON
            Map<String, Object> metadata = new HashMap<>();
            String metadataJson = (String) feedback.get("metadata");
            if (metadataJson != null && !metadataJson.isEmpty() && !"{}".equals(metadataJson)) {
                try {
                    metadata = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    System.err.println("解析metadata失败: " + e.getMessage());
                }
            }

            // 9. 构建响应数据
            FeedbackDetailData detailData = new FeedbackDetailData();
            detailData.setId(feedbackId);
            detailData.setType((String) feedback.get("type"));
            detailData.setTitle((String) feedback.get("title"));
            detailData.setContent((String) feedback.get("content"));
            detailData.setStatus((String) feedback.get("status"));
            detailData.setPriority((String) feedback.get("priority"));
            detailData.setUpvotes(((Number) feedback.get("upvotes")).intValue());
            detailData.setDownvotes(((Number) feedback.get("downvotes")).intValue());
            detailData.setUpvotedBy(upvotedBy);
            detailData.setDownvotedBy(downvotedBy);
            detailData.setUserId((String) feedback.get("user_id"));
            detailData.setUserName((String) feedback.get("user_name"));
            detailData.setUserEmail((String) feedback.get("user_email"));
            detailData.setAttachments(attachments);
            detailData.setComments(comments);
            detailData.setMetadata(metadata);
            detailData.setViewCount(((Number) feedback.get("view_count")).intValue());
            detailData.setCommentCount(comments.size());
            detailData.setRelatedFeedback(new ArrayList<>()); // 简化处理，实际需要查询相关反馈
            detailData.setChangeLog(changeLog);
            detailData.setEstimatedCompletion(feedback.get("estimated_completion") != null ?
                    feedback.get("estimated_completion").toString() : null);
            detailData.setCreatedAt(feedback.get("created_at").toString());
            detailData.setUpdatedAt(feedback.get("updated_at").toString());
            detailData.setAssignedTo((String) feedback.get("assigned_to"));
            detailData.setAssignedAt(feedback.get("assigned_at") != null ?
                    feedback.get("assigned_at").toString() : null);
            detailData.setCompletedAt(feedback.get("completed_at") != null ?
                    feedback.get("completed_at").toString() : null);

            GetByIdResponse response = new GetByIdResponse(true, "获取反馈详情成功", detailData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取反馈详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetByIdResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private List<String> getVotedUsers(String feedbackId, String voteType) {
        List<String> users = new ArrayList<>();
        try {
            String sql = "SELECT user_id FROM feedback_votes WHERE feedback_id = ? AND vote_type = ?";
            List<Map<String, Object>> votes = jdbcTemplate.queryForList(sql, feedbackId, voteType);

            for (Map<String, Object> vote : votes) {
                users.add((String) vote.get("user_id"));
            }
        } catch (Exception e) {
            System.err.println("获取投票用户失败: " + e.getMessage());
        }
        return users;
    }

    private List<Comment> getFeedbackComments(String feedbackId) {
        List<Comment> comments = new ArrayList<>();
        try {
            String sql = "SELECT r.*, u.username as user_name, u.avatar_url as user_avatar " +
                    "FROM feedback_replies r " +
                    "LEFT JOIN users u ON r.user_id = u.user_id " +
                    "WHERE r.feedback_id = ? " +
                    "ORDER BY r.created_at ASC";

            List<Map<String, Object>> replyList = jdbcTemplate.queryForList(sql, feedbackId);

            for (Map<String, Object> reply : replyList) {
                Comment comment = new Comment();
                comment.setId("cmt_" + reply.get("reply_id"));
                comment.setContent((String) reply.get("message"));
                comment.setUserId((String) reply.get("user_id"));
                comment.setUserName((String) reply.get("user_name"));
                comment.setUserAvatar((String) reply.get("user_avatar"));
                comment.setInternal((Boolean) reply.get("is_internal"));
                comment.setCreatedAt(reply.get("created_at").toString());
                comment.setUpdatedAt(reply.get("updated_at").toString());
                comment.setEdited(!reply.get("created_at").equals(reply.get("updated_at")));
                comment.setAttachments(new ArrayList<>()); // 简化处理

                comments.add(comment);
            }
        } catch (Exception e) {
            System.err.println("获取评论失败: " + e.getMessage());
        }
        return comments;
    }

    private List<ChangeLog> getFeedbackChangeLog(String feedbackId) {
        List<ChangeLog> changeLogs = new ArrayList<>();
        try {
            String sql = "SELECT l.*, u.username as changed_by_name " +
                    "FROM feedback_change_log l " +
                    "LEFT JOIN users u ON l.changed_by = u.user_id " +
                    "WHERE l.feedback_id = ? " +
                    "ORDER BY l.created_at ASC";

            List<Map<String, Object>> logList = jdbcTemplate.queryForList(sql, feedbackId);

            for (Map<String, Object> log : logList) {
                ChangeLog changeLog = new ChangeLog();
                changeLog.setId("log_" + log.get("log_id"));
                changeLog.setField((String) log.get("field"));
                changeLog.setOldValue((String) log.get("old_value"));
                changeLog.setNewValue((String) log.get("new_value"));
                changeLog.setChangedBy((String) log.get("changed_by"));
                changeLog.setChangedByName((String) log.get("changed_by_name"));
                changeLog.setChangedAt(log.get("created_at").toString());
                changeLog.setReason((String) log.get("reason"));

                changeLogs.add(changeLog);
            }
        } catch (Exception e) {
            System.err.println("获取变更日志失败: " + e.getMessage());
        }
        return changeLogs;
    }
}