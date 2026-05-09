package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchDeleteHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除搜索历史请求 ===");
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
    public static class DeleteHistoryResponse {
        private boolean success;
        private String message;
        private DeleteHistoryData data;

        public DeleteHistoryResponse(boolean success, String message, DeleteHistoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DeleteHistoryData getData() { return data; }
        public void setData(DeleteHistoryData data) { this.data = data; }
    }

    public static class DeleteHistoryData {
        private Long deletedId;
        private String keyword;
        private String timestamp;

        public DeleteHistoryData(Long deletedId, String keyword, String timestamp) {
            this.deletedId = deletedId;
            this.keyword = keyword;
            this.timestamp = timestamp;
        }

        public Long getDeletedId() { return deletedId; }
        public void setDeletedId(Long deletedId) { this.deletedId = deletedId; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    @DeleteMapping("/history/{searchId}")
    public ResponseEntity<DeleteHistoryResponse> deleteSearchHistory(
            @PathVariable("searchId") Long searchId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("删除搜索历史ID: " + searchId);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteHistoryResponse(false, "请先登录", null)
                );
            }

            // 2. 检查记录是否存在且属于当前用户
            String checkSql = "SELECT search_id, keyword, timestamp FROM search_history " +
                    "WHERE search_id = ? AND user_id = ?";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(checkSql, searchId, userId);

            if (records.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteHistoryResponse(false, "搜索历史记录不存在或无权访问", null)
                );
            }

            Map<String, Object> record = records.get(0);
            String keyword = (String) record.get("keyword");
            String timestamp = record.get("timestamp").toString();

            // 3. 删除记录
            String deleteSql = "DELETE FROM search_history WHERE search_id = ? AND user_id = ?";
            int deletedCount = jdbcTemplate.update(deleteSql, searchId, userId);

            if (deletedCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteHistoryResponse(false, "删除失败，记录不存在", null)
                );
            }

            // 4. 准备响应数据
            DeleteHistoryData data = new DeleteHistoryData(searchId, keyword, timestamp);
            DeleteHistoryResponse response = new DeleteHistoryResponse(true, "搜索历史记录删除成功", data);

            // 打印查询结果
            printQueryResult("已删除搜索历史记录: ID=" + searchId + ", 关键词=" + keyword);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除搜索历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
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
