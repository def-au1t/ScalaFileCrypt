import java.nio.file.{Files, NoSuchFileException, Path, Paths, StandardCopyOption}
import java.security.InvalidKeyException
import java.io.{BufferedInputStream, BufferedReader, File, FileInputStream, FileOutputStream, InputStream}

import scala.collection.mutable.ListBuffer
import java.util.zip.{ZipEntry, ZipException, ZipFile, ZipInputStream, ZipOutputStream}
import java.io.{FileOutputStream, InputStream}

import sun.security.util.Password

import scala.jdk.CollectionConverters._
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.IOException
import util.control.Breaks._

import scalafx.scene.control.Alert.AlertType


class FileManager(controller: Controller) {
  var files = Vector.empty[File]
  var allAreEncrypted = false
  var EncryptionAlgorithm = "AES"



  def numberOfFiles(): Int = files.size


  def encryptFiles(password: String, compressed: Int = -1): Int = {
    if (password.length < 3){
      throw new InvalidKeyException("Hasło musi mieć przynajmniej 3 znaki")
    }
    var numberEncrypted = 0
    if (this.files.isEmpty){
      throw new IllegalStateException("Nie wybrano plików.")
    }
    var encryptedFiles = ListBuffer.empty[File]
    for (file <- this.files) {
      var output = new File(file.toString + ".enc")
      if(compressed != -1) {
        output = new File(file.toString)
      }
      CryptoUtils.encrypt(password, file, output, this.EncryptionAlgorithm)
      encryptedFiles += output
      numberEncrypted += 1
    }
    this.files = encryptedFiles.toVector
    if(compressed != -1 && numberEncrypted == 1) {
      compressed
    }
    else numberEncrypted
  }

  def decryptFiles(password: String, selectedDirectory: File): Int = {

    var numberDecrypted = 0
    var decryptedFiles = ListBuffer.empty[File]
    for (file <- this.files) {
      breakable {
        var name = selectedDirectory.toString + "\\" + file.getName.substring(0, file.getName.length - 4);
        var output = new File(name)
        try {
          CryptoUtils.decrypt(password, file, output, this.EncryptionAlgorithm)
        }
        catch {
          case ex: InvalidKeyException => {
            this.controller.showAlert(AlertType.Error,
              "Nieprawidłowe hasło",
              header = ex.getMessage
            )
            break
          }
          case ex: Exception => {
            this.controller.showAlert(AlertType.Error,
              "Nieoczekiwany błąd",
              header = ex.getMessage
            )
          }
        }
        if (this.isZipFile(output)) {
          this.unpackFiles(output, selectedDirectory, decryptedFiles)
        }
        else {
          decryptedFiles += output
        }
        numberDecrypted += 1
      }
    }
    this.files = decryptedFiles.toVector
    return numberDecrypted
  }

  def compressFiles(f: File, password: String): Int = {

    val comp = new ZipOutputStream(new FileOutputStream(f.getPath))
    var compressed = 0
//    if (this.files.isEmpty){
//      throw new IllegalStateException("Nie wybrano plików.")
//    }
    for (file <- this.files) {
      comp.putNextEntry(new ZipEntry(file.getName))
      val input = new BufferedInputStream(new FileInputStream(file.toString))
      val bytes = input.readAllBytes()
      comp.write(bytes)
      input.close()
      comp.closeEntry()
      compressed += 1
    }
    comp.close()
    this.clearFiles()
    this.files = this.files :+ f
    this.encryptFiles(password, compressed)
  }

  def using[T <: {def close()}, U](resource: T)(block: T => U): U = {
    try {
      block(resource)
    } finally {
      if (resource != null) {
        resource.close()
      }
    }
  }


  @throws[IOException]
  def isZipFile(file: File): Boolean = {
    if (file.isDirectory) return false
    if (!file.canRead) throw new IOException("Cannot read file " + file.getAbsolutePath)
    if (file.length < 4) return false
    val in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))
    val test = in.readInt
    in.close()
    test == 0x504b0304
  }

  def unpackFiles(file: File, dir: File, decryptedFiles: ListBuffer[File]): Int = {
    var unpacked = 0
    val outputPath = dir.toPath
//    if (this.files.isEmpty){
//      throw new IllegalStateException("Nie wybrano plików.")
//    }
    using(new ZipFile(file)) { packedFile =>
      if (packedFile != null) {
        for (f <- packedFile.entries.asScala) {
          val path = outputPath.resolve(f.getName)
          if (f.isDirectory) {
            Files.createDirectories(path)
          } else {
            Files.createDirectories(path.getParent)
            Files.copy(packedFile.getInputStream(f), path, StandardCopyOption.REPLACE_EXISTING)
            decryptedFiles += new File(path.toString)
          }
        }
        unpacked += 1
      }
    }
    file.delete()
    unpacked
  }

  def checkIfAllEncrypted(): Boolean = {
    var result = true
    if (this.files.isEmpty) return false
    for (file <- this.files){
      if(file.getName.length < 4 || file.getName.substring(file.getName.length-4, file.getName.length) != ".enc")
        result = false
    }
    if (result != this.allAreEncrypted){
      this.allAreEncrypted = result;
    }
    return result
  }


  def addFiles(files: Seq[File]): Unit = {
    this.files ++= files
    this.onFilesChange()
  }

  def removeFiles(files: Seq[File]): Unit = {
    this.files = this.files.diff(files)
    this.onFilesChange()
  }

  def clearFiles(): Unit = {
    this.files = Vector.empty[File]
    this.onFilesChange()
  }

  def onFilesChange() = {
    this.controller.setButtonStatus()
    this.controller.updateFileListView()
  }
}
