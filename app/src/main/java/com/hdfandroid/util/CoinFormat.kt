/*
 * Copyright (c) 2018-2019 The Hdfchain developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.hdfandroid.util

import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import hdflibwallet.Hdflibwallet
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.regex.Pattern

object CoinFormat {

    val dcrWithCommas = "#,###,###,##0.########"
    val dcrWithCommasAndZeros = "#,###,###,##0.00000000"
    val dcrWithoutCommas = "#########0.########"

    fun format(str: String, relativeSize: Float = 0.7f): Spannable {
        val spannable = SpannableString(str)

        return formatSpannable(spannable, relativeSize)
    }

    fun formatSpannable(spannable: Spannable, relativeSize: Float = 0.7f): Spannable {

        val removeRelativeSpan = spannable.getSpans(0, spannable.length, RelativeSizeSpan::class.java)
        for (span in removeRelativeSpan) {
            spannable.removeSpan(span)
        }

        val doubleOrMoreDecimalPlaces = Pattern.compile("(([0-9]{1,3},*)*\\.)\\d{2,}").matcher(spannable)
        val oneDecimalPlace = Pattern.compile("(([0-9]{1,3},*)*\\.)\\d").matcher(spannable)
        val noDecimal = Pattern.compile("([0-9]{1,3},*)+").matcher(spannable)

        val span = RelativeSizeSpan(relativeSize)

        var startIndex: Int = -1

        if (noDecimal.find()) {
            startIndex = noDecimal.end()
        }

        if (oneDecimalPlace.find()) {
            val start = spannable.indexOf(".", oneDecimalPlace.start())
            if (start <= startIndex || startIndex == -1) {
                startIndex = start + 2
            }
        }

        if (doubleOrMoreDecimalPlaces.find()) {
            val start = spannable.indexOf(".", doubleOrMoreDecimalPlaces.start())
            if (start <= startIndex || startIndex == -1) {
                startIndex = start + 3
            }
        }

        if (startIndex == -1) {
            return spannable
        }

        spannable.setSpan(span, startIndex, spannable.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        return spannable
    }

    fun format(amount: Long, relativeSize: Float = 0.7f, suffix: String = " DCR"): Spannable {
        return format(Hdflibwallet.amountCoin(amount), relativeSize, suffix)
    }

    fun format(amount: Double, relativeSize: Float = 0.7f, suffix: String = " DCR"): Spannable {
        return format(formatHdfchain(amount) + suffix, relativeSize)
    }

    fun formatHdfchain(dcr: Double, pattern: String = dcrWithCommasAndZeros) = formatHdfchain(Hdflibwallet.amountAtom(dcr), pattern)

    fun formatHdfchain(dcr: Long, pattern: String = dcrWithCommasAndZeros): String {
        val convertedDcr = Hdflibwallet.amountCoin(dcr)
        val df = NumberFormat.getNumberInstance(Locale.ENGLISH) as DecimalFormat
        df.applyPattern(pattern)
        return df.format(convertedDcr)
    }
}