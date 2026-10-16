package com.vasilis.investmentcalculator.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vasilis.investmentcalculator.CalculationMode

@Composable
fun ModeTabSelector(
    selectedMode: CalculationMode,
    onModeSelected: (CalculationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = CalculationMode.entries

    PrimaryScrollableTabRow(
        selectedTabIndex = modes.indexOf(selectedMode),
        edgePadding = 16.dp,
        modifier = modifier,
        indicator = @Composable {
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(modes.indexOf(selectedMode), matchContentSize = true),
                width = Dp.Unspecified,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
        }
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selectedMode
            Tab(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                text = {
                    Text(
                        text = mode.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            )
        }
    }
}
