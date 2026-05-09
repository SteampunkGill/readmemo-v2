package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/tags")
public class TagSearch {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到搜索标签请求 ===");
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

    // 标签响应DTO
    public static class Tag {
        private String id;
        private String name;
        private String color;
        private String description;
        private String type;
        private int documentCount;
        private double relevanceScore;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getDocumentCount() { return documentCount; }
        public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    }

    // 响应类
    public static class TagSearchResponse {
        private boolean success;
        private List<Tag> data;

        public TagSearchResponse(boolean success, List<Tag> data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<Tag> getData() { return data; }
        public void setData(List<Tag> data) { this.data = data; }
    }

    @GetMapping("/search")
    public ResponseEntity<TagSearchResponse> searchTags(
            @RequestParam("query") String query,
            @RequestParam(value = "type", defaultValue = "all") String type) {

        Map<String, Object> requestData = new java.util.HashMap<>();
        requestData.put("query", query);
        requestData.put("type", type);
        printRequest(requestData);

        try {
            // 验证查询参数
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new TagSearchResponse(false, new ArrayList<>())
                );
            }

            // 构建搜索条件
            String searchPattern = "%" + query + "%";
            List<Tag> allTags = new ArrayList<>();

            // 搜索文档标签
            if ("all".equals(type) || "document".equals(type)) {
                String docSql = "SELECT * FROM document_tags WHERE tag_name LIKE ? OR description LIKE ?";
                List<Map<String, Object>> docResults = jdbcTemplate.queryForList(docSql, searchPattern, searchPattern);

                for (Map<String, Object> tagMap : docResults) {
                    Tag tag = new Tag();
                    tag.setId(String.valueOf(tagMap.get("tag_id")));
                    tag.setName((String) tagMap.get("tag_name"));
                    tag.setColor((String) tagMap.get("color"));
                    tag.setDescription((String) tagMap.get("description"));
                    tag.setType("document");

                    // 查询文档数量
                    String countSql = "SELECT COUNT(*) FROM document_tag_relations WHERE tag_id = ?";
                    int docCount = jdbcTemplate.queryForObject(countSql, Integer.class, tagMap.get("tag_id"));
                    tag.setDocumentCount(docCount);

                    // 计算相关性分数（简单实现：名称匹配度）
                    double relevance = calculateRelevance(query, (String) tagMap.get("tag_name"), (String) tagMap.get("description"));
                    tag.setRelevanceScore(relevance);

                    allTags.add(tag);
                }
            }

            // 搜索词汇标签
            if ("all".equals(type) || "vocabulary".equals(type)) {
                String vocabSql = "SELECT * FROM vocabulary_tags WHERE tag_name LIKE ? OR description LIKE ?";
                List<Map<String, Object>> vocabResults = jdbcTemplate.queryForList(vocabSql, searchPattern, searchPattern);

                for (Map<String, Object> tagMap : vocabResults) {
                    Tag tag = new Tag();
                    tag.setId(String.valueOf(tagMap.get("tag_id")));
                    tag.setName((String) tagMap.get("tag_name"));
                    tag.setColor((String) tagMap.get("color"));
                    tag.setDescription((String) tagMap.get("description"));
                    tag.setType("vocabulary");
                    tag.setDocumentCount(0);

                    // 计算相关性分数
                    double relevance = calculateRelevance(query, (String) tagMap.get("tag_name"), (String) tagMap.get("description"));
                    tag.setRelevanceScore(relevance);

                    allTags.add(tag);
                }
            }

            printQueryResult(allTags);

            // 按相关性分数排序
            allTags.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

            // 创建响应
            TagSearchResponse response = new TagSearchResponse(true, allTags);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("搜索标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new TagSearchResponse(false, new ArrayList<>())
            );
        }
    }

    // 计算相关性分数（简单实现）
    private double calculateRelevance(String query, String name, String description) {
        double score = 0.0;

        // 名称完全匹配
        if (name.equalsIgnoreCase(query)) {
            score += 1.0;
        }
        // 名称包含查询词
        else if (name.toLowerCase().contains(query.toLowerCase())) {
            score += 0.8;
        }
        // 描述包含查询词
        else if (description != null && description.toLowerCase().contains(query.toLowerCase())) {
            score += 0.3;
        }

        return score;
    }
}
