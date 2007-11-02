package com.pb.morpc.models;

import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.*;

import com.pb.morpc.synpop.SyntheticPopulation;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;
import com.pb.common.model.ChoiceModelApplication;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;
import java.io.*;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */
public class DTMOutput implements java.io.Serializable {

    static Logger logger = Logger.getLogger("com.pb.morpc.models");

    static final String[] purposeName = { "", "man", "nonman" };
    static final String[] accessMode = { "", "wt", "dt", "td" };
    static final String[] primaryMode = { "", "hwy", "transit", "nonmotor" };
    static final String[] periodName = { "", "am", "pm", "md", "nt" };        

    static final int WT = 1; 
    static final int DT = 2; 
    static final int TD = 3; 


	int numberOfZones;
	
	HashMap propertyMap;
	boolean useMessageWindow = false;
	MessageWindow mw;

	IndexValues index = new IndexValues();



    public DTMOutput (HashMap propertyMap, ZonalDataManager zdm) {

		this.propertyMap = propertyMap;

		// get the indicator for whether to use the message window or not
		// from the properties file.
		String useMessageWindowString = (String)propertyMap.get( "MessageWindow");
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				useMessageWindow = true;
				this.mw = new MessageWindow ( "MORPC Tour Destination, Time of Day, and Mode Choice Models" );
			}
		}


		this.numberOfZones = zdm.getNumberOfZones();

		logger.info ("done with DTMOutput constructor.");
		
    }




    public void writeTripTables ( Household[] hh ) { 
    
        float[][] vocRatios = getVehOccRatios ();

        // get directoy name in which to write tpplus files from properties file
        String outputDirectory = (String)propertyMap.get( "TripsDirectory.tpplus");


        // loop over types - mandatory, non-mandatory
        for ( int type=1; type <= 2; type++ ) {

            // loop over periods - am, pm, md, nt
            for ( int period=1; period <= 4; period++ ) {

                float[][][] trips = null;
                
                // write the highway mode trip tables ...
                int primaryModeIndex = 1;

                // construct filename
                String tppFileName = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[primaryModeIndex] + periodName[period] + ".tpp";
                
                String[] hNames = { "sov person", "sov vehicle", "hov person", "hov vehicle" };
                String[] hDescriptions = { String.format("%s %s sov person trips", purposeName[type], periodName[period]),
                        String.format("%s %s sov vehicle trips", purposeName[type], periodName[period]),
                        String.format("%s %s hov person trips", purposeName[type], periodName[period]),
                        String.format("%s %s hov vehicle trips", purposeName[type], periodName[period]) };
                trips = getHwyTripTables ( hh, vocRatios, period, type );
                writeTpplusMatrices ( tppFileName, trips, hNames, hDescriptions );

                
                
                // write the walk transit mode trip tables ...
                int accessModeIndex = 1;
                primaryModeIndex = 2;
                String[] subModeNames = { "wt lbs", "wt ebs", "wt brt", "wt lrt", "wt crl", "dt lbs", "dt ebs", "dt brt", "dt lrt", "dt crl", "td lbs", "td ebs", "td brt", "td lrt", "td crl" };

                
                // construct filename
                tppFileName = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[primaryModeIndex] + "_" + accessMode[accessModeIndex] + "_" + periodName[period] + ".tpp";

                String[] wtNames = { subModeNames[0], subModeNames[1], subModeNames[2], subModeNames[3], subModeNames[4] };

                String[] wtDescriptions = { String.format("%s %s %s person trips", purposeName[type], periodName[period], wtNames[0]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], wtNames[1]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], wtNames[2]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], wtNames[3]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], wtNames[4]) };

                trips = getTransitTripTables ( hh, period, type, accessModeIndex );
                writeTpplusMatrices ( tppFileName, trips, wtNames, wtDescriptions );

                
                
                
                // write the drive transit mode trip tables ...
                accessModeIndex = 2;

                // construct filename
                tppFileName = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[primaryModeIndex] + "_" + accessMode[accessModeIndex] + "_" + periodName[period] + ".tpp";
                
                String[] dtNames = { subModeNames[5], subModeNames[6], subModeNames[7], subModeNames[8], subModeNames[9] };

                String[] dtDescriptions = { String.format("%s %s %s person trips", purposeName[type], periodName[period], dtNames[0]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], dtNames[1]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], dtNames[2]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], dtNames[3]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], dtNames[4]) };

                trips = getTransitTripTables ( hh, period, type, accessModeIndex );
                writeTpplusMatrices ( tppFileName, trips, dtNames, dtDescriptions );

                
                
                
                // write the transit drive mode trip tables ...
                accessModeIndex = 3;
                             
                // construct filename
                tppFileName = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[primaryModeIndex] + "_" + accessMode[accessModeIndex] + "_" + periodName[period] + ".tpp";
                
                String[] tdNames = { subModeNames[10], subModeNames[11], subModeNames[12], subModeNames[13], subModeNames[14] };

                String[] tdDescriptions = { String.format("%s %s %s person trips", purposeName[type], periodName[period], tdNames[0]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], tdNames[1]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], tdNames[2]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], tdNames[3]),
                        String.format("%s %s %s person trips", purposeName[type], periodName[period], tdNames[4]) };

                trips = getTransitTripTables ( hh, period, type, accessModeIndex );
                writeTpplusMatrices ( tppFileName, trips, tdNames, tdDescriptions );

                
                
                
                // write the non-motorized mode trip tables ...
                primaryModeIndex = 3;

                // construct filename
                tppFileName = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[primaryModeIndex] + "_" + periodName[period] + ".tpp";
                
                String[] nmNames = { "non-motorized" };

                String[] nmDescriptions = { String.format("%s %s %s person trips", purposeName[type], periodName[period], nmNames[0]) };

                float[][][] nmTrips = new float[1][][];
                nmTrips[0] = getNmTripTables ( hh, period, type );
                writeTpplusMatrices ( tppFileName, nmTrips, nmNames, nmDescriptions );
                
            }
            
        }
            
    }

    

    private float[][][] getHwyTripTables ( Household[] hh, float[][] vocRatios, int period, int type ) {

        float[][] sovPerson = new float[numberOfZones][numberOfZones];
        float[][] sovVehicle = new float[numberOfZones][numberOfZones];
        float[][] hovPerson = new float[numberOfZones][numberOfZones];
        float[][] hovVehicle = new float[numberOfZones][numberOfZones];

        Tour[] tours = null;
        
        
        // loop through tours in Household objects and look for tours with highway primary modes - sov, hov.
        for (int i=0; i < hh.length; i++) {

            // if tour mode is sov or hov, pass tour object and trip table arrays into
            // method to accumulate person and vehicle highway trips, if trips begin in the passed in period.

            try {
                
                // Mandatory tours ...
                if ( type == 1 ) {
                    tours = hh[i].getMandatoryTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            int tourMode = tours[t].getMode();
                            if ( tourMode == TourModeType.SOV || tourMode == TourModeType.HOV ) 
                                accumulateHighwayTripsForTour ( tours[t], tourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
                        }
                    }
                }
                else if ( type == 2 ) {
                    // Joint tours ...
                    tours = hh[i].getJointTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            // if tour mode is sov or hov, pass tour object and trip table arrays into
                            // method to accumulate person and vehicle highway trips, if any.
                            int tourMode = tours[t].getMode();
                            if ( tourMode == TourModeType.SOV || tourMode == TourModeType.HOV ) 
                                accumulateHighwayTripsForTour ( tours[t], tourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
                        }
                    }
        
                    // Individual non-mandatory tours ...
                    tours = hh[i].getIndivTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            // if tour mode is sov or hov, pass tour object and trip table arrays into
                            // method to accumulate person and vehicle highway trips, if any.
                            int tourMode = tours[t].getMode();
                            if ( tourMode == TourModeType.SOV || tourMode == TourModeType.HOV ) 
                                accumulateHighwayTripsForTour ( tours[t], tourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
                        }
                    }
        
                    // at-work sub-tours ...
                    tours = hh[i].getMandatoryTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            if (tours[t].getTourType() == TourType.WORK) {
                                Tour[] subTours = tours[t].getSubTours();
                                if (subTours != null) {
                                    for (int s=0; s < subTours.length; s++) {
        
                                        // if tour mode is sov or hov, pass tour object and trip table arrays into
                                        // method to accumulate person and vehicle highway trips, if any.
                                        int subTourMode = subTours[s].getMode();
                                        if ( subTourMode == TourModeType.SOV || subTourMode == TourModeType.HOV ) 
                                            accumulateHighwayTripsForTour ( subTours[s], subTourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
                                        
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (RuntimeException e) {
                logger.fatal ("Caught runtime exception processing hhid" + i);
                throw e;
            }
            
        }

        
        float[][][] result = new float[4][][];
        result[0] = sovPerson;
        result[1] = hovPerson;
        result[2] = sovVehicle;
        result[3] = hovVehicle;
        
        return result;

    }
    


    private float[][][] getTransitTripTables ( Household[] hh, int period, int type, int access ) {

        float[][][] transitPerson = new float[SubmodeType.TYPES][numberOfZones][numberOfZones];

        Tour[] tours = null;
        
        
        // loop through tours in Household objects and look for tours with transit tour modes - WT, DT.
        for (int i=0; i < hh.length; i++) {

            // pass tour object and trip table arrays into method to accumulate transit person trips
            // by trip mode and by accessMode, if trips begin in the passed in period.

            try {
                
                
                // Mandatory tours ...
                if ( type == 1 ) {
                    
                    tours = hh[i].getMandatoryTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            int tourMode = tours[t].getMode();
                            if ( tourMode == TourModeType.WALKTRANSIT || tourMode == TourModeType.DRIVETRANSIT )
                                accumulateTransitTripsForTour ( tours[t], tourMode, period, access, transitPerson );
                        }
                    }
                    
                }
                else if ( type == 2 ) {

                    // Joint tours ...
                    tours = hh[i].getJointTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            // if tour mode is wt or dt, pass tour object and trip table arrays into
                            // method to accumulate person transit trips, if any.
                            int tourMode = tours[t].getMode();
                            if ( tourMode == TourModeType.WALKTRANSIT || tourMode == TourModeType.DRIVETRANSIT )
                                accumulateTransitTripsForTour ( tours[t], tourMode, period, access, transitPerson );
                        }
                    }

                    // Individual non-mandatory tours ...
                    tours = hh[i].getIndivTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            // if tour mode is sov or hov, pass tour object and trip table arrays into
                            // method to accumulate person and vehicle highway trips, if any.
                            int tourMode = tours[t].getMode();
                            if ( tourMode == TourModeType.WALKTRANSIT || tourMode == TourModeType.DRIVETRANSIT )
                                accumulateTransitTripsForTour ( tours[t], tourMode, period, access, transitPerson );
                        }
                    }

                    // at-work sub-tours ...
                    tours = hh[i].getMandatoryTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            if (tours[t].getTourType() == TourType.WORK) {
                                Tour[] subTours = tours[t].getSubTours();
                                if (subTours != null) {
                                    for (int s=0; s < subTours.length; s++) {

                                        // if tour mode is sov or hov, pass tour object and trip table arrays into
                                        // method to accumulate person and vehicle highway trips, if any.
                                        int subTourMode = subTours[s].getMode();
                                        if ( subTourMode == TourModeType.WALKTRANSIT || subTourMode == TourModeType.DRIVETRANSIT )
                                            accumulateTransitTripsForTour ( subTours[s], subTourMode, period, access, transitPerson );
                                        
                                    }
                                }
                            }
                        }
                    }
                }
                
            }
            catch (RuntimeException e) {
                logger.fatal ("Caught runtime exception processing hhid" + i);
                throw e;
            }
                
        }

        
        return transitPerson;

    }
    


    private float[][] getNmTripTables ( Household[] hh, int period, int type ) {

        float[][] result = new float[numberOfZones][numberOfZones];

        Tour[] tours = null;
        
        
        // loop through tours in Household objects and accumulate non-motorized trips from tours with non-motorized tour segments.
        for (int i=0; i < hh.length; i++) {

            // pass tour object and trip table arrays into method to accumulate non-motorized person trips
            // if trips begin in the passed in period.

            try {
                
                
                // Mandatory tours ...
                if ( type == 1 ) {
                    
                    tours = hh[i].getMandatoryTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            accumulateNmTripsForTour ( tours[t], period, result );
                        }
                    }
                    
                }
                else if ( type == 2 ) {

                    // Joint tours ...
                    tours = hh[i].getJointTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            accumulateNmTripsForTour ( tours[t], period, result );
                        }
                    }

                    // Individual non-mandatory tours ...
                    tours = hh[i].getIndivTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            accumulateNmTripsForTour ( tours[t], period, result );
                        }
                    }

                    // at-work sub-tours ...
                    tours = hh[i].getMandatoryTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            if (tours[t].getTourType() == TourType.WORK) {
                                Tour[] subTours = tours[t].getSubTours();
                                if (subTours != null) {
                                    for (int s=0; s < subTours.length; s++) {
                                        accumulateNmTripsForTour ( subTours[s], period, result );
                                    }
                                }
                            }
                        }
                    }
                }
                
            }
            catch (RuntimeException e) {
                logger.fatal ("Caught runtime exception processing hhid" + i);
                throw e;
            }
                
        }

        
        return result;

    }
    


    private void accumulateHighwayTripsForTour ( Tour tour, int tourMode, float[][] vocRatios, int period, float[][] sovPerson, float[][] sovVehicle, float[][] hovPerson, float[][] hovVehicle ) {
        
        // check for valid tod alternative.
        int tod = tour.getTimeOfDayAlt();
        if (tod < 1)
            return;
            
        int periodOut = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod ( tod );
        int periodIn = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod ( tod );

        int tourType = tour.getTourType();
        int tripPark = tour.getChosenPark();
        int tripOrigOB = tour.getOrigTaz();
        int tripDestIB = tour.getOrigTaz();
        
        int tripDestOB;
        int tripOrigIB;
        if (tripPark > 0) {
            tripDestOB = tripPark;
            tripOrigIB = tripPark;
        }
        else {
            tripDestOB = tour.getDestTaz();
            tripOrigIB = tour.getDestTaz();
        }
        
        // check for valid O/D info.
        if (tripOrigOB < 1 || tripOrigIB < 1 || tripDestOB < 1 || tripDestIB < 1)
            return;

        int tripStopOB;
        int tripStopIB;
        tripStopOB = tour.getStopLocOB();
        tripStopIB = tour.getStopLocIB();

        // check for valid stop location info.
        if (tripStopOB < 0 || tripStopIB < 0)
            return;

        
        if ( periodOut == period ) {
    
            float vocRatioOB = vocRatios[tourType][periodOut];

            if (tripStopOB > 0) {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigOB-1][tripStopOB-1]++;
                    sovPerson[tripStopOB-1][tripDestOB-1]++;
                    sovVehicle[tripOrigOB-1][tripStopOB-1] += 1;
                    sovVehicle[tripStopOB-1][tripDestOB-1] += 1;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigOB-1][tripStopOB-1]++;
                    hovPerson[tripStopOB-1][tripDestOB-1]++;
                    hovVehicle[tripOrigOB-1][tripStopOB-1] += 1.0/vocRatioOB;
                    hovVehicle[tripStopOB-1][tripDestOB-1] += 1.0/vocRatioOB;
                }
            }
            else {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigOB-1][tripDestOB-1]++;
                    sovVehicle[tripOrigOB-1][tripDestOB-1] += 1;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigOB-1][tripDestOB-1]++;
                    hovVehicle[tripOrigOB-1][tripDestOB-1] += 1.0/vocRatioOB;
                }
            }

        }
    
        
        if ( periodIn == period ) {

            float vocRatioIB = vocRatios[tourType][periodIn];

            if (tripStopIB > 0) {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigIB-1][tripStopIB-1]++;
                    sovPerson[tripStopIB-1][tripDestIB-1]++;
                    sovVehicle[tripOrigIB-1][tripStopIB-1] += 1;
                    sovVehicle[tripStopIB-1][tripDestIB-1] += 1;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigIB-1][tripStopIB-1]++;
                    hovPerson[tripStopIB-1][tripDestIB-1]++;
                    hovVehicle[tripOrigIB-1][tripStopIB-1] += 1.0/vocRatioIB;
                    hovVehicle[tripStopIB-1][tripDestIB-1] += 1.0/vocRatioIB;
                }
            }
            else {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigIB-1][tripDestIB-1]++;
                    sovVehicle[tripOrigIB-1][tripDestIB-1] += 1;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigIB-1][tripDestIB-1]++;
                    hovVehicle[tripOrigIB-1][tripDestIB-1] += 1.0/vocRatioIB;
                }
            }

        }
    
    }
    

    
    private void accumulateTransitTripsForTour ( Tour tour, int tourMode, int period, int access, float[][][] transitPerson ) {

        // transitPerson array is dimensioned as one of the following, depending on access type, with indices defined:
        // WT:  0=WT_LBS, 1=WT_EBS, 2=WT_BRT, 3=WT_LRT, 4=WT_CRL
        // DT:  0=DT_LBS, 1=DT_EBS, 2=DT_BRT, 3=DT_LRT, 4=DT_CRL
        // TD:  0=TD_LBS, 1=TD_EBS, 2=TD_BRT, 3=TD_LRT, 4=TD_CRL
        // [nTazs]:  origin TAZ: number of TAZS
        // [nTazs]:  destination TAZ: number of TAZS

        
        // check for valid tod alternative.
        int tod = tour.getTimeOfDayAlt();
        if (tod < 1)
            return;
            
        int periodOut = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod ( tod );
        int periodIn = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod ( tod );

        int tripPark = tour.getChosenPark();
        int tripOrigOB = tour.getOrigTaz();
        int tripDestIB = tour.getOrigTaz();
        
        int tripDestOB;
        int tripOrigIB;
        if (tripPark > 0) {
            tripDestOB = tripPark;
            tripOrigIB = tripPark;
        }
        else {
            tripDestOB = tour.getDestTaz();
            tripOrigIB = tour.getDestTaz();
        }
        
        // check for valid O/D info.
        if (tripOrigOB < 1 || tripOrigIB < 1 || tripDestOB < 1 || tripDestIB < 1)
            return;

        int tripStopOB;
        int tripStopIB;
        tripStopOB = tour.getStopLocOB();
        tripStopIB = tour.getStopLocIB();

        // check for valid stop location info.
        if (tripStopOB < 0 || tripStopIB < 0)
            return;

        
        int tripModeIk = tour.getTripIkMode();
        int tripModeKj = tour.getTripKjMode();
        int tripModeJk = tour.getTripJkMode();
        int tripModeKi = tour.getTripKiMode();
        int tourSubmodeOB = tour.getSubmodeOB();
        int tourSubmodeIB = tour.getSubmodeIB();
        
        
    
    
        int modeIndex = -1;

        if ( periodOut == period ) {

            if ( tripStopOB > 0 ) {

                // if the trip mode for either segment of this outbound half tour is SubmodeType.TYPES+1, the trip mode is nonmotorized;
                // if it's > SubmodeType.TYPES+1 or < 1, it's an error.
                
                if ( tripModeIk < 1 || tripModeIk > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for first segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripStopOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripStopOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKj < 1 || tripModeKj > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for second segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripStopOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripStopOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }

                
                // for driveTransit half-tours in outbound direction, ik segment is driveTransit and kj segment is walkTransit.
                if ( tourMode == TourModeType.DRIVETRANSIT ) {
                    // accumulate trip, if it's transit, for first segment only in DT table
                    if ( access == DT && tripModeIk <= SubmodeType.TYPES ) {
                        modeIndex = tripModeIk - 1;
                        transitPerson[modeIndex][tripOrigOB-1][tripStopOB-1]++;
                    }
                    // accumulate trip, if it's transit for second segment only in WT table
                    if ( access == WT && tripModeKj <= SubmodeType.TYPES ) {
                        modeIndex = tripModeKj - 1;
                        transitPerson[modeIndex][tripStopOB-1][tripDestOB-1]++;
                    }
                }
                // for walkTransit half-tours in outbound direction, both ik and kj segments are walkTransit.  
                else if ( tourMode == TourModeType.WALKTRANSIT ) {
                    // accumulate trips, if they're transit, for both segments in WT table.
                    if ( access == WT && tripModeIk <= SubmodeType.TYPES ) {
                        modeIndex = tripModeIk - 1;
                        transitPerson[modeIndex][tripOrigOB-1][tripStopOB-1]++;
                    }
                    if ( access == WT && tripModeKj <= SubmodeType.TYPES ) {
                        modeIndex = tripModeKj - 1;
                        transitPerson[modeIndex][tripStopOB-1][tripDestOB-1]++;
                    }
                }
                
            }
            else {
             
                if ( tourSubmodeOB < 1 || tourSubmodeOB > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tourSubmodeOB for outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripStopOB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripStopOB ) );
                    throw (new RuntimeException());
                }
                
                if ( tourMode == TourModeType.WALKTRANSIT ) {
                    // accumulate trip from half tour in WT table.
                    if ( access == WT && tourSubmodeOB <= SubmodeType.TYPES ) {
                        modeIndex = tourSubmodeOB - 1;
                        transitPerson[modeIndex][tripOrigOB-1][tripDestOB-1]++;
                    }
                }
                else if ( tourMode == TourModeType.DRIVETRANSIT ) {
                    // accumulate trip from half tour in DT table.
                    if ( access == DT && tourSubmodeOB <= SubmodeType.TYPES ) {
                        modeIndex = tourSubmodeOB - 1;
                        transitPerson[modeIndex][tripOrigOB-1][tripDestOB-1]++;
                    }
                }

            }

        }

    
        if ( periodIn == period ) {

            if (tripStopIB > 0) {

                // if the trip mode for either segment of this inbound half tour is SubmodeType.TYPES+1, the trip mode is nonmotorized;
                // if it's > SubmodeType.TYPES+1 or < 1, it's an error.
                
                if ( tripModeJk < 1 || tripModeJk > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for first segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripStopIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKi < 1 || tripModeKi > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for second segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripStopIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }


                if ( tourMode == TourModeType.DRIVETRANSIT ) {
                    // for driveTransit half-tours in inbound direction, jk segment is walkTransit and ki segment is driveTransit (TD - inbound direction).
                    // accumulate trip, if it's transit, for first segment only in WT table
                    if ( access == WT && tripModeJk <= SubmodeType.TYPES ) {
                        modeIndex = tripModeJk - 1;
                        transitPerson[modeIndex][tripOrigIB-1][tripStopIB-1]++;
                    }
                    // accumulate trip, if it's transit, for second segment only in TD table
                    if ( access == TD && tripModeKi <= SubmodeType.TYPES ) {
                        modeIndex = tripModeKi - 1;
                        transitPerson[modeIndex][tripStopIB-1][tripDestIB-1]++;
                    }
                }
                else if ( tourMode == TourModeType.WALKTRANSIT ) {
                    // for walkTransit half-tours in inbound direction, both ik and kj segments are walkTransit.  
                    // accumulate trips for both segments, if they're transit, in WT table.
                    if ( access == WT && tripModeJk <= SubmodeType.TYPES ) {
                        modeIndex = tripModeJk - 1;
                        transitPerson[modeIndex][tripOrigIB-1][tripStopIB-1]++;
                    }
                    if ( access == WT && tripModeKi <= SubmodeType.TYPES ) {
                        modeIndex = tripModeKi - 1;
                        transitPerson[modeIndex][tripStopIB-1][tripDestIB-1]++;
                    }
                }
                
            }
            else {
             
                if ( tourSubmodeIB < 1 || tourSubmodeIB > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tourSubmodeIB for inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripStopIB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB ) );
                    throw (new RuntimeException());
                }
                
                if ( tourMode == TourModeType.WALKTRANSIT ) {
                    // accumulate trip from the half tour in WT table.
                    if ( access == WT && tourSubmodeIB <= SubmodeType.TYPES ) {
                        modeIndex = tourSubmodeIB - 1;
                        transitPerson[modeIndex][tripOrigIB-1][tripDestIB-1]++;
                    }
                }
                else if ( tourMode == TourModeType.DRIVETRANSIT ) {
                    // accumulate trip from the half tour in TD table.
                    if ( access == TD && tourSubmodeIB <= SubmodeType.TYPES ) {
                        modeIndex = tourSubmodeIB - 1;
                        transitPerson[modeIndex][tripOrigIB-1][tripDestIB-1]++;
                    }
                }

            }

        }

    }
    
    

    private void accumulateNmTripsForTour ( Tour tour, int period, float[][] nmPerson ) {


        // check for valid tod alternative.
        int tod = tour.getTimeOfDayAlt();
        if (tod < 1)
            return;
            
        int periodOut = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod ( tod );
        int periodIn = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod ( tod );

        int tripPark = tour.getChosenPark();
        int tripOrigOB = tour.getOrigTaz();
        int tripDestIB = tour.getOrigTaz();
        
        int tripDestOB;
        int tripOrigIB;
        if (tripPark > 0) {
            tripDestOB = tripPark;
            tripOrigIB = tripPark;
        }
        else {
            tripDestOB = tour.getDestTaz();
            tripOrigIB = tour.getDestTaz();
        }
        
        // check for valid O/D info.
        if (tripOrigOB < 1 || tripOrigIB < 1 || tripDestOB < 1 || tripDestIB < 1)
            return;

        int tripStopOB;
        int tripStopIB;
        tripStopOB = tour.getStopLocOB();
        tripStopIB = tour.getStopLocIB();

        // check for valid stop location info.
        if (tripStopOB < 0 || tripStopIB < 0)
            return;

        
        int tripModeIk = tour.getTripIkMode();
        int tripModeKj = tour.getTripKjMode();
        int tripModeJk = tour.getTripJkMode();
        int tripModeKi = tour.getTripKiMode();
        int tourSubmodeOB = tour.getSubmodeOB();
        int tourSubmodeIB = tour.getSubmodeIB();
        
        int tourMode = tour.getMode();
    
    
        if ( periodOut == period ) {

            if ( tripStopOB > 0 ) {

                // if the trip mode for either segment of this outbound half tour is SubmodeType.TYPES+1, the trip mode is nonmotorized;
                // if it's > SubmodeType.TYPES+1 or < 1, it's an error.
                
                if ( tripModeIk < 1 || tripModeIk > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for first segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripStopOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripStopOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKj < 1 || tripModeKj > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for second segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripStopOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripStopOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }

                
                if ( tripModeIk == SubmodeType.TYPES + 1 )
                    nmPerson[tripOrigOB-1][tripStopOB-1]++;
                if ( tripModeKj == SubmodeType.TYPES + 1 )
                    nmPerson[tripStopOB-1][tripDestOB-1]++;
                
            }
            else {
             
                if ( tourSubmodeOB < 1 || tourSubmodeOB > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tourSubmodeOB for outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripStopOB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripStopOB ) );
                    throw (new RuntimeException());
                }
                
                if ( tourSubmodeOB == SubmodeType.TYPES + 1 )
                    nmPerson[tripOrigOB-1][tripDestOB-1]++;

            }

        }

    
        if ( periodIn == period ) {

            if (tripStopIB > 0) {

                // if the trip mode for either segment of this inbound half tour is SubmodeType.TYPES+1, the trip mode is nonmotorized;
                // if it's > SubmodeType.TYPES+1 or < 1, it's an error.
                
                if ( tripModeJk < 1 || tripModeJk > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for first segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripStopIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKi < 1 || tripModeKi > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tripMode for second segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripStopIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }


                if ( tripModeJk == SubmodeType.TYPES + 1 )
                    nmPerson[tripOrigIB-1][tripStopIB-1]++;
                if ( tripModeKi == SubmodeType.TYPES + 1 )
                    nmPerson[tripStopIB-1][tripDestIB-1]++;
                
            }
            else {
             
                if ( tourSubmodeIB < 1 || tourSubmodeIB > SubmodeType.TYPES+1 ) {
                    logger.fatal( "invalid tourSubmodeIB for inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripStopIB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB ) );
                    throw (new RuntimeException());
                }
                
                if ( tourSubmodeIB == SubmodeType.TYPES + 1 )
                    nmPerson[tripOrigIB-1][tripDestIB-1]++;

            }

        }

    }
    
    

    private void writeTpplusMatrices ( String tppFileName, float[][][] trips, String[] names, String[] descriptions ) {

        Matrix[] outputMatrices = new Matrix[trips.length];
       
        logger.info( String.format("matrix total for tables in %s:", tppFileName) );
        for (int i=0; i < trips.length; i++) {
            outputMatrices[i] = new Matrix( names[i], descriptions[i], trips[i] );
            trips[i] = null;
            logger.info( String.format("    [%d] %-16s: %.0f", i, names[i], outputMatrices[i].getSum()) );
        }
        MatrixWriter tppWriter = MatrixWriter.createWriter (MatrixType.TPPLUS, new File( tppFileName ) );
        tppWriter.writeMatrices(names, outputMatrices);

    }



	public void writeTableRowSums ( Matrix outputMatrix ) {

		float rowSum = 0.0f;
		
		for (int i=1; i <= outputMatrix.getRowCount(); i++) {
			rowSum = outputMatrix.getRowSum( i );
			logger.info( "rowsum[" + i + "] = " +  rowSum);				
		}
	}



	public void writeDTMOutput ( Household[] hh ) {

		String modeName[] = { "", "sov", "hov", "walktran", "drivtran", "nonmotor", "schoolbus" };        
		String tlPurposeName[] = { "", "1 Work-low", "1 Work-med", "1 Work-high", "2 University", "3 School", "4 Escorting", "5 Shopping - ind", "5 Shopping - joint", "6 Maintenance - ind", "6 Maintenance - joint", "7 Discretionary - ind", "7 Discretionary - joint", "8 Eating out - ind", "8 Eating out - joint", "9 At work" };        

        int hhCount=0;
        
		int k = 0;
		int m = 0;
		int t = 0;
		
		int hh_id;
        int serialno;
		int hh_taz_id;
		int tlIndex;
		
		int[] totTours = new int[15 + 1];
		int[][] modalTours = new int[15 + 1][7];
		float[] totDist = new float[15 + 1];
		
		int[] distSample = new int[33+1];
		Arrays.fill ( distSample, 1 );
		
		double[] resultsOB = new double[33];
		double[] resultsIB = new double[33];
		double[] resultsPark = new double[33];
				
		JointTour[] jt;
		Tour[] it;
		Tour[] st;
		
        ArrayList tableHeadings = null;
        ArrayList tableFormats = null;
		float[] tableData = null;
        String fieldFormat = null;
        
        
		int[] tripsByMode = new int[7];
		
		Household tempHH = null;
		
		PrintWriter outStream = null;
		

		//check to see if summary report has been requested.  If so, create the data
		//table and call the .logColumnFreqReport.  If not, check to see
		// if outputFile is defined, and if so create the data table
		// and write model results to it.

	    
	    
		String outputFileDTM = (String)propertyMap.get( "Model567.outputFile");

	    
		logger.info ("writing DTMS output csv file.");


		try {
		    
			if (outputFileDTM != null) {
		        // open output stream for DTM output file
				outStream = new PrintWriter (new BufferedWriter( new FileWriter(outputFileDTM) ) );
			}

        }
        catch (IOException e) {
            logger.fatal( String.format( "could not open file %s." ), e );
        }

        
        
        try {

			ChoiceModelApplication distc =  new ChoiceModelApplication("Model10.controlFile", 1,  0, propertyMap, Household.class);
			UtilityExpressionCalculator distUEC = distc.getUEC();
			int maxPartySize = 0;
			for (int i=0; i < hh.length; i++) {
				if (hh[i].jointTours != null) {
					for (int j=0; j < hh[i].jointTours.length; j++) {
						if (hh[i].jointTours[j].getNumPersons() > maxPartySize)
							maxPartySize = hh[i].jointTours[j].getNumPersons();
					}
				}
			}
	

	
			tableHeadings = new ArrayList();
			tableHeadings.add(SyntheticPopulation.HHID_FIELD);
            tableHeadings.add(SyntheticPopulation.SERIALNO_FIELD);
            tableHeadings.add(SyntheticPopulation.HHTAZID_FIELD);
			tableHeadings.add("person_id");
			tableHeadings.add("personType");
			tableHeadings.add("patternType");
			tableHeadings.add("tour_id");
			tableHeadings.add("tourCategory");
			tableHeadings.add("purpose");
			tableHeadings.add("jt_party_size");
			for (int i=0; i < maxPartySize; i++) {
				tableHeadings.add("jt_person_" + i + "_id");
				tableHeadings.add("jt_person_" + i + "_type");
			}
			tableHeadings.add("tour_orig");
			tableHeadings.add("tour_orig_WLKseg");
			tableHeadings.add("M5_DC_TAZid");
			tableHeadings.add("M5_DC_WLKseg");
			tableHeadings.add("M6_TOD");
			tableHeadings.add("M6_TOD_StartHr");
			tableHeadings.add("M6_TOD_EndHr");
			tableHeadings.add("M6_TOD_StartPeriod");
			tableHeadings.add("M6_TOD_EndPeriod");
			tableHeadings.add("TOD_Output_StartPeriod");
			tableHeadings.add("TOD_Output_EndPeriod");
			tableHeadings.add("M7_MC");
			tableHeadings.add("M7_Tour_SubmodeOB");
			tableHeadings.add("M7_Tour_SubmodeIB");
			tableHeadings.add("M81_SFC");
			tableHeadings.add("M82_SLC_OB");
			tableHeadings.add("M82_SLC_OB_Subzone");
			tableHeadings.add("M82_SLC_IB");
			tableHeadings.add("M82_SLC_IB_Subzone");
			tableHeadings.add("M83_SMC_Ik");
			tableHeadings.add("M83_SMC_Kj");
			tableHeadings.add("M83_SMC_Jk");
			tableHeadings.add("M83_SMC_Ki");
			tableHeadings.add("IJ_Dist");
			tableHeadings.add("JI_Dist");
			tableHeadings.add("IK_Dist");
			tableHeadings.add("KJ_Dist");
			tableHeadings.add("JK_Dist");
			tableHeadings.add("KI_Dist");
			tableHeadings.add("M9_Parking_Zone");
			tableHeadings.add("Dist_Orig_Park");
			tableHeadings.add("Dist_Park_Dest");
			tableHeadings.add("LBS_IVT_OB");
			tableHeadings.add("LBS_IVT_IB");
			tableHeadings.add("EBS_IVT_OB");
			tableHeadings.add("EBS_IVT_IB");
			tableHeadings.add("BRT_IVT_OB");
			tableHeadings.add("BRT_IVT_IB");
			tableHeadings.add("LRT_IVT_OB");
			tableHeadings.add("LRT_IVT_IB");
			tableHeadings.add("CRL_IVT_OB");
			tableHeadings.add("CRL_IVT_IB");

            
            tableFormats = new ArrayList();
            tableFormats.add("%.0f");     //HHID_FIELD
            tableFormats.add("%.0f");     //SERIALNO_FIELD
            tableFormats.add("%.0f");     //HHTAZID_FIELD
            tableFormats.add("%.0f");     //person_id
            tableFormats.add("%.0f");     //personType
            tableFormats.add("%.0f");     //patternType
            tableFormats.add("%.0f");     //tour_id
            tableFormats.add("%.0f");     //tourCategory
            tableFormats.add("%.0f");     //purpose
            tableFormats.add("%.0f");     //jt_party_size
            for (int i=0; i < maxPartySize; i++) {
                tableFormats.add("%.0f"); //jt_person_i_id
                tableFormats.add("%.0f"); //jt_person_i_type
            }
            tableFormats.add("%.0f");     //tour_orig
            tableFormats.add("%.0f");     //tour_orig_WLKseg
            tableFormats.add("%.0f");     //M5_DC_TAZid
            tableFormats.add("%.0f");     //M5_DC_WLKseg
            tableFormats.add("%.0f");     //M6_TOD
            tableFormats.add("%.0f");     //M6_TOD_StartHr
            tableFormats.add("%.0f");     //M6_TOD_EndHr
            tableFormats.add("%.0f");     //M6_TOD_StartPeriod
            tableFormats.add("%.0f");     //M6_TOD_EndPeriod
            tableFormats.add("%.0f");     //TOD_Output_StartPeriod
            tableFormats.add("%.0f");     //TOD_Output_EndPeriod
            tableFormats.add("%.0f");     //M7_MC
            tableFormats.add("%.0f");     //M7_Tour_SubmodeOB
            tableFormats.add("%.0f");     //M7_Tour_SubmodeIB
            tableFormats.add("%.0f");     //M81_SFC
            tableFormats.add("%.0f");     //M82_SLC_OB
            tableFormats.add("%.0f");     //M82_SLC_OB_Subzone
            tableFormats.add("%.0f");     //M82_SLC_IB
            tableFormats.add("%.0f");     //M82_SLC_IB_Subzone
            tableFormats.add("%.0f");     //M83_SMC_Ik
            tableFormats.add("%.0f");     //M83_SMC_Kj
            tableFormats.add("%.0f");     //M83_SMC_Jk
            tableFormats.add("%.0f");     //M83_SMC_Ki
            tableFormats.add("%.2f");   //IJ_Dist
            tableFormats.add("%.2f");   //JI_Dist
            tableFormats.add("%.2f");   //IK_Dist
            tableFormats.add("%.2f");   //KJ_Dist");
            tableFormats.add("%.2f");   //JK_Dist");
            tableFormats.add("%.2f");   //KI_Dist");
            tableFormats.add("%.0f");     //M9_Parking_Zone
            tableFormats.add("%.0f");     //Dist_Orig_Park
            tableFormats.add("%.0f");     //Dist_Park_Dest
            tableFormats.add("%.2f");   //LBS_IVT_OB
            tableFormats.add("%.2f");   //LBS_IVT_IB
            tableFormats.add("%.2f");   //EBS_IVT_OB
            tableFormats.add("%.2f");   //EBS_IVT_IB
            tableFormats.add("%.2f");   //BRT_IVT_OB
            tableFormats.add("%.2f");   //BRT_IVT_IB
            tableFormats.add("%.2f");   //LRT_IVT_OB
            tableFormats.add("%.2f");   //LRT_IVT_IB
            tableFormats.add("%.2f");   //CRL_IVT_OB
            tableFormats.add("%.2f");   //CRL_IVT_IB

			// define an array for use in writing output file
			tableData = new float[tableHeadings.size()];

	

	
			if (outputFileDTM != null) {

				//Print titles
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print( String.format(",%s", (String)tableHeadings.get(i)) );
				}
				outStream.println();
			
			}
			

			
			for (int i=0; i < hh.length; i++) {
		
                hhCount++;
                
				tempHH = hh[i];
				
				hh_id = hh[i].getID();
                serialno = hh[i].getSerialno();
				hh_taz_id = hh[i].getTazID();
				Person[] persons = hh[i].getPersonArray();
				

				// first put individual mandatory tours in the output table
				it = hh[i].getMandatoryTours();
				if (it != null) {
					
					m = 1;
					
					for (t=0; t < it.length; t++) {
										    
						Arrays.fill ( tableData, 0.0f );
				
						tlIndex = 0;
						if (it[t].getTourType() == TourType.WORK) {
							if (hh[i].getHHIncome() == 1)
								tlIndex = 1;
							else if (hh[i].getHHIncome() == 2)
								tlIndex = 2;
							else if (hh[i].getHHIncome() == 3)
								tlIndex = 3;
						}
						else if (it[t].getTourType() == TourType.UNIVERSITY) {
							tlIndex = 4;
						}
						else if (it[t].getTourType() == TourType.SCHOOL) {
							tlIndex = 5;
						}

						
						if ( it[t].getStopLocOB() > 0 ) {
							tripsByMode[it[t].getTripIkMode()]++;
							tripsByMode[it[t].getTripKjMode()]++;
						}
						else {
							tripsByMode[it[t].getMode()]++;
						}
					
						if ( it[t].getStopLocIB() > 0 ) {
							tripsByMode[it[t].getTripJkMode()]++;
							tripsByMode[it[t].getTripKiMode()]++;
						}
						else {
							tripsByMode[it[t].getMode()]++;
						}



						index.setOriginZone( it[t].getOrigTaz() );
						index.setDestZone( it[t].getDestTaz() );
						index.setStopZone( it[t].getStopLocOB() );
						resultsOB = distUEC.solve( index, new Object(), distSample );
						
						index.setOriginZone( it[t].getDestTaz() );
						index.setDestZone( it[t].getOrigTaz() );
						index.setStopZone( it[t].getStopLocIB() );
						resultsIB = distUEC.solve( index, new Object(), distSample );

						index.setOriginZone( it[t].getOrigTaz() );
						index.setDestZone( it[t].getDestTaz() );
						index.setStopZone( it[t].getChosenPark() );
						resultsPark = distUEC.solve( index, new Object(), distSample );

					
						tableData[0] = hh_id;
                        tableData[1] = serialno;
                        tableData[2] = hh_taz_id;
						tableData[3] = it[t].getTourPerson();
						tableData[4] = persons[it[t].getTourPerson()].getPersonType();
						tableData[5] = persons[it[t].getTourPerson()].getPatternType();
						tableData[6] = t+1;
						tableData[7] = 1;
						tableData[8] = it[t].getTourType();
						k = 10 + (2*maxPartySize);
						tableData[k] = it[t].getOrigTaz();
						tableData[k+1] = it[t].getOriginShrtWlk();
												
						tableData[k+2] = it[t].getDestTaz();
						tableData[k+3] = it[t].getDestShrtWlk();
						tableData[k+4] = it[t].getTimeOfDayAlt();
						if (it[t].getTimeOfDayAlt() < 1)
							continue;
						tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodStartHour( it[t].getTimeOfDayAlt() );
						tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodEndHour( it[t].getTimeOfDayAlt() );
						tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+10] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+11] = it[t].getMode();
						tableData[k+12] = it[t].getSubmodeOB();
						tableData[k+13] = it[t].getSubmodeIB();
						tableData[k+14] = it[t].getStopFreqAlt();
						tableData[k+15] = it[t].getStopLocOB();
						tableData[k+16] = it[t].getStopLocSubzoneOB();
						tableData[k+17] = it[t].getStopLocIB();
						tableData[k+18] = it[t].getStopLocSubzoneIB();
						tableData[k+19] = it[t].getTripIkMode();
						tableData[k+20] = it[t].getTripKjMode();
						tableData[k+21] = it[t].getTripJkMode();
						tableData[k+22] = it[t].getTripKiMode();
						tableData[k+23] = (float)resultsOB[0];
						tableData[k+24] = (float)resultsIB[0];
						tableData[k+25] = (float)resultsOB[1];
						tableData[k+26] = (float)resultsOB[2];
						tableData[k+27] = (float)resultsIB[1];
						tableData[k+28] = (float)resultsIB[2];
						tableData[k+29] = it[t].getChosenPark();
						tableData[k+30] = (float)resultsPark[1];
						tableData[k+31] = (float)resultsPark[2];
						if (it[t].getMode() == 3) {
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsOB[3];
								tableData[k+34] = (float)resultsOB[4];
								tableData[k+36] = (float)resultsOB[5];
								tableData[k+38] = (float)resultsOB[6];
								tableData[k+40] = (float)resultsOB[7];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsIB[3];
								tableData[k+34] = (float)resultsIB[4];
								tableData[k+36] = (float)resultsIB[5];
								tableData[k+38] = (float)resultsIB[6];
								tableData[k+40] = (float)resultsIB[7];
							}
							else {
								tableData[k+32] = (float)resultsOB[8];
								tableData[k+34] = (float)resultsOB[9];
								tableData[k+36] = (float)resultsOB[10];
								tableData[k+38] = (float)resultsOB[11];
								tableData[k+40] = (float)resultsOB[12];
							}
							if (tableData[k+10] == 1) {
								tableData[k+33] = (float)resultsIB[3];
								tableData[k+35] = (float)resultsIB[4];
								tableData[k+37] = (float)resultsIB[5];
								tableData[k+39] = (float)resultsIB[6];
								tableData[k+41] = (float)resultsIB[7];
							}
							else if (tableData[k+10] == 2) {
								tableData[k+33] = (float)resultsOB[3];
								tableData[k+35] = (float)resultsOB[4];
								tableData[k+37] = (float)resultsOB[5];
								tableData[k+39] = (float)resultsOB[6];
								tableData[k+41] = (float)resultsOB[7];
							}
							else {
								tableData[k+33] = (float)resultsIB[8];
								tableData[k+35] = (float)resultsIB[9];
								tableData[k+37] = (float)resultsIB[10];
								tableData[k+39] = (float)resultsIB[11];
								tableData[k+41] = (float)resultsIB[12];
							}
						}
						else if (it[t].getMode() == 4) {
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsOB[13];
								tableData[k+34] = (float)resultsOB[14];
								tableData[k+36] = (float)resultsOB[15];
								tableData[k+38] = (float)resultsOB[16];
								tableData[k+40] = (float)resultsOB[17];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsIB[13];
								tableData[k+34] = (float)resultsIB[14];
								tableData[k+36] = (float)resultsIB[15];
								tableData[k+38] = (float)resultsIB[16];
								tableData[k+40] = (float)resultsIB[17];
							}
							else {
								tableData[k+32] = (float)resultsOB[18];
								tableData[k+34] = (float)resultsOB[19];
								tableData[k+36] = (float)resultsOB[20];
								tableData[k+38] = (float)resultsOB[21];
								tableData[k+40] = (float)resultsOB[22];
							}
							if (tableData[k+10] == 1) {
								tableData[k+33] = (float)resultsIB[13];
								tableData[k+35] = (float)resultsIB[14];
								tableData[k+37] = (float)resultsIB[15];
								tableData[k+39] = (float)resultsIB[16];
								tableData[k+41] = (float)resultsIB[17];
							}
							else if (tableData[k+10] == 2) {
								tableData[k+33] = (float)resultsOB[13];
								tableData[k+35] = (float)resultsOB[14];
								tableData[k+37] = (float)resultsOB[15];
								tableData[k+39] = (float)resultsOB[16];
								tableData[k+41] = (float)resultsOB[17];
							}
							else {
								tableData[k+33] = (float)resultsIB[18];
								tableData[k+35] = (float)resultsIB[19];
								tableData[k+37] = (float)resultsIB[20];
								tableData[k+39] = (float)resultsIB[21];
								tableData[k+41] = (float)resultsIB[22];
							}
						}
						else {
							tableData[k+32] = 0.0f;
							tableData[k+33] = 0.0f;
							tableData[k+34] = 0.0f;
							tableData[k+35] = 0.0f;
							tableData[k+36] = 0.0f;
							tableData[k+37] = 0.0f;
							tableData[k+38] = 0.0f;
							tableData[k+39] = 0.0f;
							tableData[k+40] = 0.0f;
							tableData[k+41] = 0.0f;
						}
						
						if (outputFileDTM != null) {

                            fieldFormat = (String)tableFormats.get(0);
                            outStream.print( String.format(fieldFormat, tableData[0]) );
							for (int c=1; c < tableHeadings.size(); c++) {
                                fieldFormat = "," + (String)tableFormats.get(c);
                                outStream.print( String.format(fieldFormat, tableData[c]) );
							}
							outStream.println();

						}
					
					
						totTours[tlIndex]++;
						totDist[tlIndex] += (float)(resultsOB[0]);
						modalTours[tlIndex][it[t].getMode()] += 1;
					}
				}



				// next put joint tours in the output table
				jt = hh[i].getJointTours();
				if (jt != null) {
					
					m = 2;
					
					for (t=0; t < jt.length; t++) {
						
						Arrays.fill ( tableData, 0.0f );
				
						int[] jtPersons = jt[t].getJointTourPersons();

					
						tlIndex = 0;
						if (jt[t].getTourType() == TourType.SHOP) {
							tlIndex = 8;
						}
						else if (jt[t].getTourType() == TourType.OTHER_MAINTENANCE) {
							tlIndex = 10;
						}
						else if (jt[t].getTourType() == TourType.DISCRETIONARY) {
							tlIndex = 12;
						}
						else if (jt[t].getTourType() == TourType.EAT) {
							tlIndex = 14;
						}

					
						if ( jt[t].getStopLocOB() > 0 ) {
							tripsByMode[jt[t].getTripIkMode()]++;
							tripsByMode[jt[t].getTripKjMode()]++;
						}
						else {
							tripsByMode[jt[t].getMode()]++;
						}
					
						if ( jt[t].getStopLocIB() > 0 ) {
							tripsByMode[jt[t].getTripJkMode()]++;
							tripsByMode[jt[t].getTripKiMode()]++;
						}
						else {
							tripsByMode[jt[t].getMode()]++;
						}
					
						index.setOriginZone( jt[t].getOrigTaz() );
						index.setDestZone( jt[t].getDestTaz() );
						index.setStopZone( jt[t].getStopLocOB() );
						resultsOB = distUEC.solve( index, new Object(), distSample );
						
						index.setOriginZone( jt[t].getDestTaz() );
						index.setDestZone( jt[t].getOrigTaz() );
						index.setStopZone( jt[t].getStopLocIB() );
						resultsIB = distUEC.solve( index, new Object(), distSample );

						index.setOriginZone( jt[t].getOrigTaz() );
						index.setDestZone( jt[t].getDestTaz() );
						index.setStopZone( jt[t].getChosenPark() );
						resultsPark = distUEC.solve( index, new Object(), distSample );

					
						tableData[0] = hh_id;
                        tableData[1] = serialno;
                        tableData[2] = hh_taz_id;
						tableData[3] = jt[t].getTourPerson();
						tableData[4] = persons[jt[t].getTourPerson()].getPersonType();
						tableData[5] = persons[jt[t].getTourPerson()].getPatternType();
						tableData[6] = t+1;
						tableData[7] = 2;
						tableData[8] = jt[t].getTourType();
						tableData[9] = jtPersons.length;
						for (int j=0; j < jtPersons.length; j++) {
							tableData[10+(2*j)] = jtPersons[j];
							tableData[10+(2*j)+1] = persons[jtPersons[j]].getPersonType();
						}
						k = 10 + (2*maxPartySize);
						tableData[k] = jt[t].getOrigTaz();
						tableData[k+1] = jt[t].getOriginShrtWlk();
						tableData[k+2] = jt[t].getDestTaz();
						tableData[k+3] = jt[t].getDestShrtWlk();
						tableData[k+4] = jt[t].getTimeOfDayAlt();
						if (jt[t].getTimeOfDayAlt() < 1)
							continue;
						tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodStartHour( jt[t].getTimeOfDayAlt() );
						tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodEndHour( jt[t].getTimeOfDayAlt() );
						tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+10] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+11] = jt[t].getMode();
						tableData[k+12] = jt[t].getSubmodeOB();
						tableData[k+13] = jt[t].getSubmodeIB();
						tableData[k+14] = jt[t].getStopFreqAlt();
						tableData[k+15] = jt[t].getStopLocOB();
						tableData[k+16] = jt[t].getStopLocSubzoneOB();
						tableData[k+17] = jt[t].getStopLocIB();
						tableData[k+18] = jt[t].getStopLocSubzoneIB();
						tableData[k+19] = jt[t].getTripIkMode();
						tableData[k+20] = jt[t].getTripKjMode();
						tableData[k+21] = jt[t].getTripJkMode();
						tableData[k+22] = jt[t].getTripKiMode();
						tableData[k+23] = (float)resultsOB[0];
						tableData[k+24] = (float)resultsOB[0];
						tableData[k+25] = (float)resultsOB[1];
						tableData[k+26] = (float)resultsOB[2];
						tableData[k+27] = (float)resultsIB[1];
						tableData[k+28] = (float)resultsIB[2];
						tableData[k+29] = jt[t].getChosenPark();
						tableData[k+30] = (float)resultsPark[1];
						tableData[k+31] = (float)resultsPark[2];
						if (jt[t].getMode() == 3) {
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsOB[3];
								tableData[k+34] = (float)resultsOB[4];
								tableData[k+36] = (float)resultsOB[5];
								tableData[k+38] = (float)resultsOB[6];
								tableData[k+40] = (float)resultsOB[7];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsIB[3];
								tableData[k+34] = (float)resultsIB[4];
								tableData[k+36] = (float)resultsIB[5];
								tableData[k+38] = (float)resultsIB[6];
								tableData[k+40] = (float)resultsIB[7];
							}
							else {
								tableData[k+32] = (float)resultsOB[8];
								tableData[k+34] = (float)resultsOB[9];
								tableData[k+36] = (float)resultsOB[10];
								tableData[k+38] = (float)resultsOB[11];
								tableData[k+40] = (float)resultsOB[12];
							}
							if (tableData[k+10] == 1) {
								tableData[k+33] = (float)resultsIB[3];
								tableData[k+35] = (float)resultsIB[4];
								tableData[k+37] = (float)resultsIB[5];
								tableData[k+39] = (float)resultsIB[6];
								tableData[k+41] = (float)resultsIB[7];
							}
							else if (tableData[k+10] == 2) {
								tableData[k+33] = (float)resultsOB[3];
								tableData[k+35] = (float)resultsOB[4];
								tableData[k+37] = (float)resultsOB[5];
								tableData[k+39] = (float)resultsOB[6];
								tableData[k+41] = (float)resultsOB[7];
							}
							else {
								tableData[k+33] = (float)resultsIB[8];
								tableData[k+35] = (float)resultsIB[9];
								tableData[k+37] = (float)resultsIB[10];
								tableData[k+39] = (float)resultsIB[11];
								tableData[k+41] = (float)resultsIB[12];
							}
						}
						else if (jt[t].getMode() == 4) {
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsOB[13];
								tableData[k+34] = (float)resultsOB[14];
								tableData[k+36] = (float)resultsOB[15];
								tableData[k+38] = (float)resultsOB[16];
								tableData[k+40] = (float)resultsOB[17];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsIB[13];
								tableData[k+34] = (float)resultsIB[14];
								tableData[k+36] = (float)resultsIB[15];
								tableData[k+38] = (float)resultsIB[16];
								tableData[k+40] = (float)resultsIB[17];
							}
							else {
								tableData[k+32] = (float)resultsOB[18];
								tableData[k+34] = (float)resultsOB[19];
								tableData[k+36] = (float)resultsOB[20];
								tableData[k+38] = (float)resultsOB[21];
								tableData[k+40] = (float)resultsOB[22];
							}
							if (tableData[k+10] == 1) {
								tableData[k+33] = (float)resultsIB[13];
								tableData[k+35] = (float)resultsIB[14];
								tableData[k+37] = (float)resultsIB[15];
								tableData[k+39] = (float)resultsIB[16];
								tableData[k+41] = (float)resultsIB[17];
							}
							else if (tableData[k+10] == 2) {
								tableData[k+33] = (float)resultsOB[13];
								tableData[k+35] = (float)resultsOB[14];
								tableData[k+37] = (float)resultsOB[15];
								tableData[k+39] = (float)resultsOB[16];
								tableData[k+41] = (float)resultsOB[17];
							}
							else {
								tableData[k+33] = (float)resultsIB[18];
								tableData[k+35] = (float)resultsIB[19];
								tableData[k+37] = (float)resultsIB[20];
								tableData[k+39] = (float)resultsIB[21];
								tableData[k+41] = (float)resultsIB[22];
							}

						}
						else {
							tableData[k+32] = 0.0f;
							tableData[k+33] = 0.0f;
							tableData[k+34] = 0.0f;
							tableData[k+35] = 0.0f;
							tableData[k+36] = 0.0f;
							tableData[k+37] = 0.0f;
							tableData[k+38] = 0.0f;
							tableData[k+39] = 0.0f;
							tableData[k+40] = 0.0f;
							tableData[k+41] = 0.0f;
						}
						
						if (outputFileDTM != null) {

                            fieldFormat = (String)tableFormats.get(0);
                            outStream.print( String.format(fieldFormat, tableData[0]) );
                            for (int c=1; c < tableHeadings.size(); c++) {
                                fieldFormat = "," + (String)tableFormats.get(c);
                                outStream.print( String.format(fieldFormat, tableData[c]) );
                            }
                            outStream.println();

						}					
					
						totTours[tlIndex]++;
						totDist[tlIndex] += (float)(resultsOB[0]);
						modalTours[tlIndex][jt[t].getMode()] += 1;
					}
				}



				// next put individual non-mandatory tours in the output table
				it = hh[i].getIndivTours();
				if (it != null) {
					
					m = 3;
					
					for (t=0; t < it.length; t++) {
				    
						Arrays.fill ( tableData, 0.0f );
				
						tlIndex = 0;
						if (it[t].getTourType() == TourType.ESCORTING) {
							tlIndex = 6;
						}
						if (it[t].getTourType() == TourType.SHOP) {
							tlIndex = 7;
						}
						else if (it[t].getTourType() == TourType.OTHER_MAINTENANCE) {
							tlIndex = 9;
						}
						else if (it[t].getTourType() == TourType.DISCRETIONARY) {
							tlIndex = 11;
						}
						else if (it[t].getTourType() == TourType.EAT) {
							tlIndex = 13;
						}

					
						if ( it[t].getStopLocOB() > 0 ) {
							tripsByMode[it[t].getTripIkMode()]++;
							tripsByMode[it[t].getTripKjMode()]++;
						}
						else {
							tripsByMode[it[t].getMode()]++;
						}
					
						if ( it[t].getStopLocIB() > 0 ) {
							tripsByMode[it[t].getTripJkMode()]++;
							tripsByMode[it[t].getTripKiMode()]++;
						}
						else {
							tripsByMode[it[t].getMode()]++;
						}
					
						index.setOriginZone( it[t].getOrigTaz() );
						index.setDestZone( it[t].getDestTaz() );
						index.setStopZone( it[t].getStopLocOB() );
						resultsOB = distUEC.solve( index, new Object(), distSample );
						
						index.setOriginZone( it[t].getDestTaz() );
						index.setDestZone( it[t].getOrigTaz() );
						index.setStopZone( it[t].getStopLocIB() );
						resultsIB = distUEC.solve( index, new Object(), distSample );

						index.setOriginZone( it[t].getOrigTaz() );
						index.setDestZone( it[t].getDestTaz() );
						index.setStopZone( it[t].getChosenPark() );
						resultsPark = distUEC.solve( index, new Object(), distSample );

					
						tableData[0] = hh_id;
                        tableData[1] = serialno;
                        tableData[2] = hh_taz_id;
						tableData[3] = it[t].getTourPerson();
						tableData[4] = persons[it[t].getTourPerson()].getPersonType();
						tableData[5] = persons[it[t].getTourPerson()].getPatternType();
						tableData[6] = t+1;
						tableData[7] = 3;
						tableData[8] = it[t].getTourType();
						k = 10 + (2*maxPartySize);
						tableData[k] = it[t].getOrigTaz();
						tableData[k+1] = it[t].getOriginShrtWlk();
						tableData[k+2] = it[t].getDestTaz();
						tableData[k+3] = it[t].getDestShrtWlk();
						tableData[k+4] = it[t].getTimeOfDayAlt();
						if (it[t].getTimeOfDayAlt() < 1)
							continue;
						tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodStartHour( it[t].getTimeOfDayAlt() );
						tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodEndHour( it[t].getTimeOfDayAlt() );
						tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+10] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+11] = it[t].getMode();
						tableData[k+12] = it[t].getSubmodeOB();
						tableData[k+13] = it[t].getSubmodeIB();
						tableData[k+14] = it[t].getStopFreqAlt();
						tableData[k+15] = it[t].getStopLocOB();
						tableData[k+16] = it[t].getStopLocSubzoneOB();
						tableData[k+17] = it[t].getStopLocIB();
						tableData[k+18] = it[t].getStopLocSubzoneIB();
						tableData[k+19] = it[t].getTripIkMode();
						tableData[k+20] = it[t].getTripKjMode();
						tableData[k+21] = it[t].getTripJkMode();
						tableData[k+22] = it[t].getTripKiMode();
						tableData[k+23] = (float)resultsOB[0];
						tableData[k+24] = (float)resultsOB[0];
						tableData[k+25] = (float)resultsOB[1];
						tableData[k+26] = (float)resultsOB[2];
						tableData[k+27] = (float)resultsIB[1];
						tableData[k+28] = (float)resultsIB[2];
						tableData[k+29] = it[t].getChosenPark();
						tableData[k+30] = (float)resultsPark[1];
						tableData[k+31] = (float)resultsPark[2];
						if (it[t].getMode() == 3) {
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsOB[3];
								tableData[k+34] = (float)resultsOB[4];
								tableData[k+36] = (float)resultsOB[5];
								tableData[k+38] = (float)resultsOB[6];
								tableData[k+40] = (float)resultsOB[7];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsIB[3];
								tableData[k+34] = (float)resultsIB[4];
								tableData[k+36] = (float)resultsIB[5];
								tableData[k+38] = (float)resultsIB[6];
								tableData[k+40] = (float)resultsIB[7];
							}
							else {
								tableData[k+32] = (float)resultsOB[8];
								tableData[k+34] = (float)resultsOB[9];
								tableData[k+36] = (float)resultsOB[10];
								tableData[k+38] = (float)resultsOB[11];
								tableData[k+40] = (float)resultsOB[12];
							}
							if (tableData[k+10] == 1) {
								tableData[k+33] = (float)resultsIB[3];
								tableData[k+35] = (float)resultsIB[4];
								tableData[k+37] = (float)resultsIB[5];
								tableData[k+39] = (float)resultsIB[6];
								tableData[k+41] = (float)resultsIB[7];
							}
							else if (tableData[k+10] == 2) {
								tableData[k+33] = (float)resultsOB[3];
								tableData[k+35] = (float)resultsOB[4];
								tableData[k+37] = (float)resultsOB[5];
								tableData[k+39] = (float)resultsOB[6];
								tableData[k+41] = (float)resultsOB[7];
							}
							else {
								tableData[k+33] = (float)resultsIB[8];
								tableData[k+35] = (float)resultsIB[9];
								tableData[k+37] = (float)resultsIB[10];
								tableData[k+39] = (float)resultsIB[11];
								tableData[k+41] = (float)resultsIB[12];
							}
						}
						else if (it[t].getMode() == 4) {
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsOB[13];
								tableData[k+34] = (float)resultsOB[14];
								tableData[k+36] = (float)resultsOB[15];
								tableData[k+38] = (float)resultsOB[16];
								tableData[k+40] = (float)resultsOB[17];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsIB[13];
								tableData[k+34] = (float)resultsIB[14];
								tableData[k+36] = (float)resultsIB[15];
								tableData[k+38] = (float)resultsIB[16];
								tableData[k+40] = (float)resultsIB[17];
							}
							else {
								tableData[k+32] = (float)resultsOB[18];
								tableData[k+34] = (float)resultsOB[19];
								tableData[k+36] = (float)resultsOB[20];
								tableData[k+38] = (float)resultsOB[21];
								tableData[k+40] = (float)resultsOB[22];
							}
							if (tableData[k+10] == 1) {
								tableData[k+33] = (float)resultsIB[13];
								tableData[k+35] = (float)resultsIB[14];
								tableData[k+37] = (float)resultsIB[15];
								tableData[k+39] = (float)resultsIB[16];
								tableData[k+41] = (float)resultsIB[17];
							}
							else if (tableData[k+10] == 2) {
								tableData[k+33] = (float)resultsOB[13];
								tableData[k+35] = (float)resultsOB[14];
								tableData[k+37] = (float)resultsOB[15];
								tableData[k+39] = (float)resultsOB[16];
								tableData[k+41] = (float)resultsOB[17];
							}
							else {
								tableData[k+33] = (float)resultsIB[18];
								tableData[k+35] = (float)resultsIB[19];
								tableData[k+37] = (float)resultsIB[20];
								tableData[k+39] = (float)resultsIB[21];
								tableData[k+41] = (float)resultsIB[22];
							}

						}
						else {
							tableData[k+32] = 0.0f;
							tableData[k+33] = 0.0f;
							tableData[k+34] = 0.0f;
							tableData[k+35] = 0.0f;
							tableData[k+36] = 0.0f;
							tableData[k+37] = 0.0f;
							tableData[k+38] = 0.0f;
							tableData[k+39] = 0.0f;
							tableData[k+40] = 0.0f;
							tableData[k+41] = 0.0f;
						}
						
						if (outputFileDTM != null) {

                            fieldFormat = (String)tableFormats.get(0);
                            outStream.print( String.format(fieldFormat, tableData[0]) );
                            for (int c=1; c < tableHeadings.size(); c++) {
                                fieldFormat = "," + (String)tableFormats.get(c);
                                outStream.print( String.format(fieldFormat, tableData[c]) );
                            }
							outStream.println();

						}					
					
						totTours[tlIndex]++;
						totDist[tlIndex] += (float)(resultsOB[0]);
						modalTours[tlIndex][it[t].getMode()] += 1;
					}
				}


				// finally, write trips for atwork subtours
				it = hh[i].getMandatoryTours();
				if (it != null) {
					
					m = 4;
					
					for (t=0; t < it.length; t++) {
						
						if (it[t].getTourType() == TourType.WORK) {
							st = it[t].getSubTours();
							if (st != null) {

								for (int s=0; s < st.length; s++) {

									tlIndex = 15;
								
									Arrays.fill ( tableData, 0.0f );
				
									if ( st[s].getStopLocOB() > 0 ) {
										tripsByMode[st[s].getTripIkMode()]++;
										tripsByMode[st[s].getTripKjMode()]++;
									}
									else {
										tripsByMode[st[s].getMode()]++;
									}
					
									if ( st[s].getStopLocIB() > 0 ) {
										tripsByMode[st[s].getTripJkMode()]++;
										tripsByMode[st[s].getTripKiMode()]++;
									}
									else {
										tripsByMode[st[s].getMode()]++;
									}
					
									index.setOriginZone( st[s].getOrigTaz() );
									index.setDestZone( st[s].getDestTaz() );
									index.setStopZone( st[s].getStopLocOB() );
									resultsOB = distUEC.solve( index, new Object(), distSample );
						
									index.setOriginZone( st[s].getDestTaz() );
									index.setDestZone( st[s].getOrigTaz() );
									index.setStopZone( st[s].getStopLocIB() );
									resultsIB = distUEC.solve( index, new Object(), distSample );

									index.setOriginZone( st[s].getOrigTaz() );
									index.setDestZone( st[s].getDestTaz() );
									index.setStopZone( st[s].getChosenPark() );
									resultsPark = distUEC.solve( index, new Object(), distSample );

					
									tableData[0] = hh_id;
                                    tableData[1] = serialno;
                                    tableData[2] = hh_taz_id;
									tableData[3] = st[s].getTourPerson();
									tableData[4] = persons[st[s].getTourPerson()].getPersonType();
									tableData[5] = persons[st[s].getTourPerson()].getPatternType();
									tableData[6] = (t+1)*10 + (s+1);
									tableData[7] = 4;
									tableData[8] = st[s].getSubTourType();

									k = 10 + (2*maxPartySize);
									tableData[k] = st[s].getOrigTaz();
									tableData[k+1] = st[s].getOriginShrtWlk();
									tableData[k+2] = st[s].getDestTaz();
									tableData[k+3] = st[s].getDestShrtWlk();
									tableData[k+4] = st[s].getTimeOfDayAlt();
									if (st[s].getTimeOfDayAlt() < 1)
										continue;
									tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodStartHour( st[s].getTimeOfDayAlt() );
									tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodEndHour( st[s].getTimeOfDayAlt() );
									tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+10] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+11] = st[s].getMode();
									tableData[k+12] = st[s].getSubmodeOB();
									tableData[k+13] = st[s].getSubmodeIB();
									tableData[k+14] = st[s].getStopFreqAlt();
									tableData[k+15] = st[s].getStopLocOB();
									tableData[k+16] = st[s].getStopLocSubzoneOB();
									tableData[k+17] = st[s].getStopLocIB();
									tableData[k+18] = st[s].getStopLocSubzoneIB();
									tableData[k+19] = st[s].getTripIkMode();
									tableData[k+20] = st[s].getTripKjMode();
									tableData[k+21] = st[s].getTripJkMode();
									tableData[k+22] = st[s].getTripKiMode();
									tableData[k+23] = (float)resultsOB[0];
									tableData[k+24] = (float)resultsOB[0];
									tableData[k+25] = (float)resultsOB[1];
									tableData[k+26] = (float)resultsOB[2];
									tableData[k+27] = (float)resultsIB[1];
									tableData[k+28] = (float)resultsIB[2];
									tableData[k+29] = st[s].getChosenPark();
									tableData[k+30] = (float)resultsPark[1];
									tableData[k+31] = (float)resultsPark[2];
									if (st[s].getMode() == 3) {
										if (tableData[k+9] == 1) {
											tableData[k+32] = (float)resultsOB[3];
											tableData[k+34] = (float)resultsOB[4];
											tableData[k+36] = (float)resultsOB[5];
											tableData[k+38] = (float)resultsOB[6];
											tableData[k+40] = (float)resultsOB[7];
										}
										else if (tableData[k+9] == 2) {
											tableData[k+32] = (float)resultsIB[3];
											tableData[k+34] = (float)resultsIB[4];
											tableData[k+36] = (float)resultsIB[5];
											tableData[k+38] = (float)resultsIB[6];
											tableData[k+40] = (float)resultsIB[7];
										}
										else {
											tableData[k+32] = (float)resultsOB[8];
											tableData[k+34] = (float)resultsOB[9];
											tableData[k+36] = (float)resultsOB[10];
											tableData[k+38] = (float)resultsOB[11];
											tableData[k+40] = (float)resultsOB[12];
										}
										if (tableData[k+10] == 1) {
											tableData[k+33] = (float)resultsIB[3];
											tableData[k+35] = (float)resultsIB[4];
											tableData[k+37] = (float)resultsIB[5];
											tableData[k+39] = (float)resultsIB[6];
											tableData[k+41] = (float)resultsIB[7];
										}
										else if (tableData[k+10] == 2) {
											tableData[k+33] = (float)resultsOB[3];
											tableData[k+35] = (float)resultsOB[4];
											tableData[k+37] = (float)resultsOB[5];
											tableData[k+39] = (float)resultsOB[6];
											tableData[k+41] = (float)resultsOB[7];
										}
										else {
											tableData[k+33] = (float)resultsIB[8];
											tableData[k+35] = (float)resultsIB[9];
											tableData[k+37] = (float)resultsIB[10];
											tableData[k+39] = (float)resultsIB[11];
											tableData[k+41] = (float)resultsIB[12];
										}
									}
									else if (st[s].getMode() == 4) {
										if (tableData[k+9] == 1) {
											tableData[k+32] = (float)resultsOB[13];
											tableData[k+34] = (float)resultsOB[14];
											tableData[k+36] = (float)resultsOB[15];
											tableData[k+38] = (float)resultsOB[16];
											tableData[k+40] = (float)resultsOB[17];
										}
										else if (tableData[k+9] == 2) {
											tableData[k+32] = (float)resultsIB[13];
											tableData[k+34] = (float)resultsIB[14];
											tableData[k+36] = (float)resultsIB[15];
											tableData[k+38] = (float)resultsIB[16];
											tableData[k+40] = (float)resultsIB[17];
										}
										else {
											tableData[k+32] = (float)resultsOB[18];
											tableData[k+34] = (float)resultsOB[19];
											tableData[k+36] = (float)resultsOB[20];
											tableData[k+38] = (float)resultsOB[21];
											tableData[k+40] = (float)resultsOB[22];
										}
										if (tableData[k+10] == 1) {
											tableData[k+33] = (float)resultsIB[13];
											tableData[k+35] = (float)resultsIB[14];
											tableData[k+37] = (float)resultsIB[15];
											tableData[k+39] = (float)resultsIB[16];
											tableData[k+41] = (float)resultsIB[17];
										}
										else if (tableData[k+10] == 2) {
											tableData[k+33] = (float)resultsOB[13];
											tableData[k+35] = (float)resultsOB[14];
											tableData[k+37] = (float)resultsOB[15];
											tableData[k+39] = (float)resultsOB[16];
											tableData[k+41] = (float)resultsOB[17];
										}
										else {
											tableData[k+33] = (float)resultsIB[18];
											tableData[k+35] = (float)resultsIB[19];
											tableData[k+37] = (float)resultsIB[20];
											tableData[k+39] = (float)resultsIB[21];
											tableData[k+41] = (float)resultsIB[22];
										}

									}
									else {
										tableData[k+32] = 0.0f;
										tableData[k+33] = 0.0f;
										tableData[k+34] = 0.0f;
										tableData[k+35] = 0.0f;
										tableData[k+36] = 0.0f;
										tableData[k+37] = 0.0f;
										tableData[k+38] = 0.0f;
										tableData[k+39] = 0.0f;
										tableData[k+40] = 0.0f;
										tableData[k+41] = 0.0f;
									}

									if (outputFileDTM != null) {

                                        fieldFormat = (String)tableFormats.get(0);
                                        outStream.print( String.format(fieldFormat, tableData[0]) );
                                        for (int c=1; c < tableHeadings.size(); c++) {
                                            fieldFormat = "," + (String)tableFormats.get(c);
                                            outStream.print( String.format(fieldFormat, tableData[c]) );
                                        }
										outStream.println();
					
									}

					
									totTours[tlIndex]++;
									totDist[tlIndex] += (float)(resultsOB[0]);
									modalTours[tlIndex][st[s].getMode()] += 1;
								}

							}

						}

					}

				}
	
			}
				
			logger.info ("finished writing DTMS output csv file.");
			outStream.close();

			tableData = null;
			
		}
		catch (RuntimeException e) {

			logger.fatal ( String.format("runtime exception occurred in DTMOutput.writeDTMOutput() for hhCount=%d", hhCount)  );
			logger.fatal("");
			logger.fatal("tourCategory=" + m);
			logger.fatal("tour index=" + t);
            if ( index != null) {
    			logger.fatal("orig zone=" + index.getOriginZone());
    			logger.fatal("dest zone=" + index.getDestZone());
    			logger.fatal("stop zone=" + index.getStopZone());
            }
            
			for (int i=0; i < tableData.length; i++)
				logger.fatal( "[" + i + "]:  " + tableHeadings.get(i) + "  =  " + tableData[i] );
			logger.fatal("");
            if ( tempHH != null) {
                tempHH.writeContentToLogger(logger);
            }
			logger.fatal("");

			throw e;
		}
				

		

		// write trip and tour summaries
		logger.info ( "");
		logger.info ( "Total Trip Mode Shares");
		for (int i=1; i < tripsByMode.length; i++)			
			logger.info ( "index=" + i + ", mode=" + modeName[i] + ",  trips=" + tripsByMode[i]);			
		logger.info ( "");


		logger.info ("Total Tour Distance, Total Tours, Average Trip Length by Tour Purpose:");
		for (int i=1; i <= 15; i++)
			logger.info (tlPurposeName[i] + ":   total dist (miles)= " + totDist[i] + ", total tours= " + totTours[i] + ", average tour distance (miles)= " + totDist[i]/totTours[i]);
	
	
		logger.info ("");
		logger.info ("");
		logger.info ("Tour Mode Shares by Tour Purpose:");
		for (int i=1; i <= 15; i++) {
			logger.info (tlPurposeName[i] + " modal shares:");
			for (int j=1; j <= 6; j++)
				logger.info (modeName[j] + "=" + modalTours[i][j]);
		}
		logger.info ("");
		logger.info ("");
	
	
		logger.info ("Tour Length Data:");
		for (int i=1; i <= 15; i++)
			logger.info (totDist[i] + ", " + totTours[i]);
	
	
		logger.info ("Tour Mode Share Data:");
		for (int i=1; i <= 15; i++) {
			logger.info (modalTours[i][1] + ", " + modalTours[i][2] + ", " + modalTours[i][3] + ", " + modalTours[i][4] + ", " + modalTours[i][5] + ", " + modalTours[i][6]);
		}
		
		
			
		logger.info ("end of writeDTMOutput().");

	}






	private float[][] getVehOccRatios () {

		float[][] ratios = new float[TourType.TYPES+1][4+1];
		
		ratios[TourType.WORK][1] = Float.parseFloat ( (String)propertyMap.get( "work.am" ) );
		ratios[TourType.WORK][2] = Float.parseFloat ( (String)propertyMap.get( "work.pm" ) );
		ratios[TourType.WORK][3] = Float.parseFloat ( (String)propertyMap.get( "work.md" ) );
		ratios[TourType.WORK][4] = Float.parseFloat ( (String)propertyMap.get( "work.nt" ) );
		ratios[TourType.UNIVERSITY][1] = Float.parseFloat ( (String)propertyMap.get( "univ.am" ) );
		ratios[TourType.UNIVERSITY][2] = Float.parseFloat ( (String)propertyMap.get( "univ.pm" ) );
		ratios[TourType.UNIVERSITY][3] = Float.parseFloat ( (String)propertyMap.get( "univ.md" ) );
		ratios[TourType.UNIVERSITY][4] = Float.parseFloat ( (String)propertyMap.get( "univ.nt" ) );
		ratios[TourType.SCHOOL][1] = Float.parseFloat ( (String)propertyMap.get( "school.am" ) );
		ratios[TourType.SCHOOL][2] = Float.parseFloat ( (String)propertyMap.get( "school.pm" ) );
		ratios[TourType.SCHOOL][3] = Float.parseFloat ( (String)propertyMap.get( "school.md" ) );
		ratios[TourType.SCHOOL][4] = Float.parseFloat ( (String)propertyMap.get( "school.nt" ) );
		ratios[TourType.ESCORTING][1] = Float.parseFloat ( (String)propertyMap.get( "escort.am" ) );
		ratios[TourType.ESCORTING][2] = Float.parseFloat ( (String)propertyMap.get( "escort.pm" ) );
		ratios[TourType.ESCORTING][3] = Float.parseFloat ( (String)propertyMap.get( "escort.md" ) );
		ratios[TourType.ESCORTING][4] = Float.parseFloat ( (String)propertyMap.get( "escort.nt" ) );
		ratios[TourType.SHOP][1] = Float.parseFloat ( (String)propertyMap.get( "shop.am" ) );
		ratios[TourType.SHOP][2] = Float.parseFloat ( (String)propertyMap.get( "shop.pm" ) );
		ratios[TourType.SHOP][3] = Float.parseFloat ( (String)propertyMap.get( "shop.md" ) );
		ratios[TourType.SHOP][4] = Float.parseFloat ( (String)propertyMap.get( "shop.nt" ) );
		ratios[TourType.OTHER_MAINTENANCE][1] = Float.parseFloat ( (String)propertyMap.get( "maint.am" ) );
		ratios[TourType.OTHER_MAINTENANCE][2] = Float.parseFloat ( (String)propertyMap.get( "maint.pm" ) );
		ratios[TourType.OTHER_MAINTENANCE][3] = Float.parseFloat ( (String)propertyMap.get( "maint.md" ) );
		ratios[TourType.OTHER_MAINTENANCE][4] = Float.parseFloat ( (String)propertyMap.get( "maint.nt" ) );
		ratios[TourType.DISCRETIONARY][1] = Float.parseFloat ( (String)propertyMap.get( "discr.am" ) );
		ratios[TourType.DISCRETIONARY][2] = Float.parseFloat ( (String)propertyMap.get( "discr.pm" ) );
		ratios[TourType.DISCRETIONARY][3] = Float.parseFloat ( (String)propertyMap.get( "discr.md" ) );
		ratios[TourType.DISCRETIONARY][4] = Float.parseFloat ( (String)propertyMap.get( "discr.nt" ) );
		ratios[TourType.EAT][1] = Float.parseFloat ( (String)propertyMap.get( "eat.am" ) );
		ratios[TourType.EAT][2] = Float.parseFloat ( (String)propertyMap.get( "eat.pm" ) );
		ratios[TourType.EAT][3] = Float.parseFloat ( (String)propertyMap.get( "eat.md" ) );
		ratios[TourType.EAT][4] = Float.parseFloat ( (String)propertyMap.get( "eat.nt" ) );
		ratios[TourType.ATWORK][1] = Float.parseFloat ( (String)propertyMap.get( "atwork.am" ) );
		ratios[TourType.ATWORK][2] = Float.parseFloat ( (String)propertyMap.get( "atwork.pm" ) );
		ratios[TourType.ATWORK][3] = Float.parseFloat ( (String)propertyMap.get( "atwork.md" ) );
		ratios[TourType.ATWORK][4] = Float.parseFloat ( (String)propertyMap.get( "atwork.nt" ) );

		return ratios;
	}



}
