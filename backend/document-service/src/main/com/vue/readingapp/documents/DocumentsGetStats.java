package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsGetStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(String authHeader) {
        System.out.println("=== 收到获取文档统计信息请求 ===");
        System.out.println("认证头: " + authHeader);
        System.out.println("============================");
    }

    // 打印查询结果
    private void printQueryResult(Map<String, Object> stats) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("统计信息: " + stats);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(DocumentStatsResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 响应DTO
    public static class DocumentStatsResponse {
        private boolean success;
        private String message;
        private DocumentStatsData data;

        public DocumentStatsResponse(boolean success, String message, DocumentStatsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DocumentStatsData getData() { return data; }
        public void setData(DocumentStatsData data) { this.data = data; }
    }

    public static class DocumentStatsData {
        private DocumentStatsDTO stats;

        public DocumentStatsData(DocumentStatsDTO stats) {
            this.stats = stats;
        }

        public DocumentStatsDTO getStats() { return stats; }
        public void setStats(DocumentStatsDTO stats) { this.stats = stats; }
    }

    public static class DocumentStatsDTO {
        private Integer totalDocuments;
        private String totalSize;
        private Map<String, Integer> byLanguage;
        private Map<String, Integer> byType;
        private List<RecentUploadDTO> recentUploads;
        private ReadingStatsDTO readingStats;

        // Getters and Setters
        public Integer getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(Integer totalDocuments) { this.totalDocuments = totalDocuments; }

        public String getTotalSize() { return totalSize; }
        public void setTotalSize(String totalSize) { this.totalSize = totalSize; }

        public Map<String, Integer> getByLanguage() { return byLanguage; }
        public void setByLanguage(Map<String, Integer> byLanguage) { this.byLanguage = byLanguage; }

        public Map<String, Integer> getByType() { return byType; }
        public void setByType(Map<String, Integer> byType) { this.byType = byType; }

        public List<RecentUploadDTO> getRecentUploads() { return recentUploads; }
        public void setRecentUploads(List<RecentUploadDTO> recentUploads) { this.recentUploads = recentUploads; }

        public ReadingStatsDTO getReadingStats() { return readingStats; }
        public void setReadingStats(ReadingStatsDTO readingStats) { this.readingStats = readingStats; }
    }

    public static class RecentUploadDTO {
        private Integer id;
        private String title;
        private String fileName;
        private String fileSize;
        private String createdAt;

        public RecentUploadDTO(Integer id, String title, String fileName, String fileSize, String createdAt) {
            this.id = id;
            this.title = title;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class ReadingStatsDTO {
        private Integer totalReadingTime;
        private Integer averageReadingTime;
        private Integer completedDocuments;

        public ReadingStatsDTO(Integer totalReadingTime, Integer averageReadingTime, Integer completedDocuments) {
            this.totalReadingTime = totalReadingTime;
            this.averageReadingTime = averageReadingTime;
            this.completedDocuments = completedDocuments;
        }

        // Getters and Setters
        public Integer getTotalReadingTime() { return totalReadingTime; }
        public void setTotalReadingTime(Integer totalReadingTime) { this.totalReadingTime = totalReadingTime; }

        public Integer getAverageReadingTime() { return averageReadingTime; }
        public void setAverageReadingTime(Integer averageReadingTime) { this.averageReadingTime = averageReadingTime; }

        public Integer getCompletedDocuments() { return completedDocuments; }
        public void setCompletedDocuments(Integer completedDocuments) { this.completedDocuments = completedDocuments; }
    }

    @GetMapping("/stats")
    public ResponseEntity<DocumentStatsResponse> getDocumentStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentStatsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DocumentStatsResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 查询文档总数
            String countSql = "SELECT COUNT(*) as total FROM documents WHERE user_id = ? AND deleted_at IS NULL";
            Integer totalDocuments = jdbcTemplate.queryForObject(countSql, Integer.class, userId);

            // 3. 查询总文件大小
            String sizeSql = "SELECT COALESCE(SUM(file_size), 0) as total_size FROM documents WHERE user_id = ? AND deleted_at IS NULL";
            Long totalSizeBytes = jdbcTemplate.queryForObject(sizeSql, Long.class, userId);
            String totalSize = formatFileSize(totalSizeBytes);

            // 4. 按语言分组统计
            String languageSql = "SELECT language, COUNT(*) as count FROM documents WHERE user_id = ? AND deleted_at IS NULL GROUP BY language";
            List<Map<String, Object>> languageResults = jdbcTemplate.queryForList(languageSql, userId);

            Map<String, Integer> byLanguage = new HashMap<>();
            for (Map<String, Object> row : languageResults) {
                String lang = (String) row.get("language");
                if (lang == null) lang = "unknown";
                byLanguage.put(lang, ((Long) row.get("count")).intValue());
            }

            // 5. 按文件类型分组统计
            String typeSql = "SELECT file_type, COUNT(*) as count FROM documents WHERE user_id = ? AND deleted_at IS NULL GROUP BY file_type";
            List<Map<String, Object>> typeResults = jdbcTemplate.queryForList(typeSql, userId);

            Map<String, Integer> byType = new HashMap<>();
            for (Map<String, Object> row : typeResults) {
                String fileType = (String) row.get("file_type");
                if (fileType == null) fileType = "unknown";
                byType.put(fileType, ((Long) row.get("count")).intValue());
            }

            // 6. 查询最近上传的文档
            String recentSql = "SELECT document_id, title, file_name, file_size, created_at " +
                    "FROM documents WHERE user_id = ? AND deleted_at IS NULL " +
                    "ORDER BY created_at DESC LIMIT 10";
            List<Map<String, Object>> recentResults = jdbcTemplate.queryForList(recentSql, userId);

            List<RecentUploadDTO> recentUploads = new ArrayList<>();
            for (Map<String, Object> row : recentResults) {
                RecentUploadDTO dto = new RecentUploadDTO(
                        (Integer) row.get("document_id"),
                        (String) row.get("title"),
                        (String) row.get("file_name"),
                        formatFileSize((Long) row.get("file_size")),
                        formatDate((java.sql.Timestamp) row.get("created_at"))
                );
                recentUploads.add(dto);
            }

            // 7. 查询阅读统计（简化实现）
            // 总阅读时间：假设每页阅读时间30秒
            String readingTimeSql = "SELECT COALESCE(SUM(page_count * 30), 0) as total_seconds FROM documents WHERE user_id = ? AND deleted_at IS NULL";
            Integer totalReadingSeconds = jdbcTemplate.queryForObject(readingTimeSql, Integer.class, userId);

            // 平均阅读时间
            Integer averageReadingTime = totalDocuments > 0 ? totalReadingSeconds / totalDocuments : 0;

            // 已完成文档数（阅读进度>=90%）
            String completedSql = "SELECT COUNT(*) as completed FROM documents WHERE user_id = ? AND reading_progress >= 90 AND deleted_at IS NULL";
            Integer completedDocuments = jdbcTemplate.queryForObject(completedSql, Integer.class, userId);

            ReadingStatsDTO readingStats = new ReadingStatsDTO(
                    totalReadingSeconds,
                    averageReadingTime,
                    completedDocuments
            );

            // 8. 构建统计信息
            DocumentStatsDTO stats = new DocumentStatsDTO();
            stats.setTotalDocuments(totalDocuments);
            stats.setTotalSize(totalSize);
            stats.setByLanguage(byLanguage);
            stats.setByType(byType);
            stats.setRecentUploads(recentUploads);
            stats.setReadingStats(readingStats);

            printQueryResult(new HashMap<String, Object>() {{
                put("totalDocuments", totalDocuments);
                put("totalSize", totalSize);
                put("byLanguage", byLanguage);
                put("byType", byType);
                put("recentUploadsCount", recentUploads.size());
                put("readingStats", readingStats);
            }});

            // 9. 构建响应数据
            DocumentStatsData data = new DocumentStatsData(stats);
            DocumentStatsResponse response = new DocumentStatsResponse(true, "获取文档统计信息成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取文档统计信息过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DocumentStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 格式化文件大小
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    // 格式化日期
    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        return timestamp.toLocalDateTime().toString();
    }
}