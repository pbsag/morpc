package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * worker class for setting the same random number seed on all nodes in a distributed environment.
 */


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.SeededRandom;


import java.util.HashMap;
import java.util.logging.Logger;



public class RnWorker extends MessageProcessingTask implements java.io.Serializable {

	private static boolean LOGGING = true;
	private static Logger logger = Logger.getLogger("com.pb.morpc.daf");
	
	private HashMap propertyMap = null; 

	private String modelServer = "RnServer";
	
	
    public RnWorker () {
    }
    


	public void onStart() {

		if (LOGGING)
		    logger.info( this.name +  " onStart().");

		// ask the random number server for start info including the propertyMap
		if (LOGGING)
		    logger.info( this.name +  " asking for START_INFO from " + modelServer + "." );
		Message msg = createMessage();
		msg.setId( MessageID.SEND_START_INFO );
		sendTo( modelServer, msg );
		
	}



	public void onMessage(Message msg) {

		if (msg.getId().equals(MessageID.START_INFO)) {
		    
			propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
			
			if (LOGGING)
			    logger.info (this.getName() + " reading propertyMap message.");
			
			// set the global random number generator seed read from the properties file
			SeededRandom.setSeed(Integer.parseInt((String) propertyMap.get("RandomSeed")));

			Message finishedMsg = createMessage();
			finishedMsg.setId( MessageID.FINISHED );
			sendTo( modelServer, finishedMsg );
			
		}
		else if ( msg.getId().equals( MessageID.RELEASE_MEMORY ) || msg.getId().equals( MessageID.EXIT ) ) {

			if (LOGGING)
				logger.info (this.getName() + " releasing memory after getting " + msg.getId() + " from " + msg.getSender() );
		    
			propertyMap = null;

		}
		
	}

}
