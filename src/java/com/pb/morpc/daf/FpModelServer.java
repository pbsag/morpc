package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * Sever for free parking eligibility Model
 */
import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;


public class FpModelServer extends MessageProcessingTask {

	private static boolean LOGGING = false;
    static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
	private boolean serverStarted = false;
	private boolean serverExiting = false;

	private Message startWorkMessage = null; 	
	private Message exitMessage = null; 	

	private ArrayList fpWorkerQueue = new ArrayList();

	private HashMap propertyMap = null; 

	private int activeWorkers = 0;
	
	
	
	
    public FpModelServer () {
    }



    public void onStart () {

		if (LOGGING)
		    logger.info( this.name + " onStart().");

    }




	public void onMessage(Message msg) {

		if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );

		//The free parking model server gets a START_INFO message from the main server
		//when it's ready for the free parking model to begin running.
		if ( msg.getSender().equals("MorpcServer") ) {

			if (msg.getId().equals(MessageID.START_INFO)) {
				
				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
			    if(propertyMap==null){
			    	logger.fatal("FpModelServer onMessage, no propertyMap included in this message.");
			    }

				serverStarted = true;
				serverExiting = false;
				activeWorkers = 0;
			}
			else if ( msg.getId().equals( MessageID.EXIT )	) {		
			    
			    serverExiting = true;
	
			}
			
			//handle any messages from fpWorkers queued up while waiting for server to start
			for ( Iterator i = fpWorkerQueue.iterator(); i.hasNext(); ) {
			    Message qMsg = (Message)i.next(); 
				if ( qMsg.getId().equals(MessageID.SEND_START_INFO)) {
    				String sender = ( qMsg.getSender() );
				    if (serverExiting) {
						if (LOGGING)
						    logger.info( this.name + " sending an EXIT back to " + sender );
						
						exitMessage = createMessage();
						exitMessage.setId(MessageID.EXIT);
						sendTo( sender, exitMessage );
						activeWorkers--;
						i.remove();
				    }
				    else {
    					if (LOGGING)
    					    logger.info( this.name + " sending a START_INFO back to " + sender );
    
                		startWorkMessage = createMessage();
    		            startWorkMessage.setId(MessageID.START_INFO);
    					startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );

    					sendTo( sender, startWorkMessage );
    					activeWorkers++;
    					i.remove();
					}
				}
				else if (qMsg.getId().equals(MessageID.FINISHED)) {
					//send a RELEASE_MEMORY to the worker to free its big local memory allocations
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( qMsg.getSender(), rMsg );
					//queue a SEND_START_INFO from a worker that's ready to start the next model
					qMsg.setId(MessageID.SEND_START_INFO);
					fpWorkerQueue.add( qMsg );
					activeWorkers--;
				}
			}
				
		}
		//The free parking model server gets a SEND_START_INFO message from the fpWorkers
		//saying they're ready to begin work.
		else {

		    if ( serverStarted ) {
		        
				if (msg.getId().equals(MessageID.SEND_START_INFO)) {
				    if (serverExiting) {
						if (LOGGING)
						    logger.info( this.name + " sending an EXIT back to " + msg.getSender() );
						exitMessage = createMessage();
						exitMessage.setId(MessageID.EXIT);
						sendTo( msg.getSender(), exitMessage );
						activeWorkers--;
				    }
				    else {
				        try {

        					//Send a START_INFO message back to the worker
        					if (LOGGING)
        					    logger.info( this.name + " sending a START_INFO back to " + msg.getSender() );
        
                    		startWorkMessage = createMessage();
        		            startWorkMessage.setId(MessageID.START_INFO);
        					startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
				
							replyToSender(startWorkMessage);
							activeWorkers++;
				        
				        }
						catch (RuntimeException e) {
							throw e;
						}
				    }
				}
				else if (msg.getId().equals(MessageID.FINISHED)) {
					//send a RELEASE_MEMORY to the worker to free its big local memory allocations
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( msg.getSender(), rMsg );
					//queue a SEND_START_INFO from a worker that's ready to start the next model
					msg.setId(MessageID.SEND_START_INFO);
					fpWorkerQueue.add( msg );
					activeWorkers--;
				}
			
		    }
		    else {
		        
				// queue the messages until FpModelServer is told to start
				fpWorkerQueue.add( msg );
				
		    }
		}

		// if all workers that were sent START_INFO have finished, kill the server
		if ( serverStarted && activeWorkers == 0 ) {
			//all workers are done, so send an EXIT message back to the main server
			if (LOGGING)
			    logger.info( this.name + " sending an EXIT back to MorpcServer" );

			if (serverExiting) {
				propertyMap = null;
			}
			
			// send a resetHouseholdCount to the hhArrayServer to tell it to zero
			// its hhs processed count once all workers have finished.
			Message resetHHsMsg = createMessage();
			resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
			sendTo("HhArrayServer", resetHHsMsg);

			exitMessage = createMessage();
			exitMessage.setId(MessageID.EXIT);
			sendTo("MorpcServer", exitMessage);

		}

	}

	
	
	private void printMessageContents ( Message msg ) {

		//Print out message contents
		if (LOGGING) {
		    logger.info( "FpModelServer onMessage() Received " + msg.getType() + " message from=" + msg.getSender() );
			logger.info ( " messageId= " + msg.getId() );
			
			int j=0;
			for (Iterator i = msg.valueIterator(); i.hasNext(); ) {
				logger.info( " arg["+ ++j + "]: " + i.next() );
			}
		}

	}


}
