package com.zuehlke.carrera.javapilot.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.zuehlke.carrera.javapilot.Utils.PathRecognizer;
import com.zuehlke.carrera.javapilot.Utils.Segment;
import com.zuehlke.carrera.javapilot.Utils.Track;
import com.zuehlke.carrera.javapilot.Utils.TurnStateRecognizer;
import com.zuehlke.carrera.relayapi.messages.PenaltyMessage;
import com.zuehlke.carrera.relayapi.messages.RaceStartMessage;
import com.zuehlke.carrera.relayapi.messages.SensorEvent;
import com.zuehlke.carrera.relayapi.messages.VelocityMessage;
import com.zuehlke.carrera.timeseries.FloatingHistory;
import org.apache.commons.lang.StringUtils;

/**
 *  this logic node increases the power level by 10 units per 0.5 second until it receives a penalty
 *  then reduces by ten units.
 */
public class KarnafimActor extends UntypedActor {
    public enum LaunchStage {
        BuildPath, DetectMaxLimits, OptimumLaunch, FindMe
    }

    private int TURN_STATE_THRESHOLD = 300;
    private int FLOATING_HISTORY = 5;
    private int INIT_POWER = 105;

    TurnStateRecognizer turnStateRecognizer = new TurnStateRecognizer(TURN_STATE_THRESHOLD);
    Track track = new Track();
    PathRecognizer pathRecognizer = null;
    private FloatingHistory gyrozHistory = new FloatingHistory(FLOATING_HISTORY);
    private final ActorRef kobayashi;
    private LaunchStage stage = LaunchStage.BuildPath;


    private double brakepower = 110;
    private double fullThrottle = 160;
    private int maxPower = 180; // Max for this phase;


    private Segment oldest_unclosed = null;
    private Segment last_closed_segment = null;
    private double currentPower = 100;
    private long lastIncrease = 0;
    private boolean stopped = false;


    private long lastThrottleStart = System.currentTimeMillis();
    private int lastThrottleInterval = 500;
    private double previousVelocity = 0;

    //private int optimumSegmentsCount = 0; // for debug


    public static Props props( ActorRef pilotActor, int duration ) {
        return Props.create(
                KarnafimActor.class, () -> new KarnafimActor(pilotActor, duration ));
    }
    private final int duration;
    public KarnafimActor(ActorRef pilotActor, int duration) {
        lastIncrease = System.currentTimeMillis();
        this.kobayashi = pilotActor;
        this.duration = duration;
    }

//int buildCounter = 0; // test the BulshitCutter
    @Override
    public void onReceive(Object message) throws Exception {
        switch(stage) {
            case BuildPath:
                onReceive_BuildPath(message);
                if (track.isReady()){
                    stage = LaunchStage.OptimumLaunch;
                    pathRecognizer = new PathRecognizer(track);
                }
                // test the BulshitCutter
                /*if (buildCounter == 11 || buildCounter == 42) { // simulate LOST - REMOVE
                    buildCounter++;
                    track.addSegment(TurnStateRecognizer.TurnState.Left);
                    track.addSegment(TurnStateRecognizer.TurnState.Left);
                }*/
                break;
            case OptimumLaunch:
                onReceive_Optimizer(message);
                /**if (optimumSegmentsCount == 4) { // simulate LOST - REMOVE
                    optimumSegmentsCount = 0;
                    pathRecognizer.setNextState(TurnStateRecognizer.TurnState.Left);
                    pathRecognizer.setNextState(TurnStateRecognizer.TurnState.Left);
                }**/
                if (pathRecognizer.isLost()) stage = LaunchStage.FindMe;
                break;
            case FindMe:
                onReceive_FindMe(message);
                if (!pathRecognizer.isLost()) stage = LaunchStage.OptimumLaunch;
                break;
        }



    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// BuildPath Handlers /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void onReceive_BuildPath(Object message) throws Exception {
        if ( message instanceof SensorEvent ) {
            handleSensorEvent_BuildPath((SensorEvent) message);
        } else if ( message instanceof PenaltyMessage) {
            handlePenaltyMessage_BuildPath();
        } else if ( message instanceof RaceStartMessage) {
            handleRaceStart_BuildPath();
        } else if (message instanceof VelocityMessage) {
            handleVelocityMessage_BuildPath((VelocityMessage) message);
        } else {
            unhandled(message);
        }
    }

