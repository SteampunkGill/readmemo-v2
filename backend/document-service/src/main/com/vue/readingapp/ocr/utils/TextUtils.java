package com.vue.readingapp.ocr.utils;

import java.util.Map;
import java.util.regex.Pattern;

public class TextUtils {

    /**
     * 文本后处理
     */
    public static String postprocess(String text, Map<String, Object> options) {
        if (text == null) return "";

        String processed = text;

        // 1. 去除多余空格
        processed = processed.replaceAll("\\s+", " ");

        // 2. 去除首尾空格
        processed = processed.trim();

        // 3. 纠正常见OCR错误
        if (options != null && Boolean.TRUE.equals(options.get("autoCorrect"))) {
            processed = autoCorrect(processed);
        }

        // 4. 格式化段落
        if (options != null && Boolean.TRUE.equals(options.get("formatParagraphs"))) {
            processed = formatParagraphs(processed);
        }

        return processed;
    }

    /**
     * 自动纠正常见OCR错误
     */
    private static String autoCorrect(String text) {
        String corrected = text;

        // 常见OCR错误映射
        Map<String, String> corrections = Map.of(
                "l", "I",
                "0", "O",
                "rn", "m",
                "vv", "w"
        );

        for (Map.Entry<String, String> entry : corrections.entrySet()) {
            corrected = corrected.replace(entry.getKey(), entry.getValue());
        }

        return corrected;
    }

    /**
     * 格式化段落
     */
    private static String formatParagraphs(String text) {
        // 将连续换行转换为段落分隔
        return text.replaceAll("\\n{3,}", "\n\n");
    }

    /**
     * 计算文本置信度
     */
    public static float calculateTextConfidence(String text, Map<String, Object> options) {
        if (text == null || text.isEmpty()) return 0.0f;

        float confidence = 100.0f;

        // 1. 检查文本长度
        if (text.length() < 10) {
            confidence -= 20.0f;
        }

        // 2. 检查特殊字符比例
        int specialCharCount = text.replaceAll("[\\w\\s]", "").length();
        float specialCharRatio = (float) specialCharCount / text.length();
        if (specialCharRatio > 0.3) {
            confidence -= 30.0f;
        }

        // 3. 检查单词长度异常
        String[] words = text.split("\\s+");
        int longWordCount = 0;
        for (String word : words) {
            if (word.length() > 20) {
                longWordCount++;
            }
        }
        if (longWordCount > words.length * 0.1) {
            confidence -= 15.0f;
        }

        return Math.max(0.0f, Math.min(100.0f, confidence));
    }
}