/*
 * Created on Jun 23, 2004
 * 
 *A runner/testing class for special event model 
 */
package com.pb.morpc.events;

/**
 * @author Wu Sun <sunw@pbworld.com>
 *
 */

import org.apache.log4j.Logger;
import java.util.HashMap;
import com.pb.common.util.ResourceUtil;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.TPPToBinarySkim;;

public class SpecialEventRunner {
	
	private static Logger logger = Logger.getLogger("com.pb.morpc.events");
    private HashMap propertyMap =null ;
	//a hashmap of special event trip tables, by mode and TOD
	private HashMap seTripTables=new HashMap();
	private Matrix [] seTripTablesForOneEvent=null;
	private SpecialEventDataReader dataReader=null;

	/**
	 * Constructor
	 * @param skimFormat represent input skim format, "binary","tranplan" or "tpp"
	 */
	public SpecialEventRunner(String skimFormat, String projectName){
		propertyMap=ResourceUtil.getResourceBundleAsHashMap(projectName);
		
		//if input skims are not in binary format, convert to binary
		if(!skimFormat.equalsIgnoreCase("binary")){
			String tppDir=(String)propertyMap.get("SpecialTripsDirectory.binary");
			String binDir=(String)propertyMap.get("SpecialTripsDirectory.tpplus");
			TPPToBinarySkim converter=new TPPToBinarySkim(propertyMap, tppDir, binDir);
			converter.convert();
		}
		dataReader=new SpecialEventDataReader(projectName);
	}
	
	/**
	 * For FTA Summit run.
	 */
	public void runModel(){
		SpecialEventSummit summit=new SpecialEventSummit(dataReader);		
		summit.writeSummitFile();
	}
	
	/**
	 * For single event run.
	 * @param eventName
	 * @param TOD
	 */
	public void runModel(String eventName, String TOD){
		SpecialEvent event=new SpecialEvent(dataReader,TOD);
		//logger.info("--in SpecialEventRunner, before get Trip table----");
		seTripTablesForOneEvent=event.getSETripTablesByNameMode(eventName);
		//logger.info("--in SpecialEventRunner, after get Trip table----");
		for(int i=0; i<seTripTablesForOneEvent.length; i++){
			logger.info("mode "+(i+1)+" sum="+seTripTablesForOneEvent[i].getSum());
		}
	}
	
	/**
	 * For all event run.
	 * @param approach
	 */
	public void runModel(String approach){
		
	    String TOD=null;
	    SpecialEvent event=null;
	    Matrix [][] matrices=new Matrix[TODIndex.getNoTODs()][];
		for(int i=0; i<TODIndex.getNoTODs();i++){
			TOD=TODIndex.getNameByIndex(i+1);
			event=new SpecialEvent(dataReader,TOD);
			seTripTables.put(TOD,event.getSETripTablesByMode(approach));
			matrices[i]=event.getSETripTablesByMode(approach);
		}
	
	}
	
	//must execute runModel(eventName, TOD) first
	public Matrix [] getSeTripTablesForOneEvent(){
		return seTripTablesForOneEvent;
	}
	
	//must execue runModel(approach) first
	public HashMap getSeTripTables(){
		return seTripTables;
	}
	
	//for testing purpose only
	public static void main(String [] args){
			
		SpecialEventRunner runner=new SpecialEventRunner("binary", "MorpcEvent");
		Matrix inTrips=null;
		
		if(args.length==2){//if 2 arguments, 1st must be event name, 2nd TOD
			runner.runModel(args[0],args[1]);
		}else if(args.length==1){//if 1 argument, must be approach (total or average)
			runner.runModel(args[0]);
		}else if(args.length==0){//if no argument, must be for FTA summit
			runner.runModel();
		}else{
			logger.error("number of arguments not correct.");
		}
		
		logger.info("Success!");

	}
}
