package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Individual Non-Madatory Tour Generation Model
 */
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.morpc.matrix.MorpcMatrixAggregaterTpp;
import com.pb.morpc.models.HouseholdArrayManager;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.synpop.pums2000.PUMSData;
import com.pb.morpc.synpop.SyntheticPopulation;
import com.pb.morpc.structures.Household;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import org.apache.log4j.Logger;


public class MorpcModelBase {

	static final String CMD_LOCATION = "c:\\windows\\system32";
    static Logger logger = Logger.getLogger(MorpcModelBase.class);
    
	protected HashMap propertyMap = null;
	protected HouseholdArrayManager hhMgr = null;
	protected ZonalDataManager zdm = null;
    protected TODDataManager tdm = null;

    static final String PROPERTIES_FILE_BASENAME = "morpc_bench";

    
	
    public MorpcModelBase () {

        propertyMap = ResourceUtil.getResourceBundleAsHashMap ( PROPERTIES_FILE_BASENAME );


        //instantiate ZonalDataManager and TODDataManager objects so that their static members are available to other classes (eg. DTMOutput)
		zdm = new ZonalDataManager ( propertyMap );
        tdm = new TODDataManager(propertyMap);


		// set the global random number generator seed read from the properties file
		SeededRandom.setSeed ( Integer.parseInt( (String)propertyMap.get("RandomSeed") ) );

		
        // if new skims were generated as part of this model run or prior to it, run aggregation step for TPP submode skims
        if ( ((String)propertyMap.get("AGGREGATE_TPPLUS_SKIM_MATRICES")).equalsIgnoreCase("TRUE") ) {
            
            logger.info( "aggregating slc skim matrices" );                
            MorpcMatrixAggregaterTpp ma = new MorpcMatrixAggregaterTpp(propertyMap);
            ma.aggregateSlcSkims();
            logger.info( "finished aggregating slc skim matrices" );                

        }
        
        
        
        
		// build a synthetic population
		if ( ((String)propertyMap.get("RUN_POPULATION_SYNTHESIS_MODEL")).equalsIgnoreCase("true") )
			runPopulationSynthesizer();
		
		
		
		// assign auto ownership attributes to population
		if ( ((String)propertyMap.get("RUN_AUTO_OWNERSHIP_MODEL")).equalsIgnoreCase("true") ) {

            logger.info( "computing accessibilities." );                

            //update accessibily indices, and then write it to hard drive as a .csv file.
            if ( ((String)propertyMap.get("format")).equalsIgnoreCase("tpplus") ) {
                AccessibilityIndicesTpp accInd = new AccessibilityIndicesTpp( PROPERTIES_FILE_BASENAME );
                accInd.writeIndices();
            }
            else {
                AccessibilityIndices accInd = new AccessibilityIndices( PROPERTIES_FILE_BASENAME );
                accInd.writeIndices();
            }
	
			logger.info ("finished computing accessibilities");

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

        String zonalStudentsInputFileName = (String) propertyMap.get("UnivStudentsTaz.file");
        String zonalStudentsOutputFileName = (String) propertyMap.get("UnivStudentsTazOutput.file");

		sp.runSynPop(OUTPUT_HHFILE, ZONAL_TARGETS_HHFILE, zonalStudentsInputFileName, zonalStudentsOutputFileName);
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



	private void sendCommand (String command) throws InterruptedException {
	  try {
	      
		String s;
		Process proc = Runtime.getRuntime().exec( CMD_LOCATION + "\\cmd.exe /C " + command );
            
		BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
 
		while ((s = stdout.readLine()) != null) {
		  logger.warn(s);
		}

		while ((s = stderr.readLine()) != null) {
		  logger.warn(s);
		}
	  } 
	  catch (IOException e) {
		System.err.println("exception: "+e);
	  }
	}
  
  
  

}
