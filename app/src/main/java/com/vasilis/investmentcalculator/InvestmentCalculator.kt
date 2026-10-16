package com.vasilis.investmentcalculator

import java.util.Locale
import kotlin.math.exp
import kotlin.math.pow

enum class CalculationMode(val title: String, val description: String) {
    END_AMOUNT("End Amount", "Calculate final balance accumulated"),
    STARTING_AMOUNT("Starting Amount", "Calculate initial deposit required"),
    RETURN_RATE("Return Rate", "Calculate required annual return rate"),
    INVEST_LENGTH("Invest Length", "Calculate investment duration in years"),
    CONTRIBUTE_AMOUNT("Contribute Amount", "Calculate periodic deposit needed")
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
    val schedule: List<AnnualScheduleItem>
)

object InvestmentCalculator {

    fun getCurrencySymbol(locale: Locale = Locale.getDefault()): String {
        return try {
            java.util.Currency.getInstance(locale).symbol
        } catch (e: Exception) {
            "$"
        }
    }

    fun calculate(input: InvestmentInput, locale: Locale = Locale.getDefault()): InvestmentResult {
        return when (input.mode) {
            CalculationMode.END_AMOUNT -> calculateEndAmount(input, locale)
            CalculationMode.STARTING_AMOUNT -> calculateStartingAmount(input, locale)
            CalculationMode.CONTRIBUTE_AMOUNT -> calculateContributeAmount(input, locale)
            CalculationMode.RETURN_RATE -> calculateReturnRate(input)
            CalculationMode.INVEST_LENGTH -> calculateInvestLength(input)
        }
    }

    private fun getPeriodicRate(returnRatePercent: Double, contributionFreq: ContributionFrequency, compoundFreq: CompoundFrequency): Double {
        val r = returnRatePercent / 100.0
        val nc = contributionFreq.periodsPerYear.toDouble()

        return if (compoundFreq == CompoundFrequency.CONTINUOUSLY) {
            exp(r / nc) - 1.0
        } else {
            val nk = compoundFreq.periodsPerYear.toDouble()
            (1.0 + r / nk).pow(nk / nc) - 1.0
        }
    }

    private fun simulate(
        startingAmount: Double,
        investLengthYears: Double,
        returnRatePercent: Double,
        additionalContribution: Double,
        contributionFreq: ContributionFrequency,
        contributionTiming: ContributionTiming,
        compoundFreq: CompoundFrequency
    ): SimulationResult {
        val ip = getPeriodicRate(returnRatePercent, contributionFreq, compoundFreq)
        val nc = contributionFreq.periodsPerYear
        val fullYears = investLengthYears.toInt().coerceAtLeast(0)
        val remainingFraction = investLengthYears - fullYears

        var currentBalance = startingAmount.coerceAtLeast(0.0)
        var totalContributions = 0.0
        var totalInterest = 0.0

        val schedule = mutableListOf<AnnualScheduleItem>()

        for (year in 1..fullYears) {
            val yearStartBalance = currentBalance
            var yearContrib = 0.0
            var yearInterest = 0.0

            for (period in 1..nc) {
                if (contributionTiming == ContributionTiming.BEGINNING) {
                    currentBalance += additionalContribution
                    totalContributions += additionalContribution
                    yearContrib += additionalContribution
                }

                val periodInterest = currentBalance * ip
                currentBalance += periodInterest
                totalInterest += periodInterest
                yearInterest += periodInterest

                if (contributionTiming == ContributionTiming.END) {
                    currentBalance += additionalContribution
                    totalContributions += additionalContribution
                    yearContrib += additionalContribution
                }
            }

            schedule.add(
                AnnualScheduleItem(
                    year = year,
                    startingBalance = yearStartBalance,
                    annualContribution = yearContrib,
                    annualInterest = yearInterest,
                    endingBalance = currentBalance,
                    totalContributionsToDate = totalContributions,
                    totalInterestToDate = totalInterest
                )
            )
        }

        if (remainingFraction > 0.0) {
            val yearStartBalance = currentBalance
            val partialPeriods = (remainingFraction * nc).toInt().coerceAtLeast(1)
            var yearContrib = 0.0
            var yearInterest = 0.0

            for (period in 1..partialPeriods) {
                val fraction = remainingFraction / partialPeriods
                val contribPart = additionalContribution * fraction

                if (contributionTiming == ContributionTiming.BEGINNING) {
                    currentBalance += contribPart
                    totalContributions += contribPart
                    yearContrib += contribPart
                }

                val periodInterest = currentBalance * ip * fraction
                currentBalance += periodInterest
                totalInterest += periodInterest
                yearInterest += periodInterest

                if (contributionTiming == ContributionTiming.END) {
                    currentBalance += contribPart
                    totalContributions += contribPart
                    yearContrib += contribPart
                }
            }

            schedule.add(
                AnnualScheduleItem(
                    year = fullYears + 1,
                    startingBalance = yearStartBalance,
                    annualContribution = yearContrib,
                    annualInterest = yearInterest,
                    endingBalance = currentBalance,
                    totalContributionsToDate = totalContributions,
                    totalInterestToDate = totalInterest
                )
            )
        }

        return SimulationResult(
            endBalance = currentBalance,
            totalContributions = totalContributions,
            totalInterest = totalInterest,
            schedule = schedule
        )
    }

