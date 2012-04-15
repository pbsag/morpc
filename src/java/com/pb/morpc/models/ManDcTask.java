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
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;


public class ManDcTask implements Callable<List<Object>> {

	private Logger logger = Logger.getLogger("com.pb.morpc.models");

	private Household[] hhList;
	private DTMHousehold dtmHH;
	private int taskIndex;
	private int startIndex;
	private int endIndex;
	

    public ManDcTask ( DTMHousehold dtmHH, int taskIndex, int startIndex, int endIndex, Household[] hhList ) {
        this.dtmHH = dtmHH;
        this.taskIndex = taskIndex;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.hhList = hhList;
    }


    public List<Object> call() {

        dtmHH.setProcessorIndex( taskIndex );
        for (int i=startIndex; i <= endIndex; i++) {
            try {
                hhList[i].setProcessorIndex (taskIndex);       
                dtmHH.mandatoryTourDc (hhList[i]);
            }
            catch (java.lang.Exception e) {
                logger.fatal ("exception caught in ManDcTask " + taskIndex + ", hhList range [" + startIndex + "," + endIndex + "] for household id=" + hhList[i].getID() + ".", e );
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
