/**
 * Animated badge UI that reflects the current overlay [com.okmoto.calamari.overlay.ListeningState].
 *
 * Methodology:
 * - Maps each listening state to a dedicated animation spec.
 * - Keeps animation selection logic local so the surrounding Compose bubble
 *   stays a thin layout wrapper.
 */
package com.okmoto.calamari.overlay.compose

import androidx.annotation.FloatRange
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.okmoto.calamari.overlay.ListeningState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Static geometric description of the badge shape.
 */
data class BadgeShapeSpec(
    val bumpCount: Int,
    @param:FloatRange(from = 0.0, to = 1.0) val innerRadiusFraction: Float,
    val outerRadiusFraction: Float = 1f,
)

private enum class RotationMode {
    None,
    Slow,
    Medium,
    Fast,
    Reverse,
}

private data class BubbleAnimationSpec(
    val shapeSpec: BadgeShapeSpec,
    val rotationMode: RotationMode,
)

/**
 * Map the audio listening [ListeningState] into a visual/animation profile.
 */
private fun animationSpecFor(state: ListeningState): BubbleAnimationSpec =
    when (state) {
        // Idle / error: calm, almost circular, no rotation.
        ListeningState.IDLE  -> BubbleAnimationSpec(
            shapeSpec = BadgeShapeSpec(bumpCount = 10, innerRadiusFraction = 0.9f),
            rotationMode = RotationMode.None,
        )
        ListeningState.ERROR -> BubbleAnimationSpec(
            shapeSpec = BadgeShapeSpec(bumpCount = 10, innerRadiusFraction = 0.9f),
            rotationMode = RotationMode.Reverse,
        )

        // Normal listening / idle-with-context states: moderate star + medium rotation.
        ListeningState.AWAKE -> BubbleAnimationSpec(
            shapeSpec = BadgeShapeSpec(bumpCount = 10, innerRadiusFraction = 0.9f),
            rotationMode = RotationMode.Slow,
        )

        ListeningState.IDLE_TITLE,
        ListeningState.IDLE_SEND -> BubbleAnimationSpec(
            shapeSpec = BadgeShapeSpec(bumpCount = 10, innerRadiusFraction = 0.9f),
            rotationMode = RotationMode.Medium,
        )

        // Awaiting concrete input: spikier and faster than the above.
        ListeningState.AWAITING_EVENT,
        ListeningState.AWAITING_TITLE -> BubbleAnimationSpec(
            shapeSpec = BadgeShapeSpec(bumpCount = 12, innerRadiusFraction = 0.85f),
            rotationMode = RotationMode.Fast,
        )
    }

/**
 * Custom Shape that draws a scalloped / wavy badge, similar to a circular award
 * with semicircular humps around the edge.
 */
private class StarBadgeShape(
    private val spec: BadgeShapeSpec,
) : Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = min(size.width, size.height) / 2f

        // Inner radius fraction controls how pronounced the humps are.
        // 0.9f -> almost circle, 0.5f -> strong scallops.
        val amplitudeFraction = (1f - spec.innerRadiusFraction).coerceIn(0f, 0.5f)

        // We approximate the scalloped edge by sampling many points around the circle
        // and modulating the radius with a cosine at spikeCount frequency.
        val samplesPerLobe = 32
        val totalSamples = spec.bumpCount * samplesPerLobe
        val angleStep = (2f * PI).toFloat() / totalSamples

        var angle = -PI.toFloat() / 2f // start at top
        for (i in 0..totalSamples) {
            val lobePhase = cos(spec.bumpCount * angle)
            val radius = baseRadius * (1f - amplitudeFraction * 0.5f + amplitudeFraction * 0.5f * lobePhase)
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            angle += angleStep
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Modifier that applies a star/badge shape and rotation animation based on [ListeningState].
 */
fun Modifier.animatedBadgeFor(
    state: ListeningState,
    backgroundColor: Color,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animatedBadgeFor"
        properties["state"] = state
    }
) {
    val targetSpec = remember(state) { animationSpecFor(state) }

    // Animate bumps for smooth morphing when state changes.
    val animatedInnerRadius by animateFloatAsState(
        targetValue = targetSpec.shapeSpec.innerRadiusFraction,
        animationSpec = tween(durationMillis = 500),
        label = "badgeBumps",
    )

    val shape = remember(targetSpec.shapeSpec.bumpCount, animatedInnerRadius) {
        StarBadgeShape(
            spec = targetSpec.shapeSpec.copy(innerRadiusFraction = animatedInnerRadius),
        )
    }

    val rotationDegrees: Float = when (targetSpec.rotationMode) {
        RotationMode.None -> 0f
        RotationMode.Slow -> {
            val infiniteTransition = rememberInfiniteTransition(label = "badgeRotationSlow")
            val animated by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 7000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "rotationSlow",
            )
            animated
        }
        RotationMode.Medium -> {
            val infiniteTransition = rememberInfiniteTransition(label = "badgeRotationMedium")
            val animated by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 5000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "rotationMedium",
            )
            animated
        }
        RotationMode.Fast -> {
            val infiniteTransition = rememberInfiniteTransition(label = "badgeRotationFast")
            val animated by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "rotationFast",
            )
            animated
        }
        RotationMode.Reverse -> {
            val infiniteTransition = rememberInfiniteTransition(label = "badgeRotationReverse")
            val animated by infiniteTransition.animateFloat(
                initialValue = 360f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 7000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "rotationReverse",
            )
            animated
        }
    }

    this.drawRotatingBadgeBackground(shape, rotationDegrees, backgroundColor)
}

/**
 * Draws the rotating badge background (shape) first, then the normal child content on top.
 * Only the badge background/clip rotates; the inner content (month/day text) remains upright.
 */
private fun Modifier.drawRotatingBadgeBackground(
    shape: Shape,
    rotationDegrees: Float,
    backgroundColor: Color,
): Modifier = this.then(
    Modifier.drawWithContent {
        // 1) Draw the rotating badge background using the shape path.
        when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Generic -> {
                drawIntoCanvas {
                    rotate(rotationDegrees) {
                        clipPath(outline.path) {
                            drawRect(backgroundColor)
                        }
                    }
                }
            }
            else -> {
                // Fallback for non-path outlines: simple rotating rect.
                drawIntoCanvas {
                    rotate(rotationDegrees) {
                        drawRect(backgroundColor)
                    }
                }
            }
        }

        // 2) Draw the child composable content (text, etc.) unrotated on top.
        this@drawWithContent.drawContent()
    }
)

