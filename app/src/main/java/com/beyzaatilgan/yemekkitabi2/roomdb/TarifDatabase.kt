package com.beyzaatilgan.yemekkitabi2.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beyzaatilgan.yemekkitabi2.model.Tarif

@Database(entities = [Tarif::class], version = 1)
abstract class TarifDatabase : RoomDatabase() {
    abstract fun tarifDao(): TarifDAO
}