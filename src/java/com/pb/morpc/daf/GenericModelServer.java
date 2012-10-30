package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * model server class used for all models except for:
 *   free parking eligibilty model
 *   mandatory destination choice model
 */
import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.models.TODDataManager;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;


public class GenericModelServer extends MessageProcessingTask {

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
	
	private ArrayList workerTaskList = new ArrayList();
	private int activeWorkers = 0;
	


    
    public GenericModelServer () {

        if (LOGGING)
            logger.info( "GenericModelServer() constructor: " + this.name + ".");
        
    }



    public void onStart () {

        if (LOGGING)
            logger.info( "GenericModelServer (name=" + this.name + ") onStart().");

    }




	public synchronized void onMessage(Message msg) {
		
		int taskNumber = 0;

        if (LOGGING)
            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + "." );

		//The model server gets a START_INFO message from the main server
		//when it's ready for the model to begin running.
		if ( msg.getSender().equals("MorpcServer") ) {


			if (msg.getId().equals(MessageID.START_INFO)) {					
				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
			    if(propertyMap==null){
			    	logger.info("GenericModelServer onMessage, no propertyMap included in this message.");
			    }
				// resolve message contents
				//propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
				zdm = (ZonalDataManager)msg.getValue( MessageID.ZONAL_DATA_MANAGER_KEY );
				zdmMap = (HashMap)msg.getValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY );
				tdm = (TODDataManager)msg.getValue( MessageID.TOD_DATA_MANAGER_KEY );
				tdmMap = (HashMap)msg.getValue( MessageID.STATIC_TOD_DATA_MAP_KEY );
				
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
                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", taking message out of workerQueue[" + workerQueue.size() + "], received from " + sender + ", serverExiting is true, sending an EXIT back to " + sender );

                        Message exitMessage = createMessage();
						exitMessage.setId(MessageID.EXIT);
						sendTo( sender, exitMessage );
						activeWorkers--;
					}
					else {
    
					    if (LOGGING)
                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", taking message out of workerQueue[" + workerQueue.size() + "], received from " + sender + ", sending a START_INFO back to " + sender );
    
						// add the sender task name and number that requested work to a worker arrayList
						// and to a processorId HashMap.
						workerTaskList.add ( sender );

						taskNumber = getSenderIndex(sender);

						
						// create start work message to send to workers
						Message startWorkMessage = createMessage();
						startWorkMessage.setId(MessageID.START_INFO);
						startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
						startWorkMessage.setValue( MessageID.ZONAL_DATA_MANAGER_KEY, zdm );
						startWorkMessage.setValue( MessageID.TOD_DATA_MANAGER_KEY, tdm );
						startWorkMessage.setValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY, zdmMap );
						startWorkMessage.setValue( MessageID.STATIC_TOD_DATA_MAP_KEY, tdmMap );
						startWorkMessage.setValue( MessageID.PROCESSOR_ID_KEY, Integer.toString(taskNumber % ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES) );
				
                        sendTo( sender, startWorkMessage );


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
					//If this is the last worker, send a RELEASE_MATRIX_MEMORY to the workers.
					//Only AtWorkDTMWorkers and AtWorkStopsWorkers will do anything with this message
					if (activeWorkers == 1) {
					    Message rmMsg = createMessage();
					    rmMsg.setId(MessageID.RELEASE_MATRIX_MEMORY);

						// loop through all tasks that asked for work and send a RELEASE_MATRIX_MEMORY message
				        for (int m = 0; m < workerTaskList.size(); m++) {
						    logger.info( "all worker tasks have finished, " + this.name + " sending a RELEASE_MATRIX_MEMORY to " + (String)workerTaskList.get(m) );
							sendTo( (String)workerTaskList.get(m), rmMsg );
				        }
					}
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
						//Send a START_INFO message back to the worker
                        if (LOGGING)
                            logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]" + ", received from " + msg.getSender() + ", sending a START_INFO back to " + msg.getSender() );
            
						// add the sender task name and number that requested work to a worker arrayList
						// and to a processorId HashMap.
						workerTaskList.add ( msg.getSender() );

						taskNumber = getSenderIndex( msg.getSender() );

						
						// create start work message to send to workers
						Message startWorkMessage = createMessage();
						startWorkMessage.setId(MessageID.START_INFO);
						startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
						startWorkMessage.setValue( MessageID.ZONAL_DATA_MANAGER_KEY, zdm );
						startWorkMessage.setValue( MessageID.TOD_DATA_MANAGER_KEY, tdm );
						startWorkMessage.setValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY, zdmMap );
						startWorkMessage.setValue( MessageID.STATIC_TOD_DATA_MAP_KEY, tdmMap );
						startWorkMessage.setValue( MessageID.PROCESSOR_ID_KEY, Integer.toString(taskNumber % ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES) );
				
						replyToSender(startWorkMessage);
                        
						activeWorkers++;
					}
				}
				else if (msg.getId().equals(MessageID.FINISHED)) {
					//send a RELEASE_MEMORY to the worker to free its big local memory allocations
                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + " received from " + msg.getSender() + ", sending a RELEASE_MEMORY back to " + msg.getSender() );
                    
                    Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( msg.getSender(), rMsg );
					//If this is the last worker, send a RELEASE_MATRIX_MEMORY to all the workers.
					//Only AtWorkDTMWorkers and AtWorkStopsWorkers will do anything with this message
					if (activeWorkers == 1) {
						Message rmMsg = createMessage();
						rmMsg.setId(MessageID.RELEASE_MATRIX_MEMORY);

						// loop through all tasks that asked for work and send a RELEASE_MATRIX_MEMORY message
				        for (int m = 0; m < workerTaskList.size(); m++) {
						    logger.info( "all worker tasks have finished, " + this.name + " sending a RELEASE_MATRIX_MEMORY to " + (String)workerTaskList.get(m) );
							sendTo( (String)workerTaskList.get(m), rmMsg );
				        }
					}
					
                    if (LOGGING)
                        logger.info( this.name + " onMessage() id=" + msg.getId() + " adding message with SEND_START_INFO to workerQueue[" + workerQueue.size() + "] on thread " + Thread.currentThread().getName() + " for sender " + msg.getSender() );
					
					//queue a SEND_START_INFO from a worker
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
			    logger.info( this.name + " sending an EXIT back to MorpcServer" );
			
			if (serverExiting) {
				zdm = null;
				tdm = null;
				zdmMap = null;
				tdmMap = null;
				propertyMap = null;
			}

            if (LOGGING)
                logger.info( this.name + " onMessage() id=" + msg.getId() + " on thread " + Thread.currentThread().getName() + ", serverStarted && activeWorkers == 0, sending RESET_HHS_PROCESSED to HhArrayServer."  );
            
			
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
