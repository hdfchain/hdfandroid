/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.hdfandroid.BuildConfig
import com.hdfandroid.R
import com.hdfandroid.data.Constants
import com.hdfandroid.data.Transaction
import com.hdfandroid.dialog.txdetails.TransactionDetailsDialog
import com.hdfandroid.extensions.hide
import com.hdfandroid.extensions.show
import com.hdfandroid.util.CoinFormat
import com.hdfandroid.util.WalletData
import hdflibwallet.Hdflibwallet
import hdflibwallet.Wallet
import kotlinx.android.synthetic.main.transaction_page_row.view.*

class TransactionPageAdapter(val context: Context, walletID: Long, val transactions: ArrayList<Transaction>) : RecyclerView.Adapter<TransactionListViewHolder>() {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val requiredConfirmations: Int
    private val wallet: Wallet

    init {
        val multiWallet = WalletData.multiWallet!!
        wallet = multiWallet.walletWithID(walletID)
        requiredConfirmations = when {
            multiWallet.readBoolConfigValueForKey(Hdflibwallet.SpendUnconfirmedConfigKey, Constants.DEF_SPEND_UNCONFIRMED) -> 0
            else -> Constants.REQUIRED_CONFIRMATIONS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionListViewHolder {
        val view = layoutInflater.inflate(R.layout.transaction_page_row, parent, false)
        return TransactionListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onBindViewHolder(holder: TransactionListViewHolder, position: Int) {

        // background ripple
        val backgroundResource: Int = when {
            itemCount == 1 -> R.drawable.ripple_bg_white_corners_8dp // only item on the list
            position == 0 -> R.drawable.transactions_row_top
            position == (itemCount - 1) -> R.drawable.transactions_row_bottom_bg
            else -> R.drawable.transactions_row_bg
        }

        holder.itemView.ripple_layout.setBackgroundResource(backgroundResource)

        // setting top & bottom margin for top and bottom rows.
        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams

        layoutParams.bottomMargin = when (position) {
            itemCount - 1 -> context.resources.getDimensionPixelSize(R.dimen.margin_padding_size_80)
            else -> 0
        }

        layoutParams.topMargin = when (position) {
            0 -> context.resources.getDimensionPixelSize(R.dimen.margin_padding_size_4)
            else -> 0
        }

        holder.itemView.layoutParams = layoutParams


        val transaction = transactions[position]

        holder.icon.setImageResource(transaction.iconResource)

        holder.itemView.ticket_price.hide()
        holder.itemView.days_to_vote.hide()
        holder.itemView.vote_reward.hide()

        if (transaction.confirmations < requiredConfirmations) {
            holder.status.setPending()
            holder.statusImg.setImageResource(R.drawable.ic_pending)
        } else {
            holder.status.setConfirmed(transaction.timestamp)
            holder.statusImg.setImageResource(R.drawable.ic_confirmed)
        }

        if (transaction.animate) {
            val blinkAnim = AnimationUtils.loadAnimation(holder.view.context, R.anim.anim_blink)
            holder.view.animation = blinkAnim
            transaction.animate = false
        }

        if (transaction.type == Hdflibwallet.TxTypeRegular) {
            val txAmount = if (transaction.direction == Hdflibwallet.TxDirectionSent) {
                -transaction.amount
            } else {
                transaction.amount
            }
            val strAmount = CoinFormat.formatHdfchain(txAmount)

            holder.amount.apply {
                text = CoinFormat.format(strAmount + Constants.NBSP + layoutInflater.context.getString(R.string.dcr), 0.7f)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.edit_text_size_20))
            }

            holder.itemView.ticket_price.hide()

        } else if (Hdflibwallet.txMatchesFilter(transaction.type, transaction.direction, Hdflibwallet.TxFilterStaking)) {

            holder.amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.edit_text_size_18))

            holder.itemView.ticket_price.apply {
                show()
                text = CoinFormat.format(transaction.amount, 0.715f)
            }

            var title = 0
            when (transaction.type) {
                Hdflibwallet.TxTypeTicketPurchase -> {
                    title = if (transaction.confirmations < BuildConfig.TicketMaturity) {
                        R.string.immature
                    } else {
                        if(wallet.ticketHasVotedOrRevoked(transaction.hash)){
                            R.string.purchased
                        } else {
                            R.string.live
                        }
                    }
                }
                Hdflibwallet.TxTypeVote -> {
                    title = R.string.vote
                }
                Hdflibwallet.TxTypeRevocation -> {
                    title = R.string.revoked
                }
            }

            if (transaction.type == Hdflibwallet.TxTypeVote || transaction.type == Hdflibwallet.TxTypeRevocation) {
                holder.itemView.vote_reward.apply {
                    text = CoinFormat.format(transaction.voteReward, 0.715f)
                    show()
                }

                holder.itemView.days_to_vote.apply {
                    val daysToVoteOrRevoke = transaction.daysToVoteOrRevoke
                    text = if (daysToVoteOrRevoke == 1) {
                        context.getString(R.string.one_day)
                    } else {
                        context.getString(R.string.x_days, daysToVoteOrRevoke)
                    }

                    show()
                }
            }

            holder.amount.setText(title)
        }

        holder.itemView.setOnClickListener {
            TransactionDetailsDialog(transaction).show(context)
        }

    }


}