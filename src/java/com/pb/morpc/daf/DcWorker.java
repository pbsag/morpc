package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * worker class for running mandatory destination choice for
 * a list of households in a distributed environment.
 */


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.morpc.models.DTMHousehold;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.models.TODDataManager;
import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.TourType;

import java.util.HashMap;
import org.apache.log4j.Logger;



public class DcWorker extends MessageProcessingTask implements java.io.Serializable {

	private static boolean LOGGING = true;
	private static Logger logger = Logger.getLogger("com.pb.morpc.models");

	private Household[] hhList = null;
	private DTMHousehold dtmHH = null;

	private ZonalDataManager zdm = 	null;
	private TODDataManager tdm = 	null;
	private HashMap propertyMap = null;
	private HashMap zdmMap = null;
	private HashMap tdmMap = null;
	private int shadowPriceIter = 0;

	private String modelServer = "DcModelServer";

	private int processorId = 0;


    public DcWorker () {
    }



	public void onStart() {

		if (LOGGING)
		    logger.info( this.name +  " onStart().");

		// ask the model server for start info including the propertyMap
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
			    newMessage = null;

			if (newMessage != null)
			    replyToSender(newMessage);

		}

	}



	private int respondToMessage (Message msg) {

		int returnValue=0;


		if ( msg.getSender().equals( modelServer ) ) {

			if (msg.getId().equals(MessageID.START_INFO)) {

				propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
				zdm = (ZonalDataManager)msg.getValue( MessageID.ZONAL_DATA_MANAGER_KEY );
				tdm = (TODDataManager)msg.getValue( MessageID.TOD_DATA_MANAGER_KEY );
				zdmMap = (HashMap)msg.getValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY );
				tdmMap = (HashMap)msg.getValue( MessageID.STATIC_TOD_DATA_MAP_KEY );
				shadowPriceIter = Integer.parseInt((String)msg.getValue( MessageID.SHADOW_PRICE_ITER_KEY ));
				processorId = Integer.parseInt((String)msg.getValue( MessageID.PROCESSOR_ID_KEY ));

				zdm.setStaticData ( zdmMap );
				tdm.setStaticData ( tdmMap );

				// create a dtmHH object
				if (LOGGING)
				    logger.info (this.getName() + " building dtmHH object for mandatory dc.");
			    dtmHH = new DTMHousehold ( processorId, propertyMap, TourType.MANDATORY_CATEGORY, TourType.MANDATORY_TYPES, zdm );
				dtmHH.setShadowPricingIteration(shadowPriceIter);
				dtmHH.resetHouseholdCount();
				if (LOGGING)
				    logger.info (this.getName() + " dtmHH object built for mandatory dc, asking for work.");


				Message sendWorkMsg = createMessage();
				sendWorkMsg.setId(MessageID.SEND_WORK);
				sendWorkMsg.setIntValue( MessageID.TOUR_CATEGORY_KEY, TourType.MANDATORY_CATEGORY );
				sendWorkMsg.setValue( MessageID.TOUR_TYPES_KEY, TourType.MANDATORY_TYPES );
				sendTo( "HhArrayServer", sendWorkMsg );

			}
			else if ( msg.getId().equals( MessageID.RELEASE_MEMORY ) || msg.getId().equals( MessageID.EXIT ) ) {

			    if (LOGGING)
			        logger.info (this.getName() + " releasing memory after getting " + msg.getId() + " from " + msg.getSender() );
			    
				dtmHH = null;
				zdm = null;
				tdm = null;
				zdmMap = null;
				tdmMap = null;
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

					// run the Dc model for the mandatory tours in these households
					if (LOGGING)
					    logger.info ( this.getName() + " processing household ids: " + hhList[0].getID() + " to " + hhList[hhList.length-1].getID() );
					for (int i=0; i < hhList.length; i++) {
						try {

						    hhList[i].setProcessorId (processorId);		
						    
							logger.info("in dc worker, hh walk access="+hhList[i].getOriginWalkSegment());
						    
						    dtmHH.mandatoryTourDc (hhList[i]);
						}
						catch (java.lang.Exception e) {
						    e.printStackTrace();
							logger.fatal ("runtime exception occurred in mandatory DC for household id=" + hhList[i].getID() + "in " + this.getName() );
							logger.fatal(e.getMessage());
							hhList[i].writeContentToLogger(logger);
							System.exit(-1);
						}
					}


					returnValue = MessageID.RESULTS_ID;

				}
				else {

					dtmHH.printTimes ( TourType.MANDATORY_CATEGORY );

					returnValue = MessageID.FINISHED_ID;

				}

			}

		}

		return returnValue;

	}



	private Message createFinishedMessage () {

		dtmHH.clearProbabilitiesMaps( TourType.MANDATORY_TYPES );

		Message msg = createMessage();
		msg.setId( MessageID.FINISHED );
		sendTo( modelServer, msg );

		return null;
	}



	private Message createResultsMessage () {

		Message newMessage = createMessage();
		newMessage.setId( MessageID.RESULTS );
		newMessage.setIntValue( MessageID.TOUR_CATEGORY_KEY, TourType.MANDATORY_CATEGORY );
		newMessage.setValue( MessageID.TOUR_TYPES_KEY, TourType.MANDATORY_TYPES );
		newMessage.setValue( MessageID.HOUSEHOLD_LIST_KEY, hhList );

		return newMessage;
	}

}
