package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 */
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.DiskObjectArray;
import com.pb.common.util.SeededRandom;
import com.pb.common.util.IndexSort;

import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.PatternType;
import com.pb.morpc.structures.Person;
import com.pb.morpc.structures.PersonType;
import com.pb.morpc.structures.Tour;
import com.pb.morpc.structures.TourType;
import com.pb.morpc.synpop.SyntheticPopulation;
import com.pb.common.util.ResourceUtil;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;


public class HouseholdArrayManager implements java.io.Serializable {
    static Logger logger = Logger.getLogger("com.pb.morpc.models");

    
    HashMap propertyMap;

    int hhsProcessed = 0;
    int hhsPerCPU = 0;
    int totalHhsToProcess = 0;

	int[] listOfHHIds = null;
	Household[] bigHHArray = null;

	
	
    public HouseholdArrayManager(HashMap propertyMap) {
        this.propertyMap = propertyMap;

        hhsPerCPU = Integer.parseInt((String) propertyMap.get("hhsPerCPU"));
    }
    
    public void createBigHHArrayFromDiskObject(){
    	String diskObjectArrayFile=(String)propertyMap.get("DiskObjectArrayInput.file");

    	try{
    		DiskObjectArray diskObjectArray=new DiskObjectArray(diskObjectArrayFile);
    		int NoHHs=diskObjectArray.getArraySize();
    		bigHHArray=new Household[NoHHs];
    		for(int i=0; i<NoHHs; i++){
    			bigHHArray[i]=(Household)diskObjectArray.get(i);
    		}
    		totalHhsToProcess=bigHHArray.length;
    	}catch(IOException e){
    		logger.severe("can not open disk object array file for reading.");
    	}
    }

