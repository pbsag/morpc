package com.pb.morpc.synpop;

import com.pb.common.util.SeededRandom;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.NDimensionalMatrixBalancerDouble;
import com.pb.common.matrix.NDimensionalMatrixDouble;
import com.pb.common.matrix.Vector;

import com.pb.morpc.matrix.MatrixUtil;
import com.pb.morpc.models.ChoiceModelApplication;
import com.pb.morpc.models.ZonalDataManager;
import com.pb.morpc.structures.PersonType;
import com.pb.morpc.synpop.pums2000.PUMSData;
import com.pb.morpc.synpop.pums2000.PUMSHH;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * The SyntheticPopulation class defines an object for a synthetic population.
 *
 * A SyntheticPopulation has:
 *      a joint probability distribution of HHs (hhsize x income x workers)
 *      seed joint distributions by puma from PUMS
 *
 */
public class SyntheticPopulation {
    
	protected static Logger logger = Logger.getLogger("com.pb.morpc.models");
    
    public static final String ZONAL_STUDENT_INPUT_FILE = "\\jim\\projects\\USTUDENT.csv";
    public static final String ZONAL_STUDENT_OUTPUT_FILE = "\\jim\\projects\\zonalStudents.csv";
    
    // field names for zone table data
    public static final String TAZ_FIELD = "taz";
    public static final String HHORIGTAZWALKSEGMENT_FIELD = "origWalkSegment";
    public static final String PUMA_FIELD = "puma";
    public static final String POP_FIELD = "totpop";
    public static final String HHS_FIELD = "hhs_gq";
    public static final String LABORFORCE_FIELD = "labf";
    public static final String AVGINCOME_FIELD = "hhinc";
    public static final String HHID_FIELD = "hh_id";
    public static final String HHTAZID_FIELD = "hh_taz_id";
    public static final String AREATYPE_FIELD = "areatype";
    public static final String INCOME_FIELD = "income";
    public static final String WORKERS_F_FIELD = "workers_f";
    public static final String WORKERS_P_FIELD = "workers_p";
    public static final String STUDENTS_FIELD = "students";
    public static final String NONWORKERS_FIELD = "nonworkers";
    public static final String PRESCHOOL_FIELD = "preschool";
    public static final String SCHOOLPRED_FIELD = "schoolpred";
    public static final String SCHOOLDRIV_FIELD = "schooldriv";
    public static final float INCOME_DISTRIBUTION_FACTOR = 0.5f;
    static final float[] zeroHHSize = {
        0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
    };
    static final float[] zeroWorker = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
    static final float[] zeroIncome = { 0.0f, 0.0f, 0.0f };


    boolean debug = false;
    PUMSData pums;
    TableDataSet zoneTable;
    NDimensionalMatrixDouble[] seed3D;
    NDimensionalMatrixDouble[] seed2D;
    float[][] finalTargets;
    ArrayList[][][][] PUMS4Way;
    ArrayList[][][] PUMS3Way;
    HH[][] zonalHHList;
    int firstZone = 1;
    int lastZone;

    public SyntheticPopulation(PUMSData pums, TableDataSet zoneTable) {

        this.pums = pums;
        this.zoneTable = zoneTable;
        this.finalTargets = new float[zoneTable.getRowCount()][];
    }


    public HH[][] buildPop() {
        lastZone = zoneTable.getRowCount();

        HH[][] zonalHHs = new HH[lastZone + 1][];
        
        sumHouseholdsInTableData();

        seed2D = populate2WaySeedTables();
        seed3D = populate3WaySeedTables();

        int hhsPosition = zoneTable.getColumnPosition(HHS_FIELD);
        int hhincPosition = zoneTable.getColumnPosition(INCOME_FIELD);
        int HHsInZone;
        int hhNumber = 0;

        int[] inputZonalStudents = null;
        
        // read the control totals for number of university students in each zone
        try {
            String zonalStudentsFileName = ZONAL_STUDENT_INPUT_FILE;
            inputZonalStudents = readUnivStudents ( zonalStudentsFileName );
        }
        catch (Exception e) {
            logger.fatal ("Exception reading input zonal students summary file.", e);
            System.exit(1);
        }

        
        
        // open output stream for writing number of students per zone file
        PrintWriter outStream = null;
        try {
            String zonalStudentsFileName = ZONAL_STUDENT_OUTPUT_FILE;
            outStream = new PrintWriter (new BufferedWriter( new FileWriter(zonalStudentsFileName) ) );
            outStream.println ( "taz,controlTotal,synpopTotal,lo-1,md-1,hi-1,lo-2,md-2,hi-2,lo-3,md-3,hi-4p,lo-4p,md-4p,hi-4p");
        }
        catch (IOException e) {
            logger.fatal ("I/O exception opening zonal students summary file.", e);
            System.exit(1);
        }
            
        // create an array to store the number of students in each income
        // category and in each of 1, 2, 3, 4+ household sizes
        int[][] univStudentsInZone = new int[4][zeroIncome.length];
        int univStudentsTotal = 0;
            
        // generate the synthetic population for each zone
        for (int zone = firstZone; zone <= lastZone; zone++) {

            HHsInZone = (int) zoneTable.getValueAt(zone, hhsPosition);

            if (HHsInZone > 0) {
                HH[] hhList = getZonalHHs(zone);

                univStudentsTotal = 0;
                for (int i=0; i < univStudentsInZone.length; i++)
                    Arrays.fill(univStudentsInZone[i],0);
                
                for (int i = 0; i < hhList.length; i++) {
                    
                    int hhinc = PUMSHH.getIncomeCategory( hhList[i].attribs[2] );
                    int hhsz = ( hhList[i].personTypes.length > 3 ? 3 : hhList[i].personTypes.length-1 );
                    
                    // count the number of university students residing in zone
                    for (int j=0; j < hhList[i].personTypes.length; j++) {
                        if ( hhList[i].personTypes[j] == PersonType.STUDENT ) {
                            univStudentsInZone[hhsz][hhinc]++;
                            univStudentsTotal++;
                        }
                    }
                    
                    
                    hhList[i].setHHNumber(hhNumber);
                    hhNumber++;
                }

                zonalHHs[zone] = hhList;
                
            }
            else {
                // TableDataSet values are stored with zero based indexing
                setZonalFinalTargets(zone - 1, new Vector(zeroHHSize),
                    new Vector(zeroWorker), new Vector(zeroIncome));
            }
            
            outStream.print ( zone + "," + inputZonalStudents[zone] + "," + univStudentsTotal );
            for (int i=0; i < univStudentsInZone.length; i++) {
                for (int j=0; j < univStudentsInZone[i].length; j++) {
                    outStream.print ( "," + univStudentsInZone[i][j] );
                }
            }
            outStream.print( "\n" );

        }

        outStream.close();

        return zonalHHs;
    }

