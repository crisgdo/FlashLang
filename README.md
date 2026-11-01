# FlashLang

Aplicativo Android de **flashcards para aprendizado de inglês e espanhol**. Você usa uma IA (ChatGPT, Gemini, Claude etc.) para gerar baralhos a partir de textos que leu, de listas de palavras ou de regras de pronúncia, importa o resultado no app e estuda com repetição espaçada e áudio.

Projeto pessoal de estudo: os baralhos ficam no próprio aparelho, sem conta e sem servidor.

## Como funciona

1. Abra a tela **Prompts**, escolha um prompt pronto (ou crie o seu) e preencha o que ele pede: o texto que você leu, uma lista de verbos, um foco de pronúncia etc.
2. Toque em **Copiar prompt** e cole numa IA.
3. Copie o JSON que a IA devolver e importe no app (colando o texto ou escolhendo um arquivo `.json`).
4. Toque no baralho para estudar: o card mostra o termo; ao tocar, vira e mostra a tradução, a frase de exemplo e, quando houver, a pronúncia (IPA) e a regra.

## Funcionalidades

### Prompts

- **Biblioteca de prompts prontos** para vocabulário e pronúncia, com campos de entrada (texto, lista, foco) e seletor de idioma.
- **Prompts próprios**: criar, editar, duplicar e excluir. Os prontos podem ser duplicados como cópia editável.
- **Bloco de formato automático**: o app anexa ao fim de cada prompt que gera baralho as instruções do formato JSON, então a resposta da IA sempre pode ser importada, mesmo que você edite o prompt.
- **Prompts que não geram baralho**, como o de conversa, sem o bloco de formato.

### Baralhos e estudo

- **Importação por JSON**, colado ou a partir de arquivo (Storage Access Framework).
- **Importador tolerante**: aceita JSON copiado de diferentes IAs, removendo blocos de código markdown, texto antes e depois do JSON, aspas curvas, espaços invisíveis, entidades HTML, barras invertidas no fim das linhas e vírgulas finais. Normaliza valores como `language` ("English", "inglês" → `en`) e `type` ("phrase", "expressão" → `expression`).
- **Mensagens de erro claras** em português, indicando o campo e o card com problema.
- **Card que vira** ao tocar, com **pronúncia (IPA)** e **regra** no verso, quando existirem.
- **Repetição espaçada** (versão simplificada do SM-2) com quatro avaliações: *Errei*, *Difícil*, *Bom* e *Fácil*.
- **Cards pendentes do dia** por baralho, com a opção "Estudar tudo mesmo assim" sem alterar o agendamento.
- **Pronúncia por áudio** (Text-to-Speech do Android) do termo e da frase de exemplo, com escolha de voz e de velocidade.
- **Gerenciamento de baralhos e cards**: busca, edição, exclusão, adição manual e renomeação.
- **Exportar e compartilhar** baralhos em JSON (backup e troca entre aparelhos).
- Interface em **Material 3** com ícones Outlined, em português do Brasil.

## Prompts prontos

| Categoria   | Prompt                                    |
| ----------- | ----------------------------------------- |
| Vocabulário | Vocabulário de textos que li              |
| Vocabulário | Phrasal verbs e colocações do texto       |
| Vocabulário | Falsos cognatos                           |
| Pronúncia   | Verbos terminados em -ed (/t/, /d/, /ɪd/) |
| Pronúncia   | Terminações -s e -es (/s/, /z/, /ɪz/)     |
| Pronúncia   | Sons das vogais: curto e longo            |
| Pronúncia   | Pares mínimos                             |
| Pronúncia   | Sons difíceis do inglês para brasileiros  |
| Pronúncia   | Acento da palavra                         |
| Pronúncia   | Sons do espanhol                          |
| Conversa    | Conversa sobre o texto (não gera baralho) |

### Marcadores em prompts próprios

Ao criar um prompt, os marcadores abaixo viram campos na tela de uso:

