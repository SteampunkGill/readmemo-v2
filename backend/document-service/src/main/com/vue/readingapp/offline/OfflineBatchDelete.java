
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineBatchDelete {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量删除离线文档请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
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
    public static class BatchDeleteRequest {
        private String[] offline_doc_ids;

        public String[] getOffline_doc_ids() { return offline_doc_ids; }
        public void setOffline_doc_ids(String[] offline_doc_ids) { this.offline_doc_ids = offline_doc_ids; }
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
        private int total;
        private int successCount;
        private int failCount;
        private List<DeleteResult> results;

        public BatchDeleteData(int total, int successCount, int failCount, List<DeleteResult> results) {
            this.total = total;
            this.successCount = successCount;
            this.failCount = failCount;
            this.results = results;
        }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }

        public List<DeleteResult> getResults() { return results; }
        public void setResults(List<DeleteResult> results) { this.results = results; }
    }

    public static class DeleteResult {
        private String offlineDocId;
        private boolean success;
        private String message;

        public DeleteResult(String offlineDocId, boolean success, String message) {
            this.offlineDocId = offlineDocId;
            this.success = success;
            this.message = message;
        }

        public String getOfflineDocId() { return offlineDocId; }
        public void setOfflineDocId(String offlineDocId) { this.offlineDocId = offlineDocId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
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

    @DeleteMapping("/documents/batch")
    public ResponseEntity<BatchDeleteResponse> batchDeleteOffline(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BatchDeleteRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchDeleteResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchDeleteResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getOffline_doc_ids() == null || request.getOffline_doc_ids().length == 0) {
                return ResponseEntity.badRequest().body(
                        new BatchDeleteResponse(false, "请选择要删除的离线文档", null)
                );
            }

            String[] offlineDocIds = request.getOffline_doc_ids();
            if (offlineDocIds.length > 100) {
                return ResponseEntity.badRequest().body(
                        new BatchDeleteResponse(false, "一次最多删除100个离线文档", null)
                );
            }

            // 3. 处理每个离线文档的删除
            List<DeleteResult> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (String offlineDocId : offlineDocIds) {
                try {
                    // 检查离线文档是否存在且属于该用户
                    String checkSql = "SELECT offline_doc_id FROM offline_documents WHERE offline_doc_id = ? AND user_id = ?";
                    List<Map<String, Object>> offlineDocs = jdbcTemplate.queryForList(checkSql, offlineDocId, userId);

                    if (offlineDocs.isEmpty()) {
                        results.add(new DeleteResult(offlineDocId, false, "离线文档不存在或无权删除"));
                        failCount++;
                        continue;
                    }

                    // 删除离线文档
                    String deleteSql = "DELETE FROM offline_documents WHERE offline_doc_id = ? AND user_id = ?";
                    int rowsAffected = jdbcTemplate.update(deleteSql, offlineDocId, userId);

                    if (rowsAffected > 0) {
                        // 同时删除相关的下载记录
                        String deleteDownloadsSql = "DELETE FROM offline_downloads WHERE offline_doc_id = ? AND user_id = ?";
                        jdbcTemplate.update(deleteDownloadsSql, offlineDocId, userId);

                        results.add(new DeleteResult(offlineDocId, true, "删除成功"));
                        successCount++;
                    } else {
                        results.add(new DeleteResult(offlineDocId, false, "删除失败"));
                        failCount++;
                    }

                } catch (Exception e) {
                    results.add(new DeleteResult(offlineDocId, false, "处理失败: " + e.getMessage()));
                    failCount++;
                }
            }

            // 4. 准备响应数据
            BatchDeleteData data = new BatchDeleteData(offlineDocIds.length, successCount, failCount, results);
            BatchDeleteResponse response = new BatchDeleteResponse(true,
                    String.format("批量删除完成，成功%d个，失败%d个", successCount, failCount), data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量删除离线文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchDeleteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}