package com.vue.readingapp.settings;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

@RestController
@RequestMapping("/api/v1/user/settings/export")
public class SettingsExport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    private void printRequest(Object request) {
        System.out.println("=== 收到导出设置请求 ===");
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

    @GetMapping("")
    public ResponseEntity<?> exportSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            printRequest("Authorization: " + authHeader);

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "未授权访问，请先登录");
                        }}
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "会话已过期或无效，请重新登录");
                        }}
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 查询用户信息
            String userSql = "SELECT username, email, nickname, avatar_url FROM users WHERE user_id = ?";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, userId);

            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "用户不存在");
                        }}
                );
            }

            Map<String, Object> user = users.get(0);

            // 4. 查询所有设置
            String settingsSql = "SELECT setting_type, setting_key, setting_value, created_at, updated_at FROM user_settings WHERE user_id = ?";
            List<Map<String, Object>> settingsList = jdbcTemplate.queryForList(settingsSql, userId);
            printQueryResult(settingsList);

            // 5. 构建导出数据
            Map<String, Object> exportData = new HashMap<>();

            // 添加导出元数据
            exportData.put("export_version", "1.0");
            exportData.put("export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            exportData.put("app_name", "Vue Reading App");
            exportData.put("app_version", "1.0.0");

            // 添加用户信息
            exportData.put("user_info", new HashMap<String, Object>() {{
                put("username", user.get("username"));
                put("email", user.get("email"));
                put("nickname", user.get("nickname"));
                put("avatar_url", user.get("avatar_url"));
            }});

            // 按类型分组设置
            Map<String, Map<String, String>> groupedSettings = new HashMap<>();

            for (Map<String, Object> setting : settingsList) {
                String settingType = (String) setting.get("setting_type");
                String key = (String) setting.get("setting_key");
                String value = (String) setting.get("setting_value");

                if (!groupedSettings.containsKey(settingType)) {
                    groupedSettings.put(settingType, new HashMap<>());
                }

                groupedSettings.get(settingType).put(key, value);
            }

            // 添加设置数据
            exportData.put("settings", groupedSettings);

            // 6. 将数据转换为JSON字符串
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String jsonData = objectMapper.writeValueAsString(exportData);

            // 7. 创建文件名
            String fileName = "settings_export_" + userId + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".json";

            // 8. 设置响应头
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(jsonData);

        } catch (Exception e) {
            System.err.println("导出设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "服务器内部错误: " + e.getMessage());
                    }}
            );
        }
    }
}
