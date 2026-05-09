
package com.vue.readingapp.settings;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

@RestController
@RequestMapping("/api/v1/user/settings/import")
public class SettingsImport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    private void printRequest(Object request) {
        System.out.println("=== 收到导入设置请求 ===");
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

    public static class ImportResponse {
        private boolean success;
        private String message;
        private ImportData data;

        public ImportResponse(boolean success, String message, ImportData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ImportData getData() { return data; }
        public void setData(ImportData data) { this.data = data; }
    }

    public static class ImportData {
        private int importedCount;
        private String importDate;
        private String fileName;
        private String fileSize;

        public ImportData(int importedCount, String importDate, String fileName, String fileSize) {
            this.importedCount = importedCount;
            this.importDate = importDate;
            this.fileName = fileName;
            this.fileSize = fileSize;
        }

        public int getImportedCount() { return importedCount; }
        public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

        public String getImportDate() { return importDate; }
        public void setImportDate(String importDate) { this.importDate = importDate; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }
    }

    @PostMapping("")
    public ResponseEntity<ImportResponse> importSettings(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") MultipartFile file) {

        try {
            printRequest("文件名: " + file.getOriginalFilename() + ", 大小: " + file.getSize() + " bytes");

            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ImportResponse(false, "未授权访问，请先登录", null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 查询会话信息
            String sessionSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sessionSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ImportResponse(false, "会话已过期或无效，请重新登录", null)
                );
            }

            Integer userId = (Integer) sessions.get(0).get("user_id");

            // 3. 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportResponse(false, "文件为空", null)
                );
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB限制
                return ResponseEntity.badRequest().body(
                        new ImportResponse(false, "文件大小不能超过10MB", null)
                );
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
                return ResponseEntity.badRequest().body(
                        new ImportResponse(false, "只支持JSON格式文件", null)
                );
            }

            // 4. 读取并解析文件内容
            String fileContent = new String(file.getBytes(), "UTF-8");
            Map<String, Object> importData = objectMapper.readValue(fileContent, Map.class);

            // 5. 验证导入数据格式
            if (!importData.containsKey("settings")) {
                return ResponseEntity.badRequest().body(
                        new ImportResponse(false, "导入文件格式不正确，缺少settings字段", null)
                );
            }

            // 6. 开始导入设置
            LocalDateTime now = LocalDateTime.now();
            int importedCount = 0;

            Map<String, Map<String, String>> settingsMap = (Map<String, Map<String, String>>) importData.get("settings");

            // 遍历每种设置类型
            for (Map.Entry<String, Map<String, String>> entry : settingsMap.entrySet()) {
                String settingType = entry.getKey();
                Map<String, String> settings = entry.getValue();

                // 删除该类型下的所有现有设置
                String deleteSql = "DELETE FROM user_settings WHERE user_id = ? AND setting_type = ?";
                jdbcTemplate.update(deleteSql, userId, settingType);

                // 插入新的设置
                for (Map.Entry<String, String> settingEntry : settings.entrySet()) {
                    String key = settingEntry.getKey();
                    String value = settingEntry.getValue();

                    String insertSql = "INSERT INTO user_settings (user_id, setting_type, setting_key, setting_value, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql, userId, settingType, key, value, now, now);
                    importedCount++;
                }
            }

            // 7. 记录导入历史
            String insertHistorySql = "INSERT INTO import_history (user_id, file_name, file_size, imported_count, import_date) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertHistorySql, userId, originalFilename, file.getSize(), importedCount, now);

            // 8. 构建响应数据
            ImportData importResult = new ImportData(
                    importedCount,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                    originalFilename,
                    formatFileSize(file.getSize())
            );

            ImportResponse response = new ImportResponse(true, "设置导入成功", importResult);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("导入设置过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ImportResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }
}