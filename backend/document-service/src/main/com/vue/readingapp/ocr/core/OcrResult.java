package com.vue.readingapp.ocr.core;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OcrResult {
    private String text;
    private float confidence;
    private String engineName;
    private String language;
    private long processingTime;
    private List<WordInfo> words;
    private List<LineInfo> lines;
    private Map<String, Object> metadata;

    @Data
    @Builder
    public static class WordInfo {
        private String text;
        private float confidence;
        private BoundingBox bbox;
    }

    @Data
    @Builder
    public static class LineInfo {
        private String text;
        private float confidence;
        private List<WordInfo> words;
        private BoundingBox bbox;
    }

    @Data
    @Builder
    public static class BoundingBox {
        private int x0;
        private int y0;
        private int x1;
        private int y1;
    }
}