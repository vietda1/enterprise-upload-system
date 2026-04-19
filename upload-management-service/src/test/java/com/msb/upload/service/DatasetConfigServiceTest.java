package com.msb.upload.service;

import com.msb.upload.dto.response.DatasetConfigResponse;
import com.msb.upload.exception.UploadNotFoundException;
import com.msb.upload.mapper.UploadMapper;
import com.msb.upload.model.DatasetConfig;
import com.msb.upload.repository.DatasetConfigRepository;
import com.msb.upload.service.impl.DatasetConfigServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatasetConfigService")
class DatasetConfigServiceTest {

    @Mock DatasetConfigRepository datasetConfigRepository;
    @Mock UploadMapper            uploadMapper;

    @InjectMocks DatasetConfigServiceImpl datasetConfigService;

    // ── Factories ──────────────────────────────────────────────────────────────

    private DatasetConfig config(String code) {
        return DatasetConfig.builder().code(code).name(code + " Data").isActive(true).build();
    }

    private DatasetConfigResponse resp(String code) {
        return DatasetConfigResponse.builder()
            .code(code).name(code + " Data")
            .tables(List.of())
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllActive
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllActive")
    class GetAllActive {

        @Test
        @DisplayName("Repository returns 2 configs → mapped list of 2 returned")
        void returns_mapped_list() {
            DatasetConfig fin  = config("FINANCIAL");
            DatasetConfig core = config("CORE_BANKING");
            when(datasetConfigRepository.findByIsActiveTrue()).thenReturn(List.of(fin, core));
            when(uploadMapper.toDatasetConfigResponse(fin)).thenReturn(resp("FINANCIAL"));
            when(uploadMapper.toDatasetConfigResponse(core)).thenReturn(resp("CORE_BANKING"));

            List<DatasetConfigResponse> result = datasetConfigService.getAllActive();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(DatasetConfigResponse::getCode)
                .containsExactly("FINANCIAL", "CORE_BANKING");
        }

        @Test
        @DisplayName("Empty repository → empty list, mapper never called")
        void empty_repository_returns_empty_list() {
            when(datasetConfigRepository.findByIsActiveTrue()).thenReturn(List.of());

            List<DatasetConfigResponse> result = datasetConfigService.getAllActive();

            assertThat(result).isEmpty();
            verify(uploadMapper, never()).toDatasetConfigResponse(any());
        }

        @Test
        @DisplayName("Single config → list of one element")
        void single_config_returns_single_element_list() {
            DatasetConfig cfg = config("RISK");
            when(datasetConfigRepository.findByIsActiveTrue()).thenReturn(List.of(cfg));
            when(uploadMapper.toDatasetConfigResponse(cfg)).thenReturn(resp("RISK"));

            List<DatasetConfigResponse> result = datasetConfigService.getAllActive();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("RISK");
        }

        @Test
        @DisplayName("Mapper is called once per config in repository")
        void mapper_called_for_each_config() {
            when(datasetConfigRepository.findByIsActiveTrue())
                .thenReturn(List.of(config("A"), config("B"), config("C")));
            when(uploadMapper.toDatasetConfigResponse(any())).thenReturn(resp("X"));

            datasetConfigService.getAllActive();

            verify(uploadMapper, times(3)).toDatasetConfigResponse(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getByCode
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByCode")
    class GetByCode {

        @Test
        @DisplayName("Known active code → returns mapped response")
        void known_code_returns_response() {
            DatasetConfig cfg = config("FINANCIAL");
            when(datasetConfigRepository.findByCodeAndIsActiveTrue("FINANCIAL"))
                .thenReturn(Optional.of(cfg));
            when(uploadMapper.toDatasetConfigResponse(cfg)).thenReturn(resp("FINANCIAL"));

            DatasetConfigResponse result = datasetConfigService.getByCode("FINANCIAL");

            assertThat(result.getCode()).isEqualTo("FINANCIAL");
            assertThat(result.getName()).isEqualTo("FINANCIAL Data");
        }

        @Test
        @DisplayName("Unknown code → UploadNotFoundException with code in message")
        void unknown_code_throws_not_found() {
            when(datasetConfigRepository.findByCodeAndIsActiveTrue("UNKNOWN"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> datasetConfigService.getByCode("UNKNOWN"))
                .isInstanceOf(UploadNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        }

        @Test
        @DisplayName("Inactive config (findByCodeAndIsActiveTrue returns empty) → UploadNotFoundException")
        void inactive_config_throws_not_found() {
            when(datasetConfigRepository.findByCodeAndIsActiveTrue("INACTIVE_CODE"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> datasetConfigService.getByCode("INACTIVE_CODE"))
                .isInstanceOf(UploadNotFoundException.class);
        }

        @Test
        @DisplayName("Correct repository method called with exact code")
        void repository_called_with_exact_code() {
            DatasetConfig cfg = config("CORE_BANKING");
            when(datasetConfigRepository.findByCodeAndIsActiveTrue("CORE_BANKING"))
                .thenReturn(Optional.of(cfg));
            when(uploadMapper.toDatasetConfigResponse(any())).thenReturn(resp("CORE_BANKING"));

            datasetConfigService.getByCode("CORE_BANKING");

            verify(datasetConfigRepository).findByCodeAndIsActiveTrue("CORE_BANKING");
            verify(datasetConfigRepository, never()).findByIsActiveTrue();
        }
    }
}
