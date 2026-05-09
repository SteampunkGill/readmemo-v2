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
public class ReaderGetNotes {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取笔记列表请求 ===");
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
    public static class NotesResponse {
        private boolean success;
        private String message;
        private NotesData data;

        public NotesResponse(boolean success, String message, NotesData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public NotesData getData() { return data; }
        public void setData(NotesData data) { this.data = data; }
    }

    public static class NotesData {
        private List<NoteItem> notes;
        private PaginationInfo pagination;

        public NotesData() {
            this.notes = new ArrayList<>();
            this.pagination = new PaginationInfo();
        }

        public List<NoteItem> getNotes() { return notes; }
        public void setNotes(List<NoteItem> notes) { this.notes = notes; }

        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    }

    public static class NoteItem {
        private int id;
        private int documentId;
        private String content;
        private int page;
        private Map<String, Object> position;
        private Integer highlightId;
        private String createdAt;
        private String updatedAt;
        private HighlightInfo highlight;

        public NoteItem() {
            this.position = new HashMap<>();
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getDocumentId() { return documentId; }
        public void setDocumentId(int documentId) { this.documentId = documentId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public Map<String, Object> getPosition() { return position; }
        public void setPosition(Map<String, Object> position) { this.position = position; }

        public Integer getHighlightId() { return highlightId; }
        public void setHighlightId(Integer highlightId) { this.highlightId = highlightId; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public HighlightInfo getHighlight() { return highlight; }
        public void setHighlight(HighlightInfo highlight) { this.highlight = highlight; }
    }

    public static class HighlightInfo {
        private int id;
        private String text;
        private String color;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
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

    @GetMapping("/documents/{documentId}/notes")
    public ResponseEntity<NotesResponse> getNotes(
            @PathVariable("documentId") int documentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "pageNumber", required = false) Integer filterPage,
            @RequestParam(value = "highlightId", required = false) Integer filterHighlightId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("page", page);
        requestInfo.put("pageSize", pageSize);
        requestInfo.put("filterPage", filterPage);
        requestInfo.put("filterHighlightId", filterHighlightId);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NotesResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new NotesResponse(false, "登录已过期或无效，请重新登录", null)
                );
            }

            int userId = ((Number) sessions.get(0).get("user_id")).intValue();

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new NotesResponse(false, "没有权限查看此文档的笔记", null)
                );
            }

            // 3. 构建查询条件和参数
            StringBuilder whereClause = new StringBuilder("WHERE dn.document_id = ? AND dn.user_id = ?");
            List<Object> params = new ArrayList<>();
            params.add(documentId);
            params.add(userId);

            if (filterPage != null) {
                whereClause.append(" AND dn.page = ?");
                params.add(filterPage);
            }

            if (filterHighlightId != null) {
                whereClause.append(" AND dn.highlight_id = ?");
                params.add(filterHighlightId);
            }

            // 4. 获取总数
            String countSql = "SELECT COUNT(*) as total FROM document_notes dn " + whereClause;
            List<Map<String, Object>> counts = jdbcTemplate.queryForList(countSql, params.toArray());
            int total = counts.isEmpty() ? 0 : ((Number) counts.get(0).get("total")).intValue();

            // 5. 计算分页
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int offset = (page - 1) * pageSize;

            // 6. 获取笔记列表（包含关联的高亮信息）
            String querySql = "SELECT dn.*, dh.text as highlight_text, dh.color as highlight_color " +
                    "FROM document_notes dn " +
                    "LEFT JOIN document_highlights dh ON dn.highlight_id = dh.highlight_id AND dh.user_id = ? " +
                    whereClause + " ORDER BY dn.created_at DESC LIMIT ? OFFSET ?";

            // 添加分页参数
            List<Object> queryParams = new ArrayList<>();
            queryParams.add(userId);
            queryParams.addAll(params);
            queryParams.add(pageSize);
            queryParams.add(offset);

            List<Map<String, Object>> notes = jdbcTemplate.queryForList(querySql, queryParams.toArray());
            printQueryResult(notes);

            // 7. 构建响应数据
            NotesData notesData = new NotesData();

            for (Map<String, Object> note : notes) {
                NoteItem item = new NoteItem();
                item.setId(((Number) note.get("note_id")).intValue());
                item.setDocumentId(documentId);
                item.setContent((String) note.get("content"));
                item.setPage(((Number) note.get("page")).intValue());

                // 设置高亮ID
                if (note.get("highlight_id") != null) {
                    item.setHighlightId(((Number) note.get("highlight_id")).intValue());

                    // 设置高亮信息
                    HighlightInfo highlightInfo = new HighlightInfo();
                    highlightInfo.setId(((Number) note.get("highlight_id")).intValue());
                    highlightInfo.setText((String) note.get("highlight_text"));
                    highlightInfo.setColor((String) note.get("highlight_color"));
                    item.setHighlight(highlightInfo);
                }

                item.setCreatedAt(note.get("created_at").toString());
                item.setUpdatedAt(note.get("updated_at").toString());

                // 解析position JSON
                if (note.get("position") != null) {
                    try {
                        String posStr = (String) note.get("position");
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

                notesData.getNotes().add(item);
            }

            // 设置分页信息
            PaginationInfo pagination = new PaginationInfo();
            pagination.setPage(page);
            pagination.setPageSize(pageSize);
            pagination.setTotal(total);
            pagination.setTotalPages(totalPages);
            notesData.setPagination(pagination);

            NotesResponse response = new NotesResponse(true, "获取笔记列表成功", notesData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取笔记列表过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new NotesResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}