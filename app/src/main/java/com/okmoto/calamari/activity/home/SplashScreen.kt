/**
 * Simple splash screen shown while routing decides what the first screen is.
 *
 * Methodology:
 * - Uses `LaunchedEffect(Unit)` to invoke `onReady()` immediately.
 * - Displays only the app launcher icon centered on the screen.
 */
package com.okmoto.calamari.permissions.screens

import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.okmoto.calamari.R
import com.okmoto.calamari.ui.theme.CalamariTheme

@Composable
fun SplashScreen(
    onReady: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onReady()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Adaptive launcher icons (mipmap XML) are not supported by painterResource;
        // ImageView resolves them the same way as the launcher.
        AndroidView(
            modifier = Modifier.size(120.dp),
            factory = { context ->
                ImageView(context).apply {
                    setImageResource(R.mipmap.ic_launcher)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = context.getString(R.string.app_name)
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    CalamariTheme {
        SplashScreen(onReady = {})
    }
}