    public void createBigHHArray() {
        Tour[] it = null;

        String zonalFile;

//		String hhFile = (String) propertyMap.get("SyntheticHousehold.file");
		String hhFile = (String) propertyMap.get("Model1.outputFile");
        String personFile = (String) propertyMap.get("SyntheticPerson.file");

        int hh_idPosition;
        int hh_id;
        int hh_taz_idPosition;
        int taz_id;


		// read household file
        TableDataSet hhTable = null;

        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            hhTable = reader.readFile(new File(hhFile));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Household[] tempHHs = new Household[hhTable.getRowCount()];
        Person[][] tempPersons = new Person[hhTable.getRowCount()][];

        // get column positions of the fields needed from the household table
        // for either writing out or for aggregation required procedures
        hh_idPosition = hhTable.getColumnPosition(SyntheticPopulation.HHID_FIELD);

        if (hh_idPosition <= 0) {
            logger.severe(SyntheticPopulation.HHID_FIELD +
                " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        hh_taz_idPosition = hhTable.getColumnPosition(SyntheticPopulation.HHTAZID_FIELD);

        if (hh_taz_idPosition <= 0) {
            logger.severe(SyntheticPopulation.HHTAZID_FIELD +
                " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        int workers_f_col = hhTable.getColumnPosition(SyntheticPopulation.WORKERS_F_FIELD);
        int workers_p_col = hhTable.getColumnPosition(SyntheticPopulation.WORKERS_P_FIELD);
        int students_col = hhTable.getColumnPosition(SyntheticPopulation.STUDENTS_FIELD);
        int nonworkers_col = hhTable.getColumnPosition(SyntheticPopulation.NONWORKERS_FIELD);
        int preschool_col = hhTable.getColumnPosition(SyntheticPopulation.PRESCHOOL_FIELD);
        int schoolpred_col = hhTable.getColumnPosition(SyntheticPopulation.SCHOOLPRED_FIELD);
        int schooldriv_col = hhTable.getColumnPosition(SyntheticPopulation.SCHOOLDRIV_FIELD);

        int income_col = hhTable.getColumnPosition(SyntheticPopulation.INCOME_FIELD);

		int M1_colPosition = hhTable.getColumnPosition("M1");

        // loop over all households in the hh table and fill in info for
        // a new Household object in an array.
        boolean[] tempAvail;

        for (int i = 0; i < hhTable.getRowCount(); i++) {
            tempHHs[i] = new Household();
            tempHHs[i].setID((int) hhTable.getValueAt(i + 1, hh_idPosition));
            tempHHs[i].setTazID((int) hhTable.getValueAt(i + 1,
                    hh_taz_idPosition));
            tempHHs[i].setHHIncome((int) hhTable.getValueAt(i + 1, income_col));

            int numOfWorkers_f = (int) hhTable.getValueAt(i + 1, workers_f_col);
            int numOfWorkers_p = (int) hhTable.getValueAt(i + 1, workers_p_col);
            int numOfStudents = (int) hhTable.getValueAt(i + 1, students_col);
            int numOfNonworkers = (int) hhTable.getValueAt(i + 1, nonworkers_col);
            int numOfPreschool = (int) hhTable.getValueAt(i + 1, preschool_col);
            int numOfSchoolpred = (int) hhTable.getValueAt(i + 1, schoolpred_col);
            int numOfSchooldriv = (int) hhTable.getValueAt(i + 1, schooldriv_col);
            int hhSize = (int) (numOfWorkers_f + numOfWorkers_p +
                numOfStudents + numOfNonworkers + numOfPreschool +
                numOfSchoolpred + numOfSchooldriv);

            tempHHs[i].setHHSize(hhSize);

			tempHHs[i].setAutoOwnership((int) hhTable.getValueAt(i + 1, M1_colPosition));
            
            tempPersons[i] = new Person[hhSize + 1];
        }
         //next household

        // read person file and count number of adults and children with travel active daily patterns
        TableDataSet personTable = null;

        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            personTable = reader.readFile(new File(personFile));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        int[] hhTravelActiveAdults = new int[hhTable.getRowCount()];
        int[] hhTravelActiveChildren = new int[hhTable.getRowCount()];
        int[] hhTravelActiveNonPreschool = new int[hhTable.getRowCount()];

        // loop over all persons in each hh of the person table, add Person objects and info
        // to Household objects
        hh_idPosition = personTable.getColumnPosition(SyntheticPopulation.HHID_FIELD);

        if (hh_idPosition <= 0) {
            logger.severe(SyntheticPopulation.HHID_FIELD +
                " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        int person_idPosition = personTable.getColumnPosition("person_id");
        int person_typePosition = personTable.getColumnPosition("person_type");
        int M2Position = personTable.getColumnPosition("M2");

        for (int i = 0; i < personTable.getRowCount(); i++) {
            hh_id = (int) personTable.getValueAt(i + 1, hh_idPosition);

            int person_id = (int) personTable.getValueAt(i + 1,
                    person_idPosition);
            int personType = (int) personTable.getValueAt(i + 1,
                    person_typePosition);

            tempPersons[hh_id][person_id] = new Person();
            tempPersons[hh_id][person_id].setID(person_id);
            tempPersons[hh_id][person_id].setPersonType(personType);

            tempHHs[hh_id].incrementPersonsByType(personType);

            int patternType = (int) personTable.getValueAt(i + 1, M2Position);
            tempPersons[hh_id][person_id].setPatternType(patternType);

            if (patternType != PatternType.HOME) {
                if (personType < 5) {
                    hhTravelActiveAdults[hh_id]++;
                    hhTravelActiveNonPreschool[hh_id]++;
                } else {
                    if (personType != 5) {
                        hhTravelActiveNonPreschool[hh_id]++;
                    }

                    hhTravelActiveChildren[hh_id]++;
                }
            }
        }

        // set the number of travelling adults and children in the Household objects
        // and the person objects in the Household objects.
        // create an array of mandatory tours for each person in hh based on patternType
        // determined in models 2.1-2.7.
        for (int i = 0; i < hhTable.getRowCount(); i++) {
            tempHHs[i].setTravelActiveAdults(hhTravelActiveAdults[i]);
            tempHHs[i].setTravelActiveChildren(hhTravelActiveChildren[i]);
            tempHHs[i].setTravelActiveNonPreschool(hhTravelActiveNonPreschool[i]);

            tempHHs[i].setPersonArray(tempPersons[i]);

            // count the total number of mandatory tours in the hh.
            int numMandatoryTours = 0;
            Person[] persons = tempHHs[i].getPersonArray();

            for (int p = 1; p < persons.length; p++) {
                int patternType = persons[p].getPatternType();

                switch (patternType) {
                case PatternType.WORK_1:
                    numMandatoryTours += 1;

                    break;

                case PatternType.WORK_2:
                    numMandatoryTours += 2;

                    break;

                case PatternType.SCHOOL_1:
                    numMandatoryTours += 1;

                    break;

                case PatternType.SCHOOL_2:
                    numMandatoryTours += 2;

                    break;

                case PatternType.SCHOOL_WORK:
                    numMandatoryTours += 2;

                    break;

                case PatternType.UNIV_1:
                    numMandatoryTours += 1;

                    break;

                case PatternType.UNIV_2:
                    numMandatoryTours += 2;

                    break;

                case PatternType.UNIV_WORK:
                    numMandatoryTours += 2;

                    break;

                case PatternType.WORK_UNIV:
                    numMandatoryTours += 2;

                    break;
                }
            }

            // create the mandatory tours array and set person participation
            int k = 0;

            if (numMandatoryTours > 0) {
                it = new Tour[numMandatoryTours];
            } else {
                it = null;
            }

            for (int p = 1; p < persons.length; p++) {
                persons[p].setMandatoryTourParticipationArray(numMandatoryTours);

                int patternType = persons[p].getPatternType();

                switch (patternType) {
                case PatternType.WORK_1:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(0);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(1);
                    tempHHs[i].incrementMandatoryToursByType(TourType.WORK);
                    k++;

                    break;

                case PatternType.WORK_2:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[i].incrementMandatoryToursByType(TourType.WORK);
                    k++;
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setNumMandTours(2);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[i].incrementMandatoryToursByType(TourType.WORK);
                    k++;

                    break;

                case PatternType.SCHOOL_1:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(0);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(1);
                    tempHHs[i].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;

                    break;

                case PatternType.SCHOOL_2:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[i].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(2);
                    tempHHs[i].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;

                    break;

                case PatternType.SCHOOL_WORK:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.SCHOOL);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[i].incrementMandatoryToursByType(TourType.SCHOOL);
                    k++;
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(2);
                    tempHHs[i].incrementMandatoryToursByType(TourType.WORK);
                    k++;

                    break;

                case PatternType.UNIV_1:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.UNIVERSITY);
                    it[k].setTourOrder(0);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(1);
                    tempHHs[i].incrementMandatoryToursByType(TourType.UNIVERSITY);
                    k++;

                    break;

                case PatternType.UNIV_2:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.UNIVERSITY);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[i].incrementMandatoryToursByType(TourType.UNIVERSITY);
                    k++;
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.UNIVERSITY);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(2);
                    tempHHs[i].incrementMandatoryToursByType(TourType.UNIVERSITY);
                    k++;

                    break;

                case PatternType.UNIV_WORK:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.UNIVERSITY);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[i].incrementMandatoryToursByType(TourType.UNIVERSITY);
                    k++;
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(2);
                    tempHHs[i].incrementMandatoryToursByType(TourType.WORK);
                    k++;

                    break;

                case PatternType.WORK_UNIV:
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.WORK);
                    it[k].setTourOrder(1);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    tempHHs[i].incrementMandatoryToursByType(TourType.WORK);
                    k++;
                    it[k] = new Tour(tempHHs[i].getHHSize());
                    it[k].setTourType(TourType.UNIVERSITY);
                    it[k].setTourOrder(2);
                    it[k].setPersonParticipation(p, true);
                    persons[p].setMandatoryTourParticipation(k, true);
                    persons[p].setNumMandTours(2);
                    tempHHs[i].incrementMandatoryToursByType(TourType.UNIVERSITY);
                    k++;

                    break;
                }
            }

            // count the number and type of travelers in the hh.
            int nonPreschoolTravelers = 0;
            int preschoolTravelers = 0;
            int preschoolAtHome = 0;
            int predrivAtHome = 0;

            for (int p = 1; p < persons.length; p++) {
                int patternType = persons[p].getPatternType();
                int personType = persons[p].getPersonType();

                if (patternType != PatternType.HOME) {
                    if (personType == PersonType.PRESCHOOL) {
                        preschoolTravelers++;
                    } else {
                        nonPreschoolTravelers++;
                    }
                } else {
                    if (personType == PersonType.PRESCHOOL) {
                        preschoolAtHome++;
                    } else if (personType == PersonType.SCHOOL_PRED) {
                        predrivAtHome++;
                    }

                    // set all time windows unavailable for persons that chose at-home pattern
                    tempAvail = persons[p].getAvailable();

                    for (int j = 0; j < tempAvail.length; j++)
                        persons[p].setHourUnavailable(j);
                }
            }

            int hhType = 0;

            if (nonPreschoolTravelers == 0) {
                hhType = 0;
            } else if ((nonPreschoolTravelers == 1) &&
                    (preschoolTravelers == 0)) {
                hhType = 1;
            } else {
                hhType = 2;
            }

            tempHHs[i].setHHType(hhType);

            tempHHs[i].setMandatoryTours(it);

            // set the array which records what person id belongs to each person type
            tempHHs[i].setPersonsByPersonTypeArray();

        }

		hhTable = null;
		
        totalHhsToProcess = tempHHs.length;

        bigHHArray = tempHHs;
        tempPersons = null;

        logger.info("Household Array has been created");
    }

