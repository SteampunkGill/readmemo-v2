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
public class FeedbackDelete {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除反馈请求 ===");
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

    // 请求DTO
    public static class DeleteRequest {
        private String feedbackId;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    }

    // 响应DTO
    public static class DeleteResponse {
        private boolean success;
        private String message;
        private DeleteData data;

        public DeleteResponse(boolean success, String message, DeleteData data) {
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
        private String feedbackId;
        private boolean deleted;
        private String deletedAt;

        public DeleteData(String feedbackId, boolean deleted, String deletedAt) {
            this.feedbackId = feedbackId;
            this.deleted = deleted;
            this.deletedAt = deletedAt;
        }

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }

        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
    }

    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<DeleteResponse> deleteFeedback(
            @PathVariable String feedbackId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 创建请求对象用于打印
        DeleteRequest request = new DeleteRequest();
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new DeleteResponse(false, "反馈ID不能为空", null)
                );
            }

            // 2. 检查反馈是否存在
            String checkSql = "SELECT feedback_id, user_id FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteResponse(false, "未找到指定的反馈", null)
                );
            }

            Map<String, Object> feedback = feedbackList.get(0);
            String feedbackUserId = (String) feedback.get("user_id");

            // 3. 检查权限（只有反馈创建者或管理员可以删除）
            // 这里简化处理，实际项目中应该有更完善的权限控制
            if (userId != null && !userId.equals(feedbackUserId)) {
                // 检查是否是管理员（简化处理）
                String adminCheckSql = "SELECT role FROM users WHERE user_id = ?";
                List<Map<String, Object>> userList = jdbcTemplate.queryForList(adminCheckSql, userId);

                if (!userList.isEmpty()) {
                    Map<String, Object> user = userList.get(0);
                    String role = (String) user.get("role");

                    if (!"admin".equals(role) && !"moderator".equals(role)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                new DeleteResponse(false, "没有权限删除此反馈", null)
                        );
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                            new DeleteResponse(false, "没有权限删除此反馈", null)
                    );
                }
            }

            // 4. 先删除相关的投票记录
            String deleteVotesSql = "DELETE FROM feedback_votes WHERE feedback_id = ?";
            int votesDeleted = jdbcTemplate.update(deleteVotesSql, feedbackId);
            printQueryResult("删除投票记录: " + votesDeleted + " 条");

            // 5. 先删除相关的回复记录
            String deleteRepliesSql = "DELETE FROM feedback_replies WHERE feedback_id = ?";
            int repliesDeleted = jdbcTemplate.update(deleteRepliesSql, feedbackId);
            printQueryResult("删除回复记录: " + repliesDeleted + " 条");

            // 6. 先删除相关的变更日志
            String deleteLogsSql = "DELETE FROM feedback_change_log WHERE feedback_id = ?";
            int logsDeleted = jdbcTemplate.update(deleteLogsSql, feedbackId);
            printQueryResult("删除变更日志: " + logsDeleted + " 条");

            // 7. 删除反馈主记录
            String deleteFeedbackSql = "DELETE FROM user_feedback WHERE feedback_id = ?";
            int feedbackDeleted = jdbcTemplate.update(deleteFeedbackSql, feedbackId);
            printQueryResult("删除反馈记录: " + feedbackDeleted + " 条");

            if (feedbackDeleted == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteResponse(false, "删除反馈失败", null)
                );
            }

            // 8. 准备响应数据
            DeleteData deleteData = new DeleteData(
                    feedbackId,
                    true,
                    LocalDateTime.now().toString()
            );

            DeleteResponse response = new DeleteResponse(
                    true, "反馈删除成功", deleteData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("删除反馈过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}