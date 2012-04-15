package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * server for mandatory destination choice model
 */
import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.models.TODDataManager;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;


public class DcModelServer extends MessageProcessingTask {

	private static boolean LOGGING = true;
    static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
	private boolean serverStarted = false;
	private boolean serverExiting = false;

	private ArrayList workerQueue = new ArrayList();

	private ZonalDataManager zdm = 	null;
	private TODDataManager tdm = 	null;
	private HashMap propertyMap = null; 
	private HashMap zdmMap = null;
	private HashMap tdmMap = null;
	
	private int shadowPriceIter = 0;
	private int activeWorkers = 0;


    
    public DcModelServer () {

        if (LOGGING)
            logger.info( "DcModelServer() constructor: " + this.name + ".");
        
    }



    public void onStart () {

        if (LOGGING)
            logger.info( "DcModelServer (name=" + this.name + ") onStart().");

    }




	public void onMessage(Message msg) {
		
		int taskNumber = 0;

        if (LOGGING)
            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + "." );

		//The model server gets a START_INFO message from the main server
		//when it's ready for the model to begin running.
		if ( msg.getSender().equals("MorpcServer") ) {

			if (msg.getId().equals(MessageID.START_INFO)) {
			    // resolve message contents
				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
			    if(propertyMap==null){
			    	logger.info("DcModelServer onMessage, no propertyMap included in this message.");
			    }
			    
				zdm = (ZonalDataManager)msg.getValue( MessageID.ZONAL_DATA_MANAGER_KEY );
				zdmMap = (HashMap)msg.getValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY );
				tdm = (TODDataManager)msg.getValue( MessageID.TOD_DATA_MANAGER_KEY );
				tdmMap = (HashMap)msg.getValue( MessageID.STATIC_TOD_DATA_MAP_KEY );
				shadowPriceIter = Integer.parseInt((String)msg.getValue( MessageID.SHADOW_PRICE_ITER_KEY ));
				
				serverStarted = true;
				serverExiting = false;
				activeWorkers = 0;
			}
			else if ( msg.getId().equals( MessageID.EXIT )	) {		
			    
			    serverExiting = true;
	
			}
			