    private void handleRaceStart_BuildPath() {
        currentPower = INIT_POWER;
        lastIncrease = 0;
        maxPower = 180; // Max for this phase;
        gyrozHistory = new FloatingHistory(FLOATING_HISTORY);
        turnStateRecognizer = new TurnStateRecognizer(TURN_STATE_THRESHOLD);
        track = new Track();
    }

    private void handleVelocityMessage_BuildPath(VelocityMessage message) {
//        System.out.print("VELOCITY = " + message.getVelocity());
    }

    private void handlePenaltyMessage_BuildPath() {
        currentPower -= 10;
        kobayashi.tell(new PowerAction((int)currentPower), getSelf());
    }

    private void handleSensorEvent_BuildPath(SensorEvent message) {
        gyrozHistory.shift(message.getG()[2]); //push to avg history
        double avg = gyrozHistory.currentMean();
        if(turnStateRecognizer.newInput(avg)){
            //buildCounter++; // cutting the bullshit testing
            if(track.getSegmentsSize()>0){//mikre kaze zain athala
                track.setLastSegmentSharpness(turnStateRecognizer.getLastPeak());
                track.setLastSegmentInitDuraion(turnStateRecognizer.getLastStateDuration());
                track.setLastSegmentClockCounter(turnStateRecognizer.getLastStateClockCounter());
            }
            track.addSegment(turnStateRecognizer.getCurrentTurnState());
//            System.out.print(turnStateRecognizer.getLastStateDuration());
//            System.out.print(turnStateRecognizer.getCurrentTurnState() + " ---------------------------------------------------=============--------------------------------------------------------");
        }
        kobayashi.tell(new PowerAction((int)INIT_POWER), getSelf());
        show ((int)gyrozHistory.currentMean());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// OptimumLaunch Handlers /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void onReceive_Optimizer(Object message) throws Exception {
        if ( message instanceof SensorEvent ) {
            handleSensorEvent_Optimizer((SensorEvent) message);
        } else if ( message instanceof PenaltyMessage) {
            handlePenaltyMessage_Optimizer((PenaltyMessage)message);
        } else if ( message instanceof RaceStartMessage) {
            handleRaceStart_Optimizer();
        } else if (message instanceof VelocityMessage) {
            handleVelocityMessage_Optimizer((VelocityMessage) message);
        } else {
            unhandled(message);
        }
    }

    private void handleRaceStart_Optimizer() {
        System.out.println("[START] from Optimizer");
    }

    private void handleVelocityMessage_Optimizer(VelocityMessage message) {
        System.out.println("SPEED:\t"+message.getVelocity());
        //Save velocity
        previousVelocity = message.getVelocity();
        //Record new data
        if (oldest_unclosed != null) {
            System.out.println("##############################################################################");
            oldest_unclosed.recordNewData(lastThrottleInterval, previousVelocity, stopped);
            last_closed_segment = oldest_unclosed;
            oldest_unclosed = null;
            stopped = false;
        }
    }

