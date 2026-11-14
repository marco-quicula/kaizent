Checkout realizado na branch `claude/gallant-mccarthy-fx2m8p`, no commit `a2709529c5424f9c09433e3ae58a68cea49c1a83`. Li o briefing, os documentos relacionados e, por último, os documentos 03 e 04. A árvore de trabalho permanece limpa. Não executei aplicação, testes ou chamadas a modelos; não fiz commit, push, PR ou comentário no GitHub.

**Minha avaliação:** a arquitetura tem uma base coerente, mas os contratos ainda não garantem o fechamento seguro do PDCA completo. As prioridades são compatibilizar segurança com rastreamento, definir orçamento em cada chamada efetiva e tornar a publicação de lições recuperável. Acrescentar padrões de IA vem depois disso.

### A. Tabela de achados

A coluna **evidência** apresenta fatos documentados. **Problema** e **sugestão** apresentam minha avaliação. A confiança indica a sustentação documental do achado; não representa comprovação de uma falha em execução.

| id | severidade | documento e seção | evidência | problema | sugestão | confiança |
|---|---|---|---|---|---|---|
| F01 | **P1** | [0009/spec — AC-2 e AC-4](/workspace/kaizent/docs/specs/0009-trace-usage-and-budget/spec.md:30); [0008/spec — AC-12](/workspace/kaizent/docs/specs/0008-resolution-and-proposal/spec.md:88) | A 0009 exige resposta bruta e eventos CLI não mapeados; a 0008 exige remover vazamentos de todo texto produzido pelo modelo antes de armazená-lo ou mostrá-lo. | Um implementador pode sanitizar a proposta e ainda persistir o mesmo segredo no envelope bruto, nos eventos ou numa resposta inválida. Os contratos não explicitam a precedência em todos esses caminhos. | Definir uma barreira única de sanitização anterior à persistência, incluindo respostas inválidas, reparos, erros e eventos desconhecidos. “Bruto” deve significar bruto **após** sanitização, com essa limitação documentada. | Alta |
| F02 | **P1** | [0009/spec — AC-6 a AC-9](/workspace/kaizent/docs/specs/0009-trace-usage-and-budget/spec.md:39); [0009/plan — Components e Token estimation](/workspace/kaizent/docs/specs/0009-trace-usage-and-budget/plan.md:3); [0004/plan — Structured output](/workspace/kaizent/docs/specs/0004-llm-access/plan.md); [0011/spec — AC-1 e AC-5](/workspace/kaizent/docs/specs/0011-lessons/spec.md:25) | Há decorators nas portas, reparo dentro do adaptador, várias chamadas no runtime e uma lição assíncrona após o veredito. A pré-checagem descrita estima apenas entrada. | Não está definido como cada chamada interna consome saldo e prazo, como limitar a saída antes de gerá-la, nem o que acontece quando a lição nasce após o orçamento da resolução acabar. Detectar excesso depois da resposta não garante o teto anunciado. | Especificar controle por chamada efetiva, incluindo reparos e retries; limitar saída quando suportado; definir tratamento de uso desconhecido, saldo entre tentativas e orçamento/prazo da lição. Separar tempo de processamento da espera humana. | Alta |
| F03 | **P1** | [0011/spec — AC-7](/workspace/kaizent/docs/specs/0011-lessons/spec.md:46); [0011/plan — Components e Testing](/workspace/kaizent/docs/specs/0011-lessons/plan.md:3); [0007/spec — AC-9](/workspace/kaizent/docs/specs/0007-skills-and-knowledge/spec.md:51) | Aprovar escreve um arquivo, atualiza o índice e altera o estado no PostgreSQL. Colisões recebem sufixo. | Escrita atômica de arquivo não torna atômicas as três operações. Uma queda entre elas pode deixar arquivo publicado com estado pendente e gerar outro arquivo na repetição. | Usar identidade estável da lição na publicação, registrar intenção durável e permitir retomada idempotente. Tratar reindexação como operação repetível. Prever falhas entre cada etapa. | Alta |
| F04 | **P2** | [0007/spec — AC-3, AC-6 e AC-8](/workspace/kaizent/docs/specs/0007-skills-and-knowledge/spec.md:32); [ADR-0027 — Decision](/workspace/kaizent/docs/adr/0027-workspace-layout.md); [0009/spec — AC-3](/workspace/kaizent/docs/specs/0009-trace-usage-and-budget/spec.md:33) | Arquivos recarregam durante a execução; em git, a versão é o último commit que os alterou; resultados de ferramentas guardam resumo, não o documento completo. Lições são criadas sem commit automático. | Um commit pode não identificar o conteúdo efetivamente lido: alterações locais e lições novas ficam fora dele. Uma citação aberta depois pode mostrar conteúdo diferente. Isso limita auditoria e replay. | Registrar hash do conteúdo efetivo, além do commit quando existente, e a versão de cada trecho fornecido ao modelo. Definir se a revisão abre a versão usada ou avisa que está vendo a atual. | Alta |
| F05 | **P2** | [03 — Nota de atualização](/workspace/kaizent/review/external-2026-11/03-revisao-fluxo.md:1); [0006/spec — AC-2, AC-3 e AC-5](/workspace/kaizent/docs/specs/0006-triage-and-roles/spec.md) | A proposta nova permite selecionar assunto e role separadamente; a spec permite no máximo uma role por assunto. O modo `rules` descreve assunto e role, mas a triagem também deve produzir tipo, idioma e rationale. | A proposta ainda não constitui um contrato implementável: faltam compatibilidade com o catálogo, campos restantes, precedência, ambiguidades e ausência de correspondência. | Definir a saída completa comum aos três modos, regras de conflito e fallback; revisar o modelo de roles e os critérios de validação antes de substituir a 0006. | Alta |
| F06 | **P2** | [03 — Nota de atualização](/workspace/kaizent/review/external-2026-11/03-revisao-fluxo.md:1); [0008/spec — AC-2](/workspace/kaizent/docs/specs/0008-resolution-and-proposal/spec.md:38); [ADR-0028 — Decision](/workspace/kaizent/docs/adr/0028-ticket-file-format-and-canonical-problem.md) | Roles determinam acesso; o LLM escolheria uma role da lista; labels são explicitamente pistas não confiáveis. | Lista fechada valida um identificador, mas não autoriza o ticket a usar toda role existente. Regras baseadas em labels também podem ampliar acesso por dados controlados pelo solicitante. O desenho atual já herda parte desse risco pelo assunto classificado. | Separar autorização de classificação. O código determina as roles elegíveis conforme origem e política; regras ou LLM escolhem somente nesse conjunto. Registrar a procedência dos metadados confiáveis. | Alta |
| F07 | **P2** | [ADR-0032 — camada 4](/workspace/kaizent/docs/adr/0032-defense-against-prompt-injection.md); [0006/spec — AC-3](/workspace/kaizent/docs/specs/0006-triage-and-roles/spec.md); [0011/spec — AC-1](/workspace/kaizent/docs/specs/0011-lessons/spec.md:25) | A checagem de saída detalha proposta e trace da resolução. Triagem e lição também produzem rationale e outros textos persistidos. | O objetivo geral de proteção não tem cobertura igualmente explícita nas demais etapas. Uma lição também pode transportar dados sensíveis ou instruções indevidas para a base. | Aplicar política comum de saída a todas as etapas e manter procedência e aprovação nas lições. Na wave 2, incluir documentos e retornos MCP como entradas não confiáveis. | Alta |
| F08 | **P2** | [0011/spec — AC-2 a AC-5](/workspace/kaizent/docs/specs/0011-lessons/spec.md:34); [0011/plan — Components e Data](/workspace/kaizent/docs/specs/0011-lessons/plan.md:7) | A idempotência é por `run_id`, mas `worthLearning=false` não cria uma lição; falha registra apenas erro no trace. | A unicidade da tabela `lesson` não basta para representar que o veredito já foi processado sem gerar lição, nem define a retomada após falha. Pode haver chamadas repetidas e custo duplicado. | Persistir o resultado do processamento do veredito, inclusive “não há lição”; distinguir falha recuperável e conclusão. Não prometer exatamente uma chamada externa diante de quedas. | Alta |
| F09 | **P2** | [0008/spec — AC-10](/workspace/kaizent/docs/specs/0008-resolution-and-proposal/spec.md:68); [0011/plan — Testing](/workspace/kaizent/docs/specs/0011-lessons/plan.md:43); [roadmap — Wave 1 / Definition of done](/workspace/kaizent/docs/roadmap.md) | A 0008 preserva o teste do skeleton; a 0011 automatiza aprovação→busca e deixa novo ticket→uso da lição para avaliação manual. | Há cobertura de partes, mas falta um critério automatizado explícito que una o ciclo completo expandido. O sucesso do skeleton não demonstra o PDCA completo. | Definir um cenário integrado com fake runtime: ticket, triagem, ferramentas, edição, resposta, lição aprovada e segundo ticket consumindo-a; complementar com avaliação real. | Alta |
| F10 | **P2** | [0008/spec — AC-2 e AC-3](/workspace/kaizent/docs/specs/0008-resolution-and-proposal/spec.md:38); [04 — §2, Gestão da janela de contexto](/workspace/kaizent/review/external-2026-11/04-catalogo-padroes.md:64) | Há limite de dez resultados de busca e vinte chamadas de ferramenta, mas não tamanho máximo do documento ou da seção retornada. | Poucas ferramentas ainda podem produzir contexto enorme. Limite de chamadas não equivale a limite da janela do modelo. | Definir teto por resultado e por chamada ao modelo, reserva para saída e sinalização de truncamento; permitir leitura posterior de outra seção ou página. | Alta |
| F11 | **P2** | [03 — P4](/workspace/kaizent/review/external-2026-11/03-revisao-fluxo.md:94); [0008/plan — Risks and mitigations](/workspace/kaizent/docs/specs/0008-resolution-and-proposal/plan.md) | Recomenda-se colocar três a cinco trechos no primeiro prompt, alegando economia de voltas e melhores citações. O plano verifica citações contra leituras por ferramenta. | O benefício não foi medido; trechos irrelevantes podem aumentar tokens e enviesar a resposta. Contexto pré-carregado também precisa contar como fonte efetivamente apresentada. | Fazer experimento comparativo com flag, teto de contexto, consulta tratada pelo código e proveniência dos trechos; medir qualidade, tokens totais e latência antes de ativar por padrão. | Média |
| F12 | **P2** | [03 — §2, “A resposta tem base?”](/workspace/kaizent/review/external-2026-11/03-revisao-fluxo.md:65); [0008/spec — AC-6 a AC-8](/workspace/kaizent/docs/specs/0008-resolution-and-proposal/spec.md:55); [ADR-0022 — Decision](/workspace/kaizent/docs/adr/0022-confidence-scoring.md) | A validação verifica existência e escopo do caminho; a confiança parte do número informado pelo modelo. | Um documento existente pode não sustentar a afirmação. Uma resposta inventada pode continuar com confiança alta citando um arquivo válido. | Distinguir citação válida, fonte apresentada/lida e afirmação sustentada. Exibir confiança como heurística não calibrada e avaliar sustentação semântica separadamente. | Alta |
| F13 | **P2** | [ADR README — decisões 1, 7 e 8](/workspace/kaizent/docs/adr/README.md:50); [0009/plan — Risks and mitigations](/workspace/kaizent/docs/specs/0009-trace-usage-and-budget/plan.md:60); [architecture — §10](/workspace/kaizent/docs/architecture.md) | Dados pessoais são tratados antes do conector real, mas retenção e autenticação ficaram para a wave 5. A justificativa atual dos prompts completos é o uso sintético. | O primeiro dado real pode chegar por arquivo, antes do Movidesk. Uso compartilhado ou remoto também pode ocorrer antes de Kubernetes. Os prazos estão ligados à tecnologia, não à exposição. | Tornar privacidade e retenção pré-condições do primeiro dado real, qualquer que seja o canal; autenticação antes do primeiro acesso compartilhado/remoto. Manter dispensa explícita apenas para demo local sintética. | Alta |
| F14 | **P2** | [roadmap — Wave 2](/workspace/kaizent/docs/roadmap.md); [ADR README — decisão 15](/workspace/kaizent/docs/adr/README.md:64); [0006/spec — AC-3](/workspace/kaizent/docs/specs/0006-triage-and-roles/spec.md) | A wave 2 inclui perguntas de negócio e analíticas; a decisão sobre tipos independentes de domínio está sem prazo; os tipos são `BUG`, `HOW_TO`, `OTHER`. | O segundo domínio pode cair genericamente em `OTHER` e continuar recebendo enquadramento de suporte, prejudicando roteamento e métricas. | Resolver a decisão 15 antes do segundo domínio, sem antecipar uma taxonomia genérica para o MVP. | Alta |
| F15 | **P2** | [0012/spec — AC-1 e AC-3](/workspace/kaizent/docs/specs/0012-subject-metrics-and-autonomy/spec.md); [ADR README — decisão 17](/workspace/kaizent/docs/adr/README.md:66) | Os níveis agregam resultados por assunto; modelos e políticas podem mudar por configuração. | O histórico de uma configuração pode conferir aparência de confiabilidade a outra. Aprovação humana também não equivale a sucesso comprovado no cliente. | Registrar a configuração efetiva por execução desde já; antes da autonomia, avaliar por versão/coorte, com tamanho de amostra, mudança de distribuição e sinais de resultado. | Alta |
| F16 | **P2** | [00 — Estado atual](/workspace/kaizent/review/external-2026-11/00-briefing.md); [0004/spec — AC-8, AC-9 e EV-1](/workspace/kaizent/docs/specs/0004-llm-access/spec.md); [03 — P6](/workspace/kaizent/review/external-2026-11/03-revisao-fluxo.md:107); [roadmap — Wave 1](/workspace/kaizent/docs/roadmap.md) | O briefing retira Gemini CLI do escopo; a spec e o roadmap ainda o exigem. P6 propõe mover T8 e AC-4, mas CLI agente também aparece em outros critérios e avaliações. | O corte proposto está incompleto e pode deixar critérios impossíveis de concluir. A validação atual de chamadas Codex não comprova o isolamento do futuro runtime agente. | Se P6 for aprovado, revisar todas as ocorrências de escopo, configuração, critérios e avaliações. Preservar `AgentRuntime` e seu fake na wave 1; mover somente o adaptador CLI agente. | Alta |
| F17 | **P2** | [03 — P6, opção b](/workspace/kaizent/review/external-2026-11/03-revisao-fluxo.md:107); [ADR-0032 — camada 1](/workspace/kaizent/docs/adr/0032-defense-against-prompt-injection.md) | A proposta diz que, com MCP e sem shell, “a restrição de leitura sai pela construção”. | MCP define acesso às ferramentas oferecidas; não elimina automaticamente outras capacidades da CLI, acesso a credenciais ou canais de rede. | Manter o gate de segurança, com validação negativa no binário real, configuração restritiva, escopo imposto pelo servidor e controle de rede compatível com o desenho. | Alta |
| F18 | **P3** | [04 — §6, Aprendizado ativo; §5, Streaming](/workspace/kaizent/review/external-2026-11/04-catalogo-padroes.md); [0010/spec — AC-1 e AC-2](/workspace/kaizent/docs/specs/0010-review-console/spec.md) | O catálogo chama revisão por confiança de aprendizado ativo implícito e atribui ao SSE progresso de “status e passos”. A spec pede revisão de todos e SSE com ids e statuses. | São capacidades diferentes: não há política de seleção de rótulos; o contrato SSE também não entrega explicitamente progresso por ferramenta ou passo. | Corrigir os rótulos e separar “aprovado em documento”, “implementado” e “validado”. Se necessário, especificar heartbeat/progresso sem transmitir texto ainda não verificado. | Alta |

