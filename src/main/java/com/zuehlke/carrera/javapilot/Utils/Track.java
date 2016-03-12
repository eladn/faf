package com.zuehlke.carrera.javapilot.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ValkA on 12-Mar-16.
 */

public class Track extends Path {
    private boolean pathIsReady = false;
    private final int MIN_PATH_SIZE = 7;

    public Track(){
        super();
    }

    public void addSegment(TurnStateRecognizer.TurnState turnState){
        if(!pathIsReady){
            super.addSegment(turnState);
            pathIsReady = checkDoubleCyclicPath();
        }
    }

    public boolean isReady(){
        return pathIsReady;
    }

    public void setLastSegmentSharpness(double sharpness){
        list.get(list.size()-1).setSharpness(sharpness);
    }

    private boolean checkDoubleCyclicPath() {
        if(list.size()<(MIN_PATH_SIZE*2) || list.size()%2 != 0){
            return false;
        }

        for(int i=0; i<list.size()/2; ++i){
            Segment first = list.get(i);
            Segment second = list.get(i+list.size()/2);
            if(first.getTurnState() != second.getTurnState()){
                return false;
            }
        }

        System.out.println(list.toString());

        //cut
        int wantedSize = list.size()/2;
        while(list.size()>wantedSize){
            list.remove(list.size()-1);
        }
        System.out.println(list.toString());
        System.out.println(" ");

        //find max
        double maxSharpness=0;
        for (Segment segment: list) {
            if(segment.getSharpness() > maxSharpness) maxSharpness = segment.getSharpness();
        }

        if(maxSharpness==0){
            System.out.println("[ERROR] maxSharpness = 0 !!!! check WTF in java file !!!!!");
            return true;
        }

        for (Segment segment: list) {
            segment.setSharpness(segment.getSharpness()/maxSharpness);
        }

        return true;
    }
}
