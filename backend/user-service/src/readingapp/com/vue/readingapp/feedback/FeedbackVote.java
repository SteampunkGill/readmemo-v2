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
public class FeedbackVote {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到投票请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=================");
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
    public static class VoteRequest {
        private String feedbackId;
        private String voteType;

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getVoteType() { return voteType; }
        public void setVoteType(String voteType) { this.voteType = voteType; }
    }

    // 响应DTO
    public static class VoteResponse {
        private boolean success;
        private String message;
        private VoteData data;

        public VoteResponse(boolean success, String message, VoteData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public VoteData getData() { return data; }
        public void setData(VoteData data) { this.data = data; }
    }

    public static class VoteData {
        private String feedbackId;
        private String voteType;
        private int upvotes;
        private int downvotes;
        private String votedAt;

        public VoteData(String feedbackId, String voteType, int upvotes, int downvotes, String votedAt) {
            this.feedbackId = feedbackId;
            this.voteType = voteType;
            this.upvotes = upvotes;
            this.downvotes = downvotes;
            this.votedAt = votedAt;
        }

        public String getFeedbackId() { return feedbackId; }
        public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

        public String getVoteType() { return voteType; }
        public void setVoteType(String voteType) { this.voteType = voteType; }

        public int getUpvotes() { return upvotes; }
        public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

        public int getDownvotes() { return downvotes; }
        public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

        public String getVotedAt() { return votedAt; }
        public void setVotedAt(String votedAt) { this.votedAt = votedAt; }
    }

    @PostMapping("/{feedbackId}/vote")
    public ResponseEntity<VoteResponse> voteFeedback(
            @PathVariable String feedbackId,
            @RequestBody VoteRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // 设置feedbackId
        request.setFeedbackId(feedbackId);

        // 打印接收到的请求
        printRequest(request);

        try {
            // 1. 验证参数
            if (feedbackId == null || feedbackId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VoteResponse(false, "反馈ID不能为空", null)
                );
            }

            if (request.getVoteType() == null || request.getVoteType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new VoteResponse(false, "投票类型不能为空", null)
                );
            }

            // 验证投票类型
            List<String> validVoteTypes = List.of("upvote", "downvote");
            if (!validVoteTypes.contains(request.getVoteType())) {
                return ResponseEntity.badRequest().body(
                        new VoteResponse(false, "无效的投票类型", null)
                );
            }

            // 2. 验证用户身份
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new VoteResponse(false, "用户未登录", null)
                );
            }

            // 3. 检查反馈是否存在
            String checkSql = "SELECT feedback_id, upvotes, downvotes FROM user_feedback WHERE feedback_id = ?";
            List<Map<String, Object>> feedbackList = jdbcTemplate.queryForList(checkSql, feedbackId);

            if (feedbackList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new VoteResponse(false, "未找到指定的反馈", null)
                );
            }

            Map<String, Object> feedback = feedbackList.get(0);
            int currentUpvotes = ((Number) feedback.get("upvotes")).intValue();
            int currentDownvotes = ((Number) feedback.get("downvotes")).intValue();

            // 4. 检查用户是否已经投票
            String voteCheckSql = "SELECT vote_type FROM feedback_votes WHERE feedback_id = ? AND user_id = ?";
            List<Map<String, Object>> existingVotes = jdbcTemplate.queryForList(voteCheckSql, feedbackId, userId);

            String newVoteType = request.getVoteType();
            String oldVoteType = null;

            if (!existingVotes.isEmpty()) {
                // 用户已经投过票
                oldVoteType = (String) existingVotes.get(0).get("vote_type");

                if (oldVoteType.equals(newVoteType)) {
                    // 如果投的是同样的票，取消投票
                    String deleteVoteSql = "DELETE FROM feedback_votes WHERE feedback_id = ? AND user_id = ?";
                    int deleteRows = jdbcTemplate.update(deleteVoteSql, feedbackId, userId);

                    if (deleteRows > 0) {
                        // 更新反馈的投票计数
                        if ("upvote".equals(oldVoteType)) {
                            currentUpvotes--;
                        } else {
                            currentDownvotes--;
                        }

                        String updateFeedbackSql = "UPDATE user_feedback SET upvotes = ?, downvotes = ?, updated_at = NOW() WHERE feedback_id = ?";
                        jdbcTemplate.update(updateFeedbackSql, currentUpvotes, currentDownvotes, feedbackId);

                        VoteData voteData = new VoteData(
                                feedbackId,
                                "canceled",
                                currentUpvotes,
                                currentDownvotes,
                                LocalDateTime.now().toString()
                        );

                        VoteResponse response = new VoteResponse(
                                true, "投票已取消", voteData
                        );

                        printResponse(response);
                        return ResponseEntity.ok(response);
                    }
                } else {
                    // 用户改变投票类型
                    String updateVoteSql = "UPDATE feedback_votes SET vote_type = ?, created_at = NOW() WHERE feedback_id = ? AND user_id = ?";
                    int updateRows = jdbcTemplate.update(updateVoteSql, newVoteType, feedbackId, userId);

                    if (updateRows > 0) {
                        // 更新反馈的投票计数
                        if ("upvote".equals(oldVoteType)) {
                            // 原来是赞，现在是踩
                            currentUpvotes--;
                            currentDownvotes++;
                        } else {
                            // 原来是踩，现在是赞
                            currentDownvotes--;
                            currentUpvotes++;
                        }

                        String updateFeedbackSql = "UPDATE user_feedback SET upvotes = ?, downvotes = ?, updated_at = NOW() WHERE feedback_id = ?";
                        jdbcTemplate.update(updateFeedbackSql, currentUpvotes, currentDownvotes, feedbackId);

                        VoteData voteData = new VoteData(
                                feedbackId,
                                newVoteType,
                                currentUpvotes,
                                currentDownvotes,
                                LocalDateTime.now().toString()
                        );

                        VoteResponse response = new VoteResponse(
                                true, "投票类型已更改", voteData
                        );

                        printResponse(response);
                        return ResponseEntity.ok(response);
                    }
                }
            } else {
                // 用户第一次投票
                String insertVoteSql = "INSERT INTO feedback_votes (vote_id, feedback_id, user_id, vote_type, created_at) " +
                        "VALUES (?, ?, ?, ?, NOW())";

                String voteId = "vote_" + UUID.randomUUID().toString().substring(0, 8);

                int insertRows = jdbcTemplate.update(insertVoteSql,
                        voteId,
                        feedbackId,
                        userId,
                        newVoteType
                );

                if (insertRows > 0) {
                    // 更新反馈的投票计数
                    if ("upvote".equals(newVoteType)) {
                        currentUpvotes++;
                    } else {
                        currentDownvotes++;
                    }

                    String updateFeedbackSql = "UPDATE user_feedback SET upvotes = ?, downvotes = ?, updated_at = NOW() WHERE feedback_id = ?";
                    jdbcTemplate.update(updateFeedbackSql, currentUpvotes, currentDownvotes, feedbackId);

                    VoteData voteData = new VoteData(
                            feedbackId,
                            newVoteType,
                            currentUpvotes,
                            currentDownvotes,
                            LocalDateTime.now().toString()
                    );

                    VoteResponse response = new VoteResponse(
                            true, "投票成功", voteData
                    );

                    printResponse(response);
                    return ResponseEntity.ok(response);
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VoteResponse(false, "投票失败", null)
            );

        } catch (Exception e) {
            System.err.println("投票过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new VoteResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}