| Marcador     | O que vira                             |
| ------------ | -------------------------------------- |
| `{{IDIOMA}}` | Seletor de idioma (inglês ou espanhol) |
| `{{TEXTO}}`  | Campo de texto obrigatório             |
| `{{LISTA}}`  | Campo de lista obrigatório             |
| `{{FOCO}}`   | Campo opcional                         |

## Formato do JSON

json

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

| Campo                        | Valores                | Descrição                                                     |
| ---------------------------- | ---------------------- | ------------------------------------------------------------- |
| `deckName`                   | texto                  | Nome do baralho                                               |
| `language`                   | `en` ou `es`           | Idioma de estudo                                              |
| `cards[].term`               | texto                  | Palavra ou expressão                                          |
| `cards[].type`               | `word` ou `expression` | Tipo do termo                                                 |
| `cards[].termTranslation`    | texto                  | Tradução em português do Brasil                               |
| `cards[].example`            | texto                  | Frase de exemplo com o termo em contexto                      |
| `cards[].exampleTranslation` | texto                  | Tradução da frase                                             |
| `cards[].ipa`                | texto, opcional        | Pronúncia em IPA (também aceita `pronunciation` e `phonetic`) |
| `cards[].note`               | texto, opcional        | Regra ou observação (também aceita `rule` e `notes`)          |

> A pronúncia e as regras geradas por IA podem conter erros. Confira num dicionário (como Cambridge ou Oxford) os cards que parecerem estranhos e corrija-os na tela de gerenciamento.

## Repetição espaçada

Cada card guarda `easeFactor`, `intervalDays`, `repetitions` e `dueAt`. Depois de virar o card, você avalia como foi, e o agendador calcula quando ele volta:

| Avaliação   | Efeito                                                                                   |
| ----------- | ---------------------------------------------------------------------------------------- |
| **Errei**   | Zera o progresso, o card volta em 10 minutos e o fator de facilidade diminui             |
| **Difícil** | Intervalo cresce pouco e o fator de facilidade diminui                                   |
| **Bom**     | 1º acerto: 1 dia; 2º: 3 dias; depois o intervalo é multiplicado pelo fator de facilidade |
| **Fácil**   | Como "Bom", com intervalo maior e fator de facilidade aumentado                          |

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

text

```text
app/src/main/java/com/cristiano/flashlang/
├── data/          # entidades Room, DAOs, banco, repositórios, importador/exportador de JSON, agendador
└── ui/
    ├── home/      # lista de baralhos
    ├── study/     # tela de estudo, flip, avaliação e áudio
    └── ...        # prompts, importação, gerenciamento de cards e navegação
```

## Como executar

Requisitos: **Android Studio** recente (com JDK e Android SDK incluídos) e um aparelho ou emulador com Android 8.0 (API 26) ou superior.

bash

```bash
git clone <url-do-repositorio>
```

1. Abra a pasta do projeto no Android Studio e aguarde a sincronização do Gradle.
2. Selecione um emulador ou um celular com depuração USB ativada.
3. Clique em **Run**.

Pela linha de comando:

bash

