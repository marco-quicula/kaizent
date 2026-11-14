# Catálogo de padrões de IA e agentes: Kaizent (rascunho para revisão)

Objetivo: nenhum padrão conhecido fica de fora por omissão. Cada um tem um status e um motivo, e a dispensa é sempre uma decisão registrada.

**Status:**
- ✅ **Adotado:** já está numa spec ou ADR da wave 1, ou já implementado.
- 🗓️ **Planejado:** previsto no roadmap para uma wave seguinte.
- ❌ **Dispensado:** decidido não usar, com motivo.
- ❓ **Em aberto:** nunca foi discutido; precisa da sua decisão (traz a minha recomendação).
- 💬 **Condicionado ao canal de chat:** não se aplica a ticket, mas passa a valer se o Kaizent ganhar um canal conversacional (chatbot). Não é dispensa definitiva.

As referências apontam para specs, ADRs e as **decisões abertas** do índice de ADRs (`docs/adr/README.md`, seção "Open decisions"). "❓ registrado" quer dizer que o tema já é uma decisão aberta, ainda sem resposta. "❓" sozinho quer dizer que nunca foi tratado.

---

## Mapa do vídeo (os 13 conceitos citados) → onde estão no Kaizent

As linhas do catálogo marcadas com 🎬N correspondem ao item N do vídeo.

| # | Conceito do vídeo | Onde entra no Kaizent | Status |
|---|---|---|---|
| 1 | Janela de contexto (histórico em toda chamada) | Não é chat: cada execução começa do zero. A memória entre tickets são as lições. Dentro do loop, o contexto cresce e tem limites | ✅ parcial, ❓ (seção 2) |
| 2 | RAG (buscar trechos e pôr no prompt) | O agente busca por ferramenta (✅). A pré-busca no primeiro prompt é a proposta P4 (❓) | ✅ + ❓ |
| 3 | Embeddings | Wave 3 (pgvector). No MVP, a busca é por palavra (Lucene) | 🗓️ |
| 4 | Banco vetorial | Wave 3, atrás da mesma porta `KnowledgeSource`; inclui as resoluções aprovadas | 🗓️ |
| 5 | Tool calling | `search_knowledge`, `read_document`, `read_skill` (spec 0008) | ✅ |
| 6 | Agente (loop: chamar, ler, decidir) | Só na resolução (ADR-0017), com teto de chamadas | ✅ |
| 7 | Structured output (schema) | Schema gerado do record, validação e um reparo (spec 0004 AC-2) | ✅ |
| 8 | Streaming | Não se aplica a ticket; o console recebe o progresso por SSE. Volta a valer num canal de chat | 💬 |
| 9 | Guardrails (entrada e saída) | ADR-0032, quatro camadas: menor privilégio, detecção de injeção, política, checagem de saída | ✅ |
| 10 | Prompt caching | Hoje não tratado; regra barata: parte fixa no início do prompt | ❓ |
| 11 | Fallback (tentar de novo e trocar de modelo) | Retry ✅ (spec 0004 AC-4); troca de modelo ❓ | ✅ + ❓ |
| 12 | Evals | Eval runner com checagens determinísticas ✅; LLM como juiz ❓; regressão automática na wave 4 🗓️ | ✅ + ❓ + 🗓️ |
| 13 | Observabilidade (passos, prompt, ferramentas, tokens, custo) | Trace por execução, uso e custo (specs 0004 e 0009); hash do prompt registrado; OpenTelemetry ❓ | ✅ + ❓ |

Conclusão: dos 13, **o Kaizent já cobre 9 por decisão registrada** (5, 6, 7 e 9 inteiros; 1, 2, 11, 12 e 13 em parte). Embeddings e banco vetorial (3 e 4) estão planejados para a wave 3, e streaming (8) fica condicionado a um canal de chat. Os pontos **em aberto** que o vídeo reforça são prompt caching (10), fallback de modelo (11), LLM como juiz (12), pré-busca (2) e o limite de contexto por ferramenta (1).

---

## 1. Conhecimento e recuperação (RAG)

