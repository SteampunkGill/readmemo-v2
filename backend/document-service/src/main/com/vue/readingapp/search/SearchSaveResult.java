package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchSaveResult {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到保存搜索结果请求 ===");
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
    public static class SaveResultRequest {
        private Long searchId;
        private Map<String, Object> data;

        public Long getSearchId() { return searchId; }
        public void setSearchId(Long searchId) { this.searchId = searchId; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    // 响应DTO
    public static class SaveResultResponse {
        private boolean success;
        private String message;
        private SavedResultData data;

        public SaveResultResponse(boolean success, String message, SavedResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SavedResultData getData() { return data; }
        public void setData(SavedResultData data) { this.data = data; }
    }

    public static class SavedResultData {
        private Long savedId;
        private Long searchId;
        private String keyword;
        private String searchType;
        private Map<String, Object> savedData;
        private String createdAt;
        private String note;
        private List<String> tags;

        public SavedResultData(Long savedId, Long searchId, String keyword, String searchType,
                               Map<String, Object> savedData, String createdAt, String note, List<String> tags) {
            this.savedId = savedId;
            this.searchId = searchId;
            this.keyword = keyword;
            this.searchType = searchType;
            this.savedData = savedData;
            this.createdAt = createdAt;
            this.note = note;
            this.tags = tags;
        }

        public Long getSavedId() { return savedId; }
        public void setSavedId(Long savedId) { this.savedId = savedId; }

        public Long getSearchId() { return searchId; }
        public void setSearchId(Long searchId) { this.savedId = searchId; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }

        public Map<String, Object> getSavedData() { return savedData; }
        public void setSavedData(Map<String, Object> savedData) { this.savedData = savedData; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    @PostMapping("/save/{searchId}")
    public ResponseEntity<SaveResultResponse> saveSearchResult(
            @PathVariable("searchId") Long searchId,
            @RequestBody SaveResultRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 确保searchId一致
        request.setSearchId(searchId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SaveResultResponse(false, "请先登录", null)
                );
            }

            // 2. 验证请求数据
            if (request.getSearchId() == null) {
                return ResponseEntity.badRequest().body(
                        new SaveResultResponse(false, "搜索ID不能为空", null)
                );
            }

            if (request.getData() == null || request.getData().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SaveResultResponse(false, "保存数据不能为空", null)
                );
            }

            // 3. 检查搜索记录是否存在且属于当前用户
            String checkSql = "SELECT search_id, keyword, search_type FROM search_history " +
                    "WHERE search_id = ? AND user_id = ?";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(checkSql, searchId, userId);

            if (records.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new SaveResultResponse(false, "搜索记录不存在或无权访问", null)
                );
            }

            Map<String, Object> record = records.get(0);
            String keyword = (String) record.get("keyword");
            String searchType = (String) record.get("search_type");

            // 4. 检查是否已保存过相同的搜索结果
            String checkSavedSql = "SELECT saved_id FROM saved_search_results " +
                    "WHERE user_id = ? AND search_id = ?";

            List<Map<String, Object>> savedRecords = jdbcTemplate.queryForList(checkSavedSql, userId, searchId);

            if (!savedRecords.isEmpty()) {
                // 更新现有记录
                Long savedId = ((Number) savedRecords.get(0).get("saved_id")).longValue();

                // 将data转换为JSON字符串
                String dataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.getData());

                String updateSql = "UPDATE saved_search_results SET saved_data = ?, updated_at = NOW() WHERE saved_id = ?";
                jdbcTemplate.update(updateSql, dataJson, savedId);

                // 获取更新后的记录
                String getSql = "SELECT saved_id, search_id, saved_data, created_at, updated_at, note " +
                        "FROM saved_search_results WHERE saved_id = ?";

                Map<String, Object> savedRecord = jdbcTemplate.queryForMap(getSql, savedId);

                // 解析保存的数据
                Map<String, Object> savedData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        (String) savedRecord.get("saved_data"), Map.class);

                // 获取标签
                List<String> tags = getSavedSearchTags(savedId);

                SavedResultData data = new SavedResultData(
                        savedId,
                        searchId,
                        keyword,
                        searchType,
                        savedData,
                        savedRecord.get("created_at").toString(),
                        (String) savedRecord.get("note"),
                        tags
                );

                SaveResultResponse response = new SaveResultResponse(true, "搜索结果已更新", data);

                // 打印查询结果
                printQueryResult(savedRecord);

                // 打印返回数据
                printResponse(response);

                return ResponseEntity.ok(response);
            }

            // 5. 插入新的保存记录
            String dataJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.getData());

            String insertSql = "INSERT INTO saved_search_results (user_id, search_id, saved_data, created_at, updated_at) " +
                    "VALUES (?, ?, ?, NOW(), NOW())";

            jdbcTemplate.update(insertSql, userId, searchId, dataJson);

            // 6. 获取刚插入的记录ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as id";
            Map<String, Object> lastIdResult = jdbcTemplate.queryForMap(lastIdSql);
            Long savedId = ((Number) lastIdResult.get("id")).longValue();

            // 7. 获取完整的保存记录信息
            String getSql = "SELECT saved_id, search_id, saved_data, created_at, updated_at, note " +
                    "FROM saved_search_results WHERE saved_id = ?";

            Map<String, Object> savedRecord = jdbcTemplate.queryForMap(getSql, savedId);

            // 8. 解析保存的数据
            Map<String, Object> savedData = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    (String) savedRecord.get("saved_data"), Map.class);

            // 9. 准备响应数据
            SavedResultData data = new SavedResultData(
                    savedId,
                    searchId,
                    keyword,
                    searchType,
                    savedData,
                    savedRecord.get("created_at").toString(),
                    (String) savedRecord.get("note"),
                    new ArrayList<>() // 初始为空标签
            );

            SaveResultResponse response = new SaveResultResponse(true, "搜索结果保存成功", data);

            // 打印查询结果
            printQueryResult(savedRecord);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("保存搜索结果过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SaveResultResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
