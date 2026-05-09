package com.vue.readingapp.ocr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.vue.readingapp.ocr.core.OcrEngine;
import com.vue.readingapp.ocr.core.TesseractEngine;

@Configuration
public class OcrConfig {

    @Bean
    public OcrEngine ocrEngine() {
        return new TesseractEngine();
    }

    @Bean
    public OcrProperties ocrProperties() {
        return new OcrProperties();
    }

    public static class OcrProperties {
        private String tessdataPath = "src/main/resources/tessdata";
        private String defaultLanguage = "chi_sim+eng";
        private int defaultPageSegMode = 1;
        private int defaultOcrEngineMode = 1;
        private boolean enablePreprocessing = true;
        private int binarizationThreshold = 128;

        // getters and setters
        public String getTessdataPath() { return tessdataPath; }
        public void setTessdataPath(String tessdataPath) { this.tessdataPath = tessdataPath; }

        public String getDefaultLanguage() { return defaultLanguage; }
        public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }

        public int getDefaultPageSegMode() { return defaultPageSegMode; }
        public void setDefaultPageSegMode(int defaultPageSegMode) { this.defaultPageSegMode = defaultPageSegMode; }

        public int getDefaultOcrEngineMode() { return defaultOcrEngineMode; }
        public void setDefaultOcrEngineMode(int defaultOcrEngineMode) { this.defaultOcrEngineMode = defaultOcrEngineMode; }

        public boolean isEnablePreprocessing() { return enablePreprocessing; }
        public void setEnablePreprocessing(boolean enablePreprocessing) { this.enablePreprocessing = enablePreprocessing; }

        public int getBinarizationThreshold() { return binarizationThreshold; }
        public void setBinarizationThreshold(int binarizationThreshold) { this.binarizationThreshold = binarizationThreshold; }
    }
}