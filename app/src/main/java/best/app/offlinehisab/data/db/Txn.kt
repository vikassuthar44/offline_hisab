package best.app.offlinehisab.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxnType { DEBIT, CREDIT }

@Entity(
    tableName = "txns",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: String,
    val amount: Double,
    val type: TxnType,
    val note: String? = null,
    val date: Long = System.currentTimeMillis()
)