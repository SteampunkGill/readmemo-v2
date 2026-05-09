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
public class ReaderAddBookmark {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到添加书签请求 ===");
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
    public static class BookmarkRequest {
        private int pageNumber;

        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    }

    // 响应DTO
    public static class BookmarkResponse {
        private boolean success;
        private String message;
        private BookmarkData data;

        public BookmarkResponse(boolean success, String message, BookmarkData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public BookmarkData getData() { return data; }
        public void setData(BookmarkData data) { this.data = data; }
    }

    public static class BookmarkData {
        private int id;
        private int documentId;
        private int page;
        private String title;
        private String createdAt;

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
    }

    @PostMapping("/documents/{documentId}/bookmarks")
    public ResponseEntity<BookmarkResponse> addBookmark(
            @PathVariable("documentId") int documentId,
            @RequestBody BookmarkRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("request", request);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BookmarkResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new BookmarkResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new BookmarkResponse(false, "没有权限在此文档添加书签", null)
                );
            }

            // 3. 验证请求数据
            if (request.getPageNumber() < 1) {
                return ResponseEntity.badRequest().body(
                        new BookmarkResponse(false, "页码必须大于0", null)
                );
            }

            // 4. 检查页面是否存在
            String pageSql = "SELECT COUNT(*) as page_count FROM document_pages WHERE document_id = ? AND page_number = ?";
            List<Map<String, Object>> pageCounts = jdbcTemplate.queryForList(pageSql, documentId, request.getPageNumber());
            int pageCount = pageCounts.isEmpty() ? 0 : ((Number) pageCounts.get(0).get("page_count")).intValue();

            if (pageCount == 0) {
                return ResponseEntity.badRequest().body(
                        new BookmarkResponse(false, "指定的页面不存在", null)
                );
            }

            // 5. 检查是否已存在书签
            String checkSql = "SELECT COUNT(*) as bookmark_count FROM reading_history " +
                    "WHERE user_id = ? AND document_id = ? AND page = ? AND is_bookmark = true";

            List<Map<String, Object>> bookmarkCounts = jdbcTemplate.queryForList(checkSql, userId, documentId, request.getPageNumber());
            int existingBookmarks = bookmarkCounts.isEmpty() ? 0 : ((Number) bookmarkCounts.get(0).get("bookmark_count")).intValue();

            if (existingBookmarks > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new BookmarkResponse(false, "该页已有书签", null)
                );
            }

            // 6. 插入书签记录（使用reading_history表）
            String insertSql = "INSERT INTO reading_history (user_id, document_id, page, is_bookmark, created_at) " +
                    "VALUES (?, ?, ?, true, NOW())";

            jdbcTemplate.update(insertSql, userId, documentId, request.getPageNumber());

            // 7. 获取刚插入的书签ID
            String lastIdSql = "SELECT LAST_INSERT_ID() as history_id";
            List<Map<String, Object>> lastIds = jdbcTemplate.queryForList(lastIdSql);
            int bookmarkId = lastIds.isEmpty() ? 0 : ((Number) lastIds.get(0).get("history_id")).intValue();

            // 8. 获取完整的书签数据
            String bookmarkSql = "SELECT * FROM reading_history WHERE history_id = ?";
            List<Map<String, Object>> bookmarks = jdbcTemplate.queryForList(bookmarkSql, bookmarkId);
            printQueryResult(bookmarks);

            if (bookmarks.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new BookmarkResponse(false, "创建书签失败", null)
                );
            }

            Map<String, Object> bookmark = bookmarks.get(0);

            // 9. 构建响应数据
            BookmarkData bookmarkData = new BookmarkData();
            bookmarkData.setId(bookmarkId);
            bookmarkData.setDocumentId(documentId);
            bookmarkData.setPage(request.getPageNumber());
            bookmarkData.setTitle("第 " + request.getPageNumber() + " 页");
            bookmarkData.setCreatedAt(bookmark.get("created_at").toString());

            BookmarkResponse response = new BookmarkResponse(true, "书签已添加", bookmarkData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("添加书签过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BookmarkResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}