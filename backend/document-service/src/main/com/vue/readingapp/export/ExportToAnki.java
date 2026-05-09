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
public class ExportToAnki {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到Anki导出请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("====================");
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
    public static class ExportToAnkiRequest {
        private List<Integer> vocabulary_ids;
        private String deck_name = "语言学习";
        private boolean include_audio = true;
        private boolean include_images = true;

        public List<Integer> getVocabulary_ids() { return vocabulary_ids; }
        public void setVocabulary_ids(List<Integer> vocabulary_ids) { this.vocabulary_ids = vocabulary_ids; }

        public String getDeck_name() { return deck_name; }
        public void setDeck_name(String deck_name) { this.deck_name = deck_name; }

        public boolean isInclude_audio() { return include_audio; }
        public void setInclude_audio(boolean include_audio) { this.include_audio = include_audio; }

        public boolean isInclude_images() { return include_images; }
        public void setInclude_images(boolean include_images) { this.include_images = include_images; }
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

    @PostMapping("/anki")
    public ResponseEntity<?> exportToAnki(@RequestBody ExportToAnkiRequest request,
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
            if (request.getVocabulary_ids() == null || request.getVocabulary_ids().isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("vocabulary_ids", "词汇ID列表不能为空");
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (request.getVocabulary_ids().size() > 500) {
                Map<String, Object> details = new HashMap<>();
                details.put("vocabulary_ids", "最多支持导出500个词汇");
                details.put("limit", 500);
                ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", "参数错误", details);
                printResponse(errorResponse);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 3. 验证词汇是否存在且属于当前用户
            List<Integer> invalidVocabIds = new ArrayList<>();
            List<Integer> validVocabIds = new ArrayList<>();

            for (Integer vocabId : request.getVocabulary_ids()) {
                String vocabSql = "SELECT user_vocabulary_id FROM user_vocabulary WHERE user_vocabulary_id = ? AND user_id = ?";
                List<Map<String, Object>> vocabs = jdbcTemplate.queryForList(vocabSql, vocabId, userId);

                if (vocabs.isEmpty()) {
                    invalidVocabIds.add(vocabId);
                } else {
                    validVocabIds.add(vocabId);
                }
            }

            if (!invalidVocabIds.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("vocabularyIds", invalidVocabIds);
                ErrorResponse errorResponse = new ErrorResponse("VOCABULARY_NOT_FOUND", "部分词汇不存在或无权限访问", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 4. 查询词汇信息
            String placeholders = String.join(",", Collections.nCopies(validVocabIds.size(), "?"));
            String vocabSql = "SELECT uv.user_vocabulary_id, w.word, w.phonetic, wd.definition as meaning, " +
                    "w.part_of_speech, we.example_sentence, we.translation " +
                    "FROM user_vocabulary uv " +
                    "JOIN words w ON uv.word_id = w.word_id " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id AND wd.is_primary = 1 " +
                    "LEFT JOIN word_examples we ON w.word_id = we.word_id " +
                    "WHERE uv.user_vocabulary_id IN (" + placeholders + ") AND uv.user_id = ? " +
                    "ORDER BY w.word";

            List<Object> params = new ArrayList<>(validVocabIds);
            params.add(userId);

            List<Map<String, Object>> vocabulary = jdbcTemplate.queryForList(vocabSql, params.toArray());
            printQueryResult("Anki导出词汇查询结果: " + vocabulary.size() + "条记录");

            if (vocabulary.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("vocabularyIds", validVocabIds);
                ErrorResponse errorResponse = new ErrorResponse("ANKI_EXPORT_ERROR", "没有找到可导出的词汇", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 5. 生成Anki导入文件（CSV格式）
            StringBuilder csvContent = new StringBuilder();

            // Anki CSV格式：单词,音标,释义,词性,例句,例句翻译
            csvContent.append("单词,音标,释义,词性,例句,例句翻译\n");

            for (Map<String, Object> vocab : vocabulary) {
                String word = escapeCsv(vocab.get("word"));
                String phonetic = escapeCsv(vocab.get("phonetic"));
                String meaning = escapeCsv(vocab.get("meaning"));
                String partOfSpeech = escapeCsv(vocab.get("part_of_speech"));
                String example = escapeCsv(vocab.get("example_sentence"));
                String translation = escapeCsv(vocab.get("translation"));

                csvContent.append(word).append(",");
                csvContent.append(phonetic).append(",");
                csvContent.append(meaning).append(",");
                csvContent.append(partOfSpeech).append(",");
                csvContent.append(example).append(",");
                csvContent.append(translation).append("\n");
            }

            byte[] fileBytes = csvContent.toString().getBytes(StandardCharsets.UTF_8);

            // 6. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "anki_export_" + timestamp + ".csv";

            // 7. 记录导出历史
            String exportId = "export_anki_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            String entityIdsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_ARRAY(?)", String.class,
                    String.join(",", validVocabIds.stream().map(String::valueOf).toArray(String[]::new))
            );

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", "anki_csv");
            exportDetails.put("deck_name", request.getDeck_name());
            exportDetails.put("item_count", vocabulary.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);
            exportDetails.put("include_audio", request.isInclude_audio());
            exportDetails.put("include_images", request.isInclude_images());

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", "anki_csv",
                    "deck_name", request.getDeck_name(),
                    "item_count", vocabulary.size(),
                    "filename", filename,
                    "file_size", fileBytes.length,
                    "include_audio", request.isInclude_audio(),
                    "include_images", request.isInclude_images()
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "ANKI",
                    entityIdsJson,
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 8. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("deck_name", request.getDeck_name());
            responseInfo.put("vocabulary_count", vocabulary.size());
            responseInfo.put("include_audio", request.isInclude_audio());
            responseInfo.put("include_images", request.isInclude_images());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Anki导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("ANKI_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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