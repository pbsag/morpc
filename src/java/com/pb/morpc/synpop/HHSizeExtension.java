package com.pb.morpc.synpop;

import java.io.Serializable;

/**
 * Percentages for extending the HH Size PercentageCurve distribution.
 *
 */

public class HHSizeExtension implements Serializable {

    public static final int NUM_HHSIZE_EXT = 4;
    
    
    public HHSizeExtension () {
    }
    
    
    public float[] getFranklinExtensionPercentages () {
        float[] extProp = new float[NUM_HHSIZE_EXT];
        
        extProp[0] = 0.613f;
        extProp[1] = 0.254f;
        extProp[2] = 0.087f;
        extProp[3] = 0.046f;
        
        return extProp;
    }


    public float[] getNonFranklinExtensionPercentages () {
        float[] extProp = new float[NUM_HHSIZE_EXT];
        
        extProp[0] = 0.703f;
        extProp[1] = 0.231f;
        extProp[2] = 0.039f;
        extProp[3] = 0.027f;
        
        return extProp;
    }

}

