package com.pb.morpc.models;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.Format;
//import com.pb.common.logging.OutputDescription;
import com.pb.morpc.structures.*;
import com.pb.morpc.synpop.SyntheticPopulation;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * @author Jim Hicks
 *
 * Individual Non-Madatory Tour Generation Model
 */
public class IndividualNonMandatoryToursModel {

    static final int M4_DATA_SHEET = 0;
    static final int M41_MODEL_SHEET = 1;
    static final int M42_MODEL_SHEET = 2;
    static final int M431_MODEL_SHEET = 3;
    static final int M432_MODEL_SHEET = 4;
    static final int M433_MODEL_SHEET = 5;
    static final int M44_MODEL_SHEET = 6;

    static Logger logger = Logger.getLogger("com.pb.morpc.models");

    private Household[] hh;

    private IndexValues index = new IndexValues();
    private int[] utilityAvailability = null;

    HashMap propertyMap;
    boolean useMessageWindow = false;
    MessageWindow mw;
    



    public IndividualNonMandatoryToursModel ( HashMap propertyMap, Household[] hh ) {

        this.propertyMap = propertyMap;
        this.hh = hh;

        
        // get the indicator for whether to use the message window or not
        // from the properties file.
        String useMessageWindowString = (String)propertyMap.get( "MessageWindow" );
        if (useMessageWindowString != null) {
            if (useMessageWindowString.equalsIgnoreCase("true")) {
                this.useMessageWindow = true;
                this.mw = new MessageWindow ( "MORPC Individual Non-Mandatory Tour Generation Models (M41-M44) Run Time Information" );
            }
        }


        index.setOriginZone( 0 );
        index.setDestZone( 0 );

    }



    public void runMaintenanceFrequency() {

        int hh_id;
        int hh_taz_id;
        
		int[] choiceFreqs = null;
		float[] tableData = null;
		ArrayList tableHeadings = null;

	        
		PrintWriter outStream = null;


		logger.info ("Starting Model 4.1 -- Individual Maintenance Tour Frequency");
		
		if (useMessageWindow) mw.setMessage1 ("Starting Model 4.1 -- Individual Maintenance Tour Frequency");
        if (useMessageWindow) mw.setMessage2 ("Building an expression calculator");


        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String)propertyMap.get( "Model4.controlFile" );
        String outputFile41 = (String)propertyMap.get( "Model41.outputFile" );
		String summaryRequest = (String)propertyMap.get( "Model41.summaryOutput" );


		boolean summaryOutput = false;
		if (summaryRequest != null)
			if (summaryRequest.equals("true"))
				summaryOutput = true;


