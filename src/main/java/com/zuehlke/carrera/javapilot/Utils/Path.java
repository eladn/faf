package com.zuehlke.carrera.javapilot.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ValkA on 12-Mar-16.
 */
public abstract class Path {
    protected List<Segment> list;

    public Path(){
        list = new ArrayList<Segment>();
    }

    /*
        returns pathIsReady
     */
    public void addSegment(TurnStateRecognizer.TurnState turnState){
        list.add(new Segment(turnState));
    }

    public int getSegmentsSize(){
        return list.size();
    }

    protected Segment getSegment(int i){
        assert(0<=i && i<getSegmentsSize());
        return list.get(i);
    }

    public String toLettersString() {
        String str = "";
        for (Segment seg : list) {
            str += seg.getTurnState().toString();
        }
        return str;
    }

}
