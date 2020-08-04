package com.example.candleshrine

import android.util.Log
import kotlin.random.Random

class SmoothNoise(_wavelength: Int) {
    val TAG = "SmoothNoise"
    val wavelength = _wavelength.toLong() // change for convenience in calculations
    // latest and previous point are the ones that need to be interpolated between
    // given desired sampling point. They are always one wavelength apart.
    var latestPoint = wavelength.toLong()
    var previousPoint = 0L
    var latestVal = rnd()
    var previousVal = rnd()

    public fun sampleAt(x: Long): Float {
        // shift points till latest is at or ahead of x while previous is behind
        val tempLatest = latestPoint
        while((x - previousPoint) >= wavelength) {
            previousPoint = latestPoint
            latestPoint += wavelength
        }
        // if, due to sampling rate being lower than frequency of noise,
        // we've shifted more than one wavelength to reach the current
        // sampling point, then generate the random value for the previous point
        // too as it can't be copied from the random value at what was latestpoint
        val shift = (latestPoint - (tempLatest))
        if (shift > wavelength) {
            previousVal = rnd()
            Log.d(TAG, "Shifted more than one wavelength")
        }
        // otherwise if we only shifted one wavelength, shift the value along and
        // get one new random value
        else if (shift == wavelength) {
            previousVal = latestVal
            latestVal = rnd()
            Log.d(TAG, "Shifted one wavelength, new vals " + previousVal + " " + latestVal)
        }
        val linear: Float
        val t = ((x - previousPoint) / wavelength.toFloat())
        val remappedT = t * t * (3 - 2 * t)
        linear = lerp(previousVal, latestVal, remappedT)

        //test
        return linear
    }
    public fun sampleAt(x: Int): Float {
        return sampleAt(x.toLong())
    }
    fun rnd(): Float {
        return Random.nextFloat()
    }

    fun lerp(low: Float, high: Float, t: Float): Float {
        return low * (1 - t) + high * t
    }


}