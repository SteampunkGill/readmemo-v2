package com.vue.readingapp.vocabulary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 获取生词本日期统计的控制器
 * 
 * 用于在生词本首页展示“按日期查看”的功能，统计每天新增了多少个生词。
 */
@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyGetDates {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 日期统计响应的数据结构
     */
    public static class DateCountResponse {
        private boolean success;
        private String message;
        private List<DateCount> data;

        public DateCountResponse(boolean success, String message, List<DateCount> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public List<DateCount> getData() { return data; }
        public String getMessage() { return message; }
    }

    /**
     * 单个日期的统计数据
     */
    public static class DateCount {
        private String date; // 日期字符串，如 "2023-10-27"
        private int count;   // 该日期下的生词数量

        public DateCount(String date, int count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() { return date; }
        public int getCount() { return count; }
    }

    /**
     * 获取生词日期分布统计接口
     * 
     * @param authHeader 包含 Bearer Token 的请求头
     * @return 包含日期和对应单词数量的列表
     */
    @GetMapping("/dates")
    public ResponseEntity<DateCountResponse> getVocabularyDates(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 1. 验证用户身份：检查 Token 是否存在且有效
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DateCountResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            // 查询会话表，确认 Token 属于哪个用户且未过期
            String userSql = "SELECT u.user_id FROM users u " +
                    "JOIN user_sessions us ON u.user_id = us.user_id " +
                    "WHERE us.access_token = ? AND us.expires_at > NOW()";

            List<Map<String, Object>> users = jdbcTemplate.queryForList(userSql, token);
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DateCountResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 获取当前用户的 ID
            int userId = (int) users.get(0).get("user_id");

            // 2. 执行数据库统计查询
            // 使用 DATE(created_at) 将精确的时间戳转换为日期（年月日）
            // 使用 COUNT(*) 统计每个日期出现的次数
            // GROUP BY 按日期分组，ORDER BY 按日期倒序排列（最近的日期在前）
            String sql = "SELECT DATE(created_at) as vocab_date, COUNT(*) as word_count " +
                         "FROM user_vocabulary " +
                         "WHERE user_id = ? " +
                         "GROUP BY DATE(created_at) " +
                         "ORDER BY vocab_date DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
            List<DateCount> dateCounts = new ArrayList<>();

            // 将数据库查询结果转换为 Java 对象列表
            for (Map<String, Object> row : rows) {
                dateCounts.add(new DateCount(
                    row.get("vocab_date").toString(),
                    ((Number) row.get("word_count")).intValue()
                ));
            }

            // 返回成功结果
            return ResponseEntity.ok(new DateCountResponse(true, "获取日期统计成功", dateCounts));

        } catch (Exception e) {
            // 捕获异常并返回 500 错误
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DateCountResponse(false, "服务器错误: " + e.getMessage(), null)
            );
        }
    }
}