package com.example.candleshrine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // initialise the database library used for texts
        Paper.init(this)

        // First task is to decide which layout to use - depending on whether there is a shrine saved
        if (savedInstanceState != null && savedInstanceState.containsKey("lastKnownIsShrineBuilt")) {
            lastKnownIsShrineBuilt = savedInstanceState.getBoolean("lastKnownIsShrineBuilt")
            Log.d(TAG, "Restoring is shrine built flag from saved instance state, result: " + lastKnownIsShrineBuilt)
        }
        else {
            lastKnownIsShrineBuilt = Paper.book().read(getString(R.string.preferences_key_shrine_built), false)
            Log.d(TAG, "Restoring is shrine built flag from database, result: " + lastKnownIsShrineBuilt)
        }


        // not sure if I should put other listeners inside this if statement's clauses... prob yes
        if (lastKnownIsShrineBuilt) {
            setContentView(R.layout.activity_main_menu)
            Log.d(TAG, "Created standard main menu")
            debugClearButton.setOnClickListener {
                debugClearDatabase()
                lastKnownIsShrineBuilt = false
                recreate()
            }
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
            debugClearNoShrine.setOnClickListener {
                debugClearDatabase()
                recreate()
            }
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
        val updatedIsShrineBuilt = Paper.book().read<Boolean>(getString(R.string.preferences_key_shrine_built), false)
        if (updatedIsShrineBuilt != lastKnownIsShrineBuilt) {//sharedPrefs.getBoolean(getString(R.string.preferences_key_shrine_built), false) != lastKnownIsShrineBuilt) {
            lastKnownIsShrineBuilt = updatedIsShrineBuilt
            recreate()
        }
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



}