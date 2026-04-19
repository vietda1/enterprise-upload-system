package com.msb.upload.service;

import com.msb.upload.dto.request.CompletedPartInfo;
import com.msb.upload.exception.StorageException;
import com.msb.upload.service.impl.MinioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinioService")
class MinioServiceTest {

    @Mock S3Client   s3Client;
    @Mock S3Presigner s3Presigner;

    @InjectMocks MinioServiceImpl minioService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(minioService, "bucketName", "test-bucket");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generatePresignedPutUrl
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generatePresignedPutUrl")
    class GeneratePresignedPutUrl {

        @Test
        @DisplayName("Happy path → returns presigned URL string")
        void returns_presigned_url() throws Exception {
            PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
            when(presigned.url()).thenReturn(
                URI.create("https://test-bucket.s3.amazonaws.com/key?X-Amz-Signature=abc").toURL());
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presigned);

            String url = minioService.generatePresignedPutUrl("uploads/key.csv", 15);

            assertThat(url).contains("X-Amz-Signature");
            assertThat(url).startsWith("https://");
        }

        @Test
        @DisplayName("S3Presigner throws → StorageException wraps the error")
        void presigner_throws_wraps_in_storage_exception() {
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("presigner unavailable"));

            assertThatThrownBy(() ->
                minioService.generatePresignedPutUrl("uploads/key.csv", 15))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to generate upload URL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generatePresignedGetUrl
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generatePresignedGetUrl")
    class GeneratePresignedGetUrl {

        @Test
        @DisplayName("Happy path → returns presigned GET URL")
        void returns_presigned_get_url() throws Exception {
            PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
            when(presigned.url()).thenReturn(
                URI.create("https://test-bucket.s3.amazonaws.com/key?X-Amz-Signature=xyz").toURL());
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presigned);

            String url = minioService.generatePresignedGetUrl("uploads/key.csv", 60);

            assertThat(url).contains("X-Amz-Signature");
        }

        @Test
        @DisplayName("S3Presigner throws → StorageException")
        void presigner_throws_wraps_storage_exception() {
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("network error"));

            assertThatThrownBy(() ->
                minioService.generatePresignedGetUrl("uploads/key.csv", 60))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to generate download URL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // objectExists
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("objectExists")
    class ObjectExists {

        @Test
        @DisplayName("headObject succeeds → returns true")
        void object_found_returns_true() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(1024L).build());

