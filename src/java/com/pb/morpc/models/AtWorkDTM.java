package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */


import com.pb.morpc.structures.*;

import java.util.HashMap;


public class AtWorkDTM implements java.io.Serializable {

	
	HashMap propertyMap;
	
	private DTMHousehold dtmHH;
	private HouseholdArrayManager hhMgr;


	
    public AtWorkDTM (HashMap propertyMap, HouseholdArrayManager hhMgr) {

		this.propertyMap = propertyMap;
		this.hhMgr = hhMgr;


		dtmHH = new DTMHousehold (propertyMap, TourType.AT_WORK_CATEGORY, TourType.AT_WORK_TYPES);
    }
    


	public void doWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
			
		for (int i=0; i < hhList.length; i++) {
			dtmHH.atWorkTourDc (hhList[i]);
			dtmHH.atWorkTourTc (hhList[i]);
			dtmHH.atWorkTourMc (hhList[i]);
		}
			
		hhMgr.sendResults ( hhList );
	}


	
	public void printTimes ( short tourTypeCategory ) {
		
		dtmHH.printTimes( tourTypeCategory );
							
	}

	
	
}
