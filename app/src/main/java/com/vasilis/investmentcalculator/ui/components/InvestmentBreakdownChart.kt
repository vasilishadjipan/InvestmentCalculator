package com.vasilis.investmentcalculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vasilis.investmentcalculator.InvestmentResult
import com.vasilis.investmentcalculator.ui.theme.ContributionsColor
import com.vasilis.investmentcalculator.ui.theme.InterestColor
import com.vasilis.investmentcalculator.ui.theme.PrincipalColor
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieModel
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import androidx.compose.ui.platform.LocalLocale

@Composable
fun InvestmentBreakdownChart(
    result: InvestmentResult,
    modifier: Modifier = Modifier
) {
    if (result.endBalance <= 0.0 || result.totalInterest < 0.0) return

    val total = result.endBalance
    val principalPct = (result.startingAmount / total).coerceIn(0.0, 1.0).toFloat()
    val contribPct = (result.totalContributions / total).coerceIn(0.0, 1.0).toFloat()
    val interestPct = (result.totalInterest / total).coerceIn(0.0, 1.0).toFloat()

    val modelProducer = remember {
        PieChartModelProducer()
    }

    LaunchedEffect(result) {
        modelProducer.runTransaction {
            pieModel {
                series(
                    result.startingAmount,
                    result.totalContributions,
                    result.totalInterest
                )
            }
        }
    }

    val pieChart = rememberPieChart(
        sliceProvider = PieChart.SliceProvider.series(
            listOf(PrincipalColor, ContributionsColor, InterestColor).map { color ->
                PieChart.Slice(fill = Fill(color))
            }
        ),
        outerSize = PieSize.Outer.fixed(140.dp),
        innerSize = PieSize.Inner.fixed(84.dp),
        spacing = 0.dp
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Investment Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChartHost(
                        chart = pieChart,
                        modelProducer = modelProducer,
                        modifier = Modifier.size(140.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LegendItem(
                        color = PrincipalColor,
                        label = "Principal",
                        percentage = String.format(
                            LocalLocale.current.platformLocale,
                            "%.1f%%",
                            principalPct * 100
                        )
                    )
                    LegendItem(
                        color = ContributionsColor,
                        label = "Contributions",
                        percentage = String.format(
                            LocalLocale.current.platformLocale,
                            "%.1f%%",
                            contribPct * 100
                        )
                    )
                    LegendItem(
                        color = InterestColor,
                        label = "Interest",
                        percentage = String.format(
                            LocalLocale.current.platformLocale,
                            "%.1f%%",
                            interestPct * 100
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    percentage: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.size(12.dp),
            shape = RoundedCornerShape(3.dp),
            colors = CardDefaults.cardColors(containerColor = color)
        ) {}
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = percentage,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
