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

    public String toString(){
        String str = "";
        for(int i=0; i<track.getSegmentsSize(); ++i){
            String delim = " ";
            if(i==currentState){
                delim="*";
            }
            str += delim + track.getSegment(i).getTurnState().toString() + delim;
        }
        return str;
    }


}
