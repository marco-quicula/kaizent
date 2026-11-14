# Briefing para a revisão externa do Kaizent

> **Material temporário de revisão, não é para merge.** Esta pasta (`review/`) existe só nesta branch, para uma revisão externa. Está em português por ser material de trabalho; tudo o que for para a `main` é em inglês (`AGENTS.md`).

## O projeto

**Kaizent** (*kaizen* + *agent*) é uma ferramenta agêntica de resolução de problemas, open source (Apache-2.0). Ela:

- recebe problemas (primeiro, tickets de suporte e relatos de bug);
- classifica cada um e monta uma role, skills e conhecimento;
- propõe uma resolução;
- recebe o veredito de um humano;
- aprende com ele, num ciclo PDCA por resolução.

Stack: Java 25, Spring Boot 4, Spring AI, PostgreSQL 18 e um console em React. O desenvolvimento é guiado por specs (ADR-0015), com ADRs para as decisões. O domínio de demonstração é o Spring PetClinic, com dados sintéticos.

O projeto é construído com dois assistentes de IA. O Claude (Anthropic) escreve as specs e o código. O Codex (OpenAI) revisa os PRs e executa as validações locais. Um PR só é mergeado quando os dois concordam e o mantenedor autoriza.

## Estado atual

A base desta branch é a `main` em `ffaec7d`.

- **Wave 1 (MVP), em andamento.** As specs 0001 a 0012 estão aprovadas. As specs 0001, 0002 e 0003 estão prontas. A spec 0004 (acesso a LLMs) está quase pronta.
- **Spec 0004, já entregue:**
  - os adaptadores de API (Gemini, OpenAI, Anthropic, Ollama), com retry;
  - a saída estruturada com reparo;
  - o uso e o custo por chamada;
  - o executor de CLI em contêiner (ADR-0030);
  - o preset do Codex CLI, validado com 12 tickets reais;
  - o comando `llm-check`;
  - o fake com os marcadores de erro, timeout e saída inválida.
- **Spec 0004, em aberto:**
  - a porta `AgentRuntime` e o modo agente via CLI (T8);
  - o fake do runtime (T9);
  - os presets do Claude Code e do Antigravity CLI (T10, em descoberta no PR #46). O Gemini CLI saiu do escopo, porque o Antigravity o substitui no plano gratuito.

## Decisões já tomadas pelo mantenedor (contexto, não em revisão)

- **ADR-0031:** o login de uma CLI fica num volume Docker. Só o script interno da imagem o move, dentro do contêiner; a aplicação nunca o lê. O Claude Code, porém, deve usar um token de assinatura por variável de ambiente, uma exceção a registrar.
- **ADR-0032:** defesa contra prompt injection em quatro camadas:
  1. menor privilégio;
  2. detector determinístico;
  3. política que manda os tickets suspeitos para o runtime de API;
  4. checagem de saída.
  Um preset de CLI que não consegue restringir leitura nunca resolve ticket. A política de "reter o ticket até um humano decidir" foi adiada para a wave 4.
- **Modo CLI com assinatura:** só para uso pessoal e local. Um cluster compartilhado usa apenas API.

## Foco da revisão

1. **Revisão do fluxo** (`review/external-2026-11/03-revisao-fluxo.md`): quem decide cada passo (código, LLM numa chamada única ou agente), os pontos P1 a P7 e os modos de triagem.
2. **Catálogo de padrões** (`review/external-2026-11/04-catalogo-padroes.md`): cerca de 60 padrões de IA e agentes, cada um com status (✅ adotado, 🗓️ planejado, ❌ dispensado, ❓ em aberto, 💬 condicionado a um canal de chat), um mapa dos 13 conceitos de um vídeo sobre agentes e uma seção sobre uma possível evolução para chatbot.
3. **As decisões abertas**, no fim de `docs/adr/README.md`, seção "Open decisions".
4. **A definição das waves**, em `docs/roadmap.md`.

**Importante:** as recomendações dos documentos 03 e 04 são **propostas do Claude**, não decisões. O mantenedor quer uma opinião **independente**: discorde onde for o caso.

## Documentos do repositório a ler (em inglês)

- `AGENTS.md`, `docs/vision.md`, `docs/roadmap.md`, `docs/architecture.md`
- `docs/specs/README.md` e as specs `docs/specs/0004-*` a `docs/specs/0012-*` (`spec.md`, `plan.md` e `tasks.md` de cada uma)
- `docs/adr/README.md`, com o índice e as decisões abertas
- Os ADRs 0010, 0011, 0012, 0016, 0017, 0021, 0022, 0030, 0031 e 0032 (`docs/adr/00NN-*.md`)
