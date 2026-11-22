package com.vasilis.investmentcalculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
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
    val investLengthMonthsStr: String = "0",
    val investLengthDaysStr: String = "0",
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

/** Uses the locale's decimal separator and accepts ungrouped numbers only. */
object InvestmentInputValidation {
    private val decimalNumber = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")

    fun durationYears(state: InvestmentInputState, locale: Locale = Locale.getDefault()): Double {
        val years = parse(state.investLengthStr, locale) ?: 0.0
        val months = parse(state.investLengthMonthsStr, locale) ?: 0.0
        val days = parse(state.investLengthDaysStr, locale) ?: 0.0
        // Use a common denominator so equivalent durations round to the same value.
        return durationInYears(years * 12.0 + months, days)
    }

    private fun durationFieldsError(state: InvestmentInputState, locale: Locale): String? {
        for ((value, max) in listOf(state.investLengthStr to InvestmentCalculator.MAX_YEARS,
            state.investLengthMonthsStr to InvestmentCalculator.MAX_YEARS * 12,
            state.investLengthDaysStr to InvestmentCalculator.MAX_YEARS * 365)) {
            error(value, max = max, locale = locale, wholeNumber = true)?.let { return it }
        }
        return null
    }

    fun durationError(state: InvestmentInputState, locale: Locale = Locale.getDefault()): String? =
        durationFieldsError(state, locale) ?: durationTotalError(state, locale)

    fun durationTotalError(state: InvestmentInputState, locale: Locale = Locale.getDefault()): String? {
        if (durationFieldsError(state, locale) != null) return null
        return if (durationYears(state, locale) > InvestmentCalculator.MAX_YEARS)
            "Total investment length cannot exceed ${java.math.BigDecimal.valueOf(InvestmentCalculator.MAX_YEARS).stripTrailingZeros().toPlainString()} years" else null
    }

    fun parse(text: String, locale: Locale = Locale.getDefault()): Double? {
        val symbols = DecimalFormatSymbols.getInstance(locale)
        val normalized = buildString {
            for (character in text.trim()) {
                append(when (character) {
                    symbols.decimalSeparator -> '.'
                    '-', '\u2212', symbols.minusSign -> '-'
                    '+' -> '+'
                    else -> character.digitToIntOrNull() ?: return null
                })
            }
        }
        if (!decimalNumber.matches(normalized)) return null
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    fun error(text: String, min: Double = 0.0,
              max: Double = InvestmentCalculator.MAX_AMOUNT,
              locale: Locale = Locale.getDefault(), wholeNumber: Boolean = false): String? {
        if (text.isBlank()) return "Field cannot be empty"
        val value = parse(text, locale)
        if (wholeNumber && (value == null || (value in min..max && text.any { it.digitToIntOrNull() == null })))
            return "Enter a whole number"
        if (value != null && value in min..max) return null
        val format = NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            maximumFractionDigits = 340
        }
        return if (value == null) "Enter a number without thousands separators (for example ${format.format(1000.5)})"
        else "Enter a value between ${format.format(min)} and ${format.format(max)}"
    }

    fun isValid(mode: CalculationMode, state: InvestmentInputState, locale: Locale = Locale.getDefault()): Boolean =
        (mode == CalculationMode.END_AMOUNT || error(state.targetEndAmountStr, locale = locale) == null) &&
                (mode == CalculationMode.STARTING_AMOUNT || error(state.startingAmountStr, locale = locale) == null) &&
                (mode == CalculationMode.INVEST_LENGTH || durationError(state, locale) == null) &&
                (mode == CalculationMode.RETURN_RATE || error(state.returnRateStr,
                    InvestmentCalculator.MIN_RETURN, InvestmentCalculator.MAX_RETURN, locale) == null) &&
                (mode == CalculationMode.CONTRIBUTE_AMOUNT || error(state.additionalContributionStr, locale = locale) == null)
}

class InvestmentViewModel : ViewModel() {
    private var currentLocale = Locale.getDefault()
    private var calculationJob: Job? = null
    private var revision = 0
    private val _uiState = MutableStateFlow(InvestmentUiState())
    val uiState: StateFlow<InvestmentUiState> = _uiState.asStateFlow()

