package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tags/vocabulary/batch")
public class TagBatchVocabularyTags {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量生词标签操作请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class BatchVocabularyTagsRequest {
        private List<String> tagIds;
        private String operation; // delete, update, etc.
        private Map<String, Object> data;

        // Getters and Setters
        public List<String> getTagIds() { return tagIds; }
        public void setTagIds(List<String> tagIds) { this.tagIds = tagIds; }

        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    // 响应类
    public static class BatchVocabularyTagsResponse {
        private boolean success;
        private String message;
        private int processedCount;
        private int failedCount;

        public BatchVocabularyTagsResponse(boolean success, String message, int processedCount, int failedCount) {
            this.success = success;
            this.message = message;
            this.processedCount = processedCount;
            this.failedCount = failedCount;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getProcessedCount() { return processedCount; }
        public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }

        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    }

    @PostMapping("/")
    public ResponseEntity<BatchVocabularyTagsResponse> batchVocabularyTags(@RequestBody BatchVocabularyTagsRequest request) {
        printRequest(request);

        try {
            // 验证请求数据
            if (request.getTagIds() == null || request.getTagIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchVocabularyTagsResponse(false, "标签ID列表不能为空", 0, 0)
                );
            }

            if (request.getOperation() == null || request.getOperation().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchVocabularyTagsResponse(false, "操作类型不能为空", 0, 0)
                );
            }

            int processedCount = 0;
            int failedCount = 0;

            // 根据操作类型处理
            switch (request.getOperation().toLowerCase()) {
                case "delete":
                    // 批量删除标签
                    for (String tagId : request.getTagIds()) {
                        try {
                            // 检查标签是否被使用
                            String usageCheckSql = "SELECT COUNT(*) FROM user_vocabulary_tags WHERE tag_id = ?";
                            int usageCount = jdbcTemplate.queryForObject(usageCheckSql, Integer.class, Integer.parseInt(tagId));

                            if (usageCount > 0) {
                                failedCount++;
                                continue;
                            }

                            // 删除标签
                            String deleteSql = "DELETE FROM vocabulary_tags WHERE tag_id = ?";
                            int deleted = jdbcTemplate.update(deleteSql, Integer.parseInt(tagId));

                            if (deleted > 0) {
                                processedCount++;
                            } else {
                                failedCount++;
                            }
                        } catch (Exception e) {
                            failedCount++;
                            System.err.println("删除标签 " + tagId + " 时发生错误: " + e.getMessage());
                        }
                    }
                    break;

                case "update":
                    // 批量更新标签
                    if (request.getData() == null) {
                        return ResponseEntity.badRequest().body(
                                new BatchVocabularyTagsResponse(false, "更新数据不能为空", 0, 0)
                        );
                    }

                    String newName = (String) request.getData().get("name");
                    String newColor = (String) request.getData().get("color");
                    String newDescription = (String) request.getData().get("description");

                    for (String tagId : request.getTagIds()) {
                        try {
                            // 检查标签是否存在
                            String checkSql = "SELECT COUNT(*) FROM vocabulary_tags WHERE tag_id = ?";
                            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, Integer.parseInt(tagId));

                            if (count == 0) {
                                failedCount++;
                                continue;
                            }

                            // 构建更新SQL
                            StringBuilder updateSqlBuilder = new StringBuilder("UPDATE vocabulary_tags SET ");
                            java.util.List<Object> params = new java.util.ArrayList<>();

                            if (newName != null) {
                                updateSqlBuilder.append("tag_name = ?, ");
                                params.add(newName);
                            }

                            if (newColor != null) {
                                updateSqlBuilder.append("color = ?, ");
                                params.add(newColor);
                            }

                            if (newDescription != null) {
                                updateSqlBuilder.append("description = ?, ");
                                params.add(newDescription);
                            }

                            updateSqlBuilder.append("updated_at = NOW() WHERE tag_id = ?");
                            params.add(Integer.parseInt(tagId));

                            int updated = jdbcTemplate.update(updateSqlBuilder.toString(), params.toArray());

                            if (updated > 0) {
                                processedCount++;
                            } else {
                                failedCount++;
                            }
                        } catch (Exception e) {
                            failedCount++;
                            System.err.println("更新标签 " + tagId + " 时发生错误: " + e.getMessage());
                        }
                    }
                    break;

                default:
                    return ResponseEntity.badRequest().body(
                            new BatchVocabularyTagsResponse(false, "不支持的操作类型: " + request.getOperation(), 0, 0)
                    );
            }

            // 创建响应
            String message = String.format("批量操作完成，成功处理 %d 个标签，失败 %d 个", processedCount, failedCount);
            BatchVocabularyTagsResponse response = new BatchVocabularyTagsResponse(
                    true, message, processedCount, failedCount);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量生词标签操作过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchVocabularyTagsResponse(false, "服务器内部错误: " + e.getMessage(), 0, 0)
            );
        }
    }
}
