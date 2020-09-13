/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.extensions

import com.hdfandroid.data.Account
import com.hdfandroid.data.Constants
import com.hdfandroid.data.parseAccounts
import com.hdfandroid.util.WalletData
import hdflibwallet.Hdflibwallet
import hdflibwallet.Wallet

fun Wallet.walletAccounts(): ArrayList<Account> {
    return parseAccounts(this.accounts).accounts
}

fun Wallet.totalWalletBalance(): Long {
    val visibleAccounts = this.walletAccounts()

    return visibleAccounts.map { it.balance.total }.reduce { sum, element -> sum + element }
}
