package io.github.steven_stu77.lazee.feature.autolog.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.autolog.AutoLogConfig
import io.github.steven_stu77.lazee.feature.autolog.classify.MerchantKey
import io.github.steven_stu77.lazee.feature.autolog.parser.OtpFilter
import javax.inject.Inject

@HiltViewModel
class ParserTestViewModel @Inject constructor(val config: AutoLogConfig) : ViewModel()

/** Debug builds only (spec §5.10): paste a notification text and see what the rules make of it. Nothing is saved. */
@Composable
fun ParserTestScreen(onBack: () -> Unit, vm: ParserTestViewModel = hiltViewModel()) {
    val apps = vm.config.parserConfigs
    var pkg by remember { mutableStateOf(apps.firstOrNull()?.`package` ?: "") }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.pt_title), onBack, stringResource(R.string.back))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.forEach { a -> Chip(a.displayName, a.`package` == pkg, onClick = { pkg = a.`package` }) }
            }
            LazeeTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 120.dp), placeholder = stringResource(R.string.pt_hint), singleLine = false)
            if (text.isNotBlank()) SectionCard {
                val parsed = vm.config.engine.parse(pkg, text)
                when {
                    OtpFilter.isOtp(text) -> Text(stringResource(R.string.pt_otp), style = Type.body.copy(color = Base.dangerText))
                    parsed == null -> Text(stringResource(R.string.pt_no_match), style = Type.body.copy(color = Base.warnStrong))
                    else -> {
                        val key = MerchantKey.of(parsed.merchant)
                        val cls = vm.config.classifier.classify(key, null, parsed.direction.name == "PERSON_TRANSFER")
                        listOf(
                            "amount" to Money.format(parsed.amount), "merchant" to parsed.merchant, "merchantKey" to key,
                            "direction" to parsed.direction.name, "incoming" to parsed.incoming.toString(), "last4" to (parsed.last4 ?: "—"),
                            "category" to (cls.builtInKey ?: "memory"), "confidence" to cls.confidence.name,
                        ).forEach { (k, v) ->
                            Row { Text(k, style = Type.small, modifier = Modifier.weight(0.4f)); Text(v, style = Type.body, modifier = Modifier.weight(0.6f)) }
                        }
                    }
                }
            }
        }
    }
}
