package HelperClasses;

public enum ScrollingMode {
    // "Drag", "Scroll Wheel", "Flick", "Flick Dec.", "Flick + Pressure", "Scroll Bar", "Paging", "Speed variations (V2)", "Button hold", "AutoScroll", "Circle (V2)", "Circle (V3)"
    //"Scroll", "ScrollWheel", "Flick", "Flick2", "Flick3", "Bar", "Paging", "Speed2", "ButtonHold", "TrackPoint", "Circle2", "Circle3"

    DRAG("Drag"), DRAG_acceleration( "DragAcceleration"),  FLICK("Flick"), CIRCLE("Circle3"), RATE_BASED("TrackPoint"),
    RUBBING("Rubbing"), WHEEL("ScrollWheel"), THUMB("Thumb"), MULTI_SCROLL("TwoFinger"),
    FAST_FLICK("Flick");

    String value;
    ScrollingMode(String val){
        this.value = val;
    }

    public String getValue() {
        return value;
    }
}
