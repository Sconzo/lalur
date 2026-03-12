package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.infrastructure.dto.ecf.GenerateArquivoParcialRequest;
import br.com.lalurecf.infrastructure.dto.ecf.GenerateArquivoParcialResponse;

/**
 * Port IN para geração do Arquivo Parcial ECF.
 *
 * <p>Orquestra a geração do bloco M a partir dos Lançamentos da Parte B,
 * persiste o arquivo resultante e rebaixa o COMPLETE_ECF existente se necessário.
 */
public interface GenerateArquivoParcialUseCase {

  /**
   * Gera o Arquivo Parcial ECF para a empresa e ano fiscal informados.
   *
   * @param request dados da requisição (fiscalYear)
   * @param companyId ID da empresa (do contexto X-Company-Id)
   * @param generatedBy identificador do usuário autenticado
   * @return metadados da geração
   * @throws IllegalArgumentException se não existirem lançamentos ACTIVE para o ano
   */
  GenerateArquivoParcialResponse generate(
      GenerateArquivoParcialRequest request, Long companyId, String generatedBy);
}
