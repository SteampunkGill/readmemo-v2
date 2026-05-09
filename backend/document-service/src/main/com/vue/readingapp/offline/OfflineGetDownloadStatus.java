
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
public class OfflineGetDownloadStatus {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取离线下载状态请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("============================");
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
    public static class DownloadStatusResponse {
        private boolean success;
        private String message;
        private DownloadStatusData data;

        public DownloadStatusResponse(boolean success, String message, DownloadStatusData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DownloadStatusData getData() { return data; }
        public void setData(DownloadStatusData data) { this.data = data; }
    }

    public static class DownloadStatusData {
        private String downloadId;
        private String status;
        private int progress;
        private String speed;
        private String eta;
        private String documentId;
        private String documentTitle;
        private long fileSize;
        private long downloadedSize;
        private String startTime;
        private String endTime;
        private String error;

        public DownloadStatusData(String downloadId, String status, int progress, String speed,
                                  String eta, String documentId, String documentTitle,
                                  long fileSize, long downloadedSize, String startTime,
                                  String endTime, String error) {
            this.downloadId = downloadId;
            this.status = status;
            this.progress = progress;
            this.speed = speed;
            this.eta = eta;
            this.documentId = documentId;
            this.documentTitle = documentTitle;
            this.fileSize = fileSize;
            this.downloadedSize = downloadedSize;
            this.startTime = startTime;
            this.endTime = endTime;
            this.error = error;
        }

        // Getters and Setters
        public String getDownloadId() { return downloadId; }
        public void setDownloadId(String downloadId) { this.downloadId = downloadId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public String getSpeed() { return speed; }
        public void setSpeed(String speed) { this.speed = speed; }

        public String getEta() { return eta; }
        public void setEta(String eta) { this.eta = eta; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getDocumentTitle() { return documentTitle; }
        public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public long getDownloadedSize() { return downloadedSize; }
        public void setDownloadedSize(long downloadedSize) { this.downloadedSize = downloadedSize; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
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

    @GetMapping("/downloads/{downloadId}/status")
    public ResponseEntity<DownloadStatusResponse> getOfflineDownloadStatus(
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
                        new DownloadStatusResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DownloadStatusResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 查询下载状态
            String querySql = "SELECT d.*, doc.title as document_title " +
                    "FROM offline_downloads d " +
                    "LEFT JOIN documents doc ON d.document_id = doc.document_id " +
                    "WHERE d.download_id = ? AND d.user_id = ?";

            List<Map<String, Object>> downloads = jdbcTemplate.queryForList(querySql, downloadId, userId);
            printQueryResult(downloads);

            if (downloads.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DownloadStatusResponse(false, "下载任务不存在", null)
                );
            }

            Map<String, Object> row = downloads.get(0);

            // 3. 准备响应数据
            DownloadStatusData data = new DownloadStatusData(
                    (String) row.get("download_id"),
                    (String) row.get("status"),
                    row.get("progress") != null ? ((Number) row.get("progress")).intValue() : 0,
                    (String) row.get("speed"),
                    (String) row.get("eta"),
                    (String) row.get("document_id"),
                    (String) row.get("document_title"),
                    row.get("file_size") != null ? ((Number) row.get("file_size")).longValue() : 0,
                    row.get("downloaded_size") != null ? ((Number) row.get("downloaded_size")).longValue() : 0,
                    row.get("start_time") != null ? row.get("start_time").toString() : null,
                    row.get("end_time") != null ? row.get("end_time").toString() : null,
                    (String) row.get("error")
            );

            DownloadStatusResponse response = new DownloadStatusResponse(true, "获取下载状态成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取离线下载状态过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DownloadStatusResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}