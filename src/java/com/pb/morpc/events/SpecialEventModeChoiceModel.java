package com.pb.morpc.events;

/**
 * ModeChoiceModel
 * 
 * Author Joel Freedman on June 8, 2004
 * Modified by Wu Sun on June 21, 2004
 * <sunw@pbworld.com>
 *
 */
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;

import com.pb.common.summit.SummitFileWriter;
import com.pb.common.summit.SummitHeader;
import com.pb.common.summit.ConcreteSummitRecord;

import java.util.ResourceBundle;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.HashMap;

public class SpecialEventModeChoiceModel {

    private static Logger logger = Logger.getLogger("com.pb.morpc.events");
    private ResourceBundle rb;
    private HashMap propertyMap;
    
    private HashSet ptazTraceSet;
    private HashSet atazTraceSet;

    private boolean writeLogsums;
    private boolean writeSummitFile;
    private boolean writeTrips;

    private Matrix logsums;
    private Matrix inTrips;
    private SummitFileWriter summitFile;
    private String summitFileName;

    private UtilityExpressionCalculator uec;
    private LogitModel root;
    private String [] altNames;
    private ConcreteAlternative [] alts;
    private int NoAlts;
    
    private SpecialEventDataReader reader;

    /**
     * constructor 
     *
     * @param rb: A resource bundle containing the properties for this model.
     * @param TOD: time of day, decides which data page to read in UEC
     */
    public SpecialEventModeChoiceModel(ResourceBundle rb, SpecialEventDataReader dataReader, String TOD) {
        this.rb=rb;
        this.reader=dataReader;
        uec =(UtilityExpressionCalculator)(dataReader.getUecMap()).get(TOD);
        initLogitModel();
        setTraceZones();
    }
    
    /**
     * constructor
     *
     * @param propertyMap: A HashMap containing the properties for this model.
     * @param TOD: time of day, decides which data page to read in UEC
     */
    public SpecialEventModeChoiceModel(HashMap propertyMap, SpecialEventDataReader dataReader, String TOD) {
        this.propertyMap=propertyMap;
        this.reader=dataReader;
        uec =(UtilityExpressionCalculator)(dataReader.getUecMap()).get(TOD);
        initLogitModel();
        setTraceZones();
    }
    
    /**
     * Run the mode choice model.  Set writeLogsums, writeSummitFile, writeTrips
     * before running this file.
     *
     * @param externalTazs: A vector of external TAZ numbers to use for iterating through TAZs, 0-init.
     */
    public void runModel(int[] externalTazs) {

        logger.info("Running special event mode choice model");

        //write summit file header
        if(writeSummitFile)
            writeSummitHeader(externalTazs.length,new Float((String)propertyMap.get("events.beta")).floatValue());
        
        //initialize logsums
        if(writeLogsums)
            initializeLogsums(externalTazs);

        //initialize alternatives' availability
        int[] avail = new int[NoAlts+1];
        for(int i=0;i<avail.length;++i){
            avail[i]=1;
        }
        
        //start looping on origins
        for (int pTaz = 0; pTaz < externalTazs.length; ++pTaz) {
        	
            int extPTaz = externalTazs[pTaz];
            //start looping on destinations
            for(int aTaz = 0; aTaz<externalTazs.length;++aTaz){
                int extATaz = externalTazs[aTaz];
                
                double [] util=calUtilities(avail, extPTaz, extATaz);
                        
	            if(extPTaz==3&&extATaz==59){
	            	logger.info("ok, stop here.");
	            }
	            
                root = new LogitModel("Root");
                
                for(int i=0; i<NoAlts; i++){
                	alts[i].setUtility(util[i]);
                	root.addAlternative(alts[i]);
                }
                                
                root.setAvailability(true);
                root.writeAvailabilities();
                
                //get logsum for od pair (aTaz-pTaz)
                float logsum = (float)root.getUtility();
                if(writeLogsums)
                	logsums.setValueAt(extPTaz,extATaz,logsum);
                
                if(writeSummitFile||writeTrips)
                	writeSummitRecord(extPTaz, extATaz);
           } //end ataz
        } //end ptaz
        
        if(writeLogsums)
            writeLogsums();

        logger.info("Model complete");
    }
    
    /**
     * Calculate utilities for alternatives between one OD pair.
     * @param avail represents alternative availability
     * @param extPTaz represents production taz (origin)
     * @param extATaz represents attraction taz (destination)
     */
    public double [] calUtilities(int [] avail, int extPTaz, int extATaz){
        
        IndexValues iv = new IndexValues();
    	iv.setOriginZone(extPTaz);
    	iv.setZoneIndex(extATaz);
        iv.setDestZone(extATaz);
        
        double[] util = uec.solve(iv, null,avail);
    
        return util;
    }
    
