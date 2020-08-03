package com.example.candleshrine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_fullscreen_shrine.*
import kotlin.concurrent.thread

// Activities flow
// There are no interactions available from this activity. The user taps the back button to get
// back to the main menu, which will always be below this in the stack.
class FullscreenShrineActivity : AppCompatActivity() {
    val TAG = "FSActivity"

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
        Log.d(TAG, "Style display object: " + styleDisplayFullscreenShrine.toString())
        val prefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)
        val imageIndex = prefs.getInt(getString(R.string.preferences_key_image_index), 0)
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
                val bitmap = BitmapFactory.decodeStream(openFileInput(getString(R.string.sacred_image_filename)))
                runOnUiThread {
                    imageDisplayFullscreenShrine.setImageBitmap(bitmap)
                    Log.d(TAG, "Loaded image from database")
                }
            }
        }
        thread {
            Log.d(TAG, "Loading style from saved file")
            val styleFromDatabase = BitmapFactory.decodeStream(openFileInput(getString(R.string.style_resized_cropped_filename)))
            runOnUiThread {
                if (styleFromDatabase != null) {
                    Log.d(TAG, "Setting style bitmap from database on object: " + styleDisplayFullscreenShrine.toString())
                    styleDisplayFullscreenShrine.setImageBitmap(styleFromDatabase)
                    Log.d(TAG, "Loaded style bitmap from database")
                }
                else {
                    Log.d(TAG, "Couldn't load style bitmap from database")
                    finish()
                }
            }

        }

    }
}