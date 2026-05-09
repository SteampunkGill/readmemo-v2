package com.vue.readingapp.user;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/user")
public class UserGetActivityLog {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取用户活动日志请求 ===");
        System.out.println("请求参数: " + request);
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

    // 响应DTO
    public static class ActivityLogResponse {
        private boolean success;
        private String message;
        private List<ActivityData> activities;
        private PaginationData pagination;

        public ActivityLogResponse(boolean success, String message, List<ActivityData> activities, PaginationData pagination) {
            this.success = success;
            this.message = message;
            this.activities = activities;
            this.pagination = pagination;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<ActivityData> getActivities() { return activities; }
        public void setActivities(List<ActivityData> activities) { this.activities = activities; }

        public PaginationData getPagination() { return pagination; }
        public void setPagination(PaginationData pagination) { this.pagination = pagination; }
    }

    public static class ActivityData {
        private int id;
        private String type;
        private String action;
        private String targetType;
        private int targetId;
        private String targetName;
        private Map<String, Object> details;
        private String timestamp;
        private String formattedTime;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }

        public int getTargetId() { return targetId; }
        public void setTargetId(int targetId) { this.targetId = targetId; }

        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getFormattedTime() { return formattedTime; }
        public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }
    }

    public static class PaginationData {
        private int page;
        private int pageSize;
        private int total;
        private int totalPages;

        public PaginationData(int page, int pageSize, int total, int totalPages) {
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalPages = totalPages;
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        } else if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        }
        return null;
    }

    @GetMapping("/activity-log")
    public ResponseEntity<ActivityLogResponse> getUserActivityLog(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        // 打印接收到的请求
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("pageSize", pageSize);
        params.put("type", type);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        printRequest(params);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ActivityLogResponse(false, "请先登录", null, null)
                );
            }

            String accessToken = authHeader.substring(7);

            // 2. 验证token有效性
            String tokenSql = "SELECT user_id, expires_at FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, accessToken);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ActivityLogResponse(false, "登录已过期，请重新登录", null, null)
                );
            }

            Map<String, Object> session = sessions.get(0);
            int userId = ((Number) session.get("user_id")).intValue();

            // 3. 验证参数
            if (page < 1) {
                page = 1;
            }

            if (pageSize < 1 || pageSize > 100) {
                pageSize = 20;
            }

            // 4. 构建查询条件 (针对 reading_history 表)
            StringBuilder whereClause = new StringBuilder("WHERE rh.user_id = ?");
            List<Object> queryParams = new ArrayList<>();
            queryParams.add(userId);

            // 注意：reading_history 表没有 type 字段，这里我们假设所有记录都是 'reading' 类型
            if (type != null && !type.trim().isEmpty() && !"reading".equalsIgnoreCase(type)) {
                // 如果请求了非 reading 类型，直接返回空列表
                return ResponseEntity.ok(new ActivityLogResponse(true, "获取用户活动日志成功", new ArrayList<>(), new PaginationData(page, pageSize, 0, 0)));
            }

            if (startDate != null && !startDate.trim().isEmpty()) {
                whereClause.append(" AND DATE(rh.created_at) >= ?");
                queryParams.add(startDate);
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                whereClause.append(" AND DATE(rh.created_at) <= ?");
                queryParams.add(endDate);
            }

            // 5. 查询总记录数
            String countSql = "SELECT COUNT(*) as total FROM reading_history rh " + whereClause;
            Map<String, Object> countResult = jdbcTemplate.queryForMap(countSql, queryParams.toArray());
            int total = ((Number) countResult.get("total")).intValue();
            int totalPages = (int) Math.ceil((double) total / pageSize);

            // 6. 查询活动数据 (关联 documents 表获取标题)
            String activitySql = "SELECT rh.history_id, rh.document_id, d.title, rh.page, rh.reading_time, rh.created_at " +
                    "FROM reading_history rh " +
                    "LEFT JOIN documents d ON rh.document_id = d.document_id " +
                    whereClause +
                    " ORDER BY rh.created_at DESC LIMIT ? OFFSET ?";

            // 添加分页参数
            List<Object> activityParams = new ArrayList<>(queryParams);
            activityParams.add(pageSize);
            activityParams.add((page - 1) * pageSize);

            List<Map<String, Object>> activityResults = jdbcTemplate.queryForList(activitySql, activityParams.toArray());
            printQueryResult("活动记录数: " + activityResults.size());

            // 7. 格式化活动数据
            List<ActivityData> activities = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Map<String, Object> activity : activityResults) {
                ActivityData activityData = new ActivityData();
                activityData.setId(((Number) activity.get("history_id")).intValue());
                activityData.setType("reading");
                activityData.setAction("read");
                activityData.setTargetType("document");
                activityData.setTargetId(((Number) activity.get("document_id")).intValue());
                activityData.setTargetName((String) activity.get("title"));

                Map<String, Object> details = new HashMap<>();
                details.put("page", activity.get("page"));
                details.put("reading_time", activity.get("reading_time"));
                activityData.setDetails(details);

                // 格式化时间
                LocalDateTime timestamp = convertToLocalDateTime(activity.get("created_at"));
                if (timestamp != null) {
                    activityData.setTimestamp(timestamp.format(formatter));
                    activityData.setFormattedTime(formatRelativeTime(timestamp));
                }

                activities.add(activityData);
            }

            // 8. 构建分页数据
            PaginationData pagination = new PaginationData(page, pageSize, total, totalPages);

            // 9. 准备响应数据
            ActivityLogResponse response = new ActivityLogResponse(true, "获取用户活动日志成功", activities, pagination);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取用户活动日志过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ActivityLogResponse(false, "服务器内部错误: " + e.getMessage(), null, null)
            );
        }
    }

    // 格式化相对时间
    private String formatRelativeTime(LocalDateTime date) {
        if (date == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long diffSec = java.time.Duration.between(date, now).getSeconds();
        long diffMin = diffSec / 60;
        long diffHour = diffMin / 60;
        long diffDay = diffHour / 24;

        if (diffSec < 60) {
            return "刚刚";
        } else if (diffMin < 60) {
            return diffMin + "分钟前";
        } else if (diffHour < 24) {
            return diffHour + "小时前";
        } else if (diffDay < 7) {
            return diffDay + "天前";
        } else {
            return date.format(DateTimeFormatter.ofPattern("MM-dd"));
        }
    }
}
