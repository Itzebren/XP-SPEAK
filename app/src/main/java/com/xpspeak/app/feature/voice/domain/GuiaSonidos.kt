package com.xpspeak.app.feature.voice.domain

/**
 * Cómo se produce cada sonido del inglés (alfabeto IPA de Azure, en-US), explicado para un
 * hispanohablante: qué hacer con la boca y una palabra de ejemplo. Lo usa la
 * retroalimentación para que «/θ/ 40» se convierta en algo que el usuario pueda corregir.
 */
object GuiaSonidos {

    data class Consejo(
        /** Cómo colocar la boca, en una o dos frases. */
        val comoSeHace: String,
        /** Palabras en inglés con ese sonido, para escucharlas. */
        val ejemplo: String
    )

    fun consejo(fonema: String): Consejo? = CONSEJOS[fonema]

    /**
     * Qué pasó cuando se oyó [oido] en lugar de [esperado]: las confusiones típicas de un
     * hispanohablante tienen su propia explicación; las demás, una genérica.
     */
    fun confusion(esperado: String, oido: String): String =
        CONFUSIONES[esperado to oido] ?: "Sonó como /$oido/ en lugar de /$esperado/."

    private val CONFUSIONES = mapOf(
        ("ɪ" to "i") to "Sonó como la «i» larga de «see». Esta es más corta y relajada, casi entre «i» y «e».",
        ("i" to "ɪ") to "Quedó corta. Alarga la «i» y estira los labios como sonriendo.",
        ("æ" to "ɛ") to "Sonó como «e». Abre más la boca, entre «a» y «e».",
        ("æ" to "ɑ") to "Sonó como «a» española. Estira los labios hacia los lados, entre «a» y «e».",
        ("ʌ" to "ɑ") to "Sonó como «a» abierta. Relaja la boca: es una «a» corta y neutra, como en «cup».",
        ("ə" to "ɑ") to "Sonó como «a» clara. En sílabas sin acento es una vocal muy suave y corta.",
        ("v" to "b") to "Sonó como «b». Apoya los dientes de arriba en el labio de abajo y deja vibrar.",
        ("b" to "v") to "Sonó como «v». Junta los dos labios.",
        ("θ" to "s") to "Sonó como «s». Saca un poco la lengua entre los dientes y sopla.",
        ("θ" to "t") to "Sonó como «t». No toques el paladar: la lengua va entre los dientes y sale aire.",
        ("θ" to "f") to "Sonó como «f». Usa la lengua entre los dientes, no el labio.",
        ("ð" to "d") to "Sonó como «d». Pon la lengua entre los dientes y deja vibrar la garganta.",
        ("ð" to "b") to "Sonó como «b». Pon la lengua entre los dientes y deja vibrar la garganta.",
        ("z" to "s") to "Sonó como «s». Es una «s» que zumba: deja vibrar la garganta, como una abeja.",
        ("ʃ" to "tʃ") to "Sonó como «ch». No cortes el aire: sopla continuo, como pidiendo silencio «shhh».",
        ("dʒ" to "j") to "Sonó como «y». Empieza con la lengua pegada al paladar, como una «ch» con voz.",
        ("dʒ" to "ʒ") to "Le faltó el golpe inicial: empieza como «d» y suelta en «y».",
        ("ɹ" to "d") to "Sonó como «r» española de un golpe. Curva la lengua hacia atrás sin tocar el paladar.",
        ("ɹ" to "l") to "Sonó como «l». Curva la lengua hacia atrás sin tocar nada.",
        ("h" to "x") to "Sonó como «j» fuerte. Es solo aire suave, como al empañar un vidrio.",
        ("ŋ" to "n") to "Sonó como «n». Cierra con la parte de atrás de la lengua, como en «tengo», sin decir la «g».",
        ("ŋ" to "g") to "Se oyó la «g». Termina en el sonido nasal, sin soltar la «g».",
        ("w" to "g") to "Sonó como «g». Redondea los labios, como para decir «u», sin tocar el paladar.",
        ("j" to "dʒ") to "Sonó como «ll» fuerte. Es una «i» rápida, sin que la lengua toque el paladar."
    )

