package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.*;

import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import java.io.*;


public class DTMModelBase implements java.io.Serializable {

	protected static Logger logger = Logger.getLogger("com.pb.morpc.models");


	protected int processorIndex = 0;
    

	protected int model5Sheet  = 0;
	protected int model6Sheet  = 0;
	protected int model7Sheet  = 0;
	protected int model7ODSheet = 0;
	protected int m5DataSheet  = 0;
	protected int m6DataSheet  = 0;
	protected int m7DataSheet  = 0;
	protected int m7ODDataSheet = 0;

	protected int soaModelSheet  = 0;
	protected int soaDataSheet  = 0;


	protected int[] noTODAvailableIndiv = new int[TourType.TYPES];
	protected int[] noTODAvailableJoint = new int[TourType.TYPES];

	protected HashMap propertyMap;
	protected boolean useMessageWindow = false;
	protected MessageWindow mw;

	protected SampleOfAlternatives[][] soa = null;

	protected ChoiceModelApplication[] dc;
	protected ChoiceModelApplication[] tc;
	protected ChoiceModelApplication[] mc;
	protected ChoiceModelApplication[] pc;
	protected UtilityExpressionCalculator[] dcUEC;
	protected UtilityExpressionCalculator[] tcUEC;
	protected UtilityExpressionCalculator[] mcODUEC;
	protected UtilityExpressionCalculator[] mcUEC;
	protected UtilityExpressionCalculator[] smcUEC;
	protected UtilityExpressionCalculator[] pcUEC;

	protected float tcLogsumEaEa = 0.0f;
	protected float tcLogsumEaAm = 0.0f;
	protected float tcLogsumEaMd = 0.0f;
	protected float tcLogsumEaPm = 0.0f;
	protected float tcLogsumEaNt = 0.0f;
	protected float tcLogsumAmAm = 0.0f;
	protected float tcLogsumAmMd = 0.0f;
	protected float tcLogsumAmPm = 0.0f;
	protected float tcLogsumAmNt = 0.0f;
	protected float tcLogsumMdMd = 0.0f;
	protected float tcLogsumMdPm = 0.0f;
	protected float tcLogsumMdNt = 0.0f;
	protected float tcLogsumPmPm = 0.0f;
	protected float tcLogsumPmNt = 0.0f;
	protected float tcLogsumNtNt = 0.0f;

	protected double[] mcUtilities;

	protected boolean[] mcAvailability;
	protected boolean[] tcAvailability;
	protected boolean[] dcAvailability;
	protected boolean[] pcAvailability;
	
	protected int[] mcLogsumAvailability;
		
	protected int[] dcSample;
	protected int[] tcSample;
	protected int[] mcSample;
	protected int[] pcSample;
	protected int[] smcSample = new int[2];

	
	
	// This array is declared as public static, because it is referenced by the Household
	// object acting as a DMU to pass information to the UEC.  The UEC control files make
	// references to this data via @ and @@ variables.
	// Since this array is instantiated in multiple parallel tasks within the same VM, and it
	// has to be static for the Household DMU, it is further dimensioned by processor ID so
	// that each task has its own static copy of the data.
	public static float[][] dcCorrections;

	
	
	protected IndexValues index = new IndexValues();


	private int[][] allocationUniv = { { 0, 1, 2, 3, 4 }, { 0, 1, 1, 2, 2 } };
	private double[][] dispersionParametersUniv = { { 1.00, 1.00, 1.00, 1.00, 1.00 },
											{ 1.00, 0.53, 0.53, 0.53, 0.53 } };
	protected int[][] allocationSchool = { { 0, 1, 2, 3, 4, 5 }, { 0, 1, 2, 3, 2, 3 } };
	protected double[][] dispersionParametersSchool = { { 1.00, 1.00, 1.00, 1.00, 1.00, 1.00 },
												{ 1.00, 0.5333, 0.5333, 0.5333, 0.5333, 0.5333 } };

	protected int hh_id;
	protected int hh_taz_id;
	protected int person;
	protected int k=0;
	protected int start;
	protected int end;

	protected int preSampleCount = 0;
	protected int preSampleAlt = 0;
	protected int preSampleSize;		

	protected short tourTypeCategory; 
	protected short[] tourTypes; 

	protected double[] submodeUtility = new double[1];
	
	
	private HashMap[] modalODUtilityMap = null;
	
		
	
