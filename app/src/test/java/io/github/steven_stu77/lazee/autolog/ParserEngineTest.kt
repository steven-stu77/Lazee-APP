package io.github.steven_stu77.lazee.autolog

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.steven_stu77.lazee.feature.autolog.parser.Direction
import io.github.steven_stu77.lazee.feature.autolog.parser.OtpFilter
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Test
import java.io.File

/** Every sample in src/test/resources/notifications/<package>/ must parse to its .expected.json. */
class ParserEngineTest {
    private val engine = ParserEngine(File("src/main/assets/parsers").listFiles()!!.filter { it.extension == "json" }.map { ParserEngine.decode(it.readText()) })
    private val samples = File(javaClass.classLoader!!.getResource("notifications")!!.toURI())

    @Test fun allSamplesMatchExpected() {
        val txts = samples.walk().filter { it.extension == "txt" }.toList()
        assertThat(txts.size).isAtLeast(10)
        for (txt in txts) {
            val pkg = txt.parentFile.name
            val expected = Json.parseToJsonElement(File(txt.path.removeSuffix(".txt") + ".expected.json").readText()).jsonObject
            val parsed = engine.parse(pkg, txt.readText())
            val name = "${pkg}/${txt.name}"
            if (expected["ignored"] != null) {
                assertWithMessage(name).that(parsed).isNull()
                continue
            }
            assertWithMessage(name).that(parsed).isNotNull()
            assertWithMessage("$name amount").that(parsed!!.amount).isEqualTo(expected["amount"]!!.jsonPrimitive.long)
            assertWithMessage("$name merchant").that(parsed.merchant).isEqualTo(expected["merchant"]!!.jsonPrimitive.content)
            assertWithMessage("$name direction").that(parsed.direction).isEqualTo(Direction.valueOf(expected["direction"]!!.jsonPrimitive.content))
            (expected as JsonObject)["last4"]?.let { assertWithMessage("$name last4").that(parsed.last4).isEqualTo(it.jsonPrimitive.content) }
        }
    }

    @Test fun otpIsDroppedFirst() {
        listOf("Your TAC is 123", "OTP: 9999", "PIN 1234 for RM5.00", "one-time password", "Your verification code is 1", "kod pengesahan 22", "验证码 123456", "动态码 8888")
            .forEach { assertWithMessage(it).that(OtpFilter.isOtp(it)).isTrue() }
        assertThat(OtpFilter.isOtp("You have paid RM6.80 to 7-Eleven")).isFalse()
        assertThat(OtpFilter.isOtp("SPINNING class RM30")).isFalse()
    }

    @Test fun unknownPackageIsIgnored() {
        assertThat(engine.parse("com.example.chat", "You have paid RM6.80 to 7-Eleven")).isNull()
    }
}
