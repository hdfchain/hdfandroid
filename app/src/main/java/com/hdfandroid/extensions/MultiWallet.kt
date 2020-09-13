/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.extensions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import hdflibwallet.MultiWallet
import hdflibwallet.Wallet

fun MultiWallet.openedWalletsList(): ArrayList<Wallet> {
    val wallets = ArrayList<Wallet>()

    val openedWalletsJson = this.openedWalletIDs()
    val gson = Gson()
    val listType = object : TypeToken<ArrayList<Long>>() {}.type
    val openedWalletsTemp = gson.fromJson<ArrayList<Long>>(openedWalletsJson, listType)

    for (walletId in openedWalletsTemp) {
        val wallet = this.walletWithID(walletId)
        wallets.add(wallet)
    }

    wallets.sortBy { it.id }

    return wallets
}

fun MultiWallet.fullCoinWalletsList(): ArrayList<Wallet> {
    return this.openedWalletsList().filter { !it.isWatchingOnlyWallet } as ArrayList<Wallet>
}

fun MultiWallet.watchOnlyWalletsList(): ArrayList<Wallet> {
    return this.openedWalletsList().filter { it.isWatchingOnlyWallet } as ArrayList<Wallet>
}

fun MultiWallet.totalWalletBalance(): Long {
    val wallets = this.openedWalletsList()
    var totalBalance: Long = 0

    for (wallet in wallets) {
        totalBalance += wallet.totalWalletBalance()
    }

    return totalBalance
}