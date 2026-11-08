package com.akerusan.fieldincidentreporter.feature.incidenteditor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.akerusan.fieldincidentreporter.core.designsystem.label.labelRes
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory

/** Category picker: a read-only text field that opens the list of categories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryField(selected: IncidentCategory?, onSelect: (IncidentCategory) -> Unit, error: String?) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { stringResource(it.labelRes) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.incident_editor_field_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            IncidentCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes)) },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    },
                )
            }
        }
    }
}
