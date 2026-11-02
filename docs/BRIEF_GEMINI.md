# FlashLang: briefing para o Gemini

Documento de passagem de contexto. Você (Gemini) vai atuar como **arquiteto de software, engenheiro de prompts e consultor de UX/UI** do projeto FlashLang, guiando o dono do projeto, passo a passo, na migração do Codex para o Google Antigravity e na evolução visual do app. A implementação será feita pelo agente do Antigravity; você planeja, analisa e escreve os prompts para ele.

> Este briefing resume uma análise feita por leitura do código e de capturas de tela. Nada aqui foi executado. Quando algo parecer diferente do que o código mostra, **o código vence**: avise e corrija o briefing.

## 1. Como conduzir

1. Responda em português do Brasil. Dê comandos prontos para **PowerShell no Windows 11**.
2. Conduza **uma etapa por vez**. Não avance sem o dono confirmar o resultado da etapa anterior (saída de comandos, capturas, testes).
3. **Analise antes de implementar.** Nenhuma alteração de código sem plano aprovado.
4. Quando houver risco ou decisão de arquitetura, explique o problema e dê alternativas **antes** de prosseguir.
5. O dono aprende fazendo: explique o porquê em poucas linhas, sem encher de teoria.
6. Quando precisar ver código, peça os arquivos exatos que precisa em vez de supor.

## 2. O projeto

App Android pessoal de **flashcards para inglês e espanhol**. O usuário gera baralhos com uma IA externa usando prompts do próprio app, importa o JSON e estuda com repetição espaçada, áudio (TTS), IPA e regras de pronúncia. Tudo local: sem conta, sem servidor, sem chave de API.

Funcionalidades: importação de JSON tolerante, cards básicos e de lacuna (cloze), repetição espaçada (SM-2 simplificado), dois modos de estudo (Padrão e Pronúncia), escolha de voz e velocidade, categorias de baralhos, biblioteca de 12 prompts prontos e prompts próprios, backup e restauração (com progresso), exportação por compartilhamento.

## 3. Ambiente

| Item | Valor |
|---|---|
| Sistema | Windows 11, PowerShell |
| Pacote | `com.cristiano.flashlang` |
| Stack | Kotlin 2.2.10, Jetpack Compose (Material 3), MVVM (`ViewModel` + `StateFlow`), Room 2.8.4 (KSP), Navigation Compose, kotlinx.serialization, TextToSpeech |
| Build | AGP 9.5.0, Gradle 9.6.0, JDK 21, `compileSdk`/`targetSdk` 37, `minSdk` 26, `versionCode` 1 / `versionName` "1.0" |
| Aparelho de teste | Samsung Galaxy S25 Ultra, Android 17, One UI 9.0 |
| Repositório | `github.com/crisgdo/FlashLang` (**público**). O remoto local se chama **`FlashLang`**, não `origin` |
| Branches e tags | `main` (estado final do Codex), tag `codex-final`, branch de trabalho `antigravity/setup` |
| Ferramentas | Android Studio (emulador, Logcat, instalar no celular), Antigravity IDE (edição e agente), Git |

Validação de build já feita no terminal do Antigravity: `.\gradlew.bat assembleDebug` e `.\gradlew.bat test` passam (`BUILD SUCCESSFUL`).

## 4. Estado atual da migração

| Etapa | Situação |
|---|---|
| Checkpoint Git (`codex-final`, branch `antigravity/setup`) | Concluído e enviado ao GitHub |
| Documentos `docs/DECISIONS.md` e `docs/TASK_HISTORY.md` | Concluídos (ajustar a Tarefa 27 para "concluída") |
| Projeto aberto no Antigravity, build e testes validados | Concluído |
| Pacote de contexto (`context-core.md`, `context-ui.md`) e capturas de tela | Enviados e analisados |
| **Etapa 2:** `AGENTS.md` novo e regras permanentes do Antigravity | **Próximo passo** |
| Etapa 3: modelo de prompt e processo de revisão | Pendente |
| Etapa 4: evolução visual | Pendente (depende da paleta e do ícone) |
| Etapa 5: fluxo contínuo com Git | Pendente |

Decisões do dono ainda abertas: **paleta** (tema escuro: A, B ou C; ver seção 7) e **envio do ícone do app** (`icon-source/icon.png`), que deve orientar a paleta.

## 5. Achados no código

Gravidade pela ordem em que devem ser tratados.

