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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
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

public class HyperlinkViewController extends ScrollController {

    @FXML
    private ComboBox cb;
    @FXML
    private Pane topPane;
    @FXML
    private  Pane frame;
    @FXML
    private ScrollPane scrollPane_parent;

    //For Framing Task
    private double frameSize; // px
    private int targetIndex;
    private final ArrayList<Integer> Distances =  new ArrayList<Integer>(Arrays.asList(6, 24, 96, 192));  //in number of lines
    private final ArrayList<Integer> FrameSizes = new ArrayList<Integer>(Arrays.asList(6, 18)); //in number of lines

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

        //Media new File(path).toURI().toString()
        wrongPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/wrong.wav").toURI().toString()));
        rightPlayer = new MediaPlayer(new Media(new File("src/main/resources/files/success.wav").toURI().toString()));

        setUpScrollPane();

        Platform.runLater(() -> {
            setUpPanesAndTarget();
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



        Platform.runLater(() -> {
            addLineNumbers();
            setTarget();

            topPane.setVisible(false);
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
        InlineCssTextArea textArea = getTextArea();
        //** Set Target
        Random random = new Random();
        int totalNumberOfLines = textArea.getParagraphs().size();

        int startTop = random.nextInt(2);
        System.out.println("Start " + startTop);
        if(startTop==1){
            getScrollPane().scrollYToPixel(0);
        }else{
            Text t = (Text) textArea.lookup(".text");
            double lineHeight = t.getBoundsInLocal().getHeight();
            getScrollPane().scrollYToPixel(lineHeight*totalNumberOfLines);
        }


        //!! minus lines that can be reached outside the smallest frame
        Text t = (Text) textArea.lookup(".text");
        double lineHeight = t.getBoundsInLocal().getHeight();
        long visibleLines = Math.round(textArea.getHeight() / lineHeight);
        System.out.println("Visible Lines: " + visibleLines);
        //assuming the lists are ordered by size!
        int minFramesize = FrameSizes.get(0);
        int nonReachableLines = (int) (visibleLines - minFramesize);
        int maxDistance = Distances.get(Distances.size()-1);
        int min = nonReachableLines/2 + maxDistance;
        int max = totalNumberOfLines - (nonReachableLines/2) - maxDistance;

        targetIndex = min + random.nextInt(max-min);

       // targetIndex = random.nextInt(totalNumberOfLines);

        //highlight target
        int l = textArea.getParagraphLength(targetIndex);
        int start = textArea.getAbsolutePosition(targetIndex, 0);
        System.out.println("Target "+ targetIndex);
        textArea.setStyle(targetIndex, 0, l,  "-fx-fill: blue; -fx-underline: true;");

        //random frame size
        frameSize = FrameSizes.get(random.nextInt(FrameSizes.size()));
        updateFrameHeight();

        frame.setStyle("-fx-background-color: blue");

    }

    public void updateFrameHeight(){
        InlineCssTextArea textArea = getTextArea();
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
        InlineCssTextArea textArea = getTextArea();
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


}
