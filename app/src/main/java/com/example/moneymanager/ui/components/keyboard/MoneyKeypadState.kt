package com.example.moneymanager.ui.components.keyboard

sealed interface KeypadAction {
    data class Digit(val digit: Char) : KeypadAction
    object TripleZero : KeypadAction
    object Dot : KeypadAction
    data class AddAmount(val delta: Double) : KeypadAction
    object Backspace : KeypadAction
    object Clear : KeypadAction
    data class Operator(val op: Char) : KeypadAction
    object Calculate : KeypadAction
    object Done : KeypadAction
}

object MoneyKeypadHelper {

    fun processAction(currentValue: String, action: KeypadAction): String {
        return when (action) {
            is KeypadAction.Digit -> {
                if (currentValue == "0") action.digit.toString()
                else currentValue + action.digit
            }

            is KeypadAction.TripleZero -> {
                if (currentValue.isEmpty() || currentValue == "0") "0"
                else {
                    val lastChar = currentValue.lastOrNull()
                    if (lastChar != null && (lastChar == '+' || lastChar == '-' || lastChar == '.')) {
                        currentValue
                    } else {
                        currentValue + "000"
                    }
                }
            }

            is KeypadAction.Dot -> {
                if (currentValue.isEmpty()) "0."
                else {
                    // Check if current number segment already has a dot
                    val lastOpIndex = currentValue.lastIndexOfAny(charArrayOf('+', '-'))
                    val currentSegment = if (lastOpIndex >= 0) currentValue.substring(lastOpIndex + 1) else currentValue
                    if (currentSegment.contains('.')) {
                        currentValue
                    } else {
                        currentValue + "."
                    }
                }
            }

            is KeypadAction.AddAmount -> {
                val evaluated = evaluateExpression(currentValue)
                val newTotal = (evaluated + action.delta).coerceAtLeast(0.0)
                formatDoubleToString(newTotal)
            }

            is KeypadAction.Backspace -> {
                if (currentValue.length <= 1) ""
                else currentValue.dropLast(1)
            }

            is KeypadAction.Clear -> ""

            is KeypadAction.Operator -> {
                if (currentValue.isEmpty()) ""
                else {
                    val lastChar = currentValue.last()
                    if (lastChar == '+' || lastChar == '-') {
                        // Replace last operator
                        currentValue.dropLast(1) + action.op
                    } else if (lastChar == '.') {
                        currentValue.dropLast(1) + action.op
                    } else {
                        // Evaluate any existing operation first to keep simple
                        val eval = evaluateExpression(currentValue)
                        formatDoubleToString(eval) + action.op
                    }
                }
            }

            is KeypadAction.Calculate, KeypadAction.Done -> {
                if (currentValue.isEmpty()) ""
                else {
                    val eval = evaluateExpression(currentValue)
                    formatDoubleToString(eval)
                }
            }
        }
    }

    fun evaluateExpression(input: String): Double {
        if (input.isBlank()) return 0.0
        val sanitized = input.trimEnd('+', '-', '.')
        if (sanitized.isBlank()) return 0.0

        return try {
            if (sanitized.contains('+')) {
                val parts = sanitized.split('+')
                parts.sumOf { it.toDoubleOrNull() ?: 0.0 }
            } else if (sanitized.contains('-')) {
                val parts = sanitized.split('-')
                if (parts.isNotEmpty()) {
                    var result = parts[0].toDoubleOrNull() ?: 0.0
                    for (i in 1 until parts.size) {
                        result -= (parts[i].toDoubleOrNull() ?: 0.0)
                    }
                    result
                } else 0.0
            } else {
                sanitized.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun formatDoubleToString(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
