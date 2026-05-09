package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tags")
public class TagDelete {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应类
    public static class DeleteTagResponse {
        private boolean success;
        private String message;

        public DeleteTagResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<DeleteTagResponse> deleteTag(
            @PathVariable("tagId") String tagId,
            @RequestParam(value = "force", defaultValue = "false") boolean force) {

        Map<String, Object> requestData = new java.util.HashMap<>();
        requestData.put("tagId", tagId);
        requestData.put("force", force);
        printRequest(requestData);

        try {
            // 先检查标签是否存在（在document_tags中）
            String checkSql = "SELECT COUNT(*) FROM document_tags WHERE tag_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, Integer.parseInt(tagId));

            String tableName = "document_tags";
            if (count == 0) {
                // 如果在document_tags中不存在，检查vocabulary_tags
                checkSql = "SELECT COUNT(*) FROM vocabulary_tags WHERE tag_id = ?";
                count = jdbcTemplate.queryForObject(checkSql, Integer.class, Integer.parseInt(tagId));
                if (count == 0) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            new DeleteTagResponse(false, "标签不存在")
                    );
                }
                tableName = "vocabulary_tags";
            }

            // 检查标签是否被使用
            String usageCheckSql = "";
            if ("document_tags".equals(tableName)) {
                usageCheckSql = "SELECT COUNT(*) FROM document_tag_relations WHERE tag_id = ?";
            } else {
                usageCheckSql = "SELECT COUNT(*) FROM user_vocabulary_tags WHERE tag_id = ?";
            }

            int usageCount = jdbcTemplate.queryForObject(usageCheckSql, Integer.class, Integer.parseInt(tagId));

            // 如果标签被使用且不是强制删除，则返回错误
            if (usageCount > 0 && !force) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new DeleteTagResponse(false, "标签正在被使用，无法删除。请使用force=true强制删除")
                );
            }

            // 如果是强制删除，先删除关联关系
            if (force && usageCount > 0) {
                String deleteRelationsSql = "";
                if ("document_tags".equals(tableName)) {
                    deleteRelationsSql = "DELETE FROM document_tag_relations WHERE tag_id = ?";
                } else {
                    deleteRelationsSql = "DELETE FROM user_vocabulary_tags WHERE tag_id = ?";
                }
                jdbcTemplate.update(deleteRelationsSql, Integer.parseInt(tagId));
            }

            // 删除标签
            String deleteSql = "DELETE FROM " + tableName + " WHERE tag_id = ?";
            int deleted = jdbcTemplate.update(deleteSql, Integer.parseInt(tagId));

            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteTagResponse(false, "标签删除失败")
                );
            }

            // 创建响应
            DeleteTagResponse response = new DeleteTagResponse(true, "标签删除成功");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("标签ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new DeleteTagResponse(false, "标签ID格式错误")
            );
        } catch (Exception e) {
            System.err.println("删除标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteTagResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}
