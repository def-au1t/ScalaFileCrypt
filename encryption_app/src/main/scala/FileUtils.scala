import java.io.File
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.{DigestInputStream, InvalidKeyException, Key, MessageDigest, NoSuchAlgorithmException}
import java.nio.charset.StandardCharsets

object CryptoUtils {

  def getSecretKeyAlgorithm(encryption: String) : String =  encryption match {
      case "AES" => "AES"
      case "Blowfish" => "Blowfish"
      case _ => "AES"
    }

  def computeControlSum(file: File): Array[Byte] = {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(file), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest
  }

  @throws[CryptoException]
  def encrypt(password: String, inputFile: File, outputFile: File, algorithm: String): Unit = {
    val secretKeyAlgorithm = getSecretKeyAlgorithm(algorithm)
    doCrypto(Cipher.ENCRYPT_MODE, password, inputFile, outputFile, algorithm, secretKeyAlgorithm)
  }

  @throws[CryptoException]
  def decrypt(password: String, inputFile: File, outputFile: File, algorithm: String): Unit = {
    val secretKeyAlgorithm = getSecretKeyAlgorithm(algorithm)
    doCrypto(Cipher.DECRYPT_MODE, password, inputFile, outputFile, algorithm, secretKeyAlgorithm)
  }

  @throws[CryptoException]
  private def doCrypto(cipherMode: Int, password: String, inputFile: File, outputFile: File, algorithm: String,
                      secretKeyAlgorithm: String): Unit = {
    try {
      val digest = MessageDigest.getInstance("SHA-256")
      val encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8)).slice(0, 16)
      val secretKey = new SecretKeySpec(encodedHash, algorithm)
      val cipher = Cipher.getInstance(secretKeyAlgorithm)
      cipher.init(cipherMode, secretKey)
      val inputStream = new FileInputStream(inputFile)
      var inputBytes = new Array[Byte](inputFile.length.asInstanceOf[Int])
      inputStream.read(inputBytes)
      var controlSum = new Array[Byte](16)
      if(cipherMode == Cipher.DECRYPT_MODE) {
        controlSum = inputBytes.slice(inputBytes.length-16, inputBytes.length)
        inputBytes = inputBytes.slice(0, inputBytes.length-16)
      }
      var outputBytes = cipher.doFinal(inputBytes)
      if(cipherMode == Cipher.ENCRYPT_MODE) {
        outputBytes = outputBytes ++ computeControlSum(inputFile)
      }
      val outputStream = new FileOutputStream(outputFile, false)
      outputStream.write(outputBytes)
      inputStream.close()
      outputStream.close()
      if(cipherMode == Cipher.DECRYPT_MODE &&
        controlSum.map("%02x".format(_)).mkString != computeControlSum(outputFile).map("%02x".format(_)).mkString) {
        outputFile.delete()
        throw new Exception("Sumy kontrolne się nie zgadzają")
      }
    } catch {
      case e: BadPaddingException =>
        throw new InvalidKeyException("Nieprawidłowe hasło lub zły algorytm szyfrowania", e)
      case ex@(_: NoSuchPaddingException | _: NoSuchAlgorithmException | _: IllegalBlockSizeException | _: IOException) =>
        throw new Exception("Error encrypting/decrypting file", ex)
    }
  }
}


class CryptoException() extends Exception {
  def this(message: String, throwable: Throwable) {
    this()
  }
}
