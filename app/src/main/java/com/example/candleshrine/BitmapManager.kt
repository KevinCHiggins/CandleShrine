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
        val viewDims = Point(width, height)
        val viewAspectRatio = viewDims.x.toFloat() / viewDims.y
        val grabDims: Point = Point()
        var scale: Float

        if (viewAspectRatio <= rAspectRatio) { // if they are exactly equal, either dimension could be used to scale
            scale = fullDims.y.toFloat() / viewDims.y
            grabDims.x = (viewDims.x * scale).toInt()
            grabDims.y = (viewDims.y * scale).toInt()
            Log.d(TAG, "Desired aspect ratio " + viewAspectRatio + " is narrower than orig " + rAspectRatio)
        }
        else {
            scale = fullDims.x.toFloat() / viewDims.x
            grabDims.x = (viewDims.x * scale).toInt()
            grabDims.y = (viewDims.y * scale).toInt()
            Log.d(TAG, "Desired aspect ratio " + viewAspectRatio + " is broader than orig " + rAspectRatio)
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