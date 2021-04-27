package HelperClasses;

public enum ScrollingMode {
    // "Drag", "Scroll Wheel", "Flick", "Flick Dec.", "Flick + Pressure", "Scroll Bar", "Paging", "Speed variations (V2)", "Button hold", "AutoScroll", "Circle (V2)", "Circle (V3)"
    //"Scroll", "ScrollWheel", "Flick", "Flick2", "Flick3", "Bar", "Paging", "Speed2", "ButtonHold", "TrackPoint", "Circle2", "Circle3"

    DRAG("Drag"), DRAG_acceleration( "DragAcceleration"), FLICK("Flick"), FLICK_multi("MultiFlick"), FLICK_deceleration("DecelFlick"),
    FLICK_iphone("IPhoneFlick"), FLICK_iOS("iOS"), FLICK_iOS_2("iOS2"), FLICK1000("Flick1000"),
    CIRCLE("Circle3"), RATE_BASED("TrackPoint"), RUBBING("Rubbing"), WHEEL("ScrollWheel"), THUMB("Thumb"), MULTI_SCROLL("TwoFinger");

    String value;
    ScrollingMode(String val){
        this.value = val;
    }

    public String getValue() {
        return value;
    }
}
