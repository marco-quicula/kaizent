> **Nota de atualização (depois da conversa com o mantenedor):**
> - **P1 e P2 foram substituídos** por uma proposta única: a triagem tem três modos, `llm`, `rules` e `rules-then-llm` (`kaizent.triage.mode`). No modo `llm`, a mesma chamada de triagem devolve o assunto **e a role**, cada um de uma lista fechada, com uma descrição por role. No modo `rules`, regras declaradas no workspace mapeiam campos do card (categoria, labels, fila) para assunto e role, sem LLM. O `rules-then-llm` tenta as regras e cai para o LLM. Motivo: a regra fixa "assunto → role" ignora o tipo de problema (por exemplo, um ticket de `owners` com stack trace cai na role errada). O mantenedor gostou da proposta, mas ainda não decidiu formalmente.
> - **P5 está respondida:** "base de dados" quer dizer a base de conhecimento (documentos, FAQ, lições e, na wave 3, a base vetorial de resoluções). O acesso a dados de sistemas é futuro.
> - **Possível evolução para chatbot:** veja o catálogo de padrões.

# Revisão do fluxo do Kaizent: role, skills e base de conhecimento

Para o mantenedor ratificar ou retificar, ponto a ponto. Fontes: specs 0005 a 0011, ADRs 0010, 0011, 0012, 0016, 0017, 0021, 0022 e 0032, e o roadmap. Nada aqui foi alterado no repositório ainda.

---

## 1. O fluxo de ponta a ponta, com quem decide cada passo

Legenda de quem decide:
- **[Humano]**: configuração escrita uma vez no workspace, sem tokens;
- **[Código]**: regra determinística, sem modelo e sem tokens;
- **[LLM 1x]**: uma chamada única ao modelo, com saída estruturada, com tokens;
- **[Agente]**: o loop do modelo com ferramentas; várias chamadas, a maior parte dos tokens.

```
Ticket chega (inbox/)                                    [Código]   spec 0005
  │  vira o modelo canônico Problem (título, corpo, labels)
  ▼
Risco de injeção: SAFE ou SUSPICIOUS                     [Código]   spec 0006 AC-9, ADR-0032
  │  regras fixas (pedidos de token, comandos de shell, "ignore as instruções"...)
  ▼
TRIAGEM: assunto, tipo, idioma                           [LLM 1x]   spec 0006 AC-3
  │  entra: título, corpo, labels + LISTA FECHADA de assuntos (id, nome, descrição)
  │  sai:   {subjectId, problemType, language, rationale}
  ▼
ROLE = mapeamento assunto → role                         [Código]   spec 0006 AC-5
  │  a role cujo "subjects" contém o assunto; senão a role padrão
  │  a role traz: persona, skills permitidas, pastas da base permitidas  [Humano]
  ▼
Escolha do runtime (API ou CLI) pelo risco               [Código]   spec 0008 AC-11
  ▼
RESOLUÇÃO: o agente monta a resposta                     [Agente]   spec 0008
  │  recebe: persona + ticket + idioma + CATÁLOGO das skills (só nome e descrição)
  │          + ferramentas: search_knowledge, read_document, read_skill
  │  loop:   o modelo decide buscar / ler / ler skill → recebe o resultado → decide de novo
  │          a busca é Lucene local [Código], só nas pastas da role, sem tokens
  │  sai:    diagnóstico, resposta, confiança, citações, skills usadas, "falta informação"
  ▼
Validação das citações + confiança ajustada              [Código]   spec 0008 AC-6..8, ADR-0022
Checagem de vazamento na saída                           [Código]   spec 0008 AC-12
  ▼
Revisão humana: aprovar / editar / rejeitar              [Humano]   spec 0010
  ▼
LIÇÃO, se houve edição ou rejeição                       [LLM 1x]   spec 0011
  │  o modelo propõe um texto; um humano aprova; vira knowledge/lessons/*.md
  ▼
(próximos tickets encontram a lição na busca)
```

## 2. Onde está a "inteligência", e o que custa tokens

| Pergunta | Quem responde | Tokens |
|---|---|---|
| Quais assuntos existem e o que cada um significa? | Humano, no `kaizent-workspace.yaml` | não |
| De qual assunto é este ticket? | LLM, numa chamada única, escolhendo **da lista fechada** | sim, pouco (no EV-2: cerca de 540 de entrada e 515 de saída) |
| Qual role atende esse assunto? | Código, pelo `subjects` da role | não |
| Quais skills e quais pastas da base a role pode usar? | Humano, nos arquivos `roles/*.md` | não |
| Qual skill usar **neste** ticket? | Agente, a partir do catálogo (nome e descrição) | sim |
| O que buscar na base, e qual documento ler? | Agente decide o que buscar; a busca em si é Lucene, sem modelo | as decisões, sim; a busca, não |
| A resposta tem base? | Código: citações existem? estão nas pastas da role? | não |
| Quanto confiar? | Agente dá a confiança bruta; o código limita pelas evidências | parte |

Resumo: **o humano define o mapa** (assuntos, roles, escopo de skills e de base); **o LLM só aponta onde o ticket cai nesse mapa** (triagem); **o agente escolhe dentro do escopo permitido** (resolução).

Custo da resolução, como ordem de grandeza e não como medida: cada volta do loop reenvia o contexto acumulado. Um caso com 3 a 5 buscas e leituras deve ficar entre alguns milhares e algumas dezenas de milhares de tokens de entrada. O teto é de 20 chamadas de ferramenta por execução (`kaizent.budget.max-tool-calls`), mais o orçamento da spec 0009.

## 3. Pontos para ratificar ou retificar

