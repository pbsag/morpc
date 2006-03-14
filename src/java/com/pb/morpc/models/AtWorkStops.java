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


public class AtWorkStops implements java.io.Serializable {

	
	private StopsHousehold stopsHH;
	private HouseholdArrayManager hhMgr;

	
    public AtWorkStops (HashMap propertyMap, HouseholdArrayManager hhMgr) {

		this.hhMgr = hhMgr;

		stopsHH = new StopsHousehold ( propertyMap, TourType.AT_WORK_CATEGORY, TourType.AT_WORK_TYPES );
			
    }
    


	public void doSfcSlcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			

		for (int i=0; i < hhList.length; i++) {
			stopsHH.atWorkTourSfcSlc ( hhList[i] );
		}

		hhMgr.sendResults ( hhList );
	}
	
	
	
	public void doSmcWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			

		for (int i=0; i < hhList.length; i++) {
			stopsHH.atWorkTourSmc ( hhList[i] );
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
