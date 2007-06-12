package com.pb.morpc.models;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import com.pb.morpc.structures.*;

import java.io.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 * @author Jim Hicks
 *
 * Class for managing zonal data.
 * e.g. size variables, zonal TableDataSet, attractions model, stop density model.
 */
public class ZonalDataManager implements java.io.Serializable {

    // this value indicates that no more than 4 tasks will ever be run in a single VM.
    // The current implementation uses at most 2.
    // The way to check this value is to check the daf application properties file
    // where tasks are assigned to processors, and make sure that no more tasks than
    // this number are assigned to a daf node. 
	public static final int MAX_DISTRIBUTED_PROCESSORES = 4;

    public static final int WALK_SEGMENTS = 3;
    public static final int INCOME_CATEGORIES = 3;
    public static final String[] adjPurpLabels = { "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk" };

	
    protected static Logger logger = Logger.getLogger("com.pb.morpc.models");
    private TableDataSet zoneTable;



    // VERY IMPORTANT !!!
    // if any public static data members get added to this class,
    // then they must be added to the method ( createNonStaticData() )
    // which copies all public static data members into a HashMap for message passing.
    public static float[][] walkPctArray;
    public static float[][][] sizeFinal;
	public static float[] parkRate;
	public static float[] urbType;
	public static float[] cnty;
	public static float[] schdist;
	public static float[] suprdist;
	public static float[] rings;
	public static float[] cbdatype;
	public static float[] parktot;
	public static float[] parklng;
	public static float[] propfree;
	public static float[] parkrate;
	public static float[] zonalShortAccess;
	public static float[] zonalAreaType;
	public static float[] zonal_nonw_au_op;
	public static float[] zonal_nonw_walk;
	public static int numberOfZones;
	public static double[][] odUtilModeAlt;
	public static float[][] logsumDcAMPM;
	public static float[][] logsumDcAMMD;
	public static float[][] logsumDcMDMD;
	public static float[][] logsumDcPMNT;
	// see VERY IMPORTANT above !!!



	int numberOfSubzones;
    private Vector tables = new Vector();
    private HashMap propertyMap;
    private float[][] attractions;
    private float[][][] sizeOriginal;
    private float[][][] sizeOriginalAdj;
    private float[][][] sizeBalance;
    private float[][][] sizeScaled;
    private float[][][] sizePrevious;
    private float[][][] shadowPrice;
	private float[][][] attrs;
    private float[][][] prods;
	
    private float[][][] totProds;
    private float[][][] totAttrs;





    public ZonalDataManager(HashMap propertyMap) {
        
        this.propertyMap = propertyMap;
        
        
        // JH, 07/25/2006: create the derived table (formerly done externally with an SPSS script)
        Derived dr = new Derived(propertyMap);
        dr.writeDerivedTable();
        
        
        // build the zonal data table
        String zonalFile1 = (String) propertyMap.get("TAZMainData.file");
        String zonalFile2 = (String) propertyMap.get("TAZAccessibility.file");
        String zonalFile3 = (String) propertyMap.get("TAZDerived.file");
        String zonalFile4 = (String) propertyMap.get("TAZEquivalency.file");
        String zonalCombined = (String) propertyMap.get("TAZData.file");

        try {
            CSVFileReader reader = new CSVFileReader();

            tables.add(reader.readFile(new File(zonalFile1)));
            tables.add(reader.readFile(new File(zonalFile2)));
            tables.add(reader.readFile(new File(zonalFile3)));
            tables.add(reader.readFile(new File(zonalFile4)));

            zoneTable = setZoneTable(tables);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(zoneTable, new File(zonalCombined));
        } catch (Exception e) {
            logger.error("can not open or save combined zonal file");
        }

        /*
         * this code was used previously to read the single combined zonal file.
         *
                        try{
                                zoneTable = new TableDataSet(new File(zonalCombined));
                        }catch(Exception e){
                                e.printStackTrace();
                                System.exit(1);
                        }
        */
        getZoneRelatedData(zoneTable);


        
        // size variables referenced by Household object acting as DMU object with
        // getXxx() and getXxxAlt(alt) methods invoked by UEC objects
        sizeFinal = new float[TourType.TYPES + 1][][];

        // original copies of size variables before any adjustments
        sizeOriginal = new float[TourType.TYPES + 1][][];
        
        // original copies of size variables after adjustments, before scaling
        sizeOriginalAdj = new float[TourType.TYPES + 1][][];
        
        // copies of size variables after adjustments, used in balancing calculations
        sizeBalance = new float[TourType.TYPES + 1][][];

        // final adjusted, balanced size variables resulting from balancing calculations
        sizeScaled = new float[TourType.TYPES + 1][][];

        // copy of final adjusted, balanced size variables used in shadow pricing adjustments
        sizePrevious = new float[TourType.TYPES + 1][][];

        // shadow prices
        shadowPrice = new float[TourType.TYPES + 1][][];

        // tour productions and attractions resulting from DC model
        attrs = new float[TourType.TYPES + 1][][];
        prods = new float[TourType.TYPES + 1][][];
        
        // total prods and attrs are maintained by with/without override adjustments, purpose, and income categories for work purpose.
        totProds = new float[2][TourType.TYPES + 1][];
        totAttrs = new float[2][TourType.TYPES + 1][];

        
        // Allocate size array to hold original subzone size variables by type.
        // TourType.WORK has 3 categories of subzone size values, 1 for each income category.
        // Other purposes have only 1 category of subzone size values.
        for (int i=1; i <= TourType.TYPES; i++) {
            if ( i == TourType.WORK ) {
                sizeFinal[i] = new float[INCOME_CATEGORIES][];
                sizeOriginal[i] = new float[INCOME_CATEGORIES][];
                sizeOriginalAdj[i] = new float[INCOME_CATEGORIES][];
                sizeBalance[i] = new float[INCOME_CATEGORIES][];
                sizeScaled[i] = new float[INCOME_CATEGORIES][];
                sizePrevious[i] = new float[INCOME_CATEGORIES][];
                shadowPrice[i] = new float[INCOME_CATEGORIES][];
                attrs[i] = new float[INCOME_CATEGORIES][];
                prods[i] = new float[INCOME_CATEGORIES][];
                totProds[0][i] = new float[INCOME_CATEGORIES];
                totProds[1][i] = new float[INCOME_CATEGORIES];
                totAttrs[0][i] = new float[INCOME_CATEGORIES];
                totAttrs[1][i] = new float[INCOME_CATEGORIES];
            }
            else {
                sizeFinal[i] = new float[1][];
                sizeOriginal[i] = new float[1][];
                sizeOriginalAdj[i] = new float[1][];
                sizeBalance[i] = new float[1][];
                sizeScaled[i] = new float[1][];
                sizePrevious[i] = new float[1][];
                shadowPrice[i] = new float[1][];
                attrs[i] = new float[1][];
                prods[i] = new float[1][];
                
                totProds[0][i] = new float[1];
                totProds[1][i] = new float[1];
                totAttrs[0][i] = new float[1];
                totAttrs[1][i] = new float[1];
            }
        }


        
        numberOfZones = zoneTable.getRowCount();
        numberOfSubzones = numberOfZones * WALK_SEGMENTS;

        // calculate the destination choice size terms for each of the purposes
        for (int i = 1; i <= TourType.TYPES; i++) {
            calculateSize(i);
        }

		logsumDcAMPM = new float[MAX_DISTRIBUTED_PROCESSORES][numberOfSubzones + 1];
		logsumDcAMMD = new float[MAX_DISTRIBUTED_PROCESSORES][numberOfSubzones + 1];
		logsumDcMDMD = new float[MAX_DISTRIBUTED_PROCESSORES][numberOfSubzones + 1];
		logsumDcPMNT = new float[MAX_DISTRIBUTED_PROCESSORES][numberOfSubzones + 1];

		odUtilModeAlt = new double[MAX_DISTRIBUTED_PROCESSORES][];
    }

    private void getZoneRelatedData(TableDataSet zoneTable) {
        int k;

        int parkRateFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.PARKRATE);

        if (parkRateFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.PARKRATE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        parkRate = new float[zoneTable.getRowCount() + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++)
            parkRate[i] = zoneTable.getValueAt(i, parkRateFieldPosition);

        int urbtypeFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.URBTYPE);

        if (urbtypeFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.URBTYPE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        k = 1;

        float uType;
        urbType = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            uType = zoneTable.getValueAt(i, urbtypeFieldPosition);

            for (int j = 1; j <= 3; j++) {
                urbType[k] = uType;
                k++;
            }
        }

