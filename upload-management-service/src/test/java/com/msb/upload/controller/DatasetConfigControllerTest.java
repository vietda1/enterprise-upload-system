package com.msb.upload.controller;

import com.msb.upload.config.SecurityConfig;
import com.msb.upload.dto.response.DatasetConfigResponse;
import com.msb.upload.exception.UploadNotFoundException;
import com.msb.upload.security.JwtAuthenticationFilter;
import com.msb.upload.service.DatasetConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import java.util.List;

import static com.msb.upload.helper.TestJwtHelper.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DatasetConfigController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@MockBean(JpaMetamodelMappingContext.class)
@TestPropertySource(properties = {
    "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
})
@DisplayName("DatasetConfigController")
class DatasetConfigControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean DatasetConfigService datasetConfigService;

    private static final String BASE = "/api/v1/dataset-configs";

    private DatasetConfigResponse fakeConfig(String code) {
        return DatasetConfigResponse.builder()
            .id(1)
            .code(code)
            .name("Financial Data")
            .maxFileSizeMb(100)
            .requiresApproval(false)
            .tables(List.of())
            .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/dataset-configs
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /")
    class GetAll {

        @Test
        @DisplayName("No token → 200 (public endpoint)")
        void no_token_returns_200() throws Exception {
            when(datasetConfigService.getAllActive()).thenReturn(List.of());
            mockMvc.perform(get(BASE))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Valid token → 200 with list")
        void valid_returns_list() throws Exception {
            when(datasetConfigService.getAllActive())
                .thenReturn(List.of(fakeConfig("FINANCIAL"), fakeConfig("CORE_BANKING")));

            mockMvc.perform(get(BASE)
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].code").value("FINANCIAL"));
        }

        @Test
        @DisplayName("Empty list → 200 with empty array")
        void empty_list_returns_200() throws Exception {
            when(datasetConfigService.getAllActive()).thenReturn(List.of());

            mockMvc.perform(get(BASE)
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/dataset-configs/{code}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /{code}")
    class GetByCode {

        @Test
        @DisplayName("No token → 401")
        void no_token_returns_401() throws Exception {
            mockMvc.perform(get(BASE + "/FINANCIAL"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Existing code → 200 with config")
        void existing_code_returns_200() throws Exception {
            when(datasetConfigService.getByCode("FINANCIAL"))
                .thenReturn(fakeConfig("FINANCIAL"));

            mockMvc.perform(get(BASE + "/FINANCIAL")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("FINANCIAL"))
                .andExpect(jsonPath("$.data.name").value("Financial Data"));
        }

        @Test
        @DisplayName("Unknown code → 404")
        void unknown_code_returns_404() throws Exception {
            when(datasetConfigService.getByCode("UNKNOWN"))
                .thenThrow(new UploadNotFoundException("Dataset config not found: UNKNOWN"));

            mockMvc.perform(get(BASE + "/UNKNOWN")
                    .header("Authorization", bearer(userToken())))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin can also retrieve configs")
        void admin_returns_200() throws Exception {
            when(datasetConfigService.getByCode("CORE_BANKING"))
                .thenReturn(fakeConfig("CORE_BANKING"));

            mockMvc.perform(get(BASE + "/CORE_BANKING")
                    .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("CORE_BANKING"));
        }
    }
}