package uk.gov.hmcts.cp.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.api.CaseIdByCaseUrnApi;
import uk.gov.hmcts.cp.openapi.model.CaseMapperResponse;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class OpenAPISpecTest {
    @Test
    void generated_error_response_should_have_expected_fields() {
        assertThat(ErrorResponse.class).hasDeclaredFields("error", "details", "message", "timestamp", "traceId");
    }

    @Test
    void generated_case_mapper_response_should_have_expected_fields() {
        assertThat(CaseMapperResponse.class).hasDeclaredFields("caseId", "caseUrn");
    }

    @Test
    void generated_api_should_have_expected_methods() {
        assertThat(CaseIdByCaseUrnApi.PATH_GET_CASE_ID_BY_CASE_URN).isEqualTo("/urnmapper/{case_urn}");
        assertThat(CaseIdByCaseUrnApi.class).hasDeclaredMethods("getCaseIdByCaseUrn", "getRequest");
    }
}