| Padrão | O que é | Status | Onde / motivo |
|---|---|---|---|
| RAG "agêntico" (busca como ferramenta) 🎬2 | O agente decide quando e o que buscar, por ferramentas | ✅ | Spec 0008, ADR-0011: `search_knowledge`, `read_document` |
| RAG clássico (pré-recuperação) 🎬2 | O código busca antes e põe os trechos no prompt | ❓ | Proposta P4 da revisão do fluxo: pré-busca Lucene no primeiro prompt. Recomendo adotar, combinado com o agêntico |
| Busca lexical (BM25) | Busca por palavras, Lucene | ✅ | ADR-0021 |
| Embeddings e vetores 🎬3 | Busca por significado | 🗓️ wave 3 | Roadmap: pgvector atrás da mesma porta; o modelo de embedding e o chunking são a **decisão aberta nº 4**. Motivo do adiamento: o MVP precisa ser simples e auditável, e a base é pequena (ADR-0011) |
| Busca híbrida (lexical + vetorial) 🎬4 | Combina as duas | 🗓️ wave 3 | Roadmap |
| Base vetorial de resoluções aprovadas 🎬4 | Indexa tickets resolvidos para casos parecidos | 🗓️ wave 3 | Roadmap: "indexing of resolved runs and approved lessons" |
| Chunking por seção | Divide documentos por título de seção | ✅ | Spec 0007 AC-4, ADR-0021 |
| Chunking semântico ou com sobreposição | Divide por sentido ou com janelas que se sobrepõem | ❌ (provisório) | Seções de Markdown já são unidades naturais; rever na wave 3 com os vetores |
| Filtro por metadados | Restringe a busca por pasta ou tags | ✅ (parcial) | Pastas por role (spec 0007 AC-5). Filtro por tags: ❓, recomendo deixar para a wave 3 |
| Reranking (reordenação com outro modelo) | Um modelo reordena os resultados da busca | ❓ | Recomendo dispensar no MVP (custo e latência); reavaliar na wave 3 com métricas de recuperação |
| Reescrita ou expansão da consulta (incl. HyDE) | O LLM reescreve a busca antes de executá-la | ❓ | Na prática, o agente já reformula ao chamar `search_knowledge`. Recomendo dispensar como etapa separada |
| Contextual retrieval | Prefixar cada trecho com o contexto do documento | ❓ | Recomendo dispensar no MVP; considerar na wave 3 com os vetores |
| GraphRAG / grafo de conhecimento | Entidades e relações extraídas | ❌ | Custo de construção e manutenção alto para uma base pequena; sem caso de uso claro |
| Contexto longo (base inteira no prompt) | Mandar tudo e deixar o modelo achar | ❌ | Custo por chamada e perda de citações precisas; não escala com as lições |
| Cache semântico de respostas | Reaproveitar a resposta de perguntas parecidas | ❌ (provisório) | Toda resposta passa por revisão humana; risco de responder errado o que só parece igual. Reavaliar na wave 4 (autonomia) |

## 2. Padrões de agente e orquestração

