package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        SavingsGoalEntity::class,
        RecurringBillEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WalXDatabase : RoomDatabase() {
    abstract fun walXDao(): WalXDao

    companion object {
        @Volatile
        private var INSTANCE: WalXDatabase? = null

        fun getInstance(context: Context): WalXDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WalXDatabase::class.java,
                    "walx_finance_v1.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
