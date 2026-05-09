package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/tags")
public class TagGetPopular {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取热门标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
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
        private String type;
        private int documentCount;
        private int usageCount;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getDocumentCount() { return documentCount; }
        public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

        public int getUsageCount() { return usageCount; }
        public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    }

    // 响应类
    public static class PopularTagsResponse {
        private boolean success;
        private List<Tag> data;

        public PopularTagsResponse(boolean success, List<Tag> data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<Tag> getData() { return data; }
        public void setData(List<Tag> data) { this.data = data; }
    }

    @GetMapping("/popular")
    public ResponseEntity<PopularTagsResponse> getPopularTags(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "timeRange", defaultValue = "month") String timeRange) {

        Map<String, Object> requestData = new java.util.HashMap<>();
        requestData.put("limit", limit);
        requestData.put("timeRange", timeRange);
        printRequest(requestData);

        try {
            // 验证参数
            if (limit < 1 || limit > 50) {
                limit = 10;
            }

            // 计算时间范围
            LocalDateTime startDate = calculateStartDate(timeRange);

            // 查询热门文档标签（按使用次数排序）
            String popularDocTagsSql =
                    "SELECT dt.tag_id, dt.tag_name, dt.color, COUNT(dtr.relation_id) as usage_count " +
                            "FROM document_tags dt " +
                            "LEFT JOIN document_tag_relations dtr ON dt.tag_id = dtr.tag_id " +
                            "LEFT JOIN documents d ON dtr.document_id = d.document_id " +
                            "WHERE d.created_at >= ? OR d.created_at IS NULL " +
                            "GROUP BY dt.tag_id, dt.tag_name, dt.color " +
                            "ORDER BY usage_count DESC " +
                            "LIMIT ?";

            List<Map<String, Object>> docResults = jdbcTemplate.queryForList(
                    popularDocTagsSql, startDate, limit);

            List<Tag> popularTags = new ArrayList<>();

            for (Map<String, Object> result : docResults) {
                Tag tag = new Tag();
                tag.setId(String.valueOf(result.get("tag_id")));
                tag.setName((String) result.get("tag_name"));
                tag.setColor((String) result.get("color"));
                tag.setType("document");
                tag.setUsageCount(((Number) result.get("usage_count")).intValue());

                // 查询文档数量
                String countSql = "SELECT COUNT(*) FROM document_tag_relations WHERE tag_id = ?";
                int docCount = jdbcTemplate.queryForObject(countSql, Integer.class, result.get("tag_id"));
                tag.setDocumentCount(docCount);

                popularTags.add(tag);
            }

            // 如果文档标签不够，补充词汇标签
            if (popularTags.size() < limit) {
                int remaining = limit - popularTags.size();

                String popularVocabTagsSql =
                        "SELECT vt.tag_id, vt.tag_name, vt.color, COUNT(uvt.relation_id) as usage_count " +
                                "FROM vocabulary_tags vt " +
                                "LEFT JOIN user_vocabulary_tags uvt ON vt.tag_id = uvt.tag_id " +
                                "GROUP BY vt.tag_id, vt.tag_name, vt.color " +
                                "ORDER BY usage_count DESC " +
                                "LIMIT ?";

                List<Map<String, Object>> vocabResults = jdbcTemplate.queryForList(
                        popularVocabTagsSql, remaining);

                for (Map<String, Object> result : vocabResults) {
                    Tag tag = new Tag();
                    tag.setId(String.valueOf(result.get("tag_id")));
                    tag.setName((String) result.get("tag_name"));
                    tag.setColor((String) result.get("color"));
                    tag.setType("vocabulary");
                    tag.setUsageCount(((Number) result.get("usage_count")).intValue());
                    tag.setDocumentCount(0);

                    popularTags.add(tag);
                }
            }

            printQueryResult(popularTags);

            // 创建响应
            PopularTagsResponse response = new PopularTagsResponse(true, popularTags);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取热门标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new PopularTagsResponse(false, new ArrayList<>())
            );
        }
    }

    // 计算开始日期
    private LocalDateTime calculateStartDate(String timeRange) {
        LocalDateTime now = LocalDateTime.now();

        switch (timeRange.toLowerCase()) {
            case "day":
                return now.minusDays(1);
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            case "year":
                return now.minusYears(1);
            default:
                return now.minusMonths(1); // 默认一个月
        }
    }
}
