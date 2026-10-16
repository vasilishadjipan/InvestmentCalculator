package com.vasilis.investmentcalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vasilis.investmentcalculator.ui.components.AdBanner
import com.vasilis.investmentcalculator.ui.components.GrowthBarChart
import com.vasilis.investmentcalculator.ui.components.InvestmentBreakdownChart
import com.vasilis.investmentcalculator.ui.components.InvestmentInputFields
import com.vasilis.investmentcalculator.ui.components.ModeTabSelector
import com.vasilis.investmentcalculator.ui.components.ResultSummaryCard
import com.vasilis.investmentcalculator.ui.components.ScheduleTable

@Composable
fun InvestmentCalculatorScreen(
    showAds: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: InvestmentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val locale = LocalLocale.current.platformLocale

    LaunchedEffect(locale) {
        viewModel.updateLocale(locale)
    }

    LaunchedEffect(uiState.input.mode) {
        AnalyticsHelper.logScreenView(uiState.input.mode.title)
    }

    LaunchedEffect(uiState.hasCalculated) {
        if (uiState.hasCalculated) {
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
            val scrollTarget = scrollState.value + (screenHeightPx * 2 / 3)
            scrollState.animateScrollTo(scrollTarget)
        }
    }

    Scaffold(
        bottomBar = {
            if (showAds) {
                Column {
                    AdBanner()
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Tabs
            ModeTabSelector(
                selectedMode = uiState.input.mode,
                onModeSelected = { viewModel.updateMode(it) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description Banner
                Text(
                    text = uiState.input.mode.description,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight. Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Editable Input Parameters
                InvestmentInputFields(
                    input = uiState.input,
                    inputState = uiState.inputState,
                    onTargetEndAmountChange = { viewModel.updateTargetEndAmount(it) },
                    onStartingAmountChange = { viewModel.updateStartingAmount(it) },
                    onInvestLengthYearsChange = { viewModel.updateInvestLengthYears(it) },
                    onReturnRatePercentChange = { viewModel.updateReturnRatePercent(it) },
                    onAdditionalContributionChange = { viewModel.updateAdditionalContribution(it) },
                    onContributionFrequencyChange = { viewModel.updateContributionFrequency(it) },
                    onContributionTimingChange = { viewModel.updateContributionTiming(it) },
                    onCompoundFrequencyChange = { viewModel.updateCompoundFrequency(it) }
                )

                // Calculate & Reset Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.calculateResult() },
                        enabled = uiState.isFormValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Calculate", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetToDefaults() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Reset", fontWeight = FontWeight.Bold)
                    }
                }

                // Results, Charts, and Schedule Table (shown only after clicking Calculate)
                if (uiState.hasCalculated) {
                    // Results Summary Hero Card
                    ResultSummaryCard(result = uiState.result)

                    // Breakdown Donut Chart
                    InvestmentBreakdownChart(result = uiState.result)

                    // Asset Growth Stacked Bar Chart
                    GrowthBarChart(result = uiState.result)

                    // Annual Schedule Table
                    ScheduleTable(schedule = uiState.result.schedule)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InvestmentCalculatorScreenPreview() {
    InvestmentCalculatorScreen()
}
