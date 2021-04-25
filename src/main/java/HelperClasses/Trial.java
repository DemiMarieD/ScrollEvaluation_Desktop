package HelperClasses;

public class Trial {

    int participantID;
    int trialNumber;
    double frameHeight;
    int targetLine;
    int distance;
    String direction;
    Device device;
    long time_trialStart;
    double lineHeight;
    ScrollingMode mode;
    long time_trialEnd;
    long time_scrollStart;
    long time_scrollEnd;
    long time_lastVisible;

    int targetInFrame_counter;
    int targetVisible_counter;
    boolean hit;


    public Trial(int participantID, int trialNumber, int targetLine, double frameHeight, int distance, long time_trialStart, String direction, Device device) {
        this.participantID = participantID;
        this.trialNumber = trialNumber;
        this.frameHeight = frameHeight;
        this.distance = distance;
        this.time_trialStart = time_trialStart;
        this.direction = direction;
        this.targetLine = targetLine;
        this.device = device;

        targetInFrame_counter = 0;
        targetVisible_counter = 0;
    }

    public void setMode(ScrollingMode mode) {
        this.mode = mode;
    }

    public void writeTrial(){
        //TODO !

        System.out.println(toString());

        long deltaTime_total = time_trialEnd - time_trialStart;
        long deltaTime_scroll = time_scrollEnd - time_scrollStart;
        long deltaTime_startScroll = time_scrollStart - time_trialStart;
        long deltaTime_select = time_trialEnd - time_scrollEnd;
        System.out.print( " - total " + deltaTime_total + " - scroll " + deltaTime_scroll + " - start " + deltaTime_startScroll + " - select " + deltaTime_select );

        if(targetVisible_counter > 0){
            long deltaTime_fineTune = time_scrollEnd - time_lastVisible;
            System.out.println( " - fine tune " + deltaTime_fineTune);
        }

    }

    @Override
    public String toString() {
        return "Trial{" +
                "participantID=" + participantID +
                ", trialNumber=" + trialNumber +
                ", frameHeight=" + frameHeight +
                ", targetLine=" + targetLine +
                ", distance=" + distance +
                ", direction='" + direction + '\'' +
                ", device=" + device.name() +
                ", time_trialStart=" + time_trialStart +
                ", lineHeight=" + lineHeight +
                ", time_trialEnd=" + time_trialEnd +
                ", time_scrollStart=" + time_scrollStart +
                ", time_scrollEnd=" + time_scrollEnd +
                ", #Target_visible=" + targetVisible_counter +
                ", #Target_inFrame=" + targetInFrame_counter +
                ", hit=" + hit +
                '}';
        //mode is ignored because optional as it depends on device
    }

    public void targetInFrame(){
        targetInFrame_counter++;
    }
    public void targetVisible(){
        targetVisible_counter++;
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
