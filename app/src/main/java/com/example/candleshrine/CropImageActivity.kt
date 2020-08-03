package com.example.candleshrine

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.widget.ImageView
import com.adamstyrc.cookiecutter.CookieCutterShape
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop_image)
        finishedCropButton.setOnClickListener {
            val cropped = crop.croppedBitmap
            Log.d(TAG, "Parceling cropped image width: " + cropped.width + ", orig width: " + crop.width)
            val image = Intent()
            image.putExtra("image", crop.croppedBitmap)
            Log.d(TAG, "Setting successful result and finishing")
            setResult(Activity.RESULT_OK, image)
            finish()
        }


        crop.params.shape = CookieCutterShape.HOLE
        val loadCustomImage = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        Log.d(TAG, "Started load custom image activity")
        startActivityForResult(loadCustomImage, resources.getInteger(R.integer.request_load_image_code))
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == resources.getInteger(R.integer.request_load_image_code)) {
            Log.d(TAG, "Result from image load:")
            if (resultCode == Activity.RESULT_OK) {
                if(data!!.data != null) {
                    try {
                        crop.setImageURI(data.data)
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
                    Log.d(TAG, "Image picking completed but null returned.")
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG,"Image picking cancelled.")
            }
        }
    }
}