package io.github.steven_stu77.lazee.core.security

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type

/** Opaque cover with only the logo: nothing behind it is visible or tappable while locked. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Base.page)
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(R.drawable.logo), null, Modifier.size(96.dp))
        Text(stringResource(R.string.app_name), style = Type.pageTitle)
        Text(stringResource(R.string.lock_hint), style = Type.small)
        PrimaryButton(stringResource(R.string.lock_unlock), onUnlock, Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}
