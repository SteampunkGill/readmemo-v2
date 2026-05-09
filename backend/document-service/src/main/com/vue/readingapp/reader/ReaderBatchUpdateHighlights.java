package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderBatchUpdateHighlights {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量更新高亮请求 ===");
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

    // 请求DTO
    public static class BatchUpdateRequest {
        private List<Integer> highlightIds;
        private String color;
        private String note;

        public List<Integer> getHighlightIds() { return highlightIds; }
        public void setHighlightIds(List<Integer> highlightIds) { this.highlightIds = highlightIds; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    // 响应DTO
    public static class BatchUpdateResponse {
        private boolean success;
        private String message;
        private BatchUpdateData data;

        public BatchUpdateResponse(boolean success, String message, BatchUpdateData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchUpdateData getData() { return data; }
        public void setData(BatchUpdateData data) { this.data = data; }
    }

    public static class BatchUpdateData {
        private int updatedCount;
        private List<Integer> updatedIds;
        private String updatedAt;

        public BatchUpdateData() {
            this.updatedIds = new ArrayList<>();
        }

        public int getUpdatedCount() { return updatedCount; }
        public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }

        public List<Integer> getUpdatedIds() { return updatedIds; }
        public void setUpdatedIds(List<Integer> updatedIds) { this.updatedIds = updatedIds; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PutMapping("/documents/{documentId}/highlights/batch")
    public ResponseEntity<BatchUpdateResponse> batchUpdateHighlights(
            @PathVariable("documentId") int documentId,
            @RequestBody BatchUpdateRequest request,
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
                        new BatchUpdateResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchUpdateResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证请求数据
            if (request.getHighlightIds() == null || request.getHighlightIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateResponse(false, "高亮ID列表不能为空", null)
                );
            }

            // 检查是否有更新字段
            if (request.getColor() == null && request.getNote() == null) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateResponse(false, "没有提供更新数据", null)
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

            // 添加更新时间
            updateFields.add("updated_at = NOW()");

            // 4. 构建IN子句参数
            String placeholders = String.join(",", Collections.nCopies(request.getHighlightIds().size(), "?"));

            // 5. 构建完整SQL
            String updateSql = "UPDATE document_highlights SET " + String.join(", ", updateFields) +
                    " WHERE highlight_id IN (" + placeholders + ") " +
                    " AND document_id = ? AND user_id = ?";

            // 6. 构建参数列表
            List<Object> sqlParams = new ArrayList<>(updateParams);
            sqlParams.addAll(request.getHighlightIds());
            sqlParams.add(documentId);
            sqlParams.add(userId);

            // 7. 执行批量更新
            int updatedRows = jdbcTemplate.update(updateSql, sqlParams.toArray());
            printQueryResult("更新了 " + updatedRows + " 条记录");

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new BatchUpdateResponse(false, "没有找到匹配的高亮记录或没有权限修改", null)
                );
            }

            // 8. 获取更新后的高亮ID列表（验证哪些真的被更新了）
            String checkSql = "SELECT highlight_id FROM document_highlights " +
                    "WHERE highlight_id IN (" + placeholders + ") " +
                    "AND document_id = ? AND user_id = ?";

            List<Object> checkParams = new ArrayList<>(request.getHighlightIds());
            checkParams.add(documentId);
            checkParams.add(userId);

            List<Map<String, Object>> updatedHighlights = jdbcTemplate.queryForList(checkSql, checkParams.toArray());
            List<Integer> actuallyUpdatedIds = new ArrayList<>();

            for (Map<String, Object> row : updatedHighlights) {
                actuallyUpdatedIds.add(((Number) row.get("highlight_id")).intValue());
            }

            // 9. 构建响应数据
            BatchUpdateData batchData = new BatchUpdateData();
            batchData.setUpdatedCount(updatedRows);
            batchData.setUpdatedIds(actuallyUpdatedIds);
            batchData.setUpdatedAt(new Date().toString());

            BatchUpdateResponse response = new BatchUpdateResponse(true, "批量更新高亮成功", batchData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量更新高亮过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchUpdateResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}