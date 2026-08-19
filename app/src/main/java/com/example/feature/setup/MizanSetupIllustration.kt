package com.example.feature.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.MizanShapes

/**
 * Mizan Setup Hero Card Container.
 * Embeds the exact transparent vector-line illustration as a native Android drawable resource (R.drawable.mizan_setup_house).
 * Zero latency, 100% instant native rendering, perfectly fitted and centered.
 */
@Composable
fun MizanSetupIllustration(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFD6F355)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(MizanShapes.HeroPanel)
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("mizan_setup_illustration"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.mizan_setup_house),
            contentDescription = "Mizan House Setup Illustration",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.88f)
                .testTag("mizan_setup_house_image")
        )
    }
}
