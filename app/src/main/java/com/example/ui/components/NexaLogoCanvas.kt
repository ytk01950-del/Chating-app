package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * App Logo component displaying the exact Nexa logo image from project resources.
 * Preserves original logo aspect ratio, centered on a pure black background.
 */
@Composable
fun CustomNexaLogo(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    logoColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(0.dp),
    hasBorder: Boolean = false,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp,
    testTag: String = "custom_nexa_logo"
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (hasBorder) Modifier.clip(shape).border(borderWidth, borderColor, shape)
                else Modifier
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.nexa_image),
            contentDescription = "Nexa Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
