import HelperClasses.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.function.IntFunction;

public class RichTextViewController extends Controller {

    @FXML
    private  Pane frame;
    @FXML
    private ScrollPane scrollPane;

    private InlineCssTextArea textArea;
    private final VBox scrollContent = new VBox();

    // For Moose Scrolling
    private Thread scrollThread;
    private Robot robot;

    private int frameSize = 50; // px
    private int targetP = 40;

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

       // setTextPanel(scrollPane, scrollContent);


        textArea = new InlineCssTextArea();

        IntFunction<Node> numberFactory = LineNumberFactory.get(textArea);
        IntFunction<Node> graphicFactory = line -> {
            VBox vbox = new VBox(
                    numberFactory.apply(line));
            vbox.setAlignment(Pos.CENTER_LEFT);
            return vbox;
        };

        textArea.setParagraphGraphicFactory(graphicFactory);
        textArea.appendText(getText("src/main/resources/files/dogstory.txt"));
        textArea.setWrapText(true);
        textArea.setPadding(new Insets(0,10,0,0));
        //paragraph = "line number"+1 (bc starts at 0)

        int l = textArea.getParagraphLength(targetP);
        textArea.setStyle(targetP, 0, l, "-rtfx-background-color: red;");

        scrollPane.setContent(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

    }

    @Override
    public void onLoad() {
        //todo set up frame size/position 
        scrollPane.setVvalue(0);
        System.out.println(scrollPane.getBoundsInParent().getCenterY());
    }

    public void print(ActionEvent actionEvent) {
        System.out.println("___________________________");
        double centerY = scrollPane.getBoundsInParent().getCenterY();
        System.out.println("Center Y " + centerY);

        double nCenterY = frame.getBoundsInParent().getCenterY();
        System.out.println("Center Y " + nCenterY);

        System.out.println("------------------------");

        System.out.println("(logical) Lines of Text " + textArea.getParagraphs().size());
        System.out.println("Length of Text " + textArea.getLength());
        System.out.println("Length of P: " + textArea.getParagraphLength(targetP));
        System.out.println("Lines of P: " + textArea.getParagraphLinesCount(targetP));

        System.out.println("------------------------");

        Optional<Bounds> bounds = textArea.getParagraphBoundsOnScreen(targetP); //values fit more P 41 (aka index 42)
        if(!bounds.isEmpty()) {
            double lineY_min = bounds.get().getMinY();
            double lineY_max = bounds.get().getMaxY();
          // System.out.println("Bounds centerY " + lineY_min + " - " + lineY_max);

            double frameY_min = frame.localToScreen(frame.getBoundsInLocal()).getMinY();
            double frameY_max = frame.localToScreen(frame.getBoundsInLocal()).getMaxY();
          // System.out.println("Bounds Frame " + frameY_min + " - " + frameY_max); // Bounds in parent + window to get in screen

            if(frameY_min < lineY_min && frameY_max > lineY_max){
                System.out.println("IN FRAME !");
            }
        }
        //System.out.println("Bounds*2 " + textArea.getVisibleParagraphBoundsOnScreen(40)); //index should be 0-number lines visible?!
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
