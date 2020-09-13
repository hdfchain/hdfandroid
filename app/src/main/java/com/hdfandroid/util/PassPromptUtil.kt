/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.util

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.hdfandroid.R
import com.hdfandroid.data.Constants
import com.hdfandroid.dialog.FullScreenBottomSheetDialog
import com.hdfandroid.dialog.PasswordPromptDialog
import com.hdfandroid.dialog.PinPromptDialog
import hdflibwallet.Hdflibwallet

data class PassPromptTitle(val passwordTitle: Int, val pinTitle: Int, val fingerprintTitle: Int = pinTitle)

class PassPromptUtil(private val fragmentActivity: FragmentActivity, val walletID: Long?, val title: PassPromptTitle, private val allowFingerprint: Boolean,
                     private val passEntered: (dialog: FullScreenBottomSheetDialog?, passphrase: String?) -> Boolean) {

    var passType: Int = Hdflibwallet.PassphraseTypePass

    fun show() {
        val multiWallet = WalletData.multiWallet!!

        passType = if (walletID != null) {
            multiWallet.walletWithID(walletID).privatePassphraseType
        } else {
            multiWallet.startupSecurityType()
        }

        val useFingerPrint = if (walletID == null) {
            multiWallet.readBoolConfigValueForKey(Hdflibwallet.UseBiometricConfigKey, Constants.DEF_USE_FINGERPRINT)
        } else {
            multiWallet.readBoolConfigValueForKey(walletID.toString() + Hdflibwallet.UseBiometricConfigKey, Constants.DEF_USE_FINGERPRINT)
        }

        if (allowFingerprint && useFingerPrint && BiometricUtils.isFingerprintEnrolled(fragmentActivity)) {
            showFingerprintDialog(walletID)
        } else {
            showPasswordOrPin()
        }
    }

    private fun showPasswordOrPin() {
        val isSpendingPass = walletID != null

        if (passType == Hdflibwallet.PassphraseTypePass) {
            showPasswordDialog(isSpendingPass)
        } else {
            showPinDialog(isSpendingPass)
        }
    }

    private fun showPinDialog(isSpendingPass: Boolean) {
        val pinPromptDialog = PinPromptDialog(title.pinTitle, isSpendingPass, passEntered)
        pinPromptDialog.isCancelable = false
        pinPromptDialog.show(fragmentActivity)
    }

    private fun showPasswordDialog(isSpendingPass: Boolean) {
        val passwordPromptDialog = PasswordPromptDialog(title.passwordTitle, isSpendingPass, passEntered)
        passwordPromptDialog.isCancelable = false
        passwordPromptDialog.show(fragmentActivity)
    }

    private fun showFingerprintDialog(walletID: Long?) {
        if (BiometricUtils.isFingerprintEnrolled(fragmentActivity)) {

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(fragmentActivity.getString(title.fingerprintTitle))

            val negativeButtonText = if (passType == Hdflibwallet.PassphraseTypePass) {
                fragmentActivity.getString(R.string.use_password)
            } else {
                fragmentActivity.getString(R.string.use_pin)
            }

            promptInfo.setNegativeButtonText(negativeButtonText)

            BiometricUtils.displayBiometricPrompt(fragmentActivity, promptInfo.build(), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    val alias = if (walletID == null) {
                        Constants.STARTUP_PASSPHRASE
                    } else {
                        BiometricUtils.getWalletAlias(walletID)
                    }

                    val pass = BiometricUtils.readFromKeystore(fragmentActivity, alias)
                    passEntered(null, pass)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showPasswordOrPin()
                }
            })
        }
    }
}