	// this constructor used to set processorIndex when called by a distributed application
	public DTMModelBase ( int processorId, HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {
		
	    this.processorIndex = processorId % ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES;
		initDTMModelBase ( propertyMap, tourTypeCategory, tourTypes );
	  
	}
	
	
	

	// this constructor used by a non-distributed application
	public DTMModelBase ( HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {
		
		this.processorIndex = 0;
		initDTMModelBase ( propertyMap, tourTypeCategory, tourTypes );
	  
	}
	
	
	

	private void initDTMModelBase ( HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {

		this.propertyMap = propertyMap;
		this.tourTypeCategory = tourTypeCategory; 
		this.tourTypes = tourTypes; 

		// get the indicator for whether to use the message window or not
		// from the properties file.
		String useMessageWindowString = (String)propertyMap.get( "MessageWindow" );
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				useMessageWindow = true;
				this.mw = new MessageWindow ( "MORPC Tour Destination, Time of Day, and Mode Choice Models" );
			}
		}


		this.preSampleSize = Integer.parseInt ( (String)propertyMap.get ( "dcPreSampleSize") );		

		// define a SampleOfAlternatives Object for use in destination choice for each purpose
		logger.info ("Creating sample of alternative choice UECs");
		soa = new SampleOfAlternatives[tourTypes.length+1][];
		for (int i=0; i < tourTypes.length; i++) {
					
			if (tourTypes[i] == TourType.WORK) {
				soa[i] = new SampleOfAlternatives[3];
				defineSoaSheets (tourTypes[i], tourTypeCategory, 1, 0);
				soa[i][0] = new SampleOfAlternatives(propertyMap, "dc", "SoaDc.controlFile", "SoaDc.outputFile", soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 2, 0);
				soa[i][1] = new SampleOfAlternatives(propertyMap, "dc", "SoaDc.controlFile", "SoaDc.outputFile", soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 3, 0);
				soa[i][2] = new SampleOfAlternatives(propertyMap, "dc", "SoaDc.controlFile", "SoaDc.outputFile", soaModelSheet, soaDataSheet);
			}
			else if (tourTypes[i] == TourType.ATWORK) {
				soa[i] = new SampleOfAlternatives[3];
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 1);
				soa[i][0] = new SampleOfAlternatives(propertyMap, "dc", "SoaDc.controlFile", "SoaDc.outputFile", soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 2);
				soa[i][1] = new SampleOfAlternatives(propertyMap, "dc", "SoaDc.controlFile", "SoaDc.outputFile", soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 3);
				soa[i][2] = new SampleOfAlternatives(propertyMap, "dc", "SoaDc.controlFile", "SoaDc.outputFile", soaModelSheet, soaDataSheet);
			}
			else {
				soa[i] = new SampleOfAlternatives[1];
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 0);
				soa[i][0] = new SampleOfAlternatives(propertyMap, "dc", "SoaDc.controlFile", "SoaDc.outputFile", soaModelSheet, soaDataSheet);
			}
			
		}




		// create choice model objects and UECs for each purpose
		dc = new ChoiceModelApplication[tourTypes.length];
		tc = new ChoiceModelApplication[tourTypes.length];
		mc = new ChoiceModelApplication[tourTypes.length];
		pc = new ChoiceModelApplication[3];
		dcUEC   = new UtilityExpressionCalculator[tourTypes.length];
		tcUEC   = new UtilityExpressionCalculator[tourTypes.length];
		mcODUEC = new UtilityExpressionCalculator[tourTypes.length];
		mcUEC   = new UtilityExpressionCalculator[tourTypes.length];
		pcUEC   = new UtilityExpressionCalculator[3];




		int numDcAlternatives = 0;
		int numTcAlternatives = 0;
		int numMcAlternatives = 0;
		int numPcAlternatives = 0;
		