| Padrão | O que é | Status | Onde / motivo |
|---|---|---|---|
| Gestão da janela de contexto 🎬1 | O que acompanha cada chamada; histórico; o que fazer quando o contexto cresce | ✅ (parcial) / ❓ | **Kaizent não é um chat:** cada execução começa do zero, e a memória entre tickets são as lições (spec 0011). Dentro do loop do agente, o contexto cresce a cada ferramenta: hoje é limitado pelo teto de chamadas (spec 0008 AC-3), pelo `limit ≤ 10` da busca e pela leitura por seção. ❓ Recomendo definir também um **tamanho máximo por resultado de ferramenta** (cortar documentos longos). ❓ Wave 2: tickets reais têm **histórico de respostas** (thread do Movidesk); decidir quanto dele entra no prompt |
| Tool calling / ReAct 🎬5 | O modelo alterna raciocínio e uso de ferramentas | ✅ | Spec 0008 (runtime nativo com Spring AI) |
| Workflow com etapas fixas (prompt chaining) 🎬6 | Pipeline de etapas determinadas pelo código | ✅ | ADR-0017: só a resolução é um loop de agente |
| Router / classificador | Uma etapa decide o caminho | ✅ | Triagem (spec 0006); em revisão: os modos `llm`, `rules` e `rules-then-llm` |
| Saída estruturada com schema e reparo 🎬7 | JSON validado, com uma tentativa de correção | ✅ | Spec 0004 AC-2 |
| Divulgação progressiva de skills | O agente vê o catálogo e carrega a skill só quando precisa | ✅ | ADR-0010, spec 0008 |
| Plan-and-execute | Planeja os passos e depois executa | ❌ (provisório) | ADR-0017: o Embabel não foi adotado por ora; os tickets de suporte não precisam de planos longos. Reavaliar nas waves 2 e 5 (incidentes) |
| Multiagente / orquestrador e trabalhadores | Vários agentes cooperando | ❌ (provisório) | Uma role por ticket basta no MVP (ADR-0017 cita "several cooperating roles" como gatilho futuro) |
| Reflexão / autocrítica | O modelo revisa a própria resposta antes de entregar | ❓ | Custa uma ou mais chamadas a mais. Recomendo dispensar no MVP: a revisão humana e as regras de confiança cumprem esse papel; medir depois |
| Avaliador-otimizador | Um modelo avalia e outro melhora, em loop | ❓ | Mesmo motivo do anterior. Recomendo dispensar no MVP |
| Self-consistency / votação | Várias respostas e escolha da mais frequente | ❌ | Multiplica o custo; pouco ganho para respostas em texto livre |
| Humano no loop | Toda proposta passa por um humano | ✅ | ADR-0012 |
| Autonomia gradual | Níveis por assunto, com envio automático | 🗓️ wave 4 | ADR-0012 |
| MCP como cliente (ferramentas externas por role) | Acesso a sistemas por ferramentas padronizadas | 🗓️ wave 2 | Roadmap |
| Kaizent como servidor MCP | As ferramentas do Kaizent oferecidas às CLIs | 🗓️ wave 2 | Roadmap e **decisão aberta nº 2**; candidato a desenho alvo do modo agente via CLI (P6) |
| A2A (protocolo entre agentes) | Agentes de sistemas diferentes conversando | ❓ | Recomendo dispensar por ora: não há outro agente para conversar |
| Execução de código pelo agente (sandbox) | O agente roda código para responder | ❌ | Fora do escopo de suporte; amplia a superfície de ataque (ADR-0032) |

## 3. Avaliação e qualidade (evals)

| Padrão | O que é | Status | Onde / motivo |
|---|---|---|---|
| Conjunto de avaliação offline 🎬12 | Casos com resultado esperado, rodados sob demanda | ✅ | Spec 0003 (eval runner), ADR-0020 |
| Checagens determinísticas | `mentionsAll`, `mentionsNone`, idioma, citação válida | ✅ | Spec 0003; spec 0008 (citações) |
| LLM como juiz 🎬12 | Um modelo avalia a qualidade da resposta | ❓ | **Lacuna real:** os relatórios do #38 mostraram que as checagens por palavra deixam passar recurso inventado. Recomendo adotar como checagem **opcional** do eval runner, com um modelo barato e um rubric versionado |
| Avaliação de regressão 🎬12 | Rodar o conjunto a cada mudança de prompt ou skill | 🗓️ wave 4 | Roadmap ("evaluation harness built from recorded runs") |
| Métricas de recuperação (recall@k etc.) | Medir se a busca acha o documento certo | ❓ | Recomendo adotar de forma barata: o caso de avaliação já diz qual documento deveria ser citado (EV-2 da spec 0008) |
| Feedback humano como métrica | Taxa de aprovação, edição e rejeição | ✅ | Specs 0010 e 0012 |
| Calibração de confiança | Confiança declarada contra o veredito real | 🗓️ wave 4 | ADR-0022 |
| Testes A/B de prompt ou modelo | Duas versões em paralelo | ❌ (provisório) | Volume baixo no MVP; o eval offline cobre a comparação |
| CI sem LLM real | Fakes na CI, LLM real só em evals | ✅ | ADR-0020 |

## 4. Segurança e guardrails

