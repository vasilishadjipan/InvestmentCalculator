package com.vasilis.investmentcalculator

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import kotlin.math.exp
import kotlin.math.pow

/** Tests calculation behavior independently of Android and Compose. */
class InvestmentCalculatorTest {
    private val tolerance = 0.000001
    private val base = InvestmentInput(
        startingAmount = 0.0,
        investLengthYears = 1.0,
        returnRatePercent = 0.0,
        additionalContribution = 200.0,
        contributionFrequency = ContributionFrequency.MONTHLY,
        contributionTiming = ContributionTiming.END,
        compoundFrequency = CompoundFrequency.ANNUALLY
    )

    private fun calculate(input: InvestmentInput) =
        InvestmentCalculator.calculate(input, Locale.US)

    @Test fun monthlyContributions_supportWholeAndFractionalYears() {
        for ((years, expected) in listOf(0.5 to 1200.0, 1.0 to 2400.0,
            1.5 to 3600.0, 0.6 to 1400.0)) {
            val result = calculate(base.copy(investLengthYears = years))
            assertEquals("Duration: $years", expected, result.endBalance, tolerance)
            assertEquals(expected, result.totalContributions, tolerance)
            assertEquals(0.0, result.totalInterest, tolerance)
        }
    }

    @Test fun partialYearInterest_doesNotDependOnContributionFrequencyWithoutDeposits() {
        for (frequency in ContributionFrequency.entries) {
            val result = calculate(base.copy(startingAmount = 1000.0,
                additionalContribution = 0.0, returnRatePercent = 10.0,
                investLengthYears = 0.5, contributionFrequency = frequency))
            assertEquals(1000.0 * 1.1.pow(0.5), result.endBalance, tolerance)
        }
    }

    @Test fun annualEndContribution_waitsUntilYearEnds() {
        val result = calculate(base.copy(investLengthYears = 0.5,
            contributionFrequency = ContributionFrequency.ANNUALLY))
        assertEquals(0.0, result.endBalance, tolerance)
        assertEquals(0.0, result.totalContributions, tolerance)
    }

    @Test fun annualBeginningContribution_isDepositedImmediately() {
        val result = calculate(base.copy(investLengthYears = 0.5,
            contributionFrequency = ContributionFrequency.ANNUALLY,
            contributionTiming = ContributionTiming.BEGINNING,
            returnRatePercent = 10.0))
        assertEquals(200.0, result.totalContributions, tolerance)
        assertEquals(200.0 * 1.1.pow(0.5), result.endBalance, tolerance)
    }

    @Test fun partialMonth_beginningAndEndHaveDifferentPaymentCounts() {
        val input = base.copy(investLengthYears = 0.6)
        assertEquals(1400.0, calculate(input).totalContributions, tolerance)
        assertEquals(1600.0, calculate(input.copy(
            contributionTiming = ContributionTiming.BEGINNING)).totalContributions, tolerance)
    }

    @Test fun exactPeriodBoundary_doesNotAddAnotherBeginningPayment() {
        val result = calculate(base.copy(investLengthYears = 0.5,
            contributionTiming = ContributionTiming.BEGINNING))
        assertEquals(1200.0, result.totalContributions, tolerance)
    }

    @Test fun zeroDuration_hasNoInterestPaymentsOrSchedule() {
        for (timing in ContributionTiming.entries) {
            val result = calculate(base.copy(startingAmount = 1000.0,
                investLengthYears = 0.0, returnRatePercent = 10.0,
                contributionTiming = timing))
            assertEquals(1000.0, result.endBalance, tolerance)
            assertEquals(0.0, result.totalContributions, tolerance)
            assertEquals(0.0, result.totalInterest, tolerance)
            assertTrue(result.schedule.isEmpty())
        }
    }

    @Test fun allCompoundingFrequencies_matchLumpSumFormula() {
        for (frequency in CompoundFrequency.entries) {
            val years = 2.25
            val expected = if (frequency == CompoundFrequency.CONTINUOUSLY) {
                1000.0 * exp(0.12 * years)
            } else {
                val n = frequency.periodsPerYear.toDouble()
                1000.0 * (1.0 + 0.12 / n).pow(n * years)
            }
            val result = calculate(base.copy(startingAmount = 1000.0,
                additionalContribution = 0.0, returnRatePercent = 12.0,
                investLengthYears = years, compoundFrequency = frequency))
            assertEquals(frequency.name, expected, result.endBalance, tolerance)
        }
    }

