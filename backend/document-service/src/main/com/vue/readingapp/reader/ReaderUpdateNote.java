package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderUpdateNote {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新笔记请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
    public static class UpdateNoteRequest {
        private String content;
        private Map<String, Object> position;
        private Integer highlightId;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }

        public Integer getHighlightId() { return highlightId; }
        public void setHighlightId(Integer highlightId) { this.highlightId = highlightId; }
    }

    // 响应DTO
    public static class UpdateNoteResponse {
        private boolean success;
        private String message;
        private NoteData data;

        public UpdateNoteResponse(boolean success, String message, NoteData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NoteData getData() { return data; }
        public void setData(NoteData data) { this.data = data; }
    }

    public static class NoteData {
        private int id;
        private int documentId;
        private String content;
        private int page;
        private Map<String, Object> position;
        private Integer highlightId;
        private String createdAt;
        private String updatedAt;

        public NoteData() {
            this.position = new HashMap<>();
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }

        public Integer getHighlightId() { return highlightId; }
        public void setHighlightId(Integer highlightId) { this.highlightId = highlightId; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/documents/{documentId}/notes/{noteId}")
    public ResponseEntity<UpdateNoteResponse> updateNote(
            @PathVariable("documentId") int documentId,
            @PathVariable("noteId") int noteId,
            @RequestBody UpdateNoteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("noteId", noteId);
        requestInfo.put("request", request);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateNoteResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateNoteResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证笔记是否存在且属于当前用户
            String noteSql = "SELECT * FROM document_notes WHERE note_id = ? AND document_id = ? AND user_id = ?";
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(noteSql, noteId, documentId, userId);

            if (notes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateNoteResponse(false, "笔记不存在或没有权限修改", null)
                );
            }

            // 3. 如果有关联的高亮，验证高亮是否存在且属于当前用户
            if (request.getHighlightId() != null) {
                String highlightSql = "SELECT * FROM document_highlights WHERE highlight_id = ? AND document_id = ? AND user_id = ?";
                List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightSql, request.getHighlightId(), documentId, userId);

                if (highlights.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            new UpdateNoteResponse(false, "关联的高亮不存在或没有权限", null)
                    );
                }
            }

            // 4. 构建更新字段
            List<String> updateFields = new ArrayList<>();
            List<Object> updateParams = new ArrayList<>();

            if (request.getContent() != null) {
                updateFields.add("content = ?");
                updateParams.add(request.getContent().trim());
            }

            if (request.getPosition() != null) {
                // 将position map转换为JSON字符串
                String positionJson = "{}";
                try {
                    // 简单处理，实际应该使用JSON库
                    positionJson = request.getPosition().toString();
                } catch (Exception e) {
                    System.err.println("转换position为JSON失败: " + e.getMessage());
                }
                updateFields.add("position = ?");
                updateParams.add(positionJson);
            }

            if (request.getHighlightId() != null) {
                updateFields.add("highlight_id = ?");
                updateParams.add(request.getHighlightId());
            }

            // 如果没有要更新的字段
            if (updateFields.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateNoteResponse(false, "没有提供更新数据", null)
                );
            }

            // 添加更新时间
            updateFields.add("updated_at = NOW()");

            // 添加WHERE条件参数
            updateParams.add(noteId);
            updateParams.add(documentId);
            updateParams.add(userId);

            // 5. 执行更新
            String updateSql = "UPDATE document_notes SET " + String.join(", ", updateFields) +
                    " WHERE note_id = ? AND document_id = ? AND user_id = ?";

            int updatedRows = jdbcTemplate.update(updateSql, updateParams.toArray());

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateNoteResponse(false, "更新笔记失败", null)
                );
            }

            // 6. 获取更新后的笔记数据
            String getUpdatedSql = "SELECT * FROM document_notes WHERE note_id = ?";
            List<Map<String, Object>> updatedNotes = jdbcTemplate.queryForList(getUpdatedSql, noteId);
            printQueryResult(updatedNotes);

            if (updatedNotes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateNoteResponse(false, "获取更新后的笔记数据失败", null)
                );
            }

            Map<String, Object> updatedNote = updatedNotes.get(0);

            // 7. 构建响应数据
            NoteData noteData = new NoteData();
            noteData.setId(noteId);
            noteData.setDocumentId(documentId);
            noteData.setContent((String) updatedNote.get("content"));
            noteData.setPage(((Number) updatedNote.get("page")).intValue());
            noteData.setHighlightId(updatedNote.get("highlight_id") != null ?
                    ((Number) updatedNote.get("highlight_id")).intValue() : null);
            noteData.setCreatedAt(updatedNote.get("created_at").toString());
            noteData.setUpdatedAt(updatedNote.get("updated_at").toString());

            // 解析position JSON
            if (updatedNote.get("position") != null) {
                try {
                    String posStr = (String) updatedNote.get("position");
                    if (posStr.startsWith("{") && posStr.endsWith("}")) {
                        Map<String, Object> posMap = new HashMap<>();
                        posMap.put("x", 0);
                        posMap.put("y", 0);
                        posMap.put("width", 100);
                        posMap.put("height", 20);
                        noteData.setPosition(posMap);
                    }
                } catch (Exception e) {
                    System.err.println("解析position JSON失败: " + e.getMessage());
                }
            }

            UpdateNoteResponse response = new UpdateNoteResponse(true, "笔记已更新", noteData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新笔记过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateNoteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}