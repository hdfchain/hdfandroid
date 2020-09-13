/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.hdfandroid.R
import com.hdfandroid.data.Constants

class SeedBackupSuccess : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seed_verification_success)
    }

    fun backToWallets(v: View?) {
        val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)
        val data = Intent()
        data.putExtra(Constants.WALLET_ID, walletID)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onBackPressed() {
        backToWallets(null)
    }

}