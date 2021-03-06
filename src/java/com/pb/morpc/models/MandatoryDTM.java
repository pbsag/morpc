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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;


public class MandatoryDTM implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	protected static Logger logger = Logger.getLogger("com.pb.morpc.models");
	
	private static final String NUM_THREADS_PROPERTY = "numThreads";
	private static final String HHS_PER_THREAD_PROPERTY = "hhsPerThread";
	
	private DTMHousehold[] dtmHH;
	private HouseholdArrayManager hhMgr;
	private HashMap<String,String> propertyMap;

    private BlockingQueue<DTMHousehold> modelQueue = null;
    private int modelIndex = 0;
	
	private int numThreads = -1;
	private int hhsPerThread = -1;
	
	
	
	
    public MandatoryDTM (HashMap<String,String> propertyMap, HouseholdArrayManager hhMgr) {
        this.propertyMap = propertyMap;
        this.hhMgr = hhMgr;
		
        String numThreadsString = (String)propertyMap.get( NUM_THREADS_PROPERTY );
        if ( numThreadsString != null )
            numThreads = Integer.parseInt( numThreadsString );

        String hhsPerCpuString = (String)propertyMap.get( HHS_PER_THREAD_PROPERTY );
        if ( hhsPerCpuString != null )
        	hhsPerThread = Integer.parseInt( hhsPerCpuString );

        if ( numThreads <= 0 ) {
            dtmHH = new DTMHousehold[1];
            dtmHH[0] = new DTMHousehold ( propertyMap, TourType.MANDATORY_CATEGORY, TourType.MANDATORY_TYPES );
            dtmHH[0].setProcessorIndex ( 0 );       
        }
        else {
            dtmHH = new DTMHousehold[numThreads];
        	modelQueue = new LinkedBlockingQueue<DTMHousehold>( numThreads );
        }

    }
    


	public void doDcWork( int shadowPriceIter ) {
		
		// get the list of households to be processed
		Household[] hhList = hhMgr.getHouseholds();

		if (hhList == null)
			return;

		if ( numThreads <= 0 ) {
	        dtmHH[0].setShadowPricingIteration( shadowPriceIter );
	        dtmHH[0].resetHouseholdCount();
	        dtmHH[0].clearProbabilitiesMaps( TourType.MANDATORY_TYPES );
	        for (int i=0; i < hhList.length; i++) {
	            dtmHH[0].mandatoryTourDc (hhList[i]);
	        }

	        dtmHH[0].printTimes( TourType.MANDATORY_CATEGORY );	        
		}
		else {

            if ( modelIndex < numThreads ) {

                // create DTMHousehold objects
    	        dtmHH[modelIndex] = new DTMHousehold ( modelIndex, propertyMap, TourType.MANDATORY_CATEGORY, TourType.MANDATORY_TYPES );
    	        dtmHH[modelIndex].setShadowPricingIteration( shadowPriceIter );
    	        dtmHH[modelIndex].clearProbabilitiesMaps( TourType.MANDATORY_TYPES );
   
    	        modelQueue.offer( dtmHH[modelIndex] );
    	        modelIndex++;

            }

            calculateDcDistributed( hhList, shadowPriceIter );        

		}
		    
		hhMgr.sendResults ( hhList );
	}
	

    public void doTcMcWork() {
        
        // get the list of households to be processed
        Household[] hhList = hhMgr.getHouseholds();

        if (hhList == null)
            return;

        if ( numThreads <= 0 ) {
            dtmHH[0].resetHouseholdCount();
            dtmHH[0].setProcessorIndex( 0 );
            for (int i=0; i < hhList.length; i++) {
                hhList[i].setProcessorIndex ( 0 );       
                dtmHH[0].mandatoryTourTc (hhList[i]);
                dtmHH[0].mandatoryTourMc (hhList[i]);
                dtmHH[0].updateTimeWindows (hhList[i]);
            }
        }        
        else {
            calculateTcMcDistributed( hhList, numThreads );        
        }

        hhMgr.sendResults ( hhList );
    }
    
    
    private void calculateDcDistributed( Household[] hhList, int shadowPriceIter ) {

        ArrayList<int[]> startEndIndexList = getStartEndIndexList( hhList.length );

        
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        ExecutorCompletionService<List<Object>> completionService = new ExecutorCompletionService<List<Object>>(exec);

        
        int taskIndex = 1;
        for (int[] startEndIndices : startEndIndexList)
        {
            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];
            ManDcTask task = new ManDcTask( modelQueue, taskIndex, startIndex, endIndex, hhList );
            completionService.submit( task );
            taskIndex++;

        }
        
        logger.info(String.format("created %d ManDcTASKs with %d hhs each for %d total hhs.", startEndIndexList.size(), hhsPerThread, hhList.length) );


        int i = 0;
    	int start = 0;
    	int end = 0;
    	int modelId = 0;
        int task = 0;
		int numHouseholdsProcessed = 0;		
    	int nexp = 0;
        for ( i=1; i < taskIndex; i++ ) {

            try {

            	Future<List<Object>> resultFuture = completionService.take();
            	List<Object> resultBundle = resultFuture.get();
                task = (Integer) resultBundle.get(0);
                modelId = (Integer) resultBundle.get(1);
                start = (Integer) resultBundle.get(2);
                end = (Integer) resultBundle.get(3);
                numHouseholdsProcessed += ( end - start + 1 );

                if ( modelIndex < numThreads ) {

                    // create DTMHousehold objects
        	        dtmHH[modelIndex] = new DTMHousehold ( modelIndex, propertyMap, TourType.MANDATORY_CATEGORY, TourType.MANDATORY_TYPES );
        	        dtmHH[modelIndex].setShadowPricingIteration( shadowPriceIter );
        	        dtmHH[modelIndex].clearProbabilitiesMaps( TourType.MANDATORY_TYPES );

        	        modelQueue.offer( dtmHH[modelIndex] );
        	        modelIndex++;

                }

            }
            catch (InterruptedException e) {
                logger.error( "InterruptedException returned in place of result object, i=" + i, e );
                System.exit(-1);
            }
            catch (ExecutionException e) {
                logger.error( "ExecutionException returned in place of result object, i=" + i, e.getCause() );
                System.exit(-1);
            }

       		if ( (i+1) % (Math.pow(2,nexp)) == 0 ) {
	            nexp++;
                logger.info( String.format("returned ManDcTask %d, %d of %d, start=%d, end=%d, modelId=%d, numHhs=%d.", task, i, startEndIndexList.size(), start, end, modelId, numHouseholdsProcessed) );                
	        }

    	} // future

        logger.info( String.format("returned ManDcTask %d, %d of %d, start=%d, end=%d, modelId=%d, numHhs=%d.", task, i, startEndIndexList.size(), start, end, modelId, numHouseholdsProcessed) );                
    	logger.info( "shutting down ExecutorService" );
        exec.shutdown();
        logger.info( "returned all distributed ManDcTasks." );

    }


    
    private void calculateTcMcDistributed( Household[] hhList, int numThreads ) {

        ArrayList<int[]> startEndIndexList = getStartEndIndexList( hhList.length );

        
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        ExecutorCompletionService<List<Object>> completionService = new ExecutorCompletionService<List<Object>>(exec);

        
        int taskIndex = 1;
        for (int[] startEndIndices : startEndIndexList)
        {
            int startIndex = startEndIndices[0];
            int endIndex = startEndIndices[1];
            ManTcMcTask task = new ManTcMcTask( modelQueue, taskIndex, startIndex, endIndex, hhList );
            completionService.submit( task );
            taskIndex++;

        }

        logger.info(String.format("created %d ManTcMcTASKs with %d hhs each for %d total hhs.", startEndIndexList.size(), hhsPerThread, hhList.length) );

        
        int i = 0;
    	int start = 0;
    	int end = 0;
    	int modelId = 0;
        int task = 0;
		int numHouseholdsProcessed = 0;		
    	int nexp = 0;
        for ( i=1; i < taskIndex; i++ ) {

            try {

            	Future<List<Object>> resultFuture = completionService.take();
            	List<Object> resultBundle = resultFuture.get();
                task = (Integer) resultBundle.get(0);
                modelId = (Integer) resultBundle.get(1);
                start = (Integer) resultBundle.get(2);
                end = (Integer) resultBundle.get(3);
                numHouseholdsProcessed += ( end - start + 1 );

                if ( modelIndex < numThreads ) {

                    // create DTMHousehold objects
        	        dtmHH[modelIndex] = new DTMHousehold ( modelIndex, propertyMap, TourType.MANDATORY_CATEGORY, TourType.MANDATORY_TYPES );

        	        modelQueue.offer( dtmHH[modelIndex] );
        	        modelIndex++;

                }

            }
            catch (InterruptedException e) {
                logger.error( "InterruptedException returned in place of result object, i=" + i, e );
                System.exit(-1);
            }
            catch (ExecutionException e) {
                logger.error( "ExecutionException returned in place of result object, i=" + i, e.getCause() );
                System.exit(-1);
            }

       		if ( (i+1) % (Math.pow(2,nexp)) == 0 ) {
	            nexp++;
                logger.info( String.format("returned ManTcMcTask %d, %d of %d, start=%d, end=%d, modelId=%d, numHhs=%d.", task, i, startEndIndexList.size(), start, end, modelId, numHouseholdsProcessed) );
	        }

    	} // future

        logger.info( String.format("returned ManTcMcTask %d, %d of %d, start=%d, end=%d, modelId=%d, numHhs=%d.", task, i, startEndIndexList.size(), start, end, modelId, numHouseholdsProcessed) );      
    	logger.info( "shutting down ExecutorService" );
        exec.shutdown();
        logger.info( "returned all distributed ManTcMcTasks." );

    }


    
    private ArrayList<int[]> getStartEndIndexList( int hhListLength ) {

        int startIndex = 0;
        int endIndex = 0;

        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>();

        // assign start and end ranges of hhList array indices to be used to assign to tasks
        while ( endIndex < hhListLength - 1 )
        {
            endIndex = startIndex + hhsPerThread - 1;

            if ( endIndex + hhsPerThread > hhListLength )
                endIndex = hhListLength - 1;

            int[] startEndIndices = new int[2];
            startEndIndices[0] = startIndex;
            startEndIndices[1] = endIndex;
            startEndIndexList.add(startEndIndices);

            startIndex += hhsPerThread;
        }
        
        return startEndIndexList;
        
    }
    
	
	public void resetHouseholdCount () {
		dtmHH[0].resetHouseholdCount();
	}
	

	public void setShadowPricingIteration ( int iter ) {
		dtmHH[0].setShadowPricingIteration( iter );
	}
	

	public void printTimes ( short tourTypeCategory ) {
		dtmHH[0].printTimes( tourTypeCategory );
	}

	
}
