package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */

import com.pb.common.model.ModelException;
import com.pb.common.util.SeededRandom;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.*;

import java.util.HashMap;
import java.util.Arrays;


public class DTMHousehold extends DTMModelBase implements java.io.Serializable {


	int count = 1;

	long dcLogsumTime = 0;
	long tcLogsumTime = 0;
	long soaTime = 0;
	long dcTime = 0;
	long tcTime = 0;
	long mcTime = 0;
	
	int shadowPricingIteration = 0;
	
	
	

	// this constructor used in non-distributed application
	public DTMHousehold ( HashMap propertyMap, short tourTypeCategory, short[] tourTypes, ZonalDataManager zdm ) {

		super ( propertyMap, tourTypeCategory, tourTypes, zdm );

		this.count = 1;
	}
    


	// this constructor used in distributed application
	public DTMHousehold ( int processorId, HashMap propertyMap, short tourTypeCategory, short[] tourTypes, ZonalDataManager zdm ) {

		super ( processorId, propertyMap, tourTypeCategory, tourTypes, zdm );

		this.count = 1;
	}
    



	// clear the HashMaps of Sample of Alternatives probabilities for each iteration of mandatory DC models.
    public void clearProbabilitiesMaps ( short[] types ) {

        for (int m=0; m < types.length; m++) {
            for (int i=0; i < soa[m].length; i++) {
                soa[m][i].clearProbabilitiesMap();
            }
        }
            
	}
		    
		    

