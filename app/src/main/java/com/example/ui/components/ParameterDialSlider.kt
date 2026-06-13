package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ParameterDialSlider(
    title: String,
    options: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // When the option is updated externally, scroll list state to update selected visual
    LaunchedEffect(selectedValue) {
        val index = options.indexOf(selectedValue)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141414), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = selectedValue,
                color = Color(0xFF00FF66), // Glowing green selection
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("dial_selected_value_${title.lowercase()}")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(Color(0xFF090909), RoundedCornerShape(4.dp))
                .drawBehind {
                    // Draw a subtle linear gradient fading of the left and right ends
                    val brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black
                        ),
                        startX = 0f,
                        endX = size.width
                    )
                    drawRect(brush)

                    // Draw center notch arrow indicators
                    drawLine(
                        color = Color(0xFF00FF66),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, 10f),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color(0xFF00FF66),
                        start = Offset(size.width / 2f, size.height),
                        end = Offset(size.width / 2f, size.height - 10f),
                        strokeWidth = 3f
                    )
                }
        ) {
            // Snapping dial list
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dial_row_${title.lowercase()}"),
                contentPadding = PaddingValues(horizontal = 140.dp), // centers lists inside horizontal viewer
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(options) { index, option ->
                    val isSelected = option == selectedValue
                    Column(
                        modifier = Modifier
                            .width(50.dp)
                            .clickable {
                                onValueSelected(option)
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                            .testTag("dial_option_${title.lowercase()}_$option"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dial vertical tic mark
                        Canvas(modifier = Modifier.height(14.dp).width(2.dp)) {
                            drawLine(
                                color = if (isSelected) Color(0xFF00FF66) else Color.White.copy(alpha = 0.3f),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = if (isSelected) 4f else 2f
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = option,
                            color = if (isSelected) Color(0xFF00FF66) else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
