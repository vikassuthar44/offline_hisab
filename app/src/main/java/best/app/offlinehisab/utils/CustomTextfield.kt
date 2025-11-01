package best.app.offlinehisab.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue


@Composable
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxCharacter: Int = 25,
) {
    OutlinedTextField(
        value = value.text,
        onValueChange = {
            if (it.length <= maxCharacter) {
                onValueChange(TextFieldValue(it))
            }
        },
        label = {
            Text(
                placeholder
            )
        },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = imeAction,
            keyboardType = keyboardType
        ),
    )
}