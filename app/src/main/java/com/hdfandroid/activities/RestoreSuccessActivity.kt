/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.hdfandroid.HomeActivity
import com.hdfandroid.R
import com.hdfandroid.data.Constants
import kotlinx.android.synthetic.main.activity_wallet_restored.*

class RestoreSuccessActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_restored)

        tv_get_started.setOnClickListener {

            if (multiWallet!!.openedWalletsCount() > 1) {

                val walletID = intent.getLongExtra(Constants.WALLET_ID, -1)

                val data = Intent()
                data.putExtra(Constants.WALLET_ID, walletID)
                setResult(Activity.RESULT_OK, data)
                finish()
            } else {
                val homeActivityIntent = Intent(this, HomeActivity::class.java)
                homeActivityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

                startActivity(homeActivityIntent)
                ActivityCompat.finishAffinity(this)
            }
        }
    }

    // disable back button
    override fun onBackPressed() {}
}