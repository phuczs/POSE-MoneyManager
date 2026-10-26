package com.example.moneymanager.ui.components.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneymanager.ui.theme.MediumGreen
import com.example.moneymanager.ui.theme.TextGray
import com.example.moneymanager.ui.theme.TextPrimary

@Composable
fun MoneyKeypad(
    currentAmount: String,
    onAmountChanged: (String) -> Unit,
    onDonePressed: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = MediumGreen,
    currencyCode: String = "VND"
) {
    val haptic = LocalHapticFeedback.current

    val onAction: (KeypadAction) -> Unit = { action ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val newAmount = MoneyKeypadHelper.processAction(currentAmount, action)
        onAmountChanged(newAmount)
        if (action is KeypadAction.Done) {
            onDonePressed()
        }
    }

    val isVnd = currencyCode.equals("VND", ignoreCase = true)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drag handle decoration
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 1. Quick Shortcut Chips Row
            QuickShortcutsRow(
                currencyCode = currencyCode,
                primaryColor = primaryColor,
                onAddAmount = { delta -> onAction(KeypadAction.AddAmount(delta)) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Numeric & Operator Keypad Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: 7, 8, 9, C
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeypadButton("7", Modifier.weight(1f)) { onAction(KeypadAction.Digit('7')) }
                    KeypadButton("8", Modifier.weight(1f)) { onAction(KeypadAction.Digit('8')) }
                    KeypadButton("9", Modifier.weight(1f)) { onAction(KeypadAction.Digit('9')) }
                    KeypadButton(
                        text = "C",
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color(0xFFD32F2F)
                    ) { onAction(KeypadAction.Clear) }
                }

                // Row 2: 4, 5, 6, -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeypadButton("4", Modifier.weight(1f)) { onAction(KeypadAction.Digit('4')) }
                    KeypadButton("5", Modifier.weight(1f)) { onAction(KeypadAction.Digit('5')) }
                    KeypadButton("6", Modifier.weight(1f)) { onAction(KeypadAction.Digit('6')) }
                    KeypadButton(
                        text = "-",
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFFECEFF1),
                        contentColor = TextPrimary
                    ) { onAction(KeypadAction.Operator('-')) }
                }

                // Row 3: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeypadButton("1", Modifier.weight(1f)) { onAction(KeypadAction.Digit('1')) }
                    KeypadButton("2", Modifier.weight(1f)) { onAction(KeypadAction.Digit('2')) }
                    KeypadButton("3", Modifier.weight(1f)) { onAction(KeypadAction.Digit('3')) }
                    KeypadButton(
                        text = "+",
                        modifier = Modifier.weight(1f),
                        containerColor = Color(0xFFECEFF1),
                        contentColor = TextPrimary
                    ) { onAction(KeypadAction.Operator('+')) }
                }

                // Row 4: 000 / ., 0, ⌫, Done (✓)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val specialKeyText = if (isVnd) "000" else "."
                    KeypadButton(
                        text = specialKeyText,
                        modifier = Modifier.weight(1f),
                        containerColor = Color.White,
                        contentColor = TextPrimary
                    ) {
                        if (isVnd) onAction(KeypadAction.TripleZero) else onAction(KeypadAction.Dot)
                    }

                    KeypadButton("0", Modifier.weight(1f)) { onAction(KeypadAction.Digit('0')) }

                    // Backspace Icon Key
                    KeypadIconButton(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        modifier = Modifier.weight(1f),
                        containerColor = Color.White,
                        contentColor = TextGray
                    ) { onAction(KeypadAction.Backspace) }

                    // Check / Done Button (Colored with primary theme)
                    val isExpressionPending = currentAmount.contains('+') || currentAmount.contains('-')
                    KeypadButton(
                        text = if (isExpressionPending) "=" else "",
                        icon = if (!isExpressionPending) Icons.Default.Check else null,
                        modifier = Modifier.weight(1f),
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ) {
                        if (isExpressionPending) {
                            onAction(KeypadAction.Calculate)
                        } else {
                            onAction(KeypadAction.Done)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutsRow(
    currencyCode: String,
    primaryColor: Color,
    onAddAmount: (Double) -> Unit
) {
    val shortcuts = remember(currencyCode) {
        if (currencyCode.equals("VND", ignoreCase = true)) {
            listOf(
                "+10k" to 10000.0,
                "+50k" to 50000.0,
                "+100k" to 100000.0,
                "+500k" to 500000.0
            )
        } else {
            listOf(
                "+$1" to 1.0,
                "+$5" to 5.0,
                "+$10" to 10.0,
                "+$50" to 50.0
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shortcuts.forEach { (label, delta) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor.copy(alpha = 0.1f))
                    .clickable { onAddAmount(delta) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    containerColor: Color = Color.White,
    contentColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = 20.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    contentColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
