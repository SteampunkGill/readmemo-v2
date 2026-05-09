
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
public class OfflineSyncNote {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到同步笔记数据请求 ===");
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
    public static class SyncNoteRequest {
        private NoteData note;

        public NoteData getNote() { return note; }
        public void setNote(NoteData note) { this.note = note; }
    }

    public static class NoteData {
        private String id;
        private String documentId;
        private String content;
        private String createdAt;
        private int offlineVersion;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 响应DTO
    public static class SyncNoteResponse {
        private boolean success;
        private String message;
        private SyncNoteData data;

        public SyncNoteResponse(boolean success, String message, SyncNoteData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SyncNoteData getData() { return data; }
        public void setData(SyncNoteData data) { this.data = data; }
    }

    public static class SyncNoteData {
        private String noteId;
        private boolean synced;
        private String syncedAt;
        private int offlineVersion;

        public SyncNoteData(String noteId, boolean synced, String syncedAt, int offlineVersion) {
            this.noteId = noteId;
            this.synced = synced;
            this.syncedAt = syncedAt;
            this.offlineVersion = offlineVersion;
        }

        // Getters and Setters
        public String getNoteId() { return noteId; }
        public void setNoteId(String noteId) { this.noteId = noteId; }

        public boolean isSynced() { return synced; }
        public void setSynced(boolean synced) { this.synced = synced; }

        public String getSyncedAt() { return syncedAt; }
        public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }

        public int getOfflineVersion() { return offlineVersion; }
        public void setOfflineVersion(int offlineVersion) { this.offlineVersion = offlineVersion; }
    }

    // 创建 offline_notes 表（如果不存在）
    private void createOfflineNotesTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS offline_notes (" +
                "offline_note_id VARCHAR(50) PRIMARY KEY, " +
                "note_id VARCHAR(50) NOT NULL, " +
                "user_id INT NOT NULL, " +
                "document_id VARCHAR(50) NOT NULL, " +
                "content TEXT, " +
                "is_synced BOOLEAN DEFAULT FALSE, " +
                "offline_version INT DEFAULT 1, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE" +
                ")";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("offline_notes 表已创建或已存在");
        } catch (Exception e) {
            System.err.println("创建 offline_notes 表失败: " + e.getMessage());
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

    @PostMapping("/sync/note")
    public ResponseEntity<SyncNoteResponse> syncNote(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SyncNoteRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncNoteResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new SyncNoteResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getNote() == null) {
                return ResponseEntity.badRequest().body(
                        new SyncNoteResponse(false, "笔记数据不能为空", null)
                );
            }

            NoteData noteData = request.getNote();
            if (noteData.getId() == null || noteData.getId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new SyncNoteResponse(false, "笔记ID不能为空", null)
                );
            }

            // 3. 创建相关表
            createOfflineNotesTableIfNotExists();

            // 4. 检查笔记是否存在（假设有notes表）
            String checkNoteSql = "SELECT note_id FROM notes WHERE note_id = ?";
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(checkNoteSql, noteData.getId());

            if (notes.isEmpty()) {
                // 笔记不存在，创建新笔记
                String insertNoteSql = "INSERT INTO notes (note_id, user_id, document_id, content, created_at) " +
                        "VALUES (?, ?, ?, ?, NOW())";
                jdbcTemplate.update(insertNoteSql, noteData.getId(), userId,
                        noteData.getDocumentId(), noteData.getContent());
            } else {
                // 笔记存在，更新笔记
                String updateNoteSql = "UPDATE notes SET document_id = ?, content = ?, updated_at = NOW() " +
                        "WHERE note_id = ? AND user_id = ?";
                jdbcTemplate.update(updateNoteSql, noteData.getDocumentId(),
                        noteData.getContent(), noteData.getId(), userId);
            }

            // 5. 检查是否已有离线笔记记录
            String checkOfflineSql = "SELECT offline_note_id, offline_version FROM offline_notes " +
                    "WHERE note_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineNotes = jdbcTemplate.queryForList(checkOfflineSql,
                    noteData.getId(), userId);

            if (offlineNotes.isEmpty()) {
                // 创建离线笔记记录
                String offlineNoteId = "offline_note_" + UUID.randomUUID().toString();
                String insertOfflineSql = "INSERT INTO offline_notes (offline_note_id, note_id, user_id, " +
                        "document_id, content, is_synced, offline_version, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, TRUE, ?, NOW())";
                jdbcTemplate.update(insertOfflineSql, offlineNoteId, noteData.getId(), userId,
                        noteData.getDocumentId(), noteData.getContent(),
                        noteData.getOfflineVersion());
            } else {
                // 更新离线笔记记录
                Map<String, Object> offlineNote = offlineNotes.get(0);
                int currentVersion = offlineNote.get("offline_version") != null ?
                        ((Number) offlineNote.get("offline_version")).intValue() : 0;

                if (noteData.getOfflineVersion() > currentVersion) {
                    // 离线版本较新，更新服务器数据
                    String updateOfflineSql = "UPDATE offline_notes SET document_id = ?, content = ?, " +
                            "is_synced = TRUE, offline_version = ?, updated_at = NOW() " +
                            "WHERE note_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateOfflineSql, noteData.getDocumentId(),
                            noteData.getContent(), noteData.getOfflineVersion(),
                            noteData.getId(), userId);
                } else {
                    // 服务器版本较新或相同，标记为已同步
                    String updateSyncSql = "UPDATE offline_notes SET is_synced = TRUE, updated_at = NOW() " +
                            "WHERE note_id = ? AND user_id = ?";
                    jdbcTemplate.update(updateSyncSql, noteData.getId(), userId);
                }
            }

            // 6. 准备响应数据
            SyncNoteData data = new SyncNoteData(
                    noteData.getId(),
                    true,
                    "刚刚同步",
                    noteData.getOfflineVersion()
            );

            SyncNoteResponse response = new SyncNoteResponse(true, "笔记同步成功", data);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("同步笔记数据过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SyncNoteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}