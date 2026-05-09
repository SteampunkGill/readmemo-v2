package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderGetBookmarks {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取书签列表请求 ===");
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
    public static class BookmarksResponse {
        private boolean success;
        private String message;
        private BookmarksData data;

        public BookmarksResponse(boolean success, String message, BookmarksData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BookmarksData getData() { return data; }
        public void setData(BookmarksData data) { this.data = data; }
    }

    public static class BookmarksData {
        private List<BookmarkItem> bookmarks;
        private PaginationInfo pagination;

        public BookmarksData() {
            this.bookmarks = new ArrayList<>();
            this.pagination = new PaginationInfo();
        }

        public List<BookmarkItem> getBookmarks() { return bookmarks; }
        public void setBookmarks(List<BookmarkItem> bookmarks) { this.bookmarks = bookmarks; }

        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    }

    public static class BookmarkItem {
        private int id;
        private int documentId;
        private int page;
        private String title;
        private String createdAt;
        private DocumentInfo document;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

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

    @GetMapping("/documents/{documentId}/bookmarks")
    public ResponseEntity<BookmarksResponse> getBookmarks(
            @PathVariable("documentId") int documentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("page", page);
        requestInfo.put("pageSize", pageSize);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BookmarksResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BookmarksResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new BookmarksResponse(false, "没有权限查看此文档的书签", null)
                );
            }

            // 3. 获取书签总数
            String countSql = "SELECT COUNT(*) as total FROM reading_history " +
                    "WHERE user_id = ? AND document_id = ? AND is_bookmark = true";

            List<Map<String, Object>> counts = jdbcTemplate.queryForList(countSql, userId, documentId);
            int total = counts.isEmpty() ? 0 : ((Number) counts.get(0).get("total")).intValue();

            // 4. 计算分页
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int offset = (page - 1) * pageSize;

            // 5. 获取书签列表
            String querySql = "SELECT rh.*, d.title as document_title, d.author, d.language " +
                    "FROM reading_history rh " +
                    "JOIN documents d ON rh.document_id = d.document_id " +
                    "WHERE rh.user_id = ? AND rh.document_id = ? AND rh.is_bookmark = true " +
                    "ORDER BY rh.created_at DESC LIMIT ? OFFSET ?";

            List<Map<String, Object>> bookmarks = jdbcTemplate.queryForList(querySql, userId, documentId, pageSize, offset);
            printQueryResult(bookmarks);

            // 6. 构建响应数据
            BookmarksData bookmarksData = new BookmarksData();

            for (Map<String, Object> bookmark : bookmarks) {
                BookmarkItem item = new BookmarkItem();
                item.setId(((Number) bookmark.get("history_id")).intValue());
                item.setDocumentId(documentId);
                item.setPage(((Number) bookmark.get("page")).intValue());
                item.setTitle("第 " + bookmark.get("page") + " 页");
                item.setCreatedAt(bookmark.get("created_at").toString());

                // 设置文档信息
                DocumentInfo docInfo = new DocumentInfo();
                docInfo.setId(documentId);
                docInfo.setTitle((String) bookmark.get("document_title"));
                docInfo.setAuthor((String) bookmark.get("author"));
                docInfo.setLanguage((String) bookmark.get("language"));
                item.setDocument(docInfo);

                bookmarksData.getBookmarks().add(item);
            }

            // 设置分页信息
            PaginationInfo pagination = new PaginationInfo();
            pagination.setPage(page);
            pagination.setPageSize(pageSize);
            pagination.setTotal(total);
            pagination.setTotalPages(totalPages);
            bookmarksData.setPagination(pagination);

            BookmarksResponse response = new BookmarksResponse(true, "获取书签列表成功", bookmarksData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取书签列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BookmarksResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}