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
import java.util.Optional;
import java.util.Random;
import java.util.function.IntFunction;

public class RichTextViewController extends ScrollController {
    final int scrollBarWidth = 20; //px

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
    private long startTime;
    private int targetIndex;
    private int targetNumber;
    private int distance; //in number of lines
    private double frameSize; // in number of lines
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
        targetNumber = 0;
        frameSize = 6;
        distance = 60;

        setUpScrollPane();

        maxScrollVal = 11859; //default - to be overwritten
        if(getData().getDevice() == Device.MOOSE) {
            getScrollPane().estimatedScrollYProperty().addListener(new ChangeListener<Double>() {
                @Override
                public void changed(ObservableValue<? extends Double> observable, Double oldValue, Double newValue) {
                    //System.out.println("New Val: " + newValue + " max " + maxScrollVal);
                    if (newValue == 0 || newValue == maxScrollVal) {
                        Message message = new Message("Server", "Info", "StoppedScroll");
                        getCommunicator().sendMessage(message.makeMessage());
                        if(getScrollThread() != null) {
                            getScrollThread().interrupt();
                        }
                    }
                }
            });
        }

        Platform.runLater(() -> {
            setUpPanesAndTarget();
            getScrollPane().requestFocus(); // so on space-bar hit no accidental button press
        });
    }

    public void setUpScrollPane(){
        InlineCssTextArea textArea = new InlineCssTextArea();
        textArea.appendText(getText("src/main/resources/files/loremIpsum.txt"));
        textArea.setWrapText(true);
        textArea.setEditable(false);
        //padding to leave room for line numbers
        textArea.setPadding(new Insets(0,0,0,60));
        setTextArea(textArea);

        VirtualizedScrollPane scrollPane = new VirtualizedScrollPane<>(textArea);
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
            setTarget();
            setMaxScrollVal();
            getScrollPane().scrollYToPixel(0);
            topPane.setVisible(false);
        });
    }

    public void setMaxScrollVal(){
        setScrollContentHeight();
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


    //Updates Target Highlight, Indicator position and Frame size + Colors
    public void setTarget() {
        //** Set Target
        targetNumber++;
        startTime = System.currentTimeMillis();
        InlineCssTextArea textArea = getTextArea();
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
        double distanceToTop = getScrollPane().getHeight() * relativeIndex;
        double centerPosition = minY + distanceToTop;
        double yPos = centerPosition - (indicator.getHeight()/2) + 1; //+1px boarder

        double maxY = scrollPane_parent.getBoundsInParent().getMaxY() - indicator.getHeight();

        double validY = Math.max(Math.min(yPos, maxY), minY);
        indicator.setLayoutY(validY);

    }

    private int getRandomValidTargetIndex() {
        Random random = new Random();
        InlineCssTextArea textArea = getTextArea();
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

        long deltaTime = System.currentTimeMillis() - startTime;
        System.out.println("-- Time of Phase "+ targetNumber +" = " + deltaTime);

        Optional<Bounds> bounds = getTextArea().getParagraphBoundsOnScreen(targetIndex); //values fit more P 41 (aka index 42)
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
        InlineCssTextArea textArea = getTextArea();
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
        distance = Integer.parseInt(distanceInput.getText());
        frameSize = Integer.parseInt(frameInput.getText());

        System.out.println(" -------- NEW TRIAL -------- ");
        getScrollPane().requestFocus(); // so on space-bar hit no accidental button press

        //remove old target and set new one
        InlineCssTextArea textArea = getTextArea();
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


}
