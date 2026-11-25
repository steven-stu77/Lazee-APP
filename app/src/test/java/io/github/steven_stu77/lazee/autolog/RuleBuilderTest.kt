package io.github.steven_stu77.lazee.autolog

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.feature.autolog.parser.Direction
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserConfig
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserEngine
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserRule
import io.github.steven_stu77.lazee.feature.autolog.parser.RuleBuilder
import org.junit.Test

class RuleBuilderTest {
    private fun engineFor(pattern: String, dir: Direction = Direction.OUT) =
        ParserEngine(listOf(ParserConfig("app", "App", rules = listOf(ParserRule(dir, pattern)))))

    private fun pattern(sample: String, amount: String, merchant: String?): String =
        (RuleBuilder.build(sample, amount, merchant) as RuleBuilder.Result.Ok).pattern

    @Test fun realTngSampleBecomesRuleThatGeneralises() {
        val sample = "DuitNow Payment You have paid RM4.20 to MMP Time DotCom Glenmarie."
        assertThat(RuleBuilder.detectAmount(sample)).isEqualTo("RM4.20")
        assertThat(RuleBuilder.detectMerchant(sample)).isEqualTo("MMP Time DotCom Glenmarie")
        val e = engineFor(pattern(sample, "RM4.20", "MMP Time DotCom Glenmarie"))
        val p = e.parse("app", "DuitNow Payment You have paid RM1,234.50 to ZUS Coffee Mid Valley.")!!
        assertThat(p.amount).isEqualTo(123450)
        assertThat(p.merchant).isEqualTo("ZUS Coffee Mid Valley")
        // Different spacing / no final dot still matches.
        assertThat(e.parse("app", "DuitNow Payment  You have paid RM 8.00 to 7-Eleven")!!.merchant).isEqualTo("7-Eleven")
    }

    @Test fun merchantInTheMiddleAndOtherNumbersIgnored() {
        val sample = "Payment of RM129.00 to UNIFI is successful. Ref 88231 on 12/11."
        val e = engineFor(pattern(sample, "RM129.00", "UNIFI"))
        val p = e.parse("app", "Payment of RM59.90 to Celcom Digi is successful. Ref 1234567 on 3/12.")!!
        assertThat(p.amount).isEqualTo(5990)
        assertThat(p.merchant).isEqualTo("Celcom Digi")
    }

    @Test fun merchantBeforeAmount() {
        val sample = "Shopee charged you RM12.90 for your order"
        val e = engineFor(pattern(sample, "RM12.90", "Shopee"))
        assertThat(e.parse("app", "Lazada charged you RM5.00 for your order")!!.merchant).isEqualTo("Lazada")
    }

    @Test fun noMerchantFallsBackToAppName() {
        val e = engineFor(pattern("Reload of RM50.00 successful", "RM50.00", null))
        assertThat(e.parse("app", "Reload of RM30.00 successful")!!.merchant).isEqualTo("App")
    }

    @Test fun regexCharactersInSampleAreEscaped() {
        val sample = "[Alert] (Card *1234) paid RM6.80 at A+B Mart?"
        val e = engineFor(pattern(sample, "RM6.80", "A+B Mart"))
        assertThat(e.parse("app", "[Alert] (Card *9999) paid RM7.80 at C+D Mart?")!!.merchant).isEqualTo("C+D Mart")
    }

    @Test fun errors() {
        assertThat(RuleBuilder.build("paid RM5.00 to X", "RM9.99", "X")).isEqualTo(RuleBuilder.Result.Error(RuleBuilder.Reason.AMOUNT_NOT_IN_SAMPLE))
        assertThat(RuleBuilder.build("paid RM5.00 to X", "RM5.00", "Y")).isEqualTo(RuleBuilder.Result.Error(RuleBuilder.Reason.MERCHANT_NOT_IN_SAMPLE))
        assertThat(RuleBuilder.build("paid RM5.00 to X", "", "X")).isEqualTo(RuleBuilder.Result.Error(RuleBuilder.Reason.NO_AMOUNT))
    }

    @Test fun userRulesWinAndAddNewApps() {
        val builtIn = listOf(ParserConfig("tng", "TNG", preview = true, rules = listOf(ParserRule(Direction.OUT, "paid (?<amount>\\d+)"))))
        val custom = listOf(
            ParserConfig("tng", "TNG", custom = true, rules = listOf(ParserRule(Direction.IGNORE, "points"))),
            ParserConfig("boost", "Boost", custom = true, rules = listOf(ParserRule(Direction.OUT, "x(?<amount>\\d+)"))),
        )
        val merged = ParserEngine.merge(builtIn, custom)
        assertThat(merged.map { it.`package` }).containsExactly("boost", "tng").inOrder()
        val tng = merged.first { it.`package` == "tng" }
        assertThat(tng.rules.first().direction).isEqualTo(Direction.IGNORE)
        assertThat(tng.preview).isFalse()
    }
}
