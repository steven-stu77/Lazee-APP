package io.github.steven_stu77.lazee.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.steven_stu77.lazee.BuildConfig
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type

/** Open-source libraries bundled in the app and their licences. */
private val libraries = listOf(
    "AndroidX / Jetpack Compose / Room / WorkManager / DataStore / Biometric" to "Apache 2.0",
    "Dagger Hilt" to "Apache 2.0",
    "Kotlin, kotlinx.coroutines, kotlinx.serialization" to "Apache 2.0",
    "SQLCipher for Android (Zetetic)" to "BSD-style",
    "kotlin-csv" to "Apache 2.0",
    "exp4j" to "Apache 2.0",
    "Inter font (The Inter Project Authors)" to "SIL Open Font License 1.1",
)

/** 关于 (spec §5.7, §8): version, privacy note, open-source licences. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.about_title), onBack, stringResource(R.string.back))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Image(painterResource(R.drawable.logo), null, Modifier.size(72.dp))
                Text(stringResource(R.string.app_name), style = Type.pageTitle)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = Type.small)
            }
            Text(stringResource(R.string.about_privacy), style = Type.section, modifier = Modifier.padding(horizontal = 4.dp))
            SectionCard {
                listOf(R.string.about_p_reads, R.string.about_p_stores, R.string.about_p_offline, R.string.about_p_backup, R.string.about_p_off).forEach {
                    Text(stringResource(it), style = Type.body.copy(fontSize = Type.small.fontSize * 1.08f))
                }
            }
            Text(stringResource(R.string.about_licenses), style = Type.section, modifier = Modifier.padding(horizontal = 4.dp))
            SectionCard {
                libraries.forEach { (name, licence) ->
                    Column { Text(name, style = Type.body); Text(licence, style = Type.tiny) }
                }
            }
        }
    }
}
