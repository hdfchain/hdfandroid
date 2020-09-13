/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.activities.more

import android.os.Bundle
import android.view.ViewTreeObserver
import com.hdfandroid.R
import com.hdfandroid.activities.BaseActivity
import com.hdfandroid.data.Constants
import com.hdfandroid.dialog.PasswordPromptDialog
import com.hdfandroid.dialog.PinPromptDialog
import com.hdfandroid.extensions.hide
import com.hdfandroid.extensions.show
import com.hdfandroid.fragments.PasswordPinDialogFragment
import com.hdfandroid.preference.EditTextPreference
import com.hdfandroid.preference.ListPreference
import com.hdfandroid.preference.SwitchPreference
import com.hdfandroid.util.*
import hdflibwallet.Hdflibwallet
import kotlinx.android.synthetic.main.settings_activity.*
import kotlinx.android.synthetic.main.settings_activity.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val IP_ADDRESS_REGEX = "^(?:(?:1\\d?\\d|[1-9]?\\d|2[0-4]\\d|25[0-5])\\.){3}(?:1\\d?\\d|[1-9]?\\d|2[0-4]\\d|25[0-‌​5])(?:[:]\\d+)?$"

class SettingsActivity : BaseActivity(), ViewTreeObserver.OnScrollChangedListener {

    private lateinit var enableStartupSecurity: SwitchPreference
    private lateinit var useFingerprint: SwitchPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        SwitchPreference(this, Hdflibwallet.SpendUnconfirmedConfigKey, spend_unconfirmed_funds)

        SwitchPreference(this, Hdflibwallet.BeepNewBlocksConfigKey, beep_new_blocks)
        SwitchPreference(this, Hdflibwallet.SyncOnCellularConfigKey, wifi_sync)

        enableStartupSecurity = SwitchPreference(this, Hdflibwallet.IsStartupSecuritySetConfigKey, startup_pin_password) { newValue ->
            if (newValue) {
                setupStartupSecurity()
            } else {
                removeStartupSecurity()
            }

            return@SwitchPreference !newValue
        }

        useFingerprint = SwitchPreference(this, Hdflibwallet.UseBiometricConfigKey, startup_security_fingerprint) { newValue ->
            if (newValue) {
                enableStartupFingerprint()
            } else {
                // clear passphrase from keystore by saving an empty passphrase
                BiometricUtils.saveToKeystore(this@SettingsActivity, "", Constants.STARTUP_PASSPHRASE)
            }

            return@SwitchPreference false
        }
        loadStartupSecurity()
        change_startup_security.setOnClickListener {
            ChangePassUtil(this, null).begin()
        }

        setCurrencyConversionSummary(multiWallet!!.readInt32ConfigValueForKey(Hdflibwallet.CurrencyConversionConfigKey, Constants.DEF_CURRENCY_CONVERSION))
        ListPreference(this, Hdflibwallet.CurrencyConversionConfigKey, Constants.DEF_CURRENCY_CONVERSION,
                R.array.currency_conversion, currency_conversion) {
            setCurrencyConversionSummary(it)
        }

        setPeerIP(multiWallet!!.readStringConfigValueForKey(Hdflibwallet.SpvPersistentPeerAddressesConfigKey))
        EditTextPreference(this, Hdflibwallet.SpvPersistentPeerAddressesConfigKey, R.string.peer_ip_dialog_title,
                R.string.peer_ip_pref_hint, R.string.invalid_peer_ip, spv_peer_ip, validateIPAddress) {
            setPeerIP(it)
        }

        EditTextPreference(this, Hdflibwallet.UserAgentConfigKey, R.string.user_agent_dialog_title, R.string.user_agent, null, user_agent)

        go_back.setOnClickListener {
            finish()
        }

