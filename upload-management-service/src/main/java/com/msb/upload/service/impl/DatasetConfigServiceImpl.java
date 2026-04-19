package com.msb.upload.service.impl;

import com.msb.upload.dto.response.DatasetConfigResponse;
import com.msb.upload.exception.UploadNotFoundException;
import com.msb.upload.mapper.UploadMapper;
import com.msb.upload.repository.DatasetConfigRepository;
import com.msb.upload.service.DatasetConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatasetConfigServiceImpl implements DatasetConfigService {

    private final DatasetConfigRepository datasetConfigRepository;
    private final UploadMapper uploadMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DatasetConfigResponse> getAllActive() {
        return datasetConfigRepository.findByIsActiveTrue().stream()
            .map(uploadMapper::toDatasetConfigResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetConfigResponse getByCode(String code) {
        return datasetConfigRepository.findByCodeAndIsActiveTrue(code)
            .map(uploadMapper::toDatasetConfigResponse)
            .orElseThrow(() -> new UploadNotFoundException("Dataset config not found: " + code));
    }
}
