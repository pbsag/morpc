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
import java.util.logging.Logger;

public class Model24 {

    static Logger logger = Logger.getLogger("com.pb.morpc.models");

	static final int MODEL_24_SHEET = 1;
	static final int DATA_24_SHEET = 0;

	HashMap propertyMap;
	boolean useMessageWindow = false;
	MessageWindow mw;
	
	private IndexValues index = new IndexValues();
	private int[] sample;
	
    public Model24 (HashMap propertyMap) {
        
		this.propertyMap = propertyMap;
		
		// get the indicator for whether to use the message window or not
		// from the properties file.
		String useMessageWindowString = (String)propertyMap.get( "MessageWindow" );
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				this.useMessageWindow = true;
				this.mw = new MessageWindow ("MORPC Daily Activity Pattern (M24) Run Time Information");
			}
		}
    }



    public void runStudentDailyActivityPatternChoice() {

        int hh_id;
        int hh_taz_id;

          
        //open files  
        String controlFile = (String)propertyMap.get( "Model24.controlFile" );
        String outputFile = (String)propertyMap.get( "Model24.outputFile" );
        logger.fine(controlFile);
        logger.fine(outputFile);



        // create a new UEC to get utilties for this logit model
		UtilityExpressionCalculator uec = new UtilityExpressionCalculator ( new File(controlFile), MODEL_24_SHEET, DATA_24_SHEET, propertyMap, Household.class );
        int numberOfAlternatives=uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

		sample = new int[uec.getNumberOfAlternatives()+1];
		
        // create and define a new LogitModel object
//		LogitModel root= new LogitModel("root", numberOfAlternatives);
		LogitModel root= new LogitModel("root", 4);
        ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];

        for(int i=0;i<numberOfAlternatives;i++){
            logger.fine("alternative "+(i+1)+" is "+alternativeNames[i]);
            alts[i]  = new ConcreteAlternative(alternativeNames[i], new Integer(i+1));
            logger.fine(alternativeNames[i]+" has been added to the root");
        }
        
        LogitModel work = new LogitModel("work", 2);
        LogitModel university = new LogitModel("university", 3);
        
        work.addAlternative(alts[0]);
        work.addAlternative(alts[1]);
        university.addAlternative(alts[2]);
        university.addAlternative(alts[3]);
        university.addAlternative(alts[4]);
        root.addAlternative (work);
        root.addAlternative (university);
        root.addAlternative(alts[5]);
        root.addAlternative(alts[6]);    
            
        work.setDispersionParameter(1.0/0.25);
        university.setDispersionParameter(1.0/0.25);
        
        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();

        // get the household data table from the UEC control file
        TableDataSet hhTable = uec.getHouseholdData();
        if (hhTable == null) {
            logger.fine ("Could not get householdData TableDataSet from UEC");
            System.exit(1);
        }

        int hh_idPosition = hhTable.getColumnPosition( SyntheticPopulation.HHID_FIELD );
        if (hh_idPosition <= 0) {
            logger.fine (SyntheticPopulation.HHID_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }
        int hh_taz_idPosition = hhTable.getColumnPosition( SyntheticPopulation.HHTAZID_FIELD );
        if (hh_taz_idPosition <= 0) {
            logger.fine (SyntheticPopulation.HHTAZID_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        int student_idPosition = hhTable.getColumnPosition( SyntheticPopulation.STUDENTS_FIELD );
        if (student_idPosition <= 0) {
            logger.fine (SyntheticPopulation.STUDENTS_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        //define vectors which will hold results of linked attributes
        //which need to be appended to hhTable for use in next model
        float[] studentsWork1=new float[hhTable.getRowCount()];
        float[] studentsWork2=new float[hhTable.getRowCount()];
        float[] studentsUniv1=new float[hhTable.getRowCount()];
        float[] studentsUniv2=new float[hhTable.getRowCount()];
        float[] studentsUnivWork=new float[hhTable.getRowCount()];
        float[] studentsNonMand=new float[hhTable.getRowCount()];
        float[] studentsAtHome=new float[hhTable.getRowCount()];

        // loop over all households and all students in household
        //to make alternative choice.
		for (int i=0; i < hhTable.getRowCount(); i++) {
			if (useMessageWindow) mw.setMessage1 ("Model24 Choice for hh " + (i+1) + " of " + hhTable.getRowCount() );

			hh_id = (int)hhTable.getValueAt( i+1, hh_idPosition );
			hh_taz_id = (int)hhTable.getValueAt( i+1, hh_taz_idPosition );
            // check for students in the household
            if ( (int)hhTable.getValueAt( i+1, student_idPosition ) > 0 ) {

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


                //loop over number of students in household
                for (int m=0; m < (int)hhTable.getValueAt( i+1, student_idPosition ); m++){
                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();
                    if (chosenAltName.equals("Work_1"))
                        ++ studentsWork1[i];
                    else if (chosenAltName.equals("Work_2"))
                        ++ studentsWork2[i];
                    else if (chosenAltName.equals("Univ_1"))
                        ++ studentsUniv1[i];
                    else if (chosenAltName.equals("Univ_2"))
                        ++ studentsUniv2[i];
                    else if (chosenAltName.equals("Univ_work"))
                        ++ studentsUnivWork[i];
                    else if (chosenAltName.equals("Non_mand"))
                        ++ studentsNonMand[i];
                    else if (chosenAltName.equals("Home"))
                        ++ studentsAtHome[i];
                }//next predriver in household
            }//end if
        }//next household
        //append students at home onto HH data file
        hhTable.appendColumn (studentsWork1, "work_1_24");
        hhTable.appendColumn (studentsWork2, "work_2_24");
        hhTable.appendColumn (studentsUniv1, "univ_1_24");
        hhTable.appendColumn (studentsUniv2, "univ_2_24");
        hhTable.appendColumn (studentsUnivWork, "univ_work_24");
        hhTable.appendColumn (studentsNonMand, "non_mand_24");
        hhTable.appendColumn (studentsAtHome, "home_24");
        //write out updated HH file to disk to be used as input
        //into Model24
        
        
		if (outputFile != null) {
			if (useMessageWindow) mw.setMessage2 ("Writing results to: " + outputFile );
			try {
                CSVFileWriter writer = new CSVFileWriter();
                writer.writeFile(hhTable, new File(outputFile), new DecimalFormat("#.000000"));
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		if (useMessageWindow) mw.setMessage3 ("end of Model 2.4");
		logger.info ("end of Model 2.4");
 
		if (useMessageWindow) mw.setVisible(false);

    }//end of main

}