            assertThat(minioService.objectExists("uploads/data.csv")).isTrue();
        }

        @Test
        @DisplayName("headObject throws NoSuchKeyException → returns false")
        void object_not_found_returns_false() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

            assertThat(minioService.objectExists("uploads/missing.csv")).isFalse();
        }

        @Test
        @DisplayName("headObject throws non-NoSuchKey exception → StorageException")
        void other_exception_wraps_storage_exception() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 connection refused"));

            assertThatThrownBy(() -> minioService.objectExists("uploads/key.csv"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Error checking object existence");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteObject
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteObject")
    class DeleteObject {

        @Test
        @DisplayName("Happy path → deleteObject called on S3Client")
        void happy_path_calls_s3_delete() {
            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

            assertThatNoException().isThrownBy(() ->
                minioService.deleteObject("uploads/data.csv"));

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("S3Client throws → StorageException wraps the error")
        void s3_throws_wraps_storage_exception() {
            when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 error"));

            assertThatThrownBy(() -> minioService.deleteObject("uploads/data.csv"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to delete object");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getObjectSize
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getObjectSize")
    class GetObjectSize {

        @Test
        @DisplayName("Returns content-length from headObject response")
        void returns_content_length() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(5_242_880L).build());

            long size = minioService.getObjectSize("uploads/data.csv");

            assertThat(size).isEqualTo(5_242_880L);
        }

        @Test
        @DisplayName("S3Client throws → StorageException")
        void s3_throws_wraps_storage_exception() {
            when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(new RuntimeException("Timeout"));

            assertThatThrownBy(() -> minioService.getObjectSize("uploads/data.csv"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to get object size");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // initiateMultipartUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("initiateMultipartUpload")
    class InitiateMultipartUpload {

        @Test
        @DisplayName("Happy path → returns S3 uploadId")
        void returns_s3_upload_id() {
            when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder()
                    .uploadId("s3-multipart-id-abc").build());

            String id = minioService.initiateMultipartUpload("uploads/large.csv");

            assertThat(id).isEqualTo("s3-multipart-id-abc");
        }

        @Test
        @DisplayName("S3Client throws → StorageException")
        void s3_throws_wraps_storage_exception() {
            when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenThrow(new RuntimeException("S3 unavailable"));

            assertThatThrownBy(() -> minioService.initiateMultipartUpload("uploads/large.csv"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to initiate multipart upload");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generatePresignedPartUrl
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generatePresignedPartUrl")
    class GeneratePresignedPartUrl {

        @Test
        @DisplayName("Happy path → returns presigned part URL")
        void returns_presigned_part_url() throws Exception {
            PresignedUploadPartRequest presigned = mock(PresignedUploadPartRequest.class);
            when(presigned.url()).thenReturn(
                URI.create("https://test-bucket.s3.amazonaws.com/key?partNumber=1&X-Amz-Signature=ppp").toURL());
            when(s3Presigner.presignUploadPart(any(UploadPartPresignRequest.class)))
                .thenReturn(presigned);

            String url = minioService.generatePresignedPartUrl(
                "uploads/large.csv", "s3-upload-id", 1, 15);

            assertThat(url).contains("partNumber=1");
        }

        @Test
        @DisplayName("S3Presigner throws → StorageException")
        void presigner_throws_wraps_storage_exception() {
            when(s3Presigner.presignUploadPart(any(UploadPartPresignRequest.class)))
                .thenThrow(new RuntimeException("presigner error"));

            assertThatThrownBy(() ->
                minioService.generatePresignedPartUrl("uploads/large.csv", "s3-id", 1, 15))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to generate presigned part URL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // completeMultipartUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeMultipartUpload")
    class CompleteMultipartUpload {

        @Test
        @DisplayName("Happy path → completeMultipartUpload called on S3Client")
        void happy_path_calls_s3_complete() {
            when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(CompleteMultipartUploadResponse.builder().build());

            List<CompletedPartInfo> parts = List.of(
                partInfo(1, "etag-1"), partInfo(2, "etag-2"));

            assertThatNoException().isThrownBy(() ->
                minioService.completeMultipartUpload("uploads/large.csv", "s3-upload-id", parts));

            verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("Parts mapped correctly → request contains correct partNumbers and ETags")
        void parts_mapped_correctly() {
            when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(CompleteMultipartUploadResponse.builder().build());

            minioService.completeMultipartUpload("uploads/file.csv", "s3-id",
                List.of(partInfo(1, "etag-abc"), partInfo(2, "etag-def")));

            verify(s3Client).completeMultipartUpload(
                argThat((CompleteMultipartUploadRequest req) ->
                    req.multipartUpload().parts().size() == 2
                    && req.multipartUpload().parts().get(0).partNumber() == 1
                    && "etag-abc".equals(req.multipartUpload().parts().get(0).eTag())
                    && req.multipartUpload().parts().get(1).partNumber() == 2
                    && "etag-def".equals(req.multipartUpload().parts().get(1).eTag())));
        }

        @Test
        @DisplayName("S3Client throws → StorageException")
        void s3_throws_wraps_storage_exception() {
            when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(new RuntimeException("Multipart error"));

            assertThatThrownBy(() ->
                minioService.completeMultipartUpload("uploads/file.csv", "s3-id", List.of()))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to complete multipart upload");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // abortMultipartUpload
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("abortMultipartUpload")
    class AbortMultipartUpload {

        @Test
        @DisplayName("Happy path → abortMultipartUpload called on S3Client")
        void happy_path_calls_s3_abort() {
            when(s3Client.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenReturn(AbortMultipartUploadResponse.builder().build());

            assertThatNoException().isThrownBy(() ->
                minioService.abortMultipartUpload("uploads/large.csv", "s3-upload-id"));

            verify(s3Client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        }

        @Test
        @DisplayName("S3Client throws → StorageException")
        void s3_throws_wraps_storage_exception() {
            when(s3Client.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenThrow(new RuntimeException("Abort failed"));

            assertThatThrownBy(() ->
                minioService.abortMultipartUpload("uploads/large.csv", "s3-upload-id"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Failed to abort multipart upload");
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private CompletedPartInfo partInfo(int partNumber, String etag) {
        CompletedPartInfo p = new CompletedPartInfo();
        p.setPartNumber(partNumber);
        p.setEtag(etag);
        return p;
    }
}
