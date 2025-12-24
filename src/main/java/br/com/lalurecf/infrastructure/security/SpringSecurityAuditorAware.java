package br.com.lalurecf.infrastructure.security;

import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Implementação de AuditorAware para auditoria JPA.
 *
 * <p>Fornece o auditor atual (usuário autenticado) para campos @CreatedBy e @LastModifiedBy.
 * Retorna o ID do usuário autenticado ou null se não houver autenticação.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<Long> {

  private final UserJpaRepository userRepository;

  public SpringSecurityAuditorAware(UserJpaRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * ID do usuário SYSTEM (usado para operações sem autenticação).
   * Este ID é fixo e corresponde ao usuário system@lalurecf.com.br.
   */
  private static final Long SYSTEM_USER_ID = 1L;

  /**
   * Obtém o auditor atual do contexto de segurança.
   *
   * @return Optional contendo o ID do usuário autenticado ou SYSTEM_USER_ID se não autenticado
   */
  @Override
  public Optional<Long> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      return Optional.of(SYSTEM_USER_ID);
    }

    String email = authentication.getName();
    return userRepository
        .findByEmail(email)
        .map(user -> user.getId())
        .or(() -> Optional.of(SYSTEM_USER_ID)); // Fallback to SYSTEM if user not found
  }
}
