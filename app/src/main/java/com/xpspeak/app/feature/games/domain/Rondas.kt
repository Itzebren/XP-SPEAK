package com.xpspeak.app.feature.games.domain

import kotlin.math.ln
import kotlin.random.Random

/**
 * Rondas de cada juego por nivel de dificultad (0 fácil, 1 medio, 2 difícil).
 * La siguiente ronda se saca del nivel que marque la dificultad adaptativa;
 * si ese nivel se agotó, del más cercano.
 */
data class Mazo<T>(val porNivel: List<List<T>>) {
    val restantes: Int get() = porNivel.sumOf { it.size }

    fun sacar(nivel: Int): Pair<T, Mazo<T>>? {
        val orden = (0 until porNivel.size).sortedWith(compareBy({ kotlin.math.abs(it - nivel) }, { it }))
        val elegido = orden.firstOrNull { porNivel[it].isNotEmpty() } ?: return null
        val ronda = porNivel[elegido].first()
        return ronda to Mazo(porNivel.mapIndexed { i, lista -> if (i == elegido) lista.drop(1) else lista })
    }
}

/** Una oración de Orden Maestro: las fichas desordenadas y la solución. */
data class RondaOrden(
    val fichas: List<String>,
    val solucion: List<String>,
    /** Punto o signo de interrogación final; se muestra fijo al final de la oración. */
    val puntuacion: String,
    val pista: String,
    val conceptoId: String?
)

/** Un par de Ráfaga: la palabra y la traducción que se muestra (correcta o no). */
data class RondaRafaga(
    val en: String,
    val esCorrecta: String,
    /** Traducción de otro tema (fácil de descartar). */
    val distractorFacil: String,
    /** Traducción del mismo tema (más difícil, §5.3). */
    val distractorDificil: String,
    val muestraCorrecta: Boolean,
    val conceptoId: String
) {
    fun mostrada(nivel: Int): String = when {
        muestraCorrecta -> esCorrecta
        nivel >= Dificultad.MEDIO -> distractorDificil
        else -> distractorFacil
    }
}

data class RondaEco(val en: String, val es: String?, val conceptoId: String?)

object GeneradorRondas {
    const val RONDAS_ORDEN = 8
    const val RONDAS_ECO = 8
    const val PARES_RAFAGA = 120
    private const val PESO_PRIORITARIO = 2.0

    private val PRONOMBRE_I = Regex("^I('[a-z]+)?$")
    private val PUNTUACION_FINAL = Regex("[.!?]+$")

    /**
     * Orden aleatorio donde los conceptos débiles del SRS tienen el doble de
     * probabilidad de salir primero (muestreo ponderado sin reemplazo).
     */
    fun <T> ponderar(items: List<T>, prioritarios: Set<String>, random: Random, concepto: (T) -> String?): List<T> =
        items.map { item ->
            val peso = if (concepto(item) in prioritarios) PESO_PRIORITARIO else 1.0
            item to -ln(1.0 - random.nextDouble()) / peso
        }.sortedBy { it.second }.map { it.first }

    // ── Orden Maestro ──────────────────────────────────────────────────────

    fun rondasOrden(contenido: ContenidoJuegos, random: Random): Mazo<RondaOrden> {
        val nombres = nombresPropios(contenido)
        val candidatas = contenido.lecciones.flatMap { leccion ->
            leccion.dialogo.flatMap { linea -> oracionesDeLinea(linea, leccion.titulo) } +
                leccion.oraciones.map { Triple(it.en, "Tema: ${it.tema}", it.conceptoId) }
        }.distinctBy { Texto.normalizar(it.first) }
            .mapNotNull { (en, pista, concepto) -> rondaOrden(en, pista, concepto, nombres, random) }

        val ordenadas = ponderar(candidatas, contenido.prioritarios, random) { it.conceptoId }
        return Mazo(
            listOf(3..4, 5..6, 7..9).map { rango -> ordenadas.filter { it.solucion.size in rango }.take(RONDAS_ORDEN) }
        )
    }

    private fun rondaOrden(en: String, pista: String, conceptoId: String?, nombres: Set<String>, random: Random): RondaOrden? {
        val tokens = en.trim().split(Regex("\\s+"))
        if (tokens.size !in 3..9 || en.contains('…') || en.contains('—')) return null
        // Una sola oración: "Hi! What's your name?" serían dos rompecabezas en uno.
        if (tokens.dropLast(1).any { PUNTUACION_FINAL.containsMatchIn(it) }) return null
        val puntuacion = PUNTUACION_FINAL.find(tokens.last())?.value.orEmpty()
        val solucion = tokens.mapIndexed { i, token ->
            var ficha = if (i == tokens.lastIndex) token.removeSuffix(puntuacion) else token
            // La mayúscula inicial delataría la primera ficha (salvo "I" y nombres propios).
            val esSigla = ficha.count { it.isUpperCase() } > 1 // "OK", "ATM", "T-Shirt"
            if (i == 0 && !esSigla && !PRONOMBRE_I.matches(ficha) && ficha.trimEnd(',', '!') !in nombres) {
                ficha = ficha.replaceFirstChar { it.lowercase() }
            }
            ficha
        }
        if (solucion.any { it.isEmpty() }) return null
        return RondaOrden(desordenar(solucion, random), solucion, puntuacion, pista, conceptoId)
    }

    /**
     * Una línea de diálogo puede tener varias oraciones ("Hi, Laura! I'm Diego.").
     * Cada una es un rompecabezas; si la traducción tiene las mismas oraciones,
     * cada una lleva la suya como pista.
     */
    private fun oracionesDeLinea(linea: LineaDialogo, tema: String): List<Triple<String, String, String?>> {
        val en = separarOraciones(linea.en)
        val es = separarOraciones(linea.es)
        return en.mapIndexed { i, oracion ->
            Triple(oracion, if (en.size == es.size) es[i] else "Tema: $tema", null)
        }
    }