        settings_scroll_view.viewTreeObserver.addOnScrollChangedListener(this)
    }

    private val validateIPAddress: (String) -> Boolean = {
        it.isBlank() || it.matches(Regex(IP_ADDRESS_REGEX))
    }

    private fun setCurrencyConversionSummary(index: Int) {
        val preferenceSummary = resources.getStringArray(R.array.currency_conversion)[index]
        currency_conversion.pref_subtitle.text = preferenceSummary
    }

    private fun setPeerIP(ip: String) {
        if (ip.isBlank()) {
            spv_peer_ip.pref_subtitle.hide()
        } else {
            spv_peer_ip.pref_subtitle.show()
            spv_peer_ip.pref_subtitle.text = ip
        }
    }

    private fun loadStartupSecurity() {
        if (multiWallet!!.readBoolConfigValueForKey(Hdflibwallet.IsStartupSecuritySetConfigKey, Constants.DEF_STARTUP_SECURITY_SET)) {
            change_startup_security.show()
            enableStartupSecurity.setChecked(true)

            if (BiometricUtils.isFingerprintEnrolled(this)) {
                startup_security_fingerprint.show()
            }
        } else {
            change_startup_security.hide()
            startup_security_fingerprint.hide()

            useFingerprint.setValue(false)
            enableStartupSecurity.setChecked(false)
        }
    }

    private fun enableStartupFingerprint() {
        val op = this.javaClass.name + ".enableStartupFingerprint"

        val title = PassPromptTitle(R.string.enter_startup_password, R.string.enter_startup_pin)
        PassPromptUtil(this@SettingsActivity, null, title, false) { dialog, passphrase ->

            if (passphrase == null) { // dialog was dismissed/cancelled
                return@PassPromptUtil true
            }

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    multiWallet!!.verifyStartupPassphrase(passphrase.toByteArray())
                    BiometricUtils.saveToKeystore(this@SettingsActivity, passphrase, Constants.STARTUP_PASSPHRASE)

                    withContext(Dispatchers.Main) {
                        useFingerprint.setValue(true)
                        dialog?.dismiss()
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()

                    if (e.message == Hdflibwallet.ErrInvalidPassphrase) {
                        if (dialog is PinPromptDialog) {
                            dialog.setProcessing(false)
                            dialog.showError()
                        } else if (dialog is PasswordPromptDialog) {
                            dialog.setProcessing(false)
                            dialog.showError()
                        }
                    } else {
                        dialog?.dismiss()
                        Hdflibwallet.logT(op, e.message)
                        Utils.showErrorDialog(this@SettingsActivity, op + ": " + e.message)
                    }

                    return@launch
                }
            }

            false
        }.show()
    }

    private fun setupStartupSecurity() {
        PasswordPinDialogFragment(R.string.create, false, isChange = false) { dialog, passphrase, passphraseType ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    multiWallet!!.setStartupPassphrase(passphrase.toByteArray(), passphraseType)
                    multiWallet!!.setInt32ConfigValueForKey(Hdflibwallet.StartupSecurityTypeConfigKey, passphraseType)
                    multiWallet!!.setBoolConfigValueForKey(Hdflibwallet.IsStartupSecuritySetConfigKey, true)

                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        loadStartupSecurity()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                    }
                }

            }
        }.show(this)
    }

    private fun removeStartupSecurity() {
        val title = PassPromptTitle(R.string.remove_startup_security, R.string.remove_startup_security, R.string.remove_startup_security)
        PassPromptUtil(this, null, title, false) { dialog, pass ->

            if (pass == null) {
                return@PassPromptUtil true
            }

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    multiWallet!!.removeStartupPassphrase(pass.toByteArray())

                    withContext(Dispatchers.Main) {
                        dialog?.dismiss()
                        loadStartupSecurity()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (e.message == Hdflibwallet.ErrInvalidPassphrase) {
                        if (dialog is PinPromptDialog) {
                            dialog.setProcessing(false)
                            dialog.showError()
                        } else if (dialog is PasswordPromptDialog) {
                            dialog.setProcessing(false)
                            dialog.showError()
                        }
                    }
                }

            }

            false
        }.show()
    }

    override fun onScrollChanged() {
        app_bar.elevation = if (settings_scroll_view.scrollY > 0) {
            resources.getDimension(R.dimen.app_bar_elevation)
        } else {
            0f
        }
    }
}