    private void handlePenaltyMessage_Optimizer(PenaltyMessage msg) {
        System.out.println("******** PENALTY *********");
        if (oldest_unclosed != null){
            oldest_unclosed.penalize(msg);
        } else if (last_closed_segment!=null){
            last_closed_segment.penalize(msg);
        }
    }
    boolean isFirst=true;
    private void handleSensorEvent_Optimizer(SensorEvent message) {
        //show2 ((int)gyrozHistory.currentMean(), (int)gyrozHistory.currentStDev());

        if(isFirst){
            handleNewSegment(pathRecognizer.getCurrentStateSegment());
            isFirst=false;
        }
        double gyrz = gyrozHistory.shift(message.getG()[2]);
        double avg = gyrozHistory.currentMean();
        if(turnStateRecognizer.newInput(avg)){
            // state changed
            pathRecognizer.setNextState(turnStateRecognizer.getCurrentTurnState());

            //optimumSegmentsCount++; // DEBUG - remove

            System.out.println();
            System.out.println(pathRecognizer.toString());
            //System.out.println(pathRecognizer.getCurrentStateSegment().getTurnState());
            handleNewSegment(pathRecognizer.getCurrentStateSegment());
//            System.out.print(turnStateRecognizer.getLastStateDuration());
//            System.out.print(turnStateRecognizer.getCurrentTurnState() + " ---------------------------------------------------=============--------------------------------------------------------");
        }
        if((lastThrottleStart + lastThrottleInterval) < System.currentTimeMillis()) {
            // If I exceeded throttle time, break!
            kobayashi.tell(new PowerAction((int)brakepower), getSelf());
        }
        if (iAmStillStanding()) {
            kobayashi.tell(new PowerAction((int)fullThrottle), getSelf());
            lastThrottleStart=System.currentTimeMillis();
            lastThrottleInterval=1250;
            stopped=true;
        }
        show ((int)gyrozHistory.currentMean());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// adam & ron /////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void handleNewSegment(Segment seg) {
        //System.out.println("TYPE:\t"+seg.getTurnState()+"\tPENALIZED:\t"+seg.isPenalized()+"\tTOP_SPEED:\t"+seg.getTopSpeed()+"\tTARGET_SPEED:\t"+seg.getTargetSpeed());
        //System.out.println("STEP:\t"+seg.getStep());

        if (oldest_unclosed == null) {
            oldest_unclosed = seg;
            int throttle_time = seg.getThrottleTime(this.previousVelocity);
            int throttle_power = seg.get_max_power();
//            System.out.println("HANDLE NEW SEGMENT:\t"+"power="+throttle_power+"; time="+throttle_time);
            accelerate(throttle_power, throttle_time);
        } else {
        }
    }

    private void accelerate(int throttle_power, int throttle_time) {
        kobayashi.tell(new PowerAction(throttle_power), getSelf());
        lastThrottleStart = System.currentTimeMillis();
        lastThrottleInterval = throttle_time;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// FindMe Handlers /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void onReceive_FindMe(Object message) throws Exception {
        if ( message instanceof SensorEvent ) {
            handleSensorEvent_FindMe((SensorEvent) message);
        } else if ( message instanceof PenaltyMessage) {
            handlePenaltyMessage_FindMe();
        } else if ( message instanceof RaceStartMessage) {
            handleRaceStart_FindMe();
        } else {
            unhandled(message);
        }
    }

    private void handleRaceStart_FindMe() {
        currentPower = INIT_POWER;
        lastIncrease = 0;
        maxPower = 180; // Max for this phase;
    }


    private void handlePenaltyMessage_FindMe() {
        currentPower -= 10;
        kobayashi.tell(new PowerAction((int)currentPower), getSelf());
    }

    private void handleSensorEvent_FindMe(SensorEvent message) {
        gyrozHistory.shift(message.getG()[2]); //push to avg history
        double avg = gyrozHistory.currentMean();
        if(turnStateRecognizer.newInput(avg)){
            // state changed
            pathRecognizer.setNextState(turnStateRecognizer.getCurrentTurnState());
        }
        kobayashi.tell(new PowerAction((int)INIT_POWER), getSelf());
        show ((int)gyrozHistory.currentMean());
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// more bullshit //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private int increase ( double val ) {
        currentPower = Math.min ( currentPower + val, maxPower );
        return (int)currentPower;
    }

    private boolean iAmStillStanding() {
        return gyrozHistory.currentStDev() < 3;
    }

    private void show(int gyr2) {
        int scale = 120 * (gyr2 - (-10000) ) / 20000;
        System.out.println(StringUtils.repeat(" ", scale) + gyr2);
    }

    private void show2(int gyr2, int var2) {
        int scale = 120 * (gyr2 - (-10000) ) / 20000;
        System.out.print(StringUtils.repeat(" ", scale) + gyr2 + ", " + var2);
        if(var2<3) System.out.print("***");
        System.out.println();
    }


}