### B. Respostas às seis perguntas

**1. Coerência entre visão, roadmap, specs, ADRs e decisões abertas**

A estrutura central é coerente: pipeline controlado por código, agente somente na resolução, ferramentas limitadas por role, revisão humana e aprendizado aprovado. Isso está alinhado entre [visão — Principles](/workspace/kaizent/docs/vision.md), [ADR-0017 — Decision](/workspace/kaizent/docs/adr/0017-pipeline-orchestration-in-own-code.md) e [arquitetura — §5](/workspace/kaizent/docs/architecture.md).

As principais divergências são estas:

- **Seleção de role:** o roadmap atribui a escolha ao LLM; a 0006 a torna determinística; a nota inicial do 03 propõe um terceiro desenho ainda não aprovado. Até a decisão formal, a spec aprovada continua sendo a referência. A nota substitui P1/P2 como proposta, mas o restante do 03 ainda apresenta as recomendações antigas.
- **Segurança versus trace bruto:** F01 é uma incompatibilidade operacional entre contratos aprovados, não apenas diferença de redação.
- **Versão versus conteúdo utilizado:** F04 impede tratar commit, hash, arquivo atual e conteúdo histórico como equivalentes.
- **Escopo das CLIs:** F16 mostra divergência entre briefing e critérios aprovados. Também é necessário distinguir “Codex validado para chamada estruturada” de “Codex validado como agente com leitura restrita”.
- **Segundo domínio sem decisão de taxonomia:** a wave 2 depende de uma decisão atualmente sem prazo — F14.
- **Avaliação:** o [ADR-0020 — Decision](/workspace/kaizent/docs/adr/0020-testing-and-evaluation-strategy.md) já permite avaliações manuais antes de mudanças. A wave 4 acrescenta o harness construído a partir de execuções registradas. Portanto, “regressão só na wave 4” é uma leitura excessiva do catálogo.
- **OpenTelemetry:** o catálogo o marca como ❓, mas ele já está planejado na wave 5 do roadmap. O que continua aberto é o desenho, não sua existência no plano.
- **Conhecimento em git versus edição sem commit:** isso pode funcionar, mas requer versionamento do conteúdo efetivo e regras de publicação; não basta dizer que “git é o histórico”.

