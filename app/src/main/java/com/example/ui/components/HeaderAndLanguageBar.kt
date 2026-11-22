package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SupportedLanguage
import com.example.ui.theme.LocalCivicColors

@Composable
fun JanSetuHeader(
    selectedLanguage: SupportedLanguage,
    onLanguageSelected: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val civicColors = LocalCivicColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(civicColors.headerStart, civicColors.headerEnd)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 14.dp, bottom = 14.dp)
    ) {
        // Tricolor-inspired subtle saffron-to-teal civic accent hairline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(civicColors.saffronHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = stringResource(R.string.app_name),
                    tint = civicColors.onHeader,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = civicColors.onHeader,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = civicColors.onHeader.copy(alpha = 0.16f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "CIVIC AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = civicColors.onHeader,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = civicColors.onHeader.copy(alpha = 0.86f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = stringResource(R.string.section_language),
                tint = civicColors.saffronHighlight,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.section_language),
                style = MaterialTheme.typography.labelMedium,
                color = civicColors.onHeader.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SupportedLanguage.entries, key = { it.code }) { language ->
                val isSelected = language == selectedLanguage
                LanguagePill(
                    language = language,
                    selected = isSelected,
                    onClick = { onLanguageSelected(language) }
                )
            }
        }
    }
}

@Composable
private fun LanguagePill(
    language: SupportedLanguage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val civicColors = LocalCivicColors.current
    val containerColor = if (selected) {
        civicColors.saffronHighlight
    } else {
        civicColors.onHeader.copy(alpha = 0.12f)
    }
    val borderColor = if (selected) {
        civicColors.onHeader.copy(alpha = 0.45f)
    } else {
        civicColors.onHeader.copy(alpha = 0.22f)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag("language_chip_${language.code}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = language.nativeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = civicColors.onHeader,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (language != SupportedLanguage.ENGLISH) {
                Text(
                    text = language.englishName,
                    style = MaterialTheme.typography.labelSmall,
                    color = civicColors.onHeader.copy(alpha = if (selected) 0.92f else 0.72f)
                )
            }
        }
    }
}
