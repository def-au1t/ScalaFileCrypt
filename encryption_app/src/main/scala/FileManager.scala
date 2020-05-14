import java.io.File
import java.nio.file.NoSuchFileException
import java.security.InvalidKeyException

import scala.collection.mutable.ListBuffer


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

  def addFiles(files: Seq[File]): Unit = this.files ++= files

  def removeFiles(files: Seq[File]): Unit = this.files = this.files.diff(files)

  def clearFiles(): Unit = this.files = Vector.empty[File]
}
