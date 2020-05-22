import java.io.File

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.{InvalidKeyException, Key, MessageDigest, NoSuchAlgorithmException}
import java.nio.charset.StandardCharsets

object CryptoUtils {


  def getSecretKeyAlgorithm(encryption: String) : String =  encryption match {
      case "AES" => "AES"
      case "Blowfish" => "Blowfish"
      case _ => "AES"
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
      val inputBytes = new Array[Byte](inputFile.length.asInstanceOf[Int])
      inputStream.read(inputBytes)
      val outputBytes = cipher.doFinal(inputBytes)
      val outputStream = new FileOutputStream(outputFile, false)
      outputStream.write(outputBytes)
      inputStream.close()
      outputStream.close()
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
