package com.tryneuro.backend.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class ImageCompressionServiceTest {

    private ImageCompressionService service;

    private static final int MAX_WIDTH = 300;
    private static final int MAX_HEIGHT = 300;

    @BeforeEach
    void setUp() {
        service = new ImageCompressionService();
    }

    @Test
    @DisplayName("compressImage уменьшает большое изображение до 300px")
    void compressImageReducesLargeImage() throws Exception {
        byte[] largeImage = createTestImage(1200, 900, "test");

        byte[] compressed = service.compressImage(largeImage);

        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
        assertTrue(compressed.length < largeImage.length,
                "Сжатое изображение должно быть меньше исходного");

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(result, "Результат должен быть читаемым JPEG");
        assertTrue(result.getWidth() <= MAX_WIDTH, "Ширина не должна превышать " + MAX_WIDTH);
        assertTrue(result.getHeight() <= MAX_HEIGHT, "Высота не должна превышать " + MAX_HEIGHT);
    }

    @Test
    @DisplayName("compressImage не увеличивает маленькое изображение")
    void compressImageDoesNotUpscaleSmallImage() throws Exception {
        byte[] smallImage = createTestImage(100, 80, "tiny");

        byte[] compressed = service.compressImage(smallImage);

        assertNotNull(compressed);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(result);
        assertEquals(100, result.getWidth(), "Маленькое изображение не должно увеличиваться");
        assertEquals(80, result.getHeight(), "Маленькое изображение не должно увеличиваться");
    }

    @Test
    @DisplayName("compressImage сохраняет EXIF orientation в выходном JPEG")
    void compressImagePreservesExifOrientation() throws Exception {
        byte[] imageWithExif = createTestImageWithExifOrientation(400, 300, 6);

        byte[] compressed = service.compressImage(imageWithExif);

        assertNotNull(compressed);

        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(compressed));
        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        assertNotNull(exifDir, "Выходной JPEG должен содержать EXIF IFD0");
        assertTrue(exifDir.containsTag(ExifDirectoryBase.TAG_ORIENTATION),
                "Выходной JPEG должен содержать EXIF Orientation tag");
        assertEquals(6, exifDir.getInt(ExifDirectoryBase.TAG_ORIENTATION),
                "EXIF Orientation должен сохраниться (6 = 90° CW)");
    }

    @Test
    @DisplayName("compressImage корректно обрабатывает изображение с EXIF orientation = 1 (normal)")
    void compressImageWithNormalExif() throws Exception {
        byte[] imageWithExif = createTestImageWithExifOrientation(300, 200, 1);

        byte[] compressed = service.compressImage(imageWithExif);

        assertNotNull(compressed);

        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(compressed));
        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        assertNotNull(exifDir);
        assertEquals(1, exifDir.getInt(ExifDirectoryBase.TAG_ORIENTATION));
    }

    @Test
    @DisplayName("compressImage корректно обрабатывает изображение с EXIF orientation = 3 (180°)")
    void compressImageWithExif180() throws Exception {
        byte[] imageWithExif = createTestImageWithExifOrientation(400, 300, 3);

        byte[] compressed = service.compressImage(imageWithExif);

        assertNotNull(compressed);

        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(compressed));
        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        assertNotNull(exifDir);
        assertEquals(3, exifDir.getInt(ExifDirectoryBase.TAG_ORIENTATION));
    }

    @Test
    @DisplayName("compressImage корректно обрабатывает изображение с EXIF orientation = 8 (90° CCW)")
    void compressImageWithExif90Ccw() throws Exception {
        byte[] imageWithExif = createTestImageWithExifOrientation(400, 300, 8);

        byte[] compressed = service.compressImage(imageWithExif);

        assertNotNull(compressed);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(result);
        assertTrue(result.getWidth() <= MAX_WIDTH);
        assertTrue(result.getHeight() <= MAX_HEIGHT);
    }

    @Test
    @DisplayName("compressImage не ломается на изображениях без EXIF")
    void compressImageWithoutExif() throws Exception {
        byte[] imageWithoutExif = createTestImageWithoutExif(500, 400);

        byte[] compressed = service.compressImage(imageWithoutExif);

        assertNotNull(compressed);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(result);
    }

    @Test
    @DisplayName("convertToJpeg сохраняет EXIF orientation при повторном сжатии")
    void exifPreservedAfterMultipleCompression() throws Exception {
        byte[] original = createTestImageWithExifOrientation(600, 400, 6);

        byte[] firstPass = service.compressImage(original);
        byte[] secondPass = service.compressImage(firstPass);

        assertNotNull(secondPass);

        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(secondPass));
        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        assertNotNull(exifDir);
        assertEquals(6, exifDir.getInt(ExifDirectoryBase.TAG_ORIENTATION),
                "EXIF Orientation должен сохраняться при повторном сжатии");
    }

    @Test
    @DisplayName("compressImage выбрасывает IOException для неподдерживаемого формата")
    void compressImageThrowsOnUnsupportedFormat() {
        byte[] invalidData = "not an image".getBytes();

        assertThrows(IOException.class, () -> service.compressImage(invalidData));
    }

    @Test
    @DisplayName("compressImage обрабатывает пустой массив")
    void compressImageHandlesEmptyArray() {
        byte[] emptyData = new byte[0];

        assertThrows(IOException.class, () -> service.compressImage(emptyData));
    }

    @Test
    @DisplayName("compress принимает MultipartFile и делегирует compressImage")
    void compressWithMultipartFile() throws Exception {
        byte[] imageData = createTestImage(800, 600, "multipart");
        MultipartFile file = new MockMultipartFile("file", "test.jpg",
                "image/jpeg", imageData);

        byte[] compressed = service.compress(file);

        assertNotNull(compressed);
        assertTrue(compressed.length > 0);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(result);
        assertTrue(result.getWidth() <= MAX_WIDTH);
        assertTrue(result.getHeight() <= MAX_HEIGHT);
    }

    @Test
    @DisplayName("compressFromBase64 корректно обрабатывает base64 строку")
    void compressFromBase64() throws Exception {
        byte[] imageData = createTestImage(800, 600, "base64");
        String base64 = Base64.getEncoder().encodeToString(imageData);

        byte[] compressed = service.compressFromBase64(base64);

        assertNotNull(compressed);
        assertTrue(compressed.length > 0);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(result);
        assertTrue(result.getWidth() <= MAX_WIDTH);
        assertTrue(result.getHeight() <= MAX_HEIGHT);
    }

    @Test
    @DisplayName("compressFromBase64 выбрасывает исключение для невалидной base64")
    void compressFromBase64ThrowsOnInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> service.compressFromBase64("not-valid-base64!!!"));
    }

    @Test
    @DisplayName("compressFromBase64 корректно обрабатывает base64 с EXIF ориентацией")
    void compressFromBase64WithExif() throws Exception {
        byte[] imageData = createTestImageWithExifOrientation(500, 400, 6);
        String base64 = Base64.getEncoder().encodeToString(imageData);

        byte[] compressed = service.compressFromBase64(base64);

        assertNotNull(compressed);

        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(compressed));
        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        assertNotNull(exifDir, "EXIF должен сохраняться при base64 → compress");
        assertEquals(6, exifDir.getInt(ExifDirectoryBase.TAG_ORIENTATION));
    }

    @Test
    @DisplayName("Выходной JPEG читается стандартным ImageIO")
    void compressedImageIsValidJpeg() throws Exception {
        byte[] imageData = createTestImage(1000, 800, "validity");

        byte[] compressed = service.compressImage(imageData);

        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(compressed));
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        assertTrue(readers.hasNext(), "Должен быть ImageReader для формата");
        assertEquals("JPEG", readers.next().getFormatName().toUpperCase(),
                "Формат должен быть JPEG");
    }


    private byte[] createTestImage(int width, int height, String text) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(java.awt.Color.BLACK);
        g.setFont(g.getFont().deriveFont(24f));
        g.drawString(text, 50, height / 2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] createTestImageWithExifOrientation(int width, int height, int orientation) throws IOException {
        byte[] plainJpeg = createTestImageWithoutExif(width, height);
        return injectExifApp1(plainJpeg, orientation);
    }

    private byte[] createTestImageWithoutExif(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.GREEN);
        g.fillRect(0, 0, width, height);
        g.dispose();

        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam writeParam = writer.getDefaultWriteParam();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(baos));
        writer.write(null, new javax.imageio.IIOImage(img, null, null), writeParam);
        writer.dispose();

        return baos.toByteArray();
    }

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
        exif.write(new byte[]{0x45, 0x78, 0x69, 0x66, 0x00, 0x00}); // "Exif\0\0"
        exif.write(new byte[]{0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08}); // TIFF big-endian header

        // IFD0: 1 entry (orientation)
        exif.write(0x00); exif.write(0x01); // 1 entry
        exif.write(0x01); exif.write(0x12); // tag = 0x0112 (Orientation)
        exif.write(0x00); exif.write(0x03); // type = SHORT
        exif.write(0x00); exif.write(0x00); exif.write(0x00); exif.write(0x01); // count = 1
        exif.write(0x00); exif.write((byte) orientation); // value
        exif.write(0x00); exif.write(0x00); // padding
        exif.write(0x00); exif.write(0x00); exif.write(0x00); exif.write(0x00); // next IFD = 0

        int app1Len = 2 + exif.size();
        app1.write(0xFF); app1.write(0xE1);
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
}
