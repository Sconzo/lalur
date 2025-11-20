Introdução

Este documento descreve a arquitetura geral do projeto Sistema LALUR V2 ECF, incluindo sistemas backend, serviços compartilhados e aspectos não específicos de interface de usuário. Seu objetivo primário é servir como blueprint arquitetural orientador para desenvolvimento assistido por IA, garantindo consistência e aderência aos padrões e tecnologias escolhidos.

**Relação com Arquitetura Frontend:**

Este é um sistema **API backend-only** para o MVP. Um documento de Arquitetura Frontend poderá ser criado futuramente quando uma interface de usuário for necessária. As escolhas de tech stack documentadas na Seção 3 deste documento são definitivas para todo o projeto, incluindo quaisquer componentes frontend futuros.

### Decisão: Starter Template ou Projeto Existente

**Decisão:** Sem starter template.

**Justificativa:** A arquitetura hexagonal (Ports & Adapters) requer estrutura customizada de pastas e organização de código que não se alinha bem com starters convencionais (Spring Initializr gera estrutura flat). A estrutura será criada manualmente conforme definido na Seção 10 (Source Tree).

### Change Log

| Data | Versão | Descrição | Autor |
|------|--------|-----------|-------|
| 2025-01-19 | 1.0 | Versão inicial da arquitetura | Winston (Architect Agent) |

---

