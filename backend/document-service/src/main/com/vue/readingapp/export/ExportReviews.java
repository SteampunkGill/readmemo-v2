package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/export")
public class ExportReviews {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到复习记录导出请求 ===");
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
    public static class ExportReviewsRequest {
        private List<Integer> review_ids;
        private String format = "csv";
        private Map<String, Object> template;
        private String date_range;

        public List<Integer> getReview_ids() { return review_ids; }
        public void setReview_ids(List<Integer> review_ids) { this.review_ids = review_ids; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Map<String, Object> getTemplate() { return template; }
        public void setTemplate(Map<String, Object> template) { this.template = template; }

        public String getDate_range() { return date_range; }
        public void setDate_range(String date_range) { this.date_range = date_range; }
    }

    // 错误响应DTO
    public static class ErrorResponse {
        private boolean success = false;
        private ErrorDetail error;

        public ErrorResponse(String code, String message, Map<String, Object> details) {
            this.error = new ErrorDetail(code, message, details);
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public ErrorDetail getError() { return error; }
        public void setError(ErrorDetail error) { this.error = error; }

        public static class ErrorDetail {
            private String code;
            private String message;
            private Map<String, Object> details;

            public ErrorDetail(String code, String message, Map<String, Object> details) {
                this.code = code;
                this.message = message;
                this.details = details;
            }

            public String getCode() { return code; }
            public void setCode(String code) { this.code = code; }

            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }

            public Map<String, Object> getDetails() { return details; }
            public void setDetails(Map<String, Object> details) { this.details = details; }
        }
    }

    // 复习会话信息DTO
    public static class ReviewSessionInfo {
        private int review_session_id;
        private int user_id;
        private LocalDateTime start_time;
        private LocalDateTime end_time;
        private int total_items;
        private int correct_count;
        private int wrong_count;
        private double accuracy;
        private String session_type;
        private LocalDateTime created_at;
        private List<ReviewItemInfo> review_items;

        // 构造函数
        public ReviewSessionInfo(int review_session_id, int user_id, LocalDateTime start_time,
                                 LocalDateTime end_time, int total_items, int correct_count,
                                 int wrong_count, double accuracy, String session_type, LocalDateTime created_at) {
            this.review_session_id = review_session_id;
            this.user_id = user_id;
            this.start_time = start_time;
            this.end_time = end_time;
            this.total_items = total_items;
            this.correct_count = correct_count;
            this.wrong_count = wrong_count;
            this.accuracy = accuracy;
            this.session_type = session_type;
            this.created_at = created_at;
            this.review_items = new ArrayList<>();
        }

        // getter和setter方法
        public int getReview_session_id() { return review_session_id; }
        public void setReview_session_id(int review_session_id) { this.review_session_id = review_session_id; }

        public int getUser_id() { return user_id; }
        public void setUser_id(int user_id) { this.user_id = user_id; }

        public LocalDateTime getStart_time() { return start_time; }
        public void setStart_time(LocalDateTime start_time) { this.start_time = start_time; }

        public LocalDateTime getEnd_time() { return end_time; }
        public void setEnd_time(LocalDateTime end_time) { this.end_time = end_time; }

        public int getTotal_items() { return total_items; }
        public void setTotal_items(int total_items) { this.total_items = total_items; }

        public int getCorrect_count() { return correct_count; }
        public void setCorrect_count(int correct_count) { this.correct_count = correct_count; }

        public int getWrong_count() { return wrong_count; }
        public void setWrong_count(int wrong_count) { this.wrong_count = wrong_count; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public String getSession_type() { return session_type; }
        public void setSession_type(String session_type) { this.session_type = session_type; }

        public LocalDateTime getCreated_at() { return created_at; }
        public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }

        public List<ReviewItemInfo> getReview_items() { return review_items; }
        public void setReview_items(List<ReviewItemInfo> review_items) { this.review_items = review_items; }
    }

    // 复习项目信息DTO
    public static class ReviewItemInfo {
        private int review_item_id;
        private int review_session_id;
        private int user_vocabulary_id;
        private String word;
        private String answer;
        private boolean is_correct;
        private int response_time;
        private LocalDateTime created_at;

