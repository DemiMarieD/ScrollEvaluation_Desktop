package HelperClasses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Trial {

    BufferedWriter writer;

    int participantID;
    int trialNumber;
    int trialInBlock;
    int block;
    double frameHeight;
    int targetLine;
    int distance;
    String direction;
    Device device;
    long time_trialStart;
    double lineHeight;

    long time_trialEnd;
    long time_scrollStart;
    long time_scrollEnd;
    long time_lastVisible;
    int distanceFromMiddle;
    String startDirection;
    int targetInFrame_counter;
    int targetVisible_counter;
    boolean hit;

    //only for moose
    ScrollingMode mode;
    double fingerCount;
    String posMin;
    String posMax;
    String scrollAreaSize;



    //todo: add distance to trial on error !


    public Trial(int participantID, int trialNumber, int trialInBlock, int block,  int targetLine, double frameHeight, int distance, String direction, Device device) {
        this.participantID = participantID;
        this.trialNumber = trialNumber;
        this.frameHeight = frameHeight;
        this.distance = distance;
        this.direction = direction;
        this.targetLine = targetLine;
        this.device = device;
        this.trialInBlock = trialInBlock;
        this.block = block;

        targetInFrame_counter = 0;
        targetVisible_counter = 0;


        try {
            //todo .csv !
            String path = "MooseScrolling_Data" + participantID + ".txt"; //saved in storage on the phone
            File file = new File(path);
            Boolean isNew = !file.exists();
            writer = new BufferedWriter(new FileWriter(file, true));
            if(isNew){
                String text = "ID, Device, Mode, Trial Number, Block Number, Trial in Block, Frame Height, Distance, " +
                        "Direction, Target Line, Line Height, Hit, (T) Trial, (T) Scroll, (T) Fine Tune, (T) Select, " +
                        "(T) Start Scroll, Distance from middle (lines), #Target in frame, #Target visible, Start direction, " +
                        "Finger count, Finger positions MIN, Finger positions MAX, Scroll area size  \n";
                writer.write(text);
                writer.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();

          }



    }

    public void setMode(ScrollingMode mode) {
        this.mode = mode;
    }

    public void writeTrial(){

        System.out.println("Trial: " + trialNumber + ", Block: " + block +  ", -- D: "+ distance + " - " + direction);

        //todo Bug not correct times!
        long deltaTime_total = time_trialEnd - time_trialStart;
        long deltaTime_scroll = time_scrollEnd - time_scrollStart;
        long deltaTime_startScroll = time_scrollStart - time_trialStart;
        long deltaTime_select = time_trialEnd - time_scrollEnd;
        System.out.print( " - total " + deltaTime_total + " - scroll " + deltaTime_scroll + " - start " + deltaTime_startScroll + " - select " + deltaTime_select );

        long deltaTime_fineTune = 0;
        if(targetVisible_counter > 0){
            deltaTime_fineTune = time_scrollEnd - time_lastVisible;
            System.out.println( " - fine tune " + deltaTime_fineTune);
        }
        System.out.println("\n");

        //  "ID, Device, Mode, Trial Number, Block Number, Trial in Block, Frame Height, Distance, Direction, " +
        //  "Target Line, Line Height, Hit, (T) Trial, (T) Scroll, (T) Fine Tune, (T) Select, (T) Start Scroll\n";
        String data = "";
        if(device == Device.MOOSE) {
            data = participantID + "," + device.name() + "," + mode.getValue() + "," + trialNumber + "," + block + "," + trialInBlock + "," +
                    frameHeight + "," + distance + "," + direction + "," + targetLine + "," + lineHeight + "," + hit + "," + deltaTime_total + "," +
                    deltaTime_scroll + "," + deltaTime_fineTune + "," + deltaTime_select + "," + deltaTime_startScroll + "," + distanceFromMiddle + "," +
                    targetInFrame_counter + "," + targetVisible_counter+ "," + startDirection+ "," + fingerCount+ "," + posMin+ "," + posMax + "," + scrollAreaSize +
                    "\n";
        }else{
            data = participantID + "," + device.name() + "," + " --- " + "," + trialNumber + "," + block + "," + trialInBlock + "," +
                    frameHeight + "," + distance + "," + direction + "," + targetLine + "," + lineHeight + "," + hit + ","  + deltaTime_total + "," +
                    deltaTime_scroll + "," + deltaTime_fineTune + "," + deltaTime_select + "," + deltaTime_startScroll + "," + distanceFromMiddle + "," +
                    targetInFrame_counter + "," + targetVisible_counter + "," + startDirection+ ", ---, ---, ---, ---" +
                    "\n";
        }

        try {
            writer.write(data);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public void setStartDirection(String startDirection) {
        this.startDirection = startDirection;
    }

    public void setFingerCount(double fingerCount) {
        this.fingerCount = fingerCount;
    }

    public void setPosMin(String posMin) {
        this.posMin = posMin;
    }

    public void setPosMax(String posMax) {
        this.posMax = posMax;
    }

    public void setScrollAreaSize(String scrollAreaSize) {
        this.scrollAreaSize = scrollAreaSize;
    }

    public void targetInFrame(){
        targetInFrame_counter++;
    }
    public void targetVisible(){
        targetVisible_counter++;
    }
    public void setDistanceFromMiddle(int distanceFromMiddle) {
        this.distanceFromMiddle = distanceFromMiddle;
    }

    public void setTime_trialStart(long time_trialStart) {
        this.time_trialStart = time_trialStart;
    }

    public void setTime_trialEnd(long time_trialEnd) {
        this.time_trialEnd = time_trialEnd;
    }

    public void setTime_scrollStart(long time_scrollStart) {
        this.time_scrollStart = time_scrollStart;
    }

    public void setTime_scrollEnd(long time_scrollEnd) {
        this.time_scrollEnd = time_scrollEnd;
    }

    public void setTime_lastVisible(long time_lastVisible) {
        this.time_lastVisible = time_lastVisible;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public void setLineHeight(double lineHeight) {
        this.lineHeight = lineHeight;
    }
}
