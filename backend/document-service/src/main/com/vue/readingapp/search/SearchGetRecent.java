package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGetRecent {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取最近搜索请求 ===");
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
    public static class GetRecentResponse {
        private boolean success;
        private String message;
        private List<RecentItem> data;

        public GetRecentResponse(boolean success, String message, List<RecentItem> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<RecentItem> getData() { return data; }
        public void setData(List<RecentItem> data) { this.data = data; }
    }

    public static class RecentItem {
        private Long id;
        private String keyword;
        private String type;
        private String timestamp;
        private int resultCount;

        public RecentItem(Long id, String keyword, String type, String timestamp, int resultCount) {
            this.id = id;
            this.keyword = keyword;
            this.type = type;
            this.timestamp = timestamp;
            this.resultCount = resultCount;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public int getResultCount() { return resultCount; }
        public void setResultCount(int resultCount) { this.resultCount = resultCount; }
    }

    @GetMapping("/recent")
    public ResponseEntity<GetRecentResponse> getRecentSearches(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("limit: " + limit);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetRecentResponse(false, "请先登录", null)
                );
            }

            // 2. 构建SQL查询
            String sql = "SELECT search_id, keyword, search_type, timestamp, result_count " +
                    "FROM search_history " +
                    "WHERE user_id = ? " +
                    "ORDER BY timestamp DESC " +
                    "LIMIT ?";

            List<Map<String, Object>> recentSearches = jdbcTemplate.queryForList(sql, userId, limit);
            printQueryResult("找到 " + recentSearches.size() + " 条最近搜索记录");

            // 3. 处理结果
            List<RecentItem> items = new ArrayList<>();
            for (Map<String, Object> search : recentSearches) {
                RecentItem item = new RecentItem(
                        ((Number) search.get("search_id")).longValue(),
                        (String) search.get("keyword"),
                        (String) search.get("search_type"),
                        search.get("timestamp").toString(),
                        search.get("result_count") != null ? ((Number) search.get("result_count")).intValue() : 0
                );
                items.add(item);
            }

            // 4. 准备响应数据
            GetRecentResponse response = new GetRecentResponse(true, "获取最近搜索成功", items);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取最近搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetRecentResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
