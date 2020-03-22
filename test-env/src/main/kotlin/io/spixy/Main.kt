package io.spixy

import io.spixy.hyen.Record
import io.spixy.hyen.HyenRecord
import io.spixy.hyen.Hyen
import java.io.File

@Record
data class TransferMoneyV1(val from: String, val to: String, val amount: Long) : HyenRecord()

@Record
data class AddMoneyV1(val to: String, val amount: Long) : HyenRecord()

val hyen = Hyen().apply {
    registerType(TransferMoneyV1::class.java, 1)
    registerType(AddMoneyV1::class.java, 2)
    val dbFile = File("test.db")
    open(dbFile)
}

fun main() {
//    hyen.write(AddMoneyV1("A", 100L))
//    hyen.write(TransferMoneyV1("A", "B", 40L))
//    io.spixy.getHie.write(io.spixy.AddMoneyV1("A", 100L))
//    io.spixy.getHie.write(io.spixy.TransferMoneyV1("A", "B", 40L))
}