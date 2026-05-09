package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchCreateFilter {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到创建搜索过滤器请求 ===");
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

    // 请求DTO
    public static class CreateFilterRequest {
        private String name;
        private String description;
        private String filterType;
        private Map<String, Object> filterData;
        private boolean isPublic = false;
        private List<String> tags;

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
    }

    // 响应DTO
    public static class CreateFilterResponse {
        private boolean success;
        private String message;
        private FilterData data;

        public CreateFilterResponse(boolean success, String message, FilterData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public FilterData getData() { return data; }
        public void setData(FilterData data) { this.data = data; }
    }

    public static class FilterData {
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

        public FilterData(Long filterId, String name, String description, String filterType,
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

    @PostMapping("/filters")
    public ResponseEntity<CreateFilterResponse> createSearchFilter(
            @RequestBody CreateFilterRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CreateFilterResponse(false, "请先登录", null)
                );
            }

            // 2. 验证请求数据
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new CreateFilterResponse(false, "过滤器名称不能为空", null)
                );
            }

            if (request.getFilterType() == null || request.getFilterType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new CreateFilterResponse(false, "过滤器类型不能为空", null)
                );
            }
            if (request.getFilterData() == null || request.getFilterData().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new CreateFilterResponse(false, "过滤器数据不能为空", null)
                );
            }

            // 3. 检查是否已存在相同名称的过滤器（同一用户）
            String checkSql = "SELECT COUNT(*) FROM search_filters WHERE user_id = ? AND name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, request.getName());

            if (count != null && count > 0) {
                return ResponseEntity.badRequest().body(
                        new CreateFilterResponse(false, "已存在相同名称的过滤器", null)
                );
            }

            // 4. 将filterData转换为JSON字符串
            String filterDataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.getFilterData());

            // 5. 插入新的过滤器记录
            String insertSql = "INSERT INTO search_filters (user_id, name, description, filter_type, filter_data, is_public, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

            jdbcTemplate.update(insertSql,
                    userId,
                    request.getName(),
                    request.getDescription(),
                    request.getFilterType(),
                    filterDataJson,
                    request.isPublic());

            // 6. 获取刚插入的记录ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Map<String, Object> lastIdResult = jdbcTemplate.queryForMap(lastIdSql);
            Long filterId = ((Number) lastIdResult.get("id")).longValue();

            // 7. 处理标签
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                saveFilterTags(filterId, request.getTags());
            }

            // 8. 获取完整的过滤器信息
            String getSql = "SELECT filter_id, user_id, name, description, filter_type, filter_data, is_public, created_at, updated_at " +
                    "FROM search_filters WHERE filter_id = ?";

            Map<String, Object> filterRecord = jdbcTemplate.queryForMap(getSql, filterId);

            // 9. 解析filter_data
            Map<String, Object> filterData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    (String) filterRecord.get("filter_data"), Map.class);

            // 10. 获取标签
            List<String> tags = getFilterTags(filterId);

            // 11. 准备响应数据
            FilterData data = new FilterData(
                    filterId,
                    (String) filterRecord.get("name"),
                    (String) filterRecord.get("description"),
                    (String) filterRecord.get("filter_type"),
                    filterData,
                    (Boolean) filterRecord.get("is_public"),
                    tags,
                    filterRecord.get("created_at").toString(),
                    filterRecord.get("updated_at").toString(),
                    userId
            );

            CreateFilterResponse response = new CreateFilterResponse(true, "搜索过滤器创建成功", data);

            // 打印查询结果
            printQueryResult("创建过滤器: ID=" + filterId + ", 名称=" + request.getName());

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("创建搜索过滤器过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CreateFilterResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 保存过滤器标签
    private void saveFilterTags(Long filterId, List<String> tags) {
        try {
            // 先删除旧的标签
            String deleteSql = "DELETE FROM filter_tags WHERE filter_id = ?";
            jdbcTemplate.update(deleteSql, filterId);

            // 插入新的标签
            String insertSql = "INSERT INTO filter_tags (filter_id, tag_name) VALUES (?, ?)";
            for (String tag : tags) {
                jdbcTemplate.update(insertSql, filterId, tag);
            }

            System.out.println("保存过滤器标签: 过滤器ID=" + filterId + ", 标签=" + tags);

        } catch (Exception e) {
            System.err.println("保存过滤器标签失败: " + e.getMessage());
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
