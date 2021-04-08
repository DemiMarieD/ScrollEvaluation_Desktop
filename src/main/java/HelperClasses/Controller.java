package HelperClasses;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class Controller {

    private Communicator communicator;
    private Data data;

    @FXML private Pane pane;

    public void initData(Communicator communicator, Data data) {
        this.communicator = communicator;
        this.data = data;
    }

    public void incomingMessage(String message) {
        System.out.println("-- NEW MESSAGE: " + message);
        //to be overwritten
    }

    public void goToView(String fxml) throws IOException {
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


    //Getter & Setter

    public void setCommunicator(Communicator communicator){
        this.communicator = communicator;
    }

    public Communicator getCommunicator(){
        return communicator;
    }

    public Data getData(){
        return data;
    }

}
