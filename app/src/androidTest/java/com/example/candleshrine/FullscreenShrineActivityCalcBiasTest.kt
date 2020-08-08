package com.example.candleshrine

import android.os.Looper
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
class FullscreenShrineActivityCalcBiasTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().context
        Looper.prepare()
        val fsa = FullscreenShrineActivity()
        assert(fsa.calcBias(720, 1232) > fsa.calcBias(480, 764))

        assert(fsa.calcBias(1080, 2022) > fsa.calcBias(720, 1232))

    }
}