package encryptionApp

import java.io.{BufferedInputStream, File, FileInputStream}
import java.nio.file.Files

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.BorderPane
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import scalafx.Includes._

object MainWindow extends JFXApp {
  def fileImport() = {
    val fileChooser = new FileChooser {
      title = "Open Resource File"
    }
    val selectedFiles = fileChooser.showOpenMultipleDialog(stage)
    for (file <- selectedFiles) {
      var output = new File(file.toString()+".enc")
      CryptoUtils.encrypt("sometestsometest", file, output)
    }
  }
  def fileDecryption() = {
    val fileChooser = new FileChooser {
      title = "Open encrypted File"
    }
    val selectedFiles = fileChooser.showOpenMultipleDialog(stage)
    for (file <- selectedFiles) {
      if(! file.toString().endsWith("enc")){
        print("Error")
      }
      var name = file.toString() + ".dec"
      var output = new File(name)
      CryptoUtils.decrypt("sometestsometest", file, output)
    }
  }

  stage = new PrimaryStage {
    scene = new Scene {
      root = new BorderPane {
        padding = Insets(25)
        center = new Button("Decrypt") {
          onAction = () => {fileDecryption()}
        }
//        fileDecryption()
        bottom = new Button("Encrypt") {
          onAction = () => {fileImport()}
        }

      }

    }
  }
}

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.Key
import java.security.NoSuchAlgorithmException

object CryptoUtils {
  private val ALGORITHM = "AES"
  private val TRANSFORMATION = "AES"

  @throws[CryptoException]
  def encrypt(key: String, inputFile: File, outputFile: File): Unit = {
    doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile)
  }

  @throws[CryptoException]
  def decrypt(key: String, inputFile: File, outputFile: File): Unit = {
    doCrypto(Cipher.DECRYPT_MODE, key, inputFile, outputFile)
  }

  @throws[CryptoException]
  private def doCrypto(cipherMode: Int, key: String, inputFile: File, outputFile: File): Unit = {
    try {
      val secretKey = new SecretKeySpec(key.getBytes, ALGORITHM)
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