    public int getNumberHHs() {
        int hhs = 0;

        for (int zone = firstZone; zone <= lastZone; zone++)
            if (zonalHHList[zone] != null) {
                hhs += zonalHHList[zone].length;
            }

        return hhs;
    }

    private TableDataSet createTargetsTableDataSet() {
        ArrayList headings = new ArrayList();

        headings.add("ZONE");

        for (int i = 0; i < PUMSHH.HHSIZE_Labels.length; i++)
            headings.add("HHSIZE_" + PUMSHH.HHSIZE_Labels[i]);

        for (int i = 0; i < PUMSHH.WORKER_Labels.length; i++)
            headings.add("WORKER_" + PUMSHH.WORKER_Labels[i]);

        for (int i = 0; i < PUMSHH.INCOME_Labels.length; i++)
            headings.add("INCOME_" + PUMSHH.INCOME_Labels[i]);

        TableDataSet tds = TableDataSet.create(this.finalTargets, headings);

        return tds;
    }

    private TableDataSet createHHTableDataSet() {
        ArrayList headings = new ArrayList();

        headings.add(HHID_FIELD);
        headings.add(HHTAZID_FIELD);
        headings.add(HHORIGTAZWALKSEGMENT_FIELD);
        headings.add(INCOME_FIELD);
        headings.add(WORKERS_F_FIELD);
        headings.add(WORKERS_P_FIELD);
        headings.add(STUDENTS_FIELD);
        headings.add(NONWORKERS_FIELD);
        headings.add(PRESCHOOL_FIELD);
        headings.add(SCHOOLPRED_FIELD);
        headings.add(SCHOOLDRIV_FIELD);

        float[][] tableData = new float[getNumberHHs()][headings.size()];

        int hh = 0;

        for (int zone = firstZone; zone <= lastZone; zone++) {
            if (zonalHHList[zone] != null) {
                for (int i = 0; i < zonalHHList[zone].length; i++) {
                    tableData[hh][0] = zonalHHList[zone][i].getHHNumber();
                    tableData[hh][1] = zone;
                    tableData[hh][2] = getInitialOriginWalkSegment (zone);
                    tableData[hh][3] = PUMSHH.getIncomeCategory(zonalHHList[zone][i].attribs[2]) + 1;

                    for (int j = 0; j < zonalHHList[zone][i].personTypes.length; j++)
                        tableData[hh][zonalHHList[zone][i].personTypes[j] + 3]++;

                    hh++;
                }
            }
        }

        TableDataSet tds = TableDataSet.create(tableData, headings);

        return tds;
    }

    
	/**
	 * set walk segment (0-none, 1-short, 2-long walk to transit access) for the origin for this tour
	 */
	private int getInitialOriginWalkSegment (int taz) {
		double[] proportions = new double[ZonalDataManager.WALK_SEGMENTS];
		for (int i=0; i < ZonalDataManager.WALK_SEGMENTS; i++)
			proportions[i] = ZonalDataManager.getWalkPct(i, taz);
		return ChoiceModelApplication.getMonteCarloSelection(proportions);
	}

	
    private HH[] getZonalHHs(int zone) {
        ArrayList labels = null;

        // create a ZonalData object for the zone passed in
        ZonalData zd = populateZonalData(zone);

        // get the marginals (with control average adjustments) for each hhsize and worker dimensions
        Vector HHSizeMarginals = zonalHHSizeMarginals(zd);
        Vector WorkerMarginals = zonalWorkerMarginals(zd, HHSizeMarginals);

        // get the 2D PUMS seed matrix for the puma in which the zone is located
        NDimensionalMatrixDouble seed2d = get2WaySeedTable(zd.getPuma());

        NDimensionalMatrixBalancerDouble mb2 = new NDimensionalMatrixBalancerDouble();
        mb2.setSeed(seed2d);
        mb2.setTarget(HHSizeMarginals, 0);
        mb2.setTarget(WorkerMarginals, 1);

        //        mb2.setTrace(true);
        mb2.balance();

        NDimensionalMatrixDouble mb2Balanced = mb2.getBalancedMatrix();

        HHSizeWorkerLookupTable lookUp = new HHSizeWorkerLookupTable(zd);
        float[][][] incomePercentages = lookUp.getPercentages();
        float[][] hhsizeWorkerPercentages = new float[incomePercentages.length][incomePercentages[0].length];

        // sum total fractions in NDimensionalMatrixDouble mb2Balanced and adjust matrix to sum to 1.0 exactly.
        double totalFractions = mb2Balanced.getSum();
        hhsizeWorkerPercentages[0][0] += (1.0 - totalFractions);

        float[] incomeMarg = new float[incomePercentages[0][0].length];
        int[] loc = new int[2];
        float value;
        float tot = 0.0f;

        for (int i = 0; i < incomePercentages.length; i++) {
            for (int j = 0; j < incomePercentages[0].length; j++) {
                loc[0] = i;
                loc[1] = j;

                for (int k = 0; k < incomePercentages[0][0].length; k++) {
                    hhsizeWorkerPercentages[i][j] = (float) (mb2Balanced.getValue(loc) +
                        ((mb2Balanced.getValue(loc) / totalFractions) * (1.0 -
                        totalFractions)));
                    incomePercentages[i][j][k] *= hhsizeWorkerPercentages[i][j];
                    incomeMarg[k] += incomePercentages[i][j][k];
                }
            }
        }

        Vector IncomeMarginals2 = new Vector(incomeMarg);

        Vector IncomeMarginals1 = zonalIncomeMarginals(zd);

        Vector IncomeMarginals = new Vector(IncomeMarginals1.size());

        for (int i = 1; i <= IncomeMarginals.size(); i++) {
            value = (IncomeMarginals1.getValueAt(i) * (INCOME_DISTRIBUTION_FACTOR)) +
                (IncomeMarginals2.getValueAt(i) * (1.0f -
                INCOME_DISTRIBUTION_FACTOR));
            IncomeMarginals.setValueAt(i, value);
        }

        // get the 3D PUMS seed matrix for the puma in which the zone is located
        NDimensionalMatrixDouble seed = get3WaySeedTable(zd.getPuma());

        if (debug) {
            // print the 3D seed matrix
            labels = new ArrayList(3);
            labels.add(PUMSHH.HHSIZE_Labels);
            labels.add(PUMSHH.WORKER_Labels);
            labels.add(PUMSHH.INCOME_Labels);
            print3WayTable(seed, labels);
        }

        NDimensionalMatrixBalancerDouble mb3 = new NDimensionalMatrixBalancerDouble();

        //		mb3.setTrace(true);
        mb3.setSeed(seed);
        mb3.setTarget(HHSizeMarginals, 0);
        mb3.setTarget(WorkerMarginals, 1);
        mb3.setTarget(IncomeMarginals, 2);

        // save final target marginals in an array to be written out as a TableDataSet later on.
        // TableDataSet values are stored with zero based indexing
        setZonalFinalTargets(zone - 1, HHSizeMarginals, WorkerMarginals,
            IncomeMarginals);

        mb3.balance();

        //get and print the balanced matrix
        NDimensionalMatrixDouble mb3Balanced = mb3.getBalancedMatrix();

        if (debug) {
            print3WayTable(mb3Balanced, labels);
            logger.info("Balanced proportions matrix total = " +
                mb3Balanced.getSum());
            logger.info(" ");
            logger.info(" ");
        }

        totalFractions = mb3Balanced.getSum();

        //get and print the balanced fractional hhs matrix
        NDimensionalMatrixDouble balancedHHFractional = mb3Balanced.matrixMultiply(zd.getHHs());

        if (debug) {
            print3WayTable(balancedHHFractional, labels);
            logger.info("Balanced fractional HHs matrix total = " +
                balancedHHFractional.getSum());
            logger.info(" ");
            logger.info(" ");
        }

        totalFractions = balancedHHFractional.getSum();

        //get and print the balanced discretized hhs matrix
        NDimensionalMatrixDouble balancedHHDiscretized = balancedHHFractional.discretize();

        if (debug) {
            print3WayTable(balancedHHDiscretized, labels);
            logger.info("Balanced discretized HHs matrix total = " +
                balancedHHDiscretized.getSum());
            logger.info(" ");
            logger.info(" ");
        }

        totalFractions = balancedHHDiscretized.getSum();

        // create a household list by taking each household from the balanced
        // discretized matrix and selecting a matching household record from
        // the PUMS data.
        HH[] hhList = matchHHsToPUMS(zd, balancedHHDiscretized);

        return hhList;
    }

