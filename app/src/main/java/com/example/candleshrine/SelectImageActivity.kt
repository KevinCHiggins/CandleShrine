/*
* Copyright (C) 2020 Kevin Higgins
* @author Kevin Higgins
* This class lets the user the sacred image to display in their shrine. It shows a selection
* of built in images and also lets the user add a custom image to this list, by
* calling CropImageActivity which calls an image picker.
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
import android.content.SharedPreferences
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginTop
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_fullscreen_shrine_higher.*
import kotlinx.android.synthetic.main.activity_select_image.*
import kotlin.concurrent.thread
import kotlin.math.roundToInt

// Activities flow:
// This activity is reached from the SelectStyleActivity. Therefore it will have SelectStyleActivity
// and MainMenuActivity below it in the stack. When it finishes, it also finishes SelectStyleActivity, and goes
// to FullscreenShrineActivity.
// This activity checks Shared Preferences in case the user has already built a shrine, in which
// case it starts by displaying the image the user used for that shrine.
// From this activity, LoadCustomImageActivity can optionally be started.
class SelectImageActivity : AppCompatActivity() {

    val TAG = "SelectImageActivity"
    var bitmap: Bitmap? = null // note this requires a null check before it gets saved in onSaveInstanceState
    lateinit var styleBitmap: Bitmap
    //lateinit var prefs: SharedPreferences
    var styleIndex = 0
    // zero value at end of array indicates no custom image loaded so far
    val imageIds = intArrayOf(R.drawable.sacredimage1, R.drawable.sacredimage2, R.drawable.sacredimage3, R.drawable.sacredimage4, 0)
    var currIndex = 0
    lateinit var imagePos: RectF
    // special value inserted at end of array if a custom image has been loaded... crude, I know
    val customId = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_image)
        val restored = (savedInstanceState != null)

        // if the Intent doesn't have a style index (though, it should)...
        if (!intent.hasExtra(getString(R.string.intent_key_style_index))) {
            if (restored && savedInstanceState!!.containsKey("styleIndex") &&
                savedInstanceState.containsKey("imageIndex") &&
                    savedInstanceState.containsKey("imagePos")) {
                styleIndex = savedInstanceState.getInt("StyleIndex")
                currIndex = savedInstanceState.getInt("imageIndex")
                imagePos = savedInstanceState.getParcelable<RectF>("imagePos")!!
                Log.d(TAG, "Intent didn't have a style index")
            }
            else {
                Log.d(TAG, "imageIndex exists in database:" + Paper.book().contains(getString(R.string.database_key_image_index)))
                currIndex = Paper.book().read<Int>(getString(R.string.database_key_image_index), 0)
                Log.d(TAG, "styleindex in database:" + Paper.book().contains(getString(R.string.database_key_image_index)))
                currIndex = Paper.book().read<Int>(getString(R.string.database_key_style_index), 0)
                Log.d(TAG, "imagePos in database:" + Paper.book().contains(getString(R.string.database_key_image_index)))
                imagePos = Paper.book().read<RectF>(getString(R.string.database_key_actual_image_rectf), RectF())
            }
        }
        // in the case that the Intent *does* contain a style index (which is the standard scenario)...
        // some redundant code here necessary to deal with branches of logic
        else { // we know there was a value in the intent so extract that
            styleIndex = intent.getIntExtra(getString(R.string.intent_key_style_index), 0)
            // then look for image index in saved state bundle or in preferences (but typically it won't be in either)
            if (restored && savedInstanceState!!.containsKey("imageIndex") &&
                savedInstanceState.containsKey("imagePos")) {
                currIndex = savedInstanceState.getInt("imageIndex")
                imagePos = savedInstanceState.getParcelable<RectF>("imagePos")!!
            }
            else { //
                Log.d(TAG, "Was image index in database? " + Paper.book().contains(getString(R.string.database_key_image_index)))

                // *default value is necessary!* will be used when this runs straight after select type activity
                // when no shrine has been built yet
                currIndex = Paper.book().read<Int>(getString(R.string.database_key_image_index), 0) // keep default value
                // default value won't display image properly, and will only get used if there was a corrupt DB
                Log.d(TAG, "imagePos in database:" + Paper.book().contains(getString(R.string.database_key_actual_image_rectf)))
                imagePos = Paper.book().read<RectF>(getString(R.string.database_key_actual_image_rectf), RectF())
            }
            Log.d(TAG, "Loaded (or used default) image index: " + currIndex + ", style index: " + styleIndex)
        }

        val frame = FrameLayout.LayoutParams(imageHolderSelectImage.layoutParams)
        frame.topMargin = imagePos.top.roundToInt()
        frame.leftMargin = imagePos.left.roundToInt()
        val imageSize = FrameLayout.LayoutParams(imageDisplaySelectImage.layoutParams)
        imageSize.width = (imagePos.right - imagePos.left).roundToInt()
        imageSize.height = (imagePos.bottom - imagePos.top).roundToInt()
        imageSize.topMargin  = imagePos.top.roundToInt()
        imageSize.leftMargin =imagePos.left.roundToInt()
        imageHolderSelectImage.removeView(imageDisplaySelectImage)
        imageHolderSelectImage.addView(imageDisplaySelectImage, frame)
        imageDisplaySelectImage.layoutParams = imageSize

        thread {
            styleBitmap = loadStyleResizedCroppedUnlit()
            runOnUiThread {
                styleDisplaySelectImage.setImageBitmap(styleBitmap)
                Log.d(TAG, "Loaded style from file")
            }
        }

        // check if it's the custom image slot
        if (currIndex != imageIds.size - 1) {
            Log.d(TAG, "Initialising with built-in image" + currIndex)
            imageDisplaySelectImage.setImageResource(imageIds[currIndex])
        }
        // if initial index value points to custom image, try load saved custom image from
        // whatever filename is specified in image
        else {
            Log.d(TAG, "Initialising with custom image")
            thread {
                val bitmapManager = BitmapManager()
                var failed = false

                val filename = Paper.book().read(getString(R.string.database_key_current_image_filename), "")
                if (filename != "")
                {
                    val bitmapFromFile = bitmapManager.load(this, filename!!)
                    Log.d(TAG, "Loaded custom image from " + filename)
                    if (bitmapFromFile == null) {
                        failed = true

                    }
                    else {
                        runOnUiThread {
                            Log.d(TAG, "Loaded image from file, displaying now")
                            imageDisplaySelectImage.setImageBitmap(bitmapFromFile)
                        }
                    }
                }
                else { // there was no filename in preferences
                    failed = true
                }
                if (failed) {
                    runOnUiThread {
                        Log.d(TAG, "Failed to load custom image from database; displaying default")
                        currIndex = 0;
                        imageDisplaySelectImage.setImageResource(imageIds[currIndex])
                    }
                }
            }
        }
        nextImageButton.setOnClickListener { nextImage() }
        previousImageButton.setOnClickListener { prevImage() }
        finishedImageButton.setOnClickListener {
            Log.d(TAG, "Button clicked to finish image select")
            // here we handle the ending of the "Build/Edit Shrine" use case
            // save vals in preferences and in persistence, finish everything except main menu, and launch shrine view
            Paper.book().write(getString(R.string.database_key_image_index), currIndex)
            // *now* we save the styleIndex passed in the Intent, because it's at this stage that
            // the shrine is completed/confirmed, record its style...
            Paper.book().write(getString(R.string.database_key_style_index), styleIndex)
            // record its existence
            Paper.book().write(getString(R.string.database_key_shrine_built), true)
            // quench any burning candle
            Paper.book().delete(getString(R.string.database_key_last_candle_lighting_timestamp))



            // save the image to the disk if it's custom
            if (imageIds[currIndex] == customId) {
                val oldFilename = Paper.book().read(getString(R.string.database_key_current_image_filename), "")
                var newName = ""
                if (oldFilename == getString(R.string.custom_image_filename_a)) {
                    newName = getString(R.string.custom_image_filename_b)
                    Paper.book().write(getString(R.string.database_key_current_image_filename), newName)
                }
                else {
                    newName = getString(R.string.custom_image_filename_a)
                    Paper.book().write(getString(R.string.database_key_current_image_filename), newName)
                }

                Log.d(TAG, "Custom image path (to be used by fullscreen shrine) was previously " + oldFilename + " but is now " + newName)
            }


            Log.d(TAG, "Saving resized/cropped style lit")
            val bitmapUtil = BitmapManager()
            val halfLitStyleFromDatabase = bitmapUtil.getCentreFitted(this, getString(R.string.bitmaps_half_base_filename) + styleIndex+".png", styleBitmap.width, styleBitmap.height)
            val fullyLitStyleFromDatabase = bitmapUtil.getCentreFitted(this, getString(R.string.bitmaps_full_base_filename) + styleIndex+".png", styleBitmap.width, styleBitmap.height)

            if (halfLitStyleFromDatabase != null && fullyLitStyleFromDatabase != null) {
                bitmapUtil.save(this, halfLitStyleFromDatabase, getString(R.string.style_half_resized_cropped_filename)+styleIndex+".png")
                bitmapUtil.save(this, fullyLitStyleFromDatabase, getString(R.string.style_full_resized_cropped_filename)+styleIndex+".png")

                Log.d(TAG, "Resized lit and saved as final lit")
            }
            else {
                Log.d(TAG, "Couldn't load lit style bitmaps from file for final saving")
                finish()
            }
            launchFullscreenShrine()
        }
        custom.setOnClickListener {
            val cropImage = Intent()
            cropImage.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".CropImageActivity")))
            Log.d(TAG, "Starting crop activity")
            startActivityForResult(cropImage, resources.getInteger(R.integer.request_crop_image_code))
        }
        // make the image glow in the candlelight
        val paintSacredImage = Paint()
        paintSacredImage.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        Log.d(TAG, "Image display layer type " + imageDisplaySelectImage.layerType)
        imageDisplaySelectImage.setLayerPaint(paintSacredImage)

    }

    fun launchFullscreenShrine() {
        Log.d(TAG, "Setting result so select image activity can receive it and finish; then finishing.")
        setResult(-1)
        finish()

        val fullscreenShrine = Intent()
        fullscreenShrine.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".FullscreenShrineActivity")))
        Log.d(TAG, "Starting fullscreen shrine activity")
        startActivity(fullscreenShrine)
    }
    fun loadStyleResizedCroppedUnlit():Bitmap {
        val bitmapManager = BitmapManager()
        val assetName = getString(R.string.style_resized_cropped_filename) + styleIndex.toString() + ".png"
        Log.d(TAG, "Attempting to load bitmap from asset: " + assetName)
        return bitmapManager.load(this, assetName)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "Saving indices")
        super.onSaveInstanceState(outState)
        outState.putInt("currIndex", currIndex) // not gonna use string values cause it's for this class only
        outState.putInt("styleIndex", styleIndex)
        outState.putParcelable("imagePos", imagePos)
    }
    fun nextImage() {
        currIndex++
        // if null value (i.e. no custom image loaded)... skip its index
        if (imageIds[currIndex] == 0) {
            currIndex++ // if custom image is top index, this will wrap to zero in the following clause
        }

        // wrap at top of array
        if (currIndex >= imageIds.size) {
            currIndex = 0
        }

        setImage()
    }
    fun prevImage() {
        currIndex--
        // wrap at zero
        if (currIndex < 0) {
            currIndex = imageIds.size - 1
        }
        // if null value (i.e. no custom image loaded)...
        if (imageIds[currIndex] == 0) {
            currIndex--
        }
        setImage()
    }
    fun setImage() {
        if (imageIds[currIndex] == customId) {
            Log.d(TAG, "Setting custom image")
            imageDisplaySelectImage.setImageBitmap(bitmap)
        } else {
            Log.d(TAG, "Setting built-in image number " + currIndex)
            imageDisplaySelectImage.setImageResource(imageIds[currIndex])
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == resources.getInteger(R.integer.request_crop_image_code)) {
            val imagePickSuccessful = false
            if (resultCode == Activity.RESULT_OK) {
                val filename = data!!.getStringExtra("filename")
                if (filename != null) {
                    bitmap = BitmapFactory.decodeStream(openFileInput(filename))
                    currIndex = imageIds.size - 1
                    imageIds[imageIds.size - 1] =
                        customId // set value to indicate custom image loaded
                    imageDisplaySelectImage.setImageBitmap(bitmap)
                    Log.d(TAG, "Image picking and cropping successful.")
                }
            }
            if ((resultCode == Activity.RESULT_CANCELED) || (!imagePickSuccessful)) {
                Log.d(TAG,"Image picking and cropping cancelled.")
            }
        }
    }

}