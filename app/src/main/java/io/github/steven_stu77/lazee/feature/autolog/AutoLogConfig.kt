package io.github.steven_stu77.lazee.feature.autolog

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.feature.autolog.classify.Classifier
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserConfig
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserEngine
import javax.inject.Inject
import javax.inject.Singleton

/** Loads the JSON rules from assets once (spec §3 rule 5: config lives in JSON, not code). */
@Singleton
class AutoLogConfig @Inject constructor(@ApplicationContext private val context: Context) {
    private fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    val parserConfigs: List<ParserConfig> by lazy {
        context.assets.list("parsers").orEmpty().filter { it.endsWith(".json") }.map { ParserEngine.decode(asset("parsers/$it")) }
            .sortedBy { it.displayName }
    }
    val engine: ParserEngine by lazy { ParserEngine(parserConfigs) }
    val classifier: Classifier by lazy { Classifier(Classifier.decodeMap(asset("merchants.json")), Classifier.decodeMap(asset("keywords.json"))) }
}
