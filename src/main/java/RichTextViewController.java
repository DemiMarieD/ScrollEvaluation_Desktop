import HelperClasses.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
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
import java.util.*;
import java.util.List;
import java.util.function.IntFunction;

public class RichTextViewController extends ScrollController {
    final int scrollBarWidth = 20; //px
   // final List<Integer> DISTANCES = Arrays.asList(20, 70, 120, 170, 220);
    final List<Integer> DISTANCES = Arrays.asList(50, 100, 200, 300);
    final List<Integer> FRAMESIZES = Arrays.asList(3, 6);
    final int UP = -1;
    final int DOWN = 1;
    @FXML
    private  Button nextBtn;
    @FXML
    private  Label info;
    @FXML
    private  ProgressIndicator loadIndicator;
    @FXML
    private  Label topPaneLabel;

    @FXML
    private ComboBox modeSelector;

    @FXML
    private Pane topPane;
    @FXML
    private Pane indicator;
    @FXML
    private  Pane frame;
    @FXML
    private ScrollPane scrollPane_parent;

    private boolean breakSet;
    private int totalNumberOfLines;
    private int bugMove; //needed because adjustment to start will cause two "scrolls" that should not be counted
    //private double lineHeight;
    private int lastMiddleLine; //needed to scroll to start properly
    private double maxScrollVal; //needed to note when start / end of document is reached

    //For Framing Task
    private List<int[]> currentSet;
    private List<Integer> upDownMix;
    private List<int[]> possibleCombinationsBlockEven;
    private List<int[]> possibleCombinationsBlockOdd;

    private List<int[]> nextPositions;

    private final int index_distance = 0;
    private final int index_direction = 1;
    private final int index_frame = 2;
    private int blockNumber;
    //private int groupNumber;
    private int maxBlocks;
    private boolean finished;
    private int trialInBlock;
    private Trial currentTrial;
    // private long startTime;
    private int randomComIndex;
    private int targetIndex;
    private int targetNumber;
    private int distance; //in number of lines
    private int frameSize; // in number of lines
    private String direction;
    private long lastScrollTime;
    private boolean scrollStarted;
    private boolean targetVisible;
    private boolean targetInFrame;


