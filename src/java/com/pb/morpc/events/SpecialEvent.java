/*
 * Created on Jun 22, 2004
 * 
 * Special Event Model Class
 *
 */
package com.pb.morpc.events;

import java.util.logging.Logger;
import java.util.HashMap;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.model.EventModel;

/**
 * @author Wu Sun <sunw@pbworld.com>
 *
 */
public class SpecialEvent {
	
	private HashMap propertyMap=null;	
	private static Logger logger = Logger.getLogger("com.pb.morpc.events");
	private SpecialEventDataReader dataReader=null;

	private SpecialEventModeChoiceModel mcm;
	private int NoAlts=-1;
	private String [] altNames=null;
	private Matrix tripTable=null;
	
	private TableDataSet tazData=null;
	private TableDataSet eventData=null;
	private double lambda;
	private double theta;
	private String TOD;
	private String eventName=null;
		
	/**
	 * Constructor
	 * @param propertyMap represents a property map
	 * @param dataReader represents a sepcial event data reader
	 */
	public SpecialEvent(SpecialEventDataReader dataReader, String TOD){
		
		if(dataReader==null)
			dataReader=new SpecialEventDataReader("morpc1");
		
        this.propertyMap = dataReader.getPropertyMap();
		this.dataReader=dataReader;	
		this.TOD=TOD;
		
		tazData=dataReader.getTazData();
		eventData=dataReader.getEventData();
		lambda=dataReader.getLambda();
		theta=dataReader.getTheta();
		
		//create a mode choice model
		mcm=(SpecialEventModeChoiceModel)(dataReader.getModeChoiceModelMap()).get(TOD);
		NoAlts=mcm.getNoAlts();
		altNames=mcm.getAltNames();
	}
	
	
	/**
	 * Get special event trip tables by event name, all modes combined.  TOD is fixed for a specific event.
	 * @param eventName represents an event name
	 * @return
	 */
	public Matrix getSETripTablesByName(String eventName){
		return calSETripTableByName(eventName);
	}
	
	/**
	 * Get special event trip tables by event name and mode. TOD is fixed for a specific event.
	 * @param eventName represents an event name
	 * @return
	 */
	public Matrix [] getSETripTablesByNameMode(String eventName){
		return calSETripTablesByNameMode(eventName);
	}
	
	/**
	 * Get special event trip tables by mode for TOD defined in constructor of this SpecialEvent object.
	 * @param approach represents model approach, "total" or "average"
	 * @return
	 */
	public Matrix [] getSETripTablesByMode(String approach){
		return calSETripTablesByMode(approach);
	}
	
	/**
	 * Get special event trip table (all modes combined) for TOD defined in constructor of this SpecialEvent object.
	 * @param approach, "average" or "total", for FTA must be "total"
	 * @return
	 */
	public Matrix getSETripTable(String approach){
		return calSETripTable(approach);
	}
	
	public SpecialEventDataReader getDataReader(){
		return dataReader;
	}
		
	/**
	 * calculate trip tables by mode for one event
	 * @return
	 */
	private Matrix [] calSETripTablesByNameMode(String eventName){
		
		logger.info("-------in SpecialEvent.calSETripTablesByNameMode-----");
		//trip generation by event name
		Matrix tripTable=calSETripTableByName(eventName);
		
		logger.info("trip table sum="+(float)tripTable.getSum());
		
		//mode choice
		return divTripTableByMode(tripTable);	
	}
		
	/**
	 * calculate trip tables by mode for all events
	 * @param approach represents model appraoch, "total" or "average"
	 * @return
	 */
	private Matrix [] calSETripTablesByMode(String approach){
		Matrix [] result=new Matrix[NoAlts];
		
		//trip generation
		Matrix tripTable=calSETripTable(approach);
		
		//mode choice
		result=divTripTableByMode(tripTable);
		
		return result;
	}
	
