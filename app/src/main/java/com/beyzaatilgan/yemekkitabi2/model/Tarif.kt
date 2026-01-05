package com.beyzaatilgan.yemekkitabi2.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Tarif(

    @ColumnInfo(name = "isim")
    var isim : String,

    @ColumnInfo(name = "malzeme")
    var malzeme : String,

    @ColumnInfo(name = "gorsel")
    var gorsel : ByteArray

){
    // id i kendisi otomatik olarak atayacak, default id ise 0 olacaktır.
    @PrimaryKey(autoGenerate = true)
    var id = 0
}
