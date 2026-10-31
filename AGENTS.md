# FlashLang: app Android de flashcards (inglês e espanhol)

## Objetivo

App que importa baralhos de flashcards (JSON gerado por IA) e permite estudar
com cards que viram ao tocar. Idiomas de estudo: inglês e espanhol.
Traduções em português do Brasil.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Arquitetura MVVM: ViewModel + StateFlow
- Room para banco local
- Navigation Compose
- kotlinx.serialization para JSON

## Regras

- Faça UMA tarefa por vez, só o que foi pedido.
- Não adicione dependências sem me avisar e explicar o motivo.
- Ao terminar, rode `./gradlew assembleDebug` e corrija erros de build.
- Escreva testes unitários para parser de JSON e repositório.
- Textos da interface em português do Brasil.
- Use nomes de classes e variáveis em inglês.
- Explique brevemente o que mudou e quais arquivos foram alterados.
