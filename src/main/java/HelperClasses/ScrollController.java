package HelperClasses;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
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
    private double scrollContentHeight;

    private boolean maxSpeedSet;
    private final double MAX_SPEED = 2.1; // px/ms

    private final ArrayList<ScrollingMode> modes = new ArrayList<ScrollingMode>(Arrays.asList(ScrollingMode.DRAG,
            ScrollingMode.DRAG_acceleration, ScrollingMode.FLICK, ScrollingMode.FLICK_multi, ScrollingMode.FLICK_deceleration,
            ScrollingMode.FLICK_iphone, ScrollingMode.FLICK_iOS,
            ScrollingMode.RATE_BASED, ScrollingMode.CIRCLE, ScrollingMode.RUBBING,
            ScrollingMode.WHEEL, ScrollingMode.THUMB));

    private final ArrayList<String> list = new ArrayList<String>(Arrays.asList(
            "Drag", "Drag + Accel.", "Flick",  "Multi Flick", "Flick Decelerate", "IPhone Flick", "iOS", "Rate-Based", "Circle", "Rubbing",  "Wheel", "Thumb"));

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
            if(getData().getMode() != null) {
                String item = list.get(modes.indexOf(getData().getMode()));
                comboBox.setValue(item);
            }
            comboBox.setItems(FXCollections.observableArrayList(list));
            comboBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    getData().setMode(modes.get(newValue.intValue()));
                    String m = modes.get(newValue.intValue()).getValue();
                    System.out.println("Setting mode: " + m);
                    getCommunicator().sendMessage(new HelperClasses.Message("Server", "Mode", m).makeMessage());
                }
            });
        }else{
            comboBox.setVisible(false);
        }
    }

    public double getScrollContentHeight() {
        return scrollContentHeight;
    }

    public void setScrollContentHeight() {
        Text t = (Text) textArea.lookup(".text");
        double lineHeight = t.getBoundsInLocal().getHeight();
        int totalNumberOfLines = textArea.getParagraphs().size();
        scrollContentHeight = totalNumberOfLines * lineHeight;
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

    public void setScrollThread(Thread scrollThread) {
        this.scrollThread = scrollThread;
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

    public boolean getMaxSpeedSet() {
        return maxSpeedSet;
    }

    public void setMaxSpeedSet(boolean maxSpeedSet) {
        this.maxSpeedSet = maxSpeedSet;
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
                case "Scroll":
                case "Rubbing":
                case "DragAcceleration":
                case "Drag":
                    if (m.getActionName().equals("deltaY")) {
                        double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                        if (scrollPane.isHover()) {
                            scrollPane.scrollYBy(deltaY);
                        }
                    }


                    break;

                // Dragging the thumb
                case "Thumb":
                    if (m.getActionName().equals("deltaY")) {
                        double deltaY_Thumb = Double.parseDouble(m.getValue()); //should be a px value



                        // System.out.println(" Unit de/increment " + scrollBar.getUnitIncrement() ); // == 0 ..
                        // System.out.println(" Block de/increment " + scrollBar.getBlockIncrement() );
                        // Paging = Block Increment/Decrement

                        ScrollBar scrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
                        double visibleAmount = scrollBar.getVisibleAmount(); //size of page px
                        //System.out.println("Visible Amount " + visibleAmount);
                        //System.out.println("Height of Pane " + scrollPane.getHeight());

                        double deltaY = (deltaY_Thumb / visibleAmount) * scrollContentHeight; //scrollPane_parent.getHeight()

                        if (scrollPane.isHover()) {
                            scrollPane.scrollYBy(deltaY);
                        }
                    }

                    break;

                //----- Simple flick
                case "Flick":
                    switch (m.getActionName()) {
                        case "deltaY":
                            double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                            if (scrollPane.isHover()) {
                                scrollPane.scrollYBy(deltaY);
                            }

                            break;
                        case "speed":
                            double pxPerMs = Double.parseDouble(m.getValue());
                            if(maxSpeedSet){
                                pxPerMs = Math.min(MAX_SPEED, pxPerMs);
                            }
                            if(!scrollThread.isInterrupted()){
                                scrollThread.interrupt();
                            }
                            scrollThread = new Thread(new ScrollThread(1, pxPerMs));
                            scrollThread.start();

                            break;
                        case "stop":
                            scrollThread.interrupt();
                            break;
                    }



                    break;

                //----- Decelerating & nonAdditive flick
                case "IPhoneFlick":
                    switch (m.getActionName()) {
                        case "deltaY":
                            double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                            if (scrollPane.isHover()) {
                                scrollPane.scrollYBy(deltaY);
                            }

                            break;
                        case "speed":
                            double pxPerMs = Double.parseDouble(m.getValue());
                            if(maxSpeedSet){
                                pxPerMs = Math.min(MAX_SPEED, pxPerMs);
                            }
                            if(scrollThread != null && !scrollThread.isInterrupted()){
                                scrollThread.interrupt();
                            }
                            scrollThread = new Thread(new IPhoneScrollThread(pxPerMs));
                            scrollThread.start();

                            break;

                        case "stop":
                            scrollThread.interrupt();
                            break;
                    }

                    break;

                // ....
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
                            if(maxSpeedSet){
                                pxPerMs = Math.min(MAX_SPEED, pxPerMs);
                            }
                            if(scrollThread != null && !scrollThread.isInterrupted()){
                                scrollThread.interrupt();
                            }
                            scrollThread = new Thread(new ExponentialRegressionScrollThread(pxPerMs));
                            scrollThread.start();

                            break;

                        case "addSpeed":
                            double addPx = Double.parseDouble(m.getValue());
                            currentSpeed = currentSpeed + addPx;
                            if(maxSpeedSet){
                                currentSpeed = Math.min(MAX_SPEED, currentSpeed);
                            }
                            scrollThread.interrupt();
                            scrollThread = new Thread(new ExponentialRegressionScrollThread(currentSpeed));
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
                            if(maxSpeedSet){
                                pxPerMs = Math.min(MAX_SPEED, pxPerMs);
                            }
                            scrollThread = new Thread(new DecScrollThread(pxPerMs));
                            scrollThread.start();

                            break;

                        case "addSpeed":
                            double addPx = Double.parseDouble(m.getValue());
                            currentSpeed = currentSpeed + addPx;
                            if(maxSpeedSet){
                                currentSpeed = Math.min(MAX_SPEED, currentSpeed);
                            }
                            scrollThread.interrupt();
                            scrollThread = new Thread(new DecScrollThread(currentSpeed));
                            scrollThread.start();

                            break;
                        case "stop":
                            scrollThread.interrupt();
                            break;
                    }

                    break;



                //----- Multi flick - additive
                case "MultiFlick":
                    switch (m.getActionName()) {
                        case "deltaY":
                            double deltaY = Double.parseDouble(m.getValue()); //should be a px value

                            if (scrollPane.isHover()) {
                                scrollPane.scrollYBy(deltaY);
                            }

                            break;
                        case "speed":
                            double pxPerMs = Double.parseDouble(m.getValue());
                            if(maxSpeedSet){
                                pxPerMs = Math.min(MAX_SPEED, pxPerMs);
                            }
                            currentSpeed = pxPerMs;
                            scrollThread = new Thread(new ScrollThread(1, pxPerMs));
                            scrollThread.start();

                            break;

                        case "addSpeed":
                            double addPx = Double.parseDouble(m.getValue());
                            currentSpeed = currentSpeed + addPx;
                            if(maxSpeedSet){
                                currentSpeed = Math.min(MAX_SPEED, currentSpeed);
                            }
                            scrollThread.interrupt();
                            scrollThread = new Thread(new ScrollThread(1, currentSpeed));
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
                        if(maxSpeedSet){
                            deltaY = Math.min(MAX_SPEED, deltaY);
                        }
                        scrollThread = new Thread(new ScrollThread(1, deltaY));
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

        }else if(m.getActionType().equals("Action")){
            if (m.getActionName().equals("click")) {
                robot.mousePress(16);
                robot.mouseRelease(16);

            }
        } else {
            System.out.println("Mode and Action type are not same.");
        }

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

    public class DecScrollThread implements Runnable{
        double maxTime = 2500; // 2 sec
        double startTime;
        double speed_init;
        boolean end = false;
        public DecScrollThread(double speed){
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
                        //New speed = m * flick speed – (friction * delta-time)
                        //580.31 e^(-2.006x) (mm / sec) -> ms div 1000 -> sec
                        //System.out.println("Friction = " + Math.exp ( -2.006 * (deltaT/1000)));
                        double deltaPx = Math.abs (speed_init) * Math.exp ( -2.006 * (deltaT/1000) );
                        deltaPx = Math.max(0, deltaPx);

                        double move = deltaPx * (speed_init / Math.abs(speed_init)); //to set the direction
                        scrollPane.scrollYBy(move);
                        Runnable updater1 = () -> {
                            currentSpeed = move;
                        };
                        Platform.runLater(updater1);

                        end = deltaPx < 0.0001;
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

}
