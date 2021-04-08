import HelperClasses.Communicator;
import HelperClasses.Controller;
import HelperClasses.Data;
import HelperClasses.Device;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

public class ExperimentController extends Controller {


    @FXML
    private Label deviceLable;
    @FXML
    private Label modeLable;

    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        deviceLable.setText(getData().getDevice().toString());
        if(getData().getDevice() == Device.MOOSE){
            getCommunicator().changeController(this);
            modeLable.setText(getData().getMode().toString());
        }

    }

    public void clickedNext(ActionEvent actionEvent) throws IOException {
        goToView("ScrollView.fxml");
    }

    public void clickedBack(ActionEvent actionEvent) throws IOException {
        if(getData().getDevice() == Device.MOOSE){
            goToView("ConnectMooseView.fxml");
        }else{
            goToView("StartView.fxml");
        }

    }
}
