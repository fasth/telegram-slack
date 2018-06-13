package com.tsduplex.server.shared

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PostConstruct

data class Confluence(
        val slackChannelId: String,
        val slackChannelName: String,
        val confluenceKey: String? = null,
        var telegramGroupId: Int? = null,
        var telegramGroupName: String? = null
) {
    @JsonIgnore
    fun isWaitingForConfirmation() =
            telegramGroupId == null
}

@Repository
class ConfluenceRepository(
        @Qualifier(value = "yamlObjectMapper") val yamlMapper: ObjectMapper,
        val storageProps: StorageProps) {

    var confluences: List<Confluence> = arrayListOf()
    private lateinit var storage: File

    @PostConstruct
    private fun load() {
        storage = File(storageProps.dir);
        if (storage.exists() && storage.canRead() && storage.canWrite()) {
            read()
        } else {
            storage.createNewFile()
        }
    }

    private fun read() {
        try {
            val contents = String(Files.readAllBytes(Paths.get(storageProps.dir)))
            confluences = yamlMapper
                    .readValue<List<Confluence>>(contents, object : TypeReference<List<Confluence>>() {})
            confluences.toString()
        } catch (e: Exception) {
        }
    }

    private fun persist() {
        val data = yamlMapper
                .writerFor(object : TypeReference<List<Confluence>>() {}).writeValueAsString(confluences)

        FileWriter(storageProps.dir).use { out ->
            out.write(data)
            out.close()
        }
    }

    fun create(confluence: Confluence): Confluence {
        confluences += confluence
        persist()
        return confluence
    }

    fun update(mergedConfluence: Confluence): Confluence {
        confluences = confluences
                .filter { it.confluenceKey != mergedConfluence.confluenceKey }
                .plus(mergedConfluence)
        persist()
        return mergedConfluence
    }

    fun removeDangling(confluence: Confluence) {
        confluences = confluences
                .filter { it.slackChannelId != confluence.slackChannelId }
        persist()
    }
}



