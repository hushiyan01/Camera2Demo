package com.myworkshop.camera2demo

import androidx.camera.core.ImageCapture

object ImageCaptureBuilder {

    private lateinit var imageCapture: ImageCapture
    fun getImageCapture():ImageCapture{
        if(!this::imageCapture.isInitialized){
            imageCapture = ImageCapture.Builder().build()
        }
        return imageCapture
    }
}