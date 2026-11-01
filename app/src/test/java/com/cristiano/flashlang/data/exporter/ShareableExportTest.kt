package com.cristiano.flashlang.data.exporter

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ShareableExportTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun writesExportIntoExportsDirectoryAndReturnsUriFactoryResult() {
        val cacheDir = temporaryFolder.newFolder("cache")
        val oldDirectory = File(cacheDir, "exports").apply { mkdirs() }
        File(oldDirectory, "old.json").writeText("old")

        val result = writeTemporaryExportAndGetUri(
            cacheDir = cacheDir,
            fileName = "flashlang-backup-2026-11-01.json",
            content = "{\"app\":\"FlashLang\"}",
            uriFactory = { it },
        )

        val exportFile = result
        assertEquals(File(cacheDir, "exports/flashlang-backup-2026-11-01.json"), exportFile)
        assertEquals("{\"app\":\"FlashLang\"}", exportFile.readText())
        assertEquals(listOf(exportFile), oldDirectory.listFiles()?.toList())
    }
}
