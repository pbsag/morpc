/*
 * Created on Jun 25, 2004
 * A class for writing FTA summit file for special events
 */
package com.pb.morpc.events;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import java.util.logging.Logger;
import java.util.HashMap;
import com.pb.common.datafile.TableDataSet;
import java.io.File;

/**
 * @author Wu Sun <sunw@pbworld.com>
 *
 */
public class SpecialEventSummit {
	
	private static Logger logger = Logger.getLogger("com.pb.morpc.events");
	private Matrix [] inTripsByTOD=null;
	private Matrix inTrips=null;
	private HashMap events=null;
	private SpecialEventDataReader dataReader;
    private HashMap propertyMap = null;
    private String UECFileName=null;
    private String baseline;
    private String TOD;
    
	public SpecialEventSummit(SpecialEventDataReader dataReader){
		
		logger.info("starting special event summit model.");
		propertyMap=dataReader.getPropertyMap();
		UECFileName=(String)propertyMap.get("eventMCControlFile");
		this.dataReader=dataReader;	
		baseline=(String)propertyMap.get("baseline");
		TOD=(String)propertyMap.get("TOD");
		
		if(baseline.equalsIgnoreCase("true")){
			events=initSpecialEvents();
		}else{
			inTrips=readTripTable();
			setInTrips(inTrips);
		}
	}
	
	/**
	 * Write FTA Summit File
	 */
	public void writeSummitFile(){
		
		logger.info("start writing summit file process.");
				
		//if inTrips hasn't been set, calculate it.  Otherwise, must set it first.
		if(inTrips==null){
			inTripsByTOD=initTripTables();
			inTrips=combineTripTables(inTripsByTOD);	
		}
		
		//if is baseline write out trip tables to hard drive
		if(baseline.equalsIgnoreCase("true"))
			writeTripTable(inTrips);
		
		TableDataSet tazData=dataReader.getTazData();
		int [] tazs=tazData.getColumnAsInt("taz");
		
		//get mcm from special event object
		SpecialEventModeChoiceModel mcm=dataReader.getModeChoiceModel(TOD);
		
		if(mcm==null){
			logger.severe("mode choice model is null for TOD:"+TOD+", when write summit file!");
		}
		
		mcm.setWriteSummitFile(true);
		mcm.setSummitFileName((String)propertyMap.get("summitFileName"));
		mcm.setWriteLogsums(false);
		mcm.setInTrips(inTrips);
		
		logger.info("start ModeChoiceModel runModel()");
		//write summit file
		mcm.runModel(tazs);
		logger.info("finished ModeChoiceModel runModel()");
	}
	
	public void writeTripTable(Matrix tripTable){
		
        logger.info("Writing trip table");
        String tempLoc=null;
        if(propertyMap!=null){
        	tempLoc=(String)propertyMap.get("tripTable.file");
        }else{
        	logger.severe("property map is null.");
        }
        
        File tripTableFile = new File(tempLoc);
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,tripTableFile);
        mw.writeMatrix(tripTable);		
	}
	
    /**
     * Read trip table.
     *
     */
    public Matrix readTripTable(){

        logger.info("Reading trip table");
        
        String tempLoc=null;
        if(propertyMap!=null){
        	tempLoc=(String)propertyMap.get("tripTable.file");
        }else{
        	logger.severe("property map is null.");
        }
        
        File tripTableFile = new File(tempLoc);
        MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP,tripTableFile);
        Matrix tripTable=mr.readMatrix();
        return tripTable;
    }
	
	public void setInTrips(Matrix inTrips){
		this.inTrips=inTrips;
	}
	
	/**
	 * Get input trip table for FTA Summit Program.  This is a table of all events of all mode and TOD.
	 * @return
	 */
	public Matrix getInTrips(){
		if(inTrips==null)
			return combineTripTables(initTripTables());
		else
			return inTrips;
	}
	
	/**
	 * initialize special event for all time of day periods
	 * @return
	 */
	private HashMap initSpecialEvents(){
		HashMap result=new HashMap();
		int noTODs=TODIndex.getNoTODs();
		SpecialEvent event=null;
		
		for(int i=0; i<noTODs; i++){
			event=new SpecialEvent(dataReader, TODIndex.getNameByIndex(i+1));
			result.put(TODIndex.getNameByIndex(i+1),event);
		}
		return result;
	}
	
	/**
	 * Initialize trip tables by TOD.
	 *
	 */
	private Matrix [] initTripTables(){
		Matrix [] result=new Matrix[TODIndex.getNoTODs()];
		String TOD=null;
		for(int i=0; i<result.length; i++){
			TOD=TODIndex.getNameByIndex(i+1);
			//Assume Summit requires an average daily special event trips
			result[i]=((SpecialEvent)(events.get(TOD))).getSETripTable("total");
		}
		return result;
	}
	
	/**
	 * Combine TOD trip tables.
	 * @return combined trip table.
	 */
	private Matrix combineTripTables(Matrix [] inTripsByTOD){
		Matrix result=inTripsByTOD[0];
		for(int i=1; i<inTripsByTOD.length; i++){
			result=result.add(inTripsByTOD[i]);
		}
		return result;
	}
}
