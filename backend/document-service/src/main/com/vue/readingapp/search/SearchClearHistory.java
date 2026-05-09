package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchClearHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到清空搜索历史请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
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
    public static class ClearHistoryRequest {
        private String type;
        private String beforeDate;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getBeforeDate() { return beforeDate; }
        public void setBeforeDate(String beforeDate) { this.beforeDate = beforeDate; }
    }

    // 响应DTO
    public static class ClearHistoryResponse {
        private boolean success;
        private String message;
        private ClearHistoryData data;

        public ClearHistoryResponse(boolean success, String message, ClearHistoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ClearHistoryData getData() { return data; }
        public void setData(ClearHistoryData data) { this.data = data; }
    }

    public static class ClearHistoryData {
        private int deletedCount;
        private String type;
        private String beforeDate;

        public ClearHistoryData(int deletedCount, String type, String beforeDate) {
            this.deletedCount = deletedCount;
            this.type = type;
            this.beforeDate = beforeDate;
        }

        public int getDeletedCount() { return deletedCount; }
        public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getBeforeDate() { return beforeDate; }
        public void setBeforeDate(String beforeDate) { this.beforeDate = beforeDate; }
    }

    @DeleteMapping("/history")
    public ResponseEntity<ClearHistoryResponse> clearSearchHistory(
            @RequestBody(required = false) ClearHistoryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 处理可选请求体
        if (request == null) {
            request = new ClearHistoryRequest();
        }

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ClearHistoryResponse(false, "请先登录", null)
                );
            }

            // 2. 构建删除SQL
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("DELETE FROM search_history WHERE user_id = ?");

            List<Object> params = new ArrayList<>();
            params.add(userId);

            // 添加类型过滤
            if (request.getType() != null && !request.getType().isEmpty() && !"all".equals(request.getType())) {
                sqlBuilder.append(" AND search_type = ?");
                params.add(request.getType());
            }

            // 添加日期过滤
            if (request.getBeforeDate() != null && !request.getBeforeDate().isEmpty()) {
                sqlBuilder.append(" AND DATE(timestamp) <= ?");
                params.add(request.getBeforeDate());
            }

            // 3. 执行删除
            int deletedCount = jdbcTemplate.update(sqlBuilder.toString(), params.toArray());

            // 4. 准备响应数据
            ClearHistoryData data = new ClearHistoryData(
                    deletedCount,
                    request.getType() != null ? request.getType() : "all",
                    request.getBeforeDate()
            );

            ClearHistoryResponse response = new ClearHistoryResponse(
                    true,
                    "已清空 " + deletedCount + " 条搜索历史记录",
                    data
            );

            // 打印查询结果
            printQueryResult("清空搜索历史记录: 删除 " + deletedCount + " 条记录");

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("清空搜索历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ClearHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 从token获取用户ID
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
                List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);
                if (!sessions.isEmpty()) {
                    return ((Number) sessions.get(0).get("user_id")).longValue();
                }
            } catch (Exception e) {
                System.out.println("Token解析失败: " + e.getMessage());
            }
        }
        return null;
    }
}
