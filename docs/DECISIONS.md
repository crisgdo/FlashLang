# FlashLang: decisões técnicas e de produto

Registro das decisões tomadas durante o desenvolvimento com o Codex. Serve de memória do projeto para qualquer agente (Antigravity, Gemini, Claude) ou pessoa que continue o trabalho.

> Este arquivo foi escrito a partir do histórico das conversas de desenvolvimento, não a partir de uma leitura do código. Na dúvida, o código vence: confirme antes de assumir e corrija este arquivo se algo estiver diferente.

## 1. Produto

App Android pessoal de flashcards para inglês e espanhol. O usuário gera baralhos com uma IA externa (prompts do próprio app), importa o JSON e estuda com repetição espaçada, áudio, IPA e regras de pronúncia. Sem conta, sem servidor, dados locais. Textos da interface em português do Brasil; nomes de classes e variáveis em inglês.

- Pacote: `com.cristiano.flashlang`
- Repositório: `github.com/crisgdo/FlashLang` (público). O remoto local se chama `FlashLang`, e não `origin`.

## 2. Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM: `ViewModel` + `StateFlow`
- Room (com KSP), Navigation Compose, kotlinx.serialization
- TextToSpeech nativo do Android
- Ícones: Material Icons Outlined (a biblioteca `material-icons-extended` foi aceita como dependência)
- Build release com R8 (`isMinifyEnabled` e `isShrinkResources` ligados)
- `gradle.properties` tem `android.disallowKotlinSourceSets=false`, exigido para o KSP funcionar com o Kotlin embutido do AGP. É uma solução temporária; pode deixar de ser necessária quando o KSP for atualizado.

## 3. Regras de trabalho (herdadas do AGENTS.md original)

1. Uma tarefa por vez, só o que foi pedido.
2. Não adicionar dependências sem avisar e explicar o motivo.
3. Ao terminar, rodar `./gradlew assembleDebug` e `./gradlew test` e corrigir erros.
4. Escrever testes unitários junto das funcionalidades (parser, repositório, agendador).
5. Resumir o que mudou e quais arquivos foram alterados.
6. Um commit por tarefa, com mensagem em português.

## 4. Dados e banco (Room)

- Entidades: `Deck`, `Card`, `UserPrompt`.
- `Card`: term, type (`word` | `expression`), termTranslation, example, exampleTranslation, `ipa?`, `note?`, `format` (`basic` | `cloze`), e os campos de repetição espaçada (`easeFactor`, `intervalDays`, `repetitions`, `dueAt`). A chave estrangeira para `Deck` tem exclusão em cascata.
- `UserPrompt`: title, description, body, `generatesDeck`, createdAt, updatedAt.
- **Regra inviolável:** toda mudança de esquema usa `Migration` do Room que preserva os dados. Nunca usar `fallbackToDestructiveMigration`. O usuário tem baralhos e progresso reais no aparelho.
- Preferências leves (modo de estudo por baralho, reprodução automática, voz e velocidade por idioma) ficam em SharedPreferences ou DataStore, e não no banco.

## 5. Importação de baralhos

- Um único ponto de entrada: `DeckImporter` (parser + validação + normalização). Tanto o JSON colado quanto o arquivo passam por ele.
- **Tolerância deliberada** a JSON copiado de IAs diferentes. Limpeza, nesta ordem: barra invertida antes de quebra de linha, entidades HTML (`&#x20;`, `&nbsp;` etc.), BOM e espaços invisíveis, aspas curvas, blocos de código markdown e texto antes do primeiro `{` e depois do último `}`. O parser aceita vírgula final e ignora chaves desconhecidas.
- Normalizações: `language` ("English", "inglês" → `en`; "spanish", "español" → `es`) e `type` ("phrase", "idiom", "expressão" → `expression`; "palavra" → `word`).
- Chaves alternativas: `pronunciation` e `phonetic` para `ipa`; `rule` e `notes` para `note`; `lacuna` para `cloze`.
- Erros em português, dizendo o campo e o número do card.
- Campos `ipa`, `note` e `format` são sempre opcionais. Baralhos antigos continuam válidos.
- Importar um baralho com nome já existente pergunta: Substituir, Manter os dois (sufixo) ou Cancelar.
- A exportação de um baralho usa o mesmo formato de importação e **não** inclui o progresso de estudo.

## 5b. Backup e compartilhamento de arquivos

- Tela **Backup**: exportar tudo, só baralhos ou só prompts próprios; opção de incluir o progresso de estudo; "Salvar em arquivo" (Storage Access Framework) e "Compartilhar"; restaurar de arquivo com resumo e tratamento de conflitos (manter os dois, substituir ou pular), aplicado em uma transação.
- Nome sugerido dos arquivos: `flashlang-backup-AAAA-MM-DD.json`.
- **Compartilhamento usa FileProvider.** Os arquivos temporários são gravados em `cacheDir/exports/` por uma **função única**, usada tanto pela exportação de um baralho quanto pelos backups. O XML do FileProvider declara `<cache-path name="exports" path="exports/" />`. Gravar em qualquer outra pasta do cache causa o erro "Failed to find configured root".
- Os arquivos antigos de `exports/` são apagados antes de gravar um novo.

## 6. Cards de lacuna (cloze)