    private fun separarOraciones(texto: String): List<String> =
        texto.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Hablantes de los diálogos y palabras con mayúscula a mitad de una oración
     * ("Puebla", "Reforma"); no cuenta las que empiezan otra oración ("Hello! Can…").
     */
    private fun nombresPropios(contenido: ContenidoJuegos): Set<String> {
        val textos = contenido.lecciones.flatMap { l -> l.dialogo.map { it.en } + l.oraciones.map { it.en } }
        val aMitad = textos.flatMap { texto ->
            texto.split(Regex("\\s+")).zipWithNext()
                .filter { (anterior, _) -> !anterior.endsWith('.') && !anterior.endsWith('!') && !anterior.endsWith('?') && anterior != "—" }
                .map { (_, palabra) -> palabra.trim(',', '.', '!', '?') }
                .filter { it.firstOrNull()?.isUpperCase() == true && !PRONOMBRE_I.matches(it) }
        }
        return (contenido.lecciones.flatMap { l -> l.dialogo.map { it.hablante } } + aMitad).toSet()
    }

    /** Baraja hasta que el orden no sea ya la solución (si hay forma de lograrlo). */
    fun desordenar(solucion: List<String>, random: Random): List<String> {
        if (solucion.distinct().size < 2) return solucion
        var fichas = solucion.shuffled(random)
        while (fichas == solucion) fichas = solucion.shuffled(random)
        return fichas
    }

    /** ¿Las fichas colocadas forman la oración? Compara texto, así dos fichas iguales son intercambiables. */
    fun ordenCorrecto(colocadas: List<String>, solucion: List<String>): Boolean =
        colocadas.map(Texto::normalizar) == solucion.map(Texto::normalizar)

    // ── Ráfaga de Palabras ─────────────────────────────────────────────────

    fun paresRafaga(contenido: ContenidoJuegos, random: Random, cantidad: Int = PARES_RAFAGA): List<RondaRafaga> {
        val palabras = contenido.lecciones.flatMap { l -> l.vocabulario.map { l.id to it } }.distinctBy { it.second.conceptoId }
        if (palabras.size < 2) return emptyList()

        val pares = mutableListOf<RondaRafaga>()
        while (pares.size < cantidad) {
            // Si hay pocas palabras (una sola lección disponible) se repiten, en otro orden.
            for ((leccionId, palabra) in ponderar(palabras, contenido.prioritarios, random) { it.second.conceptoId }) {
                if (pares.size == cantidad) break
                val distinta = { otra: Palabra -> sentidos(otra.es).none { it in sentidos(palabra.es) } }
                val mismoTema = palabras.filter { it.first == leccionId && distinta(it.second) }.map { it.second }
                val otroTema = palabras.filter { it.first != leccionId && distinta(it.second) }.map { it.second }
                val dificil = (mismoTema.ifEmpty { otroTema }).randomOrNull(random) ?: continue
                val facil = (otroTema.ifEmpty { mismoTema }).random(random)
                pares += RondaRafaga(palabra.en, palabra.es, facil.es, dificil.es, random.nextBoolean(), palabra.conceptoId)
            }
        }
        return pares
    }

    /**
     * Traducciones que acepta una palabra, sin notas ni signos: "¡Hola! (informal)"
     * → {"hola"}, "chamarra / chaqueta" → {"chamarra", "chaqueta"}. Un distractor
     * no puede compartir ninguna, o "Hi! → ¡Hola!" saldría como incorrecto.
     */
    fun sentidos(es: String): Set<String> =
        es.replace(Regex("\\([^)]*\\)"), " ").split('/')
            .map { Texto.palabras(it).joinToString(" ") }
            .filter { it.isNotEmpty() }
            .toSet()

    // ── Eco Vocal ──────────────────────────────────────────────────────────

    fun rondasEco(contenido: ContenidoJuegos, random: Random): Mazo<RondaEco> {
        val palabras = contenido.lecciones.flatMap { it.vocabulario }
            .filter { p -> listOf('…', '/', ',').none { it in p.en } }
            .map { RondaEco(it.en, it.es, it.conceptoId) }
        val frases = contenido.lecciones.flatMap { l ->
            l.dialogo.map { RondaEco(it.en, it.es, null) } + l.oraciones.map { RondaEco(it.en, null, it.conceptoId) }
        }
        val todas = (palabras + frases).distinctBy { Texto.normalizar(it.en) }
        val ordenadas = ponderar(todas, contenido.prioritarios, random) { it.conceptoId }
        val largo = { r: RondaEco -> Texto.palabras(r.en).size }
        return Mazo(
            listOf(1..2, 3..5, 6..8).map { rango -> ordenadas.filter { largo(it) in rango }.take(RONDAS_ECO) }
        )
    }

    // ── Misión Situacional ─────────────────────────────────────────────────

    /** Prefiere misiones que practican conceptos débiles; si no, cualquiera disponible. */
    fun elegirMision(contenido: ContenidoJuegos, random: Random, evitar: String? = null): Mision? {
        val opciones = contenido.misiones.filter { it.id != evitar }.ifEmpty { contenido.misiones }
        return ponderar(opciones, contenido.prioritarios, random) { mision ->
            mision.pasos.flatMap { p -> p.opciones.mapNotNull { it.conceptoId } }.firstOrNull { it in contenido.prioritarios }
        }.firstOrNull()
    }
}
