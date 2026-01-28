package br.com.lalurecf.infrastructure.dto.taxparameter;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.infrastructure.dto.FilterDropdown;
import java.util.List;

/**
 * DTO para agrupar parâmetros tributários por tipo com sua natureza.
 */
public record TaxParameterTypeGroup(ParameterNature nature, List<FilterDropdown> parameters) {
}
