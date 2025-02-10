package com.codewithfk.arlearner.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.codewithfk.arlearner.R
import com.codewithfk.arlearner.util.Utils
import com.google.ar.core.Config
import com.google.ar.core.Frame
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

    // State to show/hide the scanning guide
    val isScanning = remember { mutableStateOf(true) }

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
            // Hide scanning guide when a plane is detected
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
                    }?.createAnchorOrNull()?.let {
                        val nodeModel = Utils.createAnchorNode(
                            engine = engine,
                            modelLoader = modelLoader,
                            materialLoader = materialLoader,
                            modelInstance = modelInstance,
                            anchor = it,
                            model = Utils.getModelForAlphabet(model),
                            onModelPlaced = { isScanning.value = false } // ✅ Hides guide when model is placed
                        )
                        childNodes += nodeModel
                    }
                }
            }
        )
    )

    // Overlay UI for scanning guide
    AnimatedScanningGuide(isScanning.value)
}

@Composable
fun AnimatedScanningGuide(isVisible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse // ✅ Fixed the unresolved reference
        ), label = "AlphaAnimation"
    )

    AnimatedVisibility(visible = isVisible) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.scan_icon), // ✅ Ensure this image is available
                contentDescription = "Scan for a plane",
                modifier = Modifier.size(150.dp),
                colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color.White.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Move your phone to detect a flat surface",
                fontSize = 18.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = alpha)
            )
        }
    }
}
