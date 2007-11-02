package com.pb.morpc.models;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.math.MathUtil;
import com.pb.common.model.ChoiceModelApplication;
import com.pb.common.model.LogitModel;

import com.pb.morpc.structures.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * Created on Jul 22, 2003
 * @author Jim Hicks
 *
 */
public class SampleOfAlternatives implements java.io.Serializable {

	static Logger logger = Logger.getLogger("com.pb.morpc.models");
	HashMap propertyMap;

	ChoiceModelApplication cm =  null;
	
	UtilityExpressionCalculator uec;
	LogitModel root;
	String[] alternativeNames = null;

	private float slcLogsumCorrection = 0.0f;
	
	IndexValues index = new IndexValues();
	
	int sampleSize;

	int[] altFreq;
	
	int[] sample;
	int[] soaSample;
	float[] sampleCorrectionFactors;

	double[][] probabilitiesList = null;
	boolean modelTypeDc = true;


	public SampleOfAlternatives (HashMap propertyMap, String modelType, String controlFile, String outputFile, int modelSheet, int dataSheet) {
		this.propertyMap = propertyMap;

		buildSampleOfAlternativesChoiceModel (controlFile, outputFile, modelSheet, dataSheet);

		alternativeNames = uec.getAlternativeNames();
		altFreq = new int[uec.getNumberOfAlternatives()+1];
		
		if (modelType.equalsIgnoreCase( "slc" )) {
			sampleSize = getSlcSampleSize ();
			modelTypeDc = false;
		}
		else if (modelType.equalsIgnoreCase( "dc" )) {
			sampleSize = getDcSampleSize ();
			modelTypeDc = true;
		}
			
		soaSample = new int[uec.getNumberOfAlternatives()+1];
		Arrays.fill (soaSample, 1);

		this.probabilitiesList = new double[uec.getNumberOfAlternatives()+1][];

	}



	/*
	 * return the number of alternatives in the sample; read from properties file.
	 */
	private int getSlcSampleSize () {

		// get number of alternatives to include in the sample from the properties file.
		String sampleSizeString = (String)propertyMap.get( "slcSampleOfAltsSize" );
		if (sampleSizeString != null)
			sampleSize = Integer.parseInt( sampleSizeString );
		else
			sampleSize = uec.getNumberOfAlternatives();
			
		return sampleSize;

	}

	/*
	 * return the number of alternatives in the sample; read from properties file.
	 */
	private int getDcSampleSize () {

		// get number of alternatives to include in the sample from the properties file.
		String sampleSizeString = (String)propertyMap.get( "dcSampleOfAltsSize" );
		if (sampleSizeString != null)
			sampleSize = Integer.parseInt( sampleSizeString );
		else
			sampleSize = uec.getNumberOfAlternatives();
			
		return sampleSize;

	}

	private void buildSampleOfAlternativesChoiceModel (String controlFile, String outputFile, int modelSheet, int dataSheet) {
		
		// create an object for use in building the choice model
		cm =  new ChoiceModelApplication ( controlFile, outputFile, propertyMap, Household.class);

		// get a UEC for this choice model		
		uec = cm.getUEC(modelSheet, dataSheet);

		// get a LogitModel for this sample of alternatives choice model
		cm.createLogitModel();

	}


	public void clearProbabilitiesMap() {
	    for (int i=0; i < uec.getNumberOfAlternatives(); i++)
	        probabilitiesList[i] = null;
	}
	
	
	
	
	/*
	 * apply the sample of alternatives choice model to calculate the sample, sample probabilities, and sample frequencies
	 */
	public void applySampleOfAlternativesChoiceModel ( Household hh, boolean[] soaAvailability ) {

		int[] soaSample = new int[soaAvailability.length];

		ArrayList altList = new ArrayList(sampleSize);
		ArrayList freqList = new ArrayList(sampleSize);
		
		int chosenAlt = 0;

		
		Arrays.fill (altFreq, 0);

		for (int i=0; i < soaSample.length; i++) {
		    if (soaAvailability[i])
		        soaSample[i] = 1;
		    else
		        soaSample[i] = 0;
		}



		
	
        IndexValues index = new IndexValues();
        index.setHHIndex(hh.getID());
        index.setZoneIndex(hh.getTazID());
        index.setOriginZone(hh.getOrigTaz());
        index.setDestZone(hh.getChosenDest());

		// LogitModel objects are cached by origin taz.  If the origin taz is not cached,
		// recompute the LogitModel probabilities and cache the probabilities.
		if ( modelTypeDc ) {
			
			if ( probabilitiesList[hh.getOrigTaz()] == null ) {
                
				// calculate probabilities in the generic root LogitModel root, and get back tempRoot with utilities and availabilities updated.
				cm.updateLogitModel (hh, index, soaAvailability, soaSample);
				probabilitiesList[hh.getOrigTaz()] = cm.getProbabilities();

			}
			
		}
		else {
		    
			// calculate probabilities in the generic root LogitModel root, and get back tempRoot with utilities and availabilities updated.
			cm.updateLogitModel (hh, index, soaAvailability, soaSample);
			probabilitiesList[hh.getOrigTaz()] = cm.getProbabilities();

		}




		// loop over sampleSize, and select alternatives based on altProb[].
		// may include duplicate alternative selections.
		for (int i=1; i <= sampleSize; i++) {
			chosenAlt = ChoiceModelApplication.getMonteCarloSelection( probabilitiesList[hh.getOrigTaz()] ) + 1;
			altFreq[chosenAlt]++;
		}

		
		// create ArrayList where the elements are the unique chosen alternatives,
		// and the frequency with which those alternatives were chosen.
		int k = 0;
		for (int i=1; i < altFreq.length; i++) {
			if (altFreq[i] > 0) {
				altList.add(k, Integer.toString(i));
				freqList.add(k, Integer.toString(altFreq[i]));
				k++;
			}
		}
		

		sample = new int[altList.size()+1];
		sampleCorrectionFactors = new float[altList.size()+1];

		
		// loop through the ArrayList, construct a sample[] and a sampleCorrectionFactors[],
		// and compute the slcLogsumCorrection over the set of unique alternatives in the sample.
		float prob;
		int freq;
		int alt;
		for (k=0; k < altList.size(); k++) {
			alt = Integer.parseInt((String)altList.get(k));
			freq = Integer.parseInt((String)freqList.get(k));
			prob = (float)probabilitiesList[hh.getOrigTaz()][alt-1];  

			sample[k+1] = alt;
			sampleCorrectionFactors[k+1] = (float)MathUtil.log( (double)freq/prob ); 
		}

		slcLogsumCorrection = sampleSize;
		
	}


