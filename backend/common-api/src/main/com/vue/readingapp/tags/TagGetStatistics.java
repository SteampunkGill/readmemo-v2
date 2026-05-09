package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/tags")
public class TagGetStatistics {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取标签统计信息请求 ===");
        System.out.println("请求参数: " + request);
        System.out.println("=============================");
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

    // 最常用标签DTO
    public static class MostUsedTag {
        private String id;
        private String name;
        private int usageCount;

        public MostUsedTag() {}

        public MostUsedTag(String id, String name, int usageCount) {
            this.id = id;
            this.name = name;
            this.usageCount = usageCount;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getUsageCount() { return usageCount; }
        public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    }

    // 标签分布DTO
    public static class TagDistribution {
        private int document;
        private int vocabulary;

        public TagDistribution() {}

        public TagDistribution(int document, int vocabulary) {
            this.document = document;
            this.vocabulary = vocabulary;
        }

        // Getters and Setters
        public int getDocument() { return document; }
        public void setDocument(int document) { this.document = document; }

        public int getVocabulary() { return vocabulary; }
        public void setVocabulary(int vocabulary) { this.vocabulary = vocabulary; }
    }

    // 统计信息DTO
    public static class StatisticsData {
        private int totalTags;
        private int documentTags;
        private int vocabularyTags;
        private MostUsedTag mostUsedTag;
        private TagDistribution tagDistribution;

        public StatisticsData() {}

        public StatisticsData(int totalTags, int documentTags, int vocabularyTags,
                              MostUsedTag mostUsedTag, TagDistribution tagDistribution) {
            this.totalTags = totalTags;
            this.documentTags = documentTags;
            this.vocabularyTags = vocabularyTags;
            this.mostUsedTag = mostUsedTag;
            this.tagDistribution = tagDistribution;
        }

        // Getters and Setters
        public int getTotalTags() { return totalTags; }
        public void setTotalTags(int totalTags) { this.totalTags = totalTags; }

        public int getDocumentTags() { return documentTags; }
        public void setDocumentTags(int documentTags) { this.documentTags = documentTags; }

        public int getVocabularyTags() { return vocabularyTags; }
        public void setVocabularyTags(int vocabularyTags) { this.vocabularyTags = vocabularyTags; }

        public MostUsedTag getMostUsedTag() { return mostUsedTag; }
        public void setMostUsedTag(MostUsedTag mostUsedTag) { this.mostUsedTag = mostUsedTag; }

        public TagDistribution getTagDistribution() { return tagDistribution; }
        public void setTagDistribution(TagDistribution tagDistribution) { this.tagDistribution = tagDistribution; }
    }

    // 响应类
    public static class StatisticsResponse {
        private boolean success;
        private StatisticsData data;

        public StatisticsResponse(boolean success, StatisticsData data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public StatisticsData getData() { return data; }
        public void setData(StatisticsData data) { this.data = data; }
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getTagStatistics(
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "timeRange", defaultValue = "month") String timeRange) {

        // 打印请求参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("type", type);
        requestParams.put("timeRange", timeRange);
        printRequest(requestParams);

        try {
            // 查询文档标签总数
            String docTagCountSql = "SELECT COUNT(*) FROM document_tags";
            int documentTags = jdbcTemplate.queryForObject(docTagCountSql, Integer.class);

            // 查询词汇标签总数
            String vocabTagCountSql = "SELECT COUNT(*) FROM vocabulary_tags";
            int vocabularyTags = jdbcTemplate.queryForObject(vocabTagCountSql, Integer.class);

            // 计算总标签数
            int totalTags = documentTags + vocabularyTags;

            // 查询最常用的文档标签
            String mostUsedDocTagSql =
                    "SELECT dt.tag_id, dt.tag_name, COUNT(dtr.relation_id) as usage_count " +
                            "FROM document_tags dt " +
                            "LEFT JOIN document_tag_relations dtr ON dt.tag_id = dtr.tag_id " +
                            "GROUP BY dt.tag_id, dt.tag_name " +
                            "ORDER BY usage_count DESC " +
                            "LIMIT 1";

            MostUsedTag mostUsedTag = null;
            try {
                Map<String, Object> mostUsedTagMap = jdbcTemplate.queryForMap(mostUsedDocTagSql);
                mostUsedTag = new MostUsedTag(
                        String.valueOf(mostUsedTagMap.get("tag_id")),
                        (String) mostUsedTagMap.get("tag_name"),
                        ((Number) mostUsedTagMap.get("usage_count")).intValue()
                );
            } catch (Exception e) {
                // 如果没有文档标签，尝试查询词汇标签
                String mostUsedVocabTagSql =
                        "SELECT vt.tag_id, vt.tag_name, COUNT(uvt.relation_id) as usage_count " +
                                "FROM vocabulary_tags vt " +
                                "LEFT JOIN user_vocabulary_tags uvt ON vt.tag_id = uvt.tag_id " +
                                "GROUP BY vt.tag_id, vt.tag_name " +
                                "ORDER BY usage_count DESC " +
                                "LIMIT 1";

                try {
                    Map<String, Object> mostUsedTagMap = jdbcTemplate.queryForMap(mostUsedVocabTagSql);
                    mostUsedTag = new MostUsedTag(
                            String.valueOf(mostUsedTagMap.get("tag_id")),
                            (String) mostUsedTagMap.get("tag_name"),
                            ((Number) mostUsedTagMap.get("usage_count")).intValue()
                    );
                } catch (Exception ex) {
                    // 如果没有标签，创建一个空的
                    mostUsedTag = new MostUsedTag("0", "无标签", 0);
                }
            }

            // 创建标签分布
            TagDistribution tagDistribution = new TagDistribution(documentTags, vocabularyTags);

            // 创建统计信息
            StatisticsData statisticsData = new StatisticsData(
                    totalTags, documentTags, vocabularyTags, mostUsedTag, tagDistribution
            );

            printQueryResult(statisticsData);

            // 创建响应
            StatisticsResponse response = new StatisticsResponse(true, statisticsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取标签统计信息过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            StatisticsResponse errorResponse = new StatisticsResponse(false, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