	public void mandatoryTourDc ( Household hh ) {

		int soaIndex = 0;
		long markTime=0;
		long startTime = System.currentTimeMillis();

		
		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ( "Destination Choice for Mandatory Tours, shadow price loop " + shadowPricingIteration );
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		

		

		// get the array of mandatory tours for this household.	
		if ( hh.getMandatoryTours() == null )
			return;
			

		// get person array for this household.
		Person[] persons = hh.getPersonArray();

		int income    = hh.getHHIncome();
		hh_taz_id = hh.getTazID();

		hh.setOrigTaz (hh_taz_id);
		hh.setTourCategory( TourType.MANDATORY_CATEGORY );

		
		// loop over all puposes for the mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.mandatoryTours.length; t++) {

				int tourTypeIndex = m;
	
				int tourType = hh.mandatoryTours[t].getTourType();
			
				// set the array of sample of alternatives objects index
				if (tourTypes[m] == TourType.WORK)
					soaIndex = income - 1;
				else
					soaIndex = 0;



				person = hh.mandatoryTours[t].getTourPerson();


				
				if ( tourType != tourTypes[m] ) {

					// if we're processing work, and the tour is school, and the patterntype is school_work,
					// process the tour as a school tour, even though the tourType is work.
					if ( tourTypes[m] == TourType.WORK && tourType == TourType.SCHOOL
						&& persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
							tourTypeIndex = 2;
							soaIndex = 0;
					}
					// if we're processing work, and the tour is university, and the patterntype is univ_work,
					// process the tour as a university tour, even though the tourType is work.
					else if ( tourTypes[m] == TourType.WORK && tourType == TourType.UNIVERSITY
						&& persons[person].getPatternType() == PatternType.UNIV_WORK ) {
							tourTypeIndex = 1;
							soaIndex = 0;
					}
					// if we're processing work, and the tour is univ, and the patterntype is univ_work,
					// don't do anything, just keep processing the univ tour.
					else {
						continue; // otherwise, it's not the right tour type, so go to the next tour.
					}
						
				}
				else {
					
					// if we're processing school, and the tourType is school, and the patterntype is school_work,
					// we've already processed the school tour, so skip to the next tour.
					if ( tourTypes[m] == TourType.SCHOOL && tourType == TourType.SCHOOL
						&& persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
							continue;
					}
					// if we're processing univ, and the tourType is univ, and the patterntype is univ_work,
					// we've already processed the univ tour, so skip to the next tour.
					else if ( tourTypes[m] == TourType.UNIVERSITY && tourType == TourType.UNIVERSITY
						&& persons[person].getPatternType() == PatternType.UNIV_WORK ) {
							continue;
					}

				}
				

				hh.mandatoryTours[t].setOrigTaz (hh_taz_id);
				hh.setPersonID ( person );
				hh.setTourID ( t );


				// get the destination choice sample of alternatives
				markTime = System.currentTimeMillis();

				Arrays.fill ( dcAvailability, false );

				// determine the set of alternatives from which the sample of alternatives will be drawn
				if (preSampleSize > 0) {

					// select the first preSampleSize alternatives to be available for being in the sample of alternatives
					k = 0;
					preSampleCount = 0;
					while ( preSampleCount < preSampleSize ) {
				
						// set destination choice alternative availability to true if size > 0 for the segment.
						preSampleAlt = (int) (dcAvailability.length * SeededRandom.getRandom());
						float size = zdm.getTotSize (tourTypes[tourTypeIndex], preSampleAlt );
						if ( size > 0.0 ) {
							dcAvailability[preSampleAlt] = true;
							preSampleCount++;
						}
					
						k++;
						if (k >= dcAvailability.length) {
							logger.severe( dcAvailability.length + " mandatory dc alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
							System.exit(-1);
						}

					}
					
				}
				else {

					for (k=0; k < dcAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						float size = zdm.getTotSize (tourTypes[tourTypeIndex], k );
						if ( size > 0.0 ) {
							dcAvailability[k] = true;
						}
					}

				}
				
				soa[tourTypeIndex][soaIndex].applySampleOfAlternativesChoiceModel ( hh, dcAvailability );
				int[] sample = soa[tourTypeIndex][soaIndex].getSampleOfAlternatives();
				float[] corrections = soa[tourTypeIndex][soaIndex].getSampleCorrectionFactors();

				Arrays.fill(dcSample, 0);
				Arrays.fill(dcAvailability, false);
				Arrays.fill(dcCorrections[processorId], 0.0f);
				for (int i=1; i < sample.length; i++) {
					dcSample[sample[i]] = 1;
					dcAvailability[sample[i]] = true;
					dcCorrections[processorId][sample[i]] = corrections[i];
				}
				soaTime += (System.currentTimeMillis()-markTime);




				// calculate mode choice logsums for dc alternatives in the sample
				markTime = System.currentTimeMillis();
				for (int i=1; i < sample.length; i++) {

					int d = (int)((sample[i]-1)/WALK_SEGMENTS) + 1;
					int s = sample[i] - (d-1)*WALK_SEGMENTS - 1;
					hh.setChosenDest( d );
					hh.setChosenWalkSegment( s );
							
					// calculate mode choice logsum based on appropriate od skims for each mandatory purpose
					if (TourType.MANDATORY_TYPES[tourTypeIndex] == TourType.WORK) {
						hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmPm" );
						zdm.setLogsumDcAMPM ( processorId, sample[i], getMcLogsums(hh, tourTypeIndex) );
					}
					else if (TourType.MANDATORY_TYPES[tourTypeIndex] == TourType.UNIVERSITY) {
						hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmMd" );
						zdm.setLogsumDcAMMD ( processorId, sample[i], getMcLogsums(hh, tourTypeIndex) );
					}
					else if (TourType.MANDATORY_TYPES[tourTypeIndex] == TourType.SCHOOL) {
						hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmMd" );
						zdm.setLogsumDcAMMD ( processorId, sample[i], getMcLogsums(hh, tourTypeIndex) );
						hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "MdMd" );
						zdm.setLogsumDcMDMD ( processorId, sample[i], getMcLogsums(hh, tourTypeIndex) );
					}

				}
				dcLogsumTime += (System.currentTimeMillis()-markTime);



				// compute destination choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				dc[tourTypeIndex].updateLogitModel ( hh, dcAvailability, dcSample );
				int chosen = dc[tourTypeIndex].getChoiceResult();
				int chosenDestAlt = (int)((chosen-1)/WALK_SEGMENTS) + 1;
				int chosenShrtWlk = chosen - (chosenDestAlt-1)*WALK_SEGMENTS - 1;
				dcTime += (System.currentTimeMillis() - markTime);


				// set the chosen value in Household and Tour objects
				hh.setChosenDest ( chosenDestAlt );
				hh.setChosenWalkSegment( chosenShrtWlk );
				hh.mandatoryTours[t].setDestTaz (chosenDestAlt);
				hh.mandatoryTours[t].setDestShrtWlk (chosenShrtWlk);


				// check to make sure that the DC subzone selected has subzone proportion > 0.0.
				if ( zdm.getWalkPct ( chosenShrtWlk, chosenDestAlt ) == 0.0f ) {
					logger.severe( TourType.TYPE_CATEGORY_LABELS[tourTypeCategory] + " " + TourType.TYPE_LABELS[tourTypeCategory][tourTypeIndex] + " tour " + t + " for person " + person + " in household " + hh_id);
					logger.severe( "selected DC alternative " + chosen + " which translates to Dest TAZ " + chosenDestAlt + " and subzone " + chosenShrtWlk);
					logger.severe( "however, the selected subzone has proportion " + (chosenShrtWlk == 1 ? "short walk" : "long walk") + " equal to 0.0.");
					System.exit(1);
				}
				
			}
			
		}
		
	}

				
				
				
	public void mandatoryTourTc ( Household hh ) {

		int soaIndex = 0;
		long markTime=0;

		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Time-of-day Choice for Mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		


		long startTime = System.currentTimeMillis();

		// get the array of mandatory tours for this household.	
		if ( hh.getMandatoryTours() == null )
			return;


		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_taz_id = hh.getTazID();
		int income    = hh.getHHIncome();

		hh.setOrigTaz ( hh_taz_id );
		hh.setTourCategory( TourType.MANDATORY_CATEGORY );

		
		// loop over all puposes for the mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.mandatoryTours.length; t++) {

				int tourTypeIndex = m;

				int tourType = hh.mandatoryTours[t].getTourType();

				// set the array of sample of alternatives objects index
				if (tourTypes[m] == TourType.WORK)
					soaIndex = income - 1;
				else
					soaIndex = 0;


				
				person = hh.mandatoryTours[t].getTourPerson();


	
				if ( tourType != tourTypes[m] ) {

					// if we're processing work, and the tour is school, and the patterntype is school_work,
					// process the tour as a school tour, even though the tourType is work.
					if ( tourTypes[m] == TourType.WORK && tourType == TourType.SCHOOL
						&& persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
							tourTypeIndex = 2;
							soaIndex = 0;
					}
					// if we're processing work, and the tour is university, and the patterntype is univ_work,
					// process the tour as a university tour, even though the tourType is work.
					else if ( tourTypes[m] == TourType.WORK && tourType == TourType.UNIVERSITY
						&& persons[person].getPatternType() == PatternType.UNIV_WORK ) {
							tourTypeIndex = 1;
							soaIndex = 0;
					}
					// if we're processing work, and the tour is univ, and the patterntype is univ_work,
					// don't do anything, just keep processing the univ tour.
					else {
						continue; // otherwise, it's not the right tour type, so go to the next tour.
					}
			
				}
				else {
		
					// if we're processing school, and the tourType is school, and the patterntype is school_work,
					// we've already processed the school tour, so skip to the next tour.
					if ( tourTypes[m] == TourType.SCHOOL && tourType == TourType.SCHOOL
						&& persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
							continue;
					}
					// if we're processing univ, and the tourType is univ, and the patterntype is univ_work,
					// we've already processed the univ tour, so skip to the next tour.
					else if ( tourTypes[m] == TourType.UNIVERSITY && tourType == TourType.UNIVERSITY
						&& persons[person].getPatternType() == PatternType.UNIV_WORK ) {
							continue;
					}

				}
	

				hh.mandatoryTours[t].setOrigTaz ( hh_taz_id );

				hh.setPersonID ( person );
				hh.setTourID ( t );
				hh.setChosenDest( hh.mandatoryTours[t].getDestTaz() );

				
				

				// update the time of day choice availabilty based on available time windows
				// tcSample and tcAvailability are 1 based
				Arrays.fill(tcSample, 1);
				Arrays.fill(tcAvailability, true);

				setTcAvailability (persons[person], tcAvailability, tcSample);


				// count the number of tours in which no time-of-day alternative was available
				int noTOD = 0;
				for (int p=1; p <= tcUEC[tourTypeIndex].getNumberOfAlternatives(); p++) {
					if (tcAvailability[p]) {
						noTOD++;
						break;
					}
				}
				if (noTOD == 0) {
					noTODAvailableIndiv[tourTypeIndex]++;
					tcAvailability[1] = true;
					tcSample[1] = 1;
					tcAvailability[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = true;
					tcSample[tcUEC[tourTypeIndex].getNumberOfAlternatives()] = 1;
				}


				// calculate the mode choice logsums for TOD choice based on chosen dest and default time periods
				markTime = System.currentTimeMillis();
				if (TourType.MANDATORY_TYPES[tourTypeIndex] != TourType.SCHOOL) {
					
					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaEa" );
					tcLogsumEaEa = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaAm" );
					tcLogsumEaAm = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaMd" );
					tcLogsumEaMd = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaPm" );
					tcLogsumEaPm = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "EaNt" );
					tcLogsumEaNt = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmAm" );
					tcLogsumAmAm = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmMd" );
					tcLogsumAmMd = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmPm" );
					tcLogsumAmPm = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "AmNt" );
					tcLogsumAmNt = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "MdMd" );
					tcLogsumMdMd = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "MdPm" );
					tcLogsumMdPm = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "MdNt" );
					tcLogsumMdNt = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "PmPm" );
					tcLogsumPmPm = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "PmNt" );
					tcLogsumPmNt = getMcLogsums ( hh, tourTypeIndex );

					hh.setTODDefaults ( TourType.MANDATORY_CATEGORY, "NtNt" );
					tcLogsumNtNt = getMcLogsums ( hh, tourTypeIndex );

				}


				// assign mode choice logsums to time-of-day choice alternatives, given correspondiong time periods
				if (TourType.MANDATORY_TYPES[tourTypeIndex] != TourType.SCHOOL) {
					com.pb.morpc.models.TODDataManager.logsumTcEAEA[processorId][1]   = tcLogsumEaEa;
					com.pb.morpc.models.TODDataManager.logsumTcEAEA[processorId][2]   = tcLogsumEaEa;
					com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][3]   = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][4]   = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][5]   = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][6]   = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][7]   = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][8]   = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][9]   = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][10]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][11]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][12]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][13]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][14]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][15]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][16]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][17]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][18]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][19]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcEAEA[processorId][20]  = tcLogsumEaEa;
					com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][21]  = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][22]  = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][23]  = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][24]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][25]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][26]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][27]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][28]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][29]  = tcLogsumEaMd;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][30]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][31]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][32]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][33]  = tcLogsumEaPm;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][34]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][35]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][36]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][37]  = tcLogsumEaNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][38]  = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][39]  = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][40]  = tcLogsumEaAm;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][41]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][42]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][43]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][44]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][45]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][46]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][47]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][48]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][49]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][50]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][51]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][52]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][53]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][54]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][55]  = tcLogsumAmAm;
					com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][56]  = tcLogsumAmAm;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][57]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][58]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][59]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][60]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][61]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][62]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][63]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][64]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][65]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][66]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][67]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][68]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][69]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][70]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][71]  = tcLogsumAmAm;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][72]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][73]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][74]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][75]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][76]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][77]  = tcLogsumAmMd;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][78]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][79]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][80]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][81]  = tcLogsumAmPm;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][82]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][83]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][84]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][85]  = tcLogsumAmNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][86]  = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][87]  = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][88]  = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][89]  = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][90]  = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][91]  = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][92]  = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][93]  = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][94]  = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][95]  = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][96]  = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][97]  = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][98]  = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][99]  = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][100] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][101] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][102] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][103] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][104] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][105] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][106] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][107] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][108] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][109] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][110] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][111] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][112] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][113] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][114] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][115] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][116] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][117] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][118] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][119] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][120] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][121] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][122] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][123] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][124] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][125] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][126] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][127] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][128] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][129] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][130] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][131] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][132] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][133] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][134] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][135] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][136] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][137] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][138] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][139] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][140] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][141] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][142] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][143] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][144] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][145] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][146] = tcLogsumMdMd;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][147] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][148] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][149] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][150] = tcLogsumMdPm;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][151] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][152] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][153] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][154] = tcLogsumMdNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][155] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][156] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][157] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][158] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][159] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][160] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][161] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][162] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][163] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][164] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][165] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][166] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][167] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][168] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][169] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][170] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][171] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][172] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][173] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][174] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][175] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][176] = tcLogsumPmPm;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][177] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][178] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][179] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][180] = tcLogsumPmNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][181] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][182] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][183] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][184] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][185] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][186] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][187] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][188] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][189] = tcLogsumNtNt;
					com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][190] = tcLogsumNtNt;
				}
				tcLogsumTime += (System.currentTimeMillis()-markTime);


				// compute time-of-day choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				tc[tourTypeIndex].updateLogitModel ( hh, tcAvailability, tcSample );
				
				int chosenTODAlt;
				try {
				    chosenTODAlt = tc[tourTypeIndex].getChoiceResult();
				}
				catch (ModelException e) {
					chosenTODAlt = SeededRandom.getRandom() < 0.5 ? 1 : 190;
				}
				tcTime += (System.currentTimeMillis()-markTime);

				
				
				
				// set the hour as unavailable for all hours between start and end for this person
				start = com.pb.morpc.models.TODDataManager.getTodStartHour ( chosenTODAlt );
				end = com.pb.morpc.models.TODDataManager.getTodEndHour ( chosenTODAlt );
				for (int j=start; j <= end; j++) {
					hh.persons[person].setHourUnavailable(j);
					persons[person].setHourUnavailable(j);
				}

				// set chosen in alternative in tour objects
				hh.mandatoryTours[t].setTimeOfDayAlt (chosenTODAlt);
				
				
			}
			
		}

	}
	



	public void mandatoryTourMc ( Household hh ) {

		int soaIndex = 0;
		long markTime=0;

		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Mode Choice for Mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		


		long startTime = System.currentTimeMillis();

		// get the array of mandatory tours for this household.	
		if ( hh.getMandatoryTours() == null )
			return;


		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_taz_id = hh.getTazID();
		int income    = hh.getHHIncome();

		hh.setOrigTaz ( hh_taz_id );
		hh.setTourCategory( TourType.MANDATORY_CATEGORY );
		

		// loop over all puposes for the mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.mandatoryTours.length; t++) {

				int tourTypeIndex = m;

				int tourType = hh.mandatoryTours[t].getTourType();

				// set the array of sample of alternatives objects index
				if (tourTypes[m] == TourType.WORK)
					soaIndex = income - 1;
				else
					soaIndex = 0;


				
				person = hh.mandatoryTours[t].getTourPerson();


	
				if ( tourType != tourTypes[m] ) {

					// if we're processing work, and the tour is school, and the patterntype is school_work,
					// process the tour as a school tour, even though the tourType is work.
					if ( tourTypes[m] == TourType.WORK && tourType == TourType.SCHOOL
						&& persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
							tourTypeIndex = 2;
							soaIndex = 0;
					}
					// if we're processing work, and the tour is university, and the patterntype is univ_work,
					// process the tour as a university tour, even though the tourType is work.
					else if ( tourTypes[m] == TourType.WORK && tourType == TourType.UNIVERSITY
						&& persons[person].getPatternType() == PatternType.UNIV_WORK ) {
							tourTypeIndex = 1;
							soaIndex = 0;
					}
					// if we're processing work, and the tour is univ, and the patterntype is univ_work,
					// don't do anything, just keep processing the univ tour.
					else {
						continue; // otherwise, it's not the right tour type, so go to the next tour.
					}
			
				}
				else {
		
					// if we're processing school, and the tourType is school, and the patterntype is school_work,
					// we've already processed the school tour, so skip to the next tour.
					if ( tourTypes[m] == TourType.SCHOOL && tourType == TourType.SCHOOL
						&& persons[person].getPatternType() == PatternType.SCHOOL_WORK ) {
							continue;
					}
					// if we're processing univ, and the tourType is univ, and the patterntype is univ_work,
					// we've already processed the univ tour, so skip to the next tour.
					else if ( tourTypes[m] == TourType.UNIVERSITY && tourType == TourType.UNIVERSITY
						&& persons[person].getPatternType() == PatternType.UNIV_WORK ) {
							continue;
					}

				}
	

				hh.mandatoryTours[t].setOrigTaz ( hh_taz_id );

				hh.setPersonID ( person );
				hh.setTourID ( t );
				hh.setChosenDest( hh.mandatoryTours[t].getDestTaz() );
				hh.setChosenTodAlt( hh.mandatoryTours[t].getTimeOfDayAlt() );

				
				// compute mode choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				
				Arrays.fill(mcSample, 1);
				Arrays.fill (mcAvailability, true);
				hh.setOrigTaz( hh.mandatoryTours[t].getOrigTaz() );
				hh.setChosenDest( hh.mandatoryTours[t].getDestTaz() );
				setMcODUtility ( hh, tourTypeIndex );
				// set transit modes to unavailable if a no walk access subzone was selected in DC.
				int chosenShrtWlk = hh.mandatoryTours[t].getDestShrtWlk();
				if ( chosenShrtWlk == 0 ) {
					mcSample[3] = 0;
					mcAvailability[3] = false;
					mcSample[4] = 0;
					mcAvailability[4] = false;
				}
				mc[tourTypeIndex].updateLogitModel ( hh, mcAvailability, mcSample );
				int chosenModeAlt = mc[tourTypeIndex].getChoiceResult();
				mcTime += (System.currentTimeMillis()-markTime);

				
				// set chosen in alternative in tour objects
				hh.mandatoryTours[t].setMode (chosenModeAlt);



				index.setOriginZone( hh.mandatoryTours[t].getOrigTaz() );
				index.setDestZone( hh.mandatoryTours[t].getDestTaz() );
				int chosenParkAlt=0;
				// determine parking location if chosenDestAlt is in the CBD and chosenModeAlt is sov or hov.
				if ( hh.getCbdDest() && chosenModeAlt < 3 ) {
					
					if ( hh.getFreeParking() == 1 ) {
						pc[0].updateLogitModel ( hh ,pcAvailability, pcSample );
						chosenParkAlt = pc[0].getChoiceResult();
					}
					else {
						pc[1].updateLogitModel ( hh, pcAvailability, pcSample );
						chosenParkAlt = pc[1].getChoiceResult();
					}

					hh.mandatoryTours[t].setChosenPark (chosenParkAlt);

				}
				else {
					
					hh.mandatoryTours[t].setChosenPark (0);
					
				}




				// set submode for transit modes
				index.setOriginZone( hh.mandatoryTours[t].getOrigTaz() );
				index.setDestZone( hh.mandatoryTours[t].getDestTaz() );

				if ( chosenModeAlt == 3 || chosenModeAlt == 4 ) {

					// set outbound submodes
					if (chosenModeAlt == 3) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// wt, am
							submodeUtility = smcUEC[0].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// wt, pm
							submodeUtility = smcUEC[2].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[1].solve( index, hh, smcSample ); // wt, op

					}
					else if (chosenModeAlt == 4) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// dt, am
							submodeUtility = smcUEC[3].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// dt, pm
							submodeUtility = smcUEC[5].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[4].solve( index, hh, smcSample );	// dt, op
				
					}
					
					
					
					if (submodeUtility[0] >= 10000)
						hh.mandatoryTours[t].setSubmodeOB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.mandatoryTours[t].setSubmodeOB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.mandatoryTours[t].setSubmodeOB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.mandatoryTours[t].setSubmodeOB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.mandatoryTours[t].setSubmodeOB ( SubmodeType.LBS );
					else
						hh.mandatoryTours[t].setSubmodeOB ( 6 );


					
					// set inbound submodes
					if (chosenModeAlt == 3) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// wt, am
							submodeUtility = smcUEC[6].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// wt, pm
							submodeUtility = smcUEC[8].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[7].solve( index, hh, smcSample ); // wt, op
				
					}
					else if (chosenModeAlt == 4) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// dt, am
							submodeUtility = smcUEC[9].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// dt, pm
							submodeUtility = smcUEC[11].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[10].solve( index, hh, smcSample );	// dt, op
				
					}


					
					if (submodeUtility[0] >= 10000)
						hh.mandatoryTours[t].setSubmodeIB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.mandatoryTours[t].setSubmodeIB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.mandatoryTours[t].setSubmodeIB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.mandatoryTours[t].setSubmodeIB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.mandatoryTours[t].setSubmodeIB ( SubmodeType.LBS );
					else
						hh.mandatoryTours[t].setSubmodeIB ( 6 );

				}
				else {

					hh.mandatoryTours[t].setSubmodeOB ( 6 );
					hh.mandatoryTours[t].setSubmodeIB ( 6 );

				}

				
				if ( (hh.mandatoryTours[t].getMode() == 3 || hh.mandatoryTours[t].getMode() == 4) && (hh.mandatoryTours[t].getSubmodeOB() == 6 || hh.mandatoryTours[t].getSubmodeIB() == 6) ) {
					logger.warning ( "invalid submode for mandatory tour=" + t + " for hhid=" + hh.getID() + ", tour mode=" + hh.mandatoryTours[t].getMode() + ", ob submode=" + hh.mandatoryTours[t].getSubmodeOB() + " and ib submode=" + hh.mandatoryTours[t].getSubmodeIB() + "." );
				}
				
				
			}
			
		}

	}
	


	
	public void jointTourDc ( Household hh ) {

		int[] jtPersons;
		
		long markTime=0;
		long startTime = System.currentTimeMillis();

		if (useMessageWindow) mw.setMessage1 ("Destination Choice for Joint Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		// get the array of mandatory tours for this household.	
		if (hh.getJointTours() == null)
			return;
			

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setOrigTaz (hh_taz_id);
		hh.setTourCategory( TourType.JOINT_CATEGORY );


		// loop over all puposes for the mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.jointTours.length; t++) {

				if ( hh.jointTours[t].getTourType() != tourTypes[m] )
					continue;

				hh.jointTours[t].setOrigTaz (hh_taz_id);
				hh.setTourID ( t );


				jtPersons = hh.jointTours[t].getJointTourPersons();
				
				if (jtPersons.length < 2) {
					logger.severe( "fewer than 2 persons participating in joint tour for household " + hh_id + ", joint tour number " + t);
					System.exit(1);
				}

				
				// get the destination choice sample of alternatives
				markTime = System.currentTimeMillis();

				Arrays.fill ( dcAvailability, false );

				// determine the set of alternatives from which the sample of alternatives will be drawn
				if (preSampleSize > 0) {

					// select the first preSampleSize alternatives to be available for being in the sample of alternatives
					k = 0;
					preSampleCount = 0;
					while ( preSampleCount < preSampleSize ) {
				
						// set destination choice alternative availability to true if size > 0 for the segment.
						preSampleAlt = (int) (dcAvailability.length * SeededRandom.getRandom());
						float size = zdm.getTotSize (tourTypes[m], preSampleAlt );
						if ( size > 0.0 ) {
							dcAvailability[preSampleAlt] = true;
							preSampleCount++;
						}
					
						k++;
						if (k >= dcAvailability.length) {
							logger.severe( dcAvailability.length + " mandatory dc alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
							System.exit(-1);
						}

					}
					
				}
				else {

					for (k=0; k < dcAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						float size = zdm.getTotSize (tourTypes[m], k );
						if ( size > 0.0 ) {
							dcAvailability[k] = true;
						}
					}

				}
				
				soa[m][0].applySampleOfAlternativesChoiceModel ( hh, dcAvailability );
				int[] sample = soa[m][0].getSampleOfAlternatives();
				float[] corrections = soa[m][0].getSampleCorrectionFactors();

				Arrays.fill(dcSample, 0);
				Arrays.fill(dcAvailability, false);
				Arrays.fill(dcCorrections[processorId], 0.0f);
				for (int i=1; i < sample.length; i++) {
					dcSample[sample[i]] = 1;
					dcAvailability[sample[i]] = true;
					dcCorrections[processorId][sample[i]] = corrections[i];
				}
				soaTime += (System.currentTimeMillis()-markTime);




				k = 1;
				markTime = System.currentTimeMillis();
				for (int i=1; i < sample.length; i++) {

					int d = (int)((sample[i]-1)/WALK_SEGMENTS) + 1;
					int s = sample[i] - (d-1)*WALK_SEGMENTS - 1;
					hh.setChosenDest( d );
					hh.setChosenWalkSegment( s );
							
					// calculate mode choice logsum based on appropriate od skims
					hh.setTODDefaults ( TourType.JOINT_CATEGORY, "MdMd" );
					zdm.setLogsumDcMDMD ( processorId, sample[i], getMcLogsums(hh, m) );
					hh.setTODDefaults ( TourType.JOINT_CATEGORY, "PmNt" );
					zdm.setLogsumDcPMNT ( processorId, sample[i], getMcLogsums(hh, m) );

				}
				dcLogsumTime += (System.currentTimeMillis()-markTime);



				// compute destination choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				dc[m].updateLogitModel ( hh, dcAvailability, dcSample );
				int chosen = dc[m].getChoiceResult();
				int chosenDestAlt = (int)((chosen-1)/WALK_SEGMENTS) + 1;
				int chosenShrtWlk = chosen - (chosenDestAlt-1)*WALK_SEGMENTS - 1;
				dcTime += (System.currentTimeMillis() - markTime);

				// set the chosen value in DMU and tour objects
				hh.jointTours[t].setDestTaz (chosenDestAlt);
				hh.jointTours[t].setDestShrtWlk (chosenShrtWlk);
				hh.jointTours[t].setChosenPark ( chosenDestAlt );


				// check to make sure that the DC subzone selected has subzone proportion > 0.0.
				if ( zdm.getWalkPct ( chosenShrtWlk, chosenDestAlt ) == 0.0f ) {
					logger.severe( TourType.TYPE_CATEGORY_LABELS[tourTypeCategory] + " " + TourType.TYPE_LABELS[tourTypeCategory][m] + " tour " + t + " for person " + person + " in household " + hh_id);
					logger.severe( "selected DC alternative " + chosen + " which translates to Dest TAZ " + chosenDestAlt + " and subzone " + chosenShrtWlk);
					logger.severe( "however, the selected subzone has proportion " + (chosenDestAlt == 1 ? "short walk" : "long walk") + " equal to 0.0.");
					System.exit(1);
				}

			}

		}

	}




	public void jointTourTc ( Household hh ) {

		int[] jtPersons;
		
		long markTime=0;
		long startTime = System.currentTimeMillis();

		if (useMessageWindow) mw.setMessage1 ("Time-of-day Choice for Joint Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		// get the array of mandatory tours for this household.	
		if (hh.getJointTours() == null)
			return;
			

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setOrigTaz (hh_taz_id);
		hh.setTourCategory( TourType.JOINT_CATEGORY );


		// loop over all puposes for the mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.jointTours.length; t++) {

				if ( hh.jointTours[t].getTourType() != tourTypes[m] )
					continue;

				hh.jointTours[t].setOrigTaz (hh_taz_id);
				hh.setTourID ( t );


				jtPersons = hh.jointTours[t].getJointTourPersons();
				
				if (jtPersons.length < 2) {
					logger.severe( "fewer than 2 persons participating in joint tour for household " + hh_id + ", joint tour number " + t);
					System.exit(1);
				}

				
				// set TOD choice availability for each person in joint tour
				// tcSample and tcAvailability are 1 based
				markTime = System.currentTimeMillis();

				Arrays.fill(tcSample, 1);
				Arrays.fill(tcAvailability, true);

				for (int p=0; p < jtPersons.length; p++) {
					setTcAvailability ( persons[ jtPersons[p] ], tcAvailability, tcSample );
				}


				// count the number of tours in which no time-of-day alternative was available
				int noTOD = 0;
				for (int p=1; p <= tcUEC[m].getNumberOfAlternatives(); p++) {
					if (tcAvailability[p]) {
						noTOD++;
						break;
					}
				}
				if (noTOD == 0) {
					noTODAvailableIndiv[m]++;
					tcAvailability[1] = true;
					tcSample[1] = 1;
					tcAvailability[tcUEC[m].getNumberOfAlternatives()] = true;
					tcSample[tcUEC[m].getNumberOfAlternatives()] = 1;
				}

				hh.setChosenDest( hh.jointTours[t].getDestTaz() );
				hh.setChosenWalkSegment( hh.jointTours[t].getDestShrtWlk() );

				// compute time-of-day choice proportions and choose alternative
				tc[m].updateLogitModel ( hh, tcAvailability, tcSample );

				int chosenTODAlt;
				try {
					chosenTODAlt = tc[m].getChoiceResult();
				}
				catch (ModelException e) {
					chosenTODAlt = SeededRandom.getRandom() < 0.5 ? 1 : 190;
				}
				tcTime += (System.currentTimeMillis()-markTime);
				

				start = com.pb.morpc.models.TODDataManager.getTodStartHour ( chosenTODAlt );
				end = com.pb.morpc.models.TODDataManager.getTodEndHour ( chosenTODAlt );
				for (int j=start; j <= end; j++) {
					// set hours unavailable for each person in joint tour
					for (int p=0; p < jtPersons.length; p++) {
						hh.persons[ jtPersons[p] ].setHourUnavailable(j);
						persons[ jtPersons[p] ].setHourUnavailable(j);
					}
				}

				// set chosen in alternative in DMU and tour objects
				hh.jointTours[t].setTimeOfDayAlt (chosenTODAlt);


			}

		}

	}




	public void jointTourMc ( Household hh ) {

		int[] jtPersons;
		
		long markTime=0;
		long startTime = System.currentTimeMillis();

		if (useMessageWindow) mw.setMessage1 ("Mode Choice for Joint Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		// get the array of mandatory tours for this household.	
		if (hh.getJointTours() == null)
			return;
			

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setOrigTaz (hh_taz_id);
		hh.setTourCategory( TourType.JOINT_CATEGORY );


		// loop over all puposes for the mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.jointTours.length; t++) {

				if ( hh.jointTours[t].getTourType() != tourTypes[m] )
					continue;

				hh.jointTours[t].setOrigTaz (hh_taz_id);
				hh.setTourID ( t );


				jtPersons = hh.jointTours[t].getJointTourPersons();
				
				if (jtPersons.length < 2) {
					logger.severe( "fewer than 2 persons participating in joint tour for household " + hh_id + ", joint tour number " + t);
					System.exit(1);
				}

				


				// compute mode choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				Arrays.fill(mcSample, 1);
				Arrays.fill (mcAvailability, true);

				hh.setChosenDest( hh.jointTours[t].getDestTaz() );
				hh.setChosenWalkSegment( hh.jointTours[t].getDestShrtWlk() );
				hh.setChosenTodAlt( hh.jointTours[t].getTimeOfDayAlt() );
				
				setMcODUtility ( hh, m );
				// set transit modes to unavailable if a no walk access subzone was selected in DC.
				if ( hh.jointTours[t].getDestShrtWlk() == 0 ) {
					mcSample[3] = 0;
					mcAvailability[3] = false;
					mcSample[4] = 0;
					mcAvailability[4] = false;
				}
				mc[m].updateLogitModel ( hh, mcAvailability, mcSample );
				int chosenModeAlt = mc[m].getChoiceResult();
				mcTime += (System.currentTimeMillis() - markTime);

				// set chosen in alternative in tour objects
				hh.jointTours[t].setMode (chosenModeAlt);


				// set park zone to zero, not used for joint.
				hh.jointTours[t].setChosenPark (0);


				// set submode for transit modes
				index.setOriginZone( hh.jointTours[t].getOrigTaz() );
				index.setDestZone( hh.jointTours[t].getDestTaz() );

				if ( chosenModeAlt == 3 || chosenModeAlt == 4 ) {

					// set outbound submodes
					if (chosenModeAlt == 3) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// wt, am
							submodeUtility = smcUEC[0].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// wt, pm
							submodeUtility = smcUEC[2].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[1].solve( index, hh, smcSample ); // wt, op
				
					}
					else if (chosenModeAlt == 4) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// dt, am
							submodeUtility = smcUEC[3].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// dt, pm
							submodeUtility = smcUEC[5].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[4].solve( index, hh, smcSample );	// dt, op
				
					}

					if (submodeUtility[0] >= 10000)
						hh.jointTours[t].setSubmodeOB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.jointTours[t].setSubmodeOB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.jointTours[t].setSubmodeOB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.jointTours[t].setSubmodeOB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.jointTours[t].setSubmodeOB ( SubmodeType.LBS );
					else
						hh.jointTours[t].setSubmodeOB ( 6 );


					
					// set inbound submodes
					if (chosenModeAlt == 3) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// wt, am
							submodeUtility = smcUEC[6].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// wt, pm
							submodeUtility = smcUEC[8].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[7].solve( index, hh, smcSample ); // wt, op
				
					}
					else if (chosenModeAlt == 4) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// dt, am
							submodeUtility = smcUEC[9].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// dt, pm
							submodeUtility = smcUEC[11].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[10].solve( index, hh, smcSample );	// dt, op
				
					}

					if (submodeUtility[0] >= 10000)
						hh.jointTours[t].setSubmodeIB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.jointTours[t].setSubmodeIB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.jointTours[t].setSubmodeIB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.jointTours[t].setSubmodeIB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.jointTours[t].setSubmodeIB ( SubmodeType.LBS );
					else
						hh.jointTours[t].setSubmodeIB ( 6 );

				}
				else {

					hh.jointTours[t].setSubmodeOB ( 6 );
					hh.jointTours[t].setSubmodeIB ( 6 );

				}

			
				if ( (hh.jointTours[t].getMode() == 3 || hh.jointTours[t].getMode() == 4) && (hh.jointTours[t].getSubmodeOB() == 6 || hh.jointTours[t].getSubmodeOB() == 6) ) {
					logger.warning ( "invalid submode for joint tour=" + t + " for hhid=" + hh.getID() + ", tour mode=" + hh.jointTours[t].getMode() + ", ob submode=" + hh.jointTours[t].getSubmodeOB() + " and ib submode=" + hh.jointTours[t].getSubmodeIB() + "." );
				}
				
			}

		}

	}




	public void indivNonMandatoryTourDc ( Household hh ) {

		long markTime=0;
		long startTime = System.currentTimeMillis();

		if (useMessageWindow) mw.setMessage1 ("Destination Choice for Non-mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		// get the array of mandatory tours for this household.	
		if (hh.getIndivTours() == null)
			return;
			

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setOrigTaz (hh_taz_id);
		hh.setTourCategory( TourType.NON_MANDATORY_CATEGORY );


		// loop over all puposes for the individual non-mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.indivTours.length; t++) {

				if ( hh.indivTours[t].getTourType() != tourTypes[m] )
					continue;

				person = hh.indivTours[t].getTourPerson();




				hh.indivTours[t].setOrigTaz (hh_taz_id);
				hh.setPersonID ( person );
				hh.setTourID ( t );


				// get the destination choice sample of alternatives
				markTime = System.currentTimeMillis();

				Arrays.fill ( dcAvailability, false );

				// determine the set of alternatives from which the sample of alternatives will be drawn
				if (preSampleSize > 0) {

					// select the first preSampleSize alternatives to be available for being in the sample of alternatives
					k = 0;
					preSampleCount = 0;
					while ( preSampleCount < preSampleSize ) {
				
						// set destination choice alternative availability to true if size > 0 for the segment.
						preSampleAlt = (int) (dcAvailability.length * SeededRandom.getRandom());
						float size = zdm.getTotSize (tourTypes[m], preSampleAlt );
						if ( size > 0.0 ) {
							dcAvailability[preSampleAlt] = true;
							preSampleCount++;
						}
					
						k++;
						if (k >= dcAvailability.length) {
							logger.severe( dcAvailability.length + " mandatory dc alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
							System.exit(-1);
						}

					}
					
				}
				else {

					for (k=0; k < dcAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						float size = zdm.getTotSize (tourTypes[m], k );
						if ( size > 0.0 ) {
							dcAvailability[k] = true;
						}
					}

				}
				
				soa[m][0].applySampleOfAlternativesChoiceModel ( hh, dcAvailability );
				int[] sample = soa[m][0].getSampleOfAlternatives();
				float[] corrections = soa[m][0].getSampleCorrectionFactors();

				Arrays.fill(dcSample, 0);
				Arrays.fill(dcAvailability, false);
				Arrays.fill(dcCorrections[processorId], 0.0f);
				for (int i=1; i < sample.length; i++) {
					dcSample[sample[i]] = 1;
					dcAvailability[sample[i]] = true;
					dcCorrections[processorId][sample[i]] = corrections[i];
				}
				soaTime += (System.currentTimeMillis()-markTime);


				k = 1;
				markTime = System.currentTimeMillis();
				for (int i=1; i < sample.length; i++) {

					int d = (int)((sample[i]-1)/WALK_SEGMENTS) + 1;
					int s = sample[i] - (d-1)*WALK_SEGMENTS - 1;
					hh.setChosenDest( d );
					hh.setChosenWalkSegment( s );
							
					// calculate mode choice logsum based on appropriate od skims for each individual non-mandatory purpose
					if (TourType.NON_MANDATORY_TYPES[m] == TourType.ESCORTING) {
						hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdMd" );
						zdm.setLogsumDcMDMD ( processorId, sample[i], getMcLogsums(hh, m) );
					}
					else {
						hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdMd" );
						zdm.setLogsumDcMDMD ( processorId, sample[i], getMcLogsums(hh, m) );
						hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "PmNt" );
						zdm.setLogsumDcPMNT ( processorId, sample[i], getMcLogsums(hh, m) );
					}

				}
				dcLogsumTime += (System.currentTimeMillis()-markTime);

				

				// compute destination choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				dc[m].updateLogitModel ( hh, dcAvailability, dcSample );
				int chosen = dc[m].getChoiceResult();
				int chosenDestAlt = (int)((chosen-1)/WALK_SEGMENTS) + 1;
				int chosenShrtWlk = chosen - (chosenDestAlt-1)*WALK_SEGMENTS - 1;
				dcTime += (System.currentTimeMillis() - markTime);

				// set the chosen value in DMU and tour objects
				hh.indivTours[t].setDestTaz (chosenDestAlt);
				hh.indivTours[t].setDestShrtWlk (chosenShrtWlk);


				// check to make sure that the DC subzone selected has subzone proportion > 0.0.
				if ( zdm.getWalkPct ( chosenShrtWlk, chosenDestAlt ) == 0.0f ) {
					logger.severe( TourType.TYPE_CATEGORY_LABELS[tourTypeCategory] + " " + TourType.TYPE_LABELS[tourTypeCategory][m] + " tour " + t + " for person " + person + " in household " + hh_id);
					logger.severe( "selected DC alternative " + chosen + " which translates to Dest TAZ " + chosenDestAlt + " and subzone " + chosenShrtWlk);
					logger.severe( "however, the selected subzone has proportion " + (chosenDestAlt == 1 ? "short walk" : "long walk") + " equal to 0.0.");
					System.exit(1);
				}

			}
				
		}

	}



	public void indivNonMandatoryTourTc ( Household hh ) {

		long markTime=0;
		long startTime = System.currentTimeMillis();

		if (useMessageWindow) mw.setMessage1 ("Time-of-day Choice for Non-mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		// get the array of mandatory tours for this household.	
		if (hh.getIndivTours() == null)
			return;
			

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setOrigTaz (hh_taz_id);
		hh.setTourCategory( TourType.NON_MANDATORY_CATEGORY );


		// loop over all puposes for the individual non-mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.indivTours.length; t++) {

				if ( hh.indivTours[t].getTourType() != tourTypes[m] )
					continue;

				person = hh.indivTours[t].getTourPerson();




				hh.indivTours[t].setOrigTaz (hh_taz_id);
				hh.setPersonID ( person );
				hh.setTourID ( t );


				
				// update the time of day choice availabilty based on available time windows
				// tcSample and tcAvailability are 1 based
				Arrays.fill(tcSample, 1);
				Arrays.fill(tcAvailability, true);

				setTcAvailability ( persons[person], tcAvailability, tcSample );

				
				// count the number of tours in which no time-of-day alternative was available
				int noTOD = 0;
				for (int p=1; p <= tcUEC[m].getNumberOfAlternatives(); p++) {
					if (tcAvailability[p]) {
						noTOD++;
						break;
					}
				}
				if (noTOD == 0) {
					noTODAvailableIndiv[m]++;
					tcAvailability[1] = true;
					tcSample[1] = 1;
					tcAvailability[tcUEC[m].getNumberOfAlternatives()] = true;
					tcSample[tcUEC[m].getNumberOfAlternatives()] = 1;
				}



				// calculate the mode choice logsums for TOD choice based on chosen dest and default time periods
				markTime = System.currentTimeMillis();
				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaEa" );
				tcLogsumEaEa = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaAm" );
				tcLogsumEaAm = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaMd" );
				tcLogsumEaMd = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaPm" );
				tcLogsumEaPm = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "EaNt" );
				tcLogsumEaNt = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmAm" );
				tcLogsumAmAm = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmMd" );
				tcLogsumAmMd = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmPm" );
				tcLogsumAmPm = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "AmNt" );
				tcLogsumAmNt = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdMd" );
				tcLogsumMdMd = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdPm" );
				tcLogsumMdPm = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "MdNt" );
				tcLogsumMdNt = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "PmPm" );
				tcLogsumPmPm = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "PmNt" );
				tcLogsumPmNt = getMcLogsums ( hh, m );

				hh.setTODDefaults ( TourType.NON_MANDATORY_CATEGORY, "NtNt" );
				tcLogsumNtNt = getMcLogsums ( hh, m );



				// assign mode choice logsums to time-of-day choice alternatives, given correspondiong time periods
				com.pb.morpc.models.TODDataManager.logsumTcEAEA[processorId][1]   = tcLogsumEaEa;
				com.pb.morpc.models.TODDataManager.logsumTcEAEA[processorId][2]   = tcLogsumEaEa;
				com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][3]   = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][4]   = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][5]   = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][6]   = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][7]   = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][8]   = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][9]   = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][10]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][11]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][12]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][13]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][14]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][15]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][16]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][17]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][18]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][19]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcEAEA[processorId][20]  = tcLogsumEaEa;
				com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][21]  = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][22]  = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcEAAM[processorId][23]  = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][24]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][25]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][26]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][27]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][28]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAMD[processorId][29]  = tcLogsumEaMd;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][30]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][31]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][32]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEAPM[processorId][33]  = tcLogsumEaPm;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][34]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][35]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][36]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcEANT[processorId][37]  = tcLogsumEaNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][38]  = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][39]  = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][40]  = tcLogsumEaAm;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][41]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][42]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][43]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][44]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][45]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][46]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][47]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][48]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][49]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][50]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][51]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][52]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][53]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][54]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][55]  = tcLogsumAmAm;
				com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][56]  = tcLogsumAmAm;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][57]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][58]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][59]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][60]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][61]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][62]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][63]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][64]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][65]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][66]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][67]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][68]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][69]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][70]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMAM[processorId][71]  = tcLogsumAmAm;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][72]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][73]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][74]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][75]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][76]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMMD[processorId][77]  = tcLogsumAmMd;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][78]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][79]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][80]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMPM[processorId][81]  = tcLogsumAmPm;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][82]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][83]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][84]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcAMNT[processorId][85]  = tcLogsumAmNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][86]  = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][87]  = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][88]  = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][89]  = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][90]  = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][91]  = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][92]  = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][93]  = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][94]  = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][95]  = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][96]  = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][97]  = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][98]  = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][99]  = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][100] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][101] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][102] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][103] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][104] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][105] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][106] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][107] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][108] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][109] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][110] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][111] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][112] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][113] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][114] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][115] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][116] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][117] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][118] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][119] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][120] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][121] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][122] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][123] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][124] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][125] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][126] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][127] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][128] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][129] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][130] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][131] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][132] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][133] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][134] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][135] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][136] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][137] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][138] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][139] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][140] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][141] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][142] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][143] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][144] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][145] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDMD[processorId][146] = tcLogsumMdMd;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][147] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][148] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][149] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDPM[processorId][150] = tcLogsumMdPm;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][151] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][152] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][153] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcMDNT[processorId][154] = tcLogsumMdNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][155] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][156] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][157] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][158] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][159] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][160] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][161] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][162] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][163] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][164] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][165] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][166] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][167] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][168] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][169] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][170] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][171] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][172] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][173] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][174] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][175] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMPM[processorId][176] = tcLogsumPmPm;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][177] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][178] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][179] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcPMNT[processorId][180] = tcLogsumPmNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][181] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][182] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][183] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][184] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][185] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][186] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][187] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][188] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][189] = tcLogsumNtNt;
				com.pb.morpc.models.TODDataManager.logsumTcNTNT[processorId][190] = tcLogsumNtNt;
				tcLogsumTime += (System.currentTimeMillis()-markTime);


				hh.setChosenDest( hh.indivTours[t].getDestTaz() );
				hh.setChosenWalkSegment( hh.indivTours[t].getDestShrtWlk() );

				
				// compute time-of-day choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				tc[m].updateLogitModel ( hh, tcAvailability, tcSample );

				int chosenTODAlt;
				try {
					chosenTODAlt = tc[m].getChoiceResult();
				}
				catch (ModelException e) {
					chosenTODAlt = SeededRandom.getRandom() < 0.5 ? 1 : 190;
				}
				tcTime += (System.currentTimeMillis()-markTime);
				

				// set the hour as unavailable for all hours between start and end for this person
				start = com.pb.morpc.models.TODDataManager.getTodStartHour ( chosenTODAlt );
				end = com.pb.morpc.models.TODDataManager.getTodEndHour ( chosenTODAlt );
				for (int j=start; j <= end; j++) {
					hh.persons[person].setHourUnavailable(j);
					persons[person].setHourUnavailable(j);
				}

				// set chosen in alternative in tour objects
				hh.indivTours[t].setTimeOfDayAlt (chosenTODAlt);

			}
				
		}

	}



	public void indivNonMandatoryTourMc ( Household hh ) {

		long markTime=0;
		long startTime = System.currentTimeMillis();

		if (useMessageWindow) mw.setMessage1 ("Mode Choice for Non-mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		// get the array of mandatory tours for this household.	
		if (hh.getIndivTours() == null)
			return;
			

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setOrigTaz (hh_taz_id);
		hh.setTourCategory( TourType.NON_MANDATORY_CATEGORY );


		// loop over all puposes for the individual non-mandatory tour category in order
		for (int m=0; m < tourTypes.length; m++) {

			// loop over individual tours of the tour purpose of interest for the hh
			for (int t=0; t < hh.indivTours.length; t++) {

				if ( hh.indivTours[t].getTourType() != tourTypes[m] )
					continue;

				person = hh.indivTours[t].getTourPerson();




				hh.indivTours[t].setOrigTaz (hh_taz_id);
				hh.setPersonID ( person );
				hh.setTourID ( t );


				// compute mode choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				Arrays.fill(mcSample, 1);
				Arrays.fill (mcAvailability, true);

				hh.setChosenDest( hh.indivTours[t].getDestTaz() );
				hh.setChosenWalkSegment( hh.indivTours[t].getDestShrtWlk() );
				hh.setChosenTodAlt( hh.indivTours[t].getTimeOfDayAlt() );
				
				
				setMcODUtility ( hh, m );
				// set transit modes to unavailable if a no walk access subzone was selected in DC.
				if ( hh.indivTours[t].getDestShrtWlk() == 0 ) {
					mcSample[3] = 0;
					mcAvailability[3] = false;
					mcSample[4] = 0;
					mcAvailability[4] = false;
				}
				mc[m].updateLogitModel ( hh, mcAvailability, mcSample );
				int chosenModeAlt = mc[m].getChoiceResult();
				mcTime += (System.currentTimeMillis()-markTime);

				// set chosen in alternative in tour objects
				hh.indivTours[t].setMode (chosenModeAlt);





				index.setOriginZone( hh.indivTours[t].getOrigTaz() );
				index.setDestZone( hh.indivTours[t].getDestTaz() );
				int chosenParkAlt=0;
				// determine parking location if chosenDestAlt is in the CBD and chosenModeAlt is sov or hov.
				if ( hh.getCbdDest() && chosenModeAlt < 3 ) {
					
					pc[2].updateLogitModel ( hh, pcAvailability, pcSample );
					chosenParkAlt = pc[2].getChoiceResult();

					hh.indivTours[t].setChosenPark (chosenParkAlt);

				}
				else { 

					hh.indivTours[t].setChosenPark (0);

				}




				// set submode for transit modes
				index.setOriginZone( hh.indivTours[t].getOrigTaz() );
				index.setDestZone( hh.indivTours[t].getDestTaz() );

				if ( chosenModeAlt == 3 || chosenModeAlt == 4 ) {

					// set outbound submodes
					if (chosenModeAlt == 3) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// wt, am
							submodeUtility = smcUEC[0].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// wt, pm
							submodeUtility = smcUEC[2].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[1].solve( index, hh, smcSample ); // wt, op
				
					}
					else if (chosenModeAlt == 4) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// dt, am
							submodeUtility = smcUEC[3].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// dt, pm
							submodeUtility = smcUEC[5].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[4].solve( index, hh, smcSample );	// dt, op
				
					}

					if (submodeUtility[0] >= 10000)
						hh.indivTours[t].setSubmodeOB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.indivTours[t].setSubmodeOB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.indivTours[t].setSubmodeOB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.indivTours[t].setSubmodeOB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.indivTours[t].setSubmodeOB ( SubmodeType.LBS );
					else
						hh.indivTours[t].setSubmodeOB ( 6 );


					
					// set inbound submodes
					if (chosenModeAlt == 3) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// wt, am
							submodeUtility = smcUEC[6].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// wt, pm
							submodeUtility = smcUEC[8].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[7].solve( index, hh, smcSample ); // wt, op
				
					}
					else if (chosenModeAlt == 4) {
					
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// dt, am
							submodeUtility = smcUEC[9].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// dt, pm
							submodeUtility = smcUEC[11].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[10].solve( index, hh, smcSample );	// dt, op
				
					}

					if (submodeUtility[0] >= 10000)
						hh.indivTours[t].setSubmodeIB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.indivTours[t].setSubmodeIB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.indivTours[t].setSubmodeIB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.indivTours[t].setSubmodeIB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.indivTours[t].setSubmodeIB ( SubmodeType.LBS );
					else
						hh.indivTours[t].setSubmodeIB ( 6 );

				}
				else {

					hh.indivTours[t].setSubmodeOB ( 6 );
					hh.indivTours[t].setSubmodeIB ( 6 );

				}

			
				if ( (hh.indivTours[t].getMode() == 3 || hh.indivTours[t].getMode() == 4) && (hh.indivTours[t].getSubmodeOB() == 6 || hh.indivTours[t].getSubmodeOB() == 6) ) {
					logger.warning ( "invalid submode for individual non-mandatory tour=" + t + " for hhid=" + hh.getID() + ", tour mode=" + hh.indivTours[t].getMode() + ", ob submode=" + hh.indivTours[t].getSubmodeOB() + " and ib submode=" + hh.indivTours[t].getSubmodeIB() + "." );
				}
				
			}
				
		}

	}



	public void atWorkTourDc ( Household hh ) {

		long markTime=0;
		int soaIndex = 0;
		Tour[] st;
		int todAlt;
		int startP;
		int endP;
		
		int m = 0;

		if (useMessageWindow) mw.setMessage1 ("Destination Choice for At-work Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		long startTime = System.currentTimeMillis();

		
		// get the array of mandatory tours for this household.	
		if (hh.getMandatoryTours() == null)
			return;
		

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setTourCategory( TourType.AT_WORK_CATEGORY );


		
		// loop over individual tours of the tour purpose of interest for the hh
		for (int t=0; t < hh.mandatoryTours.length; t++) {

			// get the array of subtours for this work tour
			if (hh.mandatoryTours[t].getSubTours() == null)
				continue;

			person = hh.mandatoryTours[t].getTourPerson();

			hh.mandatoryTours[t].setOrigTaz (hh_taz_id);
			hh.setPersonID ( person );
			hh.setTourID ( t );

				

			// loop over subtours
			for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

				// the origin for the at-work tour is the destination of the primary work tour
				hh.mandatoryTours[t].subTours[s].setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
				hh.setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
				hh.setSubtourID ( s );

        
				if (hh.mandatoryTours[t].subTours[s].getSubTourType() == SubTourType.WORK)
					soaIndex = 0;
				else if (hh.mandatoryTours[t].subTours[s].getSubTourType() == SubTourType.EAT)
					soaIndex = 1;
				else if (hh.mandatoryTours[t].subTours[s].getSubTourType() == SubTourType.OTHER)
					soaIndex = 2;

				
        		// get the destination choice sample of alternatives
				markTime = System.currentTimeMillis();

				Arrays.fill ( dcAvailability, false );

				// determine the set of alternatives from which the sample of alternatives will be drawn
				if (preSampleSize > 0) {

					// select the first preSampleSize alternatives to be available for being in the sample of alternatives
					k = 0;
					preSampleCount = 0;
					while ( preSampleCount < preSampleSize ) {
				
						// set destination choice alternative availability to true if size > 0 for the segment.
						preSampleAlt = (int) (dcAvailability.length * SeededRandom.getRandom());
						float size = zdm.getTotSize (tourTypes[m], preSampleAlt );
						if ( size > 0.0 ) {
							dcAvailability[preSampleAlt] = true;
							preSampleCount++;
						}
					
						k++;
						if (k >= dcAvailability.length) {
							logger.severe( dcAvailability.length + " mandatory dc alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
							System.exit(-1);
						}

					}
					
				}
				else {

					for (k=0; k < dcAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						float size = zdm.getTotSize (tourTypes[m], k );
						if ( size > 0.0 ) {
							dcAvailability[k] = true;
						}
					}

				}
				
        		soa[m][soaIndex].applySampleOfAlternativesChoiceModel ( hh, dcAvailability );
        		int[] sample = soa[m][soaIndex].getSampleOfAlternatives();
        		float[] corrections = soa[m][soaIndex].getSampleCorrectionFactors();

				Arrays.fill(dcSample, 0);
				Arrays.fill(dcAvailability, false);
				Arrays.fill(dcCorrections[processorId], 0.0f);
        		for (int i=1; i < sample.length; i++) {
					dcSample[sample[i]] = 1;
					dcAvailability[sample[i]] = true;
        			dcCorrections[processorId][sample[i]] = corrections[i];
        		}
    			soaTime += (System.currentTimeMillis()-markTime);
        
        
        
        		k = 1;
				markTime = System.currentTimeMillis();
				for (int i=1; i < sample.length; i++) {

					int d = (int)((sample[i]-1)/WALK_SEGMENTS) + 1;
					int w = sample[i] - (d-1)*WALK_SEGMENTS - 1;
					hh.setChosenDest( d );
					hh.setChosenWalkSegment( w );
							
					// calculate mode choice logsum based on appropriate od skims
					hh.setTODDefaults ( TourType.AT_WORK_CATEGORY, "MdMd" );
					zdm.setLogsumDcMDMD ( processorId, sample[i], getMcLogsums(hh, m) );

				}
				dcLogsumTime += (System.currentTimeMillis()-markTime);
        
				
        		// compute destination choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				dc[m].updateLogitModel ( hh, dcAvailability, dcSample );
				int chosen = dc[m].getChoiceResult();
        		int chosenDestAlt = (int)((chosen-1)/WALK_SEGMENTS) + 1;
        		int chosenShrtWlk = chosen - (chosenDestAlt-1)*WALK_SEGMENTS - 1;
				dcTime += (System.currentTimeMillis() - markTime);

        
				// check to make sure that the DC subzone selected has subzone proportion > 0.0.
				if ( zdm.getWalkPct ( chosenShrtWlk, chosenDestAlt ) == 0.0f) {
					logger.severe( "At work subtour " + s + " in mandatory tour " + t + " for person " + person + " in household " + hh_id);
					logger.severe( "selected DC alternative " + chosen + " which translates to Dest TAZ " + chosenDestAlt + " and subzone " + chosenShrtWlk);
					logger.severe( "however, the selected subzone has proportion " + (chosenDestAlt == 1 ? "short walk" : "long walk") + " equal to 0.0.");
					System.exit(1);
				}


        		// set the chosen value in DMU and tour objects
        		hh.mandatoryTours[t].subTours[s].setDestTaz (chosenDestAlt);
        		hh.mandatoryTours[t].subTours[s].setDestShrtWlk (chosenShrtWlk);
				hh.mandatoryTours[t].subTours[s].setChosenPark (chosenDestAlt);

			}
			
        }

	}



	
	public void atWorkTourTc ( Household hh ) {

		long markTime=0;
		int soaIndex = 0;
		Tour[] st;
		int todAlt;
		int startP;
		int endP;
		
		int m = 0;

		if (useMessageWindow) mw.setMessage1 ("Time-of-day Choice for At-work Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		long startTime = System.currentTimeMillis();

		
		// get the array of mandatory tours for this household.	
		if (hh.getMandatoryTours() == null)
			return;
		

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setTourCategory( TourType.AT_WORK_CATEGORY );


		
		// loop over individual tours of the tour purpose of interest for the hh
		for (int t=0; t < hh.mandatoryTours.length; t++) {

			// get the array of subtours for this work tour
			if (hh.mandatoryTours[t].getSubTours() == null)
				continue;

			person = hh.mandatoryTours[t].getTourPerson();

			hh.mandatoryTours[t].setOrigTaz (hh_taz_id);
			hh.setPersonID ( person );
			hh.setTourID ( t );

				
				


			// only hours between start and end of primary work tour are available for subtour
			todAlt = hh.mandatoryTours[t].getTimeOfDayAlt();
			start = com.pb.morpc.models.TODDataManager.getTodStartHour ( todAlt );
			end = com.pb.morpc.models.TODDataManager.getTodEndHour ( todAlt );
			for (int p=1; p < tcAvailability.length; p++) {

				startP = com.pb.morpc.models.TODDataManager.getTodStartHour ( p );
				endP = com.pb.morpc.models.TODDataManager.getTodEndHour ( p );

				if (startP >= start && endP <= end) {
					tcAvailability[p] = true;
					tcSample[p] = 1;
				}
				else {
					tcAvailability[p] = false;
					tcSample[p] = 0;
				}
			}



			// loop over subtours
			for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

				// the origin for the at-work tour is the destination of the primary work tour
				hh.mandatoryTours[t].subTours[s].setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
				hh.setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
				hh.setSubtourID ( s );

        
				// determine the TOD alts for the begin and end hours to use in case no alternatives are available
				int defaultStart = 0;
				int defaultEnd = 0;
				for (int p=1; p < tcAvailability.length; p++) {

					startP = com.pb.morpc.models.TODDataManager.getTodStartHour ( p );
					endP = com.pb.morpc.models.TODDataManager.getTodEndHour ( p );

					if ( startP == start && endP == start )
						defaultStart = p;
					if ( startP == end && endP == end )
						defaultEnd = p;
						
				}

					
					
				// count the number of tours in which no time-of-day alternative was available
				int noTOD = 0;
				for (int p=1; p <= tcUEC[m].getNumberOfAlternatives(); p++) {
					if (tcAvailability[p]) {
						noTOD++;
						break;
					}
				}
        		
        		
				if (noTOD == 0) {
					noTODAvailableIndiv[m]++;
					tcAvailability[defaultStart] = true;
					tcSample[defaultStart] = 1;
					tcAvailability[defaultEnd] = true;
					tcSample[defaultEnd] = 1;
				}

        

				hh.setChosenDest( hh.mandatoryTours[t].subTours[s].getDestTaz() );
				hh.setChosenWalkSegment( hh.mandatoryTours[t].subTours[s].getDestShrtWlk() );


				// compute time-of-day choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				tc[m].updateLogitModel ( hh, tcAvailability, tcSample );

				int chosenTODAlt;
				try {
					chosenTODAlt = tc[m].getChoiceResult();
				}
				catch (ModelException e) {
					chosenTODAlt = SeededRandom.getRandom() < 0.5 ? 1 : 190;
				}
				tcTime += (System.currentTimeMillis()-markTime);
				



				// set hours unavailable associated with this subtour in case there are more subtours
				start = com.pb.morpc.models.TODDataManager.getTodStartHour ( todAlt );
				end = com.pb.morpc.models.TODDataManager.getTodEndHour ( todAlt );
				for (int p=1; p < tcAvailability.length; p++) {

					startP = com.pb.morpc.models.TODDataManager.getTodStartHour ( p );
					endP = com.pb.morpc.models.TODDataManager.getTodEndHour ( p );

					if ( (startP >= start && startP <= end) ||
						(endP >= start && endP <= end) ||
						(startP < start && endP > end) ) {
							tcAvailability[p] = false;
							tcSample[p] = 0;
					}

				}

					
					
				for (int j=start; j <= end; j++) {
					hh.persons[person].setHourUnavailable(j);
					persons[person].setHourUnavailable(j);
				}
    
				// set chosen in alternative in DMU and tour objects
				hh.mandatoryTours[t].subTours[s].setTimeOfDayAlt (chosenTODAlt);

			}
					
		}

	}



	
	public void atWorkTourMc ( Household hh ) {

		long markTime=0;
		int soaIndex = 0;
		Tour[] st;
		int todAlt;
		int startP;
		int endP;
		
		int m = 0;

		if (useMessageWindow) mw.setMessage1 ("Mode Choice for At-work Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		
		
		
		long startTime = System.currentTimeMillis();

		
		// get the array of mandatory tours for this household.	
		if (hh.getMandatoryTours() == null)
			return;
		

		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		hh_id     = hh.getID();
		hh_taz_id = hh.getTazID();
		hh.setTourCategory( TourType.AT_WORK_CATEGORY );


		
		// loop over individual tours of the tour purpose of interest for the hh
		for (int t=0; t < hh.mandatoryTours.length; t++) {

			// get the array of subtours for this work tour
			if (hh.mandatoryTours[t].getSubTours() == null)
				continue;

			person = hh.mandatoryTours[t].getTourPerson();

			hh.mandatoryTours[t].setOrigTaz (hh_taz_id);
			hh.setPersonID ( person );
			hh.setTourID ( t );

				
			// loop over subtours
			for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

				// the origin for the at-work tour is the destination of the primary work tour
				hh.mandatoryTours[t].subTours[s].setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
				hh.setOrigTaz ( hh.mandatoryTours[t].getDestTaz() );
				hh.setSubtourID ( s );

        
				// compute mode choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				Arrays.fill(mcSample, 1);
				Arrays.fill (mcAvailability, true);

				hh.setChosenDest( hh.mandatoryTours[t].subTours[s].getDestTaz() );
				hh.setChosenWalkSegment( hh.mandatoryTours[t].subTours[s].getDestShrtWlk() );
				hh.setChosenTodAlt( hh.mandatoryTours[t].subTours[s].getTimeOfDayAlt() );


				setMcODUtility ( hh, m );
				// set transit modes to unavailable if a no walk access subzone was selected in DC.
				if ( hh.mandatoryTours[t].subTours[s].getDestShrtWlk() == 0 ) {
					mcSample[3] = 0;
					mcAvailability[3] = false;
					mcSample[4] = 0;
					mcAvailability[4] = false;
				}
				mc[m].updateLogitModel ( hh, mcAvailability, mcSample );
				int chosenModeAlt = mc[m].getChoiceResult();
				mcTime += (System.currentTimeMillis() - markTime);
    
				// set chosen in alternative in tour objects
				hh.mandatoryTours[t].subTours[s].setMode (chosenModeAlt);
    

				// set park zone to zero, not used for at-work.
				hh.mandatoryTours[t].subTours[s].setChosenPark (0);



				// set submode for transit modes
				index.setOriginZone( hh.mandatoryTours[t].subTours[s].getOrigTaz() );
				index.setDestZone( hh.mandatoryTours[t].subTours[s].getDestTaz() );


				if ( chosenModeAlt == 3 || chosenModeAlt == 4 ) {

					// set outbound submodes
					if (chosenModeAlt == 3) {
				
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// wt, am
							submodeUtility = smcUEC[0].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// wt, pm
							submodeUtility = smcUEC[2].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[1].solve( index, hh, smcSample ); // wt, op
			
					}
					else if (chosenModeAlt == 4) {
				
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 1)	// dt, am
							submodeUtility = smcUEC[3].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(start) ) == 2)	// dt, pm
							submodeUtility = smcUEC[5].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[4].solve( index, hh, smcSample );	// dt, op
			
					}

					if (submodeUtility[0] >= 10000)
						hh.mandatoryTours[t].subTours[s].setSubmodeOB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.mandatoryTours[t].subTours[s].setSubmodeOB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.mandatoryTours[t].subTours[s].setSubmodeOB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.mandatoryTours[t].subTours[s].setSubmodeOB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.mandatoryTours[t].subTours[s].setSubmodeOB ( SubmodeType.LBS );
					else
						hh.mandatoryTours[t].subTours[s].setSubmodeOB ( 6 );


				
					// set inbound submodes
					if (chosenModeAlt == 3) {
				
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// wt, am
							submodeUtility = smcUEC[6].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// wt, pm
							submodeUtility = smcUEC[8].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[7].solve( index, hh, smcSample ); // wt, op
			
					}
					else if (chosenModeAlt == 4) {
				
						if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 1)	// dt, am
							submodeUtility = smcUEC[9].solve( index, hh, smcSample );
						else if (com.pb.morpc.models.TODDataManager.getTodSkimPeriod ( com.pb.morpc.models.TODDataManager.getTodPeriod(end) ) == 2)	// dt, pm
							submodeUtility = smcUEC[11].solve( index, hh, smcSample );
						else
							submodeUtility = smcUEC[10].solve( index, hh, smcSample );	// dt, op
			
					}

					if (submodeUtility[0] >= 10000)
						hh.mandatoryTours[t].subTours[s].setSubmodeIB ( SubmodeType.CRL );
					else if (submodeUtility[0] >= 1000)
						hh.mandatoryTours[t].subTours[s].setSubmodeIB ( SubmodeType.LRT );
					else if (submodeUtility[0] >= 100)
						hh.mandatoryTours[t].subTours[s].setSubmodeIB ( SubmodeType.BRT );
					else if (submodeUtility[0] >= 10)
						hh.mandatoryTours[t].subTours[s].setSubmodeIB ( SubmodeType.EBS );
					else if (submodeUtility[0] >= 1)
						hh.mandatoryTours[t].subTours[s].setSubmodeIB ( SubmodeType.LBS );
					else
						hh.mandatoryTours[t].subTours[s].setSubmodeIB ( 6 );

				}
				else {

					hh.mandatoryTours[t].subTours[s].setSubmodeOB ( 6 );
					hh.mandatoryTours[t].subTours[s].setSubmodeIB ( 6 );

				}
			

			
				if ( (hh.mandatoryTours[t].subTours[s].getMode() == 3 || hh.mandatoryTours[t].subTours[s].getMode() == 4) && (hh.mandatoryTours[t].subTours[s].getSubmodeOB() == 6 || hh.mandatoryTours[t].subTours[s].getSubmodeOB() == 6) ) {
					logger.warning ( "invalid submode for at-work subtour=" + t + ", subtour=" + s + " for hhid=" + hh.getID() + ", tour mode=" + hh.mandatoryTours[t].subTours[s].getMode() + ", ob submode=" + hh.mandatoryTours[t].subTours[s].getSubmodeOB() + " and ib submode=" + hh.mandatoryTours[t].subTours[s].getSubmodeIB() + "." );
				}
				
			}
			
			
					
		}

	}



	
	public void updateTimeWindows ( Household hh ) {

		// loop over all households in the hh table, or up to the number specified in the properties file.
		boolean pAdult;
		boolean qAdult;


		// get person array for this household.
		Person[] persons = hh.getPersonArray();


		int maxAdultOverlapsHH = 0;
		int maxChildOverlapsHH = 0;
		int maxMixedOverlapsHH = 0;
		int maxAdultWindowHH = 0;
		int maxChildWindowHH = 0;

		int maxAdultOverlapsP = 0;
		int maxChildOverlapsP = 0;

		// loop over persons in the household and count available time windows
		for (int p=1; p < persons.length; p++) {

			// determine if person p is an adult
			if (persons[p].getPersonType() == PersonType.WORKER_F ||
				persons[p].getPersonType() == PersonType.WORKER_P ||
				persons[p].getPersonType() == PersonType.STUDENT ||
				persons[p].getPersonType() == PersonType.NONWORKER)
					pAdult = true;
			else
					pAdult = false;

			boolean[] pAvail = persons[p].getAvailable();


			// loop over time windows between 7 & 22 (hours in which to count avaiable hours)
			// and count instances where the hour is available for person p.
			int window = 0;
			for (int w=7; w <= 22; w++) {
				if (pAvail[w])
					window++;
			}

			if (pAdult && window > maxAdultWindowHH)
				maxAdultWindowHH = window;
			else if (!pAdult && window > maxChildWindowHH)
				maxChildWindowHH = window;

				
				
			// loop over persons greater than p and compute available time window overlaps.
			// Don't need q,p if we've already done p,q,
			// so we only need triangular part of matrix above diagonal.
			for (int q=p+1; q < persons.length; q++) {

				// determine if person q is an adult
				if (persons[q].getPersonType() == PersonType.WORKER_F ||
					persons[q].getPersonType() == PersonType.WORKER_P ||
					persons[q].getPersonType() == PersonType.STUDENT ||
					persons[q].getPersonType() == PersonType.NONWORKER)
						qAdult = true;
				else
						qAdult = false;
				
				boolean[] qAvail = persons[q].getAvailable();


				// loop over time windows between 7 & 22 (hours in which to start a joint tour)
				// and count instances where the hour is available for both persons
				int overlaps = 0;
				for (int w=7; w <= 22; w++) {
					if (pAvail[w] && qAvail[w])
						overlaps++;
				}
				
				// determine max time window overlap between pairs of adults,
				// pairs of children, and pairs of 1 adult 1 child
				if (pAdult && qAdult) {
					if (overlaps > maxAdultOverlapsHH)
						maxAdultOverlapsHH = overlaps;
					if (overlaps > maxAdultOverlapsP)
						maxAdultOverlapsP = overlaps;
				}
				else if (!pAdult && !qAdult) {
					if (overlaps > maxChildOverlapsHH)
						maxChildOverlapsHH = overlaps;
					if (overlaps > maxChildOverlapsP)
						maxChildOverlapsP = overlaps;
				}
				else {
					if (overlaps > maxMixedOverlapsHH)
						maxMixedOverlapsHH = overlaps;
				}
				
			} // end of person q
			
			// set person attributes
			persons[p].setAvailableWindow( window );
			persons[p].setMaxAdultOverlaps( maxAdultOverlapsP );
			persons[p].setMaxChildOverlaps( maxChildOverlapsP );
			
		} // end of person p
		
		// set household attributes
		hh.setMaxAdultOverlaps( maxAdultOverlapsHH );
		hh.setMaxChildOverlaps( maxChildOverlapsHH );
		hh.setMaxMixedOverlaps( maxMixedOverlapsHH );
		hh.setMaxAdultWindow (maxAdultWindowHH);
		hh.setMaxChildWindow (maxChildWindowHH);
			
	}

	
	public void setShadowPricingIteration ( int iter ) {
		shadowPricingIteration = iter;
	}
	

	public void resetHouseholdCount () {
		count = 0;
	}
	

	public void printTimes ( short tourTypeCategory ) {

		for (int i=1; i < 5; i++) {		
			
			if (tourTypeCategory == i) {
				
				logger.info ( "DTM Model Component Times for " + TourType.TYPE_CATEGORY_LABELS[i] + " tours:");
				logger.info ( "total seconds processing dtm dc sample of alternatives = " + (float)soaTime/1000);
				logger.info ( "total seconds processing dtm dc logsums = " + (float)dcLogsumTime/1000);
				logger.info ( "total seconds processing dtm tc logsums = " + (float)tcLogsumTime/1000);
				logger.info ( "total seconds processing dtm dest choice = " + (float)dcTime/1000);
				logger.info ( "total seconds processing dtm tod choice = " + (float)tcTime/1000);
				logger.info ( "total seconds processing dtm mode choice = " + (float)mcTime/1000);
				logger.info ( "");
				
			}
		}
							
	}
}