	/*
	 * apply the sample of alternatives choice model to calculate the sample, sample probabilities, and sample frequencies
	 */
	public void applySampleOfAlternativesChoiceModel ( Household hh ) {

		boolean[] soaAvailability = new boolean[uec.getNumberOfAlternatives()+1];
		int[] soaSample = new int[soaAvailability.length];

		ArrayList altList = new ArrayList(sampleSize);
		ArrayList freqList = new ArrayList(sampleSize);
		
		int chosenAlt = 0;

		Arrays.fill (soaAvailability, true);
		Arrays.fill (soaSample, 1);
		Arrays.fill (altFreq, 0);
	



		
	
        IndexValues index = new IndexValues();
        index.setHHIndex(hh.getID());
        index.setZoneIndex(hh.getTazID());
        index.setOriginZone(hh.getOrigTaz());
        index.setDestZone(hh.getChosenDest());

		// LogitModel objects are cached by origin taz.  If the origin taz is not cached,
		// recompute the LogitModel probabilities and cache the probabilities.
		if ( modelTypeDc ) {
			
			if ( probabilitiesList[hh.getOrigTaz()] == null ) {
	
				// calculate probabilities in the generic root LogitModel root, and get back tempRoot with utilities and availabilities updated.
				cm.updateLogitModel (hh, index, soaAvailability, soaSample);
				probabilitiesList[hh.getOrigTaz()] = cm.getProbabilities();

			}
			
		}
		else {
		    
			// calculate probabilities in the generic root LogitModel root, and get back tempRoot with utilities and availabilities updated.
			cm.updateLogitModel (hh, index, soaAvailability, soaSample);
			probabilitiesList[hh.getOrigTaz()] = cm.getProbabilities();

		}




		// loop over sampleSize, and select alternatives based on altProb[].
		// may include duplicate alternative selections.
		for (int i=1; i <= sampleSize; i++) {
			chosenAlt = ChoiceModelApplication.getMonteCarloSelection( probabilitiesList[hh.getOrigTaz()] ) + 1;
			altFreq[chosenAlt]++;
		}

		
		// create ArrayList where the elements are the unique chosen alternatives,
		// and the frequency with which those alternatives were chosen.
		int k = 0;
		for (int i=1; i < altFreq.length; i++) {
			if (altFreq[i] > 0) {
				altList.add(k, Integer.toString(i));
				freqList.add(k, Integer.toString(altFreq[i]));
				k++;
			}
		}
		

		sample = new int[altList.size()+1];
		sampleCorrectionFactors = new float[altList.size()+1];

		
		// loop through the ArrayList, construct a sample[] and a sampleCorrectionFactors[],
		// and compute the slcLogsumCorrection over the set of unique alternatives in the sample.
		float prob;
		int freq;
		int alt;
		for (k=0; k < altList.size(); k++) {
			alt = Integer.parseInt((String)altList.get(k));
			freq = Integer.parseInt((String)freqList.get(k));
			prob = (float)probabilitiesList[hh.getOrigTaz()][alt-1];  

			sample[k+1] = alt;
			sampleCorrectionFactors[k+1] = (float)MathUtil.log( (double)freq/prob ); 
		}

		slcLogsumCorrection = sampleSize;
		
	}


	/*
	 * return array with sample of alternatives
	 */
	public int[] getSampleOfAlternatives () {
		return sample;
	}


	/*
	 * return array with sample of alternatives correction factors
	 */
	public float[] getSampleCorrectionFactors () {
		return sampleCorrectionFactors;
	}


	/*
	 * return sample correction factor for stop location choice logsum.
	 */
	public float getSlcLogsumCorrectionFactor () {
		return slcLogsumCorrection;
	}


	/*
	 * return total number of alternatives in sample of alternatives choice model
	 */
	public int getTotalNumberOfAlternatives () {
		return uec.getNumberOfAlternatives();
	}


}