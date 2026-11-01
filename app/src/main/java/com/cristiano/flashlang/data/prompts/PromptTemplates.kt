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
        PromptTemplate(
            id = "s-endings",
            title = "Terminações -s e -es",
            description = "Pratique as terminações de plurais e da terceira pessoa do singular.",
            category = "Pronúncia",
            needsLanguage = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.LIST,
                    required = true,
                    label = "Palavras (substantivos no plural ou verbos na 3ª pessoa)",
                    hint = "Informe palavras separadas por vírgula ou linha.",
                ),
            ),
            body = """Você é um professor de pronúncia de inglês americano para falantes de português do Brasil. Para cada palavra da lista, crie um card em que o termo é a forma terminada em -s ou -es, em ordem misturada. No ipa, coloque a pronúncia em inglês americano. Na note, diga qual terminação soa (/s/, /z/ ou /ɪz/) e a regra em uma frase curta em português: /ɪz/ depois de /s, z, ʃ, ʒ, tʃ, dʒ/; /s/ depois de sons surdos (/p, t, k, f, θ/); /z/ depois de sons sonoros e de vogais. Traduza o termo em termTranslation e use uma frase curta de exemplo, com tradução. Se não tiver certeza da pronúncia de uma palavra, não a inclua. Nome do baralho: "Pronúncia -s/-es".
LISTA: {{LISTA}}""",
        ),
        PromptTemplate(
            id = "minimal-pairs",
            title = "Pares mínimos",
            description = "Compare sons próximos do inglês americano com pares de palavras.",
            category = "Pronúncia",
            needsLanguage = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.FOCUS,
                    required = true,
                    label = "Contraste",
                    hint = "Contraste, por exemplo: /ɪ/ e /iː/",
                ),
                PromptInput(
                    type = PromptInputType.LIST,
                    required = false,
                    label = "Palavras para incluir",
                    hint = "Opcional. Informe palavras separadas por vírgula ou linha.",
                ),
            ),
            body = """Você é um professor de pronúncia de inglês americano para falantes de português do Brasil. Crie de 10 a 15 pares mínimos para o contraste: {{FOCO}}. Cada palavra do par é um card separado (type word), com ipa em inglês americano e note em português dizendo com qual palavra ela forma par e qual é a diferença do som (por exemplo: "ship /ʃɪp/ × sheep /ʃiːp/: /ɪ/ curto × /iː/ longo"). Use uma frase curta que deixe o sentido claro, com tradução. Palavras sugeridas: {{LISTA}}. Se não tiver certeza da pronúncia de uma palavra, não a inclua.""",
        ),
        PromptTemplate(
            id = "difficult-english-sounds",
            title = "Sons difíceis do inglês para brasileiros",
            description = "Treine sons do inglês americano que costumam desafiar falantes de português.",
            category = "Pronúncia",
            needsLanguage = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.FOCUS,
                    required = false,
                    label = "Foco",
                    hint = "Som, por exemplo: th, r final, h aspirado",
                ),
            ),
            body = """Você é um professor de pronúncia de inglês americano para falantes de português do Brasil. Foco: {{FOCO}}; se não informado, cubra: th (/θ/ e /ð/), r final, /æ/ e /ɛ/, h aspirado, consoantes finais sem vogal extra (evitar "pop-i") e -ing. Crie de 20 a 30 cards com palavras-exemplo (type word), ipa e uma note em português com a dica prática de como posicionar língua, lábios ou voz, e o erro típico de falantes de português. Use uma frase curta de exemplo, com tradução. Se não tiver certeza da pronúncia, não inclua a palavra.""",
        ),
        PromptTemplate(
            id = "word-stress",
            title = "Acento da palavra",
            description = "Identifique a sílaba tônica e contrastes de acento em inglês.",
            category = "Pronúncia",
            needsLanguage = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.LIST,
                    required = true,
                    label = "Palavras",
                    hint = "Informe palavras separadas por vírgula ou linha.",
                ),
            ),
            body = """Você é um professor de pronúncia de inglês americano para falantes de português do Brasil. Para cada palavra da lista, crie um card com o ipa marcando a sílaba tônica com ˈ, e uma note em português dizendo qual sílaba é forte e, quando houver, o contraste entre substantivo e verbo (por exemplo: RECord × reCORD). Use frases curtas de exemplo, com tradução. Se não tiver certeza, não inclua a palavra. LISTA: {{LISTA}}""",
        ),
        PromptTemplate(
            id = "false-cognates",
            title = "Falsos cognatos",
            description = "Aprenda palavras que se parecem com o português, mas têm outro sentido.",
            category = "Vocabulário",
            needsLanguage = true,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.FOCUS,
                    required = false,
                    label = "Tema",
                    hint = "Tema, por exemplo: trabalho, escola",
                ),
            ),
            body = """Você é um professor de {{IDIOMA}} para falantes de português do Brasil. Crie de 15 a 25 cards de falsos cognatos (palavras que parecem com o português mas têm outro sentido) do tema {{FOCO}}; se não informado, use os mais comuns e mais úteis. No termo, a palavra em {{IDIOMA}}; em termTranslation, o significado correto; na note, o alerta em português ("parece 'pretender', mas significa 'fingir'"). Frase de exemplo com tradução. Só inclua pares de que tenha certeza.""",
        ),
        PromptTemplate(
            id = "phrasal-verbs-and-collocations",
            title = "Phrasal verbs e colocações do texto",
            description = "Extraia expressões úteis que aparecem em um texto.",
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
            body = """Você é um professor de {{IDIOMA}} para falantes de português do Brasil, nível B1/B2. Leia o texto e extraia de 15 a 25 phrasal verbs e colocações (por exemplo: make a decision, take a break) que aparecem nele. type expression. Traduza, escreva uma frase de exemplo NOVA e traduza-a; na note, explique em português quando usar. Nome do baralho: título curto sobre o tema.
TEXTO: {{TEXTO}}""",
        ),
        PromptTemplate(
            id = "spanish-sounds",
            title = "Sons do espanhol",
            description = "Pratique sons e regras de acentuação do espanhol da Espanha.",
            category = "Pronúncia",
            needsLanguage = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.FOCUS,
                    required = false,
                    label = "Foco",
                    hint = "Som, por exemplo: rr, ll/y, tilde",
                ),
            ),
            body = """Você é um professor de pronúncia de espanhol (Espanha) para falantes de português do Brasil. Foco: {{FOCO}}; se não informado, cubra: b/v, r e rr, j e g, ll e y, ñ, ditongos e as regras de acentuação (tilde: palavras agudas, graves e esdrújulas). Crie de 20 a 30 cards com palavras-exemplo (type word), ipa em espanhol da Espanha e uma note em português com a regra e o erro típico de brasileiros. Frase curta de exemplo, com tradução. Se não tiver certeza, não inclua a palavra. O campo language deve ser "es".""",
        ),
        PromptTemplate(
            id = "conversation-about-text",
            title = "Conversa sobre o texto",
            description = "Converse sobre um texto e receba correções em português.",
            category = "Conversa",
            needsLanguage = true,
            generatesDeck = false,
            inputs = setOf(
                PromptInput(
                    type = PromptInputType.TEXT,
                    required = true,
                    label = "Texto para conversar",
                    hint = "Cole aqui o texto que você leu.",
                ),
            ),
            body = """Atue como meu parceiro de conversa em {{IDIOMA}}, nível B1/B2. Com base no texto abaixo, comece com uma pergunta simples sobre ele e espere a minha resposta. A cada resposta minha: mostre a versão corrigida das minhas frases, explique os erros brevemente em português e faça a próxima pergunta, usando o vocabulário do texto. Fale em {{IDIOMA}}, mas explique correções em português.
TEXTO: {{TEXTO}}""",
        ),
    )
}
