/*
 * Created on Jun 22, 2004
 * 
 * Special event data reader class.
 *
 */

package com.pb.morpc.events;

import java.util.logging.Logger;
import java.io.File;
import java.io.IOException;
import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import java.util.HashMap;
import com.pb.common.calculator.UtilityExpressionCalculator;

/**
 * @author Wu Sun <sunw@pbworld.com>
 * 
 */
public class SpecialEventDataReader {
	
	private static Logger logger = Logger.getLogger("com.pb.morpc.events");
	private HashMap propertyMap=null;
	private CSVFileReader reader=null;
	private TableDataSet tazData=null;
	private TableDataSet parkingData=null;
	private TableDataSet eventData=null;
	private String tazDataFile=null;
	private String parkingDataFile=null;
	private String eventDataFile=null;
	private double lambda;
	private double theta;
	private String MCControlFile=null;
	private HashMap uecMap=null;
	private HashMap modeChoiceModelMap=null;
	private int NoWalkTransitModes;
	private int NoDriveTransitModes;
	private int NoAutoModes;
	private String [] WalkTransitModes=null;
	private String [] DriveTransitModes=null;
	private String [] AutoModes=null;
	private String MultiTOD=null;
	
	public SpecialEventDataReader(String projectName){
		
		//get data files from morpc property file
        propertyMap = ResourceUtil.getResourceBundleAsHashMap(projectName);
		tazDataFile=(String)propertyMap.get("tazDataFile");
		parkingDataFile=(String)propertyMap.get("parkingDataFile");
		eventDataFile=(String)propertyMap.get("eventDataFile");
		MCControlFile=(String)propertyMap.get("eventMCControlFile");
		
		reader=new CSVFileReader();
		
		//read taz, parking, and event data as TableDataSet objects
		try{
			tazData=reader.readFile(new File(tazDataFile),true);//what is true?
		}catch(IOException e){
			logger.severe("can not open taz data file:"+tazDataFile);
		}
		
		try{
			parkingData=reader.readFile(new File(parkingDataFile),true);//what is true?
		}catch(IOException e){
			logger.severe("can not open parking data file:"+parkingDataFile);
		}
		
		try{
			eventData=reader.readFile(new File(eventDataFile),true);//what is true?
		}catch(IOException e){
			logger.severe("can not open event data file:"+eventDataFile);
		}
		
		//read lambada and theta from morpc property file
		lambda=new Float((String)propertyMap.get("events.lambda")).floatValue();
		theta=new Float((String)propertyMap.get("events.theta")).floatValue();
		
		NoWalkTransitModes=new Integer((String)propertyMap.get("NoWalkTransitModes")).intValue();
		WalkTransitModes=new String[NoWalkTransitModes];
		for(int i=0; i<NoWalkTransitModes; i++){
			WalkTransitModes[i]=(String)propertyMap.get("mode.transit.walk"+(i+1));
		}
		
		NoDriveTransitModes=new Integer((String)propertyMap.get("NoDriveTransitModes")).intValue();
		DriveTransitModes=new String[NoDriveTransitModes];
		for(int i=0; i<NoDriveTransitModes; i++){
			DriveTransitModes[i]=(String)propertyMap.get("mode.transit.drive"+(i+1));
		}
		
		NoAutoModes=new Integer((String)propertyMap.get("NoAutoModes")).intValue();
		AutoModes=new String[NoAutoModes];
		for(int i=0; i<NoAutoModes; i++){
			AutoModes[i]=(String)propertyMap.get("mode.auto"+(i+1));
		}
		
		MultiTOD=(String)propertyMap.get("MultiTOD");
		
		uecMap=populateUecMap();
		modeChoiceModelMap=populateModeChoiceModelMap();
	}
	
	public HashMap getPropertyMap(){
		return propertyMap;
	}
	
	public TableDataSet getTazData(){
		return tazData;
	}
	
	public TableDataSet getParkingData(){
		return parkingData;
	}
	
	public TableDataSet getEventData(){
		return eventData;
	}
	
	public double getLambda(){
		return lambda;
	}
	
	public double getTheta(){
		return theta;
	}
	
	public String [] getWalkTransitModes(){
		return WalkTransitModes;
	}
	
	public String [] getDriveTransitModes(){
		return DriveTransitModes;
	}
	
	public String [] getAutoModes(){
		return AutoModes;
	}
	
	public String getMultiTOD(){
		return MultiTOD;
	}
	
	public int getNoWalkTransitModes(){
		return NoWalkTransitModes;
	}
	
	public int getNoDriveTransitModes(){
		return NoDriveTransitModes;
	}
	
	public int getNoAutoModes(){
		return NoAutoModes;
	}
	
	public String getMCControlFile(){
		return MCControlFile;
	}
	
	public HashMap getUecMap(){
		return uecMap;
	}
	
	public UtilityExpressionCalculator getUec(String TOD){
		UtilityExpressionCalculator result;
		result=(UtilityExpressionCalculator)uecMap.get(TOD);
		return result;
	}
	
	public HashMap getModeChoiceModelMap(){
		return modeChoiceModelMap;
	}
	
	public SpecialEventModeChoiceModel getModeChoiceModel(String TOD){
		SpecialEventModeChoiceModel result;
		result=(SpecialEventModeChoiceModel)modeChoiceModelMap.get(TOD);
		return result;
	}
	
	private HashMap populateUecMap(){
		HashMap result=new HashMap();
		int NoTOD=TODIndex.getNoTODs();
		for(int i=0; i<NoTOD; i++){
			logger.info("Reading UEC using file:"+MCControlFile+" and data sheet:"+(i+1));
			UtilityExpressionCalculator.clearData();
			UtilityExpressionCalculator uec =new UtilityExpressionCalculator(new File(MCControlFile), 0, (i+1), propertyMap,null);
			result.put(TODIndex.getNameByIndex(i+1),uec);
		}
        return result;
	}
	
	private HashMap populateModeChoiceModelMap(){
		HashMap result=new HashMap();
		int NoTOD=TODIndex.getNoTODs();
		for(int i=0; i<NoTOD; i++){
			logger.info("Creating mode choice models for:"+TODIndex.getNameByIndex(i+1));
			SpecialEventModeChoiceModel mcm=new SpecialEventModeChoiceModel(propertyMap,this,TODIndex.getNameByIndex(i+1));
			result.put(TODIndex.getNameByIndex(i+1),mcm);
		}
		return result;
	}
}
