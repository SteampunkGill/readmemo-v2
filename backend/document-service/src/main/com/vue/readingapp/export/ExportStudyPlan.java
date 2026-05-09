package com.vue.readingapp.export;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/export")
public class ExportStudyPlan {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到学习计划导出请求 ===");
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
    public static class ExportStudyPlanRequest {
        private String start_date;
        private String end_date;
        private int daily_goal = 20;
        private boolean include_progress = true;

        public String getStart_date() { return start_date; }
        public void setStart_date(String start_date) { this.start_date = start_date; }

        public String getEnd_date() { return end_date; }
        public void setEnd_date(String end_date) { this.end_date = end_date; }

        public int getDaily_goal() { return daily_goal; }
        public void setDaily_goal(int daily_goal) { this.daily_goal = daily_goal; }

        public boolean isInclude_progress() { return include_progress; }
        public void setInclude_progress(boolean include_progress) { this.include_progress = include_progress; }
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

    @PostMapping("/study-plan")
    public ResponseEntity<?> exportStudyPlan(@RequestBody ExportStudyPlanRequest request,
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
            if (request.getStart_date() == null || request.getStart_date().isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("start_date", "开始日期不能为空");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getEnd_date() == null || request.getEnd_date().isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("end_date", "结束日期不能为空");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getDaily_goal() <= 0 || request.getDaily_goal() > 100) {
                Map<String, Object> details = new HashMap<>();
                details.put("daily_goal", "每日目标应在1-100之间");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 查询需要复习的词汇
            LocalDateTime startDate = LocalDateTime.parse(request.getStart_date() + "T00:00:00");
            LocalDateTime endDate = LocalDateTime.parse(request.getEnd_date() + "T23:59:59");

            // 查询所有需要复习的词汇
            String dueWordsSql = "SELECT uv.user_vocabulary_id, w.word, w.phonetic, wd.definition as meaning, " +
                    "uv.mastery_level, uv.next_review_date " +
                    "FROM user_vocabulary uv " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id AND wd.is_primary = 1 " +
                    "WHERE uv.user_id = ? AND uv.next_review_date IS NOT NULL " +
                    "AND uv.next_review_date BETWEEN ? AND ? " +
                    "ORDER BY uv.next_review_date";

            List<Map<String, Object>> dueWords = jdbcTemplate.queryForList(
                    dueWordsSql, userId, startDate, endDate
            );

            printQueryResult("需要复习的词汇: " + dueWords.size() + "条记录");

            // 4. 生成学习计划
            Map<String, List<Map<String, Object>>> dailyPlan = generateDailyPlan(dueWords, request.getDaily_goal(), startDate, endDate);

            // 5. 生成导出文件内容
            String fileContent = generateStudyPlanContent(dailyPlan, request);
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 6. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "study_plan_" + timestamp + ".txt";

            // 7. 记录导出历史
            String exportId = "export_study_plan_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?)";

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", "text");
            exportDetails.put("start_date", request.getStart_date());
            exportDetails.put("end_date", request.getEnd_date());
            exportDetails.put("daily_goal", request.getDaily_goal());
            exportDetails.put("total_words", dueWords.size());
            exportDetails.put("plan_days", dailyPlan.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", "text",
                    "start_date", request.getStart_date(),
                    "end_date", request.getEnd_date(),
                    "daily_goal", request.getDaily_goal(),
                    "total_words", dueWords.size(),
                    "plan_days", dailyPlan.size(),
                    "filename", filename,
                    "file_size", fileBytes.length
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "STUDY_PLAN",
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 8. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("start_date", request.getStart_date());
            responseInfo.put("end_date", request.getEnd_date());
            responseInfo.put("daily_goal", request.getDaily_goal());
            responseInfo.put("total_words", dueWords.size());
            responseInfo.put("plan_days", dailyPlan.size());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("学习计划导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("STUDY_PLAN_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成每日学习计划
    private Map<String, List<Map<String, Object>>> generateDailyPlan(List<Map<String, Object>> dueWords, int dailyGoal,
                                                                     LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, List<Map<String, Object>>> dailyPlan = new LinkedHashMap<>();

        // 按复习日期分组
        Map<String, List<Map<String, Object>>> wordsByDate = new HashMap<>();

        for (Map<String, Object> word : dueWords) {
            LocalDateTime reviewDate = ((java.sql.Timestamp) word.get("next_review_date")).toLocalDateTime();
            String dateKey = reviewDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            if (!wordsByDate.containsKey(dateKey)) {
                wordsByDate.put(dateKey, new ArrayList<>());
            }
            wordsByDate.get(dateKey).add(word);
        }

        // 生成日期范围内的计划
        LocalDateTime currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateKey = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            List<Map<String, Object>> dayWords = new ArrayList<>();

            // 添加当天需要复习的词汇
            if (wordsByDate.containsKey(dateKey)) {
                List<Map<String, Object>> dueToday = wordsByDate.get(dateKey);

                // 如果当天词汇超过每日目标，按优先级排序
                if (dueToday.size() > dailyGoal) {
                    // 按掌握程度排序（掌握度低的优先）
                    dueToday.sort((a, b) -> {
                        int masteryA = ((Number) a.get("mastery_level")).intValue();
                        int masteryB = ((Number) b.get("mastery_level")).intValue();
                        return Integer.compare(masteryA, masteryB);
                    });

                    // 取前dailyGoal个
                    dayWords.addAll(dueToday.subList(0, Math.min(dailyGoal, dueToday.size())));
                } else {
                    dayWords.addAll(dueToday);
                }
            }

            dailyPlan.put(dateKey, dayWords);
            currentDate = currentDate.plusDays(1);
        }

        return dailyPlan;
    }

    // 生成学习计划内容
    private String generateStudyPlanContent(Map<String, List<Map<String, Object>>> dailyPlan, ExportStudyPlanRequest request) {
        StringBuilder content = new StringBuilder();

        content.append("学习计划\n");
        content.append("========\n\n");
        content.append("计划周期: ").append(request.getStart_date()).append(" 至 ").append(request.getEnd_date()).append("\n");
        content.append("每日目标: ").append(request.getDaily_goal()).append(" 个词汇\n");
        content.append("生成时间: ").append(LocalDateTime.now()).append("\n\n");

        int totalWords = 0;
        int planDays = dailyPlan.size();

        content.append("每日学习安排:\n");
        content.append("------------\n\n");

        for (Map.Entry<String, List<Map<String, Object>>> entry : dailyPlan.entrySet()) {
            String date = entry.getKey();
            List<Map<String, Object>> words = entry.getValue();

            content.append(date).append(" (").append(words.size()).append(" 个词汇):\n");

            if (words.isEmpty()) {
                content.append("  无需要复习的词汇\n\n");
                continue;
            }

            for (int i = 0; i < words.size(); i++) {
                Map<String, Object> word = words.get(i);
                content.append("  ").append(i + 1).append(". ").append(word.get("word")).append(" ");

                if (word.get("phonetic") != null && !((String) word.get("phonetic")).isEmpty()) {
                    content.append("[").append(word.get("phonetic")).append("] ");
                }

                if (word.get("part_of_speech") != null && !((String) word.get("part_of_speech")).isEmpty()) {
                    content.append("(").append(word.get("part_of_speech")).append(") ");
                }

                content.append("\n");

                if (word.get("meaning") != null) {
                    content.append("     释义: ").append(word.get("meaning")).append("\n");
                }

                if (word.get("mastery_level") != null) {
                    content.append("     掌握度: ").append(word.get("mastery_level")).append("%\n");
                }
            }

            content.append("\n");
            totalWords += words.size();
        }

        content.append("计划总结:\n");
        content.append("--------\n");
        content.append("总计划天数: ").append(planDays).append(" 天\n");
        content.append("总复习词汇: ").append(totalWords).append(" 个\n");
        content.append("平均每日: ").append(String.format("%.1f", (double) totalWords / planDays)).append(" 个词汇\n");

        if (request.isInclude_progress()) {
            content.append("\n进度建议:\n");
            content.append("--------\n");

            if (totalWords == 0) {
                content.append("当前没有需要复习的词汇，可以学习新词汇。\n");
            } else if (totalWords <= request.getDaily_goal() * planDays / 2) {
                content.append("复习任务较轻，建议每天额外学习 ").append(request.getDaily_goal() - (totalWords / planDays)).append(" 个新词汇。\n");
            } else if (totalWords <= request.getDaily_goal() * planDays) {
                content.append("复习任务适中，按计划完成即可。\n");
            } else {
                content.append("复习任务较重，建议适当增加每日学习时间。\n");
            }
        }

        return content.toString();
    }
}