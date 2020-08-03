package com.example.candleshrine

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_strike_match.*

// This activity is opened by EditTextsActivity, and is over MainMenuActivity and EditTextsActivity
// in the stack. When it finishes, it also finishes EditTextsActivity, and opens FullscreenShrineActivity.
class StrikeMatchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strike_match)
        strikeMatchButton.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}