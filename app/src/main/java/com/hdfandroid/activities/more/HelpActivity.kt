/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.activities.more

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.hdfandroid.R
import com.hdfandroid.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_help.*

class HelpActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        go_back.setOnClickListener { finish() }

        see_docs.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.https_docs_hdfchain_org)))
            startActivity(browserIntent)
        }
    }
}