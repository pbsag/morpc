package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Main Model Sever for Distributed Application
 */
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.morpc.matrix.MatrixDataServer;
import com.pb.morpc.matrix.MatrixDataServerRmi;
import com.pb.morpc.models.TODDataManager;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.ZDMTDM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class MorpcModelGlobal {

    private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger(MorpcModelGlobal.class);

    String CMD_LOCATION;

    String hhFile;
    String personFile;
    String zonalFile;

    private ZonalDataManager zdm = null;
    private TODDataManager tdm = null;
    private HashMap<String, String> propertyMap = null;

    private MatrixDataServerIf ms;
    
    private long startTime = 0;

	int numberOfIterations;

    Household[] hhs = null;

    private boolean FtaRestartRun = false;

    
    public MorpcModelGlobal() {
    }


    private void runMorpcModelGlobal( String basePropertName, int numIters ) {

        numberOfIterations = numIters;
        
        startTime = System.currentTimeMillis();

        propertyMap = ResourceUtil.getResourceBundleAsHashMap( basePropertName + "1" );
        
        
        // set the global random number generator seed read from the properties file
        SeededRandom.setSeed ( Integer.parseInt( (String)propertyMap.get("RandomSeed") ) );
				
        // determine whether this model run is for an FTA Summit analysis or not
        if( (String)propertyMap.get("FTA_Restart_run") != null )
        	FtaRestartRun = ((String)propertyMap.get("FTA_Restart_run")).equalsIgnoreCase("true");
        else
        	FtaRestartRun = false;

        
        if(FtaRestartRun){
        	
        	if(((String)propertyMap.get("SKIP_TSKIM_FTA")).equalsIgnoreCase("false")){
	        	//prepare for a FTA restart run
	        	String scenario=(String)propertyMap.get("scenario");
	        	String run_tskim_ftaCmd=(String)propertyMap.get("run_tskim_fta.batch")+" "+scenario;
	    		runDOSCommand(run_tskim_ftaCmd);
        	}
        	
        	ZDMTDM zdmtdm=readDiskObjectZDMTDM();
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
	         runModelIteration( basePropertName, i );
	    }        	



        if (LOGGING) {
            logger.info("Memory after running reports - end of program");
            showMemory();

            logger.info("end of MORPC Demand Models");
            logger.info("full MORPC model run finished in " + ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");
        }

        
    }

    private void runModelIteration( String basePropertyName, int iteration ) {

        logger.info("Global iteration:" + (iteration+1) + " started.");
        
        // read the property file specific to this global iteration
        propertyMap = ResourceUtil.getResourceBundleAsHashMap( basePropertyName + (iteration+1) );
 
        MatrixIO32BitJvm ioVm32Bit = startMatrixServer();        
        
        //Wu added
        //if 2nd iteration starts from disk object array, then read zdm and tdm from ZDMTDMObjetOnDisk created in 1st iteration
        //don't need to do this in 3rd iteration, because in 3rd iteration, zdm and tdm is also from the 1st iteration ZDMTDMObjectOnDisk 
                
        if(((String) propertyMap.get("StartFromDiskObjectArray")).equalsIgnoreCase("true")){
        	ZDMTDM zdmtdm=readDiskObjectZDMTDM();
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
        
        zdm.updatePropertyMap( basePropertyName, iteration+1 );



        // create a model runner object and run models for this iteration
        try {        
            
            logger.info ("starting tour based model for global iteration " + (iteration+1) + " using " + basePropertyName + (iteration+1) + ".properties.");
            MorpcModelRunner mr = new MorpcModelRunner( basePropertyName + (iteration+1) );

            //if FTA_Restart_run is false, run PopSyn, otherwise skip it.
            if (!FtaRestartRun){
            
                // build a synthetic population
                if (((String) propertyMap.get("RUN_POPULATION_SYNTHESIS_MODEL")).equalsIgnoreCase( "true"))
                    mr.runPopulationSynthesizer();
                
            }
                        
            mr.runModels();
            
        }
        catch ( RuntimeException e ) {
            logger.error ( "RuntimeException caught in com.pb.morpc.models.MorpcModelRunner.runModels() -- exiting.", e );
        }
        


        if ( ioVm32Bit != null ) {
            // establish that matrix reader and writer classes will not use the RMI versions any longer.
            // local matrix i/o, as specified by setting types, is now the default again.
            ioVm32Bit.stopMatrixDataServer();
            
            // close the JVM in which the RMI reader/writer classes were running
            ioVm32Bit.stopJVM32();
            System.out.println ("matrix data server 32 bit process stopped.");
        }
        else {
            stop32BitMatrixIoServer();
            System.out.println ("remote matrix data server 32 bit process stopped.");
        }

        
		//execute assignment and skimming
        String SKIP_ASSIGNMENT_SKIMMING=(String)propertyMap.get("SKIP_ASSIGNMENT_SKIMMING");
        if(!SKIP_ASSIGNMENT_SKIMMING.equals("true")){
            executeAssignmentSkimming(iteration);
        }
        
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
        
        
        String run_msahskimfCmd=(String)propertyMap.get("run_msahskimf.batch")+" "+scenario+" "+(iteration+1);
        
        //run external and commercial procedures in all global iterations
        
	    //if future year scenario +Y, otherwise +N
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
		            //runDOSCommand(run_bestCmd);
		        }

		        runDOSCommand(run_msahskimfCmd);

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
		            //runDOSCommand(run_bestCmd);
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
	            //runDOSCommand(run_bestCmd);
		    }

	        runDOSCommand(run_msahskimfCmd);

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
        
    private void writeDiskObjectZDMTDM( ZonalDataManager zdm, TODDataManager tdm ){
    	if ( (String)propertyMap.get("DiskObjectZDMTDM.file") != null ) {
	    	String diskObjectZDMTDMFile=(String)propertyMap.get("DiskObjectZDMTDM.file");
	    	try{
	    		FileOutputStream out=new FileOutputStream(diskObjectZDMTDMFile);
	        	ObjectOutputStream s=new ObjectOutputStream(out);
	        	s.writeObject(zdm);
	        	s.writeObject(tdm);
	        	s.flush();
	        	s=null;
	    	}catch(IOException e){
	    		System.err.println(e);
	    	}
    	}
    }
     */
    
    private ZDMTDM readDiskObjectZDMTDM(){
    	ZDMTDM result=new ZDMTDM();
    	String diskObjectZDMTDMFile=(String)propertyMap.get("DiskObjectZDMTDM.file"); 
    	try{
    		FileInputStream in=new FileInputStream(diskObjectZDMTDMFile);
        	ObjectInputStream s=new ObjectInputStream(in);
        	result.setZDM((ZonalDataManager)s.readObject());
        	result.setTDM((TODDataManager)s.readObject());
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
    
    private MatrixIO32BitJvm startMatrixServer()
    {

        MatrixIO32BitJvm ioVm32Bit = null;
        
        MatrixType mt = null;
        String matrixFormatString = (String)propertyMap.get("format");
        if ( matrixFormatString.equalsIgnoreCase( "tpplus") ) {

            mt = MatrixType.TPPLUS;

            String serverAddress = (String)propertyMap.get("MatrixServerAddress");
            String serverPortString = (String)propertyMap.get("MatrixServerPort");
            int serverPort = 0;

            if ( serverAddress != null && serverPortString != null ) {

                serverPort = Integer.parseInt( serverPortString );
                
                System.out.println("attempting connection to matrix server " + serverAddress + ":" + serverPort);

                try
                {
                    ms = new MatrixDataServerRmi(serverAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    ms.testRemote( Thread.currentThread().getName() );
                    ms.clear();
                    ms.start32BitMatrixIoServer( mt );

                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mdm.clearData();                
                    mdm.setMatrixDataServerObject(ms);

                    System.out.println( "connected to matrix server " + serverAddress + ":" + serverPort);
                }
                catch (Exception e) {
                    logger.error( "exception caught setting up connection to remote matrix server -- exiting.", e );
                    throw new RuntimeException();
                }

            }
            else {
                
                System.out.println ("starting matrix data server in a 32 bit process.");
                // start the 32 bit JVM used specifically for running matrix io classes
                ioVm32Bit = MatrixIO32BitJvm.getInstance();
                ioVm32Bit.startJVM32();
                
                // establish that matrix reader and writer classes will use the RMI versions for TRANSCAD format matrices
                ioVm32Bit.startMatrixDataServer( mt );

                MatrixDataManager mdm = MatrixDataManager.getInstance();
                mdm.clearData();                
                mdm.setMatrixDataServerObject( ms );
            }
        
        }
        
        
        return ioVm32Bit;
        
    }

    private void stop32BitMatrixIoServer() {
        
        if ( ms != null )
            ms.stop32BitMatrixIoServer();
    }
    


    public static void main(String[] args) {

        if ( args.length < 2 ) {
            logger.error( "Invalid number of arguments for " + MorpcModelGlobal.class.getCanonicalName() + "." );
            logger.error( "Expecting two arguments - first is String for base name of properties files, second is integer number of iterations." );
            logger.error( "For example:" );
            logger.error( "     " + MorpcModelGlobal.class.getCanonicalName() + " morpc 3" );
        }
        else {

            String baseProperties = args[0];
            int numIterations = Integer.parseInt( args[1] );

            MorpcModelGlobal mainObject = new MorpcModelGlobal();

            mainObject.runMorpcModelGlobal( baseProperties, numIterations );
            
        }


        System.exit(0);

    }

}
