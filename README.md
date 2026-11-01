# FlashLang

Aplicativo Android de **flashcards para aprendizado de inglês e espanhol**, com uma biblioteca de **prompts para gerar baralhos com IA**. Você escolhe um prompt (vocabulário de um texto, pronúncia de verbos com -ed, sons das vogais etc.), copia para uma IA (ChatGPT, Gemini, Claude...), importa o JSON que ela devolver e estuda com repetição espaçada, cards de lacuna (cloze), pronúncia por áudio, IPA e regras no verso do card.

Projeto pessoal de estudo, sem conta e sem servidor: os baralhos e os prompts ficam no próprio aparelho.

## Como funciona

1. Abra a tela **Prompts** e escolha uma atividade, por exemplo "Vocabulário de textos que li".
2. Escolha o idioma, cole o texto (ou a lista de palavras) e toque em **Copiar prompt**. O app monta o prompt completo, já com o formato que ele sabe importar.
3. Cole o prompt numa IA e copie o JSON que ela devolver.
4. Na tela de importação, cole o JSON (ou escolha um arquivo `.json`).
5. Toque no baralho para estudar: o card mostra o termo (ou uma frase com lacuna); ao tocar, vira e mostra tradução, pronúncia (IPA), regra, frase de exemplo e tradução da frase.

## Funcionalidades

### Prompts
- **Biblioteca de prompts prontos**, organizados por categoria (Vocabulário, Pronúncia e Conversa).
- **Marcadores** nos prompts (`{{IDIOMA}}`, `{{TEXTO}}`, `{{LISTA}}`, `{{FOCO}}`): a tela mostra só os campos necessários e monta o prompt final.
- **Bloco de formato fixo**: o app anexa automaticamente ao fim de cada prompt que gera baralho o formato JSON esperado, então a importação não quebra mesmo que você edite o texto do prompt.
- **Prompts próprios**: criar, editar, duplicar e excluir. Os prontos podem ser duplicados como cópias editáveis. Um interruptor define se o prompt gera baralho (com bloco de formato) ou serve só para copiar texto, como o de conversa.

### Baralhos e importação
- Importação de JSON **colado ou de arquivo** (Storage Access Framework).
- **Importador tolerante**: aceita JSON copiado de diferentes IAs, removendo blocos de código markdown, texto antes e depois do JSON, aspas curvas, espaços invisíveis, entidades HTML, barras invertidas no fim das linhas e vírgulas finais. Normaliza valores como `language` ("English", "inglês" → `en`) e `type` ("phrase", "expressão" → `expression`). Nos cards de lacuna, normaliza as variações do espaço em branco (`____`, `[...]`, `…`) e cria a lacuna sozinho quando a IA esquece de marcá-la.
- **Mensagens de erro claras** em português, indicando o campo e o card com problema.
- **Exportar e compartilhar** baralhos em JSON (backup e troca entre aparelhos).
- **Gerenciamento**: busca, edição, exclusão e adição manual de cards, e renomeação de baralhos.

### Estudo
- Card que **vira ao tocar** (animação de rotação).
- **Cards de lacuna (cloze):** a frente mostra a frase com um espaço em branco e a tradução como dica; o verso mostra a frase completa com a resposta em destaque.
- **Campos de pronúncia e regra:** IPA e uma nota opcionais no verso do card.
- **Dois modos de estudo**, escolhidos por baralho:
  - **Padrão:** o termo na frente, tradução e exemplo no verso.
  - **Pronúncia (ouvir primeiro):** o áudio toca sozinho, com a palavra escondida; há botões para ouvir de novo, ouvir devagar e mostrar a palavra. A ideia é repetir em voz alta e conferir ao virar o card.
