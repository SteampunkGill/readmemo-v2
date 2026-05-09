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
public class VocabularyDeleteItem {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除生词请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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

    // 响应DTO
    public static class DeleteVocabularyResponse {
        private boolean success;
        private String message;
        private DeleteData data;

        public DeleteVocabularyResponse(boolean success, String message, DeleteData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DeleteData getData() { return data; }
        public void setData(DeleteData data) { this.data = data; }
    }

    public static class DeleteData {
        private Long deletedId;
        private String word;
        private String deletedAt;

        public DeleteData(Long deletedId, String word, String deletedAt) {
            this.deletedId = deletedId;
            this.word = word;
            this.deletedAt = deletedAt;
        }

        public Long getDeletedId() { return deletedId; }
        public void setDeletedId(Long deletedId) { this.deletedId = deletedId; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
    }

    @DeleteMapping("/{userVocabId}")
    public ResponseEntity<DeleteVocabularyResponse> deleteVocabularyItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long userVocabId) {

        printRequest("userVocabId: " + userVocabId);

        try {
            // 1. 验证用户身份
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteVocabularyResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteVocabularyResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            // 2. 查询要删除的生词信息（用于返回）
            String selectSql = "SELECT user_vocab_id, word FROM user_vocabulary WHERE user_vocab_id = ? AND user_id = ?";
            List<Map<String, Object>> items = jdbcTemplate.queryForList(selectSql, userVocabId, userId);

            if (items.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteVocabularyResponse(false, "生词不存在或没有权限删除", null)
                );
            }

            Map<String, Object> item = items.get(0);
            String word = (String) item.get("word");

            // 3. 删除标签关联
            String deleteTagsSql = "DELETE FROM user_vocabulary_tags WHERE user_vocab_id = ?";
            jdbcTemplate.update(deleteTagsSql, userVocabId);

            // 4. 删除生词
            String deleteSql = "DELETE FROM user_vocabulary WHERE user_vocab_id = ? AND user_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, userVocabId, userId);

            printQueryResult("删除行数: " + deletedRows);

            if (deletedRows == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteVocabularyResponse(false, "删除失败", null)
                );
            }

            // 5. 创建响应数据
            DeleteData deleteData = new DeleteData(userVocabId, word, LocalDateTime.now().toString());
            DeleteVocabularyResponse response = new DeleteVocabularyResponse(true, "生词删除成功", deleteData);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除生词过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteVocabularyResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