		for (int i=0; i < tourTypes.length; i++) {

			defineUECModelSheets (tourTypes[i], tourTypeCategory);

			dc[i] =  new ChoiceModelApplication("Model51.controlFile", "Model51.outputFile", propertyMap);
			tc[i] =  new ChoiceModelApplication("Model6.controlFile", "Model6.outputFile", propertyMap);
			mc[i] =  new ChoiceModelApplication("Model7.controlFile", "Model7.outputFile", propertyMap);

			// create dest choice UEC
			logger.info ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Destination Choice UECs");
			if (useMessageWindow) mw.setMessage1 ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Destination Choice UECs");
			dcUEC[i] = dc[i].getUEC(model5Sheet, m5DataSheet);
	
			// create time-of-day choice UEC
			logger.info ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Time-of-Day Choice UECs");
			if (useMessageWindow) mw.setMessage1 ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Time-of-Day Choice UECs");
			tcUEC[i] = tc[i].getUEC(model6Sheet,  m6DataSheet);
	
			// create UEC to calculate OD component of mode choice utilities
			logger.info ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Mode Choice OD UECs");
			if (useMessageWindow) mw.setMessage1 ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Mode Choice OD UECs");
			mcODUEC[i] = mc[i].getUEC(model7ODSheet,  m7ODDataSheet);
	
			// create UEC to calculate OD component of mode choice utilities
			logger.info ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Mode Choice UECs");
			if (useMessageWindow) mw.setMessage1 ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Mode Choice UECs");
			mcUEC[i] = mc[i].getUEC(model7Sheet,  m7DataSheet);

			
			modalODUtilityMap = new HashMap[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES];
			for (int m=0; m < ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES; m++)
				modalODUtilityMap[m] = new HashMap();

			

			// create logit model (nested logit model for univ & school) objects
			dc[i].createLogitModel();
			tc[i].createLogitModel();


			if ( tourTypeCategory == TourType.MANDATORY_CATEGORY ) {
			
				if (tourTypes[i] == TourType.UNIVERSITY) {
					mc[i].createNestedLogitModel( allocationUniv, dispersionParametersUniv );
				}
				else if (tourTypes[i] == TourType.SCHOOL) {
					mc[i].createNestedLogitModel( allocationSchool, dispersionParametersSchool );
				}
				else {
					mc[i].createLogitModel();
				}
				
			}
			else {

				mc[i].createLogitModel();

			}


			if (dcUEC[i].getNumberOfAlternatives() > numDcAlternatives)
				numDcAlternatives = dcUEC[i].getNumberOfAlternatives();
			if (tcUEC[i].getNumberOfAlternatives() > numTcAlternatives)
				numTcAlternatives = tcUEC[i].getNumberOfAlternatives();
			if (mcUEC[i].getNumberOfAlternatives() > numMcAlternatives)
				numMcAlternatives = mcUEC[i].getNumberOfAlternatives();
		}

		smcSample[0] = 1;
		smcSample[1] = 1;
	
		// create UECs for each time period/main transit mode
		logger.info ("Creating Submode Choice UECs");
		smcUEC = new UtilityExpressionCalculator[12];
		for (int i=0; i < 12; i++)
			smcUEC[i] = new UtilityExpressionCalculator(new File( (String)propertyMap.get( "Model72.controlFile" ) ), i+1, 0, propertyMap, Household.class);


		// create UECs for each of the model 9 model sheets		
		logger.info ("Creating Parking Location Choice UECs");
		for (int i=0; i < 3; i++) {
			pc[i] =  new ChoiceModelApplication( "Model9.controlFile", "Model9.outputFile", propertyMap );
			pcUEC[i] = pc[i].getUEC( i+1, 0 );
			pc[i].createLogitModel();
		}

		if (pcUEC[0].getNumberOfAlternatives() > numPcAlternatives)
			numPcAlternatives = pcUEC[0].getNumberOfAlternatives();



		mcAvailability = new boolean[numMcAlternatives+1];
		tcAvailability = new boolean[numTcAlternatives+1];
		dcAvailability = new boolean[numDcAlternatives+1];
		pcAvailability = new boolean[numPcAlternatives+1];




