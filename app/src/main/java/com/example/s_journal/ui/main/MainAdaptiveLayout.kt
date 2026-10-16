package com.example.s_journal.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import com.example.s_journal.theme.AnimationTokens
import com.example.s_journal.theme.PanelSquircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.example.s_journal.ui.calendar.CalendarPanelScreen
import com.example.s_journal.ui.calendar.CalendarViewModel
import com.example.s_journal.ui.entry.EntryPanelScreen

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainAdaptiveLayout(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val isTabletOrExpanded = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    // For compact/phone mode single-pane navigation
    var currentPhonePane by rememberSaveable { mutableStateOf(PhonePane.CALENDAR) }

    if (isTabletOrExpanded) {
        // TABLET TWO-PANE LAYOUT (Samsung Notes Style)
        BackHandler(enabled = isFullscreen) {
            isFullscreen = false
        }

        val isDark = isSystemInDarkTheme()
        val panelShape = PanelSquircleShape

        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left Panel (Rounded right edge with visible drop shadow)
            AnimatedVisibility(
                visible = !isFullscreen,
                enter = fadeIn(animationSpec = AnimationTokens.visibilitySpring()) +
                        expandHorizontally(animationSpec = AnimationTokens.panelSpring()),
                exit = fadeOut(animationSpec = AnimationTokens.visibilitySpring()) +
                        shrinkHorizontally(animationSpec = AnimationTokens.panelSpring()),
                modifier = Modifier.zIndex(2f)
            ) {
                Surface(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                        .shadow(
                            elevation = if (isDark) 6.dp else 12.dp,
                            shape = panelShape,
                            spotColor = if (isDark) Color(0x80000000) else Color(0x38000000),
                            ambientColor = if (isDark) Color(0x50000000) else Color(0x20000000)
                        ),
                    shape = panelShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = if (!isDark) BorderStroke(1.dp, Color(0x14000000)) else null
                ) {
                    CalendarPanelScreen(
                        onAddEntry = { selectedDate ->
                            // In Phase 3: trigger new entry creation
                        },
                        onEntryClick = { entry ->
                            // Entry clicked
                        },
                        showAddButton = false,
                        viewModel = viewModel
                    )
                }
            }

            // Right Panel (Notebook Ruled Canvas + Fullscreen Button + Spen Toolbar)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .zIndex(1f)
            ) {
                EntryPanelScreen(
                    selectedDate = uiState.selectedDate,
                    entry = uiState.selectedDateEntry,
                    isFullscreen = isFullscreen,
                    onToggleFullscreen = { isFullscreen = !isFullscreen }
                )
            }
        }
    } else {
        // COMPACT PHONE SINGLE-PANE LAYOUT
        BackHandler(enabled = currentPhonePane == PhonePane.ENTRY) {
            currentPhonePane = PhonePane.CALENDAR
        }

        when (currentPhonePane) {
            PhonePane.CALENDAR -> {
                CalendarPanelScreen(
                    onAddEntry = { selectedDate ->
                        currentPhonePane = PhonePane.ENTRY
                    },
                    onEntryClick = { entry ->
                        currentPhonePane = PhonePane.ENTRY
                    },
                    showAddButton = true,
                    viewModel = viewModel,
                    modifier = modifier
                )
            }
            PhonePane.ENTRY -> {
                EntryPanelScreen(
                    selectedDate = uiState.selectedDate,
                    entry = uiState.selectedDateEntry,
                    isFullscreen = true,
                    onToggleFullscreen = { currentPhonePane = PhonePane.CALENDAR },
                    modifier = modifier
                )
            }
        }
    }
}

private enum class PhonePane {
    CALENDAR,
    ENTRY
}
