/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.hdfandroid.R
import com.hdfandroid.adapter.AccountPickerAdapter
import com.hdfandroid.data.Account
import com.hdfandroid.extensions.fullCoinWalletsList
import com.hdfandroid.extensions.openedWalletsList
import com.hdfandroid.extensions.walletAccounts
import com.hdfandroid.util.WalletData
import kotlinx.android.synthetic.main.account_picker_sheet.*

class AccountPickerDialog(@StringRes val title: Int, val currentAccount: Account, private val showWatchOnlyWallets: Boolean,
                          val accountSelected: (account: Account) -> Unit?) : FullScreenBottomSheetDialog(),
        ViewTreeObserver.OnScrollChangedListener {

    private var layoutManager: LinearLayoutManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.account_picker_sheet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        account_picker_title.setText(title)

        val multiWallet = WalletData.multiWallet!!
        val wallets = if (showWatchOnlyWallets) multiWallet.openedWalletsList() else multiWallet.fullCoinWalletsList()

        val items = ArrayList<Any>()

        for (wallet in wallets) {
            items.add(wallet)
            val accounts = wallet.walletAccounts()
                    .dropLastWhile { it.accountNumber == Int.MAX_VALUE } // remove imported account
            items.addAll(accounts)
        }

        val adapter = AccountPickerAdapter(items.toTypedArray(), context!!, currentAccount) {
            dismiss()
            accountSelected(it)
        }
        layoutManager = LinearLayoutManager(context)
        account_picker_rv.layoutManager = layoutManager
        account_picker_rv.adapter = adapter

        account_picker_rv.viewTreeObserver.addOnScrollChangedListener(this)

        go_back.setOnClickListener {
            dismiss()
        }
    }

    override fun onScrollChanged() {
        val firstVisibleItem = layoutManager!!.findFirstCompletelyVisibleItemPosition()
        app_bar.elevation = if (firstVisibleItem != 0) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }
}