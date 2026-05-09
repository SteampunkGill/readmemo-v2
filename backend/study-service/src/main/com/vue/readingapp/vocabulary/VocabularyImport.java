package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 生词本导入控制器
 * 
 * 按照指定的列逻辑导入数据：
 * A: word, B: phonetic, C: definition, D: translation pos, E: collins, F: oxford, 
 * G: tag, H: bnc, I: frq, J: exchange, K: detail, L: audio, M: metric
 */
@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyImport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void printRequest(Object request) {
        System.out.println("=== 收到导入生词本请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("=========================");
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

    public static class ImportVocabularyResponse {
        private boolean success;
        private String message;
        private ImportResultData data;

        public ImportVocabularyResponse(boolean success, String message, ImportResultData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public ImportResultData getData() { return data; }
        public void setData(ImportResultData data) { this.data = data; }
    }

    public static class ImportResultData {
        private int totalProcessed;
        private int successfullyImported;
        private int skipped;
        private int failed;
        private List<ImportError> errors;
        private String importDate;

        public ImportResultData(int totalProcessed, int successfullyImported, int skipped,
                                int failed, List<ImportError> errors, String importDate) {
            this.totalProcessed = totalProcessed;
            this.successfullyImported = successfullyImported;
            this.skipped = skipped;
            this.failed = failed;
            this.errors = errors;
            this.importDate = importDate;
        }

        public int getTotalProcessed() { return totalProcessed; }
        public int getSuccessfullyImported() { return successfullyImported; }
        public int getSkipped() { return skipped; }
        public int getFailed() { return failed; }
        public List<ImportError> getErrors() { return errors; }
        public String getImportDate() { return importDate; }
    }

    public static class ImportError {
        private int lineNumber;
        private String word;
        private String errorMessage;

        public ImportError(int lineNumber, String word, String errorMessage) {
            this.lineNumber = lineNumber;
            this.word = word;
            this.errorMessage = errorMessage;
        }

        public int getLineNumber() { return lineNumber; }
        public String getWord() { return word; }
        public String getErrorMessage() { return errorMessage; }
    }

    @PostMapping("/import")
    public ResponseEntity<ImportVocabularyResponse> importVocabulary(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "csv") String format) {

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("fileName", file.getOriginalFilename());
        requestParams.put("fileSize", file.getSize());
        requestParams.put("format", format);
        printRequest(requestParams);

        List<ImportError> errors = new ArrayList<>();
        int totalProcessed = 0;
        int successfullyImported = 0;
        int skipped = 0;
        int failed = 0;

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ImportVocabularyResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            String userSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > ?";
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(userSql, token, now);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ImportVocabularyResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Long userId = ((Number) sessions.get(0).get("user_id")).longValue();

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ImportVocabularyResponse(false, "文件为空", null)
                );
            }

            List<Map<String, String>> vocabularyItems = new ArrayList<>();
            switch (format.toLowerCase()) {
                case "csv":
                    vocabularyItems = parseCsvFile(file);
                    break;
                case "txt":
                    vocabularyItems = parseTxtFile(file);
                    break;
                default:
                    return ResponseEntity.badRequest().body(
                            new ImportVocabularyResponse(false, "不支持的导入格式: " + format, null)
                    );
            }

            printQueryResult("解析到 " + vocabularyItems.size() + " 个生词项");

            // 1. 批量插入 words 表
            String insertWordSql = "INSERT IGNORE INTO words (word, language, phonetic, translation_pos, collins, oxford, tag, bnc, frq, exchange, detail, audio, metric) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            List<Object[]> wordBatchArgs = new ArrayList<>();
            for (Map<String, String> item : vocabularyItems) {
                String word = item.get("word");
                if (word != null && !word.trim().isEmpty()) {
                    String lang = word.trim().toLowerCase().matches("^[a-zA-Z\\s\\-\\']+$") ? "en" : "zh";
                    wordBatchArgs.add(new Object[]{
                        word.trim().toLowerCase(), lang,
                        item.get("phonetic"), item.get("translation_pos"), item.get("collins"),
                        item.get("oxford"), item.get("tag"), item.get("bnc"),
                        parseInteger(item.get("frq")), item.get("exchange"), item.get("detail"),
                        item.get("audio"), parseInteger(item.get("metric"))
                    });
                }
            }
            if (!wordBatchArgs.isEmpty()) {
                for (int i = 0; i < wordBatchArgs.size(); i += 1000) {
                    int end = Math.min(i + 1000, wordBatchArgs.size());
                    jdbcTemplate.batchUpdate(insertWordSql, wordBatchArgs.subList(i, end));
                }
            }

            // 2. 批量插入 user_vocabulary
            String insertVocabSql = "INSERT INTO user_vocabulary (user_id, word_id, word, language, phonetic, definition, translation_pos, collins, oxford, tag, bnc, frq, exchange, detail, audio, metric, status, created_at, updated_at) " +
                    "VALUES (?, (SELECT word_id FROM words WHERE word = ? LIMIT 1), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "phonetic = VALUES(phonetic), definition = VALUES(definition), translation_pos = VALUES(translation_pos), " +
                    "collins = VALUES(collins), oxford = VALUES(oxford), tag = VALUES(tag), bnc = VALUES(bnc), " +
                    "frq = VALUES(frq), exchange = VALUES(exchange), detail = VALUES(detail), audio = VALUES(audio), " +
                    "metric = VALUES(metric), updated_at = VALUES(updated_at)";

            List<Object[]> batchArgs = new ArrayList<>();
            LocalDateTime currentTime = LocalDateTime.now();

            for (int i = 0; i < vocabularyItems.size(); i++) {
                totalProcessed++;
                Map<String, String> item = vocabularyItems.get(i);
                try {
                    String word = item.get("word");
                    if (word == null || word.trim().isEmpty()) {
                        if (errors.size() < 100) errors.add(new ImportError(i + 2, "", "单词不能为空"));
                        failed++;
                        continue;
                    }

                    String normalizedWord = word.trim().toLowerCase();
                    String language = normalizedWord.matches("^[a-zA-Z\\s\\-\\']+$") ? "en" : "zh";

                    batchArgs.add(new Object[]{
                        userId, normalizedWord, normalizedWord, language,
                        item.get("phonetic"), item.get("definition"), item.get("translation_pos"),
                        item.get("collins"), item.get("oxford"), item.get("tag"), item.get("bnc"),
                        parseInteger(item.get("frq")), item.get("exchange"), item.get("detail"),
                        item.get("audio"), parseInteger(item.get("metric")),
                        "new", currentTime, currentTime
                    });

                    if (batchArgs.size() >= 1000) {
                        int[] results = jdbcTemplate.batchUpdate(insertVocabSql, batchArgs);
                        for (int res : results) successfullyImported++;
                        batchArgs.clear();
                    }
                } catch (Exception e) {
                    failed++;
                    if (errors.size() < 100) errors.add(new ImportError(i + 2, item.get("word"), e.getMessage()));
                }
            }

            if (!batchArgs.isEmpty()) {
                int[] results = jdbcTemplate.batchUpdate(insertVocabSql, batchArgs);
                for (int res : results) successfullyImported++;
            }

            ImportResultData resultData = new ImportResultData(totalProcessed, successfullyImported, skipped, failed, errors, LocalDateTime.now().toString());
            String message = String.format("导入完成，成功导入 %d 项，失败 %d 项", successfullyImported, failed);
            return ResponseEntity.ok(new ImportVocabularyResponse(true, message, resultData));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ImportVocabularyResponse(false, "导入失败: " + e.getMessage(), null));
        }
    }

    private Integer parseInteger(String val) {
        try {
            if (val == null || val.trim().isEmpty()) return null;
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, String>> parseCsvFile(MultipartFile file) throws Exception {
        List<Map<String, String>> items = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            lineNumber++;
            if (lineNumber == 1) continue; // Skip header

            List<String> values = parseCsvLine(line);
            items.add(mapColumns(values));
        }
        reader.close();
        return items;
    }

    private List<Map<String, String>> parseTxtFile(MultipartFile file) throws Exception {
        List<Map<String, String>> items = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            lineNumber++;
            if (lineNumber == 1) continue; // Skip header if exists

            String[] parts = line.split(",", -1);
            items.add(mapColumns(Arrays.asList(parts)));
        }
        reader.close();
        return items;
    }

    private Map<String, String> mapColumns(List<String> values) {
        Map<String, String> item = new HashMap<>();
        item.put("word", getSafe(values, 0));
        item.put("phonetic", getSafe(values, 1));
        item.put("definition", getSafe(values, 2));
        item.put("translation_pos", getSafe(values, 3));
        item.put("collins", getSafe(values, 4));
        item.put("oxford", getSafe(values, 5));
        item.put("tag", getSafe(values, 6));
        item.put("bnc", getSafe(values, 7));
        item.put("frq", getSafe(values, 8));
        item.put("exchange", getSafe(values, 9));
        item.put("detail", getSafe(values, 10));
        item.put("audio", getSafe(values, 11));
        item.put("metric", getSafe(values, 12));
        return item;
    }

    private String getSafe(List<String> values, int index) {
        return (index < values.size()) ? values.get(index).trim() : "";
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString());
                sb = new StringBuilder();
            } else sb.append(c);
        }
        values.add(sb.toString());
        return values;
    }
}
