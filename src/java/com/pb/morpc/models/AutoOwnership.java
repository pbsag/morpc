package com.pb.morpc.models;

import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.NDimensionalMatrixDouble;
import com.pb.morpc.matrix.MatrixUtil;
import com.pb.morpc.structures.MessageWindow;
import com.pb.morpc.structures.OutputDescription;
import com.pb.morpc.synpop.SyntheticPopulation;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Implements a test of the multinomial logit model for auto ownership choice
 * 
 * @author    Jim Hicks
 * @version   1.0, 3/10/2003
 *
 */
public class AutoOwnership {


    static Logger logger = Logger.getLogger("com.pb.morpc.models");

	HashMap propertyMap;
	boolean useMessageWindow = false;
	MessageWindow mw;


    public AutoOwnership( HashMap propertyMap ) {
        
		this.propertyMap = propertyMap;

		String useMessageWindowString = (String)propertyMap.get( "MessageWindow" );
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				useMessageWindow = true;
				this.mw = new MessageWindow ( "MORPC Auto Ownership Model Run Time Information" );
			}
		}

    }


    public void runAutoOwnership() {
    	
        int hh_id;
        int hh_taz_id;
        double[] utilities;


        //open files  
        String controlFile = (String)propertyMap.get( "Model1.controlFile" );
        String outputFile = (String)propertyMap.get( "Model1.outputFile" );

		String discretizeOption = (String)propertyMap.get( "Model1.discretize" );
        boolean discretize = false;
        if (discretizeOption.equalsIgnoreCase("true"))
            discretize = true;
        
        // create a new UEC to get utilties for this logit model
        AutoOwnershipUEC uec = new AutoOwnershipUEC(controlFile, propertyMap);
        uec.setDebug(false);
        
        // create a new auto ownership logit model object 
        AutoOwnershipLM model = new AutoOwnershipLM();

        // get the household data table from the UEC control file
        TableDataSet hhTable = uec.getHouseholdData();
        if (hhTable == null) {
            logger.fatal(
                "Could not get householdData TableDataSet from UEC in AutoOwnershipUEC.run().");
            System.exit(1);
        }

        int hh_idPosition = hhTable.getColumnPosition(SyntheticPopulation.HHID_FIELD);
        if (hh_idPosition <= 0) {
            logger.fatal(
                SyntheticPopulation.HHID_FIELD
                    + " was not a field in the householdData TableDataSet returned from UEC in AutoOwnershipUEC.run().");
            System.exit(1);
        }
        int hh_taz_idPosition =
            hhTable.getColumnPosition(SyntheticPopulation.HHTAZID_FIELD);
        if (hh_taz_idPosition <= 0) {
            logger.fatal(
                SyntheticPopulation.HHTAZID_FIELD
                    + " was not a field in the householdData TableDataSet returned from UEC in AutoOwnershipUEC.run().");
            System.exit(1);
        }

        double[][] fractions = new double[hhTable.getRowCount()][];
        float[][] altUtilities =
            new float[uec.getNumberOfAlternatives()][hhTable.getRowCount()];
        
        float[] chosenAlternatives = new float[hhTable.getRowCount()];
        // loop over all households in the hh table
        float tot = 0.0f;
        for (int i = 0; i < hhTable.getRowCount(); i++) {

            if (useMessageWindow) mw.setMessage1( "Auto Ownership Choice for hh " + (i + 1) + " of " + hhTable.getRowCount() );

            hh_id = (int) hhTable.getValueAt(i + 1, hh_idPosition);
            hh_taz_id = (int) hhTable.getValueAt(i + 1, hh_taz_idPosition);

            // get utilities for each alternative for this household		
            utilities = uec.getUtilities(hh_taz_id, hh_id);

            // attach utilities to the logit model for this household
            model.attachUtilities(utilities);

            // get the set of multinomial logit choice proportions
            fractions[i] = model.getProportions();

            for (int j = 0; j < utilities.length; j++) {
                altUtilities[j][i] = (float) utilities[j];
                tot += fractions[i][j];
            }
            if (discretize == false)
                chosenAlternatives[i] = (float) model.getAlternativeNumber();
        }

        if (discretize) {
            // adjust table of fractions so each row sums exactly to 1.0
            int maxAlt = 0;
            double maxAltValue = 0.0f;
            double totFrac = 0.0;
            for (int i = 0; i < hhTable.getRowCount(); i++) {

                maxAlt = 0;
                maxAltValue = fractions[i][0];
                totFrac = fractions[i][0];
                for (int j = 1; j < fractions[i].length; j++) {
                    if (fractions[i][j] > maxAltValue) {
                        maxAltValue = fractions[i][j];
                        maxAlt = j;
                    }
                    totFrac += fractions[i][j];
                }
                fractions[i][maxAlt] += (float) (1.0 - totFrac);
            }

            if (useMessageWindow) mw.setMessage1("Discretizing auto ownership model choices");

            // convert fractional proportions into crisp choices
            NDimensionalMatrixDouble choices2D = MatrixUtil.getNDimensionalMatrixDouble(fractions);
            NDimensionalMatrixDouble newChoices2D = choices2D.discretize();

            // create a new column to append to household table with result of this choice model
            float[] modelResult = new float[hhTable.getRowCount()];

			if (useMessageWindow) mw.setMessage2("Writing results to: " + outputFile);

            for (int j = 0; j < fractions[0].length; j++) {
                int[] vectorLocation = { -1, j };


                double[] choices = newChoices2D.getVectorAsDouble(vectorLocation);

                for (int i = 0; i < hhTable.getRowCount(); i++) {
                    if (choices[i] > 0.99999 & choices[i] < 1.00001)
                        modelResult[i] = j + 1;
                }
            }

            hhTable.appendColumn(modelResult, "M1");

        } else {
        	
			if (useMessageWindow) mw.setMessage1("making monte carlo choices");

            for (int j = 0; j < uec.getNumberOfAlternatives(); ++j) {

                float[] percents = new float[hhTable.getRowCount()];
                for (int hh = 0; hh < hhTable.getRowCount(); hh++) {

                    percents[hh] = (float) fractions[hh][j];
                }

           }
		   if (useMessageWindow) mw.setMessage2("Writing results to: " + outputFile);
            
            hhTable.appendColumn(chosenAlternatives, ("M1"));
         }


        // write updated household table to new output file  
        try {
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(hhTable, new File(outputFile), new DecimalFormat("#.000000"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

		if (useMessageWindow) mw.setMessage3("Printing Auto Ownership summary reports");
        String[] descriptions = OutputDescription.getDescriptions("M1");
        TableDataSet.logColumnFreqReport(
            "Auto Ownership",
            hhTable,
            hhTable.getColumnPosition("M1"), descriptions);

		if (useMessageWindow) mw.setMessage3("end of Auto Ownership Module");
        logger.debug("end of AutoOwnership");


		if (useMessageWindow) mw.setVisible(false);
    }

}
