package com.sichuan.monogallery

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Generic fullscreen preview layout shared by the video, image, and PDF viewers.
 *
 * The content fills the whole screen (including the area behind the title bar), and the title
 * bar floats on top as a transparent overlay with a dark gradient scrim. The overlay fades in
 * and out with [chromeVisible], which the caller owns and keeps in sync with whatever control
 * surface the specific viewer shows at the bottom (video transport, PDF toolbar, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPreviewScreen(
    title: String,
    onBack: () -> Unit,
    chromeVisible: Boolean,
    onChromeVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {},
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                OverlayTopBar(title = title, onBack = onBack)
            }
        }
    }
}

/** Floating title bar shared by the fullscreen viewers: transparent background, top dark gradient, white title and back icon. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverlayTopBar(title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    // Resolve in composable context; onClick below is non-composable and cannot call composables
    val componentActivity = remember(context) {
        context.findActivity() as? ComponentActivity
    }
    TopAppBar(
        title = {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = title,
                    maxLines = 1,
                    softWrap = false,
                    color = Color.White,
                )
            }
        },
        navigationIcon = {
            // Route through the dispatcher so viewer-specific back handling also applies (the
            // video player returns landscape to portrait and stops playback before navigating).
            IconButton(onClick = {
                if (componentActivity != null) {
                    componentActivity.onBackPressedDispatcher.onBackPressed()
                } else {
                    onBack()
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
        ),
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(Color(0xB3000000), Color.Transparent),
            ),
        ),
    )
}
