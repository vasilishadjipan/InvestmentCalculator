package com.vasilis.investmentcalculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasilis.investmentcalculator.CalculationMode
import com.vasilis.investmentcalculator.CompoundFrequency
import com.vasilis.investmentcalculator.ContributionFrequency
import com.vasilis.investmentcalculator.ContributionTiming
import com.vasilis.investmentcalculator.InvestmentCalculator
import com.vasilis.investmentcalculator.InvestmentInput
import com.vasilis.investmentcalculator.InvestmentInputState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InvestmentInputFields(
    input: InvestmentInput,
    inputState: InvestmentInputState,
    onTargetEndAmountChange: (String) -> Unit,
    onStartingAmountChange: (String) -> Unit,
    onInvestLengthYearsChange: (String) -> Unit,
    onReturnRatePercentChange: (String) -> Unit,
    onAdditionalContributionChange: (String) -> Unit,
    onContributionFrequencyChange: (ContributionFrequency) -> Unit,
    onContributionTimingChange: (ContributionTiming) -> Unit,
    onCompoundFrequencyChange: (CompoundFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencySymbol = InvestmentCalculator.getCurrencySymbol()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Investment Parameters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Target End Amount
            if (input.mode != CalculationMode.END_AMOUNT) {
                NumberInputField(
                    label = "Target End Amount",
                    value = inputState.targetEndAmountStr,
                    onValueChange = onTargetEndAmountChange,
                    prefix = "$currencySymbol "
                )
            }

            // Starting Amount
            if (input.mode != CalculationMode.STARTING_AMOUNT) {
                NumberInputField(
                    label = "Starting Amount",
                    value = inputState.startingAmountStr,
                    onValueChange = onStartingAmountChange,
                    prefix = "$currencySymbol "
                )
            }

            // Investment Length
            if (input.mode != CalculationMode.INVEST_LENGTH) {
                NumberInputField(
                    label = "Investment Length",
                    value = inputState.investLengthStr,
                    onValueChange = onInvestLengthYearsChange,
                    suffix = " years"
                )
            }

            // Return Rate
            if (input.mode != CalculationMode.RETURN_RATE) {
                NumberInputField(
                    label = "Annual Return Rate",
                    value = inputState.returnRateStr,
                    onValueChange = onReturnRatePercentChange,
                    suffix = " %"
                )
            }

            // Additional Contribution
            if (input.mode != CalculationMode.CONTRIBUTE_AMOUNT) {
                NumberInputField(
                    label = "Additional Contribution",
                    value = inputState.additionalContributionStr,
                    onValueChange = onAdditionalContributionChange,
                    prefix = "$currencySymbol "
                )
            }

            // Contribution Frequency Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Contribution Frequency",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)

                ) {
                    ContributionFrequency.entries.forEach { freq ->
                        InvestmentFilterChip(
                            text = freq.title,
                            selected = input.contributionFrequency == freq,
                            onClick = { onContributionFrequencyChange(freq) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // Contribution Timing Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Contribution Timing",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    ContributionTiming.entries.forEach { timing ->
                        InvestmentFilterChip(
                            text = timing.title,
                            selected = input.contributionTiming == timing,
                            onClick = { onContributionTimingChange(timing) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // Compound Frequency Dropdown
            var compoundDropdownExpanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Compound Frequency",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                ExposedDropdownMenuBox(
                    expanded = compoundDropdownExpanded,
                    onExpandedChange = { compoundDropdownExpanded = !compoundDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = input.compoundFrequency.title,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = compoundDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = compoundDropdownExpanded,
                        onDismissRequest = { compoundDropdownExpanded = false }
                    ) {
                        CompoundFrequency.entries.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.title) },
                                onClick = {
                                    onCompoundFrequencyChange(freq)
                                    compoundDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvestmentFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.primaryContainer,
            selectedBorderColor = MaterialTheme.colorScheme.primaryContainer
        ),
        label = {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        },
        modifier = modifier
    )
}

@Composable
private fun NumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String? = null,
    suffix: String? = null,
    modifier: Modifier = Modifier
) {
    val isError = value.isEmpty()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) {
            {
                Text(
                    text = "Field cannot be empty",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else null,
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
