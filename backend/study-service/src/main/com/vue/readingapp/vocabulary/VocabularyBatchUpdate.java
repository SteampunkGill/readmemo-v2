package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyBatchUpdate {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到批量更新生词请求 ===");
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
    public static class BatchUpdateRequest {
        private List<BatchUpdateItem> items;

        public List<BatchUpdateItem> getItems() { return items; }
        public void setItems(List<BatchUpdateItem> items) { this.items = items; }
    }

    public static class BatchUpdateItem {
        private Long id;
        private String status;
        private Integer masteryLevel;
        private List<String> tags;
        private String notes;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(Integer masteryLevel) { this.masteryLevel = masteryLevel; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // 响应DTO
    public static class BatchUpdateResponse {
        private boolean success;
        private String message;
        private BatchUpdateResultData data;

        public BatchUpdateResponse(boolean success, String message, BatchUpdateResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BatchUpdateResultData getData() { return data; }
        public void setData(BatchUpdateResultData data) { this.data = data; }
    }

    public static class BatchUpdateResultData {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private List<BatchUpdateResultItem> results;

        public BatchUpdateResultData(int totalCount, int successCount, int failedCount, List<BatchUpdateResultItem> results) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.results = results;
        }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

        public List<BatchUpdateResultItem> getResults() { return results; }
        public void setResults(List<BatchUpdateResultItem> results) { this.results = results; }
    }

    public static class BatchUpdateResultItem {
        private Long id;
        private boolean success;
        private String message;

        public BatchUpdateResultItem(Long id, boolean success, String message) {
            this.id = id;
            this.success = success;
            this.message = message;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @PutMapping("/batch")
    public ResponseEntity<BatchUpdateResponse> batchUpdateVocabulary(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BatchUpdateRequest request) {

        printRequest(request);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchUpdateResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BatchUpdateResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 验证请求数据
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateResponse(false, "更新项列表不能为空", null)
                );
            }

            if (request.getItems().size() > 100) {
                return ResponseEntity.badRequest().body(
                        new BatchUpdateResponse(false, "批量更新最多支持100项", null)
                );
            }

            // 3. 执行批量更新
            List<BatchUpdateResultItem> results = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;

            for (BatchUpdateItem updateItem : request.getItems()) {
                try {
                    if (updateItem.getId() == null) {
                        results.add(new BatchUpdateResultItem(null, false, "ID不能为空"));
                        failedCount++;
                        continue;
                    }

                    // 检查生词是否存在且属于当前用户
                    String checkSql = "SELECT user_vocab_id FROM user_vocabulary WHERE user_vocab_id = ? AND user_id = ?";
                    List<Map<String, Object>> items = jdbcTemplate.queryForList(checkSql, updateItem.getId(), userId);

                    if (items.isEmpty()) {
                        results.add(new BatchUpdateResultItem(updateItem.getId(), false, "生词不存在或没有权限更新"));
                        failedCount++;
                        continue;
                    }

                    // 构建更新字段
                    Map<String, Object> updateFields = new HashMap<>();
                    List<Object> params = new ArrayList<>();
                    List<String> setClauses = new ArrayList<>();

                    if (updateItem.getStatus() != null) {
                        setClauses.add("status = ?");
                        params.add(updateItem.getStatus());
                    }

                    if (updateItem.getMasteryLevel() != null) {
                        setClauses.add("mastery_level = ?");
                        params.add(updateItem.getMasteryLevel());
                    }

                    if (updateItem.getNotes() != null) {
                        setClauses.add("notes = ?");
                        params.add(updateItem.getNotes());
                    }

                    // 添加更新时间
                    setClauses.add("updated_at = ?");
                    params.add(LocalDateTime.now());

                    // 如果没有要更新的字段
                    if (setClauses.isEmpty()) {
                        results.add(new BatchUpdateResultItem(updateItem.getId(), false, "没有提供更新数据"));
                        failedCount++;
                        continue;
                    }

                    // 执行更新
                    String updateSql = "UPDATE user_vocabulary SET " + String.join(", ", setClauses) +
                            " WHERE user_vocab_id = ? AND user_id = ?";
                    params.add(updateItem.getId());
                    params.add(userId);

                    int updatedRows = jdbcTemplate.update(updateSql, params.toArray());

                    if (updatedRows > 0) {
                        // 处理标签更新
                        if (updateItem.getTags() != null) {
                            // 删除旧的标签关联
                            String deleteTagsSql = "DELETE FROM user_vocabulary_tags WHERE user_vocab_id = ?";
                            jdbcTemplate.update(deleteTagsSql, updateItem.getId());

                            // 添加新的标签关联
                            for (String tagName : updateItem.getTags()) {
                                if (tagName != null && !tagName.trim().isEmpty()) {
                                    // 查找或创建标签
                                    String tagSql = "SELECT tag_id FROM vocabulary_tags WHERE tag_name = ?";
                                    List<Map<String, Object>> tags = jdbcTemplate.queryForList(tagSql, tagName.trim());

                                    Long tagId;
                                    if (tags.isEmpty()) {
                                        // 创建新标签
                                        String insertTagSql = "INSERT INTO vocabulary_tags (tag_name, created_at) VALUES (?, ?)";
                                        KeyHolder tagKeyHolder = new GeneratedKeyHolder();

                                        jdbcTemplate.update(connection -> {
                                            PreparedStatement ps = connection.prepareStatement(insertTagSql, Statement.RETURN_GENERATED_KEYS);
                                            ps.setString(1, tagName.trim());
                                            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                            return ps;
                                        }, tagKeyHolder);

                                        tagId = tagKeyHolder.getKey().longValue();
                                    } else {
                                        tagId = ((Number) tags.get(0).get("tag_id")).longValue();
                                    }

                                    // 关联标签
                                    String relationSql = "INSERT INTO user_vocabulary_tags (user_vocab_id, tag_id, created_at) VALUES (?, ?, ?)";
                                    jdbcTemplate.update(relationSql, updateItem.getId(), tagId, LocalDateTime.now());
                                }
                            }
                        }

                        results.add(new BatchUpdateResultItem(updateItem.getId(), true, "更新成功"));
                        successCount++;
                    } else {
                        results.add(new BatchUpdateResultItem(updateItem.getId(), false, "更新失败"));
                        failedCount++;
                    }

                } catch (Exception e) {
                    results.add(new BatchUpdateResultItem(updateItem.getId(), false, "执行错误: " + e.getMessage()));
                    failedCount++;
                }
            }

            // 4. 创建结果数据
            BatchUpdateResultData resultData = new BatchUpdateResultData(
                    request.getItems().size(),
                    successCount,
                    failedCount,
                    results
            );

            printQueryResult("批量更新结果: " + resultData);

            // 5. 创建响应
            String message = String.format("批量更新完成，成功 %d 项，失败 %d 项", successCount, failedCount);
            BatchUpdateResponse response = new BatchUpdateResponse(true, message, resultData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("批量更新生词过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BatchUpdateResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}