        public ReviewItemInfo(int review_item_id, int review_session_id, int user_vocabulary_id,
                              String word, String answer, boolean is_correct, int response_time, LocalDateTime created_at) {
            this.review_item_id = review_item_id;
            this.review_session_id = review_session_id;
            this.user_vocabulary_id = user_vocabulary_id;
            this.word = word;
            this.answer = answer;
            this.is_correct = is_correct;
            this.response_time = response_time;
            this.created_at = created_at;
        }

        // getter和setter方法
        public int getReview_item_id() { return review_item_id; }
        public void setReview_item_id(int review_item_id) { this.review_item_id = review_item_id; }

        public int getReview_session_id() { return review_session_id; }
        public void setReview_session_id(int review_session_id) { this.review_session_id = review_session_id; }

        public int getUser_vocabulary_id() { return user_vocabulary_id; }
        public void setUser_vocabulary_id(int user_vocabulary_id) { this.user_vocabulary_id = user_vocabulary_id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }

        public boolean isIs_correct() { return is_correct; }
        public void setIs_correct(boolean is_correct) { this.is_correct = is_correct; }

        public int getResponse_time() { return response_time; }
        public void setResponse_time(int response_time) { this.response_time = response_time; }

        public LocalDateTime getCreated_at() { return created_at; }
        public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }
    }

