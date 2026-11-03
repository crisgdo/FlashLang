# FlashLang — Progresso da Implementação do Redesign

> **Referência Oficial:** `FlashLang_todas_as_telas_v2.html`  
> **Diretrizes e Regras:** `AGENTS.md` e `docs/DECISIONS.md`  
> **Status Geral:** Em andamento (Fase 2 de 4 concluída)

---

## 1. Sistema Base & Design Tokens

| Módulo / Token | Especificação | Status |
|---|---|---|
| **Paleta de Cores** (C1a) | Índigo & Âmbar (Opção A) nos modos Claro e Escuro, contraste WCAG AA | **Concluído** |
| **Escala Tipográfica** (C1b) | 15 papéis M3 em `Type.kt` | **Concluído** |
| **Escala de Formas & Espaçamento** (C1c) | `Shape.kt` (r-sm 10dp, r-md 14dp, r-lg 20dp, 999dp) e `Spacing.kt` | **Concluído** |
| **Cold-start & Cores de Sistema** | `values-night/themes.xml` e `MainActivity.kt` com fundo contínuo | **Concluído** |

---

## 2. Status Tela a Tela

| Grupo no HTML | Tela / Arquivo | Componentes Principais | Status |
|---|---|---|---|
| **Grupo 2: Estudo** | `StudyScreen.kt` | • Cartões empilhados 3D com perspectiva<br>• Pills r-pill para fonética/tradução<br>• Grade 2x2 semântica de botões de avaliação (`StudyRatingButtons.kt`)<br>• Áudio contextual (termo na frente, frase no verso)<br>• TopAppBar contínua com `background` | **Concluído** (C2a - C2d) |
| **Grupo 1: Início** | `HomeScreen.kt` | • Hero banner 20dp com contagem 32sp bold<br>• Filtro por chips em pílula<br>• Seções colapsáveis com chevron animado<br>• Linhas de baralho 14dp com badges cápsula<br>• Extended FAB 18dp com glow sutil | **Concluído** (C3a) |
| **Grupo 1: Gerenciar** | `ManageCardsScreen.kt` | • Barra de busca em pílula (`CircleShape`, fundo `surface`)<br>• Linha de contadores dinâmicos (total e para revisar hoje)<br>• Term rows com divisores e setas de navegação<br>• Menu suspenso (Novo card, Renomear)<br>• Extended FAB "Estudar" (18dp) | **Concluído** (C2e) |
| **Grupo 2: Editar card** | `CardEditorScreen.kt` | • Controles segmentados `.seg` em 2 colunas (Formato e Tipo)<br>• Inputs `.input` com cantos de 10dp e labels superiores (12sp `muted`)<br>• Dica contextual para formato Lacuna<br>• Botão Salvar `.btn-primary` (14dp)<br>• Botão Excluir `.btn-danger` com borda avermelhada e confirmação | **Concluído** (C2e) |
| **Grupo 3: Prompts** | `PromptsScreen.kt`<br>`PromptDetailScreen.kt`<br>`UserPromptEditorScreen.kt` | • Seção "Meus prompts" e "Prompts prontos"<br>• Cards com tag de categoria<br>• Bloco de visualização de prompt mono/copiável<br>• Botão de cópia e atalho para importar | *Pendente* |
| **Grupo 3: Importação** | `ImportScreen.kt` | • Dropzone tracejada (`.drop`) com suporte a colagem e seleção de JSON<br>• Prévia com título e total de cards<br>• Seleção de categoria | *Pendente* |
| **Backup & Ajustes** | `BackupScreen.kt` | • Botão de exportação completa<br>• Restauração de arquivo com aviso<br>• Estatísticas do banco | *Pendente* |

---

## 3. Sheets, Diálogos e Modais Mapeados

| Componente | Protótipo HTML | Localização Atual / Prevista | Status |
|---|---|---|---|
| **Mudar Categoria** | `NOVA · Mudar categoria` (Sheet com radio buttons) | `HomeScreen.kt` / `ChangeCategorySheet` | *Pendente* |
| **Opções de Estudo** | `NOVA · Opções de estudo` (Sheet com editar, voz, modo de estudo) | `StudyScreen.kt` | *Pendente* |
| **Seletor de Voz TTS** | `NOVA · Selecionar voz` (Sheet com vozes do sistema e botão testar) | `StudyScreen.kt` | *Pendente* |
| **Popup de Baralho** | Menu rápido (Gerenciar, Exportar, Categoria, Excluir) | `HomeScreen.kt` | Integrado via DropdownMenu |
| **Diálogo Excluir Card** | Alerta de confirmação irreversível | `CardEditorScreen.kt` | **Concluído** |

---

## 4. Histórico de Verificações

- **Testes Unitários:** Todos os testes em `app/src/test/` validados com sucesso (`.\gradlew.bat test`).
- **Compilação Debug:** `.\gradlew.bat assembleDebug` executado e aprovado em cada entrega.

