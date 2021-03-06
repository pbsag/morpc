package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * worker class for running individual non-mandatory destination, time-of-day and
 * mode choice for a list of tours in a distributed environment.
 */


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.morpc.models.DTMHousehold;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.models.TODDataManager;
import com.pb.morpc.structures.Household;

import com.pb.morpc.structures.SummitAggregationRecord;
import com.pb.morpc.util.VectorToArrayConvertor;

import com.pb.morpc.structures.TourType;

import java.util.HashMap;
import java.util.Vector;
import org.apache.log4j.Logger;



public class IndivDTMWorker extends MessageProcessingTask implements java.io.Serializable {

	private static boolean LOGGING = true;
	
	private static Logger logger = Logger.getLogger("com.pb.morpc.models");

	private Household[] hhList = null;
	private DTMHousehold dtmHH = null;

	private ZonalDataManager zdm = 	null;
	private TODDataManager tdm = 	null;
	private HashMap propertyMap = null;
	private HashMap zdmMap = null;
	private HashMap tdmMap = null;

	private String modelServer = "IndivDTMModelServer";

	private int processorIndex = 0;
	
	//Wu added for Summit Aggregation
	private SummitAggregationRecord [] summitAggregationArray;
    private boolean FtaRestartRun = false;
	private boolean writeSummitAggregationFields;


    public IndivDTMWorker () {
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
		
		propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
		
	    if(propertyMap==null){
	    	logger.fatal("IndivDTMWorker onMessage, no propertyMap included in this message.");
	    }

		if (LOGGING)
		    logger.info( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );

	    Message newMessage = null;


		//Do some work ...
		int messageReturnType = respondToMessage(msg);

		if ( messageReturnType > 0 ) {

			//Send a response back to the server
			if (messageReturnType == MessageID.RESULTS_ID)
				newMessage = createResultsMessage();
			//Wu added for Summit Aggregation
			else if (messageReturnType == MessageID.SUMMIT_AGGREGATION_ID){
				//logger.info("in TcMcWorker before create result message.");
				newMessage = createSummitAggregationMessage();
			}
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

				//propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
				zdm = (ZonalDataManager)msg.getValue( MessageID.ZONAL_DATA_MANAGER_KEY );
				tdm = (TODDataManager)msg.getValue( MessageID.TOD_DATA_MANAGER_KEY );
				zdmMap = (HashMap)msg.getValue( MessageID.STATIC_ZONAL_DATA_MAP_KEY );
				tdmMap = (HashMap)msg.getValue( MessageID.STATIC_TOD_DATA_MAP_KEY );
				processorIndex = Integer.parseInt((String)msg.getValue( MessageID.PROCESSOR_ID_KEY ));

				zdm.setStaticData ( zdmMap );
				tdm.setStaticData ( tdmMap );

		        // determine whether this model run is for an FTA Summit analysis or not
		        if( (String)propertyMap.get("FTA_Restart_run") != null )
		        	FtaRestartRun = ((String)propertyMap.get("FTA_Restart_run")).equalsIgnoreCase("true");
		        else
		        	FtaRestartRun = false;

		        
				//Wu added for Summit Aggregation
				if( ((String)propertyMap.get("writeSummitAggregationFields")) != null )
					writeSummitAggregationFields = ((String)propertyMap.get("writeSummitAggregationFields")).equalsIgnoreCase("true");
				else
					writeSummitAggregationFields = false;

				

				// create a dtmHH object
				if (LOGGING)
				    logger.info (this.getName() + " building dtmHH object for individual non-mandatory dtm for PINDEX=" + processorIndex);
				if (dtmHH == null)
				    dtmHH = new DTMHousehold ( processorIndex, propertyMap, TourType.NON_MANDATORY_CATEGORY, TourType.NON_MANDATORY_TYPES );
				if (LOGGING)
				    logger.info (this.getName() + " dtmHH object built for individual non-mandatory dtm, asking for work.");


				Message sendWorkMsg = createMessage();
				sendWorkMsg.setId(MessageID.SEND_WORK);
				sendWorkMsg.setIntValue( MessageID.TOUR_CATEGORY_KEY, TourType.NON_MANDATORY_CATEGORY );
				sendWorkMsg.setValue( MessageID.TOUR_TYPES_KEY, TourType.NON_MANDATORY_TYPES );
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

				//Wu added for Summit Aggregation
				Vector summitRecords=new Vector();
				Vector currentSummitRecords=null;
				VectorToArrayConvertor convertor=null;

				// if the list is null, no more hhs left to process;
				// otherwise, put the household objects from the message into an array for processing.
				if ( hhList != null ) {

					// run the dtm models for the individual non-mandatory tours in these households
					if (LOGGING)
					    logger.info ( this.getName() + " processing household ids: " + hhList[0].getID() + " to " + hhList[hhList.length-1].getID() );

					
					for (int i=0; i < hhList.length; i++) {
						try {

							hhList[i].setProcessorIndex (processorIndex);
							//Wu added for FTA restart
							//do if FTA_Restart_run is false, otherwise skip DC and TC
							if( !FtaRestartRun ){
								dtmHH.resetHouseholdCount();
								
								//logger.info("in indiDTMWorker, before dc , hh walk access="+hhList[i].getOriginWalkSegment());
								
								dtmHH.indivNonMandatoryTourDc (hhList[i]);
								dtmHH.resetHouseholdCount();
								
								//logger.info("in indiDTMWorker, before tc , hh walk access="+hhList[i].getOriginWalkSegment());
								
								dtmHH.indivNonMandatoryTourTc (hhList[i]);
							}
							dtmHH.resetHouseholdCount();
							
							//logger.info("in indiDTMWorker, before mc , hh walk access="+hhList[i].getOriginWalkSegment());
							
							dtmHH.indivNonMandatoryTourMc (hhList[i]);
							
							//Wu added for Summit Aggregation
							if(writeSummitAggregationFields){
								currentSummitRecords=dtmHH.getSummitAggregationRecords();
								summitRecords.addAll(currentSummitRecords);
							}
							//logger.info("in TcMcWorker before create logsum records.");

						}
						catch (java.lang.Exception e) {
							logger.fatal ("runtime exception occurred in indiv non-mandatory dtm for household id=" + hhList[i].getID() + "in " + this.getName() );
							logger.fatal(e.getMessage());
							hhList[i].writeContentToLogger(logger);
						    e.printStackTrace();
							System.exit(-1);
						}
					}
					
					//Wu added for Summit Aggregation
					if(writeSummitAggregationFields){
						//logger.info("in indivDTMWorker, original records="+summitRecords.size());
						convertor=new VectorToArrayConvertor(summitRecords);
						summitAggregationArray=convertor.getSummitAggregationArray();
						//logger.info("converted records="+summitAggregationArray.length);
						returnValue = MessageID.SUMMIT_AGGREGATION_ID;
					}else{
						returnValue = MessageID.RESULTS_ID;
					}

				}
				else {

					dtmHH.printTimes ( TourType.NON_MANDATORY_CATEGORY );

					returnValue = MessageID.FINISHED_ID;

				}

			}

        }

