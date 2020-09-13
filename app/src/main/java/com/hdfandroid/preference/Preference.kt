/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.preference

import android.content.Context
import android.view.View
import com.hdfandroid.util.WalletData

open class Preference(private val context: Context, private val key: String, private val view: View) {

    open val multiWallet = WalletData.multiWallet

    init {

    }
}