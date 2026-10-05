package com.sichuan.monogallery

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Open result of a PDF: still loading, failed to open, or ready with an open [PdfDocument]. */
private sealed interface PdfOpenResult {
    object Loading : PdfOpenResult
    data class Error(val message: String) : PdfOpenResult
    data class Success(val document: PdfDocument) : PdfOpenResult
}

/**
 * Turns an exception thrown while opening a PDF into a short, user-facing Chinese description so
 * the user can see *why* a file failed to open instead of a generic message.
 */
private fun describePdfOpenError(e: Throwable?): String {
    val detail = e?.message?.trim()?.takeIf { it.isNotEmpty() }
    return when {
        e is FileNotFoundException -> "文件不存在或无法访问"
        e is SecurityException -> "文件已加密或受密码保护，暂不支持打开"
        detail != null && (
            detail.contains("not in PDF", ignoreCase = true) ||
                detail.contains("not a pdf", ignoreCase = true) ||
                detail.contains("corrupt", ignoreCase = true)
            ) -> "文件不是有效的 PDF，或已损坏"
        detail != null -> "无法打开：$detail"
        else -> "无法打开（${e?.javaClass?.simpleName ?: "未知错误"}）"
    }
}

/** Bounded per-document page cache size, in bytes (~32 MB of ARGB_8888 bitmaps). */
private const val PAGE_CACHE_BYTES = 32 * 1024 * 1024

/**
 * Hard cap on a single page's pixel count (~10 MB as ARGB_8888). Very tall pages are scaled
 * down to this budget so one page can never allocate an oversized bitmap and crash with OOM.
 */
private const val MAX_PAGE_PIXELS = 2_500_000L

/** Number of pages to render eagerly after opening so the first scroll feels instant. */
private const val PREFETCH_PAGES = 3

/** Maximum pinch-zoom scale (1x = fit to screen). */
private const val MAX_ZOOM = 4f

/** Reading modes for the PDF viewer. */
private enum class PdfReadingMode(val label: String) {
    /** Pages stacked vertically, scrolled top-to-bottom (default). */
    Vertical("上下"),

    /** Pages laid out side by side and scrolled left-to-right. */
    HorizontalLtr("左→右"),

    /** Pages laid out side by side and scrolled right-to-left. */
    HorizontalRtl("右→左"),
}

/** Pinch-zoom state: a scale multiplier and a pan offset applied to the whole page list. */
private class PdfZoomState {
    var scale by mutableStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    val isZoomed: Boolean get() = scale > 1.01f

    fun applyGesture(zoomChange: Float, panChange: Offset, maxOffsetX: Float, maxOffsetY: Float) {
        scale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        if (isZoomed) {
            offset = Offset(
                (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY),
            )
        } else {
            offset = Offset.Zero
        }
    }

    fun panBy(delta: Offset, maxOffsetX: Float, maxOffsetY: Float) {
        if (!isZoomed) return
        offset = Offset(
            (offset.x + delta.x).coerceIn(-maxOffsetX, maxOffsetX),
            (offset.y + delta.y).coerceIn(-maxOffsetY, maxOffsetY),
        )
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }
}

/**
 * Detects two-finger pinch-zoom and, while already zoomed, one-finger pan. A single-finger drag
 * while not zoomed is left unconsumed so the underlying scrollable list can scroll normally.
 */
