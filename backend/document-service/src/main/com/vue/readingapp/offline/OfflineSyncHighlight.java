
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineSyncHighlight {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到同步高亮数据请求 ===");
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

    // 请求DTO
    public static class SyncHighlightRequest {
        private HighlightData highlight;

        public HighlightData getHighlight() { return highlight; }
        public void setHighlight(HighlightData highlight) { this.highlight = highlight; }
    }

    public static class HighlightData {
        private String id;
        private String documentId;
        private String text;
        private String color;
        private String createdAt;
        private int offlineVersion;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 响应DTO
    public static class SyncHighlightResponse {
        private boolean success;
        private String message;
        private SyncHighlightData data;

        public SyncHighlightResponse(boolean success, String message, SyncHighlightData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SyncHighlightData getData() { return data; }
        public void setData(SyncHighlightData data) { this.data = data; }
    }

    public static class SyncHighlightData {
        private String highlightId;
        private boolean synced;
        private String syncedAt;
        private int offlineVersion;

        public SyncHighlightData(String highlightId, boolean synced, String syncedAt, int offlineVersion) {
            this.highlightId = highlightId;
            this.synced = synced;
            this.syncedAt = syncedAt;
            this.offlineVersion = offlineVersion;
        }

        // Getters and Setters
        public String getHighlightId() { return highlightId; }
        public void setHighlightId(String highlightId) { this.highlightId = highlightId; }

        public boolean isSynced() { return synced; }
        public void setSynced(boolean synced) { this.synced = synced; }

        public String getSyncedAt() { return syncedAt; }
        public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 创建 offline_highlights 表（如果不存在）
    private void createOfflineHighlightsTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_highlights (" +
                "offline_highlight_id VARCHAR(50) PRIMARY KEY, " +
                "highlight_id VARCHAR(50) NOT NULL, " +
                "user_id INT NOT NULL, " +
                "document_id VARCHAR(50) NOT NULL, " +
                "text TEXT, " +
                "color VARCHAR(20), " +
                "is_synced BOOLEAN DEFAULT FALSE, " +
                "offline_version INT DEFAULT 1, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_highlights 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_highlights 表失败: " + e.getMessage());
        }
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

    @PostMapping("/sync/highlight")
    public ResponseEntity<SyncHighlightResponse> syncHighlight(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SyncHighlightRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncHighlightResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncHighlightResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getHighlight() == null) {
                return ResponseEntity.badRequest().body(
                        new SyncHighlightResponse(false, "高亮数据不能为空", null)
                );
            }

            HighlightData highlightData = request.getHighlight();
            if (highlightData.getId() == null || highlightData.getId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SyncHighlightResponse(false, "高亮ID不能为空", null)
                );
            }

            // 3. 创建相关表
            createOfflineHighlightsTableIfNotExists();

            // 4. 检查高亮是否存在（假设有highlights表）
            String checkHighlightSql = "SELECT highlight_id FROM highlights WHERE highlight_id = ?";
            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(checkHighlightSql, highlightData.getId());

            if (highlights.isEmpty()) {
                // 高亮不存在，创建新高亮
                String insertHighlightSql = "INSERT INTO highlights (highlight_id, user_id, document_id, text, color, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())";
                jdbcTemplate.update(insertHighlightSql, highlightData.getId(), userId,
                        highlightData.getDocumentId(), highlightData.getText(),
                        highlightData.getColor());
            } else {
                // 高亮存在，更新高亮
                String updateHighlightSql = "UPDATE highlights SET document_id = ?, text = ?, color = ?, updated_at = NOW() " +
                        "WHERE highlight_id = ? AND user_id = ?";
                jdbcTemplate.update(updateHighlightSql, highlightData.getDocumentId(),
                        highlightData.getText(), highlightData.getColor(),
                        highlightData.getId(), userId);
            }

            // 5. 检查是否已有离线高亮记录
            String checkOfflineSql = "SELECT offline_highlight_id, offline_version FROM offline_highlights " +
                    "WHERE highlight_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineHighlights = jdbcTemplate.queryForList(checkOfflineSql,
                    highlightData.getId(), userId);

            if (offlineHighlights.isEmpty()) {
                // 创建离线高亮记录
                String offlineHighlightId = "offline_highlight_" + UUID.randomUUID().toString();
                String insertOfflineSql = "INSERT INTO offline_highlights (offline_highlight_id, highlight_id, user_id, " +
                        "document_id, text, color, is_synced, offline_version, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, TRUE, ?, NOW())";
                jdbcTemplate.update(insertOfflineSql, offlineHighlightId, highlightData.getId(), userId,
                        highlightData.getDocumentId(), highlightData.getText(),
                        highlightData.getColor(), highlightData.getOfflineVersion());
            } else {
                // 更新离线高亮记录
                Map<String, Object> offlineHighlight = offlineHighlights.get(0);
                int currentVersion = offlineHighlight.get("offline_version") != null ?
                        ((Number) offlineHighlight.get("offline_version")).intValue() : 0;

                if (highlightData.getOfflineVersion() > currentVersion) {
                    // 离线版本较新，更新服务器数据
                    String updateOfflineSql = "UPDATE offline_highlights SET document_id = ?, text = ?, color = ?, " +
                            "is_synced = TRUE, offline_version = ?, updated_at = NOW() " +
                            "WHERE highlight_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateOfflineSql, highlightData.getDocumentId(),
                            highlightData.getText(), highlightData.getColor(),
                            highlightData.getOfflineVersion(), highlightData.getId(), userId);
                } else {
                    // 服务器版本较新或相同，标记为已同步
                    String updateSyncSql = "UPDATE offline_highlights SET is_synced = TRUE, updated_at = NOW() " +
                            "WHERE highlight_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateSyncSql, highlightData.getId(), userId);
                }
            }

            // 6. 准备响应数据
            SyncHighlightData data = new SyncHighlightData(
                    highlightData.getId(),
                    true,
                    "刚刚同步",
                    highlightData.getOfflineVersion()
            );

            SyncHighlightResponse response = new SyncHighlightResponse(true, "高亮同步成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("同步高亮数据过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SyncHighlightResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}