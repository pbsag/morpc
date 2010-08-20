package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */

import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.math.MathUtil;
import com.pb.morpc.structures.*;
import com.pb.common.util.SeededRandom;

import java.util.HashMap;
import java.util.Arrays;
import java.io.File;



public class StopsHousehold extends StopsModelBase implements java.io.Serializable {

	static final int AM_PERIOD = 2;
	static final int PM_PERIOD = 5;

    private static final int MODEL83_DATA_PAGE = 0;
    private static final int MODEL83_IK_PAGE = 1;
    private static final int MODEL83_KJ_PAGE = 2;
    private static final int MODEL83_JK_PAGE = 3;
    private static final int MODEL83_KI_PAGE = 4;


	int count = 1;

	long logsumTime = 0;
	long soaTime = 0;
	long freqTime = 0;
	long locTime = 0;
	long mcTime = 0;

	private	int preSampleCount = 0;
	private int preSampleAlt = 0;
	private int preSampleSize;		


	// dimension this to 2, even though there is 1 alternative, due to 1s indexing in UEC.solve()
	private int[] smcSample = new int[2];

    UtilityExpressionCalculator smcUECik = null;
    UtilityExpressionCalculator smcUECkj = null;
    UtilityExpressionCalculator smcUECjk = null;
    UtilityExpressionCalculator smcUECki = null;





	public StopsHousehold ( HashMap<String,String> propertyMap, short tourTypeCategory, short[] tourTypes ) {

		super ( propertyMap, tourTypeCategory, tourTypes );

		
		this.preSampleSize = Integer.parseInt ( (String)propertyMap.get ( "slcPreSampleSize") );		

		// initialize the availabilty for each alternative to true for every household
		for (int j=0; j < sfcAvailability.length; j++)
			sfcAvailability[j] = true;

		for (int j=0; j < slcOBAvailability.length; j++) {
			slcOBAvailability[j] = true;
			slcIBAvailability[j] = true;
		}

		
		// 0 element is irrelevant, only element 1 matters
        smcSample[0] = -1;
        smcSample[1] = 1;
	
		
		// create UECs for each segment to use for transit modes
        smcUECik = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_IK_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
        smcUECkj = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_KJ_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
        smcUECjk = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_JK_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
        smcUECki = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_KI_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
		
		this.count = 1;
	}
    
	
	public StopsHousehold ( int processorId, HashMap<String,String> propertyMap, short tourTypeCategory, short[] tourTypes ) {

		super ( processorId, propertyMap, tourTypeCategory, tourTypes );


		this.preSampleSize = Integer.parseInt ( (String)propertyMap.get ( "slcPreSampleSize") );		

		// initialize the availabilty for each alternative to true for every household
		for (int j=0; j < sfcAvailability.length; j++)
			sfcAvailability[j] = true;

		for (int j=0; j < slcOBAvailability.length; j++) {
			slcOBAvailability[j] = true;
			slcIBAvailability[j] = true;
		}

		
        smcSample[0] = -1;
        smcSample[1] = 1;
	
        // create UECs for each segment to use for transit modes
        smcUECik = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_IK_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
        smcUECkj = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_KJ_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
        smcUECjk = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_JK_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
        smcUECki = new UtilityExpressionCalculator(new File( (String)propertyMap.get(  "Model83.controlFile") ), MODEL83_KI_PAGE, MODEL83_DATA_PAGE, this.propertyMap, Household.class);
		
		this.count = 1;
	}
    




