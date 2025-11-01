package best.app.offlinehisab.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert
    suspend fun insertCustomer(c: Customer): Long

    @Update
    suspend fun updateCustomer(c: Customer)

    @Delete
    suspend fun deleteCustomer(c: Customer)

    // Flow version for live updates
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    // keep the suspend version if you need one-off fetches
    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomer(id: String): Customer?
}
