
package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tags")
public class TagMerge {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到合并标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class MergeTagsRequest {
        private String sourceTagId;
        private String targetTagId;

        // Getters and Setters
        public String getSourceTagId() { return sourceTagId; }
        public void setSourceTagId(String sourceTagId) { this.sourceTagId = sourceTagId; }

        public String getTargetTagId() { return targetTagId; }
        public void setTargetTagId(String targetTagId) { this.targetTagId = targetTagId; }
    }

    // 响应类
    public static class MergeTagsResponse {
        private boolean success;
        private String message;
        private Map<String, Object> data;

        public MergeTagsResponse(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    @PostMapping("/merge")
    public ResponseEntity<MergeTagsResponse> mergeTags(@RequestBody MergeTagsRequest request) {
        printRequest(request);

        try {
            // 验证请求数据
            if (request.getSourceTagId() == null || request.getSourceTagId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new MergeTagsResponse(false, "源标签ID不能为空", null)
                );
            }

            if (request.getTargetTagId() == null || request.getTargetTagId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new MergeTagsResponse(false, "目标标签ID不能为空", null)
                );
            }

            if (request.getSourceTagId().equals(request.getTargetTagId())) {
                return ResponseEntity.badRequest().body(
                        new MergeTagsResponse(false, "源标签和目标标签不能相同", null)
                );
            }

            int sourceId = Integer.parseInt(request.getSourceTagId());
            int targetId = Integer.parseInt(request.getTargetTagId());

            // 检查两个标签是否存在且类型相同
            String checkSourceSql = "SELECT tag_type FROM (SELECT 'document' as tag_type FROM document_tags WHERE tag_id = ? " +
                    "UNION ALL SELECT 'vocabulary' as tag_type FROM vocabulary_tags WHERE tag_id = ?) as t LIMIT 1";
            String sourceType = null;
            try {
                sourceType = jdbcTemplate.queryForObject(checkSourceSql, String.class, sourceId, sourceId);
            } catch (Exception e) {
                // 标签不存在
            }

            if (sourceType == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new MergeTagsResponse(false, "源标签不存在", null)
                );
            }

            String checkTargetSql = "SELECT tag_type FROM (SELECT 'document' as tag_type FROM document_tags WHERE tag_id = ? " +
                    "UNION ALL SELECT 'vocabulary' as tag_type FROM vocabulary_tags WHERE tag_id = ?) as t LIMIT 1";
            String targetType = null;
            try {
                targetType = jdbcTemplate.queryForObject(checkTargetSql, String.class, targetId, targetId);
            } catch (Exception e) {
                // 标签不存在
            }

            if (targetType == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new MergeTagsResponse(false, "目标标签不存在", null)
                );
            }

            if (!sourceType.equals(targetType)) {
                return ResponseEntity.badRequest().body(
                        new MergeTagsResponse(false, "标签类型不同，无法合并", null)
                );
            }

            int mergedCount = 0;

            // 根据标签类型执行合并操作
            if ("document".equals(sourceType)) {
                // 合并文档标签
                // 1. 更新document_tag_relations表，将源标签的关系转移到目标标签
                String updateRelationsSql =
                        "UPDATE document_tag_relations SET tag_id = ? WHERE tag_id = ? " +
                                "AND NOT EXISTS (SELECT 1 FROM document_tag_relations WHERE document_id = document_tag_relations.document_id AND tag_id = ?)";

                mergedCount = jdbcTemplate.update(updateRelationsSql, targetId, sourceId, targetId);

                // 2. 删除源标签的剩余关系（重复关系）
                String deleteDuplicateSql = "DELETE FROM document_tag_relations WHERE tag_id = ?";
                jdbcTemplate.update(deleteDuplicateSql, sourceId);

                // 3. 删除源标签
                String deleteSourceSql = "DELETE FROM document_tags WHERE tag_id = ?";
                jdbcTemplate.update(deleteSourceSql, sourceId);
            } else {
                // 合并词汇标签
                // 1. 更新user_vocabulary_tags表，将源标签的关系转移到目标标签
                String updateRelationsSql =
                        "UPDATE user_vocabulary_tags SET tag_id = ? WHERE tag_id = ? " +
                                "AND NOT EXISTS (SELECT 1 FROM user_vocabulary_tags WHERE user_vocabulary_id = user_vocabulary_tags.user_vocabulary_id AND tag_id = ?)";

                mergedCount = jdbcTemplate.update(updateRelationsSql, targetId, sourceId, targetId);

                // 2. 删除源标签的剩余关系（重复关系）
                String deleteDuplicateSql = "DELETE FROM user_vocabulary_tags WHERE tag_id = ?";
                jdbcTemplate.update(deleteDuplicateSql, sourceId);

                // 3. 删除源标签
                String deleteSourceSql = "DELETE FROM vocabulary_tags WHERE tag_id = ?";
                jdbcTemplate.update(deleteSourceSql, sourceId);
            }

            // 创建响应数据
            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("sourceTagId", request.getSourceTagId());
            responseData.put("targetTagId", request.getTargetTagId());
            responseData.put("mergedCount", mergedCount);

            // 创建响应
            MergeTagsResponse response = new MergeTagsResponse(
                    true, "标签合并成功", responseData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("标签ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new MergeTagsResponse(false, "标签ID格式错误", null)
            );
        } catch (Exception e) {
            System.err.println("合并标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new MergeTagsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}