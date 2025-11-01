package best.app.offlinehisab.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao {
    @Insert
    suspend fun insertTxn(t: Txn)

    @Update
    suspend fun updateTxn(t: Txn)

    @Delete
    suspend fun deleteTxn(t: Txn)

    // Flow of all txns for a customer (live)
    @Query("SELECT * FROM txns WHERE customerId = :customerId ORDER BY date DESC")
    fun getTxnsForCustomerFlow(customerId: String): Flow<List<Txn>>

    // Flow sums (may emit null if no rows; transform to 0.0 in repo)
    @Query("SELECT SUM(amount) FROM txns WHERE type = :type")
    fun sumAllByTypeFlow(type: TxnType): Flow<Double?>

    @Query("SELECT SUM(amount) FROM txns WHERE customerId = :customerId AND type = :type")
    fun sumByCustomerAndTypeFlow(customerId: String, type: TxnType): Flow<Double?>
}
