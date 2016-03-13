package com.zuehlke.carrera.javapilot.Utils;

/**
 * Created by ValkA on 12-Mar-16.
 */


public class TurnStateRecognizer {
    public enum TurnState {
        Right, Left, Straight;

        public String toString() {
            return (this == Right) ? "R" : (this == Left ? "L" : "S");
        }
    }

    class History {
        public TurnState currentTurnState = TurnState.Straight;
        public long lastStateTimestamp;
        public long lastStateDuration = 0;
        public double currentPeakMax = 0;
        public double lastPeakMax = 0;
        public int currentClockCounter = 1;
        public int lastClockCounter = 1;

        History() {
            this.lastStateTimestamp = System.currentTimeMillis();
        }
        History(History another) {
            this.currentTurnState = another.currentTurnState;
            this.lastStateTimestamp = another.lastStateTimestamp;
            this.lastStateDuration = another.lastStateDuration;
            this.currentPeakMax = another.currentPeakMax;
            this.lastPeakMax = another.lastPeakMax;
            this.currentClockCounter = another.currentClockCounter;
            this.lastClockCounter = another.lastClockCounter;
        }

        private void updateTime(double absAvg){
            lastPeakMax = currentPeakMax;
            currentPeakMax = absAvg;

            lastClockCounter = currentClockCounter;
            currentClockCounter = 1;

            lastStateDuration = System.currentTimeMillis() - lastStateTimestamp;
            lastStateTimestamp = System.currentTimeMillis();
        }

        public long getValidThreshold() {
            return (currentTurnState != TurnState.Straight) ? MIN_TURN_CLOCKS : MIN_STRAIGHT_CLOCKS;
        }

        public boolean isValidTime() {
//            System.out.print("    " + getValidThreshold() + " < " + this.currentClockCounter + " = " + (currentClockCounter > getValidThreshold()));
            return (currentClockCounter > getValidThreshold());
        }

        public History clone() {
            History hist = new History(this);
            return hist;
        }
    }

    //  private final int GYRO_NOISE_MARGIN = 10;
    private int TURN_STATE_THRESHOLD;

    private final long MIN_STRAIGHT_TIME = 85;
    private final long MIN_TURN_TIME = 50;

    private final long MIN_STRAIGHT_CLOCKS = 4;
    private final long MIN_TURN_CLOCKS = 4;

    //private boolean maybeStraight = false; //remove

    private History stats = null;
    //private History maybyStraightStats = null; //remove
    private History maybeNewStats = null;

    public TurnStateRecognizer(int stateThreshold) {
        TURN_STATE_THRESHOLD = stateThreshold;
        stats = new History();

    }

    /*
        set your input from the Z axis gyro
        returns true if currentTurnState was changed
     */
    public boolean newInput(double avg) {
        //Right = positive
        stats.currentClockCounter++;
        if(maybeNewStats != null) maybeNewStats.currentClockCounter++;
        double absAvg = Math.abs(avg);
        if(absAvg > stats.currentPeakMax) stats.currentPeakMax = absAvg;
        if(maybeNewStats != null && absAvg > maybeNewStats.currentPeakMax) maybeNewStats.currentPeakMax = absAvg;

        TurnState sensor = (avg > TURN_STATE_THRESHOLD ? TurnState.Right : (avg < -TURN_STATE_THRESHOLD ? TurnState.Left : TurnState.Straight));
        System.out.print("Sensor: " + sensor + "    " + "current_state: " + stats.currentTurnState + "     ");

        if (maybeNewStats != null) {
            if (sensor == maybeNewStats.currentTurnState) {
                // check if already valid new state
                if(maybeNewStats.isValidTime()) { // really straight
                    stats = maybeNewStats;
                    maybeNewStats = null;
                    return true;
                }
            } else {
                if (sensor == stats.currentTurnState) {
                    maybeNewStats = null;
                } else {
                    System.out.print("set maybe: " + sensor.toString() + "   [delete old maybe set to " + maybeNewStats.currentTurnState + "]");
                    maybeNewStats = stats.clone();
                    maybeNewStats.currentTurnState = sensor;
                    maybeNewStats.updateTime(absAvg);
                }
            }
        } else if (sensor != stats.currentTurnState) { // maybeNewStats == null
            System.out.print("set maybe: " + sensor.toString());
            maybeNewStats = stats.clone();
            maybeNewStats.currentTurnState = sensor;
            maybeNewStats.updateTime(absAvg);
        }




/*
        if (avg > TURN_STATE_THRESHOLD && currentTurnState != TurnState.Right) { //Right
            maybeStraight = false;
            stats.currentTurnState = TurnState.Right;
            stats.updateTime(absAvg);
            return true;
        } else if (avg < -TURN_STATE_THRESHOLD && currentTurnState != TurnState.Left){ //Left
            maybeStraight = false;
            stats.currentTurnState = TurnState.Left;
            stats.updateTime(absAvg);
            return true;
        } else if (Math.abs(avg) <= TURN_STATE_THRESHOLD && currentTurnState != TurnState.Straight){ //Straight
            if(maybeStraight) {
                // passed more that 85ms from suspect begin.
                if(System.currentTimeMillis() - maybyStraightStats.lastStateTimestamp > MIN_STRAIGHT_TIME) { // really straight
                    maybeStraight = false;

                    // restore deemmy vars to real
                    stats.currentTurnState = TurnState.Straight;
                    stats = maybyStraightStats;
                    maybyStraightStats = null;
                    //stats.updateTime(absAvg);
                    return true;
                }
            } else {
                // chage to demmy vars
                maybyStraightStats = stats.clone();
                maybyStraightStats.updateTime(absAvg);
                maybeStraight = true;
            }
        }
        */

        return false;
    }

    public TurnState getCurrentTurnState(){
        return stats.currentTurnState;
    }

    public long getLastStateDuration(){
        return stats.lastStateDuration;
    }

    public int getLastStateClockCounter(){
        return stats.lastClockCounter;
    }

    public double getLastPeak(){
        return stats.lastPeakMax;
    }

}
