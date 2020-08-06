/*
 * Copyright (C) 2020 Kevin Higgins
 * @author Kevin Higgins
 * This class lets the user enter messages to go along with the candle
 * they are lighting. It also lets the user save and restore messages,
 * by calling SelectTextsActivity.
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
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_edit_texts.*

class EditTextsActivity : AppCompatActivity() {
    private val selectTextsCode = 2
    private val TAG = "EditTextsActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_texts)

        finishedButton.setOnClickListener {
            if (saveCheckbox.isChecked) {
                var dedications = Paper.book().read<MutableList<String>>("dedications")
                var intentions = Paper.book().read<MutableList<String>>("intentions")
                Log.d(TAG, "Loaded texts lists from database")
                if (dedications == null) {
                    dedications = mutableListOf<String>()
                }
                if (intentions == null) {
                    intentions = mutableListOf<String>()
                }
                dedications.add(dedicationField.text.toString())
                intentions.add(intentionField.text.toString())
                Paper.book().write("dedications", dedications)
                Paper.book().write("intentions", intentions)
                Log.d(TAG, "Updated database with current texts")
            }
            val strikeMatch = Intent()
            strikeMatch.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".StrikeMatchActivity")))
            startActivityForResult(strikeMatch, resources.getInteger(R.integer.request_strike_match_code))
        }
        selectButton.setOnClickListener {
            val intent = Intent()
            intent.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".SelectTextsActivity")))
            Log.d(TAG, "Starting SelectTextsActivity")
            startActivityForResult(intent, resources.getInteger(R.integer.request_select_texts_code))
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Activity resultCode is " + resultCode + ", requestCode is " + requestCode)
        if (requestCode == resources.getInteger(R.integer.request_select_texts_code)) {
            if (resultCode == Activity.RESULT_OK) {
                // doesn't take String
                dedicationField.setText(data!!.getBundleExtra("texts").get("dedication") as CharSequence)
                intentionField.setText(data.getBundleExtra("texts").get("intention") as CharSequence)
                Log.d(TAG,"Texts updated from text selection activity.")
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG,"Texts selection cancelled.")
            }
        }
        else if (requestCode == resources.getInteger(R.integer.request_strike_match_code)) {
            if (resultCode == Activity.RESULT_OK) {

                Log.d(TAG,"Strike match activity returned successful, so also finishing edit texts activity and starting fullscreen shrine.")
                val fullscreenShrine = Intent()
                fullscreenShrine.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".FullscreenShrineActivity")))
                startActivity(fullscreenShrine)
                finish()
            }
        }
    }
}