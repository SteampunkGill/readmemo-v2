
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineDeleteDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到删除离线文档请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("==========================");
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
    public static class DeleteDocumentResponse {
        private boolean success;
        private String message;

        public DeleteDocumentResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // 验证用户token
    private Integer validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String sql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql, token);

            if (sessions.isEmpty()) {
                return null;
            }

            return (Integer) sessions.get(0).get("user_id");
        } catch (Exception e) {
            System.err.println("验证token失败: " + e.getMessage());
            return null;
        }
    }

    @DeleteMapping("/documents/{offlineDocId}")
    public ResponseEntity<DeleteDocumentResponse> deleteOfflineDocument(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String offlineDocId) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("offlineDocId", offlineDocId);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteDocumentResponse(false, "请先登录")
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new DeleteDocumentResponse(false, "登录已过期，请重新登录")
                );
            }

            // 2. 检查离线文档是否存在且属于该用户
            String checkSql = "SELECT offline_doc_id FROM offline_documents WHERE offline_doc_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineDocs = jdbcTemplate.queryForList(checkSql, offlineDocId, userId);

            if (offlineDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new DeleteDocumentResponse(false, "离线文档不存在或无权删除")
                );
            }

            // 3. 删除离线文档
            String deleteSql = "DELETE FROM offline_documents WHERE offline_doc_id = ? AND user_id = ?";
            int rowsAffected = jdbcTemplate.update(deleteSql, offlineDocId, userId);

            printQueryResult("删除行数: " + rowsAffected);

            if (rowsAffected > 0) {
                // 4. 同时删除相关的下载记录
                String deleteDownloadsSql = "DELETE FROM offline_downloads WHERE offline_doc_id = ? AND user_id = ?";
                jdbcTemplate.update(deleteDownloadsSql, offlineDocId, userId);

                DeleteDocumentResponse response = new DeleteDocumentResponse(true, "离线文档删除成功");
                printResponse(response);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new DeleteDocumentResponse(false, "删除失败")
                );
            }

        } catch (Exception e) {
            System.err.println("删除离线文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new DeleteDocumentResponse(false, "服务器内部错误: " + e.getMessage())
            );
        }
    }
}