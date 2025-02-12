package com.codewithfk.arlearner.ui.screens

import android.content.Context
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.ui.layout.ContentScale


import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.codewithfk.arlearner.R
import com.codewithfk.arlearner.util.Utils
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView

@Composable
fun ARScreen(navController: NavController, model: String) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine = engine)
    val materialLoader = rememberMaterialLoader(engine = engine)
    val cameraNode = rememberARCameraNode(engine = engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine = engine)
    val collisionSystem = rememberCollisionSystem(view = view)
    val planeRenderer = remember { mutableStateOf(true) }
    val modelInstance = remember { mutableListOf<ModelInstance>() }
    val trackingFailureReason = remember { mutableStateOf<TrackingFailureReason?>(null) }
    val frame = remember { mutableStateOf<Frame?>(null) }

    // State variables
    val isScanning = remember { mutableStateOf(true) }
    val isSurfaceLocked = remember { mutableStateOf(false) }
    val lockedAnchor = remember { mutableStateOf<Anchor?>(null) }
    val lockedPosition = remember { mutableStateOf<Pose?>(null) } // Stores locked surface position

    ARScene(
        modifier = Modifier.fillMaxSize(),
        childNodes = childNodes,
        engine = engine,
        view = view,
        modelLoader = modelLoader,
        collisionSystem = collisionSystem,
        planeRenderer = planeRenderer.value,
        cameraNode = cameraNode,
        materialLoader = materialLoader,
        onTrackingFailureChanged = {
            trackingFailureReason.value = it
        },
        onSessionUpdated = { _, updatedFrame ->
            frame.value = updatedFrame
            if (updatedFrame.camera.trackingState.name == "TRACKING") {
                isScanning.value = false
            }
        },
        sessionConfiguration = { session, config ->
            config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                true -> Config.DepthMode.AUTOMATIC
                else -> Config.DepthMode.DISABLED
            }
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        },
        onGestureListener = rememberOnGestureListener(
            onSingleTapConfirmed = { e: MotionEvent, node: Node? ->
                if (node == null) {
                    val hitTestResult = frame.value?.hitTest(e.x, e.y)
                    hitTestResult?.firstOrNull {
                        it.isValid(depthPoint = false, point = false)
                    }?.let { hitResult ->
                        if (!isSurfaceLocked.value) {
                            // First tap: Lock the surface and display indicator
                            lockedAnchor.value = hitResult.createAnchorOrNull()
                            lockedPosition.value = hitResult.hitPose // Store locked position
                            isSurfaceLocked.value = true


                            planeRenderer.value = false  // Hide white dots

                        } else {
                            // Second tap: Place model on locked position
                            lockedAnchor.value?.let { anchor ->
                                val nodeModel = Utils.createAnchorNode(
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    materialLoader = materialLoader,
                                    modelInstance = modelInstance,
                                    anchor = anchor,
                                    model = Utils.getModelForAlphabet(model),
                                    onModelPlaced = { isScanning.value = false }
                                )
                                childNodes += nodeModel
                                lockedPosition.value = null // Hide circular marker after model placement
                            }
                        }
                    }
                }
            }
        )
    )

    // Overlay UI for scanning guide
    AnimatedScanningGuide(isScanning.value)

    // Draw a circular indicator at locked surface position
    lockedPosition.value?.let { pose ->
        LockedSurfaceIndicator(pose)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AnimatedScanningGuide(isVisible: Boolean) {
    if (!isVisible) return

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ Load Animated GIF using Glide (Bug-Free)
        GlideImage(
            model = "file:///android_asset/Untitled-3.gif", // ✅ Load GIF from assets
            contentDescription = "Scanning Animation",
            contentScale = ContentScale.Fit, // 🔹 Ensure correct scaling
            modifier = Modifier.size(250.dp) // 🔹 Resize GIF
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Move your phone to detect a flat surface",
            fontSize = 18.sp,
            color = Color.White
        )
    }
}



@Composable
fun LockedSurfaceIndicator(pose: Pose) {
    val pulseAnimation = rememberInfiniteTransition(label = "PulseEffect")
    val animatedSize by pulseAnimation.animateFloat(
        initialValue = 80f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.Center)
    ) {
        Canvas(modifier = Modifier.size(animatedSize.dp)) {
            drawCircle(
                color = Color(0xFF4CAF50).copy(alpha = 0.5f), // 🔹 Greenish glow
                radius = size.minDimension / 2
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f), // 🔹 Outer glow effect
                radius = size.minDimension / 2 + 5f, // Slightly larger than the inner circle
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f) // Outlined stroke
            )
        }
    }
}

