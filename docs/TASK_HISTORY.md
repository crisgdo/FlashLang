# FlashLang: histórico de tarefas

Cada tarefa foi enviada ao Codex como um prompt isolado, seguida de revisão do diff, build, testes, teste manual no celular e um commit. Os números são os rótulos usados nos prompts.

> Escrito a partir do histórico das conversas. Confirme o status real no `git log` e no código.

## Concluídas

| # | Tarefa | Resultado |
|---|---|---|
| 1 | Camada de dados | Room: `Deck`, `Card`, DAOs, `AppDatabase`, `DeckRepository`, testes |
| 2 | Tela inicial e navegação | Lista de baralhos, Navigation Compose, exclusão com confirmação |
| 3 | Importação por JSON | `DeckImporter`, validação, tela de importação |
| 4 | Botão "Copiar prompt" | Prompt de geração de cards na tela de importação |
| 5 | Tela de estudo | Card que vira (rotação), anterior/próximo, progresso |
| 6 | Importador tolerante | Limpeza de JSON de diferentes IAs (barras invertidas, entidades HTML, aspas curvas...) |
| 7 | Pronúncia por áudio | TextToSpeech, ícone de som, mensagem de idioma indisponível |
| 8 | Repetição espaçada | `Scheduler` (SM-2 simplificado), migração do Room, pendentes do dia |
| 9 | Exportar e importar arquivos | Exportação por compartilhamento e importação por arquivo (SAF) |
| 10 | Gerenciar cards | Lista com busca, editar, adicionar, excluir, renomear baralho |
| 11 | Ícone do app | Ícone adaptativo e monocromático a partir do desenho do autor |
| 12 | R8 | Minificação e redução de recursos no release |
| 13 a 15 | Card de baralho | Novo layout, botão Excluir, toque no card abre o estudo |
| 16, 16b, 18, 18b | Mecanismo de voz da Samsung | Tentativa e diagnóstico; descartada (serviço não exportado) |
| 17 | Voz e velocidade | Caixa "Voz" com teste, velocidade e escolha salva por idioma |
| 19 | Limpeza do TTS | Remoção do código da Samsung; vozes online marcadas |
| 20 e 21 | Menu do card | Menu de três pontos; conteúdo alinhado à esquerda |
| 22 | Campos `ipa` e `note` | Migração, importador, exportação, edição e verso do card |
| 23 | Tela de prompts | Prompts prontos, marcadores, `FORMAT_BLOCK`, copiar |
| 24 e 24b | Prompts próprios | Criar, editar, duplicar e excluir; `FlowRow` nos chips |
| 25 | Prompts extras | Terminações -s/-es, pares mínimos, sons difíceis, acento, falsos cognatos, phrasal verbs, espanhol, conversa |
| 26a | Cards de lacuna (cloze) | Campo `format`, importador, estudo, prompt de cloze |
| 26b | Modo Pronúncia | Áudio automático, ouvir devagar, mostrar palavra, preferência por baralho |
| 26c | Menu da tela de estudo | Ícones em todos os itens, modo atual visível |
| 27 | Categorias de baralhos | Campo `category` no `Deck` (nulo = sem categoria), migração, chips de filtro e seções na tela inicial, diálogo de categoria, gerenciar categorias, campo no JSON e na importação, linha de categoria no `FORMAT_BLOCK` para prompts prontos |
| 28 | Backup e restauração | Tela Backup: exportar tudo, só baralhos e só prompts (com progresso opcional), salvar em arquivo, compartilhar e restaurar com resumo e conflitos |
| 28b | Correção do compartilhamento | Erro de FileProvider ("Failed to find configured root") corrigido; função única de arquivo temporário em `cache/exports/` |
| A2 | Exportação de esquemas do Room e testes de migração | `exportSchema = true`, geração do `6.json` via KSP e testes unitários das migrações 1 a 6 (`AppDatabaseMigrationTest.kt`) |
| A3 | Leitura estrita no DeckImporter e restauração de backup | "Leitura estrita primeiro" em `DeckImporter.parse` (reparos de `cleanInput` apenas como fallback) e validação em memória no `BackupService` (`validateAndNormalize`), preservando aspas curvas e entidades HTML |
| A4 | Higienização de recursos e templates | Limpeza de duplicidade em `file_paths.xml`, remoção de cores mortas em `colors.xml`, alinhamento do tema base em `themes.xml` (`Theme.Material.NoActionBar`) e exclusão de testes de template (`ExampleUnitTest`, `ExampleInstrumentedTest`) |
| B1 | Contador de pendentes reativo ao tempo | Ticker periódico (a cada 30s) no `HomeViewModel` com `observeDecksWithCardCount(timeFlow)` via `flatMapLatest` no repositório; pausa automática com `WhileSubscribed(5_000)` e testes unitários determinísticos em `HomeViewModelTest.kt` |
| B2 | Ciclo de vida e retenção de estado Compose | Adoção de `collectAsStateWithLifecycle()` em todas as telas (`lifecycle-runtime-compose:2.9.4`) e `rememberSaveable` (com `TextFieldValue.Saver`) em editores e diálogos para preservar texto em rotações de tela |

- **Achado 5 (Correção de Diálogos na Rotação):**
  - Substituição de `remember` por `rememberSaveable` nos controladores de diálogos em `HomeScreen` (`deckIdToCategorize`, `deckIdToDelete`, `showCategoryManager`, `categoryToRename`, `categoryToDelete`) e `PromptsScreen` (`promptIdToDelete`).
  - Vinculação de `deckId` como chave em `rememberSaveable(deckId)` no `DeckCategoryDialog`, retendo o texto digitado em nova categoria e a opção selecionada após recreação de Activity.

Documentação: `README.md` atualizado até a Tarefa 26 (não menciona ainda o Backup).


## Ideias futuras

- Gravar a própria voz e comparar com a do app
- Reconhecimento de fala para conferir a pronúncia
- Estatísticas de progresso e sequência de estudo
- Lembretes diários de revisão
- Filtros de cards por tipo dentro do baralho (palavras, expressões, lacuna)

## Pendências antes da versão 1.0

- Revisão final das telas no celular (fonte normal e maior)
- Backup dos baralhos e dos prompts próprios
- APK release assinado, testado no aparelho
