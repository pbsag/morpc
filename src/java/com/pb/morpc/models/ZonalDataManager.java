package com.pb.morpc.models;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import com.pb.morpc.structures.*;

import java.io.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 * @author Jim Hicks
 *
 * Class for managing zonal data.
 * e.g. size variables, zonal TableDataSet, attractions model, stop density model.
 */
public class ZonalDataManager implements java.io.Serializable {

	public static final int WALK_SEGMENTS = 3;
	public static final int MAX_DISTRIBUTED_PROCESSORES = 32;

    protected static Logger logger = Logger.getLogger("com.pb.morpc.models");
    private TableDataSet zoneTable;



    // VERY IMPORTANT !!!
    // if any public static data members get added to this class,
    // then they must be added to the method ( createNonStaticData() )
    // which copies all public static data members into a HashMap for message passing.
    public static float[][] walkPctArray;
    public static float[][] loSize;
	public static float[][] mdSize;
	public static float[][] hiSize;
	public static float[][] loMdSize;
	public static float[][] totSize;
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
    private float[][] size;
    private float[][] attractions;
    private float[][] loSizeOriginal;
    private float[][] mdSizeOriginal;
    private float[][] hiSizeOriginal;
    private float[][] loMdSizeOriginal;
    private float[][] totSizeOriginal;
    private float[][] loSizeScaled;
    private float[][] mdSizeScaled;
    private float[][] hiSizeScaled;
    private float[][] loMdSizeScaled;
    private float[][] totSizeScaled;
    private float[][] loSizePrevious;
    private float[][] mdSizePrevious;
    private float[][] hiSizePrevious;
    private float[][] loMdSizePrevious;
    private float[][] totSizePrevious;
    private float[] loShadowPrice;
    private float[] mdShadowPrice;
    private float[] hiShadowPrice;
    private float[] univShadowPrice;
    private float[] schoolShadowPrice;
	private float[] loAttrs;
	private float[] mdAttrs;
	private float[] hiAttrs;
	private float[] univAttrs;
	private float[] schoolAttrs;
    private float[] totAttrs = new float[5];
    private float[] totProds = new float[5];
    private float[][] prods = new float[5][];





