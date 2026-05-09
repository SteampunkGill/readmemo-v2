package com.vue.readingapp.ocr.service;

import com.vue.readingapp.ocr.core.OcrEngine;
import com.vue.readingapp.ocr.core.OcrResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.vue.readingapp.ocr.utils.ImageUtils;
import com.vue.readingapp.ocr.utils.TextUtils;

@Service
public class OcrProcessingService {

    @Autowired
    private OcrEngine ocrEngine;

    @Autowired
    private ImagePreprocessor imagePreprocessor;

    /**
     * 处理图像OCR
     */
    public OcrResult processImage(BufferedImage image, Map<String, Object> options) {
        // 1. 图像预处理
        BufferedImage processedImage = imagePreprocessor.preprocess(image, options);

        // 2. OCR识别
        OcrResult result = ocrEngine.recognize(processedImage, options);

        // 3. 后处理（可选）
        result = postprocess(result, options);

        return result;
    }

    /**
     * 处理Base64图像OCR
     */
    public OcrResult processBase64Image(String base64Image, Map<String, Object> options) {
        try {
            // 1. 解码Base64
            BufferedImage image = ImageUtils.decodeBase64Image(base64Image);

            // 2. 处理图像
            return processImage(image, options);
        } catch (IOException e) {
            return createErrorResult("Base64图像解码失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理OCR
     */
    public List<OcrResult> batchProcess(List<BufferedImage> images, Map<String, Object> options) {
        List<OcrResult> results = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            try {
                OcrResult result = processImage(images.get(i), options);
                results.add(result);

                // 更新进度（如果需要）
                updateProgress(i + 1, images.size());

            } catch (Exception e) {
                // 记录错误，继续处理其他图像
                System.err.println("处理第 " + (i + 1) + " 张图像失败: " + e.getMessage());
                results.add(createErrorResult(e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 获取OCR引擎信息
     */
    public Map<String, Object> getEngineInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("engineName", ocrEngine.getEngineName());
        info.put("supportedLanguages", ocrEngine.getSupportedLanguages());
        info.put("version", "Tesseract 4.1.1");
        return info;
    }

    private OcrResult postprocess(OcrResult result, Map<String, Object> options) {
        // 文本后处理，如去除多余空格、纠正常见错误等
        String processedText = TextUtils.postprocess(result.getText(), options);
        result.setText(processedText);
        return result;
    }

    private void updateProgress(int current, int total) {
        // 更新处理进度，可以用于进度条显示
        int progress = (int) ((current * 100.0) / total);
        System.out.println("OCR处理进度: " + progress + "%");
    }

    private OcrResult createErrorResult(String errorMessage) {
        return OcrResult.builder()
                .text("OCR处理失败: " + errorMessage)
                .confidence(0.0f)
                .engineName("Error")
                .language("")
                .processingTime(0)
                .build();
    }
}