    private val CONSEJOS = mapOf(
        // Vocales
        "i" to Consejo("«i» larga: estira los labios como sonriendo y sostenla un poco.", "see, eat, week"),
        "ɪ" to Consejo("«i» corta y relajada, casi entre «i» y «e». No estires los labios.", "sit, big, this"),
        "eɪ" to Consejo("Empieza en «e» y desliza hacia «i»: «ei».", "day, name, play"),
        "ɛ" to Consejo("Como la «e» española, con la boca un poco más abierta.", "bed, red, yes"),
        "æ" to Consejo("Abre la boca como para «a» y estira los labios como para «e».", "cat, bad, happy"),
        "ɑ" to Consejo("«a» bien abierta y de atrás, como cuando el doctor revisa tu garganta.", "hot, father, stop"),
        "ɔ" to Consejo("«o» abierta y redonda, con la mandíbula baja.", "dog, call, law"),
        "oʊ" to Consejo("Empieza en «o» y cierra los labios hacia «u»: «ou».", "go, home, know"),
        "ʊ" to Consejo("«u» corta y relajada, con los labios poco redondeados.", "book, good, put"),
        "u" to Consejo("«u» larga con los labios bien redondos.", "food, blue, school"),
        "ʌ" to Consejo("«a» corta y neutra, con la boca relajada y a medio abrir.", "cup, but, love"),
        "ə" to Consejo("Vocal muy suave y corta (schwa) de las sílabas sin acento. No la pronuncies clara.", "about, banana, the"),
        "ɝ" to Consejo("Vocal con la lengua curvada hacia atrás, como gruñendo «er».", "bird, work, her"),
        "ɚ" to Consejo("«er» suave sin acento, con la lengua curvada hacia atrás.", "teacher, water, doctor"),
        "aɪ" to Consejo("Empieza en «a» y desliza hacia «i»: «ai».", "I, my, time"),
        "aʊ" to Consejo("Empieza en «a» y cierra hacia «u»: «au».", "now, house, out"),
        "ɔɪ" to Consejo("Empieza en «o» abierta y desliza hacia «i»: «oi».", "boy, toy, enjoy"),
        // Consonantes
        "p" to Consejo("Como la «p», pero suelta un soplo de aire al abrir los labios.", "pen, happy, stop"),
        "b" to Consejo("Junta bien los labios y deja vibrar la garganta.", "boy, big, job"),
        "t" to Consejo("La punta de la lengua toca detrás de los dientes de arriba (no los dientes) y sale un soplo.", "ten, water, cat"),
        "d" to Consejo("Como la «t» pero con voz; la lengua toca detrás de los dientes, no los dientes.", "day, red, did"),
        "k" to Consejo("Como la «k», con un soplo de aire al soltar.", "cat, book, kitchen"),
        "g" to Consejo("«g» de «gato», con voz y sin suavizarla.", "go, big, again"),
        "f" to Consejo("Dientes de arriba sobre el labio de abajo, y sopla.", "fish, coffee, life"),
        "v" to Consejo("Como la «f» pero con voz: dientes sobre el labio de abajo y deja vibrar. No es «b».", "very, have, seven"),
        "θ" to Consejo("Saca la punta de la lengua entre los dientes y sopla, sin voz (como la «z» de España).", "think, three, bath"),
        "ð" to Consejo("Lengua entre los dientes, como /θ/, pero dejando vibrar la garganta.", "the, this, mother"),
        "s" to Consejo("«s» clara y sin voz.", "see, bus, sister"),
        "z" to Consejo("«s» que zumba: deja vibrar la garganta, como una abeja.", "zoo, is, busy"),
        "ʃ" to Consejo("Labios hacia afuera y sopla continuo, como pidiendo silencio: «shhh».", "she, fish, station"),
        "ʒ" to Consejo("Como /ʃ/ pero con voz, parecido a la «y» argentina.", "usually, television, measure"),
        "tʃ" to Consejo("Como la «ch» de «chocolate».", "church, teacher, watch"),
        "dʒ" to Consejo("Como una «ch» con voz: la lengua pegada al paladar y suelta con fuerza.", "job, orange, July"),
        "h" to Consejo("Solo aire suave, como al empañar un vidrio. No es la «j» española.", "hello, house, behind"),
        "m" to Consejo("Labios juntos y el aire sale por la nariz.", "man, summer, time"),
        "n" to Consejo("Punta de la lengua detrás de los dientes de arriba; el aire sale por la nariz.", "no, dinner, ten"),
        "ŋ" to Consejo("Cierra con la parte de atrás de la lengua, como la «n» de «tengo», sin decir la «g».", "sing, morning, English"),
        "l" to Consejo("Punta de la lengua detrás de los dientes de arriba. Al final de palabra, más oscura y de atrás.", "like, hello, school"),
        "ɹ" to Consejo("Curva la lengua hacia atrás SIN tocar el paladar y redondea un poco los labios. No vibra.", "red, very, car"),
        "w" to Consejo("Redondea los labios como para «u» y ábrelos rápido.", "we, water, window"),
        "j" to Consejo("Una «i» rápida al inicio, como en «yo» suave.", "yes, you, year")
    )
}
