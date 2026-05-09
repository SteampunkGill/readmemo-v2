package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsShare {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Integer documentId, ShareRequest request, String authHeader) {
        System.out.println("=== 收到分享文档请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("分享设置: " + request);
        System.out.println("认证头: " + authHeader);
        System.out.println("=====================");
    }

    // 打印查询结果
    private void printQueryResult(Map<String, Object> document) {
        System.out.println("=== 数据库查询结果 ===");
        System.out.println("文档信息: " + document);
        System.out.println("===================");
    }

    // 打印返回数据
    private void printResponse(ShareResponse response) {
        System.out.println("=== 准备返回的响应 ===");
        System.out.println("响应数据: " + response);
        System.out.println("===================");
    }

    // 请求DTO
    public static class ShareRequest {
        private Boolean isPublic;
        private String shareLink;
        private String password;
        private Date expiryDate;
        private List<String> allowedUsers;

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public String getShareLink() { return shareLink; }
        public void setShareLink(String shareLink) { this.shareLink = shareLink; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Date getExpiryDate() { return expiryDate; }
        public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

        public List<String> getAllowedUsers() { return allowedUsers; }
        public void setAllowedUsers(List<String> allowedUsers) { this.allowedUsers = allowedUsers; }
    }

    // 响应DTO
    public static class ShareResponse {
        private boolean success;
        private String message;
        private ShareData data;

        public ShareResponse(boolean success, String message, ShareData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public ShareData getData() { return data; }
        public void setData(ShareData data) { this.data = data; }
    }

    public static class ShareData {
        private ShareDocumentDTO document;

        public ShareData(ShareDocumentDTO document) {
            this.document = document;
        }

        public ShareDocumentDTO getDocument() { return document; }
        public void setDocument(ShareDocumentDTO document) { this.document = document; }
    }

    public static class ShareDocumentDTO {
        private Integer id;
        private String title;
        private Boolean isPublic;
        private String shareLink;
        private Boolean hasPassword;
        private Date expiryDate;
        private String updatedAt;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public Boolean getIsPublic() { return isPublic; }
        public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

        public String getShareLink() { return shareLink; }
        public void setShareLink(String shareLink) { this.shareLink = shareLink; }

        public Boolean getHasPassword() { return hasPassword; }
        public void setHasPassword(Boolean hasPassword) { this.hasPassword = hasPassword; }

        public Date getExpiryDate() { return expiryDate; }
        public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @PostMapping("/{documentId}/share")
    public ResponseEntity<ShareResponse> shareDocument(
            @PathVariable("documentId") Integer documentId,
            @RequestBody ShareRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, request, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ShareResponse(false, "请先登录", null)
                );
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        new ShareResponse(false, "登录已过期，请重新登录", null)
                );
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 验证请求数据
            if (request == null) {
                return ResponseEntity.badRequest().body(
                        new ShareResponse(false, "分享设置不能为空", null)
                );
            }

            // 3. 检查文档是否存在且属于当前用户
            String checkSql = "SELECT document_id, title FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ShareResponse(false, "文档不存在或没有权限", null)
                );
            }

            Map<String, Object> document = documents.get(0);
            String documentTitle = (String) document.get("title");

            // 4. 生成分享链接（如果未提供）
            String shareLink = request.getShareLink();
            if (shareLink == null || shareLink.trim().isEmpty()) {
                // 生成唯一的分享链接
                shareLink = "https://readmemo.app/share/" + UUID.randomUUID().toString();
            }

            // 5. 更新文档分享设置
            LocalDateTime now = LocalDateTime.now();
            Timestamp timestamp = Timestamp.valueOf(now);

            String updateSql = "UPDATE documents SET is_public = ?, share_link = ?, share_password = ?, " +
                    "share_expiry_date = ?, updated_at = ? WHERE document_id = ? AND user_id = ?";

            int rowsUpdated = jdbcTemplate.update(updateSql,
                    request.getIsPublic() != null ? request.getIsPublic() : true,
                    shareLink,
                    request.getPassword(),
                    request.getExpiryDate() != null ? new Timestamp(request.getExpiryDate().getTime()) : null,
                    timestamp,
                    documentId,
                    userId
            );

            if (rowsUpdated == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new ShareResponse(false, "文档分享失败", null)
                );
            }

            // 6. 保存允许的用户列表（如果有）
            if (request.getAllowedUsers() != null && !request.getAllowedUsers().isEmpty()) {
                // 这里应该有一个文档分享权限表
                // 简化实现：暂时不实现
                System.out.println("允许的用户: " + request.getAllowedUsers());
            }

            // 7. 查询更新后的文档信息
            String querySql = "SELECT document_id, title, is_public, share_link, share_password, " +
                    "share_expiry_date, updated_at FROM documents WHERE document_id = ? AND user_id = ?";
            List<Map<String, Object>> updatedDocuments = jdbcTemplate.queryForList(querySql, documentId, userId);
            Map<String, Object> updatedDocument = updatedDocuments.get(0);

            printQueryResult(updatedDocument);

            // 8. 构建响应数据
            ShareDocumentDTO dto = new ShareDocumentDTO();
            dto.setId((Integer) updatedDocument.get("document_id"));
            dto.setTitle((String) updatedDocument.get("title"));
            dto.setIsPublic((Boolean) updatedDocument.get("is_public"));
            dto.setShareLink((String) updatedDocument.get("share_link"));
            dto.setHasPassword(updatedDocument.get("share_password") != null);

            java.sql.Timestamp expiryDate = (java.sql.Timestamp) updatedDocument.get("share_expiry_date");
            dto.setExpiryDate(expiryDate != null ? new Date(expiryDate.getTime()) : null);

            dto.setUpdatedAt(formatDate((java.sql.Timestamp) updatedDocument.get("updated_at")));

            ShareData data = new ShareData(dto);
            ShareResponse response = new ShareResponse(true, "文档 '" + documentTitle + "' 分享成功", data);

            // 打印返回数据
            printResponse(response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("分享文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ShareResponse(false, "服务器内部错误: " + e.getMessage(), null)
            );
        }
    }

    // 格式化日期
    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null) return "";
        return timestamp.toLocalDateTime().toString();
    }
}