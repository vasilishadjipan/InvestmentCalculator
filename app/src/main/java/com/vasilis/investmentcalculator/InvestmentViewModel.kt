package com.vasilis.investmentcalculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private fun Double.formatNoDecimal(): String =
    if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

data class InvestmentInputState(
    val targetEndAmountStr: String = 100000.0.formatNoDecimal(),
    val startingAmountStr: String = 10000.0.formatNoDecimal(),
    val investLengthStr: String = 10.0.formatNoDecimal(),
    val returnRateStr: String = 7.0.formatNoDecimal(),
    val additionalContributionStr: String = 200.0.formatNoDecimal()
)

data class InvestmentUiState(
    val input: InvestmentInput = InvestmentInput(),
    val inputState: InvestmentInputState = InvestmentInputState(),
    val result: InvestmentResult = InvestmentCalculator.calculate(InvestmentInput()),
    val isFormValid: Boolean = true,
    val hasCalculated: Boolean = false,
    val isCalculating: Boolean = false,
    val calculationError: String? = null,
    val calculationId: Int = 0
)

/** A comma is accepted as a decimal separator; grouping separators are not accepted. */
object InvestmentInputValidation {
    fun parse(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() }

    fun error(text: String, min: Double = 0.0,
              max: Double = InvestmentCalculator.MAX_AMOUNT): String? {
        if (text.isBlank()) return "Field cannot be empty"
        val value = parse(text) ?: return "Enter a valid number (for example 7.5 or 7,5)"
        return if (value !in min..max) "Enter a value between $min and $max" else null
    }

    fun isValid(mode: CalculationMode, state: InvestmentInputState): Boolean =
        (mode == CalculationMode.END_AMOUNT || error(state.targetEndAmountStr) == null) &&
                (mode == CalculationMode.STARTING_AMOUNT || error(state.startingAmountStr) == null) &&
                (mode == CalculationMode.INVEST_LENGTH || error(state.investLengthStr,
                    max = InvestmentCalculator.MAX_YEARS) == null) &&
                (mode == CalculationMode.RETURN_RATE || error(state.returnRateStr,
                    InvestmentCalculator.MIN_RETURN, InvestmentCalculator.MAX_RETURN) == null) &&
                (mode == CalculationMode.CONTRIBUTE_AMOUNT || error(state.additionalContributionStr) == null)
}

class InvestmentViewModel : ViewModel() {
    private var currentLocale = Locale.getDefault()
    private var calculationJob: Job? = null
    private var revision = 0
    private val _uiState = MutableStateFlow(InvestmentUiState())
    val uiState: StateFlow<InvestmentUiState> = _uiState.asStateFlow()

    fun updateLocale(locale: Locale) { currentLocale = locale }

    private fun edit(transform: (InvestmentUiState) -> InvestmentUiState) {
        calculationJob?.cancel()
        revision++
        _uiState.update { old ->
            val next = transform(old)
            next.copy(hasCalculated = false, isCalculating = false, calculationError = null,
                isFormValid = InvestmentInputValidation.isValid(next.input.mode, next.inputState))
        }
    }

    fun updateMode(mode: CalculationMode) {
        if (mode == _uiState.value.input.mode) return
        edit { it.copy(input = it.input.copy(mode = mode)) }
    }
    fun updateTargetEndAmount(text: String) = edit {
        it.copy(inputState = it.inputState.copy(targetEndAmountStr = text),
            input = it.input.copy(targetEndAmount = InvestmentInputValidation.parse(text) ?: 0.0))
    }
    fun updateStartingAmount(text: String) = edit {
        it.copy(inputState = it.inputState.copy(startingAmountStr = text),
            input = it.input.copy(startingAmount = InvestmentInputValidation.parse(text) ?: 0.0))
    }
    fun updateInvestLengthYears(text: String) = edit {
        it.copy(inputState = it.inputState.copy(investLengthStr = text),
            input = it.input.copy(investLengthYears = InvestmentInputValidation.parse(text) ?: 0.0))
    }
    fun updateReturnRatePercent(text: String) = edit {
        it.copy(inputState = it.inputState.copy(returnRateStr = text),
            input = it.input.copy(returnRatePercent = InvestmentInputValidation.parse(text) ?: 0.0))
    }
    fun updateAdditionalContribution(text: String) = edit {
        it.copy(inputState = it.inputState.copy(additionalContributionStr = text),
            input = it.input.copy(additionalContribution = InvestmentInputValidation.parse(text) ?: 0.0))
    }
    fun updateContributionFrequency(value: ContributionFrequency) = edit {
        it.copy(input = it.input.copy(contributionFrequency = value))
    }
    fun updateContributionTiming(value: ContributionTiming) = edit {
        it.copy(input = it.input.copy(contributionTiming = value))
    }
    fun updateCompoundFrequency(value: CompoundFrequency) = edit {
        it.copy(input = it.input.copy(compoundFrequency = value))
    }

    fun calculateResult() {
        val state = _uiState.value
        if (!state.isFormValid || state.isCalculating) return
        val requestedRevision = revision
        val locale = currentLocale
        AnalyticsHelper.logCalculateClicked(state.input.mode.title)
        _uiState.update { it.copy(isCalculating = true, calculationError = null, hasCalculated = false) }
        calculationJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    InvestmentCalculator.calculate(state.input, locale)
                }
                if (requestedRevision == revision) _uiState.update {
                    it.copy(result = result, hasCalculated = true, isCalculating = false,
                        calculationId = it.calculationId + 1)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                if (requestedRevision == revision) _uiState.update {
                    it.copy(isCalculating = false, calculationError = error.message ?: "Unable to calculate.")
                }
            }
        }
    }

    fun resetToDefaults() {
        AnalyticsHelper.logResetClicked()
        edit { InvestmentUiState(input = InvestmentInput(mode = it.input.mode),
            calculationId = it.calculationId) }
    }
}