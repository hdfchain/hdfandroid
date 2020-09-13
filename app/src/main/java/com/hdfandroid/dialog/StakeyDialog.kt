/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import com.hdfandroid.R

class StakeyDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        val stakey = ImageView(context)
        stakey.setImageResource(R.drawable.stakey_deal_with_it)
        setContentView(stakey)

        Thread(Runnable {
            try {
                Thread.sleep(7000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            dismiss()
        }).start()
    }
}