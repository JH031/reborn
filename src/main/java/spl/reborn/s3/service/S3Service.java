package spl.reborn.s3.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /** 업로드하고 퍼블릭 URL 반환 */
    public String uploadFile(MultipartFile file) throws IOException {
        String objectKey = generateFileName(file);

        try {
            ObjectMetadata metadata = buildMetadata(file);

            // 퍼블릭으로 바로 접근 가능하게 업로드 (버킷이 퍼블릭 차단이면 presigned URL 사용 필요)
            PutObjectRequest req = new PutObjectRequest(
                    bucketName, objectKey, file.getInputStream(), metadata
            ).withCannedAcl(CannedAccessControlList.PublicRead);

            s3Client.putObject(req);

            String region = s3Client.getRegionName(); // 예: ap-northeast-2
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);

        } catch (AmazonServiceException e) {
            // S3가 에러 응답을 보낸 경우
            throw new IOException("S3 error: " + e.getErrorMessage(), e);
        } catch (SdkClientException e) {
            // 네트워크 등 클라이언트 측 문제
            throw new IOException("Client error when uploading to S3", e);
        }
    }

    private ObjectMetadata buildMetadata(MultipartFile file) {
        ObjectMetadata meta = new ObjectMetadata();
        String contentType = file.getContentType();
        meta.setContentType(contentType != null ? contentType : "application/octet-stream");
        meta.setContentLength(file.getSize());
        return meta;
    }

    private String generateFileName(MultipartFile file) {
        String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
        // 원본 파일명 그대로 쓰고 URL에서만 인코딩 (S3 키는 인코딩 불필요)
        return UUID.randomUUID() + "-" + original;
    }
}
