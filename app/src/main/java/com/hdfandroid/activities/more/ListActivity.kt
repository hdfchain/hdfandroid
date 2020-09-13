/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.activities.more

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.hdfandroid.R
import com.hdfandroid.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_about.*

@SuppressLint("Registered")
open class ListActivity : BaseActivity() {

    open var items = arrayOf<ListItem>()

    lateinit var adapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        adapter = ListAdapter(this, items)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter

        go_back.setOnClickListener {
            finish()
        }
    }

    override fun setTitle(title: CharSequence?) {
        app_bar_title.text = title
    }
}