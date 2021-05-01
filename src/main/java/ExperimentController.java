import HelperClasses.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ExperimentController extends Controller {

    @FXML
    private TextField blocksField;
    @FXML
    private TextField idField;
    @FXML
    private Label deviceLable;

    @FXML
    private ComboBox cb;

    private ScrollingMode[] modes = new ScrollingMode[]{ ScrollingMode.WHEEL,
            ScrollingMode.DRAG, null, ScrollingMode.CIRCLE, ScrollingMode.RUBBING, ScrollingMode.FLICK_deceleration,
            ScrollingMode.FLICK_iOS, ScrollingMode.RATE_BASED};


    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        deviceLable.setText(getData().getDevice().toString());
        cb.setVisible(false);
        if(getData().getDevice() == Device.MOOSE){
            getCommunicator().changeController(this);
            cb.setVisible(true);
            cb.setItems(FXCollections.observableArrayList("Wheel", "Drag", new Separator(), "Circle", "Rubbing", "Flick Decelerate", "iOS - Demi", "Rate-Based")
            ); //new Separator(), can also be added
        }

    }


    public void clickedHyperlink(ActionEvent actionEvent) throws IOException {
        if(getData().getDevice() == Device.MOOSE) {
            int selectedIndex = cb.getSelectionModel().getSelectedIndex();
            if(selectedIndex > -1) {
                //todo check index and give error on missing selection !
                getData().setMode(modes[selectedIndex]);
                getCommunicator().sendMessage(new Message("Server", "Mode", getData().getMode().getValue()).makeMessage());
            }
        }

        goToView("HyperlinkView.fxml");
    }

    public void clickedNext(ActionEvent actionEvent) throws IOException {
        if(getData().getDevice() == Device.MOOSE) {
            int selectedIndex = cb.getSelectionModel().getSelectedIndex();
            if(selectedIndex > -1) {
                //todo check index and give error on missing selection !
                getData().setMode(modes[selectedIndex]);
                getCommunicator().sendMessage(new Message("Server", "Mode", getData().getMode().getValue()).makeMessage());
            }
        }

        getData().setParticipantID(Integer.parseInt(idField.getText()));
        getData().setNumberOfBlocks(Integer.parseInt(blocksField.getText()));

        goToView("RichTextView.fxml");
    }

    public void clickedCount(ActionEvent actionEvent) throws IOException {
        if(getData().getDevice() == Device.MOOSE) {
            int selectedIndex = cb.getSelectionModel().getSelectedIndex();
            if(selectedIndex > -1) {
                //todo check index and give error on missing selection !
                getData().setMode(modes[selectedIndex]);
                getCommunicator().sendMessage(new Message("Server", "Mode", getData().getMode().getValue()).makeMessage());
            }
        }
        goToView("CountView.fxml");
    }

    public void clickedBack(ActionEvent actionEvent) throws IOException {
        if(getData().getDevice() == Device.MOOSE){
            goToView("ConnectMooseView.fxml");
        }else{
            goToView("StartView.fxml");
        }

    }

}
