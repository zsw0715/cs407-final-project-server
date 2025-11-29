package com.example.knot_server.service;

public interface S3Service {
  /**
   * 生成上传 URL
   * 这个 URL 是临时的，用于客户端上传文件到 S3
   * @param key
   * @param contentType
   * @return
   */
  public String generateUploadUrl(String key, String contentType);

}
