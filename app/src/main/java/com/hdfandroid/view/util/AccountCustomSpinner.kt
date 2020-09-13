/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.view.util

import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.hdfandroid.data.Account
import com.hdfandroid.data.Constants
import com.hdfandroid.dialog.AccountPickerDialog
import com.hdfandroid.extensions.*
import com.hdfandroid.util.CoinFormat
import com.hdfandroid.util.WalletData
import hdflibwallet.Wallet
import kotlinx.android.synthetic.main.account_custom_spinner.view.*

class AccountCustomSpinner(private val fragmentManager: FragmentManager, private val spinnerLayout: View, private val showWatchOnlyWallets: Boolean,
                           @StringRes val pickerTitle: Int, var selectedAccountChanged: ((AccountCustomSpinner) -> Unit?)? = null) : View.OnClickListener {

    val context = spinnerLayout.context

    private val multiWallet = WalletData.multiWallet
    var wallet: Wallet

    var selectedAccount: Account? = null
        set(value) {
            if (value != null) {
                if (value.walletID != wallet.id) {
                    wallet = multiWallet!!.walletWithID(value.walletID)
                }

                spinnerLayout.spinner_account_name.text = value.accountName
                spinnerLayout.spinner_wallet_name.text = wallet.name
                spinnerLayout.spinner_total_balance.text = CoinFormat.format(value.totalBalance)
            }

            field = value

            selectedAccountChanged?.let { it1 -> it1(this) }
        }

    init {
        // Set default selected account as "default"
        // account from the first opened wallet
        wallet = if (showWatchOnlyWallets) {
            multiWallet!!.openedWalletsList()[0]
        } else {
            multiWallet!!.fullCoinWalletsList()[0]
        }


        selectedAccount = Account.from(wallet.getAccount(Constants.DEF_ACCOUNT_NUMBER))
        spinnerLayout.setOnClickListener(this)

        val visibleAccounts = wallet.walletAccounts()
                .dropLastWhile { it.accountNumber == Int.MAX_VALUE }.size

        if (multiWallet.openedWalletsCount() == 1 && visibleAccounts == 1) {
            spinnerLayout.setOnClickListener(null)
            spinnerLayout.spinner_dropdown.visibility = View.INVISIBLE
        }
    }

    override fun onClick(v: View?) {
        AccountPickerDialog(pickerTitle, selectedAccount!!, showWatchOnlyWallets) {
            selectedAccount = it
            return@AccountPickerDialog Unit
        }.show(fragmentManager, null)
    }

    fun getCurrentAddress(): String {
        return wallet.currentAddress(selectedAccount!!.accountNumber)
    }

    fun getNewAddress(): String {
        return wallet.nextAddress(selectedAccount!!.accountNumber)
    }

    fun refreshBalance() {
        val account = wallet.getAccount(selectedAccount!!.accountNumber)
        selectedAccount = Account.from(account)
    }

    fun isVisible(): Boolean {
        return spinnerLayout.visibility == View.VISIBLE
    }

    fun show() {
        spinnerLayout.show()
    }

    fun hide() {
        spinnerLayout.hide()
    }

}