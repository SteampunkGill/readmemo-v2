package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGetFilters {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取搜索过滤器请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("==========================");
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

    // 响应DTO
    public static class GetFiltersResponse {
        private boolean success;
        private String message;
        private List<FilterItem> data;

        public GetFiltersResponse(boolean success, String message, List<FilterItem> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<FilterItem> getData() { return data; }
        public void setData(List<FilterItem> data) { this.data = data; }
    }

    public static class FilterItem {
        private Long filterId;
        private String name;
        private String description;
        private String filterType;
        private Map<String, Object> filterData;
        private boolean isPublic;
        private List<String> tags;
        private String createdAt;
        private String updatedAt;
        private Long createdBy;

        public FilterItem(Long filterId, String name, String description, String filterType,
                          Map<String, Object> filterData, boolean isPublic, List<String> tags,
                          String createdAt, String updatedAt, Long createdBy) {
            this.filterId = filterId;
            this.name = name;
            this.description = description;
            this.filterType = filterType;
            this.filterData = filterData;
            this.isPublic = isPublic;
            this.tags = tags;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.createdBy = createdBy;
        }

        public Long getFilterId() { return filterId; }
        public void setFilterId(Long filterId) { this.filterId = filterId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getFilterType() { return filterType; }
        public void setFilterType(String filterType) { this.filterType = filterType; }

        public Map<String, Object> getFilterData() { return filterData; }
        public void setFilterData(Map<String, Object> filterData) { this.filterData = filterData; }

        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    }

    @GetMapping("/filters")
    public ResponseEntity<GetFiltersResponse> getSearchFilters(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("page", page);
        requestParams.put("pageSize", pageSize);
        requestParams.put("type", type);
        requestParams.put("tag", tag);
        printRequest(requestParams);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetFiltersResponse(false, "请先登录", null)
                );
            }

            // 2. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT filter_id, name, description, filter_type, filter_data, is_public, created_at, updated_at, user_id ");
            sqlBuilder.append("FROM search_filters ");
            sqlBuilder.append("WHERE (user_id = ? OR is_public = true) ");

            List<Object> params = new ArrayList<>();
            params.add(userId);

            // 添加类型过滤
            if (type != null && !type.isEmpty()) {
                sqlBuilder.append("AND filter_type = ? ");
                params.add(type);
            }

            // 添加标签过滤
            if (tag != null && !tag.isEmpty()) {
                sqlBuilder.append("AND EXISTS (SELECT 1 FROM filter_tags t WHERE t.filter_id = search_filters.filter_id AND t.tag_name = ?) ");
                params.add(tag);
            }

            // 3. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_table";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 4. 添加排序和分页
            sqlBuilder.append("ORDER BY updated_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            // 5. 执行查询
            List<Map<String, Object>> filters = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + total + " 个过滤器，返回 " + filters.size() + " 个");

            // 6. 处理结果
            List<FilterItem> items = new ArrayList<>();
            for (Map<String, Object> filter : filters) {
                Long filterId = ((Number) filter.get("filter_id")).longValue();

                // 解析filter_data
                Map<String, Object> filterData = new HashMap<>();
                try {
                    String dataJson = (String) filter.get("filter_data");
                    if (dataJson != null && !dataJson.isEmpty()) {
                        filterData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(dataJson, Map.class);
                    }
                } catch (Exception e) {
                    System.err.println("解析过滤器数据失败: " + e.getMessage());
                }

                // 获取标签
                List<String> tags = getFilterTags(filterId);

                FilterItem item = new FilterItem(
                        filterId,
                        (String) filter.get("name"),
                        (String) filter.get("description"),
                        (String) filter.get("filter_type"),
                        filterData,
                        (Boolean) filter.get("is_public"),
                        tags,
                        filter.get("created_at").toString(),
                        filter.get("updated_at").toString(),
                        ((Number) filter.get("user_id")).longValue()
                );
                items.add(item);
            }

            // 7. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 8. 准备响应数据
            GetFiltersResponse response = new GetFiltersResponse(true, "获取搜索过滤器成功", items);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取搜索过滤器过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetFiltersResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 获取过滤器标签
    private List<String> getFilterTags(Long filterId) {
        List<String> tags = new ArrayList<>();
        try {
            String sql = "SELECT tag_name FROM filter_tags WHERE filter_id = ?";
            List<Map<String, Object>> tagList = jdbcTemplate.queryForList(sql, filterId);
            for (Map<String, Object> tag : tagList) {
                tags.add((String) tag.get("tag_name"));
            }
        } catch (Exception e) {
            System.err.println("获取过滤器标签失败: " + e.getMessage());
        }
        return tags;
    }

    // 从token获取用户ID
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
                List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);
                if (!sessions.isEmpty()) {
                    return ((Number) sessions.get(0).get("user_id")).longValue();
                }
            } catch (Exception e) {
                System.out.println("Token解析失败: " + e.getMessage());
            }
        }
        return null;
    }
}