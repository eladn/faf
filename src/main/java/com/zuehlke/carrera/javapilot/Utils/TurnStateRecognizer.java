package com.zuehlke.carrera.javapilot.Utils;

/**
 * Created by ValkA on 12-Mar-16.
 */


public class TurnStateRecognizer {
    public enum TurnState {
        Right, Left, Straight
    }

    //  private final int GYRO_NOISE_MARGIN = 10;
    private int TURN_STATE_THRESHOLD;
    private TurnState currentTurnState = TurnState.Straight;
    private long lastStateTimestamp;
    private long lastStateDuration = 0;
    private double lastPeakMax = 0;

    public TurnStateRecognizer(int stateThreshold) {
        TURN_STATE_THRESHOLD = stateThreshold;
        lastStateTimestamp = System.currentTimeMillis();
    }

    /*
        set your input from the Z axis gyro
        returns true if currentTurnState was changed
     */
    public boolean newInput(double avg) {
        //Right = positive
        double absAvg = Math.abs(avg);
        if (avg > TURN_STATE_THRESHOLD) {
            if(currentTurnState != TurnState.Right) {
                currentTurnState = TurnState.Right;
                updateTime();
                lastPeakMax = absAvg;
                return true;
            } else {
                if(absAvg > lastPeakMax) lastPeakMax = absAvg;
            }
        } else if (avg < -TURN_STATE_THRESHOLD){
            if(currentTurnState != TurnState.Left) {
                currentTurnState = TurnState.Left;
                updateTime();
                lastPeakMax = absAvg;
                return true;
            } else {
                if(absAvg > lastPeakMax) lastPeakMax = absAvg;
            }
        } else if (Math.abs(avg) <= TURN_STATE_THRESHOLD){
            if(currentTurnState != TurnState.Straight) {
                currentTurnState = TurnState.Straight;
                updateTime();
                lastPeakMax = absAvg;
                return true;
            } else {
                if(absAvg > lastPeakMax) lastPeakMax = absAvg;
            }
        }
        return false;
    }

    public TurnState getCurrentTurnState(){
        return currentTurnState;
    }

    public long getLastStateDuration(){
        return lastStateDuration;
    }

    public double getLastPeak(){
        return lastPeakMax;
    }

    private void updateTime(){
        lastStateDuration = System.currentTimeMillis() - lastStateTimestamp;
        lastStateTimestamp = System.currentTimeMillis();
    }
}
