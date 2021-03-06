package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Tour mode choice model application class
 */
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.SeededRandom;

import java.io.File;
import org.apache.log4j.Logger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;


public class ChoiceModelApplication implements java.io.Serializable {

	static Logger logger = Logger.getLogger(ChoiceModelApplication.class);


    HashMap propertyMap;
    

	// get the array of alternatives for setting utilities
	private ConcreteAlternative[] alts;
	private String[] alternativeNames = null;
	private int numberOfAlternatives=0;

	private UtilityExpressionCalculator uec = null;
	double[] utilities = null;
	private LogitModel root = null;
	
	
			
	// the rootLogsum is calculated when utilities are exponentiated
	private double rootLogsum = 0.0;
	private int availabilityCount = 0;
		
    private Class dmuObject=null;
    
	String controlFile;
	String outputFile;



	public ChoiceModelApplication (String controlFile, String outputFile, HashMap propertyMap, Class dmuObject) {

		//open properties files
		this.controlFile = (String)propertyMap.get( controlFile );
		this.outputFile = (String)propertyMap.get( outputFile );
		
		this.propertyMap = propertyMap;
        
        this.dmuObject = dmuObject;
	}


	
	/*
	 * create a UEC for applying the tour mode choice model
	 */
	public UtilityExpressionCalculator getUEC(int modelSheet, int dataSheet) {
		
		// create a UEC to get utilties for this choice model class
		uec = new UtilityExpressionCalculator(new File(this.controlFile), modelSheet, dataSheet, propertyMap, dmuObject);

		// get the list of concrete alternatives from this uec
		alts= new ConcreteAlternative[uec.getNumberOfAlternatives()];
		alternativeNames = uec.getAlternativeNames();
		numberOfAlternatives = uec.getNumberOfAlternatives();

		return uec;
	}
	
	
	/*
	 * create a LogitModel object for the tour mode choice model
	 */
	public LogitModel createLogitModel() {

		// create and define a new LogitModel object
		root = new LogitModel("root", numberOfAlternatives);

		for(int i=0; i < numberOfAlternatives; i++) {
			alts[i]  = new ConcreteAlternative(alternativeNames[i], new Integer(i+1));
			root.addAlternative (alts[i]);
		}

		return root;
	}
	
	
	/*
	 * create a LogitModel object for the tour mode choice model
	 */
	public LogitModel createNestedLogitModel(int[][] allocation, double[][] dispersionParameters) {

		
		// create and define a new LogitModel object
		root= new LogitModel("root", numberOfAlternatives);

		for(int i=0; i < numberOfAlternatives; i++)
			alts[i]  = new ConcreteAlternative(alternativeNames[i], new Integer(i+1));



		// tree structure defines nested logit model hierarchy.
		// alternatives are numbered starting at 1.
		// values in allocation[0][i] refers to elemental alternatives in nested logit model.
		// values in allocation[level][i] refers to parent branch number within level.

		int level = allocation.length - 1;
		root = buildNestedLogitModel (level, allocation, dispersionParameters);
		
		return root;
	}
	

