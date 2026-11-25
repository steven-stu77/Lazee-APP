package io.github.steven_stu77.lazee.feature.autolog

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.feature.autolog.classify.Classifier
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserConfig
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser rules = bundled JSON in assets + the user's own rules in files/custom_rules.json.
 * User rules take effect immediately, no new APK needed.
 */
@Singleton
class AutoLogConfig @Inject constructor(@ApplicationContext private val context: Context) {
    private fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
    private val customFile get() = File(context.filesDir, "custom_rules.json")
    private val listSerializer = ListSerializer(ParserConfig.serializer())

    val builtIn: List<ParserConfig> by lazy {
        context.assets.list("parsers").orEmpty().filter { it.endsWith(".json") }.map { ParserEngine.decode(asset("parsers/$it")) }
    }

    private val _custom = MutableStateFlow(loadCustom())
    val custom: StateFlow<List<ParserConfig>> = _custom.asStateFlow()

    private val _engine = MutableStateFlow(ParserEngine(ParserEngine.merge(builtIn, _custom.value)))

    /** Always the latest rules. */
    val engine: ParserEngine get() = _engine.value
    val parserConfigs: List<ParserConfig> get() = ParserEngine.merge(builtIn, _custom.value)

    val classifier: Classifier by lazy { Classifier(Classifier.decodeMap(asset("merchants.json")), Classifier.decodeMap(asset("keywords.json"))) }

    private fun loadCustom(): List<ParserConfig> = runCatching {
        if (customFile.exists()) ParserEngine.json.decodeFromString(listSerializer, customFile.readText()) else emptyList()
    }.getOrDefault(emptyList())

    /** Saves the user's rules and rebuilds the engine. Throws if a pattern does not compile. */
    @Synchronized
    fun saveCustom(rules: List<ParserConfig>) {
        val cleaned = rules.map { it.copy(custom = true) }.filter { it.rules.isNotEmpty() }
        val engine = ParserEngine(ParserEngine.merge(builtIn, cleaned)) // compiles every pattern first
        customFile.writeText(ParserEngine.json.encodeToString(listSerializer, cleaned))
        _custom.value = cleaned
        _engine.value = engine
    }

    fun exportCustom(): String = ParserEngine.json.encodeToString(listSerializer, _custom.value)

    /** Accepts a list of app configs (what export produces) or a single one; merges by package. */
    fun importCustom(text: String): Int {
        val incoming = runCatching { ParserEngine.json.decodeFromString(listSerializer, text) }.getOrElse { listOf(ParserEngine.decode(text)) }
        val merged = (_custom.value + incoming).groupBy { it.`package` }.map { (_, list) ->
            list.last().copy(rules = list.flatMap { it.rules }.distinctBy { it.pattern to it.direction })
        }
        saveCustom(merged)
        return incoming.sumOf { it.rules.size }
    }
}
