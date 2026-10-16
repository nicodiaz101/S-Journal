package com.example.s_journal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.s_journal.R

@Composable
fun PanelHeader(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    menuItems: List<Pair<String, () -> Unit>> = emptyList()
) {
    var settingsExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings Icon (⚙) with optional menu
        Box {
            IconButton(
                onClick = {
                    if (menuItems.isNotEmpty()) {
                        settingsExpanded = true
                    } else {
                        onSettingsClick()
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.panel_header_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (menuItems.isNotEmpty()) {
                DropdownMenu(
                    expanded = settingsExpanded,
                    onDismissRequest = { settingsExpanded = false }
                ) {
                    menuItems.forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(text = label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                settingsExpanded = false
                                action()
                            }
                        )
                    }
                }
            }
        }
    }
}
