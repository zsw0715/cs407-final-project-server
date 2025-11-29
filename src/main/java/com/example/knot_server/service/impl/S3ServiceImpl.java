package com.example.knot_server.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.knot_server.service.S3Service;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

  private final S3Presigner presigner;

  @Value("${aws.bucket}")
  private String bucket;

  @Override
  public String generateUploadUrl(String key, String contentType) {
    PutObjectRequest objectRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(contentType)
        .build();

    PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
        .putObjectRequest(objectRequest)
        .signatureDuration(Duration.ofMinutes(5)));

    return presignedRequest.url().toString();
  }

}
