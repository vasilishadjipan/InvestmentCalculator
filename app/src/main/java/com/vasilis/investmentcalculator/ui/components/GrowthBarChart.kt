package com.vasilis.investmentcalculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GrowthBarChart(
    result: InvestmentResult,
    modifier: Modifier = Modifier,
) {
    val schedule = result.schedule

    if (schedule.isEmpty()) return

    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    val currencyFormat = remember(locale) {
        NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 0
        }
    }

    val hasValidInterest = remember(schedule) {
        schedule.all { it.totalInterestToDate >= 0.0 }
    }

    val modelProducer = remember {
        CartesianChartModelProducer()
    }

    LaunchedEffect(schedule, result.startingAmount, hasValidInterest) {
        modelProducer.runTransaction {
            columnModel {
                val years = schedule.map { it.year }
                series(years, schedule.map { result.startingAmount })
                series(years, schedule.map { it.totalContributionsToDate })
                if (hasValidInterest) {
                    series(years, schedule.map { it.totalInterestToDate })
                }
            }
        }
    }

    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            listOfNotNull(
                PrincipalColor,
                ContributionsColor,
                if (hasValidInterest) InterestColor else null
            ).map { color ->
                rememberLineComponent(
                    fill = Fill(color),
                    thickness = 12.dp
                )
            }
        ),
        mergeMode = { ColumnCartesianLayer.MergeMode.Stacked }
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Accumulation Growth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    columnLayer,

                    startAxis = VerticalAxis.rememberStart(
                        valueFormatter = { _, value, _ ->
                            currencyFormat.format(value)
                        }
                    ),

                    bottomAxis = HorizontalAxis.rememberBottom(
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                            spacing = { maxOf(1, (schedule.size + 5) / 6) }
                        ),
                        valueFormatter = { _, value, _ ->
                            value.roundToInt().toString()
                        }
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                scrollState = rememberVicoScrollState(
                    scrollEnabled = false
                )
            )

            // Legend Row
            FlowRow(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GrowthLegendItem(color = PrincipalColor, label = "Principal")
                GrowthLegendItem(color = ContributionsColor, label = "Contributions")
                if (hasValidInterest) {
                    GrowthLegendItem(color = InterestColor, label = "Interest")
                }
            }
        }
    }
}

@Composable
private fun GrowthLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Card(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(2.dp),
            colors = CardDefaults.cardColors(containerColor = color)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
