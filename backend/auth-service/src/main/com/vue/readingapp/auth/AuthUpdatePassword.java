package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 修改密码控制器
 * 
 * 负责处理已登录用户在个人设置中修改密码的请求。
 * 与“重置密码”不同，此操作需要验证用户的“当前密码”。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthUpdatePassword {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印更新密码请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到更新密码请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("====================");
    }

    /**
     * 辅助方法：打印数据库查询结果
     */
    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    /**
     * 辅助方法：打印返回给前端的响应
     */
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    /**
     * 更新密码请求的数据结构
     */
    public static class UpdatePasswordRequest {
        private String current_password;          // 当前正在使用的密码
        private String new_password;              // 准备设置的新密码
        private String new_password_confirmation; // 确认新密码

        public String getCurrent_password() { return current_password; }
        public void setCurrent_password(String current_password) { this.current_password = current_password; }

        public String getNew_password() { return new_password; }
        public void setNew_password(String new_password) { this.new_password = new_password; }

        public String getNew_password_confirmation() { return new_password_confirmation; }
        public void setNew_password_confirmation(String new_password_confirmation) { this.new_password_confirmation = new_password_confirmation; }
    }

    /**
     * 更新密码响应的统一格式
     */
    public static class UpdatePasswordResponse {
        private boolean success;
        private String message;

        public UpdatePasswordResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 修改密码接口
     * 
     * @param request 包含旧密码和新密码的请求体
     * @param httpRequest 用于获取认证 Token 的请求对象
     * @return 修改结果
     */
    @PutMapping("/password")
    public ResponseEntity<UpdatePasswordResponse> updatePassword(@RequestBody UpdatePasswordRequest request,
                                                                 HttpServletRequest httpRequest) {
        printRequest(request);

        try {
            // 1. 基础验证：确保所有字段不为空
            if (request.getCurrent_password() == null || request.getCurrent_password().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdatePasswordResponse(false, "当前密码不能为空")
                );
            }

            if (request.getNew_password() == null || request.getNew_password().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdatePasswordResponse(false, "新密码不能为空")
                );
            }

            if (request.getNew_password_confirmation() == null || request.getNew_password_confirmation().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new UpdatePasswordResponse(false, "确认密码不能为空")
                );
            }

            // 2. 逻辑验证：检查两次输入的新密码是否一致
            if (!request.getNew_password().equals(request.getNew_password_confirmation())) {
                return ResponseEntity.badRequest().body(
                        new UpdatePasswordResponse(false, "两次输入的新密码不一致")
                );
            }

            // 3. 安全验证：新密码长度检查
            if (request.getNew_password().length() < 6) {
                return ResponseEntity.badRequest().body(
                        new UpdatePasswordResponse(false, "新密码长度不能少于 6 位")
                );
            }

            // 4. 逻辑验证：新密码不能与旧密码相同
            if (request.getCurrent_password().equals(request.getNew_password())) {
                return ResponseEntity.badRequest().body(
                        new UpdatePasswordResponse(false, "新密码不能与当前密码相同")
                );
            }

            // 5. 身份识别：从请求头获取 Token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePasswordResponse(false, "未提供有效的认证令牌")
                );
            }

            String token = authHeader.substring(7);

            // 6. 关联查询：查找当前登录用户及其存储的密码哈希
            String findUserSql = "SELECT s.user_id, u.password_hash FROM user_sessions s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "WHERE s.access_token = ? AND s.expires_at > ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(findUserSql, token, LocalDateTime.now());
            printQueryResult(users);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdatePasswordResponse(false, "令牌无效或已过期")
                );
            }

            Map<String, Object> user = users.get(0);
            Integer userId = (Integer) user.get("user_id");
            String currentPasswordHash = (String) user.get("password_hash");

            // 7. 密码验证：检查输入的“当前密码”是否与数据库一致
            // 注意：实际项目中应使用 passwordEncoder.matches() 进行比对
            if (!currentPasswordHash.equals(request.getCurrent_password())) {
                return ResponseEntity.badRequest().body(
                        new UpdatePasswordResponse(false, "当前密码错误")
                );
            }

            // 8. 执行更新：修改数据库中的密码
            String updatePasswordSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
            jdbcTemplate.update(updatePasswordSql, request.getNew_password(), userId);

            // 9. 安全增强：修改密码后，强制注销该用户的所有登录会话
            String deleteSessionsSql = "DELETE FROM user_sessions WHERE user_id = ?";
            jdbcTemplate.update(deleteSessionsSql, userId);

            System.out.println("用户ID " + userId + " 的密码已更新，所有旧会话已清除");

            // 10. 返回成功响应
            UpdatePasswordResponse response = new UpdatePasswordResponse(true, "密码更新成功，请重新登录");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdatePasswordResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}