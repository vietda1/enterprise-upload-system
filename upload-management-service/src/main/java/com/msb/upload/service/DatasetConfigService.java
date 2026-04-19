package com.msb.upload.service;

import com.msb.upload.dto.response.DatasetConfigResponse;
import java.util.List;

public interface DatasetConfigService {
    List<DatasetConfigResponse> getAllActive();
    DatasetConfigResponse getByCode(String code);
}
