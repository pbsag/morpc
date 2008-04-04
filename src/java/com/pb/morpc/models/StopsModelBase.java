package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */
import com.pb.common.calculator.IndexValues;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ChoiceModelApplication;
import com.pb.morpc.structures.TourType;
import com.pb.morpc.structures.SubTourType;
import com.pb.morpc.structures.MessageWindow;
import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.ZoneTableFields;

import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import java.io.*;


public class StopsModelBase implements java.io.Serializable {

	protected static Logger logger = Logger.getLogger("com.pb.morpc.models");


	protected int processorIndex = 0;
    
	protected TableDataSet todAltsTable;
	protected TableDataSet zoneTable;


	protected int model81Sheet  = 0;
	protected int[][][] model82Sheet = new int[2][2][TourType.TYPES+1];
	protected int m81DataSheet  = 0;
	protected int m82DataSheet  = 0;

	protected int soaModelSheet  = 0;
	protected int soaDataSheet  = 0;
	protected int slcSoaModelSheet  = 0;
	protected int slcSoaDataSheet  = 0;


	private float[][] stopAttractions;
	private float[][] regionalSize;


	// These arrays are declared as public static, because they are referenced by the Household
	// object, acting as a DMU to pass information to the UEC.  The UEC control files make
	// references to these data via @ and @@ variables.
	// Since these arrays are instantiated in multiple parallel tasks within the same VM, and they
	// have to be static for the Household DMU, they are further indexed by processor ID so
	// that each task has its own static copy of the data..
	public static float[][][][] stopSize = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][2][TourType.TYPES+1][];
	public static float[][][] stopTotSize = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][2][];
	public static float[][] slcLogsum = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][2];
	public static float[][][] slcCorrections = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][2][];



	protected float[] parkRate;
	protected float[] urbType;
	protected float[] cnty;
	protected float[] schdist;


	protected int numberOfZones;
	
	protected transient HashMap propertyMap;
	protected boolean useMessageWindow = false;
	protected MessageWindow mw;


	protected int[][] soaSample = new int[2][];
	protected float[][] soaCorrections = new float[2][];


	protected SampleOfAlternatives[][][] slcSoa = null;

	protected ChoiceModelApplication sfc;
	protected ChoiceModelApplication slc[][][];
	protected ChoiceModelApplication distc;

	
	protected int[] sfcSample;
	protected int[][] slcSample = new int[2][];
	protected int[] distSample = new int[6+1];


	protected int[] sortedRandomNumberIndices;
	protected int[] preSampleRandomNumbers;
	protected boolean[] preSampleAvailability;
	protected boolean[] sfcAvailability;
	protected boolean[] slcOBAvailability;
	protected boolean[] slcIBAvailability;
	
	protected IndexValues index = new IndexValues();

	protected int hh_id;
	protected int hh_taz_id;
	protected int person;
	protected int k=0;
	protected int tourType;
	protected int durationPosition;
	
	protected short tourTypeCategory; 
	protected short[] tourTypes; 
	protected short[] halfTourTypes; 

	
	
	
	
	// this constructor used by a non-distributed application
	public StopsModelBase ( HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {
	    
	    this.processorIndex = 0;
		initStopsModelBase ( propertyMap, tourTypeCategory, tourTypes );

	}
	
	
	// this constructor used to set processorIndex when called by a distributed application
	public StopsModelBase ( int processorId, HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {

	    this.processorIndex = processorId;

	    logger.info ( "StopsModelBase constructor called with PINDEX=" + processorIndex);
	    
	    initStopsModelBase ( propertyMap, tourTypeCategory, tourTypes );
	
	}
	
	
	
	
	public void initStopsModelBase ( HashMap propertyMap, short tourTypeCategory, short[] tourTypes ) {

		this.propertyMap = propertyMap;
		this.tourTypeCategory = tourTypeCategory; 
		this.tourTypes = tourTypes; 
		this.halfTourTypes = new short[2];
		this.halfTourTypes[0] = 0;
		this.halfTourTypes[1] = 1;

		int numSfcAlternatives = 0;
		int numSlcAlternatives = 0;


		// get the indicator for whether to use the message window or not
		// from the properties file.
		String useMessageWindowString = (String)propertyMap.get(  "MessageWindow");
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				useMessageWindow = true;
				this.mw = new MessageWindow ( "MORPC Stop Frequency, Stop Location, and Trip Mode Choice Models" );
			}
		}



		// build the zonal data table
		String zonalFile = (String)propertyMap.get( "TAZData.file");

		try {
            CSVFileReader reader = new CSVFileReader();
            zoneTable = reader.readFile(new File(zonalFile));
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		getZoneRelatedData ( zoneTable );	
		
		this.numberOfZones = zoneTable.getRowCount();


		regionalSize = new float[2][];

		calculateStopDensity ( zoneTable );
		




		// create choice model objects and UECs for stop frequency and stop location choices, inbound and outbound.
		slc   = new ChoiceModelApplication[2][2][TourType.TYPES+1];


		defineUECModelSheets (tourTypeCategory);

		sfc =  new ChoiceModelApplication( (String)propertyMap.get ( "Model81.controlFile"), model81Sheet, m81DataSheet, propertyMap, Household.class);
		sfc.createLogitModel();
		if (sfc.getNumberOfAlternatives() > numSfcAlternatives)
			numSfcAlternatives = sfc.getNumberOfAlternatives();


		for (int i=0; i < 2; i++) {
			for (int j=0; j < 2; j++) {
				
				if (tourTypeCategory != TourType.AT_WORK_CATEGORY) {
					for (int k=0; k < tourTypes.length; k++) {

						slc[i][j][tourTypes[k]] =  new ChoiceModelApplication( (String)propertyMap.get ( "Model82.controlFile"), model82Sheet[i][j][tourTypes[k]],  m82DataSheet, propertyMap, Household.class);
						slc[i][j][tourTypes[k]].createLogitModel();

						if (slc[i][j][tourTypes[k]].getNumberOfAlternatives() > numSlcAlternatives)
							numSlcAlternatives = slc[i][j][tourTypes[k]].getNumberOfAlternatives();

					}
				}
				else {
					for (int k=0; k < SubTourType.SUB_TOUR_TYPES.length; k++) {

						slc[i][j][SubTourType.SUB_TOUR_TYPES[k]] =  new ChoiceModelApplication( (String)propertyMap.get ( "Model82.controlFile"), model82Sheet[i][j][SubTourType.SUB_TOUR_TYPES[k]],  m82DataSheet, propertyMap, Household.class);
						slc[i][j][SubTourType.SUB_TOUR_TYPES[k]].createLogitModel();

						if (slc[i][j][SubTourType.SUB_TOUR_TYPES[k]].getNumberOfAlternatives() > numSlcAlternatives)
							numSlcAlternatives = slc[i][j][SubTourType.SUB_TOUR_TYPES[k]].getNumberOfAlternatives();

					}
				}
			}
		}

	

		sfcAvailability = new boolean[numSfcAlternatives+1];
		slcOBAvailability = new boolean[numSlcAlternatives+1];
		slcIBAvailability = new boolean[numSlcAlternatives+1];
		preSampleAvailability = new boolean[numSlcAlternatives+1];
		preSampleRandomNumbers = new int[numSlcAlternatives];
		sortedRandomNumberIndices = new int[numSlcAlternatives];

		slcCorrections[processorIndex][0] = new float[numSlcAlternatives+1];
		slcCorrections[processorIndex][1] = new float[numSlcAlternatives+1];
		slcSample[0] = new int[numSlcAlternatives+1];
		slcSample[1] = new int[numSlcAlternatives+1];
		sfcSample = new int[numSfcAlternatives+1];
		Arrays.fill ( slcSample[0], 1 );
		Arrays.fill ( slcSample[1], 1 );
		Arrays.fill ( sfcSample, 1 );


		// define a SampleOfAlternatives Object for use in stop location choice for each purpose
		slcSoa = new SampleOfAlternatives[tourTypes.length+1][2][];
		for (int i=0; i < tourTypes.length; i++) {
					
			if (tourTypes[i] == TourType.WORK) {
				slcSoa[i][0] = new SampleOfAlternatives[3];
				slcSoa[i][1] = new SampleOfAlternatives[3];
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 1, 0, 0);
				slcSoa[i][0][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 1, 1, 0);
				slcSoa[i][1][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 2, 0, 0);
				slcSoa[i][0][1] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 2, 1, 0);
				slcSoa[i][1][1] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 3, 0, 0);
				slcSoa[i][0][2] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 3, 1, 0);
				slcSoa[i][1][2] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
			}
			else if (tourTypes[i] == TourType.UNIVERSITY || tourTypes[i] == TourType.SCHOOL) {
				slcSoa[i][0] = new SampleOfAlternatives[1];
				slcSoa[i][1] = new SampleOfAlternatives[1];
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 0, 0);
				slcSoa[i][0][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 1, 0);
				slcSoa[i][1][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
			}
			else if (tourTypes[i] == TourType.ATWORK) {
				slcSoa[i][0] = new SampleOfAlternatives[3];
				slcSoa[i][1] = new SampleOfAlternatives[3];
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 0, 1);
				slcSoa[i][0][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 1, 1);
				slcSoa[i][1][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 0, 2);
				slcSoa[i][0][1] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 1, 2);
				slcSoa[i][1][1] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 0, 3);
				slcSoa[i][0][2] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 1, 3);
				slcSoa[i][1][2] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
			}
			else {
				slcSoa[i][0] = new SampleOfAlternatives[1];
				slcSoa[i][1] = new SampleOfAlternatives[1];
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 0, 0);
				slcSoa[i][0][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
				defineSlcSoaSheets (tourTypes[i], tourTypeCategory, 0, 1, 0);
				slcSoa[i][1][0] = new SampleOfAlternatives(propertyMap, "slc", (String)propertyMap.get ( "SoaDc.controlFile"), slcSoaModelSheet, slcSoaDataSheet);
			}
			
		}


		logger.info( "Stop frequency and stop location for category " + tourTypeCategory);
    }



	private void defineUECModelSheets (int tourCategory) {

		final int M81_DATA_SHEET = 0;
		final int M82_DATA_SHEET = 0;
		final int M81_MANDATORY_MODEL_SHEET = 1;
		final int M81_NON_MANDATORY_MODEL_SHEET = 2;
		final int M81_JOINT_MODEL_SHEET = 3;
		final int M81_ATWORK_MODEL_SHEET = 4;
		

		// assign the model sheet numbers for the stop frequency choice sheets
		m81DataSheet = 0;
		m82DataSheet = 0;
		if (tourCategory == 1)
			model81Sheet  = M81_MANDATORY_MODEL_SHEET;
		else if (tourCategory == 2)
			model81Sheet  = M81_JOINT_MODEL_SHEET;
		if (tourCategory == 3)
			model81Sheet  = M81_NON_MANDATORY_MODEL_SHEET;
		else if (tourCategory == 4)
			model81Sheet  = M81_ATWORK_MODEL_SHEET;


		if (tourCategory == TourType.MANDATORY_CATEGORY) {
			model82Sheet[0][0][TourType.WORK] = 1;
			model82Sheet[0][1][TourType.WORK] = 2;
			model82Sheet[0][0][TourType.UNIVERSITY] = 3;
			model82Sheet[0][1][TourType.UNIVERSITY] = 4;
			model82Sheet[0][0][TourType.SCHOOL] = 5;
			model82Sheet[0][1][TourType.SCHOOL] = 6;
			model82Sheet[1][0][TourType.WORK] = 31;
			model82Sheet[1][1][TourType.WORK] = 32;
			model82Sheet[1][0][TourType.UNIVERSITY] = 33;
			model82Sheet[1][1][TourType.UNIVERSITY] = 34;
			model82Sheet[1][0][TourType.SCHOOL] = 35;
			model82Sheet[1][1][TourType.SCHOOL] = 36;
		}
		else if (tourCategory == TourType.NON_MANDATORY_CATEGORY) {
			model82Sheet[0][0][TourType.ESCORTING] = 7;
			model82Sheet[0][1][TourType.ESCORTING] = 8;
			model82Sheet[0][0][TourType.SHOP] = 9;
			model82Sheet[0][1][TourType.SHOP] = 10;
			model82Sheet[0][0][TourType.OTHER_MAINTENANCE] = 11;
			model82Sheet[0][1][TourType.OTHER_MAINTENANCE] = 12;
			model82Sheet[0][0][TourType.DISCRETIONARY] = 13;
			model82Sheet[0][1][TourType.DISCRETIONARY] = 14;
			model82Sheet[0][0][TourType.EAT] = 15;
			model82Sheet[0][1][TourType.EAT] = 16;
			model82Sheet[1][0][TourType.ESCORTING] = 37;
			model82Sheet[1][1][TourType.ESCORTING] = 38;
			model82Sheet[1][0][TourType.SHOP] = 39;
			model82Sheet[1][1][TourType.SHOP] = 40;
			model82Sheet[1][0][TourType.OTHER_MAINTENANCE] = 41;
			model82Sheet[1][1][TourType.OTHER_MAINTENANCE] = 42;
			model82Sheet[1][0][TourType.DISCRETIONARY] = 43;
			model82Sheet[1][1][TourType.DISCRETIONARY] = 44;
			model82Sheet[1][0][TourType.EAT] = 45;
			model82Sheet[1][1][TourType.EAT] = 46;
		}
		else if (tourCategory == TourType.JOINT_CATEGORY) {
			model82Sheet[0][0][TourType.SHOP] = 17;
			model82Sheet[0][1][TourType.SHOP] = 18;
			model82Sheet[0][0][TourType.OTHER_MAINTENANCE] = 19;
			model82Sheet[0][1][TourType.OTHER_MAINTENANCE] = 20;
			model82Sheet[0][0][TourType.DISCRETIONARY] = 21;
			model82Sheet[0][1][TourType.DISCRETIONARY] = 22;
			model82Sheet[0][0][TourType.EAT] = 23;
			model82Sheet[0][1][TourType.EAT] = 24;
			model82Sheet[1][0][TourType.SHOP] = 47;
			model82Sheet[1][1][TourType.SHOP] = 48;
			model82Sheet[1][0][TourType.OTHER_MAINTENANCE] = 49;
			model82Sheet[1][1][TourType.OTHER_MAINTENANCE] = 50;
			model82Sheet[1][0][TourType.DISCRETIONARY] = 51;
			model82Sheet[1][1][TourType.DISCRETIONARY] = 52;
			model82Sheet[1][0][TourType.EAT] = 53;
			model82Sheet[1][1][TourType.EAT] = 54;
		}
		else if (tourCategory == TourType.AT_WORK_CATEGORY) {
			model82Sheet[0][0][SubTourType.WORK] = 25;
			model82Sheet[0][1][SubTourType.WORK] = 26;
			model82Sheet[0][0][SubTourType.OTHER] = 27;
			model82Sheet[0][1][SubTourType.OTHER] = 28;
			model82Sheet[0][0][SubTourType.EAT] = 29;
			model82Sheet[0][1][SubTourType.EAT] = 30;
			model82Sheet[1][0][SubTourType.WORK] = 55;
			model82Sheet[1][1][SubTourType.WORK] = 56;
			model82Sheet[1][0][SubTourType.OTHER] = 57;
			model82Sheet[1][1][SubTourType.OTHER] = 58;
			model82Sheet[1][0][SubTourType.EAT] = 59;
			model82Sheet[1][1][SubTourType.EAT] = 60;
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


	private void defineSlcSoaSheets (int tourType, int tourCategory, int income, int dir, int atworkType) {

		final int M8_SOA_DATA_SHEET = 0;
		final int M8110_SOA_SHEET = 1;
		final int M8111_SOA_SHEET = 2;
		final int M8120_SOA_SHEET = 3;
		final int M8121_SOA_SHEET = 4;
		final int M8130_SOA_SHEET = 5;
		final int M8131_SOA_SHEET = 6;
		final int M820_SOA_SHEET = 7;
		final int M821_SOA_SHEET = 8;
		final int M830_SOA_SHEET = 9;
		final int M831_SOA_SHEET = 10;
		final int M841_SOA_SHEET = 11;
		final int M842_SOA_SHEET = 12;
		final int M843_SOA_SHEET = 13;
		final int M844_SOA_SHEET = 14;
		final int M851_SOA_SHEET = 15;
		final int M852_SOA_SHEET = 16;
		final int M853_SOA_SHEET = 17;
		final int M854_SOA_SHEET = 18;
		final int M855_SOA_SHEET = 19;
		final int M861_SOA_SHEET = 20;
		final int M862_SOA_SHEET = 21;
		final int M863_SOA_SHEET = 22;


		slcSoaDataSheet = 0;
		if (tourCategory == 1) {
			if (tourType == TourType.WORK) {
				if (income == 1)
					if (dir == 0)
						slcSoaModelSheet  = M8110_SOA_SHEET;
					else
						slcSoaModelSheet  = M8111_SOA_SHEET;
				else if (income == 2)
					if (dir == 0)
						slcSoaModelSheet  = M8120_SOA_SHEET;
					else
						slcSoaModelSheet  = M8121_SOA_SHEET;
				else if (income == 3)
					if (dir == 0)
						slcSoaModelSheet  = M8130_SOA_SHEET;
					else
						slcSoaModelSheet  = M8131_SOA_SHEET;
			}
			else if (tourType == TourType.UNIVERSITY) {
				if (dir == 0)
					slcSoaModelSheet  = M820_SOA_SHEET;
				else
					slcSoaModelSheet  = M821_SOA_SHEET;
			}
			else if (tourType == TourType.SCHOOL) {
				if (dir == 0)
					slcSoaModelSheet  = M830_SOA_SHEET;
				else
					slcSoaModelSheet  = M831_SOA_SHEET;
			}
		}
		else if (tourCategory == 2) {
			if (tourType == TourType.SHOP) {
				slcSoaModelSheet  = M841_SOA_SHEET;
			}
			else if (tourType == TourType.OTHER_MAINTENANCE) {
				slcSoaModelSheet  = M842_SOA_SHEET;
			}
			else if (tourType == TourType.DISCRETIONARY) {
				slcSoaModelSheet  = M843_SOA_SHEET;
			}
			else if (tourType == TourType.EAT) {
				slcSoaModelSheet  = M844_SOA_SHEET;
			}
		}
		else if (tourCategory == 3) {
			if (tourType == TourType.ESCORTING) {
				slcSoaModelSheet  = M851_SOA_SHEET;
			}
			else if (tourType == TourType.SHOP) {
				slcSoaModelSheet  = M852_SOA_SHEET;
			}
			else if (tourType == TourType.OTHER_MAINTENANCE) {
				slcSoaModelSheet  = M853_SOA_SHEET;
			}
			else if (tourType == TourType.DISCRETIONARY) {
				slcSoaModelSheet  = M854_SOA_SHEET;
			}
			else if (tourType == TourType.EAT) {
				slcSoaModelSheet  = M855_SOA_SHEET;
			}
		}
		else if (tourCategory == 4) {
			if (tourType == TourType.ATWORK) {
				if (atworkType == 1)
					slcSoaModelSheet = M861_SOA_SHEET;
				else if (atworkType == 2)
					slcSoaModelSheet = M862_SOA_SHEET;
				else if (atworkType == 3)
					slcSoaModelSheet = M863_SOA_SHEET;
			}
		}

	}


	private void getZoneRelatedData ( TableDataSet zoneTable ) {

		int k;


		int parkRateFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.PARKRATE );
		if (parkRateFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.PARKRATE + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		parkRate = new float[zoneTable.getRowCount()+1];
		for (int i=1; i <= zoneTable.getRowCount(); i++)
			parkRate[i] = zoneTable.getValueAt( i, parkRateFieldPosition );


		int urbtypeFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.URBTYPE );
		if (urbtypeFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.URBTYPE + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		k = 1;
		float uType;
		urbType = new float[3*zoneTable.getRowCount()+1];
		for (int i=1; i <= zoneTable.getRowCount(); i++) {
			uType = zoneTable.getValueAt( i, urbtypeFieldPosition );
			for (int j=1; j <= 3; j++) {
				urbType[k] = uType;
				k++;
			}
		}


		int countyFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.COUNTY );
		if (countyFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.COUNTY + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		k = 1;
		float co;
		cnty = new float[3*zoneTable.getRowCount()+1];
		for (int i=1; i <= zoneTable.getRowCount(); i++) {
			co = zoneTable.getValueAt( i, countyFieldPosition );
			for (int j=1; j <= 3; j++) {
				cnty[k] = co;
				k++;
			}
		}


		int schdistFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.SCHDIST );
		if (schdistFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.SCHDIST + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		k = 1;
		float sd;
		schdist = new float[3*zoneTable.getRowCount()+1];
		for (int i=1; i <= zoneTable.getRowCount(); i++) {
			sd = zoneTable.getValueAt( i, schdistFieldPosition );
			for (int j=1; j <= 3; j++) {
				schdist[k] = sd;
				k++;
			}
		}


		// get file names from properties file
//		String walkAccessFile = (String)propertyMap.get(  "WalkAccess.file");
//
//		int taz;
//		float waShrt, waLong;
//		float[] shrtArray = new float[zoneTable.getRowCount()+1];
//		float[] longArray = new float[zoneTable.getRowCount()+1];
//		walkPctArray = new float[3][zoneTable.getRowCount()+1];
//		Arrays.fill (walkPctArray[0], 1.0f);
//		Arrays.fill (walkPctArray[1], 0.0f);
//		Arrays.fill (walkPctArray[2], 0.0f);
//
//
//		if (walkAccessFile != null) {
//			try {
//                CSVFileReader reader = new CSVFileReader();
//				reader.setDelimSet( " ,\t\n\r\f\"");
//                TableDataSet wa = reader.readFile(new File(walkAccessFile));
//
//				int tazPosition = wa.getColumnPosition( "TAZ" );
//				if (tazPosition <= 0) {
//					logger.fatal( "TAZ was not a field in the walk access TableDataSet built from " + walkAccessFile + ".");
//					System.exit(1);
//				}
//
//				int shrtPosition = wa.getColumnPosition( "SHRT" );
//				if (shrtPosition <= 0) {
//					logger.fatal( "SHRT was not a field in the walk access TableDataSet built from " + walkAccessFile + ".");
//					System.exit(1);
//				}
//
//				int longPosition = wa.getColumnPosition( "LONG" );
//				if (longPosition <= 0) {
//					logger.fatal( "LONG was not a field in the walk access TableDataSet built from " + walkAccessFile + ".");
//					System.exit(1);
//				}
//
//				for (int j=1; j <= wa.getRowCount(); j++) {
//					taz = (int)wa.getValueAt( j, tazPosition );
//					shrtArray[taz] = wa.getValueAt( j, shrtPosition );
//					longArray[taz] = wa.getValueAt( j, longPosition );
//					walkPctArray[1][taz] = shrtArray[taz];
//					walkPctArray[2][taz] = longArray[taz];
//					walkPctArray[0][taz] = (float)(1.0 - (shrtArray[taz] + longArray[taz]));
//				}
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//				System.exit(1);
//			}
//		}
//		else {
//			logger.fatal( "no walk access zonal data file was named in properties file.");
//			System.exit(1);
//		}
//
//
//
//		// set 0/1 values for zone doesn't/does have short walk access for all dc alternatives
//		k = 1;
//		zonalShortAccess = new float[3*zoneTable.getRowCount()+1];
//		for (int i=1; i <= zoneTable.getRowCount(); i++) {
//			for (int j=0; j < 3; j++) {
//				zonalShortAccess[k] = ZonalDataManager.getWalkPct(j,i) > 0.0 ? 1 : 0;
//				k++;
//			}
//		}

		this.zoneTable = zoneTable;		
	}




	private void calculateStopDensity ( TableDataSet zoneTable ) {

		final int MAX_PURPOSE_CODE = 32;
		final int MAX_URBANTYPE_CODE = 3;
		final int MAX_AREATYPE_CODE = 7;
		final int NUMBER_OF_FIELDS = 7;
		
		int purp;
		int ut;
		int at;

		double[][][][] coeff = new double[MAX_PURPOSE_CODE+1][MAX_URBANTYPE_CODE+1][MAX_AREATYPE_CODE+1][NUMBER_OF_FIELDS];


		// get file name from properties file
		String stopDensityModelsFile = (String)propertyMap.get(  "StopDensityModels.file");

		// read the stop density models file to get field coefficients
		if (stopDensityModelsFile != null) {
			try {
                CSVFileReader reader = new CSVFileReader();
                TableDataSet sd = reader.readFile(new File(stopDensityModelsFile));

				int purpFieldPosition = sd.getColumnPosition( "purpose" );
				if (purpFieldPosition <= 0) {
					logger.fatal( "purpose was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int utFieldPosition = sd.getColumnPosition( "urbtype" );
				if (utFieldPosition <= 0) {
					logger.fatal( "urbtype was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int atFieldPosition = sd.getColumnPosition( "areatype" );
				if (atFieldPosition <= 0) {
					logger.fatal( "areatype was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int totpopFieldPosition = sd.getColumnPosition( "totalpop" );
				if (totpopFieldPosition <= 0) {
					logger.fatal( "totalpop was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int totempFieldPosition = sd.getColumnPosition( "totemp" );
				if (totempFieldPosition <= 0) {
					logger.fatal( "totemp was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int empretgFieldPosition = sd.getColumnPosition( "retail_g" );
				if (empretgFieldPosition <= 0) {
					logger.fatal( "retail_g was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int empretsFieldPosition = sd.getColumnPosition( "retail_s" );
				if (empretsFieldPosition <= 0) {
					logger.fatal( "retail_s was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int empoffFieldPosition = sd.getColumnPosition( "office_e" );
				if (empoffFieldPosition <= 0) {
					logger.fatal( "office_e was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int schenrFieldPosition = sd.getColumnPosition( "sch_enrol" );
				if (schenrFieldPosition <= 0) {
					logger.fatal( "sch_enrol was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}

				int unienrFieldPosition = sd.getColumnPosition( "uni_enrol" );
				if (unienrFieldPosition <= 0) {
					logger.fatal( "uni_enrol was not a field in the stop density model TableDataSet.");
					System.exit(1);
				}


				for (int i=1; i <= sd.getRowCount(); i++) {

					purp = (int)sd.getValueAt( i, purpFieldPosition );
					ut = (int)sd.getValueAt( i, utFieldPosition );
					at = (int)sd.getValueAt( i, atFieldPosition );

					coeff[purp][ut][at][0] = sd.getValueAt( i, totpopFieldPosition );
					coeff[purp][ut][at][1] = sd.getValueAt( i, totempFieldPosition );
					coeff[purp][ut][at][2] = sd.getValueAt( i, empretgFieldPosition );
					coeff[purp][ut][at][3] = sd.getValueAt( i, empretsFieldPosition );
					coeff[purp][ut][at][4] = sd.getValueAt( i, empoffFieldPosition );
					coeff[purp][ut][at][5] = sd.getValueAt( i, schenrFieldPosition );
					coeff[purp][ut][at][6] = sd.getValueAt( i, unienrFieldPosition );

				}
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		else {
			logger.fatal( "no stop density model specification file was named in properties file.");
			System.exit(1);
		}



		// read the zoneTable TableDataSet to get zonal fields for the stop density models
		int utFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.URBTYPE );
		if (utFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.URBTYPE + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int atFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.AREATYPE );
		if (atFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.AREATYPE + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int hhpopFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.POP );
		if (hhpopFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.POP + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int empoffFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.EMPOFF );
		if (empoffFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.EMPOFF + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int empotherFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.EMPOTHER );
		if (empotherFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.EMPOTHER + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int empretgFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.EMPRETGDS );
		if (empretgFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.EMPRETGDS + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int empretsFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.EMPRETSRV );
		if (empretsFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.EMPRETSRV + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int elenrFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.ELENROLL );
		if (elenrFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.ELENROLL + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int hsenrFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.HSENROLL );
		if (hsenrFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.HSENROLL + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		int unenrFieldPosition = zoneTable.getColumnPosition( ZoneTableFields.UNENROLL );
		if (unenrFieldPosition <= 0) {
			logger.fatal( ZoneTableFields.UNENROLL + " was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}


		float[] field = new float[NUMBER_OF_FIELDS];

		stopAttractions = new float[MAX_PURPOSE_CODE+1][zoneTable.getRowCount()+1];

		for (int i=1; i <= zoneTable.getRowCount(); i++) {

			ut = (int)zoneTable.getValueAt( i, utFieldPosition );
			at = (int)zoneTable.getValueAt( i, atFieldPosition );

			field[0] = zoneTable.getValueAt( i, hhpopFieldPosition );

			field[2] = zoneTable.getValueAt( i, empretgFieldPosition );
			field[3] = zoneTable.getValueAt( i, empretsFieldPosition );
			field[4] = zoneTable.getValueAt( i, empoffFieldPosition );

			field[1] = field[2] + field[3] + field[4]
						+ zoneTable.getValueAt( i, empotherFieldPosition );

			field[5] = zoneTable.getValueAt( i, elenrFieldPosition )
					+ zoneTable.getValueAt( i, hsenrFieldPosition );

			field[6] = zoneTable.getValueAt( i, unenrFieldPosition );


			for (int p=1; p <= MAX_PURPOSE_CODE; p++) {

				stopAttractions[p][i] = 0.0f;
				for (int j=0; j < NUMBER_OF_FIELDS; j++)
					stopAttractions[p][i] += field[j]*coeff[p][ut][at][j];

			}

		}


		for (int p=1; p <= TourType.TYPES; p++) {
			stopSize[processorIndex][0][p] = new float[ZonalDataManager.WALK_SEGMENTS*zoneTable.getRowCount()+1];
			stopSize[processorIndex][1][p] = new float[ZonalDataManager.WALK_SEGMENTS*zoneTable.getRowCount()+1];
		}
		stopTotSize[processorIndex][0] = new float[ZonalDataManager.WALK_SEGMENTS*zoneTable.getRowCount()+1];
		stopTotSize[processorIndex][1] = new float[ZonalDataManager.WALK_SEGMENTS*zoneTable.getRowCount()+1];

		regionalSize[0] = new float[9+1];
		regionalSize[1] = new float[9+1];

		float totAttrsOB = 0;
		float totAttrsIB = 0;
		
		float walkPercent = 0.0f;
		
		int i=-1;
		int j=-1;
		int p=-1;
		int k = 1;
		try{

		for (i=1; i <= zoneTable.getRowCount(); i++) {
		    
			totAttrsOB = stopAttractions[11][i] + stopAttractions[21][i] + stopAttractions[31][i] +
						stopAttractions[4][i] + stopAttractions[5][i] + stopAttractions[6][i] + 
						stopAttractions[7][i] + stopAttractions[8][i] + stopAttractions[9][i];

			totAttrsIB = stopAttractions[12][i] + stopAttractions[22][i] + stopAttractions[32][i] +
						stopAttractions[4][i] + stopAttractions[5][i] + stopAttractions[6][i] + 
						stopAttractions[7][i] + stopAttractions[8][i] + stopAttractions[9][i];

			for (j=0; j < ZonalDataManager.WALK_SEGMENTS; j++) {
				walkPercent = ZonalDataManager.getWalkPct(j,i);
				stopSize[processorIndex][0][1][k] = stopAttractions[11][i]*walkPercent;
				stopSize[processorIndex][1][1][k] = stopAttractions[12][i]*walkPercent;
				stopSize[processorIndex][0][2][k] = stopAttractions[21][i]*walkPercent;
				stopSize[processorIndex][1][2][k] = stopAttractions[22][i]*walkPercent;
				stopSize[processorIndex][0][3][k] = stopAttractions[31][i]*walkPercent;
				stopSize[processorIndex][1][3][k] = stopAttractions[32][i]*walkPercent;
				stopSize[processorIndex][0][4][k] = stopAttractions[4][i]*walkPercent;
				stopSize[processorIndex][1][4][k] = stopAttractions[4][i]*walkPercent;
				stopSize[processorIndex][0][5][k] = stopAttractions[5][i]*walkPercent;
				stopSize[processorIndex][1][5][k] = stopAttractions[5][i]*walkPercent;
				stopSize[processorIndex][0][6][k] = stopAttractions[6][i]*walkPercent;
				stopSize[processorIndex][1][6][k] = stopAttractions[6][i]*walkPercent;
				stopSize[processorIndex][0][7][k] = stopAttractions[7][i]*walkPercent;
				stopSize[processorIndex][1][7][k] = stopAttractions[7][i]*walkPercent;
				stopSize[processorIndex][0][8][k] = stopAttractions[8][i]*walkPercent;
				stopSize[processorIndex][1][8][k] = stopAttractions[8][i]*walkPercent;
				stopSize[processorIndex][0][9][k] = stopAttractions[9][i]*walkPercent;
				stopSize[processorIndex][1][9][k] = stopAttractions[9][i]*walkPercent;
				
				stopTotSize[processorIndex][0][k] = totAttrsOB*walkPercent;
				stopTotSize[processorIndex][1][k] = totAttrsIB*walkPercent;
				
				for (p=1; p <= 9; p++) {
					regionalSize[0][p] += stopSize[processorIndex][0][p][k];
					regionalSize[1][p] += stopSize[processorIndex][1][p][k];
				}
				
				k++;
			}
		}
		}catch(RuntimeException e){
			logger.info("NoRows in zoneTable="+zoneTable.getRowCount());
			logger.info("i="+i);
			logger.info("j="+j);
			logger.info("k="+k);
			logger.info("p="+p);
			logger.info("totAttrsOB="+totAttrsOB);
			logger.info("totAttrsIB="+totAttrsIB);
			logger.info("walkPercent="+ZonalDataManager.getWalkPct(j,i));
			for(int i1=1; i1<=MAX_PURPOSE_CODE; i1++){
				for(int i2=1; i2<=zoneTable.getRowCount(); i2++){
					logger.info("stopAttractions["+i1+"]["+i2+"]="+stopAttractions[i1][i2]);				
				}
			}
			for(int i1=0; i1<=1; i1++){
				for(int i2=1; i2<=9; i2++){
					logger.info("stopSize["+i1+"]["+i2+"]["+k+"]="+stopSize[i1][i2][k]);
				}
			}
			logger.info("stopTotSize[0]["+k+"]="+stopTotSize[0][k]);
			logger.info("stopTotSize[1]["+k+"]="+stopTotSize[1][k]);
			for(int i1=1; i1<=9; i1++){
				logger.info("regionalSize[0]["+i1+"]="+regionalSize[0][i1]);
				logger.info("regionalSize[1]["+i1+"]="+regionalSize[1][i1]);
			}
			e.printStackTrace();
			logger.info(e.getMessage());
			System.exit(1);
		}

		logger.info ("size variable report for stop location choice:");
		for (p=1; p <= 9; p++) {
			logger.info ( "outbound total regional size for purpose " + p + " = " + regionalSize[0][p] );
			logger.info ( "inbound total regional size for purpose " + p + " = " + regionalSize[1][p] );
		}

	}


	public int getIkMode ( Household hh, int tourMode, int tourSubmode, double[] submodeUtility ) {

		int ikMode = 0;

		if ( tourMode == 3) {
						
			// if the ik leg is longer, then its trip mode is the submode for the outbound half tour.
			if (submodeUtility[0] >= 100000) {
			
			    ikMode = tourSubmode;
			    
			}
			// if the ik leg is shorter, its trip mode is the highest ranked submode available.
			// if no submode is available for the shorter leg, nonmotorized is chosen for OB leg.		
			else {
			    
				// CRL available
				if (submodeUtility[0] >= 10000 && submodeUtility[0] < 100000)		
				    ikMode = 5;
				// LRT available
				else if (submodeUtility[0] >= 1000)		
				    ikMode = 4; 
				// BRT available
				else if (submodeUtility[0] >= 100)		
				    ikMode = 3; 
				// EBS available
				else if (submodeUtility[0] >= 10)		
				    ikMode = 2; 
				// LBS available
				else if (submodeUtility[0] >= 1)		
				    ikMode = 1; 
				// no transit available
				else		
				    ikMode = 6; 
			
			}
				
				
		}
		else if ( tourMode == 4) {
			
			// for tourMode=4, trip ik, mode is always 4		
			if (tourSubmode > 0)
				ikMode = tourSubmode;
			else
				ikMode = 9; 

		}
		else {
			
			ikMode = tourMode;
		}

		return ikMode;

	}


					
	public int getJkMode ( Household hh, int tourMode, int tourSubmode, double[] submodeUtility ) {

		int jkMode = 0;


		if ( tourMode == 3) {
						
			// if the jk leg is longer, then its trip mode is the submode for the outbound half tour.
			if (submodeUtility[0] >= 100000) {
			
				jkMode = tourSubmode;
			    
			}
			// if the jk leg is shorter, its trip mode is the highest ranked submode available.
			// if no submode is available for the shorter leg, nonmotorized is chosen for OB leg.		
			else {
			    
				// CRL available
				if (submodeUtility[0] >= 10000 && submodeUtility[0] < 100000)		
					jkMode = 5;
				// LRT available
				else if (submodeUtility[0] >= 1000)		
					jkMode = 4; 
				// BRT available
				else if (submodeUtility[0] >= 100)		
					jkMode = 3; 
				// EBS available
				else if (submodeUtility[0] >= 10)		
					jkMode = 2; 
				// LBS available
				else if (submodeUtility[0] >= 1)		
					jkMode = 1; 
				// no transit available
				else		
					jkMode = 6; 
			
			}
				
		}
		else if ( tourMode == 4) {
			
			// if the jk leg is longer, then its trip mode is the submode for the outbound half tour.
			if (submodeUtility[0] >= 100000) {
			
				jkMode = tourSubmode;
			    
			}
			// if the jk leg is shorter, its trip mode is the highest ranked submode available.
			// if no submode is available for the shorter leg, nonmotorized is chosen for OB leg.		
			else {
			    
				// CRL available
				if (submodeUtility[0] >= 10000 && submodeUtility[0] < 100000)		
					jkMode = 5;
				// LRT available
				else if (submodeUtility[0] >= 1000)		
					jkMode = 4; 
				// BRT available
				else if (submodeUtility[0] >= 100)		
					jkMode = 3; 
				// EBS available
				else if (submodeUtility[0] >= 10)		
					jkMode = 2; 
				// LBS available
				else if (submodeUtility[0] >= 1)		
					jkMode = 1; 
				// no transit available
				else		
					jkMode = 6; 
			
			}
				
		}
		else {
			
			jkMode = tourMode;
		}

		return jkMode;

	}


					
	public int getKiMode ( Household hh, int tourMode, int tourSubmode, double[] submodeUtility ) {

		int kiMode = 0;


		if ( tourMode == 3) {
						
			// if the ki leg is longer, then its trip mode is the submode for the inbound half tour.
			if (submodeUtility[0] >= 100000) {
			
				kiMode = tourSubmode;
			    
			}
			// if the ki leg is shorter, its trip mode is the highest ranked submode available.
			// if no submode is available for the shorter leg, nonmotorized is chosen for IB leg.		
			else {
			    
				// CRL available
				if (submodeUtility[0] >= 10000 && submodeUtility[0] < 100000)		
					kiMode = 5;
				// LRT available
				else if (submodeUtility[0] >= 1000)		
					kiMode = 4; 
				// BRT available
				else if (submodeUtility[0] >= 100)		
					kiMode = 3; 
				// EBS available
				else if (submodeUtility[0] >= 10)		
					kiMode = 2; 
				// LBS available
				else if (submodeUtility[0] >= 1)		
					kiMode = 1; 
				// no transit available
				else		
					kiMode = 6; 
			
			}
				
		}
		else if ( tourMode == 4) {
			
			// for tourMode=4, trip ki, tour mode is always 4, trip mode is tourSubmode
		    if (tourSubmode > 0)
		        kiMode = tourSubmode;
		    else
		        kiMode = 9; 

		}
		else {
			
			kiMode = tourMode;
		}

		return kiMode;

	}


					
	public int getKjMode ( Household hh, int tourMode, int tourSubmode, double[] submodeUtility ) {

		int kjMode = 0;


		if ( tourMode == 3) {
						
			// if the kj leg is longer, then its trip mode is the submode for the inbound half tour.
			if (submodeUtility[0] >= 100000) {
			
				kjMode = tourSubmode;
			    
			}
			// if the kj leg is shorter, its trip mode is the highest ranked submode available.
			// if no submode is available for the shorter leg, nonmotorized is chosen for IB leg.		
			else {
			    
				// CRL available
				if (submodeUtility[0] >= 10000 && submodeUtility[0] < 100000)		
					kjMode = 5;
				// LRT available
				else if (submodeUtility[0] >= 1000)		
					kjMode = 4; 
				// BRT available
				else if (submodeUtility[0] >= 100)		
					kjMode = 3; 
				// EBS available
				else if (submodeUtility[0] >= 10)		
					kjMode = 2; 
				// LBS available
				else if (submodeUtility[0] >= 1)		
					kjMode = 1; 
				// no transit available
				else		
					kjMode = 6; 
			
			}
				
		}
		else if ( tourMode == 4) {
			
			// if the kj leg is longer, then its trip mode is the submode for the inbound half tour.
			if (submodeUtility[0] >= 100000) {
			
				kjMode = tourSubmode;
			    
			}
			// if the kj leg is shorter, its trip mode is the highest ranked submode available.
			// if no submode is available for the shorter leg, nonmotorized is chosen for IB leg.		
			else {
			    
				// CRL available
				if (submodeUtility[0] >= 10000 && submodeUtility[0] < 100000)		
					kjMode = 5;
				// LRT available
				else if (submodeUtility[0] >= 1000)		
					kjMode = 4; 
				// BRT available
				else if (submodeUtility[0] >= 100)		
					kjMode = 3; 
				// EBS available
				else if (submodeUtility[0] >= 10)		
					kjMode = 2; 
				// LBS available
				else if (submodeUtility[0] >= 1)		
					kjMode = 1; 
				// no transit available
				else		
					kjMode = 6; 
			
			}
				
		}
		else {
			
			kjMode = tourMode;
		}

		return kjMode;

	}



}
