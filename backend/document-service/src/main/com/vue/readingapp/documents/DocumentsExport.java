package com.vue.readingapp.documents;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentsExport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 打印接收到的请求
    private void printRequest(Integer documentId, String format, String authHeader) {
        System.out.println("=== 收到导出文档请求 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("格式: " + format);
        System.out.println("认证头: " + authHeader);
        System.out.println("=====================");
    }

    @GetMapping("/{documentId}/export")
    public ResponseEntity<InputStreamResource> exportDocument(
            @PathVariable("documentId") Integer documentId,
            @RequestParam(value = "format", defaultValue = "pdf") String format,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // 打印接收到的请求
            printRequest(documentId, format, authHeader);

            // 1. 验证认证
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String token = authHeader.substring(7);

            // 验证token
            String tokenSql = "SELECT user_id FROM user_sessions WHERE access_token = ? AND expires_at > NOW()";
            List<Map<String, Object>> tokenResults = jdbcTemplate.queryForList(tokenSql, token);

            if (tokenResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Integer userId = (Integer) tokenResults.get(0).get("user_id");

            // 2. 检查文档是否存在且属于当前用户
            String checkSql = "SELECT file_path, file_name, file_type FROM documents WHERE document_id = ? AND user_id = ? AND deleted_at IS NULL";
            List<Map<String, Object>> documents = jdbcTemplate.queryForList(checkSql, documentId, userId);

            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Map<String, Object> document = documents.get(0);
            String filePath = (String) document.get("file_path");
            String fileName = (String) document.get("file_name");
            String fileType = (String) document.get("file_type");

            System.out.println("=== 数据库查询结果 ===");
            System.out.println("文件路径: " + filePath);
            System.out.println("文件名: " + fileName);
            System.out.println("文件类型: " + fileType);
            System.out.println("===================");

            // 3. 检查文件是否存在
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.err.println("文件不存在: " + filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // 4. 根据请求的格式准备文件
            String exportFileName;
            MediaType mediaType;

            switch (format.toLowerCase()) {
                case "pdf":
                    // 如果原文件不是PDF，这里应该进行转换
                    // 简化实现：直接返回原文件或生成PDF
                    exportFileName = fileName.replaceFirst("[.][^.]+$", "") + ".pdf";
                    mediaType = MediaType.APPLICATION_PDF;
                    break;
                case "txt":
                    exportFileName = fileName.replaceFirst("[.][^.]+$", "") + ".txt";
                    mediaType = MediaType.TEXT_PLAIN;
                    break;
                case "epub":
                    exportFileName = fileName.replaceFirst("[.][^.]+$", "") + ".epub";
                    mediaType = MediaType.parseMediaType("application/epub+zip");
                    break;
                default:
                    // 默认返回原文件
                    exportFileName = fileName;
                    mediaType = MediaType.parseMediaType(fileType != null ? fileType : "application/octet-stream");
            }

            // 5. 创建输入流
            InputStream inputStream = new FileInputStream(path.toFile());

            // 6. 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFileName + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            System.out.println("=== 准备返回的响应 ===");
            System.out.println("文件名: " + exportFileName);
            System.out.println("媒体类型: " + mediaType);
            System.out.println("文件大小: " + Files.size(path) + " bytes");
            System.out.println("===================");

            // 7. 返回文件流
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(Files.size(path))
                    .contentType(mediaType)
                    .body(new InputStreamResource(inputStream));

        } catch (FileNotFoundException e) {
            System.err.println("文件未找到: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            System.err.println("文件读取错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("导出文档过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}