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
public class FeedbackGetReplies {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取反馈回复请求 ===");
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
    public static class GetRepliesRequest {
        private String feedbackId;
        private Integer page;
        private Integer pageSize;
        private Boolean includeInternal;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public Boolean getIncludeInternal() { return includeInternal; }
        public void setIncludeInternal(Boolean includeInternal) { this.includeInternal = includeInternal; }
    }

    // 响应DTO
    public static class GetRepliesResponse {
        private boolean success;
        private String message;
        private RepliesData data;

        public GetRepliesResponse(boolean success, String message, RepliesData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public RepliesData getData() { return data; }
        public void setData(RepliesData data) { this.data = data; }
    }

    public static class RepliesData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<ReplyDetail> replies;

        public RepliesData(int total, int page, int pageSize, int totalPages, List<ReplyDetail> replies) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.replies = replies;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public List<ReplyDetail> getReplies() { return replies; }
        public void setReplies(List<ReplyDetail> replies) { this.replies = replies; }
    }

    public static class ReplyDetail {
        private String id;
        private String feedbackId;
        private String message;
        private String userId;
        private String userName;
        private String userAvatar;
        private boolean isInternal;
        private List<Map<String, Object>> attachments;
        private String createdAt;
        private String updatedAt;
        private boolean edited;

        // Getters and Setters
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

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public boolean isEdited() { return edited; }
        public void setEdited(boolean edited) { this.edited = edited; }
    }

    @GetMapping("/{feedbackId}/replies")
    public ResponseEntity<GetRepliesResponse> getFeedbackReplies(
            @PathVariable String feedbackId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "false") Boolean includeInternal) {

        // 创建请求对象用于打印
        GetRepliesRequest request = new GetRepliesRequest();
        request.setFeedbackId(feedbackId);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setIncludeInternal(includeInternal);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new GetRepliesResponse(false, "反馈ID不能为空", null)
                );
            }

            // 2. 参数验证和默认值设置
            if (page == null || page < 1) page = 1;
            if (pageSize == null || pageSize < 1) pageSize = 20;
            if (pageSize > 100) pageSize = 100;

            // 3. 检查反馈是否存在
            String checkSql = "SELECT feedback_id FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetRepliesResponse(false, "未找到指定的反馈", null)
                );
            }

            // 4. 构建查询条件
            List<Object> params = new ArrayList<>();
            StringBuilder whereClause = new StringBuilder(" WHERE feedback_id = ? ");
            params.add(feedbackId);

            // 处理是否包含内部回复
            if (!includeInternal) {
                whereClause.append(" AND is_internal = false ");
            }

            // 5. 获取总数
            String countSql = "SELECT COUNT(*) as total FROM feedback_replies " + whereClause.toString();
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
            if (total == null) total = 0;

            // 6. 计算分页
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int offset = (page - 1) * pageSize;

            // 7. 查询回复数据
            String querySql = "SELECT r.*, u.username as user_name, u.avatar_url as user_avatar " +
                    "FROM feedback_replies r " +
                    "LEFT JOIN users u ON r.user_id = u.user_id " +
                    whereClause.toString() +
                    " ORDER BY r.created_at ASC " +
                    " LIMIT ? OFFSET ?";

            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(pageSize);
            queryParams.add(offset);

            List<Map<String, Object>> replyList = jdbcTemplate.queryForList(querySql, queryParams.toArray());
            printQueryResult("查询到 " + replyList.size() + " 条回复");

            // 8. 转换数据
            List<ReplyDetail> replies = new ArrayList<>();
            for (Map<String, Object> row : replyList) {
                ReplyDetail reply = new ReplyDetail();
                reply.setId((String) row.get("reply_id"));
                reply.setFeedbackId((String) row.get("feedback_id"));
                reply.setMessage((String) row.get("message"));
                reply.setUserId((String) row.get("user_id"));
                reply.setUserName((String) row.get("user_name"));
                reply.setUserAvatar((String) row.get("user_avatar"));
                reply.setInternal((Boolean) row.get("is_internal"));
                reply.setCreatedAt(row.get("created_at").toString());
                reply.setUpdatedAt(row.get("updated_at").toString());
                reply.setEdited(!row.get("created_at").equals(row.get("updated_at")));

                // 处理attachments JSON
                String attachmentsJson = (String) row.get("attachments");
                if (attachmentsJson != null && !attachmentsJson.isEmpty() && !"[]".equals(attachmentsJson)) {
                    try {
                        List<Map<String, Object>> attachments = objectMapper.readValue(
                                attachmentsJson, new TypeReference<List<Map<String, Object>>>() {});
                        reply.setAttachments(attachments);
                    } catch (Exception e) {
                        reply.setAttachments(new ArrayList<>());
                    }
                } else {
                    reply.setAttachments(new ArrayList<>());
                }

                replies.add(reply);
            }

            // 9. 准备响应数据
            RepliesData repliesData = new RepliesData(
                    total, page, pageSize, totalPages, replies
            );

            GetRepliesResponse response = new GetRepliesResponse(
                    true, "获取反馈回复成功", repliesData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取反馈回复过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetRepliesResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}