/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.hdfandroid.HomeActivity
import com.hdfandroid.R
import com.hdfandroid.data.Constants
import com.hdfandroid.data.Transaction
import com.hdfandroid.dialog.InfoDialog
import hdflibwallet.Hdflibwallet
import hdflibwallet.Wallet
import java.io.*
import java.util.*

object Utils {

    fun renameDefaultAccountToLocalLanguage(context: Context, wallet: Wallet) {
        if (Locale.getDefault().language != Locale.ENGLISH.language) {
            val defaultAccountName = context.resources.getString(R.string._default)
            if (defaultAccountName != Constants.DEFAULT) {
                wallet.renameAccount(Constants.DEF_ACCOUNT_NUMBER, defaultAccountName)
            }
        }
    }

    fun getHash(hash: String): ByteArray? {
        val hashList = ArrayList<String>()
        val split = hash.split("".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if ((split.size - 1) % 2 == 0) {
            var d = ""
            var i = 0
            while (i < split.size - 1) {
                d += (split[split.size - 1 - (i + 1)] + split[split.size - 1 - i])
                hashList.add(split[split.size - 1 - (i + 1)] + split[split.size - 1 - i])
                i += 2
            }
            return hexStringToByteArray(d)
        } else {
            System.err.println("Invalid Hash")
        }
        return null
    }

    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun saveToClipboard(context: Context, text: String) {

        val sdk = Build.VERSION.SDK_INT
        if (sdk < Build.VERSION_CODES.HONEYCOMB) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
            clipboard.text = text
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData
                    .newPlainText(context.getString(R.string.your_address), text)
            clipboard.primaryClip = clip
        }
    }

    fun copyToClipboard(v: View, text: String, @StringRes successMessage: Int) {
        saveToClipboard(v.context, text)
        SnackBar.showText(v, successMessage, Toast.LENGTH_SHORT)
    }

    fun copyToClipboard(context: Context, text: String, @StringRes successMessage: Int) {
        saveToClipboard(context, text)
        SnackBar.showText(context, successMessage, Toast.LENGTH_SHORT)
    }

    fun readFromClipboard(context: Context): String {
        val sdk = Build.VERSION.SDK_INT
        if (sdk < Build.VERSION_CODES.HONEYCOMB) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
            return clipboard.text.toString()
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboard.hasPrimaryClip())
                return clipboard.primaryClip!!.getItemAt(0).text.toString()
        }
        return ""
    }

    fun translateError(ctx: Context, e: Exception): String {
        return when (e.message) {
            Hdflibwallet.ErrInsufficientBalance -> {
                if (!WalletData.instance.synced) {
                    ctx.getString(R.string.not_enough_funds_synced)
                } else ctx.getString(R.string.not_enough_funds)
            }
            Hdflibwallet.ErrEmptySeed -> ctx.getString(R.string.empty_seed)
            Hdflibwallet.ErrNotConnected -> ctx.getString(R.string.not_connected)
            Hdflibwallet.ErrPassphraseRequired -> ctx.getString(R.string.passphrase_required)
            Hdflibwallet.ErrWalletNotLoaded -> ctx.getString(R.string.wallet_not_loaded)
            Hdflibwallet.ErrInvalidPassphrase -> ctx.getString(R.string.invalid_passphrase)
            Hdflibwallet.ErrNoPeers -> ctx.getString(R.string.err_no_peers)
            else -> e.message!!
        }
    }

    fun showErrorDialog(ctx: Context, err: String) {
        InfoDialog(ctx)
                .setDialogTitle(ctx.getString(R.string.error_occurred))
                .setMessage(err)
                .setNegativeButton(ctx.getString(R.string._copy), DialogInterface.OnClickListener { _, _ ->
                    copyToClipboard(ctx, err, R.string.error_copied)
                })
                .setPositiveButton(ctx.getString(R.string.ok), null)
                .show()
    }

    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            val componentName = intent.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }

    fun registerTransactionNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(Constants.TRANSACTION_CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.importance = NotificationManager.IMPORTANCE_HIGH

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTransactionNotification(context: Context, manager: NotificationManager, amount: String,
                                    transaction: Transaction) {

        val multiWallet = WalletData.multiWallet!!
        val wallet = multiWallet.walletWithID(transaction.walletID)

        val title = if (multiWallet.openedWalletsCount() > 1) {
            context.getString(R.string.wallet_new_transaction, wallet.name)
        } else {
            context.getString(R.string.new_transaction)
        }

        val incomingNotificationsKey = transaction.walletID.toString() + Hdflibwallet.IncomingTxNotificationsConfigKey
        val incomingNotificationConfig = multiWallet.readInt32ConfigValueForKey(incomingNotificationsKey, Constants.DEF_TX_NOTIFICATION)

        val notificationSound = when (incomingNotificationConfig) {
            Constants.TX_NOTIFICATION_SOUND_ONLY,
            Constants.TX_NOTIFICATION_SOUND_VIBRATE -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> null
        }

        val vibration = when (incomingNotificationConfig) {
            Constants.TX_NOTIFICATION_VIBRATE_ONLY,
            Constants.TX_NOTIFICATION_SOUND_VIBRATE -> arrayOf(0L, 100, 100, 100).toLongArray()
            else -> null
        }

        val launchIntent = Intent(context, HomeActivity::class.java)
        launchIntent.action = Constants.NEW_TRANSACTION_NOTIFICATION
        launchIntent.putExtra(Constants.TRANSACTION, transaction)

        val launchPendingIntent = PendingIntent.getActivity(context, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationBuilder = NotificationCompat.Builder(context, Constants.TRANSACTION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(amount)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setSound(notificationSound)
                .setVibrate(vibration)
                .setOngoing(false)
                .setAutoCancel(true)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchPendingIntent)

        val groupSummary = NotificationCompat.Builder(context, Constants.TRANSACTION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.new_transaction))
                .setContentText(context.getString(R.string.new_transaction))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setGroup(Constants.TRANSACTION_NOTIFICATION_GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        val notification = notificationBuilder.build()
        manager.notify(transaction.amount.toInt() + transaction.inputs!!.size, notification)
        manager.notify(Constants.TRANSACTION_SUMMARY_ID, groupSummary)
    }


    @Throws(Exception::class)
    fun readFileToBytes(path: String): ByteArray {
        val fin = FileInputStream(path)
        val out = ByteArrayOutputStream()
        val buff = ByteArray(8192)
        var len = fin.read(buff)

        while (len != -1) {
            out.write(buff, 0, len)
            len = fin.read(buff)
        }

        out.flush()
        fin.close()

        return out.toByteArray()
    }

    @Throws(Exception::class)
    fun writeBytesToFile(output: ByteArray, path: String) {
        val file = File(path)

        val fout = FileOutputStream(file)
        val bin = ByteArrayInputStream(output)

        val buff = ByteArray(8192)
        var len = bin.read(buff)

        while (len != -1) {
            fout.write(buff, 0, len)
            len = bin.read(buff)
        }

        fout.flush()
        fout.close()
        bin.close()
    }

    fun deleteDir(file: File) {
        val contents = file.listFiles()
        if (contents != null) {
            for (f in contents) {
                deleteDir(f)
            }
        }
        file.delete()
    }
}
