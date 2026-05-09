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
public class FeedbackGetTypes {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取反馈类型请求 ===");
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
    public static class GetTypesResponse {
        private boolean success;
        private String message;
        private List<TypeData> data;

        public GetTypesResponse(boolean success, String message, List<TypeData> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<TypeData> getData() { return data; }
        public void setData(List<TypeData> data) { this.data = data; }
    }

    public static class TypeData {
        private String id;
        private String name;
        private String value;
        private String description;
        private String icon;
        private String color;
        private int feedbackCount;

        public TypeData(String id, String name, String value, String description,
                        String icon, String color, int feedbackCount) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.description = description;
            this.icon = icon;
            this.color = color;
            this.feedbackCount = feedbackCount;
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

        public int getFeedbackCount() { return feedbackCount; }
        public void setFeedbackCount(int feedbackCount) { this.feedbackCount = feedbackCount; }
    }

    @GetMapping("/types")
    public ResponseEntity<GetTypesResponse> getFeedbackTypes() {

        // 打印接收到的请求
        printRequest("获取所有反馈类型");

        try {
            // 1. 查询所有反馈类型及其统计
            List<TypeData> types = new ArrayList<>();

            // 2. 添加默认类型
            // bug类型
            int bugCount = getFeedbackCountByType("bug");
            types.add(new TypeData("bug", "错误报告", "bug", "系统功能异常或错误", "bug", "#F44336", bugCount));

            // feature类型
            int featureCount = getFeedbackCountByType("feature");
            types.add(new TypeData("feature", "功能建议", "feature", "新功能或改进建议", "light-bulb", "#2196F3", featureCount));

            // improvement类型
            int improvementCount = getFeedbackCountByType("improvement");
            types.add(new TypeData("improvement", "改进建议", "improvement", "现有功能优化建议", "chart-bar", "#4CAF50", improvementCount));

            // question类型
            int questionCount = getFeedbackCountByType("question");
            types.add(new TypeData("question", "问题咨询", "question", "使用问题或咨询", "question-mark-circle", "#FF9800", questionCount));

            // other类型
            int otherCount = getFeedbackCountByType("other");
            types.add(new TypeData("other", "其他反馈", "other", "其他类型的反馈", "chat-alt-2", "#9C27B0", otherCount));

            // 3. 查询自定义类型
            String customSql = "SELECT c.category_id, c.name, c.value, c.description, c.icon, c.color, " +
                    "COUNT(f.feedback_id) as feedback_count " +
                    "FROM feedback_categories c " +
                    "LEFT JOIN user_feedback f ON c.value = f.type " +
                    "WHERE c.is_active = TRUE " +
                    "GROUP BY c.category_id, c.name, c.value, c.description, c.icon, c.color " +
                    "ORDER BY c.order_index, c.name";

            List<Map<String, Object>> customTypes = jdbcTemplate.queryForList(customSql);
            printQueryResult("自定义类型: " + customTypes);

            for (Map<String, Object> row : customTypes) {
                TypeData type = new TypeData(
                        (String) row.get("category_id"),
                        (String) row.get("name"),
                        (String) row.get("value"),
                        (String) row.get("description"),
                        (String) row.get("icon"),
                        (String) row.get("color"),
                        ((Number) row.get("feedback_count")).intValue()
                );
                types.add(type);
            }

            // 4. 按反馈数量排序
            types.sort((a, b) -> Integer.compare(b.getFeedbackCount(), a.getFeedbackCount()));

            // 5. 准备响应数据
            GetTypesResponse response = new GetTypesResponse(
                    true, "获取反馈类型成功", types
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取反馈类型过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetTypesResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    private int getFeedbackCountByType(String type) {
        try {
            String sql = "SELECT COUNT(*) as count FROM user_feedback WHERE type = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, type);
            return count != null ? count : 0;
        } catch (Exception e) {
            System.err.println("获取类型统计失败: " + e.getMessage());
            return 0;
        }
    }
}