package br.com.lalurecf.infrastructure.security;

import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de autenticação JWT.
 *
 * <p>Intercepta requests, extrai token do header Authorization, valida e popula SecurityContext
 * com userId como principal para auditoria JPA.
 *
 * <p>Executado uma vez por request antes de UsernamePasswordAuthenticationFilter.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserJpaRepository userRepository;

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider, UserJpaRepository userRepository) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);

      if (jwtTokenProvider.validateToken(token)) {
        String email = jwtTokenProvider.getEmailFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token).name();

        // Buscar userId do banco APENAS AQUI (uma vez por request)
        // Isso evita StackOverflow ao fazer query dentro do AuditorAware
        Long userId =
            userRepository
                .findByEmail(email)
                .map(user -> user.getId())
                .orElse(1L); // Fallback para SYSTEM_USER_ID

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

        // IMPORTANTE: userId como principal (não email) para auditoria JPA
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userId, null, Collections.singletonList(authority));

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }
}