    public void createHHArrayToProcess() {
        final int MAX_ZONES = 5000;

        int numberOfHHs = -1;
        double zonalPctOfHHs = -1.0;
        boolean allHHs = false;

        String numberOfHHsString = (String) propertyMap.get("numberOfHHs");

        if (numberOfHHsString != null) {
            numberOfHHs = Integer.parseInt((String) propertyMap.get(
                        "numberOfHHs"));
        }

        String zonalPctOfHHsString = (String) propertyMap.get("zonalPctOfHHs");

        if (zonalPctOfHHsString != null) {
            zonalPctOfHHs = Double.parseDouble((String) propertyMap.get(
                        "zonalPctOfHHs"));
        }

        String allHHsString = (String) propertyMap.get("allHHs");

        if (allHHsString.equalsIgnoreCase("true")) {
            allHHs = true;
        }

        // check for errors in properties file
        if ((numberOfHHs > 0) && (zonalPctOfHHs > 0.0)) {
            logger.severe(
                "Error in setting which households to process in morpc.properties file.");
            logger.severe("Both numberOfHHs and zonalPctOfHHs have values set,");
            logger.severe(
                "but only one should be set with the other commented out.");
            logger.severe("allHHs = " + allHHs + ", numberOfHHs = " +
                numberOfHHs + ", zonalPctOfHHs = " + zonalPctOfHHs);
            logger.severe("");
            System.exit(-1);
        }

        if (allHHs && (numberOfHHs > 0)) {
            logger.severe(
                "Error in setting which households to process in morpc.properties file.");
            logger.severe(
                "allHHs has been set to true and numberOfHHs has also been set.");
            logger.severe(
                "If allHHs is true, both numberOfHHs and zonalPctOfHHs should be commented out.");
            logger.severe("allHHs = " + allHHs + ", numberOfHHs = " +
                numberOfHHs + ", zonalPctOfHHs = " + zonalPctOfHHs);
            logger.severe("");
            System.exit(-1);
        }

        if (allHHs && (zonalPctOfHHs > 0)) {
            logger.severe(
                "Error in setting which households to process in morpc.properties file.");
            logger.severe(
                "allHHs has been set to true and zonalPctOfHHs has also been set.");
            logger.severe(
                "If allHHs is true, both numberOfHHs and zonalPctOfHHs should be commented out.");
            logger.severe("allHHs = " + allHHs + ", numberOfHHs = " +
                numberOfHHs + ", zonalPctOfHHs = " + zonalPctOfHHs);
            logger.severe("");
            System.exit(-1);
        }

        if (!allHHs && ((numberOfHHs < 0) && (zonalPctOfHHs < 0))) {
            logger.severe(
                "Error in setting which households to process in morpc.properties file.");
            logger.severe(
                "allHHs has been set to false and neither numberOfHHs or zonalPctOfHHs has been set.");
            logger.severe(
                "If allHHs is false, either numberOfHHs or zonalPctOfHHs must be set.");
            logger.severe("allHHs = " + allHHs + ", numberOfHHs = " +
                numberOfHHs + ", zonalPctOfHHs = " + zonalPctOfHHs);
            logger.severe("");
            System.exit(-1);
        }

        // create an array with the list of HH ids to be procesed for this model run.
        if (allHHs) {
            // create an array of the first n households.
            listOfHHIds = new int[bigHHArray.length];

            for (int i = 0; i < listOfHHIds.length; i++)
                listOfHHIds[i] = i;
        } else if (numberOfHHs > 0) {
            // create an array of the first n households.
            listOfHHIds = new int[numberOfHHs];

            for (int i = 0; i < listOfHHIds.length; i++)
                listOfHHIds[i] = i;
        } else {
            // create an array of households from a fixed percentage of hhs in each zone.
            ArrayList hhList = new ArrayList((int) (zonalPctOfHHs * bigHHArray.length));

            // count total households by home TAZ.
            int[] hhFreqByZone = new int[MAX_ZONES];

            for (int i = 0; i < bigHHArray.length; i++)
                hhFreqByZone[bigHHArray[i].getTazID()]++;

            // create an array dimensioned to number of hhs in the zone for each zone
            int[][] hhsByZone = new int[MAX_ZONES][];

            for (int i = 0; i < MAX_ZONES; i++) {
                if (hhFreqByZone[i] > 0) {
                    hhsByZone[i] = new int[hhFreqByZone[i] + 1];
                    hhsByZone[i][0] = 1;
                }
            }

            // fill the array with household ids for each zone
            int k = 0;

            for (int i = 0; i < bigHHArray.length; i++) {
                k = hhsByZone[bigHHArray[i].getTazID()][0];
                hhsByZone[bigHHArray[i].getTazID()][k] = bigHHArray[i].getID();
                hhsByZone[bigHHArray[i].getTazID()][0]++;
            }

            for (int i = 0; i < MAX_ZONES; i++) {
                if (hhFreqByZone[i] > 0) {
                    int[] randomNumbers = new int[hhFreqByZone[i]];

                    for (int j = 0; j < hhFreqByZone[i]; j++)
                        randomNumbers[j] = (int) (1000000 * SeededRandom.getRandom());

                    int[] sortedRandomNumberIndices = IndexSort.indexSort(randomNumbers);

                    for (int j = 0;
                            j < (int) (zonalPctOfHHs * hhFreqByZone[i]); j++) {
                        k = sortedRandomNumberIndices[j];
                        hhList.add(Integer.toString(hhsByZone[i][k + 1]));
                    }
                }
            }

            listOfHHIds = new int[hhList.size()];

            for (int i = 0; i < listOfHHIds.length; i++)
                listOfHHIds[i] = Integer.parseInt((String) hhList.get(i));

        }

        if (totalHhsToProcess > listOfHHIds.length)
			totalHhsToProcess = listOfHHIds.length;
        
		if (hhsPerCPU > totalHhsToProcess)
			hhsPerCPU = totalHhsToProcess;
        
    }

