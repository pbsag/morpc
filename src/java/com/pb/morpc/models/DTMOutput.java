package com.pb.morpc.models;

import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.*;

import com.pb.morpc.synpop.SyntheticPopulation;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.calculator.IndexValues;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.*;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */
public class DTMOutput implements java.io.Serializable {

    static Logger logger = Logger.getLogger("com.pb.morpc.models");


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


		logger.info ("writing person and vehicle trip matrices.");

		//Write out trip matrices. We want trips by 5 modes and 4 time-of-day periods.
		int start;
		int end;
		int tod;
		int periodOut;
		int periodIn;
		int modeAlt;
		int numPersons;
		int hh_id;
		int tripOrigOB;
		int tripDestOB;
		int tripOrigIB;
		int tripDestIB;
		int tripPark;
		int tripStopOB;
		int tripStopIB;
		int tripModeIk;
		int tripModeKj;
		int tripModeJk;
		int tripModeKi;
		int tourSubmodeOB;
		int tourSubmodeIB;
		int tourType = 0;
		float[][] personTripTable = new float[numberOfZones][numberOfZones];
		float[][] vehicleTripTable = new float[numberOfZones][numberOfZones];
		float vocRatioOB = 1.0f;
		float vocRatioIB = 1.0f;
		Tour[] it;
		Tour[] st;
		JointTour[] jt;


		float[][] vocRatios = getVehOccRatios ();

