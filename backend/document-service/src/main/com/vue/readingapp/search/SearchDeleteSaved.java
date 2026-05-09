package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchDeleteSaved {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除保存搜索请求 ===");
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
    public static class DeleteSavedResponse {
        private boolean success;
        private String message;
        private DeleteSavedData data;

        public DeleteSavedResponse(boolean success, String message, DeleteSavedData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DeleteSavedData getData() { return data; }
        public void setData(DeleteSavedData data) { this.data = data; }
    }

    public static class DeleteSavedData {
        private Long deletedId;
        private String keyword;
        private String searchType;
        private String deletedAt;

        public DeleteSavedData(Long deletedId, String keyword, String searchType, String deletedAt) {
            this.deletedId = deletedId;
            this.keyword = keyword;
            this.searchType = searchType;
            this.deletedAt = deletedAt;
        }

        public Long getDeletedId() { return deletedId; }
        public void setDeletedId(Long deletedId) { this.deletedId = deletedId; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }

        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
    }

    @DeleteMapping("/saved/{savedId}")
    public ResponseEntity<DeleteSavedResponse> deleteSavedSearch(
            @PathVariable("savedId") Long savedId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("删除保存搜索ID: " + savedId);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteSavedResponse(false, "请先登录", null)
                );
            }

            // 2. 检查记录是否存在且属于当前用户
            String checkSql = "SELECT s.saved_id, h.keyword, h.search_type, s.updated_at " +
                    "FROM saved_search_results s " +
                    "INNER JOIN search_history h ON s.search_id = h.search_id " +
                    "WHERE s.saved_id = ? AND s.user_id = ?";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(checkSql, savedId, userId);

            if (records.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteSavedResponse(false, "保存的搜索记录不存在或无权访问", null)
                );
            }

            Map<String, Object> record = records.get(0);
            String keyword = (String) record.get("keyword");
            String searchType = (String) record.get("search_type");
            String updatedAt = record.get("updated_at").toString();

            // 3. 先删除关联的标签
            String deleteTagsSql = "DELETE FROM saved_search_tags WHERE saved_id = ?";
            jdbcTemplate.update(deleteTagsSql, savedId);

            // 4. 删除保存的搜索记录
            String deleteSql = "DELETE FROM saved_search_results WHERE saved_id = ? AND user_id = ?";
            int deletedCount = jdbcTemplate.update(deleteSql, savedId, userId);

            if (deletedCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteSavedResponse(false, "删除失败，记录不存在", null)
                );
            }

            // 5. 准备响应数据
            DeleteSavedData data = new DeleteSavedData(savedId, keyword, searchType, updatedAt);
            DeleteSavedResponse response = new DeleteSavedResponse(true, "保存的搜索记录删除成功", data);

            // 打印查询结果
            printQueryResult("已删除保存的搜索记录: ID=" + savedId + ", 关键词=" + keyword);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除保存搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteSavedResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
