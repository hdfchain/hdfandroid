/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hdfandroid.activities.BaseActivity
import com.hdfandroid.adapter.NavigationTabsAdapter
import com.hdfandroid.data.Constants
import com.hdfandroid.data.Transaction
import com.hdfandroid.dialog.*
import com.hdfandroid.dialog.send.SendDialog
import com.hdfandroid.dialog.txdetails.TransactionDetailsDialog
import com.hdfandroid.extensions.hide
import com.hdfandroid.extensions.openedWalletsList
import com.hdfandroid.extensions.show
import com.hdfandroid.fragments.MultiWalletTransactions
import com.hdfandroid.fragments.OverviewFragment
import com.hdfandroid.fragments.TransactionsFragment
import com.hdfandroid.fragments.WalletsFragment
import com.hdfandroid.fragments.more.MoreFragment
import com.hdfandroid.service.SyncService
import com.hdfandroid.util.NetworkUtil
import com.hdfandroid.util.SnackBar
import com.hdfandroid.util.Utils
import com.hdfandroid.util.WalletData
import com.google.gson.Gson
import hdflibwallet.*
import kotlinx.android.synthetic.main.activity_tabs.*
import java.text.DecimalFormat
import kotlin.math.roundToInt
import kotlin.system.exitProcess

const val TAG = "HomeActivity"

class HomeActivity : BaseActivity(), SyncProgressListener, TxAndBlockNotificationListener {

    private var deviceWidth: Int = 0
    private var blockNotificationSound: Int = 0
    private var lastBeepHeight: Int = -1
    private lateinit var alertSound: SoundPool

    private lateinit var adapter: NavigationTabsAdapter

