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


public class NonMandatoryDTM implements java.io.Serializable {

	
	HashMap propertyMap;
	
	private DTMHousehold dtmHH;
	private HouseholdArrayManager hhMgr;


	
    public NonMandatoryDTM (HashMap propertyMap, HouseholdArrayManager hhMgr, ZonalDataManager zdm) {

		this.propertyMap = propertyMap;
		this.hhMgr = hhMgr;
		

		dtmHH = new DTMHousehold (propertyMap, TourType.NON_MANDATORY_CATEGORY, TourType.NON_MANDATORY_TYPES, zdm);
    }
    


	public void doWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
			
		for (int i=0; i < hhList.length; i++) {
			dtmHH.indivNonMandatoryTourDc (hhList[i]);
			dtmHH.indivNonMandatoryTourTc (hhList[i]);
			dtmHH.indivNonMandatoryTourMc (hhList[i]);
		}

		hhMgr.sendResults ( hhList );
	}
	
	
	public void printTimes ( short tourTypeCategory ) {
		
		dtmHH.printTimes( tourTypeCategory );
							
	}

	
	
}
