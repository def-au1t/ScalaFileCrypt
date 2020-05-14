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
  private val ALGORITHM = "AES"
  private val TRANSFORMATION = "AES"

  @throws[CryptoException]
  def encrypt(password: String, inputFile: File, outputFile: File): Unit = {
    doCrypto(Cipher.ENCRYPT_MODE, password, inputFile, outputFile)
  }

  @throws[CryptoException]
  def decrypt(password: String, inputFile: File, outputFile: File): Unit = {
    doCrypto(Cipher.DECRYPT_MODE, password, inputFile, outputFile)
  }

  @throws[CryptoException]
  private def doCrypto(cipherMode: Int, password: String, inputFile: File, outputFile: File): Unit = {
    try {
      val digest = MessageDigest.getInstance("SHA-256")
      val encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8)).slice(0, 16)
      val secretKey = new SecretKeySpec(encodedHash, ALGORITHM)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(cipherMode, secretKey)
      val inputStream = new FileInputStream(inputFile)
      val inputBytes = new Array[Byte](inputFile.length.asInstanceOf[Int])
      inputStream.read(inputBytes)
      val outputBytes = cipher.doFinal(inputBytes)
      val outputStream = new FileOutputStream(outputFile)
      outputStream.write(outputBytes)
      inputStream.close()
      outputStream.close()
    } catch {
      case ex@(_: NoSuchPaddingException | _: NoSuchAlgorithmException | _: BadPaddingException | _: IllegalBlockSizeException | _: IOException) =>
        throw new Exception("Error encrypting/decrypting file", ex)
    }
  }
}


class CryptoException() extends Exception {
  def this(message: String, throwable: Throwable) {
    this()
  }
}
