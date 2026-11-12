package io.github.steven_stu77.lazee.feature.autolog.parser

/** Step 2 of spec §6: anything that looks like a one-time code is dropped before parsing. */
object OtpFilter {
    private val pattern = Regex(
        "\\bTAC\\b|\\bOTP\\b|\\bPIN\\b|one[- ]time|verification code|kod pengesahan|验证码|动态码",
        RegexOption.IGNORE_CASE,
    )

    fun isOtp(text: String): Boolean = pattern.containsMatchIn(text)
}
