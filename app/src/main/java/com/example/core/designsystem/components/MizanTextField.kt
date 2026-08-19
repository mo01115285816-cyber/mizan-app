package com.example.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.MizanColors
import com.example.core.designsystem.MizanShapes
import com.example.core.designsystem.MizanSpacing
import com.example.core.designsystem.MizanTypography

/**
 * Text field for Mizan.
 * Uses WarmWhite surface, 18dp rounded corners, Line border, and clear Arabic labels.
 */
@Composable
fun MizanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    testTag: String = "mizan_text_field"
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MizanTypography.Label,
            color = MizanColors.Charcoal
        )
        Spacer(modifier = Modifier.height(MizanSpacing.LabelToControlSpacing))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = MizanShapes.InputAndButton,
            enabled = enabled,
            singleLine = singleLine,
            isError = isError,
            textStyle = MizanTypography.Body.copy(color = MizanColors.Charcoal),
            placeholder = if (placeholder != null) {
                {
                    Text(
                        text = placeholder,
                        style = MizanTypography.Body,
                        color = MizanColors.MutedGray
                    )
                }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MizanColors.WarmWhite,
                unfocusedContainerColor = MizanColors.WarmWhite,
                disabledContainerColor = MizanColors.Paper,
                errorContainerColor = MizanColors.WarmWhite,
                focusedBorderColor = MizanColors.Charcoal,
                unfocusedBorderColor = MizanColors.Line,
                errorBorderColor = Color(0xFFD32F2F),
                focusedTextColor = MizanColors.Charcoal,
                unfocusedTextColor = MizanColors.Charcoal,
                cursorColor = MizanColors.Charcoal
            )
        )
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MizanTypography.Label,
                color = Color(0xFFD32F2F)
            )
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                style = MizanTypography.Label,
                color = MizanColors.MutedGray
            )
        }
    }
}
