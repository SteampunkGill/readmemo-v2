
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
public class OfflineCancelDownload {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到取消离线下载请求 ===");
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
    public static class CancelDownloadResponse {
        private boolean success;
        private String message;

        public CancelDownloadResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
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

    @DeleteMapping("/downloads/{downloadId}")
    public ResponseEntity<CancelDownloadResponse> cancelOfflineDownload(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String downloadId) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("downloadId", downloadId);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CancelDownloadResponse(false, "请先登录")
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CancelDownloadResponse(false, "登录已过期，请重新登录")
                );
            }

            // 2. 检查下载任务是否存在且属于该用户
            String checkSql = "SELECT download_id, status FROM offline_downloads WHERE download_id = ? AND user_id = ?";
            List<Map<String, Object>> downloads = jdbcTemplate.queryForList(checkSql, downloadId, userId);

            if (downloads.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new CancelDownloadResponse(false, "下载任务不存在或无权取消")
                );
            }

            Map<String, Object> download = downloads.get(0);
            String status = (String) download.get("status");

            // 3. 检查是否可以取消（只有pending或downloading状态可以取消）
            if ("completed".equals(status) || "failed".equals(status) || "cancelled".equals(status)) {
                return ResponseEntity.badRequest().body(
                        new CancelDownloadResponse(false, "下载任务已结束，无法取消")
                );
            }

            // 4. 取消下载任务
            String updateSql = "UPDATE offline_downloads SET status = 'cancelled', error = '用户取消', " +
                    "end_time = NOW() WHERE download_id = ? AND user_id = ?";
            int rowsAffected = jdbcTemplate.update(updateSql, downloadId, userId);

            printQueryResult("取消行数: " + rowsAffected);

            if (rowsAffected > 0) {
                CancelDownloadResponse response = new CancelDownloadResponse(true, "下载任务已取消");
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new CancelDownloadResponse(false, "取消失败")
                );
            }

        } catch (Exception e) {
            System.err.println("取消离线下载过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CancelDownloadResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}