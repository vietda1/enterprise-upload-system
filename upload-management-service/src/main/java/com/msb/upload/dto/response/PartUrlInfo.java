package com.msb.upload.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartUrlInfo {
    private int partNumber;
    private String uploadUrl;
    private long startByte;
    private long endByte;
    private long partSize;
}