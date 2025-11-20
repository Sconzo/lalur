package br.com.lalurecf.infrastructure.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Implementação de AuditorAware para auditoria JPA.
 *
 * <p>Fornece o auditor atual (usuário autenticado) para campos @CreatedBy e @LastModifiedBy.
 * Retorna o email do usuário autenticado (authentication.getName()) ou "system" se não houver
 * autenticação.
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

  /**
   * Obtém o auditor atual do contexto de segurança.
   *
   * @return Optional contendo o email do usuário autenticado ou "system"
   */
  @Override
  public Optional<String> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      return Optional.of("system");
    }

    return Optional.of(authentication.getName());
  }
}
