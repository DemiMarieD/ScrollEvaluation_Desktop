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

public class ScrollController extends Controller{

    private ComboBox comboBox;

    private VirtualizedScrollPane<InlineCssTextArea> scrollPane;
    private InlineCssTextArea textArea;
    private Thread scrollThread;
    private Robot robot;
    //for flick
    private double currentSpeed;
   // private double scrollContentHeight;
   // private double lineHeight;


    private Trial trial;

    private final ArrayList<ScrollingMode> modes = new ArrayList<ScrollingMode>(Arrays.asList(ScrollingMode.WHEEL,
            ScrollingMode.DRAG, ScrollingMode.CIRCLE, ScrollingMode.RUBBING, ScrollingMode.FLICK_deceleration,
            ScrollingMode.FLICK_iOS, ScrollingMode.RATE_BASED));

    private final ArrayList<String> list = new ArrayList<String>(Arrays.asList(
            "Wheel", "Drag", "Circle", "Rubbing", "Flick Decelerate", "iOS - Demi", "Rate-Based"));

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
                            scrollThread = new Thread(new ExponentialRegression_ScrollThread(pxPerMs));
                            scrollThread.start();

                            break;

                        case "stop":
                            scrollThread.interrupt();
                            break;
                    }

                    break;

                //----- Decelerating & Additative flick
                case "DecelFlick":
                    switch (m.getActionName()) {
                        case "deltaY":
                            double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                            if (scrollPane.isHover()) {
                                scrollPane.scrollYBy(deltaY);
                            }

                            break;
                        case "speed":
                            double pxPerMs = Double.parseDouble(m.getValue());
                            scrollThread = new Thread(new Linear_ScrollThread(pxPerMs));
                            scrollThread.start();

                            break;

                        case "addSpeed":
                            double addPx = Double.parseDouble(m.getValue());
                            currentSpeed = currentSpeed + addPx;
                            scrollThread.interrupt();
                            scrollThread = new Thread(new Linear_ScrollThread(currentSpeed));
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

                        //stop old thread
                        if (scrollPane.isHover() && scrollThread != null && !scrollThread.isInterrupted()) {
                            scrollThread.interrupt();
                        }

                        double deltaY = Double.parseDouble(m.getValue()); //px
                        scrollThread = new Thread(new Constant_ScrollThread(1, deltaY));
                        scrollThread.start();

                    } else if (m.getActionName().equals("stop")) {
                        scrollThread.interrupt();
                    }

                    break;

                // SCROLL WHEEL
                case "ScrollWheel":
                    if (m.getActionName().equals("deltaNotches")) {
                        int deltaNotches = Integer.parseInt(m.getValue()); //should be a px value
                        robot.mouseWheel(deltaNotches); //unit of scrolls = "notches of the wheel"
                    }
                    break;
            }

        }else if(m.getActionType().equals("Action")) {
            if (m.getActionName().equals("click")) {
                robot.mousePress(16);
                robot.mouseRelease(16);
            }

        } else if(m.getActionType().equals("Data")){
                if (m.getActionName().equals("fingerCount")) {
                    trial.setFingerCount(Double.parseDouble(m.getValue()));

                }else if(m.getActionName().equals("touchAreaSize")) {
                    trial.setScrollAreaSize(m.getValue());

                }else if (m.getActionName().equals("minMax")) {
                    //make look like: minX/minY,maxX/maxY
                    String[] val = m.getValue().split(",");
                    trial.setPosMin(val[0]);
                    trial.setPosMax(val[1]);
                    //assuming that this is send after finger count
                    trial.writeTrial();
                }
        } else {
            System.out.println("Mode and Action type are not same.");
        }

    }


    // Continuous Scrolling
    public class Constant_ScrollThread implements Runnable{
        int time;
        double deltaPx;
        public Constant_ScrollThread(int time, double px){
            this.time = time;
            this.deltaPx = px;
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (scrollPane.isHover()) {
                        scrollPane.scrollYBy(deltaPx);
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

    // Fixed time, with in that time linear regression
    public class Linear_ScrollThread implements Runnable{
        double maxTime = 2500; // 2 sec
        double startTime;
        double speed_init;
        boolean end = false;
        public Linear_ScrollThread(double speed){
            this.speed_init = speed;
            this.startTime = System.currentTimeMillis()+500; // + 500ms as for 0.5se the speed should stay same
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (scrollPane.isHover() && !end) {
                        double deltaT = Math.max(System.currentTimeMillis() - startTime, 0);
                        double deltaPx = speed_init - speed_init * (deltaT / maxTime);
                        Runnable updater1 = () -> {
                            currentSpeed = deltaPx;
                        };
                        Platform.runLater(updater1);
                        scrollPane.scrollYBy(deltaPx);
                        end = deltaT >= maxTime;
                        if(end){
                            Runnable updater = () -> {
                                Message message = new Message("Server", "Info", "StoppedScroll");
                                getCommunicator().sendMessage(message.makeMessage());
                            };
                            Platform.runLater(updater);
                        }
                       Thread.sleep(1); //1 min = 60*1000, 1 sec = 1000
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

                        //System.out.println("Friction = " + Math.exp ( -2.006 * deltaT_sec));
                        //System.out.println("Init v " + speed_init);

                        //formula for the current rotation speed over time w(t) may be:   w(t) = w0*exp(-c*(t - t0))
                        // mm - sec
                        double deltaPx = Math.abs (speed_init) * Math.exp ( -2.006 * deltaT_sec);
                       // System.out.println(" new px (mm - sec) " + deltaPx);

                        double move = deltaPx * (speed_init / Math.abs(speed_init)); //to set the direction
                        double scrollBy = Math.min(speed_init, move); // so its not faster then the original speed?! - needed because we want 0.5sec constant scroll before decel
                        scrollPane.scrollYBy(scrollBy);
                        Runnable updater1 = () -> {
                            currentSpeed = move;
                        };
                        Platform.runLater(updater1);

                        end = deltaPx < 0.05;
                        if(end){
                            Runnable updater = () -> {
                                Message message = new Message("Server", "Info", "StoppedScroll");
                                getCommunicator().sendMessage(message.makeMessage());
                            };
                            Platform.runLater(updater);
                        }
                        Thread.sleep(1); //1 min = 60*1000, 1 sec = 1000
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


    /*

    public class IPhoneScrollThread implements Runnable{

        double startTime;
        double speed_init;
        boolean end = false;
        // friction  = 750 px/s2
        double friction = 0.00075;
        public IPhoneScrollThread(double speed){
            this.speed_init = speed;
            this.startTime = System.currentTimeMillis()+500; // + 500ms as for 0.5se the speed should stay same
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (scrollPane.isHover() && !end) {
                        double deltaT = Math.max(System.currentTimeMillis() - startTime, 0);
                        //New speed = m * flick speed – (friction * delta-time)
                        double deltaPx = Math.abs (speed_init) - ( friction * deltaT );
                        deltaPx = Math.max(0, deltaPx);

                        double move = deltaPx * (speed_init / Math.abs(speed_init)); //to set the direction
                        scrollPane.scrollYBy(move);
                        Runnable updater1 = () -> {
                            currentSpeed = move;
                        };
                        Platform.runLater(updater1);

                        end = deltaPx == 0;
                        if(end){
                            Runnable updater = () -> {
                                Message message = new Message("Server", "Info", "StoppedScroll");
                                getCommunicator().sendMessage(message.makeMessage());
                            };
                            Platform.runLater(updater);
                        }
                        Thread.sleep(1); //1 min = 60*1000, 1 sec = 1000
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

    public class ExponentialRegressionScrollThread implements Runnable{

        double startTime;
        double speed_init;
        boolean end = false;
        public ExponentialRegressionScrollThread(double speed){
            this.speed_init = speed;
            this.startTime = System.currentTimeMillis(); // + 500ms as for 0.5se the speed should stay same
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (scrollPane.isHover() && !end) {
                        double deltaT = Math.max(System.currentTimeMillis() - startTime, 0);
                        //System.out.println("Friction = " + Math.exp ( -2.006 * (deltaT/1000)));
                        double init_mm = toMM(speed_init);
                        // speed_init == px/ms -> to convert to mm/sec? todo
                       // double init_mm = toMM(speed_init) / 1000; // v in mm/sec -> much worse !

                        //580.31 e^(-2.006x) (mm / sec) -> ms div 1000 -> sec
                        double delta_mm = Math.abs (init_mm) * Math.exp ( -2.006 * (deltaT/1000) );
                        double deltaPx = Math.max(0, toPx(delta_mm));

                        double move = deltaPx * (speed_init / Math.abs(speed_init)); //to set the direction
                        scrollPane.scrollYBy(move);
                        Runnable updater1 = () -> {
                            currentSpeed = move;
                        };
                        Platform.runLater(updater1);

                        end = deltaPx < 0.01;
                        if(end){
                            Runnable updater = () -> {
                                Message message = new Message("Server", "Info", "StoppedScroll");
                                getCommunicator().sendMessage(message.makeMessage());
                            };
                            Platform.runLater(updater);
                        }
                        Thread.sleep(1); //1 min = 60*1000, 1 sec = 1000
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

     */
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
