package com.zuehlke.carrera.javapilot.Utils;

/**
 * Created by ValkA on 12-Mar-16.
 */
public class PathRecognizer extends Path {
    Track track;
    int currentState;

    public PathRecognizer(Track track){
        this.track = track;
        currentState = 0;
    }

    public boolean setNextState(TurnStateRecognizer.TurnState turnState){
        System.out.println("TrackState = " +  track.getSegment(currentState).turnState + ", SensorState = " + turnState);

        if(turnState != track.getSegment(currentState).turnState){
            System.out.println("[INFO} WE LOST THE TRACK !!!!");
            return false;
        }

        currentState = (currentState + 1) % track.getSegmentsSize();
        return true;
    }

    public int getCurrentStateIndex(){
        return  currentState;
    }

    public Segment getCurrentStateSegment(){
        return track.getSegment(currentState);
    }


}
