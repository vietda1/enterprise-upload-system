package com.msb.upload.service;

import com.msb.upload.repository.UploadRepository;
import com.msb.upload.service.impl.UploadSchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadSchedulerService")
class UploadSchedulerServiceTest {

    @Mock UploadRepository uploadRepository;

    @InjectMocks UploadSchedulerService schedulerService;

    @Test
    @DisplayName("expireStaleUploads → calls repository.expireStaleUploads with a non-null timestamp")
    void expire_calls_repository_with_current_time() {
        when(uploadRepository.expireStaleUploads(any())).thenReturn(0);

        schedulerService.expireStaleUploads();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(uploadRepository).expireStaleUploads(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        assertThat(captor.getValue()).isBefore(LocalDateTime.now().plusSeconds(5));
    }

    @Test
    @DisplayName("Repository returns 0 expired uploads → no exception thrown")
    void zero_stale_uploads_is_no_exception() {
        when(uploadRepository.expireStaleUploads(any())).thenReturn(0);

        assertThatNoException().isThrownBy(() -> schedulerService.expireStaleUploads());
    }

    @Test
    @DisplayName("Repository returns 5 expired uploads → method completes normally")
    void multiple_stale_uploads_completes_normally() {
        when(uploadRepository.expireStaleUploads(any())).thenReturn(5);

        assertThatNoException().isThrownBy(() -> schedulerService.expireStaleUploads());
        verify(uploadRepository, times(1)).expireStaleUploads(any());
    }

    @Test
    @DisplayName("expireStaleUploads called twice → repository called exactly twice")
    void multiple_invocations_each_call_repository() {
        when(uploadRepository.expireStaleUploads(any())).thenReturn(0);

        schedulerService.expireStaleUploads();
        schedulerService.expireStaleUploads();

        verify(uploadRepository, times(2)).expireStaleUploads(any());
    }

    @Test
    @DisplayName("Repository throws exception → exception propagates (not swallowed)")
    void repository_exception_propagates() {
        when(uploadRepository.expireStaleUploads(any()))
            .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> schedulerService.expireStaleUploads())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DB connection lost");
    }
}
