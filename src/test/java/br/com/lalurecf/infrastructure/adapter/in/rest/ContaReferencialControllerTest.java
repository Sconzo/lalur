package br.com.lalurecf.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lalurecf.util.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração para ContaReferencialController.
 *
 * <p>Valida todos os endpoints do CRUD de contas referenciais RFB, incluindo:
 *
 * <ul>
 *   <li>Criação de contas (ADMIN only)
 *   <li>Listagem com filtros e paginação (ADMIN e CONTADOR)
 *   <li>Visualização individual (ADMIN e CONTADOR)
 *   <li>Atualização de contas (ADMIN only)
 *   <li>Toggle de status (ADMIN only)
 *   <li>Controle de acesso (CONTADOR pode ler mas não escrever)
 * </ul>
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("ContaReferencialController - Testes de Integração")
class ContaReferencialControllerTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("ADMIN deve conseguir criar conta referencial")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldCreateContaReferencialAsAdmin() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoRfb": "1.01.01",
          "descricao": "Receita de Vendas",
          "anoValidade": 2024
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.codigoRfb").value("1.01.01"))
        .andExpect(jsonPath("$.descricao").value("Receita de Vendas"))
        .andExpect(jsonPath("$.anoValidade").value(2024))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("Deve retornar 400 quando código duplicado com mesmo ano de validade")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldReturn400WhenCodeAndYearAreDuplicate() throws Exception {
    // Arrange - criar primeira conta
    String requestBody =
        """
        {
          "codigoRfb": "2.01.01",
          "descricao": "Custo de Mercadorias",
          "anoValidade": 2024
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated());

    // Act & Assert - tentar criar conta com mesmo código e ano
    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Já existe conta referencial com código 2.01.01 para o ano 2024"));
  }

  @Test
  @DisplayName("ADMIN deve conseguir criar mesmo código para anos diferentes")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldAllowSameCodeForDifferentYears() throws Exception {
    // Arrange - criar conta para 2024
    String requestBody2024 =
        """
        {
          "codigoRfb": "3.01.01",
          "descricao": "Despesa Administrativa 2024",
          "anoValidade": 2024
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2024))
        .andExpect(status().isCreated());

    // Act & Assert - criar mesmo código para 2025
    String requestBody2025 =
        """
        {
          "codigoRfb": "3.01.01",
          "descricao": "Despesa Administrativa 2025",
          "anoValidade": 2025
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody2025))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoRfb").value("3.01.01"))
        .andExpect(jsonPath("$.anoValidade").value(2025));
  }

  @Test
  @DisplayName("CONTADOR deve conseguir listar contas referenciais")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowContadorToListContas() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/v1/conta-referencial"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("CONTADOR deve conseguir visualizar conta referencial")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldAllowContadorToViewConta() throws Exception {
    // Arrange - criar conta como ADMIN
    String requestBody =
        """
        {
          "codigoRfb": "4.01.01",
          "descricao": "Outras Despesas",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .with(request -> {
                      request.setRemoteUser("admin@test.com");
                      return request;
                    })
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@test.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId =
        objectMapper.readTree(responseBody).get("id").asLong();

    // Act & Assert - CONTADOR consegue visualizar
    mockMvc
        .perform(get("/api/v1/conta-referencial/" + contaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contaId))
        .andExpect(jsonPath("$.codigoRfb").value("4.01.01"));
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 ao tentar criar conta referencial")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToCreate() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "codigoRfb": "5.01.01",
          "descricao": "Receita Financeira",
          "anoValidade": 2024
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 ao tentar editar conta referencial")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToUpdate() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "descricao": "Nova Descrição",
          "anoValidade": 2025
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            put("/api/v1/conta-referencial/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CONTADOR deve receber 403 ao tentar alternar status")
  @WithMockUser(username = "contador@test.com", roles = "CONTADOR")
  void shouldReturn403WhenContadorTriesToToggleStatus() throws Exception {
    // Arrange
    String requestBody =
        """
        {
          "status": "INACTIVE"
        }
        """;

    // Act & Assert
    mockMvc
        .perform(
            patch("/api/v1/conta-referencial/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("ADMIN deve conseguir editar conta referencial (sem alterar codigoRfb)")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldUpdateContaReferencialAsAdmin() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoRfb": "6.01.01",
          "descricao": "Descrição Original",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act - editar conta
    String updateRequest =
        """
        {
          "descricao": "Descrição Atualizada",
          "anoValidade": 2025
        }
        """;

    mockMvc
        .perform(
            put("/api/v1/conta-referencial/" + contaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contaId))
        .andExpect(jsonPath("$.codigoRfb").value("6.01.01")) // Código não muda
        .andExpect(jsonPath("$.descricao").value("Descrição Atualizada"))
        .andExpect(jsonPath("$.anoValidade").value(2025));
  }

  @Test
  @DisplayName("ADMIN deve conseguir alternar status da conta referencial")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldToggleStatusAsAdmin() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoRfb": "7.01.01",
          "descricao": "Conta para teste de status",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Act - alternar para INACTIVE
    String toggleRequest =
        """
        {
          "status": "INACTIVE"
        }
        """;

    mockMvc
        .perform(
            patch("/api/v1/conta-referencial/" + contaId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.newStatus").value("INACTIVE"));

    // Assert - verificar que status foi alterado
    mockMvc
        .perform(get("/api/v1/conta-referencial/" + contaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  @DisplayName("Deve filtrar contas por ano de validade")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldFilterByAnoValidade() throws Exception {
    // Arrange - criar contas para anos diferentes
    String conta2024 =
        """
        {
          "codigoRfb": "8.01.01",
          "descricao": "Conta 2024",
          "anoValidade": 2024
        }
        """;

    String conta2025 =
        """
        {
          "codigoRfb": "8.01.02",
          "descricao": "Conta 2025",
          "anoValidade": 2025
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(conta2024))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(conta2025))
        .andExpect(status().isCreated());

    // Act & Assert - filtrar por 2024
    mockMvc
        .perform(get("/api/v1/conta-referencial?ano_validade=2024"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[*].anoValidade").value(hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  @DisplayName("Deve buscar contas por termo em codigoRfb ou descricao")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldSearchByCodeOrDescription() throws Exception {
    // Arrange - criar conta
    String createRequest =
        """
        {
          "codigoRfb": "9.01.ESPECIAL",
          "descricao": "Conta Especial de Teste",
          "anoValidade": 2024
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/conta-referencial")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest))
        .andExpect(status().isCreated());

    // Act & Assert - buscar por "ESPECIAL" (presente no código)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=ESPECIAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].codigoRfb").value("9.01.ESPECIAL"));

    // Act & Assert - buscar por "Especial" (presente na descrição, case insensitive)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=Especial"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].descricao").value("Conta Especial de Teste"));
  }

  @Test
  @DisplayName("Deve retornar apenas contas ACTIVE por padrão")
  @WithMockUser(username = "admin@test.com", roles = "ADMIN")
  void shouldReturnOnlyActiveByDefault() throws Exception {
    // Arrange - criar conta e inativar
    String createRequest =
        """
        {
          "codigoRfb": "10.01.01",
          "descricao": "Conta para teste de filtro de status",
          "anoValidade": 2024
        }
        """;

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/conta-referencial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String responseBody = createResult.getResponse().getContentAsString();
    Long contaId = objectMapper.readTree(responseBody).get("id").asLong();

    // Inativar conta
    String toggleRequest =
        """
        {
          "status": "INACTIVE"
        }
        """;

    mockMvc
        .perform(
            patch("/api/v1/conta-referencial/" + contaId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleRequest))
        .andExpect(status().isOk());

    // Act & Assert - listar sem include_inactive (não deve aparecer)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=10.01.01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());

    // Act & Assert - listar com include_inactive=true (deve aparecer)
    mockMvc
        .perform(get("/api/v1/conta-referencial?search=10.01.01&include_inactive=true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(contaId))
        .andExpect(jsonPath("$.content[0].status").value("INACTIVE"));
  }
}
