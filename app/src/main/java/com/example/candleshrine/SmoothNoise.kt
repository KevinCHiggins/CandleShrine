/*
* Copyright (C) 2020 Kevin Higgins
* @author Kevin Higgins
* This class returns smoothed interpolated noise at a given frequency, assuming that the
* time variable x will always advance (that is, it does not save previous
* values from the sequence). The design of this class and the formulae for
* linear interpolation and smoothstep functions are taken from the following
* tutorial: https://www.scratchapixel.com/lessons/procedural-generation-virtual-worlds/procedural-patterns-noise-part-1/creating-simple-1D-noise
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
        // create a float value between 0 and 1 (representing previous point and latest point)
        // for t, the sampling position
        val t = ((x - previousPoint) / wavelength.toFloat())
        // remap T with a SmoothStep formula so that it changes more slowly at the start and end
        val remappedT = t * t * (3 - 2 * t)
        // linearly interpolate between the two given noise values using remappedT as the
        // position
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