# FlashLang — Diretrizes do Agente (AGENTS.md)

## 1. Visão Geral e Stack

App Android local de flashcards (inglês `en-US` e espanhol `es-ES`) com traduções e interface em português do Brasil (`pt-BR`) e código (classes, funções, variáveis) em inglês.

- **Pacote:** `com.cristiano.flashlang`
- **Stack:** Kotlin 2.2.10, Jetpack Compose (Material 3), MVVM (`ViewModel` + `StateFlow`), Room 2.8.4 (KSP), Navigation Compose, `kotlinx.serialization`, Android `TextToSpeech`.
- **Remoto Git:** chama-se `FlashLang` (não `origin`).

## 2. Regras Invioláveis de Arquitetura e Segurança

1. **Planejar antes de editar:** Analise a arquitetura e os arquivos afetados antes de modificar qualquer código. Apresente o plano (arquivos, riscos e validação) e aguarde aprovação.

2. **Uma tarefa por vez:** Faça alterações pequenas, incrementais e restritas ao que foi pedido.

3. **Seguir padrões existentes:** Mantenha MVVM (`ViewModel` + `StateFlow`), repositórios como fonte de dados, código em inglês, textos de UI e mensagens de erro em português do Brasil, todos os ícones `Outlined`, áreas de toque mínimas de 48dp e `FlowRow` para linhas de chips/botões.

4. **Reaproveitar e não duplicar:** Reutilize componentes, funções e estilos existentes. Não adicione dependências em `libs.versions.toml` ou `build.gradle.kts` sem autorização prévia e justificativa.

5. **Preservar regras de negócio:** Não altere o funcionamento do `Scheduler` (SM-2), `DeckImporter`, `ClozeText`, `BackupService` ou os textos pedagógicos dos prompts de pronúncia em `PromptTemplates.kt` sem autorização explícita. Prompts que geram baralho devem sempre incluir o `FORMAT_BLOCK`, anexado ao final pelo construtor de prompts.

6. **Proteção absoluta do Banco de Dados e Arquivos:**
   
   - **NUNCA** use `fallbackToDestructiveMigration`. O usuário possui dados reais.
   - Não altere entidades Room, esquema, migrações (`MIGRATION_1_2` a `MIGRATION_5_6`) ou formato JSON de importação/exportação/backup sem aprovação. Qualquer mudança de banco exige `Migration` explícita preservando dados e teste correspondente.
   - Compartilhamento de arquivos deve usar exclusivamente `writeTemporaryExportAndGetUri` / `writeShareableExport` gravando em `cacheDir/exports/` (configurado no `FileProvider`).

7. **TextToSpeech (TTS):** Use apenas o mecanismo padrão do sistema. **Nunca** reintroduza código tentando acessar o engine da Samsung (`com.samsung.SMT`), pois o serviço não é exportado. Logs do TTS devem ficar restritos a `BuildConfig.DEBUG`.

8. **Segurança e Git:**
   
   - **Nunca** execute `git push --force`.
   - **Nunca** versione `.jks`, keystores, `local.properties`, senhas, chaves ou arquivos `context-*.md`.

9. **Escopo e arquivos:**
   
   - Antes de começar, leia `docs/DECISIONS.md` e `docs/TASK_HISTORY.md`. Ao concluir, proponha (sem aplicar) as linhas a acrescentar neles.
   - Edite só os arquivos necessários para a tarefa. Não reformate, renomeie, mova nem apague arquivos fora do escopo.

10. **Comandos destrutivos e Git:**
    
    - Não execute `git reset --hard`, `git clean`, `git checkout -- .`, `git restore .`, `rm -rf` nem equivalentes sem aprovação explícita.
    - Não execute `git commit`, `git push`, `git merge` nem `git rebase`. Sugira a mensagem de commit; o dono faz o commit.

11. **Testes e build:**
    
    - Nunca remova, desative nem relaxe testes para fazer o build passar. Se um teste falhar por causa da sua mudança, corrija o código; se o teste estiver errado, explique e peça aprovação.
    - Testes que exigem aparelho ou emulador (`connectedAndroidTest`) não podem ser executados por você. Não os simule nem os ignore em silêncio: avise o dono do que ficou sem validar.

12. **Riscos e decisões:** Se a tarefa for arriscada ou exigir uma decisão de arquitetura (esquema, formato de arquivo, nova dependência, mudança de padrão), pare, explique o problema e apresente alternativas com prós e contras antes de prosseguir.

## 3. Validação e Entrega Obrigatórias

- Antes de mexer em código compartilhado, verifique o impacto em todas as telas e testes que o consomem.
- Ao concluir qualquer alteração, rode no PowerShell:
  `.\gradlew.bat assembleDebug`
  `.\gradlew.bat test`
- Corrija imediatamente qualquer erro de compilação ou falha de teste introduzida.
- Na entrega final da tarefa, informe:
  1. Resumo objetivo do que mudou.
  2. Lista exata de arquivos modificados e o motivo de cada um.
  3. O que o dono do projeto deve testar manualmente no celular.
