package com.vue.readingapp.documents;

import com.vue.readingapp.ocr.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentProcessingController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/trigger-ocr")
    public ResponseEntity<Map<String, Object>> triggerOcrProcessing(@RequestBody TriggerOcrRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (request.getDocumentId() == null) {
            response.put("success", false);
            response.put("message", "Document ID is required.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean triggered = ocrService.processDocument(request.getDocumentId());

            response.put("success", true);
            response.put("documentId", request.getDocumentId());

            if (triggered) {
                response.put("message", "OCR processing triggered successfully for document ID: " + request.getDocumentId());
            } else {
                // OcrService.processDocument 返回 false，表示处理已在后台启动，但不是同步完成
                response.put("message", "OCR processing initiated for document ID: " + request.getDocumentId() + ". Check status for updates.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error triggering OCR processing for document ID " + request.getDocumentId() + ": " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Failed to trigger OCR processing: " + e.getMessage());
            response.put("documentId", request.getDocumentId());

            return ResponseEntity.status(500).body(response);
        }
    }

    static class TriggerOcrRequest {
        private Integer documentId;

        public Integer getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Integer documentId) {
            this.documentId = documentId;
        }
    }
}