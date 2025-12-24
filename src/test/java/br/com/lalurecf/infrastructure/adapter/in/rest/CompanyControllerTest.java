package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.out.CnpjData;
import br.com.lalurecf.application.port.out.CnpjSearchPort;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para CompanyController.
 *
 * <p>Testa o endpoint /companies/search-cnpj/{cnpj} com Spring MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CompanyController Integration Tests")
class CompanyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CnpjSearchPort cnpjSearchPort;

  @MockBean
  private UserJpaRepository userJpaRepository;

  @Test
  @DisplayName("Should return 200 OK with CNPJ data when found")
  @WithMockUser(roles = "ADMIN")
  void shouldReturn200WhenCnpjFound() throws Exception {
    // Arrange
    String cnpj = "00000000000191";
    CnpjData cnpjData = new CnpjData(
        cnpj,
        "BANCO DO BRASIL S.A.",
        "6421200",
        "Diretor",
        "205-1"
    );
    when(cnpjSearchPort.searchByCnpj(cnpj)).thenReturn(Optional.of(cnpjData));

    // Act & Assert - usar CNPJ sem formatação no path
    mockMvc.perform(get("/companies/search-cnpj/{cnpj}", cnpj)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cnpj").value(cnpj))
        .andExpect(jsonPath("$.razaoSocial").value("BANCO DO BRASIL S.A."))
        .andExpect(jsonPath("$.cnae").value("6421200"))
        .andExpect(jsonPath("$.qualificacaoPj").value("Diretor"))
        .andExpect(jsonPath("$.naturezaJuridica").value("205-1"));

    verify(cnpjSearchPort).searchByCnpj(cnpj);
  }

  @Test
  @DisplayName("Should return 404 Not Found when CNPJ not found")
  @WithMockUser(roles = "ADMIN")
  void shouldReturn404WhenCnpjNotFound() throws Exception {
    // Arrange
    String validButNotFoundCnpj = "11222333000181"; // Valid CNPJ format and check digits
    when(cnpjSearchPort.searchByCnpj(validButNotFoundCnpj)).thenReturn(Optional.empty());

    // Act & Assert
    mockMvc.perform(get("/companies/search-cnpj/{cnpj}", validButNotFoundCnpj)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 Bad Request when CNPJ format is invalid")
  @WithMockUser(roles = "ADMIN")
  void shouldReturn400WhenCnpjInvalid() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/companies/search-cnpj/{cnpj}", "12345")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 403 Forbidden when user is not ADMIN")
  @WithMockUser(roles = "CONTADOR")
  void shouldReturn403WhenUserIsNotAdmin() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/companies/search-cnpj/{cnpj}", "00000000000191")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when user is not authenticated")
  void shouldReturn401WhenNotAuthenticated() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/companies/search-cnpj/{cnpj}", "00000000000191")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  // ==================================================================================
  // Período Contábil Tests
  // ==================================================================================

  @Test
  @DisplayName("Should update período contábil successfully")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  @org.springframework.test.context.jdbc.Sql("/reset-periodo-contabil.sql")
  @org.springframework.transaction.annotation.Transactional
  void shouldUpdatePeriodoContabilSuccessfully() throws Exception {
    // Arrange - use yesterday's date
    // @Sql resets company ID 1 período contábil to 2025-12-01 before test
    String yesterday = java.time.LocalDate.now().minusDays(1).toString();
    String requestBody = String.format("""
        {
          "novoPeriodoContabil": "%s"
        }
        """, yesterday);

    // Act & Assert
    mockMvc.perform(put("/companies/{id}/periodo-contabil", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.periodoContabilAnterior").exists())
        .andExpect(jsonPath("$.periodoContabilNovo").value(yesterday));
  }

  @Test
  @DisplayName("Should return 400 when período contábil is in the future")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldReturn400WhenPeriodoContabilIsInFuture() throws Exception {
    // Arrange
    String requestBody = """
        {
          "novoPeriodoContabil": "2099-12-31"
        }
        """;

    // Act & Assert
    mockMvc.perform(put("/companies/{id}/periodo-contabil", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when período contábil retroacts")
  @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
  void shouldReturn400WhenPeriodoContabilRetroacts() throws Exception {
    // Arrange
    String requestBody = """
        {
          "novoPeriodoContabil": "2020-01-01"
        }
        """;

    // Act & Assert
    mockMvc.perform(put("/companies/{id}/periodo-contabil", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 403 when non-ADMIN tries to update período contábil")
  @WithMockUser(roles = "CONTADOR")
  void shouldReturn403WhenNonAdminTriesToUpdatePeriodoContabil() throws Exception {
    // Arrange
    String requestBody = """
        {
          "novoPeriodoContabil": "2024-06-30"
        }
        """;

    // Act & Assert
    mockMvc.perform(put("/companies/{id}/periodo-contabil", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should get período contábil audit history successfully")
  @WithMockUser(roles = "ADMIN")
  void shouldGetPeriodoContabilAuditHistorySuccessfully() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/companies/{id}/periodo-contabil/audit", 1L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("Should return 403 when non-ADMIN tries to get audit history")
  @WithMockUser(roles = "CONTADOR")
  void shouldReturn403WhenNonAdminTriesToGetAuditHistory() throws Exception {
    // Act & Assert
    mockMvc.perform(get("/companies/{id}/periodo-contabil/audit", 1L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should return 404 when updating período contábil for non-existent company")
  @WithMockUser(roles = "ADMIN")
  void shouldReturn404WhenUpdatingPeriodoContabilForNonExistentCompany() throws Exception {
    // Arrange
    String requestBody = """
        {
          "novoPeriodoContabil": "2024-06-30"
        }
        """;

    // Act & Assert
    mockMvc.perform(put("/companies/{id}/periodo-contabil", 99999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 400 when novoPeriodoContabil is null")
  @WithMockUser(roles = "ADMIN")
  void shouldReturn400WhenNovoPeriodoContabilIsNull() throws Exception {
    // Arrange
    String requestBody = """
        {
          "novoPeriodoContabil": null
        }
        """;

    // Act & Assert
    mockMvc.perform(put("/companies/{id}/periodo-contabil", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
  }
}
