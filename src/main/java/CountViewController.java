import HelperClasses.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntFunction;

public class CountViewController extends Controller {

    @FXML
    private Label numTargetLabel;
    @FXML
    private ComboBox cb;

    @FXML
    private Pane topPane;

    @FXML
    private ScrollPane scrollPane_parent;
    private VirtualizedScrollPane<InlineCssTextArea> scrollPane;
    private InlineCssTextArea textArea;

    //For Count Task
    private int numberOfTargets;

    // For Moose Scrolling
    private Thread scrollThread;
    private Robot robot;

    private final ArrayList<ScrollingMode> modes = new ArrayList<ScrollingMode>(Arrays.asList(ScrollingMode.DRAG, ScrollingMode.FLICK,
            ScrollingMode.RATE_BASED, ScrollingMode.CIRCLE, ScrollingMode.RUBBING, null,
            ScrollingMode.WHEEL, ScrollingMode.DRAG_2, ScrollingMode.THUMB));

    private final ArrayList<String> list = new ArrayList<String>(Arrays.asList(
            "Drag", "Flick", "Rate-Based", "Circle", "Rubbing", "---------------", "Wheel", "Drag 2", "Thumb"));

    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);
        if(getData().getDevice() == Device.MOOSE){
            getCommunicator().changeController(this); //to receive Messages
            String item = list.get(modes.indexOf(data.getMode()));
            cb.setValue(item);
        }

        // Robot is used for click actions with Moose
        robot = getRobot();

        //Set mode depending on selection
        if(data.getDevice() == Device.MOOSE) {
            cb.setItems(FXCollections.observableArrayList(list));
            cb.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    data.setMode(modes.get(newValue.intValue()));
                    String m = modes.get(newValue.intValue()).getValue();
                    getCommunicator().sendMessage(new Message("Server", "Mode", m).makeMessage());
                }
            });
        }else{
            cb.setVisible(false);
        }

        setUpScrollPane();

        Platform.runLater(() -> {
            setUpPanesAndTarget();
        });
    }

    public void setUpScrollPane(){
        textArea = new InlineCssTextArea();
        textArea.appendText(getText("src/main/resources/files/loremIpsum_short.txt"));
        textArea.setWrapText(true);
        textArea.setEditable(false);
        //padding to leave room for line numbers
        textArea.setPadding(new Insets(0,0,0,60));

        scrollPane = new VirtualizedScrollPane<>(textArea);
        scrollPane_parent.setContent(scrollPane);

        scrollPane_parent.setFitToWidth(true);
        scrollPane_parent.setFitToHeight(true);
        scrollPane_parent.getStyleClass().add("scrollArea");

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

           //remove empty lines
            }else if(textArea.getParagraphLinesCount(i) == 1){
                textArea.moveTo(i, 0);
                int positionsUntilEndOfLine = textArea.getCurrentLineEndInParargraph();
                if(positionsUntilEndOfLine != 0){
                    int endOfLine = textArea.getAbsolutePosition(i, 0) + positionsUntilEndOfLine;
                    textArea.deleteText(endOfLine, endOfLine+1);
                }
            }

        }


        //** Position Panels
        double mainPaneHeight = getMainPane().getHeight();
        //Center Scroll Pane in Y
        double topMarginScrollPane = (mainPaneHeight - scrollPane_parent.getHeight()) / 2;
        scrollPane_parent.setLayoutY(topMarginScrollPane);



        Platform.runLater(() -> {
            addLineNumbers();
            setTarget();

            topPane.setVisible(false);
        });
    }

    public void addLineNumbers(){
        //** Add line numbers
        IntFunction<Node> numberFactory = LineNumberFactory.get(textArea);
        IntFunction<Node> graphicFactory = line -> {
            HBox hbox = new HBox(
                    numberFactory.apply(line));
            hbox.setAlignment(Pos.CENTER_LEFT);
            return hbox;
        };
        textArea.setParagraphGraphicFactory(graphicFactory);
        //remove padding
        textArea.setPadding(new Insets(0,0,0,0));
    }


    //Updates Target Highlight, Indicator position and Frame size + Colors
    public void setTarget() {
        //** Set Target
        Random random = new Random();
        int totalNumberOfLines = textArea.getParagraphs().size();
        scrollPane.scrollYToPixel(0);

        // random number of targets
        numberOfTargets = random.nextInt(20);

        //position targets random
        int wordLength = 10;
        for(int i=0; i <numberOfTargets; i++) {
            int startPoint =  random.nextInt(textArea.getLength());
            textArea.setStyle(startPoint, startPoint+wordLength,  "-fx-fill: blue; -fx-underline: true;");

            //todo double check no overlap
        }

    }

    public void print(ActionEvent actionEvent) {
        System.out.println("Number of Targets were : " + numberOfTargets);
        numTargetLabel.setText("Number of Targets were: \n" + numberOfTargets);
        setTarget();
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

            //----- Normal Drag or Rubbing
            if (m.getActionType().equals("Scroll") || m.getActionType().equals("Rubbing") ||  m.getActionType().equals("Drag")) {
                if (m.getActionName().equals("deltaY")) {
                    double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                    if (scrollPane.isHover()) {
                        scrollPane.scrollYBy(deltaY);
                    }
                }

            // Dragging the thumb
            } else if (m.getActionType().equals("Thumb")) {
                if (m.getActionName().equals("deltaY")) {
                    double deltaY_Thumb = Double.parseDouble(m.getValue()); //should be a px value

                    Text t = (Text) textArea.lookup(".text");
                    double lineHeight = t.getBoundsInLocal().getHeight();
                    int totalNumberOfLines = textArea.getParagraphs().size();
                    double scrollContentHeight = totalNumberOfLines*lineHeight;

                    // System.out.println(" Unit de/increment " + scrollBar.getUnitIncrement() ); // == 0 ..
                    // System.out.println(" Block de/increment " + scrollBar.getBlockIncrement() );
                    // Paging = Block Increment/Decrement

                    ScrollBar scrollBar = (ScrollBar) scrollPane.lookup(".scroll-bar:vertical");
                    double visibleAmount = scrollBar.getVisibleAmount(); //size of page px
                    //System.out.println("Visible Amount " + visibleAmount);
                    //System.out.println("Height of Pane " + scrollPane.getHeight());

                    double deltaY =  (deltaY_Thumb/visibleAmount) * scrollContentHeight; //scrollPane_parent.getHeight()

                    if (scrollPane.isHover()) {
                        scrollPane.scrollYBy(deltaY);
                    }
                }

            //----- Simple flick
            } else if (m.getActionType().equals("Flick")) {
                if (m.getActionName().equals("deltaY")) {
                    double deltaY = Double.parseDouble(m.getValue()); //should be a px value
                    if (scrollPane.isHover()) {
                        scrollPane.scrollYBy(deltaY);
                    }

                } else if (m.getActionName().equals("speed")) {
                    double pxPerMs = Double.parseDouble(m.getValue());
                    scrollThread = new Thread(new ScrollThread(1, pxPerMs));
                    scrollThread.start();

                } else if (m.getActionName().equals("stop")) {
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
            } else if (m.getActionType().equals("TrackPoint")) {
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

                    double speedVal = deltaY / factorA;
                    if (Math.abs(speedVal) >= 1) {
                        ms = 1;
                        px = speedVal;

                    } else {
                        if (speedVal >= 0) {
                            px = 1;
                        } else {
                            px = -1;
                        }

                        ms = (int) (factorB / Math.abs(speedVal)); //too make it slower /10
                    }
                    System.out.println(px + "/" + ms + " px/ms");
                    scrollThread = new Thread(new ScrollThread(ms, px));
                    scrollThread.start();

                } else if (m.getActionName().equals("stop")) {
                    scrollThread.interrupt();
                }


                // SCROLL WHEEL
            }  else if (m.getActionType().equals("ScrollWheel")) {
                if (m.getActionName().equals("deltaNotches")) {
                    int deltaNotches = Integer.parseInt(m.getValue()); //should be a px value
                    robot.mouseWheel(deltaNotches); //unit of scrolls = "notches of the wheel"
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
