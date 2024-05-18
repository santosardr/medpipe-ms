package com.tcc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class MsMedpipeApplication
val log: Logger = LoggerFactory.getLogger(MsMedpipeApplication::class.java)
fun main(args: Array<String>) {
	runApplication<MsMedpipeApplication>(*args)

	log.info(
		"""
        ------------------------
            MS-Medpipe
        ------------------------
    """
	)
}


