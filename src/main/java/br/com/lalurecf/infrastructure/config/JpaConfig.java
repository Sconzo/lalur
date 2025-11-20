package br.com.lalurecf.infrastructure.config;

import br.com.lalurecf.infrastructure.security.SpringSecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuração JPA com suporte a auditoria automática.
 *
 * <p>Habilita {@literal @}EnableJpaAuditing para que campos anotados com {@literal @}CreatedDate,
 * {@literal @}LastModifiedDate, {@literal @}CreatedBy e {@literal @}LastModifiedBy sejam
 * preenchidos automaticamente.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

  /**
   * Provedor de auditor para preencher campos createdBy e updatedBy.
   *
   * @return implementação de AuditorAware que retorna email do usuário autenticado
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
    return new SpringSecurityAuditorAware();
  }
}