    // 支持的格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
            "csv", "json", "pdf", "xlsx"
    ));

    @PostMapping("/reviews/batch")
    public ResponseEntity<?> exportReviews(@RequestBody ExportReviewsRequest request,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> details = new HashMap<>();
                details.put("auth", "缺少有效的认证令牌");
                ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", "未授权，需要重新登录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String token = authHeader.substring(7);

            // 验证token并获取用户ID
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token);

            if (sessions.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("token", "令牌无效或已过期");
                ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", "未授权，需要重新登录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证请求参数
            if (request.getReview_ids() == null || request.getReview_ids().isEmpty()) {
                // 如果没有提供review_ids，则根据date_range查询
                if (request.getDate_range() == null || request.getDate_range().isEmpty()) {
                    Map<String, Object> details = new HashMap<>();
                    details.put("review_ids", "复习记录ID列表不能为空");
                    details.put("date_range", "日期范围不能为空");
                    ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                    printResponse(errorResponse);
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            if (!SUPPORTED_FORMATS.contains(request.getFormat().toLowerCase())) {
                Map<String, Object> details = new HashMap<>();
                details.put("format", "不支持的导出格式: " + request.getFormat());
                details.put("supported_formats", SUPPORTED_FORMATS);
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 查询复习记录
            List<ReviewSessionInfo> reviewSessions = new ArrayList<>();

            if (request.getReview_ids() != null && !request.getReview_ids().isEmpty()) {
                // 根据提供的review_ids查询
                String placeholders = String.join(",", Collections.nCopies(request.getReview_ids().size(), "?"));

                // 查询复习会话基本信息
                String sessionSql = "SELECT rs.review_session_id, rs.user_id, rs.start_time, rs.end_time, " +
                        "rs.total_items, rs.correct_count, rs.wrong_count, rs.accuracy, " +
                        "rs.session_type, rs.created_at " +
                        "FROM review_sessions rs " +
                        "WHERE rs.review_session_id IN (" + placeholders + ") AND rs.user_id = ?";

                List<Object> params = new ArrayList<>(request.getReview_ids());
                params.add(userId);

                List<Map<String, Object>> sessionResults = jdbcTemplate.queryForList(sessionSql, params.toArray());
                printQueryResult("复习会话查询结果: " + sessionResults.size() + "条记录");

                // 处理每个复习会话
                for (Map<String, Object> session : sessionResults) {
                    int sessionId = (int) session.get("review_session_id");
                    int sessionUserId = (int) session.get("user_id");
                    LocalDateTime startTime = ((java.sql.Timestamp) session.get("start_time")).toLocalDateTime();
                    LocalDateTime endTime = session.get("end_time") != null ?
                            ((java.sql.Timestamp) session.get("end_time")).toLocalDateTime() : null;
                    int totalItems = (int) session.get("total_items");
                    int correctCount = (int) session.get("correct_count");
                    int wrongCount = (int) session.get("wrong_count");
                    double accuracy = (double) session.get("accuracy");
                    String sessionType = (String) session.get("session_type");
                    LocalDateTime createdAt = ((java.sql.Timestamp) session.get("created_at")).toLocalDateTime();

                    ReviewSessionInfo sessionInfo = new ReviewSessionInfo(
                            sessionId, sessionUserId, startTime, endTime, totalItems,
                            correctCount, wrongCount, accuracy, sessionType, createdAt
                    );

                    // 查询复习项目详情
                    String itemsSql = "SELECT ri.review_item_id, ri.review_session_id, ri.user_vocabulary_id, " +
                            "w.word, ri.user_answer as answer, ri.is_correct, " +
                            "ri.response_time, ri.created_at " +
                            "FROM review_items ri " +
                            "JOIN user_vocabulary uv ON ri.user_vocabulary_id = uv.user_vocabulary_id " +
                            "JOIN words w ON uv.word_id = w.word_id " +
                            "WHERE ri.review_session_id = ?";

                    List<Map<String, Object>> itemResults = jdbcTemplate.queryForList(itemsSql, sessionId);

                    for (Map<String, Object> item : itemResults) {
                        int itemId = (int) item.get("review_item_id");
                        int itemSessionId = (int) item.get("review_session_id");
                        int userVocabId = (int) item.get("user_vocabulary_id");
                        String word = (String) item.get("word");
                        String answer = (String) item.get("answer");
                        boolean isCorrect = (boolean) item.get("is_correct");
                        int responseTime = (int) item.get("response_time");
                        LocalDateTime itemCreatedAt = ((java.sql.Timestamp) item.get("created_at")).toLocalDateTime();

                        ReviewItemInfo itemInfo = new ReviewItemInfo(
                                itemId, itemSessionId, userVocabId, word, answer,
                                isCorrect, responseTime, itemCreatedAt
                        );

                        sessionInfo.getReview_items().add(itemInfo);
                    }

                    reviewSessions.add(sessionInfo);
                }
            } else {
                // 根据date_range查询
                String[] dateRange = request.getDate_range().split(",");
                if (dateRange.length != 2) {
                    Map<String, Object> details = new HashMap<>();
                    details.put("date_range", "日期范围格式错误，应为'开始日期,结束日期'");
                    ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                    printResponse(errorResponse);
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                String startDate = dateRange[0].trim();
                String endDate = dateRange[1].trim();

                // 查询指定日期范围内的复习会话
                String sessionSql = "SELECT rs.review_session_id, rs.user_id, rs.start_time, rs.end_time, " +
                        "rs.total_items, rs.correct_count, rs.wrong_count, rs.accuracy, " +
                        "rs.session_type, rs.created_at " +
                        "FROM review_sessions rs " +
                        "WHERE rs.user_id = ? AND DATE(rs.created_at) BETWEEN ? AND ? " +
                        "ORDER BY rs.created_at DESC";

                List<Map<String, Object>> sessionResults = jdbcTemplate.queryForList(
                        sessionSql, userId, startDate, endDate
                );

                printQueryResult("日期范围复习会话查询结果: " + sessionResults.size() + "条记录");

                // 处理每个复习会话
                for (Map<String, Object> session : sessionResults) {
                    int sessionId = (int) session.get("review_session_id");
                    int sessionUserId = (int) session.get("user_id");
                    LocalDateTime startTime = ((java.sql.Timestamp) session.get("start_time")).toLocalDateTime();
                    LocalDateTime endTime = session.get("end_time") != null ?
                            ((java.sql.Timestamp) session.get("end_time")).toLocalDateTime() : null;
                    int totalItems = (int) session.get("total_items");
                    int correctCount = (int) session.get("correct_count");
                    int wrongCount = (int) session.get("wrong_count");
                    double accuracy = (double) session.get("accuracy");
                    String sessionType = (String) session.get("session_type");
                    LocalDateTime createdAt = ((java.sql.Timestamp) session.get("created_at")).toLocalDateTime();

                    ReviewSessionInfo sessionInfo = new ReviewSessionInfo(
                            sessionId, sessionUserId, startTime, endTime, totalItems,
                            correctCount, wrongCount, accuracy, sessionType, createdAt
                    );

                    // 查询复习项目详情
                    String itemsSql = "SELECT ri.review_item_id, ri.review_session_id, ri.user_vocabulary_id, " +
                            "w.word, ri.user_answer as answer, ri.is_correct, " +
                            "ri.response_time, ri.created_at " +
                            "FROM review_items ri " +
                            "JOIN user_vocabulary uv ON ri.user_vocabulary_id = uv.user_vocabulary_id " +
                            "JOIN words w ON uv.word_id = w.word_id " +
                            "WHERE ri.review_session_id = ?";

                    List<Map<String, Object>> itemResults = jdbcTemplate.queryForList(itemsSql, sessionId);

                    for (Map<String, Object> item : itemResults) {
                        int itemId = (int) item.get("review_item_id");
                        int itemSessionId = (int) item.get("review_session_id");
                        int userVocabId = (int) item.get("user_vocabulary_id");
                        String word = (String) item.get("word");
                        String answer = (String) item.get("answer");
                        boolean isCorrect = (boolean) item.get("is_correct");
                        int responseTime = (int) item.get("response_time");
                        LocalDateTime itemCreatedAt = ((java.sql.Timestamp) item.get("created_at")).toLocalDateTime();

                        ReviewItemInfo itemInfo = new ReviewItemInfo(
                                itemId, itemSessionId, userVocabId, word, answer,
                                isCorrect, responseTime, itemCreatedAt
                        );

                        sessionInfo.getReview_items().add(itemInfo);
                    }

                    reviewSessions.add(sessionInfo);
                }
            }

            if (reviewSessions.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("review_ids", request.getReview_ids());
                details.put("date_range", request.getDate_range());
                ErrorResponse errorResponse = new ErrorResponse("REVIEWS_NOT_FOUND", "没有找到可导出的复习记录", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 4. 生成导出文件内容
            String fileContent = generateExportContent(reviewSessions, request.getFormat());
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 5. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String extension = getFileExtension(request.getFormat());
            String filename = "reviews_" + timestamp + "." + extension;

            // 6. 记录导出历史
            String exportId = "export_reviews_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            // 构建实体ID数组
            List<Integer> sessionIds = reviewSessions.stream()
                    .map(ReviewSessionInfo::getReview_session_id)
                    .collect(java.util.stream.Collectors.toList());

            String entityIdsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_ARRAY(?)", String.class,
                    String.join(",", sessionIds.stream().map(String::valueOf).toArray(String[]::new))
            );

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("session_count", reviewSessions.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);
            exportDetails.put("date_range", request.getDate_range());

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "session_count", reviewSessions.size(),
                    "filename", filename,
                    "file_size", fileBytes.length,
                    "date_range", request.getDate_range() != null ? request.getDate_range() : ""
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "REVIEWS",
                    entityIdsJson,
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 7. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(request.getFormat()));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("format", request.getFormat());
            responseInfo.put("session_count", reviewSessions.size());
            responseInfo.put("total_items", reviewSessions.stream().mapToInt(ReviewSessionInfo::getTotal_items).sum());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("复习记录导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("REVIEWS_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成导出内容
    private String generateExportContent(List<ReviewSessionInfo> reviewSessions, String format) {
        StringBuilder content = new StringBuilder();

        if (format.equalsIgnoreCase("json")) {
            // JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("metadata", new HashMap<String, Object>() {{
                put("exportedAt", LocalDateTime.now().toString());
                put("totalSessions", reviewSessions.size());
                put("totalItems", reviewSessions.stream().mapToInt(ReviewSessionInfo::getTotal_items).sum());
                put("format", "json");
                put("version", "1.0");
            }});

            List<Map<String, Object>> sessionsJsonList = new ArrayList<>();
            for (ReviewSessionInfo session : reviewSessions) {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("review_session_id", session.getReview_session_id());
                sessionMap.put("user_id", session.getUser_id());
                sessionMap.put("start_time", session.getStart_time().toString());
                sessionMap.put("end_time", session.getEnd_time() != null ? session.getEnd_time().toString() : null);
                sessionMap.put("total_items", session.getTotal_items());
                sessionMap.put("correct_count", session.getCorrect_count());
                sessionMap.put("wrong_count", session.getWrong_count());
                sessionMap.put("accuracy", session.getAccuracy());
                sessionMap.put("session_type", session.getSession_type());
                sessionMap.put("created_at", session.getCreated_at().toString());

                // 添加复习项目
                List<Map<String, Object>> itemsJsonList = new ArrayList<>();
                for (ReviewItemInfo item : session.getReview_items()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("review_item_id", item.getReview_item_id());
                    itemMap.put("review_session_id", item.getReview_session_id());
                    itemMap.put("user_vocabulary_id", item.getUser_vocabulary_id());
                    itemMap.put("word", item.getWord());
                    itemMap.put("answer", item.getAnswer());
                    itemMap.put("is_correct", item.isIs_correct());
                    itemMap.put("response_time", item.getResponse_time());
                    itemMap.put("created_at", item.getCreated_at().toString());
                    itemsJsonList.add(itemMap);
                }
                sessionMap.put("review_items", itemsJsonList);

                sessionsJsonList.add(sessionMap);
            }

            exportData.put("review_sessions", sessionsJsonList);

            // 简单JSON序列化
            content.append("{\n");
            content.append("  \"metadata\": {\n");
            content.append("    \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");
            content.append("    \"totalSessions\": ").append(reviewSessions.size()).append(",\n");
            content.append("    \"totalItems\": ").append(reviewSessions.stream().mapToInt(ReviewSessionInfo::getTotal_items).sum()).append(",\n");
            content.append("    \"format\": \"json\",\n");
            content.append("    \"version\": \"1.0\"\n");
            content.append("  },\n");
            content.append("  \"review_sessions\": [\n");

            for (int i = 0; i < reviewSessions.size(); i++) {
                ReviewSessionInfo session = reviewSessions.get(i);
                content.append("    {\n");
                content.append("      \"review_session_id\": ").append(session.getReview_session_id()).append(",\n");
                content.append("      \"user_id\": ").append(session.getUser_id()).append(",\n");
                content.append("      \"start_time\": \"").append(session.getStart_time()).append("\",\n");
                content.append("      \"end_time\": \"").append(session.getEnd_time() != null ? session.getEnd_time() : "").append("\",\n");
                content.append("      \"total_items\": ").append(session.getTotal_items()).append(",\n");
                content.append("      \"correct_count\": ").append(session.getCorrect_count()).append(",\n");
                content.append("      \"wrong_count\": ").append(session.getWrong_count()).append(",\n");
                content.append("      \"accuracy\": ").append(session.getAccuracy()).append(",\n");
                content.append("      \"session_type\": \"").append(escapeJson(session.getSession_type())).append("\",\n");
                content.append("      \"created_at\": \"").append(session.getCreated_at()).append("\"\n");
                content.append("    }");
                if (i < reviewSessions.size() - 1) content.append(",");
                content.append("\n");
            }

            content.append("  ]\n");
            content.append("}");

        } else if (format.equalsIgnoreCase("csv")) {
            // CSV格式 - 会话概要
            content.append("复习会话ID,用户ID,开始时间,结束时间,总项目数,正确数,错误数,正确率,会话类型,创建时间\n");
            for (ReviewSessionInfo session : reviewSessions) {
                content.append(session.getReview_session_id()).append(",");
                content.append(session.getUser_id()).append(",");
                content.append(escapeCsv(session.getStart_time().toString())).append(",");
                content.append(escapeCsv(session.getEnd_time() != null ? session.getEnd_time().toString() : "")).append(",");
                content.append(session.getTotal_items()).append(",");
                content.append(session.getCorrect_count()).append(",");
                content.append(session.getWrong_count()).append(",");
                content.append(String.format("%.2f", session.getAccuracy())).append(",");
                content.append(escapeCsv(session.getSession_type())).append(",");
                content.append(escapeCsv(session.getCreated_at().toString())).append("\n");
            }

            // 添加复习项目详情
            content.append("\n\n=== 复习项目详情 ===\n");
            content.append("复习项目ID,复习会话ID,用户词汇ID,单词,用户答案,是否正确,响应时间(秒),创建时间\n");
            for (ReviewSessionInfo session : reviewSessions) {
                for (ReviewItemInfo item : session.getReview_items()) {
                    content.append(item.getReview_item_id()).append(",");
                    content.append(item.getReview_session_id()).append(",");
                    content.append(item.getUser_vocabulary_id()).append(",");
                    content.append(escapeCsv(item.getWord())).append(",");
                    content.append(escapeCsv(item.getAnswer())).append(",");
                    content.append(item.isIs_correct() ? "是" : "否").append(",");
                    content.append(item.getResponse_time()).append(",");
                    content.append(escapeCsv(item.getCreated_at().toString())).append("\n");
                }
            }

            // 添加统计摘要
            content.append("\n\n=== 统计摘要 ===\n");
            content.append("统计项,数值\n");
            content.append("总复习会话数,").append(reviewSessions.size()).append("\n");

            int totalItems = reviewSessions.stream().mapToInt(ReviewSessionInfo::getTotal_items).sum();
            int totalCorrect = reviewSessions.stream().mapToInt(ReviewSessionInfo::getCorrect_count).sum();
            int totalWrong = reviewSessions.stream().mapToInt(ReviewSessionInfo::getWrong_count).sum();
            double overallAccuracy = totalItems > 0 ? (totalCorrect * 100.0 / totalItems) : 0.0;

            content.append("总复习项目数,").append(totalItems).append("\n");
            content.append("总正确数,").append(totalCorrect).append("\n");
            content.append("总错误数,").append(totalWrong).append("\n");
            content.append("整体正确率,").append(String.format("%.2f", overallAccuracy)).append("%\n");

            // 按会话类型统计
            Map<String, Integer> typeCount = new HashMap<>();
            Map<String, Integer> typeItems = new HashMap<>();
            Map<String, Integer> typeCorrect = new HashMap<>();

            for (ReviewSessionInfo session : reviewSessions) {
                String type = session.getSession_type();
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
                typeItems.put(type, typeItems.getOrDefault(type, 0) + session.getTotal_items());
                typeCorrect.put(type, typeCorrect.getOrDefault(type, 0) + session.getCorrect_count());
            }

            content.append("\n按会话类型统计:\n");
            content.append("会话类型,会话数,项目数,正确数,正确率\n");
            for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                String type = entry.getKey();
                int sessionsCount = entry.getValue();
                int itemsCount = typeItems.getOrDefault(type, 0);
                int correctCount = typeCorrect.getOrDefault(type, 0);
                double typeAccuracy = itemsCount > 0 ? (correctCount * 100.0 / itemsCount) : 0.0;

                content.append(escapeCsv(type)).append(",");
                content.append(sessionsCount).append(",");
                content.append(itemsCount).append(",");
                content.append(correctCount).append(",");
                content.append(String.format("%.2f", typeAccuracy)).append("%\n");
            }

        } else {
            // 默认文本格式（用于pdf和txt）
            content.append("复习记录导出报告\n");
            content.append("================\n\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("复习会话数: ").append(reviewSessions.size()).append("\n");
            content.append("导出格式: ").append(format).append("\n\n");

            // 总体统计
            int totalItems = reviewSessions.stream().mapToInt(ReviewSessionInfo::getTotal_items).sum();
            int totalCorrect = reviewSessions.stream().mapToInt(ReviewSessionInfo::getCorrect_count).sum();
            int totalWrong = reviewSessions.stream().mapToInt(ReviewSessionInfo::getWrong_count).sum();
            double overallAccuracy = totalItems > 0 ? (totalCorrect * 100.0 / totalItems) : 0.0;

            content.append("总体统计:\n");
            content.append("--------\n");
            content.append("总复习项目数: ").append(totalItems).append("\n");
            content.append("总正确数: ").append(totalCorrect).append("\n");
            content.append("总错误数: ").append(totalWrong).append("\n");
            content.append("整体正确率: ").append(String.format("%.2f", overallAccuracy)).append("%\n\n");

            content.append("复习会话列表:\n");
            content.append("------------\n");

            for (int i = 0; i < reviewSessions.size(); i++) {
                ReviewSessionInfo session = reviewSessions.get(i);
                content.append(i + 1).append(". 会话ID: ").append(session.getReview_session_id()).append("\n");
                content.append("   开始时间: ").append(session.getStart_time()).append("\n");
                content.append("   结束时间: ").append(session.getEnd_time() != null ? session.getEnd_time() : "进行中").append("\n");
                content.append("   会话类型: ").append(session.getSession_type()).append("\n");
                content.append("   项目总数: ").append(session.getTotal_items()).append("\n");
                content.append("   正确数: ").append(session.getCorrect_count()).append(" | ");
                content.append("错误数: ").append(session.getWrong_count()).append("\n");
                content.append("   正确率: ").append(String.format("%.2f", session.getAccuracy())).append("%\n");
                content.append("   创建时间: ").append(session.getCreated_at()).append("\n");

                // 显示前3个复习项目
                if (!session.getReview_items().isEmpty()) {
                    content.append("   复习项目:\n");
                    int limit = Math.min(3, session.getReview_items().size());
                    for (int j = 0; j < limit; j++) {
                        ReviewItemInfo item = session.getReview_items().get(j);
                        content.append("     - ").append(item.getWord()).append(": ");
                        content.append(item.getAnswer()).append(" (");
                        content.append(item.isIs_correct() ? "正确" : "错误").append(")\n");
                    }
                    if (session.getReview_items().size() > 3) {
                        content.append("     ... 还有").append(session.getReview_items().size() - 3).append("个项目\n");
                    }
                }

                content.append("\n");
            }

            // 按会话类型统计
            Map<String, Integer> typeCount = new HashMap<>();
            Map<String, Integer> typeItems = new HashMap<>();
            Map<String, Integer> typeCorrect = new HashMap<>();

            for (ReviewSessionInfo session : reviewSessions) {
                String type = session.getSession_type();
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
                typeItems.put(type, typeItems.getOrDefault(type, 0) + session.getTotal_items());
                typeCorrect.put(type, typeCorrect.getOrDefault(type, 0) + session.getCorrect_count());
            }

            content.append("\n按会话类型统计:\n");
            content.append("------------\n");
            for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                String type = entry.getKey();
                int sessionsCount = entry.getValue();
                int itemsCount = typeItems.getOrDefault(type, 0);
                int correctCount = typeCorrect.getOrDefault(type, 0);
                double typeAccuracy = itemsCount > 0 ? (correctCount * 100.0 / itemsCount) : 0.0;

                content.append("类型: ").append(type).append("\n");
                content.append("  会话数: ").append(sessionsCount).append("\n");
                content.append("  项目数: ").append(itemsCount).append("\n");
                content.append("  正确率: ").append(String.format("%.2f", typeAccuracy)).append("%\n\n");
            }
        }

        return content.toString();
    }

    // 获取文件扩展名
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "csv": return "csv";
            case "json": return "json";
            case "pdf": return "pdf";
            case "xlsx": return "xlsx";
            default: return "txt";
        }
    }

    // 获取媒体类型
    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "csv": return MediaType.parseMediaType("text/csv");
            case "json": return MediaType.APPLICATION_JSON;
            case "pdf": return MediaType.APPLICATION_PDF;
            case "xlsx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default: return MediaType.TEXT_PLAIN;
        }
    }

    // 转义JSON字符串
    private String escapeJson(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // 转义CSV字符串
    private String escapeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}