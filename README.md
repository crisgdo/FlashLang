# FlashLang

Aplicativo Android de **flashcards para aprendizado de inglês e espanhol**. Você cola um texto numa IA (ChatGPT, Gemini, Claude etc.), recebe um baralho em JSON com palavras e expressões do texto, importa no app e estuda com repetição espaçada e pronúncia por áudio.

Projeto pessoal, 100% offline para estudo: os baralhos ficam no próprio aparelho, sem conta e sem servidor.

## Como funciona

1. No app, escolha o idioma de estudo e toque em **Copiar prompt**.
2. Cole o prompt numa IA, acrescente o seu texto depois de `TEXTO:` e envie.
3. Copie o JSON que a IA devolver e cole na tela de importação (ou importe de um arquivo `.json`).
4. Toque no baralho para estudar: o card mostra o termo; ao tocar, vira e mostra a tradução, uma frase de exemplo e a tradução da frase.

## Funcionalidades

- **Importação de baralhos por JSON**, colado ou a partir de arquivo (Storage Access Framework).
- **Importador tolerante**: aceita JSON copiado de diferentes IAs, removendo blocos de código markdown, texto antes e depois do JSON, aspas curvas, espaços invisíveis, entidades HTML, barras invertidas no fim das linhas e vírgulas finais. Normaliza valores como `language` ("English", "inglês" → `en`) e `type` ("phrase", "expressão" → `expression`).
- **Mensagens de erro claras** em português, indicando o campo e o card com problema.
- **Botão "Copiar prompt"** para gerar baralhos com qualquer IA.
- **Estudo com card que vira** ao tocar (animação de rotação).
- **Repetição espaçada** (versão simplificada do SM-2) com quatro avaliações: *Errei*, *Difícil*, *Bom* e *Fácil*.
- **Cards pendentes do dia** por baralho, com opção de "Estudar tudo mesmo assim" sem alterar o agendamento.
- **Pronúncia por áudio** (Text-to-Speech do Android) do termo e da frase de exemplo, com escolha de voz e de velocidade.
- **Gerenciamento de baralhos e cards**: busca, edição, exclusão, adição manual de cards e renomeação de baralho.
- **Exportar e compartilhar** baralhos em JSON (backup e troca entre aparelhos).
- Interface em **Material 3** com ícones Outlined, em português do Brasil.

## Formato do JSON

```json
{
  "deckName": "Texto sobre viagens",
  "language": "en",
  "cards": [
    {
      "term": "get along with",
      "type": "expression",
      "termTranslation": "dar-se bem com",
      "example": "She gets along with her coworkers.",
      "exampleTranslation": "Ela se dá bem com os colegas de trabalho."
    }
  ]
}
```

| Campo | Valores | Descrição |
|---|---|---|
| `deckName` | texto | Nome do baralho |
| `language` | `en` ou `es` | Idioma de estudo |
| `cards[].term` | texto | Palavra ou expressão |
| `cards[].type` | `word` ou `expression` | Tipo do termo |
| `cards[].termTranslation` | texto | Tradução em português do Brasil |
| `cards[].example` | texto | Frase de exemplo com o termo em contexto |
| `cards[].exampleTranslation` | texto | Tradução da frase |

### Prompt sugerido para a IA

O app copia um prompt equivalente a este (o texto exato fica no código):

```text
Leia o texto abaixo e gere de 15 a 25 flashcards com palavras e expressões
úteis para aprendizado de [inglês/espanhol]. Responda APENAS com JSON válido,
sem texto extra e sem bloco de código, neste formato: { "deckName",
"language", "cards": [ { "term", "type" (word|expression), "termTranslation",
"example", "exampleTranslation" } ] }. Use "en" para inglês e "es" para
espanhol no campo language. Traduções em português do Brasil. A frase de
exemplo deve usar o termo em contexto, de forma natural.

TEXTO:
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
- **Room** (com KSP) para o banco local, com migrações
- **Navigation Compose**
- **kotlinx.serialization** para leitura e geração do JSON
- **TextToSpeech** nativo do Android
- Testes unitários com JUnit
- **R8** com minificação e redução de recursos no build release

## Estrutura do projeto

```text
app/src/main/java/com/cristiano/flashlang/
├── data/          # entidades Room, DAOs, banco, repositório, importador/exportador de JSON, agendador
└── ui/
    ├── home/      # lista de baralhos
    ├── study/     # tela de estudo, flip, avaliação e áudio
    └── ...        # importação, gerenciamento de cards e navegação
```

## Como executar

Requisitos: **Android Studio** recente (com JDK e Android SDK incluídos) e um aparelho ou emulador com Android 8.0 (API 26) ou superior.

```bash
git clone <url-do-repositorio>
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

- **Voz da Samsung:** o mecanismo de texto para voz da Samsung não pode ser usado por apps de terceiros nos aparelhos testados (o serviço dele não é exportado). O app usa o mecanismo padrão do sistema, normalmente o do Google, e permite escolher entre as vozes disponíveis.
- **Vozes online:** algumas vozes do Google exigem conexão com a internet. Vozes offline funcionam sem rede.
- A qualidade dos cards depende da IA usada para gerar o JSON. Use a tela de gerenciamento para corrigir ou remover cards ruins.

## Ideias futuras

- Estatísticas de progresso e sequência de estudo
- Modo de estudo apenas de expressões ou apenas de palavras
- Lembretes diários de revisão
- Importação de baralhos por link ou QR code

## Desenvolvimento

O projeto foi construído em etapas pequenas com o **Codex**, seguindo as regras do arquivo [`AGENTS.md`](AGENTS.md): uma tarefa por vez, revisão de cada alteração, build e testes a cada etapa e um commit por funcionalidade.

## Licença

Projeto pessoal de estudo. Defina uma licença (por exemplo, MIT) antes de tornar o repositório público.
