package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Individual Non-Madatory Tour Generation Model
 */
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
//import com.pb.common.util.ObjectUtil;
import com.pb.morpc.matrix.MorpcMatrixZipper;
import com.pb.morpc.matrix.MorpcMatrixAggregater;
import com.pb.morpc.models.HouseholdArrayManager;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.synpop.pums2000.PUMSData;
import com.pb.morpc.synpop.SyntheticPopulation;
import com.pb.morpc.structures.Household;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Logger;


public class MorpcModelBase {

	static final String CMD_LOCATION = "c:\\winnt\\system32";
//	static final String CMD_LOCATION = "c:\\windows\\system32";
	static final String TPP_TO_BINARY_PROGRAM_DIRECTORY = "c:\\model\\6_Pgms\\0_skim";
//	static final String TPP_TO_BINARY_PROGRAM_DIRECTORY = "c:\\jim\\util\\workspace3.0m4\\common-base\\src\\c\\matrix\\tpplus_to_binary\\release";
	static final String TPP_TO_BINARY_PROGRAM = "convertTpplusBinary.exe";
	static final String BINARY_TO_TPP_PROGRAM_DIRECTORY = "c:\\model\\6_Pgms\\0_skim";
//	static final String BINARY_TO_TPP_PROGRAM_DIRECTORY = "c:\\jim\\util\\workspace3.0m4\\common-base\\src\\c\\matrix\\binary_to_tpplus\\release";
	static final String BINARY_TO_TPP_PROGRAM = "convertBinaryTpplus.exe";

	/*
	String TPP_TO_BINARY_PROGRAM_DIRECTORY = null;
    String BINARY_TO_TPP_PROGRAM_DIRECTORY = null;
    String TPP_TO_BINARY_PROGRAM=null;
    String BINARY_TO_TPP_PROGRAM=null;
    */    
    static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
	protected HashMap propertyMap = null;
	protected HouseholdArrayManager hhMgr = null;
	protected ZonalDataManager zdm = null;

    
	
	
    public MorpcModelBase () {

		boolean SKIP_CREATE_DISK_OBJECT_ARRAY = false;

		
        propertyMap = ResourceUtil.getResourceBundleAsHashMap ("morpc" );
/*
    	String TPP_TO_BINARY_PROGRAM_DIRECTORY = (String) propertyMap.get("TPP_TO_BINARY_PROGRAM_DIRECTORY");
        String BINARY_TO_TPP_PROGRAM_DIRECTORY = (String) propertyMap.get("BINARY_TO_TPP_PROGRAM_DIRECTORY");
        String TPP_TO_BINARY_PROGRAM = (String) propertyMap.get("TPP_TO_BINARY_PROGRAM");
        String BINARY_TO_TPP_PROGRAM = (String) propertyMap.get("BINARY_TO_TPP_PROGRAM");
*/        
		// build the zonal data table
		zdm = new ZonalDataManager ( propertyMap );
	
		// build the TOD Ddata table
		TODDataManager tod = new TODDataManager ( propertyMap );

//		long[] objectSize = new long[10];
//		objectSize[0] = ObjectUtil.sizeOf(zdm);
		

		// set the global random number generator seed read from the properties file
		SeededRandom.setSeed ( Integer.parseInt( (String)propertyMap.get("RandomSeed") ) );

		
		
		// call a C program to read the tpplus skims matrices and create the binary format skims matrices needed for the model run
		if ( ((String)propertyMap.get("RUN_TPPLUS_SKIMS_CONVERTER")).equalsIgnoreCase("true") ) {
		    
			String tppDir = (String)propertyMap.get("SkimsDirectory.tpplus");
			String binDir = (String)propertyMap.get("SkimsDirectory.binary");
		    runDOSCommand ( TPP_TO_BINARY_PROGRAM_DIRECTORY + "\\" + TPP_TO_BINARY_PROGRAM + " " + tppDir + " " + binDir );
			logger.info( "done converting tpplus to binary skim matrices" );				

		    
			MorpcMatrixAggregater ma = new MorpcMatrixAggregater(propertyMap);
			ma.aggregateSlcSkims();
			logger.info( "done aggregating slc skim matrices" );				

			
			MorpcMatrixZipper mx = new MorpcMatrixZipper (propertyMap);
			mx.convertHwyBinaryToZip();	
			mx.convertWTBinaryToZip();	
			mx.convertDTBinaryToZip();	
			logger.info( "done converting MORPC Binary matrices to Zip matrices" );				
			
			ma = null;
			mx = null;
		}
		
		
		
		// build a synthetic population
		if ( ((String)propertyMap.get("RUN_POPULATION_SYNTHESIS_MODEL")).equalsIgnoreCase("true") )
			runPopulationSynthesizer();
		
		
		
		// assign auto ownership attributes to population
		if ( ((String)propertyMap.get("RUN_AUTO_OWNERSHIP_MODEL")).equalsIgnoreCase("true") ) {

			//update accessibily indices, and then write it to hard drive as a .csv file.
			AccessibilityIndices accInd=new AccessibilityIndices("morpc");
			accInd.writeIndices();
	
			logger.info ("Memory after computing accessibilities");

			runAutoOwnershipModel();
		    
		}


		
		// assign person type and daily activity pattern attributes and generate mandatory tours
		if ( ((String)propertyMap.get("RUN_DAILY_PATTERN_MODELS")).equalsIgnoreCase("true") )
			runDailyActivityPatternModels();

		

		
		
		// create an array of Household objects from the household table data
		hhMgr = new HouseholdArrayManager( propertyMap );

		// check if property is set to start the model iteration from a DiskObjectArray,
		// and if so, get HHs from the existing DiskObjectArray.
		if(((String)propertyMap.get("StartFromDiskObjectArray")).equalsIgnoreCase("true")) {
			hhMgr.createBigHHArrayFromDiskObject();
		} 
		// otherwise, create an array of HHs from the household table data stored on disk
		else {
			hhMgr.createBigHHArray ();
		}
		
		hhMgr.createHHArrayToProcess (); 


		
		// run Free Parking Eligibility Model
		Household[] hh = hhMgr.getHouseholds();
		FreeParkingEligibility fpModel = new FreeParkingEligibility( propertyMap );
		fpModel.runFreeParkingEligibility (hh);
		hhMgr.sendResults ( hh );
		fpModel = null;
		hh = null;

    }



