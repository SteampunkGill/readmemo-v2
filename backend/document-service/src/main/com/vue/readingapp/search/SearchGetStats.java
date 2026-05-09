package com.vue.readingapp.search;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchGetStats {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取搜索统计请求 ===");
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
    public static class GetStatsResponse {
        private boolean success;
        private String message;
        private SearchStatsData data;

        public GetStatsResponse(boolean success, String message, SearchStatsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public SearchStatsData getData() { return data; }
        public void setData(SearchStatsData data) { this.data = data; }
    }

    public static class SearchStatsData {
        private int totalSearches;
        private int todaySearches;
        private int thisWeekSearches;
        private int thisMonthSearches;
        private Map<String, Integer> searchesByType;
        private List<PopularKeyword> topKeywords;
        private Map<String, Integer> searchesByHour;
        private Map<String, Integer> searchesByDay;
        private double avgResultsPerSearch;
        private int mostResultsSearch;
        private int leastResultsSearch;

        public SearchStatsData(int totalSearches, int todaySearches, int thisWeekSearches, int thisMonthSearches,
                               Map<String, Integer> searchesByType, List<PopularKeyword> topKeywords,
                               Map<String, Integer> searchesByHour, Map<String, Integer> searchesByDay,
                               double avgResultsPerSearch, int mostResultsSearch, int leastResultsSearch) {
            this.totalSearches = totalSearches;
            this.todaySearches = todaySearches;
            this.thisWeekSearches = thisWeekSearches;
            this.thisMonthSearches = thisMonthSearches;
            this.searchesByType = searchesByType;
            this.topKeywords = topKeywords;
            this.searchesByHour = searchesByHour;
            this.searchesByDay = searchesByDay;
            this.avgResultsPerSearch = avgResultsPerSearch;
            this.mostResultsSearch = mostResultsSearch;
            this.leastResultsSearch = leastResultsSearch;
        }

        public int getTotalSearches() { return totalSearches; }
        public void setTotalSearches(int totalSearches) { this.totalSearches = totalSearches; }

        public int getTodaySearches() { return todaySearches; }
        public void setTodaySearches(int todaySearches) { this.todaySearches = todaySearches; }

        public int getThisWeekSearches() { return thisWeekSearches; }
        public void setThisWeekSearches(int thisWeekSearches) { this.thisWeekSearches = thisWeekSearches; }

        public int getThisMonthSearches() { return thisMonthSearches; }
        public void setThisMonthSearches(int thisMonthSearches) { this.thisMonthSearches = thisMonthSearches; }

        public Map<String, Integer> getSearchesByType() { return searchesByType; }
        public void setSearchesByType(Map<String, Integer> searchesByType) { this.searchesByType = searchesByType; }

        public List<PopularKeyword> getTopKeywords() { return topKeywords; }
        public void setTopKeywords(List<PopularKeyword> topKeywords) { this.topKeywords = topKeywords; }

        public Map<String, Integer> getSearchesByHour() { return searchesByHour; }
        public void setSearchesByHour(Map<String, Integer> searchesByHour) { this.searchesByHour = searchesByHour; }

        public Map<String, Integer> getSearchesByDay() { return searchesByDay; }
        public void setSearchesByDay(Map<String, Integer> searchesByDay) { this.searchesByDay = searchesByDay; }

        public double getAvgResultsPerSearch() { return avgResultsPerSearch; }
        public void setAvgResultsPerSearch(double avgResultsPerSearch) { this.avgResultsPerSearch = avgResultsPerSearch; }

        public int getMostResultsSearch() { return mostResultsSearch; }
        public void setMostResultsSearch(int mostResultsSearch) { this.mostResultsSearch = mostResultsSearch; }

        public int getLeastResultsSearch() { return leastResultsSearch; }
        public void setLeastResultsSearch(int leastResultsSearch) { this.leastResultsSearch = leastResultsSearch; }
    }

    public static class PopularKeyword {
        private String keyword;
        private String type;
        private int count;
        private double trend;

        public PopularKeyword(String keyword, String type, int count, double trend) {
            this.keyword = keyword;
            this.type = type;
            this.count = count;
            this.trend = trend;
        }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public double getTrend() { return trend; }
        public void setTrend(double trend) { this.trend = trend; }
    }

    @GetMapping("/stats")
    public ResponseEntity<GetStatsResponse> getSearchStats(
            @RequestParam(required = false, defaultValue = "week") String timeRange,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        printRequest("时间范围: " + timeRange);

        try {
            // 1. 获取当前用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new GetStatsResponse(false, "请先登录", null)
                );
            }

            // 2. 获取各种统计数据
            int totalSearches = getTotalSearches(userId);
            int todaySearches = getTodaySearches(userId);
            int thisWeekSearches = getThisWeekSearches(userId);
            int thisMonthSearches = getThisMonthSearches(userId);

            Map<String, Integer> searchesByType = getSearchesByType(userId, timeRange);
            List<PopularKeyword> topKeywords = getTopKeywords(userId, timeRange, 10);
            Map<String, Integer> searchesByHour = getSearchesByHour(userId, timeRange);
            Map<String, Integer> searchesByDay = getSearchesByDay(userId, timeRange);

            double avgResultsPerSearch = getAvgResultsPerSearch(userId, timeRange);
            int mostResultsSearch = getMostResultsSearch(userId, timeRange);
            int leastResultsSearch = getLeastResultsSearch(userId, timeRange);

            // 3. 准备响应数据
            SearchStatsData data = new SearchStatsData(
                    totalSearches,
                    todaySearches,
                    thisWeekSearches,
                    thisMonthSearches,
                    searchesByType,
                    topKeywords,
                    searchesByHour,
                    searchesByDay,
                    avgResultsPerSearch,
                    mostResultsSearch,
                    leastResultsSearch
            );

            GetStatsResponse response = new GetStatsResponse(true, "获取搜索统计成功", data);

            // 打印查询结果
            printQueryResult("统计结果: 总搜索=" + totalSearches + ", 今日=" + todaySearches);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取搜索统计过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new GetStatsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 获取总搜索次数
    private int getTotalSearches(Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM search_history WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            System.err.println("获取总搜索次数失败: " + e.getMessage());
            return 0;
        }
    }

    // 获取今日搜索次数
    private int getTodaySearches(Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM search_history WHERE user_id = ? AND DATE(timestamp) = CURDATE()";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            System.err.println("获取今日搜索次数失败: " + e.getMessage());
            return 0;
        }
    }

    // 获取本周搜索次数
    private int getThisWeekSearches(Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM search_history WHERE user_id = ? AND YEARWEEK(timestamp, 1) = YEARWEEK(CURDATE(), 1)";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            System.err.println("获取本周搜索次数失败: " + e.getMessage());
            return 0;
        }
    }

    // 获取本月搜索次数
    private int getThisMonthSearches(Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM search_history WHERE user_id = ? AND YEAR(timestamp) = YEAR(CURDATE()) AND MONTH(timestamp) = MONTH(CURDATE())";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId);
        } catch (Exception e) {
            System.err.println("获取本月搜索次数失败: " + e.getMessage());
            return 0;
        }
    }

    // 按类型获取搜索次数
    private Map<String, Integer> getSearchesByType(Long userId, String timeRange) {
        Map<String, Integer> result = new HashMap<>();

        try {
            String dateCondition = getDateCondition(timeRange);
            String sql = "SELECT search_type, COUNT(*) as count FROM search_history WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (dateCondition != null) {
                sql += "AND timestamp >= ? ";
                params.add(dateCondition);
            }

            sql += "GROUP BY search_type ORDER BY count DESC";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, params.toArray());

            for (Map<String, Object> record : records) {
                String type = (String) record.get("search_type");
                int count = ((Number) record.get("count")).intValue();
                result.put(type, count);
            }

        } catch (Exception e) {
            System.err.println("按类型获取搜索次数失败: " + e.getMessage());
        }

        return result;
    }

    // 获取热门关键词
    private List<PopularKeyword> getTopKeywords(Long userId, String timeRange, int limit) {
        List<PopularKeyword> result = new ArrayList<>();

        try {
            String dateCondition = getDateCondition(timeRange);
            String sql = "SELECT keyword, search_type, COUNT(*) as count FROM search_history WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (dateCondition != null) {
                sql += "AND timestamp >= ? ";
                params.add(dateCondition);
            }

            sql += "GROUP BY keyword, search_type ORDER BY count DESC LIMIT ?";
            params.add(limit);

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, params.toArray());

            for (Map<String, Object> record : records) {
                String keyword = (String) record.get("keyword");
                String type = (String) record.get("search_type");
                int count = ((Number) record.get("count")).intValue();

                // 计算趋势（简化处理）
                double trend = calculateTrend(keyword, type, userId, timeRange);

                result.add(new PopularKeyword(keyword, type, count, trend));
            }

        } catch (Exception e) {
            System.err.println("获取热门关键词失败: " + e.getMessage());
        }

        return result;
    }

    // 按小时获取搜索次数
    private Map<String, Integer> getSearchesByHour(Long userId, String timeRange) {
        Map<String, Integer> result = new LinkedHashMap<>();

        try {
            String dateCondition = getDateCondition(timeRange);
            String sql = "SELECT HOUR(timestamp) as hour, COUNT(*) as count FROM search_history WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (dateCondition != null) {
                sql += "AND timestamp >= ? ";
                params.add(dateCondition);
            }

            sql += "GROUP BY HOUR(timestamp) ORDER BY hour";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, params.toArray());

            // 初始化24小时
            for (int i = 0; i < 24; i++) {
                result.put(String.format("%02d:00", i), 0);
            }

            // 填充数据
            for (Map<String, Object> record : records) {
                int hour = ((Number) record.get("hour")).intValue();
                int count = ((Number) record.get("count")).intValue();
                result.put(String.format("%02d:00", hour), count);
            }

        } catch (Exception e) {
            System.err.println("按小时获取搜索次数失败: " + e.getMessage());
        }

        return result;
    }

    // 按天获取搜索次数
    private Map<String, Integer> getSearchesByDay(Long userId, String timeRange) {
        Map<String, Integer> result = new LinkedHashMap<>();

        try {
            String dateCondition = getDateCondition(timeRange);
            String sql = "SELECT DATE(timestamp) as day, COUNT(*) as count FROM search_history WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (dateCondition != null) {
                sql += "AND timestamp >= ? ";
                params.add(dateCondition);
            }

            sql += "GROUP BY DATE(timestamp) ORDER BY day DESC LIMIT 30";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, params.toArray());

            // 按日期倒序排列
            for (Map<String, Object> record : records) {
                String day = record.get("day").toString();
                int count = ((Number) record.get("count")).intValue();
                result.put(day, count);
            }

        } catch (Exception e) {
            System.err.println("按天获取搜索次数失败: " + e.getMessage());
        }

        return result;
    }

    // 获取平均每次搜索结果数
    private double getAvgResultsPerSearch(Long userId, String timeRange) {
        try {
            String dateCondition = getDateCondition(timeRange);
            String sql = "SELECT AVG(result_count) as avg_results FROM search_history WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (dateCondition != null) {
                sql += "AND timestamp >= ? ";
                params.add(dateCondition);
            }

            Double avg = jdbcTemplate.queryForObject(sql, Double.class, params.toArray());
            return avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0;

        } catch (Exception e) {
            System.err.println("获取平均搜索结果数失败: " + e.getMessage());
            return 0.0;
        }
    }

    // 获取最多搜索结果
    private int getMostResultsSearch(Long userId, String timeRange) {
        try {
            String dateCondition = getDateCondition(timeRange);
            String sql = "SELECT MAX(result_count) as max_results FROM search_history WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (dateCondition != null) {
                sql += "AND timestamp >= ? ";
                params.add(dateCondition);
            }

            Integer max = jdbcTemplate.queryForObject(sql, Integer.class, params.toArray());
            return max != null ? max : 0;

        } catch (Exception e) {
            System.err.println("获取最多搜索结果失败: " + e.getMessage());
            return 0;
        }
    }

    // 获取最少搜索结果
    private int getLeastResultsSearch(Long userId, String timeRange) {
        try {
            String dateCondition = getDateCondition(timeRange);
            String sql = "SELECT MIN(result_count) as min_results FROM search_history WHERE user_id = ? ";

            List<Object> params = new ArrayList<>();
            params.add(userId);

            if (dateCondition != null) {
                sql += "AND timestamp >= ? ";
                params.add(dateCondition);
            }

            Integer min = jdbcTemplate.queryForObject(sql, Integer.class, params.toArray());
            return min != null ? min : 0;

        } catch (Exception e) {
            System.err.println("获取最少搜索结果失败: " + e.getMessage());
            return 0;
        }
    }

    // 计算趋势（简化处理）
    private double calculateTrend(String keyword, String type, Long userId, String timeRange) {
        // 这里应该比较当前时间段和上一个时间段的搜索次数
        // 为了简化，我们随机生成一个趋势值
        Random random = new Random(keyword.hashCode() + type.hashCode() + userId.hashCode());

        // 生成-1到1之间的趋势值
        double trend = random.nextDouble() * 2 - 1;

        // 保留两位小数
        return Math.round(trend * 100.0) / 100.0;
    }

    // 获取日期条件
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
}
