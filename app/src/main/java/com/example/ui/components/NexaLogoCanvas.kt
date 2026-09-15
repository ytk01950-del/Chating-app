package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dedicated Jetpack Compose Canvas component drawing the custom white geometric block "N" logo.
 * Implements the exact vector geometry with:
 * 1. Left vertical block with top-left rounded corner arc.
 * 2. Right vertical block with bottom-right rounded corner arc.
 * 3. Diagonal thick connection block with top-right and bottom-left sharp cutout gaps replicating negative space cutouts.
 * 4. Framing ribbons and chamfered corner tabs.
 *
 * Uses dynamic relative canvas sizing (size.width, size.height) with solid white fill on pure black canvas.
 */
@Composable
fun CustomNexaLogo(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    logoColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(16.dp),
    hasBorder: Boolean = false,
    borderColor: Color = Color(0x33FFFFFF),
    borderWidth: Dp = 1.dp,
    testTag: String = "custom_nexa_logo"
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (hasBorder) Modifier.border(borderWidth, borderColor, shape)
                else Modifier
            )
            .testTag(testTag)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Top-Right Accent Corner Tab
            val topRightTab = Path().apply {
                moveTo(w * 0.535f, h * 0.255f)
                lineTo(w * 0.690f, h * 0.255f)
                cubicTo(
                    w * 0.745f, h * 0.255f,
                    w * 0.785f, h * 0.295f,
                    w * 0.785f, h * 0.350f
                )
                lineTo(w * 0.785f, h * 0.475f)
                lineTo(w * 0.565f, h * 0.255f)
                close()
            }
            drawPath(path = topRightTab, color = logoColor)

            // 2. Bottom-Left Accent Corner Tab (180° rotation of Top-Right Tab)
            val bottomLeftTab = Path().apply {
                moveTo(w * 0.465f, h * 0.745f)
                lineTo(w * 0.310f, h * 0.745f)
                cubicTo(
                    w * 0.255f, h * 0.745f,
                    w * 0.215f, h * 0.705f,
                    w * 0.215f, h * 0.650f
                )
                lineTo(w * 0.215f, h * 0.525f)
                lineTo(w * 0.435f, h * 0.745f)
                close()
            }
            drawPath(path = bottomLeftTab, color = logoColor)

            // 3. Top-Left Outer Ribbon with outer squircle contour & crescent swoop
            val topLeftRibbon = Path().apply {
                moveTo(w * 0.215f, h * 0.500f)
                lineTo(w * 0.215f, h * 0.350f)
                cubicTo(
                    w * 0.215f, h * 0.295f,
                    w * 0.255f, h * 0.255f,
                    w * 0.350f, h * 0.255f
                )
                lineTo(w * 0.430f, h * 0.255f)
                cubicTo(
                    w * 0.475f, h * 0.255f,
                    w * 0.510f, h * 0.310f,
                    w * 0.525f, h * 0.360f
                )
                cubicTo(
                    w * 0.475f, h * 0.305f,
                    w * 0.385f, h * 0.295f,
                    w * 0.330f, h * 0.330f
                )
                cubicTo(
                    w * 0.295f, h * 0.350f,
                    w * 0.285f, h * 0.390f,
                    w * 0.285f, h * 0.440f
                )
                lineTo(w * 0.285f, h * 0.500f)
                close()
            }
            drawPath(path = topLeftRibbon, color = logoColor)

            // 4. Bottom-Right Outer Ribbon (180° rotation of Top-Left Ribbon)
            val bottomRightRibbon = Path().apply {
                moveTo(w * 0.785f, h * 0.500f)
                lineTo(w * 0.785f, h * 0.650f)
                cubicTo(
                    w * 0.785f, h * 0.705f,
                    w * 0.745f, h * 0.745f,
                    w * 0.650f, h * 0.745f
                )
                lineTo(w * 0.570f, h * 0.745f)
                cubicTo(
                    w * 0.525f, h * 0.745f,
                    w * 0.490f, h * 0.690f,
                    w * 0.475f, h * 0.640f
                )
                cubicTo(
                    w * 0.525f, h * 0.695f,
                    w * 0.615f, h * 0.705f,
                    w * 0.670f, h * 0.670f
                )
                cubicTo(
                    w * 0.705f, h * 0.650f,
                    w * 0.715f, h * 0.610f,
                    w * 0.715f, h * 0.560f
                )
                lineTo(w * 0.715f, h * 0.500f)
                close()
            }
            drawPath(path = bottomRightRibbon, color = logoColor)

            // 5. Left Vertical Block with top-left rounded corner arc
            val leftVerticalBlock = Path().apply {
                moveTo(w * 0.285f, h * 0.715f)
                lineTo(w * 0.435f, h * 0.715f)
                lineTo(w * 0.435f, h * 0.525f)
                lineTo(w * 0.335f, h * 0.425f)
                cubicTo(
                    w * 0.335f, h * 0.375f,
                    w * 0.370f, h * 0.335f,
                    w * 0.435f, h * 0.335f
                )
                cubicTo(
                    w * 0.470f, h * 0.335f,
                    w * 0.505f, h * 0.355f,
                    w * 0.530f, h * 0.380f
                )
                cubicTo(
                    w * 0.495f, h * 0.345f,
                    w * 0.450f, h * 0.315f,
                    w * 0.390f, h * 0.315f
                )
                cubicTo(
                    w * 0.315f, h * 0.315f,
                    w * 0.285f, h * 0.365f,
                    w * 0.285f, h * 0.435f
                )
                close()
            }
            drawPath(path = leftVerticalBlock, color = logoColor)

            // 6. Right Vertical Block with bottom-right rounded corner arc (180° rotation of Left Block)
            val rightVerticalBlock = Path().apply {
                moveTo(w * 0.715f, h * 0.285f)
                lineTo(w * 0.565f, h * 0.285f)
                lineTo(w * 0.565f, h * 0.475f)
                lineTo(w * 0.665f, h * 0.575f)
                cubicTo(
                    w * 0.665f, h * 0.625f,
                    w * 0.630f, h * 0.665f,
                    w * 0.565f, h * 0.665f
                )
                cubicTo(
                    w * 0.530f, h * 0.665f,
                    w * 0.495f, h * 0.645f,
                    w * 0.470f, h * 0.620f
                )
                cubicTo(
                    w * 0.505f, h * 0.655f,
                    w * 0.550f, h * 0.685f,
                    w * 0.610f, h * 0.685f
                )
                cubicTo(
                    w * 0.685f, h * 0.685f,
                    w * 0.715f, h * 0.635f,
                    w * 0.715f, h * 0.565f
                )
                close()
            }
            drawPath(path = rightVerticalBlock, color = logoColor)

            // 7. Diagonal Thick Connection Block with top-right and bottom-left sharp cutout gaps
            val diagonalBlock = Path().apply {
                moveTo(w * 0.335f, h * 0.425f)
                lineTo(w * 0.435f, h * 0.325f)
                lineTo(w * 0.665f, h * 0.575f)
                lineTo(w * 0.565f, h * 0.675f)
                close()
            }
            drawPath(path = diagonalBlock, color = logoColor)
        }
    }
}