    @Test fun monthlyPayments_matchOrdinaryAnnuityFormula() {
        val rate = 0.12 / 12.0
        val expected = 1000.0 * (1.0 + rate).pow(12) +
                200.0 * ((1.0 + rate).pow(12) - 1.0) / rate
        val result = calculate(base.copy(startingAmount = 1000.0,
            returnRatePercent = 12.0, compoundFrequency = CompoundFrequency.MONTHLY))
        assertEquals(expected, result.endBalance, tolerance)
    }

    @Test fun beginningPayments_matchAnnuityDueFormula() {
        val rate = 0.12 / 12.0
        val expected = 200.0 * ((1.0 + rate).pow(12) - 1.0) / rate * (1.0 + rate)
        val result = calculate(base.copy(returnRatePercent = 12.0,
            compoundFrequency = CompoundFrequency.MONTHLY,
            contributionTiming = ContributionTiming.BEGINNING))
        assertEquals(expected, result.endBalance, tolerance)
    }

    @Test fun negativeReturn_reducesBalanceAndProducesNegativeInterest() {
        val result = calculate(base.copy(startingAmount = 1000.0,
            additionalContribution = 0.0, returnRatePercent = -10.0))
        assertEquals(900.0, result.endBalance, tolerance)
        assertEquals(-100.0, result.totalInterest, tolerance)
    }

    @Test fun schedule_containsPartialFinalYear() {
        val result = calculate(base.copy(investLengthYears = 1.5))
        assertEquals(listOf(1, 2), result.schedule.map { it.year })
        assertEquals(2400.0, result.schedule[0].annualContribution, tolerance)
        assertEquals(1200.0, result.schedule[1].annualContribution, tolerance)
        assertEquals(3600.0, result.schedule[1].endingBalance, tolerance)
    }

    @Test fun schedule_balancesAndCumulativeTotalsReconcile() {
        val result = calculate(base.copy(startingAmount = 1000.0,
            investLengthYears = 2.3, returnRatePercent = 7.0))
        var previousBalance = result.startingAmount
        var contributions = 0.0
        var interest = 0.0
        for (row in result.schedule) {
            assertEquals(previousBalance, row.startingBalance, tolerance)
            assertEquals(row.startingBalance + row.annualContribution + row.annualInterest,
                row.endingBalance, tolerance)
            contributions += row.annualContribution
            interest += row.annualInterest
            assertEquals(contributions, row.totalContributionsToDate, tolerance)
            assertEquals(interest, row.totalInterestToDate, tolerance)
            previousBalance = row.endingBalance
        }
        assertEquals(result.endBalance, previousBalance, tolerance)
        assertEquals(result.totalContributions, contributions, tolerance)
        assertEquals(result.totalInterest, interest, tolerance)
        assertEquals(result.startingAmount + contributions + interest, result.endBalance, tolerance)
    }

    @Test fun exactWholeYears_doNotCreateEmptyExtraScheduleRow() {
        assertEquals(listOf(1, 2), calculate(base.copy(investLengthYears = 2.0))
            .schedule.map { it.year })
    }

    @Test fun startingAmountSolver_findsRequiredPrincipal() {
        val result = calculate(base.copy(mode = CalculationMode.STARTING_AMOUNT,
            targetEndAmount = 1100.0, additionalContribution = 0.0,
            returnRatePercent = 10.0))
        assertEquals(1000.0, result.solvedValue, tolerance)
        assertEquals(1100.0, result.endBalance, tolerance)
    }

    @Test fun contributionSolver_supportsFractionalYears() {
        val result = calculate(base.copy(mode = CalculationMode.CONTRIBUTE_AMOUNT,
            targetEndAmount = 1200.0, investLengthYears = 0.5))
        assertEquals(200.0, result.solvedValue, tolerance)
        assertEquals(1200.0, result.endBalance, tolerance)
    }

    @Test fun returnRateSolver_findsKnownAnnualRate() {
        val result = calculate(base.copy(mode = CalculationMode.RETURN_RATE,
            startingAmount = 1000.0, additionalContribution = 0.0,
            targetEndAmount = 1100.0))
        assertEquals(10.0, result.solvedValue, tolerance)
        assertEquals(1100.0, result.endBalance, tolerance)
    }

    @Test fun durationSolver_findsKnownLumpSumDuration() {
        val result = calculate(base.copy(mode = CalculationMode.INVEST_LENGTH,
            startingAmount = 1000.0, additionalContribution = 0.0,
            returnRatePercent = 10.0, targetEndAmount = 1210.0))
        assertEquals(2.0, result.solvedValue, tolerance)
        assertEquals(1210.0, result.endBalance, tolerance)
    }
}

