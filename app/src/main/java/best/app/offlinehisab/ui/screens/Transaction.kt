package best.app.offlinehisab.ui.screens

import best.app.offlinehisab.data.db.TxnType

data class Transaction(
    val txnId: Long,
    val customerId: String,
    val amount: Double,
    val type: TxnType,
    val note: String? = null,
    val date: Long = System.currentTimeMillis(),
    val remainingBalance: Double,
)
