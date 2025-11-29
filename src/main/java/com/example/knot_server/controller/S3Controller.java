package com.example.knot_server.controller;

import com.example.knot_server.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.knot_server.controller.dto.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

  private final S3Service s3Service;

  @PostMapping("/presign")
  public ResponseEntity<ApiResponse<Map<String, String>>> getPresignedUrl(@RequestBody Map<String, String> body) {
    String filename = body.get("filename");
    String contentType = body.get("contentType");
    String url = s3Service.generateUploadUrl(filename, contentType);
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(Map.of("uploadUrl", url)));
  }

}
