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


public class NonMandatoryStops implements java.io.Serializable {

	
	private StopsHousehold stopsHH;
	private HouseholdArrayManager hhMgr;

	private HashMap propertyMap;
	
	
	
    public NonMandatoryStops (HashMap propertyMap, HouseholdArrayManager hhMgr) {


		this.hhMgr = hhMgr;

		stopsHH = new StopsHousehold ( propertyMap, TourType.NON_MANDATORY_CATEGORY, TourType.NON_MANDATORY_TYPES );
			
    }
    


	public void doSfcSlcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
		for (int i=0; i < hhList.length; i++) {
			stopsHH.nonMandatoryTourSfcSlc ( hhList[i] );
		}

		hhMgr.sendResults ( hhList );
	}
	
	
	
	public void doSmcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
		for (int i=0; i < hhList.length; i++) {
			stopsHH.nonMandatoryTourSmc ( hhList[i] );
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
