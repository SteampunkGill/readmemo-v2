package com.vue.readingapp.ocr;

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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/documents")
public class OcrGetStatistics {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取OCR统计请求 ===");
        System.out.println("请求参数: " + request);
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

    // 响应DTO
    public static class GetOcrStatisticsResponse {
        private boolean success;
        private String message;
        private OcrStatisticsData data;

        public GetOcrStatisticsResponse(boolean success, String message, OcrStatisticsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public OcrStatisticsData getData() { return data; }
        public void setData(OcrStatisticsData data) { this.data = data; }
    }

    public static class OcrStatisticsData {
        private String documentId;
        private Integer totalPages;
        private Integer processedPages;
        private Double averageConfidence;
        private Map<String, Integer> languageDistribution;
        private Map<String, Integer> engineDistribution;
        private List<DailyStat> dailyStats;

        public OcrStatisticsData(String documentId, Integer totalPages, Integer processedPages,
                                 Double averageConfidence, Map<String, Integer> languageDistribution,
                                 Map<String, Integer> engineDistribution, List<DailyStat> dailyStats) {
            this.documentId = documentId;
            this.totalPages = totalPages;
            this.processedPages = processedPages;
            this.averageConfidence = averageConfidence;
            this.languageDistribution = languageDistribution;
            this.engineDistribution = engineDistribution;
            this.dailyStats = dailyStats;
        }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

        public Integer getProcessedPages() { return processedPages; }
        public void setProcessedPages(Integer processedPages) { this.processedPages = processedPages; }

        public Double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(Double averageConfidence) { this.averageConfidence = averageConfidence; }

        public Map<String, Integer> getLanguageDistribution() { return languageDistribution; }
        public void setLanguageDistribution(Map<String, Integer> languageDistribution) { this.languageDistribution = languageDistribution; }

        public Map<String, Integer> getEngineDistribution() { return engineDistribution; }
        public void setEngineDistribution(Map<String, Integer> engineDistribution) { this.engineDistribution = engineDistribution; }

        public List<DailyStat> getDailyStats() { return dailyStats; }
        public void setDailyStats(List<DailyStat> dailyStats) { this.dailyStats = dailyStats; }
    }

    public static class DailyStat {
        private String date;
        private Integer processedPages;
        private Double averageConfidence;

        public DailyStat(String date, Integer processedPages, Double averageConfidence) {
            this.date = date;
            this.processedPages = processedPages;
            this.averageConfidence = averageConfidence;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public Integer getProcessedPages() { return processedPages; }
        public void setProcessedPages(Integer processedPages) { this.processedPages = processedPages; }

        public Double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(Double averageConfidence) { this.averageConfidence = averageConfidence; }
    }

    @GetMapping("/{documentId}/ocr/stats")
    public ResponseEntity<GetOcrStatisticsResponse> getOcrStatistics(
            @PathVariable String documentId,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        // 构建请求信息
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("groupBy", groupBy);
        requestInfo.put("startDate", startDate);
        requestInfo.put("endDate", endDate);

        // 打印接收到的请求
        printRequest(requestInfo);

        try {
            // 1. 验证请求数据
            if (documentId == null || documentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new GetOcrStatisticsResponse(false, "文档ID不能为空", null)
                );
            }

            // 2. 检查文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            Integer documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, documentId);

            if (documentCount == null || documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new GetOcrStatisticsResponse(false, "文档不存在", null)
                );
            }

            // 3. 检查OCR结果表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'document_ocr_results'";
            Integer tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class);

            if (tableCount == null || tableCount == 0) {
                return getEmptyStatistics(documentId);
            }

            // 4. 构建基础统计查询
            StringBuilder baseQueryBuilder = new StringBuilder();
            baseQueryBuilder.append("SELECT COUNT(*) as total_pages, ");
            baseQueryBuilder.append("AVG(confidence) as avg_confidence ");
            baseQueryBuilder.append("FROM document_ocr_results WHERE document_id = ?");

            List<Object> baseParams = new ArrayList<>();
            baseParams.add(documentId);

