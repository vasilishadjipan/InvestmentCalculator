package com.vasilis.investmentcalculator

import java.util.Locale
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.ln
import kotlin.math.floor
import kotlin.math.abs

enum class CalculationMode(val title: String, val description: String) {
    END_AMOUNT("End Amount", "Calculate final balance"),
    STARTING_AMOUNT("Starting Amount", "Calculate initial deposit"),
    RETURN_RATE("Return Rate", "Calculate required return rate"),
    INVEST_LENGTH("Invest Length", "Calculate investment duration"),
    CONTRIBUTE_AMOUNT("Contribute Amount", "Calculate periodic deposit")
}

enum class ContributionTiming(val title: String) {
    BEGINNING("Beginning of period"),
    END("End of period")
}

enum class ContributionFrequency(val title: String, val periodsPerYear: Int) {
    MONTHLY("Monthly", 12),
    ANNUALLY("Annually", 1)
}

enum class CompoundFrequency(val title: String, val periodsPerYear: Int) {
    ANNUALLY("Annually", 1),
    SEMIANNUALLY("Semiannually", 2),
    QUARTERLY("Quarterly", 4),
    MONTHLY("Monthly", 12),
    DAILY("Daily", 365),
    CONTINUOUSLY("Continuously", -1)
}

data class InvestmentInput(
    val mode: CalculationMode = CalculationMode.END_AMOUNT,
    val targetEndAmount: Double = 100000.0,
    val startingAmount: Double = 10000.0,
    val investLengthYears: Double = 10.0,
    val returnRatePercent: Double = 7.0,
    val additionalContribution: Double = 200.0,
    val contributionFrequency: ContributionFrequency = ContributionFrequency.MONTHLY,
    val contributionTiming: ContributionTiming = ContributionTiming.END,
    val compoundFrequency: CompoundFrequency = CompoundFrequency.ANNUALLY
)

data class AnnualScheduleItem(
    val year: Int,
    val startingBalance: Double,
    val annualContribution: Double,
    val annualInterest: Double,
    val endingBalance: Double,
    val totalContributionsToDate: Double,
    val totalInterestToDate: Double
)

data class InvestmentResult(
    val mode: CalculationMode,
    val solvedValue: Double,
    val solvedLabel: String,
    val solvedUnit: String,
    val endBalance: Double,
    val startingAmount: Double,
    val totalContributions: Double,
    val totalInterest: Double,
    val investLengthYears: Double,
    val returnRatePercent: Double,
    val additionalContribution: Double,
    val schedule: List<AnnualScheduleItem>,
    val contributionFrequency: ContributionFrequency = ContributionFrequency.MONTHLY
)

object InvestmentCalculator {
    const val MAX_YEARS = 100.0
    const val MAX_AMOUNT = 1_000_000_000_000.0
    const val MIN_RETURN = -90.0
    const val MAX_RETURN = 500.0

    fun getCurrencySymbol(locale: Locale = Locale.getDefault()): String =
        try { java.util.Currency.getInstance(locale).symbol } catch (_: Exception) { "$" }

