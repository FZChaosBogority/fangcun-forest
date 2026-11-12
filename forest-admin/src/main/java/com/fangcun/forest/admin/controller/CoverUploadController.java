package com.fangcun.forest.admin.controller;

import com.fangcun.forest.admin.common.BusinessException;
import com.fangcun.forest.admin.service.CoverImageStorage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestController
public class CoverUploadController {
    private final CoverImageStorage images;
    public CoverUploadController(CoverImageStorage images) { this.images = images; }

    @PostMapping(path = "/media/covers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CoverImageStorage.StoredCover> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(images.save(file));
    }

    public record UploadError(String message) { }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<UploadError> invalidImage(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new UploadError(exception.getMessage()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<UploadError> tooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new UploadError("图片不能超过 5MB。"));
    }
    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ResponseEntity<UploadError> malformed(Exception exception) {
        return ResponseEntity.badRequest().body(new UploadError("没有收到有效图片，请重新选择后上传。"));
    }
}