	/**
	 * Mode choice
	 * Index of returned matrix array same as that of alternatives in UEC
	 * @param tripTable represents a trip table
	 * @return
	 */
	private Matrix [] divTripTableByMode(Matrix tripTable){
		
		Matrix [] result=new Matrix[NoAlts];

		//get overall trip distributions
		int [] extTaz=tazData.getColumnAsInt("taz");		
		int NoRows=tripTable.getRowCount();
		int NoCols=tripTable.getColumnCount();
		
		//initialize trip distributions by mode matices
		for(int i=0; i<NoAlts; i++){
			result[i]=new Matrix(altNames[i],tripTable.getDescription(), NoRows, NoCols);
			result[i].setExternalNumbersZeroBased(extTaz);
		}
		
		//temp to hold trips between current od
		float tripTemp=0f;
		//temp to hold mode probabilities of current od
		double [] probabilityTemp=null;
		//temp to hold current external row number
		int extRow=-1;
		//temp to hold current external col number
		int extCol=-1;
		
		//loop over internal rows
		for(int i=0; i<NoRows; i++){
			
			extRow=extTaz[i];
			
			//loop over internal columns
			for(int j=0; j<NoCols; j++){
				
				extCol=extTaz[j];
				//get trip between current od
				tripTemp=tripTable.getValueAt(i,j);
				
				//get mode probabilities of current od
				probabilityTemp=calMCProbabilities(extRow, extCol);
			
				//temp to hold current cell value in a trip table by mode
				float cellValue=0f;
				
				//if calculated probabilities valid, calculate current cell value, and set it to corresponding trip table
				if(isProbabilitiesValid(probabilityTemp)){
					for(int k=0; k<NoAlts; k++){
						cellValue=(float)probabilityTemp[k]*tripTemp;
						result[k].setValueAt(i,j,cellValue);
					}
				}else{
					logger.severe("probabilities calculated are not valid.");
					System.exit(-1);
				}				
			}
		}
		
		return result;
	}
		
	/**
	 * Check if the dimension of calculated probability array is equal to number of alternatives in UEC.
	 * @param probabilities represent calculated probabilities
	 * @return
	 */
	private boolean isProbabilitiesValid(double [] probabilities){
		if(probabilities.length==NoAlts)
			return true;
		else
			return false;
	}
	
	/**
	 * Calculate mode choice logsums
	 * @return logsums of all OD pairs as a matrix
	 */
	private Matrix calLogsums(){
		Matrix result=null;
		
		int [] tazs=tazData.getColumnAsInt(tazData.getColumnPosition("taz"));
		
		//must set to true, otherwise logsums won't get instantiated
		mcm.setWriteLogsums(true);
		mcm.setWriteSummitFile(false);
		mcm.setWriteTrips(false);		
		mcm.runModel(tazs);
		
		result=mcm.getLogsums();
		return result;
	}
	
