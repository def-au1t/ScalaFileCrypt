package encryption

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage


object Main {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[Main], args :_*)
  }
}

class Main extends Application {
  @throws[Exception]
  override def start(primaryStage: Stage): Unit = {
    val root = new FXMLLoader(getClass.getResource("main_view.fxml"))

    val mainViewRoot: Parent = root.load()
    val mainViewRootScene = new Scene(mainViewRoot)
    primaryStage.setScene(mainViewRootScene)
    primaryStage.setResizable(false)
    primaryStage.setTitle("Scala Encryption Tool")
    primaryStage.show()
  }
}
