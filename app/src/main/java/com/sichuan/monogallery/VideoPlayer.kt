package com.sichuan.monogallery

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.io.File

/** Walks the ContextWrapper chain to find the hosting Activity, or null when none is found. Shared with [FileViewScreen]. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Video preview: plays a local video file with the built-in [MediaPlayer] rendered onto a
 * [TextureView], scaled to fit the screen while preserving the aspect ratio. Supports
 * play/pause (center button), seeking via the progress slider, ±10-second jumps, jumping to a
 * typed time by tapping the current playback time, and a portrait/landscape toggle at the
 * bottom-right. Tapping the video toggles the control overlay, which auto-hides after 3
 * seconds during playback; playback starts automatically once the video is prepared.
 *
 * A TextureView (rather than a SurfaceView) is used deliberately: it is composited like a
 * normal view and disappears together with the composition, so leaving the screen never
 * leaves the last frame behind. Back is intercepted so playback is stopped and the surface
 * cleared before navigating away; in landscape, the first back press returns to portrait.
 */
@Composable
fun VideoPlayer(
    file: File?,
    onBack: () -> Unit,
    controlsVisible: Boolean,
    onControlsVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val path = file?.absolutePath
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var duration by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }
    var displaySurface by remember { mutableStateOf<Surface?>(null) }
    var prepareStarted by remember { mutableStateOf(false) }

    val player = remember { MediaPlayer() }

    DisposableEffect(path) {
        duration = 0
        position = 0
        isPlaying = false
        isPrepared = false
        loadError = false
        prepareStarted = false
        onControlsVisibleChange(true)
        player.reset()
        if (path != null) {
            try {
                player.setDataSource(path)
                player.setOnPreparedListener { mp ->
                    duration = mp.duration
                    videoWidth = mp.videoWidth
                    videoHeight = mp.videoHeight
                    position = 0
                    isPrepared = true
                    // Auto-start playback once the video is ready
                    mp.start()
                    isPlaying = true
                }
                player.setOnCompletionListener {
                    isPlaying = false
                    position = duration
                    onControlsVisibleChange(true)
                }
                player.setOnErrorListener { _, _, _ ->
                    loadError = true
                    isPrepared = false
                    true
                }
            } catch (_: Exception) {
                loadError = true
            }
        } else {
            loadError = true
        }
        onDispose {
            // Detach and release on the player; the TextureView content is removed together with this composition
            try {
                player.setSurface(null)
            } catch (_: Exception) {
                // Ignore if the player was already released
            }
            try {
                player.release()
            } catch (_: Exception) {
                // Ignore the error if the player was already released
            }
        }
    }

    // The surface texture is created asynchronously: wrap it in a Surface, attach it as the
    // player's output, and kick off preparation once. If the texture is recreated, only
    // re-attach the surface (the player and its prepared state survive).
    LaunchedEffect(path, displaySurface) {
        val surface = displaySurface ?: return@LaunchedEffect
        if (path == null) return@LaunchedEffect
        try {
            player.setSurface(surface)
            if (!prepareStarted) {
                player.prepareAsync()
                prepareStarted = true
            }
        } catch (_: Exception) {
            loadError = true
        }
    }

    // Poll the playback position on a timer while playing; the coroutine cancels automatically
    // on leaving composition or when playback pauses
    LaunchedEffect(isPlaying) {
        while (isPlaying && isPrepared) {
            position = runCatching { player.currentPosition }.getOrDefault(position)
            delay(250)
        }
    }

    // Auto-hide the controls after 3 seconds of playback; any tap resets this timer
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            onControlsVisibleChange(false)
        }
    }

    fun togglePlay() {
        if (!isPrepared) return
        if (isPlaying) {
            player.pause()
            position = runCatching { player.currentPosition }.getOrDefault(position)
            isPlaying = false
            onControlsVisibleChange(true)
        } else {
            // Restart from the beginning if the previous playback reached the end
            if (duration > 0 && position >= duration) {
                player.seekTo(0)
                position = 0
            }
            player.start()
            isPlaying = true
        }
    }

    fun seekBy(deltaMillis: Int) {
        if (!isPrepared || duration <= 0) return
        val target = (player.currentPosition + deltaMillis).coerceIn(0, duration)
        player.seekTo(target)
        position = target
    }

    fun toggleOrientation() {
        val act = activity ?: return
        act.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            // USER_LANDSCAPE honors either landscape direction when auto-rotation is enabled
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        }
    }

    // Intercept back: in landscape first return to portrait; in portrait stop and clear the
    // picture BEFORE navigating, so the previous screen never shows with a leftover frame.
    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            runCatching { player.pause() }
            runCatching { player.setSurface(null) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onBack()
        }
    }

    if (loadError) {
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text("无法播放视频", color = Color(0xFFB3B3B3))
        }
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val containerAspect = this.maxWidth / this.maxHeight
        val videoAspect = if (videoHeight > 0) videoWidth.toFloat() / videoHeight else 0f
        // Fit-center: when the video is wider than the container pin the width, otherwise pin the height
        val surfaceModifier = when {
            videoAspect <= 0f -> Modifier.fillMaxSize()
            videoAspect > containerAspect -> Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspect)
            else -> Modifier
                .fillMaxHeight()
                .aspectRatio(videoAspect)
        }

        AndroidView(
            factory = { viewContext ->
                TextureView(viewContext).also { textureView ->
                    textureView.surfaceTextureListener =
                        object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                displaySurface = Surface(surfaceTexture)
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int,
                            ) {
                                // No action needed: the player scales the video onto the current texture
                            }

                            override fun onSurfaceTextureDestroyed(
                                surfaceTexture: SurfaceTexture,
                            ): Boolean {
                                // Release our Surface wrapper; returning true lets the framework release the SurfaceTexture
                                displaySurface?.release()
                                displaySurface = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                                // No action needed
                            }
                        }
                }
            },
            modifier = Modifier
                .align(Alignment.Center)
                .then(surfaceModifier),
        )

        // Transparent tap layer toggles the controls; no ripple so the video stays clean
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onControlsVisibleChange(!controlsVisible) },
        )

        if (!isPrepared) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
            )
        }

        // Center play button while paused or finished
        if (isPrepared && !isPlaying) {
            FilledIconButton(
                onClick = { togglePlay() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xCC000000),
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xB3000000)),
                        ),
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // While dragging, track a local scrub position and only commit the seek when the gesture ends,
                // so playback does not jump on every slider pixel
                Slider(
                    value = (if (isScrubbing) scrubPosition else position)
                        .toFloat()
                        .coerceIn(0f, duration.toFloat().coerceAtLeast(0f)),
                    onValueChange = {
                        isScrubbing = true
                        scrubPosition = it.toInt()
                    },
                    onValueChangeFinished = {
                        val target = scrubPosition.coerceIn(0, duration)
                        player.seekTo(target)
                        position = target
                        isScrubbing = false
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    enabled = isPrepared && duration > 0,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0x66FFFFFF),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTime(position),
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable(enabled = isPrepared && duration > 0) {
                                showTimeDialog = true
                            },
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "剩余 ${formatTime((duration - position).coerceAtLeast(0))}",
                        fontSize = 13.sp,
                        color = Color(0xFFDDDDDD),
                    )
                    // Portrait/landscape toggle pinned to the bottom-right
                    IconButton(
                        onClick = { toggleOrientation() },
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (isLandscape) {
                                Icons.Filled.FullscreenExit
                            } else {
                                Icons.Filled.Fullscreen
                            },
                            contentDescription = if (isLandscape) "竖屏播放" else "横屏播放",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { seekBy(-10000) },
                        enabled = isPrepared && duration > 0,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "向后 10 秒",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.width(24.dp))
                    FilledIconButton(
                        onClick = { togglePlay() },
                        enabled = isPrepared && duration > 0,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier.size(64.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(
                        onClick = { seekBy(10000) },
                        enabled = isPrepared && duration > 0,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "向前 10 秒",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        TimeEditDialog(
            initialMillis = position,
            durationMillis = duration,
            onConfirm = { millis ->
                player.seekTo(millis)
                position = millis
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false },
        )
    }
}
