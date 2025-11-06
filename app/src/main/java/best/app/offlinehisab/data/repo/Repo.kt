package best.app.offlinehisab.data.repo

import best.app.offlinehisab.data.db.AppDatabase
import best.app.offlinehisab.data.db.Customer
import best.app.offlinehisab.data.db.Txn
import best.app.offlinehisab.data.db.TxnType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class Repo(private val db: AppDatabase) {
    private val custDao = db.customerDao()
    private val txnDao = db.txnDao()

    // --- write operations (suspend)
    suspend fun addCustomer(name: String, phone: String? = null, note: String? = null) =
        custDao.insertCustomer(Customer(name = name, phone = phone, note = note))

    suspend fun addTxn(customerId: String, amount: Double, type: TxnType, note: String? = null, dateMillis: Long) {
        txnDao.insertTxn(Txn(customerId = customerId, amount = amount, type = type, note = note, date = dateMillis))
        val customer = getCustomer(customerId)
        customer?.update = System.currentTimeMillis()
        customer?.let { custDao.updateCustomer(c = it) }
    }

    suspend fun updateTxn(
        customerId: String,
        txnId: Long,
        amount: Double,
        type: TxnType,
        note: String? = null,
        dateMillis: Long,
    ) {
        txnDao.updateTxn(
            Txn(
                id = txnId, customerId = customerId, amount = amount, type = type, note = note,
                date = dateMillis
            )
        )
        val customer = getCustomer(customerId)
        customer?.update = System.currentTimeMillis()
        customer?.let { custDao.updateCustomer(c = it) }
    }

    suspend fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String? = null,
        note: String? = null,
    ) {
        val customer = Customer(
            id = customerId,
            name = name,
            phone = phone,
            note = note
        )
        customer.let {
            custDao.updateCustomer(
                it
            )
        }
    }


    suspend fun deleteTxn(customerId: String, txn: Txn) {
        txnDao.deleteTxn(
            txn
        )
        val customer = getCustomer(customerId)
        customer?.update = System.currentTimeMillis()
        customer?.let { custDao.updateCustomer(c = it) }
    }

    suspend fun deleteCustomer(customerId: String) {
        val customer = getCustomer(customerId)
        customer?.let {
            custDao.deleteCustomer(
                it
            )
        }
    }

    // --- read (suspend) (kept for compatibility)
    suspend fun getAllCustomers(): List<Customer> =
        db.customerDao().getAllCustomersFlow().firstOrNull() ?: emptyList()

    suspend fun getCustomer(id: String): Customer? = db.customerDao().getCustomer(id)

    // --- Flow outputs for UI

    /** Flow of all customers (live). */
    fun customersFlow(): Flow<List<Customer>> = custDao.getAllCustomersFlow()

    /** Flow of txns for a customer (live). */
    fun txnsForCustomerFlow(customerId: String): Flow<List<Txn>> =
        txnDao.getTxnsForCustomerFlow(customerId)

    /** Flow of totals (credit, debit) for a customer. Emits Pair(credit, debit) */
    fun totalsForCustomerFlow(customerId: String): Flow<Pair<Double, Double>> {
        val creditFlow =
            txnDao.sumByCustomerAndTypeFlow(customerId, TxnType.CREDIT).map { it ?: 0.0 }
        val debitFlow = txnDao.sumByCustomerAndTypeFlow(customerId, TxnType.DEBIT).map { it ?: 0.0 }
        return creditFlow.combine(debitFlow) { c, d -> Pair(c, d) }
    }

    /** Flow of global totals (credit, debit). */
    fun globalTotalsFlow(): Flow<Pair<Double, Double>> {
        val creditFlow = txnDao.sumAllByTypeFlow(TxnType.CREDIT).map { it ?: 0.0 }
        val debitFlow = txnDao.sumAllByTypeFlow(TxnType.DEBIT).map { it ?: 0.0 }
        return creditFlow.combine(debitFlow) { c, d -> Pair(c, d) }
    }

    /** Flow of latest transaction for a customer (nullable). */
    fun latestTxnForCustomerFlow(customerId: String): Flow<Txn?> =
        txnsForCustomerFlow(customerId).map { list -> list.firstOrNull() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun prefixSuggestions(inputFlow: Flow<String>, limit: Int = 20): Flow<List<String>> {
        return inputFlow
            .map { it.trim() }
            .distinctUntilChanged()
            .map { input ->
                val pattern = if (input.isEmpty()) "%" else "${escapeLike(input)}%"
                pattern to limit
            }
            .flatMapLatest { (pattern, lim) ->
                txnDao.getNotesPrefixFlow(pattern, lim)
            }
    }

    private fun escapeLike(s: String) = s.replace("%", "\\%").replace("_", "\\_")
}
