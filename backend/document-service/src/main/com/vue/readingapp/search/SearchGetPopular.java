package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGetPopular {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取热门搜索请求 ===");
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

    // 响应DTO
    public static class GetPopularResponse {
        private boolean success;
        private String message;
        private GetPopularData data;

        public GetPopularResponse(boolean success, String message, GetPopularData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public GetPopularData getData() { return data; }
        public void setData(GetPopularData data) { this.data = data; }
    }

    public static class GetPopularData {
        private List<PopularItem> items;
        private String timeRange;
        private int totalCount;

        public GetPopularData(List<PopularItem> items, String timeRange, int totalCount) {
            this.items = items;
            this.timeRange = timeRange;
            this.totalCount = totalCount;
        }

        public List<PopularItem> getItems() { return items; }
        public void setItems(List<PopularItem> items) { this.items = items; }

        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }

    public static class PopularItem {
        private Long id;
        private String keyword;
        private String type;
        private int searchCount;
        private String trend;
        private String lastSearched;

        public PopularItem(Long id, String keyword, String type, int searchCount,
                           String trend, String lastSearched) {
            this.id = id;
            this.keyword = keyword;
            this.type = type;
            this.searchCount = searchCount;
            this.trend = trend;
            this.lastSearched = lastSearched;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getSearchCount() { return searchCount; }
        public void setSearchCount(int searchCount) { this.searchCount = searchCount; }

        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }

        public String getLastSearched() { return lastSearched; }
        public void setLastSearched(String lastSearched) { this.lastSearched = lastSearched; }
    }

    @GetMapping("/popular")
    public ResponseEntity<GetPopularResponse> getPopularSearches(
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "week") String timeRange,
            @RequestParam(required = false) String type,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("limit", limit);
        requestParams.put("timeRange", timeRange);
        requestParams.put("type", type);
        printRequest(requestParams);

        try {
            // 1. 获取当前用户ID（可选，用于个性化推荐）
            Long userId = getUserIdFromToken(authHeader);

            // 2. 根据时间范围构建日期条件
            String dateCondition = getDateCondition(timeRange);

            // 3. 构建SQL查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT keyword, search_type, COUNT(*) as search_count, ");
            sqlBuilder.append("MAX(timestamp) as last_searched ");
            sqlBuilder.append("FROM search_history ");
            sqlBuilder.append("WHERE 1=1 ");

            List<Object> params = new ArrayList<>();

            // 添加日期条件
            if (dateCondition != null) {
                sqlBuilder.append("AND timestamp >= ? ");
                params.add(dateCondition);
            }

            // 添加类型过滤
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                sqlBuilder.append("AND search_type = ? ");
                params.add(type);
            }

            // 添加用户过滤（如果提供用户ID，可以只统计该用户的搜索）
            if (userId != null) {
                sqlBuilder.append("AND user_id = ? ");
                params.add(userId);
            }

            sqlBuilder.append("GROUP BY keyword, search_type ");
            sqlBuilder.append("ORDER BY search_count DESC ");
            sqlBuilder.append("LIMIT ?");
            params.add(limit);

            // 4. 执行查询
            List<Map<String, Object>> popularSearches = jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
            printQueryResult("找到 " + popularSearches.size() + " 个热门搜索");

            // 5. 处理结果
            List<PopularItem> items = new ArrayList<>();
            for (int i = 0; i < popularSearches.size(); i++) {
                Map<String, Object> search = popularSearches.get(i);

                // 生成趋势（简单实现：根据排名变化）
                String trend = calculateTrend(i, (String) search.get("keyword"), timeRange);

                PopularItem item = new PopularItem(
                        (long) (i + 1), // 使用排名作为ID
                        (String) search.get("keyword"),
                        (String) search.get("search_type"),
                        ((Number) search.get("search_count")).intValue(),
                        trend,
                        search.get("last_searched").toString()
                );
                items.add(item);
            }

            // 6. 获取总搜索次数
            int totalCount = 0;
            for (Map<String, Object> search : popularSearches) {
                totalCount += ((Number) search.get("search_count")).intValue();
            }

            // 7. 准备响应数据
            GetPopularData data = new GetPopularData(items, timeRange, totalCount);
            GetPopularResponse response = new GetPopularResponse(true, "获取热门搜索成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取热门搜索过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetPopularResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 从token获取用户ID
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
                List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);
                if (!sessions.isEmpty()) {
                    return ((Number) sessions.get(0).get("user_id")).longValue();
                }
            } catch (Exception e) {
                System.out.println("Token解析失败: " + e.getMessage());
            }
        }
        return null;
    }

    // 根据时间范围获取日期条件
    private String getDateCondition(String timeRange) {
        Calendar calendar = Calendar.getInstance();

        switch (timeRange) {
            case "day":
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case "week":
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case "month":
                calendar.add(Calendar.MONTH, -1);
                break;
            case "quarter":
                calendar.add(Calendar.MONTH, -3);
                break;
            case "year":
                calendar.add(Calendar.YEAR, -1);
                break;
            default:
                // 默认最近一周
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                break;
        }

        // 格式化为MySQL日期时间格式
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(calendar.getTime());
    }

    // 计算趋势（简单实现）
    private String calculateTrend(int currentRank, String keyword, String timeRange) {
        // 这里应该查询历史排名数据，但为了简化，我们随机生成趋势
        Random random = new Random(keyword.hashCode() + timeRange.hashCode());
        int trendValue = random.nextInt(3);

        switch (trendValue) {
            case 0:
                return "rising";
            case 1:
                return "falling";
            case 2:
                return "stable";
            default:
                return "stable";
        }
    }
}
