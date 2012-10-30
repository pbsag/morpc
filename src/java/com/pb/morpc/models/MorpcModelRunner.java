package com.pb.morpc.models;


/**
 * @author Jim Hicks
 *
 * Main program for MORPC model (single processor version)
 */


import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.SeededRandom;
import com.pb.morpc.structures.Household;
import com.pb.morpc.matrix.MatrixDataServer;
import com.pb.morpc.matrix.MatrixDataServerRmi;
import com.pb.morpc.matrix.MorpcMatrixAggregaterTpp;
//import com.pb.morpc.report.Report;

import org.apache.log4j.Logger;


public class MorpcModelRunner extends MorpcModelBase {
    static Logger logger = Logger.getLogger(MorpcModelRunner.class);


    public MorpcModelRunner( String basePropertyName ) {
        super( basePropertyName );        
    }

    public void runModels() {

		
        //instantiate ZonalDataManager and TODDataManager objects so that their static members are available to other classes (eg. DTMOutput)
        zdm = new ZonalDataManager ( propertyMap );
        tdm = new TODDataManager(propertyMap);
    
    
        // set the global random number generator seed read from the properties file
        SeededRandom.setSeed ( Integer.parseInt( (String)propertyMap.get("RandomSeed") ) );
    
        
        // if new skims were generated as part of this model run or prior to it, run aggregation step for TPP submode skims
        String property = (String)propertyMap.get("AGGREGATE_TPPLUS_SKIM_MATRICES");
        if ( property.equalsIgnoreCase("true") ) {
            
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
                AccessibilityIndicesTpp accInd = new AccessibilityIndicesTpp( basePropertyName );
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


        if ( ((String)propertyMap.get("RUN_MANDATORY_DTM")).equalsIgnoreCase("true") ) {
            
            // first apply destination choice for tours with shadow pricing
            zdm.balanceSizeVariables(hhMgr.getHouseholds());

            int shadowPriceIterations = (Integer.parseInt((String) propertyMap.get( "NumberOfShadowPriceIterations" )));

            // run destination, time-of-day, and mode choice models for indivdual mandatory tours
            MandatoryDTM manDTM = new MandatoryDTM(propertyMap, hhMgr);

            for (int i = 0; i < shadowPriceIterations; i++) {
                manDTM.doDcWork( i+1 );
				zdm.sumAttractions(hhMgr.getHouseholds());
                zdm.reportMaxDiff();
                zdm.updateShadowPrices();
                zdm.updateSizeVariables();
                zdm.updateShadowPricingInfo(i);
            }

            // next apply time-of-day and mode choice for tours
            manDTM.doTcMcWork();
            manDTM = null;
        }

		if ( ((String)propertyMap.get("RUN_JOINT_DTM")).equalsIgnoreCase("true") ) {
            // run Joint Tour Generation Models
            JointToursModel jtModel = new JointToursModel(propertyMap, hhMgr.getHouseholds());
            jtModel.runFrequencyModel();
            jtModel.runCompositionModel();
			jtModel.runParticipationModel();
            jtModel = null;

            // run destination, time-of-day, and mode choice models for joint tours
            JointDTM jointDTM = new JointDTM(propertyMap, hhMgr);
            jointDTM.doWork();
            jointDTM = null;
        }

		if ( ((String)propertyMap.get("RUN_NON_MANDATORY_DTM")).equalsIgnoreCase("true") ) {
            // run Individual Non-Mandatory Tour Generation Models
            IndividualNonMandatoryToursModel inmtModel = new IndividualNonMandatoryToursModel(propertyMap, hhMgr.getHouseholds());
            inmtModel.runMaintenanceFrequency();
            inmtModel.runMaintenanceAllocation();
            inmtModel.runDiscretionaryWorkerStudentFrequency();
            inmtModel.runDiscretionaryNonWorkerFrequency();
            inmtModel.runDiscretionaryChildFrequency();
            inmtModel.runAtWorkFrequency();
            inmtModel = null;

            // run destination, time-of-day, and mode choice models for indivdual non-mandatory tours
            NonMandatoryDTM nonManDTM = new NonMandatoryDTM(propertyMap, hhMgr);
            nonManDTM.doWork();
            nonManDTM = null;
        }

		if ( ((String)propertyMap.get("RUN_ATWORK_DTM")).equalsIgnoreCase("true") ) {
            // run destination, time-of-day, and mode choice models for subtours within work tours
            AtWorkDTM atWorkDTM = new AtWorkDTM(propertyMap, hhMgr);
            atWorkDTM.doWork();
            atWorkDTM = null;
        }

        //com.pb.common.calculator.UtilityExpressionCalculator.clearData();

		if ( ((String)propertyMap.get("RUN_MANDATORY_STOPS")).equalsIgnoreCase("true") ) {

            // run stop frequency and stop location for each of the mandatory tours
            MandatoryStops manStops = new MandatoryStops(propertyMap, hhMgr);
            manStops.doSfcSlcWork();
            manStops.doSmcWork();
            manStops = null;

		}
		
		
		if ( ((String)propertyMap.get("RUN_JOINT_STOPS")).equalsIgnoreCase("true") ) {
            // run stop frequency and stop location for each of the joint tours
            JointStops jointStops = new JointStops(propertyMap, hhMgr);
            jointStops.doSfcSlcWork();
            jointStops.doSmcWork();
            jointStops = null;
		}
		
		

		if ( ((String)propertyMap.get("RUN_NON_MANDATORY_STOPS")).equalsIgnoreCase("true") ) {
            // run stop frequency and stop location for each of the individual non-mandatory tours
            NonMandatoryStops nonManStops = new NonMandatoryStops(propertyMap, hhMgr);
            nonManStops.doSfcSlcWork();
            nonManStops.doSmcWork();
            nonManStops = null;
		}


		if ( ((String)propertyMap.get("RUN_ATWORK_STOPS")).equalsIgnoreCase("true") ) {
		    // run stop frequency and stop location for each of the at-work subtours
            AtWorkStops atWorkStops = new AtWorkStops(propertyMap, hhMgr);
            atWorkStops.doSfcSlcWork();
            atWorkStops.doSmcWork();
            atWorkStops = null;

            // release skims matrices used in stop frequency and stop location choice from memory
            //com.pb.common.calculator.UtilityExpressionCalculator.clearData();
        }

		
        //write big disk object array to disk
    	String diskObjectArrayFile=(String)propertyMap.get("DiskObjectArrayOutput.file");
    	if (diskObjectArrayFile != null)
    		hhMgr.writeDiskObjectArray( diskObjectArrayFile );

    	
        
        // write summary tables and .csv output files for DTM
        DTMOutput dtmOut = new DTMOutput(propertyMap,zdm);
		try {
			dtmOut.writeDTMOutput( hhMgr.getHouseholds() );
		} 
		catch (Exception e) {
            logger.fatal ("Caught runtime exception writing DTMOutput csv file.", e);
		}
		

		
		if ( ((String)propertyMap.get("WRITE_TRIP_TABLES")).equalsIgnoreCase("true") ) {
            
            try {
                dtmOut.writeTripTables( hhMgr.getHouseholds() );
                dtmOut = null;
            }
            catch (Exception e) {
                logger.fatal ("Caught runtime exception writing trip tables.", e);
            }
		    
		}


        
/*		
        Report report=new Report();
        report.generateReports();
*/
		
		
        logger.info("end of models 3-9");
    }
     //end runModels
    
    
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        MatrixIO32BitJvm ioVm32Bit = null;

        try {
            
            // create a model runner object and run models
            MorpcModelRunner mr = new MorpcModelRunner( args.length == 0 ? null : args[0] );

            if ( mr.serverAddress != null && mr.serverPort > 0 ) {
                try
                {
                    logger.info("attempting connection to matrix server " + mr.serverAddress + ":" + mr.serverPort);
        
                    MatrixDataManager mdm = MatrixDataManager.getInstance();
                    mr.ms = new MatrixDataServerRmi(mr.serverAddress, mr.serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                    mr.ms.testRemote("MorpcServerMain");
                    mdm.setMatrixDataServerObject(mr.ms);
                    logger.info( "connected to matrix server " + mr.serverAddress + ":" + mr.serverPort);
        
                } catch (Exception e)
                {
                    logger.error("exception caught running ctramp model components -- exiting.", e);
                    throw new RuntimeException();
                }
            }
            else {
                logger.info ("starting matrix data server in a 32 bit process.");
                // start the 32 bit JVM used specifically for running matrix io classes
                ioVm32Bit = MatrixIO32BitJvm.getInstance();
                ioVm32Bit.startJVM32();
                
                // establish that matrix reader and writer classes will use the RMI versions for TPPLUS format matrices
                ioVm32Bit.startMatrixDataServer( MatrixType.TPPLUS );
            }
            
            
            // run tour based models
            try {
            
                logger.info ("starting tour based model using " + ( args.length == 0 ? MorpcModelBase.PROPERTIES_FILE_BASENAME : args[0]) + ".properties.");
                mr.runModels();
                
            }
            catch ( RuntimeException e ) {
                logger.error ( "RuntimeException caught in com.pb.morpc.models.MorpcModelRunner.runModels() -- exiting.", e );
            }

            
            
            if ( mr.ms == null ) {
                // establish that matrix reader and writer classes will not use the RMI versions any longer.
                // local matrix i/o, as specified by setting types, is now the default again.
                ioVm32Bit.stopMatrixDataServer();
                
                // close the JVM in which the RMI reader/writer classes were running
                ioVm32Bit.stopJVM32();
                logger.info ("matrix data server 32 bit process stopped.");
            }
            else {
                mr.ms.stop32BitMatrixIoServer();
                logger.info ("matrix data server 32 bit process stopped.");
            }

        }
        catch (RuntimeException e) {
            logger.error ( "RuntimeException caught in com.pb.morpc.models.MorpcModelRunner.main() -- exiting.", e );

            // establish that matrix reader and writer classes will not use the RMI versions any longer.
            // local matrix i/o, as specified by setting types, is now the default again.
            ioVm32Bit.stopMatrixDataServer();

            // close the JVM in which the RMI reader/writer classes were running
            ioVm32Bit.stopJVM32();
            logger.info ("matrix data server 32 bit process stopped.");

        }

        
        logger.info("end of MORPC Demand Models");
        logger.info("full MORPC model run finished in " +
            ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

        System.exit(0);
    }
}