```bash
./gradlew assembleDebug   # gera o APK de debug
./gradlew test            # roda os testes unitários
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

### APK release

Use **Build → Generate Signed App Bundle / APK → APK** no Android Studio. Guarde a keystore (`.jks`) e as senhas **fora** da pasta do projeto: elas não devem ir para o Git, e sem elas não é possível instalar atualizações por cima do app.

## Limitações conhecidas

- **Voz da Samsung:** o mecanismo de texto para voz da Samsung não pode ser usado por apps de terceiros nos aparelhos testados (o serviço dele não é exportado). O app usa o mecanismo padrão do sistema, normalmente o do Google, e permite escolher entre as vozes disponíveis.
- **Vozes online:** algumas vozes do Google exigem conexão com a internet. Vozes offline funcionam sem rede.
- **Qualidade dos cards:** depende da IA usada. Use a tela de gerenciamento para corrigir ou remover cards ruins.
- **Prompts próprios:** ainda não podem ser exportados para arquivo. Guarde o texto dos importantes em outro lugar.

## Ideias futuras

- Cards do tipo **cloze** (frase com lacuna na frente)
- Modo de estudo de **pronúncia** (ouvir, tentar falar e só depois virar o card)
- Exportar e importar prompts
- Estatísticas de progresso e sequência de estudo
- Lembretes diários de revisão

## Desenvolvimento

O projeto foi construído em etapas pequenas com o **Codex**, seguindo as regras do arquivo `AGENTS.md`: uma tarefa por vez, revisão de cada alteração, build e testes a cada etapa e um commit por funcionalidade.

## Licença

Projeto pessoal de estudo.

# FlashLang

Aplicativo Android de **flashcards para aprendizado de inglês e espanhol**, com uma biblioteca de **prompts para gerar baralhos com IA**. Você escolhe um prompt (vocabulário de um texto, pronúncia de verbos com -ed, sons das vogais etc.), copia para uma IA (ChatGPT, Gemini, Claude...), importa o JSON que ela devolver e estuda com repetição espaçada, pronúncia por áudio, IPA e regras no verso do card.

Projeto pessoal de estudo, sem conta e sem servidor: os baralhos e os prompts ficam no próprio aparelho.

## Como funciona

1. Abra a tela **Prompts** e escolha uma atividade, por exemplo "Vocabulário de textos que li".
2. Escolha o idioma, cole o texto (ou a lista de palavras) e toque em **Copiar prompt**. O app monta o prompt completo, já com o formato que ele sabe importar.
3. Cole o prompt numa IA e copie o JSON que ela devolver.
4. Na tela de importação, cole o JSON (ou escolha um arquivo `.json`).
5. Toque no baralho para estudar: o card mostra o termo; ao tocar, vira e mostra tradução, pronúncia (IPA), regra, frase de exemplo e tradução da frase.

## Funcionalidades

### Prompts

- **Biblioteca de prompts prontos**, organizados por categoria (Vocabulário, Pronúncia e Conversa).
- **Marcadores** nos prompts (`{{IDIOMA}}`, `{{TEXTO}}`, `{{LISTA}}`, `{{FOCO}}`): a tela mostra só os campos necessários e monta o prompt final.
- **Bloco de formato fixo**: o app anexa automaticamente ao fim de cada prompt que gera baralho o formato JSON esperado, então a importação não quebra mesmo que você edite o texto do prompt.
- **Prompts próprios**: criar, editar, duplicar e excluir. Os prontos podem ser duplicados como cópias editáveis. Um interruptor define se o prompt gera baralho (com bloco de formato) ou serve só para copiar texto, como o de conversa.

### Baralhos e importação

- Importação de JSON **colado ou de arquivo** (Storage Access Framework).
- **Importador tolerante**: aceita JSON copiado de diferentes IAs, removendo blocos de código markdown, texto antes e depois do JSON, aspas curvas, espaços invisíveis, entidades HTML, barras invertidas no fim das linhas e vírgulas finais. Normaliza valores como `language` ("English", "inglês" → `en`) e `type` ("phrase", "expressão" → `expression`).
- **Mensagens de erro claras** em português, indicando o campo e o card com problema.
- **Exportar e compartilhar** baralhos em JSON (backup e troca entre aparelhos).
- **Gerenciamento**: busca, edição, exclusão e adição manual de cards, e renomeação de baralhos.

### Estudo

- Card que **vira ao tocar** (animação de rotação).
- **Campos de pronúncia e regra**: IPA e uma nota opcionais no verso do card.
- **Repetição espaçada** (versão simplificada do SM-2) com quatro avaliações: *Errei*, *Difícil*, *Bom* e *Fácil*.
- **Cards pendentes do dia** por baralho, com a opção "Estudar tudo mesmo assim" sem alterar o agendamento.
- **Pronúncia por áudio** (Text-to-Speech do Android) do termo e da frase de exemplo, com **escolha de voz** (vozes offline e online, estas marcadas) e de **velocidade**.

### Interface

- Material 3, ícones Outlined, em português do Brasil.
- Ações de cada baralho (Gerenciar, Exportar, Excluir) em um menu de três pontos no card; tocar no card abre o estudo.

## Prompts prontos

| Prompt                                   | Categoria   | Para que serve                                                     |
| ---------------------------------------- | ----------- | ------------------------------------------------------------------ |
| Vocabulário de textos que li             | Vocabulário | Palavras e expressões de um texto, com tradução e frase de exemplo |
| Phrasal verbs e colocações do texto      | Vocabulário | Phrasal verbs e colocações que aparecem em um texto                |
| Falsos cognatos                          | Vocabulário | Palavras que parecem com o português mas têm outro sentido         |
| Pronúncia dos verbos terminados em -ed   | Pronúncia   | Passado regular: /t/, /d/ ou /ɪd/, com a regra                     |
| Terminações -s e -es                     | Pronúncia   | /s/, /z/ ou /ɪz/, com a regra                                      |
| Sons das vogais: curto e longo           | Pronúncia   | Regras de som curto e longo, "magic e", vogais dobradas            |
| Pares mínimos                            | Pronúncia   | Treino de contrastes como ship/sheep                               |
| Sons difíceis do inglês para brasileiros | Pronúncia   | th, r final, /æ/, h aspirado e outros                              |
| Acento da palavra                        | Pronúncia   | Sílaba tônica e contraste substantivo/verbo                        |
| Sons do espanhol                         | Pronúncia   | b/v, rr, ll/y, ñ, ditongos e tilde                                 |
| Conversa sobre o texto                   | Conversa    | Diálogo com correções, sem gerar baralho                           |

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

| Campo                        | Valores                | Obrigatório | Descrição                                                         |
| ---------------------------- | ---------------------- | ----------- | ----------------------------------------------------------------- |
| `deckName`                   | texto                  | sim         | Nome do baralho                                                   |
| `language`                   | `en` ou `es`           | sim         | Idioma de estudo                                                  |
| `cards[].term`               | texto                  | sim         | Palavra ou expressão                                              |
| `cards[].type`               | `word` ou `expression` | sim         | Tipo do termo                                                     |
| `cards[].termTranslation`    | texto                  | sim         | Tradução em português do Brasil                                   |
| `cards[].example`            | texto                  | sim         | Frase de exemplo com o termo em contexto                          |
| `cards[].exampleTranslation` | texto                  | sim         | Tradução da frase                                                 |
| `cards[].ipa`                | texto                  | não         | Pronúncia em IPA (também aceita `pronunciation` e `phonetic`)     |
| `cards[].note`               | texto                  | não         | Regra ou observação em português (também aceita `rule` e `notes`) |

Baralhos antigos, sem `ipa` e `note`, continuam válidos.

## Repetição espaçada

Cada card guarda `easeFactor`, `intervalDays`, `repetitions` e `dueAt`. Depois de virar o card, você avalia como foi, e o agendador calcula quando ele volta:

| Avaliação   | Efeito                                                                                   |
| ----------- | ---------------------------------------------------------------------------------------- |
| **Errei**   | Zera o progresso, o card volta em 10 minutos e o fator de facilidade diminui             |
| **Difícil** | Intervalo cresce pouco e o fator de facilidade diminui                                   |
| **Bom**     | 1º acerto: 1 dia; 2º: 3 dias; depois o intervalo é multiplicado pelo fator de facilidade |
| **Fácil**   | Como "Bom", com intervalo maior e fator de facilidade aumentado                          |

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

- Cards de **cloze** (frase com lacuna na frente) e um modo de estudo de pronúncia, em que o áudio toca antes de virar o card
- Exportar e importar prompts próprios
- Estatísticas de progresso e sequência de estudo
- Lembretes diários de revisão

## Desenvolvimento

O projeto foi construído em etapas pequenas com o **Codex**, seguindo as regras do arquivo [`AGENTS.md`](https://claude.ai/chat/AGENTS.md): uma tarefa por vez, revisão de cada alteração, build e testes a cada etapa e um commit por funcionalidade.

## Licença

Projeto pessoal de estudo.
