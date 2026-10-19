package com.vasilis.investmentcalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val resultsRequester = remember { BringIntoViewRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val locale = LocalLocale.current.platformLocale

    LaunchedEffect(locale) {
        viewModel.updateLocale(locale)
    }

    LaunchedEffect(uiState.input.mode) {
        AnalyticsHelper.logScreenView(uiState.input.mode.title)
    }

    LaunchedEffect(uiState.calculationId, uiState.hasCalculated) {
        if (uiState.hasCalculated) {
            withFrameNanos { }
            resultsRequester.bringIntoView()
        }
    }
    LaunchedEffect(uiState.input.mode) { scrollState.scrollTo(0) }

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
                .imePadding()
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
                        onClick = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            viewModel.calculateResult()
                        },
                        enabled = uiState.isFormValid && !uiState.isCalculating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (uiState.isCalculating) "Calculating…" else "Calculate", fontWeight = FontWeight.Bold)
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

                uiState.calculationError?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }

                // Results, Charts, and Schedule Table (shown only after clicking Calculate)
                if (uiState.hasCalculated) {
                    // Results Summary Hero Card
                    Box(Modifier.bringIntoViewRequester(resultsRequester)) {
                        ResultSummaryCard(result = uiState.result)
                    }

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