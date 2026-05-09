package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderGetReadingHistory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取阅读历史请求 ===");
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
    public static class ReadingHistoryResponse {
        private boolean success;
        private String message;
        private ReadingHistoryData data;

        public ReadingHistoryResponse(boolean success, String message, ReadingHistoryData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ReadingHistoryData getData() { return data; }
        public void setData(ReadingHistoryData data) { this.data = data; }
    }

    public static class ReadingHistoryData {
        private List<HistoryItem> history;
        private PaginationInfo pagination;

        public ReadingHistoryData() {
            this.history = new ArrayList<>();
            this.pagination = new PaginationInfo();
        }

        public List<HistoryItem> getHistory() { return history; }
        public void setHistory(List<HistoryItem> history) { this.history = history; }

        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    }

    public static class HistoryItem {
        private int id;
        private int documentId;
        private int page;
        private int readingTime;
        private String date;
        private DocumentInfo document;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getReadingTime() { return readingTime; }
        public void setReadingTime(int readingTime) { this.readingTime = readingTime; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public DocumentInfo getDocument() { return document; }
        public void setDocument(DocumentInfo document) { this.document = document; }
    }

    public static class DocumentInfo {
        private int id;
        private String title;
        private String author;
        private String language;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
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

    @GetMapping("/documents/{documentId}/reading-history")
    public ResponseEntity<ReadingHistoryResponse> getReadingHistory(
            @PathVariable("documentId") int documentId,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("limit", limit);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReadingHistoryResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ReadingHistoryResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new ReadingHistoryResponse(false, "没有权限查看此文档的阅读历史", null)
                );
            }

            // 3. 获取阅读历史记录
            String historySql = "SELECT * FROM reading_history " +
                    "WHERE user_id = ? AND document_id = ? " +
                    "ORDER BY created_at DESC LIMIT ?";

            List<Map<String, Object>> historyList = jdbcTemplate.queryForList(historySql, userId, documentId, limit);
            printQueryResult(historyList);

            // 4. 构建响应数据
            ReadingHistoryData historyData = new ReadingHistoryData();

            for (Map<String, Object> history : historyList) {
                HistoryItem item = new HistoryItem();
                item.setId(((Number) history.get("history_id")).intValue());
                item.setDocumentId(documentId);
                item.setPage(((Number) history.get("page")).intValue());
                item.setReadingTime(history.get("reading_time") != null ?
                        ((Number) history.get("reading_time")).intValue() : 0);
                item.setDate(history.get("created_at").toString());

                // 设置文档信息
                Map<String, Object> document = documents.get(0);
                DocumentInfo docInfo = new DocumentInfo();
                docInfo.setId(documentId);
                docInfo.setTitle((String) document.get("title"));
                docInfo.setAuthor((String) document.get("author"));
                docInfo.setLanguage((String) document.get("language"));
                item.setDocument(docInfo);

                historyData.getHistory().add(item);
            }

            // 设置分页信息（这里limit就是pageSize，我们只有一页）
            PaginationInfo pagination = new PaginationInfo();
            pagination.setPage(1);
            pagination.setPageSize(limit);
            pagination.setTotal(historyList.size());
            pagination.setTotalPages(1);
            historyData.setPagination(pagination);

            ReadingHistoryResponse response = new ReadingHistoryResponse(true, "获取阅读历史成功", historyData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取阅读历史过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ReadingHistoryResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}