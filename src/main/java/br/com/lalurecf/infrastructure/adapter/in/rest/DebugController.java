package br.com.lalurecf.infrastructure.adapter.in.rest;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller temporário para debug de CORS e configurações.
 */
@RestController
@RequestMapping("/public/debug")
@RequiredArgsConstructor
public class DebugController {

  @Value("${cors.allowed-origins:NOT_SET}")
  private String allowedOrigins;

  @Value("${server.servlet.context-path:NOT_SET}")
  private String contextPath;

  /**
   * Endpoint para verificar configurações CORS e servidor.
   *
   * @return configurações atuais
   */
  @GetMapping("/config")
  public ResponseEntity<Map<String, String>> getConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("corsAllowedOrigins", allowedOrigins);
    config.put("contextPath", contextPath);
    config.put("status", "OK");
    config.put("timestamp", String.valueOf(System.currentTimeMillis()));
    return ResponseEntity.ok(config);
  }

  /**
   * Endpoint simples para teste de CORS.
   *
   * @return mensagem de sucesso
   */
  @GetMapping("/ping")
  public ResponseEntity<Map<String, String>> ping() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "pong");
    response.put("timestamp", String.valueOf(System.currentTimeMillis()));
    return ResponseEntity.ok(response);
  }
}
