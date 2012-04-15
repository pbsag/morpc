package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * Server for setting the same random number seed on each node in the distributed application
 */
import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;


public class RnServer extends MessageProcessingTask {

	private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.morpc.daf");
    
	private boolean serverStarted = false;
	private boolean serverExiting = false;

	private ArrayList workerQueue = new ArrayList();

	private HashMap propertyMap = null; 

    private int sentWorkers = 0;
    private int receivedWorkers = 0;
	
	
	
	
    public RnServer () {

        if (LOGGING)
            logger.info( "RnServer() constructor: " + this.name + ".");

    }



    public void onStart () {

        if (LOGGING)
            logger.info( "RnServer (name=" + this.name + ") onStart().");

    }




	public void onMessage(Message msg) {

        if (LOGGING)
            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + "." );

        //The random number server gets a START_INFO message from the main server
		//when it's ready for the random number seeds to be set.
		if ( msg.getSender().equals("MorpcServer") ) {

			if (msg.getId().equals(MessageID.START_INFO)) {
				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
				serverStarted = true;
				serverExiting = false;
				sentWorkers = 0;
				receivedWorkers = 0;
			}
			else if ( msg.getId().equals( MessageID.EXIT )	) {		
			    
			    serverExiting = true;
	
			}
			
            if (LOGGING) {
                logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + " received from " + msg.getSender() + ", sentWorkers=" + sentWorkers + ", receivedWorkers=" + receivedWorkers + ", serverExiting=" + serverExiting);                        
                logger.info( "*** starting loop to process messages in rnWorkerQueue[" + workerQueue.size() + "] ***" );                        
            }            
            
			//handle any messages from rnWorkers queued up while waiting for rnServer to start
			for ( Iterator i = workerQueue.iterator(); i.hasNext(); ) {
			    Message qMsg = (Message)i.next(); 
				if ( qMsg.getId().equals(MessageID.SEND_START_INFO)) {
    				String sender = ( qMsg.getSender() );
				    if (serverExiting) {

				        if (LOGGING)
                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", taking message out of rnWorkeQueue[" + workerQueue.size() + "], received from " + sender + ", serverExiting is true, sending an EXIT back to " + sender  + ", sentWorkers=" + sentWorkers + ", receivedWorkers=" + receivedWorkers );                        
						
						Message exitMessage = createMessage();
						exitMessage.setId(MessageID.EXIT);
						sendTo( sender, exitMessage );

				    }
				    else {

                        if (LOGGING)
                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", taking message out of rnWorkerQueue[" + workerQueue.size() + "], received from " + sender + ", sending a START_INFO back to " + sender + ", sentWorkers=" + sentWorkers + ", receivedWorkers=" + receivedWorkers );
        
    					Message startWorkMessage = createMessage();
    		            startWorkMessage.setId(MessageID.START_INFO);
    					startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );

                        //sendTo( sender, startWorkMessage );
                        sendTo( sender, startWorkMessage, logger, this.name );
    					sentWorkers++;

					}
                    i.remove();
				}
				else if (qMsg.getId().equals(MessageID.FINISHED)) {

				    //send a RELEASE_MEMORY to the worker to free its big local memory allocations
                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", taking message out of rnWorkeQueue[" + workerQueue.size() + "], received from " + qMsg.getSender() + ", sending a RELEASE_MEMORY back to " + qMsg.getSender() );

					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( qMsg.getSender(), rMsg, logger, this.name );
					receivedWorkers++;
				}
			}
				
		}
		//The random number server gets a SEND_START_INFO message from the rnWorkers
		//saying they're ready to begin work.
		else {

		    if ( serverStarted ) {
		        
				if (msg.getId().equals(MessageID.SEND_START_INFO)) {
//				    if (serverExiting) {
//
//                        if (LOGGING)
//                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + ", serverExiting is true, sending an EXIT back to " + msg.getSender() + ", sentWorkers=" + sentWorkers + ", receivedWorkers=" + receivedWorkers );                       
//						Message exitMessage = createMessage();
//						exitMessage.setId(MessageID.EXIT);
//						sendTo( msg.getSender(), exitMessage );
//						receivedWorkers++;
//				    }
//				    else {
				        try {

//				            if (activeWorkers == 0) {

	                            //Send a START_INFO message back to the worker
		                        if (LOGGING)
		                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + ", serverExiting is false, sending an START_INFO back to " + msg.getSender() + ", sentWorkers=" + sentWorkers + ", receivedWorkers=" + receivedWorkers + ", workerQueue["+workerQueue.size() + "]" );
	                
								Message startWorkMessage = createMessage();
								startWorkMessage.setId(MessageID.START_INFO);
								startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
				
	                            //replyToSender(startWorkMessage);
	                            //replyToSender( startWorkMessage, logger, this.name, msg.getSender() );
	                            sendTo( msg.getSender(), startWorkMessage, logger, this.name );

	                            sentWorkers++;
				                
//				            }
//				            else {
//				                
//		                        if (LOGGING)
//		                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + ", serverExiting is false, no action taken, activeWorkers=" + activeWorkers );                       
//				                
//				            }
				        
				        }
						catch (RuntimeException e) {
							throw e;
						}
//				    }
				}
				else if (msg.getId().equals(MessageID.FINISHED)) {

                    //send a RELEASE_MEMORY to the worker to free its big local memory allocations
                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + ", sending a RELEASE_MEMORY back to " + msg.getSender() + ", sentWorkers=" + sentWorkers + ", receivedWorkers=" + receivedWorkers + ", workerQueue["+workerQueue.size() + "]" );
                    
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( msg.getSender(), rMsg, logger, this.name );

//                    if (LOGGING)
//                        logger.info( this.name + " onMessage() id=" + msg.getId() + " adding message with SEND_START_INFO to rnWorkerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + " for sender " + msg.getSender() + ", activeWorkers=" + activeWorkers );
//
//					msg.setId(MessageID.SEND_START_INFO);
//					workerQueue.add( msg );
					receivedWorkers++;
				}
			
		    }
		    else {
		        
                if (LOGGING)
                    logger.info( this.name + " onMessage() id=" + msg.getId() + " waiting for server to start, adding message to rnWorkerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + " for sender " + msg.getSender() + ", sentWorkers=" + sentWorkers + ", receivedWorkers=" + receivedWorkers );

				// queue the messages until RnServer is told to start
				workerQueue.add( msg );
				
		    }
		}

		// if all workers that were sent START_INFO have finished, kill the server
		if ( sentWorkers > 0 && serverStarted && sentWorkers == receivedWorkers ) {
			//all workers are done, so send an EXIT message back to the main server
			if (LOGGING)
			    logger.info( this.name + " sending an EXIT back to MorpcServer" );

			if (serverExiting) {
				propertyMap = null;
			}
			
			Message exitMessage = createMessage();
			exitMessage.setId(MessageID.EXIT);
			sendTo("MorpcServer", exitMessage);

		}

	}

}
