
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineClearCache {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到清理离线缓存请求 ===");
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
    public static class ClearCacheResponse {
        private boolean success;
        private String message;
        private ClearCacheData data;

        public ClearCacheResponse(boolean success, String message, ClearCacheData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ClearCacheData getData() { return data; }
        public void setData(ClearCacheData data) { this.data = data; }
    }

    public static class ClearCacheData {
        private int deletedDocuments;
        private int deletedDownloads;
        private long freedSpace;

        public ClearCacheData(int deletedDocuments, int deletedDownloads, long freedSpace) {
            this.deletedDocuments = deletedDocuments;
            this.deletedDownloads = deletedDownloads;
            this.freedSpace = freedSpace;
        }

        // Getters and Setters
        public int getDeletedDocuments() { return deletedDocuments; }
        public void setDeletedDocuments(int deletedDocuments) { this.deletedDocuments = deletedDocuments; }

        public int getDeletedDownloads() { return deletedDownloads; }
        public void setDeletedDownloads(int deletedDownloads) { this.deletedDownloads = deletedDownloads; }

        public long getFreedSpace() { return freedSpace; }
        public void setFreedSpace(long freedSpace) { this.freedSpace = freedSpace; }
    }

    // 验证用户token
    private Integer validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);

            if (sessions.isEmpty()) {
                return null;
            }

            return (Integer) sessions.get(0).get("user_id");
        } catch (Exception e) {
            System.err.println("验证token失败: " + e.getMessage());
            return null;
        }
    }

    @DeleteMapping("/cache")
    public ResponseEntity<ClearCacheResponse> clearOfflineCache(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearCacheResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearCacheResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 统计清理前的数据
            String countDocsSql = "SELECT COUNT(*) as count, SUM(file_size) as total_size " +
                    "FROM offline_documents WHERE user_id = ?";
            List<Map<String, Object>> docStats = jdbcTemplate.queryForList(countDocsSql, userId);

            int deletedDocuments = 0;
            long freedSpace = 0;

            if (!docStats.isEmpty()) {
                Map<String, Object> row = docStats.get(0);
                deletedDocuments = row.get("count") != null ? ((Number) row.get("count")).intValue() : 0;
                freedSpace = row.get("total_size") != null ? ((Number) row.get("total_size")).longValue() : 0;
            }

            // 3. 删除离线文档
            String deleteDocsSql = "DELETE FROM offline_documents WHERE user_id = ?";
            int docsDeleted = jdbcTemplate.update(deleteDocsSql, userId);

            // 4. 删除下载记录
            String deleteDownloadsSql = "DELETE FROM offline_downloads WHERE user_id = ?";
            int downloadsDeleted = jdbcTemplate.update(deleteDownloadsSql, userId);

            printQueryResult("删除文档: " + docsDeleted + ", 删除下载记录: " + downloadsDeleted);

            // 5. 准备响应数据
            ClearCacheData data = new ClearCacheData(docsDeleted, downloadsDeleted, freedSpace);
            ClearCacheResponse response = new ClearCacheResponse(true,
                    String.format("离线缓存清理成功，释放了 %.2f MB 空间", freedSpace / (1024.0 * 1024.0)), data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("清理离线缓存过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ClearCacheResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}