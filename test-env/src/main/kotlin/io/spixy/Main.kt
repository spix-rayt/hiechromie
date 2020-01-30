package io.spixy

import io.spixy.hiechromie.ChromieRecord
import io.spixy.hiechromie.HieChromieRecord
import io.spixy.hiechromie.Hiechromie
import java.io.File

typealias Record = HieChromieRecord<Context>

@ChromieRecord
data class TransferMoneyV1(val from: String, val to: String, val amount: Long) : Record() {
    override fun handle(context: Context) {
        println("run TransferMoney $amount from $from to $to")
        context.cards[from] = (context.cards[from] ?: 0L) - amount
        context.cards[to] = (context.cards[to] ?: 0L) + amount
    }
}

@ChromieRecord
data class AddMoneyV1(val to: String, val amount: Long) : Record() {
    override fun handle(context: Context) {
        println("run AddMoney $amount to $to")
        context.cards[to] = amount
    }
}

class Context {
    val cards = hashMapOf<String, Long>()
}

val hie = Hiechromie(Context()).apply {
    registerType(TransferMoneyV1::class.java, 1)
    registerType(AddMoneyV1::class.java, 2)
    val dbFile = File("test.db")
    open(dbFile)
}

fun main() {
    println(hie.context.cards)
//    io.spixy.getHie.write(io.spixy.AddMoneyV1("A", 100L))
//    io.spixy.getHie.write(io.spixy.TransferMoneyV1("A", "B", 40L))
}