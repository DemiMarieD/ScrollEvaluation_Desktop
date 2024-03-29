package HelperClasses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Trial {

    BufferedWriter writer;

    int participantID;
    int trialNumber;
    int roundNumber;
    int trialInBlock;
    int textLength;
    int visibleLines;
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
    double pxPerMM;
    int expPart;

    //only for moose
    ScrollingMode mode;
    double fingerCount;
    String posMin_x;
    String posMax_x;
    String scrollAreaSize_x;
    String posMin_y;
    String posMax_y;
    String scrollAreaSize_y;
    //String clutchCount;


    public Trial(int participantID, int expPart, int roundNumber, int trialNumber, int trialInBlock, int block,  int targetLine, double frameHeight, int distance, String direction, Device device) {
        this.participantID = participantID;
        this.roundNumber = roundNumber;
        this.trialNumber = trialNumber;
        this.frameHeight = round2D(frameHeight);
        this.distance = distance;
        this.direction = direction;
        this.targetLine = targetLine;
        this.device = device;
        this.trialInBlock = trialInBlock;
        this.block = block;
        this.expPart = expPart;

        targetInFrame_counter = 0;
        targetVisible_counter = 0;


        try {
            String path = "logData/MooseScrolling_Data_p" + participantID + ".txt"; //saved in storage on the phone
            File file = new File(path);
            Boolean isNew = !file.exists();
            writer = new BufferedWriter(new FileWriter(file, true));
            if(isNew){
                String text = "ID, Device, Mode, ExpPart, Round, Trial, Block, Trial in Block, Text Length (lines), Visible Lines, Frame Height, Distance, " +
                        "Direction, Target Line, Line Height, Hit, T_Trial, T_Scroll, T_FineTune, T_Select, " +
                        "T_StartScrolling, errorDistance(lines), inFrameCount, visibleCount, Start direction, " +
                        "Finger count, FP_X-MIN, FP_Y-MIN, " +
                        "FP_X-MAX, FP_Y-MAX, ScrollareaX, ScrollareaY, Px_mm\n";
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
        long deltaTime_scroll = Math.max(time_scrollEnd - time_scrollStart, 0);
        long deltaTime_startScroll = Math.max(time_scrollStart - time_trialStart, 0);
        long deltaTime_select = deltaTime_total;
        if(deltaTime_scroll > 0){
           deltaTime_select = time_trialEnd - time_scrollEnd;
        }

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
                 data = participantID + "," + device.name() + "," + mode.getValue() + "," + expPart + "," + roundNumber + "," + trialNumber + "," + block + "," + trialInBlock + "," + textLength + "," + visibleLines + "," +
                    frameHeight + "," + distance + "," + direction + "," + targetLine + "," + lineHeight + "," + hit + "," + deltaTime_total + "," +
                    deltaTime_scroll + "," + deltaTime_fineTune + "," + deltaTime_select + "," + deltaTime_startScroll + "," + distanceFromMiddle + "," +
                    targetInFrame_counter + "," + targetVisible_counter+ "," + startDirection+ "," + fingerCount+ "," +
                    posMin_x + "," + posMin_y + "," + posMax_x + "," + posMax_y + "," + scrollAreaSize_x + "," + scrollAreaSize_y + "," + pxPerMM + ","
                    + "\n";
        }else{
            data = participantID + "," + device.name() + "," + " --- " + "," + "," + expPart + roundNumber + "," + trialNumber + "," + block + "," + trialInBlock + "," + textLength + "," +  visibleLines + "," +
                    frameHeight + "," + distance + "," + direction + "," + targetLine + "," + lineHeight + "," + hit + ","  + deltaTime_total + "," +
                    deltaTime_scroll + "," + deltaTime_fineTune + "," + deltaTime_select + "," + deltaTime_startScroll + "," + distanceFromMiddle + "," +
                    targetInFrame_counter + "," + targetVisible_counter + "," + startDirection+ ", ---, ---, ---, ---, ---, ---, ---," + pxPerMM +
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

  /*  public void setClutchCount(String clutchCount) {
        this.clutchCount = clutchCount;
    } */

    public void setFingerCount(double fingerCount) {
        this.fingerCount = fingerCount;
    }

    public void setPosMin_x(String posMin_x) {
        double d = Double.parseDouble(posMin_x);
        this.posMin_x = String.valueOf(round2D(d));
    }

    public void setPosMax_x(String posMax_x) {
        double d = Double.parseDouble(posMax_x);
        this.posMax_x = String.valueOf(round2D(d));
    }

    public void setScrollAreaSize_x(String scrollAreaSize_x) {
        double d = Double.parseDouble(scrollAreaSize_x);
        this.scrollAreaSize_x = String.valueOf(round2D(d));
    }

    public void setPosMin_y(String posMin_y) {
        double d = Double.parseDouble(posMin_y);
        this.posMin_y = String.valueOf(round2D(d));
    }

    public void setPosMax_y(String posMax_y) {
        double d = Double.parseDouble(posMax_y);
        this.posMax_y = String.valueOf(round2D(d));
    }

    public void setScrollAreaSize_y(String scrollAreaSize_y) {
        double d = Double.parseDouble(scrollAreaSize_y);
        this.scrollAreaSize_y = String.valueOf(round2D(d));
    }

    public void setTextLength(int textLength) {
        this.textLength = textLength;
    }

    public void setPxPerMM(double pxPerMM) {
        this.pxPerMM = round2D(pxPerMM);
    }

    public void setVisibleLines(int visibleLines) {
        this.visibleLines = visibleLines;
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
        this.lineHeight = round2D(lineHeight);
    }

    //------------------------------------------------------------------------------
    public double round2D(double d){
       return Math.round(d*1000.0)/1000.0;
    }

}
