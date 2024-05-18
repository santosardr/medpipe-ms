package com.tcc.medpipe

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class MedpipeControl(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val process: String? = null,
    var status: Status? = null,
    var directory: String = ""
)
