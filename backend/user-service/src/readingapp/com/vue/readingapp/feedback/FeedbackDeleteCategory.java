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
public class FeedbackDeleteCategory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除反馈分类请求 ===");
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
    public static class DeleteCategoryRequest {
        private String categoryId;

        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    }

    // 响应DTO
    public static class DeleteCategoryResponse {
        private boolean success;
        private String message;
        private DeleteData data;

        public DeleteCategoryResponse(boolean success, String message, DeleteData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DeleteData getData() { return data; }
        public void setData(DeleteData data) { this.data = data; }
    }

    public static class DeleteData {
        private String categoryId;
        private boolean deleted;
        private String deletedAt;

        public DeleteData(String categoryId, boolean deleted, String deletedAt) {
            this.categoryId = categoryId;
            this.deleted = deleted;
            this.deletedAt = deletedAt;
        }

        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }

        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<DeleteCategoryResponse> deleteFeedbackCategory(
            @PathVariable String categoryId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 创建请求对象用于打印
        DeleteCategoryRequest request = new DeleteCategoryRequest();
        request.setCategoryId(categoryId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (categoryId == null || categoryId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new DeleteCategoryResponse(false, "分类ID不能为空", null)
                );
            }

            // 2. 验证用户身份（需要管理员权限）
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteCategoryResponse(false, "用户未登录", null)
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
                        new DeleteCategoryResponse(false, "需要管理员权限", null)
                );
            }

            // 3. 检查分类是否存在
            String checkSql = "SELECT * FROM feedback_categories WHERE category_id = ?";
            List<Map<String, Object>> categoryList = jdbcTemplate.queryForList(checkSql, categoryId);

            if (categoryList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteCategoryResponse(false, "未找到指定的分类", null)
                );
            }

            Map<String, Object> category = categoryList.get(0);
            String categoryValue = (String) category.get("value");

            // 4. 检查是否有反馈使用此分类
            String feedbackCheckSql = "SELECT COUNT(*) as count FROM user_feedback WHERE type = ?";
            Integer feedbackCount = jdbcTemplate.queryForObject(feedbackCheckSql, Integer.class, categoryValue);

            if (feedbackCount != null && feedbackCount > 0) {
                // 如果有反馈使用此分类，不能直接删除，可以标记为不活跃
                String deactivateSql = "UPDATE feedback_categories SET is_active = FALSE, updated_at = NOW() WHERE category_id = ?";
                int rowsAffected = jdbcTemplate.update(deactivateSql, categoryId);

                printQueryResult("标记为不活跃行数: " + rowsAffected);

                if (rowsAffected > 0) {
                    DeleteData deleteData = new DeleteData(
                            categoryId,
                            true,
                            LocalDateTime.now().toString()
                    );

                    DeleteCategoryResponse response = new DeleteCategoryResponse(
                            true, "分类已被标记为不活跃（有反馈使用此分类）", deleteData
                    );

                    printResponse(response);
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            new DeleteCategoryResponse(false, "标记分类为不活跃失败", null)
                    );
                }
            }

            // 5. 删除分类记录
            String deleteSql = "DELETE FROM feedback_categories WHERE category_id = ?";
            int rowsAffected = jdbcTemplate.update(deleteSql, categoryId);

            printQueryResult("删除行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteCategoryResponse(false, "删除分类失败", null)
                );
            }

            // 6. 准备响应数据
            DeleteData deleteData = new DeleteData(
                    categoryId,
                    true,
                    LocalDateTime.now().toString()
            );

            DeleteCategoryResponse response = new DeleteCategoryResponse(
                    true, "删除反馈分类成功", deleteData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除反馈分类过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteCategoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}