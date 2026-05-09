package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderGetHighlights {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取高亮列表请求 ===");
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
    public static class HighlightsResponse {
        private boolean success;
        private String message;
        private HighlightsData data;

        public HighlightsResponse(boolean success, String message, HighlightsData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public HighlightsData getData() { return data; }
        public void setData(HighlightsData data) { this.data = data; }
    }

    public static class HighlightsData {
        private List<HighlightItem> highlights;
        private PaginationInfo pagination;

        public HighlightsData() {
            this.highlights = new ArrayList<>();
            this.pagination = new PaginationInfo();
        }

        public List<HighlightItem> getHighlights() { return highlights; }
        public void setHighlights(List<HighlightItem> highlights) { this.highlights = highlights; }

        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    }

    public static class HighlightItem {
        private int id;
        private int documentId;
        private String text;
        private int page;
        private Map<String, Object> position;
        private String color;
        private String note;
        private String createdAt;
        private String updatedAt;

        public HighlightItem() {
            this.position = new HashMap<>();
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class PaginationInfo {
        private int page;
        private int pageSize;
        private int total;
        private int totalPages;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }

    @GetMapping("/documents/{documentId}/highlights")
    public ResponseEntity<HighlightsResponse> getHighlights(
            @PathVariable("documentId") int documentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "pageNumber", required = false) Integer filterPage,
            @RequestParam(value = "color", required = false) String filterColor,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("page", page);
        requestInfo.put("pageSize", pageSize);
        requestInfo.put("filterPage", filterPage);
        requestInfo.put("filterColor", filterColor);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new HighlightsResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new HighlightsResponse(false, "登录已过期或无效，请重新登录", null)
                );
            }

            int userId = ((Number) sessions.get(0).get("user_id")).intValue();

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new HighlightsResponse(false, "没有权限查看此文档的高亮", null)
                );
            }

            // 3. 构建查询条件和参数
            StringBuilder whereClause = new StringBuilder("WHERE document_id = ? AND user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(documentId);
            params.add(userId);

            if (filterPage != null) {
                whereClause.append(" AND page = ?");
                params.add(filterPage);
            }

            if (filterColor != null && !filterColor.isEmpty()) {
                whereClause.append(" AND color = ?");
                params.add(filterColor);
            }

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) as total FROM document_highlights " + whereClause;
            List<Map<String, Object>> counts = jdbcTemplate.queryForList(countSql, params.toArray());
            int total = counts.isEmpty() ? 0 : ((Number) counts.get(0).get("total")).intValue();

            // 5. 计算分页
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int offset = (page - 1) * pageSize;

            // 6. 获取高亮列表
            String querySql = "SELECT * FROM document_highlights " + whereClause +
                    " ORDER BY created_at DESC LIMIT ? OFFSET ?";

            // 添加分页参数
            List<Object> queryParams = new ArrayList<>(params);
            queryParams.add(pageSize);
            queryParams.add(offset);

            List<Map<String, Object>> highlights = jdbcTemplate.queryForList(querySql, queryParams.toArray());
            printQueryResult(highlights);

            // 7. 构建响应数据
            HighlightsData highlightsData = new HighlightsData();

            for (Map<String, Object> highlight : highlights) {
                HighlightItem item = new HighlightItem();
                item.setId(((Number) highlight.get("highlight_id")).intValue());
                item.setDocumentId(((Number) highlight.get("document_id")).intValue());
                item.setText((String) highlight.get("text"));
                item.setPage(((Number) highlight.get("page")).intValue());
                item.setColor((String) highlight.get("color"));
                item.setNote((String) highlight.get("note"));
                item.setCreatedAt(highlight.get("created_at").toString());
                item.setUpdatedAt(highlight.get("updated_at").toString());

                // 解析position JSON
                if (highlight.get("position") != null) {
                    try {
                        String posStr = (String) highlight.get("position");
                        if (posStr.startsWith("{") && posStr.endsWith("}")) {
                            Map<String, Object> posMap = new HashMap<>();
                            posMap.put("x", 0);
                            posMap.put("y", 0);
                            posMap.put("width", 100);
                            posMap.put("height", 20);
                            item.setPosition(posMap);
                        }
                    } catch (Exception e) {
                        System.err.println("解析position JSON失败: " + e.getMessage());
                    }
                }

                highlightsData.getHighlights().add(item);
            }

            // 设置分页信息
            PaginationInfo pagination = new PaginationInfo();
            pagination.setPage(page);
            pagination.setPageSize(pageSize);
            pagination.setTotal(total);
            pagination.setTotalPages(totalPages);
            highlightsData.setPagination(pagination);

            HighlightsResponse response = new HighlightsResponse(true, "获取高亮列表成功", highlightsData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取高亮列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new HighlightsResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