            // 添加日期条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                baseQueryBuilder.append(" AND created_at >= ?");
                baseParams.add(LocalDateTime.parse(startDate + "T00:00:00"));
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                baseQueryBuilder.append(" AND created_at <= ?");
                baseParams.add(LocalDateTime.parse(endDate + "T23:59:59"));
            }

            // 5. 执行基础统计查询
            Map<String, Object> baseStats = jdbcTemplate.queryForMap(baseQueryBuilder.toString(), baseParams.toArray());
            printQueryResult(baseStats);

            Integer totalPages = baseStats.get("total_pages") != null ? ((Number) baseStats.get("total_pages")).intValue() : 0;
            Double averageConfidence = baseStats.get("avg_confidence") != null ? ((Number) baseStats.get("avg_confidence")).doubleValue() : 0.0;

            // 6. 根据groupBy参数获取不同的统计
            Map<String, Integer> languageDistribution = new HashMap<>();
            Map<String, Integer> engineDistribution = new HashMap<>();
            List<DailyStat> dailyStats = new ArrayList<>();

            if (groupBy != null) {
                switch (groupBy.toLowerCase()) {
                    case "language":
                        languageDistribution = getLanguageDistribution(documentId, startDate, endDate);
                        break;
                    case "engine":
                        engineDistribution = getEngineDistribution(documentId, startDate, endDate);
                        break;
                    case "page":
                        // 按页面统计
                        dailyStats = getPageStats(documentId, startDate, endDate);
                        break;
                    default:
                        // 默认按日期统计
                        dailyStats = getDailyStats(documentId, startDate, endDate);
                        break;
                }
            } else {
                // 默认获取所有统计
                languageDistribution = getLanguageDistribution(documentId, startDate, endDate);
                engineDistribution = getEngineDistribution(documentId, startDate, endDate);
                dailyStats = getDailyStats(documentId, startDate, endDate);
            }

            // 7. 准备响应数据
            OcrStatisticsData responseData = new OcrStatisticsData(
                    documentId,
                    totalPages,
                    totalPages, // 假设所有页面都已处理
                    averageConfidence,
                    languageDistribution,
                    engineDistribution,
                    dailyStats
            );

            GetOcrStatisticsResponse response = new GetOcrStatisticsResponse(true, "获取OCR统计成功", responseData);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取OCR统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetOcrStatisticsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private ResponseEntity<GetOcrStatisticsResponse> getEmptyStatistics(String documentId) {
        OcrStatisticsData emptyData = new OcrStatisticsData(
                documentId,
                0,
                0,
                0.0,
                new HashMap<>(),
                new HashMap<>(),
                new ArrayList<>()
        );

        GetOcrStatisticsResponse response = new GetOcrStatisticsResponse(true, "没有OCR统计数据", emptyData);

        // 打印返回数据
        printResponse(response);

        return ResponseEntity.ok(response);
    }

    private Map<String, Integer> getLanguageDistribution(String documentId, String startDate, String endDate) {
        Map<String, Integer> distribution = new HashMap<>();

        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT metadata_json FROM document_ocr_results WHERE document_id = ?");

            List<Object> params = new ArrayList<>();
            params.add(documentId);

            // 添加日期条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at >= ?");
                params.add(LocalDateTime.parse(startDate + "T00:00:00"));
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at <= ?");
                params.add(LocalDateTime.parse(endDate + "T23:59:59"));
            }

