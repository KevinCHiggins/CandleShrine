package com.example.candleshrine

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_select_image.*
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException

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

    val styleIds = intArrayOf(R.drawable.test1, R.drawable.test2, R.drawable.test3)
    // zero value at end of array indicates no custom image loaded so far
    val imageIds = intArrayOf(R.drawable.sacredimage1, R.drawable.sacredimage2, R.drawable.sacredimage3, R.drawable.sacredimage4, 0)
    var currIndex = 0
    // special value inserted at end of array if a custom image has been loaded... crude, I know
    val customId = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_select_image)
        val restored = (savedInstanceState != null)
        /*
        // BTW I wonder can the bundle be null in general?
        if (restored && savedInstanceState!!.containsKey("styleIndex")) {
            Log.d(TAG, "Initialised with style index from saved instance state")
            styleIndex = savedInstanceState.getInt("styleIndex")

        }
        // otherwise it's a first time run... for style we take the value from the previous
        // activity rather than preferences, as it may have been modified from the value saved
        // in preferences and the idea is that the select style and select image activities
        // are part of one editing/building task that completes when the user taps "Done"
        // in the image select activity
        else {
            Log.d(TAG, "No style index in saved instance state; initialising from Intent")
            styleIndex = intent.getIntExtra("styleIndex", 0)
            styleDisplaySelectImage.setImageResource(styleIds[styleIndex])
        }

         */


        Log.d(TAG, "Initialising with style bitmap from database (temp working copy saved by select style activity)")
        val styleFromDatabase = BitmapFactory.decodeStream(openFileInput(getString(R.string.temp_style_resized_cropped_filename)))
        if (styleFromDatabase != null) {
            styleBitmap = styleFromDatabase
       }
       else {
            Log.d(TAG, "Couldn't load style bitmap from database")
            finish()
        }
        styleDisplaySelectImage.setImageBitmap(styleBitmap)


        if (restored && savedInstanceState!!.containsKey("imageIndex")) {
            Log.d(TAG, "Initialising with image index from saved instance state")
            currIndex = savedInstanceState.getInt("imageIndex")
        }
        else {
            val prefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)
            Log.d(TAG, "Finding image index in preferences; found:" + prefs.contains(getString(R.string.preferences_key_image_index)))
            currIndex = prefs.getInt(getString(R.string.preferences_key_image_index), 0)
        }


        if (imageIds[currIndex] != customId) {
            Log.d(TAG, "Initialising with built-in image" + currIndex)
            imageDisplaySelectImage.setImageResource(imageIds[currIndex])
        }
        // if custom image is specified by index from preferences
        else {

            val bitmapFromDatabase: Bitmap? = Paper.book().read(getString(R.string.database_key_bitmap), null)
            if (false) {
                // hold a copy in a property in case the user clicks through other images and back
                bitmap = bitmapFromDatabase

                Log.d(TAG, "Initialising with custom image from database")
                imageDisplaySelectImage.setImageBitmap(bitmapFromDatabase)
            }
            else {
                Log.d(TAG, "Failed to load custom image from database; initialising with default")
                currIndex = 0;
                imageDisplaySelectImage.setImageResource(imageIds[currIndex])
            }

            Log.d(TAG, "Style and image successfully initialised.")



        }
        nextImageButton.setOnClickListener { nextImage() }
        previousImageButton.setOnClickListener { prevImage() }
        finishedImageButton.setOnClickListener {
            // here we handle the ending of the "Build/Edit Shrine" use case
            // save vals in preferences and in persistence, finish everything except main menu, and launch shrine view
            val editPrefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE).edit()
            editPrefs.putInt(getString(R.string.preferences_key_image_index), currIndex)
            // *now* we save the styleIndex passed in the Intent, because it's at this stage that
            // the shrine is completed/confirmed...
            //editPrefs.putInt(getString(R.string.preferences_key_style_index), styleIndex)

            editPrefs.putBoolean(getString(R.string.preferences_key_shrine_built), true)

            Log.d(TAG, "Saving preferences")
            // essential to call this - it's like commit() but the writing to file is asynchronous
            editPrefs.apply()

            // save the image to the database if it's custom
            // no idea if this is okay in terms of amount of data... also a bitmap is a bit wasteful...
            if (imageIds[currIndex] == customId) {
                Log.d(TAG, "Saving cropped custom image")
                val saver = BitmapLoader()
                saver.save(this, bitmap!!, getString(R.string.sacred_image_filename))
            }

            // save the image to the database if it's custom
            // no idea if this is okay in terms of amount of data... also a bitmap is a bit wasteful...
            if (imageIds[currIndex] == customId) {
                Log.d(TAG, "Saving cropped custom image")
                Paper.book().write(getString(R.string.database_key_bitmap), bitmap)
            }
            // this is very wasteful because the resized style is already saved
            // in the database... I will set up an alternate one after initial testing

            Log.d(TAG, "Saving resized/cropped style")
            //Paper.book().write(getString(R.string.database_key_style_bitmap_final), styleBitmap)



            val saver = BitmapLoader()
            saver.save(this, styleBitmap, getString(R.string.style_resized_cropped_filename))


            Log.d(TAG, "Setting result so select image activity can receive it and finish; then finishing.")
            setResult(-1)
            finish()

            val fullscreenShrine = Intent()
            fullscreenShrine.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".FullscreenShrineActivity")))
            Log.d(TAG, "Starting fullscreen shrine activity")
            startActivity(fullscreenShrine)

        }
        custom.setOnClickListener {
            val cropImage = Intent()
            cropImage.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".CropImageActivity")))
            Log.d(TAG, "Starting crop activity")
            startActivityForResult(cropImage, resources.getInteger(R.integer.request_crop_image_code))
        }

    }
    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "Saving indices")
        super.onSaveInstanceState(outState)
        outState.putInt("currIndex", currIndex)
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
            if (resultCode == Activity.RESULT_OK) {
                bitmap = data!!.getParcelableExtra("image")
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