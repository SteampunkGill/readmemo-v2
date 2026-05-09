package com.vue.readingapp.ocr.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

public class ImageUtils {

    /**
     * 解码Base64图像
     */
    public static BufferedImage decodeBase64Image(String base64Image) throws IOException {
        // 移除可能的头部信息
        String imageData = base64Image;
        if (base64Image.contains(",")) {
            imageData = base64Image.split(",")[1];
        }

        byte[] imageBytes = Base64.getDecoder().decode(imageData);
        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }

    /**
     * 编码图像为Base64
     */
    public static String encodeImageToBase64(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 调整图像大小
     */
    public static BufferedImage resizeImage(BufferedImage image, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    /**
     * 计算图像质量指标
     */
    public static Map<String, Object> calculateImageQuality(BufferedImage image) {
        Map<String, Object> quality = new HashMap<>();

        // 计算平均亮度
        long totalBrightness = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                totalBrightness += (color.getRed() + color.getGreen() + color.getBlue()) / 3;
            }
        }
        quality.put("averageBrightness", totalBrightness / (image.getWidth() * image.getHeight()));

        // 计算对比度（简化）
        quality.put("width", image.getWidth());
        quality.put("height", image.getHeight());
        quality.put("aspectRatio", (double) image.getWidth() / image.getHeight());

        return quality;
    }
}