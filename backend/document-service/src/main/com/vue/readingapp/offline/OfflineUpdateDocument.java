
package com.vue.readingapp.offline;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/offline")
public class OfflineUpdateDocument {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    // 打印接收到的请求
    private void printRequest(Object request) {
        System.out.println("=== 收到更新离线文档信息请求 ===");
        System.out.println("请求数据: " + request);
        System.out.println("=============================");
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
    public static class UpdateDocumentRequest {
        private UpdateData data;

        public UpdateData getData() { return data; }
        public void setData(UpdateData data) { this.data = data; }
    }

    public static class UpdateData {
        private String title;
        private String description;
        private String[] tags;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String[] getTags() { return tags; }
        public void setTags(String[] tags) { this.tags = tags; }
    }

    // 响应DTO
    public static class UpdateDocumentResponse {
        private boolean success;
        private String message;
        private UpdateDocumentData data;

        public UpdateDocumentResponse(boolean success, String message, UpdateDocumentData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public UpdateDocumentData getData() { return data; }
        public void setData(UpdateDocumentData data) { this.data = data; }
    }

    public static class UpdateDocumentData {
        private String offlineDocId;
        private String title;
        private String description;
        private String[] tags;
        private boolean isSynced;
        private String updatedAt;

        public UpdateDocumentData(String offlineDocId, String title, String description,
                                  String[] tags, boolean isSynced, String updatedAt) {
            this.offlineDocId = offlineDocId;
            this.title = title;
            this.description = description;
            this.tags = tags;
            this.isSynced = isSynced;
            this.updatedAt = updatedAt;
        }

        public String getOfflineDocId() { return offlineDocId; }
        public void setOfflineDocId(String offlineDocId) { this.offlineDocId = offlineDocId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String[] getTags() { return tags; }
        public void setTags(String[] tags) { this.tags = tags; }

        public boolean isSynced() { return isSynced; }
        public void setSynced(boolean synced) { isSynced = synced; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
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

    @PutMapping("/documents/{offlineDocId}")
    public ResponseEntity<UpdateDocumentResponse> updateOfflineDocument(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String offlineDocId,
            @RequestBody UpdateDocumentRequest request) {

        // 打印接收到的请求
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("authHeader", authHeader);
        requestInfo.put("offlineDocId", offlineDocId);
        requestInfo.put("requestBody", request);
        printRequest(requestInfo);

        try {
            // 1. 验证用户认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateDocumentResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);
            Integer userId = validateToken(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new UpdateDocumentResponse(false, "登录已过期，请重新登录", null)
                );
            }

            // 2. 验证请求数据
            if (request == null || request.getData() == null) {
                return ResponseEntity.badRequest().body(
                        new UpdateDocumentResponse(false, "请求数据不能为空", null)
                );
            }

            UpdateData updateData = request.getData();

            // 3. 检查离线文档是否存在且属于该用户
            String checkSql = "SELECT offline_doc_id FROM offline_documents WHERE offline_doc_id = ? AND user_id = ?";
            List<Map<String, Object>> offlineDocs = jdbcTemplate.queryForList(checkSql, offlineDocId, userId);

            if (offlineDocs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UpdateDocumentResponse(false, "离线文档不存在或无权更新", null)
                );
            }

            // 4. 构建更新SQL
            StringBuilder updateSql = new StringBuilder("UPDATE offline_documents SET ");
            List<Object> params = new ArrayList<>();

            if (updateData.getTitle() != null && !updateData.getTitle().trim().isEmpty()) {
                updateSql.append("title = ?, ");
                params.add(updateData.getTitle());
            }

            if (updateData.getDescription() != null) {
                updateSql.append("description = ?, ");
                params.add(updateData.getDescription());
            }

            if (updateData.getTags() != null) {
                updateSql.append("tags = ?, ");
                try {
                    String tagsJson = objectMapper.writeValueAsString(updateData.getTags());
                    params.add(tagsJson);
                } catch (Exception e) {
                    params.add("[]");
                }
            }

            // 标记为未同步（因为内容已更新）
            updateSql.append("is_synced = FALSE, ");

            // 增加版本号
            updateSql.append("offline_version = offline_version + 1, ");

            // 更新时间
            updateSql.append("updated_at = NOW() ");

            updateSql.append("WHERE offline_doc_id = ? AND user_id = ?");
            params.add(offlineDocId);
            params.add(userId);

            // 5. 执行更新
            int rowsAffected = jdbcTemplate.update(updateSql.toString(), params.toArray());
            printQueryResult("更新行数: " + rowsAffected);

            if (rowsAffected > 0) {
                // 6. 查询更新后的数据
                String querySql = "SELECT offline_doc_id, title, description, tags, is_synced, updated_at " +
                        "FROM offline_documents WHERE offline_doc_id = ? AND user_id = ?";
                List<Map<String, Object>> updatedDocs = jdbcTemplate.queryForList(querySql, offlineDocId, userId);

                if (!updatedDocs.isEmpty()) {
                    Map<String, Object> row = updatedDocs.get(0);

                    // 解析tags
                    String[] tagsArray = new String[0];
                    String tagsJson = (String) row.get("tags");
                    if (tagsJson != null && !tagsJson.isEmpty()) {
                        try {
                            tagsArray = objectMapper.readValue(tagsJson, String[].class);
                        } catch (Exception e) {
                            tagsArray = new String[0];
                        }
                    }

                    // 7. 准备响应数据
                    UpdateDocumentData data = new UpdateDocumentData(
                            (String) row.get("offline_doc_id"),
                            (String) row.get("title"),
                            (String) row.get("description"),
                            tagsArray,
                            row.get("is_synced") != null && (Boolean) row.get("is_synced"),
                            row.get("updated_at").toString()
                    );

                    UpdateDocumentResponse response = new UpdateDocumentResponse(true, "离线文档更新成功", data);
                    printResponse(response);

                    return ResponseEntity.ok(response);
                }
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateDocumentResponse(false, "更新失败", null)
            );

        } catch (Exception e) {
            System.err.println("更新离线文档信息过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new UpdateDocumentResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }
}