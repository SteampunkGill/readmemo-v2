package com.vue.readingapp.tags;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tags/document")
public class TagCreateDocumentTag {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到创建文档标签请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=========================");
    }

    // 打印返回数据
    private void printResponse(Object response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class CreateDocumentTagRequest {
        private String name;
        private String color;
        private String description;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
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
    }

    // 响应类
    public static class CreateDocumentTagResponse {
        private boolean success;
        private Tag data;

        public CreateDocumentTagResponse(boolean success, Tag data) {
            this.success = success;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Tag getData() { return data; }
        public void setData(Tag data) { this.data = data; }
    }

    @PostMapping("/")
    public ResponseEntity<CreateDocumentTagResponse> createDocumentTag(@RequestBody CreateDocumentTagRequest request) {
        printRequest(request);

        try {
            // 验证请求数据
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new CreateDocumentTagResponse(false, null)
                );
            }

            // 检查标签名是否已存在
            String checkSql = "SELECT COUNT(*) FROM document_tags WHERE tag_name = ?";
            int count = jdbcTemplate.queryForObject(checkSql, Integer.class, request.getName());
            if (count > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new CreateDocumentTagResponse(false, null)
                );
            }

            // 插入文档标签
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            String insertSql = "INSERT INTO document_tags (tag_name, color, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, request.getName());
                ps.setString(2, request.getColor());
                ps.setString(3, request.getDescription());
                ps.setTimestamp(4, timestamp);
                ps.setTimestamp(5, timestamp);
                return ps;
            }, keyHolder);

            // 获取生成的ID
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new RuntimeException("创建文档标签失败，未获取到ID");
            }
            int tagId = key.intValue();

            // 查询刚创建的标签
            String selectSql = "SELECT * FROM document_tags WHERE tag_id = ?";
            Map<String, Object> tagMap = jdbcTemplate.queryForMap(selectSql, tagId);

            // 构建返回的标签对象
            Tag tag = new Tag();
            tag.setId(String.valueOf(tagMap.get("tag_id")));
            tag.setName((String) tagMap.get("tag_name"));
            tag.setColor((String) tagMap.get("color"));
            tag.setDescription((String) tagMap.get("description"));
            tag.setType("document");
            tag.setDocumentCount(0);
            tag.setCreatedAt(tagMap.get("created_at").toString());

            // 创建响应
            CreateDocumentTagResponse response = new CreateDocumentTagResponse(true, tag);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("创建文档标签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CreateDocumentTagResponse(false, null)
            );
        }
    }
}