	/**
	 * Calculate trip table for all events
	 * @param approach represents model approach: "total" or "average"
	 * @return matrix with description: 0-AM, 1-MD, 2-PM, 3-NT
	 */
	private Matrix calSETripTable(String approach){
		
		String matrixName="total";
		EventModel em=null;	
		Matrix impedance=null;
		String logsumsExist=(String)propertyMap.get("logsumsExist");
		
		//get impedance
		impedance=calLogsums();
		
		Matrix result=new Matrix(impedance.getRowCount(),impedance.getColumnCount());
		
		//initialize result, matrix index starts from 1
		for(int i=0; i<impedance.getRowCount();i++){
			for(int j=0; j<impedance.getColumnCount();j++)
				result.setValueAt(i+1,j+1,0.0f);
		}
	
		float [] size=calSizeAttractiveness();
		ColumnVector sizeVector=new ColumnVector(size);
		sizeVector.setExternalNumbersZeroBased(tazData.getColumnAsInt("taz"));
		
		int NoEvents=eventData.getRowCount();	
		String name=null;
		int taz;
		int attendance;
		String tempTOD;
		int noDays;
		
		//get event name column position in taz table
		int namePos=eventData.getColumnPosition("name");
		if(namePos==-1){
			logger.severe("name column not in event data file");
		}
		
		//get taz column position in taz table
		int tazPos=eventData.getColumnPosition("taz");
		if(tazPos==-1){
			logger.severe("taz column not in event data file");
		}
		
		//get attendance column position in taz table
		int attendancePos=eventData.getColumnPosition("attendance");
		if(attendancePos==-1){
			logger.severe("attendance column not in event data file");
		}
		
		//get TOD column position in event table
		int TODPos=eventData.getColumnPosition("TOD");
		if(TODPos==-1){
			logger.severe("TOD column not in event data file");
		}
		
		//get TOD column position in event table
		int NoDaysPos=eventData.getColumnPosition("NoDays");
		if(NoDaysPos==-1){
			logger.severe("NoDays column not in event data file");
		}
		
		//loop over events to distribute special event trips
		//formula: P=(A**theta)e**(lambda*cu), where cu=impedence, A=size
		
		int NoEventDays=0;		//total number of days have special events
		
		for(int i=0; i<NoEvents; i++){
			
			name=eventData.getStringValueAt(i+1,namePos);
			taz=(int)eventData.getValueAt(i+1,tazPos);
			attendance=(int)eventData.getValueAt(i+1,attendancePos);
			tempTOD=(String)eventData.getStringValueAt(i+1,TODPos);
			noDays=(new Integer(eventData.getStringValueAt(i+1,NoDaysPos))).intValue();
			
			//process events with matching TOD
			if(tempTOD.equalsIgnoreCase(TOD)){
				logger.info("Starting Event Model for "+name);
				em=new EventModel(name, taz, lambda, theta);
				em.setImpedance(impedance);
				em.setSize(sizeVector);
				
				//if it is a event of multiple days, loop over all event days
				for(int j=0; j<noDays; j++){
					em.calculateTrips(attendance);
			
					if(result==null){
						result=em.getTrips();
					}else{
						result=result.add(em.getTrips());
					}
					
					NoEventDays++;
				}
			}
		}
		
		if(approach.equalsIgnoreCase("average")){
			int NoRows=result.getRowCount();
			int NoCols=result.getColumnCount();
			for(int j=0; j<NoRows; j++){
				for(int k=0; k<NoCols; k++){
					result.setValueAt(j,k,(result.getValueAt(j,k))/(float)NoEventDays);
				}
			}
			matrixName="average";
		}
		
		//set event name as matrix name
		result.setName(matrixName);
		//set TOD in matrix description
		result.setDescription(TOD);
		//set external zone numbers to trip matix
		result.setExternalNumbersZeroBased(tazData.getColumnAsInt("taz"));	
						
		return result;
	}
	
	/**
	 * Calculate trip table for one event
	 * @return trip table matrix, matrix name=event name, matrix description=TOD 
	 */
	private Matrix calSETripTableByName(String eventName){
				
		Matrix result=null;
		EventModel em=null;	
		
		//get impedance
		Matrix impedance=calLogsums();
		
		logger.info("---------in SpecialEvent.calSETripTableByName, before calculate size--------");
	
		//calculate attractiveness associated with size variables
		float [] size=calSizeAttractiveness();
		logger.info("---------in SpecialEvent.calSETripTableByName, after calculate size--------");
		ColumnVector sizeVector=new ColumnVector(size);
		sizeVector.setExternalNumbersZeroBased(tazData.getColumnAsInt("taz"));
		
		int NoEvents=eventData.getRowCount();	
		String name=null;
		int taz;
		int attendance;
		String TOD=null;
		int noDays;
		
		//get event name column position in event table
		int namePos=eventData.getColumnPosition("name");
		if(namePos==-1){
			logger.severe("name column not in event data file");
		}
		
		//get taz column position in event table
		int tazPos=eventData.getColumnPosition("taz");
		if(tazPos==-1){
			logger.severe("taz column not in event data file");
		}
		
		//get attendance column position in event table
		int attendancePos=eventData.getColumnPosition("attendance");
		if(attendancePos==-1){
			logger.severe("attendance column not in event data file");
		}
		
		//get TOD column position in event table
		int TODPos=eventData.getColumnPosition("TOD");
		if(TODPos==-1){
			logger.severe("TOD column not in event data file");
		}
		
		//get No of Days column position in event table
		int NoDaysPos=eventData.getColumnPosition("NoDays");
		if(NoDaysPos==-1){
			logger.severe("NoDays column not in event data file");
		}
		
		//loop over events to distribute special event trips
		//formula: P=(A**theta)e**(lambda*cu), where cu=impedence, A=size
		for(int i=0; i<NoEvents; i++){
			
			name=eventData.getStringValueAt(i+1,namePos);
			taz=(int)eventData.getValueAt(i+1,tazPos);
			attendance=(int)eventData.getValueAt(i+1,attendancePos);
			TOD=eventData.getStringValueAt(i+1,TODPos);
			
			//noDays doesn't matter here for one single event case, because for one single event case,
			//because trip tables are daily based.
			noDays=(new Integer(eventData.getStringValueAt(i+1,NoDaysPos))).intValue();
			
			if(name.equalsIgnoreCase(eventName)){
				logger.info("Starting Event Model for "+name);
				em=new EventModel(name, taz, lambda, theta);
				em.setImpedance(impedance);
				em.setSize(sizeVector);
				em.calculateTrips(attendance);
				result=em.getTrips();
				break;
			}		
		}
		
		//set event name as matrix name
		result.setName(eventName);
		//set TOD in matrix description
		result.setDescription(TOD);
		//set external zone numbers to trip matix
		result.setExternalNumbersZeroBased(tazData.getColumnAsInt("taz"));	
		
		return result;
	}
		
