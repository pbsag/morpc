package com.pb.morpc.synpop;

/**
 * The PercentageCurve class represents typically one dimensional distributions of Households
 * by some socioeconomic catgories (HH Size, HH Income, HH Workers, etc...)
 *
 */

public abstract class PercentageCurve {

    public PercentageCurve () {
    }
    
    
    public abstract float[] getPercentages (float[] args);
    
    
    public abstract float[] extendPercentages ();

}

