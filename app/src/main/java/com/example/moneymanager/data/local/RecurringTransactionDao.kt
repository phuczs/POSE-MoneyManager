package com.example.moneymanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextExecutionDate ASC")
    fun getAllByUser(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextExecutionDate <= :currentTime")
    suspend fun getDueItems(currentTime: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecurringTransactionEntity)

    @Update
    suspend fun update(item: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE userId = :userId AND id = :id")
    suspend fun updateActiveStatus(userId: String, id: String, isActive: Boolean)
}