Nem toda diferença é contradição. A visão mencionar aprendizado com resoluções bem-sucedidas enquanto a 0011 começa por edições/rejeições é um recorte legítimo do MVP. Da mesma forma, [ADR-0011 — Consequences](/workspace/kaizent/docs/adr/0011-markdown-knowledge-before-vectors.md) ainda dizer que a busca textual está aberta é um registro histórico posteriormente resolvido pelo ADR-0021; não é motivo para reescrever a decisão aceita.

**2. Fluxo: P1 a P7 e modos de triagem**

Minha distribuição recomendada de responsabilidades é: **código controla autorização, estados, orçamento e validações; modelos fazem interpretação semântica e redação; o agente escolhe ações dentro de limites já impostos.** Ela preserva a direção do ADR-0017.

| Ponto | Avaliação e recomendação |
|---|---|
| **P1 — seleção da role** | Concordo com o diagnóstico da nota inicial: assunto sozinho pode ser insuficiente. Não concluo que a solução deva ser deixar o LLM escolher qualquer role. Um mapeamento por assunto **e tipo**, com regras adicionais, pode resolver o caso sem acrescentar decisão ao modelo. Quando houver ambiguidade semântica real, o LLM pode escolher entre candidatas autorizadas. |
| **P2 — busca na triagem** | Manter sem ferramentas no MVP. Descrições e exemplos de classificação são um primeiro ajuste razoável, mas aumentam tokens se forem enviados ao prompt — não são literalmente “sem tokens extras”. Não faria busca em toda a base antes da definição do escopo: pode introduzir informação de áreas inelegíveis e ruído. Medir primeiro os erros da classificação. |
| **P3 — skills** | Manter divulgação progressiva para procedimentos opcionais. Regras invariantes de segurança, formato e idioma devem estar no contrato/prompt base e, quando possível, no código; não depender de o agente lembrar de ler uma skill. “Skill carregada” também não prova que o procedimento foi seguido. |
| **P4 — pré-busca** | Boa hipótese de otimização, ainda sem demonstração. Eu adotaria uma implementação experimental e limitada, sem torná-la imediatamente obrigatória no MVP. Ela precisa respeitar a role, o orçamento e a proveniência, conforme F11. |
| **P5 — base de dados** | A distinção foi resolvida na nota: trata-se de conhecimento. Manter banco operacional do produto e outras ferramentas externas fora da wave 1. Na wave 2, “somente leitura” continua exigindo autorização de quais dados podem ser consultados. |
| **P6 — CLI agente** | Concordo em adiá-lo para a wave 2 caso a restrição exigida não possa ser demonstrada. O corte protege o MVP. Porém, MCP deve permanecer uma hipótese de desenho sujeita ao gate do ADR-0032, não uma garantia antecipada. |
| **P7 — assunto desconhecido** | Manter role padrão, desde que seu escopo seja conservador. `unknown` não deve abrir mais acesso. A resposta pode pedir informação ou explicar a limitação, sem exigir agora um novo fluxo conversacional. |

