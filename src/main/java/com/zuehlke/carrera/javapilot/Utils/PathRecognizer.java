package com.zuehlke.carrera.javapilot.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ValkA on 12-Mar-16.
 */
public class PathRecognizer extends Path {
    Track track;
    int currentState, nextState;
    boolean lost = false;
    //List<TurnStateRecognizer.TurnState> turns = new ArrayList<TurnStateRecognizer.TurnState>();

    public PathRecognizer(Track track){
        this.track = track;
        currentState = 0;
        nextState = 0;
    }
    public int lostAttemptCounter = 0;
    public void setNextState(TurnStateRecognizer.TurnState turnState){
        currentState = nextState;
        nextState = (currentState + 1) % track.getSegmentsSize();

        if (lost) {
            lostAttemptCounter++;
            if (lostAttemptCounter>=5) lostAttemptCounter = 0;

            System.out.println("[INFO} Still LOST .......... [last saved turns: " + this.toLettersString() + " size:" + this.getSegmentsSize() + "]");
            list.add(new Segment(turnState));
            int idx = track.isOnlySubPath(this);
            if (idx > -1) { // found
                list.clear();
                lost = false;
                currentState = idx;
                nextState = (currentState + 1) % track.getSegmentsSize();
                System.out.println("[INFO} FOUND !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! [currentStateIdx: " + currentState + " next_state: " + nextState + "]");
            } else if (lostAttemptCounter%5 == 0 || list.size() > track.getSegmentsSize()) {
                list.remove(0);
            }
        } else if(turnState != track.getSegment(currentState).turnState){
            list.add(new Segment(turnState));
            System.out.println("[INFO} WE LOST THE TRACK  [turn_state: " + turnState + ", current_state: " + currentState + " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            lost = true;
            return; // ?
        } else {
            System.out.println("[INFO} next state succeed. current_state: " + currentState + " next_state: " + nextState);
        }

    }

    public boolean isLost() {
        return lost;
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
