package com.cristiano.flashlang.data.prompts

object PromptTemplates {
    val all: List<PromptTemplate> = listOf(
        PromptTemplate(
            id = "vocabulary-texts",
            title = "Vocabulário de textos que li",
            description = "Gere cards úteis a partir de um texto em inglês ou espanhol.",
            category = "Vocabulário",
            needsLanguage = true,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.TEXT,
                    required = true,
                    label = "Texto para analisar",
                    hint = "Cole aqui o texto que você leu.",
                ),
            ),
            body = """Você é um professor de {{IDIOMA}} para falantes de português do Brasil, nível B1/B2. Leia o texto abaixo e escolha de 15 a 25 palavras e expressões úteis para aprender, incluindo phrasal verbs e colocações quando houver. Evite palavras muito básicas e não repita itens. Para cada item: tradução em português do Brasil, uma frase de exemplo NOVA (não copie do texto) com o termo em contexto natural e a tradução da frase. Inclua o ipa somente se tiver certeza. Dê ao baralho um título curto sobre o tema do texto.

TEXTO:
{{TEXTO}}""",
        ),
        PromptTemplate(
            id = "ed-pronunciation",
            title = "Pronúncia dos verbos terminados em -ed",
            description = "Pratique as terminações /t/, /d/ e /ɪd/ de verbos regulares.",
            category = "Pronúncia",
            needsLanguage = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.LIST,
                    required = true,
                    label = "Verbos (um por linha ou separados por vírgula)",
                    hint = "Ex.: walk, play, want, decide",
                ),
            ),
            body = """Você é um professor de pronúncia de inglês americano para falantes de português do Brasil. Para cada verbo regular da lista, crie um card em que o termo é a forma do passado (terminada em -ed), em ordem misturada. No ipa, coloque a pronúncia do passado em inglês americano. Na note, diga qual terminação soa (/t/, /d/ ou /ɪd/) e a regra em uma frase curta em português: /ɪd/ depois de /t/ ou /d/; /t/ depois de sons surdos (/p/, /k/, /f/, /s/, /ʃ/, /tʃ/); /d/ depois de sons sonoros e de vogais. Em termTranslation, traduza o passado ("andou", "caminhou"). Use uma frase curta no passado como exemplo, com tradução. Se um verbo da lista não for regular ou você não tiver certeza da pronúncia, não o inclua. Nome do baralho: "Pronúncia -ed".

LISTA DE VERBOS:
{{LISTA}}""",
        ),
        PromptTemplate(
            id = "vowel-sounds",
            title = "Sons das vogais: curto e longo",
            description = "Estude padrões de vogais curtas e longas com palavras de exemplo.",
            category = "Pronúncia",
            needsLanguage = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.FOCUS,
                    required = false,
                    label = "Foco",
                    hint = "Ex.: vogal a, ou magic e",
                ),
                PromptInput(
                    type = PromptInputType.LIST,
                    required = false,
                    label = "Palavras para incluir",
                    hint = "Opcional. Separe as palavras por vírgulas ou linhas.",
                ),
            ),
            body = """Você é um professor de pronúncia de inglês americano para falantes de português do Brasil. Foco: {{FOCO}}; se não informado, cubra as cinco vogais (a, e, i, o, u). Crie de 20 a 30 cards. Cada card é uma palavra-exemplo (type word) com ipa e uma note em português que explique a regra do som da vogal: som curto ou longo, "magic e" (vogal + consoante + e), vogais dobradas (ee, ea, oo), vogal seguida de r, sílaba aberta ou fechada. Marque as exceções comuns com a palavra "exceção". Use a mesma estrutura de note para palavras do mesmo padrão e inclua pares de contraste (hat/hate, bit/bite). Se houver uma lista de palavras, priorize-as e classifique cada uma: {{LISTA}}. Em termTranslation, traduza a palavra; use uma frase curta como exemplo, com tradução. Se não tiver certeza da pronúncia de uma palavra, não a inclua.""",
        ),
    )
}
