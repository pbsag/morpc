package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * worker class for running free parking eligibility choice for
 * a list of tours in a distributed environment.
 */


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.morpc.models.FreeParkingEligibility;
import com.pb.morpc.structures.Household;


import java.util.HashMap;
import java.util.logging.Logger;



public class FpWorker extends MessageProcessingTask implements java.io.Serializable {

	private static boolean LOGGING = true;
	private static Logger logger = Logger.getLogger("com.pb.morpc.models");
	
	private Household[] hhList = null;
	private FreeParkingEligibility fpModel = null;

	private HashMap propertyMap = null; 

	private String modelServer = "FpModelServer";
	
	
    public FpWorker () {
    }
    


	public void onStart() {

		if (LOGGING)
		    logger.info( this.name +  " onStart().");

		// ask the free parking model server for start info including the propertyMap
		if (LOGGING)
		    logger.info( this.name +  " asking for START_INFO from " + modelServer + "." );
		Message msg = createMessage();
		msg.setId( MessageID.SEND_START_INFO );
		sendTo( modelServer, msg );
		
	}



	public void onMessage(Message msg) {


		if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );
		
		Message newMessage = null;

		
		//Do some work ...
		int messageReturnType = respondToMessage(msg);

		if ( messageReturnType > 0 ) {
		    
			//Send a response back to the server
			if (messageReturnType == MessageID.RESULTS_ID)
				newMessage = createResultsMessage();
			else if (messageReturnType == MessageID.FINISHED_ID)
				newMessage = createFinishedMessage();
			else if (messageReturnType == MessageID.EXIT_ID)
				fpModel = null;
	
			if (newMessage != null)
			    replyToSender(newMessage);
		
		}
		
	}



	private int respondToMessage (Message msg) {

		int returnValue=0;
		

		if ( msg.getSender().equals(modelServer) ) {

			if (msg.getId().equals(MessageID.START_INFO)) {
			    
				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
				
				if (LOGGING)
				    logger.info (this.getName() + " building fpModel object.");
				fpModel = new FreeParkingEligibility(propertyMap);
				
				Message sendWorkMsg = createMessage();
				sendWorkMsg.setId(MessageID.SEND_WORK);
				sendWorkMsg.setIntValue( MessageID.TOUR_CATEGORY_KEY, 0 );
				sendWorkMsg.setValue( MessageID.TOUR_TYPES_KEY, null );
				sendTo( "HhArrayServer", sendWorkMsg );
				
			}
			else if ( msg.getId().equals( MessageID.RELEASE_MEMORY ) || msg.getId().equals( MessageID.EXIT ) ) {

				if (LOGGING)
					logger.info (this.getName() + " releasing memory after getting " + msg.getId() + " from " + msg.getSender() );
			    
				fpModel = null;
				propertyMap = null;
				hhList = null;

			}
			
		}
		else {

			// this message should contain an array of Household objects to process
			if ( msg.getId().equals( MessageID.HOUSEHOLD_LIST )	) {		

				// retrieve the contents of the message:
				hhList = (Household[])msg.getValue( MessageID.HOUSEHOLD_LIST_KEY );



				// if the list is null, no more hhs left to process;
				// otherwise, put the household objects from the message into an array for processing.
				if ( hhList != null ) {

					// run the free parking eligibility model for the tours in these households
					if (LOGGING)
					    logger.info ( this.getName() + " processing household ids: " + hhList[0].getID() + " to " + hhList[hhList.length-1].getID() );
					fpModel.runFreeParkingEligibility (hhList);
					returnValue = MessageID.RESULTS_ID;

				}
				else {
				
					returnValue = MessageID.FINISHED_ID;
				
				}
			
			}
		
		}

		return returnValue;
		
	}
	
	
	
	private Message createFinishedMessage () {

		Message newMessage = createMessage();

		newMessage.setId( MessageID.FINISHED );
		sendTo (modelServer, newMessage);
		
		return null;
	}



	private Message createResultsMessage () {

		Message newMessage = createMessage();
		newMessage.setId( MessageID.RESULTS );
		newMessage.setIntValue( MessageID.TOUR_CATEGORY_KEY, 0 );
		newMessage.setValue( MessageID.TOUR_TYPES_KEY, null );
		newMessage.setValue( MessageID.HOUSEHOLD_LIST_KEY, hhList );

		return newMessage;
	}



}
