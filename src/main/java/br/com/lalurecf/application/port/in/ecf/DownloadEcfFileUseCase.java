package br.com.lalurecf.application.port.in.ecf;

import br.com.lalurecf.domain.model.EcfFileDownloadData;

/**
 * Port IN para download de arquivo ECF.
 *
 * <p>Busca o arquivo, verifica ownership e retorna o conteúdo em bytes (ISO-8859-1).
 */
public interface DownloadEcfFileUseCase {

  /**
   * Retorna o conteúdo do arquivo ECF para download.
   *
   * @param ecfFileId ID do arquivo ECF
   * @param companyId ID da empresa (para verificar ownership)
   * @return dados para download: bytes, fileName, fileSizeBytes
   * @throws jakarta.persistence.EntityNotFoundException se arquivo não encontrado
   * @throws org.springframework.security.access.AccessDeniedException se arquivo não pertence
   *     à empresa
   */
  EcfFileDownloadData download(Long ecfFileId, Long companyId);
}
