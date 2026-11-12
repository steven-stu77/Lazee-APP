package io.github.steven_stu77.lazee.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.calc.Calculator
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type

private val rows = listOf("789÷", "456×", "123−", ".0⌫+")

/** 4-column calculator keypad; operator keys use the theme's light tint. */
@Composable
fun CalcKeypad(onKey: (Char) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val backspace = stringResource(R.string.a11y_backspace)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    val op = k in Calculator.OPS
                    Box(
                        Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (op) p.tint else Base.card)
                            .clickable(role = Role.Button) { onKey(k) }
                            .semantics { if (k == '⌫') contentDescription = backspace },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(k.toString(), style = Type.body.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium, color = if (op) p.tintText else Base.text))
                    }
                }
            }
        }
    }
}