	public void mandatoryTourSfcSlc ( Household hh ) {

		int	soaIndex=0;
		int tourTypeIndex=0;
		long markTime=0;


		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ( "Stop frequency and location Choice for Mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		

		// get the array of mandatory tours for this household.	
		if (hh.getMandatoryTours() == null)
			return;
			
		
//		// get person array for this household.
//		Person[] persons = hh.getPersonArray();


		hh.setTourCategory( TourType.MANDATORY_CATEGORY );
		hh_taz_id = hh.getTazID();
		int income    = hh.getHHIncome();

        index.setHHIndex( hh_id );
        index.setZoneIndex( hh_taz_id );
        

		// loop over individual mandatory tours for the hh
		for (int t=0; t < hh.mandatoryTours.length; t++) {
		    
		    
		    
			hh.setOrigTaz ( hh.mandatoryTours[t].getOrigTaz() );
			hh.setChosenDest ( hh.mandatoryTours[t].getDestTaz() );
			index.setOriginZone( hh.mandatoryTours[t].getOrigTaz() );
			index.setDestZone( hh.mandatoryTours[t].getDestTaz() );

			// if the primary mode for this mandatory tour is non-motorized or school bus, skip stop freq choice 
			if (hh.mandatoryTours[t].getMode() == TourModeType.NM ||
				hh.mandatoryTours[t].getMode() == TourModeType.SB) {
				
				//Wu added for FTA restart
				hh.mandatoryTours[t].setStopFreqAlt(0);
				hh.mandatoryTours[t].setStopLocOB(0);
				hh.mandatoryTours[t].setStopLocIB(0);

					continue;
			}

        
			tourType = hh.mandatoryTours[t].getTourType();
			
			// set the array of sample of alternatives objects index
			if (tourType == TourType.WORK) {
				tourTypeIndex = 0;
				soaIndex = income - 1;
			} 
			else if (tourType == TourType.UNIVERSITY) {
				tourTypeIndex = 1;
				soaIndex = 0;
			}
			else if (tourType == TourType.SCHOOL) {
				tourTypeIndex = 2;
				soaIndex = 0;
			}


			person = hh.mandatoryTours[t].getTourPerson();


			
			
			hh.setOrigTaz (hh_taz_id);
			hh.setPersonID ( person );
			hh.setTourID ( t );


//			int orig = hh_taz_id;
			int chosenMode = hh.mandatoryTours[t].getMode();
//			int chosenDest = hh.mandatoryTours[t].getDestTaz();
//			int chosenShrtWlkSeg = hh.mandatoryTours[t].getDestShrtWlk();
			int autoTransit = hh.mandatoryTours[t].getModeIsAutoMode() == 1 ? 0 : 1;

			

			// get the stop location choice sample of alternatives for outbound sov, hov tours
			markTime = System.currentTimeMillis();
			
			
			if ( autoTransit == 0 ) {

				Arrays.fill ( slcOBAvailability, false );

				// determine the set of alternatives from which the sample of alternatives will be drawn
			    if (preSampleSize > 0) {

					// select the first preSampleSize alternatives to be available for being in the sample of alternatives
					k = 0;
					preSampleCount = 0;
					while ( preSampleCount < preSampleSize ) {
				
						// set destination choice alternative availability to true if size > 0 for the segment.
						preSampleAlt = (int) (slcOBAvailability.length * SeededRandom.getRandom());
						if ( stopSize[processorIndex][0][tourType][preSampleAlt] > 0.0 ) {
							slcOBAvailability[preSampleAlt] = true;
							preSampleCount++;
						}
					
						k++;
						if (k >= slcOBAvailability.length) {
							logger.fatal( slcOBAvailability.length + " mandatory slc OB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
							System.exit(-1);
						}

					}
					
			    }
			    else {

					for (k=0; k < slcOBAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						if ( stopSize[processorIndex][0][tourType][k] > 0.0 ) {
							slcOBAvailability[k] = true;
						}
					}

			    }
				

				
				slcSoa[tourTypeIndex][0][soaIndex].applySampleOfAlternativesChoiceModel ( hh, slcOBAvailability );
				soaSample[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleOfAlternatives();
				soaCorrections[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleCorrectionFactors();

				Arrays.fill(slcSample[0], 0);
				Arrays.fill(slcCorrections[processorIndex][0], 0.0f);
				Arrays.fill ( slcOBAvailability, false );
				for (int j=1; j < soaSample[0].length; j++) {
					slcSample[0][soaSample[0][j]] = 1;
					slcCorrections[processorIndex][0][soaSample[0][j]] = soaCorrections[0][j];
					slcOBAvailability[soaSample[0][j]] = true;
				}

				soaTime += (System.currentTimeMillis()-markTime);

			}
			else {

				Arrays.fill ( slcOBAvailability, true );
				Arrays.fill(slcSample[0], 1);
				Arrays.fill(slcCorrections[processorIndex][0], 0.0f);

				for (k=0; k < slcOBAvailability.length; k++) {
					// set destination choice alternative availability to true if size > 0 for the segment
				    // and the subzone has short walk access.
					if ( stopSize[processorIndex][0][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
						slcOBAvailability[k] = false;
						slcSample[0][k] = 0;
					}
				}

				soaTime += (System.currentTimeMillis()-markTime);

			}



			markTime = System.currentTimeMillis();

			// calculate stop location choice logsum for outbound halftours for the hh
			slc[0][autoTransit][tourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
			slcLogsum[processorIndex][0] = (float)(slc[0][autoTransit][tourType].getLogsum () - ( autoTransit == 0 ? MathUtil.log( slcSoa[tourTypeIndex][0][soaIndex].getSlcLogsumCorrectionFactor() ) : 0.0 ) );
					
			logsumTime += (System.currentTimeMillis()-markTime);



			// compute stop frequency choice proportions and choose alternative
			markTime = System.currentTimeMillis();

			sfc.computeUtilities ( hh, index, sfcAvailability, sfcSample );
			int chosenAlt = sfc.getChoiceResult();

			// set the chosen value in tour objects
			hh.mandatoryTours[t].setStopFreqAlt (chosenAlt);

			freqTime += (System.currentTimeMillis()-markTime);




			markTime = System.currentTimeMillis();

			// if chosen stop frequency is 3 or 4, calculate inbound soa and availabilities	
			if ( chosenAlt == 3 || chosenAlt == 4 ) {

				// get the stop location choice sample of alternatives for outbound sov, hov tours
				if ( autoTransit == 0 ) {
	
					Arrays.fill ( slcIBAvailability, false );
			
					// determine the set of alternatives from which the sample of alternatives will be drawn
					if (preSampleSize > 0) {

						// select the first preSampleSize alternatives to be available for being in the sample of alternatives
						k = 0;
						preSampleCount = 0;
						while ( preSampleCount < preSampleSize ) {
				
							// set destination choice alternative availability to true if size > 0 for the segment.
							preSampleAlt = (int) (slcIBAvailability.length * SeededRandom.getRandom());
							if ( stopSize[processorIndex][1][tourType][preSampleAlt] > 0.0 ) {
								slcIBAvailability[preSampleAlt] = true;
								preSampleCount++;
							}
					
							k++;
							if (k >= slcIBAvailability.length) {
								logger.fatal( slcIBAvailability.length + " mandatory slc IB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
								System.exit(-1);
							}

						}
					
					}
					else {

						for (k=0; k < slcIBAvailability.length; k++) {
							// set destination choice alternative availability to true if size > 0 for the segment.
							if ( stopSize[processorIndex][1][tourType][k] > 0.0 ) {
								slcIBAvailability[k] = true;
							}
						}

					}
				

				
					slcSoa[tourTypeIndex][1][soaIndex].applySampleOfAlternativesChoiceModel ( hh, slcIBAvailability );
					soaSample[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleOfAlternatives();
					soaCorrections[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleCorrectionFactors();

					Arrays.fill(slcSample[1], 0);
					Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
					Arrays.fill ( slcIBAvailability, false );
					for (int j=1; j < soaSample[1].length; j++) {
						slcSample[1][soaSample[1][j]] = 1;
						slcCorrections[processorIndex][1][soaSample[1][j]] = soaCorrections[1][j];
						slcIBAvailability[soaSample[1][j]] = true;
					}

					soaTime += (System.currentTimeMillis()-markTime);

				}
				else {

					Arrays.fill(slcSample[1], 1);
					Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
					Arrays.fill ( slcIBAvailability, true );
			
					for (k=0; k < slcIBAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						if ( stopSize[processorIndex][1][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
							slcIBAvailability[k] = false;
							slcSample[1][k] = 0;
						}
					}

					soaTime += (System.currentTimeMillis()-markTime);

				}


			}

			
			
			int chosen = 0;
			int chosenDestAlt = 0;
			int chosenShrtWlk = 0;


			
			markTime = System.currentTimeMillis();

			// determine the stop locations if the tour has stops	
			switch (chosenAlt) {
				
				// no stops for this tour
				case 1:

					hh.mandatoryTours[t].setStopLocOB ( 0 );
					hh.mandatoryTours[t].setStopLocIB ( 0 );

					break;
					
				// 1 outbound, 0 inbound
				case 2:

					// compute destination choice proportions and choose alternative
					slc[0][autoTransit][tourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
					if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[0][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no outbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
						hh.mandatoryTours[t].setStopFreqAlt ( 1 );
						hh.mandatoryTours[t].setStopLocOB ( 0 );
						hh.mandatoryTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}



					// set the chosen value in hh tour objects
					hh.mandatoryTours[t].setStopLocOB (chosenDestAlt);
					hh.mandatoryTours[t].setStopLocSubzoneOB (chosenShrtWlk);
					hh.mandatoryTours[t].setStopLocIB ( 0 );
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				// 0 outbound, 1 inbound
				case 3:

					// compute destination choice proportions and choose alternative
					slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
					if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[1][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no inbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
						hh.mandatoryTours[t].setStopFreqAlt ( 1 );
						hh.mandatoryTours[t].setStopLocOB ( 0 );
						hh.mandatoryTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}


					// set the chosen value in hh tour objects
					hh.mandatoryTours[t].setStopLocIB (chosenDestAlt);
					hh.mandatoryTours[t].setStopLocSubzoneIB (chosenShrtWlk);
					hh.mandatoryTours[t].setStopLocOB ( 0 );
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				// 1 outbound, 1 inbound
				case 4:

					// compute destination choice proportions and choose alternative
					slc[0][autoTransit][tourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
					if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[0][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no outbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
						hh.mandatoryTours[t].setStopFreqAlt ( 1 );
						hh.mandatoryTours[t].setStopLocOB ( 0 );
						hh.mandatoryTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}
	

					// set the chosen value in hh tour objects
					hh.mandatoryTours[t].setStopLocOB (chosenDestAlt);
					hh.mandatoryTours[t].setStopLocSubzoneOB (chosenShrtWlk);
	

					
					slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
					if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[1][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no inbound mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t + ".  Stop frequency for this tour set to 0.");
						hh.mandatoryTours[t].setStopFreqAlt ( 1 );
						hh.mandatoryTours[t].setStopLocOB ( 0 );
						hh.mandatoryTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}
	

					// set the chosen value in hh tour objects
					hh.mandatoryTours[t].setStopLocIB (chosenDestAlt);
					hh.mandatoryTours[t].setStopLocSubzoneIB (chosenShrtWlk);
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				default:

					logger.error ("invalid mandatory stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in non-mandatory stop frequency choice." );
//					hh.writeContentToLogger(logger);
					
					break;
			}

		}
		
	}




	public void mandatoryTourSmc ( Household hh ) {

		
		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ( "Stop Mode Choice for Mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		


		// get the array of mandatory tours for this household.	
		if (hh.getMandatoryTours() == null)
			return;
			


		// loop over individual mandatory tours for the hh
		for (int t=0; t < hh.mandatoryTours.length; t++) {

			// if the primary mode for this mandatory tour is non-motorized or school bus, skip stop mode choice 
            if (hh.mandatoryTours[t].getMode() == TourModeType.NM ||
                hh.mandatoryTours[t].getMode() == TourModeType.SB) {
                        return;
                }

                
	        //set hh attributes for using hh to get @dmuVariables for trip mode choice determination
	        hh.setTourCategory(TourType.MANDATORY_CATEGORY);
	        hh.setTourID(t);

            setTripModes( hh, hh.mandatoryTours[t] );

		}
		
	}




	public void jointTourSfcSlc ( Household hh ) {

		int	soaIndex=0;
		int tourTypeIndex=0;
		long markTime=0;

		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Stop frequency and location Choice for Joint Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;

		// get the array of joint tours for this household.	
		if (hh.getJointTours() == null)
			return;
			

		// get person array for this household.
//		Person[] persons = hh.getPersonArray();


		hh.setTourCategory( TourType.JOINT_CATEGORY );
		hh_taz_id = hh.getTazID();

        index.setHHIndex( hh_id );
        index.setZoneIndex( hh_taz_id );
        
		// loop over joint tours for the hh
		for (int t=0; t < hh.jointTours.length; t++) {

			hh.setOrigTaz ( hh.jointTours[t].getOrigTaz() );
			hh.setChosenDest ( hh.jointTours[t].getDestTaz() );
			index.setOriginZone( hh.jointTours[t].getOrigTaz() );
			index.setDestZone( hh.jointTours[t].getDestTaz() );

			// if the primary mode for this joint tour is non-motorized, skip stop freq choice 
			if (hh.jointTours[t].getMode() == TourModeType.NM) {
				
				//Wu added for FTA restart
				hh.jointTours[t].setStopFreqAlt(0);
				hh.jointTours[t].setStopLocOB(0);
				hh.jointTours[t].setStopLocIB(0);

					continue;
			}

        
			tourType = hh.jointTours[t].getTourType();
			
			// set the array of sample of alternatives objects index
			for (int i=0; i < TourType.JOINT_TYPES.length; i++) {
				if (tourType == TourType.JOINT_TYPES[i])
					tourTypeIndex = i;
			} 


			person = hh.jointTours[t].getTourPerson();

			hh.setOrigTaz (hh_taz_id);
			hh.setPersonID ( person );
			hh.setTourID ( t );

//			int orig = hh_taz_id;
			int chosenMode = hh.jointTours[t].getMode();
//			int chosenDest = hh.jointTours[t].getDestTaz();
//			int chosenShrtWlkSeg = hh.jointTours[t].getDestShrtWlk();
			int autoTransit = hh.jointTours[t].getModeIsAutoMode() == 1 ? 0 : 1;


			// get the destination choice sample of alternatives
			markTime = System.currentTimeMillis();

			if ( autoTransit == 0 ) {

				Arrays.fill ( slcIBAvailability, false );

				// determine the set of alternatives from which the sample of alternatives will be drawn
			    if (preSampleSize > 0) {

					// select the first preSampleSize alternatives to be available for being in the sample of alternatives
					k = 0;
					preSampleCount = 0;
					while ( preSampleCount < preSampleSize ) {
				
						// set destination choice alternative availability to true if size > 0 for the segment.
						preSampleAlt = (int) (slcIBAvailability.length * SeededRandom.getRandom());
						if ( stopSize[processorIndex][1][tourType][preSampleAlt] > 0.0 ) {
							slcIBAvailability[preSampleAlt] = true;
							preSampleCount++;
						}
					
						k++;
						if (k >= slcIBAvailability.length) {
							logger.fatal( slcIBAvailability.length + " joint slc IB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
							System.exit(-1);
						}

					}
					
			    }
			    else {

					for (k=0; k < slcIBAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						if ( stopSize[processorIndex][1][tourType][k] > 0.0 ) {
							slcIBAvailability[k] = true;
						}
					}

			    }

				slcSoa[tourTypeIndex][1][soaIndex].applySampleOfAlternativesChoiceModel ( hh, slcIBAvailability );
				soaSample[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleOfAlternatives();
				soaCorrections[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleCorrectionFactors();

				Arrays.fill(slcSample[1], 0);
				Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
				Arrays.fill(slcIBAvailability, false);
				for (int i=1; i < soaSample[1].length; i++) {
					slcSample[1][soaSample[1][i]] = 1;
					slcCorrections[processorIndex][1][soaSample[1][i]] = soaCorrections[1][i];
					slcIBAvailability[soaSample[1][i]] = true;
				}

				soaTime += (System.currentTimeMillis()-markTime);

			}
			else {

				Arrays.fill(slcSample[1], 1);
				Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
				Arrays.fill (slcIBAvailability, true);
		
				for (k=0; k < slcIBAvailability.length; k++) {
					// set destination choice alternative availability to true if size > 0 for the segment.
					if ( stopSize[processorIndex][1][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
						slcSample[1][k] = 0;
						slcIBAvailability[k] = false;
					}
				}

				soaTime += (System.currentTimeMillis()-markTime);

			}




			// calculate inbound stop location choice logsum
			markTime = System.currentTimeMillis();

			// calculate stop location choice logsum for outbound halftours for the hh
			slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
			slcLogsum[processorIndex][1] = (float)(slc[1][autoTransit][tourType].getLogsum () - ( autoTransit == 0 ? MathUtil.log( slcSoa[tourTypeIndex][1][soaIndex].getSlcLogsumCorrectionFactor() ) : 0.0 ) );
					
			logsumTime += (System.currentTimeMillis()-markTime);



			// compute stop frequency choice proportions and choose alternative
			markTime = System.currentTimeMillis();
			sfc.computeUtilities ( hh, index, sfcAvailability, sfcSample );
			int chosenAlt = sfc.getChoiceResult();

			// set the chosen value in hh tour objects
			hh.jointTours[t].setStopFreqAlt (chosenAlt);
			
			freqTime += (System.currentTimeMillis()-markTime);

			
			markTime = System.currentTimeMillis();

			// if chosen stop frequency is 2 or 4, calculate outbound soa and availabilities	
			if ( chosenAlt == 2 || chosenAlt == 4 ) {

				// get the stop location choice sample of alternatives for outbound sov, hov tours
				if (chosenMode == 1 || chosenMode == 2) {

					Arrays.fill ( slcOBAvailability, false );
			
					// determine the set of alternatives from which the sample of alternatives will be drawn
					if (preSampleSize > 0) {

						// select the first preSampleSize alternatives to be available for being in the sample of alternatives
						k = 0;
						preSampleCount = 0;
						while ( preSampleCount < preSampleSize ) {
				
							// set destination choice alternative availability to true if size > 0 for the segment.
							preSampleAlt = (int) (slcOBAvailability.length * SeededRandom.getRandom());
							if ( stopSize[processorIndex][0][tourType][preSampleAlt] > 0.0 ) {
								slcOBAvailability[preSampleAlt] = true;
								preSampleCount++;
							}
					
							k++;
							if (k >= slcOBAvailability.length) {
								logger.fatal( slcOBAvailability.length + " joint slc OB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
								System.exit(-1);
							}

						}
					
					}
					else {

						for (k=0; k < slcOBAvailability.length; k++) {
							// set destination choice alternative availability to true if size > 0 for the segment.
							if ( stopSize[processorIndex][0][tourType][k] > 0.0 ) {
								slcOBAvailability[k] = true;
							}
						}

					}
				

				
					slcSoa[tourTypeIndex][0][soaIndex].applySampleOfAlternativesChoiceModel ( hh, slcOBAvailability );
					soaSample[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleOfAlternatives();
					soaCorrections[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleCorrectionFactors();

					Arrays.fill(slcSample[0], 0);
					Arrays.fill(slcCorrections[processorIndex][0], 0.0f);
					Arrays.fill (slcOBAvailability, false);
					for (int j=1; j < soaSample[0].length; j++) {
						slcSample[0][soaSample[0][j]] = 1;
						slcCorrections[processorIndex][0][soaSample[0][j]] = soaCorrections[0][j];
						slcOBAvailability[soaSample[0][j]] = true;
					}

					soaTime += (System.currentTimeMillis()-markTime);

				}
				else {

					Arrays.fill(slcSample[0], 1);
					Arrays.fill(slcCorrections[processorIndex][0], 0.0f);
					Arrays.fill (slcOBAvailability, true);
			
					for (k=0; k < slcOBAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						if ( stopSize[processorIndex][0][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
							slcSample[0][k] = 0;
							slcOBAvailability[k] = false;
						}
					}

					soaTime += (System.currentTimeMillis()-markTime);

				}

			}

			
			
			int chosen = 0;
			int chosenDestAlt = 0;
			int chosenShrtWlk = 0;


		
			// use distance UEC to get utilities(distances) for each tour and trip segment
			// determine the stop locations if the tour has stops	
			switch (chosenAlt) {
				
				// no stops for this tour
				case 1:

					hh.jointTours[t].setStopLocOB ( 0 );
					hh.jointTours[t].setStopLocIB ( 0 );

					break;
					
				// 1 outbound, 0 inbound
				case 2:

					// compute destination choice proportions and choose alternative
					markTime = System.currentTimeMillis();
					slc[0][autoTransit][tourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
					if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[0][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no outbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.jointTours[t].setStopFreqAlt ( 1 );
						hh.jointTours[t].setStopLocOB ( 0 );
						hh.jointTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}
	
	
	
					// set the chosen value in hh tour objects
					hh.jointTours[t].setStopLocOB (chosenDestAlt);
					hh.jointTours[t].setStopLocSubzoneOB (chosenShrtWlk);
					hh.jointTours[t].setStopLocIB (0);
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				// 0 outbound, 1 inbound
				case 3:

					// compute destination choice proportions and choose alternative
					markTime = System.currentTimeMillis();
					slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
					if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[1][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no inbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.jointTours[t].setStopFreqAlt ( 1 );
						hh.jointTours[t].setStopLocOB ( 0 );
						hh.jointTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}
		


					// set the chosen value in hh tour objects
					hh.jointTours[t].setStopLocIB (chosenDestAlt);
					hh.jointTours[t].setStopLocSubzoneIB (chosenShrtWlk);
					hh.jointTours[t].setStopLocOB (0);
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				// 1 outbound, 1 inbound
				case 4:

					// compute destination choice proportions and choose alternative
					markTime = System.currentTimeMillis();
					slc[0][autoTransit][tourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
					if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[0][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no outbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.jointTours[t].setStopFreqAlt ( 1 );
						hh.jointTours[t].setStopLocOB ( 0 );
						hh.jointTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}
	

	
					// set the chosen value in hh tour objects
					hh.jointTours[t].setStopLocOB (chosenDestAlt);
					hh.jointTours[t].setStopLocSubzoneOB (chosenShrtWlk);
					locTime += (System.currentTimeMillis() - markTime);
	


					markTime = System.currentTimeMillis();
					slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
					if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[1][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no inbound joint slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.jointTours[t].setStopFreqAlt ( 1 );
						hh.jointTours[t].setStopLocOB ( 0 );
						hh.jointTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}
	

		
					// set the chosen value in hh tour objects
					hh.jointTours[t].setStopLocIB (chosenDestAlt);
					hh.jointTours[t].setStopLocSubzoneIB (chosenShrtWlk);
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				default:

					logger.error ("invalid joint stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in joint stop frequency choice." );
//					hh.writeContentToLogger(logger);
					
					break;
			}


		}

	}




	public void jointTourSmc ( Household hh ) {

	    hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Stop Mode Choice for Joint Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;

		// get the array of joint tours for this household.	
		if (hh.getJointTours() == null)
			return;

		
		// loop over joint tours for the hh
		// loop over individual joint tours for the hh
		for (int t=0; t < hh.jointTours.length; t++) {

			// if the primary mode for this joint tour is non-motorized, skip stop mode choice 
			if (hh.jointTours[t].getMode() == TourModeType.NM) {
					continue;
			}

            //set hh attributes for using hh to get @dmuVariables for trip mode choice determination
            hh.setTourCategory(TourType.JOINT_CATEGORY);
            hh.setTourID(t);

            setTripModes( hh, hh.jointTours[t] );

		}

	}




	public void nonMandatoryTourSfcSlc ( Household hh ) {

		int	soaIndex=0;
		int tourTypeIndex=0;
		long markTime=0;


		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Stop frequency and location Choice for non-mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		


		// get the array of indiv tours for this household.	
		if (hh.getIndivTours() == null)
			return;
			
			
		// get person array for this household.
//		Person[] persons = hh.getPersonArray();


		hh.setTourCategory( TourType.NON_MANDATORY_CATEGORY );
		hh_taz_id = hh.getTazID();


        index.setHHIndex( hh_id );
        index.setZoneIndex( hh_taz_id );
        
		// loop over individual tours for the hh
		for (int t=0; t < hh.indivTours.length; t++) {

			hh.setOrigTaz ( hh.indivTours[t].getOrigTaz() );
			hh.setChosenDest ( hh.indivTours[t].getDestTaz() );
			index.setOriginZone( hh.indivTours[t].getOrigTaz() );
			index.setDestZone( hh.indivTours[t].getDestTaz() );

			// if the primary mode for this indiv non-mandatory tour is non-motorized, skip stop freq choice 
			if (hh.indivTours[t].getMode() == TourModeType.NM) {
				
				//Wu added for FTA restart
				hh.indivTours[t].setStopFreqAlt(0);
				hh.indivTours[t].setStopLocOB(0);
				hh.indivTours[t].setStopLocIB(0);

				continue;
			}

        
			tourType = hh.indivTours[t].getTourType();
			
			// set the array of sample of alternatives objects index
			for (int i=0; i < TourType.NON_MANDATORY_TYPES.length; i++)
				if (tourType == TourType.NON_MANDATORY_TYPES[i]) {
					tourTypeIndex = i;
				} 


			person = hh.indivTours[t].getTourPerson();

			hh.indivTours[t].setOrigTaz (hh_taz_id);
			hh.setPersonID ( person );
			hh.setTourID ( t );

//			int orig = hh_taz_id;
			int chosenMode = hh.indivTours[t].getMode();
//			int chosenDest = hh.indivTours[t].getDestTaz();
//			int chosenShrtWlkSeg = hh.indivTours[t].getDestShrtWlk();
			int autoTransit = hh.indivTours[t].getModeIsAutoMode() == 1 ? 0 : 1;



			markTime = System.currentTimeMillis();


			// get the destination choice sample of alternatives
			if ( autoTransit == 0 ) {

				Arrays.fill ( slcIBAvailability, false );

				// determine the set of alternatives from which the sample of alternatives will be drawn
			    if (preSampleSize > 0) {

					// select the first preSampleSize alternatives to be available for being in the sample of alternatives
					k = 0;
					preSampleCount = 0;
					while ( preSampleCount < preSampleSize ) {
				
						// set destination choice alternative availability to true if size > 0 for the segment.
						preSampleAlt = (int) (slcIBAvailability.length * SeededRandom.getRandom());
						if ( stopSize[processorIndex][1][tourType][preSampleAlt] > 0.0 ) {
							slcIBAvailability[preSampleAlt] = true;
							preSampleCount++;
						}
					
						k++;
						if (k >= slcIBAvailability.length) {
							logger.fatal( slcIBAvailability.length + " indiv. non-mandatory slc IB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
							System.exit(-1);
						}

					}
					
			    }
			    else {

					for (k=0; k < slcIBAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						if ( stopSize[processorIndex][1][tourType][k] > 0.0 ) {
							slcIBAvailability[k] = true;
						}
					}

			    }

				slcSoa[tourTypeIndex][1][soaIndex].applySampleOfAlternativesChoiceModel ( hh );
				soaSample[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleOfAlternatives();
				soaCorrections[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleCorrectionFactors();
    
				Arrays.fill(slcSample[1], 0);
				Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
				Arrays.fill(slcIBAvailability, false);
				for (int i=1; i < soaSample[1].length; i++) {
					slcSample[1][soaSample[1][i]] = 1;
					slcCorrections[processorIndex][1][soaSample[1][i]] = soaCorrections[1][i];
					slcIBAvailability[soaSample[1][i]] = true;
				}

				soaTime += (System.currentTimeMillis()-markTime);

			}
			else {

				Arrays.fill(slcSample[1], 1);
				Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
				Arrays.fill (slcIBAvailability, true);
		
				for (k=0; k < slcIBAvailability.length; k++) {
					// set destination choice alternative availability to true if size > 0 for the segment.
					if ( stopSize[processorIndex][1][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
						slcSample[1][k] = 0;
						slcIBAvailability[k] = false;
					}
				}

				soaTime += (System.currentTimeMillis()-markTime);

			}



			// compute stop location choice logsum for inbound halftours for the hh
			// calculate inbound stop location choice logsum
			slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
			slcLogsum[processorIndex][1] = (float)(slc[1][autoTransit][tourType].getLogsum () - ( autoTransit == 0 ? MathUtil.log( slcSoa[tourTypeIndex][1][soaIndex].getSlcLogsumCorrectionFactor() ) : 0.0 ) );

			logsumTime += (System.currentTimeMillis()-markTime);



			// compute stop frequency choice proportions and choose alternative
			markTime = System.currentTimeMillis();

			sfc.computeUtilities ( hh, index, sfcAvailability, sfcSample );
			
			int chosenAlt = sfc.getChoiceResult();

			// set the chosen value in hh tour objects
			hh.indivTours[t].setStopFreqAlt (chosenAlt);

			freqTime += (System.currentTimeMillis()-markTime);
			
			

			markTime = System.currentTimeMillis();

			// if chosen stop frequency is 2 or 4, calculate outbound soa and availabilities	
			if ( chosenAlt == 2 || chosenAlt == 4 ) {

				// get the stop location choice sample of alternatives for outbound sov, hov tours
				if (chosenMode == 1 || chosenMode == 2) {

					Arrays.fill ( slcOBAvailability, false );
			
					// determine the set of alternatives from which the sample of alternatives will be drawn
					if (preSampleSize > 0) {

						// select the first preSampleSize alternatives to be available for being in the sample of alternatives
						k = 0;
						preSampleCount = 0;
						while ( preSampleCount < preSampleSize ) {
				
							// set destination choice alternative availability to true if size > 0 for the segment.
							preSampleAlt = (int) (slcOBAvailability.length * SeededRandom.getRandom());
							if ( stopSize[processorIndex][0][tourType][preSampleAlt] > 0.0 ) {
								slcOBAvailability[preSampleAlt] = true;
								preSampleCount++;
							}
					
							k++;
							if (k >= slcOBAvailability.length) {
								logger.fatal( slcOBAvailability.length + " indiv. non-mandatory slc OB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
								System.exit(-1);
							}

						}
					
					}
					else {

						for (k=0; k < slcOBAvailability.length; k++) {
							// set destination choice alternative availability to true if size > 0 for the segment.
							if ( stopSize[processorIndex][0][tourType][k] > 0.0 ) {
								slcOBAvailability[k] = true;
							}
						}

					}
				

					slcSoa[tourTypeIndex][0][soaIndex].applySampleOfAlternativesChoiceModel ( hh, slcOBAvailability );
					soaSample[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleOfAlternatives();
					soaCorrections[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleCorrectionFactors();

					Arrays.fill(slcSample[0], 0);
					Arrays.fill(slcCorrections[processorIndex][0], 0.0f);
					Arrays.fill(slcOBAvailability, false);
					for (int j=1; j < soaSample[0].length; j++) {
						slcSample[0][soaSample[0][j]] = 1;
						slcCorrections[processorIndex][0][soaSample[0][j]] = soaCorrections[0][j];
						slcOBAvailability[soaSample[0][j]] = true;
					}

					soaTime += (System.currentTimeMillis()-markTime);

				}
				else {

					Arrays.fill(slcSample[0], 1);
					Arrays.fill(slcCorrections[processorIndex][0], 0.0f);
					Arrays.fill(slcOBAvailability, true);
			
					for (k=0; k < slcOBAvailability.length; k++) {
						// set destination choice alternative availability to true if size > 0 for the segment.
						if ( stopSize[processorIndex][0][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
							slcOBAvailability[k] = false;
							slcSample[0][k] = 0;
						}
					}

					soaTime += (System.currentTimeMillis()-markTime);

				}

			}

			
			
			int chosen = 0;
			int chosenDestAlt = 0;
			int chosenShrtWlk = 0;

		
			// use distance UEC to get utilities(distances) for each tour and trip segment

			// determine the stop locations if the tour has stops	
			switch (chosenAlt) {
				
				// no stops for this tour
				case 1:

					hh.indivTours[t].setStopLocOB ( 0 );
					hh.indivTours[t].setStopLocIB ( 0 );

					break;
					
				// 1 outbound, 0 inbound
				case 2:

					// compute destination choice proportions and choose alternative
					markTime = System.currentTimeMillis();
					slc[0][autoTransit][tourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
					if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[0][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						chosen = 1;
						chosenDestAlt = 1;
						chosenShrtWlk = 1;
						logger.warn ( "no outbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.indivTours[t].setStopFreqAlt ( 1 );
						hh.indivTours[t].setStopLocOB ( 0 );
						hh.indivTours[t].setStopLocIB ( 0 );
//						hh.writeContentToLogger(logger);
						break;
					}
	


					// set the chosen value in hh tour objects
					hh.indivTours[t].setStopLocOB (chosenDestAlt);
					hh.indivTours[t].setStopLocSubzoneOB (chosenShrtWlk);
					hh.indivTours[t].setStopLocIB (0);
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				// 0 outbound, 1 inbound
				case 3:

					// compute destination choice proportions and choose alternative
					markTime = System.currentTimeMillis();
					slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
					if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[1][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no inbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.indivTours[t].setStopFreqAlt ( 1 );
						hh.indivTours[t].setStopLocOB ( 0 );
						hh.indivTours[t].setStopLocIB ( 0 );
						break;
					}


		
					// set the chosen value in hh tour objects
					hh.indivTours[t].setStopLocIB (chosenDestAlt);
					hh.indivTours[t].setStopLocSubzoneIB (chosenShrtWlk);
					hh.indivTours[t].setStopLocOB (0);
					locTime += (System.currentTimeMillis() - markTime);

					break;
					
				// 1 outbound, 1 inbound
				case 4:

					// compute destination choice proportions and choose alternative
					markTime = System.currentTimeMillis();
					slc[0][autoTransit][tourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
					if ( slc[0][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[0][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no outbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.indivTours[t].setStopFreqAlt ( 1 );
						hh.indivTours[t].setStopLocOB ( 0 );
						hh.indivTours[t].setStopLocIB ( 0 );
						break;
					}
	

	
					// set the chosen value in hh tour objects
					hh.indivTours[t].setStopLocOB (chosenDestAlt);
					hh.indivTours[t].setStopLocSubzoneOB (chosenShrtWlk);
					locTime += (System.currentTimeMillis() - markTime);
	


					markTime = System.currentTimeMillis();
					slc[1][autoTransit][tourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
					if ( slc[1][autoTransit][tourType].getAvailabilityCount() > 0 ) {
						chosen = slc[1][autoTransit][tourType].getChoiceResult();
						chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
						chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
					}
					else {
						logger.warn ( "no inbound non-mandatory slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
						hh.indivTours[t].setStopFreqAlt ( 1 );
						hh.indivTours[t].setStopLocOB ( 0 );
						hh.indivTours[t].setStopLocIB ( 0 );
						break;
					}
	

		
					// set the chosen value in hh tour objects
					hh.indivTours[t].setStopLocIB (chosenDestAlt);
					hh.indivTours[t].setStopLocSubzoneIB (chosenShrtWlk);
					locTime += (System.currentTimeMillis() - markTime);

					break;

				default:

				    logger.error ("invalid individual non-mandatory stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in non-mandatory stop frequency model");
//					hh.writeContentToLogger(logger);
					
					break;
			}


		}

	}




	public void nonMandatoryTourSmc ( Household hh ) {

		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Stop Mode Choice for non-mandatory Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		


		// get the array of indiv tours for this household.	
		if (hh.getIndivTours() == null)
			return;
			
			
		// loop over indiv tours for the hh
		// loop over individual indiv tours for the hh
		for (int t=0; t < hh.indivTours.length; t++) {

			// if the primary mode for this indiv non-mandatory tour is non-motorized, skip trip mode choice 
			if (hh.indivTours[t].getMode() == TourModeType.NM ) {
					continue;
			}

			
            //set hh attributes for using hh to get @dmuVariables for trip mode choice determination
            hh.setTourCategory(TourType.NON_MANDATORY_CATEGORY);
            hh.setTourID(t);

            setTripModes( hh, hh.indivTours[t] );

		}

	}




	public void atWorkTourSfcSlc ( Household hh ) {

		int	soaIndex=0;
		long markTime=0;


//		Tour[] st;
//		int todAlt;
//		int startP;
//		int endP;


		int tourTypeIndex=0;
		
		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Stop frequency and location Choice for At-work Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		


		// get the array of mandatory tours for this household.	
		if (hh.getMandatoryTours() == null)
			return;
		

		// get person array for this household.
//		Person[] persons = hh.getPersonArray();


		hh.setTourCategory( TourType.AT_WORK_CATEGORY );
		hh_taz_id = hh.getTazID();

        
        index.setHHIndex( hh_id );
        index.setZoneIndex( hh_taz_id );
        
		// loop over individual tours of the tour purpose of interest for the hh
		for (int t=0; t < hh.mandatoryTours.length; t++) {

			// get the array of subtours for this work tour
			if (hh.mandatoryTours[t].getSubTours() == null)
				continue;


			tourType = TourType.ATWORK;
			tourTypeIndex = 0;



			person = hh.mandatoryTours[t].getTourPerson();

			hh.setPersonID ( person );
			hh.setTourID ( t );


			// loop over subtours
			for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

				// the origin for the at-work tour is the destination of the primary work tour
				hh.setSubtourID ( s );
				hh.setOrigTaz ( hh.mandatoryTours[t].subTours[s].getOrigTaz() );
				hh.setChosenDest( hh.mandatoryTours[t].subTours[s].getDestTaz() );
				index.setOriginZone( hh.mandatoryTours[t].subTours[s].getOrigTaz() );
				index.setDestZone( hh.mandatoryTours[t].subTours[s].getDestTaz() );

				// if the primary mode for this at-work subtour is non-motorized, skip stop frequency choice 
				if (hh.mandatoryTours[t].subTours[s].getMode() == TourModeType.NM) {
					//Wu added for FTA restart
					hh.mandatoryTours[t].subTours[s].setStopFreqAlt(0);
					hh.mandatoryTours[t].subTours[s].setStopLocOB(0);
					hh.mandatoryTours[t].subTours[s].setStopLocIB(0);
					
					continue;
				}
			

//				int orig = hh.mandatoryTours[t].subTours[s].getOrigTaz();
				int chosenMode = hh.mandatoryTours[t].subTours[s].getMode();
//				int chosenDest = hh.mandatoryTours[t].subTours[s].getDestTaz();
//				int chosenShrtWlkSeg = hh.mandatoryTours[t].subTours[s].getDestShrtWlk();
				int autoTransit = hh.mandatoryTours[t].subTours[s].getModeIsAutoMode() == 1 ? 0 : 1;

        
				int subtourType = hh.mandatoryTours[t].subTours[s].getSubTourType();
				


				if (hh.mandatoryTours[t].subTours[s].getSubTourType() == SubTourType.WORK)
					soaIndex = 0;
				else if (hh.mandatoryTours[t].subTours[s].getSubTourType() == SubTourType.EAT)
					soaIndex = 1;
				else if (hh.mandatoryTours[t].subTours[s].getSubTourType() == SubTourType.OTHER)
					soaIndex = 2;



				// compute stop frequency choice proportions and choose alternative
				markTime = System.currentTimeMillis();
				sfc.computeUtilities ( hh, index, sfcAvailability, sfcSample );
				int chosenAlt = sfc.getChoiceResult();
				freqTime += (System.currentTimeMillis()-markTime);

				if (chosenAlt == 0) {
					logger.error ("at-work stop frequency choice == 0 household id=" + hh.getID() );
//				    hh.writeContentToLogger(logger);
				}
			
				// set the chosen value in hh tour objects
				hh.mandatoryTours[t].subTours[s].setStopFreqAlt (chosenAlt);
			



				// if chosen stop frequency is 3 or 4, calculate inbound soa and availabilities	
				if ( chosenAlt == 3 || chosenAlt == 4 ) {

					markTime = System.currentTimeMillis();

					// get the stop location choice sample of alternatives for outbound sov, hov tours
					if ( autoTransit == 0 ) {

						Arrays.fill ( slcIBAvailability, false );
				
						// determine the set of alternatives from which the sample of alternatives will be drawn
						if (preSampleSize > 0) {
	
							// select the first preSampleSize alternatives to be available for being in the sample of alternatives
							k = 0;
							preSampleCount = 0;
							while ( preSampleCount < preSampleSize ) {
					
								// set destination choice alternative availability to true if size > 0 for the segment.
								preSampleAlt = (int) (slcIBAvailability.length * SeededRandom.getRandom());
								if ( stopSize[processorIndex][1][tourType][preSampleAlt] > 0.0 ) {
									slcIBAvailability[preSampleAlt] = true;
									preSampleCount++;
								}
						
								k++;
								if (k >= slcIBAvailability.length) {
									logger.fatal( slcIBAvailability.length + " at work slc IB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
									System.exit(-1);
								}
	
							}
						
						}
						else {
	
							for (k=0; k < slcIBAvailability.length; k++) {
								// set destination choice alternative availability to true if size > 0 for the segment.
								if ( stopSize[processorIndex][1][tourType][k] > 0.0 ) {
									slcIBAvailability[k] = true;
								}
							}
	
						}
					
	
					
						slcSoa[tourTypeIndex][1][soaIndex].applySampleOfAlternativesChoiceModel ( hh, slcIBAvailability );
						soaSample[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleOfAlternatives();
						soaCorrections[1] = slcSoa[tourTypeIndex][1][soaIndex].getSampleCorrectionFactors();
	
						Arrays.fill(slcSample[1], 0);
						Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
						Arrays.fill ( slcIBAvailability, false );
						for (int j=1; j < soaSample[1].length; j++) {
							slcSample[1][soaSample[1][j]] = 1;
							slcCorrections[processorIndex][1][soaSample[1][j]] = soaCorrections[1][j];
							slcIBAvailability[soaSample[1][j]] = true;
						}
	
						soaTime += (System.currentTimeMillis()-markTime);
	
					}
					else {
	
						Arrays.fill(slcSample[1], 1);
						Arrays.fill(slcCorrections[processorIndex][1], 0.0f);
						Arrays.fill ( slcIBAvailability, true );
				
						for (k=0; k < slcIBAvailability.length; k++) {
							// set destination choice alternative availability to true if size > 0 for the segment.
							if ( stopSize[processorIndex][1][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
								slcIBAvailability[k] = false;
								slcSample[1][k] = 0;
							}
						}
	
						soaTime += (System.currentTimeMillis()-markTime);
	
					}

				}
				if ( chosenAlt == 2 || chosenAlt == 4 ) {

					markTime = System.currentTimeMillis();

					// get the stop location choice sample of alternatives for outbound sov, hov tours
					if (chosenMode == 1 || chosenMode == 2) {

						Arrays.fill ( slcOBAvailability, false );
				
						// determine the set of alternatives from which the sample of alternatives will be drawn
						if (preSampleSize > 0) {
	
							// select the first preSampleSize alternatives to be available for being in the sample of alternatives
							k = 0;
							preSampleCount = 0;
							while ( preSampleCount < preSampleSize ) {
					
								// set destination choice alternative availability to true if size > 0 for the segment.
								preSampleAlt = (int) (slcOBAvailability.length * SeededRandom.getRandom());
								if ( stopSize[processorIndex][0][tourType][preSampleAlt] > 0.0 ) {
									slcOBAvailability[preSampleAlt] = true;
									preSampleCount++;
								}
						
								k++;
								if (k >= slcOBAvailability.length) {
									logger.fatal( slcOBAvailability.length + " at work slc OB alternatives checked, but fewer than preSampleSize=" + preSampleSize + " alternatives are available for sample of alternatives.");
									System.exit(-1);
								}
	
							}
						
						}
						else {
	
							for (k=0; k < slcOBAvailability.length; k++) {
								// set destination choice alternative availability to true if size > 0 for the segment.
								if ( stopSize[processorIndex][0][tourType][k] > 0.0 ) {
									slcOBAvailability[k] = true;
								}
							}
	
						}
				

						slcSoa[tourTypeIndex][0][soaIndex].applySampleOfAlternativesChoiceModel ( hh, slcOBAvailability );
						soaSample[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleOfAlternatives();
						soaCorrections[0] = slcSoa[tourTypeIndex][0][soaIndex].getSampleCorrectionFactors();

						Arrays.fill(slcSample[0], 0);
						Arrays.fill(slcCorrections[processorIndex][0], 0.0f);
						Arrays.fill(slcOBAvailability, false);
						for (int j=1; j < soaSample[0].length; j++) {
							slcSample[0][soaSample[0][j]] = 1;
							slcCorrections[processorIndex][0][soaSample[0][j]] = soaCorrections[0][j];
							slcOBAvailability[soaSample[0][j]] = true;
						}

						soaTime += (System.currentTimeMillis()-markTime);

					}
					else {

						Arrays.fill(slcSample[0], 1);
						Arrays.fill(slcCorrections[processorIndex][0], 0.0f);
						Arrays.fill(slcOBAvailability, true);
				
						for (k=0; k < slcOBAvailability.length; k++) {
							// set destination choice alternative availability to true if size > 0 for the segment.
							if ( stopSize[processorIndex][0][tourType][k] <= 0.0 || (k+1) % 3 == 1 ) {
								slcOBAvailability[k] = false;
								slcSample[0][k] = 0;
							}
						}
	
						soaTime += (System.currentTimeMillis()-markTime);

					}

				}

			
				int chosen = 0;
				int chosenDestAlt = 0;
				int chosenShrtWlk = 0;

		
				// use distance UEC to get utilities(distances) for each tour and trip segment

				// determine the stop locations if the tour has stops	
				switch (chosenAlt) {
				
					// no stops for this tour
					case 1:

						hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
						hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );

						index.setStopZone( 0 );

						break;
					
					// 1 outbound, 0 inbound
					case 2:

						// compute destination choice proportions and choose alternative
						markTime = System.currentTimeMillis();
						slc[0][autoTransit][subtourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
						if ( slc[0][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
							chosen = slc[0][autoTransit][subtourType].getChoiceResult();
							chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
							chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
						}
						else {
							logger.warn ( "no outbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
							hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
							hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
							hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
							break;
						}
	


						// set the chosen value in hh tour objects
						hh.mandatoryTours[t].subTours[s].setStopLocOB (chosenDestAlt);
						hh.mandatoryTours[t].subTours[s].setStopLocSubzoneOB (chosenShrtWlk);
						hh.mandatoryTours[t].subTours[s].setStopLocIB (0);
						locTime += (System.currentTimeMillis() - markTime);

						break;
					
					// 0 outbound, 1 inbound
					case 3:

						// compute destination choice proportions and choose alternative
						markTime = System.currentTimeMillis();
						slc[1][autoTransit][subtourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
						if ( slc[1][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
							chosen = slc[1][autoTransit][subtourType].getChoiceResult();
							chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
							chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
						}
						else {
							logger.warn ( "no inbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
							hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
							hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
							hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
							break;
						}


		
						// set the chosen value in hh tour objects
						hh.mandatoryTours[t].subTours[s].setStopLocIB (chosenDestAlt);
						hh.mandatoryTours[t].subTours[s].setStopLocSubzoneIB (chosenShrtWlk);
						hh.mandatoryTours[t].subTours[s].setStopLocOB (0);
						locTime += (System.currentTimeMillis() - markTime);

						break;
					
					// 1 outbound, 1 inbound
					case 4:

						// compute destination choice proportions and choose alternative
						markTime = System.currentTimeMillis();
						slc[0][autoTransit][subtourType].computeUtilities ( hh, index, slcOBAvailability, slcSample[0] );
						if ( slc[0][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
							chosen = slc[0][autoTransit][subtourType].getChoiceResult();
							chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
							chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
						}
						else {
							logger.warn ( "no outbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
							hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
							hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
							hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
							break;
						}
	

	
						// set the chosen value in hh tour objects
						hh.mandatoryTours[t].subTours[s].setStopLocOB (chosenDestAlt);
						hh.mandatoryTours[t].subTours[s].setStopLocSubzoneOB (chosenShrtWlk);
						locTime += (System.currentTimeMillis() - markTime);
	


						markTime = System.currentTimeMillis();
						slc[1][autoTransit][subtourType].computeUtilities ( hh, index, slcIBAvailability, slcSample[1] );
						if ( slc[1][autoTransit][subtourType].getAvailabilityCount() > 0 ) {
							chosen = slc[1][autoTransit][subtourType].getChoiceResult();
							chosenDestAlt = (int)((chosen-1)/ZonalDataManager.WALK_SEGMENTS) + 1;
							chosenShrtWlk = chosen - (chosenDestAlt-1)*ZonalDataManager.WALK_SEGMENTS - 1;
						}
						else {
							logger.warn ( "no inbound atwork slc alternatives available, hh_id=" + hh_id + ", person=" + person + ", tour=" + t);
							hh.mandatoryTours[t].subTours[s].setStopFreqAlt ( 1 );
							hh.mandatoryTours[t].subTours[s].setStopLocOB ( 0 );
							hh.mandatoryTours[t].subTours[s].setStopLocIB ( 0 );
							break;
						}
	

		
						// set the chosen value in hh tour objects
						hh.mandatoryTours[t].subTours[s].setStopLocIB (chosenDestAlt);
						hh.mandatoryTours[t].subTours[s].setStopLocSubzoneIB (chosenShrtWlk);
						locTime += (System.currentTimeMillis() - markTime);

						break;
					
					default:

						logger.error ("invalid at-work stop frequency choice = " + chosenAlt + " for household id=" + hh.getID() + " in at-work stop frequency choice." );
//						hh.writeContentToLogger(logger);
					
						break;
				}


			}
		
		}

	}

	
	
	
	public void atWorkTourSmc ( Household hh ) {

		hh_id     = hh.getID();
		if (useMessageWindow) mw.setMessage1 ("Stop Mode Choice for At-work Tours");
		if (useMessageWindow) mw.setMessage2 ( "household " + count + " (" + hh_id + ")" );
		count++;		


		// get the array of mandatory tours for this household.	
		if (hh.getMandatoryTours() == null)
			return;
		

		// loop over individual tours of the tour purpose of interest for the hh
		for (int t=0; t < hh.mandatoryTours.length; t++) {

			// get the array of subtours for this work tour
			if (hh.mandatoryTours[t].getSubTours() == null)
				continue;


			// loop over subtours
			for (int s=0; s < hh.mandatoryTours[t].subTours.length; s++) {

				// if the primary mode for this at-work subtour is non-motorized, skip stop mode choice 
				if (hh.mandatoryTours[t].subTours[s].getMode() == TourModeType.NM) {
					continue;
				}
			
        
	            //set hh attributes for using hh to get @dmuVariables for trip mode choice determination
	            hh.setTourCategory(TourType.AT_WORK_CATEGORY);
	            hh.setTourID(t);

	            setTripModes( hh, hh.mandatoryTours[t].subTours[s] );
					
			}
		
		}
		
	}
	
	
	
    private void setTripModes( Household hh, Tour tour ){

        long markTime=0;
        
        double[] result;

        index.setOriginZone( tour.getOrigTaz() );
        index.setDestZone( tour.getDestTaz() );

        markTime = System.currentTimeMillis();
    
        // determine the half tour segment mode choices if the tour has stops   
        switch ( tour.getStopFreqAlt() ) {
            
            // no stops for this tour
            case 1:

                break;
                
            // 1 outbound, 0 inbound
            case 2:

                if (tour.getMode() == TourModeType.SOV || tour.getMode() == TourModeType.HOV) {
                    tour.setTripIkMode ( tour.getMode() );
                    tour.setTripKjMode ( tour.getMode() );
                }
                else {                       

                    index.setStopZone( tour.getStopLocOB() );
    
                    // set outbound (ik) submode 
                    result = smcUECik.solve( index, hh, smcSample );
                    tour.setTripIkMode ( (int)result[0] );
                    
                    // set outbound (kj) submode 
                    result = smcUECkj.solve( index, hh, smcSample );
                    tour.setTripKjMode ( (int)result[0] );
                    
                }   
                
                mcTime += (System.currentTimeMillis() - markTime);

                break;
                
            // 0 outbound, 1 inbound
            case 3:

                if (tour.getMode() == TourModeType.SOV || tour.getMode() == TourModeType.HOV) {
                    tour.setTripJkMode ( tour.getMode() );
                    tour.setTripKiMode ( tour.getMode() );
                }
                else {                       

                    index.setStopZone( tour.getStopLocIB() );

                    // set inbound (jk) submode 
                    result = smcUECjk.solve( index, hh, smcSample );
                    tour.setTripJkMode ( (int)result[0] );
                    
                    // set inbound (ki) submode 
                    result = smcUECki.solve( index, hh, smcSample );
                    tour.setTripKiMode ( (int)result[0] );

                }
                
                mcTime += (System.currentTimeMillis() - markTime);

                break;
                
            // 1 outbound, 1 inbound
            case 4:

                if (tour.getMode() == TourModeType.SOV || tour.getMode() == TourModeType.HOV) {
                    tour.setTripIkMode ( tour.getMode() );
                    tour.setTripKjMode ( tour.getMode() );
                    tour.setTripJkMode ( tour.getMode() );
                    tour.setTripKiMode ( tour.getMode() );
                }
                else {                       

                    index.setStopZone( tour.getStopLocOB() );

                    // set outbound (ik) submode 
                    result = smcUECik.solve( index, hh, smcSample );
                    tour.setTripIkMode ( (int)result[0] );
                    
                    // set outbound (kj) submode 
                    result = smcUECkj.solve( index, hh, smcSample );
                    tour.setTripKjMode ( (int)result[0] );
    
    
                    
                    index.setStopZone( tour.getStopLocIB() );
    
                    // set inbound (jk) submode 
                    result = smcUECjk.solve( index, hh, smcSample );
                    tour.setTripJkMode ( (int)result[0] );
                    
                    // set inbound (ki) submode 
                    result = smcUECki.solve( index, hh, smcSample );
                    tour.setTripKiMode ( (int)result[0] );

                }
                
                mcTime += (System.currentTimeMillis() - markTime);


                break;
                
        }
        
    }
        

	
	public void resetHouseholdCount () {
		count = 0;
	}
	

	
	
	public void printTimes ( short tourTypeCategory ) {

		for (int i=1; i < 5; i++) {		
			
			if ( tourTypeCategory == i ) {

				logger.info ( "Stops Model Component Times for " + TourType.TYPE_CATEGORY_LABELS[i] + " tours:");
				logger.info ( "total seconds processing stop location choice sample of alternatives = " + (float)soaTime/1000);
				logger.info ( "total seconds processing stop location choice logsums = " + (float)logsumTime/1000);
				logger.info ( "total seconds processing stop frequency choice = " + (float)freqTime/1000);
				logger.info ( "total seconds processing stop location choice = " + (float)locTime/1000);
				logger.info ( "total seconds processing stop mode choice = " + (float)mcTime/1000);
				logger.info ( "");

			}
		}
							
	}
}