- **Repetição espaçada** (versão simplificada do SM-2) com quatro avaliações: *Errei*, *Difícil*, *Bom* e *Fácil*.
- **Cards pendentes do dia** por baralho, com a opção "Estudar tudo mesmo assim" sem alterar o agendamento.
- **Pronúncia por áudio** (Text-to-Speech do Android) do termo e da frase de exemplo, com **escolha de voz** (vozes offline e online, estas marcadas) e de **velocidade**.

### Interface
- Material 3, ícones Outlined, em português do Brasil.
- Ações de cada baralho (Gerenciar, Exportar, Excluir) em um menu de três pontos no card; tocar no card abre o estudo.

## Prompts prontos

| Prompt | Categoria | Para que serve |
|---|---|---|
| Vocabulário de textos que li | Vocabulário | Palavras e expressões de um texto, com tradução e frase de exemplo |
| Phrasal verbs e colocações do texto | Vocabulário | Phrasal verbs e colocações que aparecem em um texto |
| Cloze: frases com lacuna | Vocabulário | Cards em que a frase tem um espaço em branco para completar |
| Falsos cognatos | Vocabulário | Palavras que parecem com o português mas têm outro sentido |
| Pronúncia dos verbos terminados em -ed | Pronúncia | Passado regular: /t/, /d/ ou /ɪd/, com a regra |
| Terminações -s e -es | Pronúncia | /s/, /z/ ou /ɪz/, com a regra |
| Sons das vogais: curto e longo | Pronúncia | Regras de som curto e longo, "magic e", vogais dobradas |
| Pares mínimos | Pronúncia | Treino de contrastes como ship/sheep |
| Sons difíceis do inglês para brasileiros | Pronúncia | th, r final, /æ/, h aspirado e outros |
| Acento da palavra | Pronúncia | Sílaba tônica e contraste substantivo/verbo |
| Sons do espanhol | Pronúncia | b/v, rr, ll/y, ñ, ditongos e tilde |
| Conversa sobre o texto | Conversa | Diálogo com correções, sem gerar baralho |

## Formato do JSON

```json
{
  "deckName": "Pronúncia -ed",
  "language": "en",
  "cards": [
    {
      "term": "walked",
      "type": "word",
      "termTranslation": "andou, caminhou",
      "example": "She walked to school yesterday.",
      "exampleTranslation": "Ela caminhou até a escola ontem.",
      "ipa": "/wɔːkt/",
      "note": "Termina em /t/: o som anterior (/k/) é surdo."
    }
  ]
}
```

| Campo | Valores | Obrigatório | Descrição |
|---|---|---|---|
| `deckName` | texto | sim | Nome do baralho |
| `language` | `en` ou `es` | sim | Idioma de estudo |
| `cards[].term` | texto | sim | Palavra ou expressão |
| `cards[].type` | `word` ou `expression` | sim | Tipo do termo |
| `cards[].format` | `basic` ou `cloze` | não | Formato do card; padrão `basic` (também aceita `lacuna`) |
| `cards[].termTranslation` | texto | sim | Tradução em português do Brasil |
| `cards[].example` | texto | sim | Frase de exemplo com o termo em contexto |
| `cards[].exampleTranslation` | texto | sim | Tradução da frase |
| `cards[].ipa` | texto | não | Pronúncia em IPA (também aceita `pronunciation` e `phonetic`) |
| `cards[].note` | texto | não | Regra ou observação em português (também aceita `rule` e `notes`) |

Baralhos antigos, sem `ipa`, `note` e `format`, continuam válidos.

### Cards de lacuna (cloze)

Com `"format": "cloze"`, o campo `example` traz a frase com `___` no lugar da resposta, e `term` é **exatamente a palavra que preenche a lacuna**, na forma usada na frase (conjugada, no plural...). A forma base pode ir em `note`.

```json
{
  "term": "gets along with",
  "type": "expression",
  "format": "cloze",
  "termTranslation": "se dá bem com",
  "example": "She ___ her coworkers.",
  "exampleTranslation": "Ela se dá bem com os colegas de trabalho.",
  "note": "Forma base: get along with."
}
```

