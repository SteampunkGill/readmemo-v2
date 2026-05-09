package com.vue.readingapp.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.List;

/**
 * 令牌刷新控制器
 * 
 * 负责处理访问令牌（Access Token）过期后的续期请求。
 * 用户使用长效的“刷新令牌”（Refresh Token）来换取新的短效“访问令牌”。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthRefreshToken {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 辅助方法：打印刷新令牌请求日志
     */
    private void printRequest(Object request) {
        System.out.println("=== 收到刷新令牌请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
     * 刷新令牌请求的数据结构
     */
    public static class RefreshTokenRequest {
        private String refresh_token;

        public String getRefresh_token() { return refresh_token; }
        public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }
    }

    /**
     * 刷新令牌响应的统一格式
     */
    public static class RefreshTokenResponse {
        private boolean success;
        private String message;
        private TokenData data;

        public RefreshTokenResponse(boolean success, String message, TokenData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public TokenData getData() { return data; }
        public void setData(TokenData data) { this.data = data; }
    }

    /**
     * 包含新令牌的数据对象
     */
    public static class TokenData {
        private String access_token;
        private String refresh_token;
        private int expires_in; // 有效期（秒）

        public TokenData(String access_token, String refresh_token, int expires_in) {
            this.access_token = access_token;
            this.refresh_token = refresh_token;
            this.expires_in = expires_in;
        }

        public String getAccess_token() { return access_token; }
        public void setAccess_token(String access_token) { this.access_token = access_token; }

        public String getRefresh_token() { return refresh_token; }
        public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }

        public int getExpires_in() { return expires_in; }
        public void setExpires_in(int expires_in) { this.expires_in = expires_in; }
    }

    /**
     * 刷新令牌接口
     * 
     * @param request 包含 refresh_token 的请求体
     * @return 新的访问令牌和刷新令牌
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        printRequest(request);

        try {
            // 1. 基础验证
            if (request.getRefresh_token() == null || request.getRefresh_token().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new RefreshTokenResponse(false, "刷新令牌不能为空", null)
                );
            }

            // 2. 数据库验证：检查该刷新令牌是否存在
            String checkTokenSql = "SELECT session_id, user_id, expires_at FROM user_sessions WHERE refresh_token = ?";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(checkTokenSql, request.getRefresh_token());
            printQueryResult(sessions);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new RefreshTokenResponse(false, "刷新令牌无效", null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            Integer sessionId = (Integer) session.get("session_id");
            LocalDateTime expiresAt = (LocalDateTime) session.get("expires_at");
            LocalDateTime now = LocalDateTime.now();

            // 3. 过期检查：如果刷新令牌也过期了，用户必须重新登录
            if (now.isAfter(expiresAt)) {
                // 清理已过期的会话记录
                jdbcTemplate.update("DELETE FROM user_sessions WHERE session_id = ?", sessionId);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new RefreshTokenResponse(false, "刷新令牌已过期，请重新登录", null)
                );
            }

            // 4. 生成新令牌：采用“滚动刷新”策略，每次刷新都生成一对全新的令牌
            String newAccessToken = "access_" + UUID.randomUUID().toString();
            String newRefreshToken = "refresh_" + UUID.randomUUID().toString();
            LocalDateTime newExpiresAt = now.plusHours(1); // 新访问令牌有效期 1 小时

            // 5. 更新数据库中的会话信息
            String updateSessionSql = "UPDATE user_sessions SET access_token = ?, refresh_token = ?, expires_at = ? WHERE session_id = ?";
            jdbcTemplate.update(updateSessionSql,
                    newAccessToken,
                    newRefreshToken,
                    newExpiresAt,
                    sessionId
            );

            // 6. 返回新令牌给前端
            TokenData tokenData = new TokenData(newAccessToken, newRefreshToken, 3600);
            RefreshTokenResponse response = new RefreshTokenResponse(true, "令牌刷新成功", tokenData);

            printResponse(response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new RefreshTokenResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}