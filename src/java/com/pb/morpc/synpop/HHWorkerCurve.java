package com.pb.morpc.synpop;


//import java.util.*;
//import java.lang.*;
//import java.text.*;
//import java.io.*;

import com.pb.morpc.synpop.pums2000.PUMSHH;


/**
 * HH Workers implementation of the PercentageCurve class.
 *
 */

public class HHWorkerCurve extends PercentageCurve {

	static final float MIN_VALUE = 0.00001f;
	static final float MAX_VALUE = 1.0f;

    ZonalData zd;

    public HHWorkerCurve (ZonalData zd) {
        this.zd = zd;
    }

    /**
     *  X is the zonal average Workers per Household
     */
    public float[] getPercentages (float[] args) {

    	// apply household workers model to get 0, 1, 2, 3+ worker proportions
        double[] dPcts = new double[PUMSHH.WORKERS_BASE];
        float[] pcts = new float[PUMSHH.WORKERS_BASE];


        // Average zonal workers per household is truncated between 0.4 and 2.5.
        double X = (float)Math.max (0.4, Math.min(2.5,args[0]));
        double Y1 = args[1];
        double Y2 = args[2];


        dPcts[0] = Math.min( Math.max( (0.484 - 0.423*X + 0.098*Math.pow(X,2) + 0.232*Y1 + 0.201*Y2), MIN_VALUE ), MAX_VALUE );

        dPcts[2] = Math.min( Math.max( (0.304 + 0.093*X - 0.391*Y1), MIN_VALUE ), MAX_VALUE );

        dPcts[3] = Math.min( Math.max( (0.186 + 0.0101*X - 0.213*(Y1 + Y2)), MIN_VALUE ), MAX_VALUE );

        dPcts[1] = Math.min( Math.max( (1.0 - (dPcts[0] + dPcts[2] + dPcts[3])), MIN_VALUE ), MAX_VALUE );


		// apply scaling to dPcts[] to get values to sum exactly to 1.0;
		double propTot = 0.0;
		for (int i=0; i < 4; i++)
			propTot += dPcts[i];

		double maxPct = -99999999.9;
		int maxPctIndex = 0;
		for (int i=0; i < 4; i++) {
			dPcts[i] /= propTot;
			if (dPcts[i] > maxPct) {
				maxPct = dPcts[i];
				maxPctIndex = i;
			}
		}

		// calculate the percentage for the maximum index percentage curve from the
		// residual difference.
		double residual = 0.0;
		for (int i=0; i < 4; i++)
			if (i != maxPctIndex)
				residual += dPcts[i];

		dPcts[maxPctIndex] = 1.0 - residual;



        pcts[0] = (float)dPcts[0];
        pcts[1] = (float)dPcts[1];
        pcts[2] = (float)dPcts[2];
        pcts[3] = (float)dPcts[3];


        return ( pcts );
    }



    public float[] extendPercentages () {

		// split 3+ category into 3, 4, 5+
        float[] extProps = new float[PUMSHH.WORKERS - PUMSHH.WORKERS_BASE + 1];


        if (zd.isFranklinCounty()) {

            // Franklin County, Area Type 1-7: WORKERS 3
            extProps[0] = 0.826f;

            // Franklin County, Area Type 1-7: WORKERS 4
            extProps[1] = 0.142f;

            // Franklin County, Area Type 1-7: WORKERS 5+
            extProps[2] = 0.032f;

        }
        else {

            // Non-Franklin County, Area Type 1-7: WORKERS 3
            extProps[0] = 0.824f;

            // Non-Franklin County, Area Type 1-7: WORKERS 4
            extProps[1] = 0.158f;

            // Non-Franklin County, Area Type 1-7: WORKERS 5+
            extProps[2] = 0.018f;

        }


        return ( extProps );
    }

}

