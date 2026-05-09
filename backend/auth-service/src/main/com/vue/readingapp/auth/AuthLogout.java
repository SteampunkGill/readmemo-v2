package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;

/**
 * 用户登出控制器
 * 
 * 负责处理用户的退出登录请求，通过销毁数据库中的会话令牌（Token）来确保安全性。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthLogout {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印登出请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到登出请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=================");
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
     * 登出响应的数据结构
     */
    public static class LogoutResponse {
        private boolean success;
        private String message;

        public LogoutResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 用户登出接口
     * 
     * @param httpRequest 原始 HTTP 请求对象，用于获取请求头中的 Token
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest httpRequest) {
        // 记录请求
        printRequest("登出请求");

        try {
            // 1. 身份识别：从请求头 Authorization 中提取 Bearer Token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(
                        new LogoutResponse(false, "未提供有效的认证令牌")
                );
            }

            // 截取 "Bearer " 之后的实际 Token 字符串
            String token = authHeader.substring(7);

            // 2. 查找会话：在数据库中搜索该 Token 对应的会话记录
            String findSessionSql = "SELECT session_id, user_id FROM user_sessions WHERE access_token = ?";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(findSessionSql, token);
            printQueryResult(sessions);

            if (sessions.isEmpty()) {
                // 如果数据库里已经没有这个 Token 了（可能已过期或已在别处登出），
                // 为了用户体验，我们依然返回成功，让前端清除本地缓存。
                LogoutResponse response = new LogoutResponse(true, "登出成功");
                printResponse(response);
                return ResponseEntity.ok(response);
            }

            // 3. 销毁会话：从数据库中物理删除该会话记录，使 Token 立即失效
            Map<String, Object> session = sessions.get(0);
            Integer sessionId = (Integer) session.get("session_id");
            Integer userId = (Integer) session.get("user_id");

            String deleteSessionSql = "DELETE FROM user_sessions WHERE session_id = ?";
            jdbcTemplate.update(deleteSessionSql, sessionId);

            System.out.println("用户ID " + userId + " 的会话已删除");

            // 4. 返回成功响应
            LogoutResponse response = new LogoutResponse(true, "登出成功");
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 异常处理
            System.err.println("登出过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new LogoutResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}