A proposta dos três modos faz sentido como possibilidade de configuração, mas não implementaria os três apenas para completar a matriz.

| Modo | Uso indicado | Contrato necessário |
|---|---|---|
| `llm` | Texto livre e classificação sem metadados confiáveis suficientes. | Uma chamada estruturada; candidatos autorizados; saída validada; possibilidade de `unknown`; registro de prompt e política. |
| `rules` | Canal com campos confiáveis que realmente expressem a classificação. | Definir assunto, role, tipo e idioma, ou seus defaults explícitos; precedência; comportamento sem correspondência; origem da regra. |
| `rules-then-llm` | Parte dos tickets tem classificação confiável e o restante exige interpretação. | Só dispensar a chamada quando o contrato estiver suficientemente preenchido. Registrar qual regra resolveu ou por que houve fallback. |

O modo `rules` não deve confiar automaticamente em labels enviadas pelo solicitante. O [ADR-0028 — Alternatives considered](/workspace/kaizent/docs/adr/0028-ticket-file-format-and-canonical-problem.md) já explica por que labels são pistas. Regras sobre fila interna autenticada e regras sobre texto fornecido pelo usuário têm níveis de confiança diferentes.

**Onde vejo custo evitável:**

- Repetir instruções de formatação e procedimentos curtos em várias leituras.
- Reenviar documentos completos quando bastaria uma seção limitada.
- Repetir buscas ou leituras iguais dentro da mesma execução sem reutilizar o resultado.
- Produzir rationale longo para uma classificação simples.
- Fazer nova tentativa sem distinguir falta de evidência, erro transitório e limitação do modelo.
- Gerar lição novamente para um veredito já processado.

Essas oportunidades decorrem dos contratos de ferramentas, reparo e aprendizado das specs 0004, 0008 e 0011. A economia efetiva precisa ser medida.