			//handle any messages from workers queued up while waiting for server to start
			for ( Iterator i = workerQueue.iterator(); i.hasNext(); ) {
			    Message qMsg = (Message)i.next(); 
				if ( qMsg.getId().equals(MessageID.SEND_START_INFO)) {
					String sender = qMsg.getSender();
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
						
						taskNumber = getSenderIndex(sender);
						
						// create start work message to send to workers
						Message startWorkMessage = createMessage();
						startWorkMessage.setId(MessageID.START_INFO);
						startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
						startWorkMessage.setValue( MessageID.ZONAL_DATA_MANAGER_KEY, zdm );
						startWorkMessage.setValue( MessageID.TOD_DATA_MANAGER_KEY, tdm );
						startWorkMessage.setValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY, zdmMap );
						startWorkMessage.setValue( MessageID.STATIC_TOD_DATA_MAP_KEY, tdmMap );
						startWorkMessage.setValue( MessageID.SHADOW_PRICE_ITER_KEY, Integer.toString(shadowPriceIter) );
						startWorkMessage.setValue( MessageID.PROCESSOR_ID_KEY, Integer.toString(taskNumber % ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES) );
				
						//sendTo( sender, startWorkMessage );
                        sendTo( sender, startWorkMessage, logger, this.name );
						activeWorkers++;
				    }
                    i.remove();
				}
				else if (qMsg.getId().equals(MessageID.FINISHED)) {

                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", taking message out of workerQueue[" + workerQueue.size() + "], received from " + qMsg.getSender() + ", sending a RELEASE_MEMORY back to " + qMsg.getSender() );

				    //send a RELEASE_MEMORY to the worker to free its big local memory allocations
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( qMsg.getSender(), rMsg );
					
					//queue a SEND_START_INFO from a worker
					qMsg.setId(MessageID.SEND_START_INFO);

                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + qMsg.getId() + " adding message (taken from queue) with SEND_START_INFO to workerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + " for sender " + qMsg.getSender() );

					workerQueue.add( qMsg );
					activeWorkers--;
				}
			}
				
		}
		//The server gets a SEND_START_INFO message from the workers
		//saying they're ready to begin work.
		else {

		    if ( serverStarted ) {
		        
				if (msg.getId().equals(MessageID.SEND_START_INFO)) {
				    if (serverExiting) {
				        
                        if (LOGGING)
                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", received from " + workerQueue.size() + "], received from " + msg.getSender() + ", serverExiting is true, sending an EXIT back to " + msg.getSender() );
                        
						Message exitMessage = createMessage();
						exitMessage.setId(MessageID.EXIT);
						sendTo( msg.getSender(), exitMessage );
						activeWorkers--;
				    }
				    else {
				        try {
				            
							//Send a START_INFO message back to the worker
                            if (LOGGING)
                                logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + ", sending a START_INFO back to " + msg.getSender() );

							taskNumber = getSenderIndex(msg.getSender());
							
							// create start work message to send to workers
							Message startWorkMessage = createMessage();
							startWorkMessage.setId(MessageID.START_INFO);
							startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
							startWorkMessage.setValue( MessageID.ZONAL_DATA_MANAGER_KEY, zdm );
							startWorkMessage.setValue( MessageID.TOD_DATA_MANAGER_KEY, tdm );
							startWorkMessage.setValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY, zdmMap );
							startWorkMessage.setValue( MessageID.STATIC_TOD_DATA_MAP_KEY, tdmMap );
							startWorkMessage.setValue( MessageID.SHADOW_PRICE_ITER_KEY, Integer.toString(shadowPriceIter) );
							startWorkMessage.setValue( MessageID.PROCESSOR_ID_KEY, Integer.toString(taskNumber % ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES) );
				
                            //replyToSender(startWorkMessage, logger);
							replyToSender(startWorkMessage, logger, this.name, msg.getSender() );

	                        activeWorkers++;
				        
				        }
						catch (RuntimeException e) {
							throw e;
						}
				    }
				}
				else if (msg.getId().equals(MessageID.FINISHED)) {

                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + " received from " + msg.getSender() + ", sending a RELEASE_MEMORY back to " + msg.getSender() );

					//send a RELEASE_MEMORY to the worker to free its big local memory allocations
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( msg.getSender(), rMsg );

                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " adding message with SEND_START_INFO to workerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + " for sender " + msg.getSender() );

					//queue a SEND_START_INFO from a worker that's ready to start the next model
					msg.setId(MessageID.SEND_START_INFO);
					workerQueue.add( msg );
					activeWorkers--;
				}
			
		    }
		    else {
		        
		        // queue the messages until DcModelServer is told to start
				workerQueue.add( msg );
				
                if (LOGGING)
                    logger.info( this.name + " onMessage() id=" + msg.getId() + " received from " + msg.getSender() + " added to workerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() );
		    }
		}

		// if all workers that were sent START_INFO have finished, release memory
		if ( serverStarted && activeWorkers == 0 ) {
			//all workers are done, so send an EXIT message back to the main server
            if (LOGGING)
                logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", serverStarted && activeWorkers == 0, sending RESET_HHS_PROCESSED to HhArrayServer."  );
			
			if (serverExiting) {
				zdm = null;
				tdm = null;
				zdmMap = null;
				tdmMap = null;
				propertyMap = null;
			}
			
			// send a resetHouseholdCount to the hhArrayServer to tell it to zero
			// its hhs processed count once all workers have finished.
			Message resetHHsMsg = createMessage();
			resetHHsMsg.setId(MessageID.RESET_HHS_PROCESSED);
			sendTo("HhArrayServer", resetHHsMsg);

            if (LOGGING)
                logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", serverStarted && activeWorkers == 0, sending EXIT to MorpcServer."  );
            
			Message exitMessage = createMessage();
			exitMessage.setId(MessageID.EXIT);
			sendTo("MorpcServer", exitMessage);
		}

	}

	/**
	 * get the numeric task identifier from the task name and return it.
	 * 
	 * it is assumed that the tasks are named in the "applicationDaf.properties"
	 * file by concatenating unique numeric identifiers for each task to the end of a common string,
	 * e.g. dcWorker1, dcWorker2, ..., dcWorkerN for N tasks defined accross various nodes.
	 * 
	 * this method therefore starts at the end of the sender String and extracts numeric
	 * characters to form the numeric identifier for the task name. 
	 * 
	 * @param sender
	 * @return numeric task identifier from the task name
	 */
	private int getSenderIndex ( String sender ) {
		
		int i = sender.length() - 1;
		
		while ( i >= 0 && sender.substring(i,i+1).matches( "[0-9]") ) {
			i--;
		}
			
		return Integer.parseInt( sender.substring(i+1) );
		
	}

}
