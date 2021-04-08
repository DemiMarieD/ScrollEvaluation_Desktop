import HelperClasses.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class ConnectMooseController extends Controller {


    //Layout Elements
    @FXML
    private  AnchorPane ConnectSuccess_Pane;
    @FXML
    private  AnchorPane ConnectInfo_Pane;
    @FXML
    private AnchorPane ScrollMode_pane;
    @FXML
    private ComboBox cb;
    @FXML
    private  Label label_ip;
    @FXML
    private  Label label_port;
    @FXML
    private Button nextBtn;

    //Variables

    private ScrollingMode [] modes = new ScrollingMode[]{ScrollingMode.DRAG, ScrollingMode.FLICK, ScrollingMode.RATE_BASED, ScrollingMode.CIRCLE, ScrollingMode.RUBBING};

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

    public void setLayout(Boolean connected){
        cb.setItems(FXCollections.observableArrayList(
                "Drag", "Flick", "Rate-Based", "Circle", "Rubbing")
        ); //new Separator(), can also be added

        if(connected){
            ConnectInfo_Pane.setVisible(false);
            ConnectSuccess_Pane.setVisible(true);
            ScrollMode_pane.setVisible(true);
            nextBtn.setVisible(true);
        }else{
            ConnectInfo_Pane.setVisible(true);
            ConnectSuccess_Pane.setVisible(false);
            ScrollMode_pane.setVisible(false);
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

        int selectedIndex = cb.getSelectionModel().getSelectedIndex();
        //todo check index and give error on missing selection !
        getData().setMode(modes[selectedIndex]);
       // getCommunicator().sendMessage(new HelperClasses.Message("Server", "Mode", getData().getMode().getValue()).makeMessage());

        goToView("Experiment_StartView.fxml");
    }

    public void clickedBack(ActionEvent actionEvent) throws IOException {
        goToView("StartView.fxml");
    }
}
