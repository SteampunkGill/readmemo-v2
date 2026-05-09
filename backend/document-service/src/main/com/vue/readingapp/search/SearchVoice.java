package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchVoice {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到语音搜索请求 ===");
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

    // 响应DTO
    public static class VoiceSearchResponse {
        private boolean success;
        private String message;
        private VoiceSearchData data;

        public VoiceSearchResponse(boolean success, String message, VoiceSearchData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public VoiceSearchData getData() { return data; }
        public void setData(VoiceSearchData data) { this.data = data; }
    }

    public static class VoiceSearchData {
        private String text;
        private String language;
        private double confidence;
        private Map<String, Object> searchResults;

        public VoiceSearchData(String text, String language, double confidence, Map<String, Object> searchResults) {
            this.text = text;
            this.language = language;
            this.confidence = confidence;
            this.searchResults = searchResults;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public Map<String, Object> getSearchResults() { return searchResults; }
        public void setSearchResults(Map<String, Object> searchResults) { this.searchResults = searchResults; }
    }

    @PostMapping("/voice")
    public ResponseEntity<VoiceSearchResponse> voiceSearch(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "language", defaultValue = "zh-CN") String language,
            @RequestParam(value = "maxResults", defaultValue = "1") Integer maxResults,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("audioFileSize", audioFile.getSize());
        requestParams.put("language", language);
        requestParams.put("maxResults", maxResults);
        printRequest(requestParams);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);

            // 2. 验证请求数据
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VoiceSearchResponse(false, "音频文件不能为空", null)
                );
            }

            if (audioFile.getSize() > 10 * 1024 * 1024) { // 10MB限制
                return ResponseEntity.badRequest().body(
                        new VoiceSearchResponse(false, "音频文件大小不能超过10MB", null)
                );
            }

            // 3. 模拟语音识别（课设简化处理）
            // 实际项目中应该调用语音识别API，如百度AI、讯飞等
            String recognizedText = simulateVoiceRecognition(audioFile, language);

            if (recognizedText == null || recognizedText.trim().isEmpty()) {
                return ResponseEntity.ok(new VoiceSearchResponse(
                        false,
                        "未能识别语音内容，请重试",
                        new VoiceSearchData("", language, 0.0, new HashMap<>())
                ));
            }

            // 4. 使用识别到的文本进行搜索
            Map<String, Object> searchResults = performVoiceSearch(recognizedText, userId, maxResults);

            // 5. 记录语音搜索历史
            if (userId != null) {
                saveVoiceSearchHistory(userId, recognizedText, language, searchResults.size());
            }

            // 6. 准备响应数据
            VoiceSearchData data = new VoiceSearchData(
                    recognizedText,
                    language,
                    0.8, // 模拟置信度
                    searchResults
            );

            VoiceSearchResponse response = new VoiceSearchResponse(true, "语音搜索成功", data);

            // 打印查询结果
            printQueryResult("语音识别结果: " + recognizedText + ", 找到 " + searchResults.size() + " 个结果");

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("语音搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VoiceSearchResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 模拟语音识别（课设简化处理）
    private String simulateVoiceRecognition(MultipartFile audioFile, String language) {
        try {
            // 这里应该调用真正的语音识别API
            // 为了课设，我们返回一些预设的文本
            Map<String, String> presetTexts = new HashMap<>();
            presetTexts.put("zh-CN", "英语学习");
            presetTexts.put("en-US", "English learning");
            presetTexts.put("ja-JP", "英語学習");

            String text = presetTexts.getOrDefault(language, "English learning");

            // 添加一些随机性，模拟不同的识别结果
            Random random = new Random(audioFile.getSize());
            if (random.nextDouble() < 0.3) {
                // 30%的概率返回其他文本
                String[] alternatives = {
                        "技术文档",
                        "商务英语",
                        "阅读技巧",
                        "单词记忆",
                        "英语口语"
                };
                text = alternatives[random.nextInt(alternatives.length)];
            }

            return text;

        } catch (Exception e) {
            System.err.println("模拟语音识别失败: " + e.getMessage());
            return "英语学习";
        }
    }

    // 执行语音搜索
    private Map<String, Object> performVoiceSearch(String text, Long userId, int maxResults) {
        Map<String, Object> results = new HashMap<>();

        try {
            // 1. 搜索文档
            String docSql = "SELECT document_id, title, author, language, word_count, created_at " +
                    "FROM documents " +
                    "WHERE (title LIKE ? OR content LIKE ?) AND status = 'processed' ";

            List<Object> docParams = new ArrayList<>();
            docParams.add("%" + text + "%");
            docParams.add("%" + text + "%");

            if (userId != null) {
                docSql += "AND user_id = ? ";
                docParams.add(userId);
            }

            docSql += "ORDER BY created_at DESC LIMIT ?";
            docParams.add(maxResults);

            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, docParams.toArray());

            // 2. 搜索词汇
            String vocabSql = "SELECT w.word_id, w.word, w.phonetic, wd.definition, wd.translation " +
                    "FROM words w " +
                    "LEFT JOIN word_definitions wd ON w.word_id = wd.word_id " +
                    "WHERE w.word LIKE ? OR wd.definition LIKE ? OR wd.translation LIKE ? " +
                    "ORDER BY w.word ASC LIMIT ?";

            List<Map<String, Object>> vocabulary = jdbcTemplate.queryForList(vocabSql,
                    "%" + text + "%", "%" + text + "%", "%" + text + "%", maxResults);

            // 3. 组织结果
            results.put("documents", documents);
            results.put("vocabulary", vocabulary);
            results.put("total", documents.size() + vocabulary.size());
            results.put("text", text);

        } catch (Exception e) {
            System.err.println("执行语音搜索失败: " + e.getMessage());
        }

        return results;
    }

    // 保存语音搜索历史
    private void saveVoiceSearchHistory(Long userId, String text, String language, int resultCount) {
        try {
            String sql = "INSERT INTO voice_search_history (user_id, recognized_text, language, result_count, timestamp) " +
                    "VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(sql, userId, text, language, resultCount);
            System.out.println("已保存语音搜索历史: 用户=" + userId + ", 文本=" + text);
        } catch (Exception e) {
            System.err.println("保存语音搜索历史失败: " + e.getMessage());
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