**P4 especificamente:** comparar a configuração atual com pré-busca usando os mesmos casos, mantendo um pequeno conjunto separado dos exemplos usados no ajuste. Medir qualidade factual, fontes adequadas, total de tokens, latência e chamadas de ferramentas. Economizar uma volta pode ajudar; acrescentar cinco trechos em todas as voltas pode anular esse ganho. Lucene não consome tokens ao buscar, mas o resultado enviado ao modelo consome.

**P6 especificamente:** o runtime nativo torna a superfície de ferramentas mais explícita e auditável. Isso favorece o MVP, mas altera a promessa de configuração do [roadmap — Definition of done](/workspace/kaizent/docs/roadmap.md): login CLI sozinho deixa de bastar para todo o ciclo. Essa dependência precisa aparecer no setup. `AgentRuntime`, `TraceSink` e fake continuam necessários na wave 1, independentemente do adiamento do adaptador CLI.

**3. Catálogo: decisão para cada item ❓**

A tabela abaixo cobre os itens únicos em aberto, incluindo os subitens de linhas parciais. As seções referem-se ao [documento 04](/workspace/kaizent/review/external-2026-11/04-catalogo-padroes.md). São **minhas propostas**, não decisões já autorizadas.

| Item ❓ | Status que proponho | Motivo |
|---|---|---|
| **Pré-recuperação / RAG clássico — §1** | **Planejar experimento na wave 1** | Pode reduzir voltas, mas não há evidência para torná-la default. Aplicam-se os cuidados de F11. |
| **Filtro por tags — §1** | **Planejar na wave 2, condicionado a necessidade** | É uma capacidade de metadados, independente de vetores. Pode ser útil com bases reais; não precisa esperar tecnicamente pela wave 3. |
| **Reranking — §1** | **Planejar avaliação na wave 3** | Primeiro medir se a recuperação inicial deixa documentos relevantes fora das primeiras posições. Não adicionar modelo sem diagnóstico. |
| **Reescrita/expansão da consulta, incluindo HyDE — §1** | **Dispensar etapa separada no MVP** | O agente já formula consultas. Reescrita e HyDE não são equivalentes e só devem ser reavaliados quando houver falhas específicas de recuperação. |
| **Contextual retrieval — §1** | **Dispensar enriquecimento por LLM no MVP; reavaliar na wave 3** | Preservar título e hierarquia de cabeçalhos é barato e pode entrar já na wave 1. Não exige a técnica mais complexa de gerar contexto com outro modelo. |
| **Tamanho máximo de ferramenta / janela — §2** | **Adotar na wave 1** | É requisito de previsibilidade do runtime, não otimização opcional. Incluir limite agregado e reserva de saída. |
| **Histórico do ticket — §2** | **Planejar antes do conector da wave 2** | Definir mensagens, autores, ordem, versões e o que muda quando chega nova resposta. Cortar texto sem preservar autoria pode mudar o sentido. |
| **Reflexão/autocrítica — §2** | **Dispensar no MVP** | Não há ganho demonstrado que compense nova chamada. Revisão humana não é tecnicamente equivalente, mas permite adiar a técnica. |
| **Avaliador-otimizador — §2** | **Dispensar no MVP** | Acrescenta custo, possibilidade de ciclos e dificuldade de atribuir ganhos. Reavaliar apenas com defeito recorrente mensurável. |
| **A2A — §2** | **Dispensar** | Não há integração concreta que o exija. |
| **LLM como juiz — §3** | **Planejar na wave 2, offline e opcional** | A lacuna semântica é real; sua primeira resposta pode ser revisão humana dos evals. O juiz deve ser calibrado contra essa revisão, receber evidência de referência e poder declarar incerteza. |
| **Métricas de recuperação — §3** | **Adotar na wave 1** | Medir presença/rank do documento relevante nos resultados. Separar isso da decisão posterior do agente de citá-lo. |
| **PII — §4** | **Adotar política antes de qualquer dado real; implementação antes do piloto real** | Abrange prompts, traces, lições, relatórios e futuros datasets, independentemente do conector. |
| **Retenção e exclusão — §4** | **Planejar para o primeiro piloto real, antecipando a wave 5** | Definir destino de cópias derivadas e backups, não apenas apagar `trace_event`. |
| **Kill switch — §4** | **Adotar pausa de processamento na wave 1; bloqueio de envio automático na wave 4** | Já existem chamadas que consomem cota e entregas aprovadas pendentes. O interruptor da autonomia é uma função adicional. |
| **Fallback de modelo — §5** | **Planejar na wave 2** | Preservar a decisão 18: erros elegíveis, capacidade alternativa, prazo e orçamento compartilhados, rastreabilidade e desligamento nos evals comuns. |
| **Roteamento por problema/escalada — §5** | **Planejar decisão na wave 2; implementação condicionada a evidência** | Começar com modelo fixo por etapa. Maior modelo não corrige documento ausente. Registrar separadamente roteamento inicial e escalada. |
| **OpenTelemetry — §5** | **Manter planejado na wave 5** | O roadmap já o prevê. Adiantar apenas identificadores estáveis que facilitem correlação futura. |
| **Cache de prompt — §5** | **Adotar organização estável na wave 1; integração específica por provedor depois de medir** | Prefixo estável pode ajudar, mas não garante elegibilidade, acerto ou desconto. Cache não é capacidade uniforme entre API e CLI. |
| **Temperatura/parâmetros — §5** | **Adotar na wave 1 quando suportados** | Registrar parâmetros solicitados e, quando disponíveis, efetivos. Temperatura fixa não garante determinismo; alguns modelos restringem esse parâmetro. |
| **Versão do modelo — §5** | **Adotar na wave 1 quando informada** | Guardar alias solicitado e identificação retornada. Fixar versão para comparação quando possível; registrar quando não for possível. |
| **Few-shot dinâmico — §6** | **Planejar na wave 3, condicionado a ganho** | Usar resposta final aprovada, com versão e procedência; evitar transferir dados ou particularidades de outro cliente. Não depende tecnicamente de vetores. |