	protected void runPopulationSynthesizer () {
        
		TableDataSet zoneTable = null;

		zoneTable = zdm.getZonalTableDataSet();

		
		//open files  
		String PUMSFILE =  (String)propertyMap.get( "PUMSData.file" );
		String PUMSDICT =  (String)propertyMap.get( "PUMSDictionary.file" );
		String OUTPUT_HHFILE = (String)propertyMap.get( "SyntheticHousehold.file" );
		String ZONAL_TARGETS_HHFILE = (String)propertyMap.get( "ZonalTargets.file" );

		// build the PUMS data structure and read input data
		PUMSData pums = new PUMSData (PUMSFILE, PUMSDICT);
		pums.readData (PUMSFILE);
		pums.createPumaIndices();
    
		// create a synthetic population
		SyntheticPopulation sp = new SyntheticPopulation(pums, zoneTable);
		sp.runSynPop(OUTPUT_HHFILE, ZONAL_TARGETS_HHFILE);
		pums = null;
		sp = null;
	
		logger.info("end of model 0");


	}

    
    
	protected void runAutoOwnershipModel () {

		// run the hh auto ownership choice model 
		AutoOwnership ao = new AutoOwnership( propertyMap );
		ao.runAutoOwnership();
		ao = null;
	
		logger.info("end of model 1");

	}
    
    
	
	protected void runDailyActivityPatternModels () {

	
		// run the daily activity pattern choice models 
		// preschool children
		Model21 m21 = new Model21( propertyMap );
		m21.runPreschoolDailyActivityPatternChoice();

		// predriving children
		Model22 m22 = new Model22( propertyMap );
		m22.runPredrivingDailyActivityPatternChoice();

		// driving children
		Model23 m23 = new Model23( propertyMap );
		m23.runDrivingDailyActivityPatternChoice();

		// students
		Model24 m24 = new Model24( propertyMap );
		m24.runStudentDailyActivityPatternChoice();

		// full time workers
		Model25 m25 = new Model25( propertyMap );
		m25.runWorkerFtDailyActivityPatternChoice();

		// full time workers
		Model26 m26 = new Model26( propertyMap );
		m26.runWorkerPtDailyActivityPatternChoice();

		// non workers
		Model27 m27 = new Model27( propertyMap );
		m27.runNonworkerDailyActivityPatternChoice();


		logger.info("end of models 2.1-2.7");

	}

	
	
	void runDOSCommand (String command) {
		try {
			sendCommand (command);
		} catch (InterruptedException e) {
			System.out.println ("Interrupted exception ocurred for command: " + command);
		}
	}



	public static void sendCommand (String command) throws InterruptedException {
	  try {
	      
		String s;
		Process proc = Runtime.getRuntime().exec( CMD_LOCATION + "\\cmd.exe /C " + command );
            
		BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
 
		while ((s = stdout.readLine()) != null) {
		  logger.warning(s);
		}

		while ((s = stderr.readLine()) != null) {
		  logger.warning(s);
		}
	  } 
	  catch (IOException e) {
		System.err.println("exception: "+e);
	  }
	}
  
  
  

}
