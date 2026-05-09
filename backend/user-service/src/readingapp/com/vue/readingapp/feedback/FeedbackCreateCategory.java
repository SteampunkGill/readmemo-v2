package com.vue.readingapp.feedback;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackCreateCategory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到创建反馈分类请求 ===");
        System.out.println("请求数据: " + request);
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

    // 请求DTO
    public static class CreateCategoryRequest {
        private String name;
        private String value;
        private String description;
        private String icon;
        private String color;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    // 响应DTO
    public static class CreateCategoryResponse {
        private boolean success;
        private String message;
        private CategoryData data;

        public CreateCategoryResponse(boolean success, String message, CategoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public CategoryData getData() { return data; }
        public void setData(CategoryData data) { this.data = data; }
    }

    public static class CategoryData {
        private String id;
        private String name;
        private String value;
        private String description;
        private String icon;
        private String color;
        private String createdAt;

        public CategoryData(String id, String name, String value, String description,
                            String icon, String color, String createdAt) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.description = description;
            this.icon = icon;
            this.color = color;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    @PostMapping("/categories")
    public ResponseEntity<CreateCategoryResponse> createFeedbackCategory(
            @RequestBody CreateCategoryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new CreateCategoryResponse(false, "分类名称不能为空", null)
                );
            }

            if (request.getValue() == null || request.getValue().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new CreateCategoryResponse(false, "分类值不能为空", null)
                );
            }

            // 2. 验证用户身份（需要管理员权限）
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new CreateCategoryResponse(false, "用户未登录", null)
                );
            }

            // 检查是否是管理员
            String adminCheckSql = "SELECT role FROM users WHERE user_id = ?";
            List<Map<String, Object>> userList = jdbcTemplate.queryForList(adminCheckSql, userId);

            boolean isAdmin = false;
            if (!userList.isEmpty()) {
                Map<String, Object> user = userList.get(0);
                String role = (String) user.get("role");
                isAdmin = "admin".equals(role) || "moderator".equals(role);
            }

            if (!isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new CreateCategoryResponse(false, "需要管理员权限", null)
                );
            }

            // 3. 检查分类值是否已存在
            String checkSql = "SELECT COUNT(*) as count FROM feedback_categories WHERE value = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, request.getValue());

            if (count != null && count > 0) {
                return ResponseEntity.badRequest().body(
                        new CreateCategoryResponse(false, "分类值已存在: " + request.getValue(), null)
                );
            }

            // 4. 生成分类ID
            String categoryId = "cat_" + UUID.randomUUID().toString().substring(0, 8);

            // 5. 插入分类记录
            String insertSql = "INSERT INTO feedback_categories (category_id, name, value, description, icon, color, order_index, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 0, TRUE, NOW(), NOW())";

            int rowsAffected = jdbcTemplate.update(insertSql,
                    categoryId,
                    request.getName(),
                    request.getValue(),
                    request.getDescription() != null ? request.getDescription() : "",
                    request.getIcon() != null ? request.getIcon() : "tag",
                    request.getColor() != null ? request.getColor() : "#607D8B"
            );

            printQueryResult("插入行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new CreateCategoryResponse(false, "创建分类失败", null)
                );
            }

            // 6. 查询刚插入的分类
            String selectSql = "SELECT * FROM feedback_categories WHERE category_id = ?";
            List<Map<String, Object>> categoryList = jdbcTemplate.queryForList(selectSql, categoryId);
            printQueryResult("分类详情: " + categoryList);

            if (categoryList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new CreateCategoryResponse(false, "无法获取创建的分类信息", null)
                );
            }

            Map<String, Object> category = categoryList.get(0);

            // 7. 准备响应数据
            CategoryData categoryData = new CategoryData(
                    categoryId,
                    request.getName(),
                    request.getValue(),
                    request.getDescription() != null ? request.getDescription() : "",
                    request.getIcon() != null ? request.getIcon() : "tag",
                    request.getColor() != null ? request.getColor() : "#607D8B",
                    category.get("created_at").toString()
            );

            CreateCategoryResponse response = new CreateCategoryResponse(
                    true, "创建反馈分类成功", categoryData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("创建反馈分类过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CreateCategoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}