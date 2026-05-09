package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchSave {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到保存搜索记录请求 ===");
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

    // 请求DTO
    public static class SaveSearchRequest {
        private String query;
        private String search_type = "global";

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public String getSearch_type() { return search_type; }
        public void setSearch_type(String search_type) { this.search_type = search_type; }
    }

    // 响应DTO
    public static class SaveSearchResponse {
        private boolean success;
        private String message;
        private SaveSearchData data;

        public SaveSearchResponse(boolean success, String message, SaveSearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SaveSearchData getData() { return data; }
        public void setData(SaveSearchData data) { this.data = data; }
    }

    public static class SaveSearchData {
        private Long searchId;
        private String query;
        private String searchType;
        private String timestamp;

        public SaveSearchData(Long searchId, String query, String searchType, String timestamp) {
            this.searchId = searchId;
            this.query = query;
            this.searchType = searchType;
            this.timestamp = timestamp;
        }

        public Long getSearchId() { return searchId; }
        public void setSearchId(Long searchId) { this.searchId = searchId; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    @PostMapping("/history")
    public ResponseEntity<SaveSearchResponse> saveSearch(
            @RequestBody SaveSearchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证请求数据
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SaveSearchResponse(false, "搜索关键词不能为空", null)
                );
            }

            // 2. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SaveSearchResponse(false, "请先登录", null)
                );
            }

            // 3. 检查是否已存在相同的搜索记录（同一用户、同一关键词、同一类型）
            String checkSql = "SELECT search_id, timestamp FROM search_history " +
                    "WHERE user_id = ? AND keyword = ? AND search_type = ? " +
                    "ORDER BY timestamp DESC LIMIT 1";

            List<Map<String, Object>> existingRecords = jdbcTemplate.queryForList(
                    checkSql, userId, request.getQuery(), request.getSearch_type());

            if (!existingRecords.isEmpty()) {
                // 更新现有记录的时间戳
                Long existingId = ((Number) existingRecords.get(0).get("search_id")).longValue();
                String updateSql = "UPDATE search_history SET timestamp = NOW() WHERE search_id = ?";
                jdbcTemplate.update(updateSql, existingId);

                // 获取更新后的记录
                String getSql = "SELECT search_id, keyword, search_type, timestamp FROM search_history WHERE search_id = ?";
                Map<String, Object> updatedRecord = jdbcTemplate.queryForMap(getSql, existingId);

                SaveSearchData data = new SaveSearchData(
                        ((Number) updatedRecord.get("search_id")).longValue(),
                        (String) updatedRecord.get("keyword"),
                        (String) updatedRecord.get("search_type"),
                        updatedRecord.get("timestamp").toString()
                );

                SaveSearchResponse response = new SaveSearchResponse(true, "搜索记录已更新", data);

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            }

            // 4. 插入新的搜索记录
            String insertSql = "INSERT INTO search_history (user_id, keyword, search_type, result_count, timestamp) " +
                    "VALUES (?, ?, ?, 0, NOW())";

            jdbcTemplate.update(insertSql, userId, request.getQuery(), request.getSearch_type());

            // 5. 获取刚插入的记录ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Map<String, Object> lastIdResult = jdbcTemplate.queryForMap(lastIdSql);
            Long searchId = ((Number) lastIdResult.get("id")).longValue();

            // 6. 获取完整的记录信息
            String getSql = "SELECT search_id, keyword, search_type, timestamp FROM search_history WHERE search_id = ?";
            Map<String, Object> savedRecord = jdbcTemplate.queryForMap(getSql, searchId);

            // 7. 准备响应数据
            SaveSearchData data = new SaveSearchData(
                    ((Number) savedRecord.get("search_id")).longValue(),
                    (String) savedRecord.get("keyword"),
                    (String) savedRecord.get("search_type"),
                    savedRecord.get("timestamp").toString()
            );

            SaveSearchResponse response = new SaveSearchResponse(true, "搜索记录保存成功", data);

            // 打印查询结果
            printQueryResult(savedRecord);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("保存搜索记录过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SaveSearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
