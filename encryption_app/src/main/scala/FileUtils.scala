import javax.crypto.{BadPaddingException, Cipher, IllegalBlockSizeException, NoSuchPaddingException}
import javax.crypto.spec.SecretKeySpec
import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.security.{DigestInputStream, InvalidKeyException, MessageDigest, NoSuchAlgorithmException}
import java.nio.charset.StandardCharsets

object FileUtils {

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
    doCrypt(Cipher.ENCRYPT_MODE, password, inputFile, outputFile, algorithm, secretKeyAlgorithm)
  }

  @throws[CryptoException]
  def decrypt(password: String, inputFile: File, outputFile: File, algorithm: String): Unit = {
    val secretKeyAlgorithm = getSecretKeyAlgorithm(algorithm)
    doCrypt(Cipher.DECRYPT_MODE, password, inputFile, outputFile, algorithm, secretKeyAlgorithm)
  }

  @throws[CryptoException]
  private def doCrypt(cipherMode: Int, password: String, inputFile: File, outputFile: File, algorithm: String,
                      secretKeyAlgorithm: String): Unit = {
    var inputStream  = None: Option[FileInputStream]
    var outputStream  = None: Option[FileOutputStream]
    try {
      val digest = MessageDigest.getInstance("SHA-256")
      val encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8)).slice(0, 16)
      val secretKey = new SecretKeySpec(encodedHash, algorithm)
      val cipher = Cipher.getInstance(secretKeyAlgorithm)
      var inputBytes = new Array[Byte](inputFile.length.asInstanceOf[Int])

      inputStream = Some(new FileInputStream(inputFile))
      var controlSum = new Array[Byte](16)

      cipher.init(cipherMode, secretKey)

      inputStream.get.read(inputBytes)

      if(cipherMode == Cipher.DECRYPT_MODE) {
        controlSum = inputBytes.slice(inputBytes.length-16, inputBytes.length)
        inputBytes = inputBytes.slice(0, inputBytes.length-16)
      }

      var outputBytes = cipher.doFinal(inputBytes)

      if(cipherMode == Cipher.ENCRYPT_MODE) {
        outputBytes = outputBytes ++ computeControlSum(inputFile)
      }

      outputStream = Some(new FileOutputStream(outputFile, false))
      outputStream.get.write(outputBytes)


      if(cipherMode == Cipher.DECRYPT_MODE && !controlSum.diff(computeControlSum(outputFile)).isEmpty) {
        outputFile.delete()
        throw new Exception("Sumy kontrolne się nie zgadzają")
      }

    } catch {
      case e: BadPaddingException =>
        throw new InvalidKeyException("Nieprawidłowe hasło lub zły algorytm szyfrowania", e)
      case ex@(_: NoSuchPaddingException | _: NoSuchAlgorithmException | _: IllegalBlockSizeException | _: IOException) =>
        throw new Exception("Error encrypting/decrypting file", ex)
    }
    finally{
      if (inputStream.isDefined) inputStream.get.close()
      if (outputStream.isDefined) outputStream.get.close()
    }
  }
}


class CryptoException() extends Exception {
  def this(message: String, throwable: Throwable) {
    this()
  }
}
