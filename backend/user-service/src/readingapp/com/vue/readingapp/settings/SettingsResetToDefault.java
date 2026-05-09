package com.vue.readingapp.settings;

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
@RequestMapping("/api/v1/user/settings")
public class SettingsResetToDefault {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到重置设置请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
    }

    private void printQueryResult(Object result) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("查询结果: " + result);
        System.out.println("===================");
    }

    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    public static class ResetResponse {
        private boolean success;
        private String message;
        private ResetData data;

        public ResetResponse(boolean success, String message, ResetData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ResetData getData() { return data; }
        public void setData(ResetData data) { this.data = data; }
    }

    public static class ResetData {
        private boolean reset;

        public ResetData(boolean reset) {
            this.reset = reset;
        }

        public boolean isReset() { return reset; }
        public void setReset(boolean reset) { this.reset = reset; }
    }

    @DeleteMapping("")
    public ResponseEntity<ResetResponse> resetSettingsToDefault(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ResetResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ResetResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 删除用户的所有设置
            String deleteSql = "DELETE FROM user_settings WHERE user_id = ?";
            int deletedRows = jdbcTemplate.update(deleteSql, userId);
            printQueryResult("删除了 " + deletedRows + " 条设置记录");

            // 4. 准备响应
            ResetData resetData = new ResetData(true);
            ResetResponse response = new ResetResponse(true, "所有设置已重置为默认值", resetData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("重置设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResetResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
