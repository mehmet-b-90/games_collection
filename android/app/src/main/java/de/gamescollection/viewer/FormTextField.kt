package de.gamescollection.viewer

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

internal val FormFieldShape = RoundedCornerShape(12.dp)
private val FormFieldHorizontalPadding = 24.dp
private val FormFieldVerticalPadding = 8.dp
internal val FormFieldPadding = PaddingValues(horizontal = FormFieldHorizontalPadding, vertical = FormFieldVerticalPadding)
internal val FormFieldMinHeight = 52.dp

/** Matches the filter selectors: an in-border caption above the editable value. */
@Composable internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val borderColor = when {
        !enabled -> colors.onSurface.copy(alpha = .12f)
        focused -> colors.primary
        else -> colors.outline
    }
    val valueColor = if (enabled) colors.primary else colors.onSurface.copy(alpha = .38f)
    val labelColor = if (enabled) colors.onSurfaceVariant else colors.onSurface.copy(alpha = .38f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = FormFieldMinHeight)
            .border(if (focused && enabled) 2.dp else 1.dp, borderColor, FormFieldShape)
            .semantics { contentDescription = label },
        enabled = enabled,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = valueColor),
        cursorBrush = SolidColor(colors.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        decorationBox = { input ->
            Row(Modifier.fillMaxWidth().padding(horizontal = FormFieldHorizontalPadding), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompositionLocalProvider(LocalContentColor provides valueColor) { leadingIcon?.invoke() }
                Column(Modifier.weight(1f).padding(vertical = FormFieldVerticalPadding), verticalArrangement = Arrangement.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = labelColor)
                        }
                        input()
                    }
                }
                CompositionLocalProvider(LocalContentColor provides valueColor) { trailingIcon?.invoke() }
            }
        },
    )
}
