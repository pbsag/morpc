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

	private Message startWorkMessage = null; 	
	private Message exitMessage = null; 	

	private ArrayList workerQueue = new ArrayList();

	private ZonalDataManager zdm = 	null;
	private TODDataManager tdm = 	null;
	private HashMap propertyMap = null; 
	private HashMap zdmMap = null;
	private HashMap tdmMap = null;
	
	private int shadowPriceIter = 0;
	private int activeWorkers = 0;
	


    
    public DcModelServer () {
    }



    public void onStart () {

		if (LOGGING)
		    logger.info( this.name + " onStart().");

    }




	public void onMessage(Message msg) {

		if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );

		//The model server gets a START_INFO message from the main server
		//when it's ready for the model to begin running.
		if ( msg.getSender().equals("MorpcServer") ) {

			if (msg.getId().equals(MessageID.START_INFO)) {
			    // resolve message contents
				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
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

						// create start work message to send to workers
						startWorkMessage = createMessage();
						startWorkMessage.setId(MessageID.START_INFO);
						startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
						startWorkMessage.setValue( MessageID.ZONAL_DATA_MANAGER_KEY, zdm );
						startWorkMessage.setValue( MessageID.TOD_DATA_MANAGER_KEY, tdm );
						startWorkMessage.setValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY, zdmMap );
						startWorkMessage.setValue( MessageID.STATIC_TOD_DATA_MAP_KEY, tdmMap );
						startWorkMessage.setValue( MessageID.SHADOW_PRICE_ITER_KEY, Integer.toString(shadowPriceIter) );
						startWorkMessage.setValue( MessageID.PROCESSOR_ID_KEY, Integer.toString(activeWorkers) );
				
						sendTo( sender, startWorkMessage );
						activeWorkers++;
						i.remove();
				    }
				}
				else if (qMsg.getId().equals(MessageID.FINISHED)) {

				    if (LOGGING)
						logger.info( this.name + " had a queued FINISH and is sending a RELEASE_MEMORY back to " + qMsg.getSender() );

				    //send a RELEASE_MEMORY to the worker to free its big local memory allocations
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( qMsg.getSender(), rMsg );
					
					//queue a SEND_START_INFO from a worker
					qMsg.setId(MessageID.SEND_START_INFO);
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

							// create start work message to send to workers
							startWorkMessage = createMessage();
							startWorkMessage.setId(MessageID.START_INFO);
							startWorkMessage.setValue( MessageID.PROPERTY_MAP_KEY, propertyMap );
							startWorkMessage.setValue( MessageID.ZONAL_DATA_MANAGER_KEY, zdm );
							startWorkMessage.setValue( MessageID.TOD_DATA_MANAGER_KEY, tdm );
							startWorkMessage.setValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY, zdmMap );
							startWorkMessage.setValue( MessageID.STATIC_TOD_DATA_MAP_KEY, tdmMap );
							startWorkMessage.setValue( MessageID.SHADOW_PRICE_ITER_KEY, Integer.toString(shadowPriceIter) );
							startWorkMessage.setValue( MessageID.PROCESSOR_ID_KEY, Integer.toString(activeWorkers) );
				
							replyToSender(startWorkMessage);
							activeWorkers++;
				        
				        }
						catch (RuntimeException e) {
							throw e;
						}
				    }
				}
				else if (msg.getId().equals(MessageID.FINISHED)) {

					if (LOGGING)
						logger.info( this.name + " got a FINISHED and is sending a RELEASE_MEMORY back to " + msg.getSender() );

					//send a RELEASE_MEMORY to the worker to free its big local memory allocations
					Message rMsg = createMessage();
					rMsg.setId(MessageID.RELEASE_MEMORY);
					sendTo( msg.getSender(), rMsg );

					//queue a SEND_START_INFO from a worker that's ready to start the next model
					msg.setId(MessageID.SEND_START_INFO);
					workerQueue.add( msg );
					activeWorkers--;
				}
			
		    }
		    else {
		        
		        // queue the messages until DcModelServer is told to start
				workerQueue.add( msg );
				
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

}