O servidor MCP, embora tenha uma decisão aberta associada, está marcado como planejado, não ❓. Eu manteria esse status na wave 2 e trataria separadamente **oferecer ferramentas** e **comprovar isolamento do cliente CLI**.

**Status ✅, ❌ e 💬 que eu contestaria ou qualificaria:**

- **✅ Aprendizado ativo:** retirar “adotado implícito”. Revisar todos os casos não é uma política de selecionar exemplos informativos para rotular. O catálogo pode marcar essa técnica como dispensada no MVP e candidata para a wave 4.
- **✅ Guardrails e orçamento:** o catálogo define “adotado” como aprovado **ou** implementado. Isso não está errado segundo sua legenda, mas não serve como demonstração de capacidade operacional. Separaria especificado, implementado e validado.
- **✅ Métrica de feedback:** aprovação sem edição é um sinal útil, mas não comprovação de correção nem de sucesso no cliente — F15.
- **❌ Chunking com sobreposição:** concordo em não acrescentá-lo agora. Discordo da justificativa de que seções sempre são unidades suficientes: uma seção pode ser enorme. O limite da wave 1 deve resolver esse caso sem esperar vetores.
- **❌ Cache semântico:** manter fora do MVP. A revisão humana não é o motivo principal da dispensa; ela poderia revisar uma resposta recuperada. Os riscos principais são equivalência incorreta e invalidação após mudanças.
- **❌ Execução de código:** correto no MVP, mas não uma dispensa permanente. A wave 2 inclui perguntas analíticas e a wave 5 incidentes e patches; esses escopos podem reabrir a necessidade.
- **💬 Streaming:** é dispensável para o MVP, mas não é exclusivo de chat nem obrigatório em todo chatbot. Uma resposta final rápida com progresso pode bastar. Streaming ao usuário final precisa ser compatível com a verificação da saída.
- **🗓️ Regressão na wave 4:** manter o harness avançado nessa wave; começar comparações manuais do conjunto existente já na wave 1, como o ADR-0020 permite.

**Padrões relevantes ausentes ou insuficientemente explícitos:**

| Padrão | Status proposto | Por que importa neste projeto |
|---|---|---|
| Abstenção e pedido de esclarecimento como resultado válido | **Adotar na wave 1** | `lacksInformation` existe, mas a avaliação deve premiar uma resposta útil de esclarecimento, não apenas confiança baixa. Base: 0008, AC-5 e EV-4. |
| Proveniência e conteúdo imutável por execução | **Adotar na wave 1** | Resolve F04 e sustenta auditoria e comparação. |
| Publicação idempotente e recuperação de efeitos externos | **Adotar na wave 1** | Arquivos e banco têm falhas independentes — F03 e F08. |
| Testes adversariais de capacidades | **Adotar na wave 1** | Testar acesso negado e impossibilidade de exfiltração, além de reconhecer frases suspeitas; decorre do gate do ADR-0032. |
| Tratamento de injeção indireta | **Especificar na wave 1; ampliar na wave 2** | Conhecimento, skills e futuros retornos MCP também podem carregar instruções. |
| Conjunto de avaliação separado dos exemplos de ajuste | **Adotar pequeno conjunto na wave 1** | Evita otimizar pré-busca e prompts apenas para os doze casos conhecidos da spec 0003. |
| Comparação pareada de configurações | **Adotar manualmente na wave 1** | Comparar qualidade, custo e latência nos mesmos casos antes de acrescentar roteamento ou juiz. |
| Controle de cotas e contrapressão | **Planejar na wave 2** | Orçamento por run não limita a soma de muitos runs; conecta-se à decisão 18. |
| Gestão de credenciais: revogação, rotação e escopo | **Decidir antes de liberar novo preset** | Complementa o armazenamento do ADR-0031 e a exceção do token Claude Code citada no briefing. |
| Invalidação de conhecimento e lições | **Planejar na wave 2** | Lições aprovadas envelhecem ou contradizem documentação; aprovação inicial não resolve validade futura. |

**4. Waves: ordem, corte e fechamento de ponta a ponta**

A ordem geral faz sentido: demonstrar PDCA em arquivos, integrar um canal real, melhorar recuperação, habilitar autonomia, ampliar operação e só depois avaliar treinamento. Ela respeita o princípio de justificar complexidade por necessidade, em [visão — Principles 1 e 5](/workspace/kaizent/docs/vision.md).

Eu faria os seguintes ajustes de corte, sujeitos à aprovação do mantenedor:

| Wave | Ajuste recomendado |
|---|---|
| **1** | Concentrar no runtime nativo e fake; adiar CLI agente se o gate de segurança não fechar. Limitar novos presets não validados. Acrescentar os contratos de orçamento, contexto, sanitização e publicação recuperável. |
| **2** | Priorizar um conector real com privacidade, retenção, histórico e entrega segura. MCP cliente, MCP servidor, segundo domínio e evolução de skills não precisam chegar todos juntos. |
| **3** | Condicionar vetores, reranking e few-shot a deficiências medidas da recuperação lexical. A wave pode ser pequena se BM25 continuar suficiente. |
| **4** | Manter autonomia condicionada a avaliação por configuração, resultados observáveis, revisão amostral e capacidade de interromper envios. |
| **5** | Manter Kubernetes e observabilidade distribuída. Antecipar autenticação quando houver exposição compartilhada e retenção quando houver dados reais. |
| **6** | Tratar fine-tuning como hipótese condicionada a volume, direitos sobre dados e ganho frente a prompts, skills e recuperação. |

Eu manteria as métricas da 0012 no MVP, mas não deixaria o refinamento dos níveis provisórios atrasar o ciclo. O requisito de “nível sem efeito” está corretamente explícito em sua AC-5.

