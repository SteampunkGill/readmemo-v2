
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineBatchDownload {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量下载文档到离线请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("===============================");
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
    public static class BatchDownloadRequest {
        private String[] document_ids;

        public String[] getDocument_ids() { return document_ids; }
        public void setDocument_ids(String[] document_ids) { this.document_ids = document_ids; }
    }

    // 响应DTO
    public static class BatchDownloadResponse {
        private boolean success;
        private String message;
        private BatchDownloadData data;

        public BatchDownloadResponse(boolean success, String message, BatchDownloadData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchDownloadData getData() { return data; }
        public void setData(BatchDownloadData data) { this.data = data; }
    }

    public static class BatchDownloadData {
        private String batchId;
        private int total;
        private int successCount;
        private int failCount;
        private List<DownloadResult> results;

        public BatchDownloadData(String batchId, int total, int successCount, int failCount, List<DownloadResult> results) {
            this.batchId = batchId;
            this.total = total;
            this.successCount = successCount;
            this.failCount = failCount;
            this.results = results;
        }

        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }

        public List<DownloadResult> getResults() { return results; }
        public void setResults(List<DownloadResult> results) { this.results = results; }
    }

    public static class DownloadResult {
        private String documentId;
        private boolean success;
        private String message;
        private String offlineDocId;
        private String downloadId;

        public DownloadResult(String documentId, boolean success, String message,
                              String offlineDocId, String downloadId) {
            this.documentId = documentId;
            this.success = success;
            this.message = message;
            this.offlineDocId = offlineDocId;
            this.downloadId = downloadId;
        }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getOfflineDocId() { return offlineDocId; }
        public void setOfflineDocId(String offlineDocId) { this.offlineDocId = offlineDocId; }

        public String getDownloadId() { return downloadId; }
        public void setDownloadId(String downloadId) { this.downloadId = downloadId; }
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

    @PostMapping("/documents/batch-download")
    public ResponseEntity<BatchDownloadResponse> batchDownloadOffline(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BatchDownloadRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchDownloadResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchDownloadResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getDocument_ids() == null || request.getDocument_ids().length == 0) {
                return ResponseEntity.badRequest().body(
                        new BatchDownloadResponse(false, "请选择要下载的文档", null)
                );
            }

            String[] documentIds = request.getDocument_ids();
            if (documentIds.length > 100) {
                return ResponseEntity.badRequest().body(
                        new BatchDownloadResponse(false, "一次最多下载100个文档", null)
                );
            }

            // 3. 生成批量ID
            String batchId = "batch_" + UUID.randomUUID().toString();

            // 4. 处理每个文档的下载
            List<DownloadResult> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (String documentId : documentIds) {
                try {
                    // 检查文档是否存在
                    String checkDocSql = "SELECT document_id, title FROM documents WHERE document_id = ?";
                    List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkDocSql, documentId);

                    if (documents.isEmpty()) {
                        results.add(new DownloadResult(documentId, false, "文档不存在", null, null));
                        failCount++;
                        continue;
                    }

                    Map<String, Object> document = documents.get(0);
                    String title = (String) document.get("title");

                    // 检查是否已经离线下载过
                    String checkOfflineSql = "SELECT offline_doc_id FROM offline_documents WHERE document_id = ? AND user_id = ?";
                    List<Map<String, Object>> existingOffline = jdbcTemplate.queryForList(checkOfflineSql, documentId, userId);

                    String offlineDocId;
                    if (!existingOffline.isEmpty()) {
                        // 已经存在，更新版本
                        offlineDocId = (String) existingOffline.get(0).get("offline_doc_id");
                        String updateSql = "UPDATE offline_documents SET offline_version = offline_version + 1, " +
                                "updated_at = NOW() WHERE offline_doc_id = ?";
                        jdbcTemplate.update(updateSql, offlineDocId);
                    } else {
                        // 创建新的离线文档记录
                        offlineDocId = "offline_doc_" + UUID.randomUUID().toString();
                        String insertSql = "INSERT INTO offline_documents (offline_doc_id, document_id, user_id, title, " +
                                "download_url, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
                        jdbcTemplate.update(insertSql, offlineDocId, documentId, userId, title,
                                "/api/offline/documents/" + offlineDocId + "/download");
                    }

                    // 创建下载任务
                    String downloadId = "dl_" + UUID.randomUUID().toString();
                    String insertDownloadSql = "INSERT INTO offline_downloads (download_id, user_id, document_id, " +
                            "offline_doc_id, status, progress, start_time, batch_id) " +
                            "VALUES (?, ?, ?, ?, 'pending', 0, NOW(), ?)";
                    jdbcTemplate.update(insertDownloadSql, downloadId, userId, documentId, offlineDocId, batchId);

                    results.add(new DownloadResult(documentId, true, "下载任务已创建", offlineDocId, downloadId));
                    successCount++;

                    // 启动异步下载任务（简化处理）
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            String updateSql = "UPDATE offline_downloads SET status = 'completed', progress = 100, " +
                                    "end_time = NOW() WHERE download_id = ?";
                            jdbcTemplate.update(updateSql, downloadId);

                            String updateOfflineSql = "UPDATE offline_documents SET is_synced = TRUE, updated_at = NOW() " +
                                    "WHERE offline_doc_id = ?";
                            jdbcTemplate.update(updateOfflineSql, offlineDocId);

                        } catch (Exception e) {
                            System.err.println("下载任务执行失败: " + e.getMessage());
                        }
                    }).start();

                } catch (Exception e) {
                    results.add(new DownloadResult(documentId, false, "处理失败: " + e.getMessage(), null, null));
                    failCount++;
                }
            }

            // 5. 准备响应数据
            BatchDownloadData data = new BatchDownloadData(batchId, documentIds.length, successCount, failCount, results);
            BatchDownloadResponse response = new BatchDownloadResponse(true,
                    String.format("批量下载任务已提交，成功%d个，失败%d个", successCount, failCount), data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量下载文档到离线过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchDownloadResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}