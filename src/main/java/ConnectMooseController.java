import HelperClasses.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class ConnectMooseController extends Controller {


    //Layout Elements
    @FXML
    private  AnchorPane ConnectSuccess_Pane;
    @FXML
    private  AnchorPane ConnectInfo_Pane;


    @FXML
    private  Label label_ip;
    @FXML
    private  Label label_port;
    @FXML
    private Button nextBtn;


    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        if(communicator == null){
            setCommunicator(new Communicator(this));
            label_ip.setText("IP Address: " + getCommunicator().getIP());
            label_port.setText("Port:" + getCommunicator().getPort());
            getCommunicator().startConnecting();
            setLayout(false);
        }else{
            setLayout(true);
        }
    }

    @Override
    public void portChanged() {
        super.portChanged();
        label_port.setText("Port:" + getCommunicator().getPort());
    }

    public void setLayout(Boolean connected){

        if(connected){
            ConnectInfo_Pane.setVisible(false);
            ConnectSuccess_Pane.setVisible(true);
            nextBtn.setVisible(true);
        }else{
            ConnectInfo_Pane.setVisible(true);
            ConnectSuccess_Pane.setVisible(false);
            nextBtn.setVisible(false);
        }
    }

    @Override
    public void incomingMessage(String message) {
        super.incomingMessage(message);

        Message m = new Message(message);
        System.out.println(m.getActionName());
        if(m.getActionName().equals("Connected")) {
            setLayout(true);
        }
    }


    public void clickedNext(ActionEvent actionEvent) throws IOException {
        //Inform Client/Moose
        getCommunicator().sendMessage(new Message("Server", "Next", "Connected").makeMessage());

        goToView("Experiment_StartView.fxml");
    }

    public void clickedBack(ActionEvent actionEvent) throws IOException {
        goToView("StartView.fxml");
    }
}
