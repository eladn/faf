package com.zuehlke.carrera.javapilot.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.zuehlke.carrera.javapilot.Utils.TurnStateRecognizer;
import com.zuehlke.carrera.relayapi.messages.PenaltyMessage;
import com.zuehlke.carrera.relayapi.messages.RaceStartMessage;
import com.zuehlke.carrera.relayapi.messages.SensorEvent;
import com.zuehlke.carrera.timeseries.FloatingHistory;
import org.apache.commons.lang.StringUtils;

/**
 *  this logic node increases the power level by 10 units per 0.5 second until it receives a penalty
 *  then reduces by ten units.
 */
public class KarnafimActor extends UntypedActor {
    public enum LaunchStage {
        BuildPath, DetectMaxLimits, OptimumLaunch
    };
    private int TURN_STATE_THRESHOLD = 300;
    TurnStateRecognizer turnStateRecognizer = new TurnStateRecognizer(TURN_STATE_THRESHOLD);

    private final ActorRef kobayashi;
    private double currentPower = 200;
    private long lastIncrease = 0;
    private int maxPower = 180; // Max for this phase;
    private boolean probing = false;
    private FloatingHistory gyrozHistory = new FloatingHistory(5);
    private LaunchStage stage = BuildPath;

    /**
     * @param pilotActor The central pilot actor
     * @param duration the period between two increases
     * @return the actor props
     */
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


    @Override
    public void onReceive(Object message) throws Exception {
        switch(stage) {
            case BuildPath:
                onReceive_BuildPath(message);
                break;
            case DetectMaxLimits:
                break;
            case OptimumLaunch:
                break;
        }

    }

    private void onReceive_BuildPath(Object message) throws Exception {
        if ( message instanceof SensorEvent ) {
            handleSensorEvent((SensorEvent) message);

        } else if ( message instanceof PenaltyMessage) {
            handlePenaltyMessage ();

        } else if ( message instanceof RaceStartMessage) {
            handleRaceStart();

        } else {
            unhandled(message);
        }
    }

    private void handleRaceStart() {
        currentPower = 200;
        lastIncrease = 0;
        maxPower = 180; // Max for this phase;
        probing = false;
        gyrozHistory = new FloatingHistory(5);
        turnStateRecognizer = new TurnStateRecognizer(TURN_STATE_THRESHOLD);
    }

    private void handlePenaltyMessage() {
        currentPower -= 10;
        kobayashi.tell(new PowerAction((int)currentPower), getSelf());
        probing = false;
    }

    /**
     * Strategy: increase quickly when standing still to overcome haptic friction
     * then increase slowly. Probing phase will be ended by the first penalty
     * @param message the sensor event coming in
     */
    private void handleSensorEvent(SensorEvent message) {

        double gyrz = gyrozHistory.shift(message.getG()[2]);
        double avg = gyrozHistory.currentMean();
        if(turnStateRecognizer.newInput(avg)){
            System.out.print(turnStateRecognizer.getLastStateDuration());
            System.out.print(" ---------------------------------------------------=======");
            System.out.print(turnStateRecognizer.getCurrentTurnState());
            System.out.println("======--------------------------------------------------------");
        }

        show ((int)gyrozHistory.currentMean());


        if (probing) {
            if (iAmStillStanding()) {
                increase(0.5);
            } else if (message.getTimeStamp() > lastIncrease + duration) {
                lastIncrease = message.getTimeStamp();
                increase(3);
            }
        }

        kobayashi.tell(new PowerAction((int)currentPower), getSelf());
    }

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


}
