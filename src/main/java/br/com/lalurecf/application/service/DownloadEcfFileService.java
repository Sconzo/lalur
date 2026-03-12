package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.DownloadEcfFileUseCase;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.domain.model.EcfFileDownloadData;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por preparar o download de arquivos ECF.
 *
 * <p>Busca o EcfFile, verifica ownership e converte o conteúdo String
 * para byte[] usando encoding ISO-8859-1 (padrão SPED ECF).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadEcfFileService implements DownloadEcfFileUseCase {

  private final EcfFileRepositoryPort ecfFileRepositoryPort;

  @Override
  @Transactional(readOnly = true)
  public EcfFileDownloadData download(Long ecfFileId, Long companyId) {
    log.info("Download EcfFile: id={}, companyId={}", ecfFileId, companyId);

    EcfFile ecfFile = ecfFileRepositoryPort.findById(ecfFileId)
        .orElseThrow(() -> new EntityNotFoundException(
            "EcfFile não encontrado: " + ecfFileId));

    if (!ecfFile.getCompanyId().equals(companyId)) {
      throw new AccessDeniedException("Arquivo ECF não pertence à empresa informada");
    }

    byte[] bytes = ecfFile.getContent().getBytes(StandardCharsets.ISO_8859_1);

    return new EcfFileDownloadData(bytes, ecfFile.getFileName(), bytes.length);
  }
}
