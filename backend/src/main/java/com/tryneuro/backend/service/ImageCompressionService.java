package com.tryneuro.backend.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

@Service
public class ImageCompressionService {

    private static final int MAX_WIDTH = 300;
    private static final int MAX_HEIGHT = 300;

    /**
     * Сжимает изображение до 300x300 и конвертирует в JPEG.
     * EXIF orientation сохраняется в метаданных JPEG — браузер применяет поворот через CSS.
     */
    public byte[] compressImage(byte[] imageData) throws IOException {
        // Читаем оригинальные метаданные JPEG (сохраняем EXIF orientation для браузера)
        IIOMetadata srcMetadata = null;
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData));
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                srcMetadata = reader.getImageMetadata(0);
                reader.dispose();
            }
        } catch (Exception e) {
            // метаданные опциональны — без них фото будет в ориентации пикселей
        }

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            throw new IOException("Unsupported image format");
        }

        // НЕ применяем EXIF rotation — браузер сам повернёт через image-orientation: from-image
        // image-orientation читает EXIF Orientation tag, который мы сохранили в metadata выше

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

        // Сжимаем в JPEG с сохранением EXIF метаданных (браузер применит image-orientation)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = null;
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                writer = writers.next();
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                writer.setOutput(ImageIO.createImageOutputStream(baos));

                IIOImage iioImage = srcMetadata != null
                        ? new IIOImage(resizedImage, null, srcMetadata)
                        : new IIOImage(resizedImage, null, null);
                writer.write(null, iioImage, writeParam);
            } else {
                // fallback — без метаданных, EXIF будет 1 (normal)
                ImageIO.write(resizedImage, "jpg", baos);
            }
        } finally {
            if (writer != null) writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * Читает EXIF orientation и применяет соответствующее преобразование.
     * Телефоны (особенно iPhone) хранят фото в ландшафтной ориентации,
     * а поворот указывают в EXIF теге Orientation.
     */
    private BufferedImage applyExifOrientation(BufferedImage image, byte[] imageData) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageData));
            ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exifDir == null || !exifDir.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
                return image;
            }

            int orientation = exifDir.getInt(ExifDirectoryBase.TAG_ORIENTATION);
            AffineTransform transform = orientationToTransform(orientation, image.getWidth(), image.getHeight());
            if (transform == null) {
                return image;
            }

            AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
            BufferedImage rotated = new BufferedImage(
                    orientation == 5 || orientation == 6 || orientation == 7 || orientation == 8
                            ? image.getHeight() : image.getWidth(),
                    orientation == 5 || orientation == 6 || orientation == 7 || orientation == 8
                            ? image.getWidth() : image.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            op.filter(image, rotated);
            return rotated;
        } catch (Exception e) {
            // Если не удалось прочитать EXIF — возвращаем как есть
            return image;
        }
    }

    /**
     * Преобразует EXIF orientation (1-8) в AffineTransform.
     * См. http://sylvana.net/jpegcrop/exif_orientation.html
     */
    private AffineTransform orientationToTransform(int orientation, int width, int height) {
        switch (orientation) {
            case 2: // Flip X
                return AffineTransform.getScaleInstance(-1, 1);
            case 3: // Rotate 180
                return AffineTransform.getQuadrantRotateInstance(2, width / 2.0, height / 2.0);
            case 4: // Flip Y
                return AffineTransform.getScaleInstance(1, -1);
            case 5: // Transpose (flip X + rotate 90 CW)
                return new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6: // Rotate 90 CW
                return new AffineTransform(0, 1, -1, 0, height, 0);
            case 7: // Transverse (flip X + rotate 90 CCW)
                return new AffineTransform(0, -1, -1, 0, height, width);
            case 8: // Rotate 90 CCW
                return new AffineTransform(0, -1, 1, 0, 0, width);
            default: // 1 (normal) or unknown
                return null;
        }
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