		int m=0;
		for (int w=1; w <= 2; w++) {
			for (int v=1; v <= 6; v++) {
				for (int p=1; p <= 4; p++) {
					for (int u=1; u <= 5; u++) {
					    
					    switch (v) {
							case 1:
							case 2:
							case 3:
								m = v;
								break;
							case 4:
							case 5:
								m = 4;
								break;
							case 6:
								m = 5;
								break;
					    }
					    
					    
					
						if (m != 3 && m != 4)
							u = 6;
	
						
						for (int i=0; i < hh.length; i++) {
		
							hh_id = hh[i].getID();
			
			
							// write trips for individual mandatory tours
							if ( w == 1 ) {
								it = hh[i].getMandatoryTours();
								if (it != null) {
			
									for (int t=0; t < it.length; t++) {
			
			
										tod = it[t].getTimeOfDayAlt();
										if (tod < 1)
										    continue;
										    
										start = com.pb.morpc.models.TODDataManager.getTodStartHour ( tod );
										end = com.pb.morpc.models.TODDataManager.getTodEndHour ( tod );
										periodOut = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod ( tod );
										periodIn = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod ( tod );

										modeAlt = it[t].getMode();
										tourType = it[t].getTourType();
										tripPark = it[t].getChosenPark();
										tripOrigOB = it[t].getOrigTaz();
										tripDestIB = it[t].getOrigTaz();
										if (tripPark > 0) {
											tripDestOB = tripPark;
											tripOrigIB = tripPark;
										}
										else {
											tripDestOB = it[t].getDestTaz();
											tripOrigIB = it[t].getDestTaz();
										}
										if (tripOrigOB < 1 || tripOrigIB < 1 || tripDestOB < 1 || tripDestIB < 1)
											continue;

										tripStopOB = it[t].getStopLocOB();
										tripStopIB = it[t].getStopLocIB();
										if (tripStopOB < 0 || tripStopIB < 0)
											continue;

										tripModeIk = it[t].getTripIkMode();
										tripModeKj = it[t].getTripKjMode();
										tripModeJk = it[t].getTripJkMode();
										tripModeKi = it[t].getTripKiMode();
										tourSubmodeOB = it[t].getSubmodeOB();
										tourSubmodeIB = it[t].getSubmodeIB();
			
										vocRatioOB = vocRatios[tourType][periodOut];
										vocRatioIB = vocRatios[tourType][periodIn];
		
		
										
										if ( modeAlt == m || (v == 3 && modeAlt == 4) ) {
											if ( periodOut == p ) {
												if (u <= 5) {
													if (tripStopOB > 0) {
														if (v == 3 && modeAlt == 4) {
															if (tripModeKj == u)
																personTripTable[tripStopOB-1][tripDestOB-1]++;
														}
														else if (v == 3) {
															if (tripModeIk == u)
																personTripTable[tripOrigOB-1][tripStopOB-1]++;
															if (tripModeKj == u)
																personTripTable[tripStopOB-1][tripDestOB-1]++;
														}
														else if (v == 4) {
															if (tripModeIk == u)
																personTripTable[tripOrigOB-1][tripStopOB-1]++;
														}
													}
													else {
														if (m == modeAlt && (v == 3 || v == 4)) {
														    if (tourSubmodeOB == u)
														        personTripTable[tripOrigOB-1][tripDestOB-1]++;
														}
													}
												}
												else {
													if (tripStopOB > 0) {
														personTripTable[tripOrigOB-1][tripStopOB-1]++;
														personTripTable[tripStopOB-1][tripDestOB-1]++;
														if ( modeAlt == 1 ) {
															vehicleTripTable[tripOrigOB-1][tripStopOB-1] += 1;
															vehicleTripTable[tripStopOB-1][tripDestOB-1] += 1;
														}
														else if ( modeAlt == 2 ) {
															vehicleTripTable[tripOrigOB-1][tripStopOB-1] += 1.0/vocRatioOB;
															vehicleTripTable[tripStopOB-1][tripDestOB-1] += 1.0/vocRatioOB;
														}
													}
													else {
														personTripTable[tripOrigOB-1][tripDestOB-1]++;
														if ( modeAlt == 1 )
															vehicleTripTable[tripOrigOB-1][tripDestOB-1] += 1;
														else if ( modeAlt == 2 )
															vehicleTripTable[tripOrigOB-1][tripDestOB-1] += 1.0/vocRatioOB;
													}
												}
											}
											
											if ( periodIn == p ) {
												if (u <= 5) {
													if (tripStopIB > 0) {
														if (v == 3 && modeAlt == 4) {
															if (tripModeJk == u)
																personTripTable[tripOrigIB-1][tripStopIB-1]++;
														}
														else if (v == 3) {
															if (tripModeJk == u)
																personTripTable[tripOrigIB-1][tripStopIB-1]++;
															if (tripModeKi == u)
																personTripTable[tripStopIB-1][tripDestIB-1]++;
														}
														else if (v == 5) {
															if (tripModeKi == u)
																personTripTable[tripStopIB-1][tripDestIB-1]++;
														}
													}
													else {
														if (m == modeAlt && (v==3 || v==5)) {
															if (tourSubmodeIB == u)
																personTripTable[tripOrigIB-1][tripDestIB-1]++;
														}
													}
												}
												else {
													if (tripStopIB > 0) {
														personTripTable[tripOrigIB-1][tripStopIB-1]++;
														personTripTable[tripStopIB-1][tripDestIB-1]++;
														if ( modeAlt == 1 ) {
															vehicleTripTable[tripOrigIB-1][tripStopIB-1] += 1;
															vehicleTripTable[tripStopIB-1][tripDestIB-1] += 1;
														}
														else if ( modeAlt == 2 ) {
															vehicleTripTable[tripOrigIB-1][tripStopIB-1] += 1.0/vocRatioIB;
															vehicleTripTable[tripStopIB-1][tripDestIB-1] += 1.0/vocRatioIB;
														}
													}
													else {
														personTripTable[tripOrigIB-1][tripDestIB-1]++;
														if ( modeAlt == 1 )
															vehicleTripTable[tripOrigIB-1][tripDestIB-1] += 1;
														else if ( modeAlt == 2 )
															vehicleTripTable[tripOrigIB-1][tripDestIB-1] += 1.0/vocRatioIB;
													}
												}
		
											}
										}
				
									}
			
								}
			
							}
							
							// write trips for all non-mandatory tours
							else {
							
							// write trips for joint tours
								jt = hh[i].getJointTours();
								if (jt != null) {
			
									for (int t=0; t < jt.length; t++) {
			
										tod = jt[t].getTimeOfDayAlt();
										if (tod < 1)
											continue;
										    
										start = com.pb.morpc.models.TODDataManager.getTodStartHour ( tod );
										end = com.pb.morpc.models.TODDataManager.getTodEndHour ( tod );
										periodOut = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod ( tod );
										periodIn = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod ( tod );

										modeAlt = jt[t].getMode();
										tripPark = jt[t].getChosenPark();
										tripOrigOB = jt[t].getOrigTaz();
										tripDestIB = jt[t].getOrigTaz();
										if (tripPark > 0) {
											tripDestOB = tripPark;
											tripOrigIB = tripPark;
										}
										else {
											tripDestOB = jt[t].getDestTaz();
											tripOrigIB = jt[t].getDestTaz();
										}
										if (tripOrigOB < 1 || tripOrigIB < 1 || tripDestOB < 1 || tripDestIB < 1)
											continue;

										tripStopOB = jt[t].getStopLocOB();
										tripStopIB = jt[t].getStopLocIB();
										if (tripStopOB < 0 || tripStopIB < 0)
											continue;

										tripModeIk = jt[t].getTripIkMode();
										tripModeKj = jt[t].getTripKjMode();
										tripModeJk = jt[t].getTripJkMode();
										tripModeKi = jt[t].getTripKiMode();
										tourSubmodeOB = jt[t].getSubmodeOB();
										tourSubmodeIB = jt[t].getSubmodeIB();
										numPersons = jt[t].getNumPersons(); 
			
										
										
										if ( modeAlt == m || (v == 3 && modeAlt == 4) ) {
											if ( periodOut == p ) {
												if (u <= 5) {
													if (tripStopOB > 0) {
														if (v == 3 && modeAlt == 4) {
															if (tripModeKj == u)
															    personTripTable[tripStopOB-1][tripDestOB-1] += numPersons;
														}
														else if (v == 3) {
															if (tripModeIk == u)
															    personTripTable[tripOrigOB-1][tripStopOB-1] += numPersons;
															if (tripModeKj == u)
															    personTripTable[tripStopOB-1][tripDestOB-1] += numPersons;
														}
														else if (v == 4) {
															if (tripModeIk == u)
															    personTripTable[tripOrigOB-1][tripStopOB-1] += numPersons;
														}
													}
													else {
														if (m == modeAlt && (v == 3 || v == 4)) {
															if (tourSubmodeOB == u)
																personTripTable[tripOrigOB-1][tripDestOB-1] += numPersons;
														}
													}
												}
												else {
													if (tripStopOB > 0) {
														personTripTable[tripOrigOB-1][tripStopOB-1] += numPersons;
														personTripTable[tripStopOB-1][tripDestOB-1] += numPersons;
														if ( modeAlt == 2 ) {
															vehicleTripTable[tripOrigOB-1][tripStopOB-1] += 1;
															vehicleTripTable[tripStopOB-1][tripDestOB-1] += 1;
														}
													}
													else {
														personTripTable[tripOrigOB-1][tripDestOB-1] += numPersons;
														if ( modeAlt == 2 )
															vehicleTripTable[tripOrigOB-1][tripDestOB-1] += 1;
													}
												}
											}
											
											if ( periodIn == p ) {
												if (u <= 5) {
													if (tripStopIB > 0) {
														if (v == 3 && modeAlt == 4) {
															if (tripModeJk == u)
																personTripTable[tripOrigIB-1][tripStopIB-1] += numPersons;
														}
														else if (v == 3) {
															if (tripModeJk == u)
																personTripTable[tripOrigIB-1][tripStopIB-1] += numPersons;
															if (tripModeKi == u)
																personTripTable[tripStopIB-1][tripDestIB-1] += numPersons;
														}
														else if (v == 5) {
															if (tripModeKi == u)
																personTripTable[tripStopIB-1][tripDestIB-1] += numPersons;
														}
													}
													else {
														if (m == modeAlt && (v==3 || v==5)) {
															if (tourSubmodeIB == u)
																personTripTable[tripOrigIB-1][tripDestIB-1] += numPersons;
														}
													}
												}
												else {
													if (tripStopIB > 0) {
														personTripTable[tripOrigIB-1][tripStopIB-1] += numPersons;
														personTripTable[tripStopIB-1][tripDestIB-1] += numPersons;
														if ( modeAlt == 2 ) {
															vehicleTripTable[tripOrigIB-1][tripStopIB-1] += 1;
															vehicleTripTable[tripStopIB-1][tripDestIB-1] += 1;
														}
													}
													else {
														personTripTable[tripOrigIB-1][tripDestIB-1] += numPersons;
														if ( modeAlt == 2 )
															vehicleTripTable[tripOrigIB-1][tripDestIB-1] += 1;
													}
												}
		
											}
										}
				
									}
								
								}
			
			
				
								// write trips for individual non-mandatory tours
								it = hh[i].getIndivTours();
								if (it != null) {
			
									for (int t=0; t < it.length; t++) {
			
										tod = it[t].getTimeOfDayAlt();
										if (tod < 1)
											continue;
										    
										start = com.pb.morpc.models.TODDataManager.getTodStartHour ( tod );
										end = com.pb.morpc.models.TODDataManager.getTodEndHour ( tod );
										periodOut = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod ( tod );
										periodIn = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod ( tod );

										modeAlt = it[t].getMode();
										tourType = it[t].getTourType();
										tripPark = it[t].getChosenPark();
										tripOrigOB = it[t].getOrigTaz();
										tripDestIB = it[t].getOrigTaz();
										if (tripPark > 0) {
											tripDestOB = tripPark;
											tripOrigIB = tripPark;
										}
										else {
											tripDestOB = it[t].getDestTaz();
											tripOrigIB = it[t].getDestTaz();
										}
										if (tripOrigOB < 1 || tripOrigIB < 1 || tripDestOB < 1 || tripDestIB < 1)
											continue;

										tripStopOB = it[t].getStopLocOB();
										tripStopIB = it[t].getStopLocIB();
										if (tripStopOB < 0 || tripStopIB < 0)
											continue;

										tripModeIk = it[t].getTripIkMode();
										tripModeKj = it[t].getTripKjMode();
										tripModeJk = it[t].getTripJkMode();
										tripModeKi = it[t].getTripKiMode();
										tourSubmodeOB = it[t].getSubmodeOB();
										tourSubmodeIB = it[t].getSubmodeIB();
			
										vocRatioOB = vocRatios[tourType][periodOut];
										vocRatioIB = vocRatios[tourType][periodIn];
		
										
										
										if ( modeAlt == m || (v == 3 && modeAlt == 4) ) {
											if ( periodOut == p ) {
												if (u <= 5) {
													if (tripStopOB > 0) {
														if (v == 3 && modeAlt == 4) {
															if (tripModeKj == u)
																personTripTable[tripStopOB-1][tripDestOB-1]++;
														}
														else if (v == 3) {
															if (tripModeIk == u)
																personTripTable[tripOrigOB-1][tripStopOB-1]++;
															if (tripModeKj == u)
																personTripTable[tripStopOB-1][tripDestOB-1]++;
														}
														else if (v == 4) {
															if (tripModeIk == u)
																personTripTable[tripOrigOB-1][tripStopOB-1]++;
														}
													}
													else {
														if (m == modeAlt && (v == 3 || v == 4)) {
															if (tourSubmodeOB == u)
																personTripTable[tripOrigOB-1][tripDestOB-1]++;
														}
													}
												}
												else {
													if (tripStopOB > 0) {
														personTripTable[tripOrigOB-1][tripStopOB-1]++;
														personTripTable[tripStopOB-1][tripDestOB-1]++;
														if ( modeAlt == 1 ) {
															vehicleTripTable[tripOrigOB-1][tripStopOB-1] += 1;
															vehicleTripTable[tripStopOB-1][tripDestOB-1] += 1;
														}
														else if ( modeAlt == 2 ) {
															vehicleTripTable[tripOrigOB-1][tripStopOB-1] += 1.0/vocRatioOB;
															vehicleTripTable[tripStopOB-1][tripDestOB-1] += 1.0/vocRatioOB;
														}
													}
													else {
														personTripTable[tripOrigOB-1][tripDestOB-1]++;
														if ( modeAlt == 1 )
															vehicleTripTable[tripOrigOB-1][tripDestOB-1] += 1;
														else if ( modeAlt == 2 )
															vehicleTripTable[tripOrigOB-1][tripDestOB-1] += 1.0/vocRatioOB;
													}
												}
											}
											
											if ( periodIn == p ) {
												if (u <= 5) {
													if (tripStopIB > 0) {
														if (v == 3 && modeAlt == 4) {
															if (tripModeJk == u)
																personTripTable[tripOrigIB-1][tripStopIB-1]++;
														}
														else if (v == 3) {
															if (tripModeJk == u)
																personTripTable[tripOrigIB-1][tripStopIB-1]++;
															if (tripModeKi == u)
																personTripTable[tripStopIB-1][tripDestIB-1]++;
														}
														else if (v == 5) {
															if (tripModeKi == u)
																personTripTable[tripStopIB-1][tripDestIB-1]++;
														}
													}
													else {
														if (m == modeAlt && (v==3 || v==5)) {
															if (tourSubmodeIB == u)
																personTripTable[tripOrigIB-1][tripDestIB-1]++;
														}
													}
												}
												else {
													if (tripStopIB > 0) {
														personTripTable[tripOrigIB-1][tripStopIB-1]++;
														personTripTable[tripStopIB-1][tripDestIB-1]++;
														if ( modeAlt == 1 ) {
															vehicleTripTable[tripOrigIB-1][tripStopIB-1] += 1;
															vehicleTripTable[tripStopIB-1][tripDestIB-1] += 1;
														}
														else if ( modeAlt == 2 ) {
															vehicleTripTable[tripOrigIB-1][tripStopIB-1] += 1.0/vocRatioIB;
															vehicleTripTable[tripStopIB-1][tripDestIB-1] += 1.0/vocRatioIB;
														}
													}
													else {
														personTripTable[tripOrigIB-1][tripDestIB-1]++;
														if ( modeAlt == 1 )
															vehicleTripTable[tripOrigIB-1][tripDestIB-1] += 1;
														else if ( modeAlt == 2 )
															vehicleTripTable[tripOrigIB-1][tripDestIB-1] += 1.0/vocRatioIB;
													}
												}
		
											}
										}
				
									}
			
								}
				
			
								
								// write trips for atwork subtours
								it = hh[i].getMandatoryTours();
								if (it != null) {
			
									for (int t=0; t < it.length; t++) {
										
										if (it[t].getTourType() == TourType.WORK) {
			
											st = it[t].getSubTours();
											if (st != null) {
			
												for (int s=0; s < st.length; s++) {
			
													tod = st[s].getTimeOfDayAlt();
													if (tod < 1)
														continue;
										    
													start = com.pb.morpc.models.TODDataManager.getTodStartHour ( tod );
													end = com.pb.morpc.models.TODDataManager.getTodEndHour ( tod );
													periodOut = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod ( tod );
													periodIn = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod ( tod );

													modeAlt = st[s].getMode();
													tourType = TourType.ATWORK;
													tripPark = st[s].getChosenPark();
													tripOrigOB = st[s].getOrigTaz();
													tripDestIB = st[s].getOrigTaz();
													if (tripPark > 0) {
														tripDestOB = tripPark;
														tripOrigIB = tripPark;
													}
													else {
														tripDestOB = st[s].getDestTaz();
														tripOrigIB = st[s].getDestTaz();
													}
													if (tripOrigOB < 1 || tripOrigIB < 1 || tripDestOB < 1 || tripDestIB < 1)
														continue;

													tripStopOB = st[s].getStopLocOB();
													tripStopIB = st[s].getStopLocIB();
													if (tripStopOB < 0 || tripStopIB < 0)
														continue;

													tripModeIk = st[s].getTripIkMode();
													tripModeKj = st[s].getTripKjMode();
													tripModeJk = st[s].getTripJkMode();
													tripModeKi = st[s].getTripKiMode();
													tourSubmodeOB = st[s].getSubmodeOB();
													tourSubmodeIB = st[s].getSubmodeIB();
			
													vocRatioOB = vocRatios[tourType][periodOut];
													vocRatioIB = vocRatios[tourType][periodIn];

													
													
													if ( modeAlt == m || (v == 3 && modeAlt == 4) ) {
														if ( periodOut == p ) {
															if (u <= 5) {
																if (tripStopOB > 0) {
																	if (v == 3 && modeAlt == 4) {
																		if (tripModeKj == u)
																			personTripTable[tripStopOB-1][tripDestOB-1]++;
																	}
																	else if (v == 3) {
																		if (tripModeIk == u)
																			personTripTable[tripOrigOB-1][tripStopOB-1]++;
																		if (tripModeKj == u)
																			personTripTable[tripStopOB-1][tripDestOB-1]++;
																	}
																	else if (v == 4) {
																		if (tripModeIk == u)
																			personTripTable[tripOrigOB-1][tripStopOB-1]++;
																	}
																}
																else {
																	if (m == modeAlt && (v == 3 || v == 4)) {
																		if (tourSubmodeOB == u)
																			personTripTable[tripOrigOB-1][tripDestOB-1]++;
																	}
																}
															}
															else {
																if (tripStopOB > 0) {
																	personTripTable[tripOrigOB-1][tripStopOB-1]++;
																	personTripTable[tripStopOB-1][tripDestOB-1]++;
																	if ( modeAlt == 1 ) {
																		vehicleTripTable[tripOrigOB-1][tripStopOB-1] += 1;
																		vehicleTripTable[tripStopOB-1][tripDestOB-1] += 1;
																	}
																	else if ( modeAlt == 2 ) {
																		vehicleTripTable[tripOrigOB-1][tripStopOB-1] += 1.0/vocRatioOB;
																		vehicleTripTable[tripStopOB-1][tripDestOB-1] += 1.0/vocRatioOB;
																	}
																}
																else {
																	personTripTable[tripOrigOB-1][tripDestOB-1]++;
																	if ( modeAlt == 1 )
																		vehicleTripTable[tripOrigOB-1][tripDestOB-1] += 1;
																	else if ( modeAlt == 2 )
																		vehicleTripTable[tripOrigOB-1][tripDestOB-1] += 1.0/vocRatioOB;
																}
															}
														}
											
														if ( periodIn == p ) {
															if (u <= 5) {
																if (tripStopIB > 0) {
																	if (v == 3 && modeAlt == 4) {
																		if (tripModeJk == u)
																			personTripTable[tripOrigIB-1][tripStopIB-1]++;
																	}
																	else if (v == 3) {
																		if (tripModeJk == u)
																			personTripTable[tripOrigIB-1][tripStopIB-1]++;
																		if (tripModeKi == u)
																			personTripTable[tripStopIB-1][tripDestIB-1]++;
																	}
																	else if (v == 5) {
																		if (tripModeKi == u)
																			personTripTable[tripStopIB-1][tripDestIB-1]++;
																	}
																}
																else {
																	if (m == modeAlt && (v==3 || v==5)) {
																		if (tourSubmodeIB == u)
																			personTripTable[tripOrigIB-1][tripDestIB-1]++;
																	}
																}
															}
															else {
																if (tripStopIB > 0) {
																	personTripTable[tripOrigIB-1][tripStopIB-1]++;
																	personTripTable[tripStopIB-1][tripDestIB-1]++;
																	if ( modeAlt == 1 ) {
																		vehicleTripTable[tripOrigIB-1][tripStopIB-1] += 1;
																		vehicleTripTable[tripStopIB-1][tripDestIB-1] += 1;
																	}
																	else if ( modeAlt == 2 ) {
																		vehicleTripTable[tripOrigIB-1][tripStopIB-1] += 1.0/vocRatioIB;
																		vehicleTripTable[tripStopIB-1][tripDestIB-1] += 1.0/vocRatioIB;
																	}
																}
																else {
																	personTripTable[tripOrigIB-1][tripDestIB-1]++;
																	if ( modeAlt == 1 )
																		vehicleTripTable[tripOrigIB-1][tripDestIB-1] += 1;
																	else if ( modeAlt == 2 )
																		vehicleTripTable[tripOrigIB-1][tripDestIB-1] += 1.0/vocRatioIB;
																}
															}
		
														}
													}
			
												}
			
											}
			
										}
				
									}
			
								}
			
							}
							
						}
			
						writeBinMatrices( w, v, p, u, personTripTable, vehicleTripTable );
			
						// zero out trip table for writing next mode/time period
						for (int i=0; i < numberOfZones; i++) {
							for (int j=0; j < numberOfZones; j++) {
								personTripTable[i][j] = 0;
								vehicleTripTable[i][j] = 0;
							}
						}
	
					}	
				}
			}
		}

		
		logger.info ("finished writing person and vehicle trip matrices.");

	}
	


	private void writeBinMatrices ( int w, int v, int p, int u, float[][] personTable, float[][] vehicleTable ) {

		String binFileName;
		Matrix outputMatrix;
		MatrixWriter binWriter;

		String purposeName[] = { "", "man", "nonman" };        
		String modeName[] = { "", "sov", "hov", "walktran", "drivtran", "trandriv", "nonmotor", "schoolbus" };        
		String submodeName[] = { "", "lbs", "ebs", "brt", "lrt", "crl" };        
		String periodName[] = { "", "am", "pm", "md", "nt" };        
//		String tourTypeName[] = { "", "work", "univ", "school", "escort", "shop", "maint", "discr", "eat" };        


		// get directoy name in which to writer binary files from properties file
		String outputBinaryDirectory = (String)propertyMap.get( "TripsDirectory.binary");


		// write the binary person trip tables
		if (u <= 5)
			binFileName = outputBinaryDirectory + "/" + purposeName[w] + "_" + modeName[v] + "_"  + submodeName[u] + "_" + periodName[p] + ".binary";
		else
			binFileName = outputBinaryDirectory + "/" + purposeName[w] + "_" + modeName[v] + "_" + periodName[p] + ".binary";
		outputMatrix = new Matrix (personTable);
		binWriter = MatrixWriter.createWriter (MatrixType.BINARY, new File( binFileName ) );
		binWriter.writeMatrix("1", outputMatrix);



		logger.info( "matrix total for table: " + binFileName + " = " + outputMatrix.getSum() );				


		// write the binary vehicle trip tables (sov, hov only)
		if (v == 1 || v == 2) {
			binFileName = outputBinaryDirectory + "/" + purposeName[w] + "_" + modeName[v] + "_" + periodName[p] + "_veh.binary";
			outputMatrix = new Matrix (vehicleTable);
			binWriter = MatrixWriter.createWriter (MatrixType.BINARY, new File( binFileName ) );
			binWriter.writeMatrix("1", outputMatrix);

			logger.info( "matrix total for table: " + binFileName + " = " + outputMatrix.getSum() );				
		}


		// write out the am sov matrix rowsums to the logger
//		if ( m==1 && p == 1 && u == 6 )
//			writeTableRowSums (outputMatrix);
	}



	public void writeTableRowSums ( Matrix outputMatrix ) {

		float rowSum = 0.0f;
		
		for (int i=1; i <= outputMatrix.getRowCount(); i++) {
			rowSum = outputMatrix.getRowSum( i );
			logger.info( "rowsum[" + i + "] = " +  rowSum);				
		}
	}



	public void writeDTMOutput ( Household[] hh ) throws IOException {

		String modeName[] = { "", "sov", "hov", "walktran", "drivtran", "nonmotor", "schoolbus" };        
		String tlPurposeName[] = { "", "1 Work-low", "1 Work-med", "1 Work-high", "2 University", "3 School", "4 Escorting", "5 Shopping - ind", "5 Shopping - joint", "6 Maintenance - ind", "6 Maintenance - joint", "7 Discretionary - ind", "7 Discretionary - joint", "8 Eating out - ind", "8 Eating out - joint", "9 At work" };        

		int k = 0;

		int hh_id;
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
		
		int[] tripsByMode = new int[7];
		
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



			ChoiceModelApplication distc =  new ChoiceModelApplication("Model10.controlFile", "Model10.outputFile", propertyMap);
			UtilityExpressionCalculator distUEC = distc.getUEC( 1,  0 );
			int maxPartySize = 0;
			for (int i=0; i < hh.length; i++) {
				if (hh[i].jointTours != null) {
					for (int j=0; j < hh[i].jointTours.length; j++) {
						if (hh[i].jointTours[j].getNumPersons() > maxPartySize)
							maxPartySize = hh[i].jointTours[j].getNumPersons();
					}
				}
			}
	

	
			ArrayList tableHeadings = new ArrayList();
			tableHeadings.add(SyntheticPopulation.HHID_FIELD);
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
			tableHeadings.add("Logsum");

			// define an array for use in writing output file
			float[] tableData = new float[tableHeadings.size()];

	

	
			if (outputFileDTM != null) {

				//Print titles
				outStream.print( (String)tableHeadings.get(0) );
				for (int i = 1; i < tableHeadings.size(); i++) {
					outStream.print(",");
					outStream.print( (String)tableHeadings.get(i) );
				}
				outStream.println();
			
			}
			

			
			int r=0;
			for (int i=0; i < hh.length; i++) {
			    
				hh_id = hh[i].getID();
				hh_taz_id = hh[i].getTazID();
				Person[] persons = hh[i].getPersonArray();
				

				// first put individual mandatory tours in the output table
				it = hh[i].getMandatoryTours();
				if (it != null) {
					for (int t=0; t < it.length; t++) {
				    
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
						tableData[1] = hh_taz_id;
						tableData[2] = it[t].getTourPerson();
						tableData[3] = persons[it[t].getTourPerson()].getPersonType();
						tableData[4] = persons[it[t].getTourPerson()].getPatternType();
						tableData[5] = t+1;
						tableData[6] = 1;
						tableData[7] = it[t].getTourType();
						k = 9 + (2*maxPartySize);
						tableData[k] = it[t].getOrigTaz();
						tableData[k+1] = it[t].getDestTaz();
						tableData[k+2] = it[t].getDestShrtWlk();
						tableData[k+3] = it[t].getTimeOfDayAlt();
						if (it[t].getTimeOfDayAlt() < 1)
							continue;
						tableData[k+4] = com.pb.morpc.models.TODDataManager.getTodStartHour( it[t].getTimeOfDayAlt() );
						tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodEndHour( it[t].getTimeOfDayAlt() );
						tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+10] = it[t].getMode();
						tableData[k+11] = it[t].getSubmodeOB();
						tableData[k+12] = it[t].getSubmodeIB();
						tableData[k+13] = it[t].getStopFreqAlt();
						tableData[k+14] = it[t].getStopLocOB();
						tableData[k+15] = it[t].getStopLocSubzoneOB();
						tableData[k+16] = it[t].getStopLocIB();
						tableData[k+17] = it[t].getStopLocSubzoneIB();
						tableData[k+18] = it[t].getTripIkMode();
						tableData[k+19] = it[t].getTripKjMode();
						tableData[k+20] = it[t].getTripJkMode();
						tableData[k+21] = it[t].getTripKiMode();
						tableData[k+22] = (float)resultsOB[0];
						tableData[k+23] = (float)resultsIB[0];
						tableData[k+24] = (float)resultsOB[1];
						tableData[k+25] = (float)resultsOB[2];
						tableData[k+26] = (float)resultsIB[1];
						tableData[k+27] = (float)resultsIB[2];
						tableData[k+28] = it[t].getChosenPark();
						tableData[k+29] = (float)resultsPark[1];
						tableData[k+30] = (float)resultsPark[2];
						if (it[t].getMode() == 3) {
							if (tableData[k+8] == 1) {
								tableData[k+31] = (float)resultsOB[3];
								tableData[k+33] = (float)resultsOB[4];
								tableData[k+35] = (float)resultsOB[5];
								tableData[k+37] = (float)resultsOB[6];
								tableData[k+39] = (float)resultsOB[7];
							}
							else if (tableData[k+8] == 2) {
								tableData[k+31] = (float)resultsIB[3];
								tableData[k+33] = (float)resultsIB[4];
								tableData[k+35] = (float)resultsIB[5];
								tableData[k+37] = (float)resultsIB[6];
								tableData[k+39] = (float)resultsIB[7];
							}
							else {
								tableData[k+31] = (float)resultsOB[8];
								tableData[k+33] = (float)resultsOB[9];
								tableData[k+35] = (float)resultsOB[10];
								tableData[k+37] = (float)resultsOB[11];
								tableData[k+39] = (float)resultsOB[12];
							}
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsIB[3];
								tableData[k+34] = (float)resultsIB[4];
								tableData[k+36] = (float)resultsIB[5];
								tableData[k+38] = (float)resultsIB[6];
								tableData[k+40] = (float)resultsIB[7];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsOB[3];
								tableData[k+34] = (float)resultsOB[4];
								tableData[k+36] = (float)resultsOB[5];
								tableData[k+38] = (float)resultsOB[6];
								tableData[k+40] = (float)resultsOB[7];
							}
							else {
								tableData[k+32] = (float)resultsIB[8];
								tableData[k+34] = (float)resultsIB[9];
								tableData[k+36] = (float)resultsIB[10];
								tableData[k+38] = (float)resultsIB[11];
								tableData[k+40] = (float)resultsIB[12];
							}
						}
						else if (it[t].getMode() == 4) {
							if (tableData[k+8] == 1) {
								tableData[k+31] = (float)resultsOB[13];
								tableData[k+33] = (float)resultsOB[14];
								tableData[k+35] = (float)resultsOB[15];
								tableData[k+37] = (float)resultsOB[16];
								tableData[k+39] = (float)resultsOB[17];
							}
							else if (tableData[k+8] == 2) {
								tableData[k+31] = (float)resultsIB[13];
								tableData[k+33] = (float)resultsIB[14];
								tableData[k+35] = (float)resultsIB[15];
								tableData[k+37] = (float)resultsIB[16];
								tableData[k+39] = (float)resultsIB[17];
							}
							else {
								tableData[k+31] = (float)resultsOB[18];
								tableData[k+33] = (float)resultsOB[19];
								tableData[k+35] = (float)resultsOB[20];
								tableData[k+37] = (float)resultsOB[21];
								tableData[k+39] = (float)resultsOB[22];
							}
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsIB[13];
								tableData[k+34] = (float)resultsIB[14];
								tableData[k+36] = (float)resultsIB[15];
								tableData[k+38] = (float)resultsIB[16];
								tableData[k+40] = (float)resultsIB[17];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsOB[13];
								tableData[k+34] = (float)resultsOB[14];
								tableData[k+36] = (float)resultsOB[15];
								tableData[k+38] = (float)resultsOB[16];
								tableData[k+40] = (float)resultsOB[17];
							}
							else {
								tableData[k+32] = (float)resultsIB[18];
								tableData[k+34] = (float)resultsIB[19];
								tableData[k+36] = (float)resultsIB[20];
								tableData[k+38] = (float)resultsIB[21];
								tableData[k+40] = (float)resultsIB[22];
							}
						}
						else {
							tableData[k+31] = 0.0f;
							tableData[k+32] = 0.0f;
							tableData[k+33] = 0.0f;
							tableData[k+34] = 0.0f;
							tableData[k+35] = 0.0f;
							tableData[k+36] = 0.0f;
							tableData[k+37] = 0.0f;
							tableData[k+38] = 0.0f;
							tableData[k+39] = 0.0f;
							tableData[k+40] = 0.0f;
						}
						
						tableData[k+41]=(float)it[t].getLogsum();


						if (outputFileDTM != null) {

							outStream.print( tableData[0] );
							for (int c=1; c < tableHeadings.size(); c++) {
								outStream.print(",");
								outStream.print( tableData[c] );
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
					for (int t=0; t < jt.length; t++) {

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
						tableData[1] = hh_taz_id;
						tableData[2] = jt[t].getTourPerson();
						tableData[3] = persons[jt[t].getTourPerson()].getPersonType();
						tableData[4] = persons[jt[t].getTourPerson()].getPatternType();
						tableData[5] = t+1;
						tableData[6] = 2;
						tableData[7] = jt[t].getTourType();
						tableData[8] = jtPersons.length;
						for (int j=0; j < jtPersons.length; j++) {
							tableData[9+(2*j)] = jtPersons[j];
							tableData[9+(2*j)+1] = persons[jtPersons[j]].getPersonType();
						}
						k = 9 + (2*maxPartySize);
						tableData[k] = jt[t].getOrigTaz();
						tableData[k+1] = jt[t].getDestTaz();
						tableData[k+2] = jt[t].getDestShrtWlk();
						tableData[k+3] = jt[t].getTimeOfDayAlt();
						if (jt[t].getTimeOfDayAlt() < 1)
							continue;
						tableData[k+4] = com.pb.morpc.models.TODDataManager.getTodStartHour( jt[t].getTimeOfDayAlt() );
						tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodEndHour( jt[t].getTimeOfDayAlt() );
						tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( jt[t].getTimeOfDayAlt() );
						tableData[k+10] = jt[t].getMode();
						tableData[k+11] = jt[t].getSubmodeOB();
						tableData[k+12] = jt[t].getSubmodeIB();
						tableData[k+13] = jt[t].getStopFreqAlt();
						tableData[k+14] = jt[t].getStopLocOB();
						tableData[k+15] = jt[t].getStopLocSubzoneOB();
						tableData[k+16] = jt[t].getStopLocIB();
						tableData[k+17] = jt[t].getStopLocSubzoneIB();
						tableData[k+18] = jt[t].getTripIkMode();
						tableData[k+19] = jt[t].getTripKjMode();
						tableData[k+20] = jt[t].getTripJkMode();
						tableData[k+21] = jt[t].getTripKiMode();
						tableData[k+22] = (float)resultsOB[0];
						tableData[k+23] = (float)resultsOB[0];
						tableData[k+24] = (float)resultsOB[1];
						tableData[k+25] = (float)resultsOB[2];
						tableData[k+26] = (float)resultsIB[1];
						tableData[k+27] = (float)resultsIB[2];
						tableData[k+28] = jt[t].getChosenPark();
						tableData[k+29] = (float)resultsPark[1];
						tableData[k+30] = (float)resultsPark[2];
						if (jt[t].getMode() == 3) {
							if (tableData[k+8] == 1) {
								tableData[k+31] = (float)resultsOB[3];
								tableData[k+33] = (float)resultsOB[4];
								tableData[k+35] = (float)resultsOB[5];
								tableData[k+37] = (float)resultsOB[6];
								tableData[k+39] = (float)resultsOB[7];
							}
							else if (tableData[k+8] == 2) {
								tableData[k+31] = (float)resultsIB[3];
								tableData[k+33] = (float)resultsIB[4];
								tableData[k+35] = (float)resultsIB[5];
								tableData[k+37] = (float)resultsIB[6];
								tableData[k+39] = (float)resultsIB[7];
							}
							else {
								tableData[k+31] = (float)resultsOB[8];
								tableData[k+33] = (float)resultsOB[9];
								tableData[k+35] = (float)resultsOB[10];
								tableData[k+37] = (float)resultsOB[11];
								tableData[k+39] = (float)resultsOB[12];
							}
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsIB[3];
								tableData[k+34] = (float)resultsIB[4];
								tableData[k+36] = (float)resultsIB[5];
								tableData[k+38] = (float)resultsIB[6];
								tableData[k+40] = (float)resultsIB[7];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsOB[3];
								tableData[k+34] = (float)resultsOB[4];
								tableData[k+36] = (float)resultsOB[5];
								tableData[k+38] = (float)resultsOB[6];
								tableData[k+40] = (float)resultsOB[7];
							}
							else {
								tableData[k+32] = (float)resultsIB[8];
								tableData[k+34] = (float)resultsIB[9];
								tableData[k+36] = (float)resultsIB[10];
								tableData[k+38] = (float)resultsIB[11];
								tableData[k+40] = (float)resultsIB[12];
							}
						}
						else if (jt[t].getMode() == 4) {
							if (tableData[k+8] == 1) {
								tableData[k+31] = (float)resultsOB[13];
								tableData[k+33] = (float)resultsOB[14];
								tableData[k+35] = (float)resultsOB[15];
								tableData[k+37] = (float)resultsOB[16];
								tableData[k+39] = (float)resultsOB[17];
							}
							else if (tableData[k+8] == 2) {
								tableData[k+31] = (float)resultsIB[13];
								tableData[k+33] = (float)resultsIB[14];
								tableData[k+35] = (float)resultsIB[15];
								tableData[k+37] = (float)resultsIB[16];
								tableData[k+39] = (float)resultsIB[17];
							}
							else {
								tableData[k+31] = (float)resultsOB[18];
								tableData[k+33] = (float)resultsOB[19];
								tableData[k+35] = (float)resultsOB[20];
								tableData[k+37] = (float)resultsOB[21];
								tableData[k+39] = (float)resultsOB[22];
							}
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsIB[13];
								tableData[k+34] = (float)resultsIB[14];
								tableData[k+36] = (float)resultsIB[15];
								tableData[k+38] = (float)resultsIB[16];
								tableData[k+40] = (float)resultsIB[17];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsOB[13];
								tableData[k+34] = (float)resultsOB[14];
								tableData[k+36] = (float)resultsOB[15];
								tableData[k+38] = (float)resultsOB[16];
								tableData[k+40] = (float)resultsOB[17];
							}
							else {
								tableData[k+32] = (float)resultsIB[18];
								tableData[k+34] = (float)resultsIB[19];
								tableData[k+36] = (float)resultsIB[20];
								tableData[k+38] = (float)resultsIB[21];
								tableData[k+40] = (float)resultsIB[22];
							}

						}
						else {
							tableData[k+31] = 0.0f;
							tableData[k+32] = 0.0f;
							tableData[k+33] = 0.0f;
							tableData[k+34] = 0.0f;
							tableData[k+35] = 0.0f;
							tableData[k+36] = 0.0f;
							tableData[k+37] = 0.0f;
							tableData[k+38] = 0.0f;
							tableData[k+39] = 0.0f;
							tableData[k+40] = 0.0f;
						}

						tableData[k+41]=(float)jt[t].getLogsum();
						
						if (outputFileDTM != null) {

							outStream.print( tableData[0] );
							for (int c=1; c < tableHeadings.size(); c++) {
								outStream.print(",");
								outStream.print( tableData[c] );
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
					for (int t=0; t < it.length; t++) {

				    
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
						tableData[1] = hh_taz_id;
						tableData[2] = it[t].getTourPerson();
						tableData[3] = persons[it[t].getTourPerson()].getPersonType();
						tableData[4] = persons[it[t].getTourPerson()].getPatternType();
						tableData[5] = t+1;
						tableData[6] = 3;
						tableData[7] = it[t].getTourType();
						k = 9 + (2*maxPartySize);
						tableData[k] = it[t].getOrigTaz();
						tableData[k+1] = it[t].getDestTaz();
						tableData[k+2] = it[t].getDestShrtWlk();
						tableData[k+3] = it[t].getTimeOfDayAlt();
						if (it[t].getTimeOfDayAlt() < 1)
							continue;
						tableData[k+4] = com.pb.morpc.models.TODDataManager.getTodStartHour( it[t].getTimeOfDayAlt() );
						tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodEndHour( it[t].getTimeOfDayAlt() );
						tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( it[t].getTimeOfDayAlt() );
						tableData[k+10] = it[t].getMode();
						tableData[k+11] = it[t].getSubmodeOB();
						tableData[k+12] = it[t].getSubmodeIB();
						tableData[k+13] = it[t].getStopFreqAlt();
						tableData[k+14] = it[t].getStopLocOB();
						tableData[k+15] = it[t].getStopLocSubzoneOB();
						tableData[k+16] = it[t].getStopLocIB();
						tableData[k+17] = it[t].getStopLocSubzoneIB();
						tableData[k+18] = it[t].getTripIkMode();
						tableData[k+19] = it[t].getTripKjMode();
						tableData[k+20] = it[t].getTripJkMode();
						tableData[k+21] = it[t].getTripKiMode();
						tableData[k+22] = (float)resultsOB[0];
						tableData[k+23] = (float)resultsOB[0];
						tableData[k+24] = (float)resultsOB[1];
						tableData[k+25] = (float)resultsOB[2];
						tableData[k+26] = (float)resultsIB[1];
						tableData[k+27] = (float)resultsIB[2];
						tableData[k+28] = it[t].getChosenPark();
						tableData[k+29] = (float)resultsPark[1];
						tableData[k+30] = (float)resultsPark[2];
						if (it[t].getMode() == 3) {
							if (tableData[k+8] == 1) {
								tableData[k+31] = (float)resultsOB[3];
								tableData[k+33] = (float)resultsOB[4];
								tableData[k+35] = (float)resultsOB[5];
								tableData[k+37] = (float)resultsOB[6];
								tableData[k+39] = (float)resultsOB[7];
							}
							else if (tableData[k+8] == 2) {
								tableData[k+31] = (float)resultsIB[3];
								tableData[k+33] = (float)resultsIB[4];
								tableData[k+35] = (float)resultsIB[5];
								tableData[k+37] = (float)resultsIB[6];
								tableData[k+39] = (float)resultsIB[7];
							}
							else {
								tableData[k+31] = (float)resultsOB[8];
								tableData[k+33] = (float)resultsOB[9];
								tableData[k+35] = (float)resultsOB[10];
								tableData[k+37] = (float)resultsOB[11];
								tableData[k+39] = (float)resultsOB[12];
							}
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsIB[3];
								tableData[k+34] = (float)resultsIB[4];
								tableData[k+36] = (float)resultsIB[5];
								tableData[k+38] = (float)resultsIB[6];
								tableData[k+40] = (float)resultsIB[7];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsOB[3];
								tableData[k+34] = (float)resultsOB[4];
								tableData[k+36] = (float)resultsOB[5];
								tableData[k+38] = (float)resultsOB[6];
								tableData[k+40] = (float)resultsOB[7];
							}
							else {
								tableData[k+32] = (float)resultsIB[8];
								tableData[k+34] = (float)resultsIB[9];
								tableData[k+36] = (float)resultsIB[10];
								tableData[k+38] = (float)resultsIB[11];
								tableData[k+40] = (float)resultsIB[12];
							}
						}
						else if (it[t].getMode() == 4) {
							if (tableData[k+8] == 1) {
								tableData[k+31] = (float)resultsOB[13];
								tableData[k+33] = (float)resultsOB[14];
								tableData[k+35] = (float)resultsOB[15];
								tableData[k+37] = (float)resultsOB[16];
								tableData[k+39] = (float)resultsOB[17];
							}
							else if (tableData[k+8] == 2) {
								tableData[k+31] = (float)resultsIB[13];
								tableData[k+33] = (float)resultsIB[14];
								tableData[k+35] = (float)resultsIB[15];
								tableData[k+37] = (float)resultsIB[16];
								tableData[k+39] = (float)resultsIB[17];
							}
							else {
								tableData[k+31] = (float)resultsOB[18];
								tableData[k+33] = (float)resultsOB[19];
								tableData[k+35] = (float)resultsOB[20];
								tableData[k+37] = (float)resultsOB[21];
								tableData[k+39] = (float)resultsOB[22];
							}
							if (tableData[k+9] == 1) {
								tableData[k+32] = (float)resultsIB[13];
								tableData[k+34] = (float)resultsIB[14];
								tableData[k+36] = (float)resultsIB[15];
								tableData[k+38] = (float)resultsIB[16];
								tableData[k+40] = (float)resultsIB[17];
							}
							else if (tableData[k+9] == 2) {
								tableData[k+32] = (float)resultsOB[13];
								tableData[k+34] = (float)resultsOB[14];
								tableData[k+36] = (float)resultsOB[15];
								tableData[k+38] = (float)resultsOB[16];
								tableData[k+40] = (float)resultsOB[17];
							}
							else {
								tableData[k+32] = (float)resultsIB[18];
								tableData[k+34] = (float)resultsIB[19];
								tableData[k+36] = (float)resultsIB[20];
								tableData[k+38] = (float)resultsIB[21];
								tableData[k+40] = (float)resultsIB[22];
							}

						}
						else {
							tableData[k+31] = 0.0f;
							tableData[k+32] = 0.0f;
							tableData[k+33] = 0.0f;
							tableData[k+34] = 0.0f;
							tableData[k+35] = 0.0f;
							tableData[k+36] = 0.0f;
							tableData[k+37] = 0.0f;
							tableData[k+38] = 0.0f;
							tableData[k+39] = 0.0f;
							tableData[k+40] = 0.0f;
						}
						
						tableData[k+41]=(float)it[t].getLogsum();

						if (outputFileDTM != null) {

							outStream.print( tableData[0] );
							for (int c=1; c < tableHeadings.size(); c++) {
								outStream.print(",");
								outStream.print( tableData[c] );
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
					for (int t=0; t < it.length; t++) {
						
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
									tableData[1] = hh_taz_id;
									tableData[2] = st[s].getTourPerson();
									tableData[3] = persons[st[s].getTourPerson()].getPersonType();
									tableData[4] = persons[st[s].getTourPerson()].getPatternType();
									tableData[5] = (t+1)*10 + (s+1);
									tableData[6] = 4;
									tableData[7] = st[s].getSubTourType();

									k = 9 + (2*maxPartySize);
									tableData[k] = st[s].getOrigTaz();
									tableData[k+1] = st[s].getDestTaz();
									tableData[k+2] = st[s].getDestShrtWlk();
									tableData[k+3] = st[s].getTimeOfDayAlt();
									if (st[s].getTimeOfDayAlt() < 1)
										continue;
									tableData[k+4] = com.pb.morpc.models.TODDataManager.getTodStartHour( st[s].getTimeOfDayAlt() );
									tableData[k+5] = com.pb.morpc.models.TODDataManager.getTodEndHour( st[s].getTimeOfDayAlt() );
									tableData[k+6] = com.pb.morpc.models.TODDataManager.getTodStartPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+7] = com.pb.morpc.models.TODDataManager.getTodEndPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+8] = com.pb.morpc.models.TODDataManager.getTodStartSkimPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+9] = com.pb.morpc.models.TODDataManager.getTodEndSkimPeriod( st[s].getTimeOfDayAlt() );
									tableData[k+10] = st[s].getMode();
									tableData[k+11] = st[s].getSubmodeOB();
									tableData[k+12] = st[s].getSubmodeIB();
									tableData[k+13] = st[s].getStopFreqAlt();
									tableData[k+14] = st[s].getStopLocOB();
									tableData[k+15] = st[s].getStopLocSubzoneOB();
									tableData[k+16] = st[s].getStopLocIB();
									tableData[k+17] = st[s].getStopLocSubzoneIB();
									tableData[k+18] = st[s].getTripIkMode();
									tableData[k+19] = st[s].getTripKjMode();
									tableData[k+20] = st[s].getTripJkMode();
									tableData[k+21] = st[s].getTripKiMode();
									tableData[k+22] = (float)resultsOB[0];
									tableData[k+23] = (float)resultsOB[0];
									tableData[k+24] = (float)resultsOB[1];
									tableData[k+25] = (float)resultsOB[2];
									tableData[k+26] = (float)resultsIB[1];
									tableData[k+27] = (float)resultsIB[2];
									tableData[k+28] = st[s].getChosenPark();
									tableData[k+29] = (float)resultsPark[1];
									tableData[k+30] = (float)resultsPark[2];
									if (st[s].getMode() == 3) {
										if (tableData[k+8] == 1) {
											tableData[k+31] = (float)resultsOB[3];
											tableData[k+33] = (float)resultsOB[4];
											tableData[k+35] = (float)resultsOB[5];
											tableData[k+37] = (float)resultsOB[6];
											tableData[k+39] = (float)resultsOB[7];
										}
										else if (tableData[k+8] == 2) {
											tableData[k+31] = (float)resultsIB[3];
											tableData[k+33] = (float)resultsIB[4];
											tableData[k+35] = (float)resultsIB[5];
											tableData[k+37] = (float)resultsIB[6];
											tableData[k+39] = (float)resultsIB[7];
										}
										else {
											tableData[k+31] = (float)resultsOB[8];
											tableData[k+33] = (float)resultsOB[9];
											tableData[k+35] = (float)resultsOB[10];
											tableData[k+37] = (float)resultsOB[11];
											tableData[k+39] = (float)resultsOB[12];
										}
										if (tableData[k+9] == 1) {
											tableData[k+32] = (float)resultsIB[3];
											tableData[k+34] = (float)resultsIB[4];
											tableData[k+36] = (float)resultsIB[5];
											tableData[k+38] = (float)resultsIB[6];
											tableData[k+40] = (float)resultsIB[7];
										}
										else if (tableData[k+9] == 2) {
											tableData[k+32] = (float)resultsOB[3];
											tableData[k+34] = (float)resultsOB[4];
											tableData[k+36] = (float)resultsOB[5];
											tableData[k+38] = (float)resultsOB[6];
											tableData[k+40] = (float)resultsOB[7];
										}
										else {
											tableData[k+32] = (float)resultsIB[8];
											tableData[k+34] = (float)resultsIB[9];
											tableData[k+36] = (float)resultsIB[10];
											tableData[k+38] = (float)resultsIB[11];
											tableData[k+40] = (float)resultsIB[12];
										}
									}
									else if (st[s].getMode() == 4) {
										if (tableData[k+8] == 1) {
											tableData[k+31] = (float)resultsOB[13];
											tableData[k+33] = (float)resultsOB[14];
											tableData[k+35] = (float)resultsOB[15];
											tableData[k+37] = (float)resultsOB[16];
											tableData[k+39] = (float)resultsOB[17];
										}
										else if (tableData[k+8] == 2) {
											tableData[k+31] = (float)resultsIB[13];
											tableData[k+33] = (float)resultsIB[14];
											tableData[k+35] = (float)resultsIB[15];
											tableData[k+37] = (float)resultsIB[16];
											tableData[k+39] = (float)resultsIB[17];
										}
										else {
											tableData[k+31] = (float)resultsOB[18];
											tableData[k+33] = (float)resultsOB[19];
											tableData[k+35] = (float)resultsOB[20];
											tableData[k+37] = (float)resultsOB[21];
											tableData[k+39] = (float)resultsOB[22];
										}
										if (tableData[k+9] == 1) {
											tableData[k+32] = (float)resultsIB[13];
											tableData[k+34] = (float)resultsIB[14];
											tableData[k+36] = (float)resultsIB[15];
											tableData[k+38] = (float)resultsIB[16];
											tableData[k+40] = (float)resultsIB[17];
										}
										else if (tableData[k+9] == 2) {
											tableData[k+32] = (float)resultsOB[13];
											tableData[k+34] = (float)resultsOB[14];
											tableData[k+36] = (float)resultsOB[15];
											tableData[k+38] = (float)resultsOB[16];
											tableData[k+40] = (float)resultsOB[17];
										}
										else {
											tableData[k+32] = (float)resultsIB[18];
											tableData[k+34] = (float)resultsIB[19];
											tableData[k+36] = (float)resultsIB[20];
											tableData[k+38] = (float)resultsIB[21];
											tableData[k+40] = (float)resultsIB[22];
										}

									}
									else {
										tableData[k+31] = 0.0f;
										tableData[k+32] = 0.0f;
										tableData[k+33] = 0.0f;
										tableData[k+34] = 0.0f;
										tableData[k+35] = 0.0f;
										tableData[k+36] = 0.0f;
										tableData[k+37] = 0.0f;
										tableData[k+38] = 0.0f;
										tableData[k+39] = 0.0f;
										tableData[k+40] = 0.0f;
									}
									
									tableData[k+41]=(float)st[s].getLogsum();


									if (outputFileDTM != null) {

										outStream.print( tableData[0] );
										for (int c=1; c < tableHeadings.size(); c++) {
											outStream.print(",");
											outStream.print( tableData[c] );
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
		catch (IOException e) {
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