            List<Map<String, Object>> results = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());

            for (Map<String, Object> result : results) {
                String metadataJson = (String) result.get("metadata_json");
                if (metadataJson != null && !metadataJson.trim().isEmpty()) {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                        String language = (String) metadata.get("language");
                        if (language != null) {
                            distribution.put(language, distribution.getOrDefault(language, 0) + 1);
                        }
                    } catch (Exception e) {
                        System.err.println("解析metadata失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取语言分布失败: " + e.getMessage());
        }

        return distribution;
    }

    private Map<String, Integer> getEngineDistribution(String documentId, String startDate, String endDate) {
        Map<String, Integer> distribution = new HashMap<>();

        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT metadata_json FROM document_ocr_results WHERE document_id = ?");

            List<Object> params = new ArrayList<>();
            params.add(documentId);

            // 添加日期条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at >= ?");
                params.add(LocalDateTime.parse(startDate + "T00:00:00"));
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at <= ?");
                params.add(LocalDateTime.parse(endDate + "T23:59:59"));
            }

            List<Map<String, Object>> results = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());

            for (Map<String, Object> result : results) {
                String metadataJson = (String) result.get("metadata_json");
                if (metadataJson != null && !metadataJson.trim().isEmpty()) {
                    try {
                        Map<String, Object> metadata = objectMapper.readValue(metadataJson, Map.class);
                        String engine = (String) metadata.get("engine");
                        if (engine != null) {
                            distribution.put(engine, distribution.getOrDefault(engine, 0) + 1);
                        }
                    } catch (Exception e) {
                        System.err.println("解析metadata失败: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取引擎分布失败: " + e.getMessage());
        }

        return distribution;
    }

    private List<DailyStat> getDailyStats(String documentId, String startDate, String endDate) {
        List<DailyStat> dailyStats = new ArrayList<>();

        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT DATE(created_at) as stat_date, ");
            queryBuilder.append("COUNT(*) as processed_pages, ");
            queryBuilder.append("AVG(confidence) as avg_confidence ");
            queryBuilder.append("FROM document_ocr_results WHERE document_id = ?");

            List<Object> params = new ArrayList<>();
            params.add(documentId);

            // 添加日期条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at >= ?");
                params.add(LocalDateTime.parse(startDate + "T00:00:00"));
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at <= ?");
                params.add(LocalDateTime.parse(endDate + "T23:59:59"));
            }

            queryBuilder.append(" GROUP BY DATE(created_at) ORDER BY stat_date DESC");

            List<Map<String, Object>> results = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());

            for (Map<String, Object> result : results) {
                String date = result.get("stat_date").toString();
                Integer processedPages = result.get("processed_pages") != null ? ((Number) result.get("processed_pages")).intValue() : 0;
                Double avgConfidence = result.get("avg_confidence") != null ? ((Number) result.get("avg_confidence")).doubleValue() : 0.0;

                dailyStats.add(new DailyStat(date, processedPages, avgConfidence));
            }
        } catch (Exception e) {
            System.err.println("获取每日统计失败: " + e.getMessage());
        }

        return dailyStats;
    }

    private List<DailyStat> getPageStats(String documentId, String startDate, String endDate) {
        List<DailyStat> pageStats = new ArrayList<>();

        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT page_number, ");
            queryBuilder.append("COUNT(*) as processed_count, ");
            queryBuilder.append("AVG(confidence) as avg_confidence ");
            queryBuilder.append("FROM document_ocr_results WHERE document_id = ?");

            List<Object> params = new ArrayList<>();
            params.add(documentId);

            // 添加日期条件
            if (startDate != null && !startDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at >= ?");
                params.add(LocalDateTime.parse(startDate + "T00:00:00"));
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                queryBuilder.append(" AND created_at <= ?");
                params.add(LocalDateTime.parse(endDate + "T23:59:59"));
            }

            queryBuilder.append(" GROUP BY page_number ORDER BY page_number");

            List<Map<String, Object>> results = jdbcTemplate.queryForList(queryBuilder.toString(), params.toArray());

            for (Map<String, Object> result : results) {
                String page = "Page " + result.get("page_number").toString();
                Integer processedCount = result.get("processed_count") != null ? ((Number) result.get("processed_count")).intValue() : 0;
                Double avgConfidence = result.get("avg_confidence") != null ? ((Number) result.get("avg_confidence")).doubleValue() : 0.0;

                pageStats.add(new DailyStat(page, processedCount, avgConfidence));
            }
        } catch (Exception e) {
            System.err.println("获取页面统计失败: " + e.getMessage());
        }

        return pageStats;
    }
}