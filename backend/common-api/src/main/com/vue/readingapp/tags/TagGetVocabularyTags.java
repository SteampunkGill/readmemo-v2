
package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/tags/vocabulary")
public class TagGetVocabularyTags {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取生词标签列表请求 ===");
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

    // 标签实体类
    public static class Tag {
        private String id;
        private String name;
        private String color;
        private String description;
        private String type;
        private int vocabularyCount;
        private String createdAt;

        public Tag() {}

        public Tag(String id, String name, String color, String description, String type,
                   int vocabularyCount, String createdAt) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.description = description;
            this.type = type;
            this.vocabularyCount = vocabularyCount;
            this.createdAt = createdAt;
        }

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

        public int getVocabularyCount() { return vocabularyCount; }
        public void setVocabularyCount(int vocabularyCount) { this.vocabularyCount = vocabularyCount; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // 分页信息类
    public static class Pagination {
        private int page;
        private int limit;
        private int total;

        public Pagination(int page, int limit, int total) {
            this.page = page;
            this.limit = limit;
            this.total = total;
        }

        // Getters and Setters
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
    }

    // 响应类
    public static class VocabularyTagsResponse {
        private boolean success;
        private List<Tag> data;
        private Pagination pagination;

        public VocabularyTagsResponse(boolean success, List<Tag> data, Pagination pagination) {
            this.success = success;
            this.data = data;
            this.pagination = pagination;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<Tag> getData() { return data; }
        public void setData(List<Tag> data) { this.data = data; }

        public Pagination getPagination() { return pagination; }
        public void setPagination(Pagination pagination) { this.pagination = pagination; }
    }

    @GetMapping("/")
    public ResponseEntity<VocabularyTagsResponse> getVocabularyTags(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "sort", defaultValue = "name") String sort,
            @RequestParam(value = "order", defaultValue = "asc") String order) {

        // 打印请求参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("page", page);
        requestParams.put("limit", limit);
        requestParams.put("sort", sort);
        requestParams.put("order", order);
        printRequest(requestParams);

        try {
            // 验证页码和限制
            if (page < 1) {
                page = 1;
            }
            if (limit < 1 || limit > 100) {
                limit = 20;
            }

            // 计算偏移量
            int offset = (page - 1) * limit;

            // 构建查询
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT * FROM vocabulary_tags WHERE 1=1");

            // 添加排序
            String orderBy = "tag_name";
            if ("createdAt".equals(sort)) {
                orderBy = "created_at";
            }

            queryBuilder.append(" ORDER BY ").append(orderBy);
            queryBuilder.append(" ").append("asc".equalsIgnoreCase(order) ? "ASC" : "DESC");
            queryBuilder.append(" LIMIT ? OFFSET ?");

            // 查询标签数据
            List<Tag> tags = jdbcTemplate.query(queryBuilder.toString(),
                    new Object[]{limit, offset},
                    new RowMapper<Tag>() {
                        @Override
                        public Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
                            Tag tag = new Tag();
                            tag.setId(String.valueOf(rs.getInt("tag_id")));
                            tag.setName(rs.getString("tag_name"));
                            tag.setColor(rs.getString("color"));
                            tag.setDescription(rs.getString("description"));
                            tag.setType("vocabulary");
                            tag.setCreatedAt(rs.getTimestamp("created_at").toString());

                            // 查询词汇数量
                            String countSql = "SELECT COUNT(*) FROM user_vocabulary_tags WHERE tag_id = ?";
                            int count = jdbcTemplate.queryForObject(countSql, Integer.class, rs.getInt("tag_id"));
                            tag.setVocabularyCount(count);

                            return tag;
                        }
                    });

            printQueryResult(tags);

            // 查询总数
            String countQuery = "SELECT COUNT(*) FROM vocabulary_tags";
            int total = jdbcTemplate.queryForObject(countQuery, Integer.class);

            // 创建分页信息
            Pagination pagination = new Pagination(page, limit, total);

            // 创建响应
            VocabularyTagsResponse response = new VocabularyTagsResponse(true, tags, pagination);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取生词标签列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            VocabularyTagsResponse errorResponse = new VocabularyTagsResponse(false, null, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}