    fun calculate(input: InvestmentInput, locale: Locale = Locale.getDefault()): InvestmentResult {
        validate(input)
        var resolved = input
        val solved: Double
        val label: String
        val unit: String
        when (input.mode) {
            CalculationMode.END_AMOUNT -> {
                solved = simulate(input).endBalance
                label = "End Balance"
                unit = getCurrencySymbol(locale)
            }
            CalculationMode.STARTING_AMOUNT -> {
                // Compute the principal factor independently to avoid subtractive cancellation.
                val deposits = simulate(input.copy(startingAmount = 0.0)).endBalance
                val factor = simulate(input.copy(startingAmount = 1.0,
                    additionalContribution = 0.0)).endBalance
                require(factor > 0.0) { "Cannot solve the starting amount for these inputs." }
                solved = ((input.targetEndAmount - deposits) / factor).coerceAtLeast(0.0)
                checkSolvedAmount(solved)
                resolved = input.copy(startingAmount = solved)
                label = "Starting Amount Required"
                unit = getCurrencySymbol(locale)
            }
            CalculationMode.CONTRIBUTE_AMOUNT -> {
                val principal = simulate(input.copy(additionalContribution = 0.0)).endBalance
                val factor = simulate(input.copy(startingAmount = 0.0,
                    additionalContribution = 1.0)).endBalance
                solved = if (principal >= input.targetEndAmount) 0.0 else {
                    require(factor > 0.0) {
                        "No contribution occurs within this duration. Increase the duration or change contribution timing."
                    }
                    (input.targetEndAmount - principal) / factor
                }
                checkSolvedAmount(solved)
                resolved = input.copy(additionalContribution = solved)
                label = if (input.contributionFrequency == ContributionFrequency.MONTHLY)
                    "Required Monthly Contribution" else "Required Annual Contribution"
                unit = getCurrencySymbol(locale)
            }
            CalculationMode.RETURN_RATE -> {
                solved = solveRate(input)
                resolved = input.copy(returnRatePercent = solved)
                label = "Return Rate Required"
                unit = "%"
            }
            CalculationMode.INVEST_LENGTH -> {
                solved = solveDuration(input)
                resolved = input.copy(investLengthYears = solved)
                label = "Investment Duration Required"
                unit = "Years"
            }
        }
        val sim = simulate(resolved)
        require(sim.endBalance <= 1e18 && sim.totalContributions <= 1e18 &&
                abs(sim.totalInterest) <= 1e18) {
            "The result is too large to display. Reduce the rate, duration or amounts."
        }
        return InvestmentResult(input.mode, solved, label, unit, sim.endBalance,
            resolved.startingAmount, sim.totalContributions, sim.totalInterest,
            resolved.investLengthYears, resolved.returnRatePercent,
            resolved.additionalContribution, sim.schedule, input.contributionFrequency)
    }

    private fun checkSolvedAmount(value: Double) {
        require(value.isFinite() && value in 0.0..MAX_AMOUNT) {
            "The required amount exceeds the supported limit. Adjust the target or duration."
        }
    }

    private fun validate(input: InvestmentInput) {
        if (input.mode != CalculationMode.END_AMOUNT)
            require(input.targetEndAmount.isFinite() && input.targetEndAmount in 0.0..MAX_AMOUNT) {
                "Target amount must be between 0 and $MAX_AMOUNT."
            }
        if (input.mode != CalculationMode.STARTING_AMOUNT) checkSolvedAmount(input.startingAmount)
        if (input.mode != CalculationMode.CONTRIBUTE_AMOUNT) checkSolvedAmount(input.additionalContribution)
        if (input.mode != CalculationMode.INVEST_LENGTH)
            require(input.investLengthYears.isFinite() && input.investLengthYears in 0.0..MAX_YEARS) {
                "Duration must be between 0 and $MAX_YEARS years."
            }
        if (input.mode != CalculationMode.RETURN_RATE)
            require(input.returnRatePercent.isFinite() && input.returnRatePercent in MIN_RETURN..MAX_RETURN) {
                "Annual return must be between $MIN_RETURN% and $MAX_RETURN%."
            }
    }

    private fun periodicRate(input: InvestmentInput): Double {
        val r = input.returnRatePercent / 100.0
        val nc = input.contributionFrequency.periodsPerYear.toDouble()
        return if (input.compoundFrequency == CompoundFrequency.CONTINUOUSLY) {
            exp(r / nc) - 1.0
        } else {
            val nk = input.compoundFrequency.periodsPerYear.toDouble()
            (1.0 + r / nk).pow(nk / nc) - 1.0
        }
    }

    private data class SimulationResult(
        val endBalance: Double,
        val totalContributions: Double,
        val totalInterest: Double,
        val schedule: List<AnnualScheduleItem>
    )