## Repetição espaçada

Cada card guarda `easeFactor`, `intervalDays`, `repetitions` e `dueAt`. Depois de virar o card, você avalia como foi, e o agendador calcula quando ele volta:

| Avaliação | Efeito |
|---|---|
| **Errei** | Zera o progresso, o card volta em 10 minutos e o fator de facilidade diminui |
| **Difícil** | Intervalo cresce pouco e o fator de facilidade diminui |
| **Bom** | 1º acerto: 1 dia; 2º: 3 dias; depois o intervalo é multiplicado pelo fator de facilidade |
| **Fácil** | Como "Bom", com intervalo maior e fator de facilidade aumentado |

A lógica fica em uma função pura, separada da interface, e é coberta por testes unitários.

## Tecnologias

- **Kotlin** e **Jetpack Compose** (Material 3)
- Arquitetura **MVVM** com `ViewModel` e `StateFlow`
- **Room** (com KSP) para o banco local, com migrações que preservam os dados
- **Navigation Compose**
- **kotlinx.serialization** para leitura e geração do JSON
- **TextToSpeech** nativo do Android
- Testes unitários com JUnit
- **R8** com minificação e redução de recursos no build release

## Estrutura do projeto

```text
app/src/main/java/com/cristiano/flashlang/
├── data/          # entidades Room, DAOs, banco, repositórios, importador/exportador de JSON, agendador
└── ui/
    ├── home/      # lista de baralhos
    ├── study/     # tela de estudo, flip, avaliação, áudio e escolha de voz
    └── ...        # importação, gerenciamento de cards, prompts (prontos e próprios) e navegação
```

## Como executar

Requisitos: **Android Studio** recente (com JDK e Android SDK incluídos) e um aparelho ou emulador com Android 8.0 (API 26) ou superior.

```bash
git clone https://github.com/crisgdo/FlashLang.git
```

1. Abra a pasta do projeto no Android Studio e aguarde a sincronização do Gradle.
2. Selecione um emulador ou um celular com depuração USB ativada.
3. Clique em **Run**.

Pela linha de comando:

```bash
./gradlew assembleDebug   # gera o APK de debug
./gradlew test            # roda os testes unitários
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

### APK release

Use **Build → Generate Signed App Bundle / APK → APK** no Android Studio. Guarde a keystore (`.jks`) e as senhas **fora** da pasta do projeto: elas não devem ir para o Git, e sem elas não é possível instalar atualizações por cima do app.

## Limitações conhecidas

- **Qualidade do conteúdo gerado:** o IPA e as regras de pronúncia vêm da IA e podem conter erros, principalmente em palavras menos comuns. Confira num dicionário (Cambridge, Oxford) e use a tela de gerenciamento para corrigir ou remover cards.
- **Voz da Samsung:** o mecanismo de texto para voz da Samsung não pode ser usado por apps de terceiros nos aparelhos testados (o serviço dele não é exportado). O app usa o mecanismo padrão do sistema, normalmente o do Google, e permite escolher entre as vozes disponíveis.
- **Vozes online:** algumas vozes exigem conexão com a internet. As offline funcionam sem rede.
- **Prompts próprios** ficam apenas no aparelho e ainda não podem ser exportados.

## Ideias futuras

- **Gravar a própria voz** e compará-la com a do app
- **Reconhecimento de fala** para conferir se a palavra falada bate com o card
- Exportar e importar prompts próprios
- Estatísticas de progresso e sequência de estudo
- Lembretes diários de revisão

## Desenvolvimento

O projeto foi construído em etapas pequenas com o **Codex**, seguindo as regras do arquivo [`AGENTS.md`](AGENTS.md): uma tarefa por vez, revisão de cada alteração, build e testes a cada etapa e um commit por funcionalidade.

## Licença

Projeto pessoal de estudo. Defina uma licença (por exemplo, MIT) antes de permitir o reuso do código por terceiros.
