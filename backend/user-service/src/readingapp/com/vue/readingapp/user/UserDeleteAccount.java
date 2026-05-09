package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserDeleteAccount {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除账户请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=================");
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
    public static class DeleteAccountRequest {
        private String password;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // 响应DTO
    public static class DeleteAccountResponse {
        private boolean success;
        private String message;

        public DeleteAccountResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @DeleteMapping("/account")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody DeleteAccountRequest request) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteAccountResponse(false, "请先登录")
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteAccountResponse(false, "登录已过期，请重新登录")
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = (int) session.get("user_id");

            // 3. 验证密码
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new DeleteAccountResponse(false, "密码不能为空")
                );
            }

            // 查询用户密码
            String passwordSql = "SELECT password_hash FROM users WHERE user_id = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(passwordSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteAccountResponse(false, "用户不存在")
                );
            }

            Map<String, Object> user = users.get(0);
            String passwordHash = (String) user.get("password_hash");

            // 验证密码（课设简单处理，实际应该使用加密验证）
            if (!passwordHash.equals(request.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteAccountResponse(false, "密码错误")
                );
            }

            // 4. 开始删除用户数据
            // 由于数据库中设置了大量的 ON DELETE CASCADE 约束，
            // 大多数关联数据会在删除用户记录时自动删除。
            // 这里只手动处理一些可能没有级联删除或需要特殊处理的表。

            // 4.1 删除文档分页（因为 document_pages 的主键是 UUID，且可能没有设置级联删除）
            String deleteDocPagesSql = "DELETE FROM document_pages WHERE document_id IN (SELECT document_id FROM documents WHERE user_id = ?)";
            jdbcTemplate.update(deleteDocPagesSql, userId);

            // 4.2 删除反馈回复（如果 user_feedback 没有级联删除）
            String deleteFeedbackRepliesSql = "DELETE FROM feedback_replies WHERE user_id = ?";
            jdbcTemplate.update(deleteFeedbackRepliesSql, userId);

            // 4.3 最后删除用户记录，触发数据库级联删除
            String deleteUserSql = "DELETE FROM users WHERE user_id = ?";
            int rowsDeleted = jdbcTemplate.update(deleteUserSql, userId);

            printQueryResult("删除用户行数: " + rowsDeleted);

            if (rowsDeleted == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteAccountResponse(false, "用户不存在")
                );
            }

            // 5. 准备响应数据
            DeleteAccountResponse response = new DeleteAccountResponse(true, "账户删除成功");

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除账户过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteAccountResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}
