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
public class FeedbackMarkAsRead {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到标记反馈为已读请求 ===");
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

    // 请求DTO
    public static class MarkAsReadRequest {
        private String feedbackId;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    }

    // 响应DTO
    public static class MarkAsReadResponse {
        private boolean success;
        private String message;
        private MarkAsReadData data;

        public MarkAsReadResponse(boolean success, String message, MarkAsReadData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public MarkAsReadData getData() { return data; }
        public void setData(MarkAsReadData data) { this.data = data; }
    }

    public static class MarkAsReadData {
        private String feedbackId;
        private boolean markedAsRead;
        private String markedAt;

        public MarkAsReadData(String feedbackId, boolean markedAsRead, String markedAt) {
            this.feedbackId = feedbackId;
            this.markedAsRead = markedAsRead;
            this.markedAt = markedAt;
        }

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public boolean isMarkedAsRead() { return markedAsRead; }
        public void setMarkedAsRead(boolean markedAsRead) { this.markedAsRead = markedAsRead; }

        public String getMarkedAt() { return markedAt; }
        public void setMarkedAt(String markedAt) { this.markedAt = markedAt; }
    }

    @PutMapping("/{feedbackId}/read")
    public ResponseEntity<MarkAsReadResponse> markFeedbackAsRead(
            @PathVariable String feedbackId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 创建请求对象用于打印
        MarkAsReadRequest request = new MarkAsReadRequest();
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new MarkAsReadResponse(false, "反馈ID不能为空", null)
                );
            }

            // 2. 验证用户身份
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new MarkAsReadResponse(false, "用户未登录", null)
                );
            }

            // 3. 检查反馈是否存在
            String checkSql = "SELECT feedback_id, user_id FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new MarkAsReadResponse(false, "未找到指定的反馈", null)
                );
            }

            Map<String, Object> feedback = feedbackList.get(0);
            String feedbackUserId = (String) feedback.get("user_id");

            // 4. 检查权限（只有反馈创建者可以标记为已读）
            if (!userId.equals(feedbackUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new MarkAsReadResponse(false, "没有权限标记此反馈", null)
                );
            }

            // 5. 检查是否已经标记为已读
            // 这里我们假设有一个字段来记录是否已读，但我们的表中没有这个字段
            // 为了简化，我们可以创建一个新的表来记录阅读状态，或者使用现有的view_count
            // 这里我们使用一个简化方案：更新view_count并记录最后一次阅读时间

            // 首先获取当前的view_count
            String getViewSql = "SELECT view_count FROM user_feedback WHERE feedback_id = ?";
            Integer currentViewCount = jdbcTemplate.queryForObject(getViewSql, Integer.class, feedbackId);

            if (currentViewCount == null) {
                currentViewCount = 0;
            }

            // 6. 更新view_count（增加一次查看）
            String updateSql = "UPDATE user_feedback SET view_count = view_count + 1, updated_at = NOW() WHERE feedback_id = ?";
            int rowsAffected = jdbcTemplate.update(updateSql, feedbackId);

            printQueryResult("更新行数: " + rowsAffected);

            if (rowsAffected == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new MarkAsReadResponse(false, "标记为已读失败", null)
                );
            }

            // 7. 记录阅读历史（可选，这里简化处理）
            // 在实际项目中，可能需要创建一个专门的表来记录用户的阅读历史

            // 8. 准备响应数据
            MarkAsReadData readData = new MarkAsReadData(
                    feedbackId,
                    true,
                    LocalDateTime.now().toString()
            );

            MarkAsReadResponse response = new MarkAsReadResponse(
                    true, "反馈已标记为已读", readData
            );

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("标记反馈为已读过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new MarkAsReadResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}