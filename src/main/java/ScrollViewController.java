import HelperClasses.*;
import javafx.event.ActionEvent;

import java.io.IOException;

public class ScrollViewController extends Controller {

    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        if(getData().getDevice() == Device.MOOSE){
            getCommunicator().changeController(this); //to receive Messages
            getCommunicator().sendMessage(new Message("Server", "Mode", getData().getMode().getValue()).makeMessage());
        }
    }



    public void clickedNext(ActionEvent actionEvent) throws IOException {
        //goToView(".fxml");
    }

    public void clickedBack(ActionEvent actionEvent) throws IOException {
        goToView("Experiment_StartView.fxml");
    }
}