        int cbdatypeFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.CBDATYPE);

        if (cbdatypeFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.CBDATYPE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        k = 1;

        float aType;
        cbdatype = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            aType = zoneTable.getValueAt(i, cbdatypeFieldPosition);

            for (int j = 1; j <= 3; j++) {
                cbdatype[k] = aType;
                k++;
            }
        }

        int parktotFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.PARKTOT);

        if (parktotFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.PARKTOT +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        k = 1;

        float ptot;
        parktot = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            ptot = zoneTable.getValueAt(i, parktotFieldPosition);

            for (int j = 1; j <= 3; j++) {
                parktot[k] = ptot;
                k++;
            }
        }

        int parklngFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.PARKLNG);

        if (parklngFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.PARKLNG +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        k = 1;

        float plng;
        parklng = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            plng = zoneTable.getValueAt(i, parklngFieldPosition);

            for (int j = 1; j <= 3; j++) {
                parklng[k] = plng;
                k++;
            }
        }

        int propfreeFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.PROPFREE);

        if (propfreeFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.PROPFREE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        k = 1;

        float pfree;
        propfree = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            pfree = zoneTable.getValueAt(i, propfreeFieldPosition);

            for (int j = 1; j <= 3; j++) {
                propfree[k] = pfree;
                k++;
            }
        }

        int parkrateFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.PARKRATE);

        if (parkrateFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.PARKRATE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        k = 1;

        float prate;
        parkrate = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            prate = zoneTable.getValueAt(i, parkrateFieldPosition);

            for (int j = 1; j <= 3; j++) {
                parkrate[k] = prate;
                k++;
            }
        }

        int areatypeFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.AREATYPE);

        if (areatypeFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.AREATYPE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        zonalAreaType = new float[zoneTable.getRowCount() + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            zonalAreaType[i] = zoneTable.getValueAt(i, areatypeFieldPosition);
        }

        int nonw_au_op_FieldPosition = zoneTable.getColumnPosition(ZoneTableFields.NONWORKAUOP);

        if (nonw_au_op_FieldPosition <= 0) {
            logger.fatal(ZoneTableFields.NONWORKAUOP +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        zonal_nonw_au_op = new float[zoneTable.getRowCount() + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            zonal_nonw_au_op[i] = zoneTable.getValueAt(i,
                    nonw_au_op_FieldPosition);
        }

        int zonal_nonw_walk_FieldPosition = zoneTable.getColumnPosition(ZoneTableFields.NONWORKWALK);

        if (zonal_nonw_walk_FieldPosition <= 0) {
            logger.fatal(ZoneTableFields.NONWORKWALK +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        zonal_nonw_walk = new float[zoneTable.getRowCount() + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            zonal_nonw_walk[i] = zoneTable.getValueAt(i,
                    zonal_nonw_walk_FieldPosition);
        }

        int countyFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.COUNTY);

        if (countyFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.COUNTY +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        k = 1;

        float co;
        cnty = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            co = zoneTable.getValueAt(i, countyFieldPosition);

            for (int j = 1; j <= 3; j++) {
                cnty[k] = co;
                k++;
            }
        }

		int ringsPosition = zoneTable.getColumnPosition(ZoneTableFields.RINGS);

		if (ringsPosition <= 0) {
			logger.fatal(ZoneTableFields.RINGS +
				" was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		k = 1;

		float r;
		rings = new float[(3 * zoneTable.getRowCount()) + 1];

		for (int i = 1; i <= zoneTable.getRowCount(); i++) {
			r = zoneTable.getValueAt(i, ringsPosition);

			for (int j = 1; j <= 3; j++) {
				rings[k] = r;
				k++;
			}
		}
        
		int suprdistPosition = zoneTable.getColumnPosition(ZoneTableFields.SUPRDIST);

		if (suprdistPosition <= 0) {
			logger.fatal(ZoneTableFields.SUPRDIST +
				" was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

		k = 1;

		float sup;
		suprdist = new float[(3 * zoneTable.getRowCount()) + 1];

		for (int i = 1; i <= zoneTable.getRowCount(); i++) {
			sup = zoneTable.getValueAt(i, suprdistPosition);

			for (int j = 1; j <= 3; j++) {
				suprdist[k] = sup;
				k++;
			}
		}
        
		int schdistFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.SCHDIST);

		if (schdistFieldPosition <= 0) {
			logger.fatal(ZoneTableFields.SCHDIST +
				" was not a field in the zoneData TableDataSet.");
			System.exit(1);
		}

        k = 1;

        float sd;
        schdist = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            sd = zoneTable.getValueAt(i, schdistFieldPosition);

            for (int j = 1; j <= 3; j++) {
                schdist[k] = sd;
                k++;
            }
        }

        // get file names from properties file
        String walkAccessFile = (String) propertyMap.get("WalkAccess.file");

        int taz;
        float waShrt;
        float waLong;
        float[] shrtArray = new float[zoneTable.getRowCount() + 1];
        float[] longArray = new float[zoneTable.getRowCount() + 1];
        walkPctArray = new float[3][zoneTable.getRowCount() + 1];
        Arrays.fill(walkPctArray[0], 1.0f);
        Arrays.fill(walkPctArray[1], 0.0f);
        Arrays.fill(walkPctArray[2], 0.0f);

        if (walkAccessFile != null) {
            try {
                OLD_CSVFileReader reader = new OLD_CSVFileReader();
                reader.setDelimSet( " " + reader.getDelimSet() );
                TableDataSet wa = reader.readFile(new File(walkAccessFile));

                int tazPosition = wa.getColumnPosition("TAZ");

                if (tazPosition <= 0) {
                    logger.fatal(
                        "TAZ was not a field in the walk access TableDataSet built from " +
                        walkAccessFile + ".");
                    System.exit(1);
                }

                int shrtPosition = wa.getColumnPosition("SHRT");

                if (shrtPosition <= 0) {
                    logger.fatal(
                        "SHRT was not a field in the walk access TableDataSet built from " +
                        walkAccessFile + ".");
                    System.exit(1);
                }

                int longPosition = wa.getColumnPosition("LONG");

                if (longPosition <= 0) {
                    logger.fatal(
                        "LONG was not a field in the walk access TableDataSet built from " +
                        walkAccessFile + ".");
                    System.exit(1);
                }

                for (int j = 1; j <= wa.getRowCount(); j++) {
                    taz = (int) wa.getValueAt(j, tazPosition);
                    shrtArray[taz] = wa.getValueAt(j, shrtPosition);
                    longArray[taz] = wa.getValueAt(j, longPosition);
                    walkPctArray[1][taz] = shrtArray[taz];
                    walkPctArray[2][taz] = longArray[taz];
                    walkPctArray[0][taz] = (float) (1.0 -
                        (shrtArray[taz] + longArray[taz]));
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            logger.fatal(
                "no walk access zonal data file was named in properties file.");
            System.exit(1);
        }

        // set 0/1 values for zone doesn't/does have short walk access for all dc alternatives
        k = 1;
        zonalShortAccess = new float[(3 * zoneTable.getRowCount()) + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            for (int j = 1; j <= 3; j++) {
                zonalShortAccess[k] = (shrtArray[i] > 0.0) ? 1 : 0;
                k++;
            }
        }

        // calculate attractions for use in size variable calculations
        calculateAttractions(zoneTable);
    }

    public static float getWalkPct(int subzoneIndex, int taz) {
        return walkPctArray[subzoneIndex][taz];
    }

    private void calculateSize(int tourType) {
        int cntyFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.COUNTY);

        if (cntyFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.COUNTY +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int earningsFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.WORKEARN);

        if (earningsFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.WORKEARN +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empoffFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPOFF);

        if (empoffFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPOFF +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empotherFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPOTHER);

        if (empotherFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPOTHER +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empretgdsFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPRETGDS);

        if (empretgdsFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPRETGDS +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empretsrvFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPRETSRV);

        if (empretsrvFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPRETSRV +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int cnty;
        float inc;
        float emp;
        float regionalEmploymentEarnings = 0.0f;
        float regionalEmployment = 0.0f;
        float[][] size = new float[3][zoneTable.getRowCount() + 1];
        float[][] earningsPct = new float[3][zoneTable.getRowCount() + 1];
        float[] earnings = new float[zoneTable.getRowCount() + 1];
        float[] totEmp = new float[zoneTable.getRowCount() + 1];
        float[] earningsRatio = new float[zoneTable.getRowCount() + 1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            earnings[i] = zoneTable.getValueAt(i, earningsFieldPosition);
            totEmp[i] = zoneTable.getValueAt(i, empoffFieldPosition) +
                zoneTable.getValueAt(i, empotherFieldPosition) +
                zoneTable.getValueAt(i, empretgdsFieldPosition) +
                zoneTable.getValueAt(i, empretsrvFieldPosition);
            regionalEmployment += totEmp[i];
            regionalEmploymentEarnings += (totEmp[i] * earnings[i]);
        }

        float avgEarnings = regionalEmploymentEarnings / regionalEmployment;

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            earningsRatio[i] = (earnings[i] > 0)
                ? (float) (Math.min(earnings[i] / avgEarnings, 2.5)) : 1.0f;

            earningsPct[0][i] = (earningsRatio[i] <= 1.7)
                ? (float) (Math.min(1.0,
                    1.038 - (1.157 * earningsRatio[i]) +
                    (0.342 * Math.pow(earningsRatio[i], 2)))) : 0.059f;
            earningsPct[1][i] = (float) (Math.max(0.0,
                    (float) (Math.min(1.0,
                        -0.065 + (0.253 * earningsRatio[i]) +
                        (0.059 * Math.pow(earningsRatio[i], 2))))));
            earningsPct[2][i] = (float) (1.0 -
                (earningsPct[0][i] + earningsPct[1][i]));

            cnty = (int) zoneTable.getValueAt(i, cntyFieldPosition);

            if (cnty == 1) {
                size[0][i] = (float) ((earningsPct[0][i] * 0.543) +
                    (earningsPct[1][i] * 0.228) + (earningsPct[2][i] * 0.000)) * this.attractions[tourType][i];
                size[1][i] = (float) ((earningsPct[0][i] * 0.341) +
                    (earningsPct[1][i] * 0.623) + (earningsPct[2][i] * 0.421)) * this.attractions[tourType][i];
                size[2][i] = (float) ((earningsPct[0][i] * 0.116) +
                    (earningsPct[1][i] * 0.149) + (earningsPct[2][i] * 0.579)) * this.attractions[tourType][i];
            } else {
                size[0][i] = (float) ((earningsPct[0][i] * 0.559) +
                    (earningsPct[1][i] * 0.203) + (earningsPct[2][i] * 0.000)) * this.attractions[tourType][i];
                size[1][i] = (float) ((earningsPct[0][i] * 0.345) +
                    (earningsPct[1][i] * 0.639) + (earningsPct[2][i] * 0.448)) * this.attractions[tourType][i];
                size[2][i] = (float) ((earningsPct[0][i] * 0.096) +
                    (earningsPct[1][i] * 0.158) + (earningsPct[2][i] * 0.552)) * this.attractions[tourType][i];
            }
        }


        
        // allolcate space for subzone size values in size arrays for the specific type.  If it's WORK, there are INCOME_CATEGORIES subzone size arrays
        if ( tourType == TourType.WORK ) {
            for (int i=0; i < INCOME_CATEGORIES; i++) {
                sizeFinal[tourType][i] = new float[numberOfSubzones + 1];
                sizeOriginal[tourType][i] = new float[numberOfSubzones + 1];
                sizeOriginalAdj[tourType][i] = new float[numberOfSubzones + 1];
                sizeBalance[tourType][i] = new float[numberOfSubzones + 1];
                sizeScaled[tourType][i] = new float[numberOfSubzones + 1];
                sizePrevious[tourType][i] = new float[numberOfSubzones + 1];
                shadowPrice[tourType][i] = new float[numberOfSubzones + 1];
                attrs[tourType][i] = new float[numberOfSubzones + 1];
                prods[tourType][i] = new float[numberOfSubzones + 1];

                Arrays.fill(shadowPrice[tourType][i], 1.0f);
            }
        }
        else {
            sizeFinal[tourType][0] = new float[numberOfSubzones + 1];
            sizeOriginal[tourType][0] = new float[numberOfSubzones + 1];
            sizeOriginalAdj[tourType][0] = new float[numberOfSubzones + 1];
            sizeBalance[tourType][0] = new float[numberOfSubzones + 1];
            sizeScaled[tourType][0] = new float[numberOfSubzones + 1];
            sizePrevious[tourType][0] = new float[numberOfSubzones + 1];
            shadowPrice[tourType][0] = new float[numberOfSubzones + 1];
            attrs[tourType][0] = new float[numberOfSubzones + 1];
            prods[tourType][0] = new float[numberOfSubzones + 1];

            Arrays.fill(shadowPrice[tourType][0], 1.0f);
        }

        float regionalSize = 0.0f;




        // allocate zonal size variables to subzones, if WORK, save by income category
        int k = 1;
        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            for (int j = 0; j < WALK_SEGMENTS; j++) {
                
                if ( tourType == TourType.WORK ) {
                    for (int m=0; m < INCOME_CATEGORIES; m++) {
                        sizeFinal[tourType][m][k] = size[m][i] * walkPctArray[j][i];
                        regionalSize += sizeFinal[tourType][m][k];
                    }
                }
                else {
                    sizeFinal[tourType][0][k] = (size[0][i] + size[1][i] + size[2][i]) * walkPctArray[j][i];
                    regionalSize += sizeFinal[tourType][0][k];
                }
                
                k++;
                
            }
        }

        
        
        // save original size variable arrays and arrays to be used in balancing
        for (k = 1; k <= numberOfSubzones; k++) {
            if ( tourType == TourType.WORK ) {
                for (int m=0; m < INCOME_CATEGORIES; m++) {
                    sizeOriginal[tourType][m][k] = sizeFinal[tourType][m][k];
                    sizeBalance[tourType][m][k] = sizeFinal[tourType][m][k];
                }
            }
            else {
                sizeOriginal[tourType][0][k] = sizeFinal[tourType][0][k];
                sizeBalance[tourType][0][k] = sizeFinal[tourType][0][k];
            }
        }

        logger.info("total original, unadjusted regional destination choice size for purpose " + tourType + " = " + regionalSize);
        
    }




    public void balanceSizeVariables(Household[] hh) {
        int orig;
        int typeCategory = -1;

        Iterator it = null;

        
        
        
        // set initial sizeBalance values to original size variable values determined when ZonalDataManager object was created.
        for (int k = 1; k <= numberOfSubzones; k++) {
            for (int m=0; m < INCOME_CATEGORIES; m++) {
                sizeBalance[TourType.WORK][m][k] = sizeOriginal[TourType.WORK][m][k];
            }
            sizeBalance[TourType.UNIVERSITY][0][k] = sizeOriginal[TourType.UNIVERSITY][0][k];
            sizeBalance[TourType.SCHOOL][0][k] = sizeOriginal[TourType.SCHOOL][0][k];

            for (int j=TourType.ESCORTING; j <= TourType.ATWORK; j++) {
                sizeBalance[j][0][k] = sizeOriginal[j][0][k];
            }

        }

        
        
        // initialize total prods and attrs arrays
        for (int i=1; i <= TourType.TYPES; i++) {
            if ( i == TourType.WORK ) {
                for (int m=0; m < INCOME_CATEGORIES; m++) {
                    for (int n=0; n < 2; n++) {
                        totProds[n][i][m] = 0;
                        totAttrs[n][i][m] = 0;
                    }
                }
            }
            else {
                for (int n=0; n < 2; n++) {
                    totProds[n][i][0] = 0;
                    totAttrs[n][i][0] = 0;
                }
            }
        }

        
        // determine number of tour productions of each type
        // sum unadjusted, unscaled attractions by type for reporting
        Tour[] mt = null;

        // initalize productions arrays
        for (int k=1; k < numberOfSubzones; k++) {
            for (int j=0; j < INCOME_CATEGORIES; j++)
                prods[TourType.WORK][j][k] = 0;
            prods[TourType.UNIVERSITY][0][k] = 0;
            prods[TourType.SCHOOL][0][k] = 0;
        }

        for (int i = 0; i < hh.length; i++) {
            mt = hh[i].getMandatoryTours();

            if (mt == null) {
                continue;
            }

            orig = hh[i].getTazID();
            for (int t = 0; t < mt.length; t++) {

                // determine type category, 0-2 if WORK, 0 otherwise
                typeCategory = 0;
                if (mt[t].getTourType() == TourType.WORK) {
                    if (hh[i].getHHIncome() == 1) {
                        typeCategory = 0;
                    }
                    else if (hh[i].getHHIncome() == 2) {
                        typeCategory = 1;
                    }
                    else if (hh[i].getHHIncome() == 3) {
                        typeCategory = 2;
                    }
                } 

                for (int w = 0; w < WALK_SEGMENTS; w++) {
                    int subzone = ((orig - 1) * WALK_SEGMENTS) + w + 1;
                    prods[mt[t].getTourType()][typeCategory][subzone] += walkPctArray[w][orig];
                    totProds[0][mt[t].getTourType()][typeCategory] += walkPctArray[w][orig];
                }
            }
        }

        logger.info("");
        logger.info("total tour productions by mandatory type before adjustments, before scaling:");
        logger.info("work lo=" + totProds[0][TourType.WORK][0]);
        logger.info("work md=" + totProds[0][TourType.WORK][1]);
        logger.info("work hi=" + totProds[0][TourType.WORK][2]);
        logger.info("university= " + totProds[0][TourType.UNIVERSITY][0]);
        logger.info("school= " + totProds[0][TourType.SCHOOL][0]);

        // copy productions totals to with overrides arrays
        for (int k=1; k < numberOfSubzones; k++) {
            for (int j=0; j < INCOME_CATEGORIES; j++)
                totProds[1][TourType.WORK][j] = totProds[0][TourType.WORK][j];
            for (int j=TourType.UNIVERSITY; j <= TourType.TYPES; j++)
                totProds[1][j][0] = totProds[0][j][0];
        }

        
        
        // sum size variables into total without overrides
        for (int k=1; k < numberOfSubzones; k++) {
            totAttrs[0][TourType.WORK][0] += sizeBalance[TourType.WORK][0][k];
            totAttrs[0][TourType.WORK][1] += sizeBalance[TourType.WORK][1][k];
            totAttrs[0][TourType.WORK][2] += sizeBalance[TourType.WORK][2][k];
            totAttrs[0][TourType.UNIVERSITY][0] += sizeBalance[TourType.UNIVERSITY][0][k];
            totAttrs[0][TourType.SCHOOL][0] += sizeBalance[TourType.SCHOOL][0][k];

            totAttrs[0][TourType.ESCORTING][0] += sizeBalance[TourType.ESCORTING][0][k];
            totAttrs[0][TourType.SHOP][0] += sizeBalance[TourType.SHOP][0][k];
            totAttrs[0][TourType.OTHER_MAINTENANCE][0] += sizeBalance[TourType.OTHER_MAINTENANCE][0][k];
            totAttrs[0][TourType.DISCRETIONARY][0] += sizeBalance[TourType.DISCRETIONARY][0][k];
            totAttrs[0][TourType.EAT][0] += sizeBalance[TourType.EAT][0][k];
            totAttrs[0][TourType.ATWORK][0] += sizeBalance[TourType.ATWORK][0][k];
        }

        // copy size variable totals to with overrides arrays
        for (int k=1; k < numberOfSubzones; k++) {
            for (int j=0; j < INCOME_CATEGORIES; j++)
                totAttrs[1][TourType.WORK][j] = totAttrs[0][TourType.WORK][j];
            for (int j=TourType.UNIVERSITY; j <= TourType.TYPES; j++)
                totAttrs[1][j][0] = totAttrs[0][j][0];
        }
        
        logger.info("");
        logger.info("total initial size values by mandatory type before adjustments, before scaling:");
        logger.info("work lo=" + totAttrs[0][TourType.WORK][0]);
        logger.info("work md=" + totAttrs[0][TourType.WORK][1]);
        logger.info("work hi=" + totAttrs[0][TourType.WORK][2]);
        logger.info("university = " + totAttrs[0][TourType.UNIVERSITY][0]);
        logger.info("school = " + totAttrs[0][TourType.SCHOOL][0]);

        logger.info("");
        logger.info("total initial size values by non-mandatory type before adjustments, before scaling:");
        logger.info("escort=" + totAttrs[0][TourType.ESCORTING][0]);
        logger.info("shop=" + totAttrs[0][TourType.SHOP][0]);
        logger.info("other maint.=" + totAttrs[0][TourType.OTHER_MAINTENANCE][0]);
        logger.info("discretionary= " + totAttrs[0][TourType.DISCRETIONARY][0]);
        logger.info("eat= " + totAttrs[0][TourType.EAT][0]);
        logger.info("at-work= " + totAttrs[0][TourType.ATWORK][0]);

        

        // log out un-adjusted, un-scaled totProds and totAttrs values summed over all subzones.
        logger.info( "" );
        logger.info( "" );
        logger.info( "Original, un-adjusted, un-scaled totProds (mandatory) and total size variables over all zones before adjustments, before scaling" );
        logger.info( String.format("%8s %8s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "", "", "", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10s %10s %10s %10s %10s %10s", "tot Ps", "", "",
                totProds[0][TourType.WORK][0], totProds[0][TourType.WORK][1], totProds[0][TourType.WORK][2], totProds[0][TourType.UNIVERSITY][0],
                totProds[0][TourType.SCHOOL][0], "N/A", "N/A", "N/A", "N/A", "N/A", "N/A") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", "tot As", "", "",
                totAttrs[0][TourType.WORK][0], totAttrs[0][TourType.WORK][1], totAttrs[0][TourType.WORK][2], totAttrs[0][TourType.UNIVERSITY][0],
                totAttrs[0][TourType.SCHOOL][0], totAttrs[0][TourType.ESCORTING][0], totAttrs[0][TourType.SHOP][0], totAttrs[0][TourType.OTHER_MAINTENANCE][0],
                totAttrs[0][TourType.DISCRETIONARY][0], totAttrs[0][TourType.EAT][0], totAttrs[0][TourType.ATWORK][0]) );
        

        
        // get an ArrayList of Adjustment objects and apply adjustments prior to scaling calculations
        // make an array of length of zones that indicate if over-ride adjustments have been defined for each zone
        ArrayList adjustmentSet = getAttractionsAdjustments();


        // log out the adjustments specified
        logger.info( "" );
        logger.info( "Size Variable Adjustments Specified" );
        String logString = String.format("%8s %8s %8s", "Zone", "Code", "" );
        for (int i=0; i < adjPurpLabels.length; i++)
            logString += String.format(" %10s", adjPurpLabels[i] );
        logger.info( logString );
        
        it = adjustmentSet.iterator();
        while ( it.hasNext() ) {
            Adjustment adj = (Adjustment)it.next();
            
            logString = String.format("%8d %8s %8s", adj.zone, adj.code, "");
            for (int i=0; i < adj.adjustment.length; i++)
                logString += String.format(" %10.2f", adj.adjustment[i] );
            logger.info( logString );

        }
        

        // log out original size values for subzones affected by specified adjustments
        logger.info( "" );
        logger.info( "Size variables for subzones with adjustments specified, before adjustments, before scaling" );
        logger.info( String.format("%8s %8s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "Zone", "Segment", "Subzone", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        boolean[] subzoneLogged = new boolean[numberOfSubzones+1];
        it = adjustmentSet.iterator();
        while ( it.hasNext() ) {
            Adjustment adj = (Adjustment)it.next();
            for (int w = 0; w < WALK_SEGMENTS; w++) {
                int i = ((adj.zone - 1) * WALK_SEGMENTS) + w + 1;
                if ( subzoneLogged[i] == false ) {
                    logger.info( String.format("%8d %8d %8d %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", adj.zone, w, i,
                            sizeBalance[TourType.WORK][0][i], sizeBalance[TourType.WORK][1][i], sizeBalance[TourType.WORK][2][i], sizeBalance[TourType.UNIVERSITY][0][i],
                            sizeBalance[TourType.SCHOOL][0][i], sizeBalance[TourType.ESCORTING][0][i], sizeBalance[TourType.SHOP][0][i], sizeBalance[TourType.OTHER_MAINTENANCE][0][i],
                            sizeBalance[TourType.DISCRETIONARY][0][i], sizeBalance[TourType.EAT][0][i], sizeBalance[TourType.ATWORK][0][i]) );
                    subzoneLogged[i] = true;
                }
            }
        }

        
        
        boolean[][] subzoneOverRides = new boolean[numberOfSubzones+1][TourType.TYPES+2]; // num purposes + 2 for 2 extra income categories for work purpose
        
        // apply adjustments
        it = adjustmentSet.iterator();
        while ( it.hasNext() ) {
            Adjustment adj = (Adjustment)it.next();
            applySizeVariableAdjustments( adj );

            if ( adj.code.equalsIgnoreCase("O") ) {
                for (int w = 0; w < WALK_SEGMENTS; w++) {
                    int subzone = ((adj.zone - 1) * WALK_SEGMENTS) + w + 1;
                    int purp = -1;
                    for (int p=0; p < adj.adjustment.length; p++)
                        if ( adj.adjustment[p] != 0.0 ) {
                            subzoneOverRides[subzone][p] = true;
                        }
                }
            }
            
        }
        
        
        
        // log out adjusted, but un-scaled totProds and totAttrs values summed over all subzones.
        logger.info( "" );
        logger.info( "TotProds (mandatory) and size variables over all zones after adjustments, before scaling" );
        logger.info( String.format("%-17s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "without override", "", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10s %10s %10s %10s %10s %10s", "tot Ps", "", "",
                totProds[0][TourType.WORK][0], totProds[0][TourType.WORK][1], totProds[0][TourType.WORK][2], totProds[0][TourType.UNIVERSITY][0],
                totProds[0][TourType.SCHOOL][0], "N/A", "N/A", "N/A", "N/A", "N/A", "N/A") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", "tot As", "", "",
                totAttrs[0][TourType.WORK][0], totAttrs[0][TourType.WORK][1], totAttrs[0][TourType.WORK][2], totAttrs[0][TourType.UNIVERSITY][0],
                totAttrs[0][TourType.SCHOOL][0], totAttrs[0][TourType.ESCORTING][0], totAttrs[0][TourType.SHOP][0], totAttrs[0][TourType.OTHER_MAINTENANCE][0],
                totAttrs[0][TourType.DISCRETIONARY][0], totAttrs[0][TourType.EAT][0], totAttrs[0][TourType.ATWORK][0]) );
        logger.info( "" );
        logger.info( String.format("%-17s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "with override", "", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10s %10s %10s %10s %10s %10s", "tot Ps", "", "",
                totProds[1][TourType.WORK][0], totProds[1][TourType.WORK][1], totProds[1][TourType.WORK][2], totProds[1][TourType.UNIVERSITY][0],
                totProds[1][TourType.SCHOOL][0], "N/A", "N/A", "N/A", "N/A", "N/A", "N/A") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", "tot As", "", "",
                totAttrs[1][TourType.WORK][0], totAttrs[1][TourType.WORK][1], totAttrs[1][TourType.WORK][2], totAttrs[1][TourType.UNIVERSITY][0],
                totAttrs[1][TourType.SCHOOL][0], totAttrs[1][TourType.ESCORTING][0], totAttrs[1][TourType.SHOP][0], totAttrs[1][TourType.OTHER_MAINTENANCE][0],
                totAttrs[1][TourType.DISCRETIONARY][0], totAttrs[1][TourType.EAT][0], totAttrs[1][TourType.ATWORK][0]) );
        
        
        // log out adjusted, but un-scaled size values for subzones affected by specified adjustments
        logger.info( "" );
        logger.info( "Size variables for subzones with adjustments specified, after adjustments, before scaling" );
        logger.info( String.format("%8s %8s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "Zone", "Segment", "Subzone", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        Arrays.fill(subzoneLogged, false);
        it = adjustmentSet.iterator();
        while ( it.hasNext() ) {
            Adjustment adj = (Adjustment)it.next();
            for (int w = 0; w < WALK_SEGMENTS; w++) {
                int i = ((adj.zone - 1) * WALK_SEGMENTS) + w + 1;
                if ( subzoneLogged[i] == false ) {
                    logger.info( String.format("%8d %8d %8d %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", adj.zone, w, i,
                            sizeBalance[TourType.WORK][0][i], sizeBalance[TourType.WORK][1][i], sizeBalance[TourType.WORK][2][i], sizeBalance[TourType.UNIVERSITY][0][i],
                            sizeBalance[TourType.SCHOOL][0][i], sizeBalance[TourType.ESCORTING][0][i], sizeBalance[TourType.SHOP][0][i], sizeBalance[TourType.OTHER_MAINTENANCE][0][i],
                            sizeBalance[TourType.DISCRETIONARY][0][i], sizeBalance[TourType.EAT][0][i], sizeBalance[TourType.ATWORK][0][i]) );
                    subzoneLogged[i] = true;
                }
            }
        }
        


        
        // save original adjusted size variable arrays prior to balancing
        for (int k = 1; k <= numberOfSubzones; k++) {
            for (int m=0; m < INCOME_CATEGORIES; m++) {
                sizeOriginalAdj[TourType.WORK][m][k] = sizeBalance[TourType.WORK][m][k];
            }
            sizeOriginalAdj[TourType.UNIVERSITY][0][k] = sizeBalance[TourType.UNIVERSITY][0][k];
            sizeOriginalAdj[TourType.SCHOOL][0][k] = sizeBalance[TourType.SCHOOL][0][k];
        }
        
        
        // balance initial size variables
        for (int i = 1; i <= numberOfSubzones; i++) {
            
            // if the subzone belongs to a zone that had an adjustment over-ride, skip scaling values for the subzone for its purpose

            // set scaled values for mandatory work types if an override was specified for the subzone and purpose.
            for (int j=0; j < INCOME_CATEGORIES; j++) {
                if ( subzoneOverRides[i][j] ) {
                    sizeScaled[TourType.WORK][j][i] = 0.0f;
                    if ( sizeBalance[TourType.WORK][j][i] > 0 )
                        sizeScaled[TourType.WORK][j][i] = sizeBalance[TourType.WORK][j][i];
                }
                else {
                    sizeScaled[TourType.WORK][j][i] = 0.0f;
                    if (totAttrs[0][TourType.WORK][j] > 0.0)
                        sizeScaled[TourType.WORK][j][i] = (sizeBalance[TourType.WORK][j][i] * totProds[0][TourType.WORK][j]) / totAttrs[0][TourType.WORK][j];
                }
            }

            // set scaled values for mandatory non-work types if an override was specified for the subzone and purpose.
            if ( subzoneOverRides[i][TourType.UNIVERSITY+1] ) {
                sizeScaled[TourType.UNIVERSITY][0][i] = 0.0f;
                if ( sizeBalance[TourType.UNIVERSITY][0][i] > 0 )
                    sizeScaled[TourType.UNIVERSITY][0][i] = sizeBalance[TourType.UNIVERSITY][0][i];
            }
            else {
                sizeScaled[TourType.UNIVERSITY][0][i] = 0.0f;
                if (totAttrs[0][TourType.UNIVERSITY][0] > 0.0)
                    sizeScaled[TourType.UNIVERSITY][0][i] = (sizeBalance[TourType.UNIVERSITY][0][i] * totProds[0][TourType.UNIVERSITY][0]) / totAttrs[0][TourType.UNIVERSITY][0];
            }
            
            if ( subzoneOverRides[i][TourType.SCHOOL+1] ) {
                sizeScaled[TourType.SCHOOL][0][i] = 0.0f;
                if ( sizeBalance[TourType.SCHOOL][0][i] > 0 )
                    sizeScaled[TourType.SCHOOL][0][i] = sizeBalance[TourType.SCHOOL][0][i];
            }
            else {
                sizeScaled[TourType.SCHOOL][0][i] = 0.0f;
                if (totAttrs[0][TourType.SCHOOL][0] > 0.0)
                    sizeScaled[TourType.SCHOOL][0][i] = (sizeBalance[TourType.SCHOOL][0][i] * totProds[0][TourType.SCHOOL][0]) / totAttrs[0][TourType.SCHOOL][0];
            }
                
                
            // no scaling of values is done for non-mandatory types, so just set the scaled value.
            for (int j=TourType.ESCORTING; j <= TourType.ATWORK; j++) {
                sizeScaled[j][0][i] = 0.0f;
                if ( sizeBalance[j][0][i] > 0 )
                    sizeScaled[j][0][i] = sizeBalance[j][0][i];
            }

        }


        
        // log out final adjusted and scaled size values for subzones affected by specified adjustments
        logger.info( "" );
        logger.info( "Size variables for subzones with adjustments specified, after adjustments, after scaling" );
        logger.info( String.format("%8s %8s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "Zone", "Segment", "Subzone", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        Arrays.fill(subzoneLogged, false);
        it = adjustmentSet.iterator();
        while ( it.hasNext() ) {
            Adjustment adj = (Adjustment)it.next();
            for (int w = 0; w < WALK_SEGMENTS; w++) {
                int i = ((adj.zone - 1) * WALK_SEGMENTS) + w + 1;
                if ( subzoneLogged[i] == false ) {
                    logger.info( String.format("%8d %8d %8d %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", adj.zone, w, i,
                        sizeScaled[TourType.WORK][0][i], sizeScaled[TourType.WORK][1][i], sizeScaled[TourType.WORK][2][i], sizeScaled[TourType.UNIVERSITY][0][i],
                        sizeScaled[TourType.SCHOOL][0][i], sizeScaled[TourType.ESCORTING][0][i], sizeScaled[TourType.SHOP][0][i], sizeScaled[TourType.OTHER_MAINTENANCE][0][i],
                        sizeScaled[TourType.DISCRETIONARY][0][i], sizeScaled[TourType.EAT][0][i], sizeScaled[TourType.ATWORK][0][i]) );
                    subzoneLogged[i] = true;
                }
            }
        }
        
        
        
        // set final size variables for mandatory typers to scaled values
        // non-mandatory type size values aren't scaled.
        for (int k=1; k <= numberOfSubzones; k++) {

            for (int m=0; m < INCOME_CATEGORIES; m++)
                sizeFinal[TourType.WORK][m][k] = sizeScaled[TourType.WORK][m][k];

            // set scaled values for non-work mandatory and all non-mandatory types
            for (int j=TourType.UNIVERSITY; j <= TourType.ATWORK; j++)
                sizeFinal[j][0][k] = sizeScaled[j][0][k];

        }

        
        
        // sum scaled attractions for all types for reporting
        for (int i=1; i <= TourType.TYPES; i++) {
            if ( i == TourType.WORK ) {
                for (int m=0; m < INCOME_CATEGORIES; m++) {
                    totAttrs[0][i][m] = 0.0f;
                    totAttrs[1][i][m] = 0.0f;
                }
            }
            else {
                totAttrs[0][i][0] = 0.0f;
                totAttrs[1][i][0] = 0.0f;
            }
        }

		for (int k=1; k <= numberOfSubzones; k++) {

            for (int i=1; i <= TourType.TYPES; i++) {
                if ( i == TourType.WORK ) {
                    for (int m=0; m < INCOME_CATEGORIES; m++) {
                        if ( subzoneOverRides[k][m] == false )
                            totAttrs[0][i][m] += sizeFinal[i][m][k];
                        totAttrs[1][i][m] += sizeFinal[i][m][k];
                    }
                }
                else {
                    if ( subzoneOverRides[k][i+1] == false )
                        totAttrs[0][i][0] += sizeFinal[i][0][k];
                    totAttrs[1][i][0] += sizeFinal[i][0][k];
                }
            }
            
		}


        // log out adjusted and scaled totProds and totAttrs values summed over all subzones.
        logger.info( "" );
        logger.info( "TotProds (mandatory) and size variables over all zones after adjustments, after scaling" );
        logger.info( String.format("%-17s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "without override", "", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10s %10s %10s %10s %10s %10s", "tot Ps", "", "",
                totProds[0][TourType.WORK][0], totProds[0][TourType.WORK][1], totProds[0][TourType.WORK][2], totProds[0][TourType.UNIVERSITY][0],
                totProds[0][TourType.SCHOOL][0], "N/A", "N/A", "N/A", "N/A", "N/A", "N/A") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", "tot As", "", "",
                totAttrs[0][TourType.WORK][0], totAttrs[0][TourType.WORK][1], totAttrs[0][TourType.WORK][2], totAttrs[0][TourType.UNIVERSITY][0],
                totAttrs[0][TourType.SCHOOL][0], totAttrs[0][TourType.ESCORTING][0], totAttrs[0][TourType.SHOP][0], totAttrs[0][TourType.OTHER_MAINTENANCE][0],
                totAttrs[0][TourType.DISCRETIONARY][0], totAttrs[0][TourType.EAT][0], totAttrs[0][TourType.ATWORK][0]) );
        logger.info( "" );
        logger.info( String.format("%-17s %8s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s %10s", "with override", "", "wLo", "wMd", "wHi", "univ", "schl", "esc", "shop", "main", "disc", "eat", "atWk") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10s %10s %10s %10s %10s %10s", "tot Ps", "", "",
                totProds[1][TourType.WORK][0], totProds[1][TourType.WORK][1], totProds[1][TourType.WORK][2], totProds[1][TourType.UNIVERSITY][0],
                totProds[1][TourType.SCHOOL][0], "N/A", "N/A", "N/A", "N/A", "N/A", "N/A") );
        logger.info( String.format("%8s %8s %8s %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f %10.2f", "tot As", "", "",
                totAttrs[1][TourType.WORK][0], totAttrs[1][TourType.WORK][1], totAttrs[1][TourType.WORK][2], totAttrs[1][TourType.UNIVERSITY][0],
                totAttrs[1][TourType.SCHOOL][0], totAttrs[1][TourType.ESCORTING][0], totAttrs[1][TourType.SHOP][0], totAttrs[1][TourType.OTHER_MAINTENANCE][0],
                totAttrs[1][TourType.DISCRETIONARY][0], totAttrs[1][TourType.EAT][0], totAttrs[1][TourType.ATWORK][0]) );
        
        
        
        logger.info("");
        logger.info("");
        logger.info("total adjusted, scaled size variables by mandatory type:");
        logger.info("work lo=" + totAttrs[1][TourType.WORK][0]);
        logger.info("work md=" + totAttrs[1][TourType.WORK][1]);
        logger.info("work hi=" + totAttrs[1][TourType.WORK][2]);
        logger.info("university = " + totAttrs[1][TourType.UNIVERSITY][0]);
        logger.info("school = " + totAttrs[1][TourType.SCHOOL][0]);
        
        logger.info("");
        logger.info("total adjusted size variables by non-mandatory type:");
        logger.info("escorting = " + totAttrs[1][TourType.ESCORTING][0]);
        logger.info("shopping = " + totAttrs[1][TourType.SHOP][0]);
        logger.info("other maint. = " + totAttrs[1][TourType.OTHER_MAINTENANCE][0]);
        logger.info("discretionary = " + totAttrs[1][TourType.DISCRETIONARY][0]);
        logger.info("eat = " + totAttrs[1][TourType.EAT][0]);
        logger.info("at-work = " + totAttrs[1][TourType.ATWORK][0]);
        logger.info("");
        


        // set size variables used in shadow price adjustmnents in mandatory DC model
        for (int k=1; k <= numberOfSubzones; k++) {

            for (int i=1; i <= TourType.TYPES; i++) {
                if ( i == TourType.WORK ) {
                    for (int m=0; m < INCOME_CATEGORIES; m++)
                        sizePrevious[i][m][k] = sizeScaled[i][m][k];
                }
                else {
                    sizePrevious[i][0][k] = sizeScaled[i][0][k];
                }
            }
            
        }

        
        hh = null;
    }


    
    private ArrayList getAttractionsAdjustments() {
        
        /* define a set of adjustments to make, for testing purposes
        
        // create an Adjustment object for a zone - (multiply, add, overRide, zone)
        // for a tourType - (wLo, wMd, wHi, Uni, Sch).
        // These will eventually bea read in from a file
        
        // put adjustment objects in an ArrayList to return 
        ArrayList adjustmentSet = new ArrayList();
        
        double[] testAdj1 = { 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0 };
        double[] testAdj2 = { 0.0, 75.0, 0.0, 0.0, 50.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        double[] testAdj3 = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1100.0, 0.0, 0.0, 0.0, 0.0 };
        
        adjustmentSet.add( new Adjustment( 800, "M", testAdj1 ) );
        adjustmentSet.add( new Adjustment( 800, "A", testAdj2 ) );
        adjustmentSet.add( new Adjustment( 300, "O", testAdj3 ) );
        
        end of code to define testing adjustments  */
        
        
        ArrayList adjustmentSet = readAdjustmentsFile();
        return adjustmentSet;
        
    }
    
    
    private ArrayList readAdjustmentsFile () {
        
        String filename = (String) propertyMap.get("AttractionAdjustments.file");
        if ( filename == null || filename.equals("") )
            return new ArrayList();

        
        // put adjustment objects in an ArrayList to return 
        ArrayList adjustmentSet = new ArrayList();
        TableDataSet table = null;
        
        try {
            CSVFileReader reader = new CSVFileReader();
            table = reader.readFile(new File(filename));
        }
        catch (IOException e) {
            logger.error ( "exception thrown reading attraction adjustnments file: " + filename, e );
        }
        
        for (int row=1; row <= table.getRowCount(); row++) {
            String[] rowStringValues = table.getRowValuesAsString(row);
            
            int zone = Integer.parseInt(rowStringValues[0]);
            String code = rowStringValues[1].toUpperCase();

            double[] values = new double[rowStringValues.length-2];
            for (int j=2; j < rowStringValues.length; j++)
                values[j-2] = Double.parseDouble(rowStringValues[j]);

            adjustmentSet.add( new Adjustment( zone, code, values ) );
            
        }
        
        return adjustmentSet;
    }

    private void applySizeVariableAdjustments( Adjustment adj ) {    


        // make adjustments as specified
        for (int w = 0; w < WALK_SEGMENTS; w++) {
            int subzone = ((adj.zone - 1) * WALK_SEGMENTS) + w + 1;

            for ( int p=0; p < adj.adjustment.length; p++ ) {
                
                // it is assumed that only non-zero adjustment values will be indications of an adjustment to be made
                if ( adj.adjustment[p] == 0.0 )
                    continue;
                
                // get the original size value for the specific purpose
                float origValue = 0.0f;
                float adjValue = 0.0f;

                switch ( p ) {
                    // work purposes
                    case 0:
                        origValue = sizeBalance[TourType.WORK][0][subzone];
                        break;
                    case 1:
                        origValue = sizeBalance[TourType.WORK][1][subzone];
                        break;
                    case 2:
                        origValue = sizeBalance[TourType.WORK][2][subzone];
                        break;
                    // non-work purposes
                    default:
                        // p-1 maps to the tourType indices for non-work purposes.
                        origValue = sizeBalance[p-1][0][subzone];
                        break;
                }
                
                adjValue = origValue;
                
                
                
                // apply adjustments and save adjusted size value for the specific purpose.
                // scaling for zones with O adjustments will be based on total Ps and As without the over-rides.
                // scaling for zones with M or A adjustments will be based on Ps and As that include those adjustments
                
                // if over-ride value, ignore any multiply and/or add values also specified:
                if ( adj.code.equalsIgnoreCase("O") ) {
                    
                    // get over-ride value for this subzone
                    adjValue = (float)(walkPctArray[w][adj.zone]*adj.adjustment[p]);

                    //  adjust attr and prod totals for mandatory types; don't need to adjust for non-mandatory as no scaling is done.
                    switch ( p ) {
                        // mandatory purposes
                        case 0:
                            totProds[0][TourType.WORK][0] -= adjValue;
                            totAttrs[0][TourType.WORK][0] -= origValue;
                            totAttrs[1][TourType.WORK][0] += ( adjValue - origValue );
                            break;
                        case 1:
                            totProds[0][TourType.WORK][1] -= adjValue;
                            totAttrs[0][TourType.WORK][1] -= origValue;
                            totAttrs[1][TourType.WORK][1] += ( adjValue - origValue );
                            break;
                        case 2:
                            totProds[0][TourType.WORK][2] -= adjValue;
                            totAttrs[0][TourType.WORK][2] -= origValue;
                            totAttrs[1][TourType.WORK][2] += ( adjValue - origValue );
                            break;
                        case 3:
                            totProds[0][TourType.UNIVERSITY][0] -= adjValue;
                            totAttrs[0][TourType.UNIVERSITY][0] -= origValue;
                            totAttrs[1][TourType.UNIVERSITY][0] += ( adjValue - origValue );
                            break;
                        case 4:
                            totProds[0][TourType.SCHOOL][0] -= adjValue;
                            totAttrs[0][TourType.SCHOOL][0] -= origValue;
                            totAttrs[1][TourType.SCHOOL][0] += ( adjValue - origValue );
                            break;
                        // non-mandatory purposes
                        default:
                            // p-1 maps to the tourType indices for non-work purposes.
                            // don't need to adjust totals for non-mandatory purposes since no scaling is done,
                            // but attr totals are modified so that the adjustment is reflected in totals when logged.
                            totAttrs[0][p-1][0] -= origValue;
                            totAttrs[1][p-1][0] += ( adjValue - origValue );
                            break;
                    }

                }
                // otherwise, apply multiply and/or add:
                
                else {
                    if ( adj.code.equalsIgnoreCase("M") )
                        adjValue *= adj.adjustment[p];
                    if ( adj.code.equalsIgnoreCase("A") )
                        adjValue += walkPctArray[w][adj.zone]*adj.adjustment[p];

                    //  adjust attr and prod totals for mandatory types by the adjustment increment; don't need to adjust for non-mandatory as no scaling is done.
                    switch ( p ) {
                        // mandatory purposes
                        case 0:
                            totAttrs[0][TourType.WORK][0] += (adjValue - origValue);
                            totAttrs[1][TourType.WORK][0] += (adjValue - origValue);
                            break;
                        case 1:
                            totAttrs[0][TourType.WORK][1] += (adjValue - origValue);
                            totAttrs[1][TourType.WORK][1] += (adjValue - origValue);
                            break;
                        case 2:
                            totAttrs[0][TourType.WORK][2] += (adjValue - origValue);
                            totAttrs[1][TourType.WORK][2] += (adjValue - origValue);
                            break;
                        case 3:
                            totAttrs[0][TourType.UNIVERSITY][0] += (adjValue - origValue);
                            totAttrs[1][TourType.UNIVERSITY][0] += (adjValue - origValue);
                            break;
                        case 4:
                            totAttrs[0][TourType.SCHOOL][0] += (adjValue - origValue);
                            totAttrs[1][TourType.SCHOOL][0] += (adjValue - origValue);
                            break;
                        // non-mandatory purposes
                        default:
                            // p-1 maps to the tourType indices for non-work purposes.
                            // don't need to adjust totals for non-mandatory purposes since no scaling is done,
                            // but totals are modified so that the adjustment is reflected in totals when logged.
                            totAttrs[0][p-1][0] += ( adjValue - origValue );
                            totAttrs[1][p-1][0] += ( adjValue - origValue );
                            break;
                    }

                }

                
                
                // apply adjusted values to size variable arrays for specific purpose
                switch ( p ) {
                    // mandatory - work purposes
                    case 0:
                        sizeBalance[TourType.WORK][0][subzone] = adjValue;
                        break;
                    case 1:
                        sizeBalance[TourType.WORK][1][subzone] = adjValue;
                        break;
                    case 2:
                        sizeBalance[TourType.WORK][2][subzone] = adjValue;
                        break;
                    // non-work purposes
                    // p-1 maps to the tourType indices for non-work purposes.
                    default:
                        sizeBalance[p-1][0][subzone] = adjValue;
                        break;
                }
            
            }

        }
        
        
    }
    
    
    
    
	public void sumAttractions(Household[] hh) {
		int subzone;
		int destTaz;
		int destWalkSeg;
		int type = 0;


        // zero the attrs arrays
		for (int k=1; k <= numberOfSubzones; k++) {
            for (int i=1; i <= TourType.TYPES; i++) {
                if ( i == TourType.WORK ) {
                    for (int m=0; m < INCOME_CATEGORIES; m++)
                        attrs[i][m][k] = 0.0f;
                }
                else {
                    attrs[i][0][k] = 0.0f;
                }
            }
		}


		// determine number of modeled tour attractions of each type
        // only need to count mandatory attractions, as they're the only ones scaled.
		Tour[] mt = null;

		for (int i = 0; i < hh.length; i++) {
			mt = hh[i].getMandatoryTours();

			if (mt == null)
				continue;


			for (int t=0; t < mt.length; t++) {

			    destTaz = mt[t].getDestTaz();
				destWalkSeg = mt[t].getDestShrtWlk();

                type = 0;
				if (mt[t].getTourType() == TourType.WORK) {
					if (hh[i].getHHIncome() == 1) {
						type = 0;
					} else if (hh[i].getHHIncome() == 2) {
						type = 1;
					} else if (hh[i].getHHIncome() == 3) {
						type = 2;
					}
				}

				subzone = ((destTaz - 1) * WALK_SEGMENTS) + destWalkSeg + 1;

                attrs[mt[t].getTourType()][type][subzone]++;

			}
		}
	}




    public void reportMaxDiff() {
        
        // only reporting for the 5 mandatory tourtype categories.
        
        int[] maxI = new int[5];
        float[] diff = new float[5];

        for (int i = 1; i <= numberOfSubzones; i++) {
            
            if (Math.abs(sizeScaled[TourType.WORK][0][i] - attrs[TourType.WORK][0][i]) > diff[0]) {
                diff[0] = Math.abs(sizeScaled[TourType.WORK][0][i] - attrs[TourType.WORK][0][i]);
                maxI[0] = i;
            }

            if (Math.abs(sizeScaled[TourType.WORK][1][i] - attrs[TourType.WORK][1][i]) > diff[1]) {
                diff[1] = Math.abs(sizeScaled[TourType.WORK][1][i] - attrs[TourType.WORK][1][i]);
                maxI[1] = i;
            }

            if (Math.abs(sizeScaled[TourType.WORK][2][i] - attrs[TourType.WORK][2][i]) > diff[2]) {
                diff[2] = Math.abs(sizeScaled[TourType.WORK][2][i] - attrs[TourType.WORK][2][i]);
                maxI[2] = i;
            }

            if (Math.abs(sizeScaled[TourType.UNIVERSITY][0][i] - attrs[TourType.UNIVERSITY][0][i]) > diff[3]) {
                diff[3] = Math.abs(sizeScaled[TourType.UNIVERSITY][0][i] - attrs[TourType.UNIVERSITY][0][i]);
                maxI[3] = i;
            }

            if (Math.abs(sizeScaled[TourType.SCHOOL][0][i] - attrs[TourType.SCHOOL][0][i]) > diff[4]) {
                diff[4] = Math.abs(sizeScaled[TourType.SCHOOL][0][i] - attrs[TourType.SCHOOL][0][i]);
                maxI[4] = i;
            }

        }

        
        logger.info( "maximum residual discrepency between scaled size variables and modeled attractions by mandatory type:" );
        logger.info("work lo:     scaled size=" + sizeScaled[TourType.WORK][0][maxI[0]] + ", model attrs=" + attrs[TourType.WORK][0][maxI[0]] + ", abs diff=" + diff[0] + ", rel diff=" + (diff[0] / sizeScaled[TourType.WORK][0][maxI[0]]));
        logger.info("work md:     scaled size=" + sizeScaled[TourType.WORK][1][maxI[1]] + ", model attrs=" + attrs[TourType.WORK][1][maxI[1]] + ", abs diff=" + diff[1] + ", rel diff=" + (diff[1] / sizeScaled[TourType.WORK][1][maxI[1]]));
        logger.info("work hi:     scaled size=" + sizeScaled[TourType.WORK][2][maxI[2]] + ", model attrs=" + attrs[TourType.WORK][2][maxI[2]] + ", abs diff=" + diff[2] + ", rel diff=" + (diff[2] / sizeScaled[TourType.WORK][2][maxI[2]]));
        logger.info("university:  scaled size=" + sizeScaled[TourType.UNIVERSITY][0][maxI[3]] + ", model attrs=" + attrs[TourType.UNIVERSITY][0][maxI[3]] + ", abs diff=" + diff[3] + ", rel diff=" + (diff[3] / sizeScaled[TourType.UNIVERSITY][0][maxI[3]]));
        logger.info("school:      scaled size=" + sizeScaled[TourType.SCHOOL][0][maxI[4]] + ", model attrs=" + attrs[TourType.SCHOOL][0][maxI[4]] + ", abs diff=" + diff[4] + ", rel diff=" + (diff[4] / sizeScaled[TourType.SCHOOL][0][maxI[4]]));
        
    }


    public void updateSizeVariables() {
        
        // only size variables for mandatory types are adjusted by shadow prices

        for (int i = 1; i <= numberOfSubzones; i++) {
            for (int m=0; m < INCOME_CATEGORIES; m++) {
                sizeFinal[TourType.WORK][m][i] = sizeScaled[TourType.WORK][m][i] * shadowPrice[TourType.WORK][m][i];
                if ( sizeFinal[TourType.WORK][m][i] < 0.0f )
                    sizeFinal[TourType.WORK][m][i] = 0.0f;
            }

            sizeFinal[TourType.UNIVERSITY][0][i] = sizeScaled[TourType.UNIVERSITY][0][i] * shadowPrice[TourType.UNIVERSITY][0][i];
            if ( sizeFinal[TourType.UNIVERSITY][0][i] < 0.0f )
                sizeFinal[TourType.UNIVERSITY][0][i] = 0.0f;

            sizeFinal[TourType.SCHOOL][0][i] = sizeScaled[TourType.SCHOOL][0][i] * shadowPrice[TourType.SCHOOL][0][i];
            if ( sizeFinal[TourType.SCHOOL][0][i] < 0.0f )
                sizeFinal[TourType.SCHOOL][0][i] = 0.0f;
        }
        
    }
    
    

    public void updateShadowPrices() {
        
        // shadow proces are maintained for mandatory types only
        
        for (int i = 1; i <= numberOfSubzones; i++) {
            for (int m=0; m < INCOME_CATEGORIES; m++)
                shadowPrice[TourType.WORK][m][i] *= ((sizeScaled[TourType.WORK][m][i]+1) / (attrs[TourType.WORK][m][i]+1));

            shadowPrice[TourType.UNIVERSITY][0][i] *= ((sizeScaled[TourType.UNIVERSITY][0][i]+1) / (attrs[TourType.UNIVERSITY][0][i]+1));
            shadowPrice[TourType.SCHOOL][0][i] *= ((sizeScaled[TourType.SCHOOL][0][i]+1) / (attrs[TourType.SCHOOL][0][i]+1));
        }
    }

    
    
    public void updateShadowPricingInfo(int iteration) {
        String outputFile = (String) propertyMap.get("ShadowPricing.outputFile");

        if (outputFile == null) {
            return;
        }

        ArrayList tableHeadings = new ArrayList();
        tableHeadings.add("dc_alt");
        tableHeadings.add("dc_taz");
        tableHeadings.add("dc_walk");
        tableHeadings.add("lo_prods");
        tableHeadings.add("md_prods");
        tableHeadings.add("hi_prods");
        tableHeadings.add("univ_prods");
        tableHeadings.add("school_prods");
        tableHeadings.add("lo_size_orig");
        tableHeadings.add("md_size_orig");
        tableHeadings.add("hi_size_orig");
        tableHeadings.add("univ_size_orig");
        tableHeadings.add("school_size_orig");
        tableHeadings.add("lo_size_orig_adj");
        tableHeadings.add("md_size_orig_adj");
        tableHeadings.add("hi_size_orig_adj");
        tableHeadings.add("univ_size_orig_adj");
        tableHeadings.add("school_size_orig_adj");
        tableHeadings.add("lo_size_scaled");
        tableHeadings.add("md_size_scaled");
        tableHeadings.add("hi_size_scaled");
        tableHeadings.add("univ_size_scaled");
        tableHeadings.add("school_size_scaled");
        tableHeadings.add("lo_size_model");
        tableHeadings.add("md_size_model");
        tableHeadings.add("hi_size_model");
        tableHeadings.add("univ_size_model");
        tableHeadings.add("school_size_model");
        tableHeadings.add("lo_attrs_model");
        tableHeadings.add("md_attrs_model");
        tableHeadings.add("hi_attrs_model");
        tableHeadings.add("univ_attrs_model");
        tableHeadings.add("school_attrs_model");
        tableHeadings.add("lo_size_adjusted");
        tableHeadings.add("md_size_adjusted");
        tableHeadings.add("hi_size_adjusted");
        tableHeadings.add("univ_size_adjusted");
        tableHeadings.add("school_size_adjusted");
        tableHeadings.add("lo_shadow_price");
        tableHeadings.add("md_shadow_price");
        tableHeadings.add("hi_shadow_price");
        tableHeadings.add("univ_shadow_price");
        tableHeadings.add("school_shadow_price");

        // define a TableDataSet for use in writing output file
        float[][] tableData = new float[numberOfSubzones][tableHeadings.size()];

        for (int i = 1; i <= numberOfSubzones; i++) {
            tableData[i - 1][0] = i;
            tableData[i - 1][1] = (int) ((i - 1) / WALK_SEGMENTS) + 1;
            ;
            tableData[i - 1][2] = i -
                ((tableData[i - 1][1] - 1) * WALK_SEGMENTS) - 1;
            tableData[i - 1][3]  = (float) prods[TourType.WORK][0][i];
            tableData[i - 1][4]  = (float) prods[TourType.WORK][1][i];
            tableData[i - 1][5]  = (float) prods[TourType.WORK][2][i];
            tableData[i - 1][6]  = (float) prods[TourType.UNIVERSITY][0][i];
            tableData[i - 1][7]  = (float) prods[TourType.SCHOOL][0][i];
            tableData[i - 1][8]  = (float) sizeOriginal[TourType.WORK][0][i];
            tableData[i - 1][9]  = (float) sizeOriginal[TourType.WORK][1][i];
            tableData[i - 1][10] = (float) sizeOriginal[TourType.WORK][2][i];
            tableData[i - 1][11] = (float) sizeOriginal[TourType.UNIVERSITY][0][i];
            tableData[i - 1][12] = (float) sizeOriginal[TourType.SCHOOL][0][i];
            tableData[i - 1][13] = (float) sizeOriginalAdj[TourType.WORK][0][i];
            tableData[i - 1][14] = (float) sizeOriginalAdj[TourType.WORK][1][i];
            tableData[i - 1][15] = (float) sizeOriginalAdj[TourType.WORK][2][i];
            tableData[i - 1][16] = (float) sizeOriginalAdj[TourType.UNIVERSITY][0][i];
            tableData[i - 1][17] = (float) sizeOriginalAdj[TourType.SCHOOL][0][i];
            tableData[i - 1][18] = (float) sizeScaled[TourType.WORK][0][i];
            tableData[i - 1][19] = (float) sizeScaled[TourType.WORK][1][i];
            tableData[i - 1][20] = (float) sizeScaled[TourType.WORK][2][i];
            tableData[i - 1][21] = (float) sizeScaled[TourType.UNIVERSITY][0][i];
            tableData[i - 1][22] = (float) sizeScaled[TourType.SCHOOL][0][i];
            tableData[i - 1][23] = (float) sizePrevious[TourType.WORK][0][i];
            tableData[i - 1][24] = (float) sizePrevious[TourType.WORK][1][i];
            tableData[i - 1][25] = (float) sizePrevious[TourType.WORK][2][i];
            tableData[i - 1][26] = (float) sizePrevious[TourType.UNIVERSITY][0][i];
            tableData[i - 1][27] = (float) sizePrevious[TourType.SCHOOL][0][i];
            tableData[i - 1][28] = (float) attrs[TourType.WORK][0][i];
            tableData[i - 1][29] = (float) attrs[TourType.WORK][1][i];
            tableData[i - 1][30] = (float) attrs[TourType.WORK][2][i];
            tableData[i - 1][31] = (float) attrs[TourType.UNIVERSITY][0][i];
            tableData[i - 1][32] = (float) attrs[TourType.SCHOOL][0][i];
            tableData[i - 1][33] = (float) sizeFinal[TourType.WORK][0][i];
            tableData[i - 1][34] = (float) sizeFinal[TourType.WORK][1][i];
            tableData[i - 1][35] = (float) sizeFinal[TourType.WORK][2][i];
            tableData[i - 1][36] = (float) sizeFinal[TourType.UNIVERSITY][0][i];
            tableData[i - 1][37] = (float) sizeFinal[TourType.SCHOOL][0][i];
            tableData[i - 1][38] = (float) shadowPrice[TourType.WORK][0][i];
            tableData[i - 1][39] = (float) shadowPrice[TourType.WORK][1][i];
            tableData[i - 1][40] = (float) shadowPrice[TourType.WORK][2][i];
            tableData[i - 1][41] = (float) shadowPrice[TourType.UNIVERSITY][0][i];
            tableData[i - 1][42] = (float) shadowPrice[TourType.SCHOOL][0][i];
        }

        TableDataSet outputTable = TableDataSet.create(tableData, tableHeadings);

        // write outputTable to new output file
        try {
            String newFilename = outputFile.replaceFirst(".csv", "_" + iteration + ".csv");
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(outputTable, new File(newFilename), new DecimalFormat("#.00000"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // set initial size variables used in DC model for reporting
        for (int i = 1; i <= numberOfSubzones; i++) {
            for (int m=0; m < INCOME_CATEGORIES; m++)
                sizePrevious[TourType.WORK][m][i] = sizeFinal[TourType.WORK][m][i];
            sizePrevious[TourType.UNIVERSITY][0][i] = sizeFinal[TourType.UNIVERSITY][0][i];
            sizePrevious[TourType.SCHOOL][0][i] = sizeFinal[TourType.SCHOOL][0][i];
        }

        tableData = null;
        tableHeadings.clear();
        outputTable = null;
    }

    private void calculateAttractions(TableDataSet zoneTable) {
        int purp;
        int ut;
        int at;

        double[][][][] coeff = new double[10][4][8][8];

        // get file name from properties file
        String attractionModelsFile = (String) propertyMap.get(
                "AttractionModels.file");

        // read the attraction models file to get field coefficients
        if (attractionModelsFile != null) {
            try {
                CSVFileReader reader = new CSVFileReader();
                TableDataSet am = reader.readFile(new File(attractionModelsFile));

                int purpFieldPosition = am.getColumnPosition("purpose");

                if (purpFieldPosition <= 0) {
                    logger.fatal(
                        "purpose was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int utFieldPosition = am.getColumnPosition("urbtype");

                if (utFieldPosition <= 0) {
                    logger.fatal(
                        "urbtype was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int atFieldPosition = am.getColumnPosition("areatype");

                if (atFieldPosition <= 0) {
                    logger.fatal(
                        "areatype was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int totpopFieldPosition = am.getColumnPosition("totalpop");

                if (totpopFieldPosition <= 0) {
                    logger.fatal(
                        "totalpop was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int totempFieldPosition = am.getColumnPosition("totemp");

                if (totempFieldPosition <= 0) {
                    logger.fatal(
                        "totemp was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int empretgFieldPosition = am.getColumnPosition("retail_g");

                if (empretgFieldPosition <= 0) {
                    logger.fatal(
                        "retail_g was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int empretsFieldPosition = am.getColumnPosition("retail_s");

                if (empretsFieldPosition <= 0) {
                    logger.fatal(
                        "retail_s was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int empoffFieldPosition = am.getColumnPosition("office_e");

                if (empoffFieldPosition <= 0) {
                    logger.fatal(
                        "office_e was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int schenrFieldPosition = am.getColumnPosition("sch_enrol");

                if (schenrFieldPosition <= 0) {
                    logger.fatal(
                        "sch_enrol was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int unienrFieldPosition = am.getColumnPosition("uni_enrol");

                if (unienrFieldPosition <= 0) {
                    logger.fatal(
                        "uni_enrol was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                int totpop50FieldPosition = am.getColumnPosition("totpop_50");

                if (totpop50FieldPosition <= 0) {
                    logger.fatal(
                        "totpop_50 was not a field in the attraction model TableDataSet.");
                    System.exit(1);
                }

                for (int i = 1; i <= am.getRowCount(); i++) {
                    purp = (int) am.getValueAt(i, purpFieldPosition);
                    ut = (int) am.getValueAt(i, utFieldPosition);
                    at = (int) am.getValueAt(i, atFieldPosition);

                    coeff[purp][ut][at][0] = am.getValueAt(i,
                            totpopFieldPosition);
                    coeff[purp][ut][at][1] = am.getValueAt(i,
                            totempFieldPosition);
                    coeff[purp][ut][at][2] = am.getValueAt(i,
                            empretgFieldPosition);
                    coeff[purp][ut][at][3] = am.getValueAt(i,
                            empretsFieldPosition);
                    coeff[purp][ut][at][4] = am.getValueAt(i,
                            empoffFieldPosition);
                    coeff[purp][ut][at][5] = am.getValueAt(i,
                            schenrFieldPosition);
                    coeff[purp][ut][at][6] = am.getValueAt(i,
                            unienrFieldPosition);
                    coeff[purp][ut][at][7] = am.getValueAt(i,
                            totpop50FieldPosition);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            logger.fatal(
                "no attraction model specification file was named in properties file.");
            System.exit(1);
        }

        // read the zoneTable TableDataSet to get zonal fields for the attraction models
        int utFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.URBTYPE);

        if (utFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.URBTYPE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int atFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.AREATYPE);

        if (atFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.AREATYPE +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int hhpopFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.POP);

        if (hhpopFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.POP +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empoffFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPOFF);

        if (empoffFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPOFF +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empotherFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPOTHER);

        if (empotherFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPOTHER +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empretgFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPRETGDS);

        if (empretgFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPRETGDS +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int empretsFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.EMPRETSRV);

        if (empretsFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.EMPRETSRV +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int elenrFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.ELENROLL);

        if (elenrFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.ELENROLL +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int hsenrFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.HSENROLL);

        if (hsenrFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.HSENROLL +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        int unenrFieldPosition = zoneTable.getColumnPosition(ZoneTableFields.UNENROLL);

        if (unenrFieldPosition <= 0) {
            logger.fatal(ZoneTableFields.UNENROLL +
                " was not a field in the zoneData TableDataSet.");
            System.exit(1);
        }

        float[] field = new float[8];

        this.attractions = new float[TourType.TYPES + 1][zoneTable.getRowCount() +
            1];

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            ut = (int) zoneTable.getValueAt(i, utFieldPosition);
            at = (int) zoneTable.getValueAt(i, atFieldPosition);

            field[0] = zoneTable.getValueAt(i, hhpopFieldPosition);

            field[2] = zoneTable.getValueAt(i, empretgFieldPosition);
            field[3] = zoneTable.getValueAt(i, empretsFieldPosition);
            field[4] = zoneTable.getValueAt(i, empoffFieldPosition);

            field[1] = field[2] + field[3] + field[4] +
                zoneTable.getValueAt(i, empotherFieldPosition);

            field[5] = zoneTable.getValueAt(i, elenrFieldPosition) +
                zoneTable.getValueAt(i, hsenrFieldPosition);

            field[6] = zoneTable.getValueAt(i, unenrFieldPosition);

            field[7] = (field[0] >= 50) ? field[0] : 0.0f;

            for (int p = 1; p <= TourType.TYPES; p++) {
                this.attractions[p][i] = 0.0f;

                for (int j = 0; j < 8; j++)
                    this.attractions[p][i] += (field[j] * coeff[p][ut][at][j]);
            }
        }
    }




    private TableDataSet setZoneTable(Vector tables) {
        //initialize result with table 1
        TableDataSet result = (TableDataSet) tables.get(0);

        //No. of tables
        int noTables = tables.size();
        int[] noRow = new int[noTables];
        int[] noCol = new int[noTables];
        float[] taz = new float[noTables];

        for (int i = 0; i < noTables; i++) {
            //No. of rows and columns of each table
            noRow[i] = ((TableDataSet) tables.get(i)).getRowCount();
            noCol[i] = ((TableDataSet) tables.get(i)).getColumnCount();

            //all talbes must have same number of rows
            if (noRow[i] != noRow[0]) {
                System.exit(1);
            }
        }

        //taz in all tables must match, table index starts from 1
        for (int i = 1; i < (noRow[0] + 1); i++) {
            for (int j = 0; j < noTables; j++) {
                //taz in each row of each table
                taz[j] = ((TableDataSet) tables.get(j)).getValueAt(i, 1);

                if (taz[j] != taz[0]) {
                    System.exit(2);
                }
            }
        }

        //apend columns of table 2, 3, 4, ....to table 1
        for (int i = 1; i < noTables; i++) {
            TableDataSet t = (TableDataSet) tables.get(i);

            for (int j = 2; j < (t.getColumnCount() + 1); j++) { //table index start from 1, firt column is taz not append

                String colName = t.getColumnLabel(j);

                //if a colmn exists already, don't append it
                if (!isColExist(result, colName)) {
                    result.appendColumn(t.getColumnAsFloat(j), colName);
                }
            }
        }

        //reorder zone table as orginal
        result = shuffleZoneTable(result);

        return result;
    }

    /**
     * Check if a column already exists
     * @param table: A TableDataSet table
     * @param colName: A column name to be checked
     * @return true is column exists, false otherwise
     */
    private boolean isColExist(TableDataSet table, String colName) {
        boolean result = false;

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (colName.equals(table.getColumnLabel(i))) {
                result = true;
            }
        }

        return result;
    }

    private TableDataSet shuffleZoneTable(TableDataSet table) {
        //original column order in ZSEDN.csv
        String[] colTitles = {
            "taz", "puma", "schdist", "cnty", "acres", "area", "hhpop", "totpop",
            "hhs", "hhs_gq", "gqpop", "labf", "hhinc", "workearn", "empoff", "empretgds",
            "empretsrv", "empother", "elenr", "hsenr", "univenr", "work_au_pk",
            "work_au_op", "work_tr_pk", "work_tr_op", "work_walk", "nonw_au_pk",
            "nonw_au_op", "nonw_tr_pk", "nonw_tr_op", "nonw_walk", "cbdatype",
            "parktot", "parklng", "propfree", "areatype", "urbtype", "parkrate",
            "rings", "suprdist", "licking"
        };

        TableDataSet result = new TableDataSet();

        for (int i = 0; i < colTitles.length; i++) {
            int colPosition = table.getColumnPosition(colTitles[i]);
            result.appendColumn(table.getColumnAsFloat(colPosition),
                colTitles[i]);
        }

        return result;
    }

    public TableDataSet getZonalTableDataSet() {
        return zoneTable;
    }



	/**
	 * copy the static members of this class into a HashMap.  The non-static HashMap
	 * will be serialized in a message and sent to client VMs in a distributed application.
	 * Static data itself is not serialized with the object, so the extra step is necessary.
	 *
	 * @return
	 */
	public HashMap createStaticDataMap () {

	    HashMap staticDataMap = new HashMap();

		staticDataMap.put ( "walkPctArray", walkPctArray );
		staticDataMap.put ( "sizeFinal", sizeFinal );
		staticDataMap.put ( "parkRate", parkRate );
		staticDataMap.put ( "urbType", urbType );
		staticDataMap.put ( "cnty", cnty );
		staticDataMap.put ( "schdist", schdist );
		staticDataMap.put ( "suprdist", suprdist );
		staticDataMap.put ( "rings", rings );
		staticDataMap.put ( "cbdatype", cbdatype );
		staticDataMap.put ( "parktot", parktot );
		staticDataMap.put ( "parklng", parklng );
		staticDataMap.put ( "propfree", propfree );
		staticDataMap.put ( "parkrate", parkrate );
		staticDataMap.put ( "zonalShortAccess", zonalShortAccess );
		staticDataMap.put ( "zonalAreaType", zonalAreaType );
		staticDataMap.put ( "zonal_nonw_au_op", zonal_nonw_au_op );
		staticDataMap.put ( "zonal_nonw_walk", zonal_nonw_walk );
		staticDataMap.put ( "odUtilModeAlt", odUtilModeAlt );
		staticDataMap.put ( "logsumDcAMPM", logsumDcAMPM );
		staticDataMap.put ( "logsumDcAMMD", logsumDcAMMD );
		staticDataMap.put ( "logsumDcMDMD", logsumDcMDMD );
		staticDataMap.put ( "logsumDcPMNT", logsumDcPMNT );


	    return staticDataMap;

	}


	/**
	 * set values in the static members of this class from data in a HashMap.
	 *
	 */
	public void setStaticData ( HashMap staticDataMap ) {

		walkPctArray     = (float[][])staticDataMap.get ( "walkPctArray" );
		sizeFinal        = (float[][][])staticDataMap.get ( "sizeFinal" );
		parkRate         = (float[])staticDataMap.get ( "parkRate" );
		urbType          = (float[])staticDataMap.get ( "urbType" );
		cnty             = (float[])staticDataMap.get ( "cnty" );
		schdist          = (float[])staticDataMap.get ( "schdist" );
		suprdist         = (float[])staticDataMap.get ( "suprdist" );
		rings            = (float[])staticDataMap.get ( "rings" );
		cbdatype         = (float[])staticDataMap.get ( "cbdatype" );
		parktot          = (float[])staticDataMap.get ( "parktot" );
		parklng          = (float[])staticDataMap.get ( "parklng" );
		propfree         = (float[])staticDataMap.get ( "propfree" );
		parkrate         = (float[])staticDataMap.get ( "parkrate" );
		zonalShortAccess = (float[])staticDataMap.get ( "zonalShortAccess" );
		zonalAreaType    = (float[])staticDataMap.get ( "zonalAreaType" );
		zonal_nonw_au_op = (float[])staticDataMap.get ( "zonal_nonw_au_op" );
		zonal_nonw_walk  = (float[])staticDataMap.get ( "zonal_nonw_walk" );
		odUtilModeAlt    = (double[][])staticDataMap.get ( "odUtilModeAlt" );
		logsumDcAMPM     = (float[][])staticDataMap.get ( "logsumDcAMPM" );
		logsumDcAMMD     = (float[][])staticDataMap.get ( "logsumDcAMMD" );
		logsumDcMDMD     = (float[][])staticDataMap.get ( "logsumDcMDMD" );
		logsumDcPMNT     = (float[][])staticDataMap.get ( "logsumDcPMNT" );

		numberOfZones = walkPctArray.length - 1;

	}



	public static float getTotSize ( int tourTypeIndex, int subzone ) {
        float result = 0.0f;
        if ( tourTypeIndex == TourType.WORK )
            for (int m=0; m < INCOME_CATEGORIES; m++)
                result += sizeFinal[TourType.WORK][m][subzone];
        else
            result = sizeFinal[tourTypeIndex][0][subzone];
		
        return result;
	}



	public int getNumberOfZones () {
		return numberOfZones;
	}



	public static void setLogsumDcAMPM ( int processorIndex, int altIndex, float logsum ) {
		logsumDcAMPM[processorIndex][altIndex] = logsum;
	}


	public static void setLogsumDcAMMD ( int processorIndex, int altIndex, float logsum ) {
		logsumDcAMMD[processorIndex][altIndex] = logsum;
	}


	public static void setLogsumDcMDMD ( int processorIndex, int altIndex, float logsum ) {
		logsumDcMDMD[processorIndex][altIndex] = logsum;
	}


	public static void setLogsumDcPMNT ( int processorIndex, int altIndex, float logsum ) {
		logsumDcPMNT[processorIndex][altIndex] = logsum;
	}


	public static void setOdUtilModeAlt ( int processorIndex, double[] ModalUtilities ) {
	    odUtilModeAlt[processorIndex] = ModalUtilities;
	}

	/**
	 * update property map in each global iteration
	 */
	public void updatePropertyMap(int iteration){
		propertyMap=ResourceUtil.getResourceBundleAsHashMap("morpc"+iteration);
	}

    
    
    
    
    // Inner class to define data structure for size variable adjustment data
    
    class Adjustment {
        int zone;               // taz for which adjustment should be made
        String code;            // adjustment code - M(multiply), A(add) or O(override)
        double[] adjustment;    // adjustment value
        Adjustment( int zone, String code, double[] adj ) {
            this.zone = zone;
            this.code = code;
            this.adjustment = adj;
        }
    }
    

    
    
    
	//for testing purpose
    public static void main(String[] args) {
        HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap("morpc");
        String output = (String) propertyMap.get("TAZData.file");
        ZonalDataManager zm = new ZonalDataManager(propertyMap);
        TableDataSet tds = zm.getZonalTableDataSet();

        try {
            File file = new File(output);

            //            tds.saveFile(file,"%8.2f");
        } catch (Exception e) {
            logger.error("can not open or save file");
        }
    }

}