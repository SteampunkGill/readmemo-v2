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
public class FeedbackGetCategories {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取反馈分类请求 ===");
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

    // 响应DTO
    public static class GetCategoriesResponse {
        private boolean success;
        private String message;
        private List<CategoryData> data;

        public GetCategoriesResponse(boolean success, String message, List<CategoryData> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<CategoryData> getData() { return data; }
        public void setData(List<CategoryData> data) { this.data = data; }
    }

    public static class CategoryData {
        private String id;
        private String name;
        private String value;
        private String description;
        private String icon;
        private String color;

        public CategoryData(String id, String name, String value, String description, String icon, String color) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.description = description;
            this.icon = icon;
            this.color = color;
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
    }

    @GetMapping("/categories")
    public ResponseEntity<GetCategoriesResponse> getFeedbackCategories() {

        // 打印接收到的请求
        printRequest("获取所有反馈分类");

        try {
            // 1. 查询所有激活的分类
            String sql = "SELECT category_id, name, value, description, icon, color " +
                    "FROM feedback_categories " +
                    "WHERE is_active = TRUE " +
                    "ORDER BY order_index, name";

            List<Map<String, Object>> categoryList = jdbcTemplate.queryForList(sql);
            printQueryResult("查询到 " + categoryList.size() + " 个分类");

            // 2. 转换数据
            List<CategoryData> categories = new ArrayList<>();

            // 添加默认分类
            categories.add(new CategoryData("bug", "错误报告", "bug", "系统功能异常或错误", "bug", "#F44336"));
            categories.add(new CategoryData("feature", "功能建议", "feature", "新功能或改进建议", "light-bulb", "#2196F3"));
            categories.add(new CategoryData("improvement", "改进建议", "improvement", "现有功能优化建议", "chart-bar", "#4CAF50"));
            categories.add(new CategoryData("question", "问题咨询", "question", "使用问题或咨询", "question-mark-circle", "#FF9800"));
            categories.add(new CategoryData("other", "其他反馈", "other", "其他类型的反馈", "chat-alt-2", "#9C27B0"));

            // 添加数据库中的自定义分类
            for (Map<String, Object> row : categoryList) {
                CategoryData category = new CategoryData(
                        String.valueOf(row.get("category_id")),
                        (String) row.get("name"),
                        (String) row.get("value"),
                        (String) row.get("description"),
                        (String) row.get("icon"),
                        (String) row.get("color")
                );
                categories.add(category);
            }

            // 3. 准备响应数据
            GetCategoriesResponse response = new GetCategoriesResponse(
                    true, "获取反馈分类成功", categories
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取反馈分类过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetCategoriesResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}