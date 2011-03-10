package com.pb.morpc.models;

import com.pb.morpc.matrix.MatrixDataServer;
import com.pb.morpc.matrix.MatrixDataServerRmi;
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
    static final String[] localAccessMode = { "", "wLw", "pLw", "kLw", "wLp", "wLk" };
    static final String[] premiumAccessMode = { "", "wPw", "pPw", "kPw", "wPp", "wPk" };
    static final String[] subMode = { "", "lbs", "ebs", "brt", "lrt", "crl" };
    static final String[] primaryMode = { "", "hwy", "localTransit", "premiumTransit", "nonmotor" };
    static final String[] periodName = { "", "am", "pm", "md", "nt" };        

    // submodes
    static final int LBS = 1;
    static final int EBS = 2;
    static final int BRT = 3;
    static final int LRT = 4;
    static final int CRL = 5;
    
    // access modes - used for bothe LOCAL and PREMIUM
    static final int WTW = 1;
    static final int PTW = 2;
    static final int KTW = 3;
    static final int WTP = 4;
    static final int WTK = 5;
        
    // primary modes
    static final int HWY = 1;
    static final int LOCAL = 2;
    static final int PREM = 3;
    static final int NM = 4;
    


    // dist and ivt skim matrix indices 
    private static final int SOV_MODE = 1;
    private static final int HOV_MODE = 2;
    private static final int WL_MODE = 3;
    private static final int PL_MODE = 4;
    private static final int KL_MODE = 5;
    private static final int WP_MODE = 6;
    private static final int PP_MODE = 7;
    private static final int KP_MODE = 8;

    private static final int AM_SKIM_PERIOD = 1;
    private static final int PM_SKIM_PERIOD = 2;
    private static final int MD_SKIM_PERIOD = 3;
    private static final int NT_SKIM_PERIOD = 4;
    
    private static final int IJ = 0;
    private static final int JI = 1;
    private static final int IK = 2;
    private static final int JK = 3;
    private static final int KJ = 4;
    private static final int KI = 5;

    // this matrix holds walk trips on transit tour segments found while scanning transit tours
    // and adds them to non-motorized tables created from non-motorized tour modes.
    float[][] accumulatedNonMotorized;
    


    int numberOfZones;
    
    HashMap<String, String> propertyMap;
    boolean useMessageWindow = false;
    MessageWindow mw;

    String matrixSeverAddress;
    String matrixSeverPort;
    
    IndexValues index = new IndexValues();



    public DTMOutput (HashMap<String, String> propertyMap, ZonalDataManager zdm) {

        this.propertyMap = propertyMap;

        matrixSeverAddress = (String) propertyMap.get("RunModel.MatrixServerAddress");
        matrixSeverPort = (String) propertyMap.get("RunModel.MatrixServerPort");

        // get the indicator for whether to use the message window or not
        // from the properties file.
        String useMessageWindowString = (String)propertyMap.get( "MessageWindow");
        if (useMessageWindowString != null) {
            if (useMessageWindowString.equalsIgnoreCase("true")) {
                useMessageWindow = true;
                this.mw = new MessageWindow ( "MORPC Tour Destination, Time of Day, and Mode Choice Models" );
            }
        }


        numberOfZones = zdm.getNumberOfZones();

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

                accumulatedNonMotorized = new float[numberOfZones][numberOfZones];

                float[][][] hwyTrips = null;                
                
                // write the highway mode trip tables ...

                // construct filename
                String tppFileName = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[HWY] + periodName[period] + ".tpp";
                
                String[] hNames = { "", "sov person", "hov person", "sov vehicle", "hov vehicle" };
                String[] hDescriptions = { "",
                        String.format("%s %s sov person trips", purposeName[type], periodName[period]),
                        String.format("%s %s hov person trips", purposeName[type], periodName[period]),
                        String.format("%s %s sov vehicle trips", purposeName[type], periodName[period]),
                        String.format("%s %s hov vehicle trips", purposeName[type], periodName[period]) };
                hwyTrips = getHwyTripTables ( hh, vocRatios, period, type );
                writeTpplusMatrices ( tppFileName, hwyTrips, hNames, hDescriptions );
                hwyTrips = null;

                
                
                
                float[][][][] transitTrips = null;                
                
                // write the local transit mode trip tables ...
                int[] localSubModeIndices = { -1, LBS };
                int[] localTransitModes = { TourModeType.WL, TourModeType.PL, TourModeType.KL };

                // construct filenames
                String[] tppFileNames = new String[localAccessMode.length];
                String[][] tableNames = new String[localAccessMode.length][localSubModeIndices.length];
                String[][] tableDescriptions = new String[localAccessMode.length][localSubModeIndices.length];
                for (int i=1; i < tppFileNames.length; i++){
                    tppFileNames[i] = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[LOCAL] + "_" + localAccessMode[i] + "_" + periodName[period] + ".tpp";
                    for (int j=1; j < localSubModeIndices.length; j++){
                        tableNames[i][j] = localAccessMode[i] + " " + subMode[localSubModeIndices[j]];
                        tableDescriptions[i][j] = String.format("%s %s %s person trips", purposeName[type], periodName[period], tableNames[i][j]);
                    }
                }

                transitTrips = getTransitTripTables ( hh, period, type, localSubModeIndices, localTransitModes );                    
                for (int i=1; i < tppFileNames.length; i++){
                    writeTpplusMatrices ( tppFileNames[i], transitTrips[i], tableNames[i], tableDescriptions[i] );
                }
                
                
                
                
                // write the premium transit mode trip tables ...
                int[] premiumSubModeIndices = { -1, LBS, EBS, BRT, LRT, CRL };
                int[] premiumTransitModes = { TourModeType.WP, TourModeType.PP, TourModeType.KP };

                // construct filenames
                tppFileNames = new String[premiumAccessMode.length];
                tableNames = new String[premiumAccessMode.length][premiumSubModeIndices.length];
                tableDescriptions = new String[premiumAccessMode.length][premiumSubModeIndices.length];
                for (int i=1; i < tppFileNames.length; i++){
                    tppFileNames[i] = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[PREM] + "_" + premiumAccessMode[i] + "_" + periodName[period] + ".tpp";
                    for (int j=1; j < premiumSubModeIndices.length; j++){
                        tableNames[i][j] = premiumAccessMode[i] + " " + subMode[premiumSubModeIndices[j]];
                        tableDescriptions[i][j] = String.format("%s %s %s person trips", purposeName[type], periodName[period], tableNames[i][j]);
                    }
                }

                transitTrips = getTransitTripTables ( hh, period, type, premiumSubModeIndices, premiumTransitModes );
                for (int i=1; i < tppFileNames.length; i++){
                    writeTpplusMatrices ( tppFileNames[i], transitTrips[i], tableNames[i], tableDescriptions[i] );
                }
                transitTrips = null;
                
                
                
                // write the non-motorized mode trip tables ...

                // construct filename
                tppFileName = outputDirectory + "/" + purposeName[type] + "_" + primaryMode[NM] + "_" + periodName[period] + ".tpp";
                
                String[] nmNames = { "", "non-motorized" };

                String[] nmDescriptions = { "", String.format("%s %s %s person trips", purposeName[type], periodName[period], nmNames[0]) };

                float[][][] nmTrips = new float[1+1][][];
                nmTrips[1] = getNmTripTables ( hh, period, type );
                writeTpplusMatrices ( tppFileName, nmTrips, nmNames, nmDescriptions );
                nmTrips = null;
                
            }
            
        }
            
    }


    /*
    private void logArrayTotal(String label, float[][] array){
        float sum = 0;
        for ( int k=0; k < array.length; k++ )
            for ( int m=0; m < array[k].length; m++ )
                sum += array[k][m];
        logger.info(label + sum );
    }
    */
    

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
                                accumulateHighwayTripsForTour ( tours[t], false, tourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
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
                            if ( tourMode == TourModeType.SOV  ) {
                                logger.error ( String.format("joint tour has tour mode of SOV, i=%d, hhid=%d, t=%d", i, hh[i].getID(), t) );
                                throw new RuntimeException();
                            }
                            else if ( tourMode == TourModeType.HOV ) { 
                                accumulateHighwayTripsForTour ( tours[t], true, tourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
                            }
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
                                accumulateHighwayTripsForTour ( tours[t], false, tourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
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
                                            accumulateHighwayTripsForTour ( subTours[s], false, subTourMode, vocRatios, period, sovPerson, sovVehicle, hovPerson, hovVehicle );
                                        
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

        
        float[][][] result = new float[4+1][][];
        result[1] = sovPerson;
        result[2] = hovPerson;
        result[3] = sovVehicle;
        result[4] = hovVehicle;
        
        return result;

    }
    


    private float[][][][] getTransitTripTables ( Household[] hh, int period, int type, int[] subModeIndices, int[] transitModes ) {

        float[][][][] transitPerson = new float[SubmodeType.TYPES+1][subModeIndices.length][numberOfZones][numberOfZones];

        Tour[] tours = null;        
        
        // loop through tours in Household objects and look for tours with transit tour modes.
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
                            for (int m=0; m < transitModes.length; m++){
                                if ( tourMode == transitModes[m] )
                                    accumulateTransitTripsForTour ( tours[t], false, tourMode, period, subModeIndices, transitPerson );
                            }
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
                            for (int m=0; m < transitModes.length; m++){
                                if ( tourMode == transitModes[m] )
                                    accumulateTransitTripsForTour ( tours[t], true, tourMode, period, subModeIndices, transitPerson );
                            }
                        }
                    }

                    // Individual non-mandatory tours ...
                    tours = hh[i].getIndivTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            // if tour mode is sov or hov, pass tour object and trip table arrays into
                            // method to accumulate person and vehicle highway trips, if any.
                            int tourMode = tours[t].getMode();
                            for (int m=0; m < transitModes.length; m++){
                                if ( tourMode == transitModes[m] )
                                    accumulateTransitTripsForTour ( tours[t], false, tourMode, period, subModeIndices, transitPerson );
                            }
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
                                        for (int m=0; m < transitModes.length; m++){
                                            if ( subTourMode == transitModes[m] )
                                                accumulateTransitTripsForTour ( subTours[s], false, subTourMode, period, subModeIndices, transitPerson );
                                        }
                                        
                                    }
                                }
                            }
                        }
                    }
                }
                
            }
            catch (RuntimeException e) {
                logger.fatal ("Caught runtime exception processing hhid " + i);
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
                            if ( tours[t].getMode() == TourModeType.NM )
                                accumulateNmTripsForTour ( tours[t], false, period, result );
                        }
                    }
                    
                }
                else if ( type == 2 ) {

                    // Joint tours ...
                    tours = hh[i].getJointTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            if ( tours[t].getMode() == TourModeType.NM )
                                accumulateNmTripsForTour ( tours[t], true, period, result );
                        }
                    }

                    // Individual non-mandatory tours ...
                    tours = hh[i].getIndivTours();
                    if (tours != null) {
                        for (int t=0; t < tours.length; t++) {
                            if ( tours[t].getMode() == TourModeType.NM )
                                accumulateNmTripsForTour ( tours[t], false, period, result );
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
                                        if ( subTours[s].getMode() == TourModeType.NM )
                                            accumulateNmTripsForTour ( subTours[s], false, period, result );
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

        
        // add the walk trips accumulated from transit tours to the non-motorized table.
        for (int i=0; i < result.length; i++)
            for (int j=0; j < result.length; j++)
                result[i][j] += accumulatedNonMotorized[i][j];
        
                
        return result;

    }
    


    private void accumulateHighwayTripsForTour ( Tour tour, boolean isJointTour, int tourMode, float[][] vocRatios, int period, float[][] sovPerson, float[][] sovVehicle, float[][] hovPerson, float[][] hovVehicle ) {
        
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

        
        float sovPersonTripUnit = 0;
        float hovPersonTripUnit = 0;
        float sovVehicleTripUnit = 0;
        float hovVehicleTripUnit = 0;


        // accumulate motorized trips in the outbound direction if the specified period equals the tour start period.
        if ( period == periodOut ) {
    
            float vocRatioOB = vocRatios[tourType][periodOut];
            
            if ( isJointTour ) {
                sovPersonTripUnit = 0;
                hovPersonTripUnit = tour.getNumPersons();
                sovVehicleTripUnit = 0;
                hovVehicleTripUnit = 1;
            }
            else {
                sovPersonTripUnit = 1;
                hovPersonTripUnit = 1;
                sovVehicleTripUnit = 1;
                hovVehicleTripUnit = 1.0f/vocRatioOB;
            }

            if (tripStopOB > 0) {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigOB-1][tripStopOB-1] += sovPersonTripUnit;
                    sovPerson[tripStopOB-1][tripDestOB-1] += sovPersonTripUnit;
                    sovVehicle[tripOrigOB-1][tripStopOB-1] += sovVehicleTripUnit;
                    sovVehicle[tripStopOB-1][tripDestOB-1] += sovVehicleTripUnit;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigOB-1][tripStopOB-1] += hovPersonTripUnit;
                    hovPerson[tripStopOB-1][tripDestOB-1] += hovPersonTripUnit;
                    hovVehicle[tripOrigOB-1][tripStopOB-1] += hovVehicleTripUnit;
                    hovVehicle[tripStopOB-1][tripDestOB-1] += hovVehicleTripUnit;
                }
            }
            else {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigOB-1][tripDestOB-1] += sovPersonTripUnit;
                    sovVehicle[tripOrigOB-1][tripDestOB-1] += sovVehicleTripUnit;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigOB-1][tripDestOB-1] += hovPersonTripUnit;
                    hovVehicle[tripOrigOB-1][tripDestOB-1] += hovVehicleTripUnit;
                }
            }

        }
    
        
        // accumulate motorized trips in the inbound direction if the specified period equals the tour end period.
        if ( period == periodIn ) {

            float vocRatioIB = vocRatios[tourType][periodIn];

            if ( isJointTour ) {
                sovPersonTripUnit = 0;
                hovPersonTripUnit = tour.getNumPersons();
                sovVehicleTripUnit = 0;
                hovVehicleTripUnit = 1;
            }
            else {
                sovPersonTripUnit = 1;
                hovPersonTripUnit = 1;
                sovVehicleTripUnit = 1;
                hovVehicleTripUnit = 1.0f/vocRatioIB;
            }

            if (tripStopIB > 0) {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigIB-1][tripStopIB-1] += sovPersonTripUnit;
                    sovPerson[tripStopIB-1][tripDestIB-1] += sovPersonTripUnit;
                    sovVehicle[tripOrigIB-1][tripStopIB-1] += sovVehicleTripUnit;
                    sovVehicle[tripStopIB-1][tripDestIB-1] += sovVehicleTripUnit;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigIB-1][tripStopIB-1] += hovPersonTripUnit;
                    hovPerson[tripStopIB-1][tripDestIB-1] += hovPersonTripUnit;
                    hovVehicle[tripOrigIB-1][tripStopIB-1] += hovVehicleTripUnit;
                    hovVehicle[tripStopIB-1][tripDestIB-1] += hovVehicleTripUnit;
                }
            }
            else {
                if ( tourMode == 1 ) {
                    sovPerson[tripOrigIB-1][tripDestIB-1] += sovPersonTripUnit;
                    sovVehicle[tripOrigIB-1][tripDestIB-1] += sovVehicleTripUnit;
                }
                else if ( tourMode == 2 ) {
                    hovPerson[tripOrigIB-1][tripDestIB-1] += hovPersonTripUnit;
                    hovVehicle[tripOrigIB-1][tripDestIB-1] += hovVehicleTripUnit;
                }
            }

        }
    
    }

    
    private boolean tripModeIsValidMode( int modeIndex, int[] subModeIndices ){
        for ( int m : subModeIndices ){
            if ( m == modeIndex )
                return true;
        }
        return false;
    }


    private void invalidTripMode( int modeIndex, int[] subModeIndices, String identifier ){
        logger.fatal( "invalid trip mode = " + modeIndex + " for half tour." );
        logger.fatal( "tour identifier: " + identifier );
        String validIndices = "[ " + subModeIndices[1];
        for (int i=2; i < subModeIndices.length; i++)
            validIndices += ", " + subModeIndices[i];
        validIndices += " ]";
        logger.fatal( "valid indices for this trip mode are: " + validIndices );
        throw new RuntimeException();
    }
    
    
    private void accumulateTransitTripsForTour ( Tour tour, boolean isJointTour, int tourMode, int period, int[] subModeIndices, float[][][][] transitPerson ) {

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
        
        
    
        float tripUnit = 1.0f;
        if ( isJointTour ) {
            tripUnit = tour.getNumPersons();
        }
    

        // accumulate transit trips in the outbound direction if the specified period equals the tour start period.
        if ( period == periodOut ) {

            // there is an outbound stop for this tour
            if ( tripStopOB > 0 ) {

                // if the trip mode for either segment of this outbound half tour is SubmodeType.NM, the trip mode is nonmotorized;
                // if it's > SubmodeType.NM or < 1, it's an error.  In either case, don't accumulate it.

                if ( tripModeIk < 1 || tripModeIk > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for first segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKj < 1 || tripModeKj > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for second segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }

                
                // for p&r half-tours in outbound direction, ik segment is p&r and kj segment is walkTransit or walk.
                if ( tourMode == TourModeType.PL ) {
                    // accumulate local trip, if it's transit, for first segment only in pTw table
                    if ( tripModeIsValidMode( tripModeIk, subModeIndices ) )
                        transitPerson[PTW][tripModeIk][tripOrigOB-1][tripStopOB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeIk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                   
                    // accumulate local trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKj != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKj, subModeIndices ) )
                            transitPerson[WTW][tripModeKj][tripStopOB-1][tripDestOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKj, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopOB-1][tripDestOB-1] += tripUnit;
                    }
                }
                // for p&r half-tours in outbound direction, ik segment is p&r and kj segment is walkTransit or walk.
                else if ( tourMode == TourModeType.PP ) {
                    // accumulate premium trip, if it's transit, for first segment only in pTw table
                    if ( tripModeIsValidMode( tripModeIk, subModeIndices ) )
                        transitPerson[PTW][tripModeIk][tripOrigOB-1][tripStopOB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeIk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                   
                    // accumulate premium trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKj != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKj, subModeIndices ) )
                            transitPerson[WTW][tripModeKj][tripStopOB-1][tripDestOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKj, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopOB-1][tripDestOB-1] += tripUnit;
                    }
                }
                // for k&r half-tours in outbound direction, ik segment is k&r and kj segment is walkTransit or walk.
                else if ( tourMode == TourModeType.KL ) {
                    // accumulate local trip, if it's transit, for first segment only in kTw table
                    if ( tripModeIsValidMode( tripModeIk, subModeIndices ) )
                        transitPerson[KTW][tripModeIk][tripOrigOB-1][tripStopOB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeIk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    
                    // accumulate local trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKj != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKj, subModeIndices ) )
                            transitPerson[WTW][tripModeKj][tripStopOB-1][tripDestOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKj, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopOB-1][tripDestOB-1] += tripUnit;
                    }
                }
                // for k&r half-tours in outbound direction, ik segment is k&r and kj segment is walkTransit or walk.
                else if ( tourMode == TourModeType.KP ) {
                    // accumulate premium trip, if it's transit, for first segment only in kTw table
                    if ( tripModeIsValidMode( tripModeIk, subModeIndices ) )
                        transitPerson[KTW][tripModeIk][tripOrigOB-1][tripStopOB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeIk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    
                    // accumulate premium trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKj != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKj, subModeIndices ) )
                            transitPerson[WTW][tripModeKj][tripStopOB-1][tripDestOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKj, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopOB-1][tripDestOB-1] += tripUnit;
                    }
                }
                // for walk transit half-tours in outbound direction, ik segment and kj segment are either walkTransit or walk.
                else if ( tourMode == TourModeType.WL ) {
                    // accumulate local trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeIk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeIk, subModeIndices ) )
                            transitPerson[WTW][tripModeIk][tripOrigOB-1][tripStopOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeIk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%dtripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripOrigOB-1][tripStopOB-1] += tripUnit;
                    }
                    
                    // accumulate local trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKj != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKj, subModeIndices ) )
                            transitPerson[WTW][tripModeKj][tripStopOB-1][tripDestOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKj, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopOB-1][tripDestOB-1] += tripUnit;
                    }
                }
                else if ( tourMode == TourModeType.WP ) {
                    // accumulate premium trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeIk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeIk, subModeIndices ) )
                            transitPerson[WTW][tripModeIk][tripOrigOB-1][tripStopOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeIk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripOrigOB-1][tripStopOB-1] += tripUnit;
                    }
                    
                    // accumulate premium trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKj != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKj, subModeIndices ) )
                            transitPerson[WTW][tripModeKj][tripStopOB-1][tripDestOB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKj, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopOB-1][tripDestOB-1] += tripUnit;
                    }
                }
                
            }
            // no outbound stop for this tour
            else {                
                
                // for p&r half-tours in outbound direction.
                if ( tourMode == TourModeType.PL || tourMode == TourModeType.PP ) {
                    // accumulate pnr transit trip, if it's valid transit, in pTw table
                    if ( tripModeIsValidMode( tourSubmodeOB, subModeIndices ) )
                        transitPerson[PTW][tourSubmodeOB][tripOrigOB-1][tripDestOB-1] += tripUnit;
                    else
                        invalidTripMode( tourSubmodeOB, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB ) );
                }
                // for k&r half-tours in outbound direction, ik segment is k&r and kj segment is walkTransit.
                else if ( tourMode == TourModeType.KL || tourMode == TourModeType.KP ) {
                    // accumulate knr transit trip, if it's walid transit, in kTw table
                    if ( tripModeIsValidMode( tourSubmodeOB, subModeIndices ) )
                        transitPerson[KTW][tourSubmodeOB][tripOrigOB-1][tripDestOB-1] += tripUnit;
                    else
                        invalidTripMode( tourSubmodeOB, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB ) );
                }
                // for walk local half-tours in outbound direction, ik segment and kj segment are walkTransit.
                else if ( tourMode == TourModeType.WL || tourMode == TourModeType.WP ) {
                    // accumulate walk transit trip, if it's valid transit, in wTw table
                    if ( tripModeIsValidMode( tourSubmodeOB, subModeIndices ) )
                        transitPerson[WTW][tourSubmodeOB][tripOrigOB-1][tripDestOB-1] += tripUnit;
                    else
                        invalidTripMode( tourSubmodeOB, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeOB=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeOB, tripOrigOB, tripStopOB, tripDestOB ) );
                }
                
            }

        }
        
        
        // accumulate transit trips in the inbound direction if the specified period equals the tour end period.
        if ( period == periodIn ) {
                        
            // there is an inbound stop for this tour
            if ( tripStopIB > 0 ) {

                // if the trip mode for either segment of this inbound half tour is SubmodeType.NM, the trip mode is nonmotorized;
                // if it's > SubmodeType.NM or < 1, it's an error.  In either case, don't accumulate it.

                if ( tripModeJk < 1 || tripModeJk > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for first segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKi < 1 || tripModeKi > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for second segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }

                
                // for p&r half-tours in inbound direction, jk segment is walkTransit or walk and ki segment is p&r.
                if ( tourMode == TourModeType.PL ) {
                    // accumulate local trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeJk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeJk, subModeIndices ) )
                            transitPerson[WTW][tripModeJk][tripOrigIB-1][tripStopIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeJk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripOrigIB-1][tripStopIB-1] += tripUnit;
                    }
                   
                    // accumulate local trip, if it's transit for second segment only in wTp table
                    if ( tripModeIsValidMode( tripModeKi, subModeIndices ) )
                        transitPerson[WTP][tripModeKi][tripStopIB-1][tripDestIB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeKi, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                }
                else if ( tourMode == TourModeType.PP ) {
                    // accumulate premium trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeJk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeJk, subModeIndices ) )
                            transitPerson[WTW][tripModeJk][tripOrigIB-1][tripStopIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeJk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripOrigIB-1][tripStopIB-1] += tripUnit;
                    }
                   
                    // accumulate premium trip, if it's transit for second segment only in wTp table
                    if ( tripModeIsValidMode( tripModeKi, subModeIndices ) )
                        transitPerson[WTP][tripModeKi][tripStopIB-1][tripDestIB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeKi, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                }
                // for k&r half-tours in inbound direction, ik segment is walkTransit or walk and kj segment is k&r.
                else if ( tourMode == TourModeType.KL ) {
                    // accumulate local trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeJk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeJk, subModeIndices ) )
                            transitPerson[WTW][tripModeJk][tripOrigIB-1][tripStopIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeJk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripOrigIB-1][tripStopIB-1] += tripUnit;
                    }
                    
                    // accumulate local trip, if it's transit for second segment only in wTk table
                    if ( tripModeIsValidMode( tripModeKi, subModeIndices ) )
                        transitPerson[WTK][tripModeKi][tripStopIB-1][tripDestIB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeKi, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                }
                else if ( tourMode == TourModeType.KP ) {
                    // accumulate premium trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeJk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeJk, subModeIndices ) )
                            transitPerson[WTW][tripModeJk][tripOrigIB-1][tripStopIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeJk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripOrigIB-1][tripStopIB-1] += tripUnit;
                    }
                    
                    // accumulate premium trip, if it's transit for second segment only in wTk table
                    if ( tripModeIsValidMode( tripModeKi, subModeIndices ) )
                        transitPerson[WTK][tripModeKi][tripStopIB-1][tripDestIB-1] += tripUnit;
                    else
                        invalidTripMode( tripModeKi, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                }
                // for walk transit half-tours in inbound direction, jk segment and ki segment are either walkTransit or walk.
                else if ( tourMode == TourModeType.WL ) {
                    // accumulate local trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeJk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeJk, subModeIndices ) )
                            transitPerson[WTW][tripModeJk][tripOrigIB-1][tripStopIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeJk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripOrigIB-1][tripStopIB-1] += tripUnit;
                    }
                    
                    // accumulate local trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKi != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKi, subModeIndices ) )
                            transitPerson[WTW][tripModeKi][tripStopIB-1][tripDestIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKi, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopIB-1][tripDestIB-1] += tripUnit;
                    }
                }
                else if ( tourMode == TourModeType.WP ) {
                    // accumulate premium trip, if it's transit, for first segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeJk != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeJk, subModeIndices ) )
                            transitPerson[WTW][tripModeJk][tripStopIB-1][tripDestIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeJk, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopIB-1][tripDestIB-1] += tripUnit;
                    }
                    
                    // accumulate premium trip, if it's transit for second segment only in wTw table
                    // or if walk, accumulate in non-motorized table
                    if ( tripModeKi != SubmodeType.NM ){
                        if ( tripModeIsValidMode( tripModeKi, subModeIndices ) )
                            transitPerson[WTW][tripModeKi][tripStopIB-1][tripDestIB-1] += tripUnit;
                        else
                            invalidTripMode( tripModeKi, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    }
                    else {
                        accumulatedNonMotorized[tripStopIB-1][tripDestIB-1] += tripUnit;
                    }
                }
                
            }
            // no inbound stop for this tour
            else {                
                
                // for p&r half-tours in inbound direction, half-tour is p&r.
                if ( tourMode == TourModeType.PL || tourMode == TourModeType.PP ) {
                    // accumulate pnr trip, if it's valid transit, in wTp table
                    if ( tripModeIsValidMode( tourSubmodeIB, subModeIndices ) )
                        transitPerson[WTP][tourSubmodeIB][tripOrigIB-1][tripDestIB-1] += tripUnit;
                    else
                        invalidTripMode( tourSubmodeIB, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB ) );
                }
                // for k&r half-tours in inbound direction, half-tour is k&r.
                else if ( tourMode == TourModeType.KL || tourMode == TourModeType.KP ) {
                    // accumulate knr trip, if it's valid transit, in wTk table
                    if ( tripModeIsValidMode( tourSubmodeIB, subModeIndices ) )
                        transitPerson[WTK][tourSubmodeIB][tripOrigIB-1][tripDestIB-1] += tripUnit;
                    else
                        invalidTripMode( tourSubmodeIB, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB ) );
                }
                // for walk local half-tours in inbound direction, half-tour walkTransit.
                else if ( tourMode == TourModeType.WL || tourMode == TourModeType.WP ) {
                    // accumulate walk transit trip, if it's valid transit, in wTw table
                    if ( tripModeIsValidMode( tourSubmodeIB, subModeIndices ) )
                        transitPerson[WTW][tourSubmodeIB][tripOrigIB-1][tripDestIB-1] += tripUnit;
                    else
                        invalidTripMode( tourSubmodeIB, subModeIndices, String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tourSubmodeIB=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tourSubmodeIB, tripStopIB ) );
                }
                
            }
            
        }

    }
    
    

    private void accumulateNmTripsForTour ( Tour tour, boolean isJointTour, int period, float[][] nmPerson ) {


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
        
        int tourMode = tour.getMode();
    
    
        float tripUnit = 1.0f;
        if ( isJointTour ) {
            tripUnit = tour.getNumPersons();
        }
    

        // accumulate non-motorized trips in the outbound direction if the specified period equals the tour start period.
        if ( period == periodOut ) {

            if ( tripStopOB > 0 ) {

                // if the trip mode for either segment of this outbound half tour is SubmodeType.NM, the trip mode is nonmotorized;
                // if it's > SubmodeType.NM or < 1, it's an error.
                
                if ( tripModeIk < 1 || tripModeIk > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for first segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKj < 1 || tripModeKj > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for second segment in outbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tripOrigOB=%d, tripStopOB=%d, tripDestOB=%d, tripModeIk=%d, tripModeKj=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tripOrigOB, tripStopOB, tripDestOB, tripModeIk, tripModeKj ) );
                    throw (new RuntimeException());
                }

                
                nmPerson[tripOrigOB-1][tripStopOB-1] += tripUnit;
                nmPerson[tripStopOB-1][tripDestOB-1] += tripUnit;
                
            }
            else {
             
                nmPerson[tripOrigOB-1][tripDestOB-1] += tripUnit;

            }

        }

    
        // accumulate non-motorized trips in the inbound direction if the specified period equals the tour end period.
        if ( period == periodIn ) {

            if (tripStopIB > 0) {

                // if the trip mode for either segment of this inbound half tour is SubmodeType.NM, the trip mode is nonmotorized;
                // if it's > SubmodeType.NM or < 1, it's an error.
                
                if ( tripModeJk < 1 || tripModeJk > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for first segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }
                if ( tripModeKi < 1 || tripModeKi > SubmodeType.NM ) {
                    logger.fatal( "invalid tripMode for second segment in inbound half tour." );
                    logger.fatal( String.format( "person=%d, tourType=%d, tourOrder=%d, tourMode=%d, tripOrigIB=%d, tripStopIB=%d, tripDestIB=%d, tripModeJk=%d, tripModeKi=%d.", tour.getTourPerson(), tour.getTourType(), tour.getTourOrder(), tourMode, tripOrigIB, tripStopIB, tripDestIB, tripModeJk, tripModeKi ) );
                    throw (new RuntimeException());
                }


                nmPerson[tripOrigIB-1][tripStopIB-1] += tripUnit;
                nmPerson[tripStopIB-1][tripDestIB-1] += tripUnit;
                
            }
            else {
             
                nmPerson[tripOrigIB-1][tripDestIB-1] += tripUnit;

            }

        }

    }
    
    

    private void writeTpplusMatrices ( String tppFileName, float[][][] trips, String[] names, String[] descriptions ) {
        Matrix[] outputMatrices = new Matrix[trips.length-1];
        String[] newNames = new String[trips.length-1];            
        String[] newDescriptions = new String[trips.length-1];            
        logger.info( String.format("matrix totals for %d tables written to %s:", trips.length-1, tppFileName) );
        for (int i=1; i < trips.length; i++) {
            newNames[i-1] = names[i];
            newDescriptions[i-1] = descriptions[i];
            outputMatrices[i-1] = new Matrix( names[i], descriptions[i], trips[i] );
            logger.info( String.format("    [%d] %-16s: %.0f", i-1, newNames[i-1], outputMatrices[i-1].getSum()) );
        }            

        if (matrixSeverAddress == null){
            MatrixWriter tppWriter = MatrixWriter.createWriter (MatrixType.TPPLUS, new File( tppFileName ) );            
            tppWriter.writeMatrices(newNames, outputMatrices);
            trips = null;
        }
        else {
            MatrixDataServerRmi ms = new MatrixDataServerRmi( matrixSeverAddress, Integer.parseInt(matrixSeverPort), MatrixDataServer.MATRIX_DATA_SERVER_NAME);
            ms.writeTpplusMatrices(tppFileName, trips, newNames, newDescriptions);
            trips = null;
        }
    }



    public void writeTableRowSums ( Matrix outputMatrix ) {

        float rowSum = 0.0f;
        
        for (int i=1; i <= outputMatrix.getRowCount(); i++) {
            rowSum = outputMatrix.getRowSum( i );
            logger.info( "rowsum[" + i + "] = " +  rowSum);             
        }
    }



    public void writeDTMOutput ( Household[] hh ) {

        float[][] vocRatios = getVehOccRatios ();
        
        String modeName[] = { "", "sov", "hov", "walkLocal", "pnrLocal", "knrLocal", "walkPremium", "pnrPremium", "knrPremium", "nonmotor", "schoolbus" };        
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
        int[][] modalTours = new int[15 + 1][modeName.length];
        float[] totDist = new float[15 + 1];
        
        JointTour[] jt;
        Tour[] it;
        Tour[] st;
        
        ArrayList<String> tableHeadings = null;
        ArrayList<String> tableFormats = null;
        float[] tableData = null;
        String fieldFormat = null;
        
        
        int[] tripsByMode = new int[modeName.length];
        
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

            ChoiceModelApplication hDistOb =  new ChoiceModelApplication( (String)propertyMap.get ( "Model10.controlFile"), 1,  0, propertyMap, Household.class);
            ChoiceModelApplication hDistIb =  new ChoiceModelApplication( (String)propertyMap.get ( "Model10.controlFile"), 2,  0, propertyMap, Household.class);
            ChoiceModelApplication lDistOb =  new ChoiceModelApplication( (String)propertyMap.get ( "Model10.controlFile"), 3,  0, propertyMap, Household.class);
            ChoiceModelApplication lDistIb =  new ChoiceModelApplication( (String)propertyMap.get ( "Model10.controlFile"), 4,  0, propertyMap, Household.class);
            ChoiceModelApplication pDistOb =  new ChoiceModelApplication( (String)propertyMap.get ( "Model10.controlFile"), 5,  0, propertyMap, Household.class);
            ChoiceModelApplication pDistIb =  new ChoiceModelApplication( (String)propertyMap.get ( "Model10.controlFile"), 6,  0, propertyMap, Household.class);
            UtilityExpressionCalculator hDistUECob = hDistOb.getUEC();
            UtilityExpressionCalculator hDistUECib = hDistIb.getUEC();
            UtilityExpressionCalculator lDistUECob = lDistOb.getUEC();
            UtilityExpressionCalculator lDistUECib = lDistIb.getUEC();
            UtilityExpressionCalculator pDistUECob = pDistOb.getUEC();
            UtilityExpressionCalculator pDistUECib = pDistIb.getUEC();

            // same number of alternatives ob as there are ib, so can use sample array for both ob and ib.
            int[] hDistSample = new int[hDistUECob.getNumberOfAlternatives()+1];
            int[] lDistSample = new int[lDistUECob.getNumberOfAlternatives()+1];
            int[] pDistSample = new int[pDistUECob.getNumberOfAlternatives()+1];
            Arrays.fill ( hDistSample, 1 );
            Arrays.fill ( lDistSample, 1 );
            Arrays.fill ( pDistSample, 1 );
            
            double[] hResultsIJ = new double[hDistUECob.getNumberOfAlternatives()+1];
            double[] hResultsJI = new double[hDistUECob.getNumberOfAlternatives()+1];
            double[] hResultsIJPark = new double[hDistUECob.getNumberOfAlternatives()+1];
            double[] lResultsIJ = new double[lDistUECob.getNumberOfAlternatives()+1];
            double[] lResultsJI = new double[lDistUECob.getNumberOfAlternatives()+1];
            double[] pResultsIJ = new double[pDistUECob.getNumberOfAlternatives()+1];
            double[] pResultsJI = new double[pDistUECob.getNumberOfAlternatives()+1];
                    
            
            
            int maxPartySize = 0;
            for (int i=0; i < hh.length; i++) {
                if (hh[i].jointTours != null) {
                    for (int j=0; j < hh[i].jointTours.length; j++) {
                        if (hh[i].jointTours[j].getNumPersons() > maxPartySize)
                            maxPartySize = hh[i].jointTours[j].getNumPersons();
                    }
                }
            }
    

    
            tableHeadings = new ArrayList<String>();
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
            tableHeadings.add("SOV_Dist_IJ");
            tableHeadings.add("HOV_Dist_IJ");
            tableHeadings.add("SOV_Dist_JI");
            tableHeadings.add("HOV_Dist_JI");
            tableHeadings.add("SOV_Dist_IK");
            tableHeadings.add("HOV_Dist_IK");
            tableHeadings.add("SOV_Dist_KJ");
            tableHeadings.add("HOV_Dist_KJ");
            tableHeadings.add("SOV_Dist_JK");
            tableHeadings.add("HOV_Dist_JK");
            tableHeadings.add("SOV_Dist_KI");
            tableHeadings.add("HOV_Dist_KI");
            tableHeadings.add("M9_Parking_Zone");
            tableHeadings.add("SOV_Dist_Orig_Park");
            tableHeadings.add("HOV_Dist_Orig_Park");
            tableHeadings.add("SOV_Dist_Park_Dest");
            tableHeadings.add("HOV_Dist_Park_Dest");
            tableHeadings.add("LBS_IVT_IJ");
            tableHeadings.add("LBS_IVT_JI");
            tableHeadings.add("EBS_IVT_IJ");
            tableHeadings.add("EBS_IVT_JI");
            tableHeadings.add("BRT_IVT_IJ");
            tableHeadings.add("BRT_IVT_JI");
            tableHeadings.add("LRT_IVT_IJ");
            tableHeadings.add("LRT_IVT_JI");
            tableHeadings.add("CRL_IVT_IJ");
            tableHeadings.add("CRL_IVT_JI");
            tableHeadings.add("VEH_OCC_RATIO_OB");
            tableHeadings.add("VEH_OCC_RATIO_IB");

            
            tableFormats = new ArrayList<String>();
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
            tableFormats.add("%.2f");     //SOV_Dist_IJ
            tableFormats.add("%.2f");     //HOV_Dist_IJ
            tableFormats.add("%.2f");     //SOV_Dist_JI
            tableFormats.add("%.2f");     //HOV_Dist_JI
            tableFormats.add("%.2f");     //SOV_Dist_IK
            tableFormats.add("%.2f");     //HOV_Dist_IK
            tableFormats.add("%.2f");     //SOV_Dist_KJ
            tableFormats.add("%.2f");     //HOV_Dist_KJ
            tableFormats.add("%.2f");     //SOV_Dist_JK
            tableFormats.add("%.2f");     //HOV_Dist_JK
            tableFormats.add("%.2f");     //SOV_Dist_KI
            tableFormats.add("%.2f");     //HOV_Dist_KI
            tableFormats.add("%.2f");     //M9_Parking_Zone
            tableFormats.add("%.2f");     //SOV_Dist_Orig_Park
            tableFormats.add("%.2f");     //HOV_Dist_Orig_Park
            tableFormats.add("%.2f");     //SOV_Dist_Park_Dest
            tableFormats.add("%.2f");     //HOV_Dist_Park_Dest
            tableFormats.add("%.2f");     //LBS_IVT_OB
            tableFormats.add("%.2f");     //LBS_IVT_IB
            tableFormats.add("%.2f");     //EBS_IVT_OB
            tableFormats.add("%.2f");     //EBS_IVT_IB
            tableFormats.add("%.2f");     //BRT_IVT_OB
            tableFormats.add("%.2f");     //BRT_IVT_IB
            tableFormats.add("%.2f");     //LRT_IVT_OB
            tableFormats.add("%.2f");     //LRT_IVT_IB
            tableFormats.add("%.2f");     //CRL_IVT_OB
            tableFormats.add("%.2f");     //CRL_IVT_IB
            tableFormats.add("%.2f");     //VEH_OCC_OB
            tableFormats.add("%.2f");     //VEH_OCC_IB

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



                        // distance and IVT sheets are coded with od for outbound and do for inbound, so don't need to change index values
                        index.setOriginZone( it[t].getOrigTaz() );
                        index.setDestZone( it[t].getDestTaz() );
                        index.setStopZone( it[t].getStopLocOB() );
                        hResultsIJ = hDistUECob.solve( index, new Object(), hDistSample );
                        lResultsIJ = lDistUECob.solve( index, new Object(), lDistSample );
                        pResultsIJ = pDistUECob.solve( index, new Object(), pDistSample );
                        index.setStopZone( it[t].getStopLocIB() );
                        hResultsJI = hDistUECib.solve( index, new Object(), hDistSample );
                        lResultsJI = lDistUECib.solve( index, new Object(), lDistSample );
                        pResultsJI = pDistUECib.solve( index, new Object(), pDistSample );
                        index.setStopZone( it[t].getChosenPark() );
                        hResultsIJPark = hDistUECob.solve( index, new Object(), hDistSample );
                        
                    
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
                        tableData[k+23] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ ) ];
                        tableData[k+24] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ ) ];
                        tableData[k+25] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI ) ];
                        tableData[k+26] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI ) ];
                        tableData[k+27] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+28] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+29] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+30] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+31] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JK ) ];
                        tableData[k+32] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JK ) ];
                        tableData[k+33] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), KI ) ];
                        tableData[k+34] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), KI ) ];
                        tableData[k+35] = it[t].getChosenPark();
                        tableData[k+36] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+37] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+38] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+39] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        if (it[t].getMode() >= 3 && it[t].getMode() <= 5) {
                            int obIndex = getLocalIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ );
                            int ibIndex = getLocalIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI );
                            tableData[k+40] = (float)lResultsIJ[obIndex];
                            tableData[k+41] = (float)lResultsJI[ibIndex];
                            tableData[k+42] = (float)lResultsIJ[obIndex+1];
                            tableData[k+43] = (float)lResultsJI[ibIndex+1];
                            tableData[k+44] = (float)lResultsIJ[obIndex+2];
                            tableData[k+45] = (float)lResultsJI[ibIndex+2];
                            tableData[k+46] = (float)lResultsIJ[obIndex+3];
                            tableData[k+47] = (float)lResultsJI[ibIndex+3];
                            tableData[k+48] = (float)lResultsIJ[obIndex+4];
                            tableData[k+49] = (float)lResultsJI[ibIndex+4];
                        }
                        else if (it[t].getMode() >= 6 && it[t].getMode() <= 8) {
                            int obIndex = getPremiumIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ );
                            int ibIndex = getPremiumIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI );
                            tableData[k+40] = (float)pResultsIJ[obIndex];
                            tableData[k+41] = (float)pResultsJI[ibIndex];
                            tableData[k+42] = (float)pResultsIJ[obIndex+1];
                            tableData[k+43] = (float)pResultsJI[ibIndex+1];
                            tableData[k+44] = (float)pResultsIJ[obIndex+2];
                            tableData[k+45] = (float)pResultsJI[ibIndex+2];
                            tableData[k+46] = (float)pResultsIJ[obIndex+3];
                            tableData[k+47] = (float)pResultsJI[ibIndex+3];
                            tableData[k+48] = (float)pResultsIJ[obIndex+4];
                            tableData[k+49] = (float)pResultsJI[ibIndex+4];
                        }
                        else {
                            tableData[k+40] = 0.0f;
                            tableData[k+41] = 0.0f;
                            tableData[k+42] = 0.0f;
                            tableData[k+43] = 0.0f;
                            tableData[k+44] = 0.0f;
                            tableData[k+45] = 0.0f;
                            tableData[k+46] = 0.0f;
                            tableData[k+47] = 0.0f;
                            tableData[k+48] = 0.0f;
                            tableData[k+49] = 0.0f;
                        }
                        
                        if ( it[t].getMode() == HOV_MODE ){
                            tableData[k+50] = vocRatios[it[t].getTourType()][(int)tableData[k+9]];
                            tableData[k+51] = vocRatios[it[t].getTourType()][(int)tableData[k+10]];
                        }
                        else {
                            tableData[k+50] = 1.0f;
                            tableData[k+51] = 1.0f;
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
                        totDist[tlIndex] += (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ ) ];
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
                    


                        
                        // distance and IVT sheets are coded with od for outbound and do for inbound, so don't need to change index values
                        index.setOriginZone( jt[t].getOrigTaz() );
                        index.setDestZone( jt[t].getDestTaz() );
                        index.setStopZone( jt[t].getStopLocOB() );
                        hResultsIJ = hDistUECob.solve( index, new Object(), hDistSample );
                        lResultsIJ = lDistUECob.solve( index, new Object(), lDistSample );
                        pResultsIJ = pDistUECob.solve( index, new Object(), pDistSample );
                        index.setStopZone( jt[t].getStopLocIB() );
                        hResultsJI = hDistUECib.solve( index, new Object(), hDistSample );
                        lResultsJI = lDistUECib.solve( index, new Object(), lDistSample );
                        pResultsJI = pDistUECib.solve( index, new Object(), pDistSample );
                        index.setStopZone( jt[t].getChosenPark() );
                        hResultsIJPark = hDistUECob.solve( index, new Object(), hDistSample );
                        
                    
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
                        tableData[k+23] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IJ ) ];
                        tableData[k+24] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IJ ) ];
                        tableData[k+25] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), JI ) ];
                        tableData[k+26] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), JI ) ];
                        tableData[k+27] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+28] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+29] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+30] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+31] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), JK ) ];
                        tableData[k+32] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), JK ) ];
                        tableData[k+33] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), KI ) ];
                        tableData[k+34] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), KI ) ];
                        tableData[k+35] = jt[t].getChosenPark();
                        tableData[k+36] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+37] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+38] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+39] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), KJ ) ];
                        if (jt[t].getMode() >= 3 && jt[t].getMode() <= 5) {
                            int obIndex = getLocalIvtResultIndex( jt[t].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IJ );
                            int ibIndex = getLocalIvtResultIndex( jt[t].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), JI );
                            tableData[k+40] = (float)lResultsIJ[obIndex];
                            tableData[k+41] = (float)lResultsJI[ibIndex];
                            tableData[k+42] = (float)lResultsIJ[obIndex+1];
                            tableData[k+43] = (float)lResultsJI[ibIndex+1];
                            tableData[k+44] = (float)lResultsIJ[obIndex+2];
                            tableData[k+45] = (float)lResultsJI[ibIndex+2];
                            tableData[k+46] = (float)lResultsIJ[obIndex+3];
                            tableData[k+47] = (float)lResultsJI[ibIndex+3];
                            tableData[k+48] = (float)lResultsIJ[obIndex+4];
                            tableData[k+49] = (float)lResultsJI[ibIndex+4];
                        }
                        else if (jt[t].getMode() >= 6 && jt[t].getMode() <= 8) {
                            int obIndex = getPremiumIvtResultIndex( jt[t].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IJ );
                            int ibIndex = getPremiumIvtResultIndex( jt[t].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() ), JI );
                            tableData[k+40] = (float)pResultsIJ[obIndex];
                            tableData[k+41] = (float)pResultsJI[ibIndex];
                            tableData[k+42] = (float)pResultsIJ[obIndex+1];
                            tableData[k+43] = (float)pResultsJI[ibIndex+1];
                            tableData[k+44] = (float)pResultsIJ[obIndex+2];
                            tableData[k+45] = (float)pResultsJI[ibIndex+2];
                            tableData[k+46] = (float)pResultsIJ[obIndex+3];
                            tableData[k+47] = (float)pResultsJI[ibIndex+3];
                            tableData[k+48] = (float)pResultsIJ[obIndex+4];
                            tableData[k+49] = (float)pResultsJI[ibIndex+4];
                        }
                        else {
                            tableData[k+40] = 0.0f;
                            tableData[k+41] = 0.0f;
                            tableData[k+42] = 0.0f;
                            tableData[k+43] = 0.0f;
                            tableData[k+44] = 0.0f;
                            tableData[k+45] = 0.0f;
                            tableData[k+46] = 0.0f;
                            tableData[k+47] = 0.0f;
                            tableData[k+48] = 0.0f;
                            tableData[k+49] = 0.0f;
                        }
                        
                        tableData[k+50] = 1.0f;
                        tableData[k+51] = 1.0f;

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
                        totDist[tlIndex] += (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() ), IJ ) ];
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
                    


                        
                        // distance and IVT sheets are coded with od for outbound and do for inbound, so don't need to change index values
                        index.setOriginZone( it[t].getOrigTaz() );
                        index.setDestZone( it[t].getDestTaz() );
                        index.setStopZone( it[t].getStopLocOB() );
                        hResultsIJ = hDistUECob.solve( index, new Object(), hDistSample );
                        lResultsIJ = lDistUECob.solve( index, new Object(), lDistSample );
                        pResultsIJ = pDistUECob.solve( index, new Object(), pDistSample );
                        index.setStopZone( it[t].getStopLocIB() );
                        hResultsJI = hDistUECib.solve( index, new Object(), hDistSample );
                        lResultsJI = lDistUECib.solve( index, new Object(), lDistSample );
                        pResultsJI = pDistUECib.solve( index, new Object(), pDistSample );
                        index.setStopZone( it[t].getChosenPark() );
                        hResultsIJPark = hDistUECob.solve( index, new Object(), hDistSample );
                        
                        
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
                        tableData[k+23] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ ) ];
                        tableData[k+24] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ ) ];
                        tableData[k+25] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI ) ];
                        tableData[k+26] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI ) ];
                        tableData[k+27] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+28] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+29] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+30] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+31] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JK ) ];
                        tableData[k+32] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JK ) ];
                        tableData[k+33] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), KI ) ];
                        tableData[k+34] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), KI ) ];
                        tableData[k+35] = it[t].getChosenPark();
                        tableData[k+36] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+37] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IK ) ];
                        tableData[k+38] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        tableData[k+39] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), KJ ) ];
                        if (it[t].getMode() >= 3 && it[t].getMode() <= 5) {
                            int obIndex = getLocalIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ );
                            int ibIndex = getLocalIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI );
                            tableData[k+40] = (float)lResultsIJ[obIndex];
                            tableData[k+41] = (float)lResultsJI[ibIndex];
                            tableData[k+42] = (float)lResultsIJ[obIndex+1];
                            tableData[k+43] = (float)lResultsJI[ibIndex+1];
                            tableData[k+44] = (float)lResultsIJ[obIndex+2];
                            tableData[k+45] = (float)lResultsJI[ibIndex+2];
                            tableData[k+46] = (float)lResultsIJ[obIndex+3];
                            tableData[k+47] = (float)lResultsJI[ibIndex+3];
                            tableData[k+48] = (float)lResultsIJ[obIndex+4];
                            tableData[k+49] = (float)lResultsJI[ibIndex+4];
                        }
                        else if (it[t].getMode() >= 6 && it[t].getMode() <= 8) {
                            int obIndex = getPremiumIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ );
                            int ibIndex = getPremiumIvtResultIndex( it[t].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() ), JI );
                            tableData[k+40] = (float)pResultsIJ[obIndex];
                            tableData[k+41] = (float)pResultsJI[ibIndex];
                            tableData[k+42] = (float)pResultsIJ[obIndex+1];
                            tableData[k+43] = (float)pResultsJI[ibIndex+1];
                            tableData[k+44] = (float)pResultsIJ[obIndex+2];
                            tableData[k+45] = (float)pResultsJI[ibIndex+2];
                            tableData[k+46] = (float)pResultsIJ[obIndex+3];
                            tableData[k+47] = (float)pResultsJI[ibIndex+3];
                            tableData[k+48] = (float)pResultsIJ[obIndex+4];
                            tableData[k+49] = (float)pResultsJI[ibIndex+4];
                        }
                        else {
                            tableData[k+40] = 0.0f;
                            tableData[k+41] = 0.0f;
                            tableData[k+42] = 0.0f;
                            tableData[k+43] = 0.0f;
                            tableData[k+44] = 0.0f;
                            tableData[k+45] = 0.0f;
                            tableData[k+46] = 0.0f;
                            tableData[k+47] = 0.0f;
                            tableData[k+48] = 0.0f;
                            tableData[k+49] = 0.0f;
                        }
                        
                        if ( it[t].getMode() == HOV_MODE ){
                            tableData[k+50] = vocRatios[it[t].getTourType()][(int)tableData[k+9]];
                            tableData[k+51] = vocRatios[it[t].getTourType()][(int)tableData[k+10]];
                        }
                        else {
                            tableData[k+50] = 1.0f;
                            tableData[k+51] = 1.0f;
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
                        totDist[tlIndex] += (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() ), IJ ) ];
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
                    


                                    // distance and IVT sheets are coded with od for outbound and do for inbound, so don't need to change index values
                                    index.setOriginZone( st[s].getOrigTaz() );
                                    index.setDestZone( st[s].getDestTaz() );
                                    index.setStopZone( st[s].getStopLocOB() );
                                    hResultsIJ = hDistUECob.solve( index, new Object(), hDistSample );
                                    lResultsIJ = lDistUECob.solve( index, new Object(), lDistSample );
                                    pResultsIJ = pDistUECob.solve( index, new Object(), pDistSample );
                                    index.setStopZone( st[s].getStopLocIB() );
                                    hResultsJI = hDistUECib.solve( index, new Object(), hDistSample );
                                    lResultsJI = lDistUECib.solve( index, new Object(), lDistSample );
                                    pResultsJI = pDistUECib.solve( index, new Object(), pDistSample );
                                    index.setStopZone( st[s].getChosenPark() );
                                    hResultsIJPark = hDistUECob.solve( index, new Object(), hDistSample );
                                    
                                    
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
                                    tableData[k+23] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IJ ) ];
                                    tableData[k+24] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IJ ) ];
                                    tableData[k+25] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), JI ) ];
                                    tableData[k+26] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), JI ) ];
                                    tableData[k+27] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IK ) ];
                                    tableData[k+28] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IK ) ];
                                    tableData[k+29] = (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), KJ ) ];
                                    tableData[k+30] = (float)hResultsIJ[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), KJ ) ];
                                    tableData[k+31] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), JK ) ];
                                    tableData[k+32] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), JK ) ];
                                    tableData[k+33] = (float)hResultsJI[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), KI ) ];
                                    tableData[k+34] = (float)hResultsJI[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), KI ) ];
                                    tableData[k+35] = st[s].getChosenPark();
                                    tableData[k+36] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IK ) ];
                                    tableData[k+37] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IK ) ];
                                    tableData[k+38] = (float)hResultsIJPark[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), KJ ) ];
                                    tableData[k+39] = (float)hResultsIJPark[ getHwyDistResultIndex( HOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), KJ ) ];
                                    if (st[s].getMode() >= 3 && st[s].getMode() <= 5) {
                                        int obIndex = getLocalIvtResultIndex( st[s].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IJ );
                                        int ibIndex = getLocalIvtResultIndex( st[s].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), JI );
                                        tableData[k+40] = (float)lResultsIJ[obIndex];
                                        tableData[k+41] = (float)lResultsJI[ibIndex];
                                        tableData[k+42] = (float)lResultsIJ[obIndex+1];
                                        tableData[k+43] = (float)lResultsJI[ibIndex+1];
                                        tableData[k+44] = (float)lResultsIJ[obIndex+2];
                                        tableData[k+45] = (float)lResultsJI[ibIndex+2];
                                        tableData[k+46] = (float)lResultsIJ[obIndex+3];
                                        tableData[k+47] = (float)lResultsJI[ibIndex+3];
                                        tableData[k+48] = (float)lResultsIJ[obIndex+4];
                                        tableData[k+49] = (float)lResultsJI[ibIndex+4];
                                    }
                                    else if (st[s].getMode() >= 6 && st[s].getMode() <= 8) {
                                        int obIndex = getPremiumIvtResultIndex( st[s].getMode(), com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IJ );
                                        int ibIndex = getPremiumIvtResultIndex( st[s].getMode(), com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() ), JI );
                                        tableData[k+40] = (float)pResultsIJ[obIndex];
                                        tableData[k+41] = (float)pResultsJI[ibIndex];
                                        tableData[k+42] = (float)pResultsIJ[obIndex+1];
                                        tableData[k+43] = (float)pResultsJI[ibIndex+1];
                                        tableData[k+44] = (float)pResultsIJ[obIndex+2];
                                        tableData[k+45] = (float)pResultsJI[ibIndex+2];
                                        tableData[k+46] = (float)pResultsIJ[obIndex+3];
                                        tableData[k+47] = (float)pResultsJI[ibIndex+3];
                                        tableData[k+48] = (float)pResultsIJ[obIndex+4];
                                        tableData[k+49] = (float)pResultsJI[ibIndex+4];
                                    }
                                    else {
                                        tableData[k+40] = 0.0f;
                                        tableData[k+41] = 0.0f;
                                        tableData[k+42] = 0.0f;
                                        tableData[k+43] = 0.0f;
                                        tableData[k+44] = 0.0f;
                                        tableData[k+45] = 0.0f;
                                        tableData[k+46] = 0.0f;
                                        tableData[k+47] = 0.0f;
                                        tableData[k+48] = 0.0f;
                                        tableData[k+49] = 0.0f;
                                    }
                                    
                                    if ( st[s].getMode() == HOV_MODE ){
                                        tableData[k+50] = vocRatios[it[t].getTourType()][(int)tableData[k+9]];
                                        tableData[k+51] = vocRatios[it[t].getTourType()][(int)tableData[k+10]];
                                    }
                                    else {
                                        tableData[k+50] = 1.0f;
                                        tableData[k+51] = 1.0f;
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
                                    totDist[tlIndex] += (float)hResultsIJ[ getHwyDistResultIndex( SOV_MODE, com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() ), IJ ) ];
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
            for (int j=1; j < modalTours[i].length; j++)
                logger.info (modeName[j] + "=" + modalTours[i][j]);
        }
        logger.info ("");
        logger.info ("");
    
    
        logger.info ("Tour Length Data:");
        for (int i=1; i <= 15; i++)
            logger.info (totDist[i] + ", " + totTours[i]);
    
    
        logger.info ("Tour Mode Share Data:");
        for (int i=1; i <= 15; i++) {
            String logString = Integer.toString(modalTours[i][1]);
            for (int j=2; j < modalTours[i].length; j++)
                logString += ", " + modalTours[i][j];
            logger.info (logString);
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


    private int getHwyDistResultIndex( int mode, int skimPeriod, int segment ){
        
        int returnValue = -1;
        
        // get the UEC alternative number for the mode/skim period combination
        if ( segment == IJ || segment == JI ){
            if ( mode == SOV_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 1;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 2;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 3;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 4;
            }
            else if ( mode == HOV_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 5;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 6;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 7;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 8;
            }
        }
        else if ( segment == IK || segment == JK ){
            if ( mode == SOV_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 9;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 10;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 11;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 12;
            }
            else if ( mode == HOV_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 13;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 14;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 15;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 16;
            }
        }
        else if ( segment == KJ || segment == KI ){
            if ( mode == SOV_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 17;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 18;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 19;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 20;
            }
            else if ( mode == HOV_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 21;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 22;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 23;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 24;
            }
        }
    
        // return the alternative number - 1, sonce the result array from the UEC is 0-based.
        return returnValue - 1;
    }
    
    
    private int getPremiumIvtResultIndex( int mode, int skimPeriod, int segment ){
        
        int returnValue = -1;
        
        // get the UEC alternative number for the mode/skim period combination
        // The EBS, BRT, LRT, and CRL indices follow the LBS value.
        if ( segment == IJ || segment == JI ){
            if ( mode == WP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 1;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 6;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 11;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 16;
            }
            else if ( mode == PP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 21;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 26;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 31;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 36;
            }
            else if ( mode == KP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 41;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 46;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 51;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 56;
            }
        }
        else if ( segment == IK || segment == JK ){
            if ( mode == WP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 61;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 66;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 71;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 76;
            }
            else if ( mode == PP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 81;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 86;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 91;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 96;
            }
            else if ( mode == KP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 101;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 106;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 111;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 116;
            }
        }
        else if ( segment == KJ || segment == KI ){
            if ( mode == WP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 121;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 126;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 131;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 136;
            }
            else if ( mode == PP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 141;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 146;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 151;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 156;
            }
            else if ( mode == KP_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 161;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 166;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 171;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 176;
            }
        }
    
        // return the alternative number - 1, sonce the result array from the UEC is 0-based.
        return returnValue - 1;
    }

    
    private int getLocalIvtResultIndex( int mode, int skimPeriod, int segment ){
        
        int returnValue = -1;
        
        // get the UEC alternative number of the LBS skim for the mode/skim period combination.
        if ( segment == IJ || segment == JI ){
            if ( mode == WL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 1;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 2;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 3;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 4;
            }
            else if ( mode == PL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 5;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 6;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 7;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 8;
            }
            else if ( mode == KL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 9;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 10;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 11;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 12;
            }
        }
        else if ( segment == IK || segment == JK ){
            if ( mode == WL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 13;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 14;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 15;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 16;
            }
            else if ( mode == PL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 17;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 18;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 19;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 20;
            }
            else if ( mode == KL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 21;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 22;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 23;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 24;
            }
        }
        else if ( segment == KJ || segment == KI ){
            if ( mode == WL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 25;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 26;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 27;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 28;
            }
            else if ( mode == PL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 29;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 30;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 31;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 32;
            }
            else if ( mode == KL_MODE){
                if ( skimPeriod == AM_SKIM_PERIOD )
                    returnValue = 33;
                else if ( skimPeriod == PM_SKIM_PERIOD )
                    returnValue = 34;
                else if ( skimPeriod == MD_SKIM_PERIOD )
                    returnValue = 35;
                else if ( skimPeriod == NT_SKIM_PERIOD )
                    returnValue = 36;
            }
        }
    
        // return the alternative number - 1, sonce the result array from the UEC is 0-based.
        return returnValue - 1;
    }
    
}
