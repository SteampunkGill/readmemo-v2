package com.vue.readingapp.review;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewGetHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到获取复习历史请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("时间: " + LocalDateTime.now());
        System.out.println("========================");
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

    public static class ReviewHistoryResponse {
        private boolean success;
        private String message;
        private ReviewHistoryData data;

        public ReviewHistoryResponse(boolean success, String message, ReviewHistoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReviewHistoryData getData() { return data; }
        public void setData(ReviewHistoryData data) { this.data = data; }
    }

    public static class ReviewHistoryData {
        private List<ReviewSession> sessions;
        private Pagination pagination;

        public ReviewHistoryData(List<ReviewSession> sessions, Pagination pagination) {
            this.sessions = sessions;
            this.pagination = pagination;
        }

        public List<ReviewSession> getSessions() { return sessions; }
        public void setSessions(List<ReviewSession> sessions) { this.sessions = sessions; }

        public Pagination getPagination() { return pagination; }
        public void setPagination(Pagination pagination) { this.pagination = pagination; }
    }

    public static class ReviewSession {
        private String id;
        private String mode;
        private int total_words;
        private int correct_words;
        private double accuracy;
        private int duration;
        private String language;
        private String completed_at;
        private Map<String, Object> details;

        public ReviewSession(String id, String mode, int total_words, int correct_words,
                             double accuracy, int duration, String language, String completed_at,
                             Map<String, Object> details) {
            this.id = id;
            this.mode = mode;
            this.total_words = total_words;
            this.correct_words = correct_words;
            this.accuracy = accuracy;
            this.duration = duration;
            this.language = language;
            this.completed_at = completed_at;
            this.details = details;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public int getTotal_words() { return total_words; }
        public void setTotal_words(int total_words) { this.total_words = total_words; }

        public int getCorrect_words() { return correct_words; }
        public void setCorrect_words(int correct_words) { this.correct_words = correct_words; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getCompleted_at() { return completed_at; }
        public void setCompleted_at(String completed_at) { this.completed_at = completed_at; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    public static class Pagination {
        private int page;
        private int page_size;
        private int total;
        private int total_pages;

        public Pagination(int page, int page_size, int total, int total_pages) {
            this.page = page;
            this.page_size = page_size;
            this.total = total;
            this.total_pages = total_pages;
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPage_size() { return page_size; }
        public void setPage_size(int page_size) { this.page_size = page_size; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getTotal_pages() { return total_pages; }
        public void setTotal_pages(int total_pages) { this.total_pages = total_pages; }
    }

    @GetMapping("/history")
    public ResponseEntity<ReviewHistoryResponse> getReviewHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "language", required = false) String language) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("page", page);
        requestParams.put("pageSize", pageSize);
        requestParams.put("startDate", startDate);
        requestParams.put("endDate", endDate);
        requestParams.put("language", language);
        printRequest(requestParams);

        try {
            // 1. 验证token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewHistoryResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReviewHistoryResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) users.get(0).get("user_id");
            System.out.println("当前用户ID: " + userId);

            // 2. 参数验证和默认值
            if (page < 1) page = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 20;

            // 3. 构建查询条件
            StringBuilder sqlBuilder = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sqlBuilder.append("SELECT rs.session_id, rs.mode, rs.total_words, rs.correct_words, ");
            sqlBuilder.append("rs.accuracy, rs.duration, rs.completed_at, ");
            sqlBuilder.append("COALESCE(GROUP_CONCAT(DISTINCT w.language), 'mixed') as languages ");
            sqlBuilder.append("FROM review_sessions rs ");
            sqlBuilder.append("LEFT JOIN user_vocabulary uv ON rs.user_id = uv.user_id ");
            sqlBuilder.append("LEFT JOIN words w ON uv.word_id = w.word_id ");
            sqlBuilder.append("WHERE rs.user_id = ? ");

            params.add(userId);

            // 日期筛选
            if (startDate != null && !startDate.trim().isEmpty()) {
                sqlBuilder.append("AND DATE(rs.completed_at) >= ? ");
                params.add(startDate);
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                sqlBuilder.append("AND DATE(rs.completed_at) <= ? ");
                params.add(endDate);
            }

            sqlBuilder.append("GROUP BY rs.session_id, rs.mode, rs.total_words, rs.correct_words, ");
            sqlBuilder.append("rs.accuracy, rs.duration, rs.completed_at ");

            // 语言筛选
            if (language != null && !language.trim().isEmpty()) {
                sqlBuilder.append("HAVING languages LIKE ? ");
                params.add("%" + language + "%");
            }

            sqlBuilder.append("ORDER BY rs.completed_at DESC ");

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder.toString() + ") as temp";
            int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 5. 添加分页
            sqlBuilder.append("LIMIT ? OFFSET ?");
            int offset = (page - 1) * pageSize;
            params.add(pageSize);
            params.add(offset);

            // 6. 执行查询
            System.out.println("执行SQL: " + sqlBuilder.toString());
            System.out.println("参数: " + params);

            List<Map<String, Object>> historyList = jdbcTemplate.queryForList(
                    sqlBuilder.toString(), params.toArray());

            printQueryResult("查询到 " + historyList.size() + " 条复习历史记录");

            // 7. 处理查询结果
            List<ReviewSession> sessions = new ArrayList<>();

            for (Map<String, Object> row : historyList) {
                String sessionId = (String) row.get("session_id");
                String languages = (String) row.get("languages");

                // 确定主要语言
                String primaryLanguage = "mixed";
                if (languages != null && !languages.isEmpty()) {
                    String[] langArray = languages.split(",");
                    if (langArray.length == 1) {
                        primaryLanguage = langArray[0];
                    }
                }

                // 构建详情信息
                Map<String, Object> details = new HashMap<>();
                details.put("languages", languages);

                ReviewSession session = new ReviewSession(
                        sessionId,
                        (String) row.get("mode"),
                        ((Number) row.get("total_words")).intValue(),
                        ((Number) row.get("correct_words")).intValue(),
                        Math.round(((Number) row.get("accuracy")).doubleValue() * 100.0) / 100.0,
                        ((Number) row.get("duration")).intValue(),
                        primaryLanguage,
                        row.get("completed_at").toString(),
                        details
                );

                sessions.add(session);
            }

            // 8. 计算分页信息
            int totalPages = (int) Math.ceil((double) total / pageSize);
            Pagination pagination = new Pagination(page, pageSize, total, totalPages);

            // 9. 构建响应数据
            ReviewHistoryData data = new ReviewHistoryData(sessions, pagination);
            ReviewHistoryResponse response = new ReviewHistoryResponse(true, "获取复习历史成功", data);

            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取复习历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReviewHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
