package com.pb.morpc.synpop;


import com.pb.morpc.synpop.pums2000.PUMSHH;


/**
 * HH Income implementation of the PercentageCurve class.
 *
 */

public class HHSizeWorkerLookupTable {

    ZonalData zd;

    public HHSizeWorkerLookupTable (ZonalData zd) {
        this.zd = zd;
    }


    public float[][][] getPercentages () {

        float[][][] props = new float[PUMSHH.HHSIZES_BASE][PUMSHH.WORKERS_BASE][PUMSHH.INCOMES_BASE];

        if (zd.isFranklinCounty()) {

            props[0][0][0] = 0.8468f;
            props[0][0][1] = 0.1460f;
            props[0][0][2] = 0.0072f;
            props[0][1][0] = 0.4369f;
            props[0][1][1] = 0.5399f;
            props[0][1][2] = 0.0232f;
            props[0][2][0] = 0.0000f;
            props[0][2][1] = 0.0000f;
            props[0][2][2] = 0.0000f;
            props[0][3][0] = 0.0000f;
            props[0][3][1] = 0.0000f;
            props[0][3][2] = 0.0000f;

            props[1][0][0] = 0.4811f;
            props[1][0][1] = 0.4821f;
            props[1][0][2] = 0.0368f;
            props[1][1][0] = 0.2782f;
            props[1][1][1] = 0.6393f;
            props[1][1][2] = 0.0825f;
            props[1][2][0] = 0.0978f;
            props[1][2][1] = 0.7442f;
            props[1][2][2] = 0.1580f;
            props[1][3][0] = 0.0000f;
            props[1][3][1] = 0.0000f;
            props[1][3][2] = 0.0000f;

            props[2][0][0] = 0.7407f;
            props[2][0][1] = 0.2421f;
            props[2][0][2] = 0.0172f;
            props[2][1][0] = 0.2808f;
            props[2][1][1] = 0.6496f;
            props[2][1][2] = 0.0696f;
            props[2][2][0] = 0.0725f;
            props[2][2][1] = 0.7500f;
            props[2][2][2] = 0.1771f;
            props[2][3][0] = 0.0455f;
            props[2][3][1] = 0.7181f;
            props[2][3][2] = 0.2364f;

            props[3][0][0] = 0.8991f;
            props[3][0][1] = 0.0879f;
            props[3][0][2] = 0.0130f;
            props[3][1][0] = 0.1964f;
            props[3][1][1] = 0.6802f;
            props[3][1][2] = 0.1234f;
            props[3][2][0] = 0.0595f;
            props[3][2][1] = 0.7663f;
            props[3][2][2] = 0.1742f;
            props[3][3][0] = 0.0355f;
            props[3][3][1] = 0.6882f;
            props[3][3][2] = 0.2763f;

            props[4][0][0] = 0.9384f;
            props[4][0][1] = 0.0468f;
            props[4][0][2] = 0.0148f;
            props[4][1][0] = 0.2044f;
            props[4][1][1] = 0.6263f;
            props[4][1][2] = 0.1693f;
            props[4][2][0] = 0.0544f;
            props[4][2][1] = 0.7535f;
            props[4][2][2] = 0.1921f;
            props[4][3][0] = 0.0063f;
            props[4][3][1] = 0.6651f;
            props[4][3][2] = 0.3286f;

            props[5][0][0] = 0.8506f;
            props[5][0][1] = 0.1494f;
            props[5][0][2] = 0.0000f;
            props[5][1][0] = 0.3198f;
            props[5][1][1] = 0.6095f;
            props[5][1][2] = 0.0707f;
            props[5][2][0] = 0.0800f;
            props[5][2][1] = 0.8025f;
            props[5][2][2] = 0.1175f;
            props[5][3][0] = 0.0403f;
            props[5][3][1] = 0.6260f;
            props[5][3][2] = 0.3337f;

        }
        else {

            props[0][0][0] = 0.8966f;
            props[0][0][1] = 0.1007f;
            props[0][0][2] = 0.0027f;
            props[0][1][0] = 0.5064f;
            props[0][1][1] = 0.4809f;
            props[0][1][2] = 0.0127f;
            props[0][2][0] = 0.0000f;
            props[0][2][1] = 0.0000f;
            props[0][2][2] = 0.0000f;
            props[0][3][0] = 0.0000f;
            props[0][3][1] = 0.0000f;
            props[0][3][2] = 0.0000f;

            props[1][0][0] = 0.5459f;
            props[1][0][1] = 0.4355f;
            props[1][0][2] = 0.0186f;
            props[1][1][0] = 0.3034f;
            props[1][1][1] = 0.6285f;
            props[1][1][2] = 0.0681f;
            props[1][2][0] = 0.1002f;
            props[1][2][1] = 0.7624f;
            props[1][2][2] = 0.1374f;
            props[1][3][0] = 0.0000f;
            props[1][3][1] = 0.0000f;
            props[1][3][2] = 0.0000f;

            props[2][0][0] = 0.7011f;
            props[2][0][1] = 0.2811f;
            props[2][0][2] = 0.0178f;
            props[2][1][0] = 0.2915f;
            props[2][1][1] = 0.6545f;
            props[2][1][2] = 0.0540f;
            props[2][2][0] = 0.0735f;
            props[2][2][1] = 0.8115f;
            props[2][2][2] = 0.1150f;
            props[2][3][0] = 0.0331f;
            props[2][3][1] = 0.7472f;
            props[2][3][2] = 0.2197f;

            props[3][0][0] = 0.7961f;
            props[3][0][1] = 0.1873f;
            props[3][0][2] = 0.0166f;
            props[3][1][0] = 0.2615f;
            props[3][1][1] = 0.6589f;
            props[3][1][2] = 0.0796f;
            props[3][2][0] = 0.0753f;
            props[3][2][1] = 0.7963f;
            props[3][2][2] = 0.1284f;
            props[3][3][0] = 0.0224f;
            props[3][3][1] = 0.6822f;
            props[3][3][2] = 0.2954f;

            props[4][0][0] = 0.8101f;
            props[4][0][1] = 0.1899f;
            props[4][0][2] = 0.0000f;
            props[4][1][0] = 0.1867f;
            props[4][1][1] = 0.7075f;
            props[4][1][2] = 0.1058f;
            props[4][2][0] = 0.0679f;
            props[4][2][1] = 0.8270f;
            props[4][2][2] = 0.1051f;
            props[4][3][0] = 0.0139f;
            props[4][3][1] = 0.7437f;
            props[4][3][2] = 0.2424f;

            props[5][0][0] = 0.6521f;
            props[5][0][1] = 0.3479f;
            props[5][0][2] = 0.0000f;
            props[5][1][0] = 0.3392f;
            props[5][1][1] = 0.6083f;
            props[5][1][2] = 0.0525f;
            props[5][2][0] = 0.1149f;
            props[5][2][1] = 0.8010f;
            props[5][2][2] = 0.0841f;
            props[5][3][0] = 0.0231f;
            props[5][3][1] = 0.7022f;
            props[5][3][2] = 0.2747f;

        }

        return ( props );

    }


}

