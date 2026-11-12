package com.fangcun.forest.admin;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.service.CoverImageStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoverImageStorageTests {
    @TempDir Path directory;

    @Test void normalizesPixelsAndIgnoresUntrustedFilenameAndContentType() throws Exception {
        var source = new ByteArrayOutputStream();
        source.write(image("png", 16, 12));
        source.write("untrusted-trailing-payload".getBytes(StandardCharsets.UTF_8));
        var saved = storage().save(new MockMultipartFile("file", "../../outside.html", "text/html", source.toByteArray()));
        assertTrue(saved.path().matches("/images/uploads/cover-[a-f0-9]{32}\\.png"));
        assertEquals(16, saved.width()); assertEquals(12, saved.height());
        Path file = directory.resolve(Path.of(saved.path()).getFileName());
        assertEquals(16, ImageIO.read(file.toFile()).getWidth());
        assertFalse(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1).contains("untrusted-trailing-payload"));
        assertEquals(1, fileCount());
    }

    @Test void jpegUploadsReceiveIndependentNamesAndRemainReadable() throws Exception {
        var upload = new MockMultipartFile("file", "photo.jpeg", "image/jpeg", image("jpeg", 16, 12));
        var first = storage().save(upload); var second = storage().save(upload);
        assertNotEquals(first.path(), second.path());
        assertTrue(first.path().endsWith(".jpg"));
        assertEquals(12, ImageIO.read(directory.resolve(Path.of(first.path()).getFileName()).toFile()).getHeight());
        assertEquals(2, fileCount());
    }

    @Test void rejectsDisguisedSvgAndUnsupportedRasterWithoutWritingFiles() throws Exception {
        assertRejected(new MockMultipartFile("file", "cover.png", "image/png", "<svg onload='alert(1)'/>".getBytes(StandardCharsets.UTF_8)), HttpStatus.BAD_REQUEST);
        assertRejected(new MockMultipartFile("file", "cover.png", "image/png", image("gif", 16, 12)), HttpStatus.BAD_REQUEST);
        assertEquals(0, fileCount());
    }

    @Test void rejectsEmptyOversizedOrExcessiveDimensionsBeforeWritingFiles() throws Exception {
        assertRejected(new MockMultipartFile("file", new byte[0]), HttpStatus.BAD_REQUEST);
        var oversized = mock(MultipartFile.class);
        when(oversized.getSize()).thenReturn((long) CoverImageStorage.MAX_BYTES + 1);
        assertRejected(oversized, HttpStatus.PAYLOAD_TOO_LARGE);
        verify(oversized, never()).getInputStream();
        assertRejected(new MockMultipartFile("file", "wide.png", "image/png", image("png", 6001, 1)), HttpStatus.BAD_REQUEST);
        assertEquals(0, fileCount());
    }

    @Test void enforcesByteLimitEvenIfReportedFileSizeIsIncorrect() throws Exception {
        var oversized = new MockMultipartFile("file", "cover.png", "image/png", new byte[CoverImageStorage.MAX_BYTES + 1]) {
            @Override public long getSize() { return 1; }
        };
        assertRejected(oversized, HttpStatus.PAYLOAD_TOO_LARGE);
        assertEquals(0, fileCount());
    }

    private CoverImageStorage storage() { return new CoverImageStorage(directory.toString()); }
    private void assertRejected(MultipartFile file, HttpStatus status) {
        assertEquals(status, assertThrows(BusinessException.class, () -> storage().save(file)).getStatus());
    }
    private long fileCount() throws Exception { try (var files = Files.list(directory)) { return files.count(); } }
    private byte[] image(String format, int width, int height) throws Exception {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, bytes)); image.flush(); return bytes.toByteArray();
    }
}
