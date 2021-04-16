package HelperClasses;

import com.sun.javafx.scene.SceneEventDispatcher;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Controller {

    private Communicator communicator;
    private Data data;
    private Robot robot;

    @FXML private Pane pane;

    public void initData(Communicator communicator, Data data) {
        this.communicator = communicator;
        this.data = data;
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void onLoad(){
    }

    public void incomingMessage(String message) {
        System.out.println("-- NEW MESSAGE: " + message);
        Message m = new Message(message);

        if(m.getActionType().equals("Action")) {
            if (m.getActionName().equals("click")) {
                robot.mousePress(16);
                robot.mouseRelease(16);

            }
        }
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

      //  controller.onLoad();
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

    public Pane getMainPane(){
        return pane;
    }

    public Robot getRobot(){
        return robot;
    }

}