| Padrão | O que é | Status | Onde / motivo |
|---|---|---|---|
| Menor privilégio 🎬9 | O agente só acessa o que a tarefa exige | ✅ | ADR-0032 (camada 1), ADR-0030 |
| Guardrail de entrada (detecção de injeção) 🎬9 | Regras sobre o texto do ticket | ✅ | ADR-0032 (camada 2) |
| Guardrail de saída (vazamento) 🎬9 | Redação de tokens, chaves, caminhos e URLs | ✅ | ADR-0032 (camada 4) |
| Sandbox / contêiner por chamada | Isolamento das CLIs | ✅ | ADR-0030 |
| **PII: dados pessoais antes de ir ao modelo** | Mascarar e-mail, CPF, telefone etc. do ticket e dos traces | ❓ registrado | **Decisão aberta nº 1** do índice de ADRs, prazo: antes do primeiro conector real (wave 2). Recomendo: decidir junto com a retenção |
| Retenção e exclusão de dados | Quanto tempo guardar tickets, traces e prompts | ❓ registrado | **Decisão aberta nº 8** (traces), hoje com prazo na wave 5. Recomendo antecipar para junto da nº 1 (wave 2), porque tickets reais trazem dados pessoais |
| Moderação de conteúdo | Filtrar conteúdo ofensivo | ❌ (provisório) | Suporte interno, com revisão humana; reavaliar com o envio automático (wave 4) |
| Orçamento por execução | Limite de chamadas, tokens e custo | ✅ | Spec 0009, spec 0008 AC-3 |
| Interruptor de emergência (kill switch) | Desligar o envio automático na hora | 🗓️ wave 4 | Só faz sentido com autonomia; ❓ registrar como requisito da wave 4 |

## 5. Operação (LLMOps)

| Padrão | O que é | Status | Onde / motivo |
|---|---|---|---|
| Gateway neutro de provedor | Trocar de provedor por configuração | ✅ | ADR-0007, ADR-0016 |
| Retry com backoff 🎬11 | Novas tentativas em erros transitórios | ✅ | Spec 0004 AC-4 |
| Fallback de modelo 🎬11 | Cair para outro modelo se o primeiro falhar | ❓ registrado | **Decisão aberta nº 18**, sem prazo, já com os pontos em aberto (quais erros disparam, orçamento único de tentativas por execução, registro de cada tentativa, evals com o fallback desligado). Recomendo agendar para a wave 2: útil com o plano gratuito, mas não é pequeno como eu tinha dito |
| Roteamento por custo ou dificuldade | Modelo barato para casos fáceis | ✅ (parcial) / ❓ registrado | Um perfil por passo (spec 0004 AC-1). Escolha por problema e escalada para um modelo mais forte: **decisão aberta nº 17**, sem prazo |
| Versionamento de prompts | Prompts em arquivos, com hash registrado | ✅ | AGENTS.md, specs 0006 e 0008 |
| Rastreamento (tracing) por execução 🎬13 | Cada chamada, ferramenta e custo registrados | ✅ | Spec 0009 |
| OpenTelemetry (convenções GenAI) 🎬13 | Exportar os traces num padrão aberto | ❓ | Recomendo planejar para a wave 5 (operações); o trace próprio cobre o MVP |
| Contabilidade de custo e tokens 🎬13 | Uso e custo por chamada | ✅ | Spec 0004 AC-5 |
| Cache de prompt do provedor 🎬10 | Desconto em prefixos repetidos | ❓ | Recomendo adotar sem esforço: deixar a parte fixa (persona e catálogo) no início do prompt. Registrar como regra de escrita de prompt |
| Temperatura e determinismo | Fixar parâmetros para reprodutibilidade | ❓ | Recomendo fixar a temperatura por passo na configuração e registrá-la no trace |
| Fixar a versão do modelo | Alias contra versão específica | ❓ (já notado) | O #38 registrou que o uso guarda o alias, não a versão resolvida. Recomendo registrar a versão quando o provedor informar |
| Streaming de resposta 🎬8 | Mostrar a resposta enquanto é gerada | 💬 | No ticket, a resolução roda em segundo plano, então ninguém fica esperando a resposta na tela. A necessidade que o vídeo descreve ("o usuário acha que travou") é atendida de outro jeito: o console recebe o **progresso** da execução por SSE (status e passos, specs 0009 e 0010). Num canal de chat, passa a ser necessário |

## 6. Aprendizado contínuo

