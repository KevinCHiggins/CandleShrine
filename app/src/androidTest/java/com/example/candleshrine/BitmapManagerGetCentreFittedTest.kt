package com.example.candleshrine

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class BitmapManagerGetCentreFittedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().context
        val bmm = BitmapManager()
        val TAG = "BitmapManagerGetCentreFittedTest"
        val dstWidth = 720
        val dstHeight = 1280
        // taller and wider
        var bm = bmm.getCentreFitted(appContext, "800x1300.png", dstWidth, dstHeight)
        assert(bm.width == dstWidth)
        assert(bm.height == dstHeight)
        // shorter and narrower
        bm = bmm.getCentreFitted(appContext, "400x650.png", dstWidth, dstHeight)
        assert(bm.width == dstWidth)
        assert(bm.height == dstHeight)
        // taller and narrower
        bm = bmm.getCentreFitted(appContext, "400x1300.png", dstWidth, dstHeight)
        assert(bm.width == dstWidth)
        assert(bm.height == dstHeight)
        // wider and narrower
        bm = bmm.getCentreFitted(appContext, "800x650.png", dstWidth, dstHeight)
        assert(bm.width == dstWidth)
        assert(bm.height == dstHeight)
        // much taller and much wider
        bm = bmm.getCentreFitted(appContext, "1600x2600.png", dstWidth, dstHeight)
        assert(bm.width == dstWidth)
        assert(bm.height == dstHeight)

        /*
        val fromNone = bmm.getCentreFitted(appContext, "", dstWidth, dstHeight)
        assert(fromTallerAndWider.width == dstWidth)
        assert(fromTallerAndWider.height == dstHeight)
        fromNone.recycle()
         */

    }
}