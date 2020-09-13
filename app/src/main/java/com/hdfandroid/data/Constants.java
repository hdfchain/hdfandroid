/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.data;

/**
 * Created by afomic on 2/28/18.
 */

public class Constants {
    public static final String
            TRANSACTION_NOTIFICATION_GROUP = "com.hdfandroid.NEW_TRANSACTIONS",
            NEW_TRANSACTION_NOTIFICATION = "new_transaction_notification",
            NBSP = " ",
            SYNCED = "synced",
            BADGER_DB = "badgerdb",
            STARTUP_PASSPHRASE = "startup_passphrase",
            SPENDING_PASSPHRASE = "spending_passphrase",
            LICENSE = "license",
            WALLET_ID = "wallet_id",
            RESULT = "result",
            AMOUNT = "amount",
            SEED = "seed",
            DEFAULT = "default",
            ANDROID_KEY_STORE = "AndroidKeyStore",
            TRANSFORMATION = "AES/GCM/NoPadding",
            ENCRYPTION_DATA = "encryption_data",
            ENCRYPTION_IV = "encryption_iv",
            TRANSACTION = "tx_hash",
            TRANSACTION_CHANNEL_ID = "new_transaction",
            SELECTED_SOURCE_ACCOUNT = "selected_source_account_id",
            SELECTED_DESTINATION_ACCOUNT = "selected_dest_account_no",
            SEND_TO_ACCOUNT = "send_to_account",
            SEND_MAX = "send_max";

    public static final int TRANSACTION_SUMMARY_ID = 5552478,
            REQUIRED_CONFIRMATIONS = 2,
            DEF_ACCOUNT_NUMBER = 0,
            DEF_LOG_LEVEL = 2,
            TX_NOTIFICATION_NONE = 0,
            TX_NOTIFICATION_SILENT = 1,
            TX_NOTIFICATION_VIBRATE_ONLY = 2,
            TX_NOTIFICATION_SOUND_ONLY = 3,
            TX_NOTIFICATION_SOUND_VIBRATE = 4,
            DEF_TX_NOTIFICATION = TX_NOTIFICATION_VIBRATE_ONLY,
            DEF_CURRENCY_CONVERSION = 0; // None


    public static final boolean DEF_SPEND_UNCONFIRMED = false,
            DEF_STARTUP_SECURITY_SET = false,
            DEF_USE_FINGERPRINT = false,
            DEF_SYNC_ON_CELLULAR = false;

}
