package com.fangcun.forest.admin.service;

import com.fangcun.forest.admin.common.BusinessException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CoverImageStorage {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 12_000_000;
    private static final Logger log = LoggerFactory.getLogger(CoverImageStorage.class);
    private final Path directory;

    public CoverImageStorage(@Value("${forest.storage.upload-dir}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public record StoredCover(String path, int width, int height) { }

    public StoredCover save(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("请选择一张 JPG 或 PNG 图片。");
        if (file.getSize() > MAX_BYTES) throw tooLarge();
        BufferedImage decoded;
        String format;
        try (InputStream source = file.getInputStream()) {
            byte[] bytes = source.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw tooLarge();
            try (var imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw invalid("文件不是可读取的 JPG 或 PNG 图片。");
                ImageReader reader = readers.next();
                try {
                    format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) {
                        throw invalid("封面仅支持 JPG 或 PNG 图片。");
                    }
                    reader.setInput(imageInput, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 6000 || height > 6000 || (long) width * height > MAX_PIXELS) {
                        throw invalid("图片尺寸过大，请使用单边不超过 6000 像素、总像素不超过 1200 万的图片。");
                    }
                    decoded = reader.read(0);
                    if (decoded == null) throw invalid("图片内容无法读取，请重新导出后上传。");
                } finally { reader.dispose(); }
            }
        } catch (IOException exception) {
            throw invalid("图片已损坏或无法读取，请重新选择。");
        }

        boolean png = format.equals("png");
        int width = decoded.getWidth(), height = decoded.getHeight();
        BufferedImage normalized = new BufferedImage(width, height, png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalized.createGraphics();
        try { graphics.drawImage(decoded, 0, 0, null); }
        finally { graphics.dispose(); decoded.flush(); }

        // Ignore the original filename and re-encode pixels, dropping source metadata.
        String filename = "cover-" + UUID.randomUUID().toString().replace("-", "") + (png ? ".png" : ".jpg");
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, ".cover-", ".tmp");
            if (!ImageIO.write(normalized, png ? "png" : "jpeg", temporary.toFile())) {
                throw new IOException("No image writer available");
            }
            if (Files.size(temporary) > MAX_BYTES) throw invalid("图片转换后超过 5MB，请缩小图片后重试。");
            Path destination = directory.resolve(filename);
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, destination); }
            temporary = null;
            return new StoredCover("/images/uploads/" + filename, width, height);
        } catch (IOException exception) {
            log.error("Cover image could not be stored", exception);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "图片暂时无法保存，请检查上传目录权限后重试。");
        } finally {
            normalized.flush();
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException exception) { log.warn("Temporary cover image cleanup failed", exception); }
            }
        }
    }

    private BusinessException invalid(String message) { return new BusinessException(HttpStatus.BAD_REQUEST, message); }
    private BusinessException tooLarge() { return new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "图片不能超过 5MB。"); }
}