| Padrão | O que é | Status | Onde / motivo |
|---|---|---|---|
| Lições a partir de edições e rejeições | Memória escrita e revisada por humanos | ✅ | Spec 0011 |
| Memória de longo prazo / episódica | Lembrar de interações anteriores | ✅ (via lições) | As lições são a memória. Memória automática, sem revisão: ❌ (risco de aprender errado) |
| Few-shot dinâmico (exemplos de casos aprovados) | Pôr no prompt 1 ou 2 resoluções aprovadas parecidas | ❓ | Recomendo planejar com a base vetorial (wave 3): é o uso mais direto das "resoluções" que você citou |
| Evolução de skills (proposta como PR) | O sistema sugere melhorias nas skills | 🗓️ wave 2 | ADR-0010, roadmap |
| Fine-tuning / destilação | Treinar um modelo com os dados | 🗓️ wave 6 | Roadmap |
| RLHF / DPO a partir dos vereditos | Treino por preferência | ❌ (provisório) | Exige volume e infraestrutura; reavaliar com o fine-tuning (wave 6) |
| Aprendizado ativo | Pedir rótulo humano nos casos mais incertos | ✅ (implícito) | A banda de confiança na fila de revisão já prioriza; nada a mais no MVP |

---

## Se o Kaizent evoluir para chatbot (canal conversacional)

A estrutura interna (porta de entrada, triagem, roles, skills, base, agente, revisão) serve a um chat. O que muda é o canal, e com ele alguns padrões passam a valer:

| Padrão | Ticket (hoje) | Chat (futuro) |
|---|---|---|
| Streaming 🎬8 | Não se aplica | **Necessário:** a resposta aparece enquanto é gerada |
| Janela de contexto / histórico 🎬1 | Cada execução começa do zero | **Necessário:** memória da conversa, com resumo ou corte quando cresce |
| Revisão humana antes de responder | Obrigatória (ADR-0012) | **Inviável em tempo real.** Depende da autonomia da wave 4: só assuntos com nível alto respondem sozinhos, e o resto vai para um humano |
| Orçamento de latência | Minutos são aceitáveis | Segundos: menos voltas do loop; a pré-busca (P4) e o cache de prompt ganham peso |
| Moderação de conteúdo | Dispensada (uso interno, com revisão) | **Necessária:** o usuário final fala direto com o sistema |
| Cache semântico | Dispensado | Reavaliar: perguntas repetidas são comuns em chat |
| Guardrails | Quatro camadas (ADR-0032) | As mesmas, e a checagem de saída passa a bloquear em vez de só sinalizar, porque não há revisor no meio |
| Passagem para humano (handoff) | O ticket já tem humano | **Necessária:** transferir a conversa com o contexto |

**Proposta:** registrar no roadmap o "canal conversacional" como **evolução candidata, sem wave definida**, com a dependência explícita da autonomia da wave 4, e marcar no catálogo os padrões 💬. Assim a decisão de não fazer agora fica registrada, sem fechar a porta.

---

## Lacunas que merecem decisão logo

1. **LLM como juiz nos evals:** as checagens por palavra não pegam invenção.
2. **PII e retenção de dados** (decisões abertas nº 1 e nº 8): antes da wave 2 (tickets reais).
3. **Pré-busca no primeiro prompt:** P4 da revisão do fluxo.
4. **Fallback de modelo** (decisão aberta nº 18): agendar; é maior do que parece.
5. **Temperatura fixa, versão do modelo e ordem do prompt para cache:** três regras baratas de operação.
6. **Few-shot dinâmico com resoluções aprovadas:** planejar com a wave 3.
7. **Limite de tamanho por resultado de ferramenta** (janela de contexto, vídeo 🎬1): barato e evita que um documento longo encha o contexto do agente.
8. **Histórico de respostas do ticket** (wave 2): quanto da conversa do help desk entra no prompt.

## Proposta de onde isso fica no repositório

- Um documento `docs/patterns.md`, em inglês: o catálogo com status, motivo e link para a spec ou o ADR, revisado a cada wave. É o "por que não usamos".
- Cada **dispensa relevante** e cada **adoção nova** ganha um ADR curto, ou entra num ADR já existente quando for o caso, seguindo a regra do projeto.
- O índice de ADRs e o roadmap apontam para o catálogo.
