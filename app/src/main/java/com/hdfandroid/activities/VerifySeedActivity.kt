/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hdfandroid.R
import com.hdfandroid.adapter.InputSeed
import com.hdfandroid.adapter.ShuffledSeeds
import com.hdfandroid.adapter.VerifySeedAdapter
import com.hdfandroid.data.Constants
import com.hdfandroid.dialog.PasswordPromptDialog
import com.hdfandroid.dialog.PinPromptDialog
import com.hdfandroid.util.PassPromptTitle
import com.hdfandroid.util.PassPromptUtil
import com.hdfandroid.util.SnackBar
import com.hdfandroid.util.Utils
import hdflibwallet.Hdflibwallet
import hdflibwallet.Wallet
import kotlinx.android.synthetic.main.verify_seed_page.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifySeedActivity : BaseActivity() {

    private lateinit var seeds: Array<String>
    private lateinit var allSeeds: Array<String>

    private lateinit var verifySeedAdapter: VerifySeedAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var wallet: Wallet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(R.layout.verify_seed_page)

        linearLayoutManager = LinearLayoutManager(this)
        recycler_view_seeds.layoutManager = linearLayoutManager

        allSeeds = Hdflibwallet.AlternatingWords.split("\n").toTypedArray()
        prepareData()

        recycler_view_seeds.viewTreeObserver.addOnScrollChangedListener {

            val firstVisibleItem = linearLayoutManager.findFirstCompletelyVisibleItemPosition()

            app_bar.elevation = if (firstVisibleItem != 0) {
                resources.getDimension(R.dimen.app_bar_elevation)
            } else {
                0f
            }
        }

        btn_verify.setOnClickListener {
            verifySeed()
        }

        go_back.setOnClickListener {
            finish()
        }
    }

    private fun verifySeed() {

        val title = PassPromptTitle(R.string.confirm_verify_seed, R.string.confirm_verify_seed, R.string.confirm_verify_seed)
        PassPromptUtil(this, wallet!!.id, title, allowFingerprint = true) { passDialog, pass ->
            if (pass == null) {
                return@PassPromptUtil true
            }

            GlobalScope.launch(Dispatchers.Default) {
                val op = this@VerifySeedActivity.javaClass.name + ": " + this.javaClass.name + ": verifySeed"
                try {
                    val seedMnemonic = verifySeedAdapter.enteredSeeds.joinToString(" ")
                    multiWallet!!.verifySeedForWallet(wallet!!.id, seedMnemonic, pass.toByteArray())
                    val data = Intent(this@VerifySeedActivity, SeedBackupSuccess::class.java)
                    data.putExtra(Constants.WALLET_ID, wallet!!.id)
                    data.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    startActivity(data)
                    finish()
                    passDialog?.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()

                    if (passDialog is PinPromptDialog) {
                        passDialog.setProcessing(false)
                    } else if (passDialog is PasswordPromptDialog) {
                        passDialog.setProcessing(false)
                    }

                    if (e.message == Hdflibwallet.ErrInvalidPassphrase) {
                        if (passDialog is PinPromptDialog) {
                            passDialog.showError()
                        } else if (passDialog is PasswordPromptDialog) {
                            passDialog.showError()
                        }
                    } else if (e.message == Hdflibwallet.ErrInvalid) {
                        passDialog?.dismiss()
                        SnackBar.showError(this@VerifySeedActivity, R.string.seed_verification_failed)
                    } else {
                        withContext(Dispatchers.Main) {
                            Hdflibwallet.logT(op, e.message)
                            Utils.showErrorDialog(this@VerifySeedActivity, op + ": " + e.message)
                        }
                    }
                }
            }

            return@PassPromptUtil false
        }.show()
    }

    private fun prepareData() {

        val walletId = intent.getLongExtra(Constants.WALLET_ID, -1)
        wallet = multiWallet!!.walletWithID(walletId)

        val seed = intent.getStringExtra(Constants.SEED)

        if (seed.isNotBlank()) {
            seeds = seed!!.split(Constants.NBSP.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            initSeedAdapter()
        }
    }

    private fun getMultiSeedList(): ArrayList<ShuffledSeeds> {
        val multiSeedList = ArrayList<ShuffledSeeds>()
        for (seed in seeds) {
            multiSeedList.add(getMultiSeed(allSeeds.indexOf(seed)))
        }

        return multiSeedList
    }

    private fun getMultiSeed(realSeedIndex: Int): ShuffledSeeds {

        val list = (0 until 33).toMutableList()
        list.remove(realSeedIndex)

        val firstRandom = list.random()
        val firstInputSeed = InputSeed(firstRandom, allSeeds[firstRandom])
        list.remove(firstRandom)

        val secondRandom = list.random()
        val secondInputSeed = InputSeed(secondRandom, allSeeds[secondRandom])

        val realInputSeed = InputSeed(realSeedIndex, allSeeds[realSeedIndex])

        val arr = arrayListOf(firstInputSeed, secondInputSeed, realInputSeed).apply { shuffle() }.toTypedArray()
        return ShuffledSeeds(arr)
    }

    private fun initSeedAdapter() {
        val allSeedWords = getMultiSeedList()
        verifySeedAdapter = VerifySeedAdapter(this, allSeedWords) { seedIndex ->
            linearLayoutManager.scrollToPosition(seedIndex + 2)
            btn_verify.isEnabled = verifySeedAdapter.allSeedsSelected
        }
        recycler_view_seeds.adapter = verifySeedAdapter
    }

}