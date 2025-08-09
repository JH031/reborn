package spl.reborn.s3.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spl.reborn.s3.service.S3Service;

import java.io.IOException;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class S3Controller {
    private final S3Service s3Service;

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String url = s3Service.uploadFile(file);
        return ResponseEntity.ok(url); // 200 OK + body
    }
}
