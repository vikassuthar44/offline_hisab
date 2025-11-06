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
import best.app.offlinehisab.ui.screens.Transaction
import best.app.offlinehisab.utils.FilterOptionType
import best.app.offlinehisab.utils.Prefs
import best.app.offlinehisab.utils.filterDateRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val selectedTxn = mutableStateOf<Transaction?>(null)
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

    private val _noteQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val suggestions: StateFlow<List<String>> = repo
        .prefixSuggestions(_noteQuery.debounce(150), limit = 12)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onNoteTextChanged(new: String) {
        _noteQuery.value = new
    }

    // write helpers
    fun addCustomer(name: String, phone: String? = null, note: String? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.addCustomer(name, phone, note)
        }

    fun addTxn(
        customerId: String,
        amount: Double,
        type: TxnType,
        note: String? = null,
        dateMillis: Long,
    ) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.addTxn(customerId, amount, type, note, dateMillis)
        }

    fun updateTxn(
        customerId: String,
        txnId: Long,
        amount: Double,
        type: TxnType,
        note: String? = null,
        dateMillis: Long,
    ) =
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateTxn(customerId, txnId = txnId, amount, type, note, dateMillis)
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

    fun txnsForCustomerFlow(customerId: String): Flow<List<Transaction>> {
        return repo.txnsForCustomerFlow(customerId = customerId)
            .map {
                setTxns(it)
                calculateRunningBalance(it)
            }
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
    val filteredTxns: StateFlow<List<Transaction>> = combine(_allTxns, _selectedFilter) { txns, filter ->
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
    ): List<Transaction> {
        val (startDate, endDate) = filterDateRange(filter, zone)

        Log.d("Vikas", "applyFilter: start $startDate and end $endDate")
        val filterList =  txns.filter { txn ->
            Log.d("Vikas", "applyFilter: txn date ${txn.date}")
            // Convert txn epoch -> LocalDate (ignore time)
            val txnDate = Instant.ofEpochMilli(txn.date).atZone(zone).toLocalDate()
            Log.d("Vikas", "applyFilter: convert txn date $txnDate")

            val afterStart = startDate?.let { !txnDate.isBefore(it) } ?: true
            val beforeEnd = endDate?.let { !txnDate.isAfter(it) } ?: true

            afterStart && beforeEnd
        }.sortedByDescending { it.date } // latest first
        return calculateRunningBalance(filterList)
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val savePin = Prefs.isStringValue(Prefs.SAVE_PIN)
        if (savePin == null) {
            if (pin.length == 4) {
                Prefs.setStringValue(pin, Prefs.SAVE_PIN)
                onSuccess()
            } else {
                onFailure("Please enter 4 digit pin")
            }
        } else {
            if (savePin == pin) {
                onSuccess()
            } else {
                if (isClearClick.intValue >= 15) {
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

    private fun calculateRunningBalance(txns: List<Txn>): List<Transaction> {
        if (txns.isEmpty()) return emptyList()

        // Sort by date ascending to ensure proper sequence
        val sorted = txns.sortedBy { it.date }

        var runningBalance = 0.0
        val result = mutableListOf<Transaction>()

        for (txn in sorted) {
            runningBalance += when (txn.type) {
                TxnType.CREDIT -> txn.amount   // increase balance
                TxnType.DEBIT -> -txn.amount       // decrease balance
            }

            result.add(
                Transaction(
                    txnId = txn.id,
                    customerId = txn.customerId,
                    amount = txn.amount,
                    type = txn.type,
                    note = txn.note,
                    date = txn.date,
                    remainingBalance = runningBalance
                )
            )
        }

        return result.reversed()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun previousAmount(filter: FilterOptionType, zone: ZoneId = ZoneId.systemDefault()): Double {
        val txns = allTxns.value // StateFlow/List<Txn> assumed available in scope

        // Helpers to get epoch millis for period starts
        val today = LocalDate.now(zone)
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()

        // week start according to locale (usually Monday in many locales)
        val wf = WeekFields.of(Locale.getDefault())
        val startOfThisWeek = today.with(wf.dayOfWeek(), 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfLastWeek = LocalDate.ofInstant(Instant.ofEpochMilli(startOfThisWeek), zone)
            .minusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val startOfThisMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfLastMonth = LocalDate.ofInstant(Instant.ofEpochMilli(startOfThisMonth), zone)
            .minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Predicate: whether txn is before the "period start to exclude"
        val includePredicate: (Txn) -> Boolean = when (filter) {
            FilterOptionType.Today -> { txn ->
                // include transactions before start of today (i.e., exclude today's txns)
                txn.date < startOfToday
            }
            FilterOptionType.ThisWeek -> { txn ->
                // include txns before start of this week (exclude this week)
                txn.date < startOfThisWeek
            }
            FilterOptionType.LastWeek -> { txn ->
                // include txns before start of last week (exclude this week and last week)
                txn.date < startOfLastWeek
            }
            FilterOptionType.ThisMonth -> { txn ->
                // include txns before start of this month (exclude this month)
                txn.date < startOfThisMonth
            }
            FilterOptionType.LastMonth -> { txn ->
                // include txns before start of last month (exclude this month and last month)
                txn.date < startOfLastMonth
            }
            FilterOptionType.All -> {
                // "previous amount" for All — nothing previous to exclude, return 0
                { _ -> false }
            }
        }

        // Sum received and paid using sign convention:
        // - Positive amount => received
        // - Negative amount => paid
        val included = txns.filter { includePredicate(it) }

        val totalReceived = included.filter { it.type == TxnType.CREDIT }.sumOf { it.amount }
        val totalPaid = included.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }

        Log.d("Previous", "previousAmount: total received $totalReceived")
        Log.d("Previous", "previousAmount: total paid $totalPaid")
        Log.d("Previous", "previousAmount: ${totalReceived - totalPaid}")
        return totalReceived - totalPaid
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun recentAmount(filter: FilterOptionType, zone: ZoneId = ZoneId.systemDefault()): Double {
        val txns = allTxns.value // List<Txn> assumed in scope

        val today = LocalDate.now(zone)
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Week start according to locale (e.g., Monday)
        val wf = WeekFields.of(Locale.getDefault())
        val startOfThisWeekLocal = today.with(wf.dayOfWeek(), 1) // first day-of-week
        val startOfThisWeek = startOfThisWeekLocal.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextWeek = startOfThisWeekLocal.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfLastWeek = startOfThisWeekLocal.minusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Month starts
        val startOfThisMonthLocal = today.withDayOfMonth(1)
        val startOfThisMonth = startOfThisMonthLocal.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextMonth = startOfThisMonthLocal.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfLastMonth = startOfThisMonthLocal.minusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Determine period [periodStart, periodEnd) to include txns in the period
        val (periodStart, periodEnd) = when (filter) {
            FilterOptionType.Today -> Pair(startOfToday, startOfTomorrow)
            FilterOptionType.ThisWeek -> Pair(startOfThisWeek, startOfNextWeek)
            FilterOptionType.LastWeek -> Pair(startOfLastWeek, startOfThisWeek)
            FilterOptionType.ThisMonth -> Pair(startOfThisMonth, startOfNextMonth)
            FilterOptionType.LastMonth -> Pair(startOfLastMonth, startOfThisMonth)
            FilterOptionType.All -> Pair(Long.MIN_VALUE, Long.MAX_VALUE)
        }

        val included = txns.filter { it.date in periodStart..<periodEnd }

        val totalReceived = included.filter { it.amount > 0.0 }.sumOf { it.amount }
        val totalPaid = included.filter { it.amount < 0.0 }.sumOf { abs(it.amount) }

        return totalReceived - totalPaid
    }
}
