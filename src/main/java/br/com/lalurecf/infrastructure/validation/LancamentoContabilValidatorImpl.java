package br.com.lalurecf.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * Implementação do validador customizado para Lançamento Contábil.
 *
 * <p>Valida regras de negócio de partidas dobradas usando reflexão para acessar
 * os campos do DTO anotado.
 *
 * <p>Validações realizadas:
 *
 * <ul>
 *   <li>contaDebitoId != contaCreditoId (contas devem ser diferentes)
 *   <li>valor > 0 (valor deve ser positivo)
 * </ul>
 */
public class LancamentoContabilValidatorImpl
    implements ConstraintValidator<LancamentoContabilValidator, Object> {

  @Override
  public void initialize(LancamentoContabilValidator constraintAnnotation) {
    // Inicialização não necessária
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value == null) {
      return true; // null values são tratados por @NotNull
    }

    try {
      // Obter campos via reflexão
      Long contaDebitoId = getFieldValue(value, "contaDebitoId", Long.class);
      Long contaCreditoId = getFieldValue(value, "contaCreditoId", Long.class);
      BigDecimal valor = getFieldValue(value, "valor", BigDecimal.class);

      // Validar contas diferentes
      if (contaDebitoId != null
          && contaCreditoId != null
          && contaDebitoId.equals(contaCreditoId)) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(
                "Conta de débito e conta de crédito devem ser diferentes")
            .addConstraintViolation();
        return false;
      }

      // Validar valor positivo
      if (valor != null && valor.compareTo(BigDecimal.ZERO) <= 0) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate("Valor deve ser maior que zero")
            .addConstraintViolation();
        return false;
      }

      return true;

    } catch (Exception e) {
      // Se falhar ao acessar campos, considera válido (campos serão validados por outras
      // anotações)
      return true;
    }
  }

  /**
   * Obtém valor de um campo usando reflexão.
   *
   * @param object objeto alvo
   * @param fieldName nome do campo
   * @param fieldType tipo esperado do campo
   * @param <T> tipo genérico
   * @return valor do campo ou null se não encontrado
   */
  private <T> T getFieldValue(Object object, String fieldName, Class<T> fieldType) {
    try {
      Field field = object.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      Object fieldValue = field.get(object);
      if (fieldValue == null) {
        return null;
      }
      return fieldType.cast(fieldValue);
    } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
      return null;
    }
  }
}
