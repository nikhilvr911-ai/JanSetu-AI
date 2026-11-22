package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.AnalysisUiState
import com.example.ui.components.DocumentInputPanel
import com.example.ui.components.JanSetuHeader
import com.example.ui.components.ResultsOutputPanel
import com.example.viewmodel.JanSetuViewModel
import com.example.viewmodel.MobileTab

@Composable
fun JanSetuScreen(
    viewModel: JanSetuViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    // Zero-permission Storage Access Framework picker for PDFs and document images
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadDocumentFromUri(uri)
        }
    }

    // Zero-permission Android Photo Picker for notice photos (Google Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadDocumentFromUri(uri)
        }
    }

    val onPickPdfOrFile = {
        documentPickerLauncher.launch(arrayOf("application/pdf", "image/*"))
    }
    val onPickNoticePhoto = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top civic header & multilingual selector
            JanSetuHeader(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelected = viewModel::selectLanguage
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isExpandedTwoColumn = maxWidth >= 840.dp

                if (isExpandedTwoColumn) {
                    // Two-column desktop / tablet / foldable layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.46f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            DocumentInputPanel(
                                uiState = uiState,
                                onPickPdfOrFile = onPickPdfOrFile,
                                onPickNoticePhoto = onPickNoticePhoto,
                                onClearAttachment = viewModel::clearAttachment,
                                onDocumentTextChange = viewModel::updateDocumentText,
                                onClearDocumentText = viewModel::clearDocumentText,
                                onApplySamplePreset = { preset ->
                                    viewModel.applySamplePreset(preset, applyProfile = true)
                                },
                                onAgeChange = viewModel::updateAge,
                                onOccupationChange = viewModel::updateOccupation,
                                onIncomeChange = viewModel::updateAnnualIncome,
                                onStateChange = viewModel::updateStateName,
                                onContextChange = viewModel::updateAdditionalContext,
                                onAnalyzeClick = viewModel::analyzeDocument
                            )
                        }

                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.54f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            ResultsOutputPanel(
                                uiState = uiState,
                                voiceState = voiceState,
                                isCompactLayout = false,
                                onEditInputsClick = {},
                                onRetryClick = viewModel::analyzeDocument,
                                onReexplainInLanguageClick = viewModel::analyzeDocument,
                                onQuickTrySampleAndAnalyze = { preset ->
                                    viewModel.applySamplePreset(preset, applyProfile = true)
                                    viewModel.analyzeDocument()
                                },
                                onPlaySummary = viewModel::playSummaryVoice,
                                onPauseVoice = viewModel::pauseVoice,
                                onResumeVoice = viewModel::resumeVoice,
                                onStopVoice = viewModel::stopVoice,
                                onToggleDocumentCheck = viewModel::toggleDocumentChecked,
                                onFollowUpInputChange = viewModel::updateFollowUpInput,
                                onSendFollowUp = viewModel::askFollowUpQuestion,
                                onPlayFollowUpVoice = viewModel::playFollowUpVoice
                            )
                        }
                    }
                } else {
                    // Compact / Phone stacked layout with segmented tab switcher
                    BackHandler(enabled = uiState.activeMobileTab == MobileTab.AI_RESULTS) {
                        viewModel.selectMobileTab(MobileTab.INPUT_PROFILE)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    SegmentedButton(
                                        selected = uiState.activeMobileTab == MobileTab.INPUT_PROFILE,
                                        onClick = { viewModel.selectMobileTab(MobileTab.INPUT_PROFILE) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("tab_input_profile")
                                    ) {
                                        Text(
                                            text = stringResource(R.string.tab_input_profile),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    SegmentedButton(
                                        selected = uiState.activeMobileTab == MobileTab.AI_RESULTS,
                                        onClick = { viewModel.selectMobileTab(MobileTab.AI_RESULTS) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("tab_ai_results")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.tab_ai_results),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            if (uiState.analysisState is AnalysisUiState.Success) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondary)
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }

                        AnimatedContent(
                            targetState = uiState.activeMobileTab,
                            label = "mobile_panel_switch",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) { tab ->
                            when (tab) {
                                MobileTab.INPUT_PROFILE -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        DocumentInputPanel(
                                            uiState = uiState,
                                            onPickPdfOrFile = onPickPdfOrFile,
                                            onPickNoticePhoto = onPickNoticePhoto,
                                            onClearAttachment = viewModel::clearAttachment,
                                            onDocumentTextChange = viewModel::updateDocumentText,
                                            onClearDocumentText = viewModel::clearDocumentText,
                                            onApplySamplePreset = { preset ->
                                                viewModel.applySamplePreset(preset, applyProfile = true)
                                            },
                                            onAgeChange = viewModel::updateAge,
                                            onOccupationChange = viewModel::updateOccupation,
                                            onIncomeChange = viewModel::updateAnnualIncome,
                                            onStateChange = viewModel::updateStateName,
                                            onContextChange = viewModel::updateAdditionalContext,
                                            onAnalyzeClick = viewModel::analyzeDocument
                                        )
                                    }
                                }

                                MobileTab.AI_RESULTS -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        ResultsOutputPanel(
                                            uiState = uiState,
                                            voiceState = voiceState,
                                            isCompactLayout = true,
                                            onEditInputsClick = {
                                                viewModel.selectMobileTab(MobileTab.INPUT_PROFILE)
                                            },
                                            onRetryClick = viewModel::analyzeDocument,
                                            onReexplainInLanguageClick = viewModel::analyzeDocument,
                                            onQuickTrySampleAndAnalyze = { preset ->
                                                viewModel.applySamplePreset(preset, applyProfile = true)
                                                viewModel.analyzeDocument()
                                            },
                                            onPlaySummary = viewModel::playSummaryVoice,
                                            onPauseVoice = viewModel::pauseVoice,
                                            onResumeVoice = viewModel::resumeVoice,
                                            onStopVoice = viewModel::stopVoice,
                                            onToggleDocumentCheck = viewModel::toggleDocumentChecked,
                                            onFollowUpInputChange = viewModel::updateFollowUpInput,
                                            onSendFollowUp = viewModel::askFollowUpQuestion,
                                            onPlayFollowUpVoice = viewModel::playFollowUpVoice
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
