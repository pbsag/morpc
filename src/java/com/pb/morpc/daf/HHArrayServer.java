package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * server for Household array manager
 */
import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.morpc.models.HouseholdArrayManager;
import com.pb.morpc.structures.Household;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

//Wu added for Summit Aggregation
import com.pb.morpc.structures.SummitAggregationRecord;
//import java.text.DecimalFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;


public class HHArrayServer extends MessageProcessingTask {


	private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
	
	private boolean hhServerStarted = false;

	private Message startWorkMessage = null; 	

	private ArrayList hhServerQueue = new ArrayList();

	private HouseholdArrayManager hhMgr = null;
	
	//Wu added to send propertyMap to workers
	private HashMap propertyMap=null;
	

	private boolean FtaRestartRun = false;
    
	
    
    public HHArrayServer () {
    }



	public void onStart () {

		if (LOGGING)
		    logger.info("HhArrayServer onStart().");

		startWorkMessage = createMessage();
		startWorkMessage.setId(MessageID.START_INFO);

		MorpcModelServer.showMemory ();
	}




	public void onMessage(Message msg) {

		if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );
		
		//The hh array server gets a START_INFO message from the main server
		//when it's ready for the hh DiskObjectArray to be built.
		if ( msg.getSender().equals("MorpcServer") ) {

			if (msg.getId().equals(MessageID.START_INFO)) {
				
			    propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
			    if(propertyMap==null){
			    	logger.fatal("HHArrayServer onMessage, no propertyMap included in this message.");
			    }

				hhMgr = new HouseholdArrayManager( propertyMap );

				
		        // determine whether this model run is for an FTA Summit analysis or not
		        if( (String)propertyMap.get("FTA_Restart_run") != null )
		        	FtaRestartRun = ((String)propertyMap.get("FTA_Restart_run")).equalsIgnoreCase("true");
		        else
		        	FtaRestartRun = false;

		        
				
				// if the FTA_Restart_run property is true, start the model iteration with HH Array from a DiskObjectArray,
				// otherwise create a new HH Array.
				if( FtaRestartRun ) {
					hhMgr.createBigHHArrayFromDiskObject();
				} 
				// otherwise, create an array of HHs from the household table data stored on disk
				else {
					hhMgr.createBigHHArray ();
				}
				
				
				hhMgr.createHHArrayToProcess (); 

				
				if (LOGGING)
				    logger.info("HhArrayServer finished building HH Array.");
				Message doneMsg = createMessage();
				doneMsg.setId( MessageID.HH_ARRAY_FINISHED );
				sendTo("MorpcServer", doneMsg);
				
				hhServerStarted = true;
				
			}
			else if ( msg.getId().equals( MessageID.RESET_HHS_PROCESSED )	) {		

				hhMgr.resetHhsProcessed();
			
			}
			else if ( msg.getId().equals( MessageID.SEND_HHS_ARRAY )	) {		

				Message sendMsg = createMessage();
				sendMsg.setId( MessageID.HHS_ARRAY_KEY );
				sendMsg.setValue( MessageID.HHS_ARRAY_KEY, hhMgr.getHouseholds() );
				replyToSender(sendMsg);
			
			}
			else if ( msg.getId().equals( MessageID.UPDATE_HHS_ARRAY )	) {		

				hhMgr.sendResults( (Household[])msg.getValue( MessageID.UPDATED_HHS ) );

				// reply with a HHS_ARRAY_KEY when hh array has been updated
				Message sendMsg = createMessage();
				sendMsg.setId( MessageID.HHS_ARRAY_KEY );
				replyToSender(sendMsg);
				
			}
			else if ( msg.getId().equals( MessageID.CLEAR_HHS_ARRAY )	) {		

			    hhMgr = null;
			    
				Message sendMsg = createMessage();
				sendMsg.setId( MessageID.FINISHED );
				replyToSender(sendMsg);
			
			}
			
			//handle any messages from fpWorkers queued up while waiting for server to start
			for ( Iterator i = hhServerQueue.iterator(); i.hasNext(); ) {
				onMessage ( (Message)i.next() );
				i.remove();
			}
				
		}
		//The hh array server gets a SEND_START_INFO message from the fpWorkers
		//saying they're ready to begin work.
		else {			
			if ( hhServerStarted ) {
		        
				if ( msg.getId().equals( MessageID.SEND_WORK ) ) {		

					// retrieve the contents of the message.
					short category = (short)msg.getIntValue( MessageID.TOUR_CATEGORY_KEY );
					short[] types = (short[])msg.getValue( MessageID.TOUR_TYPES_KEY );	


					sendWork ( category, types );
			
				}
				else if ( msg.getId().equals( MessageID.RESET_HHS_PROCESSED )	) {		

					hhMgr.resetHhsProcessed();
			
				}
				else if ( msg.getId().equals( MessageID.RESULTS ) ) {		

					// retrieve the contents of the message.
					short category = (short)msg.getIntValue( MessageID.TOUR_CATEGORY_KEY );
					short[] types = (short[])msg.getValue( MessageID.TOUR_TYPES_KEY );	
									
					hhMgr.sendResults ( (Household[])msg.getValue( MessageID.HOUSEHOLD_LIST_KEY ) );

					sendWork ( category, types );
				}
				/*
				Wu added for Summit Aggregation.
				next if block get SUMMIT_AGGREGATION message from
				TcMcWorker, IndivDTMWorker, JointDTMWorker, and AtWorkDTMWorker
				*/
				else if(msg.getId().equals(MessageID.SUMMIT_AGGREGATION)){
					//write Summit aggregation records to file on disk
					writeRecords((SummitAggregationRecord[])msg.getValue(MessageID.SUMMIT_LIST_KEY));
					//still need to do following so that HHArrayServer continue sending houselholds
					short category = (short)msg.getIntValue( MessageID.TOUR_CATEGORY_KEY );
					short[] types = (short[])msg.getValue( MessageID.TOUR_TYPES_KEY );	
					hhMgr.sendResults ( (Household[])msg.getValue( MessageID.HOUSEHOLD_LIST_KEY ) );
					sendWork ( category, types );
				}
				
			}
			else {
				// queue the messages until FpModelServer is told to start
				hhServerQueue.add( msg );
				
			}
		}

