/*
 * Created on Jul 22, 2004
 * This class updates UEC control file.  More specifically, it updates the constant of all alternatives in model sheet.
 * Use this class with care, it is UEC control file specific.  Cell positions of alternative constants are 
 * hard-coded in this class.  Also, in calTransitRidership() it assumes AUTO is the first mode alternative in UEC.
 */
package com.pb.morpc.events;

/**
 * @author Wu Sun
 * <sunw@pbworld.com>
 */

import jxl.*;
import jxl.write.*;
import jxl.write.Number;

import java.io.File;
import org.apache.log4j.Logger;
import java.util.HashMap;
import com.pb.common.util.ResourceUtil;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.TPPToBinarySkim;
import com.pb.common.util.DOSCommandExecutor;

public class SpecialEventCalibrator {
	
    protected static Logger logger = Logger.getLogger("com.pb.morpc.events");
    protected HashMap propertyMap;
	protected String MCControlFile;
	protected String tempControlFile;
	protected double scaler;
    protected int MaxCalibrationNo;
	protected double calibrationThreshhold;
	protected double observedTransitRidership;
	protected String skimFormat;
	protected String projectName;
    
	/**
	 * Constructor
	 * @param skimFormat represents input skim format, "binary", "tranplan" or "tpp"
	 */
	public SpecialEventCalibrator(String skimFormat, String projectName){
		
		propertyMap=ResourceUtil.getResourceBundleAsHashMap(projectName);
	
		this.skimFormat=skimFormat;
		this.projectName=projectName;
		//if input skims are not in binary format, convert to binary
		if(!skimFormat.equalsIgnoreCase("binary")){
			String tppDir=(String)propertyMap.get("SpecialTripsDirectory.binary");
			String binDir=(String)propertyMap.get("SpecialTripsDirectory.tpplus");
			TPPToBinarySkim converter=new TPPToBinarySkim(propertyMap, tppDir, binDir);
			converter.convert();
		}

		//make sure following calibration entries are in the property file
		MCControlFile=(String)propertyMap.get("eventMCControlFile");
		MaxCalibrationNo=(new Integer((String)propertyMap.get("calibration.MAX"))).intValue();
		calibrationThreshhold=new Double((String)propertyMap.get("calibration.threshhold")).doubleValue();
		observedTransitRidership=new Double((String)propertyMap.get("calibration.observedTransitRidership")).doubleValue();

		//a temporary file to keep a copy of UEC control file
		tempControlFile="c:\\temp\\test.xls";
	}

	/**
	 * Start calibration
	 * @param args represents runtime arguments, the first one is event name, the second one is TOD
	 */
	public void startCalibraton(String [] args){
		
		SpecialEventRunner runner;
		Matrix inTrips;
		double calculatedTransitRidership;
		
		//calibration loop
		for(int i=0; i<MaxCalibrationNo; i++){
			
			runner=new SpecialEventRunner(skimFormat, projectName);
			inTrips=null;
			
			if(args.length==2){//if 2 arguments, 1st must be event name, 2nd TOD
				runner.runModel(args[0],args[1]);
			}else{
				logger.error("number of arguments not correct, for calibration run, there must be 2 arguments.");
			}
		
			logger.info("calibration:"+i+" started!");	
			calculatedTransitRidership=calTransitRidership(runner);
			logger.info("calibration:"+i+" ridership calculated!");
			updateUEC(calculatedTransitRidership);
			
	        String command = "copy "+tempControlFile +" "+ MCControlFile;
	        command = command.replace('/', '\\');
	        command=command+" /y";
	        DOSCommandExecutor.runDOSCommand(command);
	        
	        command="del "+tempControlFile;
	        DOSCommandExecutor.runDOSCommand(command);
	        
			if(isConverged(calculatedTransitRidership)){
				logger.info("calibration converged.");
				break;
			}
		}
		logger.info("calibration finished.");	
	}
	
	/**
	 * Update UEC control file on hard drive.
	 *
	 */
	public void updateUEC(double transitRidership){
		
	    Workbook workbook=null;
	    WritableWorkbook copy=null;
	    //first sheet in workbook, indexed as 0
	    WritableSheet sheet=null;
		
		try{
			File file=new File(MCControlFile);
			workbook = Workbook.getWorkbook(file); 
			File tempFile=new File(tempControlFile);
			copy = Workbook.createWorkbook(tempFile,workbook);
		}catch (Throwable t) {
            t.printStackTrace();
        }
		
		//model sheet of UEC control file
		sheet=copy.getSheet(0);
		scaler=-Math.log(transitRidership/observedTransitRidership);
		
		int NoUpdateCells=0;
		if(projectName.equalsIgnoreCase("morpc1")){
			NoUpdateCells=4;
		}else if(projectName.equalsIgnoreCase("MiamiEvent")){
			NoUpdateCells=3;
		}else{
			logger.error("in clibration, project name is invalid.");
		}
		
		//update constant cells of all alternatives
		for(int i=0; i<NoUpdateCells; i++){
			WritableCell cell=null;
			//getWritableCell(i,j), i is col number, j is row number, i and j are 0 based.
			if(projectName.equalsIgnoreCase("morpc1")){
				cell = sheet.getWritableCell(7+i, 13); 
			}else if(projectName.equalsIgnoreCase("MiamiEvent")){
				cell = sheet.getWritableCell(7+i, 11);				
			}else{
				logger.error("in clibration, project name is invalid.");
			}
			
			if (cell.getType() == CellType.NUMBER) 
			{ 
				Number nc = (Number) cell; 
				nc.setValue(nc.getValue()+scaler);
			}else if(cell.getType()==CellType.LABEL){
				Label lc=(Label)cell;
				String temp=(new Double((lc.getContents()).trim()).doubleValue()+scaler)+" ";
				temp.trim();
				lc.setString(temp);
			}else{
				logger.error("cell is not a number.");
			}
		}
		
		//update UEC control file
		try{
			copy.write(); 
			copy.close(); 
		}catch(Throwable t){
			t.printStackTrace();
			logger.fatal("updating UEC control file failed.");
		}
	}
	
	private float calTransitRidership(SpecialEventRunner runner){

		Matrix [] tripsByMode=runner.getSeTripTablesForOneEvent();
		float calculatedTransitRidership=0f;
		
		//index 0 must be auto, others refer to transit modes
		if(projectName.equalsIgnoreCase("morpc1")){
			for(int i=1; i<tripsByMode.length; i++){
				calculatedTransitRidership=calculatedTransitRidership+(float)tripsByMode[i].getSum();
			}
		}else if(projectName.equalsIgnoreCase("MiamiEvent")){
			for(int i=1; i<4; i++){
				calculatedTransitRidership=calculatedTransitRidership+(float)tripsByMode[i].getSum();
			}			
		}else{
			logger.error("in clibration, project name is invalid.");
		}
		return calculatedTransitRidership;
		
	}
	
	/**
	 * Check if calibration converge.  Assume AUTO is the first mode in UEC.
	 * @param transitRidership
	 * @return
	 */
	private boolean isConverged(double transitRidership){
		boolean result=false;

		if((Math.abs(transitRidership-observedTransitRidership)/observedTransitRidership)<=calibrationThreshhold){
			result=true;
		}
		return result;
	}
	
	//for testing purpose only
	public static void main(String [] args){
		
		SpecialEventCalibrator calibrator=new SpecialEventCalibrator("binary", "MiamiEvent");
		calibrator.startCalibraton(args);
			
	}
	
}
