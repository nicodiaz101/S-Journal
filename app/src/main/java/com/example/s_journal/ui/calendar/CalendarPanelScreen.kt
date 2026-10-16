package com.example.s_journal.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.s_journal.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.s_journal.data.local.entity.JournalEntry
import com.example.s_journal.data.model.EntryType
import com.example.s_journal.theme.CardSquircleShape
import com.example.s_journal.ui.components.AddNowButton
import com.example.s_journal.ui.components.PanelHeader
import com.example.s_journal.ui.components.SJournalCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CalendarPanelScreen(
    onAddEntry: (LocalDate) -> Unit,
    onEntryClick: (JournalEntry) -> Unit,
    modifier: Modifier = Modifier,
    showAddButton: Boolean = false,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalendarPanelContent(
        uiState = uiState,
        onSelectDate = viewModel::onSelectDate,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onGoToToday = viewModel::onGoToToday,
        onAddEntry = { onAddEntry(uiState.selectedDate) },
        onEntryClick = onEntryClick,
        showAddButton = showAddButton,
        modifier = modifier
    )
}

@Composable
internal fun CalendarPanelContent(
    uiState: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    onAddEntry: () -> Unit,
    onEntryClick: (JournalEntry) -> Unit,
    showAddButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = if (showAddButton) 16.dp else 8.dp)
        ) {
            // Top Bar with ☰ and ⚙ (Samsung Notes style)
            PanelHeader(
                menuItems = listOf(
                    stringResource(R.string.calendar_go_to_today) to onGoToToday
                )
            )

            // Scrollable Content (Calendar + Entry Preview)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Month Calendar
                MonthCalendar(
                    currentMonth = uiState.currentMonth,
                    selectedDate = uiState.selectedDate,
                    datesWithEntries = uiState.datesWithEntries,
                    onDateSelected = onSelectDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onGoToToday = onGoToToday
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Entry Preview Card for the Selected Date
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (uiState.selectedDateEntry != null) {
                        SelectedEntryCard(
                            entry = uiState.selectedDateEntry,
                            onClick = { onEntryClick(uiState.selectedDateEntry) }
                        )
                    } else {
                        EmptyDayPlaceholder(
                            selectedDate = uiState.selectedDate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ADD NOW Button — only displayed on Phone / Single-Pane mode
            if (showAddButton) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AddNowButton(
                        onClick = onAddEntry,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedEntryCard(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SJournalCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardSquircleShape)
            .clickable { onClick() },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CardSquircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.type == EntryType.CANVAS) Icons.Default.Brush else Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title ?: if (entry.type == EntryType.CANVAS) stringResource(R.string.calendar_drawing_entry) else stringResource(R.string.calendar_journal_entry),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!entry.textContent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.textContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (entry.type == EntryType.CANVAS) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.calendar_handwritten_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDayPlaceholder(
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()) }

    SJournalCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = selectedDate.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.calendar_no_entry_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
