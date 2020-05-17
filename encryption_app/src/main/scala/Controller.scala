import java.io.File
import java.net.URL
import java.security.InvalidKeyException

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.{ListView, PasswordField, SelectionMode}
import javafx.scene.input.{DragEvent, Dragboard}
import javafx.stage.DirectoryChooser
//import javax.annotation.Resources
import scalafx.stage.FileChooser
import scalafx.stage.Stage
import scalafx.scene.control.{Alert, ButtonType}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.input.TransferMode

import scala.jdk.javaapi.CollectionConverters
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class Controller(){

  val fileManager = new FileManager(this)


  @FXML
  var filesList: ListView[File] = _

  @FXML
  var passwordField: PasswordField = _

  @FXML
  def initialize(): Unit = {
    filesList.getSelectionModel.setSelectionMode(SelectionMode.MULTIPLE)
  }

  @FXML
  def menuFileCloseOnClick(event: ActionEvent): Unit = {
    System.exit(0)
  }

  @FXML
  def menuHelpAboutOnClick(event: ActionEvent): Unit = {
    this.showAlert(AlertType.Information,
      "O Aplikacji ",
      "Aplikacja powstała w ramach projektu z przedmiotu:\nProgramowanie w języku Scala",
      "Wersja: 0.1\n\nAutorzy: \n- Kamil Koczera\n- Jacek Nitychoruk")
  }

  @FXML
  def buttonEncryptOnClick(event: ActionEvent): Unit = {
    var encrypted = 0
    try {
      encrypted = this.fileManager.encryptFiles(passwordField.getText)
    } catch {
      case ex: IllegalStateException =>{
        this.showAlert(AlertType.Error,
          "Wystąpił błąd",
          header=ex.getMessage
          )
        return
      }
      case ex: InvalidKeyException =>{
        this.showAlert(AlertType.Error,
          "Nieprawidłowe hasło",
          header=ex.getMessage
        )
        return
      }
    }
    this.showAlert(AlertType.Information,
      "Szyfrowanie zakończone",
      "Zakończono pomyślnie szyfrowanie plików",
      "Zaszyfrowano pomyślnie: " + encrypted + "\\" + this.fileManager.numberOfFiles)
    this.passwordField.setText("")
    this.updateFileListView()
  }

  @FXML
  def buttonDecryptOnClick(): Unit = {
    var decrypted = 0
    try {
      decrypted = this.fileManager.decryptFiles(passwordField.getText)
    } catch{
      case ex: IllegalStateException =>{
        this.showAlert(AlertType.Error,
          "Wystąpił błąd",
          header="Nie wybrano plików"
        )
        return
      }
      case ex: InvalidKeyException =>{
        this.showAlert(AlertType.Error,
          "Nieprawidłowe hasło",
          header=ex.getMessage
        )
        return
      }
    }
    this.showAlert(AlertType.Information,
      "Deszyfrowanie zakończone",
      "Zakończono pomyślnie odszyfrowywanie plików",
      "Zaszyfrowano pomyślnie: " + decrypted + "\\" + this.fileManager.numberOfFiles)
    this.passwordField.setText("")
    this.updateFileListView()
  }

  @FXML
  def buttonCompressFilesOnClick(event: ActionEvent): Unit = {
    var compressed = 0
    try {
      val stage = new Stage()
      val fileChooser = new FileChooser()

      val extFilter = new FileChooser.ExtensionFilter("ZIP files (*.zip)", "*.zip")
      fileChooser.getExtensionFilters.add(extFilter)

      val file = fileChooser.showSaveDialog(stage)
      compressed = this.fileManager.compressFiles(file)

      this.showAlert(AlertType.Information,
        "Kompresja zakończona",
        "Zakończono pomyślnie kompresowanie plików",
        "Skompresowano pomyślnie: " + compressed + "\\" + this.fileManager.numberOfFiles)

    } catch {
      case ex: IllegalStateException =>{
        this.showAlert(AlertType.Error,
          "Wystąpił błąd",
          header=ex.getMessage
        )
        return
      }
    }
    this.updateFileListView()
  }

  @FXML
  def buttonUnpackFilesOnClick(event: ActionEvent): Unit = {
    var unpacked = 0
    try {
      val stage = new Stage()
      val directoryChooser = new DirectoryChooser()
      directoryChooser.setTitle("Wybierz miejsce do zapisu wypakowanych plików")
      val selectedDirectory = directoryChooser.showDialog(stage)
      if (selectedDirectory != null){
        unpacked = this.fileManager.unpackFiles(selectedDirectory)
        this.showAlert(AlertType.Information,
          "Wypakowywanie zakończone",
          "Zakończono pomyślnie wypakowywanie plików",
          "Wypakowano pomyślnie: " + unpacked + "\\" + this.fileManager.numberOfFiles)

      }
    } catch {
      case ex: IllegalStateException =>{
        this.showAlert(AlertType.Error,
          "Wystąpił błąd",
          header="Nie wybrano plików"
        )
        return
      }
    }
    this.updateFileListView()
  }

  @FXML
  def buttonSelectFilesOnClick(event: ActionEvent): Unit = {
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Wybierz pliki")
    val stage = new Stage()
    val files = fileChooser.showOpenMultipleDialog(stage)
    if (files != null){
      this.addFiles(files)
    }
  }

  @FXML
  def buttonDeleteSelectedFilesOnClick(event: ActionEvent): Unit = {
    val selectedFiles : ArrayBuffer[File] = ArrayBuffer.empty
    val it = this.filesList.getSelectionModel.getSelectedItems.iterator()
    it.forEachRemaining(selectedFiles.addOne)
    this.removeFiles(selectedFiles.toSeq)

  }

  @FXML
  def buttonCleanFilesOnClick(event: ActionEvent): Unit = {
    this.fileManager.clearFiles()
    this.updateFileListView()
  }

  def updateFileListView(): Unit = {
    this.filesList.getItems.clear()
    if (this.fileManager.files != null) {
      for (file <- this.fileManager.files) {
        this.filesList.getItems.add(file)
      }
    }
  }

  def draggedFileOver(event: DragEvent): Unit = {
    if (event.getDragboard.hasFiles) {
      event.acceptTransferModes(TransferMode.Copy)
    }
    event.consume()
  }

  def droppedFile(event: DragEvent): Unit = {
    val db : Dragboard = event.getDragboard
    if(db.hasFiles){
      val droppedFiles = db.getFiles
      this.addFiles(CollectionConverters.asScala(droppedFiles).toSeq)
    }
    event.setDropCompleted(true)
  }

  def showAlert(alertType: AlertType, title : String, header : String = null, content: String = null): Option[ButtonType] = {
    val alert = new Alert(alertType)
    alert.setTitle(title)
    alert.setHeaderText(header)
    alert.setContentText(content)
    alert.showAndWait()
  }

  def addFiles(files: Seq[File]): Unit = {
    this.fileManager.addFiles(files)
    this.updateFileListView()
  }

  def removeFiles(files: Seq[File]): Unit = {
    this.fileManager.removeFiles(files)
    this.updateFileListView()
  }

}
