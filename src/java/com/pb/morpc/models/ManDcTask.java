package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * worker class for running mandatory destination choice for
 * a list of households in a distributed environment.
 */

import com.pb.morpc.models.DTMHousehold;
import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.TourType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;


public class ManDcTask implements Callable<List<Object>> {

	private Logger logger = Logger.getLogger("com.pb.morpc.models");

	private BlockingQueue<DTMHousehold> modelQueue;
	private HashMap<String,String> propertyMap;
	private Household[] hhList;
	private int taskIndex;
	private int startIndex;
	private int endIndex;
	

    public ManDcTask ( BlockingQueue<DTMHousehold> modelQueue, HashMap<String,String> propertyMap, int taskIndex, int startIndex, int endIndex, Household[] hhList ) {
    	this.modelQueue = modelQueue;
    	this.propertyMap = propertyMap;
    	this.taskIndex = taskIndex;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.hhList = hhList;
    }


    public List<Object> call() {

    	DTMHousehold dtmHH = null;
    	try {
			dtmHH = modelQueue.take();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	int processorIndex = dtmHH.getProcessorIndex();
        dtmHH.setProcessorIndex( processorIndex );
        for (int i=startIndex; i <= endIndex; i++) {
            try {
                hhList[i].setProcessorIndex ( processorIndex );       
                dtmHH.mandatoryTourDc (hhList[i]);
            }
            catch (java.lang.Exception e) {
                logger.fatal ("exception caught in ManDcTask " + taskIndex + ", hhList range [" + startIndex + "," + endIndex + "] for household id=" + hhList[i].getID() + ".", e );
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