		if (LOGGING)
		    logger.info( "end of onMessage() for " + this.name +  " , sent by " + msg.getSender() + "." );
		MorpcModelServer.showMemory ();

	}

	
	
	private void sendWork ( short tourCategory, short[] tourTypes ) {

		// generate a message with the next list of households to send to the worker
		Message newMessage = createMessage();
		newMessage.setId(MessageID.HOUSEHOLD_LIST);

		// send a message back to the sender with an array of Households to process.
		// if the Household array sent to the worker is null, then the worker shuts down.
		newMessage.setIntValue( MessageID.TOUR_CATEGORY_KEY, tourCategory );
		newMessage.setValue( MessageID.TOUR_TYPES_KEY, tourTypes );	
		newMessage.setValue( MessageID.HOUSEHOLD_LIST_KEY, hhMgr.getHouseholdsForWorker() );
		
		//Wu added, send propertyMap to workers
		newMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap);

		//Send this message back to worker
		replyToSender(newMessage);
		
	}
	
	/*
	 * Wu added for Summit Aggregation
	 * Write out records (including utilities and probabilities) as CSV file
	 */
	private void writeRecords(SummitAggregationRecord [] records){
		logger.info("num of summit aggregation records="+records.length);	
        String fileName=(String)propertyMap.get("summitAggregationFile");
        String [] ColTitles={"hh_id","person_id","tour_id","purpose","tourCategory","prob1","prob2","prob3","prob4","prob5","prob6","expUtil1","expUtil2","expUtil3","expUtil4","expUtil5","expUtil6"};
        File file=null;
                        
        try {
            file=new File(fileName);    
            PrintWriter outStream = new PrintWriter (new BufferedWriter( new FileWriter(file, true) ) );
            
            //if summit file doesn't exit, first print column titles
            if(!file.exists()){     	
    	        for(int i=0; i<ColTitles.length; i++){
    	        	outStream.print(ColTitles[i]);
    	        	if(i!=ColTitles.length-1)
    	        		outStream.print(",");
    	        }
            }
            
            for(int i=0; i<records.length; i++){
    	        double [] probs=records[i].getProbs();
    	        double [] expUtils=records[i].getExpUtils();
    	        int NoAlts=probs.length;
    	                  
    	        outStream.print(records[i].getHouseholdID());
    	        outStream.print(",");
    	        outStream.print(records[i].getPersonID());
    	        outStream.print(",");
    	        outStream.print(records[i].getTourID());
    	        outStream.print(",");
    	        outStream.print(records[i].getPurpose());
    	        outStream.print(",");
    	        outStream.print(records[i].getTourCategory());
    	        outStream.print(",");
    	        outStream.print(records[i].getPartySize());
    	        outStream.print(",");
    	        outStream.print(records[i].getHHIncome());
    	        outStream.print(",");
    	        outStream.print(records[i].getAuto());
    	        outStream.print(",");
    	        outStream.print(records[i].getWorkers());
    	        outStream.print(",");
    	        outStream.print(records[i].getOrigin());
    	        outStream.print(",");
    	        outStream.print(records[i].getDestination());
    	        outStream.print(",");
    	        outStream.print(records[i].getOrigSubZone());
    	        outStream.print(",");
    	        outStream.print(records[i].getDestSubZone());
    	        outStream.print(",");
    	        outStream.print(records[i].getOutTOD());
    	        outStream.print(",");
    	        outStream.print(records[i].getInTOD());
    	        outStream.print(",");
    	        outStream.print(records[i].getMode());
    	        outStream.print(",");
    	        
    	        //logger.info("num of alts="+NoAlts);
    	            
    	        for(int j=0; j<NoAlts; j++){
    	        	//logger.info("prob"+j+"="+probs[j]);
    	        	outStream.print(probs[j]);
    	            outStream.print(",");
    	        }
    	            
    	        for(int j=0; j<NoAlts; j++){
    	        	//logger.info("exp"+j+"="+expUtils[j]);
    	            outStream.print(expUtils[j]);
    	            if(j!=NoAlts-1)
    	            	outStream.print(",");
    	        }
    	        outStream.println();
            }
            outStream.close();
        }catch(IOException e){
        	logger.fatal("failed open file:"+fileName+", or failed writing record to:"+fileName);
        }
        logger.info("finished writing records");
	}
}
