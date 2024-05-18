package com.tcc.medpipe

import com.tcc.file.DirectoryRoot.MEDPIPE_FILES
import com.tcc.file.ResultFile
import com.tcc.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.io.path.extension

@Service
class MedpipeService {
    private lateinit var root: Path
    @Autowired
    private lateinit var medpipeControlRepository: MedpipeControlRepository
    fun init(directoryRoot: String): Path? {
        try {
            root = Paths.get(directoryRoot)
            return Files.createDirectories(root)
        } catch (e: IOException) {
            throw RuntimeException("Could not initialize folder for upload!")
        }
    }

    fun buildDirectory(folderName: String): String {
        return "/"+MEDPIPE_FILES.description+"/$folderName"
    }

    @Async
    fun runScript(
        fileResult: String,
        cellWall: String,
        organismGroup: String,
        epitopeLength: String,
        email: String,
        membraneCitoplasm: String,
        medpipeControl: MedpipeControl
    ) {
        try {
            val command = "sh medpipe $fileResult $cellWall $organismGroup $epitopeLength $email $membraneCitoplasm"
            log.info("[runScript] - Start exec: " + LocalDateTime.now() + " commad: " + command)
            val process = Runtime.getRuntime().exec(command)
            val processEnd = process.waitFor()
            val result = BufferedReader(InputStreamReader(process.inputStream)).readText()
            log.info("[runScript] - Time: " + LocalDateTime.now() + " Result: " + result)
            log.info("[runScript] End process: $processEnd")
            updateStatus(Status.FINISHED, medpipeControl)
        } catch (e: Exception) {
            log.error("[runScript] Error in process: " + e.message)
            updateStatus(Status.ERROR, medpipeControl)
            throw RuntimeException(e.message)
        }
    }

    fun getPrediction(id: Long): StringBuilder {
        val medpipeControl = findProcess(id) ?: throw RuntimeException(HttpStatus.NOT_FOUND.toString())
        val fileResult = medpipeControl.directory + "/" + ResultFile.TARGET_FASTA_RESULT_SORT.description
        var line = ""
        val result = StringBuilder()
        BufferedReader(FileReader(fileResult)).use { br ->
            while (br.readLine()?.also { line = it } != null) {
                val values: Array<String> = line.split(" ").toTypedArray()
                result.append(values[0] + " " + values[4].substring(4) + " " + values[6] + "\n")
            }
        }
        return result
    }

    fun readFileInfo(
        fileName: String,
        type: String
    ): StringBuilder {
        BufferedReader(FileReader(fileName)).use { br ->
            return if (type == "SECRETED") {
                getSignalSecreted(br)
            } else {
                getTmh(br)
            }
        }
    }

    fun getTmh(br: BufferedReader): StringBuilder {
        val result = StringBuilder()
        var line = ""
        var tmhs = ""
        while (br.readLine()?.also { line = it } != null) {
            val values: Array<String> = line.split(";").toTypedArray()
            var tmh = 60
            if (values.size >= 45 && (values[2] == "PSE" || values[2] == "MEMBRANE")) {
                tmhs = values[44]
                tmhs.also {
                    while (tmh < (60 + (tmhs.toInt() * 4))) {
                        result.append(values[0] + " " + values[tmh - 3] + " " + values[tmh - 1] + " " + values[tmh] + "\n")
                        tmh += 4
                    }
                }

            }
        }
        log.info("BUILDER length: " + result.length)
        return result
    }

    private fun getSignalSecreted(br: BufferedReader): StringBuilder {
        var line = ""
        val result = StringBuilder()
        while (br.readLine()?.also { line = it } != null) {
            val values: Array<String> = line.split(";").toTypedArray()
            if (values.size >= 10 && values[2] == "SECRETED") {
                result.append(values[0] + " " + values[2] + " " + values[10] + "\n")
            }
        }
        log.info("BUILDER length: " + result.length)
        return result
    }

    fun saveFile(file: MultipartFile, directoryRoot: String): String {
        try {
            log.info("[saveFile] - Start - File: " + file.originalFilename + " directoryRoot: " + directoryRoot)
            val path = init(directoryRoot)
            Files.copy(file.inputStream, path?.resolve(file.originalFilename))
            log.info("[saveFile] - Result File: " + (path?.toAbsolutePath() as Any).toString() + path.root.toString() + file.originalFilename)
            return (path?.toAbsolutePath() as Any).toString() + path.root.toString() + file.originalFilename
        } catch (e: Exception) {
            log.error("[saveFile] -Error: " + e.message)
            if (e is FileAlreadyExistsException) {
                throw RuntimeException("A file of that name already exists.")
            }
            throw RuntimeException(e.message)
        }
    }

    fun updateStatus(status: Status, medpipeControl: MedpipeControl) {
        log.info("[updateStatus] - Start - Status: $status")
        medpipeControl.status = status
        saveControl(medpipeControl)
    }

    fun saveControl(medpipeControl: MedpipeControl): MedpipeControl {
        log.info("[saveControl] - Start - medpipe process: " + medpipeControl.process)
        return medpipeControlRepository.save(medpipeControl)
    }

    fun findStatusProcess(id: Long): Long? {
        val result = medpipeControlRepository.findById(id)

        return if (result.isPresent) result.get().status?.statusCode else Status.NOT_FOUND?.statusCode
    }

    fun findProcess(id: Long): MedpipeControl? {
        val result = medpipeControlRepository.findById(id)

        return if (result.isPresent) result.get() else null
    }

    fun findAllStatusProcess(): List<MedpipeControl> {
        return medpipeControlRepository.findAll().toList()
    }

    fun deleteAll() {
        FileSystemUtils.deleteRecursively(root.toFile())
    }

    fun getFile(dir: String): ByteArray? {
        log.info("[getFile] - Start - DIR: $dir")
        val fileSet: MutableSet<String> = HashSet()
        var byteResource: ByteArray? = ByteArray(1)
        Files.newDirectoryStream(Paths.get(dir)).use { stream ->
            for (path in stream) {
                if (!Files.isDirectory(path) && path.extension == "zip") {
                    byteResource =Files.readAllBytes(path)
                    fileSet.add(
                        path.fileName
                            .toString()
                    )
                }
            }
        }
        return byteResource
    }

    fun loadAll(): Stream<Path?>? {
        return try {
            Files.walk(this.root, 1).filter { path -> !path.equals(this.root) }.map(this.root::relativize)
        } catch (e: IOException) {
            throw RuntimeException("Could not load the files!")
        }
    }
}