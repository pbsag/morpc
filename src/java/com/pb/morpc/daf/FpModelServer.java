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

	private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
	private boolean serverStarted = false;
	private boolean serverExiting = false;

	private ArrayList workerQueue = new ArrayList();

	private HashMap propertyMap = null; 

	private int activeWorkers = 0;
	
	
    public FpModelServer () {

        if (LOGGING)
            logger.info( "FpModelServer() constructor: " + this.name + ".");
        
    }



    public void onStart () {

		if (LOGGING)
		    logger.info( "FpModelServer (name=" + this.name + ") onStart().");

    }




	public void onMessage(Message msg) {

		if (LOGGING)
		    logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + "." );

		//The free parking model server gets a START_INFO message from the main server
		//when it's ready for the free parking model to begin running.
		if ( msg.getSender().equals("MorpcServer") ) {

			if (msg.getId().equals(MessageID.START_INFO)) {
				
				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
			    if(propertyMap==null){
			    	logger.fatal("FpModelServer onMessage, no propertyMap included in this message.");
			    	throw new RuntimeException();
			    }

				serverStarted = true;
				serverExiting = false;
				activeWorkers = 0;
			}
			else if ( msg.getId().equals( MessageID.EXIT )	) {		
			    
			    serverExiting = true;
	
			}
			
			//handle any messages from fpWorkers queued up while waiting for server to start
			for ( Iterator i = workerQueue.iterator(); i.hasNext(); ) {
			    Message qMsg = (Message)i.next(); 
				if ( qMsg.getId().equals(MessageID.SEND_START_INFO)) {
    				String sender = ( qMsg.getSender() );
				    if (serverExiting) {
						
				        if (LOGGING)
                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", taking message out of workerQueue[" + workerQueue.size() + "], received from " + sender + ", serverExiting is true, sending an EXIT back to " + sender + ", activeWorkers=" + activeWorkers );
						
						Message exitMessage = createMessage();
						exitMessage.setId(MessageID.EXIT);

						//sendTo( sender, exitMessage );
                        sendTo( sender, exitMessage, logger, this.name );
						activeWorkers--;
				    }
				    else {

				        if (LOGGING)
    					    logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", taking message out of workerQueue[" + workerQueue.size() + "], received from " + sender + ", sending a START_INFO back to " + sender + ", activeWorkers=" + activeWorkers);
    
    					Message startWorkMessage = createMessage();
    		            startWorkMessage.setId(MessageID.START_INFO);
    					startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );

                        //sendTo( sender, startWorkMessage );
                        sendTo( sender, startWorkMessage, logger, this.name );
    					activeWorkers++;
					}
                    i.remove();
				}
				else if (qMsg.getId().equals(MessageID.FINISHED)) {
					//send a RELEASE_MEMORY to the worker to free its big local memory allocations
                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", taking message out of workerQueue[" + workerQueue.size() + "], received from " + qMsg.getSender() + ", sending a RELEASE_MEMORY back to " + qMsg.getSender() );
                    
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( qMsg.getSender(), rMsg );

					//queue a SEND_START_INFO from a worker that's ready to start the next model
					qMsg.setId(MessageID.SEND_START_INFO);

                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + qMsg.getId() + " adding message (taken from queue) with SEND_START_INFO to workerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + " for sender " + qMsg.getSender() + ", activeWorkers=" + activeWorkers );

                    workerQueue.add( qMsg );
					activeWorkers--;
				}
			}
				
		}
		//The free parking model server gets a SEND_START_INFO message from the fpWorkers
		//saying they're ready to begin work.
		else {

		    if ( serverStarted ) {
		        
				if (msg.getId().equals(MessageID.SEND_START_INFO)) {
//				    if (serverExiting) {
//
//                        if (LOGGING)
//                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + workerQueue.size() + "], received from " + msg.getSender() + ", serverExiting is true, sending an EXIT back to " + msg.getSender() + ", activeWorkers=" + activeWorkers );
//                        
//						Message exitMessage = createMessage();
//						exitMessage.setId(MessageID.EXIT);
//						
//						//sendTo( msg.getSender(), exitMessage );
//                        sendTo( msg.getSender(), exitMessage, logger, this.name );
//						activeWorkers--;
//				    }
//				    else {
				        try {

        					//Send a START_INFO message back to the worker
	                        if (LOGGING)
	                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + ", sending a START_INFO back to " + msg.getSender() );
	            
        					Message startWorkMessage = createMessage();
        		            startWorkMessage.setId(MessageID.START_INFO);
        					startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
				
							//replyToSender(startWorkMessage);
							replyToSender( startWorkMessage, logger, this.name, msg.getSender() );
							activeWorkers++;
				        
				        }
						catch (RuntimeException e) {
							throw e;
						}
//				    }
				}
				else if (msg.getId().equals(MessageID.FINISHED)) {
					//send a RELEASE_MEMORY to the worker to free its big local memory allocations
                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + " received from " + msg.getSender() + ", sending a RELEASE_MEMORY back to " + msg.getSender() );
                    
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( msg.getSender(), rMsg );

//                    if (LOGGING)
//                        logger.info( this.name + " onMessage() id=" + msg.getId() + " adding message with SEND_START_INFO to workerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + " for sender " + msg.getSender() );
//
//					//queue a SEND_START_INFO from a worker that's ready to start the next model
					Message qMsg = createMessage();
					qMsg.setId(MessageID.SEND_START_INFO);
					workerQueue.add( qMsg );
					activeWorkers--;
				}
			
		    }
		    else {
		        
				// queue the messages until FpModelServer is told to start
				workerQueue.add( msg );
	            if (LOGGING)
	                logger.info( this.name + " onMessage() id=" + msg.getId() + " received from " + msg.getSender() + " added to workerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" );
				
		    }
		}

		// if all workers that were sent START_INFO have finished, kill the server
		if ( serverStarted && activeWorkers == 0 ) {

		    //all workers are done, so send an EXIT message back to the main server

			if (serverExiting) {
				propertyMap = null;
			}
			
            if (LOGGING)
                logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", serverStarted && activeWorkers == 0, sending RESET_HHS_PROCESSED to HhArrayServer."  );
            
			// send a resetHouseholdCount to the hhArrayServer to tell it to zero
			// its hhs processed count once all workers have finished.
			Message resetHHsMsg = createMessage();
			resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
			sendTo("HhArrayServer", resetHHsMsg);

            if (LOGGING)
                logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + "[" + Thread.currentThread().getId() + "]" + ", serverStarted && activeWorkers == 0, sending EXIT to MorpcServer."  );
            
			Message exitMessage = createMessage();
			exitMessage.setId(MessageID.EXIT);
			sendTo("MorpcServer", exitMessage);

		}

	}

}
