package com.zuehlke.carrera.javapilot.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ValkA on 12-Mar-16.
 */

public class Track extends Path {
    private boolean pathIsReady = false;
    private final int MIN_PATH_SIZE = 7;
    private final int MAX_PATH_SIZE = 25;
    private final int MAX_SEGMENTS = (15*4);

    public Track(){
        super();
    }

    public void addSegment(TurnStateRecognizer.TurnState turnState){
        if(!pathIsReady){
            super.addSegment(turnState);
            pathIsReady = checkDoubleCyclicPath();
        }
        if (list.size() > MAX_PATH_SIZE*2) {
            System.out.println("[Info] Cutting the bulshit ---------------------------------------------------------------");
            System.out.println("before cutting: ");
            System.out.println(list);
            list = list.subList(list.size()-MIN_PATH_SIZE*2, list.size());
            System.out.println(list);
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

    public String toString() {
        String str = "";
        for (int i = 0; i < list.size(); ++i) {
            str += ((i % 16) + ":" + list.get(i) + ",  ");
        }
        return str;
    }

    private boolean checkDoubleCyclicPath() {
        System.out.println(this.toString());
        System.out.println("");

        if(list.size()<(MIN_PATH_SIZE*2) || list.size()%2 != 0){  // remove second condition if using multi role.
            return false;
        }


        /*boolean match = false;
        int listLen = list.size();
        for (int partitions = 2; !match && partitions <= 5; partitions++) {
            if (listLen%partitions != 0) continue;
            int partitionLength = listLen / partitions;
            if (partitionLength < MIN_PATH_SIZE) break;
            int lastPartitionStart = partitionLength * (partitions-1);
            for (int start1 = 0; start1 < lastPartitionStart; start1+=partitionLength) {
                if (checkSameInRanges(start1, lastPartitionStart, partitionLength)) {
                    // match found
                    match = true;
                    List<Segment> newList = new ArrayList<Segment>();
                    for (int i = start1; i < start1+partitionLength; i++) {
                        newList.add(this.list.get(i));
                    }
                    this.list = newList;
                    break;
                }
            }
        }
        if (!match) return false;*/


        for(int i=0; i<list.size()/2; ++i){
            Segment first = list.get(i);
            Segment second = list.get(i+list.size()/2);
            if(first.getTurnState() != second.getTurnState()){
                return false;
            }
        }
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

        System.out.println(this.toString());
        System.out.println(" ");


        return true;
    }

    public boolean checkSameInRanges(int start1, int start2, int len) {
        if (start1+len-1 >= list.size()) return false;
        if (start2+len-1 >= list.size()) return false;
        for (int i = 0; i < len; ++i) {
            Segment first = list.get(start1+i);
            Segment second = list.get(start2+i);
            if (first.getTurnState() != second.getTurnState()) {
                return false;
            }
        }
        return true;
    }

    public void demonstrate(List<TurnStateRecognizer.TurnState> lst) {
        for(TurnStateRecognizer.TurnState trn : lst) {
            list.add(new Segment(trn));
        }
    }


    /*
        -1 if not, else returns last common index (in Track)
     */
    int isOnlySubPath(Path path){
        int offset = -1;
        for(int i=0; i<this.getSegmentsSize(); ++i){
            boolean match = true;
            for(int j=0; j<path.getSegmentsSize(); ++j){
                if(path.getSegment(j).getTurnState() != this.getSegment((i+j)%this.getSegmentsSize()).getTurnState()){
                    match = false;
                    break;
                }
            }
            if(!match) continue;
            if (offset > -1) { // found twice. not only one
                return -1;
            }
            offset = ((i + path.getSegmentsSize() - 1) % this.getSegmentsSize());
        }

        return offset;
    }
}

//12195634
