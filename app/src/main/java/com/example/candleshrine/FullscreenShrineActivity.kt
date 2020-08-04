package com.example.candleshrine

import android.animation.TimeAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_fullscreen_shrine.*
import kotlin.concurrent.thread

// Activities flow
// There are no interactions available from this activity. The user taps the back button to get
// back to the main menu, which will always be below this in the stack.
class FullscreenShrineActivity : AppCompatActivity(), TimeAnimator.TimeListener {
    val paintSacredImage = Paint()
    val TAG = "FSActivity"
    val timer = TimeAnimator()
    val noiseOctave1 = SmoothNoise(400) // in ms
    val noiseOctave2 = SmoothNoise(200) // in ms
    val noiseOctave3 = SmoothNoise(100) // in ms
    val imageIds = intArrayOf(R.drawable.sacredimage1, R.drawable.sacredimage2, R.drawable.sacredimage3, R.drawable.sacredimage4, 0)
    override fun onBackPressed() {
        Log.d(TAG, "Intercepted back button press to avoid minimising app")
        val mainMenu = Intent()
        mainMenu.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".MainMenuActivity")))
        startActivity(mainMenu)
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_shrine)
        timer.setTimeListener(this)
        timer.start()
        Log.d(TAG, "Style display object: " + styleDisplayFullscreenShrine.toString())
        val prefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)
        val currentFilename = prefs.getString(getString(R.string.preferences_key_current_image_filename), "")
        Log.d(TAG, "Filename from prefs is " + currentFilename)
        val imageIndex = prefs.getInt(getString(R.string.preferences_key_image_index), 0)
        val styleIndex = prefs.getInt(getString(R.string.preferences_key_style_index), -1)
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
                    Log.d(TAG, "Loaded image from database")
                }
            }
        }
        thread {
            Log.d(TAG, "Loading style from saved file")
            val styleFromDatabase = BitmapFactory.decodeStream(openFileInput(getString(R.string.style_resized_cropped_filename)+ styleIndex.toString() + ".png"))
            val styleLitFromDatabase = BitmapFactory.decodeStream(openFileInput(getString(R.string.style_lit_resized_cropped_filename)+ styleIndex.toString() + ".png"))
            runOnUiThread {
                if (styleFromDatabase != null) {
                    Log.d(TAG, "Setting style bitmap from database on object: " + styleDisplayFullscreenShrine.toString())
                    styleDisplayFullscreenShrine.setImageBitmap(styleFromDatabase)
                    styleLitDisplayFullscreenShrine.setImageBitmap(styleLitFromDatabase)
                    Log.d(TAG, "Loaded style bitmaps from database")
                }
                else {
                    Log.d(TAG, "Couldn't load style bitmaps from database")
                    finish()
                }
            }
            paintSacredImage.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            Log.d(TAG, "Image display layer type " + imageDisplayFullscreenShrine.layerType)
            imageDisplayFullscreenShrine.setLayerPaint(paintSacredImage)

        }

    }
    override fun onTimeUpdate(animation: TimeAnimator?, totalTime: Long, deltaTime: Long) {
        val result = 0 + (noiseOctave1.sampleAt(totalTime)*64) + noiseOctave2.sampleAt(totalTime)*64
        + noiseOctave3.sampleAt(totalTime)*128

        styleLitDisplayFullscreenShrine.imageAlpha = result.toInt()
        /*
        perturb+= (Random.nextInt(0,101) - 50)
        phase = phase + ((deltaTime.toFloat() + perturb) / 20000)
        result = (sin.getInterpolation(phase) + 1) / 2
        debug.text = phase.toString() + "\nResult" + result + "\nTotal: " + totalTime +
                "\ndelta: " + deltaTime + "\nPerturb: " + perturb + "\nAlpha: " + alpha
        alpha = (result * 255).toInt()
        lit.imageAlpha = alpha

         */
    }
}