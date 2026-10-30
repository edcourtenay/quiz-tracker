package uk.co.edcourtenay.quiztracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * A compact numeric field for a nullable integer score.
 * An empty field maps to `null` ("not entered"), matching a blank sheet cell.
 *
 * When [requestFocus] flips to true the field takes focus and selects its
 * current text, so typing replaces the existing score.
 */
@Composable
fun NumberField(
    value: Int?,
    label: String,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
) {
    // Selection is kept at the end so the cursor does not jump to the start when
    // the value round-trips through Room.
    var field by remember(value) {
        val text = value?.toString().orEmpty()
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            field = field.copy(selection = TextRange(0, field.text.length))
            keyboard?.show()
        }
    }

    OutlinedTextField(
        value = field,
        onValueChange = { input ->
            val digits = input.text.filter { it.isDigit() }.take(5)
            field = if (digits == input.text) {
                input
            } else {
                TextFieldValue(digits, TextRange(digits.length))
            }
            onValueChange(digits.toIntOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.focusRequester(focusRequester),
    )
}

/**
 * A field for a decimal value, used for tie-break guesses and correct answers.
 * An empty field maps to `null`.
 */
@Composable
fun DecimalField(
    value: Double?,
    label: String,
    onValueChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var field by remember(value) {
        val text = formatDecimal(value)
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }

    OutlinedTextField(
        value = field,
        onValueChange = { input ->
            val cleaned = cleanDecimal(input.text)
            field = if (cleaned == input.text) {
                input
            } else {
                TextFieldValue(cleaned, TextRange(cleaned.length))
            }
            onValueChange(cleaned.toDoubleOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/** Keeps digits and at most one decimal point. */
private fun cleanDecimal(input: String): String {
    val digitsAndDots = input.filter { it.isDigit() || it == '.' }
    val firstDot = digitsAndDots.indexOf('.')
    if (firstDot < 0) return digitsAndDots
    return digitsAndDots.take(firstDot + 1) + digitsAndDots.drop(firstDot + 1).replace(".", "")
}

private fun formatDecimal(value: Double?): String = when {
    value == null -> ""
    value % 1.0 == 0.0 -> value.toLong().toString()
    else -> value.toString()
}

/** A small label/value tile used for the summary strip and computed values. */@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasis) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A pill showing a computed value such as Subtotal or Final Position. */
@Composable
fun ValueChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
