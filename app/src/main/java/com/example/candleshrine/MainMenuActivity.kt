/*
* Copyright (C) 2020 Kevin Higgins
* @author Kevin Higgins
* This class shows the app's main menu, which has different options depending
* on whether a shrine has already been built, and whether a candle has
* recently been lit.
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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.ViewGroup
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_main_menu.*
import kotlinx.android.synthetic.main.activity_main_menu_no_shrine.*
import kotlin.concurrent.thread

// Activities flow:
// This is the launcher Activity. It always stays open at the bottom of the stack.
// In onResume() and onStart() it checks Shared Preferences for whether the candle is
// burning and whether a shrine has been built.
// If the shrine has not been built it displays the option "Build Shrine" leading to
// SelectStyleActivity; if it has already been built then this option is renamed "Edit Shrine".
// If the candle is not burning but the shrine has been built, it displays an option to "Light A
// Candle" which leads to EditTextsActivity.
// If the shrine has been built it also displays the option to "View Shrine" leading to
// FullscreenShrineActivity. Finally it has an option to turn on and off instructions.

class MainMenuActivity : AppCompatActivity() {
    val TAG = "MainMenuActivity"
    //lateinit var sharedPrefs: SharedPreferences
    var lastKnownIsShrineBuilt = false
    var lastKnownCandleTimestamp = 0L
    var candleIsLit = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialise the database library used for texts
        Paper.init(this)

        // First task is to decide which layout to use - depending on whether there is a shrine saved
        // and also if we should show "Light Candle".
        if (savedInstanceState != null && savedInstanceState.containsKey("lastKnownIsShrineBuilt") && savedInstanceState.containsKey("candleIsLit")) {
            lastKnownIsShrineBuilt = savedInstanceState.getBoolean("lastKnownIsShrineBuilt")
            if (savedInstanceState.getBoolean("candleIsLit")) {
                updateCandleStatus()
            }
            Log.d(TAG, "Restoring from saved instance state, shrine built: " + lastKnownIsShrineBuilt + ", candle lit: " + candleIsLit)
        }
        else {
            lastKnownIsShrineBuilt = Paper.book().read(getString(R.string.database_key_shrine_built), false)

            // check if candle has gone out, i.e. candle_duration_minutes has elapsed since any timestamp
            // if no timestamp found, we assume candle was last lit at beginning of Unix epoch, 1 January 1970
            lastKnownCandleTimestamp = Paper.book().read(getString(R.string.database_key_last_candle_lighting_timestamp), 0)
            updateCandleStatus()

            Log.d(TAG, "Restoring from database, shrine built: " + lastKnownIsShrineBuilt + ", candle lit: " + candleIsLit)
        }


        // not sure if I should put other listeners inside this if statement's clauses... prob yes
        if (lastKnownIsShrineBuilt) {
            setContentView(R.layout.activity_main_menu)
            if (candleIsLit) {
                lightCandleButton.setEnabled(false)
            }

            Log.d(TAG, "Created standard main menu")
            /*
            debugClearButton.setOnClickListener {
                debugClearDatabase()
                lastKnownIsShrineBuilt = false
                recreate()
            }

             */
            editShrineButton.setOnClickListener() {
                val selectStyle = Intent()
                selectStyle.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".SelectStyleActivity")))
                startActivity(selectStyle)
            }
            lightCandleButton.setOnClickListener {
                val lightCandle = Intent()
                lightCandle.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".EditTextsActivity")))

                startActivity(lightCandle)
            }
            viewShrineButton.setOnClickListener {
                val fullscreenShrine = Intent()
                fullscreenShrine.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".FullscreenShrineActivity")))
                startActivity(fullscreenShrine)
                finish()
            }
        }
        else {
            setContentView(R.layout.activity_main_menu_no_shrine)
            Log.d(TAG, "Created no-shrine main menu")
            /*
            debugClearNoShrine.setOnClickListener {
                debugClearDatabase()
                recreate()
            }

             */
            buildShrineButton.setOnClickListener {
                val selectStyle = Intent()
                selectStyle.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".SelectStyleActivity")))
                startActivity(selectStyle)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Resuming main menu, including check for change of shrine status")

        val updatedIsShrineBuilt = Paper.book().read<Boolean>(getString(R.string.database_key_shrine_built), false)
        if (updatedIsShrineBuilt != lastKnownIsShrineBuilt) {//sharedPrefs.getBoolean(getString(R.string.preferences_key_shrine_built), false) != lastKnownIsShrineBuilt) {
            lastKnownIsShrineBuilt = updatedIsShrineBuilt
            recreate()
        }
        lastKnownCandleTimestamp = Paper.book().read(getString(R.string.database_key_last_candle_lighting_timestamp), 0)
        updateCandleStatus()

    }
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putBoolean("lastKnownIsShrineBuilt", lastKnownIsShrineBuilt)
    }
    fun debugClearDatabase() {
        Log.d(TAG, "Database had " + Paper.book().allKeys.size + " keys.")
        Paper.book().destroy()
        Log.d(TAG, "After deletion, database has " + Paper.book().allKeys.size + " keys.")
    }

    fun updateCandleStatus() {
        Log.d(TAG, "Checking candle status")
        candleIsLit = (
                System.currentTimeMillis() -
                        lastKnownCandleTimestamp <
                        resources.getInteger(R.integer.candle_duration_minutes) * 60000)

        if (candleIsLit) {
            Log.d(TAG, "Candle status: lit")
            if (lightCandleButton != null) lightCandleButton.setEnabled(false)
        }
        else {
            Log.d(TAG, "Candle status: not lit")
            if (lightCandleButton != null) lightCandleButton.setEnabled(true)
        }
    }
}