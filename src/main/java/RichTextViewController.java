import HelperClasses.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

import java.io.*;
import java.util.*;
import java.util.function.IntFunction;

public class RichTextViewController extends ScrollController {
    final int scrollBarWidth = 20; //px
    final List<Integer> DISTANCES = Arrays.asList(20, 60, 100, 140, 180, 220);
    final List<Integer> FRAMESIZES = Arrays.asList(6);
    final int UP = -1;
    final int DOWN = 1;

    @FXML
    private RadioButton maxSpeedButton;
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

    //For Framing Task
    private List<int[]> possibleCombinations;
    private final int index_distance = 0;
    private final int index_direction = 1;
    private final int index_frame = 2;
    private int blockNumber;
    private int maxBlocks;
    private boolean finished;
    private int trialInBlock;
    private Trial currentTrial;
    // private long startTime;
    private int targetIndex;
    private int targetNumber;
    private int distance; //in number of lines
    private int frameSize; // in number of lines
    private String direction;
    private boolean scrollStarted;
    private boolean targetVisible;
    private boolean targetInFrame;


    private double maxScrollVal;

    private MediaPlayer wrongPlayer;
    private MediaPlayer rightPlayer;

    @Override
    public void initData(Communicator communicator, Data data) {
        super.initData(communicator, data);

        setComboBox(cb);

        if(getData().getDevice() == Device.MOOSE) {
            setController(this);
        }

        getMainPane().addEventFilter(KeyEvent.KEY_PRESSED, event->{
            if (event.getCode() == KeyCode.SPACE) {
               checkTarget();
            }
        });



        maxSpeedButton.setSelected(false);
        setMaxSpeedSet(maxSpeedButton.isSelected());
        maxSpeedButton.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean old, Boolean newer) {
                setMaxSpeedSet(maxSpeedButton.isSelected());
            }
        });

        //Media new File(path).toURI().toString()
        wrongPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/wrong.wav").toURI().toString()));
        rightPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/success.wav").toURI().toString()));
        //default values
        possibleCombinations = new ArrayList<>();
        maxBlocks = getData().getNumberOfBlocks(); // todo get from data?
        finished = false;
        blockNumber = 0;
        targetNumber = 0;

        setUpScrollPane();

        maxScrollVal = 11859; //default - to be overwritten

        getScrollPane().estimatedScrollYProperty().addListener(new ChangeListener<Double>() {
            @Override
            public void changed(ObservableValue<? extends Double> observable, Double oldValue, Double newValue) {
               //on set up it also scrolls so we need to check that currentTrial is already defined
                if(currentTrial != null) {
                    if (!scrollStarted) {
                        //TODO BUG if not scrolling still time is taken!!
                        currentTrial.setTime_scrollStart(System.currentTimeMillis());
                        scrollStarted = true;
                    } else {
                        currentTrial.setTime_scrollEnd( System.currentTimeMillis());
                    }
                }

                //check if target visible -> on change increment counter
                boolean isVisible = isVisible();
                if( !targetVisible && isVisible){
                    currentTrial.targetVisible();
                    currentTrial.setTime_lastVisible(System.currentTimeMillis());
                }
                targetVisible = isVisible;

                //check if target in frame -> on change increment counter
                boolean isInFrame = isInFrame();
                if( !targetInFrame && isInFrame){
                    currentTrial.targetInFrame();
                }
                targetInFrame = isInFrame;



                if(getData().getDevice() == Device.MOOSE) {
                    //System.out.println("New Val: " + newValue + " max " + maxScrollVal);
                    if (newValue == 0 || newValue == maxScrollVal) {
                        Message message = new Message("Server", "Info", "StoppedScroll");
                        getCommunicator().sendMessage(message.makeMessage());
                        if(getScrollThread() != null) {
                            getScrollThread().interrupt();
                        }
                    }
                }
            }
        });

        Platform.runLater(() -> {
            setUpPanesAndTarget();
            //will scroll to start
            setTrial();
            getScrollPane().requestFocus(); // so on space-bar hit no accidental button press
        });
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
            //needed to recognize when bottom is reached
            setMaxScrollVal();
            topPane.setVisible(false);
        });
    }

    public void setMaxScrollVal(){
        setScrollContentHeight();
        Platform.runLater(() -> {
            getScrollPane().scrollYToPixel(getScrollContentHeight());
            maxScrollVal = getScrollPane().estimatedScrollYProperty().getValue();
            scrollToStart();
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
        if(possibleCombinations.size() == 0){
            if(blockNumber < maxBlocks){
                //ToDO BREAK
                System.out.println("__________ Block finished! Take a Break _________");

                blockNumber++;
                setParametersForBlock();
            }else{
                //ToDO END
                System.out.println("__________ END _________");

                //finished
                finished = true;
            }

        }

        if(!finished) {
            trialInBlock++;
            targetNumber++;
            Random random = new Random();

            int randomIndex = random.nextInt(possibleCombinations.size());
            int[] parameters = possibleCombinations.get(randomIndex);
            possibleCombinations.remove(randomIndex);

            frameSize = parameters[index_frame];
            updateFrameHeight();

            distance = parameters[index_distance];
            int d = parameters[index_direction];

            if(d == UP) {
                direction = "UP";
                setTarget("UP");
            }else if(d == DOWN) {
                direction = "DOWN";
                setTarget("DOWN");
            }

            scrollToStart();

            currentTrial = new Trial(getData().getParticipantID(), targetNumber, trialInBlock, blockNumber, targetIndex, frameSize, distance, direction, getData().getDevice());
            currentTrial.setMode(getData().getMode());
            currentTrial.setLineHeight(getLineHeight());

            Platform.runLater(() -> {
                currentTrial.setTime_trialStart(System.currentTimeMillis());
                targetVisible = isVisible();
                targetInFrame = isInFrame();
                scrollStarted = false;
            });
        }
    }

    public void setParametersForBlock(){
        trialInBlock = 0;
        possibleCombinations = new ArrayList<>();
        for(int d : DISTANCES){
            for(int f : FRAMESIZES){
                int[] combiUp = new int[]{d, UP, f};
                possibleCombinations.add(combiUp);
                int[] combiDown = new int[]{d, DOWN, f};
                possibleCombinations.add(combiDown);
            }
        }
    }

    //Updates Target Highlight, Indicator position and Frame size + Colors
    public void setTarget(String direction){

        InlineCssTextArea textArea = getTextArea();

        //** Special case first target
        targetIndex = getRandomValidTargetIndex(frameSize, distance);

        //highlight target
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: red;");

        frame.setStyle("-fx-background-color: red");
        indicator.setStyle("-fx-background-color: #efc8c8");

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
        if (direction.equals("UP")) {
            int startPoint = targetIndex + distance;
            scrollToLine(startPoint);

        } else {
            int startPoint = targetIndex - distance;
            scrollToLine(startPoint);
        }

    }

    private int getRandomValidTargetIndex(int frameSize, int distance) {
        Random random = new Random();
        InlineCssTextArea textArea = getTextArea();
        int totalNumberOfLines = textArea.getParagraphs().size();
        System.out.println("Number of lines: " + totalNumberOfLines);

        //!! minus lines that can be reached outside the smallest frame
        Text t = (Text) textArea.lookup(".text");
        double lineHeight = t.getBoundsInLocal().getHeight();
        long visibleLines = Math.round(textArea.getHeight() / lineHeight);
        int nonReachableLines = (int) (visibleLines - frameSize);
        int boarder = nonReachableLines/2;

        int usableLines = totalNumberOfLines - nonReachableLines - distance;
        int randIndex = boarder + random.nextInt(usableLines);
        //if down then start must be positioned above thats why we need +distance space
        if(direction.equals("DOWN")){
            randIndex += distance;
        }

        return randIndex;

    }

    public void updateFrameHeight(){
        Text t = (Text) getTextArea().lookup(".text");
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
        currentTrial.setTime_trialEnd(System.currentTimeMillis());

        if(isInFrame()){
            rightPlayer.play();
            currentTrial.setHit(true);
        }else{
            wrongPlayer.play();
            currentTrial.setHit(false);
            //todo get delta lines that the target was missed by!
           // scrollToLine(targetIndex); -> setTarget will scroll to new start position
        }

        //write all data
        currentTrial.writeTrial();

        //remove old target and set new one
        InlineCssTextArea textArea = getTextArea();
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: transparent;");
        setTrial();
    }

    public void scrollToLine(int line){
        //Centering (old) target in screen so the scrolling distance is constant !
        int firstLineToBeVisible = line - (getNumberOfVisibleLines()/2);
        int absPositionTarget;
        Optional<Bounds> bounds = getTextArea().getParagraphBoundsOnScreen(firstLineToBeVisible);
        //if first line is already visible -> target is too far south -> scroll to last line
        if(bounds.isEmpty()) {
            absPositionTarget = getTextArea().getAbsolutePosition(firstLineToBeVisible, 0);
        }else{
            int lastLineToBeVisible = line + (getNumberOfVisibleLines()/2);
            absPositionTarget = getTextArea().getAbsolutePosition(lastLineToBeVisible, 0);
        }
        getTextArea().moveTo(absPositionTarget);
        getTextArea().requestFollowCaret();
    }

    public void stopSounds(){
        rightPlayer.stop();
        wrongPlayer.stop();
    }

    public void startTrial(ActionEvent actionEvent) {
        targetNumber = 0; //restart phases
        distance = Integer.parseInt(distanceInput.getText());
        frameSize = Integer.parseInt(frameInput.getText());

        System.out.println(" -------- NEW TRIAL -------- ");
        getScrollPane().requestFocus(); // so on space-bar hit no accidental button press

        //remove old target and set new one
        InlineCssTextArea textArea = getTextArea();
        int l = textArea.getParagraphLength(targetIndex);
        textArea.setStyle(targetIndex, 0, l, "-rtfx-background-color: transparent;");
        setTrial();
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


}
