package com.pb.morpc.models;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.morpc.structures.MessageWindow;
import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.PersonType;
import com.pb.morpc.structures.Person;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;



/**
 * Implements a test of the multinomial logit model for choice
 * 
 * @author    Jim Hicks
 * @version   1.0, 3/10/2003
 *
 */
public class FreeParkingEligibility {

	static final int M5_DATA_SHEET = 0;
	static final int M5_MODEL_SHEET = 1;

    static Logger logger = Logger.getLogger("com.pb.morpc.models");

    
	HashMap propertyMap;
	boolean useMessageWindow = false;
	MessageWindow mw;
	

	
	public FreeParkingEligibility ( HashMap propertyMap ) {

		this.propertyMap = propertyMap;
		
		// get the indicator for whether to use the message window or not
		// from the properties file.
		String useMessageWindowString = (String)propertyMap.get( "MessageWindow" );
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				useMessageWindow = true;
				this.mw = new MessageWindow ( "MORPC FreeParkingEligibility Choice Model Run Time Information" ); 
			}
		}

    }

    

	public void runFreeParkingEligibility ( Household[] hh ) {

		int hh_id;
		int hh_taz_id;

		IndexValues index = new IndexValues();
		int[] utilityAvailability = null;

	

		if (useMessageWindow) mw.setMessage1 ("Starting Model 5 -- Free Parking Eligibility");
		if (useMessageWindow) mw.setMessage2 ("Building an expression calculator");


        //open files  
        String controlFile = (String)propertyMap.get( "Model5.controlFile" );
        String outputFile = (String)propertyMap.get( "Model5.outputFile" );

 

        // create a new UEC to get utilties for this logit model
        UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), M5_MODEL_SHEET, M5_DATA_SHEET, propertyMap, Household.class);
        int numberOfAlternatives = uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

        // create and define a new LogitModel object
        LogitModel root= new LogitModel("root", numberOfAlternatives);
        ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];


        for(int i=0;i<numberOfAlternatives;i++){
            logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
            alts[i]  = new ConcreteAlternative(alternativeNames[i], new Integer(i+1));
	        root.addAlternative (alts[i]);
            logger.debug(alternativeNames[i]+" has been added to the root");
        }

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();


		utilityAvailability = new int[numberOfAlternatives+1];
		Arrays.fill (utilityAvailability, 1);

		index.setOriginZone( 0 );
		index.setDestZone( 0 );

		// loop over all households in the hh table
		for (int h=0; h < hh.length; h++) {

			if (useMessageWindow) mw.setMessage2 ("Model5 Choice for hh " + (h+1) + " of " + hh.length );

			hh_id = hh[h].getID();
			hh_taz_id = hh[h].getTazID();

			// get person array for this household.
			Person[] persons = hh[h].getPersonArray();

			// loop over individual tours array for the hh
			for (int p=1; p < persons.length; p++) {

				// this model only applies to adults (personTypes=1,2,3,4).
				int personType = persons[p].getPersonType();
				if (personType != PersonType.WORKER_F && personType != PersonType.WORKER_P
					&& personType != PersonType.STUDENT && personType != PersonType.NONWORKER)
						continue;

				hh[h].setPersonID( p );
				
				// get utilities for each alternative for this household
				index.setZoneIndex(hh_taz_id);
				index.setHHIndex(hh_id);
				double[] utilities = uec.solve( index, hh[h], utilityAvailability);

				//set utility for each alternative
				for(int a=0;a < numberOfAlternatives;a++){
					alts[a].setAvailability( utilityAvailability[a+1] == 1 );
					if (utilityAvailability[a+1] == 1)
						alts[a].setAvailability( (utilities[a] > -99.0) );
					alts[a].setUtility(utilities[a]);
				}
				// set availabilities
				root.computeAvailabilities();


				root.getUtility();
				root.calculateProbabilities();


				ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
				String chosenAltName= chosen.getName();

				// find index of the chosen alternative
				int alt=0;
				for(int a=0;a < numberOfAlternatives;a++){
					if (chosenAltName.equals(alternativeNames[a])) {
						alt = a+1;
						break;
					}
				}

				// set the result of this choice in the person object
				persons[p].setFreeParking( alt );

			}//end for persons

		}//next household

		if (useMessageWindow) mw.setVisible(false);
	}

}