package com.vue.readingapp.reader;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderGetDocumentOutline {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到获取文档目录请求 ===");
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
    public static class OutlineResponse {
        private boolean success;
        private String message;
        private OutlineData data;

        public OutlineResponse(boolean success, String message, OutlineData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public OutlineData getData() { return data; }
        public void setData(OutlineData data) { this.data = data; }
    }

    public static class OutlineData {
        private List<OutlineItem> outline;

        public OutlineData() {
            this.outline = new ArrayList<>();
        }

        public List<OutlineItem> getOutline() { return outline; }
        public void setOutline(List<OutlineItem> outline) { this.outline = outline; }
    }

    public static class OutlineItem {
        private int level;
        private String title;
        private int page;
        private List<OutlineItem> children;

        public OutlineItem() {
            this.children = new ArrayList<>();
        }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public List<OutlineItem> getChildren() { return children; }
        public void setChildren(List<OutlineItem> children) { this.children = children; }
    }

    @GetMapping("/documents/{documentId}/outline")
    public ResponseEntity<OutlineResponse> getDocumentOutline(
            @PathVariable("documentId") int documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("documentId", documentId);
        requestInfo.put("authHeader", authHeader != null ? "provided" : "missing");
        printRequest(requestInfo);

        try {
            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OutlineResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(tokenSql, token);

            if (sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new OutlineResponse(false, "登录已过期，请重新登录", null)
                );
            }

            int userId = (int) sessions.get(0).get("user_id");

            // 2. 验证文档权限
            String docSql = "SELECT * FROM documents WHERE document_id = ? AND (user_id = ? OR is_public = true)";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(docSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        new OutlineResponse(false, "没有权限查看此文档的目录", null)
                );
            }

            // 3. 获取文档的目录信息
            // 在实际项目中，目录信息可能存储在单独的表中，或者从文档内容中解析
            // 这里我们假设有一个document_outlines表，或者从document_pages表中提取标题

            // 方法1：如果存在document_outlines表
            String outlineSql = "SELECT * FROM document_outlines WHERE document_id = ? ORDER BY order_index";
            List<Map<String, Object>> outlines = new ArrayList<>();

            try {
                outlines = jdbcTemplate.queryForList(outlineSql, documentId);
            } catch (Exception e) {
                System.out.println("document_outlines表不存在，尝试从document_pages提取");
                // 方法2：从document_pages表中提取第一行作为标题
                String pageSql = "SELECT page_number, SUBSTRING(content, 1, 100) as preview FROM document_pages WHERE document_id = ? ORDER BY page_number";
                outlines = jdbcTemplate.queryForList(pageSql, documentId);
            }

            printQueryResult(outlines);

            // 4. 构建目录结构
            List<OutlineItem> outlineItems = new ArrayList<>();

            for (Map<String, Object> outline : outlines) {
                OutlineItem item = new OutlineItem();

                // 设置级别（默认为1）
                item.setLevel(outline.get("level") != null ?
                        ((Number) outline.get("level")).intValue() : 1);

                // 设置标题
                if (outline.get("title") != null) {
                    item.setTitle((String) outline.get("title"));
                } else if (outline.get("preview") != null) {
                    String preview = (String) outline.get("preview");
                    // 取前30个字符作为标题
                    item.setTitle(preview.length() > 30 ? preview.substring(0, 30) + "..." : preview);
                } else {
                    item.setTitle("未命名章节");
                }

                // 设置页码
                if (outline.get("page_number") != null) {
                    item.setPage(((Number) outline.get("page_number")).intValue());
                } else if (outline.get("page") != null) {
                    item.setPage(((Number) outline.get("page")).intValue());
                } else {
                    item.setPage(1);
                }

                outlineItems.add(item);
            }

            // 5. 构建层次结构（这里简化处理，实际应该根据level字段构建树形结构）
            // 假设只有一级标题
            List<OutlineItem> rootItems = new ArrayList<>();
            for (OutlineItem item : outlineItems) {
                if (item.getLevel() == 1) {
                    rootItems.add(item);
                } else {
                    // 如果不是一级标题，添加到最后一个一级标题的子项中
                    if (!rootItems.isEmpty()) {
                        OutlineItem lastRoot = rootItems.get(rootItems.size() - 1);
                        lastRoot.getChildren().add(item);
                    } else {
                        // 如果没有一级标题，将其作为一级标题
                        item.setLevel(1);
                        rootItems.add(item);
                    }
                }
            }

            // 6. 构建响应数据
            OutlineData outlineData = new OutlineData();
            outlineData.setOutline(rootItems);

            OutlineResponse response = new OutlineResponse(true, "获取文档目录成功", outlineData);
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("获取文档目录过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new OutlineResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}
