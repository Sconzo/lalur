package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.out.CnpjData;
import br.com.lalurecf.application.port.out.CnpjSearchPort;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
  @DisplayName("Should return 403 Forbidden when user is not authenticated")
  void shouldReturn403WhenNotAuthenticated() throws Exception {
    // Act & Assert
    // Note: Current SecurityConfig returns 403 instead of 401 when not authenticated
    // This is due to missing authentication entry point configuration
    mockMvc.perform(get("/companies/search-cnpj/{cnpj}", "00000000000191")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }
}