- No card de lacuna, `example` contém a frase com `___` e `term` é **exatamente a palavra que preenche a lacuna**, na forma usada na frase. A forma base vai em `note`.
- O importador normaliza variações da lacuna (`____`, `[...]`, `…`) para `___`. Se a lacuna faltar mas o termo estiver na frase, ele a cria sozinho; se nenhum dos dois existir, devolve erro.
- Frente: frase com `___` e a tradução como dica, sem áudio. Verso: frase completa com a resposta em destaque; o áudio lê a frase completa.

## 7. Repetição espaçada

- Função pura `Scheduler`, separada da interface e coberta por testes.
- Avaliações: AGAIN (Errei), HARD (Difícil), GOOD (Bom), EASY (Fácil).
- AGAIN: zera repetições e intervalo, volta em 10 minutos, `easeFactor` −0,2 (mínimo 1,3). HARD: intervalo = max(1, anterior × 1,2), `easeFactor` −0,15. GOOD: 1 dia, depois 3 dias, depois anterior × `easeFactor`. EASY: como GOOD com × 1,3 e `easeFactor` +0,15.
- Baralho mostra "N para revisar hoje". A tela de estudo usa só os pendentes, em ordem aleatória, e oferece "Estudar tudo mesmo assim" sem alterar o agendamento.

## 8. Estudo e áudio

- Modos por baralho: **Padrão** e **Pronúncia (ouvir primeiro)** (áudio automático, "Ouvir de novo", "Ouvir devagar" a 0,6× da velocidade escolhida, "Mostrar palavra").
- O áudio usa o mecanismo de voz padrão do sistema. A caixa "Voz" lista as vozes do idioma do baralho (en-US e es-ES), marca as online com "(online)", permite testar e escolher a velocidade, e salva a escolha por idioma.
- O manifesto declara em `<queries>` a intent `android.intent.action.TTS_SERVICE` (necessário no Android 11+).
- **Decisão fechada:** o mecanismo de voz da Samsung (`com.samsung.SMT`) **não** é utilizável por apps de terceiros: o serviço dele é declarado com `exported=false`. A tentativa foi removida do código. Não reintroduzir.
- Log de diagnóstico do TTS só em `BuildConfig.DEBUG`.

## 9. Prompts

- Prompts prontos ficam em código (`PromptTemplates.kt`); prompts do usuário ficam no Room.
- Marcadores: `{{IDIOMA}}`, `{{TEXTO}}`, `{{LISTA}}`, `{{FOCO}}`. Marcador sem valor vira "(não informado)".
- **Fonte única do formato:** a constante `FORMAT_BLOCK` é anexada automaticamente ao fim de todo prompt que gera baralho. Assim, o usuário pode editar o corpo do prompt sem quebrar a importação. Prompts com `generatesDeck = false` (ex.: conversa) não recebem o bloco e não oferecem "Ir para importar".
- Prompts prontos não podem ser editados nem excluídos; só duplicados como prompts próprios.
- **Não encurtar nem reescrever** os textos dos prompts de pronúncia (regras de /t/, /d/, /ɪd/; /s/, /z/, /ɪz/; vogais). Eles são a base dos cards.
- Pronúncia: inglês americano (casa com a voz en-US do app) e espanhol da Espanha (es-ES). A IA deve omitir a palavra quando não tiver certeza do IPA.
- Prompts prontos: Vocabulário de textos, Phrasal verbs e colocações, Falsos cognatos, Cloze, Verbos -ed, Terminações -s/-es, Sons das vogais, Pares mínimos, Sons difíceis do inglês, Acento da palavra, Sons do espanhol e Conversa sobre o texto.

## 10. Padrões de interface

- Material 3, tema escuro como referência visual atual. Textos em português.
- **Todos os ícones Outlined** e todo item de menu com `leadingIcon`. Botões de ação com área de toque mínima de 48dp.
- Card de baralho: conteúdo alinhado à esquerda, título em até 2 linhas com reticências, menu de três pontos no canto superior direito (Gerenciar, Exportar e, separado por divisor, Excluir em vermelho com confirmação). **Tocar no corpo do card abre o estudo.**
- Linhas de chips e botões usam `FlowRow` (nunca `Row` sem quebra) para não espremer o conteúdo com fonte maior.
- Respeitar os insets da barra de status e da barra de navegação em todas as telas.
- Ações destrutivas sempre com confirmação.

## 11. Release e segurança

- Keystore (`.jks`) e senhas **fora** da pasta do projeto e fora do Git. Assinar pelo assistente do Android Studio (Generate Signed App Bundle / APK).
- Nada de chave de API no app. O app não tem backend nem rede, exceto vozes online do mecanismo do sistema.
- Verificar antes de cada push que não há `.jks`, `keystore` nem `local.properties` versionados.
- `versionCode` e `versionName` só mudam ao publicar uma versão nova.

## 12. Git

- Remoto chamado `FlashLang`. Para evitar o editor Vim em merges: `git pull --no-edit`.
- Nunca usar `git push --force`.
- O `README.md` foi criado primeiro no GitHub e depois substituído pela versão completa local.

## 13. Limitações conhecidas

- IPA e regras vêm da IA e podem estar errados; conferir em dicionário.
- Vozes online exigem internet.