    private void setZonalFinalTargets(int zone, Vector HHSizeMarginals,
        Vector WorkerMarginals, Vector IncomeMarginals) {
        this.finalTargets[zone] = new float[1 + HHSizeMarginals.size() +
            WorkerMarginals.size() + IncomeMarginals.size()];

        int hhsPosition = zoneTable.getColumnPosition(HHS_FIELD);
        int HHsInZone = (int) zoneTable.getValueAt(zone + 1, hhsPosition);

        this.finalTargets[zone][0] = zone + 1;

        for (int i = 0; i < HHSizeMarginals.size(); i++)
            this.finalTargets[zone][1 + i] = HHSizeMarginals.getValueAt(i + 1) * HHsInZone;

        for (int i = 0; i < WorkerMarginals.size(); i++)
            this.finalTargets[zone][1 + i + HHSizeMarginals.size()] = WorkerMarginals.getValueAt(i +
                    1) * HHsInZone;

        for (int i = 0; i < IncomeMarginals.size(); i++)
            this.finalTargets[zone][1 + i + HHSizeMarginals.size() +
            WorkerMarginals.size()] = IncomeMarginals.getValueAt(i + 1) * HHsInZone;
    }

    private float[][] getFinalTargets() {
        return this.finalTargets;
    }

    private HH[] matchHHsToPUMS(ZonalData zd, NDimensionalMatrixDouble hhs3D) {
        ArrayList pumsHHs;
        int hhs;
        boolean adjusted = false;
        int[] location = new int[3];
        ArrayList tempHHs = new ArrayList();
        HH[] hhList = null;

        HashMap pumaIndex = pums.getPumaIndex();
        int intPumaIndex = Integer.parseInt((String) pumaIndex.get(
                    Integer.toString(zd.getPuma())));

        // loop over dimensions of thye 3-D matrix
        int[] shape = hhs3D.getShape();

        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    location[0] = i;
                    location[1] = j;
                    location[2] = k;
                    hhs = (int) hhs3D.getValue(location);

                    if (hhs > 0) {
                        adjusted = false;
                        pumsHHs = PUMS4Way[intPumaIndex][i][j][k];

                        while (pumsHHs.size() == 0) {
                            // adjust hhsize downward until a combination is found with PUMS hhs in it.
                            if (i > 0) {
                                i--;

                                // adjust workers downward if hhsize becomes smaller than workers.
                                if ((j > 0) && (i < (j - 1))) {
                                    j--;
                                }

                                // if we're at i==0, j==0, then adjust k downward, but start adjustments over
                                // again with original i,j.
                                if ((i == 0) && (j == 0) && (k > 0)) {
                                    i = location[0];
                                    j = location[1];
                                    k--;
                                }

                                pumsHHs = PUMS4Way[intPumaIndex][i][j][k];
                                adjusted = true;
                            } else {
                                // adjust workers downward if hhsize becomes smaller than workers.
                                if (j > 0) {
                                    j--;
                                }

                                // if we're at i==0, j==0, then adjust k downward, but start adjustments over
                                // again with original i,j.
                                if ((i == 0) && (j == 0) && (k > 0)) {
                                    i = location[0];
                                    j = location[1];
                                    k--;
                                }

                                pumsHHs = PUMS4Way[intPumaIndex][i][j][k];
                                adjusted = true;
                            }
                        }

                        hhList = getHHsFromPUMS(hhs, pumsHHs);

                        for (int m = 0; m < hhList.length; m++) {
                            hhList[m].categoryValues[0] = i;
                            hhList[m].categoryValues[1] = j;
                            hhList[m].categoryValues[2] = k;
                            hhList[m].setAdjusted(adjusted);
                            tempHHs.add(hhList[m]);
                        }

                        i = location[0];
                        j = location[1];
                        k = location[2];
                    }
                }
            }
        }

        hhList = new HH[tempHHs.size()];

        for (int m = 0; m < tempHHs.size(); m++) {
            hhList[m] = (HH) tempHHs.get(m);
            hhList[m].areaType = zd.getAreaType();
        }

        return hhList;
    }

    private HH[] getHHsFromPUMS(int hhCount, ArrayList pumsHHs) {
        int[] randomNumbers = new int[pumsHHs.size()];
        int[] index;
        HH[] hhList = new HH[hhCount];

        int hh = 0;

        while (hh < hhCount) {
            for (int i = 0; i < randomNumbers.length; i++)
                randomNumbers[i] = (int) (1000000000 * SeededRandom.getRandom());

            index = NDimensionalMatrixDouble.indexSort(randomNumbers);

            int last = Math.min(randomNumbers.length, (hhCount - hh));

            for (int i = 0; i < last; i++) {
                hhList[hh] = (HH) (pums.pumsHH[Integer.parseInt((String) pumsHHs.get(
                            index[i]))]).clone();
                hh++;
            }
        }

        return hhList;
    }

    private void sumHouseholdsInTableData() {
        float hhs;

        int hhsPosition = zoneTable.getColumnPosition(HHS_FIELD);

        hhs = 0.0f;

        for (int i = 1; i <= zoneTable.getRowCount(); i++)
            hhs += zoneTable.getValueAt(i, hhsPosition);

        logger.info("");
        logger.info(String.format(
                "Number of data records in zonal data file = %d",
                zoneTable.getRowCount()));
        logger.info(String.format(
                "Number of data fields in zonal data file = %d",
                zoneTable.getColumnCount()));
        logger.info(String.format(
                "HHs read from zonal data file = %.2f", hhs));
        logger.info("");
    }

    private float getRegionalAverageIncome() {
        float income;
        float hhs;
        float totIncome = 0.0f;
        float totHHs = 0.0f;

        int hhsPosition = zoneTable.getColumnPosition(HHS_FIELD);
        int incomePosition = zoneTable.getColumnPosition(AVGINCOME_FIELD);

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            income = zoneTable.getValueAt(i, incomePosition);
            hhs = zoneTable.getValueAt(i, hhsPosition);
            totIncome += (income * hhs);
            totHHs += hhs;
        }

        return totIncome / totHHs;
    }

    private ZonalData populateZonalData(int zone) {
        ZonalData zd = new ZonalData();
        float puma;
        float areaType;
        float avgHHSize;
        float avgWorkers;
        float avgIncome;
        float hhs;
        float pop;
        float laborForce;

        int hhsPosition = zoneTable.getColumnPosition(HHS_FIELD);
        int areaTypePosition = zoneTable.getColumnPosition(AREATYPE_FIELD);
        int popPosition = zoneTable.getColumnPosition(POP_FIELD);
        int laborForcePosition = zoneTable.getColumnPosition(LABORFORCE_FIELD);
        int incomePosition = zoneTable.getColumnPosition(AVGINCOME_FIELD);
        int pumaPosition = zoneTable.getColumnPosition(PUMA_FIELD);

        // assume TableDataSet is indexed by zone
        pop = zoneTable.getValueAt(zone, popPosition);
        areaType = zoneTable.getValueAt(zone, areaTypePosition);
        laborForce = zoneTable.getValueAt(zone, laborForcePosition);
        avgIncome = zoneTable.getValueAt(zone, incomePosition);
        hhs = zoneTable.getValueAt(zone, hhsPosition);
        puma = zoneTable.getValueAt(zone, pumaPosition);

        if (hhs > 0) {
            avgHHSize = pop / hhs;
            avgWorkers = laborForce / hhs;
        } else {
            avgHHSize = 0.0f;
            avgWorkers = 0.0f;
        }

        zd.setAvgHHSize(avgHHSize);
        zd.setAvgWorkers(avgWorkers);
        zd.setAvgIncome(avgIncome);
        zd.setHHs(hhs);
        zd.setPuma(puma);
        zd.setAreaType(areaType);

        return zd;
    }

    private Vector zonalHHSizeMarginals(ZonalData zd) {
        Vector marginals;

        // get the marginal distributions of HHs for the zone by applying percentage curves
        marginals = getHHSizeMarginals(zd);

        if (debug) {
            // print mean household size control value
            logger.info(String.format(
                    "\n\nMean Household Size Control Value = %10.5f",
                    zd.getAvgHHSize()));

            // list the contents of the returned marginal proportions before adjustment
            printMarginalProportions("Household Size Marginals before adjustment:",
                marginals.copyValues1D(), PUMSHH.HHSIZE_Categories,
                PUMSHH.HHSIZE_Labels);
        }

        // adjust the marginal distributions returned from the percentage curves
        // such that the mean of the distributions equals the known mean.
        marginals = MatrixUtil.adjustAverageToControl(marginals,
                new Vector(PUMSHH.HHSIZE_Categories), zd.getAvgHHSize());

        if (debug) {
            // list the contents of the marginal proportions after adjustment
            printMarginalProportions("Household Size Marginals after adjustment:",
                marginals.copyValues1D(), PUMSHH.HHSIZE_Categories,
                PUMSHH.HHSIZE_Labels);
        }

        return marginals;
    }

    private Vector zonalWorkerMarginals(ZonalData zd, Vector HHSizeMarginals) {
        Vector marginals;

        // get the marginal distributions of HHs for the zone by applying percentage curves
        marginals = getWorkerMarginals(zd, HHSizeMarginals);

        if (debug) {
            // print mean household size control value
            logger.info(String.format(
                    "\n\nMean Workers Control Value = %10.5f",
                    zd.getAvgWorkers()));

            // list the contents of the returned marginal proportions before adjustment
            printMarginalProportions("Workers Marginals before adjustment:",
                marginals.copyValues1D(), PUMSHH.WORKER_Categories,
                PUMSHH.WORKER_Labels);
        }

        // adjust the marginal distributions returned from the percentage curves
        // such that the mean of the distributions equals the known mean.
        marginals = MatrixUtil.adjustAverageToControl(marginals,
                new Vector(PUMSHH.WORKER_Categories), zd.getAvgWorkers());

        if (debug) {
            // list the contents of the marginal proportions after adjustment
            printMarginalProportions("Household Size Marginals after adjustment:",
                marginals.copyValues1D(), PUMSHH.WORKER_Categories,
                PUMSHH.WORKER_Labels);
        }

        return marginals;
    }

    private Vector zonalIncomeMarginals(ZonalData zd) {
        Vector marginals;

        // get the marginal distributions of HHs for the zone by applying percentage curves
        marginals = getIncomeMarginals(zd);

        if (debug) {
            // print mean household size control value
            logger.info(String.format(
                    "\n\nMean Income Control Value = %10.5f", zd.getAvgIncome()));

            // list the contents of the returned marginal proportions before adjustment
            printMarginalProportions("Income Marginals (no adjustment):",
                marginals.copyValues1D(), PUMSHH.INCOME_Categories,
                PUMSHH.INCOME_Labels);
        }

        return marginals;
    }

    private void printMarginalProportions(String title, float[] proportions,
        float[] categories, String[] labels) {
        double mean;
        double sum;

        // list the contents of the marginal proportions array
        logger.info(String.format("%s", title));
        mean = 0.0;
        sum = 0.0;

        for (int i = 0; i < proportions.length; i++) {
            logger.info(String.format("%-6s", labels[i]));
            logger.info(String.format(" %10.3f", proportions[i]));
            sum += proportions[i];
            mean += (proportions[i] * categories[i]);
        }

        mean /= sum;
        logger.info(String.format("%-6s", "Total"));
        logger.info(String.format(" %10.3f", sum));
        logger.info(String.format("Mean Category Value = %10.5f", mean));
        logger.info("");
        logger.info("");
    }

    private Vector getHHSizeMarginals(ZonalData zd) {
        HHSizeCurve pctCurve = new HHSizeCurve(zd);
        float[] args = { zd.getAvgHHSize() };
        float[] baseProps = pctCurve.getPercentages(args);
        float[] extProps = pctCurve.extendPercentages();
        float[] finalProps = new float[PUMSHH.HHSIZES];
        double[] dProps = new double[PUMSHH.HHSIZES];

        for (int i = 0; i < (PUMSHH.HHSIZES_BASE - 1); i++)
            finalProps[i] = baseProps[i];

        for (int i = PUMSHH.HHSIZES_BASE - 1; i < PUMSHH.HHSIZES; i++)
            finalProps[i] = baseProps[PUMSHH.HHSIZES_BASE - 1] * extProps[i -
                (PUMSHH.HHSIZES_BASE - 1)];

        // apply truncation and scaling to dProps[] to get values to sum exactly to 1.0;
        double propTot = 0.0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] = Math.max(0.00001, Math.min(1.0f, finalProps[i]));
            propTot += dProps[i];
        }

        double maxPct = -99999999.9;
        int maxPctIndex = 0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] /= propTot;

            if (dProps[i] > maxPct) {
                maxPct = dProps[i];
                maxPctIndex = i;
            }
        }

        // calculate the percentage for the maximum index percentage curve from the
        // residual difference.
        double residual = 0.0;

        for (int i = 0; i < finalProps.length; i++)
            if (i != maxPctIndex) {
                residual += dProps[i];
            }

        dProps[maxPctIndex] = 1.0 - residual;

        for (int i = 0; i < finalProps.length; i++)
            finalProps[i] = (float) dProps[i];

        return (new Vector(finalProps));
    }

    private Vector getWorkerMarginals(ZonalData zd, Vector HHSizeMarginals) {
        HHWorkerCurve pctCurve = new HHWorkerCurve(zd);
        float[] args = {
            zd.getAvgWorkers(), HHSizeMarginals.getValueAt(1),
            HHSizeMarginals.getValueAt(2)
        };
        float[] baseProps = pctCurve.getPercentages(args);
        float[] extProps = pctCurve.extendPercentages();
        float[] finalProps = new float[PUMSHH.WORKERS];
        double[] dProps = new double[PUMSHH.WORKERS];

        for (int i = 0; i < (PUMSHH.WORKERS_BASE - 1); i++)
            finalProps[i] = baseProps[i];

        for (int i = PUMSHH.WORKERS_BASE - 1; i < PUMSHH.WORKERS; i++)
            finalProps[i] = baseProps[PUMSHH.WORKERS_BASE - 1] * extProps[i -
                (PUMSHH.WORKERS_BASE - 1)];

        // apply truncation and scaling to dProps[] to get values to sum exactly to 1.0;
        double propTot = 0.0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] = Math.max(0.0000001, Math.min(1.0f, finalProps[i]));
            propTot += dProps[i];
        }

        double maxPct = -99999999.9;
        int maxPctIndex = 0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] /= propTot;

            if (dProps[i] > maxPct) {
                maxPct = dProps[i];
                maxPctIndex = i;
            }
        }

        // calculate the percentage for the maximum index percentage curve from the
        // residual difference.
        double residual = 0.0;

        for (int i = 0; i < finalProps.length; i++)
            if (i != maxPctIndex) {
                residual += dProps[i];
            }

        dProps[maxPctIndex] = 1.0 - residual;

        for (int i = 0; i < finalProps.length; i++)
            finalProps[i] = (float) dProps[i];

        return (new Vector(finalProps));
    }

    private Vector getIncomeMarginals(ZonalData zd) {
        HHIncomeCurve pctCurve = new HHIncomeCurve(zd);
        float[] args = { zd.getAvgIncome() / getRegionalAverageIncome() };
        float[] baseProps = pctCurve.getPercentages(args);
        float[] extProps = pctCurve.extendPercentages();
        float[] finalProps = new float[PUMSHH.INCOMES];
        double[] dProps = new double[PUMSHH.INCOMES];

        for (int i = 0; i < (PUMSHH.INCOMES_BASE - 1); i++)
            finalProps[i] = baseProps[i];

        for (int i = PUMSHH.INCOMES_BASE - 1; i < PUMSHH.INCOMES; i++)
            finalProps[i] = baseProps[PUMSHH.INCOMES_BASE - 1] * extProps[i -
                (PUMSHH.INCOMES_BASE - 1)];

        // apply truncation and scaling to dProps[] to get values to sum exactly to 1.0;
        double propTot = 0.0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] = Math.max(0.0000001, Math.min(1.0f, finalProps[i]));
            propTot += dProps[i];
        }

        double maxPct = -99999999.9;
        int maxPctIndex = 0;

        for (int i = 0; i < finalProps.length; i++) {
            dProps[i] /= propTot;

            if (dProps[i] > maxPct) {
                maxPct = dProps[i];
                maxPctIndex = i;
            }
        }

        // calculate the percentage for the maximum index percentage curve from the
        // residual difference.
        double residual = 0.0;

        for (int i = 0; i < finalProps.length; i++)
            if (i != maxPctIndex) {
                residual += dProps[i];
            }

        dProps[maxPctIndex] = 1.0 - residual;

        for (int i = 0; i < finalProps.length; i++)
            finalProps[i] = (float) dProps[i];

        return (new Vector(finalProps));
    }

    private NDimensionalMatrixDouble get3WaySeedTable(int puma) {
        HashMap pumaIndex = pums.getPumaIndex();
        int intPumaIndex = Integer.parseInt((String) pumaIndex.get(
                    Integer.toString(puma)));

        return seed3D[intPumaIndex];
    }

    private NDimensionalMatrixDouble get2WaySeedTable(int puma) {
        HashMap pumaIndex = pums.getPumaIndex();
        int intPumaIndex = Integer.parseInt((String) pumaIndex.get(
                    Integer.toString(puma)));

        return seed2D[intPumaIndex];
    }

    private NDimensionalMatrixDouble[] populate3WaySeedTables() {
        int[] matrixShape = { PUMSHH.HHSIZES, PUMSHH.WORKERS, PUMSHH.INCOMES };
        int[] cellCoords = new int[matrixShape.length];
        int matrixDimensions = matrixShape.length;

        int intPumaIndex;
        int maxPuma;
        int numPumas;
        int puma;
        int hhsize;
        int income;
        int workers;
        double weight;
        double newValue;

        String key;

        // get the maximum puma number in the PUMS data
        maxPuma = pums.getMaxPuma();
        logger.info("Largest PUMA value = " + maxPuma);

        // get the number of pumas in the PUMS data
        numPumas = pums.getNumberPumas();
        logger.info("Number of unique PUMA values = " + numPumas);

        // create an array of ArrayList to hold PUMS household record numbers associated
        // with each puma, HHSize, Workers, and Income categories.
        ArrayList[][][][] hhs = new ArrayList[numPumas][PUMSHH.HHSIZES][PUMSHH.WORKERS][PUMSHH.INCOMES];

        for (int i = 0; i < numPumas; i++)
            for (int j = 0; j < PUMSHH.HHSIZES; j++)
                for (int k = 0; k < PUMSHH.WORKERS; k++)
                    for (int m = 0; m < PUMSHH.INCOMES; m++)
                        hhs[i][j][k][m] = new ArrayList();

        NDimensionalMatrixDouble[] seed3D = new NDimensionalMatrixDouble[numPumas];
        HashMap indexPuma = pums.getIndexPuma();
        HashMap pumaIndex = pums.getPumaIndex();

        // create a 3-D matrix object for each puma and populate the matrices from
        // the corresponding HHs in the puma.
        for (int i = 0; i < numPumas; i++) {
            seed3D[i] = new NDimensionalMatrixDouble("Puma " + i +
                    " Household Frequency", matrixDimensions, matrixShape);
        }

        // go through the list of PUMS households and accumulate weights in 3-way table.
        for (int hh = 0; hh < pums.getPUMShhCount(); hh++) {
            hhsize = pums.pumsHH[hh].attribs[pums.getAttribIndex("PERSONS")];
            workers = pums.pumsHH[hh].attribs[pums.getAttribIndex("WIF")];
            income = pums.pumsHH[hh].attribs[pums.getAttribIndex("HINC")];

            hhsize = PUMSHH.getHHSizeCategory(hhsize);
            workers = PUMSHH.getWorkerCategory(workers);
            income = PUMSHH.getIncomeCategory(income);

            puma = pums.pumsHH[hh].attribs[pums.getAttribIndex("PUMA")];
            weight = pums.pumsHH[hh].attribs[pums.getAttribIndex("HWEIGHT")];

            cellCoords[0] = hhsize;
            cellCoords[1] = workers;
            cellCoords[2] = income;

            intPumaIndex = Integer.parseInt((String) pumaIndex.get(
                        Integer.toString(puma)));
            newValue = weight + seed3D[intPumaIndex].getValue(cellCoords);
            seed3D[intPumaIndex].setValue(newValue, cellCoords);

            // accumulate hh record indices for later reference by geog and 3-Way cross-class.
            hhs[intPumaIndex][hhsize][workers][income].add(Integer.toString(hh));
        }

        for (int i = 0; i < numPumas; i++) {
            for (int j = 0; j < PUMSHH.HHSIZES; j++) {
                for (int k = 0; k < PUMSHH.WORKERS; k++) {
                    for (int m = 0; m < PUMSHH.INCOMES; m++) {
                        cellCoords[0] = j;
                        cellCoords[1] = k;
                        cellCoords[2] = m;

                        if (k > (j + 1)) {
                            seed3D[i].setValue(0.0f, cellCoords);
                        } else if (seed3D[i].getValue(cellCoords) == 0.0) {
                            seed3D[i].setValue(0.00001f, cellCoords);
                        }
                    }
                }
            }
        }

        // set the 4-way table fpr this object to the array just computed.
        setPUMS4Way(hhs);

        return seed3D;
    }

    private NDimensionalMatrixDouble[] populate2WaySeedTables() {
        int[] matrixShape = { PUMSHH.HHSIZES, PUMSHH.WORKERS };
        int[] cellCoords = new int[matrixShape.length];
        int matrixDimensions = matrixShape.length;

        int intPumaIndex;
        int maxPuma;
        int numPumas;
        int puma;
        int hhsize;
        int workers;
        double weight;
        double newValue;

        String key;

        // get the maximum puma number in the PUMS data
        maxPuma = pums.getMaxPuma();
        logger.info("Largest PUMA value = " + maxPuma);

        // get the number of pumas in the PUMS data
        numPumas = pums.getNumberPumas();

        // create an array of ArrayList to hold PUMS household record numbers associated
        // with each puma, HHSize, Workers, and Income categories.
        ArrayList[][][] hhs = new ArrayList[numPumas][PUMSHH.HHSIZES_BASE][PUMSHH.WORKERS_BASE];

        for (int i = 0; i < numPumas; i++)
            for (int j = 0; j < PUMSHH.HHSIZES_BASE; j++)
                for (int k = 0; k < PUMSHH.WORKERS_BASE; k++)
                    hhs[i][j][k] = new ArrayList();

        NDimensionalMatrixDouble[] seed2D = new NDimensionalMatrixDouble[numPumas];
        HashMap indexPuma = pums.getIndexPuma();
        HashMap pumaIndex = pums.getPumaIndex();

        // create a 3-D matrix object for each puma and populate the matrices from
        // the corresponding HHs in the puma.
        for (int i = 0; i < numPumas; i++) {
            seed2D[i] = new NDimensionalMatrixDouble("Puma " + i +
                    " Household Frequency", matrixDimensions, matrixShape);
        }

        // go through the list of PUMS households and accumulate weights in 2-way table.
        for (int hh = 0; hh < pums.getPUMShhCount(); hh++) {
            hhsize = pums.pumsHH[hh].attribs[pums.getAttribIndex("PERSONS")];
            workers = pums.pumsHH[hh].attribs[pums.getAttribIndex("WIF")];

            hhsize = PUMSHH.getHHSizeCategory(hhsize);

            if (hhsize > (PUMSHH.HHSIZES_BASE - 1)) {
                hhsize = (PUMSHH.HHSIZES_BASE - 1);
            }

            workers = PUMSHH.getWorkerCategory(workers);

            if (workers > (PUMSHH.WORKERS_BASE - 1)) {
                workers = (PUMSHH.WORKERS_BASE - 1);
            }

            puma = pums.pumsHH[hh].attribs[pums.getAttribIndex("PUMA")];
            weight = pums.pumsHH[hh].attribs[pums.getAttribIndex("HWEIGHT")];

            cellCoords[0] = hhsize;
            cellCoords[1] = workers;

            intPumaIndex = Integer.parseInt((String) pumaIndex.get(
                        Integer.toString(puma)));
            newValue = weight + seed2D[intPumaIndex].getValue(cellCoords);
            seed2D[intPumaIndex].setValue(newValue, cellCoords);

            // accumulate hh record indices for later reference by geog and 3-Way cross-class.
            hhs[intPumaIndex][hhsize][workers].add(Integer.toString(hh));
        }

        for (int i = 0; i < numPumas; i++) {
            for (int j = 0; j < PUMSHH.HHSIZES_BASE; j++) {
                for (int k = 0; k < PUMSHH.WORKERS_BASE; k++) {
                    cellCoords[0] = j;
                    cellCoords[1] = k;

                    if (k > (j + 1)) {
                        seed2D[i].setValue(0.0f, cellCoords);
                    } else if (seed2D[i].getValue(cellCoords) == 0.0) {
                        seed2D[i].setValue(0.00001f, cellCoords);
                    }
                }
            }
        }

        // set the 3-way table for this object to the array just computed.
        setPUMS3Way(hhs);

        return seed2D;
    }

    public void print3WaySeedTotals() {
        HashMap indexPuma = pums.getIndexPuma();

        for (int puma = 0; puma < pums.getNumberPumas(); puma++)
            logger.info("total for PUMA " +
                indexPuma.get(Integer.toString(puma)) + " = " +
                seed3D[puma].getSum());
    }

    public void print3WayTable(NDimensionalMatrixDouble table, ArrayList labels) {
        String[] s0 = (String[]) labels.get(0);
        String[] s1 = (String[]) labels.get(1);
        String[] s2 = (String[]) labels.get(2);
        int[] loc = new int[3];
        float[] rtots = new float[s1.length];
        float[] ctots = new float[s2.length];
        double value;
        double tot;

        for (int i = 0; i < s0.length; i++) {
            logger.info("Household Size = " + s0[i]);

            logger.info(String.format("%-15s", "Income"));

            for (int k = 0; k < s2.length; k++)
                logger.info(String.format("%15s", s2[k]));

            logger.info(String.format("%15s", "Total"));

            logger.info(String.format("%-15s", "Workers"));

            tot = 0.0f;

            for (int j = 0; j < s1.length; j++)
                rtots[j] = 0.0f;

            for (int k = 0; k < s2.length; k++)
                ctots[k] = 0.0f;

            for (int j = 0; j < s1.length; j++) {
                logger.info(String.format("%-15s", s1[j]));

                for (int k = 0; k < s2.length; k++) {
                    loc[0] = i;
                    loc[1] = j;
                    loc[2] = k;
                    value = table.getValue(loc);
                    logger.info(String.format("%15.4f", value));
                    rtots[j] += value;
                    ctots[k] += value;
                    tot += value;
                }

                logger.info(String.format("%15.4f", rtots[j]));
            }

            logger.info(String.format("%-15s", "Total"));

            for (int k = 0; k < s2.length; k++)
                logger.info(String.format("%15.4f", ctots[k]));

            logger.info(String.format("%15.4f", tot));
            logger.info("");
            logger.info("");
        }
    }

    private void setPUMS4Way(ArrayList[][][][] a) {
        this.PUMS4Way = a;
    }

    private void setPUMS3Way(ArrayList[][][] a) {
        this.PUMS3Way = a;
    }

    // the following main() is used to test the methods implemented in this object.
    public void runSynPop(String OUTPUT_HHFILE, String ZONAL_TARGETS_HHFILE) {
        zonalHHList = buildPop();

        // write households to output file
        TableDataSet hhTableDataSet = createHHTableDataSet();

        try {
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(hhTableDataSet, new File(OUTPUT_HHFILE), new DecimalFormat("#.00"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // write zonal targets to output file
        TableDataSet targetsDataSet = createTargetsTableDataSet();

        if (ZONAL_TARGETS_HHFILE != null) {
            try {
                CSVFileWriter writer = new CSVFileWriter();
                writer.writeFile(targetsDataSet, new File(ZONAL_TARGETS_HHFILE), new DecimalFormat("#.00"));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }


        for (int i = 3; i <= 10; i++)
            TableDataSet.logColumnFreqReport("Synthetic Households",
                hhTableDataSet, i);

    }



    private int[] readUnivStudents ( String fileName ) {
        
        // read the csv file into a TableDataSet
        CSVFileReader reader = new CSVFileReader();
        
        TableDataSet table = null;
        try {
            table = reader.readFile(new File( fileName ));
        } catch (IOException e) {
            logger.error ("problem reading zonal university students count into TableDataSet", e);
        }

        // determine the max zone number from the 1st column of TableDataSet
        int maxZone = 0;
        for (int r=0; r < table.getRowCount(); r++) {
            int zone = (int)table.getValueAt(r+1, 1);
            if ( zone > maxZone )
                maxZone = zone;
        }
        
        // put values from 2nd column in TableDataSet into array to return
        int[] values = new int[maxZone+1];

        for (int r=0; r < table.getRowCount(); r++) {
            int zone = (int)table.getValueAt(r+1, 1);
            int value = (int)table.getValueAt(r+1, 2);
            values[zone] = value;
        }
        
        return values;
        
    }

}
