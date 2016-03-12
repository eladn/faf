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
    public void setLastSegmentInitDuraion(long duraion){
        list.get(list.size()-1).setInitDuration(duraion);
    }
    public void setLastSegmentClockCounter(int clocks){
        list.get(list.size()-1).setInitClocks(clocks);
    }

    private boolean checkDoubleCyclicPath() {
        System.out.println(list.toString());
        System.out.println("");

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

        //find max
        double maxSharpness=0;
        for (Segment segment: list) {
            if(segment.getSharpness() > maxSharpness) maxSharpness = segment.getSharpness();
        }

        if(maxSharpness==0){
            System.out.println("[ERROR] maxSharpness = 0 !!!! check WTF in java file !!!!!");
            return true;
        } else {
            System.out.println("maxSharpness = " + maxSharpness);
        }

        for (Segment segment: list) {
            segment.setSharpness(1 - segment.getSharpness()/maxSharpness);
        }

        System.out.println(list.toString());
        System.out.println(" ");


        return true;
    }

    /*
        -1 if not, else returns last common index (in Track)
     */
    int isOnlySubPath(Path path){
        int offset = -1;
        for(int i=0; i<this.getSegmentsSize(); ++i){
            boolean match = true;
            for(int j=0; j<path.getSegmentsSize(); ++j){
                if(path.getSegment(j).getTurnState() != this.getSegment(i+j).getTurnState()){
                    match = false;
                    break;
                }
            }
            if(!match) continue;
            if (offset > -1) { // found twice. not only one
                return -1;
            }
            offset = ((i + path.getSegmentsSize()) % this.getSegmentsSize());
        }

        return offset;
    }
}

//12195634
