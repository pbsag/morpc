package com.pb.morpc.models;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.morpc.synpop.SyntheticPopulation;
import com.pb.morpc.structures.MessageWindow;
import com.pb.morpc.structures.Household;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;

public class Model26 {

    static Logger logger = Logger.getLogger("com.pb.morpc.models");

	static final int MODEL_26_SHEET = 1;
	static final int DATA_26_SHEET = 0;

	HashMap propertyMap;
	boolean useMessageWindow = false;
	MessageWindow mw;
	
	private IndexValues index = new IndexValues();
	private int[] sample;
	
    public Model26 (HashMap propertyMap) {
        
        this.propertyMap = propertyMap;

		// get the indicator for whether to use the message window or not
		// from the properties file.
		String useMessageWindowString = (String)propertyMap.get( "MessageWindow" );
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				this.useMessageWindow = true;
				this.mw = new MessageWindow ( "MORPC Daily Activity Pattern (M26) Run Time Information" );
			}
		}
    }



    public void runWorkerPtDailyActivityPatternChoice() {

        int hh_id;
        int hh_taz_id;

          
        //open files  
        String controlFile = (String)propertyMap.get( "Model26.controlFile" );
        String outputFile = (String)propertyMap.get( "Model26.outputFile" );
        logger.debug(controlFile);
        logger.debug(outputFile);


        // create a new UEC to get utilties for this logit model
		UtilityExpressionCalculator uec = new UtilityExpressionCalculator ( new File(controlFile), MODEL_26_SHEET, DATA_26_SHEET, propertyMap, Household.class );
        int numberOfAlternatives=uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

		sample = new int[uec.getNumberOfAlternatives()+1];
		
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

        // get the household data table from the UEC control file
        TableDataSet hhTable = uec.getHouseholdData();
        if (hhTable == null) {
            logger.debug ("Could not get householdData TableDataSet from UEC");
            System.exit(1);
        }

        int hh_idPosition = hhTable.getColumnPosition( SyntheticPopulation.HHID_FIELD );
        if (hh_idPosition <= 0) {
            logger.debug (SyntheticPopulation.HHID_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }
        int hh_taz_idPosition = hhTable.getColumnPosition( SyntheticPopulation.HHTAZID_FIELD );
        if (hh_taz_idPosition <= 0) {
            logger.debug (SyntheticPopulation.HHTAZID_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        int workers_p_idPosition = hhTable.getColumnPosition( SyntheticPopulation.WORKERS_P_FIELD );
        if (workers_p_idPosition <= 0) {
            logger.debug (SyntheticPopulation.WORKERS_P_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        //define vectors which will hold results of linked attributes
        //which need to be appended to hhTable for use in next model
        float[] workersPartWork1=new float[hhTable.getRowCount()];
        float[] workersPartWork2=new float[hhTable.getRowCount()];
        float[] workersPartWorkUniv=new float[hhTable.getRowCount()];
        float[] workersPartUniv1=new float[hhTable.getRowCount()];
        float[] workersPartNonMand=new float[hhTable.getRowCount()];
        float[] workersPartAtHome=new float[hhTable.getRowCount()];

        // loop over all households and all part-time workers in household
        //to make alternative choice.
		for (int i=0; i < hhTable.getRowCount(); i++) {
			if (useMessageWindow) mw.setMessage1 ("Model26 Choice for hh " + (i+1) + " of " + hhTable.getRowCount() );

			hh_id = (int)hhTable.getValueAt( i+1, hh_idPosition );
			hh_taz_id = (int)hhTable.getValueAt( i+1, hh_taz_idPosition );
            // check for part-time workers in the household
            if ( (int)hhTable.getValueAt( i+1, workers_p_idPosition ) > 0 ) {

				// get utilities for each alternative for this household
				index.setZoneIndex( hh_taz_id );
				index.setHHIndex( hh_id );
        
				Arrays.fill(sample, 1);
				double[] utilities = uec.solve( index, new Object(), sample );

				//set utility for each alternative
				for(int a=0;a < numberOfAlternatives;a++){
					alts[a].setAvailability( sample[a+1] == 1 );
					if (sample[a+1] == 1)
						alts[a].setAvailability( (utilities[a] > -99.0) );
					alts[a].setUtility(utilities[a]);
				}
				// set availabilities
				root.computeAvailabilities();


				root.getUtility();
				root.calculateProbabilities();


                //loop over number of part-time workers in household
                for (int m=0; m < (int)hhTable.getValueAt( i+1, workers_p_idPosition ); m++){
                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();
                    if (chosenAltName.equals("Work_1"))
                        ++ workersPartWork1[i];
                    else if (chosenAltName.equals("Work_2"))
                        ++ workersPartWork2[i];
                    else if (chosenAltName.equals("Work_univ"))
                        ++ workersPartWorkUniv[i];
                    else if (chosenAltName.equals("Univ_1"))
                        ++ workersPartUniv1[i];
                    else if (chosenAltName.equals("Non_mand"))
                        ++ workersPartNonMand[i];
                    else if (chosenAltName.equals("Home"))
                        ++ workersPartAtHome[i];
                }//next predriver in household
            }//end if
        }//next household
        //append part-time workers at home onto HH data file
        hhTable.appendColumn (workersPartWork1, "work_1_26");
        hhTable.appendColumn (workersPartWork2, "work_2_26");
        hhTable.appendColumn (workersPartWorkUniv, "work_univ_26");
        hhTable.appendColumn (workersPartUniv1, "univ_1_26");
        hhTable.appendColumn (workersPartNonMand, "non_mand_26");
        hhTable.appendColumn (workersPartAtHome, "home_26");
        //write out updated HH file to disk to be used as input
        //into Model26
        
        
		if (outputFile != null) {
			if (useMessageWindow) mw.setMessage2 ("Writing results to: " + outputFile );
			try {
                CSVFileWriter writer = new CSVFileWriter();
                writer.writeFile(hhTable, new File(outputFile), new DecimalFormat("#########"));
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		if (useMessageWindow) mw.setMessage3 ("end of Model 2.6");
		logger.info ("end of Model 2.6");

		if (useMessageWindow) mw.setVisible(false);
 
    }//end of main

}