    private MediaPlayer wrongPlayer;
    private MediaPlayer rightPlayer;

    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);

        setComboBox(modeSelector);

        if(getData().getDevice() == Device.MOOSE) {
            setController(this);
        }

        getMainPane().addEventFilter(KeyEvent.KEY_PRESSED, event->{
            if (event.getCode() == KeyCode.SPACE) {
                if(finished){
                    try {
                        goToView("Experiment_StartView.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                /* }
                    else if(breakSet) {
                    topPane.setVisible(false);
                    breakSet = false;
                    //getScrollPane().requestFocus(); // so on space-bar hit no accidental button press
                    initTrial();
                    */
                }else if(!breakSet) {
                    checkTarget();
                }
            }
        });


        //Media new File(path).toURI().toString()
        wrongPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/wrong.wav").toURI().toString()));
        rightPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/success.wav").toURI().toString()));
        nextBtn.setVisible(false);

        //default values
        currentSet = new ArrayList<>();
        possibleCombinationsBlockOdd = new ArrayList<>();
        possibleCombinationsBlockEven = new ArrayList<>();

        maxBlocks = getData().getNumberOfBlocks();
        finished = false;
        blockNumber = 0;
        targetNumber = 0;
        bugMove = 0;
        breakSet = false;
        maxScrollVal = 11859; //default - to be overwritten

        //set upDownMix
        upDownMix = new ArrayList<Integer>();
        int length = DISTANCES.size() * FRAMESIZES.size();
        for(int i=0; i < length; i++){
            if(i < length/2){
                upDownMix.add(0);
            }else{
                upDownMix.add(1);
            }
        }
        //set possible next Button positions
        nextPositions = new ArrayList<>();
        nextPositions.add(new int[]{100, 516});
        nextPositions.add(new int[]{100, 50});
        nextPositions.add(new int[]{970, 516});
        nextPositions.add(new int[]{970, 50});

        setUpScrollPane();

        getScrollPane().estimatedScrollYProperty().addListener(new ChangeListener<Double>() {
            @Override
            public void changed(ObservableValue<? extends Double> observable, Double oldValue, Double newValue) {
                //on set up it also scrolls so we need to check that currentTrial is already defined
               // System.out.println("...moved " + oldValue + " -> " + newValue);
                bugMove++;
                if(bugMove > 2) {
                    if (currentTrial != null) {
                        if (!scrollStarted) {
                            // System.out.println(" ** Scroll started");
                            currentTrial.setTime_scrollStart(System.currentTimeMillis());
                            scrollStarted = true;
                            if (oldValue > newValue) {
                                currentTrial.setStartDirection("UP");
                            } else {
                                currentTrial.setStartDirection("DOWN");
                            }
                        } else {
                            lastScrollTime = System.currentTimeMillis();
                        }
                    }


                    //check if target visible -> on change increment counter
                    boolean isVisible = isVisible();
                    if (!targetVisible && isVisible) {
                        currentTrial.targetVisible();
                        currentTrial.setTime_lastVisible(System.currentTimeMillis());
                    }
                    targetVisible = isVisible;

                    //check if target in frame -> on change increment counter
                    boolean isInFrame = isInFrame();
                    if (!targetInFrame && isInFrame) {
                        currentTrial.targetInFrame();
                    }
                    targetInFrame = isInFrame;
                }


                if (getData().getDevice() == Device.MOOSE) {
                    //System.out.println("New Val: " + newValue + " max " + maxScrollVal);
                    if (newValue == 0 || newValue == maxScrollVal) {
                        Message message = new Message("Server", "Info", "StoppedScroll");
                        getCommunicator().sendMessage(message.makeMessage());
                        if (getScrollThread() != null) {
                            getScrollThread().interrupt();
                        }
                    }
                }

            }
        });

        Task<Void> scroller = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    setUpPanesAndTarget(); //needs to run on application thread
                });
                return null;
            }
        };
        scroller.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                //Set Layout parameters
                addLineNumbers();
                setMaxScrollVal();
                lastMiddleLine = (int) (getScrollContentHeight()/getLineHeight());
                getTextArea().requestFocus();
                //Set Experiment Task
                setTrial();
            }
        });
        new Thread(scroller).start();

    }

    private boolean isVisible() {
        Optional<Bounds> bounds = getTextArea().getParagraphBoundsOnScreen(targetIndex);
        return bounds.isPresent();
    }

    private boolean isInFrame() {
        Optional<Bounds> bounds = getTextArea().getParagraphBoundsOnScreen(targetIndex);
        if(bounds.isPresent()) {
            double lineY_min = bounds.get().getMinY();
            double lineY_max = bounds.get().getMaxY();
            // System.out.println("Bounds centerY " + lineY_min + " - " + lineY_max);

            double frameY_min = frame.localToScreen(frame.getBoundsInLocal()).getMinY();
            double frameY_max = frame.localToScreen(frame.getBoundsInLocal()).getMaxY();
            // System.out.println("Bounds Frame " + frameY_min + " - " + frameY_max); // Bounds in parent + window to get in screen
           return frameY_min < lineY_min && frameY_max > lineY_max;

        }else{
            return false;
        }
    }

    public void setUpScrollPane(){

        InlineCssTextArea textArea = new InlineCssTextArea();
        textArea.appendText(getText("src/main/resources/files/loremIpsum.txt"));
        textArea.setWrapText(true);
        textArea.setEditable(false);
        //dont allow text selection
        textArea.selectedTextProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!textArea.getSelectedText().isEmpty()) {
                    textArea.deselect();
                }
            }
        });

        //padding to leave room for line numbers
        textArea.setPadding(new Insets(0,0,0,60));
        setTextArea(textArea);

        VirtualizedScrollPane<InlineCssTextArea> scrollPane = new VirtualizedScrollPane<>(textArea);
        setScrollPane(scrollPane);
        scrollPane_parent.setContent(scrollPane);

        scrollPane_parent.setFitToWidth(true);
        scrollPane_parent.setFitToHeight(true);
        scrollPane_parent.getStyleClass().add("scrollArea");

        indicator.setPrefHeight(20);
        indicator.setPrefWidth(scrollBarWidth-2);

    }

    public void setUpPanesAndTarget(){

        InlineCssTextArea textArea = getTextArea();
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
            /*
            //remove empty lines
            else if(textArea.getParagraphLinesCount(i) == 1){
                textArea.moveTo(i, 0);
                int positionsUntilEndOfLine = textArea.getCurrentLineEndInParargraph();
                if(positionsUntilEndOfLine != 0){
                    int endOfLine = textArea.getAbsolutePosition(i, 0) + positionsUntilEndOfLine;
                    textArea.deleteText(endOfLine, endOfLine+1);
                }
            } */

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
        //Text t = (Text) textArea.lookup(".text");
        //lineHeight = t.getBoundsInLocal().getHeight();
        totalNumberOfLines = textArea.getParagraphs().size();
        System.out.println("NL " + totalNumberOfLines);
        double scrollContentHeight = totalNumberOfLines*getLineHeight();
        double indicatorHeight = scrollContentHeight/scrollPane_parent.getHeight();
        indicator.setPrefHeight(indicatorHeight);

    }

    public void setMaxScrollVal(){
        Platform.runLater(() -> {
            getScrollPane().scrollYToPixel(getScrollContentHeight());
            maxScrollVal = getScrollPane().estimatedScrollYProperty().getValue();
        });
    }

    public void addLineNumbers(){
        InlineCssTextArea textArea = getTextArea();
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

    public void setTrial() {
        if(currentSet.size() == 0){
            if(blockNumber < maxBlocks){
               // System.out.println("__________ Block finished! Take a Break _________");

                trialInBlock = 0;
                blockNumber++;

                if(blockNumber%2 != 0) {
                    setParametersForBlock();
                    currentSet = possibleCombinationsBlockOdd;
                }else{
                    currentSet = possibleCombinationsBlockEven;
                }
                //todo always true?
                if(blockNumber > 0) {
                    showTopPane();
                }
            }else{
                //finished
                finished = true;
                showTopPane();
            }

        }

        if(!finished) {
            trialInBlock++;
            targetNumber++;
            info.setText("Trial " + trialInBlock + " / " + (FRAMESIZES.size()*DISTANCES.size()) +
                    "\nIn Block " + blockNumber + " / " + maxBlocks +
                    "\n\nPRESS SPACE BAR TO SELECT");

            Random random = new Random();

            randomComIndex = random.nextInt(currentSet.size());
            int[] parameters = currentSet.get(randomComIndex);
            //currentSet.remove(randomComIndex);

            frameSize = parameters[index_frame];
            updateFrameHeight();

            distance = parameters[index_distance];
            int d = parameters[index_direction];

            if(d == UP) {
                direction = "UP";
                setTarget();
            }else if(d == DOWN) {
                direction = "DOWN";
                setTarget();
            }


             scrollToStart(); //should do the setUp of
            //its still scrolling after -> bug move counter

        }
    }

    public void setParametersForBlock(){
        Collections.shuffle(upDownMix);
        //possibleCombinations = new ArrayList<>();
        possibleCombinationsBlockOdd = new ArrayList<>();
        possibleCombinationsBlockEven = new ArrayList<>();
        int counter = 0;
        for(int d : DISTANCES){
            for(int f : FRAMESIZES){
                int[] combiUp = new int[]{d, UP, f};
                int[] combiDown = new int[]{d, DOWN, f};

                if(upDownMix.get(counter) == 0) {
                    possibleCombinationsBlockOdd.add(combiUp);
                    possibleCombinationsBlockEven.add(combiDown);
                }else{
                    possibleCombinationsBlockOdd.add(combiDown);
                    possibleCombinationsBlockEven.add(combiUp);
                }
                counter++;
            }
        }

    }

    //Updates Target Highlight, Indicator position and Frame size + Colors
    public void setTarget(){

        InlineCssTextArea textArea = getTextArea();

        //** Special case first target
        targetIndex = getRandomValidTargetIndex(frameSize, distance);

        //highlight target
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: #68c1f3;");

        frame.setStyle("-fx-background-color: #68c1f3");
        indicator.setStyle("-fx-background-color: #c8e1ef");

        //** Set Target indicator
        double minY = scrollPane_parent.getBoundsInParent().getMinY();
        double relativeIndex = (double) targetIndex / getTextArea().getParagraphs().size();
        double distanceToTop = getScrollPane().getHeight() * relativeIndex;
        double centerPosition = minY + distanceToTop;
        double yPos = centerPosition - (indicator.getHeight()/2) + 1; //+1px boarder

        double maxY = scrollPane_parent.getBoundsInParent().getMaxY() - indicator.getHeight();

        double validY = Math.max(Math.min(yPos, maxY), minY);
        indicator.setLayoutY(validY);

    }

    public void scrollToStart(){
        int startPoint;

        if (direction.equals("UP")) {
            startPoint = targetIndex + distance;
           // scrollToLine(startPoint);

        } else {
            startPoint = targetIndex - distance;
          //  scrollToLine(startPoint);
        }

      //  System.out.println("Scroll to start " + (startPoint+1) + " (target "+ (targetIndex+1) + ")" + "- D: " + distance + " vs. " + Math.abs(startPoint-targetIndex));

        int absPositionTarget;
        if(lastMiddleLine > startPoint) {
            int firstLineToBeVisible = Math.max( startPoint - (getNumberOfVisibleLines()/2), 0);
            absPositionTarget = getTextArea().getAbsolutePosition(firstLineToBeVisible, 0);
        }else{
            int lastLineToBeVisible = Math.min( startPoint + (getNumberOfVisibleLines()/2), totalNumberOfLines-1);
            absPositionTarget = getTextArea().getAbsolutePosition(lastLineToBeVisible, 0);
        }

        Task<Void> scroller = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                             getTextArea().moveTo(absPositionTarget); //todo figure out when this is finished
                             getTextArea().requestFollowCaret();
                        });

                return null;
            }
        };
        scroller.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                bugMove = 0; //needed because request to scroll will only be received after initTrial, therefore I ignore the first two scrolls with the help of bugMove counter
                initTrial();
            }
        });
        new Thread(scroller).start();

    }

    private void initTrial() {
        targetVisible = isVisible();
        targetInFrame = isInFrame();
       // System.out.println("\n ________Trial START_______");
        currentTrial = new Trial(getData().getParticipantID(), getData().getPart(), getData().getRoundNumber(), targetNumber, trialInBlock, blockNumber, targetIndex, frameSize, distance, direction, getData().getDevice());
        currentTrial.setMode(getData().getMode());
        currentTrial.setVisibleLines(getNumberOfVisibleLines());
        currentTrial.setTextLength(totalNumberOfLines);
        currentTrial.setLineHeight(getLineHeight());
        currentTrial.setPxPerMM(toPx(1));
        currentTrial.setTime_trialStart(System.currentTimeMillis());
        lastScrollTime = -1;
       // System.out.println("Scroll false");
        scrollStarted = false;
    }

    private void showTopPane() {
            topPane.setVisible(true);
            loadIndicator.setVisible(false);

            if (blockNumber == 1) {
                String info = "Thanks for your help! \n\n";
                topPaneLabel.setText(info + "Click 'next' to start.");
                nextBtn.setVisible(true);
                nextBtn.setLayoutX(nextPositions.get(0)[0]);
                nextBtn.setLayoutY(nextPositions.get(0)[1]);
                breakSet = true;

            } else if (!finished) {
                Random random = new Random();
                int i =  random.nextInt(nextPositions.size());
                nextBtn.setLayoutX(nextPositions.get(i)[0]);
                nextBtn.setLayoutY(nextPositions.get(i)[1]);

                String info = "Take a break. " +
                        "\n\nBlock " + (blockNumber-1) + " of " + maxBlocks + " Blocks finished. " +
                        "\n\n";
                topPaneLabel.setText(info);
                topPaneLabel.setText(info + "Click 'next' to continue.");
                breakSet = true;

            } else {
                nextBtn.setVisible(false);
                topPaneLabel.setText(" Finished with this mode!  " +
                        "\n\n  Thanks for participating. ");
            }

    }

    private int getRandomValidTargetIndex(int frameSize, int distance) {
        Random random = new Random();
        InlineCssTextArea textArea = getTextArea();
        totalNumberOfLines = textArea.getParagraphs().size();
      //  System.out.println("Number of lines: " + totalNumberOfLines);

        //!! minus lines that can be reached outside the smallest frame
      //  Text t = (Text) textArea.lookup(".text");
      //  lineHeight = t.getBoundsInLocal().getHeight();
        long visibleLines = Math.round(textArea.getHeight() / getLineHeight());
        int nonReachableLines = (int) (visibleLines - frameSize);
        int boarder = (int) Math.round(nonReachableLines/2.0);

        int usableLines = totalNumberOfLines - nonReachableLines - distance - 1; //-1 just to be save
        boolean valid = false;
        int randIndex = 0;
        while (!valid) {
            randIndex = boarder + random.nextInt(usableLines);
            //if down then start must be positioned above thats why we need +distance space
            if (direction.equals("DOWN")) {
                randIndex += distance;
            }
            String targetText = getTextArea().getText(randIndex);
            //System.out.println("TT: >" +targetText +"<");
            if(!targetText.equals("")){
                valid = true;
            }
        }
        return randIndex;
    }

    public void updateFrameHeight(){
      //  Text t = (Text) getTextArea().lookup(".text");
      //  double lineHeight = t.getBoundsInLocal().getHeight();
        double frameSize_px = frameSize * getLineHeight();

      // double frameSize_px = toPx(frameSize);
       frame.setPrefHeight(frameSize_px);

       double mainPaneHeight = getMainPane().getHeight();
       double topMarginFrame = (mainPaneHeight - frameSize_px) / 2;
       frame.setLayoutY(topMarginFrame);

    }

    public void checkTarget(){
        stopSounds();
        if(getScrollThread() != null && !getScrollThread().isInterrupted()){
            getScrollThread().interrupt();
        }
        currentTrial.setTime_scrollEnd(lastScrollTime);
        currentTrial.setTime_trialEnd(System.currentTimeMillis());

       // int middleIndex = (int) Math.round( Math.round(getTextArea().getHeight()/getLineHeight()) / 2.0 ); //should be 16
       // System.out.println("Middle Index " + lM/2 ); // should be 18
       // int middleLine = getTextArea().visibleParToAllParIndex(middleIndex+1); // index start at 0 -> -1, but list starts with two "non visible items" -> -1+2 = +1

        int visibleLines = (getTextArea().   lastVisibleParToAllParIndex() - getTextArea().firstVisibleParToAllParIndex());
        int middleLine = getTextArea().firstVisibleParToAllParIndex() + (visibleLines/2); //rounds down
       // System.out.println("Middle Line = " + (middleLine+1));
        int deltaLines = targetIndex - middleLine;
        if(visibleLines%2 != 0){
            //we dont have a true middle
                        // for get the closer value to the middle  // add the direction
           int d;
           if(deltaLines == 0){
               d = 1;
           }else{
               d = (deltaLines)/Math.abs(deltaLines);
           }
            deltaLines = (int)  Math.floor(Math.abs(deltaLines - 0.5)) * d ;
        }
        lastMiddleLine = middleLine;
        currentTrial.setDistanceFromMiddle(deltaLines);
       // System.out.println("Delta Lines = " + deltaLines);

        boolean inFrame = Math.abs(deltaLines) < Math.round(frameSize/2.0);
        //isInFrame()
        if(inFrame){
           // System.out.println("-- Hit - dL " + deltaLines);
            rightPlayer.play();
            //remove the combination from the list
            currentSet.remove(randomComIndex);
            currentTrial.setHit(true);
        }else{
          //  System.out.println("-- Miss - dL " + deltaLines);
            trialInBlock--; //reset trial in block, not really necessary
            wrongPlayer.play();
            currentTrial.setHit(false);
        }

        //write all data
        if(getData().getDevice() == Device.MOOSE){
            setTrial(currentTrial);
            //request data from phone
            Message message = new Message("Server", "Info", "RequestData");
            getCommunicator().sendMessage(message.makeMessage());
            //scrollController does the rest (when receiving the answer from the client)

        }else{
            currentTrial.writeTrial();
        }
        //System.out.println("________Trial END_______" + System.currentTimeMillis());
        currentTrial = null;

        //remove old target and set new one
        InlineCssTextArea textArea = getTextArea();
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: transparent;");
        //set new trial
        setTrial();

    }


    public void stopSounds(){
        rightPlayer.stop();
        wrongPlayer.stop();
    }

    public void next(ActionEvent actionEvent){
        topPane.setVisible(false);
        breakSet = false;
        //getScrollPane().requestFocus(); // so on space-bar hit no accidental button press
        initTrial();
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


    @Override
    public void incomingMessage(String message) {
        super.incomingMessage(message);
        Message m = new Message(message);
        if(m.getActionType().equals("Action")){
            if (m.getActionName().equals("click")) {
                try {
                    Robot robot = new Robot();
                    robot.mousePress(16);
                    robot.mouseRelease(16);
                } catch (AWTException e) {
                    e.printStackTrace();
                }

            }
        }

    }

}
