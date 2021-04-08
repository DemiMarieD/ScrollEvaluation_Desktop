import HelperClasses.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ScrollViewController extends Controller {

    @FXML
    private ScrollPane scrollPane;

    private final VBox scrollContent = new VBox();

    // For Moose Scrolling
    private Thread scrollThread;
    private Robot robot;


    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        if(getData().getDevice() == Device.MOOSE){
            getCommunicator().changeController(this); //to receive Messages
            getCommunicator().sendMessage(new Message("Server", "Mode", getData().getMode().getValue()).makeMessage());
        }

        // Robot is used for click actions with Moose
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        setTextPanel(scrollPane, scrollContent);
    }

    public void setTextPanel(ScrollPane pane, VBox contentBox){
        javafx.scene.control.Label txt = new javafx.scene.control.Label();
        txt.setText(getText("src/main/resources/files/dogstory.txt"));
        txt.setWrapText(true);

        contentBox.getChildren().add(txt);
        contentBox.setSpacing(10);
        contentBox.setPadding(new Insets(10));
        contentBox.setFillWidth(true);
        pane.setFitToWidth(true);

        pane.setContent(contentBox);
        //  scrollPane_right.setPannable(true); // it means that the user should be able to pan the viewport by using the mouse.
        pane.setVvalue(0);

        //addListeners(pane); //Right now not doing anything
    }

    public void addListeners(javafx.scene.control.ScrollPane pane){
        pane.vvalueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                //todo smth when scrolled
            }
        });

        //When hover-value changed over Panel - send the updated infos to phone
        pane.hoverProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                //todo smth when Focus changes (aka mouse hover)
                //Only was needed for scrollbar
                /*
                if(scrollPane.isHover()) {
                    Message m = new Message("server", "Info", "panelSize");
                    m.setValue(String.valueOf(focusedPane.getHeight()));
                    Message m2 = new Message("server", "Info", "contentSize");
                    m2.setValue(String.valueOf(focusedContent.getHeight()));
                    Message m3 = new Message("server", "Info", "scrollValue");
                    m3.setValue(String.valueOf(pane.getVvalue()));

                    getCommunicator().sendMessage(m.makeMessage());
                    getCommunicator().sendMessage(m2.makeMessage());
                    getCommunicator().sendMessage(m3.makeMessage());
                }else{
                    Message m = new Message("server", "Info", "focusNull");
                    getCommunicator().sendMessage(m.makeMessage());
                }
                 */
            }
        });
    }

    public void clickedNext(ActionEvent actionEvent) throws IOException {
        //goToView(".fxml");
    }

    public void clickedBack(ActionEvent actionEvent) throws IOException {
        goToView("Experiment_StartView.fxml");
    }

    public String getText(String file) {
        File f = new File(file);
        String line;
        String content="";

        try {
            FileReader fileReader = new FileReader(f);
            BufferedReader buffer = new BufferedReader(fileReader);

            while ((line = buffer.readLine()) != null) {
                content += line + "\n";
            }
            buffer.close();
        } catch (Exception e){
            System.out.println(e);
        }

        return content;
    }


    // --------------------  Moose Scrolling ------------------------------------

    // Incoming Messages from Moose for scrolling
    @Override
    public void incomingMessage(String message) {
        super.incomingMessage(message);
        Message m = new Message(message);

        //check if mode and action are the same
        String mode = getData().getMode().getValue();
        if(mode.equals(m.getActionType())) {

            //----- Normal Drag
            if (m.getActionType().equals("Scroll")){
                if(m.getActionName().equals("deltaY")) {
                    double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                    if(scrollPane.isHover()){ verticalScrollByPx(scrollPane, scrollContent, deltaY);}
                }


            //----- Simple flick
            } else if (m.getActionType().equals("Flick")){
                if(m.getActionName().equals("deltaY")) {
                    double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                    if(scrollPane.isHover()){ verticalScrollByPx(scrollPane, scrollContent, deltaY);}

                }else if(m.getActionName().equals("speed")){
                    double pxPerMs = Double.parseDouble(m.getValue());
                    scrollThread = new Thread(new ScrollThread(1, pxPerMs));
                    scrollThread.start();

                }else if(m.getActionName().equals("stop")){
                    scrollThread.interrupt();
                }


            //----- Circle
            } else if (m.getActionType().equals("Circle3")) {
                if (m.getActionName().equals("deltaAngle")) {
                    double deltaY = Double.parseDouble(m.getValue());
                    if (scrollPane.isHover()) {
                        verticalScrollByPx(scrollPane, scrollContent, deltaY);
                    }
                }


            //----- Rate-Based
            } else if (m.getActionType().equals("TrackPoint")){
                if (m.getActionName().equals("deltaY")) {

                    //stop old thread
                    if (scrollPane.isHover() && scrollThread != null && !scrollThread.isInterrupted()) {
                        scrollThread.interrupt();
                    }

                    double deltaY = Double.parseDouble(m.getValue()); //val between 0 - 1
                    int ms; //ms
                    double px;

                    // 100px => 1px/1ms = '1';  200px => '2' = 2px/1ms; 50px => '0.5' = 1px/2ms; (factorA = 100, factorB = 1)
                    // (factorB 1) 99 = 0.99 = 1/1ms; 10px => '0.1' = 1px/10ms  -> 10px too fast
                    // (factorB 10) 99 = 0.99 = 1/10ms; 10px => '0.1' = 1px/100ms  -> jump from 99px to 100px
                    // -> maybe adapt factorA
                    // todo improve factors
                    int factorA = 100;
                    int factorB = 10;

                    double speedVal =  deltaY/factorA;
                    if(Math.abs(speedVal) >= 1){
                        ms = 1;
                        px = speedVal;

                    }else{
                        if(speedVal >= 0){
                            px = 1;
                        }else{
                            px = -1;
                        }

                        ms = (int) (factorB/Math.abs(speedVal)); //too make it slower /10
                    }
                    System.out.println( px + "/" + ms + " px/ms");
                    scrollThread = new Thread(new ScrollThread(ms, px));
                    scrollThread.start();

                } else if (m.getActionName().equals("stop")) {
                    scrollThread.interrupt();
                }
            }

        }else if(m.getActionType().equals("Action")){
            if (m.getActionName().equals("click")) {
                robot.mousePress(16);
                robot.mouseRelease(16);

            }
        } else {
            System.out.println("Mode and Action type are not same.");
        }

    }

    // Scroll by px
    public void verticalScrollByPx(ScrollPane pane, VBox paneContent, double deltaPx){
        //ScrollPane V / H values are min 0 max 1 -> %
        double change = deltaPx/ paneContent.getHeight();
        double newVal = pane.getVvalue() + change;

        //use math min to not exceed the bounds
        newVal = Math.max(pane.getVmin(), newVal); //not smaller then 0.0
        newVal = Math.min(newVal, pane.getVmax()); //not bigger then max

        pane.setVvalue(newVal);
    }

    // Continuous Scrolling
    public class ScrollThread implements Runnable{
        int time;
        double deltaPx;
        public ScrollThread(int time, double px){
            this.time = time;
            this.deltaPx = px;
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (scrollPane.isHover()) {
                        verticalScrollByPx(scrollPane, scrollContent, deltaPx);
                        Thread.sleep(time); //1 min = 60*1000, 1 sec = 1000
                    }else{
                        scrollThread.interrupt();
                    }
                }
            } catch (InterruptedException e) {
                //we need this because when a sleep the interrupt from outside throws an exception
                Thread.currentThread().interrupt();
            }
        }

    }


}
