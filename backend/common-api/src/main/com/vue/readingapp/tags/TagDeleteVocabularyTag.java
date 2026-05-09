package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tags/vocabulary")
public class TagDeleteVocabularyTag {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除生词标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应类
    public static class DeleteVocabularyTagResponse {
        private boolean success;
        private String message;

        public DeleteVocabularyTagResponse(boolean success, String message) {
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
    public ResponseEntity<DeleteVocabularyTagResponse> deleteVocabularyTag(@PathVariable("tagId") String tagId) {
        printRequest(tagId);

        try {
            // 检查标签是否存在
            String checkSql = "SELECT COUNT(*) FROM vocabulary_tags WHERE tag_id = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, Integer.parseInt(tagId));
            if (count == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteVocabularyTagResponse(false, "生词标签不存在")
                );
            }

            // 检查标签是否被使用
            String usageCheckSql = "SELECT COUNT(*) FROM user_vocabulary_tags WHERE tag_id = ?";
            int usageCount = jdbcTemplate.queryForObject(usageCheckSql, Integer.class, Integer.parseInt(tagId));

            if (usageCount > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new DeleteVocabularyTagResponse(false, "生词标签正在被使用，无法删除")
                );
            }

            // 删除生词标签
            String deleteSql = "DELETE FROM vocabulary_tags WHERE tag_id = ?";
            int deleted = jdbcTemplate.update(deleteSql, Integer.parseInt(tagId));

            if (deleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteVocabularyTagResponse(false, "生词标签删除失败")
                );
            }

            // 创建响应
            DeleteVocabularyTagResponse response = new DeleteVocabularyTagResponse(true, "生词标签删除成功");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("标签ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new DeleteVocabularyTagResponse(false, "标签ID格式错误")
            );
        } catch (Exception e) {
            System.err.println("删除生词标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteVocabularyTagResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}
