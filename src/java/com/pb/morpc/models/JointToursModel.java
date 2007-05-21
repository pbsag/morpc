package com.pb.morpc.models;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;

import com.pb.morpc.structures.*;
import com.pb.morpc.synpop.SyntheticPopulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;


/**
 * @author Freedman
 *
 * Joint Tour Frequency Model
 */
public class JointToursModel {
    static final int M3_DATA_SHEET = 0;
    static final int M31_MODEL_SHEET = 1;
    static final int M32_MODEL_SHEET = 2;
    static final int M33_MODEL_SHEET = 3;
    static Logger logger = Logger.getLogger("com.pb.morpc.models");
    private Household[] hh;
    HashMap propertyMap;
    boolean useMessageWindow = false;
    MessageWindow mw;

    public JointToursModel(HashMap propertyMap, Household[] hh) {
        this.propertyMap = propertyMap;
        this.hh = hh;

        // get the indicator for whether to use the message window or not
        // from the properties file.
        String useMessageWindowString = (String) propertyMap.get(
                "MessageWindow");

        if (useMessageWindowString != null) {
            if (useMessageWindowString.equalsIgnoreCase("true")) {
                this.useMessageWindow = true;
                this.mw = new MessageWindow(
                        "MORPC Joint Tour Generation Models (M31-M33) Run Time Information");
            }
        }
    }

    public void runFrequencyModel() {
        PrintWriter outStream = null;
        File file = null;

        //M31.csv heading
        String[] tableHeadings31 = {
            SyntheticPopulation.HHID_FIELD, SyntheticPopulation.HHTAZID_FIELD,
            "MaxAdultOverlaps", "MaxChildOverlaps", "MaxMixedOverlaps",
            "TravelActiveAdults", "TravelActiveChildren",
            "TravelActiveNonPreschool", "M31"
        };

        //current record
        float[] tempRecord = new float[9];
        //default householdChoice value
        int tempHouseholdChoice = 1;
		int[] choiceFreqs = null;

        //summary request status
        String summaryRequest = (String) propertyMap.get(
                "Model31.summaryOutput");
        boolean summaryOutput = false;

        if (summaryRequest != null) {
            if (summaryRequest.equals("true")) {
                summaryOutput = true;
            }
        }

        int hh_id;
        int hh_taz_id;

        if (useMessageWindow) {
            mw.setMessage1("Starting Model 3.1 -- Joint Tour Frequency");
        }

        if (useMessageWindow) {
            mw.setMessage2("Building an expression calculator");
        }

        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String) propertyMap.get("Model3.controlFile");
        String outputFile31 = (String) propertyMap.get("Model31.outputFile");

        //M31.csv file
        file = new File(outputFile31);