/** Exercises require() through the public calculate() API. */
class InvestmentCalculatorValidationTest {
    private fun assertRejected(input: InvestmentInput) {
        assertThrows(IllegalArgumentException::class.java) {
            InvestmentCalculator.calculate(input, Locale.US)
        }
    }

    @Test fun rejectsNegativeDuration() {
        assertRejected(InvestmentInput(investLengthYears = -0.5))
    }

    @Test fun rejectsNonFiniteDuration() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertRejected(InvestmentInput(investLengthYears = value))
        }
    }

    @Test fun rejectsDurationBeyondPeriodLimit() {
        assertRejected(InvestmentInput(investLengthYears = 10001.0,
            contributionFrequency = ContributionFrequency.MONTHLY))
    }

    @Test fun rejectsNonFiniteReturnRate() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertRejected(InvestmentInput(returnRatePercent = value))
        }
    }

    @Test fun rejectsUnsupportedAnnualReturnRates() {
        for (value in listOf(-100.0, -110.0)) {
            assertRejected(InvestmentInput(returnRatePercent = value,
                contributionFrequency = ContributionFrequency.MONTHLY,
                compoundFrequency = CompoundFrequency.ANNUALLY))
        }
    }
}

/**
 * Regression tests for unreachable targets. Invalid or unsolvable inputs use
 * IllegalArgumentException, which the ViewModel presents as a user-facing error.
 */
class InvestmentCalculatorSolverRegressionTest {
    @Test fun durationSolver_waitsForSixthEndPayment() {
        val result = InvestmentCalculator.calculate(InvestmentInput(
            mode = CalculationMode.INVEST_LENGTH, startingAmount = 0.0,
            additionalContribution = 200.0, returnRatePercent = 0.0,
            targetEndAmount = 1200.0, contributionTiming = ContributionTiming.END))
        assertEquals(0.5, result.solvedValue, 1e-9)
        assertTrue(result.endBalance >= 1200.0)
    }

    @Test fun durationSolver_handlesBeginningPaymentBoundary() {
        val result = InvestmentCalculator.calculate(InvestmentInput(
            mode = CalculationMode.INVEST_LENGTH, startingAmount = 0.0,
            additionalContribution = 200.0, returnRatePercent = 0.0,
            targetEndAmount = 1200.0, contributionTiming = ContributionTiming.BEGINNING))
        assertEquals(5.0 / 12.0, result.solvedValue, 1e-9)
        assertTrue(result.endBalance >= 1200.0)
    }

    @Test fun durationSolver_returnsZeroWhenTargetAlreadyMet() {
        val result = InvestmentCalculator.calculate(InvestmentInput(
            mode = CalculationMode.INVEST_LENGTH, startingAmount = 1200.0,
            targetEndAmount = 1000.0))
        assertEquals(0.0, result.solvedValue, 0.0)
    }

    @Test fun durationSolver_supportsNegativeReturnWithGrowingDeposits() {
        val input = InvestmentInput(mode = CalculationMode.INVEST_LENGTH,
            startingAmount = 0.0, targetEndAmount = 300.0,
            additionalContribution = 200.0, returnRatePercent = -10.0)
        val result = InvestmentCalculator.calculate(input)
        assertEquals(2.0 / 12.0, result.solvedValue, 1e-9)
        assertTrue(result.endBalance >= input.targetEndAmount)
    }

    @Test fun durationSolver_rejectsUnreachableTarget() {
        assertThrows(IllegalArgumentException::class.java) {
            InvestmentCalculator.calculate(InvestmentInput(
                mode = CalculationMode.INVEST_LENGTH,
                startingAmount = 0.0, additionalContribution = 0.0,
                returnRatePercent = 0.0, targetEndAmount = 1000.0))
        }
    }

    @Test fun returnRateSolver_rejectsUnreachableTarget() {
        assertThrows(IllegalArgumentException::class.java) {
            InvestmentCalculator.calculate(InvestmentInput(
                mode = CalculationMode.RETURN_RATE,
                startingAmount = 0.0, additionalContribution = 0.0,
                targetEndAmount = 1000.0))
        }
    }

    @Test fun contributionSolver_rejectsPositiveTargetAtZeroDuration() {
        assertThrows(IllegalArgumentException::class.java) {
            InvestmentCalculator.calculate(InvestmentInput(
                mode = CalculationMode.CONTRIBUTE_AMOUNT,
                startingAmount = 0.0, investLengthYears = 0.0,
                targetEndAmount = 1000.0))
        }
    }
}