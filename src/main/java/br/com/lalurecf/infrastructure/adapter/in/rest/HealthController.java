package br.com.lalurecf.infrastructure.adapter.in.rest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller para endpoints públicos de health check e status.
 */
@RestController
@RequestMapping("/public")
public class HealthController {

  /**
   * Endpoint público para testar se a API está respondendo.
   *
   * @return status da API
   */
  @GetMapping("/ping")
  public ResponseEntity<Map<String, Object>> ping() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "OK");
    response.put("message", "LALUR V2 ECF API is running");
    response.put("timestamp", LocalDateTime.now());
    response.put("environment", "production");
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para testar CORS.
   *
   * @return mensagem de teste
   */
  @GetMapping("/cors-test")
  public ResponseEntity<Map<String, String>> corsTest() {
    Map<String, String> response = new HashMap<>();
    response.put("message", "CORS is working!");
    response.put("access", "public");
    return ResponseEntity.ok(response);
  }
}
