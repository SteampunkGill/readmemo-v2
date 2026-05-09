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
public class FeedbackSearch {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到搜索反馈请求 ===");
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
    public static class SearchRequest {
        private String keyword;
        private Integer page;
        private Integer pageSize;
        private String sortBy;
        private String sortOrder;
        private Boolean includeClosed;

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }

        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }

        public Boolean getIncludeClosed() { return includeClosed; }
        public void setIncludeClosed(Boolean includeClosed) { this.includeClosed = includeClosed; }
    }

    // 响应DTO
    public static class SearchResponse {
        private boolean success;
        private String message;
        private SearchData data;

        public SearchResponse(boolean success, String message, SearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SearchData getData() { return data; }
        public void setData(SearchData data) { this.data = data; }
    }

    public static class SearchData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<FeedbackItem> items;
        private Map<String, Object> filters;
        private Map<String, Object> statistics;

        public SearchData(int total, int page, int pageSize, int totalPages,
                          List<FeedbackItem> items, Map<String, Object> filters,
                          Map<String, Object> statistics) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.items = items;
            this.filters = filters;
            this.statistics = statistics;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public List<FeedbackItem> getItems() { return items; }
        public void setItems(List<FeedbackItem> items) { this.items = items; }

        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }

        public Map<String, Object> getStatistics() { return statistics; }
        public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }
    }

    public static class FeedbackItem {
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

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchFeedback(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "false") Boolean includeClosed) {

        // 创建请求对象用于打印
        SearchRequest request = new SearchRequest();
        request.setKeyword(keyword);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);
        request.setIncludeClosed(includeClosed);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 参数验证和默认值设置
            if (page == null || page < 1) page = 1;
            if (pageSize == null || pageSize < 1) pageSize = 20;
            if (pageSize > 100) pageSize = 100;

            // 2. 构建查询条件
            List<Object> params = new ArrayList<>();
            StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");

            // 处理关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword + "%";
                whereClause.append(" AND (title LIKE ? OR content LIKE ?) ");
                params.add(searchKeyword);
                params.add(searchKeyword);
            }

            // 处理是否包含已关闭的反馈
            if (!includeClosed) {
                whereClause.append(" AND status NOT IN ('completed', 'rejected', 'duplicate') ");
            }

            // 3. 获取总数
            String countSql = "SELECT COUNT(*) as total FROM user_feedback " + whereClause.toString();
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
            if (total == null) total = 0;

            // 4. 计算分页
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int offset = (page - 1) * pageSize;

            // 5. 构建排序
            String orderByClause = " ORDER BY ";
            if ("relevance".equals(sortBy) && keyword != null && !keyword.trim().isEmpty()) {
                // 简单相关性排序：标题匹配优先于内容匹配
                orderByClause += "CASE WHEN title LIKE ? THEN 1 ELSE 2 END, ";
                params.add("%" + keyword + "%");
            }

            orderByClause += "created_at ";
            orderByClause += "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

            // 6. 查询数据
            String querySql = "SELECT f.*, u.username as user_name " +
                    "FROM user_feedback f " +
                    "LEFT JOIN users u ON f.user_id = u.user_id " +
                    whereClause.toString() +
                    orderByClause +
                    " LIMIT ? OFFSET ?";

            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(pageSize);
            queryParams.add(offset);

            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(querySql, queryParams.toArray());
            printQueryResult("搜索到 " + feedbackList.size() + " 条记录");

            // 7. 转换数据
            List<FeedbackItem> items = new ArrayList<>();
            for (Map<String, Object> row : feedbackList) {
                FeedbackItem item = new FeedbackItem();
                item.setId((String) row.get("feedback_id"));
                item.setType((String) row.get("type"));
                item.setTitle((String) row.get("title"));
                item.setContent((String) row.get("content"));
                item.setStatus((String) row.get("status"));
                item.setPriority((String) row.get("priority"));
                item.setUpvotes(((Number) row.get("upvotes")).intValue());
                item.setDownvotes(((Number) row.get("downvotes")).intValue());
                item.setUserId((String) row.get("user_id"));
                item.setUserName((String) row.get("user_name"));

                // 处理attachments JSON
                String attachmentsJson = (String) row.get("attachments");
                if (attachmentsJson != null && !attachmentsJson.isEmpty() && !"[]".equals(attachmentsJson)) {
                    try {
                        List<Map<String, Object>> attachments = objectMapper.readValue(
                                attachmentsJson, new TypeReference<List<Map<String, Object>>>() {});
                        item.setAttachments(attachments);
                    } catch (Exception e) {
                        item.setAttachments(new ArrayList<>());
                    }
                } else {
                    item.setAttachments(new ArrayList<>());
                }

                // 处理metadata JSON
                String metadataJson = (String) row.get("metadata");
                if (metadataJson != null && !metadataJson.isEmpty() && !"{}".equals(metadataJson)) {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(
                                metadataJson, new TypeReference<Map<String, Object>>() {});
                        item.setMetadata(metadata);
                    } catch (Exception e) {
                        item.setMetadata(new HashMap<>());
                    }
                } else {
                    item.setMetadata(new HashMap<>());
                }

                item.setCreatedAt(row.get("created_at").toString());
                item.setUpdatedAt(row.get("updated_at").toString());
                item.setAssignedTo((String) row.get("assigned_to"));
                item.setAssignedAt(row.get("assigned_at") != null ? row.get("assigned_at").toString() : null);
                item.setCompletedAt(row.get("completed_at") != null ? row.get("completed_at").toString() : null);

                items.add(item);
            }

            // 8. 准备过滤器信息
            Map<String, Object> filters = new HashMap<>();
            filters.put("search", keyword);
            filters.put("sortBy", sortBy);
            filters.put("sortOrder", sortOrder);

            // 9. 准备统计信息（简化处理）
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", total);

            // 10. 准备响应数据
            SearchData searchData = new SearchData(
                    total, page, pageSize, totalPages, items, filters, statistics
            );

            SearchResponse response = new SearchResponse(
                    true, "搜索反馈成功", searchData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("搜索反馈过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}