/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.dialog.txdetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.text.HtmlCompat
import com.hdfandroid.BuildConfig
import com.hdfandroid.R
import com.hdfandroid.data.Constants
import com.hdfandroid.data.Transaction
import com.hdfandroid.dialog.FullScreenBottomSheetDialog
import com.hdfandroid.dialog.InfoDialog
import com.hdfandroid.extensions.hide
import com.hdfandroid.extensions.show
import com.hdfandroid.extensions.toggleVisibility
import com.hdfandroid.util.CoinFormat
import com.hdfandroid.util.SnackBar
import com.hdfandroid.util.Utils
import hdflibwallet.Hdflibwallet
import kotlinx.android.synthetic.main.transaction_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TransactionDetailsDialog(val transaction: Transaction) : FullScreenBottomSheetDialog(null), View.OnClickListener, ViewTreeObserver.OnScrollChangedListener {

    private val wallet = multiWallet.walletWithID(transaction.walletID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.transaction_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        tx_details_icon.setImageResource(transaction.iconResource)

        val txAmount = if (transaction.direction == Hdflibwallet.TxDirectionSent && transaction.type == Hdflibwallet.TxTypeRegular) {
            -transaction.amount
        } else {
            transaction.amount
        }
        tx_details_amount.text = CoinFormat.format(txAmount, 0.625f)

        val sdf = SimpleDateFormat(getString(R.string.date_time_format), Locale.getDefault())

        val symbols = DateFormatSymbols(Locale.getDefault())
        symbols.amPmStrings = arrayOf(getString(R.string.am), getString(R.string.pm))
        sdf.dateFormatSymbols = symbols
        val timestamp = sdf.format(transaction.timestampMillis)
        tx_timestamp.text = timestamp

        setConfirmationStatus()

        rebroadcast_button.setOnClickListener(this)
        view_dcrdata.setOnClickListener(this)
        iv_info.setOnClickListener(this)
        tx_details_id.setOnClickListener(this)
        tx_details_id.text = transaction.hash
        tx_details_type.text = transaction.type
        tx_details_fee.text = getString(R.string.x_dcr, CoinFormat.formatHdfchain(transaction.fee))

        when (transaction.type) {
            Hdflibwallet.TxTypeRegular -> {
                when (transaction.direction) {
                    Hdflibwallet.TxDirectionSent -> {
                        tx_source_row.show()
                        tx_details_source.text = getSourceAccount()

                        tx_source_wallet_badge.text = wallet.name
                        tx_source_wallet_badge.show()

                        tx_dest_row.show()
                        tx_details_dest.apply {
                            text = getDestinationAddress()
                            tx_details_dest.setOnClickListener(this@TransactionDetailsDialog)
                        }

                        toolbar_title.setText(R.string.sent)
                    }
                    Hdflibwallet.TxDirectionReceived -> {
                        tx_source_row.show()
                        tx_details_source.setText(R.string.external)

                        tx_dest_row.show()
                        tx_dest_label.setText(R.string.to_account)
                        tx_details_dest.text = getReceiveAccount()
                        tx_details_dest.setTextColor(resources.getColor(R.color.darkBlueTextColor))

                        tx_dest_wallet_badge.text = wallet.name
                        tx_dest_wallet_badge.show()

                        toolbar_title.setText(R.string.received)
                    }
                    else -> toolbar_title.setText(R.string.transferred)
                }
            }
            else -> {
                val title = when(transaction.type){
                    Hdflibwallet.TxTypeTicketPurchase -> R.string.ticket_purchase
                    Hdflibwallet.TxTypeVote -> R.string.vote
                    Hdflibwallet.TxTypeRevocation -> R.string.revoked
                    else -> R.string.tx_sort_coinbase
                }

                toolbar_title.setText(title)
            }
        }

        tv_toggle_details.setOnClickListener(this)
        tx_details_scroll.viewTreeObserver.addOnScrollChangedListener(this)
        populateInputOutput()
    }

    override fun onTxOrBalanceUpdateRequired(walletID: Long?) {
        super.onTxOrBalanceUpdateRequired(walletID)
        GlobalScope.launch(Dispatchers.Main) {
            setConfirmationStatus()
        }
    }

    private fun setConfirmationStatus() {
        val spendUnconfirmedFunds = multiWallet.readBoolConfigValueForKey(Hdflibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED)

        status_icon.setImageResource(transaction.getConfirmationIconRes(spendUnconfirmedFunds))
        tv_confirmations.setTextColor(context!!.getColor(R.color.blueGraySecondTextColor))

        if (transaction.confirmations >= Hdflibwallet.DefaultRequiredConfirmations || spendUnconfirmedFunds) {
            tv_confirmations.text = HtmlCompat.fromHtml(getString(R.string.tx_details_confirmations, transaction.confirmations), 0)

            tx_block_row.show()
            tx_block.text = transaction.height.toString()
        } else if (transaction.confirmations == 1) {
            tv_confirmations.text = HtmlCompat.fromHtml(getString(R.string.tx_details_pending_confirmation, transaction.confirmations), 0)

            tx_block_row.show()
            tx_block.text = transaction.height.toString()
        } else {
            tv_confirmations.apply {
                setText(R.string.pending)
                setTextColor(context.getColor(R.color.lightGrayTextColor))
            }
        }

        if (transaction.height == -1) {
            rebroadcast_button.show()
        } else {
            rebroadcast_button.hide()
        }
    }

    // returns first external output address if any
    private fun getDestinationAddress(): String? {
        for (output in transaction.outputs!!) {
            if (output.account == -1) {
                return output.address
            }
        }
        return null
    }

    private fun getSourceAccount(): String? {
        for (input in transaction.inputs!!) {
            if (input.accountNumber != null && input.accountNumber != -1) {
                return input.accountName
            }
        }

        return null
    }

    // returns first internal output address if any
    private fun getReceiveAccount(): String? {
        for (output in transaction.outputs!!) {
            if (output.account != -1) {
                return output.accountName
            }
        }
        return null
    }

    private fun populateInputOutput() {
        val inputs = ArrayList<DropDownItem>()
        for (input in transaction.inputs!!) {
            val amount = getString(R.string.tx_details_account, CoinFormat.formatHdfchain(input.amount), input.accountName)
            var inputBadge = ""
            if (input.accountNumber != null && input.accountNumber != -1) {
                inputBadge = wallet.name
            }
            inputs.add(DropDownItem(amount, input.previousOutpoint!!, inputBadge))
        }

        InputOutputDropdown(input_dropdown_layout, inputs.toTypedArray(), top_bar)

        val outputs = ArrayList<DropDownItem>()
        for (output in transaction.outputs!!) {
            val amount = getString(R.string.tx_details_account, CoinFormat.formatHdfchain(output.amount), output.accountName)
            var outputBadge = ""
            if (output.account != -1) {
                outputBadge = wallet.name
            }
            outputs.add(DropDownItem(amount, output.address!!, outputBadge))
        }

        InputOutputDropdown(output_dropdown_layout, outputs.toTypedArray(), isInput = false, toastAnchor = top_bar)
    }

    override fun onScrollChanged() {
        top_bar.elevation = if (tx_details_scroll.scrollY == 0) {
            0f
        } else {
            resources.getDimension(R.dimen.app_bar_elevation)
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_toggle_details -> {
                tx_extra_details.toggleVisibility()
                val strRes = if (tx_extra_details.visibility == View.VISIBLE) {
                    R.string.hide_details
                } else {
                    R.string.show_details
                }

                tv_toggle_details.setText(strRes)
            }
            R.id.tx_details_id -> {
                Utils.copyToClipboard(top_bar, tx_details_id.text.toString(), R.string.tx_id_copied)
            }
            R.id.tx_details_dest -> {
                Utils.copyToClipboard(top_bar, tx_details_dest.text.toString(), R.string.address_copy_text)
            }
            R.id.iv_info -> {
                val content = HtmlCompat.fromHtml(getString(R.string.tx_details_copy_info), 0)
                InfoDialog(context!!)
                        .setDialogTitle(getString(R.string.how_to_copy))
                        .setMessage(content)
                        .setPositiveButton(getString(R.string.got_it), null)
                        .show()
            }
            R.id.view_dcrdata -> {
                val url = if (BuildConfig.IS_TESTNET) {
                    "https://testnet.dcrdata.org/tx/" + transaction.hash
                } else {
                    "https://explorer.dcrdata.org/tx/" + transaction.hash
                }

                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
            R.id.rebroadcast_button -> {

                if (!multiWallet.isConnectedToDecredNetwork) {
                    SnackBar.showError(context!!, R.string.not_connected)
                    return
                }

                try {
                    wallet.publishUnminedTransactions()
                    SnackBar.showText(context!!, R.string.rebroadcast_tx_success)
                } catch (e: Exception) {
                    e.printStackTrace()
                    val op = "rebroadcast tx"
                    Utils.showErrorDialog(context!!, op + ": " + e.message)
                    Hdflibwallet.logT(op, e.message)
                }
            }
        }
    }

}