/*
* Copyright (C) 2020 Kevin Higgins
* @author Kevin Higgins
* This class lets the user choose the appearance of their shrine.
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
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.RectF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.ViewTreeObserver
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_select_style.*
import kotlin.concurrent.thread

// Activities flow:
// This activity is reached from the main menu's "Build Shrine" or "Edit Shrine" option. From this
// activity, the Select Image activity can be accessed. This activity checks Shared Preferences
// in case it should set the style on starting to be the one the user had previously set, if the
// user has already built a shrine.
// This activity finishes later, when the Select Image Activity finishes.
class SelectStyleActivity : AppCompatActivity() {

    val TAG = "SelectStyleActivity"
    val numStyles = 3
    val bitmapManager = BitmapManager()
    var styleDisplayWidth = 0
    var styleDisplayHeight = 0

    // will load this from a text file ASAP
    val origCanvasPos = arrayOf<Float>(
        708f,
        1457f,
        1212f,
        2164f,
        708f,
        1267f,
        1212f,
        1974f,
        708f,
        1242f,
        1212f,
        1949f
    )
    // checked against Shared Preferences on load, updated if needed after background loading and saving,
    // and used to decide whether "Done" button creates a "Wait a moment" alert or goes to next activity immediately
    var stylesSavedToDisk = false

    // flag set if user tries to move to next activity by clicking "Done" before resized
    // styles have been saved
    var waitingOnSaveToDisk = false
    val isStyleLoaded = Array<Boolean>(numStyles) { false }
    val styles = arrayOfNulls<Bitmap>(numStyles)

    //val styleIds = intArrayOf(R.drawable.test1, R.drawable.test2, R.drawable.test3)

    var currId = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        Paper.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_style)
        // Restore flags and style index from saved state or database if possible
        if (savedInstanceState != null &&
            savedInstanceState.containsKey("currId") &&
            savedInstanceState.containsKey("stylesSavedToDisk") &&
                savedInstanceState.containsKey("styleDisplayWidth") &&
                savedInstanceState.containsKey(("styleDisplayHeight"))) {
            currId = savedInstanceState.getInt("currId")
            stylesSavedToDisk = savedInstanceState.getBoolean("stylesSavedToDisk")
            styleDisplayWidth = savedInstanceState.getInt("styleDisplayWidth")
            styleDisplayHeight = savedInstanceState.getInt("styleDisplayHeight")
            Log.d(TAG, "Restored from saved instance state - index: " + currId + ", saved flag: " + stylesSavedToDisk)
        }
        else {
            currId = Paper.book().read(getString(R.string.database_key_style_index), 0) // will be 0 on first run or if none saved
            stylesSavedToDisk = Paper.book().read(getString(R.string.database_key_resized_unlit_styles_saved), false)
            Log.d(TAG, "Restored from database (or using defaults) - index: " + currId + ", saved flag: " + stylesSavedToDisk)

        }




        styleDisplaySelectStyle.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                styleDisplaySelectStyle.viewTreeObserver.removeOnGlobalLayoutListener(this)
                styleDisplayWidth = styleDisplaySelectStyle.width
                styleDisplayHeight = styleDisplaySelectStyle.height // I tried reducing it by statusBarHeight, didn't work

                Log.d(TAG, "Style width: " + styleDisplayWidth + ", style height: " + styleDisplayHeight)
                // now that styleDisplayWidth/Height properties are set, loadAndResizeBitmap will work...
                thread {
                    for (i in 0..2) {
                        if (stylesSavedToDisk) {
                            loadBitmap(i)
                            Log.d(TAG, "Loaded bitmap from private files of width: " + styles[i]!!.width + ", height: " + styles[i]!!.height + " for style " + i)

                        }
                        else {
                            loadAndResizeBitmap(i, styleDisplayWidth, styleDisplayHeight)
                            Log.d(TAG, "Loaded and resized asset, now of width: " + styles[i]!!.width + ", height: " + styles[i]!!.height + " for style " + i)

                        }
                        isStyleLoaded[i] = true
                        // if the style bitmap just loaded is the one that should be displaying, display it instantly
                        if (i == currId) {
                            runOnUiThread {
                                displayStyle(currId)
                            }
                        }
                    }
                    if (!stylesSavedToDisk) { // yes, ugly to check this so many times... still only a tiny handful of times
                        // also looks weird to have another iteration, but it's because I want to
                        // load all into memory first in case user wants to see them, and only then
                        // save to disk:
                        for (i in 0..2) {
                            saveBitmap(i)
                        }


                        Paper.book().write(getString(R.string.database_key_resized_unlit_styles_saved), true)
                        Log.d(TAG, "Saved flag for unlit resized styles on disk to database")
                        runOnUiThread {
                            stylesSavedToDisk = true
                            if (waitingOnSaveToDisk) {
                                launchSelectImage()
                            }
                        }
                    }


                }

                Log.d(TAG, "Dispatched shrine loading with width: " + styleDisplaySelectStyle.width + ", height: " + styleDisplaySelectStyle.height)

            }
        })

        finishedStyleButton.setOnClickListener {
            // for the moment I have decided NOT to save it here, instead it will
            // be passed in the intent to the select image activity and only persisted
            // if the user clicks "Build Shrine!" there
            /*
            Paper.book().write(getString(R.string.preferences_key_style_index), currId)

            Log.d(TAG, "Saved style index to database")


             */
            nextStyleButton.isEnabled = false
            previousStyleButton.isEnabled = false
            finishedStyleButton.isEnabled = false
            styleLoadingText.text = "Please wait. Saving custom data..."
            Log.d(TAG, "Saving absolute image position for current screen size")
            val transformer = RectFTransformer()
            Paper.book().write<RectF>(getString(R.string.database_key_actual_image_rectf), transformer.transform(getOrigCanvasPos(), styleDisplayWidth, styleDisplayHeight))

            Paper.book().write(getString(R.string.database_key_resized_width), styleDisplayWidth)
            Paper.book().write(getString(R.string.database_key_resized_height), styleDisplayHeight)
            Log.d(TAG, "Saving complete: " + Paper.book().contains((getString(R.string.database_key_actual_image_rectf))) + ", with " + Paper.book().read(getString(R.string.database_key_actual_image_rectf)))

            if (stylesSavedToDisk) {

                launchSelectImage()
            }
            else {
                Log.d(TAG, "Button clicked too early, need to wait till resized images are saved")
                waitingOnSaveToDisk = true
            }

        }
        nextStyleButton.setOnClickListener { nextStyle() }
        previousStyleButton.setOnClickListener { prevStyle() }

    }
    fun launchSelectImage() {
        // reset things for if we come back (press back button
        styleLoadingText.text = ""
        finishedStyleButton.isEnabled = true
        previousStyleButton.isEnabled = true
        nextStyleButton.isEnabled = true

        val selectImage = Intent()
        selectImage.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".SelectImageActivity")))
        selectImage.putExtra(getString(R.string.intent_key_style_index), currId)
        Log.d(TAG, "Starting select image activity")

        startActivityForResult(selectImage, resources.getInteger(R.integer.request_select_image_code))
    }

    fun displayStyle(index: Int) {
        if (isStyleLoaded[index]) {
            styleDisplaySelectStyle.setImageBitmap(styles[index])
            styleLoadingText.text = ""
        }
        else {
            // yeah, should be template. Don't let innocent users know about zero-indexing.
            styleLoadingText.text = "Please wait. Loading style " + (index + 1).toString() + "..."
        }
    }
    fun loadAndResizeBitmap(index: Int, dstWidth: Int, dstHeight: Int) {
        val assetName = getString(R.string.bitmaps_base_filename) + index.toString() + ".png"
        Log.d(TAG, "Attempting to load bitmap from asset: " + assetName)
        styles[index] = bitmapManager.getCentreFitted(this, assetName, dstWidth, dstHeight)
    }
    fun loadBitmap(index: Int) {
        //val bitmapManager = BitmapManager()
        val assetName = getString(R.string.style_resized_cropped_filename) + index.toString() + ".png"
        Log.d(TAG, "Attempting to load bitmap from asset: " + assetName)
        styles[index] = bitmapManager.load(this, assetName)

    }
    fun saveBitmap(index: Int) {
        val bitmapManager = BitmapManager()
        val assetName = getString(R.string.style_resized_cropped_filename) + index.toString() + ".png"
        Log.d(TAG, "Attempting to save bitmap to: " + assetName)
        bitmapManager.save(this, styles[index]!!, assetName)
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

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putInt("currId", currId)
        outState.putBoolean("stylesSavedToDisk", stylesSavedToDisk)
        outState.putInt("styleDisplayWidth", styleDisplayWidth)
        outState.putInt("styleDisplayHeight", styleDisplayHeight)
        Log.d(TAG, "Saved index: " + currId + ", styles saved flag: " +
                stylesSavedToDisk + "width: " + styleDisplayWidth + ", height: " + styleDisplayHeight + " in saved state")
    }
    fun nextStyle() {
        currId++
        if (currId >= numStyles) {
            currId = 0
        }
        Log.d(TAG, "Setting style number " + currId)
        displayStyle(currId)
    }
    fun prevStyle() {
        currId--
        if (currId < 0) {
            currId = numStyles - 1
        }
        Log.d(TAG, "Setting style number " + currId)
        displayStyle(currId)
    }

    fun getOrigCanvasPos(): RectF {
        val rectf = RectF()
        var baseOffset = currId * 4
        rectf.left = origCanvasPos[baseOffset++]
        rectf.top = origCanvasPos[baseOffset++]
        rectf.right = origCanvasPos[baseOffset++]
        rectf.bottom = origCanvasPos[baseOffset++]
        return rectf
    }
}

