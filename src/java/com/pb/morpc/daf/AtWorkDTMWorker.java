package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * worker class for running at-work sub-tour destination, time-of-day and
 * mode choice for a list of households in a distributed environment.
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



public class AtWorkDTMWorker extends MessageProcessingTask implements java.io.Serializable {

    private static boolean LOGGING = true;
	private static Logger logger = Logger.getLogger("com.pb.morpc.models");

	private Household[] hhList = null;
	private DTMHousehold dtmHH = null;

	private ZonalDataManager zdm = 	null;
	private TODDataManager tdm = 	null;
	private HashMap propertyMap = null;
	private HashMap zdmMap = null;
	private HashMap tdmMap = null;

	private String modelServer = "AtWorkDTMModelServer";

	private int processorIndex = 0;
	
	//Wu added for Summit Aggregation
	private SummitAggregationRecord [] summitAggregationArray;
    private boolean FtaRestartRun = false;
	private boolean writeSummitAggregationFields;


    public AtWorkDTMWorker () {
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
	    	logger.fatal("AtWorkDTMWorker onMessage, no propertyMap included in this message.");
	    }

		if (LOGGING)
		    logger.debug( this.name +  " onMessage() id=" + msg.getId() + ", sent by " + msg.getSender() + "." );

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
				    logger.info (this.getName() + " building dtmHH object for atwork dtm for PINDEX=" + processorIndex);

				if (dtmHH == null)
				    dtmHH = new DTMHousehold ( processorIndex, propertyMap, TourType.AT_WORK_CATEGORY, TourType.AT_WORK_TYPES );

				if (LOGGING)
				    logger.info (this.getName() + " dtmHH object built for atwork dtm, asking for work.");


				Message sendWorkMsg = createMessage();
				sendWorkMsg.setId(MessageID.SEND_WORK);
				sendWorkMsg.setIntValue( MessageID.TOUR_CATEGORY_KEY, TourType.AT_WORK_CATEGORY );
				sendWorkMsg.setValue( MessageID.TOUR_TYPES_KEY, TourType.AT_WORK_TYPES );
				sendTo( "HhArrayServer", sendWorkMsg );

			}
			else if ( msg.getId().equals( MessageID.RELEASE_MEMORY ) ) {

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
			else if ( msg.getId().equals( MessageID.RELEASE_MATRIX_MEMORY ) ) {

			    /*
				if (LOGGING)
					logger.info (this.getName() + " releasing UEC matrix memory after getting " + msg.getId() + " from " + msg.getSender() );

				com.pb.common.calculator.UtilityExpressionCalculator.clearData();
                */

			    
			}
			else if ( msg.getId().equals( MessageID.EXIT ) ) {

				if (LOGGING)
					logger.info (this.getName() + " message: " + msg.getId() + " from " + msg.getSender() );
			    
				//com.pb.common.calculator.UtilityExpressionCalculator.clearData();
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

					// run the dtm models for the atwork subtours in these households
					if (LOGGING)
					    logger.info ( this.getName() + " processing household ids: " + hhList[0].getID() + " to " + hhList[hhList.length-1].getID() );
					
					for (int i=0; i < hhList.length; i++) {
						try {

							hhList[i].setProcessorIndex (processorIndex);
							//Wu added for FTA restart
							//do if FTA_Restart_run is false, otherwise skip DC and TC
							if( !FtaRestartRun ){
								dtmHH.resetHouseholdCount();
								
								//logger.info("in atworkDTMWorker, before dc , hh walk access="+hhList[i].getOriginWalkSegment());
								
								dtmHH.atWorkTourDc (hhList[i]);
								dtmHH.resetHouseholdCount();
								
								//logger.info("in atworkDTMWorker, before tc , hh walk access="+hhList[i].getOriginWalkSegment());
								
								dtmHH.atWorkTourTc (hhList[i]);
							}
							dtmHH.resetHouseholdCount();
							
							//logger.info("in atworkDTMWorker, before mc , hh walk access="+hhList[i].getOriginWalkSegment());
							
							dtmHH.atWorkTourMc (hhList[i]);
							
							//Wu added for Summit Aggregation
							if(writeSummitAggregationFields){
								currentSummitRecords=dtmHH.getSummitAggregationRecords();
								summitRecords.addAll(currentSummitRecords);
							}
							//logger.info("in TcMcWorker before create logsum records.");
						}
						catch (java.lang.Exception e) {
							logger.fatal ("runtime exception occurred in at-work dtm for household id=" + hhList[i].getID() + " in " + this.getName() );
							logger.fatal(e.getMessage());
                            hhList[i].writeContentToLogger(logger);
							e.printStackTrace();
                            System.exit(-1);
						}
					}
					//Wu added for Summit Aggregation
					if(writeSummitAggregationFields){
						//logger.info("in AtWorkWorker, original records="+summitRecords.size());
						convertor=new VectorToArrayConvertor(summitRecords);
						summitAggregationArray=convertor.getSummitAggregationArray();
						//logger.info("converted records="+summitAggregationArray.length);
						returnValue = MessageID.SUMMIT_AGGREGATION_ID;
					}else{
						returnValue = MessageID.RESULTS_ID;
					}

				}
				else {

					dtmHH.printTimes ( TourType.AT_WORK_CATEGORY );

					returnValue = MessageID.FINISHED_ID;

				}

			}

		}

		return returnValue;

	}



	private Message createFinishedMessage () {

		dtmHH.clearProbabilitiesMaps( TourType.AT_WORK_TYPES );

		Message msg = createMessage();
		msg.setId( MessageID.FINISHED );
		sendTo( modelServer, msg );

		return null;
	}



	private Message createResultsMessage () {

		Message newMessage = createMessage();
		newMessage.setId( MessageID.RESULTS);
		newMessage.setIntValue( MessageID.TOUR_CATEGORY_KEY, TourType.AT_WORK_CATEGORY );
		newMessage.setValue( MessageID.TOUR_TYPES_KEY, TourType.AT_WORK_TYPES );
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
