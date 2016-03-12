package com.zuehlke.carrera.javapilot.Utils;

/**
 * Created by ValkA on 12-Mar-16.
 */


public class TurnStateRecognizer {
    public enum TurnState {
        Right, Left, Straight
    }

    class History {
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

        public History clone() {
            History hist = new History(this);
            return hist;
        }
    }

    //  private final int GYRO_NOISE_MARGIN = 10;
    private int TURN_STATE_THRESHOLD;
    private TurnState currentTurnState = TurnState.Straight;
    private final long MIN_STRAIGHT_TIME = 85;

    private boolean maybeStraight = false;

    private History stats = null;
    private History maybyStraightStats = null;

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
        if(maybyStraightStats != null) maybyStraightStats.currentClockCounter++;
        double absAvg = Math.abs(avg);
        if(absAvg > stats.currentPeakMax) stats.currentPeakMax = absAvg;
        if(maybyStraightStats != null && absAvg > maybyStraightStats.currentPeakMax) maybyStraightStats.currentPeakMax = absAvg;

        //TurnState sensor = (avg > TURN_STATE_THRESHOLD ? TurnState.Left : (avg < -TURN_STATE_THRESHOLD ? TurnState.Right : TurnState.Straight));

        //if (sensor != )

        if (avg > TURN_STATE_THRESHOLD) { //Right
            if(currentTurnState != TurnState.Right) {
                maybeStraight = false;
                currentTurnState = TurnState.Right;
                stats.updateTime(absAvg);
                return true;
            } else {
            }
        } else if (avg < -TURN_STATE_THRESHOLD){ //Left
            if(currentTurnState != TurnState.Left) {
                maybeStraight = false;
                currentTurnState = TurnState.Left;
                stats.updateTime(absAvg);
                return true;
            } else {
            }
        } else if (Math.abs(avg) <= TURN_STATE_THRESHOLD && currentTurnState != TurnState.Straight){ //Straight
            if(maybeStraight) {
                // passed more that 85ms from suspect begin.
                if(System.currentTimeMillis() - maybyStraightStats.lastStateTimestamp > MIN_STRAIGHT_TIME) { // really straight
                    maybeStraight = false;

                    // restore deemmy vars to real
                    currentTurnState = TurnState.Straight;
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
        return false;
    }

    public TurnState getCurrentTurnState(){
        return currentTurnState;
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