    public ZonalDataManager(HashMap propertyMap) {
        this.propertyMap = propertyMap;

        // build the zonal data table
        String zonalFile1 = (String) propertyMap.get("TAZMainData.file");
        String zonalFile2 = (String) propertyMap.get("TAZAccessibility.file");
        String zonalFile3 = (String) propertyMap.get("TAZDerived.file");
        String zonalFile4 = (String) propertyMap.get("TAZEquivalency.file");
        String zonalCombined = (String) propertyMap.get("TAZData.file");

        try {
            CSVFileReader reader = new CSVFileReader();
            reader.setDelimSet( " ,\t\n\r\f\"");

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

        loMdSize = new float[TourType.TYPES + 1][];
        loSize = new float[TourType.TYPES + 1][];
        mdSize = new float[TourType.TYPES + 1][];
        hiSize = new float[TourType.TYPES + 1][];
        totSize = new float[TourType.TYPES + 1][];

        loMdSizeOriginal = new float[TourType.TYPES + 1][];
        loSizeOriginal = new float[TourType.TYPES + 1][];
        mdSizeOriginal = new float[TourType.TYPES + 1][];
        hiSizeOriginal = new float[TourType.TYPES + 1][];
        totSizeOriginal = new float[TourType.TYPES + 1][];

        loMdSizeScaled = new float[TourType.TYPES + 1][];
        loSizeScaled = new float[TourType.TYPES + 1][];
        mdSizeScaled = new float[TourType.TYPES + 1][];
        hiSizeScaled = new float[TourType.TYPES + 1][];
        totSizeScaled = new float[TourType.TYPES + 1][];

        loMdSizePrevious = new float[TourType.TYPES + 1][];
        loSizePrevious = new float[TourType.TYPES + 1][];
        mdSizePrevious = new float[TourType.TYPES + 1][];
        hiSizePrevious = new float[TourType.TYPES + 1][];
        totSizePrevious = new float[TourType.TYPES + 1][];

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
                CSVFileReader reader = new CSVFileReader();
				reader.setDelimSet( " ,\t\n\r\f\"");
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

    public float getWalkPct(int chosenShrtWlkAlt, int chosenDestAlt) {
        return walkPctArray[chosenShrtWlkAlt][chosenDestAlt];
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

        loMdSize[tourType] = new float[numberOfSubzones + 1];
        loSize[tourType] = new float[numberOfSubzones + 1];
        mdSize[tourType] = new float[numberOfSubzones + 1];
        hiSize[tourType] = new float[numberOfSubzones + 1];
        totSize[tourType] = new float[numberOfSubzones + 1];

        loMdSizeOriginal[tourType] = new float[numberOfSubzones + 1];
        loSizeOriginal[tourType] = new float[numberOfSubzones + 1];
        mdSizeOriginal[tourType] = new float[numberOfSubzones + 1];
        hiSizeOriginal[tourType] = new float[numberOfSubzones + 1];
        totSizeOriginal[tourType] = new float[numberOfSubzones + 1];

        loMdSizeScaled[tourType] = new float[numberOfSubzones + 1];
        loSizeScaled[tourType] = new float[numberOfSubzones + 1];
        mdSizeScaled[tourType] = new float[numberOfSubzones + 1];
        hiSizeScaled[tourType] = new float[numberOfSubzones + 1];
        totSizeScaled[tourType] = new float[numberOfSubzones + 1];

        loMdSizePrevious[tourType] = new float[numberOfSubzones + 1];
        loSizePrevious[tourType] = new float[numberOfSubzones + 1];
        mdSizePrevious[tourType] = new float[numberOfSubzones + 1];
        hiSizePrevious[tourType] = new float[numberOfSubzones + 1];
        totSizePrevious[tourType] = new float[numberOfSubzones + 1];

        loShadowPrice = new float[numberOfSubzones + 1];
        mdShadowPrice = new float[numberOfSubzones + 1];
        hiShadowPrice = new float[numberOfSubzones + 1];
        univShadowPrice = new float[numberOfSubzones + 1];
        schoolShadowPrice = new float[numberOfSubzones + 1];

        loAttrs = new float[numberOfSubzones + 1];
        mdAttrs = new float[numberOfSubzones + 1];
        hiAttrs = new float[numberOfSubzones + 1];
        univAttrs = new float[numberOfSubzones + 1];
        schoolAttrs = new float[numberOfSubzones + 1];

        float regionalSize = 0.0f;

        Arrays.fill(loShadowPrice, 1.0f);
        Arrays.fill(mdShadowPrice, 1.0f);
        Arrays.fill(hiShadowPrice, 1.0f);
        Arrays.fill(univShadowPrice, 1.0f);
        Arrays.fill(schoolShadowPrice, 1.0f);

        int k = 1;

        for (int i = 1; i <= zoneTable.getRowCount(); i++) {
            for (int j = 0; j < WALK_SEGMENTS; j++) {
                totSize[tourType][k] = (size[0][i] + size[1][i] + size[2][i]) * walkPctArray[j][i];
                loMdSize[tourType][k] = (size[0][i] + size[1][i]) * walkPctArray[j][i];
                loSize[tourType][k] = (size[0][i]) * walkPctArray[j][i];
                mdSize[tourType][k] = (size[1][i]) * walkPctArray[j][i];
                hiSize[tourType][k] = (size[2][i]) * walkPctArray[j][i];

                regionalSize += totSize[tourType][k];

                k++;
            }
        }

        // save original size variable arrays
        for (int i = 1; i <= numberOfSubzones; i++) {
            totSizeOriginal[tourType][i] = totSize[tourType][i];
            loMdSizeOriginal[tourType][i] = loMdSize[tourType][i];
            loSizeOriginal[tourType][i] = loSize[tourType][i];
            mdSizeOriginal[tourType][i] = mdSize[tourType][i];
            hiSizeOriginal[tourType][i] = hiSize[tourType][i];
        }

        logger.info("total regional destination choice size for purpose " +
            tourType + " = " + regionalSize);
    }




    public void balanceSizeVariables(Household[] hh) {
        int subzone;
        int orig;
        int type = 0;


        for (int i = 0; i < 5; i++) {
            prods[i] = new float[numberOfSubzones + 1];
			totProds[i] = 0;
			totAttrs[i] = 0;
        }

        // determine number of tour productions of each type
        Tour[] mt = null;

        for (int i = 0; i < hh.length; i++) {
            mt = hh[i].getMandatoryTours();

            if (mt == null) {
                continue;
            }

            for (int t = 0; t < mt.length; t++) {
                orig = hh[i].getTazID();

                if (mt[t].getTourType() == TourType.WORK) {
                    if (hh[i].getHHIncome() == 1) {
                        type = 0;
                    } else if (hh[i].getHHIncome() == 2) {
                        type = 1;
                    } else if (hh[i].getHHIncome() == 3) {
                        type = 2;
                    }
                } else if (mt[t].getTourType() == TourType.UNIVERSITY) {
                    type = 3;
                } else if (mt[t].getTourType() == TourType.SCHOOL) {
                    type = 4;
                }

                for (int w = 0; w < WALK_SEGMENTS; w++) {
                    subzone = ((orig - 1) * WALK_SEGMENTS) + w;
                    prods[type][subzone] += walkPctArray[w][orig];
                    totProds[type] += walkPctArray[w][orig];
                }
            }
        }

        logger.info("total productions by mandatory type:");
        logger.info("work lo = " + totProds[0]);
        logger.info("work md = " + totProds[1]);
        logger.info("work hi = " + totProds[2]);
        logger.info("university = " + totProds[3]);
        logger.info("school = " + totProds[4]);

        // sum total attractions by type
        for (int i = 1; i <= numberOfSubzones; i++) {
            totAttrs[0] += loSizeOriginal[TourType.WORK][i];
            totAttrs[1] += mdSizeOriginal[TourType.WORK][i];
            totAttrs[2] += hiSizeOriginal[TourType.WORK][i];
            totAttrs[3] += totSizeOriginal[TourType.UNIVERSITY][i];
            totAttrs[4] += totSizeOriginal[TourType.SCHOOL][i];
        }

        logger.info("total initial attractions by mandatory type:");
        logger.info("work lo = " + totAttrs[0]);
        logger.info("work md = " + totAttrs[1]);
        logger.info("work hi = " + totAttrs[2]);
        logger.info("university = " + totAttrs[3]);
        logger.info("school = " + totAttrs[4]);

        // balance initial size variables
        for (int i = 1; i <= numberOfSubzones; i++) {
            if (totAttrs[0] > 0.0) {
                loSizeScaled[TourType.WORK][i] = (loSizeOriginal[TourType.WORK][i] * totProds[0]) / totAttrs[0];
            } else {
                loSizeScaled[TourType.WORK][i] = 0.0f;
            }

            if (totAttrs[1] > 0.0) {
                mdSizeScaled[TourType.WORK][i] = (mdSizeOriginal[TourType.WORK][i] * totProds[1]) / totAttrs[1];
            } else {
                mdSizeScaled[TourType.WORK][i] = 0.0f;
            }

            if (totAttrs[2] > 0.0) {
                hiSizeScaled[TourType.WORK][i] = (hiSizeOriginal[TourType.WORK][i] * totProds[2]) / totAttrs[2];
            } else {
                hiSizeScaled[TourType.WORK][i] = 0.0f;
            }

            if (totAttrs[3] > 0.0) {
                totSizeScaled[TourType.UNIVERSITY][i] = (totSizeOriginal[TourType.UNIVERSITY][i] * totProds[3]) / totAttrs[3];
            } else {
                totSizeScaled[TourType.UNIVERSITY][i] = 0.0f;
            }

            if (totAttrs[4] > 0.0) {
                totSizeScaled[TourType.SCHOOL][i] = (totSizeOriginal[TourType.SCHOOL][i] * totProds[4]) / totAttrs[4];
            } else {
                totSizeScaled[TourType.SCHOOL][i] = 0.0f;
            }
        }

        // set initial size variables to scaled values
        for (int i = 1; i <= numberOfSubzones; i++) {
            loSize[TourType.WORK][i] = loSizeScaled[TourType.WORK][i];
            mdSize[TourType.WORK][i] = mdSizeScaled[TourType.WORK][i];
            hiSize[TourType.WORK][i] = hiSizeScaled[TourType.WORK][i];
            loMdSize[TourType.WORK][i] = loSizeScaled[TourType.WORK][i] +
                mdSizeScaled[TourType.WORK][i];
            totSize[TourType.WORK][i] = loSizeScaled[TourType.WORK][i] +
                mdSizeScaled[TourType.WORK][i] +
                hiSizeScaled[TourType.WORK][i];
            totSize[TourType.UNIVERSITY][i] = totSizeScaled[TourType.UNIVERSITY][i];
            totSize[TourType.SCHOOL][i] = totSizeScaled[TourType.SCHOOL][i];
        }

		for (int i = 0; i < 5; i++)
			totAttrs[i] = 0;

		// sum scaled attractions by type for reporting
		for (int i = 1; i <= numberOfSubzones; i++) {
			totAttrs[0] += loSize[TourType.WORK][i];
			totAttrs[1] += mdSize[TourType.WORK][i];
			totAttrs[2] += hiSize[TourType.WORK][i];
			totAttrs[3] += totSize[TourType.UNIVERSITY][i];
			totAttrs[4] += totSize[TourType.SCHOOL][i];
		}

		logger.info("total balanced attractions by mandatory type:");
		logger.info("work lo = " + totAttrs[0]);
		logger.info("work md = " + totAttrs[1]);
		logger.info("work hi = " + totAttrs[2]);
		logger.info("university = " + totAttrs[3]);
		logger.info("school = " + totAttrs[4]);



        // set initial size variables used in DC model for reporting
        for (int i = 1; i <= numberOfSubzones; i++) {
            loSizePrevious[TourType.WORK][i] = loSizeScaled[TourType.WORK][i];
            mdSizePrevious[TourType.WORK][i] = mdSizeScaled[TourType.WORK][i];
            hiSizePrevious[TourType.WORK][i] = hiSizeScaled[TourType.WORK][i];
            totSizePrevious[TourType.UNIVERSITY][i] = totSizeScaled[TourType.UNIVERSITY][i];
            totSizePrevious[TourType.SCHOOL][i] = totSizeScaled[TourType.SCHOOL][i];
        }

        hh = null;
    }



	public void sumAttractions(Household[] hh) {
		int subzone;
		int destTaz;
		int destWalkSeg;
		int type = 0;



		for (int i = 1; i <= numberOfSubzones; i++) {
			loAttrs[i] = 0;
			mdAttrs[i] = 0;
			hiAttrs[i] = 0;
			univAttrs[i] = 0;
			schoolAttrs[i] = 0;
		}


		// determine number of modeled tour attractions of each type
		Tour[] mt = null;

		for (int i = 0; i < hh.length; i++) {
			mt = hh[i].getMandatoryTours();

			if (mt == null) {
				continue;
			}

			for (int t = 0; t < mt.length; t++) {

			    destTaz = mt[t].getDestTaz();
				destWalkSeg = mt[t].getDestShrtWlk();

				if (mt[t].getTourType() == TourType.WORK) {
					if (hh[i].getHHIncome() == 1) {
						type = 0;
					} else if (hh[i].getHHIncome() == 2) {
						type = 1;
					} else if (hh[i].getHHIncome() == 3) {
						type = 2;
					}
				} else if (mt[t].getTourType() == TourType.UNIVERSITY) {
					type = 3;
				} else if (mt[t].getTourType() == TourType.SCHOOL) {
					type = 4;
				}

				subzone = ((destTaz - 1) * WALK_SEGMENTS) + destWalkSeg + 1;

				incrementModeledAttractions( subzone, type );

			}
		}
	}




    public void incrementModeledAttractions(int k, int index) {
        // increment the number of attractions computed for subzone k, index:
        // 0=low, 1=med, 2=hi, 3=univ, 4=school.
        switch (index) {
        case 0:
            loAttrs[k]++;

            break;

        case 1:
            mdAttrs[k]++;

            break;

        case 2:
            hiAttrs[k]++;

            break;

        case 3:
            univAttrs[k]++;

            break;

        case 4:
            schoolAttrs[k]++;

            break;
        }
    }

    public void reportMaxDiff() {
        int[] maxI = new int[5];
        float[] diff = new float[5];
        Arrays.fill(diff, 0.0f);

        for (int i = 1; i <= numberOfSubzones; i++) {
            if (Math.abs(loSizeScaled[TourType.WORK][i] - loAttrs[i]) > diff[0]) {
                diff[0] = Math.abs(loSizeScaled[TourType.WORK][i] - loAttrs[i]);
                maxI[0] = i;
            }

            if (Math.abs(mdSizeScaled[TourType.WORK][i] - mdAttrs[i]) > diff[1]) {
                diff[1] = Math.abs(mdSizeScaled[TourType.WORK][i] - mdAttrs[i]);
                maxI[1] = i;
            }

            if (Math.abs(hiSizeScaled[TourType.WORK][i] - hiAttrs[i]) > diff[2]) {
                diff[2] = Math.abs(hiSizeScaled[TourType.WORK][i] - hiAttrs[i]);
                maxI[2] = i;
            }

            if (Math.abs(totSizeScaled[TourType.UNIVERSITY][i] - univAttrs[i]) > diff[3]) {
                diff[3] = Math.abs(totSizeScaled[TourType.UNIVERSITY][i] -
                        univAttrs[i]);
                maxI[3] = i;
            }

            if (Math.abs(totSizeScaled[TourType.SCHOOL][i] - schoolAttrs[i]) > diff[4]) {
                diff[4] = Math.abs(totSizeScaled[TourType.SCHOOL][i] -
                        schoolAttrs[i]);
                maxI[4] = i;
            }
        }

        logger.info(
            "maximum residual discrepency between scaled size variables and modeled attractions by type:");
        logger.info("work lo:     scaled size=" +
            loSizeScaled[TourType.WORK][maxI[0]] + ", model attrs=" +
            loAttrs[maxI[0]] + ", abs diff=" + diff[0] + ", rel diff=" +
            (diff[0] / loSizeScaled[TourType.WORK][maxI[0]]));
        logger.info("work md:     scaled size=" +
            mdSizeScaled[TourType.WORK][maxI[1]] + ", model attrs=" +
            mdAttrs[maxI[1]] + ", abs diff=" + diff[1] + ", rel diff=" +
            (diff[1] / mdSizeScaled[TourType.WORK][maxI[1]]));
        logger.info("work hi:     scaled size=" +
            hiSizeScaled[TourType.WORK][maxI[2]] + ", model attrs=" +
            hiAttrs[maxI[2]] + ", abs diff=" + diff[2] + ", rel diff=" +
            (diff[2] / hiSizeScaled[TourType.WORK][maxI[2]]));
        logger.info("university:  scaled size=" +
            totSizeScaled[TourType.UNIVERSITY][maxI[3]] + ", model attrs=" +
            univAttrs[maxI[3]] + ", abs diff=" + diff[3] + ", rel diff=" +
            (diff[3] / totSizeScaled[TourType.UNIVERSITY][maxI[3]]));
        logger.info("school:      scaled size=" +
            totSizeScaled[TourType.SCHOOL][maxI[4]] + ", model attrs=" +
            schoolAttrs[maxI[4]] + ", abs diff=" + diff[4] + ", rel diff=" +
            (diff[4] / totSizeScaled[TourType.SCHOOL][maxI[4]]));
    }


    public void updateSizeVariables() {
        for (int i = 1; i <= numberOfSubzones; i++) {
            loSize[TourType.WORK][i] = loSizeScaled[TourType.WORK][i] * loShadowPrice[i];
            mdSize[TourType.WORK][i] = mdSizeScaled[TourType.WORK][i] * mdShadowPrice[i];
            hiSize[TourType.WORK][i] = hiSizeScaled[TourType.WORK][i] * hiShadowPrice[i];
            totSize[TourType.UNIVERSITY][i] = totSizeScaled[TourType.UNIVERSITY][i] * univShadowPrice[i];
            totSize[TourType.SCHOOL][i] = totSizeScaled[TourType.SCHOOL][i] * schoolShadowPrice[i];

            if (loSize[TourType.WORK][i] < 0.0f) {
                loSize[TourType.WORK][i] = 0.0f;
            }

            if (mdSize[TourType.WORK][i] < 0.0f) {
                mdSize[TourType.WORK][i] = 0.0f;
            }

            if (hiSize[TourType.WORK][i] < 0.0f) {
                hiSize[TourType.WORK][i] = 0.0f;
            }

            if (totSize[TourType.UNIVERSITY][i] < 0.0f) {
                totSize[TourType.UNIVERSITY][i] = 0.0f;
            }

            if (totSize[TourType.SCHOOL][i] < 0.0f) {
                totSize[TourType.SCHOOL][i] = 0.0f;
            }

            loMdSize[TourType.WORK][i] = loSize[TourType.WORK][i] + mdSize[TourType.WORK][i];
            totSize[TourType.WORK][i] = loSize[TourType.WORK][i] + mdSize[TourType.WORK][i] + hiSize[TourType.WORK][i];
        }
    }

    public void updateShadowPrices() {
        for (int i = 1; i <= numberOfSubzones; i++) {
			loShadowPrice[i] *= ((loSizeScaled[TourType.WORK][i] + 1) / (loAttrs[i] + 1));
			mdShadowPrice[i] *= ((mdSizeScaled[TourType.WORK][i] + 1) / (mdAttrs[i] + 1));
			hiShadowPrice[i] *= ((hiSizeScaled[TourType.WORK][i] + 1) / (hiAttrs[i] + 1));
			univShadowPrice[i] *= ((totSizeScaled[TourType.UNIVERSITY][i] + 1) / (univAttrs[i] + 1));
			schoolShadowPrice[i] *= ((totSizeScaled[TourType.SCHOOL][i] + 1) / (schoolAttrs[i] + 1));
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
        tableHeadings.add("lo_size_original");
        tableHeadings.add("md_size_original");
        tableHeadings.add("hi_size_original");
        tableHeadings.add("univ_size_original");
        tableHeadings.add("school_size_original");
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
            tableData[i - 1][3] = (float) prods[0][i];
            tableData[i - 1][4] = (float) prods[1][i];
            tableData[i - 1][5] = (float) prods[2][i];
            tableData[i - 1][6] = (float) prods[3][i];
            tableData[i - 1][7] = (float) prods[4][i];
            tableData[i - 1][8] = (float) loSizeOriginal[TourType.WORK][i];
            tableData[i - 1][9] = (float) mdSizeOriginal[TourType.WORK][i];
            tableData[i - 1][10] = (float) hiSizeOriginal[TourType.WORK][i];
            tableData[i - 1][11] = (float) totSizeOriginal[TourType.UNIVERSITY][i];
            tableData[i - 1][12] = (float) totSizeOriginal[TourType.SCHOOL][i];
            tableData[i - 1][13] = (float) loSizeScaled[TourType.WORK][i];
            tableData[i - 1][14] = (float) mdSizeScaled[TourType.WORK][i];
            tableData[i - 1][15] = (float) hiSizeScaled[TourType.WORK][i];
            tableData[i - 1][16] = (float) totSizeScaled[TourType.UNIVERSITY][i];
            tableData[i - 1][17] = (float) totSizeScaled[TourType.SCHOOL][i];
            tableData[i - 1][18] = (float) loSizePrevious[TourType.WORK][i];
            tableData[i - 1][19] = (float) mdSizePrevious[TourType.WORK][i];
            tableData[i - 1][20] = (float) hiSizePrevious[TourType.WORK][i];
            tableData[i - 1][21] = (float) totSizePrevious[TourType.UNIVERSITY][i];
            tableData[i - 1][22] = (float) totSizePrevious[TourType.SCHOOL][i];
            tableData[i - 1][23] = (float) loAttrs[i];
            tableData[i - 1][24] = (float) mdAttrs[i];
            tableData[i - 1][25] = (float) hiAttrs[i];
            tableData[i - 1][26] = (float) univAttrs[i];
            tableData[i - 1][27] = (float) schoolAttrs[i];
            tableData[i - 1][28] = (float) loSize[TourType.WORK][i];
            tableData[i - 1][29] = (float) mdSize[TourType.WORK][i];
            tableData[i - 1][30] = (float) hiSize[TourType.WORK][i];
            tableData[i - 1][31] = (float) totSize[TourType.UNIVERSITY][i];
            tableData[i - 1][32] = (float) totSize[TourType.SCHOOL][i];
            tableData[i - 1][33] = (float) loShadowPrice[i];
            tableData[i - 1][34] = (float) mdShadowPrice[i];
            tableData[i - 1][35] = (float) hiShadowPrice[i];
            tableData[i - 1][36] = (float) univShadowPrice[i];
            tableData[i - 1][37] = (float) schoolShadowPrice[i];
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
            loSizePrevious[TourType.WORK][i] = loSize[TourType.WORK][i];
            mdSizePrevious[TourType.WORK][i] = mdSize[TourType.WORK][i];
            hiSizePrevious[TourType.WORK][i] = hiSize[TourType.WORK][i];
            totSizePrevious[TourType.UNIVERSITY][i] = totSize[TourType.UNIVERSITY][i];
            totSizePrevious[TourType.SCHOOL][i] = totSize[TourType.SCHOOL][i];
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
				reader.setDelimSet( " ,\t\n\r\f\"");
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
		staticDataMap.put ( "loSize", loSize );
		staticDataMap.put ( "mdSize", mdSize );
		staticDataMap.put ( "hiSize", hiSize );
		staticDataMap.put ( "loMdSize", loMdSize );
		staticDataMap.put ( "totSize", totSize );
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
	 * @return
	 */
	public void setStaticData ( HashMap staticDataMap ) {

		walkPctArray     = (float[][])staticDataMap.get ( "walkPctArray" );
		loSize           = (float[][])staticDataMap.get ( "loSize" );
		mdSize           = (float[][])staticDataMap.get ( "mdSize" );
		hiSize           = (float[][])staticDataMap.get ( "hiSize" );
		loMdSize         = (float[][])staticDataMap.get ( "loMdSize" );
		totSize          = (float[][])staticDataMap.get ( "totSize" );
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

		numberOfZones = loSize[TourType.WORK].length;

	}



	public float getTotSize ( int tourTypeIndex, int altIndex ) {
		return totSize[tourTypeIndex][altIndex];
	}



	public int getNumberOfZones () {
		return numberOfZones;
	}



	public void setLogsumDcAMPM ( int processorId, int altIndex, float logsum ) {
		logsumDcAMPM[processorId][altIndex] = logsum;
	}


	public void setLogsumDcAMMD ( int processorId, int altIndex, float logsum ) {
		logsumDcAMMD[processorId][altIndex] = logsum;
	}


	public void setLogsumDcMDMD ( int processorId, int altIndex, float logsum ) {
		logsumDcMDMD[processorId][altIndex] = logsum;
	}


	public void setLogsumDcPMNT ( int processorId, int altIndex, float logsum ) {
		logsumDcPMNT[processorId][altIndex] = logsum;
	}


	public void setOdUtilModeAlt ( int processorId, double[] ModalUtilities ) {
	    odUtilModeAlt[processorId] = ModalUtilities;
	}

	/**
	 * update property map in each global iteration
	 */
	public void updatePropertyMap(int iteration){
		propertyMap=ResourceUtil.getResourceBundleAsHashMap("morpc"+iteration);
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