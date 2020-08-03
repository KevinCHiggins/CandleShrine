package com.example.candleshrine

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.TextView
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_select_style.*

// Activities flow:
// This activity is reached from the main menu's "Build Shrine" or "Edit Shrine" option. From this
// activity, the Select Image activity can be accessed. This activity checks Shared Preferences
// in case it should set the style on starting to be the one the user had previously set, if the
// user has already built a shrine.
// This activity finishes later, when the Select Image Activity finishes.
class SelectStyleActivity : AppCompatActivity() {

    val TAG = "SelectStyleActivity"
    val numStyles = 3
    val styles = arrayOfNulls<Bitmap>(numStyles)
    val styleIds = intArrayOf(R.drawable.test1, R.drawable.test2, R.drawable.test3)
    var styleDisplayWidth = 0
    var styleDisplayHeight = 0
    var currId = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_style)
        val prefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)

        Log.d(TAG, "Style index found in preferences? " + prefs.contains(getString(R.string.preferences_key_style_index)))
        currId = prefs.getInt(getString(R.string.preferences_key_style_index), 0)
        Log.d(TAG, "Setting style number " + currId)
        styleDisplaySelectStyle.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Log.d(TAG, "Start")
                styleDisplaySelectStyle.viewTreeObserver.removeOnGlobalLayoutListener(this)


                styleDisplayWidth = styleDisplaySelectStyle.width
                styleDisplayHeight = styleDisplaySelectStyle.height // I tried reducing it by statusBarHeight, didn't work
                Log.d(TAG, "Style width: " + styleDisplayWidth + ", style height: " + styleDisplayHeight)
                // now that styleDisplayWidth/Height properties are set, we can load bitmaps
                // using it to resize
                for (i in 0..2) {
                    loadResizedBitmap(i)
                    Log.d(TAG, "Loaded bitmap of width: " + styles[i]!!.width + ", height: " + styles[i]!!.height)
                }
                styleDisplaySelectStyle.setImageBitmap(styles[currId])
                Log.d(TAG, "Displayed shrine in view of width: " + styleDisplaySelectStyle.width + ", height: " + styleDisplaySelectStyle.height)

            }
        })

        finishedStyleButton.setOnClickListener {
            val editPrefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE).edit()
            editPrefs.putInt(getString(R.string.preferences_key_style_index), currId)
            Log.d(TAG, "Saving resized/cropped style bitmap width: " + styles[currId]!!.width + " to file")
            val saver = BitmapLoader()
            saver.save(this, styles[currId]!!, getString(R.string.temp_style_resized_cropped_filename))

            Log.d(TAG, "Saving style index to preferences")
            // hopefully async save will be fast enough - any hint of an issue and I will change to synchronous
            editPrefs.apply()
            val selectImage = Intent()
            selectImage.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".SelectImageActivity")))
            selectImage.putExtra("styleId", currId)
            Log.d(TAG, "Starting select image activity")

            startActivityForResult(selectImage, resources.getInteger(R.integer.request_select_image_code))
        }
        nextStyleButton.setOnClickListener { nextStyle() }
        previousStyleButton.setOnClickListener { prevStyle() }

    }
    fun loadResizedBitmap(index: Int) {
        val loader = BitmapLoader()
        val assetName = getString(R.string.bitmaps_base_filename) + index.toString() + ".png"
        Log.d(TAG, "Attempting to load bitmap from asset: " + assetName)
        styles[index] = loader.getCentreFitted(this, assetName, styleDisplayWidth, styleDisplayHeight)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode==resources.getInteger(R.integer.request_select_image_code)) {
            Log.d(TAG, "Select image returned result code " + resultCode)
            // RESULT_OK means the user has finished the image selection so we will end the
            // editing process and display the shrine, which I want to be above the main
            // menu in the stack, so I finish this activity to take it out of the stack
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Select image finished successfully, so also finishing select style")
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    fun nextStyle() {
        currId++
        if (currId >= styleIds.size) {
            currId = 0
        }
        Log.d(TAG, "Setting style number " + currId)
        styleDisplaySelectStyle.setImageBitmap(styles[currId])
    }
    fun prevStyle() {
        currId--
        if (currId < 0) {
            currId = styleIds.size - 1
        }
        Log.d(TAG, "Setting style number " + currId)
        styleDisplaySelectStyle.setImageBitmap(styles[currId])
    }
}

