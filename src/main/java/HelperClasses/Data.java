package HelperClasses;

public class Data {
    private Device device;
    private ScrollingMode mode;

    private int participantID;
    private int numberOfBlocks;

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    private int roundNumber;

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public void setNumberOfBlocks(int numberOfBlocks) {
        this.numberOfBlocks = numberOfBlocks;
    }

    public int getParticipantID() {
        return participantID;
    }

    public void setParticipantID(int participantID) {
        this.participantID = participantID;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public ScrollingMode getMode() {
        return mode;
    }

    public void setMode(ScrollingMode mode) {
        this.mode = mode;
    }
}
