package com.pb.morpc.synpop;


import com.pb.morpc.synpop.pums2000.PUMSHH;

/**
 * HH Income implementation of the PercentageCurve class.
 *
 */

public class HHIncomeCurve extends PercentageCurve {

	static final double MIN_VALUE = 0.2;
	static final double MAX_VALUE = 2.5;

    ZonalData zd;

    public HHIncomeCurve (ZonalData zd) {
        this.zd = zd;
    }

    /**
     *  X is the zonal average Income per Household
     */
    public float[] getPercentages (float[] args) {

        double[] dPcts = new double[PUMSHH.INCOMES_BASE];
        float[] pcts = new float[PUMSHH.INCOMES_BASE];

        double X = Math.min( MAX_VALUE, ( Math.max( MIN_VALUE, args[0] ) ) );

        dPcts[0] = Math.max ( 0.00001, Math.min (1.0, 1.26 - 1.25*X + 0.32*Math.pow(X,2) ) );

        dPcts[2] = Math.max ( 0.00001, Math.min (1.0, -0.108 + 0.207*X + 0.047*Math.pow(X,2) ) );

        dPcts[1] = 1.0 - (dPcts[0] + dPcts[2]);



		// apply scaling to dPcts[] to get values to sum exactly to 1.0;
		double propTot = 0.0;
		for (int i=0; i < 3; i++)
			propTot += dPcts[i];

		double maxPct = -99999999.9;
		int maxPctIndex = 0;
		for (int i=0; i < 3; i++) {
			dPcts[i] /= propTot;
			if (dPcts[i] > maxPct) {
				maxPct = dPcts[i];
				maxPctIndex = i;
			}
		}

		// calculate the percentage for the maximum index percentage curve from the
		// residual difference.
		double residual = 0.0;
		for (int i=0; i < 3; i++)
			if (i != maxPctIndex)
				residual += dPcts[i];

		dPcts[maxPctIndex] = 1.0 - residual;



	    pcts[0] = (float)dPcts[0];
	    pcts[1] = (float)dPcts[1];
	    pcts[2] = (float)dPcts[2];

        return ( pcts );
    }



    public float[] extendPercentages () {

        float[] extProps = new float[1];
        extProps[0] = 1.0f;

        return ( extProps );
    }


}

