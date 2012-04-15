package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ChoiceModelApplication;
import com.pb.common.util.SeededRandom;

import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import java.io.*;


public class DTMModelBase implements java.io.Serializable {

//    protected static final int DEBUG_HH_ID = -1;
    protected static final int DEBUG_HH_ID = 2770;
    
	protected static Logger logger = Logger.getLogger(DTMModelBase.class);


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

    protected UtilityExpressionCalculator[] mcODUEC;
    protected UtilityExpressionCalculator[] smcUEC;
    
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

    protected int[] transitTourModeIndices = { 3, 4, 5, 6, 7, 8 };
    protected int[] localTransitTourModeIndices = { 3, 4, 5 };
    protected int[] premiumTransitTourModeIndices = { 6, 7, 8 };
	
    protected TableDataSet cbdAltsTable = null;
    
	
	
	// This array is declared as public static, because it is referenced by the Household
	// object acting as a DMU to pass information to the UEC.  The UEC control files make
	// references to this data via @ and @@ variables.
	// Since this array is instantiated in multiple parallel tasks within the same VM, and it
	// has to be static for the Household DMU, it is further dimensioned by processor ID so
	// that each task has its own static copy of the data.
	public static float[][] dcCorrections;

	


    // university tour mode choice alternatives:    Alt1    Alt2    Alt3    Alt4    Alt5
    //                                              SOV     HOV     WT      DT      NM      SB
	//private int[][] allocationUniv = { { 0, 1, 2, 3, 4 }, { 0, 1, 1, 2, 2 } };
	//private double[][] dispersionParametersUniv = { { 1.00, 1.00, 1.00, 1.00, 1.00 },
	//										{ 1.00, 0.53, 0.53, 0.53, 0.53 } };
    
    // school tour mode choice alternatives:    Alt1    Alt2    Alt3    Alt4    Alt5    Alt6
    //                                          SOV     HOV     WT      DT      NM      SB
	//protected int[][] allocationSchool = { { 0, 1, 2, 3, 4, 5 }, { 0, 1, 2, 3, 2, 3 } };
	//protected double[][] dispersionParametersSchool = { { 1.00, 1.00, 1.00, 1.00, 1.00, 1.00 },
	//											{ 1.00, 0.5333, 0.5333, 0.5333, 0.5333, 0.5333 } };

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

	
	
    protected static final int WWL_OB = 1;
    protected static final int PWL_OB = 2;
    protected static final int KWL_OB = 3;
    protected static final int WWP_OB = 4;
    protected static final int PWP_OB = 5;
    protected static final int KWP_OB = 6;
    protected static final int WWL_IB = 7;
    protected static final int WPL_IB = 8;
    protected static final int WKL_IB = 9;
    protected static final int WWP_IB = 10;
    protected static final int WPP_IB = 11;
    protected static final int WKP_IB = 12;
    protected static final int NUM_SUBMODE_SHEETS = 12;
	
    protected double[] obSubmodeUtility = new double[1];
    protected double[] ibSubmodeUtility = new double[1];
	
	
//	private HashMap[] modalODUtilityMap = null;
	
		
	
