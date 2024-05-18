package com.tcc.medpipe

import java.sql.ClientInfoStatus

data class MedpipeControlRunDto (
    val processId: Long = 0,
    val status: String
        )