### P1. A role é escolhida por código, não pelo LLM (inconsistência a corrigir)
- **Hoje:** a spec 0006 diz "determinístico: assunto → role". O roadmap (wave 1, linha "Triage") diz "o LLM classifica e **seleciona uma role**".
- **Recomendação:** ratificar o determinístico. É previsível, auditável e barato. Corrigir o texto do roadmap.

### P2. A triagem consulta a base de conhecimento?
- **Hoje:** não. A triagem decide só pelo texto do ticket e pelas descrições dos assuntos.
- **Opções:**
  - (a) manter assim;
  - (b) enriquecer os assuntos com **palavras-chave e exemplos** no `kaizent-workspace.yaml` (humano, sem tokens extras, melhora a classificação);
  - (c) antes da triagem, rodar uma busca Lucene **sem LLM** em toda a base e passar à triagem os títulos dos 3 a 5 trechos mais parecidos, como pista (poucos tokens a mais);
  - (d) dar ferramentas de busca à triagem (vira um agente: mais caro e mais lento).
- **Recomendação:** (a) já, com (b) como evolução barata. Medir a acurácia da triagem no EV da spec 0006 e só ir para (c) se ela errar. (d) não compensa.

### P3. Skills: o agente escolhe pelo catálogo (divulgação progressiva)
- **Hoje:** o agente vê só nome e descrição das skills permitidas e decide qual ler. A qualidade da **descrição** da skill é o que guia a escolha.
- **Opções:**
  - (a) manter;
  - (b) a role pode marcar skills **sempre carregadas** (por exemplo `write-customer-reply`), que entram no prompt sem o agente pedir.
- **Recomendação:** (a) no MVP, com uma regra de escrita: "a descrição da skill diz **quando** usá-la". (b) só se as avaliações mostrarem que o agente esquece skills obrigatórias.

### P4. Base de conhecimento: o agente começa "às cegas"
- **Hoje:** o agente recebe o ticket e precisa **buscar** para ver qualquer documento. Isso gasta pelo menos uma volta do loop.
- **Opções:**
  - (a) manter;
  - (b) **pré-busca**: antes de chamar o agente, o código roda a busca Lucene com o texto do ticket, só nas pastas da role, e põe os 3 a 5 melhores trechos (caminho, seção, trecho) no primeiro prompt. Sem chamada extra ao LLM; o agente pode buscar mais se quiser.
- **Recomendação:** (b). Costuma poupar voltas, e portanto tokens, e melhora as citações. Seria uma mudança pequena na spec 0008 (AC-1) e no `AgentTask`.

### P5. "Base de dados": o que exatamente?
"Base de dados" pode querer dizer duas coisas diferentes:
- **Base de conhecimento** (documentos, FAQ, problemas conhecidos, lições): é o que a wave 1 cobre, em Markdown com Lucene. A busca semântica (vetores) vem na wave 3.
- **Dados estruturados de sistemas** (banco do produto, cadastro do cliente, status de um pedido): **não** está na wave 1. A wave 2 prevê ferramentas MCP por role (por exemplo, uma consulta somente leitura) e perguntas de negócio e analíticas.
- **Pergunta:** qual dos dois você tem em mente? Se for o segundo, decidimos se algo dele precisa entrar antes da wave 2.

### P6. O modo agente via CLI (T8) e o risco do login
- **Hoje:** o plano monta as pastas da role **dentro do contêiner** da CLI, e a ferramenta lê os arquivos sozinha. O ADR-0032 exige que ela **não consiga ler o próprio login**. Na descoberta do Codex, o sandbox `read-only` dele lê qualquer arquivo do contêiner, então essa exigência provavelmente não é atendida.
- **Opções:**
  - (a) seguir o plano: descoberta da restrição de leitura, provavelmente terminando em `read-restricted: false`, ou seja, a CLI fica fora da resolução;
  - (b) antecipar da wave 2 o item "**Kaizent como servidor MCP**": a CLI roda **sem** shell e sem arquivos montados, só com as ferramentas do Kaizent (`search_knowledge`, `read_document`, `read_skill`), já limitadas à role. A restrição de leitura sai pela construção, e a CLI vê exatamente o mesmo que o runtime nativo;
  - (c) na wave 1, a resolução usa **só o runtime nativo (API)**, e a CLI fica para chamadas únicas (como hoje); o modo agente via CLI vai para a wave 2, junto com o MCP.
- **Recomendação:** (c) agora, com (b) como o desenho alvo da wave 2. É o mais simples e seguro, e não bloqueia o MVP. Implica mover a T8 da spec 0004 e a AC-4 da spec 0008 para a wave 2 e registrar um ADR.
- **Custo:** a resolução no MVP passa a exigir uma chave de API (Gemini gratuito ou outro). A CLI com assinatura continua servindo para triagem e lições.

### P7. Ticket sem assunto conhecido
- **Hoje:** vai para a role padrão.
- **Recomendação:** ratificar.

## 4. Decisões que preciso de você

1. **P1:** a role é determinística (corrigir o roadmap)? (recomendo: sim)
2. **P2:** triagem sem busca agora, com palavras-chave e exemplos nos assuntos como evolução? (recomendo: sim)
3. **P3:** skills pelo catálogo, sem "sempre carregadas" no MVP? (recomendo: sim)
4. **P4:** pré-busca Lucene no primeiro prompt do agente? (recomendo: sim)
5. **P5:** "base de dados" é a base de conhecimento ou dados de sistemas?
6. **P6:** CLI agente fica para a wave 2 (opção c), com o MCP como alvo (opção b)? (recomendo: sim)
7. **P7:** role padrão para assunto desconhecido? (recomendo: sim)

Com as respostas, registro o que mudar: roadmap; specs 0004, 0006 e 0008; um ADR para P4 e outro para P6. Depois volto ao código (porta `AgentRuntime` e fake).
