package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGetSaved {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取保存搜索请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
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
    public static class GetSavedResponse {
        private boolean success;
        private String message;
        private GetSavedData data;

        public GetSavedResponse(boolean success, String message, GetSavedData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public GetSavedData getData() { return data; }
        public void setData(GetSavedData data) { this.data = data; }
    }

    public static class GetSavedData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<SavedItem> items;

        public GetSavedData(int total, int page, int pageSize, int totalPages, List<SavedItem> items) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.items = items;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

        public List<SavedItem> getItems() { return items; }
        public void setItems(List<SavedItem> items) { this.items = items; }
    }

    public static class SavedItem {
        private Long id;
        private Long searchId;
        private String keyword;
        private String searchType;
        private Map<String, Object> savedData;
        private String createdAt;
        private String updatedAt;
        private String note;
        private List<String> tags;

        public SavedItem(Long id, Long searchId, String keyword, String searchType,
                         Map<String, Object> savedData, String createdAt, String updatedAt,
                         String note, List<String> tags) {
            this.id = id;
            this.searchId = searchId;
            this.keyword = keyword;
            this.searchType = searchType;
            this.savedData = savedData;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.note = note;
            this.tags = tags;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getSearchId() { return searchId; }
        public void setSearchId(Long searchId) { this.searchId = searchId; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }

        public Map<String, Object> getSavedData() { return savedData; }
        public void setSavedData(Map<String, Object> savedData) { this.savedData = savedData; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    @GetMapping("/saved")
    public ResponseEntity<GetSavedResponse> getSavedSearches(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("page", page);
        requestParams.put("pageSize", pageSize);
        requestParams.put("keyword", keyword);
        requestParams.put("type", type);
        requestParams.put("tag", tag);
        requestParams.put("dateFrom", dateFrom);
        requestParams.put("dateTo", dateTo);
        printRequest(requestParams);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetSavedResponse(false, "请先登录", null)
                );
            }

            // 2. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT s.saved_id, s.search_id, s.saved_data, s.created_at, s.updated_at, s.note, ");
            sqlBuilder.append("h.keyword, h.search_type ");
            sqlBuilder.append("FROM saved_search_results s ");
            sqlBuilder.append("INNER JOIN search_history h ON s.search_id = h.search_id ");
            sqlBuilder.append("WHERE s.user_id = ? ");

            List<Object> params = new ArrayList<>();
            params.add(userId);

            // 添加关键词过滤
            if (keyword != null && !keyword.isEmpty()) {
                sqlBuilder.append("AND h.keyword LIKE ? ");
                params.add("%" + keyword + "%");
            }

            // 添加类型过滤
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                sqlBuilder.append("AND h.search_type = ? ");
                params.add(type);
            }

            // 添加日期过滤
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sqlBuilder.append("AND DATE(s.created_at) >= ? ");
                params.add(dateFrom);
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                sqlBuilder.append("AND DATE(s.created_at) <= ? ");
                params.add(dateTo);
            }

            // 添加标签过滤
            if (tag != null && !tag.isEmpty()) {
                sqlBuilder.append("AND EXISTS (SELECT 1 FROM saved_search_tags t WHERE t.saved_id = s.saved_id AND t.tag_name = ?) ");
                params.add(tag);
            }

            // 3. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_table";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 4. 添加排序和分页
            sqlBuilder.append("ORDER BY s.updated_at DESC ");
            sqlBuilder.append("LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            // 5. 执行查询
            List<Map<String, Object>> savedSearches = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + total + " 条保存的搜索记录，返回 " + savedSearches.size() + " 条");

            // 6. 处理结果
            List<SavedItem> items = new ArrayList<>();
            for (Map<String, Object> saved : savedSearches) {
                Long savedId = ((Number) saved.get("saved_id")).longValue();
                Long searchId = ((Number) saved.get("search_id")).longValue();

                // 解析保存的数据
                Map<String, Object> savedData = new HashMap<>();
                try {
                    String dataJson = (String) saved.get("saved_data");
                    if (dataJson != null && !dataJson.isEmpty()) {
                        savedData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(dataJson, Map.class);
                    }
                } catch (Exception e) {
                    System.err.println("解析保存数据失败: " + e.getMessage());
                }

                // 获取标签
                List<String> tags = getSavedSearchTags(savedId);

                SavedItem item = new SavedItem(
                        savedId,
                        searchId,
                        (String) saved.get("keyword"),
                        (String) saved.get("search_type"),
                        savedData,
                        saved.get("created_at").toString(),
                        saved.get("updated_at").toString(),
                        (String) saved.get("note"),
                        tags
                );
                items.add(item);
            }

            // 7. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 8. 准备响应数据
            GetSavedData data = new GetSavedData(total, page, pageSize, totalPages, items);
            GetSavedResponse response = new GetSavedResponse(true, "获取保存搜索成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取保存搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetSavedResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
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

    // 获取保存搜索的标签
    private List<String> getSavedSearchTags(Long savedId) {
        List<String> tags = new ArrayList<>();
        try {
            String sql = "SELECT tag_name FROM saved_search_tags WHERE saved_id = ?";
            List<Map<String, Object>> tagList = jdbcTemplate.queryForList(sql, savedId);
            for (Map<String, Object> tag : tagList) {
                tags.add((String) tag.get("tag_name"));
            }
        } catch (Exception e) {
            System.err.println("获取保存搜索标签失败: " + e.getMessage());
        }
        return tags;
    }
}