        // create a new UEC to get utilties for this logit model
        UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(
                    controlFile), M31_MODEL_SHEET, M3_DATA_SHEET, propertyMap,
                Household.class);
        int numberOfAlternatives = uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

		choiceFreqs = new int[numberOfAlternatives+1];

		
		// create and define a new LogitModel object
        LogitModel root = new LogitModel("root", numberOfAlternatives);
        ConcreteAlternative[] alts = new ConcreteAlternative[numberOfAlternatives];

        for (int i = 0; i < numberOfAlternatives; i++) {
            logger.debug("alternative " + (i + 1) + " is " +
                alternativeNames[i]);
            alts[i] = new ConcreteAlternative(alternativeNames[i],
                    new Integer(i + 1));
            root.addAlternative(alts[i]);
            logger.debug(alternativeNames[i] + " has been added to the root");
        }

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();

        int[] availability = new int[numberOfAlternatives + 1];
        Arrays.fill(availability, 1);

        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            //write M31.csv heading
            if (outputFile31 != null) {
                writeRecord(outStream, tableHeadings31);
            }

            // loop over all households in the hh table, write out current M31.csv record
            for (int i = 0; i < hh.length; i++) {
                if (useMessageWindow) {
                    mw.setMessage2("Model31 Choice for hh " + (i + 1) + " of " +
                        hh.length);
                }

                hh_id = hh[i].getID();
                hh_taz_id = hh[i].getTazID();
                tempRecord[0] = hh_id;
                tempRecord[1] = hh_taz_id;

                // apply joint tour model only to households with at least 2 travel active persons, at least 1 traveling no-proeschool person,
                // and either adult or mixed non-zero overlaps.
                int hhType = hh[i].getHHType();

                if ((hhType > 1) &&
                        ((hh[i].getMaxAdultOverlaps() > 0.0) ||
                        (hh[i].getMaxMixedOverlaps() > 0))) {
                    tempRecord[2] = hh[i].getMaxAdultOverlaps();
                    tempRecord[3] = hh[i].getMaxChildOverlaps();
                    tempRecord[4] = hh[i].getMaxMixedOverlaps();
                    tempRecord[5] = hh[i].getTravelActiveAdults();
                    tempRecord[6] = hh[i].getTravelActiveChildren();
                    tempRecord[7] = hh[i].getTravelActiveNonPreschool();

                    // get utilities for each alternative for this household
                    IndexValues index = new IndexValues();
                    index.setZoneIndex(hh[i].getTazID());
                    index.setHHIndex(hh[i].getID());

                    double[] utilities = uec.solve(index, hh[i], availability);

                    //set utility for each alternative
                    for (int a = 0; a < numberOfAlternatives; a++) {
                        alts[a].setAvailability(availability[a + 1] == 1);

                        if (availability[a + 1] == 1) {
                            alts[a].setAvailability((utilities[a] > -99.0));
                        }

                        alts[a].setUtility(utilities[a]);
                    }

                    // set availabilities
                    root.computeAvailabilities();

                    root.getUtility();
                    root.calculateProbabilities();

                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName = chosen.getName();

                    // save chosen alternative in resultsArray
                    for (int a = 0; a < numberOfAlternatives; a++) {
                        if (chosenAltName.equals(alternativeNames[a])) {
                            tempHouseholdChoice = a + 1;

                            if (a == 0) {
                                hh[i].jointTours = null;
                            } else if ((a > 0) && (a < 5)) {
                                hh[i].jointTours = new JointTour[1];
                                hh[i].jointTours[0] = new JointTour(hh[i].getHHSize());
                            } else if (a >= 5) {
                                hh[i].jointTours = new JointTour[2];
                                hh[i].jointTours[0] = new JointTour(hh[i].getHHSize());
                                hh[i].jointTours[1] = new JointTour(hh[i].getHHSize());
                            }

                            switch (a) {
                            case 0:
                                break;

                            case 1:
                                hh[i].jointTours[0].setTourType(TourType.SHOP);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.SHOP);

                                break;

                            case 2:
                                hh[i].jointTours[0].setTourType(TourType.EAT);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.EAT);

                                break;

                            case 3:
                                hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);

                                break;

                            case 4:
                                hh[i].jointTours[0].setTourType(TourType.DISCRETIONARY);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                break;

                            case 5:
                                hh[i].jointTours[0].setTourType(TourType.SHOP);
                                hh[i].jointTours[0].setTourOrder(1);
                                hh[i].incrementJointToursByType(TourType.SHOP);
                                hh[i].jointTours[1].setTourType(TourType.SHOP);
                                hh[i].jointTours[1].setTourOrder(2);
                                hh[i].incrementJointToursByType(TourType.SHOP);

                                break;

                            case 6:
                                hh[i].jointTours[0].setTourType(TourType.SHOP);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.SHOP);
                                hh[i].jointTours[1].setTourType(TourType.EAT);
                                hh[i].jointTours[1].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.EAT);

                                break;

                            case 7:
                                hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[1].setTourType(TourType.SHOP);
                                hh[i].jointTours[1].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.SHOP);

                                break;

                            case 8:
                                hh[i].jointTours[0].setTourType(TourType.SHOP);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.SHOP);
                                hh[i].jointTours[1].setTourType(TourType.DISCRETIONARY);
                                hh[i].jointTours[1].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                break;

                            case 9:
                                hh[i].jointTours[0].setTourType(TourType.EAT);
                                hh[i].jointTours[0].setTourOrder(1);
                                hh[i].incrementJointToursByType(TourType.EAT);
                                hh[i].jointTours[1].setTourType(TourType.EAT);
                                hh[i].jointTours[1].setTourOrder(2);
                                hh[i].incrementJointToursByType(TourType.EAT);

                                break;

                            case 10:
                                hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[1].setTourType(TourType.EAT);
                                hh[i].jointTours[1].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.EAT);

                                break;

                            case 11:
                                hh[i].jointTours[0].setTourType(TourType.DISCRETIONARY);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.DISCRETIONARY);
                                hh[i].jointTours[1].setTourType(TourType.EAT);
                                hh[i].jointTours[1].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.EAT);

                                break;

                            case 12:
                                hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[0].setTourOrder(1);
                                hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[1].setTourType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[1].setTourOrder(2);
                                hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);

                                break;

                            case 13:
                                hh[i].jointTours[0].setTourType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[0].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.OTHER_MAINTENANCE);
                                hh[i].jointTours[1].setTourType(TourType.DISCRETIONARY);
                                hh[i].jointTours[1].setTourOrder(0);
                                hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                break;

                            case 14:
                                hh[i].jointTours[0].setTourType(TourType.DISCRETIONARY);
                                hh[i].jointTours[0].setTourOrder(1);
                                hh[i].incrementJointToursByType(TourType.DISCRETIONARY);
                                hh[i].jointTours[1].setTourType(TourType.DISCRETIONARY);
                                hh[i].jointTours[1].setTourOrder(2);
                                hh[i].incrementJointToursByType(TourType.DISCRETIONARY);

                                break;
                            }

                            break; // break out of for loop after finding chosen alternative
                        }
                    }
                } else {
                    tempHouseholdChoice = 1;
                }

                tempRecord[8] = tempHouseholdChoice;
                choiceFreqs[tempHouseholdChoice]++;

                //check to see if outputFile is defined, and if so write current record to it.
                //write current record to M31.csv
                if (outputFile31 != null) {
                    writeRecord(outStream, tempRecord);
                }
            }

            outStream.close();
        } catch (IOException e) {
            logger.error("IO exception when writing M31.csv");
        }

		if (summaryOutput) {
			writeFreqSummaryToLogger ( "Joint Tour Frequency", "M31", choiceFreqs );
		}

		if (useMessageWindow) {
            mw.setMessage3("End of Model31 (Joint Tour Frequency)");
        }
    }

    public void runCompositionModel() {
        PrintWriter outStream = null;
        File file = null;
        String[] tableHeadings32 = {
            SyntheticPopulation.HHID_FIELD, SyntheticPopulation.HHTAZID_FIELD,
            "joint_tour_id", "joint_tour_comp"
        };
        float[] tempRecord = new float[4];
		int[] choiceFreqs = null;

        int hh_id;
        int hh_taz_id;

        if (useMessageWindow) {
            mw.setMessage1("Starting Model 3.2 -- Joint Tour Party Composition");
        }

        if (useMessageWindow) {
            mw.setMessage2("Building an expression calculator");
        }

        if (useMessageWindow) {
            mw.setMessage3("");
        }

        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String) propertyMap.get("Model3.controlFile");
        String outputFile32 = (String) propertyMap.get("Model32.outputFile");
        file = new File(outputFile32);

        // create a new UEC to get utilties for this logit model
        UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(
                    controlFile), M32_MODEL_SHEET, M3_DATA_SHEET, propertyMap,
                Household.class);
        int numberOfAlternatives = uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

		choiceFreqs = new int[numberOfAlternatives+1];

        // create and define a new LogitModel object
        LogitModel root = new LogitModel("root", numberOfAlternatives);
        ConcreteAlternative[] alts = new ConcreteAlternative[numberOfAlternatives];

        for (int i = 0; i < numberOfAlternatives; i++) {
            logger.debug("alternative " + (i + 1) + " is " +
                alternativeNames[i]);
            alts[i] = new ConcreteAlternative(alternativeNames[i],
                    new Integer(i + 1));
            root.addAlternative(alts[i]);
            logger.debug(alternativeNames[i] + " has been added to the root");
        }

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();

        int[] availability = new int[numberOfAlternatives + 1];
        Arrays.fill(availability, 1);

        // loop over all households in the hh table
        int numOutputRecords = 0;

        for (int i = 0; i < hh.length; i++) {
            if (useMessageWindow) {
                mw.setMessage2("Model32 Choice for hh " + (i + 1) + " of " +
                    hh.length);
            }

            hh_id = hh[i].getID();
            hh_taz_id = hh[i].getTazID();

            // loop through joint tours for this household if there are any.
            JointTour[] jt = hh[i].getJointTours();

            if (jt != null) {
                for (int j = 0; j < jt.length; j++) {

                    hh[i].setTourID(j);
                    hh[i].setTourCategory(TourType.JOINT_CATEGORY);

                    // get utilities for each alternative for this household
                    IndexValues index = new IndexValues();
                    index.setZoneIndex(hh[i].getTazID());
                    index.setHHIndex(hh[i].getID());

                    double[] utilities = uec.solve(index, hh[i], availability);

                    //set utility for each alternative
                    for (int a = 0; a < numberOfAlternatives; a++) {
                        alts[a].setAvailability(availability[a + 1] == 1);

                        if (availability[a + 1] == 1) {
                            alts[a].setAvailability((utilities[a] > -99.0));
                        }

                        alts[a].setUtility(utilities[a]);
                    }

                    // set availabilities
                    root.computeAvailabilities();

                    root.getUtility();
                    root.calculateProbabilities();

                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName = chosen.getName();

                    // save chosen alternative in resultsArray
                    for (int a = 0; a < numberOfAlternatives; a++) {
                        if (chosenAltName.equals(alternativeNames[a])) {
                            // set tour composition: 1-adults, 2-children, 3-both
                            jt[j].setTourComposition(a + 1);

                            break;
                        }
                    }
                }
            } else {
                numOutputRecords++;
            }
        }

        //next household
        String summaryRequest = (String) propertyMap.get(
                "Model32.summaryOutput");
        boolean summaryOutput = false;

        if (summaryRequest != null) {
            if (summaryRequest.equals("true")) {
                summaryOutput = true;
            }
        }

        if (outputFile32 != null) {
            try {
                outStream = new PrintWriter(new BufferedWriter(
                            new FileWriter(file)));

                    if (useMessageWindow) {
                        mw.setMessage3("Writing output file to: " +
                            outputFile32);
                    }

                    //write M32.csv heading
                    writeRecord(outStream, tableHeadings32);

                    // loop over all households in the hh table
                    for (int i = 0; i < hh.length; i++) {
                        hh_id = hh[i].getID();
                        hh_taz_id = hh[i].getTazID();

                        JointTour[] jt = hh[i].getJointTours();

                        if (jt != null) {
                            for (int j = 0; j < jt.length; j++) {
                                tempRecord[0] = hh_id;
                                tempRecord[1] = hh_taz_id;
                                tempRecord[2] = j + 1;
                                tempRecord[3] = jt[j].getTourCompositionCode();
                                choiceFreqs[jt[j].getTourCompositionCode()]++;

								//write current record to M32.csv
								writeRecord(outStream, tempRecord);
                            }
                        } else {
                            tempRecord[0] = hh_id;
                            tempRecord[1] = hh_taz_id;
                            tempRecord[2] = 0;
                            tempRecord[3] = 0;
							choiceFreqs[0]++;

							//write current record to M32.csv
							writeRecord(outStream, tempRecord);
                        }

                    }

                outStream.close();
            } catch (IOException e) {
                logger.error("IO Exception when write M32.csv");
            }
        }


		if (summaryOutput) {
			writeFreqSummaryToLogger ( "Party Composition", "M32", choiceFreqs );
		}


		if (useMessageWindow) {
            mw.setMessage3("End of Model32 (Party Composition)");
        }
    }

    public void runParticipationModel() {
      PrintWriter outStream = null;
      File file = null;

      //M33.csv heading
      String[] tableHeadings33 = {SyntheticPopulation.HHID_FIELD,SyntheticPopulation.HHTAZID_FIELD,"person_id","joint_tour_id","personType","patternType","tourType","composition","participation","personAvailTimeWindow"};
      //current record
      float[] tempRecord = new float[10];
	  int[] choiceFreqs = null;

        int hh_id;
        int hh_taz_id;

        int personType;
        boolean validParty;

        if (useMessageWindow) {
            mw.setMessage1("Starting Model 3.3 -- Joint Tour Participation");
        }

        if (useMessageWindow) {
            mw.setMessage2("Building an expression calculator");
        }

        if (useMessageWindow) {
            mw.setMessage3("");
        }

        //open files named in morpc.proerties and run fully joint tour frequency model
        String controlFile = (String) propertyMap.get("Model3.controlFile");
        String outputFile33 = (String) propertyMap.get("Model33.outputFile");
        file=new File(outputFile33);

        // create a new UEC to get utilties for this logit model
        UtilityExpressionCalculator uec = new UtilityExpressionCalculator(new File(
                    controlFile), M33_MODEL_SHEET, M3_DATA_SHEET, propertyMap,
                Household.class);
        int numberOfAlternatives = uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

		choiceFreqs = new int[numberOfAlternatives+1];

        // create and define a new LogitModel object
        LogitModel root = new LogitModel("root", numberOfAlternatives);
        ConcreteAlternative[] alts = new ConcreteAlternative[numberOfAlternatives];

        for (int i = 0; i < numberOfAlternatives; i++) {
            logger.debug("alternative " + (i + 1) + " is " +
                alternativeNames[i]);
            alts[i] = new ConcreteAlternative(alternativeNames[i],
                    new Integer(i + 1));
            root.addAlternative(alts[i]);
            logger.debug(alternativeNames[i] + " has been added to the root");
        }

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();

        int[] availability = new int[numberOfAlternatives + 1];
        Arrays.fill(availability, 1);

        // loop over all households in the hh table
        for (int i = 0; i < hh.length; i++) {
            if (useMessageWindow) {
                mw.setMessage2("Model33 Choice for hh " + (i + 1) + " of " +
                    hh.length);
            }

            // get joint tours array for this household.
            JointTour[] jt = hh[i].getJointTours();

            if (jt == null) {
                continue;
            }

            hh_id = hh[i].getID();
            hh_taz_id = hh[i].getTazID();

            // get person array for this household.
            Person[] persons = hh[i].getPersonArray();

            // loop through joint tours for this household and set person participation
            for (int j = 0; j < jt.length; j++) {
                hh[i].setTourID(j);
                hh[i].setTourCategory(TourType.JOINT_CATEGORY);

                // make sure each joint tour has a valid composition before going to the next one.
                validParty = false;

                while (!validParty) {
                    int adults = 0;
                    int children = 0;

                    for (int p = 1; p < persons.length; p++) {
                        hh[i].setPersonID(p);

                        // define an array in the person objects to hold whether or not person
                        // participates in each joint tour.
                        persons[p].setJointTourParticipationArray(jt.length);

                        personType = persons[p].getPersonType();

                        // if person type is inconsistent with tour composition, participation is by definition no.
                        if (((personType > 4) && jt[j].areChildrenInTour()) ||
                                ((personType <= 4) && jt[j].areAdultsInTour())) {
                            // get utilities for each alternative for this household
                            IndexValues index = new IndexValues();
                            index.setZoneIndex(hh[i].getTazID());
                            index.setHHIndex(hh[i].getID());

                            double[] utilities = uec.solve(index, hh[i],
                                    availability);

                            //set utility for each alternative
                            for (int a = 0; a < numberOfAlternatives; a++) {
                                alts[a].setAvailability(availability[a + 1] == 1);

                                if (availability[a + 1] == 1) {
                                    alts[a].setAvailability((utilities[a] > -99.0));
                                }

                                alts[a].setUtility(utilities[a]);
                            }

                            // set availabilities
                            root.computeAvailabilities();

                            root.getUtility();
                            root.calculateProbabilities();

                            ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                            String chosenAltName = chosen.getName();

                            // find index of the chosen alternative
                            int alt = 0;

                            for (int a = 0; a < numberOfAlternatives; a++) {
                                if (chosenAltName.equals(alternativeNames[a])) {
                                    alt = a + 1;

                                    break;
                                }
                            }

                            if (alt == 1) {
                                // set person participation to true for this tour
                                jt[j].setPersonParticipation(p, true);
                                persons[p].setJointTourParticipation(j, true);

                                if (personType > 4) {
                                    children++;
                                } else {
                                    adults++;
                                }
                            } else {
                                // set person participation for this tour to false
                                jt[j].setPersonParticipation(p, false);
                                persons[p].setJointTourParticipation(j, false);
                            }
                        } else {
                            // set person participation for this tour to false
                            jt[j].setPersonParticipation(p, false);
                            persons[p].setJointTourParticipation(j, false);
                        }
                    }

                    //end for persons
                    validParty = jt[j].isTourValid(children, adults);
                }

                // end while
            }

            //end for joint tours
            // save number of joint tours in which person participates
            for (int p = 1; p < persons.length; p++) {
                int numJointTours = 0;

                for (int j = 0; j < jt.length; j++) {
                    if (jt[j].getPersonParticipation(p)) {
                        numJointTours++;
                    }
                }

                persons[p].setNumJointTours(numJointTours);
            }

            // save number of persons in joint tours
            for (int j = 0; j < jt.length; j++) {
                int numPersons = 0;
                int numAdults = 0;
                int numChildren = 0;
                int numPreschool = 0;
                int numPredriv = 0;
                int numUniv = 0;
                int allWorkFull = 1;

                for (int p = 1; p < persons.length; p++) {
                    if (jt[j].getPersonParticipation(p)) {
                        numPersons++;

                        if ((persons[p].getPersonType() == PersonType.PRESCHOOL) ||
                                (persons[p].getPersonType() == PersonType.SCHOOL_DRIV) ||
                                (persons[p].getPersonType() == PersonType.SCHOOL_PRED)) {
                            numChildren++;
                        } else {
                            numAdults++;

                            if (persons[p].getPersonType() != PersonType.WORKER_F) {
                                allWorkFull = 0;
                            }
                        }

                        if (persons[p].getPersonType() == PersonType.PRESCHOOL) {
                            numPreschool++;
                        } else if (persons[p].getPersonType() == PersonType.SCHOOL_PRED) {
                            numPredriv++;
                        } else if (persons[p].getPersonType() == PersonType.STUDENT) {
                            numUniv++;
                        }
                    }
                }

                jt[j].setNumPersons(numPersons);
                jt[j].setNumAdults(numAdults);
                jt[j].setNumChildren(numChildren);

                jt[j].setNumPreschool(numPreschool);
                jt[j].setNumPredriv(numPredriv);
                jt[j].setNumUniv(numUniv);
                jt[j].setAllAdultsWorkFull(allWorkFull);
            }
        }//next household

        //M33.csv summary request status
        String summaryRequest = (String) propertyMap.get(
                "Model33.summaryOutput");
        boolean summaryOutput = false;

        if (summaryRequest != null) {
            if (summaryRequest.equals("true")) {
                summaryOutput = true;
            }
        }

        if (outputFile33 != null) {
            // loop over all households in the hh table to count number of output records
            try {
              outStream = new PrintWriter(new BufferedWriter(
                  new FileWriter(file)));

                if (useMessageWindow) {
                  mw.setMessage3("Writing output file to: " +
                                 outputFile33);
                }

                //write M33.csv heading
                writeRecord(outStream, tableHeadings33);

                // loop over all households in the hh table
                for (int i = 0; i < hh.length; i++) {
                  hh_id = hh[i].getID();
                  hh_taz_id = hh[i].getTazID();

                  JointTour[] jt = hh[i].getJointTours();
                  Person[] persons = hh[i].getPersonArray();

                  for (int p = 1; p < persons.length; p++) {
                    if (jt != null) {
                      for (int j = 0; j < jt.length; j++) {
                        tempRecord[0] = hh_id;
                        tempRecord[1] = hh_taz_id;
                        tempRecord[2] = persons[p].getID();
                        tempRecord[3] = j + 1;
                        tempRecord[4] = persons[p].getPersonType();
                        tempRecord[5] = persons[p].getPatternType();
                        tempRecord[6] = (jt[j].getTourType());
                        tempRecord[7] = (jt[j].getTourCompositionCode());
                        tempRecord[8] = (jt[j].getPersonParticipation(p)
                                         ? 1 : 2);
                        tempRecord[9] = (persons[p].getAvailableWindow());
                        if (jt[j].getPersonParticipation(p))
                            choiceFreqs[1]++;
                        else
                            choiceFreqs[2]++;

                        //write current record to M33.csv
						writeRecord(outStream, tempRecord);
                      }
                    }
                    else {
                      tempRecord[0] = hh_id;
                      tempRecord[1] = hh_taz_id;
                      tempRecord[2] = persons[p].getID();
                      tempRecord[3] = 0;
                      tempRecord[4] = persons[p].getPersonType();
                      tempRecord[5] = persons[p].getPatternType();
                      tempRecord[6] = 0;
                      tempRecord[7] = 0;
                      tempRecord[8] = 0;
                      tempRecord[9] = (persons[p].getAvailableWindow());
					  choiceFreqs[0]++;

					  //write current record to M33.csv
					  writeRecord(outStream, tempRecord);
                    }
                  }
                }
              outStream.close();
            }catch(IOException e){
              logger.error("IO exception when write m33.csv");
            }
        }

		if (summaryOutput) {
			writeFreqSummaryToLogger ( "Joint Tour Participation", "M33", choiceFreqs );
		}


        if (useMessageWindow) {
            mw.setMessage3("End of Model33 (Joint Tour Participation)");
        }

        if (useMessageWindow) {
            mw.setVisible(false);
        }
    }

    private void writeRecord(PrintWriter outStream, String[] record) {

        outStream.print( String.format("%s", record[0]) );
        for (int i=1; i < record.length; i++) {
            outStream.print( String.format(",%s", record[i]) );
        }

        outStream.println();
    }

    private void writeRecord(PrintWriter outStream, float[] record) {
        
        outStream.print( String.format("%.0f", record[0]) );
        for (int i=1; i < record.length; i++) {
            outStream.print( String.format(",%.0f", record[i]) );
        }

        outStream.println();
    }


	private void writeFreqSummaryToLogger ( String tableTitle, String fieldName, int[] freqs ) {
    
		// print a simple summary table
		logger.info( "Frequency Report table: " + tableTitle );
		logger.info( "Frequency for field " + fieldName );
		logger.info(String.format("%8s", "Value") + "  " + String.format("%-20s", "Description") + "  " + String.format("%11s", "Frequency"));
		
		int total = 0;
		for (int i = 0; i < freqs.length; i++) {
		    if (freqs[i] > 0) {
		        String description = OutputDescription.getDescription(fieldName, i);
		        logger.info( String.format("%8d", i) + "  " + String.format("%-20s", description) + "  " + String.format("%11d", freqs[i] ) );
				total += freqs[i];
		    }
		}
		
		logger.info(String.format("%8s", "Total") + String.format("%35d\n\n\n", total));
	}
}
