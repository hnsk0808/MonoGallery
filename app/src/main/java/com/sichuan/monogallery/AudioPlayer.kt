package com.sichuan.monogallery

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

/**
 * Audio preview: plays a local audio file with the built-in [MediaPlayer].
 * Supports seeking via the progress slider, play/pause, ±10-second jumps, and
 * jumping to a typed time by tapping the current playback time.
 */
@Composable
fun AudioPlayer(file: File?, modifier: Modifier = Modifier) {
    val path = file?.absolutePath

    var duration by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val player = remember { MediaPlayer() }

    DisposableEffect(path) {
        if (path != null) {
            try {
                player.setDataSource(path)
                player.setOnPreparedListener { mp ->
                    duration = mp.duration
                    position = 0
                    isPrepared = true
                }
                player.setOnCompletionListener {
                    isPlaying = false
                    position = duration
                }
                player.setOnErrorListener { _, _, _ ->
                    loadError = true
                    isPrepared = false
                    true
                }
                // Prepare asynchronously: duration and the ready state are delivered via the listeners above
                player.prepareAsync()
            } catch (_: Exception) {
                loadError = true
            }
        } else {
            loadError = true
        }
        onDispose {
            try {
                player.release()
            } catch (_: Exception) {
                // Ignore the error if the player was already released
            }
        }
    }

    // Poll the playback position on a timer while playing; the coroutine cancels automatically on
    // leaving composition or when playback pauses
    LaunchedEffect(isPlaying) {
        while (isPlaying && isPrepared) {
            position = runCatching { player.currentPosition }.getOrDefault(position)
            delay(250)
        }
    }

    fun togglePlay() {
        if (!isPrepared) return
        if (isPlaying) {
            player.pause()
            position = runCatching { player.currentPosition }.getOrDefault(position)
            isPlaying = false
        } else {
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

    if (loadError) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("无法播放音频", color = ColorTextSecondary)
        }
        return
    }

    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(ColorSearchField, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "音频",
                tint = ColorAccent,
                modifier = Modifier.size(60.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "总时长 ${formatTime(duration)}",
            fontSize = 13.sp,
            color = ColorTextSecondary,
        )
        Spacer(Modifier.height(8.dp))
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
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTime(position),
                fontSize = 14.sp,
                color = ColorAccent,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable(enabled = isPrepared && duration > 0) { showTimeDialog = true },
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "剩余 ${formatTime((duration - position).coerceAtLeast(0))}",
                fontSize = 13.sp,
                color = ColorTextSecondary,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { seekBy(-10000) },
                enabled = isPrepared && duration > 0,
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay10,
                    contentDescription = "向后 10 秒",
                    tint = ColorTextPrimary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(24.dp))
            FilledIconButton(
                onClick = { togglePlay() },
                enabled = isPrepared && duration > 0,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ColorAccent,
                    contentColor = Color.White,
                ),
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(36.dp),
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
                    tint = ColorTextPrimary,
                    modifier = Modifier.size(32.dp),
                )
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

/** Dialog shown after tapping the playback time: enter a time (e.g. 1:30 or 90 seconds) and seek there on confirm. Shared by the audio and video players. */
@Composable
fun TimeEditDialog(
    initialMillis: Int,
    durationMillis: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(formatTime(initialMillis)) }
    val targetMillis = parseTimeToMillis(text)?.coerceIn(0, durationMillis)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转到") },
        text = {
            Column {
                Text(
                    text = "输入时间（如 1:30 或 90）",
                    fontSize = 12.sp,
                    color = ColorTextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("播放时间") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { targetMillis?.let(onConfirm) },
                enabled = targetMillis != null,
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** Formats milliseconds as `mm:ss` (or `h:mm:ss` once an hour is reached). Shared by the audio and video players. */
fun formatTime(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Parses user input into milliseconds, accepting bare `seconds`, `mm:ss`, or `h:mm:ss`; returns null on invalid input. */
private fun parseTimeToMillis(input: String): Int? {
    val parts = input.trim().split(":")
    return when (parts.size) {
        1 -> parts[0].toIntOrNull()?.takeIf { it >= 0 }?.times(1000)
        2 -> {
            val m = parts[0].toIntOrNull() ?: return null
            val s = parts[1].toIntOrNull() ?: return null
            if (m < 0 || s !in 0..59) return null
            (m * 60 + s) * 1000
        }
        3 -> {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val s = parts[2].toIntOrNull() ?: return null
            if (h < 0 || m !in 0..59 || s !in 0..59) return null
            (h * 3600 + m * 60 + s) * 1000
        }
        else -> null
    }
}
