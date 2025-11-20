# Documentação de Arquitetura - LALUR V2 ECF

Este diretório contém a documentação arquitetural do Sistema LALUR V2 ECF organizada em seções para facilitar leitura e manutenção.

## Estrutura de Arquivos

| Arquivo | Seção | Conteúdo |
|---------|-------|----------|
| `01-Introdução.md` | Introdução | Visão geral, objetivos, changelog |
| `02-Arquitetura-de-Alto-Nível.md` | Arquitetura de Alto Nível | Resumo técnico, diagramas, padrões |
| `03-Stack-Tecnológico.md` | Stack Tecnológico | Tecnologias, versões, justificativas |
| `04-Modelos-de-Dados.md` | Modelos de Dados | Entities, atributos, relacionamentos |
| `05-Componentes.md` | Componentes | Bounded contexts, services, use cases |
| `06-APIs-Externas.md` | APIs Externas | Integrações externas (BrasilAPI, ReceitaWS) |
| `07-Workflows-Principais.md` | Workflows Principais | Fluxos de negócio end-to-end |
| `08-Especificação-da-API-REST-OpenAPI-3.0.md` | Especificação da API REST | Endpoints, DTOs, OpenAPI spec |
| `09-Database-Schema.md` | Database Schema | DDL, constraints, índices |
| `10-Source-Tree-Estrutura-de-Pastas.md` | Source Tree | Estrutura de pastas, organização código |
| `11-Infraestrutura-e-Deployment.md` | Infraestrutura e Deployment | Docker, CI/CD, deployment |
| `12-Estratégia-de-Tratamento-de-Erros.md` | Estratégia de Tratamento de Erros | Exception handling, error codes |
| `13-Padrões-de-Código-Coding-Standards.md` | Padrões de Código | Code style, conventions |
| `14-Estratégia-e-Padrões-de-Testes.md` | Estratégia e Padrões de Testes | Test strategy, coverage |
| `15-Segurança.md` | Segurança | Auth, authorization, security practices |
| `16-Relatório-de-Validação-da-Arquitetura.md` | Relatório de Validação | Validação arquitetural |
| `17-Próximos-Passos.md` | Próximos Passos | Roadmap técnico |

## Architecture Decision Records (ADRs)

Mudanças arquiteturais significativas são documentadas como ADRs neste diretório com prefixo `adr-NNN-`:

- **Formato:** `adr-NNN-titulo-descritivo.md`
- **Exemplo:** `adr-001-simplificacao-modelo-dados.md`

## Documento Consolidado

O arquivo `docs/architecture.md` (raiz) contém a versão consolidada de todas as seções para referência rápida.

## Como Navegar

1. **Leitura inicial:** Comece por `01-Introdução.md` e `02-Arquitetura-de-Alto-Nível.md`
2. **Implementação:** Consulte `03-Stack-Tecnológico.md`, `04-Modelos-de-Dados.md`, e `10-Source-Tree-Estrutura-de-Pastas.md`
3. **APIs:** Veja `08-Especificação-da-API-REST-OpenAPI-3.0.md`
4. **Mudanças:** Consulte ADRs para entender decisões arquiteturais

## Manutenção

- **Atualizações:** Edite os arquivos individuais e depois regenere `docs/architecture.md` consolidado
- **Versionamento:** Mantenha changelog em `01-Introdução.md`
- **Decisões importantes:** Crie novo ADR ao invés de editar arquitetura diretamente
