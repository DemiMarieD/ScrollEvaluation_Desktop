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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntFunction;

public class RichTextViewController extends Controller {
    final int scrollBarWidth = 20; //px

    @FXML
    private ComboBox cb;
    @FXML
    private TextField frameInput;
    @FXML
    private TextField distanceInput;
    @FXML
    private Pane topPane;
    @FXML
    private Pane indicator;
    @FXML
    private  Pane frame;
    @FXML
    private ScrollPane scrollPane_parent;
    private VirtualizedScrollPane<InlineCssTextArea> scrollPane;
    private InlineCssTextArea textArea;

    //For Framing Task
    private long startTime;
    private int targetIndex;
    private int targetNumber;
    private int distance; //in number of lines
    private double frameSize; // in number of lines

   // private final ArrayList<Integer> Distances =  new ArrayList<Integer>(Arrays.asList(6, 24, 96, 192));  //in number of lines
   // private final ArrayList<Integer> FrameSizes = new ArrayList<Integer>(Arrays.asList(6, 18)); //in number of lines

    private MediaPlayer wrongPlayer;
    private MediaPlayer rightPlayer;

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
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        //Set mode depending on selection
        if(data.getDevice() == Device.MOOSE) {
            cb.setItems(FXCollections.observableArrayList(list));
            cb.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    data.setMode(modes.get(newValue.intValue()));
                    String m = modes.get(newValue.intValue()).getValue();
                    getCommunicator().sendMessage(new HelperClasses.Message("Server", "Mode", m).makeMessage());
                }
            });
        }else{
            cb.setVisible(false);
        }

        getMainPane().addEventFilter(KeyEvent.KEY_PRESSED, event->{
            if (event.getCode() == KeyCode.SPACE) {
               checkTarget();
            }
        });

        //Media new File(path).toURI().toString()
        wrongPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/wrong.wav").toURI().toString()));
        rightPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/success.wav").toURI().toString()));
        //default values
        targetNumber = 0;
        frameSize = 6;
        distance = 60;

        setUpScrollPane();

        Platform.runLater(() -> {
            setUpPanesAndTarget();
            scrollPane.requestFocus(); // so on space-bar hit no accidental button press
        });
    }

    public void setUpScrollPane(){
        textArea = new InlineCssTextArea();
        textArea.appendText(getText("src/main/resources/files/loremIpsum.txt"));
        textArea.setWrapText(true);
        textArea.setEditable(false);
        //padding to leave room for line numbers
        textArea.setPadding(new Insets(0,0,0,60));

        scrollPane = new VirtualizedScrollPane<>(textArea);
        scrollPane_parent.setContent(scrollPane);

        scrollPane_parent.setFitToWidth(true);
        scrollPane_parent.setFitToHeight(true);
        scrollPane_parent.getStyleClass().add("scrollArea");

        indicator.setPrefHeight(20);
        indicator.setPrefWidth(scrollBarWidth-2);
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
        //Center Frame in Y
        frame.setLayoutX(scrollPane_parent.getBoundsInParent().getMinX()-frame.getWidth());
        //Position Indicator
        indicator.setLayoutX(scrollPane_parent.getBoundsInParent().getMaxX() - scrollBarWidth );
       // indicator.setLayoutY(scrollPane_parent.getBoundsInParent().getMinY() + (scrollPane_parent.getHeight() / 2));
        Text t = (Text) textArea.lookup(".text");
        double lineHeight = t.getBoundsInLocal().getHeight();
        int totalNumberOfLines = textArea.getParagraphs().size();
        double scrollContentHeight = totalNumberOfLines*lineHeight;
        double indicatorHeight = scrollContentHeight/scrollPane_parent.getHeight();
        indicator.setPrefHeight(indicatorHeight);


        Platform.runLater(() -> {
            addLineNumbers();
            setTarget();
            scrollPane.scrollYToPixel(0);
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
        targetNumber++;
        startTime = System.currentTimeMillis();

        // first target random ?!
        if(targetNumber == 1){
            targetIndex = getRandomValidTargetIndex();

            //highlight target
            int l = textArea.getParagraphLength(targetIndex);
            textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: red;");

            //random frame size
           // frameSize = FrameSizes.get(random.nextInt(FrameSizes.size()));
            updateFrameHeight();

            frame.setStyle("-fx-background-color: red");
            indicator.setStyle("-fx-background-color: #efc8c8");

        // second (and all other even trials) UP a random distance
        } else if(targetNumber % 2 == 0) {
           //set NEW distance
           //  distance = Distances.get(random.nextInt(Distances.size()));

            //scroll UP
            targetIndex = targetIndex-distance;

            //highlight target
            int l = textArea.getParagraphLength(targetIndex);
            textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: blue;");

            frame.setStyle("-fx-background-color: blue");
            indicator.setStyle("-fx-background-color: #c0c0f1");


        //third (and all other uneven trials) DOWN the same distance BUT Update framesize
        }else{
            //scroll DOWN
            targetIndex = targetIndex+distance;

            //highlight target
            int l = textArea.getParagraphLength(targetIndex);
            textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: red;");

            //new random frame size
            //frameSize = FrameSizes.get(random.nextInt(FrameSizes.size()));
            updateFrameHeight();

            frame.setStyle("-fx-background-color: red");
            indicator.setStyle("-fx-background-color: #efc8c8");
        }


        //** Set Target indicator
        double minY = scrollPane_parent.getBoundsInParent().getMinY();
        double relativeIndex = (double) targetIndex / textArea.getParagraphs().size();
        double distanceToTop = scrollPane.getHeight() * relativeIndex;
        double centerPosition = minY + distanceToTop;
        double yPos = centerPosition - (indicator.getHeight()/2) + 1; //+1px boarder

        double maxY = scrollPane_parent.getBoundsInParent().getMaxY() - indicator.getHeight();

        double validY = Math.max(Math.min(yPos, maxY), minY);
        indicator.setLayoutY(validY);

    }

    private int getRandomValidTargetIndex() {
        Random random = new Random();
        int totalNumberOfLines = textArea.getParagraphs().size();

        //!! minus lines that can be reached outside the smallest frame
        Text t = (Text) textArea.lookup(".text");
        double lineHeight = t.getBoundsInLocal().getHeight();
        long visibleLines = Math.round(textArea.getHeight() / lineHeight);
        System.out.println("Visible Lines: " + visibleLines);
        //assuming the lists are ordered by size!
        // int minFramesize = FrameSizes.get(0);
        int minFramesize = (int) frameSize;
        int nonReachableLines = (int) (visibleLines - minFramesize);
        //  int maxDistance = Distances.get(Distances.size()-1);
        int maxDistance = (int) distance;
        int min = nonReachableLines/2 + maxDistance;
        int max = totalNumberOfLines - (nonReachableLines/2) - maxDistance;
        targetIndex = min + random.nextInt(max-min);

        return targetIndex;
    }

    public void updateFrameHeight(){
        Text t = (Text) textArea.lookup(".text");
        double lineHeight = t.getBoundsInLocal().getHeight();
        double frameSize_px = frameSize * lineHeight;

      // double frameSize_px = toPx(frameSize);
       frame.setPrefHeight(frameSize_px);

       double mainPaneHeight = getMainPane().getHeight();
       double topMarginFrame = (mainPaneHeight - frameSize_px) / 2;
       frame.setLayoutY(topMarginFrame);

    }

    public void checkTarget(){
        stopSounds();

        long deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("-- Time of Phase "+ targetNumber +" = " + deltaTime);

        Optional<Bounds> bounds = textArea.getParagraphBoundsOnScreen(targetIndex); //values fit more P 41 (aka index 42)
        if(!bounds.isEmpty()) {
            double lineY_min = bounds.get().getMinY();
            double lineY_max = bounds.get().getMaxY();
            // System.out.println("Bounds centerY " + lineY_min + " - " + lineY_max);

            double frameY_min = frame.localToScreen(frame.getBoundsInLocal()).getMinY();
            double frameY_max = frame.localToScreen(frame.getBoundsInLocal()).getMaxY();
            // System.out.println("Bounds Frame " + frameY_min + " - " + frameY_max); // Bounds in parent + window to get in screen

            if(frameY_min < lineY_min && frameY_max > lineY_max){
                rightPlayer.play();

            }else{
                wrongPlayer.play();
            }

        }else{
            wrongPlayer.play();
        }


        //remove old target and set new one
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: transparent;");
        setTarget();
    }

    public void stopSounds(){
        rightPlayer.stop();
        wrongPlayer.stop();
    }

    public void startTrial(ActionEvent actionEvent) {
        targetNumber = 0; //restart phases
        targetIndex = Integer.parseInt(distanceInput.getText());
        frameSize = Integer.parseInt(frameInput.getText());

        System.out.println(" -------- NEW TRIAL -------- ");
        scrollPane.requestFocus(); // so on space-bar hit no accidental button press

        //remove old target and set new one
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: transparent;");
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

    public double toPx(int mm){
        // dpi = pixels/inch
        double dpi = Screen.getPrimary().getDpi();
        // mm  * pixels/inch * inch/mm
        return (mm * dpi) / 25.4;
    }

    // --------------------  Moose Scrolling ------------------------------------

    // Incoming Messages from Moose for scrolling -todo maybe in controller ?
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
