/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.util

import hdflibwallet.MultiWallet

/**
 * Created by collins on 2/24/18.
 */

class WalletData {
    var synced = false
    var peers = 0

    var multiWallet: MultiWallet? = null

    companion object {
        val instance = WalletData()
        val multiWallet
            get() = instance.multiWallet
    }


}
