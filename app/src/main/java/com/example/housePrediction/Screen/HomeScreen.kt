package com.example.housePrediction.Screen

import android.content.Context
import android.content.res.AssetFileDescriptor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var rm by remember { mutableStateOf("") }
    var lstat by remember { mutableStateOf("") }
    var ptratio by remember { mutableStateOf("") }
    var prediction by remember { mutableStateOf<Float?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏠 Boston Housing Prediction", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = rm,
            onValueChange = { rm = it },
            label = { Text("RM") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = lstat,
            onValueChange = { lstat = it },
            label = { Text("LSTAT") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = ptratio,
            onValueChange = { ptratio = it },
            label = { Text("PTRATIO") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val rmF = rm.toFloatOrNull() ?: 0f
                        val lstatF = lstat.toFloatOrNull() ?: 0f
                        val ptratioF = ptratio.toFloatOrNull() ?: 0f

                        val result = runTFLiteModel(context, floatArrayOf(rmF, lstatF, ptratioF))
                        prediction = result
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Predict")
        }

        Spacer(Modifier.height(24.dp))

        prediction?.let {
            Text(
                text = "Predicted Price: ${String.format("%.2f", it)}k",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

// Load TFLite model from assets
fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
    val fileDescriptor: AssetFileDescriptor = context.assets.openFd(filename)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel: FileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

// Run TFLite prediction
fun runTFLiteModel(context: Context, input: FloatArray): Float {
    val tfliteModel = loadModelFile(context, "house_price_model.tflite")
    val interpreter = Interpreter(tfliteModel)
    //
    // Input shape [1,3], output shape [1,1]
    val inputArray = arrayOf(floatArrayOf(input[0], input[1], input[2]))
    val outputArray = Array(1) { FloatArray(1) }

    interpreter.run(inputArray, outputArray)
    interpreter.close()

    return outputArray[0][0]
}
