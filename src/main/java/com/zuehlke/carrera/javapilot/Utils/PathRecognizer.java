package com.zuehlke.carrera.javapilot.Utils;

/**
 * Created by ValkA on 12-Mar-16.
 */
public class PathRecognizer extends Path {
    Track track;
    int nextState;

    public PathRecognizer(Track track){
        this.track = track;
        nextState = 0;
    }

    public boolean setNextState(TurnStateRecognizer.TurnState turnState){
        System.out.println("TrackState = " +  track.getSegment(nextState).turnState + ", SensorState = " + turnState);

        if(turnState != track.getSegment(nextState).turnState){
            System.out.println("[INFO} WE LOST THE TRACK !!!!");
            return false;
        }

        nextState = (nextState + 1) % track.getSegmentsSize();
        return true;
    }

    public int getCurrentStateIndex(){
        return  nextState;
    }

    public Segment getCurrentStateSegment(){
        return track.getSegment(nextState);
    }


}
