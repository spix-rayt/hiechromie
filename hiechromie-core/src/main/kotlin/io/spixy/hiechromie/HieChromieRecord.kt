package io.spixy.hiechromie

abstract class HieChromieRecord<T> {
    var id: Long = -1
        internal set

    abstract fun handle(context: T)
}