    public Household[] getHouseholds() {
        // return the array of hh objects.
        return bigHHArray;
    }

    public Household[] getHouseholdsForWorker() {

        if ((hhsProcessed + hhsPerCPU) < totalHhsToProcess) {

			Household[] workerHHs = new Household[hhsPerCPU];
            for (int i = 0; i < hhsPerCPU; i++)
                workerHHs[i] = (Household) bigHHArray[listOfHHIds[i + hhsProcessed]];

            hhsProcessed += hhsPerCPU;

			// return the array of hh objects so the worker can process DTM choices.
			return workerHHs;
        }
        else {

			int hhsRemaining = totalHhsToProcess - hhsProcessed;

			Household[] tempHHs = null;
            if (hhsRemaining > 0) {
                
                tempHHs = new Household[hhsRemaining];

                for (int i = 0; i < hhsRemaining; i++)
                    tempHHs[i] = (Household) bigHHArray[listOfHHIds[i + hhsProcessed]];

                hhsProcessed += hhsRemaining;
            }

			// return the array of hh objects so the worker can process DTM choices.
			return tempHHs;
        }

    }

	public void sendResults(Household[] hh) {
		int hhId;

		for (int i = 0; i < hh.length; i++) {
			hhId = hh[i].getID();
			bigHHArray[hhId] = hh[i];
		}
	}

    public void resetHhsProcessed() {
        hhsProcessed = 0;
    }

    public int getNumberOfHouseoldsToProcess() {
        return totalHhsToProcess;
    }
}
