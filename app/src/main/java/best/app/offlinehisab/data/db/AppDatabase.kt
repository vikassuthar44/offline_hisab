package best.app.offlinehisab.data.db

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [Customer::class, Txn::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun txnDao(): TxnDao
}