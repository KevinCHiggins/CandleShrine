/*
* Copyright (C) 2020 Kevin Higgins
* @author Kevin Higgins
* This class affords a simulation of striking a match off the side of a matchbox
* using a standard GestureDetector. When the match has been "lit", it launches
* the FullscreenShrineActivity.
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

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import kotlinx.android.synthetic.main.activity_strike_match.*

// This activity is opened by EditTextsActivity, and is over MainMenuActivity and EditTextsActivity
// in the stack. When it finishes, it also finishes EditTextsActivity, and opens FullscreenShrineActivity.
class StrikeMatchActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    val TAG = "SMA"
    lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strike_match)
        gestureDetector = GestureDetector(this, this)
    }
    fun launchFullscreenShrine() {
        Log.d(TAG, "Launching fullscreen shrine")
        val fullscreenShrine = Intent()
        fullscreenShrine.putExtra(getString(R.string.intent_key_from_candle_lighting), true)
        fullscreenShrine.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".FullscreenShrineActivity")))
        startActivity(fullscreenShrine)
        finish()
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        Log.d(TAG, "velocityX: " + velocityX + ", velocityY: " + velocityY)
        // gotta get enough for the match to catch fire
        if (Math.abs(velocityX) > 300 && Math.abs(e1!!.x - e2!!.x) > 50) {

            launchFullscreenShrine()
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {

    }
}