package com.pb.morpc.synpop.pums2000;

import com.pb.common.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.HashMap;
import java.util.logging.Logger;


/**
 * The PUMSData class in the synpop.pums package is used to manage input
 * of PUMS data.
 *
 */
public class PUMSData {
    protected static Logger logger = Logger.getLogger("com.pb.morpc.synpop.pums2000");
    public DataDictionary dd;
    public PUMSHH[] pumsHH;
    private int hhCount;
    private int recCount;
    private HashMap pumaIndex;
    private HashMap indexPuma;

    public PUMSData(String PUMSData, String PUMSDataDictionary) {
        this.dd = new DataDictionary(PUMSDataDictionary);

        this.pumsHH = new PUMSHH[countPUMSHHRecords(PUMSData)];
    }

    /**
    returns a correspondence HashMap for pumas to indices.
    **/
    public HashMap getPumaIndex() {
        return pumaIndex;
    }

    /**
    returns a correspondence HashMap for indices to pumas.
    **/
    public HashMap getIndexPuma() {
        return indexPuma;
    }

    /**
    creates a correspondence HashMap for pumas to indices.
    **/
    public void createPumaIndices() {
        int puma;
        int index;
        pumaIndex = new HashMap(getNumberPumas(), 1.0f);
        indexPuma = new HashMap(getNumberPumas(), 1.0f);

        // get the index in the HH attributes array of the PUMA variable
        int fieldIndex = getAttribIndex("PUMA");

        // go through all PUMS HH records and enter the key, value
        // pair if the key is not in the HashMap
        index = 0;

        for (int i = 0; i < hhCount; i++) {
            puma = pumsHH[i].attribs[fieldIndex];

            if (!pumaIndex.containsKey(Integer.toString(puma))) {
                pumaIndex.put(Integer.toString(puma), Integer.toString(index));
                indexPuma.put(Integer.toString(index), Integer.toString(puma));
                index++;
            }
        }
    }

    /**
    Gets the largest value of PUMA in the data file.
    Should not be used until after PUMSData.readData() has built the PUMS household list.
    **/
    public int getMaxPuma() {
        int fieldIndex = -1;
        int maxPuma = 0;

        // get the index in the HH attributes array of the PUMA variable
        fieldIndex = getAttribIndex("PUMA");

        // go through all PUMS HH records and return the maximum PUMA value
        for (int i = 0; i < hhCount; i++) {
            if (pumsHH[i].attribs[fieldIndex] > maxPuma) {
                maxPuma = pumsHH[i].attribs[fieldIndex];
            }
        }

        return maxPuma;
    }

    /**
    Gets the number of unique PUMAs in the data file.
    Should not be used until after PUMSData.readData() has built the PUMS household list.
    **/
    public int getNumberPumas() {
        int fieldIndex = -1;
        int pumaCount = 0;
        int[] usedPuma = new int[getMaxPuma() + 1];

        // get the index in the HH attributes array of the PUMA variable
        fieldIndex = getAttribIndex("PUMA");

        // go through all PUMS HH records and flag PUMA values found
        for (int i = 0; i < hhCount; i++) {
            usedPuma[pumsHH[i].attribs[fieldIndex]] = 1;
        }

        // count the number of unique PUMAs found
        for (int i = 0; i < usedPuma.length; i++) {
            if (usedPuma[i] == 1) {
                pumaCount++;
            }
        }

        return pumaCount;
    }

    /**
    Gets the index number of the HH variable.
    Should not be used until after PUMSData.readData() has built the PUMS household list.
    **/
    public int getAttribIndex(String Variable) {
        int index = -1;

        // get the index in the HH attributes array of the PUMA variable
        for (int i = 0; i < pumsHH[0].attribs.length; i++) {
            if (pumsHH[0].attribLabels[i].equals(Variable)) {
                index = i;

                break;
            }
        }

        if (index < 0) {
            logger.severe("variable " + Variable +
                " not defined in list of attributes for pumsHH[0]");
            logger.severe("exiting getAttribIndex() in PUMSData.");
            logger.severe("exit (24)");
            System.exit(24);
        }

        return index;
    }

    private int countPUMSHHRecords(String fileName) {
        hhCount = 0;

        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String s = new String();

            while ((s = in.readLine()) != null) {
                // count household records only
                if (getPUMSRecType(s).equals("H")) {
                    hhCount++;
                }
            }
        } catch (Exception e) {
            logger.severe(
                "IO Exception caught reading data dictionary file: " +
                fileName);
            e.printStackTrace();
        }

        logger.info(hhCount + " HH records found in: " + fileName);

