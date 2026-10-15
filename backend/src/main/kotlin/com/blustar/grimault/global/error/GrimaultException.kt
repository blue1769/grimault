package com.blustar.grimault.global.error

class GrimaultException(
    val errorCode: ErrorCode,
    message: String? = null
) : RuntimeException(message ?: errorCode.message)
