package com.enterprise.upload.service;

import com.enterprise.upload.dto.response.DatasetConfigResponse;
import java.util.List;

public interface DatasetConfigService {
    List<DatasetConfigResponse> getAllActive();
    DatasetConfigResponse getByCode(String code);
}
