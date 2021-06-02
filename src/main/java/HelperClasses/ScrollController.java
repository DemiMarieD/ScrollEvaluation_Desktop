package HelperClasses;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScrollController extends Controller{

    private ComboBox comboBox;

    private VirtualizedScrollPane<InlineCssTextArea> scrollPane;
    private InlineCssTextArea textArea;
    private Thread scrollThread;
    private Robot robot;
    //for flick
   // private double currentSpeed;
   // private double scrollContentHeight;
   // private double lineHeight;


    private Trial trial;

    private final ArrayList<ScrollingMode> modes = new ArrayList<ScrollingMode>(Arrays.asList(
            ScrollingMode.DRAG, ScrollingMode.CIRCLE, ScrollingMode.RUBBING,
            ScrollingMode.FLICK_iOS, ScrollingMode.RATE_BASED));

    private final ArrayList<String> list = new ArrayList<String>(Arrays.asList(
            "Drag", "Circle", "Rubbing", "Flick", "Rate-Based"));

    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        robot = getRobot();
    }

    public void setController(Controller c){
        getCommunicator().changeController(c);
    }

    public void setComboBox(ComboBox comboBox) {
        this.comboBox = comboBox;
        if(getData().getDevice() == Device.MOOSE){
            this.comboBox.setVisible(true);
            if(getData().getMode() != null) {
                String item = list.get(modes.indexOf(getData().getMode()));
                this.comboBox.setValue(item);
            }
            this.comboBox.setItems(FXCollections.observableArrayList(list));
            this.comboBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    getData().setMode(modes.get(newValue.intValue()));
                    String m = modes.get(newValue.intValue()).getValue();
                    System.out.println("Setting mode: " + m);
                    getCommunicator().sendMessage(new HelperClasses.Message("Server", "Mode", m).makeMessage());
                }
            });
        }else{
            this.comboBox.setVisible(false);
        }
    }

    public double getLineHeight() {
        Text t = (Text) textArea.lookup(".text");
        return t.getBoundsInLocal().getHeight();
    }
    public double getScrollContentHeight() {
        int totalNumberOfLines = textArea.getParagraphs().size();
        return totalNumberOfLines * getLineHeight();
        //return scrollContentHeight;
    }
    public int getNumberOfVisibleLines(){
        double numberOfLinesVisible = textArea.getHeight() / getLineHeight();
        return  (int) Math.round(numberOfLinesVisible);
    }

    public VirtualizedScrollPane<InlineCssTextArea> getScrollPane() {
        return scrollPane;
    }

    public void setScrollPane(VirtualizedScrollPane<InlineCssTextArea> scrollPane) {
        this.scrollPane = scrollPane;
    }

    public InlineCssTextArea getTextArea() {
        return textArea;
    }

    public void setTextArea(InlineCssTextArea textArea) {
        this.textArea = textArea;
    }

    public Thread getScrollThread() {
        return scrollThread;
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

    public void setTrial(Trial trial) {
        this.trial = trial;
    }

    // Incoming Messages from Moose for scrolling
    @Override
    public void incomingMessage(String message) {
        super.incomingMessage(message);
        Message m = new Message(message);

        //check if mode and action are the same
        String mode = getData().getMode().getValue();
        if(mode.equals(m.getActionType())) {

            //----- Normal Drag or Rubbing
            switch (m.getActionType()) {
                case "Rubbing":
                case "Drag":
                    if (m.getActionName().equals("deltaY")) {
                        double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                        if (scrollPane.isHover()) {
                            scrollPane.scrollYBy(deltaY);
                        }
                    }
                    break;


                // Slightly adjusted iOS flick
                case "iOS":
                    switch (m.getActionName()) {
                        case "deltaY":
                            double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                            if (scrollPane.isHover()) {
                                scrollPane.scrollYBy(deltaY);
                            }

                            break;
                        case "speed":
                            double pxPerMs = Double.parseDouble(m.getValue());
                            if(scrollThread != null && !scrollThread.isInterrupted()){
                                scrollThread.interrupt();
                            }
                            scrollThread = new Thread(new ExponentialRegression_ScrollThread(pxPerMs), "iOS Thread");
                            scrollThread.start();

                            break;

                        case "stop":
                            scrollThread.interrupt();
                            break;
                    }

                    break;

                //----- Circle
                case "Circle3":
                    if (m.getActionName().equals("deltaAngle")) {
                        double deltaY = Double.parseDouble(m.getValue());
                        if (scrollPane.isHover()) {
                            scrollPane.scrollYBy(deltaY);
                        }
                    }

                    break;

                //----- Rate-Based
                case "TrackPoint":
                    if (m.getActionName().equals("deltaY")) {

                        if (scrollPane.isHover()) {
                            if (scrollThread != null && !scrollThread.isInterrupted()) {
                                scrollThread.interrupt();
                            }

                            double deltaY = Double.parseDouble(m.getValue()); //px
                            scrollThread = new Thread(new Constant_ScrollThread(deltaY), "TrackPointer Thread");
                            scrollThread.start();
                        }

                    } else if (m.getActionName().equals("stop")) {
                        scrollThread.interrupt();
                    }

                    break;

            }

        }else if(m.getActionType().equals("Action")) {
            if (m.getActionName().equals("click")) {
                robot.mousePress(16);
                robot.mouseRelease(16);
            }

        } else if(m.getActionType().equals("Data")){

            if (m.getActionName().equals("save")) {
                //format scrollArea-FingerCount-MinMaxPosition
                // x/y-n-x/y,x/y

                String[] data = m.getValue().split("-");
                //scroll area size
                String[] scrollArea = data[0].split("/");
                trial.setScrollAreaSize_x(scrollArea[0]);
                trial.setScrollAreaSize_y(scrollArea[1]);

                //Finger count
                trial.setFingerCount(Double.parseDouble(data[1]));

                //Min & Max Finger Positions
                String[] positions = data[2].split(",");
                String[] min_val = positions[0].split("/");
                trial.setPosMin_x(min_val[0]);
                trial.setPosMin_y(min_val[1]);
                String[] max_val = positions[1].split("/");
                trial.setPosMax_x(max_val[0]);
                trial.setPosMax_y(max_val[1]);
                //assuming that this is send after finger count
                trial.writeTrial();
            }

        } else {
            System.out.println("Mode and Action type are not same.");
        }

    }


    // Continuous Scrolling
    public class Constant_ScrollThread implements Runnable{

        private final double deltaPx;
        public Constant_ScrollThread(double deltaPx){
            this.deltaPx = deltaPx;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (scrollPane.isHover()) {
                        scrollPane.scrollYBy(deltaPx);
                        Thread.sleep(1); //1 min = 60*1000, 1 sec = 1000
                    }else{
                        //scrollThread.interrupt();
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                //we need this because when a sleep the interrupt from outside throws an exception
                Thread.currentThread().interrupt();
            }
        }

    }


    // Fixed friction, with in exponential regression
    public class ExponentialRegression_ScrollThread implements Runnable{
        double startTime;
        double speed_init;
        boolean end = false;
        public ExponentialRegression_ScrollThread(double speed){
            this.speed_init = speed;
            this.startTime = System.currentTimeMillis() + 500; // + 500ms as for 0.5se the speed should stay same
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (scrollPane.isHover() && !end) {
                        double deltaT_sec = Math.max(System.currentTimeMillis() - startTime, 0) / 1000;
                        // used sec in reference paper
                        // formula for the current rotation speed over time w(t) may be:   w(t) = w0*exp(-c*(t - t0))
                        double friction = Math.exp ( -2.006 * deltaT_sec);
                      //  System.out.println("Friction =" + friction);
                        double deltaPx = Math.abs (speed_init) * friction;

                        double newSpeed = Math.min(Math.abs(speed_init), deltaPx); // so its not faster then the original speed?! - needed because we want 0.5sec constant scroll before decel
                        double scrollVal  = newSpeed * (speed_init / Math.abs(speed_init));  //to set the direction

                        scrollPane.scrollYBy(scrollVal);
                        end = deltaPx < 0.05;
                        if(end){
                            Runnable updater = () -> {
                                Message message = new Message("Server", "Info", "StoppedScroll");
                                getCommunicator().sendMessage(message.makeMessage());
                            };
                            Platform.runLater(updater);
                            Thread.currentThread().interrupt(); //end thread!!
                            break;
                        }else {
                            Thread.sleep(1); //1 min = 60*1000, 1 sec = 1000
                        }
                    }else{
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                //we need this because when a sleep the interrupt from outside throws an exception
                Thread.currentThread().interrupt();
            }
        }

    }

    public double toMM(double px){
        // dpi = pixels/inch
        double dpi = Screen.getPrimary().getDpi();
        // mm  * pixels/inch * inch/mm
        return (px * 25.4) / dpi;
    }

    public double toPx(double mm){
        // dpi = pixels/inch
        double dpi = Screen.getPrimary().getDpi();
        // mm  * pixels/inch * inch/mm
        return (mm * dpi) / 25.4;
    }

}
