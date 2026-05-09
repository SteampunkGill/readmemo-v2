package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGetHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取搜索历史请求 ===");
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
    public static class GetHistoryResponse {
        private boolean success;
        private String message;
        private GetHistoryData data;

        public GetHistoryResponse(boolean success, String message, GetHistoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public GetHistoryData getData() { return data; }
        public void setData(GetHistoryData data) { this.data = data; }
    }

    public static class GetHistoryData {
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<HistoryItem> items;

        public GetHistoryData(int total, int page, int pageSize, int totalPages, List<HistoryItem> items) {
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

        public List<HistoryItem> getItems() { return items; }
        public void setItems(List<HistoryItem> items) { this.items = items; }
    }

    public static class HistoryItem {
        private Long id;
        private String keyword;
        private String type;
        private String timestamp;
        private Integer resultCount;
        private Map<String, Object> filters;
        private Boolean success;

        public HistoryItem(Long id, String keyword, String type, String timestamp,
                           Integer resultCount, Map<String, Object> filters, Boolean success) {
            this.id = id;
            this.keyword = keyword;
            this.type = type;
            this.timestamp = timestamp;
            this.resultCount = resultCount;
            this.filters = filters;
            this.success = success;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public Integer getResultCount() { return resultCount; }
        public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }

        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
    }

    @GetMapping("/history")
    public ResponseEntity<GetHistoryResponse> getSearchHistory(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false, defaultValue = "timestamp") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("page", page);
        requestParams.put("pageSize", pageSize);
        requestParams.put("sortBy", sortBy);
        requestParams.put("sortOrder", sortOrder);
        requestParams.put("type", type);
        requestParams.put("keyword", keyword);
        requestParams.put("dateFrom", dateFrom);
        requestParams.put("dateTo", dateTo);
        printRequest(requestParams);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetHistoryResponse(false, "请先登录", null)
                );
            }

            // 2. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT search_id, keyword, search_type, result_count, timestamp ");
            sqlBuilder.append("FROM search_history ");
            sqlBuilder.append("WHERE user_id = ? ");

            List<Object> params = new ArrayList<>();
            params.add(userId);

            // 添加类型过滤
            if (type != null && !type.isEmpty()) {
                sqlBuilder.append("AND search_type = ? ");
                params.add(type);
            }

            // 添加关键词过滤
            if (keyword != null && !keyword.isEmpty()) {
                sqlBuilder.append("AND keyword LIKE ? ");
                params.add("%" + keyword + "%");
            }

            // 添加日期过滤
            if (dateFrom != null && !dateFrom.isEmpty()) {
                sqlBuilder.append("AND DATE(timestamp) >= ? ");
                params.add(dateFrom);
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                sqlBuilder.append("AND DATE(timestamp) <= ? ");
                params.add(dateTo);
            }

            // 3. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") AS count_table";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 4. 添加排序
            if ("timestamp".equals(sortBy)) {
                sqlBuilder.append("ORDER BY timestamp ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else if ("keyword".equals(sortBy)) {
                sqlBuilder.append("ORDER BY keyword ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else if ("resultCount".equals(sortBy)) {
                sqlBuilder.append("ORDER BY result_count ");
                sqlBuilder.append("desc".equalsIgnoreCase(sortOrder) ? "DESC " : "ASC ");
            } else {
                sqlBuilder.append("ORDER BY timestamp DESC ");
            }

            // 5. 添加分页
            sqlBuilder.append("LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            // 6. 执行查询
            List<Map<String, Object>> historyRecords = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + total + " 条搜索历史记录，返回 " + historyRecords.size() + " 条");

            // 7. 处理结果
            List<HistoryItem> items = new ArrayList<>();
            for (Map<String, Object> record : historyRecords) {
                HistoryItem item = new HistoryItem(
                        ((Number) record.get("search_id")).longValue(),
                        (String) record.get("keyword"),
                        (String) record.get("search_type"),
                        record.get("timestamp").toString(),
                        record.get("result_count") != null ? ((Number) record.get("result_count")).intValue() : 0,
                        new HashMap<>(), // 这里假设filters字段为空，实际可能需要从数据库解析
                        true // 假设所有记录都是成功的
                );
                items.add(item);
            }

            // 8. 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 9. 准备响应数据
            GetHistoryData data = new GetHistoryData(total, page, pageSize, totalPages, items);
            GetHistoryResponse response = new GetHistoryResponse(true, "获取搜索历史成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取搜索历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
}
