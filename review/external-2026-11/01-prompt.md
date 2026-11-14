Você vai fazer uma **revisão independente e crítica** do projeto Kaizent. Leia primeiro `review/external-2026-11/00-briefing.md`, depois os documentos do repositório que ele lista, e por fim `03-revisao-fluxo.md` e `04-catalogo-padroes.md`, na mesma pasta.

**Regras:**
- Só leitura e análise. Não altere arquivos, não faça commit nem push, não abra PR, não comente no GitHub. Entregue a revisão **na sua resposta**.
- O objetivo é encontrar problemas, não confirmar o que já está escrito. As recomendações dos documentos 03 e 04 são **propostas do Claude**. Discorde explicitamente sempre que achar que estão erradas, incompletas ou mal priorizadas.
- Fundamente cada afirmação no texto dos documentos (cite arquivo e seção). Separe o que é fato documentado do que é sua opinião. Se não tiver certeza, diga.
- Responda em português do Brasil.

**Entregue nesta ordem:**

### A. Tabela de achados

Colunas: `id` | `severidade` (P1: bloqueia ou causa retrabalho grande; P2: importante; P3: melhoria) | `documento e seção` | `evidência` (trecho ou fato) | `problema` | `sugestão` | `confiança` (alta, média ou baixa).

### B. Respostas às seis perguntas

1. **Coerência:** contradições ou lacunas entre a visão, o roadmap, as specs, os ADRs e as decisões abertas (um exemplo conhecido: o roadmap diz que o LLM "seleciona a role", enquanto a spec 0006 diz que a seleção é determinística).
2. **Fluxo** (`03`): avalie P1 a P7 e os modos de triagem (`llm`, `rules`, `rules-then-llm`). Onde a "inteligência" está mal colocada (decisão demais com o LLM, ou de menos)? Onde há custo de tokens evitável? A pré-busca (P4) e o adiamento do modo agente via CLI para a wave 2 (P6) são boas escolhas?
3. **Catálogo** (`04`): para cada item ❓, diga se adota, planeja (em qual wave) ou dispensa, e por quê. Algum ✅, ❌ ou 💬 que você contestaria? **Que padrões relevantes faltam?** Liste-os com o status que você proporia.
4. **Waves:** a ordem e o corte das waves fazem sentido? Algo da wave 1 deveria sair, ou algo de uma wave futura deveria entrar? A wave 1 está de fato garantida "de ponta a ponta" (ticket, triagem, resolução, revisão, resposta, lição)? Onde está o maior risco de ela não fechar?
5. **Riscos futuros:** segurança (ADR-0032, CLI com assinatura, token em variável de ambiente), dados pessoais (wave 2), custo e cotas, dependência de fornecedor, as decisões abertas sem prazo que deveriam ter prazo, e a evolução para chatbot.
6. **Top 5 recomendações**, priorizadas, cada uma com o impacto esperado e o custo aproximado (baixo, médio ou alto).

### C. Discordâncias com o Claude

Uma lista curta de onde você discorda das propostas do Claude (documentos 03 e 04), com o motivo.
