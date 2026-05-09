package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsGetProcessingStatus {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Integer documentId, String authHeader) {
        System.out.println("=== 收到获取文档处理状态请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("认证头: " + authHeader);
        System.out.println("============================");
    }

    // 打印查询结果
    private void printQueryResult(Map<String, Object> status) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("处理状态: " + status);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(ProcessingStatusResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应DTO
    public static class ProcessingStatusResponse {
        private boolean success;
        private String message;
        private ProcessingStatusData data;

        public ProcessingStatusResponse(boolean success, String message, ProcessingStatusData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ProcessingStatusData getData() { return data; }
        public void setData(ProcessingStatusData data) { this.data = data; }
    }

    public static class ProcessingStatusData {
        private ProcessingStatusDTO status;

        public ProcessingStatusData(ProcessingStatusDTO status) {
            this.status = status;
        }

        public ProcessingStatusDTO getStatus() { return status; }
        public void setStatus(ProcessingStatusDTO status) { this.status = status; }
    }

    public static class ProcessingStatusDTO {
        private Integer documentId;
        private String status;
        private Integer progress;
        private String errorMessage;
        private String startedAt;
        private String completedAt;
        private String estimatedCompletionTime;

        // Getters and Setters
        public Integer getDocumentId() { return documentId; }
        public void setDocumentId(Integer documentId) { this.documentId = documentId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getStartedAt() { return startedAt; }
        public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

        public String getEstimatedCompletionTime() { return estimatedCompletionTime; }
        public void setEstimatedCompletionTime(String estimatedCompletionTime) { this.estimatedCompletionTime = estimatedCompletionTime; }
    }

    @GetMapping("/{documentId}/processing-status")
    public ResponseEntity<ProcessingStatusResponse> getDocumentProcessingStatus(
            @PathVariable("documentId") Integer documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ProcessingStatusResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ProcessingStatusResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 检查文档是否存在且属于当前用户
            String checkSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ProcessingStatusResponse(false, "文档不存在或没有权限", null)
                );
            }

            // 3. 查询文档处理状态
            String statusSql = "SELECT d.status, d.processing_progress, d.processing_error, " +
                    "d.processing_started_at, d.processing_completed_at, " +
                    "q.status as queue_status, q.priority, q.created_at as queue_created_at " +
                    "FROM documents d " +
                    "LEFT JOIN document_processing_queue q ON d.document_id = q.document_id " +
                    "WHERE d.document_id = ? AND d.user_id = ?";

            List<Map<String, Object>> statusResults = jdbcTemplate.queryForList(statusSql, documentId, userId);

            if (statusResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ProcessingStatusResponse(false, "文档处理状态不存在", null)
                );
            }

            Map<String, Object> status = statusResults.get(0);
            printQueryResult(status);

            // 4. 构建响应数据
            ProcessingStatusDTO dto = new ProcessingStatusDTO();
            dto.setDocumentId(documentId);
            dto.setStatus((String) status.get("status"));
            dto.setProgress((Integer) status.get("processing_progress"));
            dto.setErrorMessage((String) status.get("processing_error"));

            // 格式化日期
            java.sql.Timestamp startedAt = (java.sql.Timestamp) status.get("processing_started_at");
            dto.setStartedAt(startedAt != null ? startedAt.toLocalDateTime().toString() : null);

            java.sql.Timestamp completedAt = (java.sql.Timestamp) status.get("processing_completed_at");
            dto.setCompletedAt(completedAt != null ? completedAt.toLocalDateTime().toString() : null);

            // 估算完成时间（简单逻辑：如果进度>0，估算剩余时间）
            Integer progress = dto.getProgress();
            if (progress != null && progress > 0 && progress < 100) {
                // 假设每1%进度需要1秒（实际应根据历史数据计算）
                int remainingPercent = 100 - progress;
                dto.setEstimatedCompletionTime(remainingPercent + "秒");
            } else {
                dto.setEstimatedCompletionTime(null);
            }

            ProcessingStatusData data = new ProcessingStatusData(dto);
            ProcessingStatusResponse response = new ProcessingStatusResponse(true, "获取文档处理状态成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取文档处理状态过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ProcessingStatusResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}