
package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyBatchAction {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量操作生词请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("==========================");
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
    public static class BatchActionRequest {
        private String action;
        private List<Long> user_vocab_ids;
        private Map<String, Object> data;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public List<Long> getUser_vocab_ids() { return user_vocab_ids; }
        public void setUser_vocab_ids(List<Long> user_vocab_ids) { this.user_vocab_ids = user_vocab_ids; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    // 响应DTO
    public static class BatchActionResponse {
        private boolean success;
        private String message;
        private BatchResultData data;

        public BatchActionResponse(boolean success, String message, BatchResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchResultData getData() { return data; }
        public void setData(BatchResultData data) { this.data = data; }
    }

    public static class BatchResultData {
        private int processedCount;
        private int successCount;
        private int failedCount;
        private List<FailedItem> failedItems;

        public BatchResultData(int processedCount, int successCount, int failedCount, List<FailedItem> failedItems) {
            this.processedCount = processedCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.failedItems = failedItems;
        }

        public int getProcessedCount() { return processedCount; }
        public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

        public List<FailedItem> getFailedItems() { return failedItems; }
        public void setFailedItems(List<FailedItem> failedItems) { this.failedItems = failedItems; }
    }

    public static class FailedItem {
        private Long id;
        private String reason;

        public FailedItem(Long id, String reason) {
            this.id = id;
            this.reason = reason;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchActionResponse> batchVocabularyAction(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BatchActionRequest request) {

        printRequest(request);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchActionResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchActionResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 验证请求数据
            if (request.getAction() == null || request.getAction().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchActionResponse(false, "操作类型不能为空", null)
                );
            }

            if (request.getUser_vocab_ids() == null || request.getUser_vocab_ids().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchActionResponse(false, "生词ID列表不能为空", null)
                );
            }

            String action = request.getAction().toLowerCase();
            List<Long> ids = request.getUser_vocab_ids();

            // 3. 执行批量操作
            int successCount = 0;
            int failedCount = 0;
            List<FailedItem> failedItems = new ArrayList<>();

            for (Long id : ids) {
                try {
                    boolean success = false;

                    switch (action) {
                        case "delete":
                            success = executeDeleteAction(id, userId);
                            break;
                        case "mark_as_mastered":
                            success = executeMarkAsMasteredAction(id, userId);
                            break;
                        case "reset_learning":
                            success = executeResetLearningAction(id, userId);
                            break;
                        case "update_status":
                            success = executeUpdateStatusAction(id, userId, request.getData());
                            break;
                        default:
                            failedItems.add(new FailedItem(id, "不支持的操作类型: " + action));
                            failedCount++;
                            continue;
                    }

                    if (success) {
                        successCount++;
                    } else {
                        failedItems.add(new FailedItem(id, "操作失败"));
                        failedCount++;
                    }

                } catch (Exception e) {
                    failedItems.add(new FailedItem(id, "执行错误: " + e.getMessage()));
                    failedCount++;
                }
            }

            // 4. 创建结果数据
            BatchResultData resultData = new BatchResultData(
                    ids.size(),
                    successCount,
                    failedCount,
                    failedItems
            );

            printQueryResult("批量操作结果: " + resultData);

            // 5. 创建响应
            String message = String.format("批量操作完成，成功 %d 项，失败 %d 项", successCount, failedCount);
            BatchActionResponse response = new BatchActionResponse(true, message, resultData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量操作生词过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchActionResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private boolean executeDeleteAction(Long id, Long userId) {
        // 检查生词是否存在且属于当前用户
        String checkSql = "SELECT user_vocab_id FROM user_vocabulary WHERE user_vocab_id = ? AND user_id = ?";
        List<Map<String, Object>> items = jdbcTemplate.queryForList(checkSql, id, userId);

        if (items.isEmpty()) {
            return false;
        }

        // 删除标签关联
        String deleteTagsSql = "DELETE FROM user_vocabulary_tags WHERE user_vocab_id = ?";
        jdbcTemplate.update(deleteTagsSql, id);

        // 删除生词
        String deleteSql = "DELETE FROM user_vocabulary WHERE user_vocab_id = ? AND user_id = ?";
        int deletedRows = jdbcTemplate.update(deleteSql, id, userId);

        return deletedRows > 0;
    }

    private boolean executeMarkAsMasteredAction(Long id, Long userId) {
        String updateSql = "UPDATE user_vocabulary SET status = 'mastered', mastery_level = 5, updated_at = ? " +
                "WHERE user_vocab_id = ? AND user_id = ?";

        int updatedRows = jdbcTemplate.update(updateSql, LocalDateTime.now(), id, userId);
        return updatedRows > 0;
    }

    private boolean executeResetLearningAction(Long id, Long userId) {
        String updateSql = "UPDATE user_vocabulary SET status = 'new', mastery_level = 0, review_count = 0, " +
                "last_reviewed_at = NULL, next_review_at = NULL, updated_at = ? " +
                "WHERE user_vocab_id = ? AND user_id = ?";

        int updatedRows = jdbcTemplate.update(updateSql, LocalDateTime.now(), id, userId);
        return updatedRows > 0;
    }

    private boolean executeUpdateStatusAction(Long id, Long userId, Map<String, Object> data) {
        if (data == null || !data.containsKey("status")) {
            return false;
        }

        String status = (String) data.get("status");
        String updateSql = "UPDATE user_vocabulary SET status = ?, updated_at = ? WHERE user_vocab_id = ? AND user_id = ?";

        int updatedRows = jdbcTemplate.update(updateSql, status, LocalDateTime.now(), id, userId);
        return updatedRows > 0;
    }
}