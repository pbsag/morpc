package com.pb.morpc.models;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.Format;
import com.pb.morpc.structures.Household;


import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.HashMap;

/**
 * Provides implementation for the Auto Ownership Model class.
 *
 * @author    Jim Hicks
 * @version   1.0, 3/10/2003
 */

public class AutoOwnershipUEC {

    static Logger logger = Logger.getLogger("com.pb.morpc.models");

	static final int MODEL_1_SHEET = 1;
	static final int DATA_1_SHEET = 0;

	private UtilityExpressionCalculator uec;
    private String controlFileName;
    private boolean debug = false;

	private IndexValues index = new IndexValues();
	private int[] sample;

    public AutoOwnershipUEC(String controlFileName, HashMap proprtyMap) {
        this.controlFileName = controlFileName;
        uec = new UtilityExpressionCalculator ( new File(this.controlFileName), MODEL_1_SHEET, DATA_1_SHEET, proprtyMap, Household.class );
        
        sample = new int[uec.getNumberOfAlternatives()+1];
    }

    public TableDataSet getHouseholdData () {
        return  uec.getHouseholdData ();
    }

    public int getNumberOfAlternatives () {
        return uec.getNumberOfAlternatives();
    }
    
    public void setDebug (boolean debug) {
        this.debug = debug;
    }

    public double[] getUtilities (int hh_taz_id, int hh_id) {


		index.setZoneIndex( hh_taz_id );
		index.setHHIndex( hh_id );

		Arrays.fill(sample, 1);
		double[] utilities = uec.solve( index, new Object(), sample );
	        
	        
        if (debug) {
   			logger.info( "utilities[] array for hh_taz_id=" + hh_taz_id + ", hh_id=" + hh_id );
            logger.info( Format.print("%6s", " ") );
    		for (int c=0; c < utilities.length; c++)
    			logger.info( Format.print("%12s", ("Alt " + (c+1)) ) );
    		logger.info ( "" );
    		
            logger.info( Format.print("%6s", " ") );
       		for (int c=0; c < utilities.length; c++)
       			logger.info( Format.print("%12.5f", utilities[c]) );
       		logger.info ( "" );
            logger.info ( "" );
        }
        
        
        return utilities;
    }

    //A user defined object with public fields that are accessed for values.
    //Note: All fields should be declared double.
    public class DMU {
    	// e.g.:
    	// double schooldriv = 5;
    	
		// DMU not used for auto ownershiop model
    }


}
