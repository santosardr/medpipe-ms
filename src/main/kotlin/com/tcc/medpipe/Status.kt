package com.tcc.medpipe

enum class Status(val statusCode: Long) {
    PROCESSING(1),
    FINISHED(0),
    ERROR(-500),
    NOT_FOUND(-404)
}