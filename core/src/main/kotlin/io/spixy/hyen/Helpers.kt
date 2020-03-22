package io.spixy.hyen

import java.nio.charset.StandardCharsets

fun ByteArray.toHex(): String {
    return this.joinToString(separator = " ") { it.toUByte().toString(16).padStart(2, '0')}
}

fun String.toHex(): String {
    return this.toByteArray(StandardCharsets.UTF_8).toHex()
}