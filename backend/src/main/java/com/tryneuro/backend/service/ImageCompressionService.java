package com.tryneuro.backend.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
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

    /**
     * Сжимает изображение до 300x300 и конвертирует в JPEG.
     * EXIF orientation внедряется в JPEG — браузер применяет поворот через CSS.
     */
    public byte[] compressImage(byte[] imageData) throws IOException {
        // Читаем EXIF orientation из сырых байтов (metadata-extractor)
        int exifOrientation = 1;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageData));
            ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifDir != null && exifDir.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
                exifOrientation = exifDir.getInt(ExifDirectoryBase.TAG_ORIENTATION);
            }
        } catch (Exception e) {
            // если EXIF не читается — считаем orientation = 1 (normal)
        }

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            throw new IOException("Unsupported image format");
        }

        // НЕ поворачиваем пиксели — браузер сам повернёт через image-orientation: from-image
        // EXIF Orientation tag внедряется в JPEG на шаге записи

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

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Сжимаем в JPEG через ImageIO.write (надёжно, без исключений)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);
        byte[] jpegBytes = baos.toByteArray();

        // Внедряем EXIF Orientation tag в JPEG
        if (exifOrientation != 1) {
            jpegBytes = injectExifApp1(jpegBytes, exifOrientation);
        }

        return jpegBytes;
    }

    /**
     * Внедряет EXIF APP1-маркер с тегом Orientation в JPEG-поток перед SOS-маркером.
     * Браузер через CSS image-orientation: from-image прочитает тег и повернёт фото.
     */
    private byte[] injectExifApp1(byte[] jpegData, int orientation) throws IOException {
        int sosIdx = -1;
        for (int i = 0; i < jpegData.length - 1; i++) {
            if ((jpegData[i] & 0xFF) == 0xFF && (jpegData[i + 1] & 0xFF) == 0xDA) {
                sosIdx = i;
                break;
            }
        }

        ByteArrayOutputStream app1 = new ByteArrayOutputStream();
        ByteArrayOutputStream exif = new ByteArrayOutputStream();
        // "Exif\0\0" идентификатор
        exif.write(new byte[]{0x45, 0x78, 0x69, 0x66, 0x00, 0x00});
        // TIFF header (big-endian, offset to IFD0 = 8)
        exif.write(new byte[]{0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08});
        // IFD0: 1 entry (Orientation tag 0x0112, type SHORT, value = orientation)
        exif.write(0x00); exif.write(0x01); // 1 entry
        exif.write(0x01); exif.write(0x12); // tag = Orientation (0x0112)
        exif.write(0x00); exif.write(0x03); // type = SHORT
        exif.write(0x00); exif.write(0x00); exif.write(0x00); exif.write(0x01); // count = 1
        exif.write(0x00); exif.write((byte) orientation); // value
        exif.write(0x00); exif.write(0x00); // padding to 4 bytes
        exif.write(0x00); exif.write(0x00); exif.write(0x00); exif.write(0x00); // next IFD = 0

        int app1Len = 2 + exif.size();
        app1.write(0xFF); app1.write(0xE1); // APP1 marker
        app1.write((app1Len >> 8) & 0xFF); app1.write(app1Len & 0xFF);
        app1.write(exif.toByteArray());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (sosIdx >= 0) {
            out.write(jpegData, 0, sosIdx);
            out.write(app1.toByteArray());
            out.write(jpegData, sosIdx, jpegData.length - sosIdx);
        } else {
            out.write(jpegData);
        }
        return out.toByteArray();
    }

    /**
     * Принимает base64 строку, декодирует и сжимает изображение
     */
    public byte[] compressFromBase64(String base64Data) throws IOException {
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