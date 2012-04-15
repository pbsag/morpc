package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * worker class for running mandatory destination choice for
 * a list of households in a distributed environment.
 */

import com.pb.morpc.structures.Household;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;


public class ManSfcSlcTask implements Callable<List<Object>> {

	private Logger logger = Logger.getLogger("com.pb.morpc.models");

	private Household[] hhList;
	private StopsHousehold stopsHH;
	private int taskIndex;
	private int startIndex;
	private int endIndex;
	

    public ManSfcSlcTask ( StopsHousehold stopsHH, int taskIndex, int startIndex, int endIndex, Household[] hhList ) {
        this.stopsHH = stopsHH;
        this.taskIndex = taskIndex;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.hhList = hhList;
    }


    public List<Object> call() {

        stopsHH.setProcessorIndex( taskIndex );
        for (int i=startIndex; i <= endIndex; i++) {
            try {
                hhList[i].setProcessorIndex (taskIndex);       
                stopsHH.mandatoryTourSfcSlc (hhList[i]);
            }
            catch (java.lang.Exception e) {
                logger.fatal ("exception caught in ManSfcSlcTask " + taskIndex + ", hhList range [" + startIndex + "," + endIndex + "] for household id=" + hhList[i].getID() + ".", e );
                hhList[i].writeContentToLogger(logger);
                System.exit(-1);
            }
        }

        List<Object> resultBundle = new ArrayList<Object>(3);
        resultBundle.add(taskIndex);
        resultBundle.add(startIndex);
        resultBundle.add(endIndex);

        return resultBundle;
        
	}

}
