package com.tcc.medpipe

import com.tcc.file.ResultFile
import com.tcc.log
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@Api(value = "Medpipe")
@RequestMapping("/v1/medpipe")
class MedpipeController(val medpipeService: MedpipeService) {

    @ApiOperation(value = "Running the Medpipe script")
    @PostMapping("/run")
    fun runFileProcess(
        @ApiParam(name = "file", value = "file for Medpipe processing")
        @RequestParam("file") file: MultipartFile,
        @ApiParam(name = "cellWall", value = "cell wall")
        @RequestParam("cellWall") cellWall: String,
        @ApiParam(name = "organismGroup", value = "organism group")
        @RequestParam("organismGroup") organismGroup: String,
        @ApiParam(name = "epitopeLength", value = "epitope length")
        @RequestParam(value = "epitopeLength", required = false, defaultValue = "9") epitopeLength: String,
        @ApiParam(name = "email", value = "email address to which Medpipe results will be sent")
        @RequestParam("email") email: String,
        @ApiParam(name = "membraneCitoplasm", value = "cytoplasmic membrane")
        @RequestParam("membraneCitoplasm", required = false, defaultValue = "") membraneCitoplasm: String
    ): String {
        log.info("[runFileProcess] - Init run...")
        val directoryRoot = medpipeService.buildDirectory("MsMedpipe" + System.currentTimeMillis().toString())
        log.info("[runFileProcess] - directoryRoot: $directoryRoot")
        val fileResult = medpipeService.saveFile(file, directoryRoot)
        log.info("[runFileProcess] - fileResult: $fileResult")
        val process = medpipeService.saveControl(
            MedpipeControl(
                process = email,
                status = Status.PROCESSING,
                directory = directoryRoot
            )
        )
        log.info("[runFileProcess] - process: $process")
        medpipeService.runScript(
            fileResult,
            cellWall,
            organismGroup,
            epitopeLength,
            email,
            membraneCitoplasm,
            process
        )
        log.info("[runFileProcess] - script terminated: $directoryRoot;${process.id}")
        return "${process.id}"
    }

    @ApiOperation(value = "Fetches the resulting file from Medpipe")
    @GetMapping(
        value = ["/{id}/result-file"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun getResultFile(@ApiParam(name = "id", value = "process id") @PathVariable id: Long): ByteArray? {
        val medpipeControl = medpipeService.findProcess(id) ?: throw RuntimeException(HttpStatus.NOT_FOUND.toString())
        return medpipeService.getFile(medpipeControl.directory)
    }

    @ApiOperation(value = "Fetch Medpipe script processing status")
    @GetMapping("/{id}/status")
    fun getStatus(@ApiParam(name = "id", value = "process id") @PathVariable id: Long): Long? {
        return medpipeService.findStatusProcess(id)
    }

    @GetMapping("/controls")
    fun getAllControls(): List<MedpipeControl> {
        return medpipeService.findAllStatusProcess()
    }

    @GetMapping("/{id}/control")
    fun getAllStatus(@ApiParam(name = "id", value = "process id") @PathVariable id: Long): MedpipeControl {
        return medpipeService.findProcess(id) ?: throw RuntimeException(HttpStatus.NOT_FOUND.toString())
    }

    @ApiOperation(value = "Fetch Medpipe script processing status")
    @GetMapping("/{id}/predictions")
    fun getPredictions(@ApiParam(name = "id", value = "process id") @PathVariable id: Long): StringBuilder {
        return medpipeService.getPrediction(id)
    }

    @GetMapping("/{id}/signal")
    fun getSignalFileResult(
        @ApiParam(name = "id", value = "process id") @PathVariable id: Long
    ): StringBuilder {
        val medpipeControl = medpipeService.findProcess(id) ?: throw RuntimeException(HttpStatus.NOT_FOUND.toString())
        return medpipeService.readFileInfo(
            medpipeControl.directory + "/" + ResultFile.TARGET_FASTA_CSV.description,
            "SECRETED"
        )
    }

    @GetMapping("/{id}/tmh")
    fun getTmhFileResult(
        @ApiParam(name = "id", value = "process id") @PathVariable id: Long
    ): StringBuilder {
        val medpipeControl = medpipeService.findProcess(id) ?: throw RuntimeException(HttpStatus.NOT_FOUND.toString())
        return medpipeService.readFileInfo(
            medpipeControl.directory + "/" + ResultFile.TARGET_FASTA_CSV.description,
            "TMH"
        )
    }

    @PostMapping
    fun save(@RequestBody medpipeControl: MedpipeControl): MedpipeControl {
        return medpipeService.saveControl(medpipeControl)
    }

}