    private fun simulate(input: InvestmentInput): SimulationResult {
        val rate = periodicRate(input)
        val nc = input.contributionFrequency.periodsPerYear
        // No rounding up: an end-of-period payment must never arrive early.
        val periods = input.investLengthYears * nc
        val whole = floor(periods).toInt()
        val remainder = periods - whole
        var balance = input.startingAmount
        var contributions = 0.0
        var interest = 0.0
        var year = 1
        var start = balance
        var yearContributions = 0.0
        var yearInterest = 0.0
        var pending = false
        val schedule = mutableListOf<AnnualScheduleItem>()

        fun deposit() {
            balance += input.additionalContribution
            contributions += input.additionalContribution
            yearContributions += input.additionalContribution
        }
        fun accrue(fraction: Double) {
            val amount = balance * (if (fraction == 1.0) rate else (1.0 + rate).pow(fraction) - 1.0)
            balance += amount
            interest += amount
            yearInterest += amount
            require(balance.isFinite() && interest.isFinite()) {
                "The result is too large. Reduce the rate, duration or amounts."
            }
        }
        fun recordYear() {
            schedule.add(AnnualScheduleItem(year, start, yearContributions, yearInterest,
                balance, contributions, interest))
            year++
            start = balance
            yearContributions = 0.0
            yearInterest = 0.0
            pending = false
        }
        for (period in 1..whole) {
            if (input.contributionTiming == ContributionTiming.BEGINNING) deposit()
            accrue(1.0)
            if (input.contributionTiming == ContributionTiming.END) deposit()
            pending = true
            if (period % nc == 0) recordYear()
        }
        if (remainder > 0.0) {
            if (input.contributionTiming == ContributionTiming.BEGINNING) deposit()
            accrue(remainder)
            pending = true
        }
        if (pending) recordYear()
        return SimulationResult(balance, contributions, interest, schedule)
    }

    private fun solveRate(input: InvestmentInput): Double {
        var low = MIN_RETURN
        var high = MAX_RETURN
        val minimum = simulate(input.copy(returnRatePercent = low)).endBalance
        val maximum = simulate(input.copy(returnRatePercent = high)).endBalance
        val target = input.targetEndAmount
        require(target >= minimum && target <= maximum) {
            "Target cannot be reached with an annual return between $MIN_RETURN% and $MAX_RETURN%."
        }
        require(maximum > minimum) { "These inputs do not determine a return rate." }
        repeat(100) {
            val mid = (low + high) / 2.0
            if (simulate(input.copy(returnRatePercent = mid)).endBalance < target) low = mid
            else high = mid
        }
        val achieved = simulate(input.copy(returnRatePercent = high)).endBalance
        require(abs(achieved - target) <= maxOf(0.01, target * 1e-9)) {
            "Could not find a reliable return rate for this target."
        }
        return high
    }

    private fun solveDuration(input: InvestmentInput): Double {
        // Duration means the earliest time the balance reaches AT LEAST the target.
        if (input.startingAmount >= input.targetEndAmount) return 0.0
        val nc = input.contributionFrequency.periodsPerYear
        val rate = periodicRate(input)
        var balance = input.startingAmount
        val target = input.targetEndAmount
        val contribution = input.additionalContribution
        for (period in 0 until (MAX_YEARS * nc).toInt()) {
            val startTime = period.toDouble() / nc
            if (input.contributionTiming == ContributionTiming.BEGINNING) {
                balance += contribution
                if (balance >= target) {
                    // simulate(0) intentionally has no deposits. Choose the first representable
                    // time inside this period, so simulation includes its beginning deposit.
                    var time = Math.nextUp(startTime)
                    while (time * nc <= period.toDouble()) time = Math.nextUp(time)
                    if (simulate(input.copy(investLengthYears = time)).endBalance >= target) return time
                }
            }
            val afterInterest = balance * (1.0 + rate)
            if (rate > 0.0 && balance > 0.0 && afterInterest >= target) {
                val fraction = (ln(target / balance) / ln(1.0 + rate)).coerceIn(0.0, 1.0)
                var time = (period + fraction) / nc
                // Round toward a reached target, never return the unsuccessful side.
                repeat(8) {
                    if (simulate(input.copy(investLengthYears = time)).endBalance >= target) return time
                    time = Math.nextUp(time)
                }
            }
            balance = afterInterest
            if (input.contributionTiming == ContributionTiming.END) {
                balance += contribution
                if (balance >= target) {
                    var time = (period + 1.0) / nc
                    while (time * nc < period + 1.0) time = Math.nextUp(time)
                    return time
                }
            }
        }
        throw IllegalArgumentException("Target cannot be reached within $MAX_YEARS years with these inputs.")
    }
}