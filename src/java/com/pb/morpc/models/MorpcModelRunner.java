package com.pb.morpc.models;


/**
 * @author Jim Hicks
 *
 * Main program for MORPC model (single processor version)
 */


import com.pb.morpc.structures.TourType;
//import com.pb.morpc.report.Report;

import org.apache.log4j.Logger;


public class MorpcModelRunner extends MorpcModelBase {
    static Logger logger = Logger.getLogger(MorpcModelRunner.class);

    public MorpcModelRunner() {
        super();
    }

    public void runModels() {

		
		if ( ((String)propertyMap.get("RUN_MANDATORY_DTM")).equalsIgnoreCase("true") ) {
            
            // first apply destination choice for tours with shadow pricing
            zdm.balanceSizeVariables(hhMgr.getHouseholds());

            int shadowPriceIterations = (Integer.parseInt((String) propertyMap.get(
                        "NumberOfShadowPriceIterations")));

            // run destination, time-of-day, and mode choice models for indivdual mandatory tours
            MandatoryDTM manDTM = new MandatoryDTM(propertyMap, hhMgr);

            for (int i = 0; i < shadowPriceIterations; i++) {
                manDTM.setShadowPricingIteration(i + 1);
                manDTM.resetHouseholdCount();

                manDTM.doDcWork();
				zdm.sumAttractions(hhMgr.getHouseholds());
                zdm.reportMaxDiff();
                zdm.updateShadowPrices();
                zdm.updateSizeVariables();
                zdm.updateShadowPricingInfo(i);
            }

            // next apply time-of-day and mode choice for tours
            manDTM.resetHouseholdCount();
            manDTM.doTcMcWork();
            manDTM.printTimes(TourType.MANDATORY_CATEGORY);
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
            jointDTM.printTimes(TourType.JOINT_CATEGORY);
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
            nonManDTM.printTimes(TourType.NON_MANDATORY_CATEGORY);
            nonManDTM = null;
        }

		if ( ((String)propertyMap.get("RUN_ATWORK_DTM")).equalsIgnoreCase("true") ) {
            // run destination, time-of-day, and mode choice models for subtours within work tours
            AtWorkDTM atWorkDTM = new AtWorkDTM(propertyMap, hhMgr);
            atWorkDTM.doWork();
            atWorkDTM.printTimes(TourType.AT_WORK_CATEGORY);
            atWorkDTM = null;
        }

        com.pb.common.calculator.UtilityExpressionCalculator.clearData();

		if ( ((String)propertyMap.get("RUN_MANDATORY_STOPS")).equalsIgnoreCase("true") ) {

            // run stop frequency and stop location for each of the mandatory tours
            MandatoryStops manStops = new MandatoryStops(propertyMap, hhMgr);
            manStops.resetHouseholdCount();
            manStops.doSfcSlcWork();
            manStops.resetHouseholdCount();
            manStops.doSmcWork();
            manStops.printTimes(TourType.MANDATORY_CATEGORY);
            manStops = null;

		}
		
		
		if ( ((String)propertyMap.get("RUN_JOINT_STOPS")).equalsIgnoreCase("true") ) {
            // run stop frequency and stop location for each of the joint tours
            JointStops jointStops = new JointStops(propertyMap, hhMgr);
            jointStops.resetHouseholdCount();
            jointStops.doSfcSlcWork();
            jointStops.resetHouseholdCount();
            jointStops.doSmcWork();
            jointStops.printTimes(TourType.JOINT_CATEGORY);
            jointStops = null;
		}
		
		

		if ( ((String)propertyMap.get("RUN_NON_MANDATORY_STOPS")).equalsIgnoreCase("true") ) {
            // run stop frequency and stop location for each of the individual non-mandatory tours
            NonMandatoryStops nonManStops = new NonMandatoryStops(propertyMap, hhMgr);
            nonManStops.resetHouseholdCount();
            nonManStops.doSfcSlcWork();
            nonManStops.resetHouseholdCount();
            nonManStops.doSmcWork();
            nonManStops.printTimes(TourType.NON_MANDATORY_CATEGORY);
            nonManStops = null;
		}


		if ( ((String)propertyMap.get("RUN_ATWORK_STOPS")).equalsIgnoreCase("true") ) {
		    // run stop frequency and stop location for each of the at-work subtours
            AtWorkStops atWorkStops = new AtWorkStops(propertyMap, hhMgr);
            atWorkStops.resetHouseholdCount();
            atWorkStops.doSfcSlcWork();
            atWorkStops.resetHouseholdCount();
            atWorkStops.doSmcWork();
            atWorkStops = null;

            // release skims matrices used in stop frequency and stop location choice from memory
            com.pb.common.calculator.UtilityExpressionCalculator.clearData();
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

        // create a model runner object and run models
        MorpcModelRunner mr = new MorpcModelRunner();
        mr.runModels();

        logger.info("end of MORPC Demand Models");
        logger.info("full MORPC model run finished in " +
            ((System.currentTimeMillis() - startTime) / 60000.0) + " minutes");

        System.exit(0);
    }
}