		try {
			    
			if (outputFile41 != null) {
			        
				// open output stream for DTM output file
				outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile41) ) );


				// define filed names for .csv file being written
				tableHeadings = new ArrayList();
				tableHeadings.add(SyntheticPopulation.HHID_FIELD);
				tableHeadings.add(SyntheticPopulation.HHTAZID_FIELD);
				tableHeadings.add("M41");

				
				// temp array for holding field values to be written
				tableData = new float[tableHeadings.size()];

				
				//Print field names to .csv file header record
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print(",");
					outStream.print( (String)tableHeadings.get(i) );
				}
				outStream.println();
				
			}



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), M41_MODEL_SHEET, M4_DATA_SHEET, propertyMap, Household.class);
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
    
    		choiceFreqs = new int[numberOfAlternatives+1];
    	
    	
            // loop over all households in the hh table
            for (int i=0; i < hh.length; i++) {
    
                if (useMessageWindow) mw.setMessage2 ("Model41 Choice for hh " + (i+1) + " of " + hh.length );
    
                hh_id = hh[i].getID();
                hh_taz_id = hh[i].getTazID();
                int alt = 0;
                
    
    
                int hhType = hh[i].getHHType();
    
                // apply individual maintenance tour frequency model only to households with at least 1 travel active person
                alt = 0;
                if ( hhType > 0 ) {
    
                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[i], utilityAvailability);
    
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
    
                    // save chosen alternative in  householdChoice Array
                    for(int a=0; a < numberOfAlternatives; a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
                            break;
                        }
                    }
                    
                    
                    switch (alt-1) {
                    case 0:
                        hh[i].indivTours = null;
                        break;
                    case 1:
                        hh[i].indivTours = new Tour[1];
                        hh[i].indivTours[0] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 2:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 3:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 4:
                        hh[i].indivTours = new Tour[1];
                        hh[i].indivTours[0] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        break;
                    case 5:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 6:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 7:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 8:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        break;
                    case 9:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 10:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 11:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 12:
                        hh[i].indivTours = new Tour[1];
                        hh[i].indivTours[0] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 13:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 14:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 15:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 16:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 17:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 18:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 19:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (0);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 20:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 21:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 22:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 23:
                        hh[i].indivTours = new Tour[6];
                        for (int k=0; k < 6; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 24:
                        hh[i].indivTours = new Tour[2];
                        for (int k=0; k < 2; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 25:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 26:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 27:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.SHOP);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 28:
                        hh[i].indivTours = new Tour[3];
                        for (int k=0; k < 3; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 29:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 30:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 31:
                        hh[i].indivTours = new Tour[6];
                        for (int k=0; k < 6; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (0);
                        hh[i].indivTours[1].setTourType (TourType.SHOP);
                        hh[i].indivTours[1].setTourOrder (1);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (2);
                        hh[i].indivTours[3].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[3].setTourOrder (1);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (2);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 32:
                        hh[i].indivTours = new Tour[4];
                        for (int k=0; k < 4; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        break;
                    case 33:
                        hh[i].indivTours = new Tour[5];
                        for (int k=0; k < 5; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (0);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 34:
                        hh[i].indivTours = new Tour[6];
                        for (int k=0; k < 6; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (1);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (2);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    case 35:
                        hh[i].indivTours = new Tour[7];
                        for (int k=0; k < 7; k++)
                            hh[i].indivTours[k] = new Tour(hh[i].getHHSize());
                        hh[i].indivTours[0].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[0].setTourOrder (1);
                        hh[i].indivTours[1].setTourType (TourType.ESCORTING);
                        hh[i].indivTours[1].setTourOrder (2);
                        hh[i].indivTours[2].setTourType (TourType.SHOP);
                        hh[i].indivTours[2].setTourOrder (1);
                        hh[i].indivTours[3].setTourType (TourType.SHOP);
                        hh[i].indivTours[3].setTourOrder (2);
                        hh[i].indivTours[4].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[4].setTourOrder (1);
                        hh[i].indivTours[5].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[5].setTourOrder (2);
                        hh[i].indivTours[6].setTourType (TourType.OTHER_MAINTENANCE);
                        hh[i].indivTours[6].setTourOrder (3);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.ESCORTING);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.SHOP);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        hh[i].incrementIndivToursByType (TourType.OTHER_MAINTENANCE);
                        break;
                    }
    
                }
    
    
				if (outputFile41 != null) {
				    
					tableData[0] = hh_id;
					tableData[1] = hh_taz_id;
					tableData[2] = alt;
					choiceFreqs[alt]++;
					
					// write out .csv file record for this tour
					outStream.print( tableData[0] );
					for (int c=1; c < tableHeadings.size(); c++) {
						outStream.print(",");
						outStream.print( tableData[c] );
					}
					outStream.println();
					
				}
				
				
            }//next household

			if (outputFile41 != null) {

				logger.info ("finished writing M41.csv output file.");
				outStream.close();

			}
			
        }
		catch (IOException e) {
		    
			   logger.error ("error occured writing M41.csv file.");
			   
		}

        
		if(summaryOutput){

			writeFreqSummaryToLogger ( "Maintenance Tour Frequency", "M41", choiceFreqs );
	
		}
		
		

        if (useMessageWindow) mw.setVisible(false);
        
    }


    public void runMaintenanceAllocation () {

        int hh_id;
        int hh_taz_id;
        int person;
        int personType;

		int[] choiceFreqs = null;
        int[] tourChoice = null;
		float[] tableData = null;
		ArrayList tableHeadings = null;

	        
		PrintWriter outStream = null;


		logger.info ("Starting Model 4.2 -- Individual Maintenance Tour Allocation");

		if (useMessageWindow) mw.setMessage1 ("Starting Model 4.2 -- Individual Maintenance Tour Allocation");
        if (useMessageWindow) mw.setMessage2 ("Building an expression calculator");
        if (useMessageWindow) mw.setMessage3 ("");


        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String)propertyMap.get( "Model4.controlFile");
        String outputFile42 = (String)propertyMap.get( "Model42.outputFile");
		String summaryRequest = (String)propertyMap.get( "Model42.summaryOutput" );


		boolean summaryOutput = false;
		if (summaryRequest != null)
			if (summaryRequest.equals("true"))
				summaryOutput = true;


		try {
			    
			if (outputFile42 != null) {
			        
				// open output stream for DTM output file
				outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile42) ) );


				// define filed names for .csv file being written
				tableHeadings = new ArrayList();
				tableHeadings.add(SyntheticPopulation.HHID_FIELD);
				tableHeadings.add(SyntheticPopulation.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("indiv_tour_id");
                tableHeadings.add("M42");
                tableHeadings.add("personType");
                tableHeadings.add("patternType");
                tableHeadings.add("tourType");
                tableHeadings.add("participation");

				
				// temp array for holding field values to be written
				tableData = new float[tableHeadings.size()];

				
				//Print field names to .csv file header record
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print(",");
					outStream.print( (String)tableHeadings.get(i) );
				}
				outStream.println();
				
			}



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), M42_MODEL_SHEET, M4_DATA_SHEET, propertyMap, Household.class);
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
    
    
            int[] patternTypeAlt = new int[numberOfAlternatives+1];
            int[] timeWindowAvailAlt = new int[numberOfAlternatives+1];
            int[] jointToursPersonAlt = new int[numberOfAlternatives+1];
    
    
            utilityAvailability = new int[numberOfAlternatives+1];
            Arrays.fill (utilityAvailability, 1);
    
    	
    		choiceFreqs = new int[numberOfAlternatives+1];
    
            
            // loop over all households in the hh table
            for (int i=0; i < hh.length; i++) {
    
                if (useMessageWindow) mw.setMessage2 ("Model42 Choice for hh " + (i+1) + " of " + hh.length );
    
                hh_id = hh[i].getID();
                hh_taz_id = hh[i].getTazID();
    
                
                // get individual tours array for this household.
                Tour[] it = hh[i].getIndivTours();
    
    
                // get next household if no individual non-mandatory tours
                if (it == null){
                    choiceFreqs[0]++;
                    continue;
                }
    
    
                // get joint tours array for this household.
                JointTour[] jt = hh[i].getJointTours();
    
                // get person array for this household.
                Person[] persons = hh[i].getPersonArray();
    

                tourChoice= new int[it.length];

    
                // define an array for each person to keep track of participation in individual tours per person
                for (int p=1; p < persons.length; p++) {
                    persons[p].setIndividualTourParticipationArray(it.length);
                }
    
    
                
                // loop over individual tours array for the hh
                for (int j=0; j < it.length; j++) {
    
                    // this model only applicable for maintenance tours
                    int tourType = it[j].getTourType();
                    if (tourType != TourType.ESCORTING && tourType != TourType.SHOP && tourType != TourType.OTHER_MAINTENANCE) {
                        choiceFreqs[0]++;
                        continue;
                    }
                    
    
                    hh[i].setTourID(j);
                    hh[i].setTourCategory(TourType.NON_MANDATORY_CATEGORY);
    
    
                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[i], utilityAvailability);
    
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
                            tourChoice[j]=alt;
                            break;
                        }
                    }
    
                    int[][] personsByPersonTypeArray = hh[i].getPersonsByPersonTypeArray();
                    int p = 0;
    
                    switch (alt) {
                    case 1:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][0];
                        break;
                    case 2:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][1];
                        break;
                    case 3:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][2];
                        break;
                    case 4:
                        p = personsByPersonTypeArray[PersonType.WORKER_F][3];
                        break;
                    case 5:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][0];
                        break;
                    case 6:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][1];
                        break;
                    case 7:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][2];
                        break;
                    case 8:
                        p = personsByPersonTypeArray[PersonType.WORKER_P][3];
                        break;
                    case 9:
                        p = personsByPersonTypeArray[PersonType.STUDENT][0];
                        break;
                    case 10:
                        p = personsByPersonTypeArray[PersonType.STUDENT][1];
                        break;
                    case 11:
                        p = personsByPersonTypeArray[PersonType.STUDENT][2];
                        break;
                    case 12:
                        p = personsByPersonTypeArray[PersonType.STUDENT][3];
                        break;
                    case 13:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][0];
                        break;
                    case 14:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][1];
                        break;
                    case 15:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][2];
                        break;
                    case 16:
                        p = personsByPersonTypeArray[PersonType.NONWORKER][3];
                        break;
                    case 17:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][0];
                        break;
                    case 18:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][1];
                        break;
                    case 19:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][2];
                        break;
                    case 20:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_PRED][3];
                        break;
                    case 21:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][0];
                        break;
                    case 22:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][1];
                        break;
                    case 23:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][2];
                        break;
                    case 24:
                        p = personsByPersonTypeArray[PersonType.SCHOOL_DRIV][3];
                        break;
                    }
    
                    it[j].setPersonParticipation(p, true);
                    persons[p].setIndividualTourParticipation(j, true);
                    
                    persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );
    
                    if (tourType != TourType.ESCORTING) 
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                    
                }//end for individual tours

                
                
				if (outputFile42 != null) {
    				    
					for (int p=1; p < persons.length; p++) {
						if (it != null) {
							for (int t=0; t < it.length; t++) {
								tableData[0] = hh_id;
								tableData[1] = hh_taz_id;
								tableData[2] = persons[p].getID();
								tableData[3] = t+1;
								tableData[4] = tourChoice[t];
								tableData[5] = persons[p].getPersonType();
								tableData[6] = persons[p].getPatternType();
								tableData[7] = ( it[t].getTourType() );
								tableData[8] = ( it[t].getPersonParticipation(p) ? 1 : 2 );
								choiceFreqs[tourChoice[t]]++;
	    
	                        
								// write out .csv file record for this tour
								outStream.print( tableData[0] );
								for (int c=1; c < tableHeadings.size(); c++) {
									outStream.print(",");
									outStream.print( tableData[c] );
								}
								outStream.println();
							}
						}
						else {
							tableData[0] = hh_id;
							tableData[1] = hh_taz_id;
							tableData[2] = persons[p].getID();
							tableData[3] = 0;
							tableData[4] = 0;
							tableData[5] = persons[p].getPersonType();
							tableData[6] = persons[p].getPatternType();
							tableData[7] = 0;
							tableData[8] = 0;
							choiceFreqs[0]++;

							// write out .csv file record for this tour
							outStream.print( tableData[0] );
							for (int c=1; c < tableHeadings.size(); c++) {
								outStream.print(",");
								outStream.print( tableData[c] );
							}
							outStream.println();
						}
					}
				
				}
                        
            }//next household

			if (outputFile42 != null) {

				logger.info ("finished writing M42.csv output file.");
				outStream.close();

			}
			
        }
		catch (IOException e) {
		    
			   logger.fatal ("error occured writing M42.csv file.");
			   
		}

        
		if(summaryOutput){

			writeFreqSummaryToLogger ( "Individual Tour Participation", "M42", choiceFreqs );
	
		}
		
		

        if (useMessageWindow) mw.setVisible(false);
        
    }


    public void runDiscretionaryWorkerStudentFrequency () {

        int hh_id;
        int hh_taz_id;

		int alt=0;
        int newRecordPos;
        int[] personChoice = null;
		int[] choiceFreqs = null;
		float[] tableData = null;
		ArrayList tableHeadings = null;

	        
		PrintWriter outStream = null;


		logger.info ("Starting Model 4.3.1 - Individual Discretionary Tour Frequency (Workers & Students)");

		if (useMessageWindow) mw.setMessage1 ("Starting Model 4.3.1 - Individual Discretionary Tour Frequency (Workers & Students)");
        if (useMessageWindow) mw.setMessage2 ("Building an expression calculator");
        if (useMessageWindow) mw.setMessage3 ("");


        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String)propertyMap.get( "Model4.controlFile");
        String outputFile431 = (String)propertyMap.get( "Model431.outputFile");
		String summaryRequest = (String)propertyMap.get( "Model431.summaryOutput" );


		boolean summaryOutput = false;
		if (summaryRequest != null)
			if (summaryRequest.equals("true"))
				summaryOutput = true;


		try {
			    
			if (outputFile431 != null) {
			        
				// open output stream for DTM output file
				outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile431) ) );


				// define filed names for .csv file being written
				tableHeadings = new ArrayList();
				tableHeadings.add(SyntheticPopulation.HHID_FIELD);
				tableHeadings.add(SyntheticPopulation.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("indiv_tour_id");
                tableHeadings.add("M431");
                tableHeadings.add("personType");
                tableHeadings.add("patternType");
                tableHeadings.add("tourType");
                tableHeadings.add("participation");

				
				// temp array for holding field values to be written
				tableData = new float[tableHeadings.size()];

				
				//Print field names to .csv file header record
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print(",");
					outStream.print( (String)tableHeadings.get(i) );
				}
				outStream.println();
				
			}


            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), M431_MODEL_SHEET, M4_DATA_SHEET, propertyMap, Household.class);
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
        
        	
            choiceFreqs = new int[numberOfAlternatives+1];
        
                
            // loop over all households in the hh table
            for (int h=0; h < hh.length; h++) {
    
                if (useMessageWindow) mw.setMessage2 ("Model431 Choice for hh " + (h+1) + " of " + hh.length );
    
                hh_id = hh[h].getID();
                hh_taz_id = hh[h].getTazID();
    
    
                // get joint tours array for this household.
                JointTour[] jt = hh[h].getJointTours();
    
                // get person array for this household.
                Person[] persons = hh[h].getPersonArray();
    
    
				personChoice = new int[persons.length];

    
                Tour[] it = hh[h].getIndivTours();
                newRecordPos = ( it == null ? 0 : it.length );
    
                // loop over persons array for the hh
                for (int p=1; p < persons.length; p++) {
    
    
                    // this model only applies to full-time, part-time, and student person types with nactivities away from home.
                    int personType = persons[p].getPersonType();
					int patternType = persons[p].getPatternType();
                    if (personType != PersonType.WORKER_F && personType != PersonType.WORKER_P && personType != PersonType.STUDENT || patternType == PatternType.HOME){
                        choiceFreqs[0]++;
                        personChoice[p-1] = 0;
						continue;
                    }
    

    
                    hh[h].setPersonID(p);
                    hh[h].setTourCategory(TourType.NON_MANDATORY_CATEGORY);
    
    
                    // if this person has NONMANDATORY pattern, but hasn't been allocated
                    // any non-mandatory tours so far, then alternative 1 (0 tours) is not available.
                    Arrays.fill (utilityAvailability, 1);
                    if ( patternType == PatternType.NON_MAND && persons[p].getNumJointTours() == 0 && persons[p].getNumIndNonMandInceTours() == 0 )
                        utilityAvailability[1] = 0;
    
                    // get utilities for each alternative for this household
                    index.setZoneIndex(hh_taz_id);
                    index.setHHIndex(hh_id);
                    double[] utilities = uec.solve( index, hh[h], utilityAvailability );
    
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
                    for(int a=0;a < numberOfAlternatives;a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
							personChoice[p-1] = alt;
                            break;
                        }
                    }
    
    
    
                    Tour[] it0;
                    Tour[] it1;
                    boolean[] tempParticipate;
                    int currentLength = 0;
                    
					choiceFreqs[alt]++;
                    switch (alt) {
                    case 1:
                        it1 = null;
                        break;
                    case 2:
                        // add this EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.EAT);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );
    
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    case 3:
                        // add this DISCRETIONARY tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 4:
                        // add two DISCRETIONARY tours to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 2 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 5:
                        // add one DISCRETIONARY and one EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.EAT);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    }

                }//end for persons

                
                
				if (outputFile431 != null) {
    				    
					it = hh[h].getIndivTours();

					if (it != null) {

					    for (int p=1; p < persons.length; p++) {

							// this model only applies to full-time, part-time, and student person types with nactivities away from home.
							int personType = persons[p].getPersonType();
							int patternType = persons[p].getPatternType();
							if (personType != PersonType.WORKER_F && personType != PersonType.WORKER_P && personType != PersonType.STUDENT || patternType == PatternType.HOME){
								continue;
							}
    
							for (int t=0; t < it.length; t++) {
								tableData[0] = hh_id;
								tableData[1] = hh_taz_id;
								tableData[2] = persons[p].getID();
								tableData[3] = t+1;
								tableData[4] = personChoice[p-1];
								tableData[5] = persons[p].getPersonType();
								tableData[6] = persons[p].getPatternType();
								tableData[7] = ( it[t].getTourType() );
								tableData[8] = ( it[t].getPersonParticipation(p) ? 1 : 2 );
								choiceFreqs[personChoice[p-1]]++;
	                        
								// write out .csv file record for this tour
								outStream.print( tableData[0] );
								for (int c=1; c < tableHeadings.size(); c++) {
									outStream.print(",");
									outStream.print( tableData[c] );
								}
								outStream.println();
							}
							
						}
					}
				
				}
                        
            }//next household

			if (outputFile431 != null) {

				logger.info ("finished writing M431.csv output file.");
				outStream.close();

			}
			
        }
		catch (IOException e) {
		    
			   logger.error ("error occured writing M431.csv file.");
			   
		}

        
		if(summaryOutput){

			writeFreqSummaryToLogger ( "Individual Discretionary Tour Frequency (Workers & Students)", "M431", choiceFreqs );
	
		}
		
		

        if (useMessageWindow) mw.setVisible(false);
        
    }



    public void runDiscretionaryNonWorkerFrequency () {

        int hh_id;
        int hh_taz_id;


        int alt=0;
		int newRecordPos = 0;
		int[] personChoice = null;
		int[] choiceFreqs = null;
		float[] tableData = null;
		ArrayList tableHeadings = null;

	        
		PrintWriter outStream = null;


		logger.info ("Starting Model 4.3.2 - Individual Discretionary Tour Frequency (NonWorkers)");

		if (useMessageWindow) mw.setMessage1 ("Starting Model 4.3.2 - Individual Discretionary Tour Frequency (NonWorkers)");
        if (useMessageWindow) mw.setMessage2 ("Building an expression calculator");
        if (useMessageWindow) mw.setMessage3 ("");


        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String)propertyMap.get( "Model4.controlFile");
        String outputFile432 = (String)propertyMap.get( "Model432.outputFile");
		String summaryRequest = (String)propertyMap.get( "Model432.summaryOutput" );


		boolean summaryOutput = false;
		if (summaryRequest != null)
			if (summaryRequest.equals("true"))
				summaryOutput = true;


		try {
			    
			if (outputFile432 != null) {
			        
				// open output stream for DTM output file
				outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile432) ) );


				// define filed names for .csv file being written
				tableHeadings = new ArrayList();
				tableHeadings.add(SyntheticPopulation.HHID_FIELD);
				tableHeadings.add(SyntheticPopulation.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("indiv_tour_id");
                tableHeadings.add("M432");
                tableHeadings.add("personType");
                tableHeadings.add("patternType");
                tableHeadings.add("tourType");
                tableHeadings.add("participation");

				
				// temp array for holding field values to be written
				tableData = new float[tableHeadings.size()];

				
				//Print field names to .csv file header record
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print(",");
					outStream.print( (String)tableHeadings.get(i) );
				}
				outStream.println();
				
			}



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), M432_MODEL_SHEET, M4_DATA_SHEET, propertyMap, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();

			choiceFreqs = new int[numberOfAlternatives+1];
            
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
    
    
    
            // loop over all households in the hh table
            for (int h=0; h < hh.length; h++) {
    
    			if (useMessageWindow) mw.setMessage2 ("Model432 Choice for hh " + (h+1) + " of " + hh.length );
    
                hh_id = hh[h].getID();
                hh_taz_id = hh[h].getTazID();
    
    
    
                // get joint tours array for this household.
                JointTour[] jt = hh[h].getJointTours();
    
				// get person array for this household.
				Person[] persons = hh[h].getPersonArray();
    
    
    
				personChoice = new int[persons.length];

    
				Tour[] it = hh[h].getIndivTours();
				newRecordPos = ( it == null ? 0 : it.length );
    
                // loop over individual tours array for the hh
                for (int p=1; p < persons.length; p++) {
    
    
					// this model only applies to nonworker person types with activities away from home.
					int personType = persons[p].getPersonType();
					int patternType = persons[p].getPatternType();
					if (personType != PersonType.NONWORKER || patternType == PatternType.HOME){
						personChoice[p-1] = 0;
						choiceFreqs[0]++;
						continue;
					}
    
                    hh[h].setPersonID(p);
                    hh[h].setTourCategory(TourType.NON_MANDATORY_CATEGORY);
    
    
                    // if this person has NONMANDATORY pattern, but hasn't been allocated
                    // any non-mandatory tours so far, then alternative 1 (0 tours) is not available.
                    Arrays.fill (utilityAvailability, 1);
                    if ( patternType == PatternType.NON_MAND && persons[p].getNumJointTours() == 0 && persons[p].getNumIndNonMandInceTours() == 0 )
                        utilityAvailability[1] = 0;
    
    
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
                    for(int a=0;a < numberOfAlternatives;a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
							personChoice[p-1] = alt;
                            break;
                        }
                    }
    
    
    
                    Tour[] it0;
                    Tour[] it1;
                    boolean[] tempParticipate;
                    int currentLength = 0;

					choiceFreqs[alt]++;
                    switch (alt) {
                    case 1:
                        it1 = null;
                        break;
                    case 2:
                        // add this EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.EAT);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );
    
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    case 3:
                        // add this DISCRETIONARY tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );
                        
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 4:
                        // add two DISCRETIONARY tours to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 2 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 5:
                        // add one DISCRETIONARY and one EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.EAT);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    }
    
        
                }//end for persons


            
				if (outputFile432 != null) {
        			    
					it = hh[h].getIndivTours();

					if (it != null) {

					    for (int p=1; p < persons.length; p++) {
						
					        // this model only applies to nonworker person types with activities away from home.
							int personType = persons[p].getPersonType();
							int patternType = persons[p].getPatternType();
							if (personType != PersonType.NONWORKER || patternType == PatternType.HOME){
								continue;
							}
    
							for (int t=0; t < it.length; t++) {
								tableData[0] = hh_id;
								tableData[1] = hh_taz_id;
								tableData[2] = persons[p].getID();
								tableData[3] = t+1;
								tableData[4] = personChoice[p-1];
								tableData[5] = persons[p].getPersonType();
								tableData[6] = persons[p].getPatternType();
								tableData[7] = ( it[t].getTourType() );
								tableData[8] = ( it[t].getPersonParticipation(p) ? 1 : 2 );
								choiceFreqs[personChoice[p-1]]++;
	                        
								// write out .csv file record for this tour
								outStream.print( tableData[0] );
								for (int c=1; c < tableHeadings.size(); c++) {
									outStream.print(",");
									outStream.print( tableData[c] );
								}
								outStream.println();
							}
							
						}
					}
        				
				}
				
            }//next household

			if (outputFile432 != null) {

				logger.info ("finished writing M432.csv output file.");
				outStream.close();

			}
			
        }
		catch (IOException e) {
		    
			   logger.error ("error occured writing M432.csv file.");
			   
		}

        
		if(summaryOutput){

			writeFreqSummaryToLogger ( "Individual Discretionary Tour Frequency (Nonworkers)", "M432", choiceFreqs );
	
		}
		
		

        if (useMessageWindow) mw.setVisible(false);
        
    }



    public void runDiscretionaryChildFrequency () {

        int hh_id;
        int hh_taz_id;


        int alt=0;
		int newRecordPos = 0;
		int[] personChoice = null;
		int[] choiceFreqs = null;
		float[] tableData = null;
		ArrayList tableHeadings = null;

	        
		PrintWriter outStream = null;


		logger.info ("Starting Model 4.3.3 - Individual Discretionary Tour Frequency (School age children)");
		
		if (useMessageWindow) mw.setMessage1 ("Starting Model 4.3.3 - Individual Discretionary Tour Frequency (School age children)");
        if (useMessageWindow) mw.setMessage2 ("Building an expression calculator");
        if (useMessageWindow) mw.setMessage3 ("");


        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String)propertyMap.get( "Model4.controlFile");
        String outputFile433 = (String)propertyMap.get( "Model433.outputFile");
		String summaryRequest = (String)propertyMap.get( "Model433.summaryOutput" );


		boolean summaryOutput = false;
		if (summaryRequest != null)
			if (summaryRequest.equals("true"))
				summaryOutput = true;


		try {
			    
			if (outputFile433 != null) {
			        
				// open output stream for DTM output file
				outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile433) ) );


				// define filed names for .csv file being written
				tableHeadings = new ArrayList();
				tableHeadings.add(SyntheticPopulation.HHID_FIELD);
				tableHeadings.add(SyntheticPopulation.HHTAZID_FIELD);
                tableHeadings.add("person_id");
                tableHeadings.add("indiv_tour_id");
                tableHeadings.add("M433");
                tableHeadings.add("personType");
                tableHeadings.add("patternType");
                tableHeadings.add("tourType");
                tableHeadings.add("participation");

				
				// temp array for holding field values to be written
				tableData = new float[tableHeadings.size()];

				
				//Print field names to .csv file header record
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print(",");
					outStream.print( (String)tableHeadings.get(i) );
				}
				outStream.println();
				
			}



            // create a new UEC to get utilties for this logit model
            UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), M433_MODEL_SHEET, M4_DATA_SHEET, propertyMap, Household.class);
            int numberOfAlternatives = uec.getNumberOfAlternatives();
            String[] alternativeNames = uec.getAlternativeNames();
    
			choiceFreqs = new int[numberOfAlternatives+1];

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
    
    
    
            // loop over all households in the hh table
            for (int h=0; h < hh.length; h++) {
    			
    			if (useMessageWindow) mw.setMessage2 ("Model433 Choice for hh " + (h+1) + " of " + hh.length );
    
                hh_id = hh[h].getID();
                hh_taz_id = hh[h].getTazID();
    
    
				// get joint tours array for this household.
				JointTour[] jt = hh[h].getJointTours();
    
				// get person array for this household.
				Person[] persons = hh[h].getPersonArray();
    
    
    
				personChoice = new int[persons.length];

    
				Tour[] it = hh[h].getIndivTours();
				newRecordPos = ( it == null ? 0 : it.length );
    
                // loop over individual tours array for the hh
                for (int p=1; p < persons.length; p++) {
    
    
					// this model only applies to school aged person types with activities away from home.
					int personType = persons[p].getPersonType();
					int patternType = persons[p].getPatternType();
					if (personType != PersonType.SCHOOL_DRIV && personType != PersonType.SCHOOL_PRED || patternType == PatternType.HOME){
						personChoice[p-1] = 0;
						choiceFreqs[0]++;
						continue;
					}

                    hh[h].setPersonID(p);
                    hh[h].setTourCategory(TourType.NON_MANDATORY_CATEGORY);
    
    
                    // if this person has NONMANDATORY pattern, but hasn't been allocated
                    // any non-mandatory tours so far, then alternative 1 (0 tours) is not available.
                    Arrays.fill (utilityAvailability, 1);
                    if ( patternType == PatternType.NON_MAND && persons[p].getNumJointTours() == 0 && persons[p].getNumIndNonMandInceTours() == 0 )
                        utilityAvailability[1] = 0;
    
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
                    for(int a=0;a < numberOfAlternatives;a++){
                        if (chosenAltName.equals(alternativeNames[a])) {
                            alt = a+1;
							personChoice[p-1] = alt;
                            break;
                        }
                    }
    
    
    
                    Tour[] it0;
                    Tour[] it1;
                    boolean[] tempParticipate;
                    int currentLength = 0;

					choiceFreqs[alt]++;
                    switch (alt) {
                    case 1:
                        it1 = null;
                        break;
                    case 2:
                        // add this EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.EAT);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );
    
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    case 3:
                        // add this DISCRETIONARY tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 1];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (0);
                        it1[currentLength].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+1);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 1 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 1 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 4:
                        // add two DISCRETIONARY tours to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].setIndivTours(it1);
                        break;
                    case 5:
                        // add one DISCRETIONARY and one EAT tour to the array of individual tours for the household
                        it0 = hh[h].getIndivTours();
                        currentLength = (it0 == null ? 0 : it0.length);
                        it1 = new Tour[currentLength + 2];
                        for (int i=0; i < currentLength; i++)
                            it1[i] = it0[i];
                        it1[currentLength] = new Tour(hh[h].getHHSize());
                        it1[currentLength].setTourType (TourType.DISCRETIONARY);
                        it1[currentLength].setTourOrder (1);
                        it1[currentLength].setPersonParticipation(p, true);
                        it1[currentLength+1] = new Tour(hh[h].getHHSize());
                        it1[currentLength+1].setTourType (TourType.EAT);
                        it1[currentLength+1].setTourOrder (2);
                        it1[currentLength+1].setPersonParticipation(p, true);
    
                        // lengthen the individualTourParticipationArray for each person in the household
                        tempParticipate = new boolean[currentLength];
                        for (int q=1; q < persons.length; q++) {
                            for (int i=0; i < currentLength; i++)
                                tempParticipate[i] = persons[q].getIndividualTourParticipation(i);
                            persons[q].setIndividualTourParticipationArray(currentLength+2);
                            for (int i=0; i < currentLength; i++)
                                persons[q].setIndividualTourParticipation(i, tempParticipate[i]);
                            persons[q].setIndividualTourParticipation(currentLength, false);
                            persons[q].setIndividualTourParticipation(currentLength+1, false);
                        }
                        persons[p].setIndividualTourParticipation(currentLength, true);
                        persons[p].setIndividualTourParticipation(currentLength+1, true);
                        persons[p].setNumIndNonMandDiscrTours( persons[p].getNumIndNonMandDiscrTours() + 1 );
                        persons[p].setNumIndNonMandEatTours( persons[p].getNumIndNonMandEatTours() + 1 );
                        persons[p].setNumIndNonMandTours( persons[p].getNumIndNonMandTours() + 2 );
                        persons[p].setNumIndNonMandInceTours( persons[p].getNumIndNonMandInceTours() + 2 );
    
                        hh[h].incrementIndivToursByType (TourType.DISCRETIONARY);
                        hh[h].incrementIndivToursByType (TourType.EAT);
                        hh[h].setIndivTours(it1);
                        break;
                    }
        
            
                }//end for persons

                
                
				if (outputFile433 != null) {
        			    
					it = hh[h].getIndivTours();

					if (it != null) {
					    
					    for (int p=1; p < persons.length; p++) {
						
					        // this model only applies to school aged person types with activities away from home.
							int personType = persons[p].getPersonType();
							int patternType = persons[p].getPatternType();
							if (personType != PersonType.SCHOOL_DRIV && personType != PersonType.SCHOOL_PRED || patternType == PatternType.HOME){
								continue;
							}

							for (int t=0; t < it.length; t++) {
								tableData[0] = hh_id;
								tableData[1] = hh_taz_id;
								tableData[2] = persons[p].getID();
								tableData[3] = t+1;
								tableData[4] = personChoice[p-1];
								tableData[5] = persons[p].getPersonType();
								tableData[6] = persons[p].getPatternType();
								tableData[7] = ( it[t].getTourType() );
								tableData[8] = ( it[t].getPersonParticipation(p) ? 1 : 2 );
								choiceFreqs[personChoice[p-1]]++;
	                        
								// write out .csv file record for this tour
								outStream.print( tableData[0] );
								for (int c=1; c < tableHeadings.size(); c++) {
									outStream.print(",");
									outStream.print( tableData[c] );
								}
								outStream.println();
							}
						}
					}
        				
				}
				
            }//next household

			if (outputFile433 != null) {

				logger.info ("finished writing M433.csv output file.");
				outStream.close();

			}
			
        }
		catch (IOException e) {
		    
			   logger.error ("error occured writing M433.csv file.");
			   
		}

        
		if(summaryOutput){

			writeFreqSummaryToLogger ( "Individual Discretionary Tour Frequency (Nonworkers)", "M433", choiceFreqs );
	
		}
		
		

        if (useMessageWindow) mw.setVisible(false);
        
    }



    public void runAtWorkFrequency () {

        int hh_id;
        int hh_taz_id;
        int person=0;
        int personType=0;

		int[] choiceFreqs = null;
		int[] tourChoice = null;
		float[][] tourMaker = null;
		float[] tableData = null;
		ArrayList tableHeadings = null;

	        
		PrintWriter outStream = null;


		logger.info ("Starting Model 4.4 - Individual AtWork Tour Frequency");

		if (useMessageWindow) mw.setMessage1 ("Starting Model 4.4 - Individual AtWork Tour Frequency");
        if (useMessageWindow) mw.setMessage2 ("Building an expression calculator");
        if (useMessageWindow) mw.setMessage3 ("");


        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String)propertyMap.get( "Model4.controlFile" );
        String outputFile44 = (String)propertyMap.get( "Model44.outputFile" );
		String summaryRequest = (String)propertyMap.get( "Model44.summaryOutput" );

        
		boolean summaryOutput = false;
		if (summaryRequest != null)
			if (summaryRequest.equals("true"))
				summaryOutput = true;


		try {
			    
			if (outputFile44 != null) {
			        
				// open output stream for DTM output file
				outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFile44) ) );


				// define filed names for .csv file being written
				tableHeadings = new ArrayList();
				tableHeadings.add(SyntheticPopulation.HHID_FIELD);
				tableHeadings.add(SyntheticPopulation.HHTAZID_FIELD);
				tableHeadings.add("man_tour_id");
				tableHeadings.add("man_tour_type");
				tableHeadings.add("workTourMaker_ID");
				tableHeadings.add("workTourMaker_type");
				tableHeadings.add("M44");

				
				// temp array for holding field values to be written
				tableData = new float[tableHeadings.size()];

				
				//Print field names to .csv file header record
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print(",");
					outStream.print( (String)tableHeadings.get(i) );
				}
				outStream.println();
				
			}



	        // create a new UEC to get utilties for this logit model
	        UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(controlFile), M44_MODEL_SHEET, M4_DATA_SHEET, propertyMap, Household.class);
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
	
			choiceFreqs = new int[numberOfAlternatives+1];
	
	
	        // loop over all households in the hh table
	        for (int h=0; h < hh.length; h++) {
	            
	            if (useMessageWindow) mw.setMessage2 ("Model44 Choice for hh " + (h+1) + " of " + hh.length );
	
	            hh_id = hh[h].getID();
	            hh_taz_id = hh[h].getTazID();
	
	
	
	            // get joint tours array for this household.
	            JointTour[] jt = hh[h].getJointTours();
	
	            // get person array for this household.
	            Person[] persons = hh[h].getPersonArray();
	
	
	            Tour[] mt = hh[h].getMandatoryTours();
	            if (mt != null) {
		            tourChoice = new int[mt.length];
		            tourMaker = new float[mt.length][2];
	            }
	            else {
	                choiceFreqs[0]++;
	                continue;
	            }
	
	
	            // loop over mandatory tours array for the hh
	            for (int t=0; t < mt.length; t++) {
	
	                // this model only applies to work tours.
	                int tourType = mt[t].getTourType();
	                if (tourType != TourType.WORK){
	                    tourChoice[t]=0;
	                    tourMaker[t][0]=0;
	                    tourMaker[t][1]=0;
						choiceFreqs[0]++;
	                    continue;
	                }
	
	                // determine which person made this work tour
	                personType = 0;
	                for (int p=1; p < persons.length; p++) {
	                    if (mt[t].getPersonParticipation(p)) {
	                        personType = persons[p].getPersonType();
	                        person = p;
	                        break;
	                    }
	                }
	                tourMaker[t][0]=person;
	                tourMaker[t][1]=personType;
	
	                if (personType == 0) {
	                    logger.debug ("error in IndividualNonMandatoryToursModel.runAtWorkFrequency()");
	                    logger.debug ("no person in persons[] had participation == true");
	                    System.exit(1);
	                }
	
	
	                hh[h].setPersonID(person);
	                hh[h].setTourID(t);
	                hh[h].setTourCategory(TourType.MANDATORY_CATEGORY);
	
	
	
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
	                        tourChoice[t]=alt;
	                        break;
	                    }
	                }
	
	                
	
	                Tour[] it1;
	                switch (alt) {
	                case 1:
	                    it1 = null;
	                    break;
	                case 2:
	                    // add this EAT at-work subtour to the array of subtours for this work tour
	                    it1 = new Tour[1];
	                    it1[0] = new Tour(hh[h].getHHSize());
	                    it1[0].setTourType (TourType.ATWORK);
	                    it1[0].setSubTourType (SubTourType.EAT);
	                    it1[0].setTourOrder (0);
	                    it1[0].setSubTourPerson(person);
	                    it1[0].setPersonParticipation(person, true);
	                    mt[t].incrementSubToursByType (SubTourType.EAT);
	                    mt[t].setSubTours(it1);
	                    break;
	                case 3:
	                    // add this WORK at-work subtour to the array of subtours for this work tour
	                    it1 = new Tour[1];
	                    it1[0] = new Tour(hh[h].getHHSize());
	                    it1[0].setTourType (TourType.ATWORK);
	                    it1[0].setSubTourType (SubTourType.WORK);
	                    it1[0].setTourOrder (0);
	                    it1[0].setSubTourPerson(person);
	                    it1[0].setPersonParticipation(person, true);
	                    mt[t].incrementSubToursByType (SubTourType.WORK);
	                    mt[t].setSubTours(it1);
	                    break;
	                case 4:
	                    // add this OTHER at-work subtour to the array of subtours for this work tour
	                    it1 = new Tour[1];
	                    it1[0] = new Tour(hh[h].getHHSize());
	                    it1[0].setTourType (TourType.ATWORK);
	                    it1[0].setTourType (TourType.ATWORK);
	                    it1[0].setSubTourType (SubTourType.OTHER);
	                    it1[0].setTourOrder (0);
	                    it1[0].setSubTourPerson(person);
	                    it1[0].setPersonParticipation(person, true);
	                    mt[t].incrementSubToursByType (SubTourType.OTHER);
	                    mt[t].setSubTours(it1);
	                    break;
	                case 5:
	                    // add these two WORK at-work subtours to the array of subtours for this work tour
	                    it1 = new Tour[2];
	                    it1[0] = new Tour(hh[h].getHHSize());
	                    it1[0].setTourType (TourType.ATWORK);
	                    it1[0].setSubTourType (SubTourType.WORK);
	                    it1[0].setTourOrder (1);
	                    it1[0].setSubTourPerson(person);
	                    it1[0].setPersonParticipation(person, true);
	                    it1[1] = new Tour(hh[h].getHHSize());
	                    it1[1].setTourType (TourType.ATWORK);
	                    it1[1].setSubTourType (SubTourType.WORK);
	                    it1[1].setTourOrder (2);
	                    it1[1].setSubTourPerson(person);
	                    it1[1].setPersonParticipation(person, true);
	                    mt[t].incrementSubToursByType (SubTourType.WORK);
	                    mt[t].incrementSubToursByType (SubTourType.WORK);
	                    mt[t].setSubTours(it1);
	                    break;
	                case 6:
	                    // add these WORK and EAT at-work subtours to the array of subtours for this work tour
	                    it1 = new Tour[2];
	                    it1[0] = new Tour(hh[h].getHHSize());
	                    it1[0].setTourType (TourType.ATWORK);
	                    it1[0].setSubTourType (SubTourType.EAT);
	                    it1[0].setTourOrder (1);
	                    it1[0].setSubTourPerson(person);
	                    it1[0].setPersonParticipation(person, true);
	                    it1[1] = new Tour(hh[h].getHHSize());
	                    it1[1].setTourType (TourType.ATWORK);
	                    it1[1].setSubTourType (SubTourType.WORK);
	                    it1[1].setTourOrder (2);
	                    it1[1].setSubTourPerson(person);
	                    it1[1].setPersonParticipation(person, true);
	                    mt[t].incrementSubToursByType (SubTourType.EAT);
	                    mt[t].incrementSubToursByType (SubTourType.WORK);
	                    mt[t].setSubTours(it1);
	                    break;
	                }
	
	            }//end for individual mandatory tours


				if (outputFile44 != null) {
				    
					if (mt != null && mt.length > 0) {
						for (int t=0; t < mt.length; t++) {
							tableData[0] = hh_id;
							tableData[1] = hh_taz_id;
							tableData[2] = t+1;
							tableData[3] = mt[t].getTourType();
							tableData[4] = tourMaker[t][0];
							tableData[5] = tourMaker[t][1];
							tableData[6] = tourChoice[t];
							choiceFreqs[tourChoice[t]]++;
							
							// write out .csv file record for this tour
							outStream.print( tableData[0] );
							for (int c=1; c < tableHeadings.size(); c++) {
								outStream.print(",");
								outStream.print( tableData[c] );
							}
							outStream.println();
						}
					}
					else {
						tableData[0] = hh_id;
						tableData[1] = hh_taz_id;
						tableData[2] = 0;
						tableData[3] = 0;
						tableData[4] = 0;
						tableData[5] = 0;
						tableData[6] = 0;
						choiceFreqs[0]++;

						// write out .csv file record for this tour
						outStream.print( tableData[0] );
						for (int c=1; c < tableHeadings.size(); c++) {
							outStream.print(",");
							outStream.print( tableData[c] );
						}
						outStream.println();
					}
					
				}
				
				
	        }//next household
        
			if (outputFile44 != null) {

				logger.info ("finished writing M44.csv output file.");
				outStream.close();

			}
			
        }
		catch (IOException e) {
		    
			   logger.error ("error occured writing M44.csv file.");
			   
		}

        
		if(summaryOutput){

			writeFreqSummaryToLogger ( "At-work Tour Frequency", "M44", choiceFreqs );
	
		}
		
		

        if (useMessageWindow) mw.setVisible(false);
        
    }


    private void writeFreqSummaryToLogger ( String tableTitle, String fieldName, int[] freqs ) {
    
		// print a simple summary table
		logger.info( "Frequency Report table: " + tableTitle );
		logger.info( "Frequency for field " + fieldName );
		logger.info(Format.print("%8s", "Value") + "  " + Format.print("%-20s", "Description") + "  " + Format.print("%11s", "Frequency"));
		
		int total = 0;
		for (int i = 0; i < freqs.length; i++) {
		    if (freqs[i] > 0) {
		        String description = OutputDescription.getDescription(fieldName, i);
		        logger.info( Format.print("%8d", i) + "  " + Format.print("%-20s", description) + "  " + Format.print("%11d", freqs[i] ) );
				total += freqs[i];
		    }
		}
		
		logger.info(Format.print("%8s", "Total") + Format.print("%35d\n\n\n", total));
    }

}
