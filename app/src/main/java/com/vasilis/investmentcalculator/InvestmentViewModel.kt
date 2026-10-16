package com.vasilis.investmentcalculator

import androidx.lifecycle.ViewModel
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
    val hasCalculated: Boolean = false
)

class InvestmentViewModel : ViewModel() {

    private var currentLocale: Locale = Locale.getDefault()

    private val _uiState = MutableStateFlow(InvestmentUiState())
    val uiState: StateFlow<InvestmentUiState> = _uiState.asStateFlow()

    fun updateLocale(locale: Locale) {
        if (currentLocale != locale) {
            currentLocale = locale
            _uiState.update { currentState ->
                currentState.copy(
                    result = InvestmentCalculator.calculate(currentState.input, locale)
                )
            }
        }
    }

    fun updateMode(mode: CalculationMode) {
        _uiState.update { currentState ->
            val newInput = currentState.input.copy(mode = mode)
            currentState.copy(
                input = newInput,
                isFormValid = validateForm(mode, currentState.inputState),
                hasCalculated = false
            )
        }
    }

    fun updateTargetEndAmount(text: String) {
        _uiState.update { currentState ->
            val newTextState = currentState.inputState.copy(targetEndAmountStr = text)
            val parsed = text.toDoubleOrNull()
            val newInput = if (parsed != null) currentState.input.copy(targetEndAmount = parsed) else currentState.input
            currentState.copy(
                inputState = newTextState,
                input = newInput,
                isFormValid = validateForm(currentState.input.mode, newTextState),
                hasCalculated = false
            )
        }
    }

    fun updateStartingAmount(text: String) {
        _uiState.update { currentState ->
            val newTextState = currentState.inputState.copy(startingAmountStr = text)
            val parsed = text.toDoubleOrNull()
            val newInput = if (parsed != null) currentState.input.copy(startingAmount = parsed) else currentState.input
            currentState.copy(
                inputState = newTextState,
                input = newInput,
                isFormValid = validateForm(currentState.input.mode, newTextState),
                hasCalculated = false
            )
        }
    }

    fun updateInvestLengthYears(text: String) {
        _uiState.update { currentState ->
            val newTextState = currentState.inputState.copy(investLengthStr = text)
            val parsed = text.toDoubleOrNull()
            val newInput = if (parsed != null) currentState.input.copy(investLengthYears = parsed) else currentState.input
            currentState.copy(
                inputState = newTextState,
                input = newInput,
                isFormValid = validateForm(currentState.input.mode, newTextState),
                hasCalculated = false
            )
        }
    }

    fun updateReturnRatePercent(text: String) {
        _uiState.update { currentState ->
            val newTextState = currentState.inputState.copy(returnRateStr = text)
            val parsed = text.toDoubleOrNull()
            val newInput = if (parsed != null) currentState.input.copy(returnRatePercent = parsed) else currentState.input
            currentState.copy(
                inputState = newTextState,
                input = newInput,
                isFormValid = validateForm(currentState.input.mode, newTextState),
                hasCalculated = false
            )
        }
    }

    fun updateAdditionalContribution(text: String) {
        _uiState.update { currentState ->
            val newTextState = currentState.inputState.copy(additionalContributionStr = text)
            val parsed = text.toDoubleOrNull()
            val newInput = if (parsed != null) currentState.input.copy(additionalContribution = parsed) else currentState.input
            currentState.copy(
                inputState = newTextState,
                input = newInput,
                isFormValid = validateForm(currentState.input.mode, newTextState),
                hasCalculated = false
            )
        }
    }

    fun updateContributionFrequency(frequency: ContributionFrequency) {
        _uiState.update { currentState ->
            val newInput = currentState.input.copy(contributionFrequency = frequency)
            currentState.copy(input = newInput, hasCalculated = false)
        }
    }

    fun updateContributionTiming(timing: ContributionTiming) {
        _uiState.update { currentState ->
            val newInput = currentState.input.copy(contributionTiming = timing)
            currentState.copy(input = newInput, hasCalculated = false)
        }
    }

    fun updateCompoundFrequency(frequency: CompoundFrequency) {
        _uiState.update { currentState ->
            val newInput = currentState.input.copy(compoundFrequency = frequency)
            currentState.copy(input = newInput, hasCalculated = false)
        }
    }

    fun calculateResult() {
        _uiState.update { currentState ->
            if (currentState.isFormValid) {
                AnalyticsHelper.logCalculateClicked(currentState.input.mode.title)
                currentState.copy(
                    result = InvestmentCalculator.calculate(currentState.input, currentLocale),
                    hasCalculated = true
                )
            } else {
                currentState
            }
        }
    }

    fun resetToDefaults() {
        AnalyticsHelper.logResetClicked()
        val defaultInput = InvestmentInput(mode = _uiState.value.input.mode)
        val defaultState = InvestmentInputState(
            targetEndAmountStr = defaultInput.targetEndAmount.formatNoDecimal(),
            startingAmountStr = defaultInput.startingAmount.formatNoDecimal(),
            investLengthStr = defaultInput.investLengthYears.formatNoDecimal(),
            returnRateStr = defaultInput.returnRatePercent.formatNoDecimal(),
            additionalContributionStr = defaultInput.additionalContribution.formatNoDecimal()
        )
        val defaultResult = InvestmentCalculator.calculate(defaultInput, currentLocale)
        _uiState.value = InvestmentUiState(
            input = defaultInput,
            inputState = defaultState,
            result = defaultResult,
            isFormValid = true,
            hasCalculated = false
        )
    }

    private fun validateForm(mode: CalculationMode, state: InvestmentInputState): Boolean {
        val targetValid = mode == CalculationMode.END_AMOUNT || state.targetEndAmountStr.toDoubleOrNull() != null
        val startingValid = mode == CalculationMode.STARTING_AMOUNT || state.startingAmountStr.toDoubleOrNull() != null
        val lengthValid = mode == CalculationMode.INVEST_LENGTH || state.investLengthStr.toDoubleOrNull() != null
        val rateValid = mode == CalculationMode.RETURN_RATE || state.returnRateStr.toDoubleOrNull() != null
        val contribValid = mode == CalculationMode.CONTRIBUTE_AMOUNT || state.additionalContributionStr.toDoubleOrNull() != null
        return targetValid && startingValid && lengthValid && rateValid && contribValid
    }
}
