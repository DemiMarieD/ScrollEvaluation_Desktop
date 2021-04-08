import HelperClasses.Communicator;
import HelperClasses.Controller;
import HelperClasses.Data;
import HelperClasses.Device;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class StartController extends Controller{

    //Layout Elements
    @FXML
    private Pane pane;

    private Data data = new Data();
    private Communicator communicator = null;

    @Override
    public void initData(Communicator communicator, Data data) {
        //super.initData(communicator, data);
        this.communicator = communicator;
        this.data = data;
    }

    public void nextMoose(ActionEvent actionEvent) throws Exception {
        data.setDevice(Device.MOOSE);
        next("ConnectMooseView.fxml");
    }

    public void nextMouse(ActionEvent actionEvent) throws Exception {
        data.setDevice(Device.MOUSE);
        next("Experiment_StartView.fxml");
    }

    public void next(String fxml) throws IOException {
        //set scene
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource(fxml));
        Parent root = loader.load();
        Scene newScene = new Scene(root);

        //access controller
        Controller controller = loader.getController();
        //pass communicator
        controller.initData(communicator, data);

        //set stage
        Stage window = (Stage) pane.getScene().getWindow();
        window.setScene(newScene);
        window.show();
    }


}
