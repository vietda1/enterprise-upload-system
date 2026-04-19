package com.msb.upload.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class UploadPageResponse {
    private List<UploadResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
