package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderUpdateHighlight {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新高亮请求 ===");
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
    public static class UpdateHighlightRequest {
        private String color;
        private String note;

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    // 响应DTO
    public static class UpdateHighlightResponse {
        private boolean success;
        private String message;
        private HighlightData data;

        public UpdateHighlightResponse(boolean success, String message, HighlightData data) {
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
        private String createdAt;
        private String updatedAt;

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

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/documents/{documentId}/highlights/{highlightId}")
    public ResponseEntity<UpdateHighlightResponse> updateHighlight(
            @PathVariable("documentId") int documentId,
            @PathVariable("highlightId") int highlightId,
            @RequestBody UpdateHighlightRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("highlightId", highlightId);
        requestInfo.put("request", request);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateHighlightResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateHighlightResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证高亮是否存在且属于当前用户
            String highlightSql = "SELECT * FROM document_highlights WHERE highlight_id = ? AND document_id = ? AND user_id = ?";
            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(highlightSql, highlightId, documentId, userId);

            if (highlights.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateHighlightResponse(false, "高亮不存在或没有权限修改", null)
                );
            }

            // 3. 构建更新字段
            List<String> updateFields = new ArrayList<>();
            List<Object> updateParams = new ArrayList<>();

            if (request.getColor() != null) {
                updateFields.add("color = ?");
                updateParams.add(request.getColor());
            }

            if (request.getNote() != null) {
                updateFields.add("note = ?");
                updateParams.add(request.getNote().trim());
            }

            // 如果没有要更新的字段
            if (updateFields.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdateHighlightResponse(false, "没有提供更新数据", null)
                );
            }

            // 添加更新时间
            updateFields.add("updated_at = NOW()");

            // 添加WHERE条件参数
            updateParams.add(highlightId);
            updateParams.add(documentId);
            updateParams.add(userId);

            // 4. 执行更新
            String updateSql = "UPDATE document_highlights SET " + String.join(", ", updateFields) +
                    " WHERE highlight_id = ? AND document_id = ? AND user_id = ?";

            int updatedRows = jdbcTemplate.update(updateSql, updateParams.toArray());

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateHighlightResponse(false, "更新高亮失败", null)
                );
            }

            // 5. 获取更新后的高亮数据
            String getUpdatedSql = "SELECT * FROM document_highlights WHERE highlight_id = ?";
            List<Map<String, Object>> updatedHighlights = jdbcTemplate.queryForList(getUpdatedSql, highlightId);
            printQueryResult(updatedHighlights);

            if (updatedHighlights.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new UpdateHighlightResponse(false, "获取更新后的高亮数据失败", null)
                );
            }

            Map<String, Object> updatedHighlight = updatedHighlights.get(0);

            // 6. 构建响应数据
            HighlightData highlightData = new HighlightData();
            highlightData.setId(highlightId);
            highlightData.setDocumentId(documentId);
            highlightData.setText((String) updatedHighlight.get("text"));
            highlightData.setPage(((Number) updatedHighlight.get("page")).intValue());
            highlightData.setColor((String) updatedHighlight.get("color"));
            highlightData.setNote((String) updatedHighlight.get("note"));
            highlightData.setCreatedAt(updatedHighlight.get("created_at").toString());
            highlightData.setUpdatedAt(updatedHighlight.get("updated_at").toString());

            // 解析position JSON
            if (updatedHighlight.get("position") != null) {
                try {
                    String posStr = (String) updatedHighlight.get("position");
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

            UpdateHighlightResponse response = new UpdateHighlightResponse(true, "高亮已更新", highlightData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("更新高亮过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateHighlightResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}