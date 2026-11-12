package io.github.steven_stu77.lazee.autolog

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.data.Confidence
import io.github.steven_stu77.lazee.feature.autolog.classify.Classifier
import io.github.steven_stu77.lazee.feature.autolog.classify.MerchantKey
import io.github.steven_stu77.lazee.feature.autolog.classify.Memory
import org.junit.Test
import java.io.File

class ClassifierTest {
    private val c = Classifier(
        Classifier.decodeMap(File("src/main/assets/merchants.json").readText()),
        Classifier.decodeMap(File("src/main/assets/keywords.json").readText()),
    )

    @Test fun memoryBeatsEverything() {
        val r = c.classify("GRAB", Memory(categoryId = 42, confirmCount = 1, alwaysAsk = false))
        assertThat(r.categoryId).isEqualTo(42)
        assertThat(r.confidence).isEqualTo(Confidence.HIGH)
    }

    @Test fun grabVsGrabFood() {
        assertThat(c.classify("GRABFOOD", null).builtInKey).isEqualTo("food")
        assertThat(c.classify("GRAB", null).builtInKey).isEqualTo("transport")
        assertThat(c.classify("GRAB*RIDE 1234", null).builtInKey).isEqualTo("transport")
    }

    @Test fun dictionaryThenKeywords() {
        assertThat(c.classify(MerchantKey.of("7-Eleven Jalan Ampang"), null).builtInKey).isEqualTo("shopping")
        assertThat(c.classify("RESTORAN AH HUAT", null).let { it.builtInKey to it.confidence }).isEqualTo("food" to Confidence.MEDIUM)
        assertThat(c.classify("KLINIK DR TAN", null).builtInKey).isEqualTo("other")
        assertThat(c.classify("UNKNOWN SHOP", null).let { it.builtInKey to it.confidence }).isEqualTo("other" to Confidence.LOW)
    }

    @Test fun personTransfersAreNeverGuessed() {
        val r = c.classify("KFC", null, isPersonTransfer = true)
        assertThat(r.confidence).isEqualTo(Confidence.LOW)
    }

    @Test fun merchantKeys() {
        assertThat(MerchantKey.of(" 7-Eleven Jalan Ampang ")).isEqualTo("7-ELEVEN JALAN AMPANG")
        assertThat(MerchantKey.of("ZUS Coffee - KL")).isEqualTo("ZUS COFFEE")
        assertThat(MerchantKey.of("Kedai Runcit Ah Seng Sdn Bhd")).isEqualTo("KEDAI RUNCIT AH SENG")
        assertThat(MerchantKey.of("MR DIY Enterprise #0123")).isEqualTo("MR DIY")
        assertThat(MerchantKey.of("7-ELEVEN #1234")).isEqualTo("7-ELEVEN")
    }
}
