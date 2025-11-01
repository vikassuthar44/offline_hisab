package best.app.offlinehisab.backup

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileZipper {

    fun zipFiles(files: List<File>, zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
            files.forEach { file ->
                if (file.exists()) {
                    val entry = ZipEntry(file.name)
                    out.putNextEntry(entry)
                    file.inputStream().use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
    }

    fun unzip(zipFile: File, destinationDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zipInput ->
            var entry: ZipEntry?
            while (zipInput.nextEntry.also { entry = it } != null) {
                val file = File(destinationDir, entry!!.name)
                FileOutputStream(file).use { output ->
                    zipInput.copyTo(output)
                }
                zipInput.closeEntry()
            }
        }
    }
}