| # | Achado | Gravidade |
|---|---|---|
| 1 | `DeckImporter.cleanInput` "conserta" o texto antes de ler o JSON: troca aspas curvas `“ ”` por `"`, decodifica entidades HTML (`&amp;`, `&#..;`) e apaga crases, **inclusive dentro do conteúdo dos cards**. Um valor com `“` gera JSON inválido; um `&amp;` vira `&`. A restauração de backup (`BackupService.parseInput`) reserializa e passa pelo mesmo importador, então **backups podem falhar ou alterar texto**. Ainda não foi provado por teste. Correção sugerida: tentar a leitura estrita primeiro e só reparar se falhar; no backup, não reparar. | Alto |
| 2 | `AppDatabase` tem `exportSchema = false`, versão 6 e migrações 1→6 sem teste. Os testes de repositório parecem usar um DAO falso (a confirmar), então as consultas SQL (`LOWER`, `TRIM`) também não são testadas. | Alto |
| 3 | Três caminhos gravam baralhos (`DeckRepository`, `DeckDao`, `RoomBackupDataStore`), com tratamento diferente de categoria; restaurar pode criar categorias duplicadas por diferença de maiúsculas. O `LOWER` do SQLite só trata ASCII. | Médio |
| 4 | `HomeViewModel` chama `observeDecksWithCardCount()` com `now` capturado uma vez; "para revisar hoje" não acompanha o relógio (ex.: card respondido com "Errei" volta em 10 minutos mas não entra na contagem). | Médio |
| 5 | Nenhum `rememberSaveable`; os editores (prompt e card) perdem o texto ao girar a tela. Coleta com `collectAsState` em vez da versão atenta ao ciclo de vida. | Médio |
| 6 | `buildImportPrompt` (tela de importar) produz "TEXTO: (não informado)" no lugar do texto. | Baixo |
| 7 | Três conjuntos de classes JSON quase iguais: `ImportDeckJson`/`ImportCardJson`, `ExportDeckJson`/`ExportCardJson`, `BackupDeckJson`/`BackupCardJson`. | Manutenção |
| 8 | `DeckDao` tem consultas de categoria (`renameCategory`, `removeCategory`, `findCategory`) que o repositório não usa; ele refaz a lógica em Kotlin. | Manutenção |
| 9 | `type`, `format` e `language` são `String`, não enums. | Manutenção |
| 10 | `FlashLangNavHost` contém lógica (exportação de baralho, criação do `BackupService` a partir do `AppDatabase`, rotas montadas com texto solto, `getDecks().first {}` para achar um baralho). | Manutenção |
| 11 | `StudyScreen.kt` (823 linhas) contém também `SpeechEngineController`. | Manutenção |
| 12 | Todo texto da interface está no código; `strings.xml` só tem `app_name`. | Manutenção |
| 13 | Dependências desalinhadas: `lifecycle-runtime-ktx` 2.6.1 com `lifecycle-viewmodel-compose` 2.9.4; `activity-compose` 1.8.0 e `core-ktx` 1.10.1 antigos. | Baixo |
| 14 | Higiene: arquivos de `.idea/` versionados (`deploymentTargetSelector.xml`, `deviceManager.xml` podem registrar dados do aparelho; repositório público), `file_paths.xml` com duas entradas iguais, `ExampleUnitTest`, cores roxas do template, tema XML `Theme.FlashLang` herdando de `Material.Light`, `AGENTS.md` ainda na versão inicial de 28 linhas, `allowBackup="true"` com regras vazias. | Baixo |
| 15 | `Theme.kt` é o modelo padrão do Android Studio: cor dinâmica ligada, paleta roxa/rosa nunca exibida, tipografia só com `bodyLarge`. Nenhuma tela usa cor fixa (tudo vem de `MaterialTheme`), o que torna a evolução visual barata. | Informativo |

Pontos positivos: camadas `data/` e `ui/` claras, `Scheduler` como função pura, `BackupDataStore` como interface testável, restauração atômica, cerca de 100 testes unitários, nenhuma cor fixa nas telas.

## 6. Achados visuais (capturas das telas)