        return hhCount;
    }

    public void readData(String fileName) {
        hhCount = 0;
        recCount = 0;

        PUMSHH hh = new PUMSHH();

        int adultCount = 0;
        int wrkCount = 0;
        int perCount = 0;
        int rlabor = 0;
        int age = 0;

        int school = 0;
        int hours = 0;
        int personType = 0;
        int invalid = 0;
        int personsField = 0;
        int hhid = 0;

        // get the PERSONS field index
        for (int i = 0; i < hh.attribs.length; i++) {
            if (hh.attribLabels[i].equals("PERSONS")) {
                personsField = i;

                break;
            }
        }

        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String s = new String();

            while ((s = in.readLine()) != null) {
                recCount++;

                // skip HH records where persons field is zero
                if ((getPUMSHHDataValue(s, hh.attribLabels[personsField])) > 0) {
                    // read the household attributes from the household data record
                    if (getPUMSRecType(s).equals("H")) {
                        hh = new PUMSHH();

                        for (int i = 0; i < hh.attribs.length; i++) {
                            hh.attribs[i] = (getPUMSHHDataValue(s,
                                    hh.attribLabels[i]));
                        }

                        hh.personTypes = new int[hh.attribs[personsField]];
                        hhid++;

                        // read the person records for the number of persons in the household.
                        perCount = 0;
                        wrkCount = 0;
                        adultCount = 0;

                        for (int i = 0; i < hh.attribs[personsField]; i++) {
                            s = in.readLine();

                            if (!getPUMSRecType(s).equals("P")) {
                                logger.severe(
                                    "Expected P record type on record: " +
                                    recCount + " but got: " +
                                    getPUMSRecType(s) + ".");
                                logger.severe("exiting readData(" + fileName +
                                    ") in PUMSData.");
                                logger.severe("exit (21)");
                                System.exit(21);
                            }

                            age = getPUMSPersDataValue(s, "AGE");
                            school = getPUMSPersDataValue(s, "ENROLL");
                            rlabor = getPUMSPersDataValue(s, "ESR");
                            hours = getPUMSPersDataValue(s, "HOURS");

                            switch (rlabor) {
                            case 0:
                            case 3:
                            case 6:

                                if ((age > 17) && (school >= 2)) {
                                    personType = 3;
                                } else if ((age > 17) && (school < 2)) {
                                    personType = 4;
                                } else if (age <= 5) {
                                    personType = 5;
                                } else if ((age >= 6) && (age <= 15)) {
                                    personType = 6;
                                } else if ((age == 16) || (age == 17)) {
                                    personType = 7;
                                }

                                break;

                            case 1:
                            case 2:
                            case 4:
                            case 5:
                                wrkCount++;

                                if ((age > 17) && (hours >= 35)) {
                                    personType = 1;
                                } else if ((age > 17) && (hours < 35)) {
                                    personType = 2;
                                }

                                break;
                            }

                            perCount++;
                            recCount++;

                            if (personType < 5) {
                                adultCount++;
                            }

                            // set the personType attribute value
                            hh.personTypes[i] = personType;

                            // set the hhNumber
                            hh.setHHNumber(hhid);
                        }
                    } else {
                        logger.severe("Expected H record type on record: " +
                            recCount + " but got: " + getPUMSRecType(s) + ".");
                        logger.severe("exiting readData(" + fileName +
                            ") in PUMSData.");
                        logger.severe("exit (20)");
                        System.exit(20);
                    }

                    // override the number of workers (0 - 3+) HH variable with actual
                    // number of workers from person records.
                    for (int i = 0; i < hh.attribs.length; i++) {
                        if (hh.attribLabels[i].equals("WIF")) {
                            hh.attribs[i] = wrkCount;

                            break;
                        }
                    }

                    // override the number of persons (1 - 9) HH variable with actual
                    // number of person records.
                    if (hh.attribs[personsField] != perCount) {
                        hh.attribs[personsField] = perCount;
                        invalid++;
                    }

                    // only keep households where at least 1 adult is present
                    if (adultCount > 0) {
                        pumsHH[hhCount++] = hh;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe(
                "IO Exception caught reading data dictionary file: " +
                fileName);
            e.printStackTrace();
        }

        logger.severe(
            "person field value different from number of person records in " +
            invalid + " of " + hhCount + " households.");
    }

    public int getPUMSRecCount() {
        return recCount;
    }

    public int getPUMShhCount() {
        return hhCount;
    }

    private String getPUMSRecType(String s) {
        return s.substring(dd.getStartCol(dd.HHAttribs, "RECTYPE"),
            dd.getLastCol(dd.HHAttribs, "RECTYPE"));
    }

    private int getPUMSHHDataValue(String s, String PUMSVariable) {
        String temp = s.substring(dd.getStartCol(dd.HHAttribs, PUMSVariable),
                dd.getLastCol(dd.HHAttribs, PUMSVariable));

        if (temp.equals(" ")) {
            return 0;
        } else {
            return Integer.parseInt(temp);
        }
    }

    private int getPUMSPersDataValue(String s, String PUMSVariable) {
        String temp = s.substring(dd.getStartCol(dd.PersAttribs, PUMSVariable),
                dd.getLastCol(dd.PersAttribs, PUMSVariable));

        if (temp.equals(" ")) {
            return 0;
        } else {
            return Integer.parseInt(temp);
        }
    }

    public void printPUMSDictionary() {
        logger.info("PUMS Houshold Attributes");
        logger.info("------------------------");
        dd.printDictionary(dd.HHAttribs);

        logger.info(" ");
        logger.info(" ");

        logger.info("PUMS Person Attributes");
        logger.info("----------------------");
        dd.printDictionary(dd.PersAttribs);
    }

    // the following main() is used to test the methods implemented in this object.
    public static void main(String[] args) {
        HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap("morpc");
        String output = (String) propertyMap.get("TAZData.file");
        String PUMSFILE = (String) propertyMap.get("PUMSData.file");
        String PUMSDICT = (String) propertyMap.get("PUMSDictionary.file");

        PUMSData pums = new PUMSData(PUMSFILE, PUMSDICT);

        //        pums.printPUMSDictionary ();
        pums.readData(PUMSFILE);
        logger.info(pums.getPUMShhCount() + " households read from " +
            pums.getPUMSRecCount() + " PUMS records.");
    }
}
