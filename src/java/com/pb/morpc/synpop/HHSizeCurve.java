package com.pb.morpc.synpop;


import com.pb.common.math.MathUtil;
import com.pb.morpc.synpop.pums2000.PUMSHH;


/**
 * HH Income implementation of the PercentageCurve class.
 *
 */

public class HHSizeCurve extends PercentageCurve {

	static final float MIN_VALUE = 0.00001f;
	static final float MAX_VALUE = 1.0f;

    ZonalData zd;

    public HHSizeCurve (ZonalData zd) {
        this.zd = zd;
    }

    /**
     *  X is the zonal average household size
     */
    public float[] getPercentages (float[] args) {

        double a, b, c, d;
        double[] dProps = new double[PUMSHH.HHSIZES_BASE];
        float[] props = new float[PUMSHH.HHSIZES_BASE];


        float X = args[0];

        if (zd.getAreaType() < 5) {

            // Area Type 1-4: HHSIZE 1
            a = -0.48374024;
            b = -1.04707510;
            c = -0.37860791;
            dProps[0] = Math.min( Math.max( (a/(1.0 + b*MathUtil.exp(-c*X))) + (0.2*Math.max(0.0,(1.43-X))), MIN_VALUE ), MAX_VALUE );

            // Area Type 1-4: HHSIZE 3
            a = -0.19999500;
            b =  1.69739700;
            c =  0.34822357;
            d =  1.38263280;
            dProps[2] = Math.min( Math.max( ((a*b + c*Math.pow(X,d))/(b + Math.pow(X,d))) - 0.0044*Math.max(0.0,(1.7-X)), MIN_VALUE ), MAX_VALUE );

            // Area Type 1-4: HHSIZE 4
            a =  0.014256312;
            b =  41.07246100;
            c =  0.217218820;
            d =  4.525472900;
            dProps[3] = Math.min( Math.max( ((a*b + c*Math.pow(X,d))/(b + Math.pow(X,d))) - 0.027*Math.max(0.0,(1.7-X)), MIN_VALUE ), MAX_VALUE );

            // Area Type 1-4: HHSIZE 5
            a =  0.015607181;
            b =  631.5075600;
            c =  0.133933750;
            d =  6.680459900;
            dProps[4] = Math.min( Math.max( ((a*b + c*Math.pow(X,d))/(b + Math.pow(X,d))) - 0.023*Math.max(0.0,(1.7-X)), MIN_VALUE ), MAX_VALUE );

            // Area Type 1-4: HHSIZE 6+
            a =  0.0090866386;
            b =  8022.3720000;
            c =  0.0837475320;
            d =  8.9739066000;
            dProps[5] = Math.min( Math.max( ((a*b + c*Math.pow(X,d))/(b + Math.pow(X,d))) - 0.013*Math.max(0.0,(1.7-X)), MIN_VALUE ), MAX_VALUE );

            // Area Type 1-4: HHSIZE 2
            dProps[1] = Math.min( Math.max( (1.0 - dProps[0] - dProps[2] - dProps[3] - dProps[4] - dProps[5] ), MIN_VALUE ), MAX_VALUE );

        }
        else {

            // Area Type 5-7: HHSIZE 1
            a = -0.1997589600;
            b =  1.0324601000;
            dProps[0] = Math.min( Math.max( ( a + b/X ), 0.0 ), 1.0 );

            // Area Type 5-7: HHSIZE 3
            a =  0.2765693800;
            b = -0.2538669600;
            dProps[2] = Math.min( Math.max( ( a + b/X ), 0.0 ), MAX_VALUE );

            // Area Type 5-7: HHSIZE 4
            a =  0.0741856660;
            b =  213.49900000;
            c =  0.8864074800;
            d =  3.3624530000;
            dProps[3] = Math.min( Math.max( ( (a*b + c*Math.pow(X,d))/(b + Math.pow(X,d)) ), MIN_VALUE ), MAX_VALUE );

            // Area Type 5-7: HHSIZE 5
            a = -0.0245457980;
            b =  0.0660512520;
            c = -0.0320018440;
            d =  0.0074622636;
            dProps[4] = Math.min( Math.max( ( a + b*X + c*Math.pow(X,2) + d*Math.pow(X,3) ), MIN_VALUE ), MAX_VALUE );

            // Area Type 5-7: HHSIZE 6+
            a = -0.1206624900;
            b =  0.1466708900;
            c = -0.0538144750;
            d =  0.0073420860;
            dProps[5] = Math.min( Math.max( ( a + b*X + c*Math.pow(X,2) + d*Math.pow(X,3) ), MIN_VALUE ), MAX_VALUE );

            // Area Type 5-7: HHSIZE 2
            dProps[1] = Math.min( Math.max( (1.0 - dProps[0] - dProps[2] - dProps[3] - dProps[4] - dProps[5] ), MIN_VALUE ), MAX_VALUE );

        }


		// apply scaling to dProps[] to get values to sum exactly to 1.0;
		double propTot = 0.0;
		for (int i=0; i < 6; i++)
			propTot += dProps[i];

		double maxPct = -99999999.9;
		int maxPctIndex = 0;
		for (int i=0; i < 6; i++) {
			dProps[i] /= propTot;
			if (dProps[i] > maxPct) {
				maxPct = dProps[i];
				maxPctIndex = i;
			}
		}

		// calculate the percentage for the maximum index percentage curve from the
		// residual difference.
		double residual = 0.0;
		for (int i=0; i < 6; i++)
			if (i != maxPctIndex)
				residual += dProps[i];

		dProps[maxPctIndex] = 1.0 - residual;



        props[0] = (float)dProps[0];
        props[1] = (float)dProps[1];
        props[2] = (float)dProps[2];
        props[3] = (float)dProps[3];
        props[4] = (float)dProps[4];
        props[5] = (float)dProps[5];

        return ( props );
    }



    public float[] extendPercentages () {

        float[] extProps = new float[PUMSHH.HHSIZES - PUMSHH.HHSIZES_BASE + 1];

        if (zd.isFranklinCounty()) {

            // Franklin County, Area Type 1-7: HHSIZE 6
            extProps[0] = 0.614f;

            // Franklin County, Area Type 1-7: HHSIZE 7
            extProps[1] = 0.254f;

            // Franklin County, Area Type 1-7: HHSIZE 8
            extProps[2] = 0.087f;

            // Franklin County, Area Type 1-7: HHSIZE 9+
            extProps[3] = 0.045f;
        }
        else {

            // Non-Franklin County, Area Type 1-7: HHSIZE 6
            extProps[0] = 0.703f;

            // Non-Franklin County, Area Type 1-7: HHSIZE 7
            extProps[1] = 0.231f;

            // Non-Franklin County, Area Type 1-7: HHSIZE 8
            extProps[2] = 0.039f;

            // Non-Franklin County, Area Type 1-7: HHSIZE 9+
            extProps[3] = 0.027f;
        }

        return ( extProps );

    }


}