	// this constructor used to set processorIndex when called by a distributed application
	public DTMModelBase ( int processorId, HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {
		
	    this.processorIndex = processorId;
        this.propertyMap = propertyMap;
	    
	    logger.info ( "DTMModelBase constructor called with PINDEX=" + processorIndex);
	    
		initDTMModelBase ( tourTypeCategory, tourTypes );
	  
	}
	
	
	

	// this constructor used by a non-distributed application
	public DTMModelBase ( HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {
		
		this.processorIndex = 0;
        this.propertyMap = propertyMap;

        initDTMModelBase ( tourTypeCategory, tourTypes );
	  
	}
	
	
	public void setProcessorIndex( int index ){
	    processorIndex = index;
	}
	

	private void initDTMModelBase ( short tourTypeCategory, short[] tourTypes ) {

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
				soa[i][0] = new SampleOfAlternatives(propertyMap, "dc", (String)propertyMap.get ( "SoaDc.controlFile"), soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 2, 0);
				soa[i][1] = new SampleOfAlternatives(propertyMap, "dc", (String)propertyMap.get ( "SoaDc.controlFile"), soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 3, 0);
				soa[i][2] = new SampleOfAlternatives(propertyMap, "dc", (String)propertyMap.get ( "SoaDc.controlFile"), soaModelSheet, soaDataSheet);
			}
			else if (tourTypes[i] == TourType.ATWORK) {
				soa[i] = new SampleOfAlternatives[3];
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 1);
				soa[i][0] = new SampleOfAlternatives(propertyMap, "dc", (String)propertyMap.get ( "SoaDc.controlFile"), soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 2);
				soa[i][1] = new SampleOfAlternatives(propertyMap, "dc", (String)propertyMap.get ( "SoaDc.controlFile"), soaModelSheet, soaDataSheet);
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 3);
				soa[i][2] = new SampleOfAlternatives(propertyMap, "dc", (String)propertyMap.get ( "SoaDc.controlFile"), soaModelSheet, soaDataSheet);
			}
			else {
				soa[i] = new SampleOfAlternatives[1];
				defineSoaSheets (tourTypes[i], tourTypeCategory, 0, 0);
				soa[i][0] = new SampleOfAlternatives(propertyMap, "dc", (String)propertyMap.get ( "SoaDc.controlFile"), soaModelSheet, soaDataSheet);
			}
			
		}




		// create choice model objects and UECs for each purpose
		dc = new ChoiceModelApplication[tourTypes.length];
		tc = new ChoiceModelApplication[tourTypes.length];
		mc = new ChoiceModelApplication[tourTypes.length];
		pc = new ChoiceModelApplication[3];


        mcODUEC = new UtilityExpressionCalculator[tourTypes.length];


		int numDcAlternatives = 0;
		int numTcAlternatives = 0;
		int numMcAlternatives = 0;
		int numPcAlternatives = 0;
		
		for (int i=0; i < tourTypes.length; i++) {

			defineUECModelSheets (tourTypes[i], tourTypeCategory);
                 

			dc[i] =  new ChoiceModelApplication( (String)propertyMap.get ( "Model51.controlFile"), model5Sheet, m5DataSheet, propertyMap, Household.class);
			tc[i] =  new ChoiceModelApplication( (String)propertyMap.get ( "Model6.controlFile"), model6Sheet,  m6DataSheet, propertyMap, Household.class);
            
			// create UEC to calculate OD component of mode choice utilities - used by derived class
			logger.info ("Processor index " + processorIndex + " creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Mode Choice OD UECs");
			if (useMessageWindow) mw.setMessage1 ("Creating " + TourType.TYPE_LABELS[tourTypeCategory][i] + " Mode Choice OD UECs");
            mcODUEC[i] = new UtilityExpressionCalculator(new File( (String)propertyMap.get( "Model7.controlFile" ) ), model7ODSheet,  m7ODDataSheet, propertyMap, Household.class);
	
            mc[i] =  new ChoiceModelApplication( (String)propertyMap.get ( "Model7.controlFile"), model7Sheet,  m7DataSheet, propertyMap, Household.class);
			
//			modalODUtilityMap = new HashMap[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES];
//			for (int m=0; m < ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES; m++)
//				modalODUtilityMap[m] = new HashMap();

			

            /*
			// create logit model (nested logit model for univ & school) objects
			dc[i].createLogitModel();
			c[i].createLogitModel();


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
             */

            
            
			if (dc[i].getNumberOfAlternatives() > numDcAlternatives)
				numDcAlternatives = dc[i].getNumberOfAlternatives();
			if (tc[i].getNumberOfAlternatives() > numTcAlternatives)
				numTcAlternatives = tc[i].getNumberOfAlternatives();
			if (mc[i].getNumberOfAlternatives() > numMcAlternatives)
				numMcAlternatives = mc[i].getNumberOfAlternatives();
		}

		smcSample[0] = 1;
		smcSample[1] = 1;
	
		// create UECs for each transit tour mode - outbound and inbound
        // used by derived classes to implement trip mode allocations for tours with any transit tour mode choice.
		logger.info ("Creating Submode Choice UECs");
        smcUEC = new UtilityExpressionCalculator[NUM_SUBMODE_SHEETS+1];
		for (int i=1; i < smcUEC.length; i++)
			smcUEC[i] = new UtilityExpressionCalculator(new File( (String)propertyMap.get( "Model72.controlFile" ) ), i, 0, propertyMap, Household.class);


		// create UECs for each of the model 9 model sheets		
		logger.info ("Creating Parking Location Choice UECs");
		for (int i=0; i < 3; i++) {
			pc[i] =  new ChoiceModelApplication( (String)propertyMap.get ( "Model9.controlFile"), i+1, 0, propertyMap, Household.class );
			pc[i].createLogitModel();
		}

		if (pc[0].getNumberOfAlternatives() > numPcAlternatives)
			numPcAlternatives = pc[0].getNumberOfAlternatives();


        

        // read the parking choice alternatives data file to get alternatives names
        String cbdFile = (String)propertyMap.get( "CBDAlternatives.file" );

        try {
            CSVFileReader reader = new CSVFileReader();
            cbdAltsTable = reader.readFile(new File(cbdFile));
        }
        catch (IOException e) {
            logger.error ("problem reading table of cbd zones for parking location choice model.", e);
            System.exit(1);
        }


        
        
		mcAvailability = new boolean[numMcAlternatives+1];
		tcAvailability = new boolean[numTcAlternatives+1];
		dcAvailability = new boolean[numDcAlternatives+1];
		pcAvailability = new boolean[numPcAlternatives+1];




		dcSample = new int[numDcAlternatives+1];
		tcSample = new int[numTcAlternatives+1];
		mcSample = new int[numMcAlternatives+1];
		pcSample = new int[numPcAlternatives+1];
		dcCorrections = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numDcAlternatives+1];

		mcLogsumAvailability = new int[numMcAlternatives+1];
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



	protected float getMcLogsums ( Household hh, IndexValues dmuIndex, int tourTypeIndex ) {

		// first calculate the OD related mode choice utility
		setMcODUtility ( hh, dmuIndex, tourTypeIndex );

		// calculate the final mode choice utilities, exponentiate them, and calcualte the logsum
		// use  the computeUtilities method that uses default availabilty(all true) and sample(all 1) arrays
        mc[tourTypeIndex].computeUtilities ( hh, dmuIndex );

		//mc[tourTypeIndex].computeUtilities ( hh, dmuIndex, mcAvailability, mcLogsumAvailability );

		// return the mode choice logsum
		return (float)mc[tourTypeIndex].getLogsum();
	}


	protected void setMcODUtility ( Household hh, IndexValues dmuIndex, int tourTypeIndex ) {

		double[] ModalUtilities = null;

//		int todAlt = hh.getChosenTodAlt();
//		int startPeriod = TODDataManager.getTodStartPeriod( todAlt ); 
//		int startSkimPeriod = com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( startPeriod );		
//		int endPeriod = TODDataManager.getTodEndPeriod( todAlt ); 
//		int endSkimPeriod = com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( endPeriod );		

		
		
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

        
            try {
    			ModalUtilities = mcODUEC[tourTypeIndex].solve(dmuIndex, hh, mcLogsumAvailability);
    			
//    			if ( hh.getOrigTaz() == 121 && hh.getChosenDest() == 815 && hh.getTourCategory() == 3 ){
//                    ModalUtilities = mcODUEC[tourTypeIndex].solve(dmuIndex, hh, mcLogsumAvailability);
//                    logger.info("");
//    			    mcODUEC[tourTypeIndex].logAnswersArray(logger, "$$$$$ Mode Choice OD Utility Expression values for hh=" + hh.getID() + " $$$$$");
//    			    logger.info("$$$$$ End Debug Logging for Tour $$$$$");
//                    logger.info("");
//    			}
    		}
    		catch (java.lang.Exception e) {
    			logger.fatal ("runtime exception occurred in DTMModelBase.setMcODUtility() for household id=" + hh.getID(), e );
    			logger.fatal("");
    			logger.fatal("tourTypeIndex=" + tourTypeIndex);
    			logger.fatal("processorIndex=" + processorIndex);
    			logger.fatal("UEC NumberOfAlternatives=" + mcODUEC[tourTypeIndex].getNumberOfAlternatives());
    			logger.fatal("UEC MethodInvoker Source Code=");
    			logger.fatal(mcODUEC[tourTypeIndex].getMethodInvokerSourceCode());
    			logger.fatal("UEC MethodInvoker Variable Table=");
    			logger.fatal(mcODUEC[tourTypeIndex].getVariableTable());
    			logger.fatal("UEC AlternativeNames=" + mcODUEC[tourTypeIndex].getAlternativeNames());
    			String[] altNames = mcODUEC[tourTypeIndex].getAlternativeNames();
    			for (int i=0; i < altNames.length; i++)
    				logger.fatal( "[" + i + "]:  " + altNames[i] );
    			logger.fatal("");
    			hh.writeContentToLogger(logger);
    			logger.fatal("");
    			throw new RuntimeException();
    		}
			
//
//			modalODUtilityMap[processorIndex].put ( mapKey, ModalUtilities );
//
//		}


        ZonalDataManager.setOdUtilModeAlt (processorIndex, ModalUtilities);
		
		if ( processorIndex != hh.getProcessorIndex() ) {
			logger.fatal ( "processorIndex in DTMModelBase.setMcODUtility() = " + processorIndex );
			logger.fatal ( "processorIndex in hh object = " + hh.getProcessorIndex() );
			logger.fatal ( "the processorIndex values are expected to be the same.");
			throw new RuntimeException();
		}

	}


	private void defineUECModelSheets (int tourType, int tourCategory) {

//		final int M5_DATA_SHEET = 0;
//		final int M6_DATA_SHEET = 0;
//		final int M7_DATA_SHEET = 0;
//		final int M7_DATA_OD_SHEET = 1;
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

//        final int M71_MODEL_SHEET = 2;
//        final int M71_OD_UTIL_SHEET = 3;
//        final int M72_MODEL_SHEET = 4;
//        final int M72_OD_UTIL_SHEET = 5;
//        final int M73_MODEL_SHEET = 6;
//        final int M73_OD_UTIL_SHEET = 7;
//        final int M741_MODEL_SHEET = 8;
//        final int M741_OD_UTIL_SHEET = 9;
//        final int M742_MODEL_SHEET = 10;
//        final int M742_OD_UTIL_SHEET = 11;
//        final int M743_MODEL_SHEET = 12;
//        final int M743_OD_UTIL_SHEET = 13;
//        final int M744_MODEL_SHEET = 14;
//        final int M744_OD_UTIL_SHEET = 15;
//        final int M751_MODEL_SHEET = 16;
//        final int M751_OD_UTIL_SHEET = 17;
//        final int M752_MODEL_SHEET = 18;
//        final int M752_OD_UTIL_SHEET = 19;
//        final int M753_MODEL_SHEET = 20;
//        final int M753_OD_UTIL_SHEET = 21;
//        final int M754_MODEL_SHEET = 22;
//        final int M754_OD_UTIL_SHEET = 23;
//        final int M755_MODEL_SHEET = 24;
//        final int M755_OD_UTIL_SHEET = 25;
//        final int M76_MODEL_SHEET = 26;
//        final int M76_OD_UTIL_SHEET = 27;

        final int M71_MODEL_SHEET = 1;
        final int M71_OD_UTIL_SHEET = 2;
        final int M72_MODEL_SHEET = 3;
        final int M72_OD_UTIL_SHEET = 4;
        final int M73_MODEL_SHEET = 5;
        final int M73_OD_UTIL_SHEET = 6;
        final int M741_MODEL_SHEET = 8;
        final int M741_OD_UTIL_SHEET = 7;
        final int M742_MODEL_SHEET = 9;
        final int M742_OD_UTIL_SHEET = 7;
        final int M743_MODEL_SHEET = 10;
        final int M743_OD_UTIL_SHEET = 7;
        final int M744_MODEL_SHEET = 11;
        final int M744_OD_UTIL_SHEET = 7;
        final int M751_MODEL_SHEET = 13;
        final int M751_OD_UTIL_SHEET = 12;
        final int M752_MODEL_SHEET = 14;
        final int M752_OD_UTIL_SHEET = 12;
        final int M753_MODEL_SHEET = 15;
        final int M753_OD_UTIL_SHEET = 12;
        final int M754_MODEL_SHEET = 16;
        final int M754_OD_UTIL_SHEET = 12;
        final int M755_MODEL_SHEET = 17;
        final int M755_OD_UTIL_SHEET = 12;
        final int M76_MODEL_SHEET = 19;
        final int M76_OD_UTIL_SHEET = 18;


		// assign the model sheet numbers for the desired tour purpose
		m5DataSheet = 0;
		m6DataSheet = 0;
		m7ODDataSheet = 0;
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

//		final int M5_SOA_DATA_SHEET = 0;
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
	
    // if the tour mode argument is in the list of transit tour mode indices, return true, otherwise return false.
    protected boolean tourModeIsTransit( int tourModeIndex ){     
        for ( int index : transitTourModeIndices )
            if ( index == tourModeIndex )
                return true;
        
        return false;
    }

    // if the tour mode argument is in the list of local transit tour mode indices, return true, otherwise return false.
    protected boolean tourModeIsLocalTransit( int tourModeIndex ){     
        for ( int index : localTransitTourModeIndices )
            if ( index == tourModeIndex )
                return true;
        
        return false;
    }

    // if the tour mode argument is in the list of premium transit tour mode indices, return true, otherwise return false.
    protected boolean tourModeIsPremiumTransit( int tourModeIndex ){     
        for ( int index : premiumTransitTourModeIndices )
            if ( index == tourModeIndex )
                return true;
        
        return false;
    }

    
}
