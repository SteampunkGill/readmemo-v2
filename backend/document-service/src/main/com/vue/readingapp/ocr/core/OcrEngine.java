package com.vue.readingapp.ocr.core;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.List; // <-- 添加这一行导入 List

public interface OcrEngine {
    /**
     * 识别图像中的文本
     */
    OcrResult recognize(BufferedImage image, Map<String, Object> options);

    /**
     * 识别Base64编码的图像
     */
    OcrResult recognizeFromBase64(String base64Image, Map<String, Object> options);

    /**
     * 获取支持的语种列表
     */
    List<String> getSupportedLanguages();

    /**
     * 获取引擎名称
     */
    String getEngineName();
}