- **Contraste de superfície fraco:** card sobre fundo com razão de cerca de 1,9:1 no tema escuro e 1,17:1 no claro. Chip selecionado quase indistinguível.
- **Inconsistência causada pela cor dinâmica:** o botão `+` é azul-escuro no tema escuro e azul-pastel no claro.
- **Botões sem hierarquia:** todos preenchidos no mesmo azul-pastel (Backup, Importar, Detalhe do prompt). No detalhe do prompt, "Copiar prompt" (principal) está cinza e "Ir para importar" parece o principal.
- **Estudo:** card grande com muito espaço vazio; no verso, a tradução tem o mesmo tamanho do termo; IPA e regra (centro das cartas de pronúncia) aparecem pequenos; os quatro botões de avaliação têm a mesma cor, larguras diferentes e **sem o intervalo previsto** ("10 min", "1 d", "3 d"); progresso só como texto "1 / 25"; título "Estudar" sem o nome do baralho; possível canto reto atrás do card arredondado (a confirmar).
- **Início:** título do baralho e cabeçalho de seção com peso parecido; a quantidade para revisar (o dado mais importante) não se destaca; ícone de brilhos (prompts) ambíguo; botão `+` cobre o último item.
- **Lista de prompts:** três níveis de título quase iguais; `+` cobre o último card; 12 prompts em uma rolagem sem filtro.
- **Detalhe do prompt:** prévia grande aberta por padrão empurra as ações para o fim; o bloco de formato tem quebras de linha no meio das frases.
- **Backup:** três botões idênticos; a caixa "Exportação pronta" distribui as três ações de forma irregular.
- **Importar:** dois botões preenchidos concorrem; a ordem dos campos não segue o fluxo real; falta um botão "Colar".
- **Gerenciar:** lápis repetido em cada linha; título longo em duas linhas na barra.
- **Voz:** nomes técnicos das vozes (`en-us-x-iob-network`) como subtítulo; sem agrupamento Offline e Online.
- **Padrões gerais:** botão flutuante cobrindo conteúdo em várias telas, rádios para escolhas binárias (botões segmentados seriam mais compactos), ação principal no fim da rolagem (Salvar, Importar, Copiar), textos em português com tipografia padrão do sistema.

## 7. Direção visual (decisão do dono)

O dono quer **paleta própria, sem Material You**. Candidatas para o tema escuro (contrastes verificados, todos acima de 6:1 para texto, botões e destaques):

| Direção | Fundo / superfície | Primária | Destaque |
|---|---|---|---|
| A. Índigo e âmbar | `#121218` / `#1D1D27` | `#8E9CFF` | `#FFB547` |
| B. Verde-água e coral | `#0F1514` / `#18211F` | `#5ED3B8` | `#FF9A76` |
| C. Grafite e cobalto | `#16181D` / `#20242B` | `#78A6FF` | `#7BE0A6` |

Texto principal entre `#E3EEEB` e `#E8EAF0`; erro `#FF8A80`. O tema claro deve ser derivado depois, e o app deve seguir o modo do sistema. A paleta final deve harmonizar com o ícone do app (cor de fundo do ícone: `#242424`).

Princípios: preservar a identidade e a estrutura atuais (não é uma reformulação aleatória), definir **tokens** (cores, tipografia, formas, espaçamento) no tema e só depois aplicar tela por tela, a partir de componentes compartilhados.

## 8. Plano

| Fase | Conteúdo |
|---|---|
| **A. Rede de segurança** | A1: reescrever o `AGENTS.md` e criar as regras do Antigravity. A2: `exportSchema = true` e testes de migração. A3: teste que prova o achado 1 e correção (ler estrito primeiro). A4: limpeza de `.idea`, `file_paths.xml` e restos do template. |
| **B. Comportamento** | B1: contador "para revisar hoje" acompanhando o relógio. B2: `rememberSaveable` e coleta atenta ao ciclo de vida. B3: prompt de importação sem "(não informado)". B4: bloco de formato reescrito em parágrafos e JSON de exemplo bem formatado. |
| **C. Identidade visual** | C1: tema (paletas escura e clara, tipografia, formas, espaçamentos), sem cor dinâmica, e ajuste do tema XML. C2: componentes compartilhados (cartão de baralho, botões principal e secundário, seleção segmentada, cabeçalho de seção, barra de avaliação com intervalos, seção de formulário). C3: telas na ordem Início, Estudo, Prompts (lista e detalhe), Importar, Backup, Gerenciar e Editar, corrigindo o botão flutuante e as ações fixas. |
| **D. Refatorações** | Um único conjunto de classes JSON, enums, `NavHost` mais enxuto, controlador de voz fora de `StudyScreen.kt`, textos em `strings.xml`. Só com os testes protegendo; opcional. |

Cada tarefa: uma por vez, build e testes verdes, teste no celular, um commit na branch; só vai para a `main` o que foi validado.

## 9. Regras de segurança (base do novo AGENTS.md)

O agente do Antigravity deve:

