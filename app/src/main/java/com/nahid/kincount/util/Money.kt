package com.nahid.kincount.util

import com.nahid.kincount.data.model.Transaction
import com.nahid.kincount.data.model.TxType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

object Money {

    /** "৳ 12,450" — fractions only when they exist ("৳ 12,450.50"). */
    fun format(amount: Double): String {
        val hasFraction = abs(amount - floor(amount)) > 1e-9
        val pattern = if (hasFraction) "%,.2f" else "%,.0f"
        return "৳ " + String.format(Locale.US, pattern, amount)
    }

    /** Signed form for ledger rows: "−৳250" / "+৳1,200". */
    fun formatSigned(tx: Transaction): String {
        val sign = if (tx.type == TxType.EXPENSE) "−" else "+"
        return sign + format(tx.amount)
    }
}