	private LogitModel buildNestedLogitModel (int level, int[][] allocation, double[][] dispersionParameters) {

		int a=0;

		// level is the index number for the arrays in the allocation array for the current nesting level
		int newLevel;
		int[][] newAllocation = new int[level][];
		double[][] newDispersionParameters = new double[level][];
		LogitModel lm = null;
		LogitModel newLm = null;

		// find the maximum alternative number in the current nesting level
		int maxAlt = 0;
		int minAlt = 999999999;
		for (int i=0; i < allocation[level].length; i++) {
			if (allocation[level][i] > maxAlt)
				maxAlt = allocation[level][i];
			else if (allocation[level][i] < minAlt)
				minAlt = allocation[level][i];
		}

		// create an array of branches for each alternative up to the altCount
		ArrayList[] branchAlts = new ArrayList[maxAlt+1];
		for (int i=0; i <= maxAlt; i++)
			branchAlts[i] = new ArrayList();

		// add alllocation[level] element numbers to the ArrayLists for each branch
		int altCount = 0;
		for (int i=0; i < allocation[level].length; i++) {
			if (branchAlts[allocation[level][i]].size() == 0)
				altCount++;
			branchAlts[allocation[level][i]].add(Integer.toString(i) );
		}
			
		// create a LogitModel for this level
		// with the number of unique alternatives determined from allocation[level].
		lm = new LogitModel( "level_"+level+"_alt_"+minAlt+"_to_"+maxAlt, altCount );
			
				
		boolean[] altSet = new boolean[maxAlt+1];
		Arrays.fill (altSet, false);

		
		for (int i=0; i <= maxAlt; i++) {
			if (branchAlts[i].size() == 0)
				continue;
				
			if (branchAlts[i].size() > 1) {
				lm.setDispersionParameter(1.0/dispersionParameters[level][0]);
				for (int k=0; k < level; k++) {
					newAllocation[k] = new int[branchAlts[i].size()];
					newDispersionParameters[k] = new double[branchAlts[i].size()];
					for (int j=0; j < branchAlts[i].size(); j++) {
						newAllocation[k][j] = allocation[k][Integer.parseInt( (String)branchAlts[i].get(j) )];
						newDispersionParameters[k][j] = dispersionParameters[k][Integer.parseInt( (String)branchAlts[i].get(j) )];
					}
				}
				newLevel = level - 1;				
				newLm = buildNestedLogitModel (newLevel, newAllocation, newDispersionParameters);
				lm.addAlternative(newLm);
			}
			else {
				a = allocation[level][Integer.parseInt( (String)branchAlts[i].get(0) )];
				if ( altSet[a] == false) {
					lm.addAlternative(alts[a]);
					altSet[a] = true;
				}
			}
		}

		return lm;
	}


	
	/*
	 * calculate utilities and update utilities and availabilities in the logit model passed in
	 */
	public LogitModel updateLogitModel (Object hh, IndexValues index, boolean[] availability, int[] sample) {

	    boolean debug = false;
	    
		// get utilities for each alternative for this household
		utilities = uec.solve( index, hh, sample );


		//set utility for each alternative
		availabilityCount = 0;
		for(int a=0; a < alts.length; a++){
			alts[a].setAvailability( availability[a+1] );
			if (sample[a+1] == 1 && availability[a+1])
				alts[a].setAvailability( (utilities[a] > -99.0) );
			alts[a].setUtility( utilities[a] );
			if (sample[a+1] == 1 && availability[a+1] && utilities[a] > -99.0)
				availabilityCount++;
		}
		
		
		
//		int dummy=0;
//		if (availabilityCount == 0) {
//		    dummy=1;
//			utilities = uec.solve( index, hh, sample );
//		}
		
        
		
		if (debug) {
		 
			for(int a=0; a < alts.length; a++) {
			    
				if (sample[a+1] == 1 && availability[a+1] && utilities[a] > -99.0) {
                    int origTaz = index.getOriginZone();
                    logger.info ( "a=" + a + ", origTaz=" + origTaz + ", ringOrig=" + ZonalDataManager.rings[3*origTaz] + ", ringDestAlt=" + ZonalDataManager.rings[a] + ", suprdistOrig=" + ZonalDataManager.suprdist[3*origTaz] + ", suprdistDestAlt=" + ZonalDataManager.suprdist[a] + ", utilities[a]=" + utilities[a] );
                }

			}
			
		}
		
		
		
		root.computeAvailabilities();

		// call root.getUtility() to calculate exponentiated utilties.  The logit model logsum is returned.
		rootLogsum =  root.getUtility();
		
		// calculate logit probabilities
		root.calculateProbabilities();
		
		return root;
	}


	/*
	 * apply the logit choice UEC to calculate the logsum for this household's choice model
	 */
	public double getLogsum() {

		return rootLogsum;

	}
	
	
	/*
	 * return the array of probabilities for this logit choice model
	 */
	public double[] getProbabilities() {

	    double[] probabilities = new double[numberOfAlternatives];
	    double[] tempProbabilities = root.getProbabilities();
	    
	    for (int i=0; i < numberOfAlternatives; i++)
	        probabilities[i] = tempProbabilities[i]; 
	    
		return probabilities;

	}
	
	
	public int getAvailabilityCount () {
		return availabilityCount;
	}



	/*
	 * apply the tour mode choice UEC to calculate the logit choice probabilities
	 * and return a chosen alternative for this household's tour mode choice
	 */
	public int getChoiceResult() {

		int chosenAlt = 0;

        ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
		String chosenAltName= chosen.getName();

		// save chosen alternative in  householdChoice Array
		for(int a=0; a < alts.length; a++) {
			if (chosenAltName.equals(alternativeNames[a])) {
				chosenAlt = a+1;
				break;
			}
		}

		return chosenAlt;

	}

	
	public static int getMonteCarloSelection (double[] probabilities) {
	    
		double randomNumber = SeededRandom.getRandom();
		int returnValue = 0;

		double sum = probabilities[0];
		for (int i=0; i < probabilities.length-1; i++) {
		    if (randomNumber <= sum) {
		        returnValue = i;
		        break;
		    }
		    else {
		        sum += probabilities[i+1];
				returnValue = i+1;
		    }
		}
		
		return returnValue;
		
	}
	
}
