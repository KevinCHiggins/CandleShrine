package com.example.candleshrine

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_select_image.*
import kotlin.concurrent.thread

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
    lateinit var prefs: SharedPreferences
    var styleIndex = 0
    // zero value at end of array indicates no custom image loaded so far
    val imageIds = intArrayOf(R.drawable.sacredimage1, R.drawable.sacredimage2, R.drawable.sacredimage3, R.drawable.sacredimage4, 0)
    var currIndex = 0
    // special value inserted at end of array if a custom image has been loaded... crude, I know
    val customId = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_image)
        val restored = (savedInstanceState != null)
        // would be nice to avoid loading prefs if not strictly needed but I'll keep things
        // simple and just initialise a property
        // if the Intent doesn't have a style index (though, it should)
        prefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)
        //litStylesSavedToDisk = prefs.getBoolean(getString(R.string.preferences_key_resized_lit_styles_saved), false)

        if (!intent.hasExtra(getString(R.string.intent_key_style_index))) {
            if (restored && savedInstanceState!!.containsKey("styleIndex") && savedInstanceState!!.containsKey("imageIndex")) {
                styleIndex = savedInstanceState.getInt("StyleIndex")
                currIndex = savedInstanceState.getInt("imageIndex")
            }
            else {
                                Log.d(TAG, "Finding image index in preferences; found:" + prefs.contains(getString(R.string.preferences_key_image_index)))
                currIndex = prefs.getInt(getString(R.string.preferences_key_image_index), 0)
                Log.d(TAG, "Finding styleindex in preferences; found:" + prefs.contains(getString(R.string.preferences_key_image_index)))
                currIndex = prefs.getInt(getString(R.string.preferences_key_image_index), 0)
            }
        }
        // in the case that the Intent did contain a style index (which is the standard scenario)...
        // some redundant code here necessary to deal with branches of logic
        else { // we know there was a value in the intent so extract that
            styleIndex = intent.getIntExtra(getString(R.string.intent_key_style_index), 0)
            // then look for image index in saved state bundle or in preferences (but typically it won't be in either)
            if (restored && savedInstanceState!!.containsKey("imageIndex")) {
                currIndex = savedInstanceState.getInt("imageIndex")
            }
            else { //
                Log.d(TAG, "Finding image index in preferences; found:" + prefs.contains(getString(R.string.preferences_key_image_index)))
                // default value will be used when this runs straight after select type activity
                // when no shrine has been built yet
                currIndex = prefs.getInt(getString(R.string.preferences_key_image_index), 0)
            }
        }

        thread {
            styleBitmap = loadStyleResizedCroppedUnlit()
            runOnUiThread {
                styleDisplaySelectImage.setImageBitmap(styleBitmap)
                Log.d(TAG, "Loaded style from file")
            }
        }


        if (imageIds[currIndex] != customId) {
            Log.d(TAG, "Initialising with built-in image" + currIndex)
            imageDisplaySelectImage.setImageResource(imageIds[currIndex])
        }
        // if initial index value points to custom image, try load saved custom image from
        // whatever filename is specified in image
        else {
            thread {
                val bitmapManager = BitmapManager()
                var failed = false

                val filename = prefs.getString(getString(R.string.preferences_key_current_image_filename), "")
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
            val editPrefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE).edit()
            editPrefs.putInt(getString(R.string.preferences_key_image_index), currIndex)
            // *now* we save the styleIndex passed in the Intent, because it's at this stage that
            // the shrine is completed/confirmed...
            //editPrefs.putInt(getString(R.string.preferences_key_style_index), styleIndex)

            editPrefs.putBoolean(getString(R.string.preferences_key_shrine_built), true)



            // save the image to the database if it's custom
            // no idea if this is okay in terms of amount of data... also a bitmap is a bit wasteful...
            if (imageIds[currIndex] == customId) {
                val editPrefs = prefs.edit()
                val oldFilename = prefs.getString(getString(R.string.preferences_key_current_image_filename), "")
                var newName = ""
                if (oldFilename == getString(R.string.custom_image_filename_a)) {
                    newName = getString(R.string.custom_image_filename_b)
                    editPrefs.putString(getString(R.string.preferences_key_current_image_filename), newName)
                }
                else {
                    newName = getString(R.string.custom_image_filename_a)
                    editPrefs.putString(getString(R.string.preferences_key_current_image_filename), newName)
                }

                Log.d(TAG, "Custom image path (to be used by fullscreen shrine) was previously " + oldFilename + " but is now " + newName)
            }


            Log.d(TAG, "Saving preferences")
            // essential to call this - it's like commit() but the writing to file is asynchronous
            editPrefs.commit()
            Log.d(TAG, "SAVED: " + prefs.getString(getString(R.string.preferences_key_current_image_filename), ""))

            Log.d(TAG, "Saving resized/cropped style lit")


            val bitmapUtil = BitmapManager()
            val litStyleFromDatabase = bitmapUtil.getCentreFitted(this, getString(R.string.bitmaps_lit_base_filename) + styleIndex+".png", styleBitmap.width, styleBitmap.height)
            if (litStyleFromDatabase != null) {
                bitmapUtil.save(this, litStyleFromDatabase, getString(R.string.style_lit_resized_cropped_filename)+styleIndex+".png")
                Log.d(TAG, "Resized lit and saved as final lit")
            }
            else {
                Log.d(TAG, "Couldn't load lit style bitmap from file for final saving")
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
    }
    fun nextImage() {
        currIndex++
        // wrap at top of array
        if (currIndex >= imageIds.size) {
            currIndex = 0
        }
        // if null value (i.e. no custom image loaded)... skip its index
        if (imageIds[currIndex] == 0) {
            currIndex++ // if custom image is top index, this will wrap to zero in the following clause
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
            if (resultCode == Activity.RESULT_OK) {
                val filename = data!!.getStringExtra("filename")
                bitmap = BitmapFactory.decodeStream(openFileInput(filename))
                currIndex = imageIds.size - 1
                imageIds[imageIds.size - 1] = customId // set value to indicate custom image loaded
                imageDisplaySelectImage.setImageBitmap(bitmap)
                Log.d(TAG,"Image picking and cropping successful.")

            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG,"Image picking and cropping cancelled.")
            }
        }
    }

}