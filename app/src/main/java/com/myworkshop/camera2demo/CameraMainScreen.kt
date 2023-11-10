package com.myworkshop.camera2demo

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.myworkshop.camera2demo.ImageCaptureBuilder.getImageCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen() {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
//    val isCapturing by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera 2 Jetpack Compose") },
                actions = {
                    IconButton(
                        onClick = {

                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .scrollable(scrollState, Orientation.Vertical)
        ) {
            // Display the camera preview
            CameraPreview(modifier = Modifier.padding(paddingValues.calculateTopPadding()))

            Button(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    takePhoto(imageCapture = getImageCapture(), context = context) { bitmap ->
                        imageBitmap = bitmap.asImageBitmap()
                    }
                }
            }) {
                Text(text = "Take photo")
            }
            imageBitmap?.let {
                Image(bitmap = imageBitmap!!, contentDescription = null)
            }

        }
    }
}

/**
 * base 64 string
 */
private fun takePhoto(
    imageCapture: ImageCapture,
    context: Context,
    onSuccessCallBack: (Bitmap) -> Unit
) {
    // Get a stable reference of the modifiable image capture use case

    // Create time stamped name and MediaStore entry.
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }

    // Create output options object which contains file + metadata
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        .build()

    // Set up image capture listener, which is triggered after photo has
    // been taken
    var result: ImageBitmap? = null

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val msg = "Photo capture succeeded: ${output.savedUri}"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }
        }
    )

    imageCapture.takePicture(Executors.newSingleThreadExecutor(), object :
        ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
            onSuccessCallBack(image.toBitmap())
        }
    })
}


@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set up the camera preview
    val previewView = PreviewView(context)
    val preview = androidx.camera.core.Preview.Builder().build()
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Bind the camera preview to the PreviewView
    preview.setSurfaceProvider(previewView.surfaceProvider)

    // Use the ProcessCameraProvider to bind the lifecycle of the camera to the lifecycle owner
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    AndroidView(
        factory = { previewView },
        modifier = modifier
            .height(300.dp)
            .aspectRatio(0.8f)
    ) { preview_view ->
        cameraProviderFuture.addListener({
            // CameraProvider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Unbind any previous use-cases
            cameraProvider.unbindAll()

            // Bind the new use-case with the camera selector and preview
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                getImageCapture()
            )
        }, ContextCompat.getMainExecutor(context))
    }
}