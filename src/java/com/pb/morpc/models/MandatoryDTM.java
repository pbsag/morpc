package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */


import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.*;

import java.util.HashMap;
import java.util.logging.Logger;


public class MandatoryDTM implements java.io.Serializable {

	protected static Logger logger = Logger.getLogger("com.pb.morpc.models");
	private DTMHousehold dtmHH;
	private HouseholdArrayManager hhMgr;

	private HashMap propertyMap;
	
	
	
    public MandatoryDTM (HashMap propertyMap, HouseholdArrayManager hhMgr, ZonalDataManager zdm) {
		this.hhMgr = hhMgr;
		dtmHH = new DTMHousehold ( propertyMap, TourType.MANDATORY_CATEGORY, TourType.MANDATORY_TYPES, zdm );
    }
    


	public void doDcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
		dtmHH.clearProbabilitiesMaps( TourType.MANDATORY_TYPES );
		dtmHH.resetHouseholdCount();
		for (int i=0; i < hhList.length; i++) {
			dtmHH.mandatoryTourDc (hhList[i]);
		}

		hhMgr.sendResults ( hhList );
	}
	
	
	
	public void doTcMcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
		dtmHH.resetHouseholdCount();
		for (int i=0; i < hhList.length; i++) {
			dtmHH.mandatoryTourTc (hhList[i]);
			dtmHH.mandatoryTourMc (hhList[i]);
			dtmHH.updateTimeWindows (hhList[i]);
		}

		hhMgr.sendResults ( hhList );
	}
	
	
	
	public void resetHouseholdCount () {
		dtmHH.resetHouseholdCount();
	}
	

	public void setShadowPricingIteration ( int iter ) {
		dtmHH.setShadowPricingIteration( iter );
	}
	

	public void printTimes ( short tourTypeCategory ) {
		dtmHH.printTimes( tourTypeCategory );
	}

	
	
}
