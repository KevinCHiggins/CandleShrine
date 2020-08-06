/*
 * Copyright (C) 2020 Kevin Higgins
 * @author Kevin Higgins
 * This class provides utility functions for bitmap handling. It can
 * save and load to the app's private storage, and also can
 * load a bitmap from the assets directory and fit it, centred, to a given rectangle
 *  - Kevin Higgins 05/08/20
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.candleshrine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

// This should have downsampling as in the developer guide...
// Also: try-catch; rename variables
class BitmapManager {
    val TAG = "BitmapLoader"
    fun save(context: Context, bitmap: Bitmap, filename: String) {
        try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            val fileoutputStream: FileOutputStream =
                context.openFileOutput(filename, Context.MODE_PRIVATE)
            fileoutputStream.write(bytes.toByteArray())
            fileoutputStream.close()
            Log.d(TAG, "Bitmap saved to filename " + filename)
        } catch (ioe: IOException) {
            Log.d(TAG, "Couldn't save to file")
        }
    }

    fun load(context: Context, filename: String): Bitmap {

        val fileInputStream = context.openFileInput(filename)
        return BitmapFactory.decodeStream(fileInputStream)
    }
    fun getCentreFitted(context: Context, assetName: String, width: Int, height: Int): Bitmap {
        Log.d(TAG, "Trying to load and then resize asset to " + width + "x" + height)
        var asset = context.assets.open(assetName)
        // load metadata only to save full bitmap dimensions
        val bitmapOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(asset, null, bitmapOptions)
        val fullDims = Point(bitmapOptions.outWidth, bitmapOptions.outHeight)

        Log.d(TAG, "Bitmap metadata width: " + fullDims.x + ", height: " + fullDims.y)
        val rAspectRatio = fullDims.x.toFloat() / fullDims.y
        val dstDims = Point(width, height)
        val dstAspectRatio = dstDims.x.toFloat() / dstDims.y
        val grabDims: Point = Point()
        var scale: Float

        //(note that in case of equal aspect ratios it
        // doesn't matter which of these clauses would run, I choose first arbitrarily)...
        // if destination aspect ratio is narrower or equal
        if (dstAspectRatio <= rAspectRatio) {
            scale = fullDims.y.toFloat() / dstDims.y
            grabDims.x = (dstDims.x * scale).toInt()
            grabDims.y = (dstDims.y * scale).toInt()
            Log.d(TAG, "Desired aspect ratio " + dstAspectRatio + " is narrower than orig " + rAspectRatio)
        }
        // if destination is broader
        else {
            scale = fullDims.x.toFloat() / dstDims.x
            grabDims.x = (dstDims.x * scale).toInt()
            grabDims.y = (dstDims.y * scale).toInt()
            Log.d(TAG, "Desired aspect ratio " + dstAspectRatio + " is broader than orig " + rAspectRatio)
        }
        Log.d(TAG, "Grabbing " + grabDims.x + "x" + grabDims.y + " portion from full bitmap")

        // workaround for this bug https://stackoverflow.com/questions/39316069/bitmapfactory-decodestream-from-assets-returns-null-on-android-7
        // is to reopen the stream
        asset = context.assets.open(assetName)

        val bitmapFull = BitmapFactory.decodeStream(asset)
        Log.d("TAG", "Full bmp x: " + bitmapFull.width + ", y: " + bitmapFull.height)
        Log.d(TAG, "Got bmp")
        val x = (fullDims.x - grabDims.x) / 2
        val y = (fullDims.y - grabDims.y) / 2
        Log.d("getCroppedAndScaled", "Crop x: " + x + ", y: " + y + ", width: " + grabDims.x + " height: " + grabDims.y)
        val bitmapCropped = Bitmap.createBitmap(bitmapFull, x, y, grabDims.x, grabDims.y)
        Log.d("getCroppedAndScaled", "Actual cropped dimensions x: " + bitmapCropped.width + ", y: " + bitmapCropped.height)
        assert(bitmapCropped != null)
        bitmapFull.recycle() // for memory usage
        val bitmapCroppedAndScaled = Bitmap.createScaledBitmap(
            // use roundToInt() so as not to drop a pixel e.g. for 720x1024 final res
            bitmapCropped, (grabDims.x / scale).roundToInt(), (grabDims.y / scale).roundToInt(), true
        )
        bitmapCropped.recycle()
        Log.d("getCroppedAndScaled", "Bitmap resized to x " + bitmapCroppedAndScaled.width + " y " + bitmapCroppedAndScaled.height)
        return bitmapCroppedAndScaled
    }
}