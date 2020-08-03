package com.example.candleshrine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
    lateinit var sharedPrefs: SharedPreferences
    var lastKnownIsShrineBuilt = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialise the database library used for texts
        Paper.init(this)

        // access shared preferences file
        sharedPrefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)

        val defaultValue = false
        lastKnownIsShrineBuilt = sharedPrefs.getBoolean(getString(R.string.preferences_key_shrine_built), defaultValue)
        // not sure if I should put other listeners inside this if statement's clauses... prob yes
        if (lastKnownIsShrineBuilt) {
            setContentView(R.layout.activity_main_menu)
            Log.d(TAG, "Created standard main menu")
            debugClearButton.setOnClickListener {
                debugClearDatabase()
                debugClearPrefs()
                recreate()
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
                debugClearPrefs()
                recreate()
            }
            buildShrineButton.setOnClickListener {
                val selectStyle = Intent()
                selectStyle.setComponent(ComponentName(this, getString(R.string.app_fullname).plus(".SelectStyleActivity")))
                //
                startActivity(selectStyle)
            }
        }

    }
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Resuming main menu, including check for change of shrine status")
        if (sharedPrefs.getBoolean(getString(R.string.preferences_key_shrine_built), false) != lastKnownIsShrineBuilt) {
            recreate()
        }
    }
    fun debugClearPrefs() {
        val keys = arrayOf(getString(R.string.preferences_key_style_index), getString(R.string.preferences_key_image_index), getString(R.string.preferences_key_shrine_built))
        var report = "Preferences keys deleted"
        var count = 0
        val prefs = getSharedPreferences(getString(R.string.preferences_filename), Context.MODE_PRIVATE)
        val editPrefs = prefs.edit()
        for (e in keys) {
            if (prefs.contains(e)) {
                report = report.plus(" - ")
                editPrefs.remove(e)
                count++
            }
        }
        editPrefs.apply()
        report = report.plus(count).plus(" keys deleted out of " ).plus(keys.size)
        Log.d(TAG, report)
    }
    fun debugClearDatabase() {
        val keys = arrayOf(getString(R.string.database_key_dedications), getString(R.string.database_key_intentions))
        var report = "Database keys deleted"
        thread {


            var count = 0
            for (e in keys) {
                if (Paper.book().contains(e)) {
                    report = report.plus(" - ")
                    Paper.book().delete(e)
                    count++
                }
            }
            report = report.plus(count).plus(" keys deleted out of ").plus(keys.size)
            Log.d(TAG, report)
        }
    }

}