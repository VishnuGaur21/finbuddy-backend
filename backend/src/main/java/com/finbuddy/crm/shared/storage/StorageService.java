package com.finbuddy.crm.shared.storage;

import java.io.InputStream;

/**
 * Storage service abstraction for S3-compatible object storage (AWS S3, MinIO, Ceph).
 */
public interface StorageService {

    String uploadFile(String path, InputStream inputStream, long contentLength, String contentType);

    InputStream downloadFile(String path);

    void deleteFile(String path);

    String generatePresignedUrl(String path, int expiryMinutes);
}
