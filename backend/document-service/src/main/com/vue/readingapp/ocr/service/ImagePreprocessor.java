package com.vue.readingapp.ocr.service;

import org.springframework.stereotype.Component;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Component
public class ImagePreprocessor {

    /**
     * 图像预处理
     */
    public BufferedImage preprocess(BufferedImage image, Map<String, Object> options) {
        if (options == null || !Boolean.TRUE.equals(options.get("preprocess"))) {
            return image;
        }

        // 转换为灰度图
        BufferedImage grayImage = convertToGrayScale(image);

        // 二值化
        BufferedImage binaryImage = applyBinarization(grayImage, options);

        // 去噪
        BufferedImage denoisedImage = applyDenoising(binaryImage, options);

        // 锐化（可选）
        if (options.containsKey("sharpen") && Boolean.TRUE.equals(options.get("sharpen"))) {
            return applySharpening(denoisedImage);
        }

        return denoisedImage;
    }

    private BufferedImage convertToGrayScale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g = grayImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return grayImage;
    }

    private BufferedImage applyBinarization(BufferedImage image, Map<String, Object> options) {
        int threshold = options.containsKey("threshold") ?
                Integer.parseInt(options.get("threshold").toString()) : 128;

        BufferedImage binaryImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                int gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                int newColor = gray > threshold ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                binaryImage.setRGB(x, y, newColor);
            }
        }

        return binaryImage;
    }

    private BufferedImage applyDenoising(BufferedImage image, Map<String, Object> options) {
        // 简单的去噪：中值滤波
        int kernelSize = options.containsKey("kernelSize") ?
                Integer.parseInt(options.get("kernelSize").toString()) : 3;

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                image.getType()
        );

        int halfKernel = kernelSize / 2;

        for (int y = halfKernel; y < image.getHeight() - halfKernel; y++) {
            for (int x = halfKernel; x < image.getWidth() - halfKernel; x++) {
                // 收集邻域像素
                List<Integer> neighbors = new ArrayList<>();
                for (int ky = -halfKernel; ky <= halfKernel; ky++) {
                    for (int kx = -halfKernel; kx <= halfKernel; kx++) {
                        neighbors.add(image.getRGB(x + kx, y + ky) & 0xFF);
                    }
                }

                // 中值
                neighbors.sort(Integer::compareTo);
                int median = neighbors.get(neighbors.size() / 2);

                // 设置结果像素
                int newColor = (median << 16) | (median << 8) | median;
                result.setRGB(x, y, newColor);
            }
        }

        return result;
    }

    private BufferedImage applySharpening(BufferedImage image) {
        // 简单的锐化：拉普拉斯算子
        int[][] kernel = {
                {0, -1, 0},
                {-1, 5, -1},
                {0, -1, 0}
        };

        return applyConvolution(image, kernel);
    }

    private BufferedImage applyConvolution(BufferedImage image, int[][] kernel) {
        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                image.getType()
        );

        int kernelSize = kernel.length;
        int halfKernel = kernelSize / 2;

        for (int y = halfKernel; y < image.getHeight() - halfKernel; y++) {
            for (int x = halfKernel; x < image.getWidth() - halfKernel; x++) {
                int sum = 0;

                for (int ky = -halfKernel; ky <= halfKernel; ky++) {
                    for (int kx = -halfKernel; kx <= halfKernel; kx++) {
                        int pixel = image.getRGB(x + kx, y + ky) & 0xFF;
                        int weight = kernel[ky + halfKernel][kx + halfKernel];
                        sum += pixel * weight;
                    }
                }

                // 限制范围
                sum = Math.max(0, Math.min(255, sum));

                int newColor = (sum << 16) | (sum << 8) | sum;
                result.setRGB(x, y, newColor);
            }
        }

        return result;
    }
}