private suspend fun PointerInputScope.detectPinchToZoom(
    zoom: PdfZoomState,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        // The first event with two pointers only establishes the baseline; applying it would
        // jump because the centroid/distance reference only one pointer from the previous event.
        var twoFingerBaseline = false
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed >= 2) {
                if (twoFingerBaseline) {
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    val newScale = (zoom.scale * zoomChange).coerceIn(1f, MAX_ZOOM)
                    val maxX = (newScale - 1f) * viewportWidthPx / 2f
                    val maxY = (newScale - 1f) * viewportHeightPx / 2f
                    zoom.applyGesture(zoomChange, panChange, maxX, maxY)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                } else {
                    twoFingerBaseline = true
                }
            } else if (pressed == 1 && zoom.isZoomed) {
                val panChange = event.calculatePan()
                val maxX = (zoom.scale - 1f) * viewportWidthPx / 2f
                val maxY = (zoom.scale - 1f) * viewportHeightPx / 2f
                zoom.panBy(panChange, maxX, maxY)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * An open PDF document. [PdfRenderer] allows only one page to be open at a time and is not
 * thread-safe, so every page render AND the final close are serialized through [mutex] and run
 * on an IO thread. Rendered pages are cached (bounded, keyed by page and render size) so
 * scrolling back is instant; bitmaps are intentionally never recycled manually — on API 29+
 * the GC reclaims native bitmap memory, and recycling can crash while an [Image] still shows it.
 */
private class PdfDocument private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    private val mutex = Mutex()

    @Volatile
    private var closed = false

    // Keyed by "$index@${width}x$height" so a rotation or mode change (size change) re-renders
    // instead of reusing a stale size.
    private val pageCache = object : LruCache<String, Bitmap>(PAGE_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    val pageCount: Int get() = renderer.pageCount

    companion object {
        /**
         * Opens [file] and returns the document, or a [Result.failure] carrying the exception that
         * describes why it could not be opened (missing/inaccessible file, password-protected PDF,
         * corrupt or non-PDF data, or no pages). Resources acquired before a failure are closed.
         */
        fun open(file: File): Result<PdfDocument> {
            return try {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                try {
                    val renderer = PdfRenderer(fd)
                    if (renderer.pageCount <= 0) {
                        renderer.close()
                        fd.close()
                        return Result.failure(IOException("该 PDF 不含任何页面"))
                    }
                    Result.success(PdfDocument(fd, renderer))
                } catch (e: Exception) {
                    try {
                        fd.close()
                    } catch (_: Exception) {
                    }
                    throw e
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Renders the page at [index] to fit within [maxWidthPx]×[maxHeightPx] (preserving its aspect
     * ratio; pass [Int.MAX_VALUE] for the unconstrained dimension), or null on failure. The total
     * pixel count is hard-capped to avoid OOM, and cached pages are returned without re-rendering.
     */
    suspend fun renderPage(index: Int, maxWidthPx: Int, maxHeightPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed) return@withLock null
            val key = "$index@${maxWidthPx}x$maxHeightPx"
            pageCache.get(key)?.let { return@withLock it.asImageBitmap() }
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(index)
                if (page.width <= 0 || page.height <= 0) return@withLock null
                // Scale to fit the requested box, then clamp so a single page can't OOM.
                val fitScale = minOf(
                    maxWidthPx.toFloat() / page.width,
                    maxHeightPx.toFloat() / page.height,
                )
                val cappedScale = sqrt(MAX_PAGE_PIXELS.toDouble() / (page.width.toLong() * page.height)).toFloat()
                val scale = minOf(fitScale, cappedScale)
                val width = maxOf(1, (page.width * scale).toInt())
                val height = maxOf(1, (page.height * scale).toInt())
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pageCache.put(key, bitmap)
                bitmap.asImageBitmap()
            } catch (_: Exception) {
                null
            } finally {
                page?.close()
            }
        }
    }

    /**
     * Closes the renderer and file descriptor exactly once, serialized with any in-flight render
     * so it can never race a running [PdfRenderer.Page.render]. Safe to call multiple times.
     */
    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed) return@withLock
            closed = true
            pageCache.evictAll()
            try {
                renderer.close()
            } catch (_: Exception) {
            }
            try {
                pfd.close()
            } catch (_: Exception) {
            }
        }
    }
}

/**
 * PDF preview screen. Pages are rendered lazily as they scroll into view (off-screen pages are
 * not decoded), cached so revisiting a page is instant, and a few pages are prefetched up front.
 * Supports pinch-to-zoom, a tap-to-toggle toolbar (progress slider, an editable current-page
 * field, total page count, and three reading modes: top-to-bottom scrolling, left-to-right and
 * right-to-left continuous flipping). The document is opened once and reliably closed when the
 * screen leaves composition or the path changes.
 */
