package com.zuehlke.carrera.javapilot.Utils;
import com.zuehlke.carrera.relayapi.messages.PenaltyMessage;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Created by ValkA on 12-Mar-16.
 */
public class Segment{
    TurnStateRecognizer.TurnState turnState;

    private static final int power_limit = 180;
    private static final int init_throttle_time=250;
    private static final int max_penalty_speed=300;
    private static final int min_penalty_speed=200;
    private static final int penalties_threshold=2;

    private int max_power = 150;


    private int velocity_step=20;
    private int min_penalized_time = Integer.MAX_VALUE;
    private boolean penalized=false;
    private int number_of_penalties=0;
    private double top_speed;
    private double last_speed;
    private double current_penalty_speed;
    private double sharpness;
    private boolean stopped;
    private int throttle_time;
    private double target_speed;
    private long initDuration;
    private int initClocks;
    SimpleRegression stats;

    public Segment(TurnStateRecognizer.TurnState turnState){
        this.turnState = turnState;
        top_speed = 0.9 * min_penalty_speed;
        target_speed = top_speed + velocity_step;
        last_speed = 0;
        current_penalty_speed = max_penalty_speed;
        sharpness = 0;
        recalcMaxPower();
        stopped=true;
        throttle_time = init_throttle_time;
        stats = new SimpleRegression();
    }

    private void recalcMaxPower() {
        //max_power = (int)(power_limit * (0.5 + 0.5 * sharpness));
        max_power=150;
    }

    public TurnStateRecognizer.TurnState getTurnState() {
        return turnState;
    }

    public double getTopSpeed() {
        return top_speed;
    }

    public int getStep(){
        return velocity_step;
    }

    public double getTargetSpeed(){ return target_speed; }

    public double getSharpness() {
        return sharpness;
    }

    public long getInitDuration() {
        return initDuration;
    }

    public void setInitDuration(long initDuration) {
        this.initDuration = initDuration;
    }

    public void setInitClocks(int initClocks) {
        this.initClocks = initClocks;
    }

    public int getInitClocks() {
        return initClocks;
    }

    public void setSharpness(double sharpness) {
        this.sharpness = sharpness;
        recalcMaxPower();
    }
    public void recordNewData(int throttleTime, double velocityD, boolean stopped){

        if(!penalized){
            throttle_time+=100;
        } else {
            if(throttle_time<0.7*min_penalized_time){
            throttle_time+=Math.min(25,(min_penalized_time-throttle_time)/4);
        }
        }

        top_speed = Math.min(target_speed,max_penalty_speed);
        target_speed=Math.min(top_speed+velocity_step,max_penalty_speed);
        if(!stopped) {
            stats.addData(throttleTime, velocityD);
        }
    }

    public int getThrottleTime(double last_recorded_velocity) {
        //double targetDelta = target_speed - last_recorded_velocity;
        //double slope = stats.getSlope();
        //double intercept = stats.getIntercept();
        //int timeByStat = 0;
       // if (stats.getN() >= 2)
        //    timeByStat = (int) ((targetDelta - intercept) / slope);
       // throttle_time = (stopped || stats.getN() < 2) ? Math.max(timeByStat, throttle_time) : timeByStat;
        return throttle_time;
    }

    public int get_max_power(){
        return max_power;
        //return max_power;
    }

    public void penalize(PenaltyMessage msg) {
        penalized=true;
        number_of_penalties++;
        velocity_step = 0;
        target_speed = 0.95 * msg.getSpeedLimit();
        top_speed = target_speed;
        min_penalized_time=throttle_time;
        throttle_time-=Math.min(throttle_time,(msg.getActualSpeed()-msg.getSpeedLimit())*20);
    }


    @Override
    public String toString(){
//        return "\n" + turnState.toString() + ":mp=" + this.max_power + ":shrp=" + this.sharpness;
//        return turnState.toString();
          return turnState.toString() + ":" + this.getInitDuration() + "";
    }
}