    private data class SimulationResult(
        val endBalance: Double,
        val totalContributions: Double,
        val totalInterest: Double,
        val schedule: List<AnnualScheduleItem>
    )

    private fun calculateEndAmount(input: InvestmentInput, locale: Locale): InvestmentResult {
        val sim = simulate(
            startingAmount = input.startingAmount,
            investLengthYears = input.investLengthYears,
            returnRatePercent = input.returnRatePercent,
            additionalContribution = input.additionalContribution,
            contributionFreq = input.contributionFrequency,
            contributionTiming = input.contributionTiming,
            compoundFreq = input.compoundFrequency
        )

        return InvestmentResult(
            mode = CalculationMode.END_AMOUNT,
            solvedValue = sim.endBalance,
            solvedLabel = "End Balance",
            solvedUnit = getCurrencySymbol(locale),
            endBalance = sim.endBalance,
            startingAmount = input.startingAmount,
            totalContributions = sim.totalContributions,
            totalInterest = sim.totalInterest,
            investLengthYears = input.investLengthYears,
            returnRatePercent = input.returnRatePercent,
            additionalContribution = input.additionalContribution,
            schedule = sim.schedule
        )
    }

    private fun calculateStartingAmount(input: InvestmentInput, locale: Locale): InvestmentResult {
        // Linear relationship: EndBalance = P * factorP + factorC
        val sim0 = simulate(0.0, input.investLengthYears, input.returnRatePercent, input.additionalContribution, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)
        val sim1 = simulate(10000.0, input.investLengthYears, input.returnRatePercent, input.additionalContribution, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)

        val factorP = (sim1.endBalance - sim0.endBalance) / 10000.0
        val startingAmount = if (factorP != 0.0) {
            ((input.targetEndAmount - sim0.endBalance) / factorP).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val simFinal = simulate(startingAmount, input.investLengthYears, input.returnRatePercent, input.additionalContribution, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)

        return InvestmentResult(
            mode = CalculationMode.STARTING_AMOUNT,
            solvedValue = startingAmount,
            solvedLabel = "Starting Amount Required",
            solvedUnit = getCurrencySymbol(locale),
            endBalance = simFinal.endBalance,
            startingAmount = startingAmount,
            totalContributions = simFinal.totalContributions,
            totalInterest = simFinal.totalInterest,
            investLengthYears = input.investLengthYears,
            returnRatePercent = input.returnRatePercent,
            additionalContribution = input.additionalContribution,
            schedule = simFinal.schedule
        )
    }

    private fun calculateContributeAmount(input: InvestmentInput, locale: Locale): InvestmentResult {
        // Linear relationship: EndBalance = C * factorC + factorP
        val sim0 = simulate(input.startingAmount, input.investLengthYears, input.returnRatePercent, 0.0, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)
        val sim100 = simulate(input.startingAmount, input.investLengthYears, input.returnRatePercent, 100.0, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)

        val factorC = (sim100.endBalance - sim0.endBalance) / 100.0
        val contribAmount = if (factorC != 0.0) {
            ((input.targetEndAmount - sim0.endBalance) / factorC).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val simFinal = simulate(input.startingAmount, input.investLengthYears, input.returnRatePercent, contribAmount, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)

        return InvestmentResult(
            mode = CalculationMode.CONTRIBUTE_AMOUNT,
            solvedValue = contribAmount,
            solvedLabel = "Contribution Amount Required",
            solvedUnit = getCurrencySymbol(locale),
            endBalance = simFinal.endBalance,
            startingAmount = input.startingAmount,
            totalContributions = simFinal.totalContributions,
            totalInterest = simFinal.totalInterest,
            investLengthYears = input.investLengthYears,
            returnRatePercent = input.returnRatePercent,
            additionalContribution = contribAmount,
            schedule = simFinal.schedule
        )
    }

    private fun calculateReturnRate(input: InvestmentInput): InvestmentResult {
        // Binary search for return rate r in [-90.0, 500.0]
        var low = -90.0
        var high = 500.0
        var bestRate = 0.0

        for (i in 0..60) {
            val mid = (low + high) / 2.0
            val sim = simulate(input.startingAmount, input.investLengthYears, mid, input.additionalContribution, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)
            if (sim.endBalance < input.targetEndAmount) {
                low = mid
            } else {
                high = mid
            }
            bestRate = mid
        }

        val simFinal = simulate(input.startingAmount, input.investLengthYears, bestRate, input.additionalContribution, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)

        return InvestmentResult(
            mode = CalculationMode.RETURN_RATE,
            solvedValue = bestRate,
            solvedLabel = "Return Rate Required",
            solvedUnit = "%",
            endBalance = simFinal.endBalance,
            startingAmount = input.startingAmount,
            totalContributions = simFinal.totalContributions,
            totalInterest = simFinal.totalInterest,
            investLengthYears = input.investLengthYears,
            returnRatePercent = bestRate,
            additionalContribution = input.additionalContribution,
            schedule = simFinal.schedule
        )
    }

    private fun calculateInvestLength(input: InvestmentInput): InvestmentResult {
        // Binary search for invest length in [0.01, 100.0] years
        var low = 0.01
        var high = 100.0
        var bestLength = 1.0

        for (i in 0..50) {
            val mid = (low + high) / 2.0
            val sim = simulate(input.startingAmount, mid, input.returnRatePercent, input.additionalContribution, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)
            if (sim.endBalance < input.targetEndAmount) {
                low = mid
            } else {
                high = mid
            }
            bestLength = mid
        }

        val simFinal = simulate(input.startingAmount, bestLength, input.returnRatePercent, input.additionalContribution, input.contributionFrequency, input.contributionTiming, input.compoundFrequency)

        return InvestmentResult(
            mode = CalculationMode.INVEST_LENGTH,
            solvedValue = bestLength,
            solvedLabel = "Investment Duration Required",
            solvedUnit = "Years",
            endBalance = simFinal.endBalance,
            startingAmount = input.startingAmount,
            totalContributions = simFinal.totalContributions,
            totalInterest = simFinal.totalInterest,
            investLengthYears = bestLength,
            returnRatePercent = input.returnRatePercent,
            additionalContribution = input.additionalContribution,
            schedule = simFinal.schedule
        )
    }
}
