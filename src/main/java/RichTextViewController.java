import HelperClasses.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.awt.*;
import java.io.*;
import java.util.Optional;
import java.util.function.IntFunction;

public class RichTextViewController extends Controller {


    @FXML
    private  Pane frame;
    @FXML
    private ScrollPane scrollPane_parent;
    private VirtualizedScrollPane<InlineCssTextArea> scrollPane;

    private InlineCssTextArea textArea;
    private final VBox scrollContent = new VBox();

    // For Moose Scrolling
    private Thread scrollThread;
    private Robot robot;

    private int frameSize_mm = 30; // mm
    private int targetIndex = 41; // starts at 0

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

        setUpScrollPane();

        Platform.runLater(() -> {
            setUpPanesAndTarget();
            scrollPane.scrollYToPixel(0);
        });
    }

    public void setUpScrollPane(){
        textArea = new InlineCssTextArea();

        //** Add line numbers
        IntFunction<Node> numberFactory = LineNumberFactory.get(textArea);

        IntFunction<Node> graphicFactory = line -> {
            HBox hbox = new HBox(
                    numberFactory.apply(line));
            hbox.setAlignment(Pos.CENTER_LEFT);
            return hbox;
        };
        textArea.setParagraphGraphicFactory(graphicFactory);

        textArea.appendText(getText("src/main/resources/files/loremIpsum.txt"));
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPadding(new Insets(0,10,0,0));

        scrollPane = new VirtualizedScrollPane<>(textArea);
        scrollPane_parent.setContent(scrollPane);

        scrollPane_parent.setFitToWidth(true);
        scrollPane_parent.setFitToHeight(true);
    }

    public void setUpPanesAndTarget(){
        //** Split each line of paragraph
        for(int i = 0; i < textArea.getParagraphs().size(); i++){
            if(textArea.getParagraphLinesCount(i) > 1) {
                textArea.moveTo(i, 0);
                int positionsUntilEndOfLine = textArea.getCurrentLineEndInParargraph();
                int endOfLine = textArea.getAbsolutePosition(i, 0) + positionsUntilEndOfLine;
                textArea.insertText(endOfLine, "\n");
                //delete space " " that would otherwise be now at the beginning of the new line
                textArea.deleteText(endOfLine+1, endOfLine+2);
            }
        }


        //** Position Panels
        double mainPaneHeight = getMainPane().getHeight();
        //Center Scroll Pane in Y
        double topMarginScrollPane = (mainPaneHeight - scrollPane_parent.getHeight()) / 2;
        scrollPane_parent.setLayoutY(topMarginScrollPane);
        //Center Frame in Y
        updateFrame();
        frame.setLayoutX(scrollPane_parent.getBoundsInParent().getMinX()-frame.getWidth());

        Platform.runLater(() -> {
            setTarget();
        });
    }

    public void setTarget() {
        //highlight target
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: red;");

        //Set Target indicator
        double centerP = textArea.getAbsolutePosition(targetIndex,textArea.getParagraphLength(targetIndex)/2);
        double relativePos =  centerP / textArea.getLength();
        try {
            Image img = new Image(new FileInputStream("src/main/resources/files/arrow.png"), 50, 0, true, false);
            ImageView imageView = new ImageView(img);
            getMainPane().getChildren().add(imageView);
            //todo still to improve.. not optimal position..
            imageView.setX(scrollPane_parent.getBoundsInParent().getMaxX());
            imageView.setY(scrollPane_parent.getBoundsInParent().getMinY() + (scrollPane_parent.getHeight() * relativePos) - (img.getHeight()/2));

        } catch (FileNotFoundException e) {
            System.out.println("Error loading image");
            e.printStackTrace();
        }

    }

    public void updateFrame(){
       double frameSize_px = toPx(frameSize_mm);
       frame.setPrefHeight(frameSize_px);

       double mainPaneHeight = getMainPane().getHeight();
       double topMarginFrame = (mainPaneHeight - frameSize_px) / 2;
       frame.setLayoutY(topMarginFrame);
    }

    public void print(ActionEvent actionEvent) {

        Optional<Bounds> bounds = textArea.getParagraphBoundsOnScreen(targetIndex); //values fit more P 41 (aka index 42)
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

    public double toPx(int mm){
        // dpi = pixels/inch
        double dpi = Screen.getPrimary().getDpi();
        // mm  * pixels/inch * inch/mm
        return (mm * dpi) / 25.4;
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
                    if(scrollPane.isHover()){ scrollPane.scrollYBy(deltaY);}
                }


            //----- Simple flick
            } else if (m.getActionType().equals("Flick")){
                if(m.getActionName().equals("deltaY")) {
                    double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                    if(scrollPane.isHover()){scrollPane.scrollYBy(deltaY);}

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
                        scrollPane.scrollYBy(deltaY);
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


}
