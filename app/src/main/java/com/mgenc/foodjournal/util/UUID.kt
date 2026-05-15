package com.mgenc.foodjournal.util

import java.security.SecureRandom

private val random = SecureRandom()

fun uuidV7(): String {
    val timestamp = System.currentTimeMillis()
    val buf = ByteArray(16)

    random.nextBytes(buf)
    for (i in 0..5) {
        buf[i] = (timestamp ushr (40 - i * 8)).toByte()
    }

    buf[6] = ((buf[6].toInt() and 0x0F) or 0x70).toByte()
    buf[8] = ((buf[8].toInt() and 0x3F) or 0x80).toByte()

    val hex = buf.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}
