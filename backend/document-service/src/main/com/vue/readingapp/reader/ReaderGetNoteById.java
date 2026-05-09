package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderGetNoteById {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取笔记详情请求 ===");
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
    public static class NoteDetailResponse {
        private boolean success;
        private String message;
        private NoteDetailData data;

        public NoteDetailResponse(boolean success, String message, NoteDetailData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NoteDetailData getData() { return data; }
        public void setData(NoteDetailData data) { this.data = data; }
    }

    public static class NoteDetailData {
        private int id;
        private int documentId;
        private String content;
        private int page;
        private Map<String, Object> position;
        private Integer highlightId;
        private String createdAt;
        private String updatedAt;
        private DocumentInfo document;
        private HighlightInfo highlight;

        public NoteDetailData() {
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

        public DocumentInfo getDocument() { return document; }
        public void setDocument(DocumentInfo document) { this.document = document; }

        public HighlightInfo getHighlight() { return highlight; }
        public void setHighlight(HighlightInfo highlight) { this.highlight = highlight; }
    }

    public static class DocumentInfo {
        private int id;
        private String title;
        private String author;
        private String language;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    public static class HighlightInfo {
        private int id;
        private String text;
        private String color;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    @GetMapping("/documents/{documentId}/notes/{noteId}")
    public ResponseEntity<NoteDetailResponse> getNoteById(
            @PathVariable("documentId") int documentId,
            @PathVariable("noteId") int noteId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("noteId", noteId);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NoteDetailResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NoteDetailResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new NoteDetailResponse(false, "没有权限查看此文档的笔记", null)
                );
            }

            // 3. 获取笔记详情
            String noteSql = "SELECT * FROM document_notes WHERE note_id = ? AND document_id = ? AND user_id = ?";
            List<Map<String, Object>> notes = jdbcTemplate.queryForList(noteSql, noteId, documentId, userId);
            printQueryResult(notes);

            if (notes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new NoteDetailResponse(false, "笔记不存在", null)
                );
            }

            Map<String, Object> note = notes.get(0);

            // 4. 获取文档信息
            DocumentInfo documentInfo = new DocumentInfo();
            Map<String, Object> document = documents.get(0);
            documentInfo.setId(documentId);
            documentInfo.setTitle((String) document.get("title"));
            documentInfo.setAuthor((String) document.get("author"));
            documentInfo.setLanguage((String) document.get("language"));

            // 5. 获取关联的高亮信息（如果有）
            HighlightInfo highlightInfo = null;
            if (note.get("highlight_id") != null) {
                String highlightSql = "SELECT * FROM document_highlights WHERE highlight_id = ? AND user_id = ?";
                List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightSql, note.get("highlight_id"), userId);

                if (!highlights.isEmpty()) {
                    Map<String, Object> highlight = highlights.get(0);
                    highlightInfo = new HighlightInfo();
                    highlightInfo.setId(((Number) highlight.get("highlight_id")).intValue());
                    highlightInfo.setText((String) highlight.get("text"));
                    highlightInfo.setColor((String) highlight.get("color"));
                }
            }

            // 6. 构建响应数据
            NoteDetailData noteData = new NoteDetailData();
            noteData.setId(noteId);
            noteData.setDocumentId(documentId);
            noteData.setContent((String) note.get("content"));
            noteData.setPage(((Number) note.get("page")).intValue());
            noteData.setHighlightId(note.get("highlight_id") != null ?
                    ((Number) note.get("highlight_id")).intValue() : null);
            noteData.setCreatedAt(note.get("created_at").toString());
            noteData.setUpdatedAt(note.get("updated_at").toString());
            noteData.setDocument(documentInfo);
            noteData.setHighlight(highlightInfo);

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

            NoteDetailResponse response = new NoteDetailResponse(true, "获取笔记详情成功", noteData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取笔记详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new NoteDetailResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}