**A wave 1 está garantida de ponta a ponta? Ainda não.**

Há três níveis de evidência distintos:

1. **Fluxo fino entregue:** a spec 0002 está `Done` e documenta ticket→proposta→aprovação→outbox.
2. **PDCA completo especificado:** as specs 0006–0012 descrevem triagem, ferramentas, revisão completa, aprendizado e métricas.
3. **PDCA completo demonstrado:** não resulta do estado documental atual. Essas specs continuam aprovadas e pendentes no [índice](/workspace/kaizent/docs/specs/README.md); F09 identifica a lacuna de aceitação integrada.

O maior risco de a wave não fechar é **gastar o esforço restante ampliando a camada de acesso a modelos enquanto a integração resolução→revisão→lição ainda não está demonstrada**. O briefing mostra a concentração atual em presets e runtime; o fechamento depende de atravessar as specs seguintes, não de oferecer mais fornecedores.

O cenário de aceitação que eu exigiria ao concluir a wave seria:

- Processar um ticket com triagem e ferramentas restritas à role.
- Editar e aprovar a proposta, produzindo a resposta em `outbox/`.
- Produzir e aprovar a lição, publicando-a uma única vez.
- Submeter outro ticket com identidade nova e verificar que recebe o conhecimento da lição.
- Conferir rastreamento sanitizado, uso e métricas.
- Demonstrar retomada após falha de entrega e de publicação da lição.

Esse é um critério proposto para trabalho futuro; não executei essa validação nesta revisão. A qualidade semântica continuaria exigindo os evals reais da 0008 e da 0011.

**5. Riscos futuros**

**Segurança e CLI com assinatura.** Concordo com o núcleo do ADR-0032: a segurança deve continuar funcionando mesmo quando o detector não reconhecer a injeção. `SAFE` significa ausência de correspondência nas regras, não confiança no ticket. Preservaria integralmente esse gate.

O token Claude Code por variável de ambiente, citado como exceção no [briefing — Decisões já tomadas](/workspace/kaizent/review/external-2026-11/00-briefing.md), precisa de contrato próprio antes de liberar o preset. Variável de ambiente evita arquivo de login, mas não torna o segredo inacessível ao processo nem a capacidades que consigam inspecionar o ambiente. A revisão deveria verificar escopo, redaction de erros, revogação e ausência de persistência em estado ou trace.

A política pessoal/local e API em cluster compartilhado já foi decidida; não proponho reabri-la. Proponho documentar corretamente a modalidade de cada preset e não inferir permissão de uso somente porque a ferramenta funciona tecnicamente.

Também separaria **risco de injeção** de **elegibilidade de destino**. O fallback de um ticket suspeito para runtime API não deve, no futuro, enviar a um provedor externo dados cuja política permita apenas processamento local. A seleção precisa respeitar ambas as condições. Isso decorre do compromisso de privacidade da visão e da seleção de runtime da 0008, AC-11.

**Dados pessoais.** A decisão 1 cobre mascaramento, mas a política precisa alcançar o ciclo inteiro: entrada, prompts, trace, lições, relatórios, índices, exportações e backups. Identificadores estruturados são apenas parte do problema; texto livre também pode identificar alguém. Por isso, “usar dados reais” deve ser um marco de prontidão explícito, e não uma consequência automática de concluir o adaptador Movidesk.

Antes da indexação de resoluções da wave 3, distinguiria:

- proposta do modelo;
- resposta final aprovada;
- comentário de rejeição;
- fato confirmado sobre o produto;
- informação particular de um atendimento.

Aprovar uma resposta não torna todos esses conteúdos conhecimento reutilizável. A fonte dessa preocupação é a combinação de [roadmap — Wave 3](/workspace/kaizent/docs/roadmap.md) com os dados capturados nas specs 0009 e 0011.

**Custo e cotas.** F02 precisa ser resolvido antes de prometer limites fortes. Depois, na wave 2, acrescentaria limite agregado de consumo e controle de admissão. Muitos runs baratos podem exceder uma cota diária.

Manteria as distinções já previstas na decisão 18: um 429 pode representar limites diferentes; trocar de modelo dentro da mesma cota pode não ajudar. Usos sem contagem informada devem permanecer desconhecidos, não virar zero. Assinatura também tem cota e latência, ainda que não tenha preço incremental por chamada.

**Dependência de fornecedor.** As portas reduzem acoplamento do código, mas não igualam qualidade, ferramentas, schemas, contabilidade ou restrições de cada fornecedor. A própria 0008 prevê comportamentos diferentes entre runtimes. Eu registraria capacidades suportadas e configuração efetiva, além de manter uma avaliação mínima por adaptador efetivamente recomendado. Presets apenas exemplificados não deveriam aparecer como equivalentes aos validados.

**Prazos das decisões abertas.** Minha proposta para a seção [Open decisions](/workspace/kaizent/docs/adr/README.md:43) é:

