package encryptionApp {

  import java.io.File
  import javafx.event.ActionEvent
  import javafx.fxml.FXML
  import javafx.scene.control.ListView
  import scalafx.stage.FileChooser
  import scalafx.stage.Stage
  import scalafx.scene.control.Alert
  import scalafx.scene.control.Alert.AlertType

  import scala.collection.mutable.ListBuffer

  class Controller {
    var selectedFiles: Seq[File] = _

    @FXML
    var filesList: ListView[File] = _

    @FXML
    def fileEncrypt(event: ActionEvent) = {
      if(this.selectedFiles != null) {
        var encryptedFiles : ListBuffer[File] = ListBuffer()
        for (file <- this.selectedFiles) {
          var output = new File(file.toString + ".enc")
          CryptoUtils.encrypt("sometestsometest", file, output)
          encryptedFiles += output
        }
        this.selectedFiles = encryptedFiles.toList
        this.updateFileListView()

        val alert = new Alert(AlertType.Information)
        alert.setTitle("Szyfrowanie zakończone")
        alert.setHeaderText("Zakończono pomyślnie szyfrowanie plików")
        alert.setContentText("Plików: " + this.selectedFiles.size)

        alert.showAndWait();
      }
    }

    @FXML
    def fileDecrypt() = {
      if(this.selectedFiles != null) {
        var decryptedFiles : ListBuffer[File] = ListBuffer()
        for (file <- this.selectedFiles) {
          var name = file.toString.substring(0, file.toString.length - 4)
          var output = new File(name)
          CryptoUtils.decrypt("sometestsometest", file, output)
          decryptedFiles += output
        }

        this.selectedFiles = decryptedFiles.toList
        this.updateFileListView()

        val alert = new Alert(AlertType.Information)
        alert.setTitle("Odszyfrowywanie zakończone")
        alert.setHeaderText("Zakończono pomyślnie deszyfrowanie plików")
        alert.setContentText("Plików: " + this.selectedFiles.size)

        alert.showAndWait();
      }
    }


    @FXML
    def selectFiles(event: ActionEvent) = {
      val fileChooser = new FileChooser()
      fileChooser.setTitle("Wybierz pliki")
      val stage = new Stage()
      val files = fileChooser.showOpenMultipleDialog(stage)
      if (files != null){
        this.selectedFiles = files
        this.updateFileListView()
      }
    }

    def cleanFiles(event: ActionEvent) = {
      this.selectedFiles = null
      this.updateFileListView()
    }

    def updateFileListView() = {
      this.filesList.getItems.clear()
      if (this.selectedFiles != null) {
        for (file <- this.selectedFiles) {
          this.filesList.getItems.add(file)
        }
      }
    }
  }

}