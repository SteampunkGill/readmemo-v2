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
@RequestMapping("/api/v1/documents")
public class TagGetDocumentTags {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取文档标签请求 ===");
        System.out.println("请求参数: " + request);
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

    // 标签实体类
    public static class Tag {
        private String id;
        private String name;
        private String color;
        private String description;
        private String type;
        private String createdAt;

        public Tag() {}

        public Tag(String id, String name, String color, String description, String type, String createdAt) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.description = description;
            this.type = type;
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

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // 响应类
    public static class DocumentTagsResponse {
        private boolean success;
        private List<Tag> data;

        public DocumentTagsResponse(boolean success, List<Tag> data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public List<Tag> getData() { return data; }
        public void setData(List<Tag> data) { this.data = data; }
    }

    @GetMapping("/{documentId}/tags")
    public ResponseEntity<DocumentTagsResponse> getDocumentTags(@PathVariable("documentId") String documentId) {
        printRequest(documentId);

        try {
            // 验证文档是否存在
            String checkDocumentSql = "SELECT COUNT(*) FROM documents WHERE document_id = ?";
            int documentCount = jdbcTemplate.queryForObject(checkDocumentSql, Integer.class, Integer.parseInt(documentId));
            if (documentCount == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DocumentTagsResponse(false, null)
                );
            }

            // 查询文档的所有标签
            String sql =
                    "SELECT dt.tag_id, dt.tag_name, dt.color, dt.description, dt.created_at " +
                            "FROM document_tags dt " +
                            "JOIN document_tag_relations dtr ON dt.tag_id = dtr.tag_id " +
                            "WHERE dtr.document_id = ? " +
                            "ORDER BY dt.tag_name";

            List<Tag> tags = jdbcTemplate.query(sql,
                    new Object[]{Integer.parseInt(documentId)},
                    new RowMapper<Tag>() {
                        @Override
                        public Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
                            Tag tag = new Tag();
                            tag.setId(String.valueOf(rs.getInt("tag_id")));
                            tag.setName(rs.getString("tag_name"));
                            tag.setColor(rs.getString("color"));
                            tag.setDescription(rs.getString("description"));
                            tag.setType("document");
                            tag.setCreatedAt(rs.getTimestamp("created_at").toString());
                            return tag;
                        }
                    });

            printQueryResult(tags);

            // 创建响应
            DocumentTagsResponse response = new DocumentTagsResponse(true, tags);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            System.err.println("文档ID格式错误: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    new DocumentTagsResponse(false, null)
            );
        } catch (Exception e) {
            System.err.println("获取文档标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            DocumentTagsResponse errorResponse = new DocumentTagsResponse(false, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
