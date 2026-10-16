package com.vasilis.investmentcalculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }
    }

    val modelProducer = remember {
        CartesianChartModelProducer()
    }

    LaunchedEffect(schedule, result.startingAmount) {
        modelProducer.runTransaction {

            columnModel {

                // Principal
                series(
                    schedule.indices.map { it.toFloat() },
                    schedule.map { item ->
                        result.startingAmount
                            .coerceAtMost(item.endingBalance)
                    }
                )

                // Contributions
                series(
                    schedule.indices.map { it.toFloat() },
                    schedule.map { item ->
                        val principal = result.startingAmount
                            .coerceAtMost(item.endingBalance)

                        item.totalContributionsToDate
                            .coerceAtMost(
                                item.endingBalance - principal
                            )
                    }
                )

                // Interest
                series(
                    schedule.indices.map { it.toFloat() },
                    schedule.map { item ->
                        item.totalInterestToDate
                            .coerceAtLeast(0.0)
                    }
                )
            }
        }
    }

    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            rememberLineComponent(
                fill = Fill(PrincipalColor),
                thickness = 12.dp
            ),
            rememberLineComponent(
                fill = Fill(ContributionsColor),
                thickness = 12.dp
            ),
            rememberLineComponent(
                fill = Fill(InterestColor),
                thickness = 12.dp
            )
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
                    color = MaterialTheme.colorScheme.primary
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
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                        valueFormatter = { _, value, _ ->
                            val index = value.roundToInt()
                            schedule.getOrNull(index)?.year?.toString() ?: value.roundToInt().toString()
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
        }
    }
}