    /**
     * Get the writeLogsums setting
     * @return writeLogsums setting
     */
    public boolean isWriteLogsums() {
        return writeLogsums;
    }

    /**
     * Get the writeSummitFile setting
     * @return writeSummitFile setting
     */
    public boolean isWriteSummitFile() {
        return writeSummitFile;
    }

    /**
     * Get the writeTrips setting
     * @return writeTrips setting
     */
    public boolean isWriteTrips() {
        return writeTrips;
    }

    /**
     * Set writeLogsums
     *
     * @param b: True to write logsums when runModel() is executed
     */
    public void setWriteLogsums(boolean b) {
        writeLogsums = b;
    }

    /**
     * Set writeSummitFile.
     *
     * Note: If using this option, must set inTrips matrix and summitFileName
     * or runtime error will result.
     *
     * @param b: True to write SUMMIT input file when runModel() is executed
     */
    public void setWriteSummitFile(boolean b) {
        writeSummitFile = b;
    }
    
    /**
     * Set writeTrips
     *
     * @param b: True to write trip files when runModel() is executed
     */
    public void setWriteTrips(boolean b) {
        writeTrips = b;
    }

    /**
     * Set summitFileName.
     *
     * @param filename Name of the SUMMIT input file.
     */
    public void setSummitFileName(String filename) {
        summitFileName = filename;
    }
 
    /**
     * Set the input trip matrix.
     *
     * @param matrix  A matrix of trips.
     */
    public void setInTrips(Matrix matrix) {
        inTrips = matrix;
    }

    /**
     * Set trace zones
     * (currently hard-coded)
     */
    public void setTraceZones(){
        // put tracing tazs into sets
        ptazTraceSet = new HashSet();
        atazTraceSet = new HashSet();

        ptazTraceSet.add(new Integer(137));
        atazTraceSet.add(new Integer(1084));

    }

    /**
     * Get the logsum matrix.  Call after runModel()
     *
     * @return The logsum matrix
     */
    public Matrix getLogsums() {
        return logsums;
    }
    
    /**
     * Get number of alternatives in a UEC.
     * @return number of alternatives.
     */
    public int getNoAlts(){
    	return NoAlts;
    }
        
    /**
     * Get current logit model alternatives
     * @return alternative array
     */
    public ConcreteAlternative [] getAlternatives(){
    	return alts;
    }
    
    /**
     * Get alternative names.
     * @return
     */
    public String [] getAltNames(){
    	return altNames;
    }


    /**
     * Initialize logit model
     * @param uecFileName represents UEC control file name
     */
    private void initLogitModel(){
       
        logger.info("Initializing logit model");

        root = new LogitModel("Root");
        altNames=uec.getAlternativeNames();
        NoAlts=altNames.length;
        alts=new ConcreteAlternative[NoAlts];
        
        for(int i=0; i<NoAlts; i++){
        	alts[i]=new ConcreteAlternative(altNames[i], new Integer(i+1));
        	root.addAlternative(alts[i]);
        }   	
    }
    