    fun updateLocale(locale: Locale) {
        if (locale == currentLocale) return
        val previous = _uiState.value
        val updated = previous.withInputLocale(currentLocale, locale)
        currentLocale = locale
        if (previous.isCalculating && !updated.isCalculating) {
            calculationJob?.cancel()
            revision++
        }
        _uiState.value = updated
    }

    private fun edit(transform: (InvestmentUiState) -> InvestmentUiState) {
        calculationJob?.cancel()
        revision++
        _uiState.update { transform(it).parseInputs(currentLocale).clearCalculation() }
    }

    fun updateMode(mode: CalculationMode) {
        if (mode == _uiState.value.input.mode) return
        edit { it.copy(input = it.input.copy(mode = mode)) }
    }
    fun updateTargetEndAmount(text: String) = edit {
        it.copy(inputState = it.inputState.copy(targetEndAmountStr = text))
    }
    fun updateStartingAmount(text: String) = edit {
        it.copy(inputState = it.inputState.copy(startingAmountStr = text))
    }
    private fun updateDuration(transform: (InvestmentInputState) -> InvestmentInputState) = edit {
        it.copy(inputState = transform(it.inputState))
    }
    fun updateInvestLengthYears(text: String) = updateDuration { it.copy(investLengthStr = text) }
    fun updateInvestLengthMonths(text: String) = updateDuration { it.copy(investLengthMonthsStr = text) }
    fun updateInvestLengthDays(text: String) = updateDuration { it.copy(investLengthDaysStr = text) }
    fun updateReturnRatePercent(text: String) = edit {
        it.copy(inputState = it.inputState.copy(returnRateStr = text))
    }
    fun updateAdditionalContribution(text: String) = edit {
        it.copy(inputState = it.inputState.copy(additionalContributionStr = text))
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

internal fun InvestmentUiState.withInputLocale(previousLocale: Locale, locale: Locale): InvestmentUiState {
    val previousSeparator = DecimalFormatSymbols.getInstance(previousLocale).decimalSeparator
    val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    fun convert(text: String): String =
        if (InvestmentInputValidation.parse(text, previousLocale) != null) text.replace(previousSeparator, separator)
        else text

    val updated = copy(inputState = inputState.copy(
        targetEndAmountStr = convert(inputState.targetEndAmountStr),
        startingAmountStr = convert(inputState.startingAmountStr),
        returnRateStr = convert(inputState.returnRateStr),
        additionalContributionStr = convert(inputState.additionalContributionStr)
    )).parseInputs(locale)

    // The value solved by the active mode is ignored by the calculator.
    val previousInputs = when (input.mode) {
        CalculationMode.END_AMOUNT -> input.copy(targetEndAmount = updated.input.targetEndAmount)
        CalculationMode.STARTING_AMOUNT -> input.copy(startingAmount = updated.input.startingAmount)
        CalculationMode.RETURN_RATE -> input.copy(returnRatePercent = updated.input.returnRatePercent)
        CalculationMode.INVEST_LENGTH -> input.copy(investLengthYears = updated.input.investLengthYears)
        CalculationMode.CONTRIBUTE_AMOUNT -> input.copy(additionalContribution = updated.input.additionalContribution)
    }
    return if (updated.isFormValid && updated.input == previousInputs) updated else updated.clearCalculation()
}

private fun InvestmentUiState.parseInputs(locale: Locale): InvestmentUiState = copy(
    input = input.copy(
        targetEndAmount = InvestmentInputValidation.parse(inputState.targetEndAmountStr, locale) ?: 0.0,
        startingAmount = InvestmentInputValidation.parse(inputState.startingAmountStr, locale) ?: 0.0,
        investLengthYears = InvestmentInputValidation.durationYears(inputState, locale),
        returnRatePercent = InvestmentInputValidation.parse(inputState.returnRateStr, locale) ?: 0.0,
        additionalContribution = InvestmentInputValidation.parse(inputState.additionalContributionStr, locale) ?: 0.0
    ),
    isFormValid = InvestmentInputValidation.isValid(input.mode, inputState, locale)
)

private fun InvestmentUiState.clearCalculation(): InvestmentUiState =
    copy(hasCalculated = false, isCalculating = false, calculationError = null)
