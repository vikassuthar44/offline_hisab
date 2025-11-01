package best.app.offlinehisab.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var update: Long = System.currentTimeMillis(),
)