| Decisão | Prazo recomendado |
|---|---|
| **1 — dados pessoais; 8 — retenção** | Antes de qualquer piloto com dados reais, inclusive por arquivos. |
| **7 — autenticação e roles** | Antes de acesso compartilhado ou remoto; pode continuar adiada enquanto o uso for exclusivamente local e individual. |
| **10 — bootstrap** | Caminho manual validado no README para fechar a wave 1; decisão sobre automação antes do primeiro deployment externo. |
| **15 — tipos e prompts independentes de domínio** | Antes de introduzir perguntas de negócio/analíticas na wave 2. |
| **16 — fonte de verdade do workspace** | Antes de implementar edição pela console, sincronização ou importação contínua. Não é necessário construir agora dois modos de armazenamento. |
| **11 — importação** | Antes do primeiro piloto que dependa de conteúdo externo ao formato Markdown atual. |
| **12 — edição pela console** | Antes de oferecer qualquer edição de roles, skills ou conhecimento geral pela console. |
| **17 — modelo por problema/escalada** | Na especificação da wave 2: decidir se permanece adiado e quais métricas justificariam adotá-lo. |
| **18 — fallback** | Na especificação da wave 2; a fronteira de orçamento de retries/reparos deve estar definida já na wave 1. |
| **13 — múltiplos workspaces** | Antes de prometer uma instância compartilhada com isolamento entre workspaces. Até lá, manter explicitamente um workspace por instância. |
| **14 — múltiplas instâncias** | Antes da primeira implantação replicada. `SKIP LOCKED` resolve apenas parte da coordenação. |
| **2 — servidor MCP; 3 — entrega de mudanças de skills** | Antes das respectivas funcionalidades da wave 2. |
| **4 — embeddings/chunking** | Antes da implementação da wave 3, apoiada em métricas de recuperação. |
| **5 — autonomia** | Antes de qualquer envio automático na wave 4. |
| **6 — runner Kubernetes** | Antes de implementar o runner da wave 5. |
| **9 — marca** | Manter antes da promoção pública, como já previsto. |

**Chatbot.** Concordo em registrá-lo como evolução candidata, mas discordo de tratá-lo apenas como outro adaptador de entrada. A [0005 — Out of scope](/workspace/kaizent/docs/specs/0005-problem-intake/spec.md) exclui atualizações e threading; o [ADR-0028](/workspace/kaizent/docs/adr/0028-ticket-file-format-and-canonical-problem.md) ignora alterações de tickets já recebidos. Uma conversa exige identidade de sessão e mensagens, ordem, resposta à mensagem correta, cancelamento de geração obsoleta e transferência para humano.

Também não colocaria uma dependência universal de wave 4. Um chat **assistido por humano** pode existir antes da autonomia. Já respostas autônomas ao cliente exigem os controles da wave 4 e uma política de elegibilidade por interação.

Streaming acrescenta uma tensão real: texto enviado antes da checagem de saída não pode ser recolhido. É necessário escolher entre resposta final verificada, buffering verificável ou uma política explícita para streaming seguro. A frase “streaming é necessário” do catálogo não resolve esse compromisso.

**6. Top 5 recomendações**

| Prioridade | Recomendação | Impacto esperado | Custo aproximado |
|---|---|---|---|
| **1** | **Fechar o contrato de cada chamada efetiva:** orçamento, prazo, tentativas, contabilidade e sanitização antes de persistir. | Evita os principais desvios de segurança e custo e reduz retrabalho entre 0004, 0008, 0009 e 0011. Resolve F01, F02 e parte de F07. | **Médio** |
| **2** | **Tornar publicação e processamento de lições recuperáveis e idempotentes.** Incluir resultado “sem lição” e identidade estável da publicação. | Protege o fechamento do Act contra quedas, repetições e divergência entre arquivo, banco e índice. Resolve F03 e F08. | **Médio** |
| **3** | **Fixar um corte executável da wave 1 e sua aceitação integrada.** Recomendo runtime nativo e fake, preservando o gate da CLI e adiando o adaptador agente se necessário. | Concentra o trabalho no PDCA completo e evita que descoberta de ferramentas bloqueie o produto. Resolve F09 e F16. | **Médio** |
| **4** | **Definir triagem completa e separar classificação de autorização antes de alterar a 0006.** | Evita roles inadequadas, ambiguidades entre modos e expansão de acesso por metadados não confiáveis. Resolve F05 e F06. | **Médio** |
| **5** | **Medir antes de ampliar inteligência:** proveniência por conteúdo, limites de contexto, pequeno conjunto independente e comparação da pré-busca. | Permite justificar P4, roteamento, vetores e juiz por resultado observável. Reduz falsa confiança em citações e métricas. Resolve F04, F10, F11 e F12. | **Médio** |

### C. Discordâncias com o Claude

1. **Não adotaria P4 como melhoria já demonstrada.** Pré-busca é um experimento promissor; pode aumentar tokens e viés. A mudança também alcança proveniência, orçamento e validação de fontes, não apenas `AgentTask` e AC-1.

2. **Não substituiria assunto→role por escolha irrestrita do LLM entre todas as roles.** Primeiro separaria autorização e classificação; depois avaliaria regras por assunto/tipo e seleção entre candidatas elegíveis. A nota inicial do 03 ainda não resolve esse contrato.

3. **MCP não torna a restrição de leitura automática.** Concordo com adiar CLI agente, mas discordo da garantia dada à opção b de P6. O gate do ADR-0032 continua necessário.

4. **LLM como juiz não é minha primeira prioridade.** A limitação das checagens por palavras é real; primeiro fecharia o ciclo e estabeleceria avaliação humana de referência. Um juiz barato sem calibração pode apenas produzir outra avaliação incorreta.

5. **Revisão integral não é aprendizado ativo, e citação existente não demonstra sustentação factual.** Os dois rótulos do catálogo/fluxo atribuem mais força aos mecanismos atuais do que os contratos oferecem.

6. **Chat não exige sempre streaming nem autonomia prévia.** Pode operar com assistência humana e resposta final verificada. O novo canal exige mudanças de ciclo de vida, não apenas conexão à arquitetura existente.

7. **Não deixaria toda regressão para a wave 4 nem todo controle operacional para depois.** Comparações manuais, limites de contexto e pausa de processamento já têm utilidade no MVP; retenção e autenticação devem acompanhar a exposição real.

8. **Não criaria um ADR para cada padrão adotado ou dispensado.** O [AGENTS.md — Record decisions](/workspace/kaizent/AGENTS.md) pede ADR para decisões arquiteturalmente significativas. Um catálogo enxuto pode registrar decisões menores; transformar cada linha em ADR aumentaria manutenção sem necessariamente melhorar o projeto.
