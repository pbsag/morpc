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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;


public class HHArrayServer extends MessageProcessingTask {


	private static boolean LOGGING = false;
    static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
	
	private boolean hhServerStarted = false;

	private Message startWorkMessage = null; 	

	private ArrayList hhServerQueue = new ArrayList();

	private HouseholdArrayManager hhMgr = null;
	
	//Wu added to send propertyMap to workers
	private HashMap propertyMap=null;
	
	
    
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

				hhMgr = new HouseholdArrayManager( propertyMap );

				// check if property is set to start the model iteration from a DiskObjectArray,
				// and if so, get HHs from the existing DiskObjectArray.
				if(((String)propertyMap.get("FTA_Restart_run")).equalsIgnoreCase("true")) {
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
	

}
