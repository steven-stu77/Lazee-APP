package io.github.steven_stu77.lazee.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.onboarding.filterMoney

/** Asks for one RM amount. [onSave] gets cents (0 when empty). */
@Composable
fun MoneyDialog(title: String, initial: Long, onDismiss: () -> Unit, message: String? = null, onSave: (Long) -> Unit) {
    var text by remember { mutableStateOf(Money.toInput(initial)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message != null) Text(message, style = Type.small)
                LazeeTextField(text, { text = it.filterMoney() }, Modifier.fillMaxWidth(), placeholder = "0.00", prefix = "RM", keyboardType = KeyboardType.Decimal)
            }
        },
        confirmButton = { TextButton({ onSave(Money.parse(text) ?: 0); onDismiss() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
