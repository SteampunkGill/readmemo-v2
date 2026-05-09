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
public class DocumentsBatchDocumentAction {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(String action, BatchActionRequest request, String authHeader) {
        System.out.println("=== 收到批量操作文档请求 ===");
        System.out.println("操作类型: " + action);
        System.out.println("请求数据: " + request);
        System.out.println("认证头: " + authHeader);
        System.out.println("=========================");
    }

    // 打印查询结果
    private void printQueryResult(int processedCount, int successCount, List<Integer> failedIds) {
        System.out.println("=== 批量操作结果 ===");
        System.out.println("处理总数: " + processedCount);
        System.out.println("成功数量: " + successCount);
        System.out.println("失败数量: " + failedIds.size());
        System.out.println("失败ID列表: " + failedIds);
        System.out.println("==================");
    }

    // 打印返回数据
    private void printResponse(BatchActionResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class BatchActionRequest {
        private List<Integer> ids;
        private Map<String, Object> data;

        public List<Integer> getIds() { return ids; }
        public void setIds(List<Integer> ids) { this.ids = ids; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    // 响应DTO
    public static class BatchActionResponse {
        private boolean success;
        private String message;
        private BatchActionData data;

        public BatchActionResponse(boolean success, String message, BatchActionData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchActionData getData() { return data; }
        public void setData(BatchActionData data) { this.data = data; }
    }

    public static class BatchActionData {
        private Integer processedCount;
        private Integer successCount;
        private Integer failedCount;
        private List<Integer> failedIds;
        private List<Map<String, Object>> failedItems;

        public BatchActionData(Integer processedCount, Integer successCount, Integer failedCount,
                               List<Integer> failedIds, List<Map<String, Object>> failedItems) {
            this.processedCount = processedCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.failedIds = failedIds;
            this.failedItems = failedItems;
        }

        // Getters and Setters
        public Integer getProcessedCount() { return processedCount; }
        public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }

        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

        public Integer getFailedCount() { return failedCount; }
        public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }

        public List<Integer> getFailedIds() { return failedIds; }
        public void setFailedIds(List<Integer> failedIds) { this.failedIds = failedIds; }

        public List<Map<String, Object>> getFailedItems() { return failedItems; }
        public void setFailedItems(List<Map<String, Object>> failedItems) { this.failedItems = failedItems; }
    }

    @PostMapping("/batch-action")
    public ResponseEntity<BatchActionResponse> batchDocumentAction(
            @RequestParam("action") String action,
            @RequestBody BatchActionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(action, request, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchActionResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchActionResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证请求数据
            if (action == null || action.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchActionResponse(false, "操作类型不能为空", null)
                );
            }

            if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchActionResponse(false, "文档ID列表不能为空", null)
                );
            }

            List<Integer> documentIds = request.getIds();
            List<Integer> failedIds = new ArrayList<>();
            List<Map<String, Object>> failedItems = new ArrayList<>();
            int successCount = 0;

            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            // 3. 根据操作类型执行批量操作
            switch (action.toLowerCase()) {
                case "update":
                    successCount = batchUpdateAction(userId, documentIds, request.getData(), timestamp, failedIds, failedItems);
                    break;

                case "delete":
                    successCount = batchDeleteAction(userId, documentIds, timestamp, failedIds, failedItems);
                    break;

                case "favorite":
                    successCount = batchFavoriteAction(userId, documentIds, true, timestamp, failedIds, failedItems);
                    break;

                case "unfavorite":
                    successCount = batchFavoriteAction(userId, documentIds, false, timestamp, failedIds, failedItems);
                    break;

                case "public":
                    successCount = batchPublicAction(userId, documentIds, true, timestamp, failedIds, failedItems);
                    break;

                case "private":
                    successCount = batchPublicAction(userId, documentIds, false, timestamp, failedIds, failedItems);
                    break;

                default:
                    return ResponseEntity.badRequest().body(
                            new BatchActionResponse(false, "不支持的操作类型: " + action, null)
                    );
            }

            // 4. 打印查询结果
            printQueryResult(documentIds.size(), successCount, failedIds);

            // 5. 构建响应数据
            BatchActionData data = new BatchActionData(
                    documentIds.size(),
                    successCount,
                    failedIds.size(),
                    failedIds,
                    failedItems
            );

            String message;
            if (failedIds.isEmpty()) {
                message = "成功对 " + successCount + " 个文档执行 " + action + " 操作";
            } else {
                message = "成功对 " + successCount + " 个文档执行 " + action + " 操作，" + failedIds.size() + " 个文档操作失败";
            }

            BatchActionResponse response = new BatchActionResponse(true, message, data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量操作文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchActionResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 批量更新操作
    private int batchUpdateAction(Integer userId, List<Integer> documentIds, Map<String, Object> data,
                                  Timestamp timestamp, List<Integer> failedIds, List<Map<String, Object>> failedItems) {
        int successCount = 0;

        if (data == null || data.isEmpty()) {
            // 如果没有更新数据，直接返回
            return 0;
        }

        for (Integer documentId : documentIds) {
            try {
                // 检查文档是否存在且属于当前用户
                String checkSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
                List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

                if (documents.isEmpty()) {
                    failedIds.add(documentId);
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "文档不存在或没有权限");
                    failedItems.add(failedItem);
                    continue;
                }

                // 构建更新SQL
                StringBuilder updateSql = new StringBuilder("UPDATE documents SET ");
                List<Object> params = new ArrayList<>();

                // 处理标题更新
                if (data.containsKey("title")) {
                    updateSql.append("title = ?, ");
                    params.add(data.get("title"));
                }

                // 处理描述更新
                if (data.containsKey("description")) {
                    updateSql.append("description = ?, ");
                    params.add(data.get("description"));
                }

                // 处理语言更新
                if (data.containsKey("language")) {
                    updateSql.append("language = ?, ");
                    params.add(data.get("language"));
                }

                // 处理公开状态更新
                if (data.containsKey("isPublic")) {
                    updateSql.append("is_public = ?, ");
                    params.add(data.get("isPublic"));
                }

                // 处理收藏状态更新
                if (data.containsKey("isFavorite")) {
                    updateSql.append("is_favorite = ?, ");
                    params.add(data.get("isFavorite"));
                }

                // 添加更新时间
                updateSql.append("updated_at = ? ");
                params.add(timestamp);

                updateSql.append("WHERE document_id = ? AND user_id = ?");
                params.add(documentId);
                params.add(userId);

                // 执行更新
                int rowsUpdated = jdbcTemplate.update(updateSql.toString(), params.toArray());

                if (rowsUpdated > 0) {
                    successCount++;
                } else {
                    failedIds.add(documentId);
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "更新失败");
                    failedItems.add(failedItem);
                }

            } catch (Exception e) {
                System.err.println("更新文档 " + documentId + " 时发生错误: " + e.getMessage());
                failedIds.add(documentId);
                Map<String, Object> failedItem = new HashMap<>();
                failedItem.put("id", documentId);
                failedItem.put("reason", e.getMessage());
                failedItems.add(failedItem);
            }
        }

        return successCount;
    }

