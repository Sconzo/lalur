package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ChangePasswordUseCase;
import br.com.lalurecf.application.port.out.UserRepositoryPort;
import br.com.lalurecf.domain.exception.BusinessRuleViolationException;
import br.com.lalurecf.domain.exception.InvalidCurrentPasswordException;
import br.com.lalurecf.domain.model.User;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordRequest;
import br.com.lalurecf.infrastructure.dto.auth.ChangePasswordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de troca de senha.
 *
 * <p>Implementa caso de uso de troca de senha validando senha atual e atualizando para nova senha.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChangePasswordService implements ChangePasswordUseCase {

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    log.debug("Alterando senha para usuário: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> new IllegalStateException("Usuário autenticado não encontrado no banco"));

    // Valida senha atual
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      log.warn("Tentativa de troca de senha com senha atual incorreta: {}", email);
      throw new InvalidCurrentPasswordException("Senha atual inválida");
    }

    // Valida que nova senha é diferente da atual
    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
      log.warn("Tentativa de usar mesma senha ao trocar: {}", email);
      throw new BusinessRuleViolationException("Nova senha não pode ser igual à atual");
    }

    // Atualiza senha e flag mustChangePassword
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    user.setMustChangePassword(false);
    userRepository.save(user);

    log.info("Senha alterada com sucesso para usuário: {}", email);

    return ChangePasswordResponse.builder()
        .success(true)
        .message("Senha alterada com sucesso")
        .build();
  }
}