		return returnValue;

	}



	private Message createFinishedMessage () {

		dtmHH.clearProbabilitiesMaps( TourType.NON_MANDATORY_TYPES );

		Message msg = createMessage();
		msg.setId( MessageID.FINISHED );
		sendTo( modelServer, msg );

		return null;
	}



	private Message createResultsMessage () {

		Message newMessage = createMessage();
		newMessage.setId( MessageID.RESULTS);
		newMessage.setIntValue( MessageID.TOUR_CATEGORY_KEY, TourType.NON_MANDATORY_CATEGORY );
		newMessage.setValue( MessageID.TOUR_TYPES_KEY, TourType.NON_MANDATORY_TYPES );
		newMessage.setValue( MessageID.HOUSEHOLD_LIST_KEY, hhList );
		return newMessage;
	}
	
	//Wu added for Summit Aggregation
	private Message createSummitAggregationMessage () {

		Message newMessage = createMessage();	
		newMessage.setId( MessageID.SUMMIT_AGGREGATION);
		newMessage.setIntValue( MessageID.TOUR_CATEGORY_KEY, TourType.MANDATORY_CATEGORY );
		newMessage.setValue( MessageID.TOUR_TYPES_KEY, TourType.MANDATORY_TYPES );
		//still need to send hhList back
		newMessage.setValue( MessageID.HOUSEHOLD_LIST_KEY, hhList );
		newMessage.setValue(MessageID.SUMMIT_LIST_KEY, summitAggregationArray);
		return newMessage;
	}

}
