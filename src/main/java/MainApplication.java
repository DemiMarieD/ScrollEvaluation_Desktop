import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //set scene
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("StartView.fxml"));
        Parent root = loader.load();
        Scene newScene = new Scene(root, 1280, 650);

        primaryStage.setTitle("Scroll Evaluator");
        primaryStage.setScene(newScene);
        primaryStage.show();
    }



}