    // 批量删除操作
    private int batchDeleteAction(Integer userId, List<Integer> documentIds, Timestamp timestamp,
                                  List<Integer> failedIds, List<Map<String, Object>> failedItems) {
        int successCount = 0;

        for (Integer documentId : documentIds) {
            try {
                // 检查文档是否存在且属于当前用户
                String checkSql = "SELECT document_id, title FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
                List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

                if (documents.isEmpty()) {
                    failedIds.add(documentId);
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "文档不存在或没有权限");
                    failedItems.add(failedItem);
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
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "删除失败");
                    failedItems.add(failedItem);
                }

            } catch (Exception e) {
                System.err.println("删除文档 " + documentId + " 时发生错误: " + e.getMessage());
                failedIds.add(documentId);
                Map<String, Object> failedItem = new HashMap<>();
                failedItem.put("id", documentId);
                failedItem.put("reason", e.getMessage());
                failedItems.add(failedItem);
            }
        }

        return successCount;
    }

    // 批量收藏操作
    private int batchFavoriteAction(Integer userId, List<Integer> documentIds, boolean favorite,
                                    Timestamp timestamp, List<Integer> failedIds, List<Map<String, Object>> failedItems) {
        int successCount = 0;

        for (Integer documentId : documentIds) {
            try {
                // 检查文档是否存在且属于当前用户
                String checkSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
                List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

                if (documents.isEmpty()) {
                    failedIds.add(documentId);
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "文档不存在或没有权限");
                    failedItems.add(failedItem);
                    continue;
                }

                // 更新收藏状态
                String updateSql = "UPDATE documents SET is_favorite = ?, updated_at = ? WHERE document_id = ? AND user_id = ?";
                int rowsUpdated = jdbcTemplate.update(updateSql, favorite, timestamp, documentId, userId);

                if (rowsUpdated > 0) {
                    successCount++;
                } else {
                    failedIds.add(documentId);
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "更新收藏状态失败");
                    failedItems.add(failedItem);
                }

            } catch (Exception e) {
                System.err.println("更新文档 " + documentId + " 收藏状态时发生错误: " + e.getMessage());
                failedIds.add(documentId);
                Map<String, Object> failedItem = new HashMap<>();
                failedItem.put("id", documentId);
                failedItem.put("reason", e.getMessage());
                failedItems.add(failedItem);
            }
        }

        return successCount;
    }

    // 批量公开操作
    private int batchPublicAction(Integer userId, List<Integer> documentIds, boolean isPublic,
                                  Timestamp timestamp, List<Integer> failedIds, List<Map<String, Object>> failedItems) {
        int successCount = 0;

        for (Integer documentId : documentIds) {
            try {
                // 检查文档是否存在且属于当前用户
                String checkSql = "SELECT document_id FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
                List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

                if (documents.isEmpty()) {
                    failedIds.add(documentId);
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "文档不存在或没有权限");
                    failedItems.add(failedItem);
                    continue;
                }

                // 更新公开状态
                String updateSql = "UPDATE documents SET is_public = ?, updated_at = ? WHERE document_id = ? AND user_id = ?";
                int rowsUpdated = jdbcTemplate.update(updateSql, isPublic, timestamp, documentId, userId);

                if (rowsUpdated > 0) {
                    successCount++;
                } else {
                    failedIds.add(documentId);
                    Map<String, Object> failedItem = new HashMap<>();
                    failedItem.put("id", documentId);
                    failedItem.put("reason", "更新公开状态失败");
                    failedItems.add(failedItem);
                }

            } catch (Exception e) {
                System.err.println("更新文档 " + documentId + " 公开状态时发生错误: " + e.getMessage());
                failedIds.add(documentId);
                Map<String, Object> failedItem = new HashMap<>();
                failedItem.put("id", documentId);
                failedItem.put("reason", e.getMessage());
                failedItems.add(failedItem);
            }
        }

        return successCount;
    }
}