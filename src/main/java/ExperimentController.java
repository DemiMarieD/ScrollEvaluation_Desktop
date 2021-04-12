import HelperClasses.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;

import java.io.IOException;

public class ExperimentController extends Controller {


    @FXML
    private Label deviceLable;

    @FXML
    private ComboBox cb;

    private ScrollingMode[] modes = new ScrollingMode[]{ScrollingMode.DRAG, ScrollingMode.FLICK, ScrollingMode.RATE_BASED,
            ScrollingMode.CIRCLE, ScrollingMode.RUBBING, null, ScrollingMode.WHEEL, ScrollingMode.DRAG_2, ScrollingMode.THUMB};

    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        deviceLable.setText(getData().getDevice().toString());
        cb.setVisible(false);
        if(getData().getDevice() == Device.MOOSE){
            getCommunicator().changeController(this);
            cb.setVisible(true);
            cb.setItems(FXCollections.observableArrayList(
                    "Drag", "Flick", "Rate-Based", "Circle", "Rubbing", new Separator(), "Wheel", "real Drag", "Thumb")
            ); //new Separator(), can also be added
        }

    }

    public void clickedNext(ActionEvent actionEvent) throws IOException {
        if(getData().getDevice() == Device.MOOSE) {
            int selectedIndex = cb.getSelectionModel().getSelectedIndex();
            //todo check index and give error on missing selection !
            getData().setMode(modes[selectedIndex]);
            getCommunicator().sendMessage(new Message("Server", "Mode", getData().getMode().getValue()).makeMessage());
        }

        goToView("RichTextView.fxml");
    }

    public void clickedBack(ActionEvent actionEvent) throws IOException {
        if(getData().getDevice() == Device.MOOSE){
            goToView("ConnectMooseView.fxml");
        }else{
            goToView("StartView.fxml");
        }

    }

    public void clickedOther(ActionEvent actionEvent) throws IOException {
        goToView("ScrollView.fxml");
    }
}
