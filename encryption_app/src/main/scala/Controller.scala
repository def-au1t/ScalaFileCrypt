import java.io.File

import encryption.CryptoUtils
import encryption.Main
import javafx.event.ActionEvent
import javafx.fxml.FXML
import scalafx.stage.FileChooser

package encryptionApp {

  import scalafx.stage.Stage

  class Controller {
    @FXML
    def fileEncryptChoose(event: ActionEvent) = {
      val fileChooser = new FileChooser()
      fileChooser.setTitle("Open Resource File")
      val stage = new Stage()
      val selectedFiles = fileChooser.showOpenMultipleDialog(stage)
      for (file <- selectedFiles) {
        var output = new File(file.toString() + ".enc")
        CryptoUtils.encrypt("sometestsometest", file, output)
      }
    }

    @FXML
    def fileDecryptChoose() = {
      val fileChooser = new FileChooser {
        title = "Open encrypted File"
      }
      val stage = new Stage()
      val selectedFiles = fileChooser.showOpenMultipleDialog(stage)
      for (file <- selectedFiles) {
        if(! file.toString.endsWith("enc")){
          print("Error")
        }
        var name = file.toString.substring(0, file.toString.length - 4)
        var output = new File(name)
        CryptoUtils.decrypt("sometestsometest", file, output)
      }
    }
  }

}