package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderAddNote {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到添加笔记请求 ===");
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
    public static class NoteRequest {
        private String content;
        private int page;
        private Map<String, Object> position;
        private Integer highlightId;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }

        public Integer getHighlightId() { return highlightId; }
        public void setHighlightId(Integer highlightId) { this.highlightId = highlightId; }
    }

    // 响应DTO
    public static class NoteResponse {
        private boolean success;
        private String message;
        private NoteData data;

        public NoteResponse(boolean success, String message, NoteData data) {
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
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @PostMapping("/documents/{documentId}/notes")
    public ResponseEntity<NoteResponse> addNote(
            @PathVariable("documentId") int documentId,
            @RequestBody NoteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("request", request);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NoteResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token：直接在SQL中检查有效期，确保时区一致性
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NoteResponse(false, "登录已过期或无效，请重新登录", null)
                );
            }

            int userId = ((Number) sessions.get(0).get("user_id")).intValue();

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new NoteResponse(false, "没有权限在此文档添加笔记", null)
                );
            }

            // 3. 验证请求数据
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new NoteResponse(false, "笔记内容不能为空", null)
                );
            }

            if (request.getPage() < 1) {
                return ResponseEntity.badRequest().body(
                        new NoteResponse(false, "页码必须大于0", null)
                );
            }

            // 4. 检查页面是否存在
            String pageSql = "SELECT COUNT(*) as page_count FROM document_pages WHERE document_id = ? AND page_number = ?";
            List<Map<String, Object>> pageCounts = jdbcTemplate.queryForList(pageSql, documentId, request.getPage());
            int pageCount = pageCounts.isEmpty() ? 0 : ((Number) pageCounts.get(0).get("page_count")).intValue();

            if (pageCount == 0) {
                return ResponseEntity.badRequest().body(
                        new NoteResponse(false, "指定的页面不存在", null)
                );
            }

            // 5. 如果有关联的高亮，验证高亮是否存在且属于当前用户
            if (request.getHighlightId() != null) {
                String highlightSql = "SELECT * FROM document_highlights WHERE highlight_id = ? AND document_id = ? AND user_id = ?";
                List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightSql, request.getHighlightId(), documentId, userId);

                if (highlights.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            new NoteResponse(false, "关联的高亮不存在或没有权限", null)
                    );
                }
            }

            // 6. 插入笔记记录
            String insertSql = "INSERT INTO document_notes (document_id, user_id, content, page, position, highlight_id, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

            // 将position map转换为JSON字符串
            String positionJson = "{}";
            if (request.getPosition() != null && !request.getPosition().isEmpty()) {
                try {
                    // 简单处理，实际应该使用JSON库
                    positionJson = request.getPosition().toString();
                } catch (Exception e) {
                    System.err.println("转换position为JSON失败: " + e.getMessage());
                }
            }

            jdbcTemplate.update(insertSql,
                    documentId,
                    userId,
                    request.getContent().trim(),
                    request.getPage(),
                    positionJson,
                    request.getHighlightId()
            );

            // 7. 获取刚插入的笔记ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as note_id";
            List<Map<String, Object>> lastIds = jdbcTemplate.queryForList(lastIdSql);
            int noteId = lastIds.isEmpty() ? 0 : ((Number) lastIds.get(0).get("note_id")).intValue();

            // 8. 获取完整的笔记数据
            String noteSql = "SELECT * FROM document_notes WHERE note_id = ?";
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(noteSql, noteId);
            printQueryResult(notes);

            if (notes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new NoteResponse(false, "创建笔记失败", null)
                );
            }

            Map<String, Object> note = notes.get(0);

            // 9. 构建响应数据
            NoteData noteData = new NoteData();
            noteData.setId(noteId);
            noteData.setDocumentId(documentId);
            noteData.setContent((String) note.get("content"));
            noteData.setPage(((Number) note.get("page")).intValue());
            noteData.setHighlightId(note.get("highlight_id") != null ?
                    ((Number) note.get("highlight_id")).intValue() : null);
            
            Object createdAt = note.get("created_at");
            if (createdAt instanceof java.sql.Timestamp) {
                noteData.setCreatedAt(((java.sql.Timestamp) createdAt).toLocalDateTime());
            } else if (createdAt instanceof LocalDateTime) {
                noteData.setCreatedAt((LocalDateTime) createdAt);
            }

            Object updatedAt = note.get("updated_at");
            if (updatedAt instanceof java.sql.Timestamp) {
                noteData.setUpdatedAt(((java.sql.Timestamp) updatedAt).toLocalDateTime());
            } else if (updatedAt instanceof LocalDateTime) {
                noteData.setUpdatedAt((LocalDateTime) updatedAt);
            }

            // 解析position JSON
            if (note.get("position") != null) {
                try {
                    String posStr = (String) note.get("position");
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

            NoteResponse response = new NoteResponse(true, "笔记已添加", noteData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("添加笔记过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new NoteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}