package com.tryneuro.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class ImageCompressionService {

    private static final int MAX_WIDTH = 300;
    private static final int MAX_HEIGHT = 300;
    private static final float JPEG_QUALITY = 0.85f;

    /**
     * Сжимает изображение до 300x300 и конвертирует в JPEG
     */
    public byte[] compressImage(byte[] imageData) throws IOException {
        // Читаем исходное изображение
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            throw new IOException("Unsupported image format");
        }

        // Рассчитываем новые размеры с сохранением пропорций
        int newWidth = originalImage.getWidth();
        int newHeight = originalImage.getHeight();

        if (newWidth > MAX_WIDTH || newHeight > MAX_HEIGHT) {
            double ratioX = (double) MAX_WIDTH / newWidth;
            double ratioY = (double) MAX_HEIGHT / newHeight;
            double ratio = Math.min(ratioX, ratioY);

            newWidth = (int) (newWidth * ratio);
            newHeight = (int) (newHeight * ratio);
        }

        // Создаём уменьшенное изображение
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Включаем сглаживание для лучшего качества
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Сжимаем в JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage jpegImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        g2d = jpegImage.createGraphics();
        g2d.drawImage(resizedImage, 0, 0, null);
        g2d.dispose();

        ImageIO.write(jpegImage, "jpg", baos);

        return baos.toByteArray();
    }

    /**
     * Принимает base64 строку, декодирует и сжимает изображение
     */
    public byte[] compressFromBase64(String base64Data) throws IOException {
        // Декодируем base64
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        return compressImage(imageBytes);
    }

    /**
     * Принимает MultipartFile и сжимает изображение
     */
    public byte[] compress(MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();
        return compressImage(imageBytes);
    }
}
