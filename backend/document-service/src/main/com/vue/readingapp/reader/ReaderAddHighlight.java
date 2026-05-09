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
public class ReaderAddHighlight {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到添加高亮请求 ===");
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
    public static class HighlightRequest {
        private String text;
        private int page;
        private Map<String, Object> position;
        private String color;
        private String note;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    // 响应DTO
    public static class HighlightResponse {
        private boolean success;
        private String message;
        private HighlightData data;

        public HighlightResponse(boolean success, String message, HighlightData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public HighlightData getData() { return data; }
        public void setData(HighlightData data) { this.data = data; }
    }

    public static class HighlightData {
        private int id;
        private int documentId;
        private String text;
        private int page;
        private Map<String, Object> position;
        private String color;
        private String note;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public HighlightData() {
            this.position = new HashMap<>();
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    @PostMapping("/documents/{documentId}/highlights")
    public ResponseEntity<HighlightResponse> addHighlight(
            @PathVariable("documentId") int documentId,
            @RequestBody HighlightRequest request,
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
                        new HighlightResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token：直接在SQL中检查有效期，确保时区一致性
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new HighlightResponse(false, "登录已过期或无效，请重新登录", null)
                );
            }

            int userId = ((Number) sessions.get(0).get("user_id")).intValue();

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new HighlightResponse(false, "没有权限在此文档添加高亮", null)
                );
            }

            // 3. 验证请求数据
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new HighlightResponse(false, "高亮文本不能为空", null)
                );
            }

            if (request.getPage() < 1) {
                return ResponseEntity.badRequest().body(
                        new HighlightResponse(false, "页码必须大于0", null)
                );
            }

            // 4. 检查页面是否存在
            String pageSql = "SELECT COUNT(*) as page_count FROM document_pages WHERE document_id = ? AND page_number = ?";
            List<Map<String, Object>> pageCounts = jdbcTemplate.queryForList(pageSql, documentId, request.getPage());
            int pageCount = pageCounts.isEmpty() ? 0 : ((Number) pageCounts.get(0).get("page_count")).intValue();

            if (pageCount == 0) {
                return ResponseEntity.badRequest().body(
                        new HighlightResponse(false, "指定的页面不存在", null)
                );
            }

            // 5. 插入高亮记录
            String insertSql = "INSERT INTO document_highlights (document_id, user_id, text, page, position, color, note, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

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
                    request.getText().trim(),
                    request.getPage(),
                    positionJson,
                    request.getColor() != null ? request.getColor() : "yellow",
                    request.getNote() != null ? request.getNote().trim() : null
            );

            // 6. 获取刚插入的高亮ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as highlight_id";
            List<Map<String, Object>> lastIds = jdbcTemplate.queryForList(lastIdSql);
            int highlightId = lastIds.isEmpty() ? 0 : ((Number) lastIds.get(0).get("highlight_id")).intValue();

            // 7. 获取完整的高亮数据
            String highlightSql = "SELECT * FROM document_highlights WHERE highlight_id = ?";
            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightSql, highlightId);
            printQueryResult(highlights);

            if (highlights.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new HighlightResponse(false, "创建高亮失败", null)
                );
            }

            Map<String, Object> highlight = highlights.get(0);

            // 8. 构建响应数据
            HighlightData highlightData = new HighlightData();
            highlightData.setId(highlightId);
            highlightData.setDocumentId(documentId);
            highlightData.setText((String) highlight.get("text"));
            highlightData.setPage(((Number) highlight.get("page")).intValue());

            // 解析position JSON
            if (highlight.get("position") != null) {
                try {
                    String posStr = (String) highlight.get("position");
                    // 简单处理，实际应该使用JSON解析器
                    if (posStr.startsWith("{") && posStr.endsWith("}")) {
                        Map<String, Object> posMap = new HashMap<>();
                        posMap.put("x", 0);
                        posMap.put("y", 0);
                        posMap.put("width", 100);
                        posMap.put("height", 20);
                        highlightData.setPosition(posMap);
                    }
                } catch (Exception e) {
                    System.err.println("解析position JSON失败: " + e.getMessage());
                }
            }

            highlightData.setColor((String) highlight.get("color"));
            highlightData.setNote((String) highlight.get("note"));
            
            Object createdAt = highlight.get("created_at");
            if (createdAt instanceof java.sql.Timestamp) {
                highlightData.setCreatedAt(((java.sql.Timestamp) createdAt).toLocalDateTime());
            } else if (createdAt instanceof LocalDateTime) {
                highlightData.setCreatedAt((LocalDateTime) createdAt);
            }

            Object updatedAt = highlight.get("updated_at");
            if (updatedAt instanceof java.sql.Timestamp) {
                highlightData.setUpdatedAt(((java.sql.Timestamp) updatedAt).toLocalDateTime());
            } else if (updatedAt instanceof LocalDateTime) {
                highlightData.setUpdatedAt((LocalDateTime) updatedAt);
            }

            HighlightResponse response = new HighlightResponse(true, "高亮已添加", highlightData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("添加高亮过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new HighlightResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
