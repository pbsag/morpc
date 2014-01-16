package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * worker class for running mandatory destination choice for
 * a list of households in a distributed environment.
 */

import com.pb.morpc.models.DTMHousehold;
import com.pb.morpc.structures.Household;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;


public class ManTcMcTask implements Callable<List<Object>> {

	private Logger logger = Logger.getLogger("com.pb.morpc.models");

	private BlockingQueue<DTMHousehold> modelQueue;
	private Household[] hhList;
	private int taskIndex;
	private int startIndex;
	private int endIndex;
	

    public ManTcMcTask ( BlockingQueue<DTMHousehold> modelQueue, int taskIndex, int startIndex, int endIndex, Household[] hhList ) {
    	this.modelQueue = modelQueue;
        this.taskIndex = taskIndex;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.hhList = hhList;
    }


    public List<Object> call() {

    	DTMHousehold dtmHH = null;
    	try {
			dtmHH = modelQueue.take();
		}
    	catch (InterruptedException e1) {
            logger.fatal ( "InterruptedException caught taking DTMHousehold from modelQueue in ManTcMcTask " + taskIndex + ", hhList range [" + startIndex + "," + endIndex + "]." );
            logger.fatal ( e1.getCause().getMessage() );
            logger.fatal ( "", e1 );
            System.exit(-1);
		}
    	
    	int processorIndex = dtmHH.getProcessorIndex();
        dtmHH.setProcessorIndex( processorIndex );
        for (int i=startIndex; i <= endIndex; i++) {
            try {
                hhList[i].setProcessorIndex ( processorIndex );       
                dtmHH.mandatoryTourTc (hhList[i]);
                dtmHH.mandatoryTourMc (hhList[i]);
                dtmHH.updateTimeWindows (hhList[i]);
            }
            catch (java.lang.Exception e) {
                logger.fatal ("exception caught in ManTcMcTask " + taskIndex + ", hhList range [" + startIndex + "," + endIndex + "] for household id=" + hhList[i].getID() + ".", e );
                hhList[i].writeContentToLogger(logger);
                System.exit(-1);
            }
        }

        List<Object> resultBundle = new ArrayList<Object>(4);
        resultBundle.add(taskIndex);
        resultBundle.add(processorIndex);
        resultBundle.add(startIndex);
        resultBundle.add(endIndex);

        modelQueue.offer( dtmHH );
        
        return resultBundle;
	}

}
