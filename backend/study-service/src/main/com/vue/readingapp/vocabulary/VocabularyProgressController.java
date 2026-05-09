package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vocabulary/progress")
public class VocabularyProgressController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(String method, Object request) {
        System.out.println("=== 收到学习进度请求 [" + method + "] ===");
        System.out.println("请求数据: " + request);
        System.out.println("==========================");
    }

    // 响应DTO
    public static class ProgressResponse {
        private boolean success;
        private String message;
        private Map<String, ProgressItem> data;

        public ProgressResponse(boolean success, String message, Map<String, ProgressItem> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, ProgressItem> getData() { return data; }
    }

    public static class ProgressItem {
        private String status;
        private String lastReviewed;

        public ProgressItem(String status, String lastReviewed) {
            this.status = status;
            this.lastReviewed = lastReviewed;
        }

        public String getStatus() { return status; }
        public String getLastReviewed() { return lastReviewed; }
    }

    public static class UpdateProgressRequest {
        private Long wordId;
        private String status;

        public Long getWordId() { return wordId; }
        public void setWordId(Long wordId) { this.wordId = wordId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @GetMapping("")
    public ResponseEntity<ProgressResponse> getProgress(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        printRequest("GET", null);

        try {
            // 1. 验证用户身份
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ProgressResponse(false, "请先登录", null)
                );
            }

            // 2. 查询进度
            String sql = "SELECT word_id, status, last_reviewed_at FROM user_vocabulary WHERE user_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);

            Map<String, ProgressItem> progressMap = new HashMap<>();
            for (Map<String, Object> row : results) {
                String wordId = row.get("word_id").toString();
                String status = (String) row.get("status");
                String lastReviewed = row.get("last_reviewed_at") != null ? row.get("last_reviewed_at").toString() : null;
                
                // 映射状态以符合前端要求 ('new'|'known'|'reviewing')
                // 数据库中可能是 'new', 'learning', 'mastered'
                String mappedStatus = status;
                if ("mastered".equals(status)) mappedStatus = "known";
                else if ("learning".equals(status)) mappedStatus = "reviewing";

                progressMap.put(wordId, new ProgressItem(mappedStatus, lastReviewed));
            }

            return ResponseEntity.ok(new ProgressResponse(true, "获取进度成功", progressMap));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ProgressResponse(false, "服务器错误: " + e.getMessage(), null)
            );
        }
    }

    @PostMapping("")
    public ResponseEntity<ProgressResponse> updateProgress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateProgressRequest request) {

        printRequest("POST", request);

        try {
            // 1. 验证用户身份
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ProgressResponse(false, "请先登录", null)
                );
            }

            // 2. 映射状态回数据库格式
            String dbStatus = request.getStatus();
            if ("known".equals(request.getStatus())) dbStatus = "mastered";
            else if ("reviewing".equals(request.getStatus())) dbStatus = "learning";

            // 3. 更新进度
            String updateSql = "UPDATE user_vocabulary SET status = ?, last_reviewed_at = NOW(), updated_at = NOW() " +
                               "WHERE user_id = ? AND word_id = ?";
            int updated = jdbcTemplate.update(updateSql, dbStatus, userId, request.getWordId());

            if (updated == 0) {
                // 如果不存在，可能需要插入（这里假设单词已经在生词本中，或者根据业务逻辑处理）
                // 为了简单起见，如果更新失败返回错误
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ProgressResponse(false, "未找到该单词的记录", null)
                );
            }

            return ResponseEntity.ok(new ProgressResponse(true, "更新进度成功", null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ProgressResponse(false, "服务器错误: " + e.getMessage(), null)
            );
        }
    }

    @PatchMapping("")
    public ResponseEntity<ProgressResponse> patchProgress(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody UpdateProgressRequest request) {
        return updateProgress(authHeader, request);
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token);

        if (sessions.isEmpty()) {
            return null;
        }

        return ((Number) sessions.get(0).get("user_id")).longValue();
    }
}