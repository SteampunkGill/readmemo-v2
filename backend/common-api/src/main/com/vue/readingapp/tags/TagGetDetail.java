package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/tags")
public class TagGetDetail {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取标签详情请求 ===");
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

    // 文档DTO
    public static class Document {
        private String id;
        private String title;
        private String createdAt;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // 标签响应DTO
    public static class Tag {
        private String id;
        private String name;
        private String color;
        private String description;
        private String type;
        private int documentCount;
        private String createdAt;
        private String updatedAt;
        private List<Document> documents;

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

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public List<Document> getDocuments() { return documents; }
        public void setDocuments(List<Document> documents) { this.documents = documents; }
    }

    // 响应类
    public static class TagDetailResponse {
        private boolean success;
        private Tag data;

        public TagDetailResponse(boolean success, Tag data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Tag getData() { return data; }
        public void setData(Tag data) { this.data = data; }
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<TagDetailResponse> getTagDetail(@PathVariable("tagId") String tagId) {
        printRequest(tagId);

        try {
            // 先检查标签是否存在（在document_tags中）
            String checkSql = "SELECT * FROM document_tags WHERE tag_id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql, Integer.parseInt(tagId));

            String tableName = "document_tags";
            if (results.isEmpty()) {
                // 如果在document_tags中不存在，检查vocabulary_tags
                checkSql = "SELECT * FROM vocabulary_tags WHERE tag_id = ?";
                results = jdbcTemplate.queryForList(checkSql, Integer.parseInt(tagId));
                if (results.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                            new TagDetailResponse(false, null)
                    );
                }
                tableName = "vocabulary_tags";
            }

            Map<String, Object> tagMap = results.get(0);
            printQueryResult(tagMap);

            // 构建标签对象
            Tag tag = new Tag();
            tag.setId(String.valueOf(tagMap.get("tag_id")));
            tag.setName((String) tagMap.get("tag_name"));
            tag.setColor((String) tagMap.get("color"));
            tag.setDescription((String) tagMap.get("description"));
            tag.setType("document".equals(tableName) ? "document" : "vocabulary");
            tag.setCreatedAt(tagMap.get("created_at").toString());
            tag.setUpdatedAt(tagMap.get("updated_at").toString());

            // 查询关联的文档（如果是文档标签）
            if ("document_tags".equals(tableName)) {
                // 查询文档数量
                String countSql = "SELECT COUNT(*) FROM document_tag_relations WHERE tag_id = ?";
                int docCount = jdbcTemplate.queryForObject(countSql, Integer.class, Integer.parseInt(tagId));
                tag.setDocumentCount(docCount);

                // 查询关联的文档详情
                String docSql = "SELECT d.document_id, d.title, d.created_at " +
                        "FROM documents d " +
                        "JOIN document_tag_relations r ON d.document_id = r.document_id " +
                        "WHERE r.tag_id = ? " +
                        "ORDER BY d.created_at DESC " +
                        "LIMIT 10";

                List<Map<String, Object>> docResults = jdbcTemplate.queryForList(docSql, Integer.parseInt(tagId));
                List<Document> documents = new ArrayList<>();

                for (Map<String, Object> docMap : docResults) {
                    Document doc = new Document();
                    doc.setId(String.valueOf(docMap.get("document_id")));
                    doc.setTitle((String) docMap.get("title"));
                    doc.setCreatedAt(docMap.get("created_at").toString());
                    documents.add(doc);
                }

                tag.setDocuments(documents);
            } else {
                tag.setDocumentCount(0);
                tag.setDocuments(new ArrayList<>());
            }

            // 创建响应
            TagDetailResponse response = new TagDetailResponse(true, tag);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("标签ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new TagDetailResponse(false, null)
            );
        } catch (Exception e) {
            System.err.println("获取标签详情过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new TagDetailResponse(false, null)
            );
        }
    }
}