    /**
     * Write the summit file header.
     *
     * @param zones  Number of TAZs in model
     * @param ivt    In-vehicle time parameter
     */
    private void writeSummitHeader(int zones, float ivt){

        //create summit file and write header
        if (summitFileName.equals(null)) {
            throw new RuntimeException("SUMMIT filename not set.");
        }
        summitFile = new SummitFileWriter(summitFileName);
        SummitHeader header = new SummitHeader();
        header.setPurpose(new String("EVENT"));
        header.setTimeOfDay(new String("OFFP"));
        header.setTitle(new String("MORPC Special Event"));
        header.setMarketSegments(1);
        header.setZones(zones);
        header.setAutoInVehicleTime(ivt);
        header.setTransitInVehicleTime(ivt);

        try{
            summitFile.writeHeader(header);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Write a summit record
     * @param extPTaz represents current external PTaz
     * @param extATaz represents current external ATaz
     */
    private void writeSummitRecord(int extPTaz, int extATaz){
    	
    	int NoWalkTransitModes=reader.getNoWalkTransitModes();
    	int NoDriveTransitModes=reader.getNoDriveTransitModes();
    	int NoAutoModes=reader.getNoAutoModes();
    	String [] WalkTransitModes=reader.getWalkTransitModes();
    	String [] DriveTransitModes=reader.getDriveTransitModes();
    	String [] AutoModes=reader.getAutoModes();
    	
    	if(inTrips==null){
    		logger.severe("No inTrip table. Must set inTrip table before write summit records!");
			System.exit(-1);
    	}

    	float trips = inTrips.getValueAt(extPTaz,extATaz);
 //       if(trips!=0){
        	root.calculateProbabilities();
	        double[] probabilities = root.getProbabilities();
	
	        if(writeSummitFile){
	        	ConcreteSummitRecord rec=new ConcreteSummitRecord();
	            rec.setPtaz((short)extPTaz);
	            rec.setAtaz((short)extATaz);
	            rec.setMarket((short)1);
	            rec.setMotorizedTrips(trips);
	            rec.setTrips(trips);
	
	            //set exponentiated auto utility
	            float tempAutoExp=0;
	            for(int i=0; i<NoAutoModes; i++){
	            	if(alts[findAltIndex(AutoModes[i])].getUtility()>-900)
	            		tempAutoExp=tempAutoExp+(float)Math.exp(alts[findAltIndex(AutoModes[i])].getUtility());
	            }
	            rec.setExpAuto(tempAutoExp);
	            
	            //calculate transit share of walk to transit
                double transitShareOfWalkTransit=0f; 
                for(int i=0; i<NoWalkTransitModes; i++){
                	int tempIndex=findAltIndex(WalkTransitModes[i]);
                	transitShareOfWalkTransit=transitShareOfWalkTransit+probabilities[tempIndex];
                }
                
                //calculate transit share of drive to transit
                double transitShareOfDriveTransit=0f;
                for(int i=0; i<NoDriveTransitModes; i++){
                	int tempIndex=findAltIndex(DriveTransitModes[i]);
                	transitShareOfDriveTransit=transitShareOfDriveTransit+probabilities[tempIndex];
                }
	            
                //check if walk to transit is available
	            boolean walkAvailable=false;
	            for(int i=0; i<NoWalkTransitModes; i++){
	            	if(alts[findAltIndex(WalkTransitModes[i])].getUtility()>-900)
	            		walkAvailable=true;
	            }
	            
	            //check if only drive to transit is available
	            boolean driveOnly=false;
	            if(walkAvailable==false){
		            for(int i=0; i<NoDriveTransitModes; i++){
		            	if(alts[findAltIndex(DriveTransitModes[i])].getUtility()>-900)
		            		driveOnly=true;
		            }
	            }
	            
	            //set walk to transit available share
	            if(walkAvailable){
	                rec.setWalkTransitAvailableShare((float)1.0);
	            }else{
	                rec.setWalkTransitAvailableShare((float)0.0);
	            }
	     
	            //set drive to transit only available share
	            if(driveOnly){
	                rec.setDriveTransitOnlyShare((float)1.0);
	            }else{
	                rec.setDriveTransitOnlyShare((float)0.0);
	            }
	            	            
	            //set transit share when walk to transit available
	            //set transit share when drive to transit only available
	            if(driveOnly){
	                rec.setTransitShareOfWalkTransit(0f);
	                rec.setTransitShareOfDriveTransitOnly((float)transitShareOfDriveTransit);	            	
	            }else if(walkAvailable){
	            	rec.setTransitShareOfWalkTransit((float)(transitShareOfWalkTransit+transitShareOfDriveTransit));
	            	rec.setTransitShareOfDriveTransitOnly(0f);
	            }else{
	            	rec.setTransitShareOfWalkTransit(0f);
	            	rec.setTransitShareOfDriveTransitOnly(0f);
	            }
	            	
	            try{
	                summitFile.writeRecord(rec);
	            } catch (IOException e) {
	                e.printStackTrace();
	                System.exit(1);
	            }
	        }
//	    }
    }
    
    /**
     * Initialize the logsum matrix.
     *
     * @param externalTazs: A sequential array of zone numbers, 0-init
     */
    private void initializeLogsums(int[] externalTazs){

        logger.info("Initializing logsum matrix");
        logsums = new Matrix(externalTazs.length,externalTazs.length);
        logsums.setExternalNumbersZeroBased(externalTazs);
    }

    /**
     * Write the logsum matrix.
     *
     */
    private void writeLogsums(){

        logger.info("Writing logsum matrix");
        
        String tempLoc=null;
        if(propertyMap==null){
        	tempLoc=rb.getString("logsum.file");
        }
        if(rb==null){
        	tempLoc=(String)propertyMap.get("logsum.file");
        }
        
        File logsumFile = new File(tempLoc);
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,logsumFile);
        mw.writeMatrix(logsums);
    }
    
    /**
     * Given an alternative name, find its index in UEC.  If not found, return -1.
     * @param altName represetns a given alternative name
     * @return an index
     */
    private int findAltIndex(String altName){
    	int result=-1;
    	for(int i=0; i<NoAlts; i++){
    		if(altNames[i].equalsIgnoreCase(altName)){
    			result=i;
    			break;
    		}
    	}
    	return result;
    }
}