@Composable
fun PdfViewer(file: File?, modifier: Modifier = Modifier) {
    val path = file?.absolutePath
    val openState = produceState<PdfOpenResult>(initialValue = PdfOpenResult.Loading, path) {
        // Reset immediately so a path change shows the spinner instead of the previous document.
        value = PdfOpenResult.Loading
        if (path == null) {
            value = PdfOpenResult.Error("文件不存在")
            return@produceState
        }
        var document: PdfDocument? = null
        try {
            val opened = withContext(Dispatchers.IO) { PdfDocument.open(File(path)) }
            val doc = opened.getOrNull()
            if (doc == null) {
                value = PdfOpenResult.Error(describePdfOpenError(opened.exceptionOrNull()))
                return@produceState
            }
            document = doc
            value = PdfOpenResult.Success(doc)
            // Stay alive until the composable leaves or the path changes; the finally below
            // closes the document on the IO thread (serialized with renders) so it can't race.
            awaitCancellation()
        } finally {
            document?.let { doc ->
                withContext(NonCancellable + Dispatchers.IO) {
                    doc.close()
                }
            }
        }
    }

    Box(modifier = modifier.background(ColorBackground)) {
        when (val result = openState.value) {
            PdfOpenResult.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ColorAccent)
            }

            is PdfOpenResult.Error -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("无法打开该 PDF 文件", color = ColorTextSecondary)
                Text(
                    text = result.message,
                    color = ColorTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            is PdfOpenResult.Success -> {
                val document = result.document
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val horizontalPadding = 10.dp
                val verticalPadding = 12.dp

                // Content area minus the list's padding; used to size rendered pages.
                val contentWidthPx = with(density) {
                    (configuration.screenWidthDp.dp - horizontalPadding * 2).roundToPx()
                }
                val contentHeightPx = with(density) {
                    (configuration.screenHeightDp.dp - verticalPadding * 2).roundToPx()
                }
                // Full viewport, used to bound pinch-zoom panning.
                val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

                val pages = remember(document) { List(document.pageCount) { it } }
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()

                var readingMode by remember { mutableStateOf(PdfReadingMode.Vertical) }
                var showToolbar by remember { mutableStateOf(true) }
                val zoom = remember { PdfZoomState() }

                val horizontal = readingMode != PdfReadingMode.Vertical
                // Vertical renders to the content width; horizontal renders to the content height
                // so pages are tall enough to fill the viewport side by side.
                val maxWidthPx = if (horizontal) Int.MAX_VALUE else contentWidthPx
                val maxHeightPx = if (horizontal) contentHeightPx else Int.MAX_VALUE

                // Warm the cache with the first few pages so the initial scroll is smooth.
                LaunchedEffect(document, maxWidthPx, maxHeightPx) {
                    val count = document.pageCount
                    for (i in 0 until minOf(count, PREFETCH_PAGES)) {
                        document.renderPage(i, maxWidthPx, maxHeightPx)
                    }
                }

                // Zoom is per-orientation; reset it whenever the reading mode changes.
                LaunchedEffect(readingMode) { zoom.reset() }

                val currentPageIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

                Box(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { showToolbar = !showToolbar })
                            }
                            .pointerInput(zoom, screenWidthPx, screenHeightPx) {
                                detectPinchToZoom(zoom, screenWidthPx, screenHeightPx)
                            }
                            .graphicsLayer {
                                scaleX = zoom.scale
                                scaleY = zoom.scale
                                translationX = zoom.offset.x
                                translationY = zoom.offset.y
                            },
                    ) {
                        when (readingMode) {
                            PdfReadingMode.Vertical -> LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = !zoom.isZoomed,
                                contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(pages, key = { it }) { index ->
                                    PdfPageImage(document, index, maxWidthPx, maxHeightPx, horizontal = false)
                                }
                            }

                            PdfReadingMode.HorizontalLtr,
                            PdfReadingMode.HorizontalRtl,
                            -> LazyRow(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = !zoom.isZoomed,
                                reverseLayout = readingMode == PdfReadingMode.HorizontalRtl,
                                contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(pages, key = { it }) { index ->
                                    PdfPageImage(document, index, maxWidthPx, maxHeightPx, horizontal = true)
                                }
                            }
                        }
                    }

                    if (showToolbar) {
                        PdfToolbar(
                            currentPageIndex = currentPageIndex,
                            pageCount = document.pageCount,
                            onGoToPage = { index -> scope.launch { listState.scrollToItem(index) } },
                            readingMode = readingMode,
                            onReadingModeChange = { readingMode = it },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}

/** A single PDF page: renders lazily and shows a neutral placeholder until its bitmap is ready. */
@Composable
private fun PdfPageImage(
    document: PdfDocument,
    index: Int,
    maxWidthPx: Int,
    maxHeightPx: Int,
    horizontal: Boolean,
) {
    val bitmapState = produceState<ImageBitmap?>(
        initialValue = null,
        document, index, maxWidthPx, maxHeightPx,
    ) {
        value = document.renderPage(index, maxWidthPx, maxHeightPx)
    }
    val bitmap = bitmapState.value
    val clipModifier = Modifier.clip(RoundedCornerShape(6.dp))

    if (bitmap != null) {
        val aspect = bitmap.width.toFloat() / bitmap.height
        Image(
            bitmap = bitmap,
            contentDescription = "第 ${index + 1} 页",
            contentScale = ContentScale.Fit,
            modifier = clipModifier.then(
                if (horizontal) Modifier.fillMaxHeight().aspectRatio(aspect)
                else Modifier.fillMaxWidth().aspectRatio(aspect),
            ),
        )
    } else {
        Box(
            modifier = clipModifier.then(
                if (horizontal) Modifier.fillMaxHeight().aspectRatio(0.707f)
                else Modifier.fillMaxWidth().height(220.dp),
            ).background(ColorSearchField),
        )
    }
}

/** Bottom toolbar: progress slider, an editable current-page field with total count, and the reading-mode switch. */
@Composable
private fun PdfToolbar(
    currentPageIndex: Int,
    pageCount: Int,
    onGoToPage: (Int) -> Unit,
    readingMode: PdfReadingMode,
    onReadingModeChange: (PdfReadingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPage = currentPageIndex + 1
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPage by remember { mutableStateOf(currentPageIndex.toFloat()) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ColorCard,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Slider(
                value = (if (isScrubbing) scrubPage else currentPageIndex.toFloat())
                    .coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat()),
                onValueChange = {
                    isScrubbing = true
                    scrubPage = it
                },
                onValueChangeFinished = {
                    isScrubbing = false
                    onGoToPage(scrubPage.roundToInt().coerceIn(0, pageCount - 1))
                },
                valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
                enabled = pageCount > 1,
                colors = SliderDefaults.colors(
                    thumbColor = ColorAccent,
                    activeTrackColor = ColorAccent,
                    inactiveTrackColor = ColorSearchField,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageNumberField(currentPage = currentPage, pageCount = pageCount, onJump = onGoToPage)
                Text(
                    text = "/ $pageCount",
                    fontSize = 13.sp,
                    color = ColorTextSecondary,
                    modifier = Modifier.padding(start = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                ReadingModeSelector(readingMode, onReadingModeChange)
            }
        }
    }
}

/** Compact editable field showing the current page; jump on IME "Done". */
@Composable
private fun PageNumberField(
    currentPage: Int,
    pageCount: Int,
    onJump: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(currentPage.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Follow the current page while the user isn't editing, so typing isn't overwritten mid-edit.
    LaunchedEffect(currentPage, focused) {
        if (!focused) text = currentPage.toString()
    }

    BasicTextField(
        value = text,
        onValueChange = { new -> text = new.filter { it.isDigit() }.take(4) },
        modifier = Modifier
            .width(52.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(ColorSearchField, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        textStyle = TextStyle(
            color = ColorTextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            val page = text.toIntOrNull()?.coerceIn(1, pageCount)
            if (page != null) onJump(page - 1)
            focusManager.clearFocus()
            keyboard?.hide()
        }),
    )
}

/** Three-way reading-mode switch, highlighting the active mode. */
@Composable
private fun ReadingModeSelector(
    current: PdfReadingMode,
    onSelect: (PdfReadingMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PdfReadingMode.values().forEach { mode ->
            val selected = mode == current
            Text(
                text = mode.label,
                fontSize = 13.sp,
                color = if (selected) ColorCard else ColorTextPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) ColorAccent else ColorSearchField)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}
