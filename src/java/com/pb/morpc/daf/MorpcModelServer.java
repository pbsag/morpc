package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * Main Model Sever for Distributed Application
 */
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.calculator.TableDataSetManager;
import com.pb.common.daf.DAF;
import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.daf.NodeDef;
import com.pb.common.daf.Port;
import com.pb.common.daf.PortManager;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.DiskObjectArray;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.morpc.matrix.MatrixDataServer;
import com.pb.morpc.matrix.MatrixDataServerRmi;
import com.pb.morpc.matrix.MorpcMatrixAggregaterTpp;
import com.pb.morpc.matrix.MorpcMatrixZipper;
import com.pb.morpc.models.AccessibilityIndicesTpp;
import com.pb.morpc.models.AutoOwnership;
import com.pb.morpc.models.DTMOutput;
import com.pb.morpc.models.IndividualNonMandatoryToursModel;
import com.pb.morpc.models.JointToursModel;
import com.pb.morpc.models.Model21;
import com.pb.morpc.models.Model22;
import com.pb.morpc.models.Model23;
import com.pb.morpc.models.Model24;
import com.pb.morpc.models.Model25;
import com.pb.morpc.models.Model26;
import com.pb.morpc.models.Model27;
import com.pb.morpc.models.TODDataManager;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.ZDMTDM;
import com.pb.morpc.synpop.SyntheticPopulation;
import com.pb.morpc.synpop.pums2000.PUMSData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class MorpcModelServer extends MessageProcessingTask {

    private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.morpc.models");

    String CMD_LOCATION;

    String hhFile;
    String personFile;
    String zonalFile;

    private ZonalDataManager zdm = null;
    private TODDataManager tdm = null;
    private HashMap propertyMap = null;
    private HashMap staticZonalDataMap = null;
    private HashMap staticTodDataMap = null;


    private long startTime = 0;

	int numberOfIterations;
    String[] iterationPropertyFiles = { "morpc1", "morpc2", "morpc3"};

    Household[] hhs = null;

    private boolean FtaRestartRun = false;
    
    public MorpcModelServer() {
    }

    public void onStart() {

        //MatrixIO32BitJvm ioVm32Bit = null;

        startTime = System.currentTimeMillis();

        propertyMap = ResourceUtil.getResourceBundleAsHashMap(iterationPropertyFiles[0]);
        
        
        // Added by Jim Hicks - 14 mar 2008:
        // A 32 bit JVM is started at the beginning of onStart() for MorpcModelServer and stopped at the
        // end.  A class is run in this JVM that allows TPPLUS matrix data to be read/written in the 32 bit
        // VM and communicated to the main model classes using RMI.
        //
        // A 32 bit VM is started on each of the worker nodes as well for similar purposes, by including
        // similar startup code in the RnWorker.onStart() at it's beginning, and in the AtWorkStopsWorker.onMessage()
        // when it's told to EXIT.  This ensures the JVM and matrix i/o classes are able to be used by main model
        // UECs, and are unloaded from memory before the model ends.
        
        
        // start the 32 bit JVM used specifically for running matrix io classes
        //ioVm32Bit = MatrixIO32BitJvm.getInstance();
        //ioVm32Bit.startJVM32();
        
        // establish that matrix reader and writer classes will use the RMI versions for TPPLUS format matrices
        //ioVm32Bit.startMatrixDataServer( MatrixType.TPPLUS );
        
        
		// send a message to the random number server so that the random number seed will be set on all nodes
		PortManager pManager = PortManager.getInstance();
		Port receivePort = pManager.getReceivePort();

		Message waitMsg = null;
		Message propertyMapMsg = createMessage();

		propertyMapMsg.setId(MessageID.START_INFO);
		propertyMapMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
		
		// set the global random number generator seed read from the properties file on the nodes
		// that runs the worker tasks
		if (LOGGING)
			logger.info(this.name + " starting the random number server task.");
		sendTo("RnServer", propertyMapMsg);

		// wait here until a message from the RnServer says it's heard from all RnWorkers.
		if (LOGGING)
			logger.info(this.name + " waiting to hear from RnServer that random number seeds have been set.");
		waitMsg = receivePort.receive();
		while (!(waitMsg.getSender().equals("RnServer") && waitMsg.getId().equals(MessageID.EXIT))) {
			waitMsg = receivePort.receive();
		}

		// send an exit message to the RnServer to tell it to free memory in its workers and itself.
		Message exitMsg = createMessage();
		exitMsg.setId(MessageID.EXIT);
		sendTo("RnServer", exitMsg);

		// set the global random number generator seed read from the properties file on the node
		// that runs the server tasks
		SeededRandom.setSeed(Integer.parseInt((String) propertyMap.get("RandomSeed")));

		if (LOGGING) {
			logger.info("Memory at end of RnServer");
			showMemory();
		}

		
		
		
        // set the global number of iterations read from the properties file
        numberOfIterations = (Integer.parseInt((String) propertyMap.get( "GlobalIterations")));

        
        // determine whether this model run is for an FTA Summit analysis or not
        if( (String)propertyMap.get("FTA_Restart_run") != null )
        	FtaRestartRun = ((String)propertyMap.get("FTA_Restart_run")).equalsIgnoreCase("true");
        else
        	FtaRestartRun = false;

        
        startMatrixServer();        
        
        
        if(FtaRestartRun){
        	
        	if(((String)propertyMap.get("SKIP_TSKIM_FTA")).equalsIgnoreCase("false")){
	        	//prepare for a FTA restart run
	        	String scenario=(String)propertyMap.get("scenario");
	        	String run_tskim_ftaCmd=(String)propertyMap.get("run_tskim_fta.batch")+" "+scenario;
	    		runDOSCommand(run_tskim_ftaCmd);
        	}
        	
        	ZDMTDM zdmtdm=readDiskObjectZDMTDM(propertyMap);
        	//instantiate ZonalDataManager and TODDataManager object so that their static members are available to other classes (eg. DTMOutput)
        	zdm=new ZonalDataManager(propertyMap);
        	tdm=new TODDataManager(propertyMap);
        	zdm=zdmtdm.getZDM();
        	showMemory();
        	tdm=zdmtdm.getTDM();
        	showMemory();
        }else{
        	// build the zonal data table
        	zdm = new ZonalDataManager(propertyMap);
        	showMemory();
    		// build the TOD Ddata table
    		tdm = new TODDataManager(propertyMap);
    		showMemory();
        }

	    for (int i = 0; i < numberOfIterations; i++) {
	         runModelIteration(i);
	    }        	

        if (LOGGING) {
            logger.info("Memory after running reports - end of program");
            showMemory();
        }

        
        // establish that matrix reader and writer classes will not use the RMI versions any longer.
        // local matrix i/o, as specified by setting types, is now the default again.
        //ioVm32Bit.stopMatrixDataServer();
        
        // close the JVM in which the RMI reader/writer classes were running
        //ioVm32Bit.stopJVM32();
        

        if (LOGGING) {
            logger.info("end of MORPC Demand Models");
            logger.info("full MORPC model run finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
        }

        System.exit(0);
        
    }

    private void runModelIteration(int iteration) {

        logger.info("Global iteration:" + (iteration+1) + " started.");
        
        // read the property file specific to this global iteration
        propertyMap = ResourceUtil.getResourceBundleAsHashMap(iterationPropertyFiles[iteration]);
 
        //Wu added
        //if 2nd iteration starts from disk object array, then read zdm and tdm from ZDMTDMObjetOnDisk created in 1st iteration
        //don't need to do this in 3rd iteration, because in 3rd iteration, zdm and tdm is also from the 1st iteration ZDMTDMObjectOnDisk 
                
        if(((String) propertyMap.get("StartFromDiskObjectArray")).equalsIgnoreCase("true")){
        	ZDMTDM zdmtdm=readDiskObjectZDMTDM(propertyMap);
        	//instantiate ZonalDataManager and TODDataManager object so that their static members are available to other classes (eg. DTMOutput)
        	zdm=new ZonalDataManager(propertyMap);
        	tdm=new TODDataManager(propertyMap);
        	zdm=zdmtdm.getZDM();
        	tdm=zdmtdm.getTDM();
        }
        else{
            // build the zonal data table
            zdm = new ZonalDataManager(propertyMap);
            // build the TOD Ddata table
            tdm = new TODDataManager(propertyMap);
        }
        
        zdm.updatePropertyMap( "morpc", iteration+1 );


		//if FTA_Restart_run is false, run PopSyn, otherwise skip it.
        if (!FtaRestartRun){
		
			// build a synthetic population
	        if (((String) propertyMap.get("RUN_POPULATION_SYNTHESIS_MODEL")).equalsIgnoreCase( "true"))
	            runPopulationSynthesizer();
	        
        }
		
        
        // if new skims were generated as part of this model run or prior to it, run aggregation step for TPP submode skims
        if ( ((String)propertyMap.get("AGGREGATE_TPPLUS_SKIM_MATRICES")).equalsIgnoreCase("TRUE") ) {
            
            logger.info( "aggregating slc skim matrices" );                
            MorpcMatrixAggregaterTpp ma = new MorpcMatrixAggregaterTpp(propertyMap);
            ma.aggregateSlcSkims();
            logger.info( "finished aggregating slc skim matrices" );                

	        //copyFileToWorkers("tpplus");
	        //logger.info("done copying Zip matrices to workers");
	
            AccessibilityIndicesTpp accInd = new AccessibilityIndicesTpp( iterationPropertyFiles[iteration] );
            accInd.writeIndices();
            accInd = null;

	        //copyFileToWorkers("socec");
	        //logger.info("done copy socec files to workers");

        }


	
        runCoreModel(iteration);
        

        // write binary matrices and summary tables and .csv output files for DTM
        PortManager pManager = PortManager.getInstance();
        Port receivePort = pManager.getReceivePort();

        Message waitMsg = null;
        Message sendHHsMsg = createMessage();

        // get the Household[] from the HhArrayServer
        if (LOGGING)
            logger.info(this.name + " requesting array of households for writing output files.");
        sendHHsMsg.setId(MessageID.SEND_HHS_ARRAY);
        sendTo("HhArrayServer", sendHHsMsg);

        // wait here until a message from the HhArrayServer says it's done sending the big hh array.
        waitMsg = receivePort.receive();
        while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HHS_ARRAY_KEY))) {
            waitMsg = receivePort.receive();
        }

        hhs = (Household[]) waitMsg.getValue(MessageID.HHS_ARRAY_KEY);


        // write summary tables and .csv output files for DTM
        DTMOutput dtmOut = new DTMOutput(propertyMap,zdm);
		try {
			dtmOut.writeDTMOutput( hhs );
		} 
		catch (Exception e) {
            logger.fatal ("Caught runtime exception writing DTMOutput csv file.", e);
		}
		

		
		if ( ((String)propertyMap.get("WRITE_TRIP_TABLES")).equalsIgnoreCase("true") ) {
            
            try {
                dtmOut.writeTripTables( hhs );
                dtmOut = null;
            }
            catch (Exception e) {
                logger.fatal ("Caught runtime exception writing trip tables.", e);
            }
		    
		}



        dtmOut = null;

        if (LOGGING) {
            logger.info("Memory after writing output files");
            showMemory();
        }
        
        // clear the household data from the TableDataSetManager for the next iteration
        TableDataSetManager.getInstance().clearData();
               
        String createDiskObjectArray=(String)propertyMap.get("CreateDiskObjectArray");
        
		//write disk object array to hard drive if not FTA restart run, 
        //and if CreateDiskObjectArray is set to true, then write object array to disk
        if ( !FtaRestartRun && createDiskObjectArray.equalsIgnoreCase("true") ){
            //write big disk object array to disk
        	writeDiskObjectArray(hhs);
        	hhs=null;
        	writeDiskObjectZDMTDM(zdm,tdm,staticZonalDataMap,staticTodDataMap,propertyMap);
        }
        
        //delete big household array from memory
        hhs=null;
        
		//execute assignment and skimming
        String SKIP_ASSIGNMENT_SKIMMING=(String)propertyMap.get("SKIP_ASSIGNMENT_SKIMMING");
        if(!SKIP_ASSIGNMENT_SKIMMING.equals("true")){
            executeAssignmentSkimming(iteration);
        }
        
    }



    private void runCoreModel(int iteration) {
    	
		//if FTA_Restart_run is false, run AutoOwnership and Daily Pattern steps, otherwise skip them
        if ( !FtaRestartRun ){
	        // assign auto ownership attributes to population
	        if (((String) propertyMap.get("RUN_AUTO_OWNERSHIP_MODEL")).equalsIgnoreCase( "true")) {
	
                //update accessibily indices, and then write it to hard drive as a .csv file.
                logger.info( "computing accessibilities." );                
                AccessibilityIndicesTpp accInd = new AccessibilityIndicesTpp( iterationPropertyFiles[iteration] );
                accInd.writeIndices();
    			logger.info("done with computing accessibilities");
    
    	
    			//copyFileToWorkers("socec");
    			//logger.info("done copy socec files to workers");
    
                accInd = null;
                
                runAutoOwnershipModel();
            
            }
	
	        // assign person type and daily activity pattern attributes and generate mandatory tours
	        if (((String) propertyMap.get("RUN_DAILY_PATTERN_MODELS")).equalsIgnoreCase("true"))
	            runDailyActivityPatternModels();
	            
        }

        PortManager pManager = PortManager.getInstance();
        Port receivePort = pManager.getReceivePort();

        Message waitMsg = null;

        Message propertyMapMsg = createMessage();
        propertyMapMsg.setId(MessageID.START_INFO);
        propertyMapMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);

        Message exitMsg = createMessage();
        exitMsg.setId(MessageID.EXIT);

        Message sendHHsMsg = createMessage();
        Message resetHHsMsg = createMessage();
        Message updateHHsMsg = createMessage();
        Message startInfoMsg = createMessage();
        
        // for each iteration:
        // after the DAP models have run, respond to the HouseholdArray server
        // so the Household Array can be built and the server started up.
        // ask the main server for the propertyMap
        if (LOGGING)
            logger.info(this.name + " starting HhArrayServer to build big array for Household objects.");
        sendTo("HhArrayServer", propertyMapMsg);

        // wait here until a message from the hhArrayServer says it's done creating the big hh array.
        if (LOGGING)
            logger.info(this.name + " waiting to hear from HhArrayServer that hh array is built.");
        waitMsg = receivePort.receive();
        while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HH_ARRAY_FINISHED))) {
            waitMsg = receivePort.receive();
        }
                
		//if FTA_Restart_run is false, run Free Parking model, otheriwse skip it
        if ( !FtaRestartRun ){

	        if (LOGGING) {
	            logger.info("Memory before starting FpModel");
	            showMemory();
	        }
	
	        // send a resetHouseholdCount to the hhArrayServer to tell it to zero its hhs processed count.
	        resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
	        sendTo("HhArrayServer", resetHHsMsg);
	
	        // send a message to the FreeParkingServer so that the freeParkingEligibility model will be run.
	        if (LOGGING)
	            logger.info(this.name + " starting the free parking eligibility model.");
	        sendTo("FpModelServer", propertyMapMsg);
	
	        // wait here until a message from the FpModelServer says it's done processing all hhs.
	        if (LOGGING)
	            logger.info(this.name + " waiting to hear from FpModelServer that free parking model for all hhs is processed.");
	        waitMsg = receivePort.receive();
	        while (!(waitMsg.getSender().equals("FpModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
	            
	            int dummy=0;
	            if  ( !(waitMsg.getSender().equals("FpModelServer") ) )
	                dummy = 1;
	            else if ( waitMsg.getId().equals(MessageID.EXIT) )
	                dummy = 2;
	            
	            waitMsg = receivePort.receive();
	        }
	
			// send an exit message to the FpModelServer to tell it to free memory in its workers and itself.
	        if ( iteration+1 == numberOfIterations ) {
				exitMsg.setId(MessageID.EXIT);
				sendTo("FpModelServer", exitMsg);
	        }
	        
			if (LOGGING) {
	            logger.info("Memory at end of FpModel");
	            showMemory();
	        }
        }




	    int shadowPriceIterations = (Integer.parseInt((String) propertyMap.get("NumberOfShadowPriceIterations")));
		//if FTA_Restart_run is false, run balancing size variable steps, otherwise skip
	    if ( !FtaRestartRun ){
		    // run the mandatory DC with shadow pricing.	
	        if (LOGGING) {
	            logger.info("Memory before balancing size variables");
	            showMemory();
	        }
	
	        if (LOGGING)
	            logger.info(this.name + " requesting array of households for balancing size variables.");
	        sendHHsMsg.setId(MessageID.SEND_HHS_ARRAY);
	        sendTo("HhArrayServer", sendHHsMsg);
	
	        // wait here until a message from the hhArrayServer says it's done sending the big hh array.
	        waitMsg = receivePort.receive();
	        while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HHS_ARRAY_KEY))) {
	            waitMsg = receivePort.receive();
	        }
	
	        hhs = (Household[]) waitMsg.getValue(MessageID.HHS_ARRAY_KEY);
	        zdm.balanceSizeVariables(hhs);
	        zdm.updateShadowPricingInfo(999);
	        hhs = null;
	        waitMsg = null;
	
	        if (LOGGING) {
	            logger.info("Memory after balancing size variables");
	            showMemory();
	        }


	        // create a non-static collection of the static data members in TODDataMangager
	        // so that the data can be serialized and passed in a message.
	        staticTodDataMap = tdm.createStaticDataMap();
	
	        // create a non-static collection of the static data members in ZonalDataMangager
	        // so that the data can be serialized and passed in a message.
	        staticZonalDataMap = zdm.createStaticDataMap();
	
	        if (LOGGING) {
	            logger.info("Memory after creating TOD static data map");
	            showMemory();
	        }
        }



        if (((String) propertyMap.get("RUN_MANDATORY_DTM")).equalsIgnoreCase("true")) {
        	
    		//if FTA_Restart_run is false, run Mandatory DC step, otherwise skip
            if ( !FtaRestartRun ){
	
	            for (int i = 0; i < shadowPriceIterations; i++) {
	
	                if (LOGGING) {
	                    logger.info("Memory at start of iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                // send a propertyMap to the ModelServer to tell it to start its workers.
	                // also send a ZonalDataManager object that the workers' UECs will need.
	                // finally, send the static part of the ZonalDataManager separately since static data
	                // doesn't get serialized and thus does not get included in the message.
	                startInfoMsg.setId(MessageID.START_INFO);
	                startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
	                startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
	                startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
	                startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
	                startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
	                startInfoMsg.setValue(MessageID.SHADOW_PRICE_ITER_KEY, Integer.toString(i + 1));
	                sendTo("DcModelServer", startInfoMsg);
	
	                if (LOGGING) {
	                    logger.info("Memory after sending message to DcModelServer in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                // wait here until a message from the DcModelServer says it's done processing all hhs.
	                if (LOGGING)
	                    logger.info( "main server waiting to hear from DcModelServer that DC has run for all hhs.");
	                waitMsg = receivePort.receive();
	                while (!(waitMsg.getSender().equals("DcModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
	                    waitMsg = receivePort.receive();
	                }
	
	                if (LOGGING) {
	                    logger.info("Memory after receiving message from DcModelServer in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                // update the shadow pricing data elements in the zonal data manager
	                if (LOGGING)
	                    logger.info(this.name + " requesting array of households for summarizing modeled attractions.");
	                sendHHsMsg.setId(MessageID.SEND_HHS_ARRAY);
	                sendTo("HhArrayServer", sendHHsMsg);
	
	                if (LOGGING) {
	                    logger.info("Memory after sending message to HhArrayServer in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                // wait here until a message from the hhArrayServer says it's done sending the big hh array.
	                waitMsg = receivePort.receive();
	                while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HHS_ARRAY_KEY))) {
	                    waitMsg = receivePort.receive();
	                }
	
	                if (LOGGING) {
	                    logger.info("Memory after receiving message from HhArrayServer in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                hhs = (Household[]) waitMsg.getValue(MessageID.HHS_ARRAY_KEY);
	
	                if (LOGGING) {
	                    logger.info("Memory after receiving hh array in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                zdm.sumAttractions(hhs);
	
	                if (LOGGING) {
	                    logger.info("Memory after summing attractions in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                hhs = null;
	
	                if (LOGGING) {
	                    logger.info("Memory after nulling hh array in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                zdm.reportMaxDiff();
	
	                if (LOGGING) {
	                    logger.info("Memory after reporting maxDiff in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                zdm.updateShadowPrices();
	
	                if (LOGGING) {
	                    logger.info("Memory after update shadow prices in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                zdm.updateSizeVariables();
	
	                if (LOGGING) {
	                    logger.info("Memory after update size variables in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                zdm.updateShadowPricingInfo(i);
	
	                if (LOGGING) {
	                    logger.info("Memory after update shadow price info in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	                // create a non-static collection of the static data members in ZonalDataMangager
	                // so that the data can be serialized and passed in a message.
	                staticZonalDataMap = zdm.createStaticDataMap();
	
	                if (LOGGING) {
	                    logger.info("Memory after createStaticDataMap() in iteration " + i + " of shadow pricing");
	                    showMemory();
	                }
	
	            }
	
	
	            // send an exit message to the DcModelServer to tell it to free memory in its workers and itself.
				if ( iteration+1 == numberOfIterations ) {
				    exitMsg.setId(MessageID.EXIT);
				    sendTo("DcModelServer", exitMsg);
				}
	
	            if (LOGGING) {
	                logger.info("Memory after all shadow price iterations");
	                showMemory();
	            }
            
        	}
            
            
            
            // send a message to the TcMcModelServer so that the mandatory TcMc model will be run.
            if (LOGGING)
                logger.info(this.name + " starting the mandatory TcMc model.");
            staticZonalDataMap = zdm.createStaticDataMap();
            staticTodDataMap = tdm.createStaticDataMap();
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("TcMcModelServer", startInfoMsg);

            if (LOGGING) {
                logger.info("Memory after sending message to TcMcModelServer");
                showMemory();
            }

            // wait here until a message from the TcMcModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from TcMcModelServer that TcMc has run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("TcMcModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the TcMcModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("TcMcModelServer", exitMsg);
			}
			
            if (LOGGING) {
                logger.info("Memory after exit TcMcModelServer");
                showMemory();
            }
        }

        if (((String) propertyMap.get("RUN_JOINT_DTM")).equalsIgnoreCase("true")) {
        	
    		//if FTA_Restart_run is false, run joint tour generation step, otherwise skip
            if ( !FtaRestartRun ){
	
	            // get the Household[] from the HhArrayServer
	            if (LOGGING)
	                logger.info(this.name + " requesting array of households for joint tour generation.");
	            sendHHsMsg.setId(MessageID.SEND_HHS_ARRAY);
	            sendTo("HhArrayServer", sendHHsMsg);
	
	            // wait here until a message from the HhArrayServer says it's done sending the big hh array.
	            waitMsg = receivePort.receive();
	            while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HHS_ARRAY_KEY))) {
	                waitMsg = receivePort.receive();
	            }
	
	            hhs = (Household[]) waitMsg.getValue(MessageID.HHS_ARRAY_KEY);
	
	            if (LOGGING) {
	                logger.info("Memory after getting hhs for joint tour generation");
	                showMemory();
	            }
	
	            JointToursModel jtModel = new JointToursModel(propertyMap, hhs);
	            jtModel.runFrequencyModel();
	            jtModel.runCompositionModel();
	            jtModel.runParticipationModel();
	
	            if (LOGGING) {
	                logger.info("Memory after joint tour generation");
	                showMemory();
	            }
	
	            updateHHsMsg.setId(MessageID.UPDATE_HHS_ARRAY);
	            updateHHsMsg.setValue(MessageID.UPDATED_HHS, hhs);
	            sendTo("HhArrayServer", updateHHsMsg);
	
	            // wait here until a message from the HhArrayServer says it's done updating the big hh array.
	            waitMsg = receivePort.receive();
	            while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HHS_ARRAY_KEY))) {
	                waitMsg = receivePort.receive();
	            }
	
	            jtModel = null;
	            hhs = null;
	
	            if (LOGGING) {
	                logger.info("Memory after hh array update and nulling jtModel and hhs");
	                showMemory();
	            }
            }




            // send a resetHouseholdCount to the hhArrayServer to tell it to zero its hhs processed count.
            resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
            sendTo("HhArrayServer", resetHHsMsg);

            // send a message to the JointDTMModelServer so that the joint DTM model will be run.
            if (LOGGING)
                logger.info(this.name + " starting the joint DTM model.");
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("JointDTMModelServer", startInfoMsg);

            // wait here until a message from the JointDTMModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from JointDTMModelServer that DTM has run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("JointDTMModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the JointDTMModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("JointDTMModelServer", exitMsg);
			}
			
            if (LOGGING) {
                logger.info("Memory after joint tour DTM");
                showMemory();
            }

        }




        if (((String) propertyMap.get("RUN_NON_MANDATORY_DTM")).equalsIgnoreCase("true")) {
    		//if FTA_Restart_run is false, run non-mandatory tour generation step, otherwise skip
            if ( !FtaRestartRun ){
	            // get the Household[] from the HhArrayServer
	            if (LOGGING)
	                logger.info(this.name + " requesting array of households for individual non-mandatory tour generation.");
	            sendHHsMsg.setId(MessageID.SEND_HHS_ARRAY);
	            sendTo("HhArrayServer", sendHHsMsg);
	
	            // wait here until a message from the HhArrayServer says it's done sending the big hh array.
	            waitMsg = receivePort.receive();
	            while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HHS_ARRAY_KEY))) {
	                waitMsg = receivePort.receive();
	            }
	
	            hhs = (Household[]) waitMsg.getValue(MessageID.HHS_ARRAY_KEY);
	
	            // run Individual Non-Mandatory Tour Generation Models
	            IndividualNonMandatoryToursModel inmtModel = new IndividualNonMandatoryToursModel(propertyMap, hhs);
	            inmtModel.runMaintenanceFrequency();
	            inmtModel.runMaintenanceAllocation();
	            inmtModel.runDiscretionaryWorkerStudentFrequency();
	            inmtModel.runDiscretionaryNonWorkerFrequency();
	            inmtModel.runDiscretionaryChildFrequency();
	            inmtModel.runAtWorkFrequency();
	
	            updateHHsMsg.setId(MessageID.UPDATE_HHS_ARRAY);
	            updateHHsMsg.setValue(MessageID.UPDATED_HHS, hhs);
	            sendTo("HhArrayServer", updateHHsMsg);
	
	            // wait here until a message from the HhArrayServer says it's done updating the big hh array.
	            waitMsg = receivePort.receive();
	            while (!(waitMsg.getSender().equals("HhArrayServer") && waitMsg.getId().equals(MessageID.HHS_ARRAY_KEY))) {
	                waitMsg = receivePort.receive();
	            }
	
	            inmtModel = null;
	            hhs = null;
	
	            if (LOGGING) {
	                logger.info("Memory after individual non-mandatory tour generation");
	                showMemory();
	            }

            }

            // send a resetHouseholdCount to the hhArrayServer to tell it to zero its hhs processed count.
            resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
            sendTo("HhArrayServer", resetHHsMsg);

            // send a message to the IndivDTMModelServer so that the indiv. non-mandatory DTM model will be run.
            if (LOGGING)
                logger.info(this.name +  " starting the individual non-mandatory DTM model.");
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("IndivDTMModelServer", startInfoMsg);

            // wait here until a message from the IndivDTMModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from IndivDTMModelServer that DTM has run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("IndivDTMModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the IndivDTMModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("IndivDTMModelServer", exitMsg);
			}
            if (LOGGING) {
                logger.info("Memory after individual non-mandatory tour DTM");
                showMemory();
            }

        }




        if (((String) propertyMap.get("RUN_ATWORK_DTM")).equalsIgnoreCase("true")) {

            // send a message to the AtWorkDTMModelServer so that the atwork subtour DTM model will be run.
            if (LOGGING)
                logger.info(this.name + " starting the at-work DTM model.");
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("AtWorkDTMModelServer", startInfoMsg);

            // wait here until a message from the AtWorkDTMModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from AtWorkDTMModelServer that DTM has run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("AtWorkDTMModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the AtWorkDTMModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("AtWorkDTMModelServer", exitMsg);
			}
            if (LOGGING) {
                logger.info("Memory after at-work tour DTM");
                showMemory();
            }


        }







        if (((String) propertyMap.get("RUN_MANDATORY_STOPS")).equalsIgnoreCase("true")) {

            // send a resetHouseholdCount to the hhArrayServer to tell it to zero its hhs processed count.
            resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
            sendTo("HhArrayServer", resetHHsMsg);

            // send a message to the MandatoryStopsModelServer so that the mandatory stops model will be run.
            if (LOGGING)
                logger.info(this.name + " starting the mandatory Stops model server.");
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("MandatoryStopsModelServer", startInfoMsg);

            // wait here until a message from the IndivDTMModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from MandatoryStopsModelServer that Stops models have run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("MandatoryStopsModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the MandatoryStopsModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("MandatoryStopsModelServer", exitMsg);
			}
            if (LOGGING) {
                logger.info("Memory after mandatory tour Stops");
                showMemory();
            }

        }



        if (((String) propertyMap.get("RUN_JOINT_STOPS")).equalsIgnoreCase("true")) {

            // send a resetHouseholdCount to the hhArrayServer to tell it to zero its hhs processed count.
            resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
            sendTo("HhArrayServer", resetHHsMsg);

            // send a message to the JointStopsModelServer so that the joint stops model will be run.
            if (LOGGING)
                logger.info(this.name + " starting the joint Stops model server.");
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("JointStopsModelServer", startInfoMsg);

            // wait here until a message from the JointStopsModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from JointStopsModelServer that Stops models have run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("JointStopsModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the JointStopsModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("JointStopsModelServer", exitMsg);
			}
            if (LOGGING) {
                logger.info("Memory after joint tour Stops");
                showMemory();
            }

        }




        if (((String) propertyMap.get("RUN_NON_MANDATORY_STOPS")).equalsIgnoreCase("true")) {

            // send a resetHouseholdCount to the hhArrayServer to tell it to zero its hhs processed count.
            resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
            sendTo("HhArrayServer", resetHHsMsg);

            // send a message to the IndivStopsModelServer so that the indiv. non-mandatory stops model will be run.
            if (LOGGING)
                logger.info(this.name + " starting the individual non-mandatory Stops model server.");
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("IndivStopsModelServer", startInfoMsg);

            // wait here until a message from the IndivDTMModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from IndivStopsModelServer that Stops models have run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("IndivStopsModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the IndivStopsModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("IndivStopsModelServer", exitMsg);
			}
            if (LOGGING) {
                logger.info("Memory after individual non-mandatory tour Stops");
                showMemory();
            }

        }



        if (((String) propertyMap.get("RUN_ATWORK_STOPS")).equalsIgnoreCase("true")) {

            // send a resetHouseholdCount to the hhArrayServer to tell it to zero its hhs processed count.
            resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
            sendTo("HhArrayServer", resetHHsMsg);

            // send a message to the AtWorkStopsModelServer so that the at-work Stops models will be run.
            if (LOGGING)
                logger.info(this.name + " starting the at-work Stops model server.");
            startInfoMsg.setId(MessageID.START_INFO);
            startInfoMsg.setValue(MessageID.PROPERTY_MAP_KEY, propertyMap);
            startInfoMsg.setValue(MessageID.ZONAL_DATA_MANAGER_KEY, zdm);
            startInfoMsg.setValue(MessageID.TOD_DATA_MANAGER_KEY, tdm);
            startInfoMsg.setValue(MessageID.STATIC_ZONAL_DATA_MAP_KEY, staticZonalDataMap);
            startInfoMsg.setValue(MessageID.STATIC_TOD_DATA_MAP_KEY, staticTodDataMap);
            sendTo("AtWorkStopsModelServer", startInfoMsg);

            // wait here until a message from the AtWorkStopsModelServer says it's done processing all hhs.
            if (LOGGING)
                logger.info(this.name + " waiting to hear from AtWorkStopsModelServer that Stops models have run for all hhs.");
            waitMsg = receivePort.receive();
            while (!(waitMsg.getSender().equals("AtWorkStopsModelServer") && waitMsg.getId().equals(MessageID.EXIT))) {
                waitMsg = receivePort.receive();
            }

            // send an exit message to the AtWorkStopsModelServer to tell it to free memory in its workers and itself.
			if ( iteration+1 == numberOfIterations ) {
	            exitMsg.setId(MessageID.EXIT);
	            sendTo("AtWorkStopsModelServer", exitMsg);
			}
            if (LOGGING) {
                logger.info("Memory after at-work tour Stops");
                showMemory();
            }

        }


    }




    public void onMessage(Message msg) {
    }







    private void runPopulationSynthesizer() {

        TableDataSet zoneTable = zdm.getZonalTableDataSet();

        //open files
        String PUMSFILE = (String) propertyMap.get("PUMSData.file");
        String PUMSDICT = (String) propertyMap.get("PUMSDictionary.file");
        String OUTPUT_HHFILE = (String) propertyMap.get("SyntheticHousehold.file");
        String ZONAL_TARGETS_HHFILE = (String) propertyMap.get("ZonalTargets.file");

        // build the PUMS data structure and read input data
        PUMSData pums = new PUMSData(PUMSFILE, PUMSDICT);
        pums.readData(PUMSFILE);
        pums.createPumaIndices();

        // create a synthetic population
        SyntheticPopulation sp = new SyntheticPopulation(pums, zoneTable);
        
        String zonalStudentsInputFileName = (String) propertyMap.get("UnivStudentsTaz.file");
        String zonalStudentsOutputFileName = (String) propertyMap.get("UnivStudentsTazOutput.file");

        sp.runSynPop(OUTPUT_HHFILE, ZONAL_TARGETS_HHFILE, zonalStudentsInputFileName, zonalStudentsOutputFileName);
        pums = null;
        sp = null;

        if (LOGGING)
            logger.info("end of model 0");

    }



    private void runAutoOwnershipModel() {
    	
    	logger.info("beginning of runAutoOwnershipModel");

        // run the hh auto ownership choice model
        AutoOwnership ao = new AutoOwnership(propertyMap);
        ao.runAutoOwnership();
        ao = null;

        if (LOGGING)
            logger.info("end of model 1");

    }



    private void runDailyActivityPatternModels() {

        // run the daily activity pattern choice models
        // preschool children
        Model21 m21 = new Model21(propertyMap);
        m21.runPreschoolDailyActivityPatternChoice();
        m21=null;

        // predriving children
        Model22 m22 = new Model22(propertyMap);
        m22.runPredrivingDailyActivityPatternChoice();
        m22=null;

        // driving children
        Model23 m23 = new Model23(propertyMap);
        m23.runDrivingDailyActivityPatternChoice();
        m23=null;

        // students
        Model24 m24 = new Model24(propertyMap);
        m24.runStudentDailyActivityPatternChoice();
        m24=null;

        // full time workers
        Model25 m25 = new Model25(propertyMap);
        m25.runWorkerFtDailyActivityPatternChoice();
        m25=null;

        // full time workers
        Model26 m26 = new Model26(propertyMap);
        m26.runWorkerPtDailyActivityPatternChoice();
        m26=null;

        // non workers
        Model27 m27 = new Model27(propertyMap);
        m27.runNonworkerDailyActivityPatternChoice();
        m27=null;


        if (LOGGING)
            logger.info("end of models 2.1-2.7");

    }



    public static void showMemory() {

    	long runningTime=0;
    	long markTime=0;
    	
        runningTime = System.currentTimeMillis() - markTime;
        if (logger.isDebugEnabled()) {
            logger.debug("total running minutes = " + (float) ((runningTime / 1000.0) / 60.0));
            logger.debug("totalMemory()=" + Runtime.getRuntime().totalMemory() + " mb.");
            logger.debug("freeMemory()=" + Runtime.getRuntime().freeMemory() + " mb.");
        }

    }



    private void runDOSCommand(String command) {
        try {
            logger.info ("issuing DOS command: " + command);
            sendCommand(command);
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception ocurred for command: " + command);
        }
    }



    private void sendCommand(String command) throws InterruptedException {
        try {

            String s;
            Process proc = Runtime.getRuntime().exec( getDosCommandFolderName() + "\\cmd.exe /C " + command );

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
            System.err.println("exception: " + e);
        }
    }


    /**
     * Copy files from server to workers
     * @param fileType represents file types, either "socec" or "zip"
    private void copyFileToWorkers(String fileType) {
        //get worker nodes
        NodeDef[] nodes = DAF.getRemoteNodeDefinitions();
        int NoWorkers = nodes.length;
        String[] ipaddress = new String[NoWorkers];

        for(int i=0; i<NoWorkers; i++){
          ipaddress[i]=nodes[i].getAddress();
        }

        
        // root level data directory for defined shares on each machine
		String shareDirecory = (String) propertyMap.get("share.directory");
		String shareUser = (String) propertyMap.get("share.user");
		String sharePassword = (String) propertyMap.get("share.password");

        //target directory on workers
        String targetDir;

        if (fileType.equals("socec")) {
			
			String temp = (String) propertyMap.get("TAZData.file");
			temp = temp.replace('/', '\\');
			targetDir = temp.substring(0,temp.lastIndexOf("\\"));

            for (int i = 0; i < NoWorkers; i++) {
                
                String netUseCommmand = "net use \\\\" + ipaddress[i] + "\\" + shareDirecory;
				netUseCommmand = netUseCommmand.replace('/', '\\');
				netUseCommmand = netUseCommmand + " /user:" + shareUser + " " + sharePassword;
				runDOSCommand(netUseCommmand);
                
				String command = "copy " + (String) propertyMap.get("TAZData.file") + " \\"+"\\" + ipaddress[i] + targetDir;
				command=command.replace('/', '\\');
				runDOSCommand(command);
                
				command = "copy " + (String) propertyMap.get("TAZMainData.file") + " \\"+"\\" + ipaddress[i] + targetDir;
				command=command.replace('/', '\\');
				runDOSCommand(command);
                
                command = "copy " + (String) propertyMap.get("TAZAccessibility.file") + " \\"+"\\" + ipaddress[i] + targetDir;
                command=command.replace('/', '\\');
                runDOSCommand(command);
                
            }
        }
        else if (fileType.equals("tpplus")) {
			
            String sourceFiles = "*.skm";
            String sourceDir = (String) propertyMap.get("SkimsDirectory.tpplus");
            targetDir = sourceDir;

            for (int i = 0; i < NoWorkers; i++) {

				String netUseCommmand = "net use \\\\" + ipaddress[i] + "\\" + shareDirecory;
				netUseCommmand = netUseCommmand.replace('/', '\\');
				netUseCommmand = netUseCommmand + " /user:" + shareUser + " " + sharePassword;
				runDOSCommand(netUseCommmand);
                
                String command = "copy " + sourceDir + "\\" + sourceFiles + " \\" + "\\" + ipaddress[i]+ targetDir;
                command=command.replace('/', '\\');
                runDOSCommand(command);
                
            }
        }
        else {
            logger.error("illegal file type encountered when copy files to workers");
        }
    }
     */

    
    private void executeAssignmentSkimming(int iteration){

        String scenario=(String)propertyMap.get("scenario");
        String baseYearScenario=(String)propertyMap.get("BaseYearScenario");
		//int year=Integer.parseInt(scenario.substring(0,4));
		
		String RUN_TRANSIT_ASSIGNMENT_SKIMMING=(String)propertyMap.get("RUN_TRANSIT_ASSIGNMENT_SKIMMING");
		String RUN_HIGHWAY_ASSIGNMENT_SKIMMING=(String)propertyMap.get("RUN_HIGHWAY_ASSIGNMENT_SKIMMING");
		String RUN_EVAL_TGFLOAD=(String)propertyMap.get("RUN_EVAL_TGFLOAD");

		//create DOS commands
		String run_cmvCmd=(String)propertyMap.get("run_cmv.batch")+" "+scenario;
		String run_extCmd=(String)propertyMap.get("run_ext.batch")+" "+scenario;
		String run_extfutureCmd=(String)propertyMap.get("run_extfuture.batch")+" "+scenario;
		String run_hasnammdCmd=(String)propertyMap.get("run_hasnammd.batch")+" "+scenario;
		String run_hskimfCmd=(String)propertyMap.get("run_hskimf.batch")+" "+scenario;
		String run_hasnCmd=(String)propertyMap.get("run_hasn.batch")+" "+scenario;
		String run_tasnCmd=(String)propertyMap.get("run_tasn.batch")+" "+scenario;
		String run_evalCmd=(String)propertyMap.get("run_eval.batch")+" "+scenario;
		String run_tgfloadCmd=(String)propertyMap.get("run_tgfload.batch")+" "+scenario;
		String run_tskimfCmd=(String)propertyMap.get("run_tskimf.batch")+" "+scenario;
		String run_bestCmd=(String)propertyMap.get("run_best.batch")+" "+scenario;
		
		String assignDir = (String) propertyMap.get("AssignDirectory.tpplus");
		
        String skimDir = (String) propertyMap.get("SkimsDirectory.tpplus");
        logger.info ("copying skim matrix files (*.skm) prior to assignments from " + skimDir + " to " + skimDir+"\\Iter"+(iteration+1));
        String command = "copy " + skimDir + "\\" + "*.skm " + skimDir+"\\Iter"+(iteration+1);
        command=command.replace('/', '\\');
        runDOSCommand(command); 
        
        String tripsDir = (String) propertyMap.get("TripsDirectory.tpplus");
        logger.info ("copying trip matrix files (*.tpp) prior to assignments from " + tripsDir + " to " + tripsDir+"\\Iter"+(iteration+1));
        command = "copy " + tripsDir + "\\" + "*.tpp " + tripsDir+"\\Iter"+(iteration+1);
        command=command.replace('/', '\\');
        runDOSCommand(command); 
        
        
        //run external and commercial procedures in all global iterations
        
	    //if future year scenario +Y, othewise +N
		if(!scenario.equalsIgnoreCase(baseYearScenario)){
	    	runDOSCommand(run_extfutureCmd+" Y");
	    }else{
	    	runDOSCommand(run_extfutureCmd+" N");
	    }
	    runDOSCommand(run_cmvCmd);
	    runDOSCommand(run_extCmd);
      
		// first or last iteration
		if(iteration+1 == 1 || iteration+1 == numberOfIterations) { 

		    // if is 1st iteration in multiple iterations
		    if( iteration+1 == 1 && iteration+1!=numberOfIterations) {
              
		        if(RUN_HIGHWAY_ASSIGNMENT_SKIMMING.equals("true")){
		            runDOSCommand(run_hasnammdCmd);
		            runDOSCommand(run_hskimfCmd);
		        }	

		        if(RUN_TRANSIT_ASSIGNMENT_SKIMMING.equals("true")){
		            runDOSCommand(run_tskimfCmd);
		            runDOSCommand(run_bestCmd);
		        }

		    }
		    // last iteration only
		    else {
              
		        if(RUN_HIGHWAY_ASSIGNMENT_SKIMMING.equals("true")){
		            runDOSCommand(run_hasnCmd);
		            runDOSCommand(run_hskimfCmd);
		        }	

		        if(RUN_TRANSIT_ASSIGNMENT_SKIMMING.equals("true")){
		            runDOSCommand(run_tasnCmd);
		            runDOSCommand(run_tskimfCmd);
		            runDOSCommand(run_bestCmd);
		        }
              
		        if(RUN_EVAL_TGFLOAD.equals("true")){
		            runDOSCommand(run_evalCmd);
		            runDOSCommand(run_tgfloadCmd);
		        }

		    }
          
		}
		// iterations 2,...,n-1 of n 
		else {
	      
		    if(RUN_HIGHWAY_ASSIGNMENT_SKIMMING.equals("true")){
		        runDOSCommand(run_hasnammdCmd);
		        runDOSCommand(run_hskimfCmd);
		    }
		    
		    if(RUN_TRANSIT_ASSIGNMENT_SKIMMING.equals("true")){
		        runDOSCommand(run_tskimfCmd);
	            runDOSCommand(run_bestCmd);
		    }

		}
		
		//copy intermediate assignment results to sub iteration folders  
		String tempDir=assignDir.trim();
		
        command = "copy "+tempDir + "\\" + "*.trp " + tempDir+"\\Iter"+(iteration+1);
        command=command.replace('/', '\\');
        runDOSCommand(command);	
        command = "copy "+tempDir + "\\" + "*.lod " + tempDir+"\\Iter"+(iteration+1);
        command=command.replace('/', '\\');
        runDOSCommand(command);
        command = "copy "+tempDir + "\\" + "*.dbf " + tempDir+"\\Iter"+(iteration+1);
        command=command.replace('/', '\\');
        runDOSCommand(command);
        command = "copy "+tempDir + "\\" + "*.dat " + tempDir+"\\Iter"+(iteration+1);
        command=command.replace('/', '\\');
        runDOSCommand(command);
        command = "copy "+tempDir + "\\" + "hasn.s " + tempDir+"\\Iter"+(iteration+1);
        command=command.replace('/', '\\');
        runDOSCommand(command);
	      
    }
    
    /*
     * Write disk object array
     */
    private void writeDiskObjectArray(Household [] hhs){

    	int NoHHs=hhs.length;
    	
    	//create disk object array
    	String diskObjectArrayFile=(String)propertyMap.get("DiskObjectArrayOutput.file");
    	
    	try{
    		DiskObjectArray diskObjectArray=new DiskObjectArray(diskObjectArrayFile,NoHHs,10000);       	
    		//write each hh to disk object array
    		for(int i=0; i<NoHHs; i++){
        		diskObjectArray.add(i,hhs[i]);
        	}
    	}catch(IOException e){
    		logger.fatal("can not open disk object array file for writing");
    	}
    }
        
    private void writeDiskObjectZDMTDM(ZonalDataManager zdm, TODDataManager tdm, HashMap staticZonalDataMap, HashMap staticTodDataMap, HashMap propertyMap){
    	if ( (String)propertyMap.get("DiskObjectZDMTDM.file") != null ) {
	    	String diskObjectZDMTDMFile=(String)propertyMap.get("DiskObjectZDMTDM.file");
	    	try{
	    		FileOutputStream out=new FileOutputStream(diskObjectZDMTDMFile);
	        	ObjectOutputStream s=new ObjectOutputStream(out);
	        	s.writeObject(zdm);
	        	s.writeObject(tdm);
	        	s.writeObject(staticZonalDataMap);
	        	s.writeObject(staticTodDataMap);
	        	s.flush();
	        	s=null;
	    	}catch(IOException e){
	    		System.err.println(e);
	    	}
    	}
    }
    
    private ZDMTDM readDiskObjectZDMTDM(HashMap propertyMap){
    	ZDMTDM result=new ZDMTDM();
    	String diskObjectZDMTDMFile=(String)propertyMap.get("DiskObjectZDMTDM.file"); 
    	try{
    		FileInputStream in=new FileInputStream(diskObjectZDMTDMFile);
        	ObjectInputStream s=new ObjectInputStream(in);
        	result.setZDM((ZonalDataManager)s.readObject());
        	result.setTDM((TODDataManager)s.readObject());
        	result.setStaticZonalDataMap((HashMap)s.readObject());
        	result.setStaticTodDataMap((HashMap)s.readObject());
    	}catch(IOException e){
    		System.err.println(e);
    	}catch(ClassNotFoundException e){
    		System.err.println(e);
    	}
    	return result;
    } 
    
    
    private String getDosCommandFolderName() {
		return (String) propertyMap.get("CMD_LOCATION");
    }
    
    private void startMatrixServer()
    {

        String serverAddress = (String)propertyMap.get("MatrixServerAddress");
        int serverPort = Integer.parseInt( (String)propertyMap.get("MatrixServerPort") );

        try
        {
            MatrixDataServerIf ms = new MatrixDataServerRmi(serverAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
            ms.testRemote( Thread.currentThread().getName() );
            ms.start32BitMatrixIoServer(MatrixType.TPPLUS);

            MatrixDataManager mdm = MatrixDataManager.getInstance();
            mdm.setMatrixDataServerObject(ms);
        }
        catch (Exception e) {
            logger.error( "exception caught setting up connection to remote matrix server -- exiting.", e );
            throw new RuntimeException();
        }

    }

}
