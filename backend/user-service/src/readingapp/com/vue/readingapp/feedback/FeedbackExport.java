package com.vue.readingapp.feedback;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackExport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到导出反馈数据请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=======================");
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
    public static class ExportRequest {
        private String format;
        private String type;
        private String status;
        private String startDate;
        private String endDate;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }

        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    // 响应DTO
    public static class ExportResponse {
        private boolean success;
        private String message;
        private ExportData data;

        public ExportResponse(boolean success, String message, ExportData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ExportData getData() { return data; }
        public void setData(ExportData data) { this.data = data; }
    }

    public static class ExportData {
        private Map<String, Object> metadata;
        private List<Map<String, Object>> data;

        public ExportData(Map<String, Object> metadata, List<Map<String, Object>> data) {
            this.metadata = metadata;
            this.data = data;
        }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }
    }

    @GetMapping("/export")
    public ResponseEntity<ExportResponse> exportFeedbackData(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 创建请求对象用于打印
        ExportRequest request = new ExportRequest();
        request.setFormat(format);
        request.setType(type);
        request.setStatus(status);
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证用户身份（需要管理员权限）
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ExportResponse(false, "用户未登录", null)
                );
            }

            // 检查是否是管理员
            String adminCheckSql = "SELECT role FROM users WHERE user_id = ?";
            List<Map<String, Object>> userList = jdbcTemplate.queryForList(adminCheckSql, userId);

            boolean isAdmin = false;
            if (!userList.isEmpty()) {
                Map<String, Object> user = userList.get(0);
                String role = (String) user.get("role");
                isAdmin = "admin".equals(role) || "moderator".equals(role);
            }

            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new ExportResponse(false, "需要管理员权限", null)
                );
            }

            // 2. 构建查询条件
            List<Object> params = new ArrayList<>();
            StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");

            // 处理type条件
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                whereClause.append(" AND type = ? ");
                params.add(type);
            }

            // 处理status条件
            if (status != null && !status.isEmpty() && !"all".equals(status)) {
                whereClause.append(" AND status = ? ");
                params.add(status);
            }

            // 处理日期范围条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                whereClause.append(" AND DATE(created_at) >= ? ");
                params.add(startDate);
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                whereClause.append(" AND DATE(created_at) <= ? ");
                params.add(endDate);
            }

            // 3. 查询反馈数据
            String querySql = "SELECT f.*, u.username as user_name, u.email as user_email " +
                    "FROM user_feedback f " +
                    "LEFT JOIN users u ON f.user_id = u.user_id " +
                    whereClause.toString() +
                    " ORDER BY f.created_at DESC";

            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(querySql, params.toArray());
            printQueryResult("导出 " + feedbackList.size() + " 条记录");

            // 4. 转换数据为导出格式
            List<Map<String, Object>> exportData = new ArrayList<>();
            for (Map<String, Object> row : feedbackList) {
                Map<String, Object> exportRow = new HashMap<>();

                // 添加基本字段
                exportRow.put("id", row.get("feedback_id"));
                exportRow.put("title", row.get("title"));
                exportRow.put("type", row.get("type"));
                exportRow.put("status", row.get("status"));
                exportRow.put("priority", row.get("priority"));
                exportRow.put("upvotes", row.get("upvotes"));
                exportRow.put("downvotes", row.get("downvotes"));
                exportRow.put("userName", row.get("user_name"));
                exportRow.put("userEmail", row.get("user_email"));
                exportRow.put("createdAt", row.get("created_at").toString());
                exportRow.put("updatedAt", row.get("updated_at").toString());
                exportRow.put("assignedTo", row.get("assigned_to"));
                exportRow.put("assignedAt", row.get("assigned_at") != null ? row.get("assigned_at").toString() : null);
                exportRow.put("completedAt", row.get("completed_at") != null ? row.get("completed_at").toString() : null);
                exportRow.put("viewCount", row.get("view_count"));
                exportRow.put("commentCount", row.get("comment_count"));

                // 处理metadata JSON
                String metadataJson = (String) row.get("metadata");
                if (metadataJson != null && !metadataJson.isEmpty() && !"{}".equals(metadataJson)) {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(
                                metadataJson, new TypeReference<Map<String, Object>>() {});
                        exportRow.put("metadata", metadata);
                    } catch (Exception e) {
                        exportRow.put("metadata", new HashMap<>());
                    }
                } else {
                    exportRow.put("metadata", new HashMap<>());
                }

                exportData.add(exportRow);
            }

            // 5. 准备元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exportDate", LocalDateTime.now().toString());
            metadata.put("exportFormat", format);
            metadata.put("totalRecords", feedbackList.size());

            Map<String, Object> filters = new HashMap<>();
            filters.put("type", type != null ? type : "all");
            filters.put("status", status != null ? status : "all");
            filters.put("startDate", startDate);
            filters.put("endDate", endDate);
            metadata.put("filters", filters);

            // 6. 根据格式处理数据
            if ("csv".equals(format)) {
                // 对于CSV格式，我们可以将数据扁平化
                List<Map<String, Object>> csvData = new ArrayList<>();
                for (Map<String, Object> row : exportData) {
                    Map<String, Object> csvRow = new HashMap<>(row);

                    // 将metadata中的字段提取出来
                    Map<String, Object> metadataMap = (Map<String, Object>) csvRow.get("metadata");
                    if (metadataMap != null) {
                        for (Map.Entry<String, Object> entry : metadataMap.entrySet()) {
                            csvRow.put("metadata_" + entry.getKey(), entry.getValue());
                        }
                    }
                    csvRow.remove("metadata");

                    csvData.add(csvRow);
                }
                exportData = csvData;
            }

            // 7. 准备响应数据
            ExportData exportDataObj = new ExportData(metadata, exportData);

            ExportResponse response = new ExportResponse(
                    true, "导出反馈数据成功", exportDataObj
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("导出反馈数据过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ExportResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}