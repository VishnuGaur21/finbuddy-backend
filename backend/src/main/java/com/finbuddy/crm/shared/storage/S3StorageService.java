package com.finbuddy.crm.shared.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;

    @Override
    public String uploadFile(String path, InputStream inputStream, long contentLength, String contentType) {
        log.info("Uploading object to S3: bucket={}, path={}, size={}", s3Config.getBucketName(), path, contentLength);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(path)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        return path;
    }

    @Override
    public InputStream downloadFile(String path) {
        log.info("Downloading object from S3: bucket={}, path={}", s3Config.getBucketName(), path);
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(path)
                .build();

        ResponseInputStream<GetObjectResponse> s3Response = s3Client.getObject(getRequest);
        return s3Response;
    }

    @Override
    public void deleteFile(String path) {
        log.info("Deleting object from S3: bucket={}, path={}", s3Config.getBucketName(), path);
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(path)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    @Override
    public String generatePresignedUrl(String path, int expiryMinutes) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(path)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(getRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
