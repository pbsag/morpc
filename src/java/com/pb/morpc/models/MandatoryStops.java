package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */


import com.pb.morpc.structures.TourType;
import com.pb.morpc.structures.Household;

import java.util.HashMap;


public class MandatoryStops implements java.io.Serializable {

	
	private StopsHousehold stopsHH;
	private HouseholdArrayManager hhMgr;

	private HashMap propertyMap;
	
	

	
	
    public MandatoryStops (HashMap propertyMap, HouseholdArrayManager hhMgr) {

		this.hhMgr = hhMgr;
		this.stopsHH = new StopsHousehold ( propertyMap, TourType.MANDATORY_CATEGORY, TourType.MANDATORY_TYPES );
			
    }
    


	public void doSfcSlcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
		for (int i=0; i < hhList.length; i++) {
			stopsHH.mandatoryTourSfcSlc ( hhList[i] );
		}

		hhMgr.sendResults ( hhList );
	}


	
	public void doSmcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
		for (int i=0; i < hhList.length; i++) {
			stopsHH.mandatoryTourSmc ( hhList[i] );
		}

		hhMgr.sendResults ( hhList );
	}


	
	public void resetHouseholdCount () {
		stopsHH.resetHouseholdCount();
	}
	

	public void printTimes ( short tourTypeCategory ) {
		
		stopsHH.printTimes( tourTypeCategory );
							
	}

	
	
}
