package best.app.offlinehisab.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import best.app.offlinehisab.data.db.Customer
import best.app.offlinehisab.data.db.Txn
import best.app.offlinehisab.data.db.TxnType
import best.app.offlinehisab.data.repo.Repo
import best.app.offlinehisab.di.AppModule
import best.app.offlinehisab.utils.FilterOptionType
import best.app.offlinehisab.utils.Prefs
import best.app.offlinehisab.utils.filterDateRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val selectedTxn = mutableStateOf<Txn?>(null)
    val selectedCustomer = mutableStateOf<Customer?>(null)
    private val repo: Repo by lazy { AppModule.provideRepo(application.applicationContext) }

    // customers as StateFlow (live)
    private val _customers: StateFlow<List<Customer>> =
        repo.customersFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = _customers
    val isClearClick = mutableIntStateOf(0)

    // global totals as StateFlow
    private val _globalTotals: StateFlow<Pair<Double, Double>> =
        repo.globalTotalsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    val globalTotals: StateFlow<Pair<Double, Double>> = _globalTotals

    // write helpers
    fun addCustomer(name: String, phone: String? = null, note: String? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.addCustomer(name, phone, note)
        }

    fun addTxn(customerId: String, amount: Double, type: TxnType, note: String? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.addTxn(customerId, amount, type, note)
        }

    fun updateTxn(
        customerId: String,
        txnId: Long,
        amount: Double,
        type: TxnType,
        note: String? = null,
    ) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateTxn(customerId, txnId = txnId, amount, type, note)
        }

    fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String?,
        note: String? = null,
    ) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateCustomer(
                customerId, name, phone, note
            )
        }

    fun deleteTxn(customerId: String, txn: Txn) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteTxn(customerId, txn)
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteCustomer(customerId)
        }
    }

    // expose per-customer flows (used by UI to collect)
    fun totalsForCustomerFlow(customerId: String): Flow<Pair<Double, Double>> =
        repo.totalsForCustomerFlow(customerId)

    fun txnsForCustomerFlow(customerId: String): Flow<List<Txn>> {
        val txnsListFlow = repo.txnsForCustomerFlow(customerId)
        viewModelScope.launch(Dispatchers.IO) {
            txnsListFlow.collect {
                setTxns(it)
            }
        }
        return txnsListFlow
    }

    fun latestTxnForCustomerFlow(customerId: String): Flow<Txn?> =
        repo.latestTxnForCustomerFlow(customerId)

    // suspend one-off APIs if needed
    suspend fun getCustomerById(id: String): Customer? = repo.getCustomer(id)

    fun exportPDF() {
        viewModelScope.launch(Dispatchers.IO) {

        }
    }


    // source data. Replace with your real source (Room/Repo).
    private val _allTxns = MutableStateFlow<List<Txn>>(emptyList())
    val allTxns: StateFlow<List<Txn>> = _allTxns.asStateFlow()

    // UI state for selected filter
    private val _selectedFilter = MutableStateFlow(FilterOptionType.All)
    val selectedFilter: StateFlow<FilterOptionType> = _selectedFilter.asStateFlow()

    // derived filtered list
    @RequiresApi(Build.VERSION_CODES.O)
    val filteredTxns: StateFlow<List<Txn>> = combine(_allTxns, _selectedFilter) { txns, filter ->
        applyFilter(txns, filter)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setTxns(list: List<Txn>) {
        _allTxns.value = list
    }

    fun selectFilter(filter: FilterOptionType) {
        _selectedFilter.value = filter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyFilter(
        txns: List<Txn>,
        filter: FilterOptionType,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Txn> {
        val (startDate, endDate) = filterDateRange(filter, zone)

        Log.d("Vikas", "applyFilter: start $startDate and end $endDate")
        return txns.filter { txn ->
            Log.d("Vikas", "applyFilter: txn date ${txn.date}")
            // Convert txn epoch -> LocalDate (ignore time)
            val txnDate = Instant.ofEpochMilli(txn.date).atZone(zone).toLocalDate()
            Log.d("Vikas", "applyFilter: convert txn date $txnDate")

            val afterStart = startDate?.let { !txnDate.isBefore(it) } ?: true
            val beforeEnd = endDate?.let { !txnDate.isAfter(it) } ?: true

            afterStart && beforeEnd
        }.sortedByDescending { it.date } // latest first
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val savePin = Prefs.isStringValue(Prefs.SAVE_PIN)
        if (savePin == null) {
            if(pin.length == 4) {
                Prefs.setStringValue(pin, Prefs.SAVE_PIN)
                onSuccess()
            } else {
                onFailure("Please enter 4 digit pin")
            }
        } else {
            if (savePin == pin) {
                onSuccess()
            } else {
                if(isClearClick.intValue >= 15) {
                    isClearClick.intValue = 0
                    Prefs.clearKey()
                    onSuccess()
                } else {
                    onFailure("Please enter correct 4 digit pin")
                }
            }
        }
    }

    fun resetClearClick() {
        isClearClick.intValue = 0
    }


}