	/**
	 * Calculate mode choice probabilites between a production and an attraction zone.
	 * @param ptaz represents a production zone
	 * @param ataz represents an attraction zone
	 * @return probabilites of mode choices
	 */
	private double [] calMCProbabilities(int ptaz, int ataz){
		
		//create a new logit model
		LogitModel root=new LogitModel("Root");
		
		//get alternatives from mode choice model
		int NoAlts=mcm.getNoAlts();
		ConcreteAlternative [] alts=mcm.getAlternatives();
		
		//set alterntive availability to available
		int [] avail=new int[NoAlts+1];
		for(int i=0; i<avail.length; i++){
			avail[i]=1;
		}
		
		//calculate mode choice utilities
		double [] util=mcm.calUtilities(avail, ptaz, ataz);

		//add alternatives to logit model
		for(int i=0; i<NoAlts; i++){
			alts[i].setUtility(util[i]);
			root.addAlternative(alts[i]);
		}
		
		root.setAvailability(true);
		root.writeAvailabilities();
		double logsum=root.getUtility();
		
		//calculate and return mode choice probabilities
		root.calculateProbabilities();
		return root.getProbabilities();
	}
	
	/**
	 * Calcualte attractiveness associated with size variables
	 * @return
	 */
	private float [] calSizeAttractiveness(){
		
		int noRows=tazData.getRowCount();
		float [] result=new float[noRows];
		float [] poppr=new float[noRows];
		float [] emppr=new float[noRows];
		float [] hipr=new float[noRows];
		
		float totpopsum=tazData.getColumnTotal(tazData.getColumnPosition("totpop"));
		float [] totpop=tazData.getColumnAsFloat("totpop");
		float empoffsum=tazData.getColumnTotal(tazData.getColumnPosition("empoff"));
		float [] empoff=tazData.getColumnAsFloat("empoff");
		float empretgdssum=tazData.getColumnTotal(tazData.getColumnPosition("empretgds"));
		float [] empretgds=tazData.getColumnAsFloat("empretgds");
		float empretsrvsum=tazData.getColumnTotal(tazData.getColumnPosition("empretsrv"));
		float [] empretsrv=tazData.getColumnAsFloat("empretsrv");
		float [] hhinc=tazData.getColumnAsFloat("hhinc");

		for(int i=0; i<noRows; i++){
			poppr[i]=totpop[i]/totpopsum;
			emppr[i]=(empoff[i]+empretgds[i]+empretsrv[i])/(empoffsum+empretgdssum+empretsrvsum);
			hipr[i]=hhinc[i]/110000f;
			result[i]=(float)(0.4456*poppr[i]+0.1265*emppr[i]+0.004635*hipr[i]);
		}
		
		return result;
	}
}
