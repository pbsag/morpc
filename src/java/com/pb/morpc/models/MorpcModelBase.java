package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Individual Non-Madatory Tour Generation Model
 */
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.morpc.models.HouseholdArrayManager;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.synpop.pums2000.PUMSData;
import com.pb.morpc.synpop.SyntheticPopulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import org.apache.log4j.Logger;


public class MorpcModelBase {

	static final String CMD_LOCATION = "c:\\windows\\system32";
    static Logger logger = Logger.getLogger(MorpcModelBase.class);
    
	protected HashMap<String,String> propertyMap = null;
	protected HouseholdArrayManager hhMgr = null;
	protected ZonalDataManager zdm = null;
    protected TODDataManager tdm = null;

    public static final String PROPERTIES_FILE_BASENAME = "morpc_bench";
    protected String basePropertyName;

    protected MatrixDataServerIf ms;
    protected String serverAddress = null;
    protected int serverPort = -1;
    
	
    public MorpcModelBase ( String basePropertyName ) {

    	this.basePropertyName = basePropertyName;
        propertyMap = ResourceUtil.getResourceBundleAsHashMap ( basePropertyName );

        logger.info("");
        logger.info("");
        serverAddress = (String)propertyMap.get("MatrixServerAddress");

        String serverPortString = (String)propertyMap.get("MatrixServerPort");
        if ( serverPortString != null )
            serverPort = Integer.parseInt(serverPortString);

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
