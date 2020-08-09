/*
* Copyright (C) 2020 Kevin Higgins
* @author Kevin Higgins
* This class displays the shrine, with candle flame and glow if it
* calculates the candle is lit (based on a timestamp of when it was lit).
* The flickering of the glow is modulated using three octaves of
* smoothed 1 dimensional noise. (These modify the alpha value of an
* overlaid image of the shrine at full brightness.) Each octave is an instance of the
* SmoothNoise class I wrote. Combining the octaves like this is
* inspired by Ken Perlin's "Perlin Noise" although I don't follow his rules
* exactly.
* To light the sacred image, a multiply blend mode is used.
* Three different MediaDecoderAlphaMovieViews are used to hold different
* aspects of the candle flame.
* Finally, the actual lighting of the candle takes place in this class
* when the the timestamp is saved by the initialiseCandleStatus() function.
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

import android.animation.TimeAnimator
import android.content.ComponentName
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_fullscreen_shrine_higher.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt

// Activities flow
// There are no interactions available from this activity. The user taps the back button to get
// back to the main menu, which will always be below this in the stack.
class FullscreenShrineActivity : AppCompatActivity(), TimeAnimator.TimeListener {
    val paintSacredImage = Paint()
    val TAG = "FSActivity"
    val timer = TimeAnimator()
    var lastKnownCandleTimestamp = 0L
    var candleIsLit = false
    var styleIndex = 0
    val noiseOctave1 = SmoothNoise(400) // in ms
    val noiseOctave2 = SmoothNoise(200) // in ms
    val noiseOctave3 = SmoothNoise(100) // in ms
    var styleDisplayWidth = 0
    var styleDisplayHeight = 0
    // will load this from a text file ASAP
    val origCandlePos = arrayOf<Float>(
        929f,
        2217f,
        1019f,
        2307f,
        935f,
        2050f,
        1025f,
        2140f,
        935f,
        2055f,
        1025f,
        2145f
    )
    var extractorsReady = 0
    lateinit var imagePos: RectF

    val imageIds = intArrayOf(R.drawable.sacredimage1, R.drawable.sacredimage2, R.drawable.sacredimage3, R.drawable.sacredimage4, R.drawable.sacredimage5, R.drawable.sacredimage6, R.drawable.sacredimage7, R.drawable.sacredimage8, R.drawable.sacredimage9, 0)
    override fun onBackPressed() {
        Log.d(TAG, "Intercepted back button press to avoid minimising app")
        val mainMenu = Intent()
        mainMenu.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".MainMenuActivity")))
        startActivity(mainMenu)
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadValues(savedInstanceState)


        val origPos = getOrigCandlePos()
        Log.d(TAG, "Transforming candle location data " + origPos.toString() + " with dst width " + styleDisplayWidth + ", height: " + styleDisplayHeight + ", index: " + styleIndex)


        val bias = calcBias(styleDisplayWidth, styleDisplayHeight)
        Log.d(TAG, "Bias: " + bias)
        if (bias > 0.78) {
            Log.d(TAG, "Loading for bias: 0.78+")
            setContentView(R.layout.activity_fullscreen_shrine_lower_78)
        }
        else if (bias > 0.77) {
            Log.d(TAG, "Loading for bias: 0.77+")
            setContentView(R.layout.activity_fullscreen_shrine_lower_77)
        }
        else if (bias > 0.76) {
            Log.d(TAG, "Loading for bias: 0.76+")
            setContentView(R.layout.activity_fullscreen_shrine_lower_76)
        }
        else if (bias > 0.72) {
            Log.d(TAG, "Loading for bias: 0.72+")
            setContentView(R.layout.activity_fullscreen_shrine_lower_72)
        }
        else if (bias > 0.71) {
            Log.d(TAG, "Loading for bias: 0.71+")
            setContentView(R.layout.activity_fullscreen_shrine_lower_71)
        }
        else if (bias > 0.70) {
            Log.d(TAG, "Loading for bias: 0.70+")
            setContentView(R.layout.activity_fullscreen_shrine_lower_70)
        }
        else {
            Log.d(TAG, "Loading for bias: high")
            setContentView(R.layout.activity_fullscreen_shrine_higher)
        }




        timer.setTimeListener(this)
        flameView.setOnVideoStartedListener { this }


        imageDisplayFullscreenShrine.top = imagePos.top.toInt()
        imageDisplayFullscreenShrine.left = imagePos.left.toInt()
        imageDisplayFullscreenShrine.bottom = imagePos.bottom.toInt()
        imageDisplayFullscreenShrine.right = imagePos.right.toInt()

        initialiseCandleStatus()
        if (candleIsLit) {
            Log.d(TAG, "Setting up candle flame as candle is lit")
            // won't come on till video starts playing!
            styleLitDisplayFullscreenShrine.imageAlpha = 0
            setUpFlame()
        }
        else {
            Log.d(TAG, "Candle not lit")
            candleHolderFullscreenShrine.removeAllViews()
        }

        val frame = FrameLayout.LayoutParams(imageHolderFullscreenShrine.layoutParams)
        frame.topMargin = imagePos.top.roundToInt()
        frame.leftMargin = imagePos.left.roundToInt()
        val imageSize = FrameLayout.LayoutParams(imageDisplayFullscreenShrine.layoutParams)
        imageSize.width = (imagePos.right - imagePos.left).roundToInt()
        imageSize.height = (imagePos.bottom - imagePos.top).roundToInt()
        imageSize.topMargin  = imagePos.top.roundToInt()
        imageSize.leftMargin =imagePos.left.roundToInt()
        imageHolderFullscreenShrine.removeView(imageDisplayFullscreenShrine)
        imageHolderFullscreenShrine.addView(imageDisplayFullscreenShrine, frame)
        imageDisplayFullscreenShrine.layoutParams = imageSize


        val currentFilename = Paper.book().read(getString(R.string.database_key_current_image_filename), "")

        Log.d(TAG, "Filename from database is " + currentFilename)
        val imageIndex = Paper.book().read(getString(R.string.database_key_image_index), 0)


        //val styleIndex = prefs.getInt(getString(R.string.preferences_key_style_index), 0)
        if (imageIndex < imageIds.size - 1) {
            imageDisplayFullscreenShrine.setImageResource(imageIds[imageIndex])
            Log.d(TAG, "Loaded image from resources")
        }
        // otherwise use custom image
        else {
            // gonna load it from the database for the moment, but this should be improved
            // to avoid unnecessary database accesses e.g. if we are coming from SelectImageActivity
            // which already has the bitmap in memory
            // i.e. bitmap should be sent as an extra in the Intent to start this activity
            thread {
                val bitmap = BitmapFactory.decodeStream(openFileInput(currentFilename))
                runOnUiThread {
                    imageDisplayFullscreenShrine.setImageBitmap(bitmap)
                    Log.d(TAG, "Loaded image from file")
                }
            }
        }
        thread {
            Log.d(TAG, "Loading style from saved file")
            if (candleIsLit) {
                val backgroundStyle =
                    BitmapFactory.decodeStream(openFileInput(getString(R.string.style_half_resized_cropped_filename) + styleIndex.toString() + ".png"))
                val fullyLitStyle =
                    BitmapFactory.decodeStream(openFileInput(getString(R.string.style_full_resized_cropped_filename) + styleIndex.toString() + ".png"))
                if (fullyLitStyle != null && backgroundStyle != null) {
                    runOnUiThread {
                        styleLitDisplayFullscreenShrine.setImageBitmap(fullyLitStyle)
                        styleDisplayFullscreenShrine.setImageBitmap(backgroundStyle)
                        Log.d(TAG, "Loaded lit style bitmaps from database")
                        styleDisplayFullscreenShrine.post {
                            styleDisplayFullscreenShrine.visibility = View.VISIBLE
                            imageDisplayFullscreenShrine.visibility = View.VISIBLE
                        }
                    }
                }
            }
            else {
                val backgroundStyle = BitmapFactory.decodeStream(openFileInput(getString(R.string.style_resized_cropped_filename)+ styleIndex.toString() + ".png"))
                if (backgroundStyle != null) {
                    runOnUiThread {
                        styleDisplayFullscreenShrine.setImageBitmap(backgroundStyle)
                        Log.d(TAG, "Loaded unlit style from database on object: " + styleDisplayFullscreenShrine.toString())
                        styleDisplayFullscreenShrine.post {
                            styleDisplayFullscreenShrine.visibility = View.VISIBLE
                            imageDisplayFullscreenShrine.visibility = View.VISIBLE
                        }
                    }

                }
            }

        }
        // make the image glow in the candlelight
        val paintSacredImage = Paint()
        paintSacredImage.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        Log.d(TAG, "Image display layer type " + imageDisplayFullscreenShrine.layerType)
        imageDisplayFullscreenShrine.setLayerPaint(paintSacredImage)


    }
    fun calcBias(width: Int, height: Int):Float {
        val transformer = RectFTransformer()
        val candlePos = transformer.transform(getOrigCandlePos(), width, height)
        val centre = candlePos.bottom - ((candlePos. bottom - candlePos.top)/2)
        val bias = (centre / height)
        return bias
    }
    fun setUpFlame() {

/*
        val frame = FrameLayout.LayoutParams(imageHolderFullscreenShrine.layoutParams)

        Log.d(TAG, "New pos: " + candlePos.toString())
        frame.topMargin = candlePos.top.roundToInt()
        frame.leftMargin = candlePos.left.roundToInt()
        frame.width = (candlePos.right - candlePos.left).toInt()
        frame.height = (candlePos.bottom - candlePos.top).toInt()



        //val testImage = ImageView(this)
        //testImage.setImageResource(R.drawable.test)
        val thicknessUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.candle)
        thicknessView.addReadyListener(this)
        thicknessView.setVideoFromUri(this, thicknessUri)
       // testImage.layoutParams = frame;
        //val testFlame = MediaDecoderAlphaMovieView(this, null)
        //candleHolderFullscreenShrine.addView(testImage)


        val candleSize = FrameLayout.LayoutParams(flameView.layoutParams)

        candleHolderFullscreenShrine.addView(flameView, frame)

        candleHolderFullscreenShrine.addView(thicknessView, frame)

        candleHolderFullscreenShrine.addView(blurView, frame)
        thicknessView.layoutParams = candleSize
        flameView.layoutParams = candleSize
        blurView.layoutParams = candleSize
                 */
        val paintScreenMode = Paint()
        paintScreenMode.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        flameView.setLayerPaint(paintScreenMode)

        val flameUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.candle)
        //val thicknessUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.candle)
        //val blurUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.blur7)
        flameView.setVideoFromUri(this, flameUri)
        //thicknessView.setVideoFromUri(this, thicknessUri)
        //blurView.setVideoFromUri(this, blurUri)

        //blurView.setLayerPaint(paintScreenMode)
        Log.d(TAG, "Flame setup complete")


    }
    override fun onStart() {
        super.onStart()

        if (candleIsLit) {
            //thicknessView.start()
            timer.start()
            flameView.start()
            //blurView.start()

        }


    }
    override fun onPause() {
        super.onPause()
        timer.pause()
        //thicknessView.onPause()
        flameView.onPause()
        //blurView.onPause()
    }
    override fun onResume() {
        super.onResume()
        timer.resume()
        //thicknessView.onResume()

        flameView.onResume()
        //blurView.onResume()
    }
    override fun onTimeUpdate(animation: TimeAnimator?, totalTime: Long, deltaTime: Long) {
        val result = (noiseOctave1.sampleAt(totalTime)*64f) + (noiseOctave2.sampleAt(totalTime)*64f) + (noiseOctave3.sampleAt(totalTime)*128f)
        styleLitDisplayFullscreenShrine.imageAlpha = result.toInt()
        Log.d(TAG, "Alpha is: " + result)
        //thicknessView.onTimeUpdate(animation, totalTime, deltaTime)
        //flameView.onTimeUpdate(animation, totalTime, deltaTime)
        //blurView.onTimeUpdate(animation, totalTime, deltaTime)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putParcelable("imagePos", imagePos)
        outState.putInt("StyleDisplayWidth", styleDisplayWidth)
        outState.putInt("StyleDisplayHeight", styleDisplayHeight)
    }


    fun loadValues(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.containsKey("imagePos") &&
            savedInstanceState.containsKey("styleDisplayWidth") &&
            savedInstanceState.containsKey("styleDisplayHeight")) {
            imagePos = savedInstanceState.getParcelable<RectF>("imagePos")!!
            styleDisplayWidth = savedInstanceState.getInt("styleDisplayWidth")
            styleDisplayHeight = savedInstanceState.getInt("styleDisplayHeight")
            Log.d(TAG, "Got imagePos from saved state")
        }
        else {
            Log.d(TAG, "imagePos in database:" + Paper.book().contains(getString(R.string.database_key_image_index)))
            styleDisplayWidth = Paper.book().read(getString(R.string.database_key_resized_width))
            styleDisplayHeight = Paper.book().read(getString(R.string.database_key_resized_height))
            imagePos = Paper.book().read<RectF>(getString(R.string.database_key_actual_image_rectf), RectF())
        }
        // -1 indicates nothing was in database rather than defaulting to possibly wrong style
        styleIndex = Paper.book().read(getString(R.string.database_key_style_index), -1)
    }
    fun initialiseCandleStatus() {
        // here is where the candle is actually "lit" by saving the timestamp
        if (intent.hasExtra(getString(R.string.intent_key_from_candle_lighting)) &&
            intent.getBooleanExtra(getString(R.string.intent_key_from_candle_lighting), false) == true) {
            lastKnownCandleTimestamp = System.currentTimeMillis()
            Paper.book().write(getString(R.string.database_key_last_candle_lighting_timestamp), lastKnownCandleTimestamp)
            Log.d(TAG, "Candle newly lit")
        }
        else {
            lastKnownCandleTimestamp = Paper.book().read(getString(R.string.database_key_last_candle_lighting_timestamp), 0)
        }
        candleIsLit = (
                System.currentTimeMillis() -
                        lastKnownCandleTimestamp <
                        resources.getInteger(R.integer.candle_duration_minutes) * 60000)
    }
    fun getOrigCandlePos(): RectF {
        val rectf = RectF()
        var baseOffset = styleIndex * 4
        rectf.left = origCandlePos[baseOffset++]
        rectf.top = origCandlePos[baseOffset++]
        rectf.right = origCandlePos[baseOffset++]
        rectf.bottom = origCandlePos[baseOffset++]
        return rectf
    }


}


/*
        candleSize.width = (candlePos.right - candlePos.left).roundToInt()
        candleSize.height = (candlePos.bottom - candlePos.top).roundToInt()
        candleSize.topMargin  = candlePos.top.roundToInt()
        candleSize.leftMargin = candlePos.left.roundToInt()
        candleHolderFullscreenShrine.removeView(flameView)
        Log.d(TAG, "Flame view is " + flameView.toString() + ", width: " + flameView.layoutParams.width)
        Log.d(TAG, "Thickness view is " + thicknessView.toString() + ", width: " + thicknessView.layoutParams.width)
 */