		dcSample = new int[numDcAlternatives+1];
		tcSample = new int[numTcAlternatives+1];
		mcSample = new int[numMcAlternatives+1];
		pcSample = new int[numPcAlternatives+1];
		dcCorrections = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numDcAlternatives+1];

		int[] mcLogsumAvailability = new int[numDcAlternatives+1];
		Arrays.fill (mcLogsumAvailability, 1);
		Arrays.fill (dcAvailability, true);
		Arrays.fill (pcSample, 1);
		Arrays.fill (pcAvailability, true);


		logger.info( "DTM for category " + tourTypeCategory);
    }


	protected void setTcAvailability (Person person, boolean[] tcAvailability, int[] tcSample) {

		int start;
		int end;

		boolean[] personAvailability = person.getAvailable();

		for (int p=1; p < tcAvailability.length; p++) {

			start = com.pb.morpc.models.TODDataManager.getTodStartHour (p);
			end = com.pb.morpc.models.TODDataManager.getTodEndHour (p);

			// if any hour between the start and end of tod combination p is unavailable,
			// the combination is unavailable.
			for (int j=start; j <= end; j++) {
				if (!personAvailability[j]) {
					tcAvailability[p] = false;
					tcSample[p] = 0;
					break;
				}
			}
		}
	}



	protected float getMcLogsums ( Household hh, int tourTypeIndex ) {

		// first calculate the OD related mode choice utility
		setMcODUtility ( hh, tourTypeIndex );

		// calculate the final mode choice utilities, exponentiate them, and calcualte the logsum
		mc[tourTypeIndex].updateLogitModel ( hh, mcAvailability, mcSample );

		// return the mode choice logsum
		return (float)mc[tourTypeIndex].getLogsum();
	}


	protected void setMcODUtility ( Household hh, int tourTypeIndex ) {

		double[] ModalUtilities;

		int todAlt = hh.getChosenTodAlt();
		int startPeriod = TODDataManager.getTodStartPeriod( todAlt ); 
		int startSkimPeriod = com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( startPeriod );		
		int endPeriod = TODDataManager.getTodEndPeriod( todAlt ); 
		int endSkimPeriod = com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( endPeriod );		

		
		
//		String mapKey = Integer.toString(tourTypeIndex) + "_"
//						+ Integer.toString(startSkimPeriod) + "_"
//						+ Integer.toString(endSkimPeriod) + "_"
//						+ Integer.toString(hh.getOrigTaz()) + "_"
//						+ Integer.toString(hh.getChosenDest() );
//		
//
//		if ( modalODUtilityMap[processorIndex].containsKey( mapKey ) ) {
//			
//			ModalUtilities = (double[]) modalODUtilityMap[processorIndex].get( mapKey );
//
//		}
//		else {
//
			index.setOriginZone( hh.getOrigTaz() );
			index.setDestZone( hh.getChosenDest() );
			index.setZoneIndex( hh.getTazID() );
			index.setHHIndex( hh.getID() );

			ModalUtilities = mcODUEC[tourTypeIndex].solve(index, hh, mcLogsumAvailability);
//
//			modalODUtilityMap[processorIndex].put ( mapKey, ModalUtilities );
//
//		}

			ZonalDataManager.setOdUtilModeAlt (processorIndex, ModalUtilities);

	}


	private void defineUECModelSheets (int tourType, int tourCategory) {

		final int M5_DATA_SHEET = 0;
		final int M6_DATA_SHEET = 0;
		final int M7_DATA_SHEET = 0;
		final int M7_DATA_OD_SHEET = 1;
		final int M51_MODEL_SHEET = 1;
		final int M52_MODEL_SHEET = 2;
		final int M53_MODEL_SHEET = 3;
		final int M541_MODEL_SHEET = 4;
		final int M542_MODEL_SHEET = 5;
		final int M543_MODEL_SHEET = 6;
		final int M544_MODEL_SHEET = 7;
		final int M551_MODEL_SHEET = 8;
		final int M552_MODEL_SHEET = 9;
		final int M553_MODEL_SHEET = 10;
		final int M554_MODEL_SHEET = 11;
		final int M555_MODEL_SHEET = 12;
		final int M56_MODEL_SHEET = 13;
		final int M61_MODEL_SHEET = 1;
		final int M62_MODEL_SHEET = 2;
		final int M63_MODEL_SHEET = 3;
		final int M64_MODEL_SHEET = 4;
		final int M651_MODEL_SHEET = 5;
		final int M652_MODEL_SHEET = 6;
		final int M66_MODEL_SHEET = 7;
		final int M71_MODEL_SHEET = 2;
		final int M71_OD_UTIL_SHEET = 3;
		final int M72_MODEL_SHEET = 4;
		final int M72_OD_UTIL_SHEET = 5;
		final int M73_MODEL_SHEET = 6;
		final int M73_OD_UTIL_SHEET = 7;
		final int M741_MODEL_SHEET = 8;
		final int M741_OD_UTIL_SHEET = 9;
		final int M742_MODEL_SHEET = 10;
		final int M742_OD_UTIL_SHEET = 11;
		final int M743_MODEL_SHEET = 12;
		final int M743_OD_UTIL_SHEET = 13;
		final int M744_MODEL_SHEET = 14;
		final int M744_OD_UTIL_SHEET = 15;
		final int M751_MODEL_SHEET = 16;
		final int M751_OD_UTIL_SHEET = 17;
		final int M752_MODEL_SHEET = 18;
		final int M752_OD_UTIL_SHEET = 19;
		final int M753_MODEL_SHEET = 20;
		final int M753_OD_UTIL_SHEET = 21;
		final int M754_MODEL_SHEET = 22;
		final int M754_OD_UTIL_SHEET = 23;
		final int M755_MODEL_SHEET = 24;
		final int M755_OD_UTIL_SHEET = 25;
		final int M76_MODEL_SHEET = 26;
		final int M76_OD_UTIL_SHEET = 27;


		// assign the model sheet numbers for the desired tour purpose
		m5DataSheet = 0;
		m6DataSheet = 0;
		m7ODDataSheet = 1;
		m7DataSheet = 0;
		if (tourCategory == 1) {
			if (tourType == TourType.WORK) {
				model5Sheet  = M51_MODEL_SHEET;
				model6Sheet  = M61_MODEL_SHEET;
				model7Sheet  = M71_MODEL_SHEET;
				model7ODSheet = M71_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.UNIVERSITY) {
				model5Sheet  = M52_MODEL_SHEET;
				model6Sheet  = M62_MODEL_SHEET;
				model7Sheet  = M72_MODEL_SHEET;
				model7ODSheet = M72_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.SCHOOL) {
				model5Sheet  = M53_MODEL_SHEET;
				model6Sheet  = M63_MODEL_SHEET;
				model7Sheet  = M73_MODEL_SHEET;
				model7ODSheet = M73_OD_UTIL_SHEET;
			}
		}
		else if (tourCategory == 2) {
			if (tourType == TourType.OTHER_MAINTENANCE) {
				model5Sheet  = M542_MODEL_SHEET;
				model6Sheet  = M64_MODEL_SHEET;
				model7Sheet  = M742_MODEL_SHEET;
				model7ODSheet = M742_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.SHOP) {
				model5Sheet  = M541_MODEL_SHEET;
				model6Sheet  = M64_MODEL_SHEET;
				model7Sheet  = M741_MODEL_SHEET;
				model7ODSheet = M741_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.DISCRETIONARY) {
				model5Sheet  = M543_MODEL_SHEET;
				model6Sheet  = M64_MODEL_SHEET;
				model7Sheet  = M743_MODEL_SHEET;
				model7ODSheet = M743_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.EAT) {
				model5Sheet  = M544_MODEL_SHEET;
				model6Sheet  = M64_MODEL_SHEET;
				model7Sheet  = M744_MODEL_SHEET;
				model7ODSheet = M744_OD_UTIL_SHEET;
			}
		}
		else if (tourCategory == 3) {
			if (tourType == TourType.ESCORTING) {
				model5Sheet  = M551_MODEL_SHEET;
				model6Sheet  = M651_MODEL_SHEET;
				model7Sheet  = M751_MODEL_SHEET;
				model7ODSheet = M751_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.SHOP) {
				model5Sheet  = M552_MODEL_SHEET;
				model6Sheet  = M652_MODEL_SHEET;
				model7Sheet  = M752_MODEL_SHEET;
				model7ODSheet = M752_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.OTHER_MAINTENANCE) {
				model5Sheet  = M553_MODEL_SHEET;
				model6Sheet  = M652_MODEL_SHEET;
				model7Sheet  = M753_MODEL_SHEET;
				model7ODSheet = M753_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.DISCRETIONARY) {
				model5Sheet  = M554_MODEL_SHEET;
				model6Sheet  = M652_MODEL_SHEET;
				model7Sheet  = M754_MODEL_SHEET;
				model7ODSheet = M754_OD_UTIL_SHEET;
			}
			else if (tourType == TourType.EAT) {
				model5Sheet  = M555_MODEL_SHEET;
				model6Sheet  = M652_MODEL_SHEET;
				model7Sheet  = M755_MODEL_SHEET;
				model7ODSheet = M755_OD_UTIL_SHEET;
			}
		}
		else if (tourCategory == 4) {
			if (tourType == TourType.ATWORK) {
				model5Sheet  = M56_MODEL_SHEET;
				model6Sheet  = M66_MODEL_SHEET;
				model7Sheet  = M76_MODEL_SHEET;
				model7ODSheet = M76_OD_UTIL_SHEET;
			}
		}
	}



	private void defineSoaSheets (int tourType, int tourCategory, int income, int atworkType) {

		final int M5_SOA_DATA_SHEET = 0;
		final int M511_SOA_SHEET = 1;
		final int M512_SOA_SHEET = 2;
		final int M513_SOA_SHEET = 3;
		final int M52_SOA_SHEET  = 4;
		final int M53_SOA_SHEET  = 5;
		final int M541_SOA_SHEET = 6;
		final int M542_SOA_SHEET = 7;
		final int M543_SOA_SHEET = 8;
		final int M544_SOA_SHEET = 9;
		final int M551_SOA_SHEET = 10;
		final int M552_SOA_SHEET = 11;
		final int M553_SOA_SHEET = 12;
		final int M554_SOA_SHEET = 13;
		final int M555_SOA_SHEET = 14;
		final int M561_SOA_SHEET = 15;
		final int M562_SOA_SHEET = 16;
		final int M563_SOA_SHEET = 17;


		soaDataSheet = 0;
		if (tourCategory == 1) {
			if (tourType == TourType.WORK) {
				if (income == 1)
					soaModelSheet  = M511_SOA_SHEET;
				else if (income == 2)
					soaModelSheet  = M512_SOA_SHEET;
				else if (income == 3)
					soaModelSheet  = M513_SOA_SHEET;
			}
			else if (tourType == TourType.UNIVERSITY) {
				soaModelSheet  = M52_SOA_SHEET;
			}
			else if (tourType == TourType.SCHOOL) {
				soaModelSheet  = M53_SOA_SHEET;
			}
		}
		else if (tourCategory == 2) {
			if (tourType == TourType.SHOP) {
				soaModelSheet  = M541_SOA_SHEET;
			}
			else if (tourType == TourType.OTHER_MAINTENANCE) {
				soaModelSheet  = M542_SOA_SHEET;
			}
			else if (tourType == TourType.DISCRETIONARY) {
				soaModelSheet  = M543_SOA_SHEET;
			}
			else if (tourType == TourType.EAT) {
				soaModelSheet  = M544_SOA_SHEET;
			}
		}
		else if (tourCategory == 3) {
			if (tourType == TourType.ESCORTING) {
				soaModelSheet  = M551_SOA_SHEET;
			}
			else if (tourType == TourType.SHOP) {
				soaModelSheet  = M552_SOA_SHEET;
			}
			else if (tourType == TourType.OTHER_MAINTENANCE) {
				soaModelSheet  = M553_SOA_SHEET;
			}
			else if (tourType == TourType.DISCRETIONARY) {
				soaModelSheet  = M554_SOA_SHEET;
			}
			else if (tourType == TourType.EAT) {
				soaModelSheet  = M555_SOA_SHEET;
			}
		}
		else if (tourCategory == 4) {
			if (tourType == TourType.ATWORK) {
				if (atworkType == 1)
					soaModelSheet = M561_SOA_SHEET;
				else if (atworkType == 2)
					soaModelSheet = M562_SOA_SHEET;
				else if (atworkType == 3)
					soaModelSheet = M563_SOA_SHEET;
			}
		}

	}


	/**
	 * copy the static members of this class into a HashMap.  The non-static HashMap
	 * will be serialized in a message and sent to client VMs in a distributed application.
	 * Static data itself is not serialized with the object, so the extra step is necessary.
	 * 
	 * @return
	 */
	public HashMap createStaticDataMap () {
	    
		HashMap staticDataMap = new HashMap();

		staticDataMap.put ( "dcCorrections", dcCorrections );
	    
		return staticDataMap;
	    
	}

	
	/**
	 * set values in the static members of this class from data in a HashMap.
	 * 
	 * @return
	 */
	public void setStaticData ( HashMap staticDataMap ) {
	    
		dcCorrections     = (float[][])staticDataMap.get ( "dcCorrections" );

	}
	
	
	
}
