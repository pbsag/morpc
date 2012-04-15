package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */


import com.pb.morpc.structures.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;


public class NonMandatoryDTM implements java.io.Serializable {

	
    protected static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
    private static final String NUM_THREADS_PROPERTY = "numThreads";
    
    private HashMap<String,String> propertyMap;
    private int numThreads = -1;
    private DTMHousehold[] dtmHH;
    private HouseholdArrayManager hhMgr;

	
    public NonMandatoryDTM (HashMap<String,String> propertyMap, HouseholdArrayManager hhMgr) {

		this.propertyMap = propertyMap;
		this.hhMgr = hhMgr;
		

        String numThreadsString = (String)propertyMap.get( NUM_THREADS_PROPERTY );
        if ( numThreadsString != null )
            numThreads = Integer.parseInt( numThreadsString );

        if ( numThreads <= 0 ) {
            dtmHH = new DTMHousehold[1];
            dtmHH[0] = new DTMHousehold ( propertyMap, TourType.NON_MANDATORY_CATEGORY, TourType.NON_MANDATORY_TYPES );
            dtmHH[0].setProcessorIndex ( 0 );       
        }
        else {
            dtmHH = new DTMHousehold[numThreads+1];
        }

    }
    


	public void doWork() {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;
			
			
        if ( numThreads <= 0 ) {
            dtmHH[0].resetHouseholdCount();
            for (int i=0; i < hhList.length; i++) {
                hhList[i].setProcessorIndex ( 0 );       
                dtmHH[0].indivNonMandatoryTourDc (hhList[i]);
                dtmHH[0].indivNonMandatoryTourTc (hhList[i]);
                dtmHH[0].indivNonMandatoryTourMc (hhList[i]);
            }
        }
        else {
            calculateDistributed( propertyMap, hhList, numThreads );        
        }

		hhMgr.sendResults ( hhList );
	}
	
	
    private ArrayList<int[]> getStartEndIndexList( int numThreads, int hhListLength ) {

        int packetSize = hhListLength / numThreads;

        int startIndex = 0;
        int endIndex = 0;

        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>();

        // assign start and end ranges of hhList array indices to be used to assign to tasks
        while ( endIndex < hhListLength - 1 )
        {
            endIndex = startIndex + packetSize - 1;

            if ( endIndex + packetSize > hhListLength )
                endIndex = hhListLength - 1;

            int[] startEndIndices = new int[2];
            startEndIndices[0] = startIndex;
            startEndIndices[1] = endIndex;
            startEndIndexList.add(startEndIndices);

            startIndex += packetSize;
        }
        
        return startEndIndexList;
        
    }
    
    
    private void calculateDistributed( HashMap<String,String> propertyMap, Household[] hhList, int numThreads ) {

        int numPackets = numThreads;
        ArrayList<int[]> startEndIndexList = getStartEndIndexList( numThreads, hhList.length );

        
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        ArrayList<Future<List<Object>>> results = new ArrayList<Future<List<Object>>>();

        
        int taskIndex = 1;
        for (int[] startEndIndices : startEndIndexList)
        {
            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];

            logger.info(String.format("creating NonMan DcTcMcTASK: %d range: %d to %d.", taskIndex, startIndex, endIndex));

            dtmHH[taskIndex] = new DTMHousehold ( taskIndex, propertyMap, TourType.NON_MANDATORY_CATEGORY, TourType.NON_MANDATORY_TYPES );

            NonManDcTcMcTask task = new NonManDcTcMcTask( dtmHH[taskIndex], taskIndex, startIndex, endIndex, hhList );

            results.add(exec.submit(task));
            taskIndex++;
        }

        for (Future<List<Object>> fs : results) {

            try {
                List<Object> resultBundle = fs.get();
                int task = (Integer) resultBundle.get(0);
                int start = (Integer) resultBundle.get(1);
                int end = (Integer) resultBundle.get(2);
                logger.info(String.format("returned nonMan dcTcMcTask %d of %d, start=%d, end=%d.", task, numPackets, start, end));
            }
            catch (InterruptedException e) {
                logger.error("InterruptedException returned in place of result object.", e);
                System.exit(-1);
            }
            catch (ExecutionException e) {
                logger.error("ExecutionException returned in place of result object.", e);
                System.exit(-1);
            }
            finally {
                exec.shutdown();
            }

        } // future

    }


	public void printTimes ( short tourTypeCategory ) {
		
		dtmHH[0].printTimes( tourTypeCategory );
							
	}

	
	
}
