package br.com.lalurecf.infrastructure.exception;

import br.com.lalurecf.domain.exception.BusinessRuleViolationException;
import br.com.lalurecf.domain.exception.InvalidCredentialsException;
import br.com.lalurecf.domain.exception.InvalidCurrentPasswordException;
import br.com.lalurecf.domain.exception.MustChangePasswordException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler global de exceções.
 *
 * <p>Captura exceções lançadas pelos controllers e retorna respostas HTTP padronizadas.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  public static final String BAD_REQUEST = "Bad Request";

  /**
   * Handler para InvalidCredentialsException.
   *
   * @param ex exceção lançada
   * @return response 401 Unauthorized
   */
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
    log.warn("Credenciais inválidas: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Unauthorized")
            .message(ex.getMessage())
            .build();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  /**
   * Handler para MustChangePasswordException.
   *
   * @param ex exceção lançada
   * @return response 400 Bad Request
   */
  @ExceptionHandler(MustChangePasswordException.class)
  public ResponseEntity<ErrorResponse> handleMustChangePassword(MustChangePasswordException ex) {
    log.warn("Tentativa de login com mustChangePassword=true: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Password Change Required")
            .message(ex.getMessage())
            .build();
    return ResponseEntity.badRequest().body(error);
  }

  /**
   * Handler para erros de validação Bean Validation.
   *
   * @param ex exceção de validação
   * @return response 400 Bad Request com detalhes dos erros
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Error")
            .message("Erro de validação")
            .validationErrors(errors)
            .build();
    return ResponseEntity.badRequest().body(error);
  }

  /**
   * Handler para InvalidCurrentPasswordException.
   *
   * @param ex exceção lançada
   * @return response 400 Bad Request
   */
  @ExceptionHandler(InvalidCurrentPasswordException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCurrentPassword(
      InvalidCurrentPasswordException ex) {
    log.warn("Senha atual inválida: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(BAD_REQUEST)
            .message(ex.getMessage())
            .build();
    return ResponseEntity.badRequest().body(error);
  }

  /**
   * Handler para BusinessRuleViolationException.
   *
   * @param ex exceção lançada
   * @return response 400 Bad Request
   */
  @ExceptionHandler(BusinessRuleViolationException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(
      BusinessRuleViolationException ex) {
    log.warn("Violação de regra de negócio: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(BAD_REQUEST)
            .message(ex.getMessage())
            .build();
    return ResponseEntity.badRequest().body(error);
  }

  /**
   * Handler para ResourceNotFoundException.
   *
   * @param ex exceção lançada
   * @return response 404 Not Found
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
    log.warn("Recurso não encontrado: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  /**
   * Handler para PeriodoContabilViolationException.
   *
   * @param ex exceção lançada
   * @return response 400 Bad Request
   */
  @ExceptionHandler(PeriodoContabilViolationException.class)
  public ResponseEntity<ErrorResponse> handlePeriodoContabilViolation(
      PeriodoContabilViolationException ex) {
    log.warn("Violação de Período Contábil: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Período Contábil Violation")
            .message(ex.getMessage())
            .build();
    return ResponseEntity.badRequest().body(error);
  }

  /**
   * Handler para IllegalArgumentException.
   *
   * @param ex exceção lançada
   * @return response 400 Bad Request
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Argumento inválido: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(BAD_REQUEST)
            .message(ex.getMessage())
            .build();
    return ResponseEntity.badRequest().body(error);
  }

  /**
   * Handler para EntityNotFoundException (JPA).
   *
   * @param ex exceção lançada
   * @return response 404 Not Found
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
    log.warn("Entidade não encontrada: {}", ex.getMessage());
    ErrorResponse error =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }
}
