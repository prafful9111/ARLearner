package com.codewithfk.arlearner.util

import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode

object Utils {
    val alphabets = mapOf(
        "Bike" to "yamaha_rx-king.glb",
        "Laptop" to "laptop.glb",
        "Car" to "car.glb",
        "Commando" to "commando.glb",
        "Tank" to "tank.glb",
        "Model" to "model.glb",
        "G" to "goat.glb",
        "H" to "hen.glb",
        "I" to "icecream.glb",
        "J" to "jug.glb",
        "K" to "kite.glb",
        "L" to "lion.glb",
        "M" to "monkey.glb",
        "N" to "nest.glb",
        "O" to "owl.glb",
        "P" to "parrot.glb",
        "Q" to "quail.glb",
        "R" to "rat.glb",
        "S" to "ship.glb",
        "T" to "telephone.glb",
        "U" to "umbrella.glb",
        "V" to "van.glb",
        "W" to "watch.glb",
        "X" to "xylophone.glb",
        "Y" to "yacht.glb",
        "Z" to "zebra.glb"
    )


    fun getModelForAlphabet(alphabet: String): String {
        val modelName = alphabets[alphabet] ?: error("Model not found")
        return "models/$modelName"
    }

    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        materialLoader: MaterialLoader,
        modelInstance: MutableList<ModelInstance>,
        anchor: Anchor,
        model: String,
        onModelPlaced: () -> Unit // ✅ New callback
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = ModelNode(
            modelInstance = modelInstance.apply {
                if (isEmpty()) {
                    this += modelLoader.createInstancedModel(model, 10)
                }
            }.removeLast(),
            scaleToUnits = 0.2f
        ).apply {
            isEditable = true
        }

        anchorNode.addChildNode(modelNode)
        onModelPlaced() // ✅ Notify ARScreen that a model was placed

        return anchorNode
    }


    fun randomModel(): Pair<String, String> {
        val randomIndex = (0 until alphabets.size).random()
        val alphabet = alphabets.keys.elementAt(randomIndex)
        return Pair(alphabet, getModelForAlphabet(alphabet))
    }
}