    private lateinit var currentFragment: Fragment
    private var currentBottomSheet: FullScreenBottomSheetDialog? = null
    private var sendPageSheet: FullScreenBottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)
        setSupportActionBar(toolbar)

        if (walletData.multiWallet == null) {
            println("Restarting app")
            Utils.restartApp(this)
        }

        Utils.registerTransactionNotificationChannel(this)

        val builder = SoundPool.Builder().setMaxStreams(3)
        val attributes = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
        builder.setAudioAttributes(attributes)

        alertSound = builder.build()
        blockNotificationSound = alertSound.load(this, R.raw.beep, 1)

        try {
            multiWallet?.removeSyncProgressListener(TAG)
            multiWallet?.removeTxAndBlockNotificationListener(TAG)

            multiWallet?.addSyncProgressListener(this, TAG)
            multiWallet?.addTxAndBlockNotificationListener(this, TAG)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        initNavigationTabs()

        fab_receive.setOnClickListener {
            if (multiWallet!!.isSyncing) {
                SnackBar.showError(this, R.string.wait_for_sync)
                return@setOnClickListener
            } else if (!multiWallet!!.isConnectedToHdfchainNetwork) {
                SnackBar.showError(this, R.string.not_connected)
                return@setOnClickListener
            }
            currentBottomSheet = ReceiveDialog(bottomSheetDismissed)
            currentBottomSheet!!.show(this)
        }

        fab_send.setOnClickListener {

            if (multiWallet!!.allWalletsAreWatchOnly()) { // only wallet is watch only
                SnackBar.showError(this, R.string.watch_only_wallet_error)
                return@setOnClickListener
            } else if (multiWallet!!.isSyncing) {
                SnackBar.showError(this, R.string.wait_for_sync)
                return@setOnClickListener
            } else if (!multiWallet!!.isConnectedToHdfchainNetwork) {
                SnackBar.showError(this, R.string.not_connected)
                return@setOnClickListener
            }
            if (sendPageSheet == null) {
                sendPageSheet = SendDialog(this, bottomSheetDismissed)
            }
            sendPageSheet!!.show(this)
            currentBottomSheet = sendPageSheet
        }

        setupLogoAnim()
        Handler().postDelayed({ checkWifiSync() }, 1000)
    }

    private val bottomSheetDismissed = DialogInterface.OnDismissListener {
        currentBottomSheet = null
    }

    override fun onBackPressed() {
        if (currentFragment is OverviewFragment) {
            InfoDialog(this)
                    .setDialogTitle(getString(R.string.exit_app_prompt_title))
                    .setMessage(getString(R.string.exit_app_prompt_message))
                    .setPositiveButton(getString(R.string.yes), DialogInterface.OnClickListener { _, _ ->
                        finish()
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show()
        } else {
            switchFragment(OverviewFragment.FRAGMENT_POSITION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (multiWallet == null || multiWallet?.openedWalletsCount() == 0) {
            return
        }

        val syncIntent = Intent(this, SyncService::class.java)
        stopService(syncIntent)

        multiWallet?.shutdown()
        finish()
        exitProcess(1)
    }

    private fun initNavigationTabs() {

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay
                .getMetrics(displayMetrics)
        deviceWidth = displayMetrics.widthPixels

        val mLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recycler_view_tabs.layoutManager = mLayoutManager

        adapter = NavigationTabsAdapter(this, 0, deviceWidth, multiWallet!!.numWalletsNeedingSeedBackup()) { position ->
            switchFragment(position)
        }
        recycler_view_tabs.adapter = adapter

        switchFragment(0)

    }

    fun refreshNavigationTabs() {
        adapter.backupsNeeded = multiWallet!!.numWalletsNeedingSeedBackup()
        adapter.notifyItemChanged(2) // Wallets Page
    }

    private fun setTabIndicator() {
        tab_indicator.post {
            val tabWidth = deviceWidth / 4
            val tabIndicatorWidth = resources.getDimension(R.dimen.tab_indicator_width)

            var leftMargin = tabWidth * adapter.activeTab
            leftMargin += ((tabWidth - tabIndicatorWidth) / 2f).roundToInt()

            ObjectAnimator.ofFloat(tab_indicator, View.TRANSLATION_X, leftMargin.toFloat()).apply {
                duration = 350
                start()
            }
        }
    }

    private fun showOrHideFab(position: Int) {
        send_receive_layout.post {
            if (position < 2) { // show send and receive buttons for overview & transactions page
                send_receive_layout.show()
                ObjectAnimator.ofFloat(send_receive_layout, View.TRANSLATION_Y, 0f).setDuration(350).start() // bring view down
            } else {
                val objectAnimator = ObjectAnimator.ofFloat(send_receive_layout, View.TRANSLATION_Y, send_receive_layout.height.toFloat())

                objectAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        send_receive_layout.hide()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }
                })

                objectAnimator.duration = 350

                objectAnimator.start()
            }
        }
    }

    fun switchFragment(position: Int) {

        currentFragment = when (position) {
            0 -> OverviewFragment()
            1 -> {
                if (multiWallet!!.openedWalletsCount() > 1) {
                    MultiWalletTransactions()
                } else {
                    val wallet = multiWallet!!.openedWalletsList()[0]
                    TransactionsFragment().setWalletID(wallet.id)
                }
            }
            2 -> WalletsFragment()
            else -> MoreFragment()
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame, currentFragment)
                .commit()

        setTabIndicator()

        showOrHideFab(position)

        adapter.changeActiveTab(position)
    }

    fun setToolbarTitle(title: CharSequence, showShadow: Boolean) {
        toolbar_title.text = title
        app_bar.elevation = if (showShadow) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }

    fun checkWifiSync() {
        if (!multiWallet!!.readBoolConfigValueForKey(Hdflibwallet.SyncOnCellularConfigKey, Constants.DEF_SYNC_ON_CELLULAR)) {
            // Check if wifi is connected
            val isWifiConnected = this.let { NetworkUtil.isWifiConnected(it) }
            if (!isWifiConnected) {
                showWifiNotice()
                return
            }
        }

        startSyncing()
    }

    private fun showWifiNotice() {
        val wifiSyncDialog = WiFiSyncDialog(this)
                .setPositiveButton(DialogInterface.OnClickListener { dialog, _ ->
                    startSyncing()

                    val syncDialog = dialog as WiFiSyncDialog
                    multiWallet!!.setBoolConfigValueForKey(Hdflibwallet.SyncOnCellularConfigKey, syncDialog.checked)

                })

        wifiSyncDialog.setOnCancelListener {
            sendBroadcast(Intent(Constants.SYNCED))
        }

        wifiSyncDialog.show()
    }

    fun startSyncing() {
        for (w in multiWallet!!.openedWalletsList()) {
            if (!w.hasDiscoveredAccounts && w.isLocked) {
                ResumeAccountDiscovery()
                        .setWalletID(w.id)
                        .show(supportFragmentManager, ResumeAccountDiscovery::javaClass.name)
                return
            }
        }
        sendBroadcast(Intent(Constants.SYNCED))
        val syncIntent = Intent(this, SyncService::class.java)
        startService(syncIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        currentBottomSheet?.onActivityResult(requestCode, resultCode, data)
    }

    // -- Block Notification

    override fun onTransactionConfirmed(walletID: Long, hash: String, blockHeight: Int) {
    }

    override fun onBlockAttached(walletID: Long, blockHeight: Int) {
        if (lastBeepHeight == -1 || blockHeight > lastBeepHeight) {
            lastBeepHeight = blockHeight
            val beepNewBlocks = multiWallet!!.readBoolConfigValueForKey(Hdflibwallet.BeepNewBlocksConfigKey, false)
            if (beepNewBlocks && !multiWallet!!.isSyncing) {
                alertSound.play(blockNotificationSound, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    override fun onTransaction(transactionJson: String?) {
        val gson = Gson()
        val transaction = gson.fromJson(transactionJson, Transaction::class.java)

        if (transaction.direction == Hdflibwallet.TxDirectionReceived) {
            val dcrFormat = DecimalFormat("#.######## DCR")

            val amount = Hdflibwallet.amountCoin(transaction.amount)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            Utils.sendTransactionNotification(this, notificationManager, dcrFormat.format(amount),
                    transaction)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) {
            return
        }
        val tx = intent.getSerializableExtra(Constants.TRANSACTION) as Transaction
        TransactionDetailsDialog(
                Transaction.from(multiWallet!!.walletWithID(tx.walletID).getTransaction(tx.hashBytes))
        ).show(this)
    }

    // -- Sync Progress Listener

    override fun onSyncStarted(wasRestarted: Boolean) {
    }

    override fun onHeadersRescanProgress(headersRescanProgress: HeadersRescanProgressReport?) {
    }

    override fun onAddressDiscoveryProgress(addressDiscoveryProgress: AddressDiscoveryProgressReport?) {
    }

    override fun onSyncCanceled(willRestart: Boolean) {
    }

    override fun onPeerConnectedOrDisconnected(numberOfConnectedPeers: Int) {
        WalletData.instance.peers = numberOfConnectedPeers
    }

    override fun onSyncCompleted() {
    }

    override fun onHeadersFetchProgress(headersFetchProgress: HeadersFetchProgressReport?) {
    }

    override fun onSyncEndedWithError(err: java.lang.Exception?) {
    }

    override fun debug(debugInfo: DebugInfo?) {
    }
}

private fun HomeActivity.setupLogoAnim() {
    val runnable = Runnable {
        val anim = AnimationUtils.loadAnimation(this, R.anim.logo_anim)
        home_logo.startAnimation(anim)
    }

    val handler = Handler()
    toolbar_title.setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> handler.postDelayed(runnable, 10000)
            MotionEvent.ACTION_UP -> handler.removeCallbacks(runnable)
        }
        true
    }
}