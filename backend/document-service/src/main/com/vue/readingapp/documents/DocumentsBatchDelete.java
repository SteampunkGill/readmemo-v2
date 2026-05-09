package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsBatchDelete {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(BatchDeleteRequest request) {
        System.out.println("=== 收到批量删除文档请求 ===");
        System.out.println("文档ID列表: " + request.getDocumentIds());
        System.out.println("=========================");
    }

    // 打印查询结果
    private void printQueryResult(int deletedCount, List<Integer> failedIds) {
        System.out.println("=== 批量删除结果 ===");
        System.out.println("成功删除数量: " + deletedCount);
        System.out.println("失败文档ID: " + failedIds);
        System.out.println("==================");
    }

    // 打印返回数据
    private void printResponse(BatchDeleteResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class BatchDeleteRequest {
        private List<Integer> documentIds;

        public List<Integer> getDocumentIds() { return documentIds; }
        public void setDocumentIds(List<Integer> documentIds) { this.documentIds = documentIds; }
    }

    // 响应DTO
    public static class BatchDeleteResponse {
        private boolean success;
        private String message;
        private BatchDeleteData data;

        public BatchDeleteResponse(boolean success, String message, BatchDeleteData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchDeleteData getData() { return data; }
        public void setData(BatchDeleteData data) { this.data = data; }
    }

    public static class BatchDeleteData {
        private Integer processedCount;
        private Integer successCount;
        private Integer failedCount;
        private List<Integer> failedIds;

        public BatchDeleteData(Integer processedCount, Integer successCount, Integer failedCount, List<Integer> failedIds) {
            this.processedCount = processedCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.failedIds = failedIds;
        }

        public Integer getProcessedCount() { return processedCount; }
        public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }

        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

        public Integer getFailedCount() { return failedCount; }
        public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }

        public List<Integer> getFailedIds() { return failedIds; }
        public void setFailedIds(List<Integer> failedIds) { this.failedIds = failedIds; }
    }

    @DeleteMapping("/batch")
    public ResponseEntity<BatchDeleteResponse> batchDeleteDocuments(
            @RequestBody BatchDeleteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(request);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchDeleteResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchDeleteResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证请求数据
            if (request == null || request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchDeleteResponse(false, "文档ID列表不能为空", null)
                );
            }

            List<Integer> documentIds = request.getDocumentIds();
            List<Integer> failedIds = new ArrayList<>();
            int successCount = 0;

            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            // 3. 批量删除每个文档
            for (Integer documentId : documentIds) {
                try {
                    // 检查文档是否存在且属于当前用户
                    String checkSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
                    List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

                    if (documents.isEmpty()) {
                        failedIds.add(documentId);
                        continue;
                    }

                    // 执行软删除
                    String deleteSql = "UPDATE documents SET deleted_at = ?, updated_at = ? WHERE document_id = ? AND user_id = ?";
                    int rowsDeleted = jdbcTemplate.update(deleteSql, timestamp, timestamp, documentId, userId);

                    if (rowsDeleted > 0) {
                        successCount++;

                        // 从处理队列中移除
                        String deleteQueueSql = "DELETE FROM document_processing_queue WHERE document_id = ?";
                        jdbcTemplate.update(deleteQueueSql, documentId);
                    } else {
                        failedIds.add(documentId);
                    }

                } catch (Exception e) {
                    System.err.println("删除文档 " + documentId + " 时发生错误: " + e.getMessage());
                    failedIds.add(documentId);
                }
            }

            // 4. 打印查询结果
            printQueryResult(successCount, failedIds);

            // 5. 构建响应数据
            BatchDeleteData data = new BatchDeleteData(
                    documentIds.size(),
                    successCount,
                    failedIds.size(),
                    failedIds
            );

            String message;
            if (failedIds.isEmpty()) {
                message = "成功删除 " + successCount + " 个文档";
            } else {
                message = "成功删除 " + successCount + " 个文档，" + failedIds.size() + " 个文档删除失败";
            }

            BatchDeleteResponse response = new BatchDeleteResponse(true, message, data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量删除文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchDeleteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}