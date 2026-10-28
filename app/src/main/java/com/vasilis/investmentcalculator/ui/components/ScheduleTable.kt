package com.vasilis.investmentcalculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasilis.investmentcalculator.AnnualScheduleItem
import com.vasilis.investmentcalculator.R
import java.text.NumberFormat
import kotlinx.coroutines.launch

@Composable
fun ScheduleTable(
    schedule: List<AnnualScheduleItem>,
    modifier: Modifier = Modifier
) {
    if (schedule.isEmpty()) return

    val currencyFormat =
        NumberFormat.getCurrencyInstance(LocalLocale.current.platformLocale).apply {
            maximumFractionDigits = 0
        }

    val tableWidth = (400 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val edgeColor = MaterialTheme.colorScheme.surfaceContainerLow

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Annual Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.horizontalScroll(scrollState)) {
                    Column(
                        modifier = Modifier.width(tableWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Year",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Contribution",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(4f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Interest",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(4f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "End Balance",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(5f),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Schedule Rows
                        schedule.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.year}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = currencyFormat.format(item.annualContribution),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(4f)
                                )
                                Text(
                                    text = currencyFormat.format(item.annualInterest),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(4f)
                                )
                                Text(
                                    text = currencyFormat.format(item.endingBalance),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(5f)
                                )
                            }
                            if (index < schedule.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.3f
                                    )
                                )
                            }
                        }
                    }
                }
                Canvas(Modifier.matchParentSize()) {
                    val fadeWidth = 36.dp.toPx().coerceAtMost(size.width / 2)
                    if (scrollState.canScrollBackward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(edgeColor, Color.Transparent),
                                startX = 0f,
                                endX = fadeWidth
                            ),
                            size = Size(fadeWidth, size.height)
                        )
                    }
                    if (scrollState.canScrollForward) {
                        val startX = size.width - fadeWidth
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, edgeColor),
                                startX = startX,
                                endX = size.width
                            ),
                            topLeft = Offset(startX, 0f),
                            size = Size(fadeWidth, size.height)
                        )
                    }
                }
                if (scrollState.canScrollBackward) {
                    ScrollArrow(
                        pointsRight = false,
                        onClick = {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    (scrollState.value - scrollState.viewportSize * 3 / 4)
                                        .coerceAtLeast(0)
                                )
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
                if (scrollState.canScrollForward) {
                    ScrollArrow(
                        pointsRight = true,
                        onClick = {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    (scrollState.value + scrollState.viewportSize * 3 / 4)
                                        .coerceAtMost(scrollState.maxValue)
                                )
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollArrow(
    pointsRight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val description = stringResource(
        if (pointsRight) R.string.schedule_more_right else R.string.schedule_more_left
    )
    Canvas(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = description }
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick)
    ) {
        val tailX = size.width * if (pointsRight) 0.4f else 0.6f
        val tipX = size.width * if (pointsRight) 0.6f else 0.4f
        val tip = Offset(tipX, size.height * 0.5f)
        drawLine(color, Offset(tailX, size.height * 0.3f), tip, 2.dp.toPx(), StrokeCap.Round)
        drawLine(color, tip, Offset(tailX, size.height * 0.7f), 2.dp.toPx(), StrokeCap.Round)
    }
}
