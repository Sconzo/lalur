package br.com.lalurecf.infrastructure.dto.auth;

import br.com.lalurecf.domain.enums.UserRole;
import lombok.Builder;
import lombok.Data;

/**
 * DTO para resposta de login.
 *
 * <p>Contém tokens JWT (access e refresh) e dados do usuário autenticado.
 */
@Data
@Builder
public class LoginResponse {
  private String accessToken;
  private String refreshToken;
  private String email;
  private String firstName;
  private String lastName;
  private UserRole role;
  private Boolean mustChangePassword;
}
