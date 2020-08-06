package com.example.candleshrine

import android.graphics.Point
import android.graphics.RectF
import android.util.Log

// Could this be a freestanding function?
class RectFTransformer {
    val TAG = "RectFTransformer"

    // quick, hardcoded fix - really should be done with a Matrix calculated once using values from
    // the loaded renders, and used for bitmap resizing too (i.e. BitmapManager.getCentreFitted
    fun transform(rectf: RectF, styleDisplayWidth: Int, styleDisplayHeight: Int): RectF {
        val fullDims = Point(1920, 2960) //hardcoded, yes
        val rAspectRatio = fullDims.x.toFloat() / fullDims.y
        val dstDims = Point(styleDisplayWidth, styleDisplayHeight)
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
        // one of these will be zero
        val marginAcross = (fullDims.x - grabDims.x) / 2
        val marginDown = (fullDims.y - grabDims.y) / 2
        rectf.left = (rectf.left - marginAcross) / scale
        rectf.right = (rectf.right - marginAcross) / scale
        rectf.top = (rectf.top - marginDown) / scale
        rectf.bottom = (rectf.bottom - marginDown) / scale
        return rectf
    }
}