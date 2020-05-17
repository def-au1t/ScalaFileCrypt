import java.nio.file.{Files, NoSuchFileException, Path, Paths, StandardCopyOption}
import java.security.InvalidKeyException
import java.io.{BufferedInputStream, BufferedReader, File, FileInputStream, FileOutputStream, InputStream}

import scala.collection.mutable.ListBuffer
import java.util.zip.{ZipEntry, ZipException, ZipFile, ZipInputStream, ZipOutputStream}
import java.io.{FileOutputStream, InputStream}

import scala.collection.JavaConverters._

class FileManager(controller: Controller) {
  var files = Vector.empty[File]


  def numberOfFiles(): Int = files.size


  def encryptFiles(password: String): Int = {
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
      CryptoUtils.encrypt(password, file, output)
      encryptedFiles += output
      numberEncrypted += 1
    }
    this.files = encryptedFiles.toVector
    return numberEncrypted
  }
  def decryptFiles(password: String): Int = {
    if (password.length < 3){
      throw new InvalidKeyException("Hasło musi mieć przynajmniej 3 znaki")
    }
    var numberDecrypted = 0
    if (this.files.isEmpty) {
      throw new IllegalStateException("Nie wybrano plików")
    }
    var decryptedFiles = ListBuffer.empty[File]
    for (file <- this.files) {
      var name = file.toString.substring(0, file.toString.length - 4)
      var output = new File(name)
      CryptoUtils.decrypt(password, file, output)
      decryptedFiles += output
      numberDecrypted += 1
    }
    this.files = decryptedFiles.toVector
    return numberDecrypted
  }

  def compressFiles(f: File): Int = {

    val comp = new ZipOutputStream(new FileOutputStream(f.getPath))
    var compressed = 0
    if (this.files.isEmpty){
      throw new IllegalStateException("Nie wybrano plików.")
    }
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
    compressed
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

  import java.io.BufferedInputStream
  import java.io.DataInputStream
  import java.io.FileInputStream
  import java.io.IOException

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

  def unpackFiles(f: File): Int = {
    var unpacked = 0
    val outputPath = f.toPath
    if (this.files.isEmpty){
      throw new IllegalStateException("Nie wybrano plików.")
    }
    for (file <- files) {
      if (isZipFile(file)) {
        using(new ZipFile(file)) { packedFile =>
          if (packedFile != null) {
            for (f <- packedFile.entries.asScala) {
              val path = outputPath.resolve(f.getName)
              if (f.isDirectory) {
                Files.createDirectories(path)
              } else {
                Files.createDirectories(path.getParent)
                Files.copy(packedFile.getInputStream(f), path, StandardCopyOption.REPLACE_EXISTING)
              }
            }
            unpacked += 1
          }
        }
      }
    }
    unpacked
  }

  def addFiles(files: Seq[File]): Unit = this.files ++= files

  def removeFiles(files: Seq[File]): Unit = this.files = this.files.diff(files)

  def clearFiles(): Unit = this.files = Vector.empty[File]
}