1. **Analisar a arquitetura antes de modificar** qualquer código, e apresentar o plano antes de editar.
2. Identificar e **seguir os padrões e convenções existentes** (MVVM, repositórios, `StateFlow`, nomes em inglês, textos da interface em português do Brasil).
3. **Reaproveitar** componentes, funções e estilos; **não duplicar** código nem adicionar dependências sem aprovação e justificativa.
4. **Preservar as funcionalidades existentes.** Não alterar regras de negócio (agendador, importador, formato do JSON, backup) sem autorização.
5. **Não alterar o banco de dados, o esquema, as migrações, o formato dos arquivos de importação, exportação e backup** sem necessidade e aprovação. Se for inevitável: migração do Room que preserva os dados; **nunca** `fallbackToDestructiveMigration`.
6. Fazer **alterações pequenas, organizadas e incrementais**, uma tarefa por vez.
7. **Verificar o impacto** em outras partes do app antes de mexer em algo compartilhado.
8. Rodar `.\gradlew.bat assembleDebug` e `.\gradlew.bat test` após cada implementação e corrigir as falhas causadas por ela.
9. Informar **quais arquivos foram modificados e por quê**.
10. Quando a alteração for arriscada ou exigir decisão arquitetural, **explicar o problema e apresentar alternativas antes de prosseguir**.
11. Nunca usar `git push --force`, nunca versionar `.jks`, `local.properties`, senhas ou chaves, e nunca reintroduzir a tentativa de usar o mecanismo de voz da Samsung (o serviço dele não é exportado).

## 10. Modelo de prompt para o Antigravity

```text
CONTEXTO
Leia AGENTS.md e docs/DECISIONS.md antes de começar. [Resumo de 2 a 4 linhas do estado da área afetada.]

OBJETIVO
[Uma frase: o que deve mudar para o usuário ou para o código.]

ARQUIVOS PROVÁVEIS
[Lista de caminhos que você identificou. O agente confirma e pode acrescentar.]

PADRÕES A SEGUIR
[Padrões do projeto que se aplicam: componentes a reaproveitar, convenção de nomes, arquitetura.]

RESTRIÇÕES
[O que NÃO pode mudar: esquema do banco, formato do JSON, regras do agendador, textos de prompts etc.]

ANTES DE EDITAR
Apresente seu plano: arquivos que vai tocar, riscos e como validará. Aguarde minha aprovação.

VALIDAÇÃO
Rode .\gradlew.bat assembleDebug e .\gradlew.bat test. [Testes novos exigidos.]

ENTREGA
Resuma o que mudou, liste os arquivos alterados com o motivo, e diga o que eu devo testar no celular.
```

Exemplo preenchido (achado 4, contador de pendentes): objetivo "o contador 'para revisar hoje' deve refletir o horário atual"; arquivos `ui/home/HomeViewModel.kt`, `data/repository/DeckRepository.kt`, `data/local/DeckDao.kt`; restrições "não mudar a regra de vencimento `dueAt <= agora` nem o agendador"; testes: a contagem muda quando o tempo avança.

## 11. Fluxo de trabalho com o dono

1. O dono apresenta uma necessidade, melhoria ou bug.
2. Você pede os arquivos de que precisa (ele os cola ou anexa) e analisa o impacto.
3. Você escreve o prompt para o Antigravity usando o modelo da seção 10.
4. O dono cola no Antigravity em modo de **planejamento**, revisa o plano e o aprova.
5. O agente implementa e valida; o dono testa no celular e confere `git diff`.
6. O dono faz o commit na branch; você confere o resultado e passa para a próxima tarefa.

Atualize `docs/DECISIONS.md` e `docs/TASK_HISTORY.md` ao fim de cada tarefa relevante.

## 12. Armadilhas conhecidas

- O remoto Git é **`FlashLang`**. Use `git pull --no-edit` para não abrir o Vim.
- A tag `codex-final` é anotada: `git rev-parse "codex-final^{commit}"` mostra o commit.
- `.\gradlew.bat` no PowerShell; se `JAVA_HOME` faltar: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`.
- Testes instrumentados (`connectedAndroidTest`) precisam de aparelho ou emulador; o agente não consegue rodá-los sozinho.
- Erros de rede com o GitHub já ocorreram (porta 443); um `git push` pode falhar sem o repositório estar errado.
- Arquivos de contexto (`context-*.md`) estão no `.gitignore`; não os versione.

## 13. Arquivos para anexar na conversa com o Gemini

`docs/DECISIONS.md`, `docs/TASK_HISTORY.md`, `AGENTS.md`, `context-core.md`, `context-ui.md`, as capturas de tela das telas e o ícone (`icon-source/icon.png`).

## 14. Primeira tarefa para você (Gemini)

1. Confirme que entendeu o projeto e o plano em até 10 linhas, e liste dúvidas ou divergências com o código anexado.
2. Conduza a **Etapa 2**: proponha o novo `AGENTS.md` (curto, com as regras da seção 9 e o essencial de `docs/DECISIONS.md`) e os arquivos de regras do Antigravity; explique onde ficam e como validar que o agente os leu.
3. Só depois de eu confirmar que o agente respeita as regras, passe para a Etapa 3.
