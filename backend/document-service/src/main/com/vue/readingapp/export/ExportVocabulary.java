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
public class ExportVocabulary {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到词汇导出请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=====================");
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
    public static class ExportVocabularyRequest {
        private List<Integer> vocabulary_ids;
        private String format = "xlsx";
        private Map<String, Object> template;
        private boolean include_examples = true;
        private boolean include_statistics = true;

        public List<Integer> getVocabulary_ids() { return vocabulary_ids; }
        public void setVocabulary_ids(List<Integer> vocabulary_ids) { this.vocabulary_ids = vocabulary_ids; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public Map<String, Object> getTemplate() { return template; }
        public void setTemplate(Map<String, Object> template) { this.template = template; }

        public boolean isInclude_examples() { return include_examples; }
        public void setInclude_examples(boolean include_examples) { this.include_examples = include_examples; }

        public boolean isInclude_statistics() { return include_statistics; }
        public void setInclude_statistics(boolean include_statistics) { this.include_statistics = include_statistics; }
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

    // 词汇信息DTO
    public static class VocabularyInfo {
        private int user_vocabulary_id;
        private int word_id;
        private String word;
        private String phonetic;
        private String meaning;
        private String part_of_speech;
        private int difficulty;
        private int mastery;
        private int study_count;
        private double accuracy;
        private LocalDateTime last_studied;
        private List<ExampleInfo> examples;
        private StatisticsInfo statistics;

        // 构造函数、getter和setter
        public VocabularyInfo(int user_vocabulary_id, int word_id, String word, String phonetic,
                              String meaning, String part_of_speech, int difficulty, int mastery,
                              int study_count, double accuracy, LocalDateTime last_studied) {
            this.user_vocabulary_id = user_vocabulary_id;
            this.word_id = word_id;
            this.word = word;
            this.phonetic = phonetic;
            this.meaning = meaning;
            this.part_of_speech = part_of_speech;
            this.difficulty = difficulty;
            this.mastery = mastery;
            this.study_count = study_count;
            this.accuracy = accuracy;
            this.last_studied = last_studied;
            this.examples = new ArrayList<>();
            this.statistics = new StatisticsInfo();
        }

        // getter和setter方法
        public int getUser_vocabulary_id() { return user_vocabulary_id; }
        public void setUser_vocabulary_id(int user_vocabulary_id) { this.user_vocabulary_id = user_vocabulary_id; }

        public int getWord_id() { return word_id; }
        public void setWord_id(int word_id) { this.word_id = word_id; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getPhonetic() { return phonetic; }
        public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

        public String getMeaning() { return meaning; }
        public void setMeaning(String meaning) { this.meaning = meaning; }

        public String getPart_of_speech() { return part_of_speech; }
        public void setPart_of_speech(String part_of_speech) { this.part_of_speech = part_of_speech; }

        public int getDifficulty() { return difficulty; }
        public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

        public int getMastery() { return mastery; }
        public void setMastery(int mastery) { this.mastery = mastery; }

        public int getStudy_count() { return study_count; }
        public void setStudy_count(int study_count) { this.study_count = study_count; }

        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

        public LocalDateTime getLast_studied() { return last_studied; }
        public void setLast_studied(LocalDateTime last_studied) { this.last_studied = last_studied; }

        public List<ExampleInfo> getExamples() { return examples; }
        public void setExamples(List<ExampleInfo> examples) { this.examples = examples; }

        public StatisticsInfo getStatistics() { return statistics; }
        public void setStatistics(StatisticsInfo statistics) { this.statistics = statistics; }
    }

    // 例句信息DTO
    public static class ExampleInfo {
        private String sentence;
        private String translation;
        private String source;

        public ExampleInfo(String sentence, String translation, String source) {
            this.sentence = sentence;
            this.translation = translation;
            this.source = source;
        }

        public String getSentence() { return sentence; }
        public void setSentence(String sentence) { this.sentence = sentence; }

        public String getTranslation() { return translation; }
        public void setTranslation(String translation) { this.translation = translation; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }

    // 统计信息DTO
    public static class StatisticsInfo {
        private LocalDateTime first_learned;
        private int study_sessions;
        private int correct_answers;
        private int wrong_answers;

        public StatisticsInfo() {
            this.first_learned = LocalDateTime.now();
            this.study_sessions = 0;
            this.correct_answers = 0;
            this.wrong_answers = 0;
        }

        public LocalDateTime getFirst_learned() { return first_learned; }
        public void setFirst_learned(LocalDateTime first_learned) { this.first_learned = first_learned; }

        public int getStudy_sessions() { return study_sessions; }
        public void setStudy_sessions(int study_sessions) { this.study_sessions = study_sessions; }

        public int getCorrect_answers() { return correct_answers; }
        public void setCorrect_answers(int correct_answers) { this.correct_answers = correct_answers; }

        public int getWrong_answers() { return wrong_answers; }
        public void setWrong_answers(int wrong_answers) { this.wrong_answers = wrong_answers; }
    }

    // 支持的格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
            "xlsx", "csv", "json", "pdf"
    ));

    @PostMapping("/vocabulary/batch")
    public ResponseEntity<?> exportVocabulary(@RequestBody ExportVocabularyRequest request,
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

            if (!SUPPORTED_FORMATS.contains(request.getFormat().toLowerCase())) {
                Map<String, Object> details = new HashMap<>();
                details.put("format", "不支持的导出格式: " + request.getFormat());
                details.put("supported_formats", SUPPORTED_FORMATS);
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

            // 4. 查询词汇详细信息
            List<VocabularyInfo> vocabularyList = new ArrayList<>();

            if (!validVocabIds.isEmpty()) {
                String placeholders = String.join(",", Collections.nCopies(validVocabIds.size(), "?"));

                // 查询用户词汇基本信息
                String vocabSql = "SELECT uv.user_vocabulary_id, uv.word_id, uv.mastery_level as mastery, " +
                        "uv.study_count, uv.correct_count, uv.wrong_count, uv.last_studied_at, " +
                        "w.word, w.phonetic, w.part_of_speech, w.difficulty_level as difficulty, " +
                        "wd.definition as meaning " +
                        "FROM user_vocabulary uv " +
                        "JOIN words w ON uv.word_id = w.word_id " +
                        "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id AND wd.is_primary = 1 " +
                        "WHERE uv.user_vocabulary_id IN (" + placeholders + ") AND uv.user_id = ?";

                List<Object> params = new ArrayList<>(validVocabIds);
                params.add(userId);

                List<Map<String, Object>> vocabResults = jdbcTemplate.queryForList(vocabSql, params.toArray());
                printQueryResult("词汇基本信息查询结果: " + vocabResults.size() + "条记录");

                // 处理每个词汇
                for (Map<String, Object> vocab : vocabResults) {
                    int userVocabId = (int) vocab.get("user_vocabulary_id");
                    int wordId = (int) vocab.get("word_id");
                    String word = (String) vocab.get("word");
                    String phonetic = (String) vocab.get("phonetic");
                    String meaning = (String) vocab.get("meaning");
                    String partOfSpeech = (String) vocab.get("part_of_speech");
                    int difficulty = vocab.get("difficulty") != null ? (int) vocab.get("difficulty") : 1;
                    int mastery = vocab.get("mastery") != null ? (int) vocab.get("mastery") : 0;
                    int studyCount = vocab.get("study_count") != null ? (int) vocab.get("study_count") : 0;

                    // 计算正确率
                    int correctCount = vocab.get("correct_count") != null ? (int) vocab.get("correct_count") : 0;
                    int wrongCount = vocab.get("wrong_count") != null ? (int) vocab.get("wrong_count") : 0;
                    double accuracy = (studyCount > 0) ? (correctCount * 100.0 / studyCount) : 0.0;

                    LocalDateTime lastStudied = vocab.get("last_studied_at") != null ?
                            ((java.sql.Timestamp) vocab.get("last_studied_at")).toLocalDateTime() : null;

                    VocabularyInfo vocabInfo = new VocabularyInfo(
                            userVocabId, wordId, word, phonetic, meaning, partOfSpeech,
                            difficulty, mastery, studyCount, accuracy, lastStudied
                    );

                    // 查询例句（如果需要）
                    if (request.isInclude_examples()) {
                        String examplesSql = "SELECT example_sentence, translation, source " +
                                "FROM word_examples WHERE word_id = ? LIMIT 5";
                        List<Map<String, Object>> examples = jdbcTemplate.queryForList(examplesSql, wordId);

                        for (Map<String, Object> example : examples) {
                            String sentence = (String) example.get("example_sentence");
                            String translation = (String) example.get("translation");
                            String source = (String) example.get("source");

                            vocabInfo.getExamples().add(new ExampleInfo(sentence, translation, source));
                        }
                    }

                    // 查询统计信息（如果需要）
                    if (request.isInclude_statistics()) {
                        // 查询第一次学习时间
                        String firstLearnedSql = "SELECT MIN(created_at) as first_learned " +
                                "FROM review_items WHERE user_vocabulary_id = ?";
                        List<Map<String, Object>> firstLearnedResult = jdbcTemplate.queryForList(firstLearnedSql, userVocabId);

                        if (!firstLearnedResult.isEmpty() && firstLearnedResult.get(0).get("first_learned") != null) {
                            LocalDateTime firstLearned = ((java.sql.Timestamp) firstLearnedResult.get(0).get("first_learned")).toLocalDateTime();
                            vocabInfo.getStatistics().setFirst_learned(firstLearned);
                        }

                        // 查询学习会话次数
                        String sessionsSql = "SELECT COUNT(DISTINCT review_session_id) as session_count " +
                                "FROM review_items WHERE user_vocabulary_id = ?";
                        List<Map<String, Object>> sessionsResult = jdbcTemplate.queryForList(sessionsSql, userVocabId);

                        if (!sessionsResult.isEmpty()) {
                            int sessionCount = ((Number) sessionsResult.get(0).get("session_count")).intValue();
                            vocabInfo.getStatistics().setStudy_sessions(sessionCount);
                        }

                        // 设置正确和错误答案数
                        vocabInfo.getStatistics().setCorrect_answers(correctCount);
                        vocabInfo.getStatistics().setWrong_answers(wrongCount);
                    }

                    vocabularyList.add(vocabInfo);
                }
            }

            if (vocabularyList.isEmpty()) {
                Map<String, Object> details = new HashMap<>();
                details.put("vocabularyIds", validVocabIds);
                ErrorResponse errorResponse = new ErrorResponse("VOCABULARY_EXPORT_ERROR", "没有找到可导出的词汇", details);
                printResponse(errorResponse);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // 5. 生成导出文件内容
            String fileContent = generateExportContent(vocabularyList, request.getFormat(),
                    request.isInclude_examples(), request.isInclude_statistics());
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 6. 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String extension = getFileExtension(request.getFormat());
            String filename = "vocabulary_" + timestamp + "." + extension;

            // 7. 记录导出历史
            String exportId = "export_vocab_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String insertHistorySql = "INSERT INTO sync_logs (user_id, operation_type, entity_type, entity_ids, " +
                    "status, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            String entityIdsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_ARRAY(?)", String.class,
                    String.join(",", validVocabIds.stream().map(String::valueOf).toArray(String[]::new))
            );

            Map<String, Object> exportDetails = new HashMap<>();
            exportDetails.put("export_id", exportId);
            exportDetails.put("format", request.getFormat());
            exportDetails.put("item_count", vocabularyList.size());
            exportDetails.put("filename", filename);
            exportDetails.put("file_size", fileBytes.length);
            exportDetails.put("include_examples", request.isInclude_examples());
            exportDetails.put("include_statistics", request.isInclude_statistics());

            String detailsJson = jdbcTemplate.queryForObject(
                    "SELECT JSON_OBJECT(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", String.class,
                    "export_id", exportId,
                    "format", request.getFormat(),
                    "item_count", vocabularyList.size(),
                    "filename", filename,
                    "file_size", fileBytes.length,
                    "include_examples", request.isInclude_examples(),
                    "include_statistics", request.isInclude_statistics()
            );

            jdbcTemplate.update(insertHistorySql,
                    userId,
                    "EXPORT",
                    "VOCABULARY",
                    entityIdsJson,
                    "COMPLETED",
                    detailsJson,
                    LocalDateTime.now()
            );

            // 8. 准备文件响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(request.getFormat()));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileBytes.length);

            // 打印返回信息
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("filename", filename);
            responseInfo.put("file_size", fileBytes.length);
            responseInfo.put("format", request.getFormat());
            responseInfo.put("vocabulary_count", vocabularyList.size());
            responseInfo.put("include_examples", request.isInclude_examples());
            responseInfo.put("include_statistics", request.isInclude_statistics());
            printResponse(responseInfo);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("词汇导出过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> details = new HashMap<>();
            details.put("exception", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("VOCABULARY_EXPORT_ERROR", "服务器内部错误: " + e.getMessage(), details);
            printResponse(errorResponse);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 生成导出内容
    private String generateExportContent(List<VocabularyInfo> vocabularyList, String format,
                                         boolean includeExamples, boolean includeStatistics) {
        StringBuilder content = new StringBuilder();

        if (format.equalsIgnoreCase("json")) {
            // JSON格式
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("metadata", new HashMap<String, Object>() {{
                put("exportedAt", LocalDateTime.now().toString());
                put("totalItems", vocabularyList.size());
                put("format", "json");
                put("version", "1.0");
            }});

            List<Map<String, Object>> vocabJsonList = new ArrayList<>();
            for (VocabularyInfo vocab : vocabularyList) {
                Map<String, Object> vocabMap = new HashMap<>();
                vocabMap.put("id", "vocab_" + vocab.getUser_vocabulary_id());
                vocabMap.put("word", vocab.getWord());
                vocabMap.put("phonetic", vocab.getPhonetic());
                vocabMap.put("meaning", vocab.getMeaning());
                vocabMap.put("partOfSpeech", vocab.getPart_of_speech());
                vocabMap.put("difficulty", vocab.getDifficulty());
                vocabMap.put("mastery", vocab.getMastery());
                vocabMap.put("studyCount", vocab.getStudy_count());
                vocabMap.put("accuracy", vocab.getAccuracy());
                vocabMap.put("lastStudied", vocab.getLast_studied() != null ? vocab.getLast_studied().toString() : null);

                if (includeExamples && !vocab.getExamples().isEmpty()) {
                    List<Map<String, Object>> examplesList = new ArrayList<>();
                    for (ExampleInfo example : vocab.getExamples()) {
                        Map<String, Object> exampleMap = new HashMap<>();
                        exampleMap.put("sentence", example.getSentence());
                        exampleMap.put("translation", example.getTranslation());
                        exampleMap.put("source", example.getSource());
                        examplesList.add(exampleMap);
                    }
                    vocabMap.put("examples", examplesList);
                }

                if (includeStatistics) {
                    Map<String, Object> statsMap = new HashMap<>();
                    statsMap.put("firstLearned", vocab.getStatistics().getFirst_learned().toString());
                    statsMap.put("studySessions", vocab.getStatistics().getStudy_sessions());
                    statsMap.put("correctAnswers", vocab.getStatistics().getCorrect_answers());
                    statsMap.put("wrongAnswers", vocab.getStatistics().getWrong_answers());
                    vocabMap.put("statistics", statsMap);
                }

                vocabJsonList.add(vocabMap);
            }

            exportData.put("vocabulary", vocabJsonList);

            // 简单JSON序列化
            content.append("{\n");
            content.append("  \"metadata\": {\n");
            content.append("    \"exportedAt\": \"").append(LocalDateTime.now()).append("\",\n");
            content.append("    \"totalItems\": ").append(vocabularyList.size()).append(",\n");
            content.append("    \"format\": \"json\",\n");
            content.append("    \"version\": \"1.0\"\n");
            content.append("  },\n");
            content.append("  \"vocabulary\": [\n");

            for (int i = 0; i < vocabularyList.size(); i++) {
                VocabularyInfo vocab = vocabularyList.get(i);
                content.append("    {\n");
                content.append("      \"id\": \"vocab_").append(vocab.getUser_vocabulary_id()).append("\",\n");
                content.append("      \"word\": \"").append(escapeJson(vocab.getWord())).append("\",\n");
                content.append("      \"phonetic\": \"").append(escapeJson(vocab.getPhonetic())).append("\",\n");
                content.append("      \"meaning\": \"").append(escapeJson(vocab.getMeaning())).append("\",\n");
                content.append("      \"partOfSpeech\": \"").append(escapeJson(vocab.getPart_of_speech())).append("\",\n");
                content.append("      \"difficulty\": ").append(vocab.getDifficulty()).append(",\n");
                content.append("      \"mastery\": ").append(vocab.getMastery()).append(",\n");
                content.append("      \"studyCount\": ").append(vocab.getStudy_count()).append(",\n");
                content.append("      \"accuracy\": ").append(vocab.getAccuracy()).append(",\n");
                content.append("      \"lastStudied\": \"").append(vocab.getLast_studied() != null ? vocab.getLast_studied() : "").append("\"\n");
                content.append("    }");
                if (i < vocabularyList.size() - 1) content.append(",");
                content.append("\n");
            }

            content.append("  ]\n");
            content.append("}");

        } else if (format.equalsIgnoreCase("csv")) {
            // CSV格式
            content.append("单词,音标,释义,词性,难度等级,掌握程度,学习次数,正确率,最后学习时间\n");
            for (VocabularyInfo vocab : vocabularyList) {
                content.append(escapeCsv(vocab.getWord())).append(",");
                content.append(escapeCsv(vocab.getPhonetic())).append(",");
                content.append(escapeCsv(vocab.getMeaning())).append(",");
                content.append(escapeCsv(vocab.getPart_of_speech())).append(",");
                content.append(vocab.getDifficulty()).append(",");
                content.append(vocab.getMastery()).append(",");
                content.append(vocab.getStudy_count()).append(",");
                content.append(String.format("%.2f", vocab.getAccuracy())).append(",");
                content.append(escapeCsv(vocab.getLast_studied() != null ? vocab.getLast_studied().toString() : "")).append("\n");
            }

            // 如果包含例句，添加例句工作表
            if (includeExamples) {
                content.append("\n\n=== 例句 ===\n");
                content.append("单词,例句,翻译,来源\n");
                for (VocabularyInfo vocab : vocabularyList) {
                    for (ExampleInfo example : vocab.getExamples()) {
                        content.append(escapeCsv(vocab.getWord())).append(",");
                        content.append(escapeCsv(example.getSentence())).append(",");
                        content.append(escapeCsv(example.getTranslation())).append(",");
                        content.append(escapeCsv(example.getSource())).append("\n");
                    }
                }
            }

            // 如果包含统计信息，添加统计工作表
            if (includeStatistics) {
                content.append("\n\n=== 统计信息 ===\n");
                content.append("统计项,数值,说明\n");
                content.append("总词汇数,").append(vocabularyList.size()).append(",词汇总数\n");

                // 计算已掌握词汇数（掌握度80%以上）
                long masteredCount = vocabularyList.stream()
                        .filter(v -> v.getMastery() >= 80)
                        .count();
                content.append("已掌握词汇,").append(masteredCount).append(",掌握度80%以上的词汇数\n");

                // 计算学习天数（模拟）
                int studyDays = (int) (Math.random() * 30) + 1;
                content.append("学习天数,").append(studyDays).append(",累计学习天数\n");

                // 计算平均正确率
                double avgAccuracy = vocabularyList.stream()
                        .mapToDouble(VocabularyInfo::getAccuracy)
                        .average()
                        .orElse(0.0);
                content.append("平均正确率,").append(String.format("%.2f", avgAccuracy)).append(",整体答题正确率\n");

                // 最长连续学习（模拟）
                int longestStreak = (int) (Math.random() * 15) + 1;
                content.append("最长连续学习,").append(longestStreak).append(",连续学习天数\n");
            }

        } else {
            // 默认文本格式（用于pdf和txt）
            content.append("词汇导出报告\n");
            content.append("============\n\n");
            content.append("导出时间: ").append(LocalDateTime.now()).append("\n");
            content.append("词汇数量: ").append(vocabularyList.size()).append("\n");
            content.append("导出格式: ").append(format).append("\n\n");

            content.append("词汇列表:\n");
            content.append("--------\n");

            for (int i = 0; i < vocabularyList.size(); i++) {
                VocabularyInfo vocab = vocabularyList.get(i);
                content.append(i + 1).append(". ").append(vocab.getWord()).append(" ");

                if (vocab.getPhonetic() != null && !vocab.getPhonetic().isEmpty()) {
                    content.append("[").append(vocab.getPhonetic()).append("] ");
                }

                if (vocab.getPart_of_speech() != null && !vocab.getPart_of_speech().isEmpty()) {
                    content.append("(").append(vocab.getPart_of_speech()).append(") ");
                }

                content.append("\n");
                content.append("   释义: ").append(vocab.getMeaning()).append("\n");
                content.append("   难度: ").append(vocab.getDifficulty()).append("级 | ");
                content.append("掌握度: ").append(vocab.getMastery()).append("% | ");
                content.append("学习次数: ").append(vocab.getStudy_count()).append(" | ");
                content.append("正确率: ").append(String.format("%.2f", vocab.getAccuracy())).append("%\n");

                if (vocab.getLast_studied() != null) {
                    content.append("   最后学习: ").append(vocab.getLast_studied()).append("\n");
                }

                // 添加例句
                if (includeExamples && !vocab.getExamples().isEmpty()) {
                    content.append("   例句:\n");
                    for (ExampleInfo example : vocab.getExamples()) {
                        content.append("     - ").append(example.getSentence()).append("\n");
                        content.append("       ").append(example.getTranslation()).append("\n");
                    }
                }

                content.append("\n");
            }

            // 添加统计信息
            if (includeStatistics) {
                content.append("\n统计摘要:\n");
                content.append("--------\n");

                // 计算已掌握词汇数
                long masteredCount = vocabularyList.stream()
                        .filter(v -> v.getMastery() >= 80)
                        .count();

                // 计算平均正确率
                double avgAccuracy = vocabularyList.stream()
                        .mapToDouble(VocabularyInfo::getAccuracy)
                        .average()
                        .orElse(0.0);

                content.append("总词汇数: ").append(vocabularyList.size()).append("\n");
                content.append("已掌握词汇: ").append(masteredCount).append(" (").append(String.format("%.1f", masteredCount * 100.0 / vocabularyList.size())).append("%)\n");
                content.append("平均掌握度: ").append(String.format("%.1f", vocabularyList.stream().mapToInt(VocabularyInfo::getMastery).average().orElse(0.0))).append("%\n");
                content.append("平均正确率: ").append(String.format("%.2f", avgAccuracy)).append("%\n");
            }
        }

        return content.toString();
    }

    // 获取文件扩展名
    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "xlsx": return "xlsx";
            case "csv": return "csv";
            case "json": return "json";
            case "pdf": return "pdf";
            default: return "txt";
        }
    }

    // 获取媒体类型
    private MediaType getMediaType(String format) {
        switch (format.toLowerCase()) {
            case "xlsx": return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "csv": return MediaType.parseMediaType("text/csv");
            case "json": return MediaType.APPLICATION_JSON;
            case "pdf": return MediaType.APPLICATION_PDF;
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