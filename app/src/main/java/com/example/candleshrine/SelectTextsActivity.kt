/*
* Copyright (C) 2020 Kevin Higgins
* @author Kevin Higgins
* This class lets the user select from texts they have previously saved.
* Texts can only be chosen as a block combination of intention text and dedication text.
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

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import io.paperdb.Paper
import kotlinx.android.synthetic.main.activity_select_text.*

// Need to make sure this always loads the latest version from Paper
class SelectTextsActivity : AppCompatActivity() {
    val TAG = "SelectTextsActivity"
    val substringMaxLength = 10
    lateinit var loadedDedications: MutableList<String>
    lateinit var loadedIntentions: MutableList<String>
    lateinit var excerpts: Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_text)

        // by including a default value we avoid the possibility of nulls
        loadedDedications = Paper.book().read<MutableList<String>>("dedications", mutableListOf<String>())
        loadedIntentions = Paper.book().read("intentions", mutableListOf<String>())
        if (loadedDedications.size != loadedIntentions.size) { throw RuntimeException("Database corrupted. Lists not equal in size.") }
        // build the list of one-line excerpts from each pair of dedications and intentions
        // first initialise the backing array
        excerpts = Array<String>(loadedIntentions.size, { "" })
        for (i in 0..loadedIntentions.size - 1) {
            var excerpt: String
            // if the String is longer than the desired substring (maximum) length
            if (loadedDedications[i].length >= substringMaxLength) {
                excerpt = loadedDedications[i].substring(0, substringMaxLength)
            }
            // if it's shorter, take all of it
            else {
                excerpt = loadedDedications[i]
            }

            excerpt = excerpt.plus(" / ")
            // same checks for intentions, then we concatenate them to the excerpt
            if (loadedIntentions[i].length >= substringMaxLength) {
                excerpt = excerpt.plus(loadedIntentions[i].substring(0, substringMaxLength))
            } else {
                excerpt = excerpt.plus(loadedIntentions[i])
            }
            Log.d(TAG, "Concatenated String is " + excerpt)
            excerpts[i] = excerpt
        }
        // create an adapter to mediate between the array and the ListView
        savedTextsList.adapter = ArrayAdapter<String>(this, R.layout.activity_list_view, R.id.savedTextsDisplay, excerpts)


        savedTextsList.setOnItemClickListener { parent, view, position, id ->
            // supports lists up to Integer.MAX_VALUE in size
            val texts = Bundle()
            texts.putString("dedication", loadedDedications[id.toInt()] )
            texts.putString("intention", loadedIntentions[id.toInt()])
            val intent = Intent()
            intent.putExtra("texts", texts)
            setResult(-1, intent)
            Log.d(TAG, "Finishing texts selection")
            finish()
        }
    }
}