package com.tcc.medpipe

import com.tcc.medpipe.MedpipeControl
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MedpipeControlRepository : JpaRepository<MedpipeControl, Long>