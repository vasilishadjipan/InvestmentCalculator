package com.vasilis.investmentcalculator

import com.vasilis.investmentcalculator.ui.components.formatInvestmentDuration
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class InvestmentDurationTest {
    @Test fun requiredDurationDisplaysYearsMonthsAndDays() {
        val result = InvestmentCalculator.calculate(InvestmentInput(
            mode = CalculationMode.INVEST_LENGTH, targetEndAmount = 200.0, startingAmount = 0.0,
            returnRatePercent = 0.0, additionalContribution = 200.0))
        assertEquals("0y 1m 0d", formatInvestmentDuration(result.solvedValue, Locale.US))
        assertEquals("1y 2m 3d", formatInvestmentDuration(durationInYears(14.0, 3.0), Locale.US))
        assertEquals("0y 0m 1d", formatInvestmentDuration(0.5 / 365.0, Locale.US))
        assertEquals("1y 0m 0d", formatInvestmentDuration(Math.nextDown(1.0), Locale.US))
    }

    @Test fun rowErrorOnlyShowsCombinedLimitForValidFields() {
        for (value in listOf("", "101", "-1", "1.5", "abc")) {
            val state = InvestmentInputState(investLengthStr = value)
            assertNotNull(InvestmentInputValidation.durationError(state))
            assertNull(InvestmentInputValidation.durationTotalError(state))
        }
        val overLimit = InvestmentInputState(investLengthStr = "100", investLengthMonthsStr = "1")
        assertEquals("Total investment length cannot exceed 100 years",
            InvestmentInputValidation.durationTotalError(overLimit))
        assertFalse(InvestmentInputValidation.isValid(CalculationMode.END_AMOUNT, overLimit))
        assertNull(InvestmentInputValidation.durationTotalError(InvestmentInputState()))
    }

    @Test fun rangeErrorsRemoveTrailingZerosWithoutRounding() {
        assertEquals("Enter a value between 0 and 100",
            InvestmentInputValidation.error("101", max = 100.0))
        assertEquals("Enter a value between -2.5 and 7.5",
            InvestmentInputValidation.error("8", min = -2.5, max = 7.50))
        assertEquals("Enter a value between 0 and 1000000000000",
            InvestmentInputValidation.error("1000000000001"))
    }

    @Test fun durationErrorsUseIntegerLimits() {
        assertEquals("Enter a value between 0 and 100",
            InvestmentInputValidation.durationError(InvestmentInputState(investLengthStr = "101")))
        assertEquals("Enter a value between 0 and 1200",
            InvestmentInputValidation.error("1201", max = 1200.0))
        assertEquals("Enter a value between 0 and 36500",
            InvestmentInputValidation.error("36501", max = 36500.0))
    }

    @Test fun sharedValidationPreservesDecimalInputs() {
        assertNull(InvestmentInputValidation.error("7.5"))
        assertNull(InvestmentInputValidation.error("7,5"))
        assertNull(InvestmentInputValidation.error("-2.5", min = -90.0))
        assertEquals("Enter a whole number",
            InvestmentInputValidation.durationError(InvestmentInputState(investLengthStr = "7.5")))
        assertEquals("Field cannot be empty",
            InvestmentInputValidation.error(""))
    }

    @Test fun combinesYearsMonthsAndDays() {
        val state = InvestmentInputState(investLengthStr = "2",
            investLengthMonthsStr = "6", investLengthDaysStr = "73")
        assertEquals(2.7, InvestmentInputValidation.durationYears(state), 1e-12)
        assertNull(InvestmentInputValidation.durationError(state))
    }

    @Test fun enforcesCombinedLimitAndValidComponents() {
        val limit = InvestmentInputState(investLengthStr = "99", investLengthMonthsStr = "12")
        assertNull(InvestmentInputValidation.durationError(limit))
        assertNotNull(InvestmentInputValidation.durationError(limit.copy(investLengthDaysStr = "1")))
        for (value in listOf("", "-1", "NaN", "Infinity", "abc", "1.5", "1,5", "1e1")) {
            assertNotNull(InvestmentInputValidation.durationError(limit.copy(investLengthStr = value)))
            assertNotNull(InvestmentInputValidation.durationError(limit.copy(investLengthMonthsStr = value)))
            assertNotNull(InvestmentInputValidation.durationError(limit.copy(investLengthDaysStr = value)))
        }
    }

    @Test fun durationInputsPreserveMonthlyContributionBoundaries() {
        val monthAndDayInputs = InvestmentInputState(investLengthStr = "0",
            investLengthMonthsStr = "13", investLengthDaysStr = "365")
        val cases = listOf(
            InvestmentInputState(investLengthStr = "0", investLengthMonthsStr = "6") to 6,
            InvestmentInputState(investLengthStr = "1", investLengthMonthsStr = "13") to 25,
            monthAndDayInputs to 25,
            monthAndDayInputs.copy(investLengthDaysStr = "364") to 24
        )
        for ((state, expectedPayments) in cases) {
            val result = InvestmentCalculator.calculate(InvestmentInput(startingAmount = 0.0,
                returnRatePercent = 0.0, additionalContribution = 200.0,
                investLengthYears = InvestmentInputValidation.durationYears(state)))
            assertEquals(expectedPayments * 200.0, result.endBalance, 1e-9)
        }
    }
}
