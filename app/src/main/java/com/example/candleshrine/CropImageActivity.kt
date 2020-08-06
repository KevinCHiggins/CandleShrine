/*
 * Copyright (C) 2020 Kevin Higgins
 * @author Kevin Higgins
 * This class uses a modified version of Adam Styrc's cookie-cutter library for
 * cropping images using pinch and drag gestures. It is only used
 * for custom images. It launches the image picker and processes the image it returns.
 * This image is stored in one of two files - the alternation is to avoid overwriting
 * the current shrine's image if the user edits their current shrine but then cancels
 * the edit.
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
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.adamstyrc.cookiecutter.CookieCutterImageView
import com.adamstyrc.cookiecutter.CookieCutterShape
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_crop_image.*
import kotlinx.android.synthetic.main.activity_select_image.*

// Activities flow:
// This activity is started by SelectImageActivity to be running below the custom image picker
//  in the stack, to receive and process the image result returned by the custom image picker.
// When this activity finishes, it saves the cropped image in internal storage and sets a key
// in Shared Preferences that a custom image has been selected.
// It does not need to finish LoadCustomImageActivity as this finishes on user confirming image choice.
// The stack below this activity will always be MainMenuActivity, SelectStyleActivity,
// SelectImageActivity. Therefore on finishing this activity the user
// will return to SelectImageActivity.
class CropImageActivity : AppCompatActivity() {
    val TAG = "CropImageActivity"
    var imageWidth: Int = 0 // should load from resources after initialisation
    var imageHeight: Int = 0 // same
    lateinit var programmaticCrop: CookieCutterImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)
        imageWidth = resources.getInteger(R.integer.standard_sacred_image_width)
        imageHeight = resources.getInteger(R.integer.standard_sacred_image_height)
        finishedCropButton.setOnClickListener {
            val cropped = programmaticCrop.croppedBitmap
            // find out what filename is currently *not* referenced by the shrine
            //val prefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)
            val filenameInUse = Paper.book().read(getString(R.string.database_key_current_image_filename), "")
            val newName: String
            if (filenameInUse.equals(getString(R.string.custom_image_filename_a))){
                newName = getString(R.string.custom_image_filename_b)
            }
            else { // if was originally file "B" OR if it was blank, use file "A"
                newName = getString(R.string.custom_image_filename_a)
            }
            Log.d(TAG, "Saving cropped image width: " + cropped.width + ", orig width: " + crop.width)
            val bitmapManager = BitmapManager()
            bitmapManager.save(this, cropped, newName)
            Log.d(TAG, "Setting successful result and finishing")
            val passFilename = Intent()
            passFilename.putExtra("filename", newName)
            setResult(Activity.RESULT_OK, passFilename)
            finish()
        }


        val loadCustomImage = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        Log.d(TAG, "Started load custom image activity")
        startActivityForResult(loadCustomImage, resources.getInteger(R.integer.request_load_image_code))
        //Log.d(TAG, "End of creation - Kevin debug - width: " + crop.width + ", height: " + crop.height)
    }

    override fun onResume() {
        super.onResume()
        //Log.d(TAG, "Resuming! Kevin debug. crop.params.shape is hole: " + (crop.params.shape == CookieCutterShape.HOLE))
        //Log.d(TAG, "Resuming - Kevin debug - width: " + crop.width + ", height: " + crop.height)

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == resources.getInteger(R.integer.request_load_image_code)) {
            Log.d(TAG, "Result from image load:")
            if (resultCode == Activity.RESULT_OK) {
                if(data!!.data != null) {
                    try {
                        // rebuild view to avoid bugginess
                        val params = ConstraintLayout.LayoutParams(crop.layoutParams)
                        cropImageLayout.removeView(crop)
                        programmaticCrop = CookieCutterImageView(this)
                        programmaticCrop.params.shape = CookieCutterShape.HOLE
                        programmaticCrop.setImageURI(data.data)

                        cropImageLayout.addView(programmaticCrop, params)
                        //crop.onImageLoaded()
                        Log.d(TAG, "Newcrop added, width; " + programmaticCrop.width)
                        Log.d(TAG, "Image picking successful.")

                    }
                    catch (npe: NullPointerException) {
                        Log.d(TAG, "Image picking completed but URI was not resolved to a bitmap.")
                        // finish here because there's nothing to crop

                        setResult(Activity.RESULT_CANCELED)
                        // following the style I've seen in examples, methods are called on
                        // the builder, not the alert itself
                        val alertBuilder = AlertDialog.Builder(this)
                        alertBuilder.setMessage("This image could not be opened. It may be damaged or in an unsupported format.")
                        // set finish on clicking OK
                        alertBuilder.setNeutralButton("OK", DialogInterface.OnClickListener({dialog: DialogInterface?, which: Int ->  finish()}))
                        alertBuilder.show()

                    }
                }
                else {
                    Log.d(TAG, "Image picking completed but null returned. Finishing crop activity to return to select image.")
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
            // if nothing came back from the image picking, e.g. if the user
            // tapped back button, then go back to image select with a negative result
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG,"Image picking cancelled. Finishing crop activity to return to select image.")
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }
}