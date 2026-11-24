package com.anmedia.player.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex

/**
 * A minimal drag-to-reorder [LazyColumn], built without a third-party dependency. The drag
 * handle (long-press anywhere on the row, by default) reports live swaps through [onMove]
 * and the caller commits the final order in [onDragEnd].
 */
@Composable
fun <T> ReorderableList(
    items: List<T>,
    key: (T) -> Any,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, isDragging: Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    val itemHeights = remember { mutableStateMapOf<Any, Int>() }

    LazyColumn(state = listState, modifier = modifier) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            val itemKey = key(item)
            val isDragging = itemKey == draggingKey

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragging) dragOffsetY else 0f }
                    .onGloballyPositioned { coordinates ->
                        itemHeights[itemKey] = coordinates.size.height
                    }
                    .pointerInput(itemKey) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingKey = itemKey
                                draggingIndex = index
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val currentHeight = itemHeights[itemKey] ?: return@detectDragGesturesAfterLongPress
                                val threshold = currentHeight / 2
                                val from = draggingIndex
                                if (dragOffsetY > threshold && from < items.lastIndex) {
                                    onMove(from, from + 1)
                                    draggingIndex = from + 1
                                    dragOffsetY -= currentHeight
                                } else if (dragOffsetY < -threshold && from > 0) {
                                    onMove(from, from - 1)
                                    draggingIndex = from - 1
                                    dragOffsetY += currentHeight
                                }
                            },
                            onDragEnd = {
                                draggingKey = null
                                dragOffsetY = 0f
                                draggingIndex = -1
                                onDragEnd()
                            },
                            onDragCancel = {
                                draggingKey = null
                                dragOffsetY = 0f
                                draggingIndex = -1
                            }
                        )
                    }
            ) {
                itemContent(item, isDragging)
            }
        }
    }
}
