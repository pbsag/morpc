package com.pb.morpc.models;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MathUtil;
import com.pb.common.util.ResourceUtil;

import java.io.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Nov. 15 2003
 */
public class Derived {
    private TableDataSet zsed;
    private TableDataSet accessi;
    int NoOfZones;
    float[] areaType;
    float[] urbType;
    float[] parkRate;
    String[] colTitles = { "areatype", "urbtype", "parkrate" };
    HashMap propertyMap;

    protected static transient Logger logger = Logger.getLogger( Derived.class );

    public Derived(HashMap propertyMap) {

        this.propertyMap = propertyMap;
        
        try {
            CSVFileReader reader = new CSVFileReader();
            zsed = reader.readFile(new File((String) propertyMap.get("TAZMainData.file")));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        try {
            CSVFileReader reader = new CSVFileReader();
            accessi = reader.readFile(new File((String) propertyMap.get("TAZAccessibility.file")));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        NoOfZones = zsed.getRowCount();
        areaType = new float[NoOfZones];
        urbType = new float[NoOfZones];
        parkRate = new float[NoOfZones];

        areaType = createAreaType();
        urbType = createUrbType();
        parkRate = createParkRate();
    }

    public void writeDerivedTable() {
        float[] taz = new float[NoOfZones];
        ArrayList colTitlesArrayList = new ArrayList();

        for (int i = 0; i < colTitles.length; i++) {
            colTitlesArrayList.add(colTitles[i]);
        }

        for (int i = 0; i < NoOfZones; i++) {
            taz[i] = i + 1;
        }

        TableDataSet completeTable = new TableDataSet();
        completeTable.appendColumn(taz, "taz");
        completeTable.appendColumn(areaType, "areatype");
        completeTable.appendColumn(urbType, "urbtype");
        completeTable.appendColumn(parkRate, "parkrate");

        try {
            File AccessibilityIndicesFile = new File((String) propertyMap.get("TAZDerived.file"));
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(completeTable, AccessibilityIndicesFile, new DecimalFormat("#.00"));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        
    }

    private void logDerivedTableFreqs() {
        float[] taz = new float[NoOfZones];
        ArrayList colTitlesArrayList = new ArrayList();

        for (int i = 0; i < colTitles.length; i++) {
            colTitlesArrayList.add(colTitles[i]);
        }

        for (int i = 0; i < NoOfZones; i++) {
            taz[i] = i + 1;
        }

        TableDataSet completeTable = new TableDataSet();
        completeTable.appendColumn(taz, "taz");
        completeTable.appendColumn(areaType, "areatype");
        completeTable.appendColumn(urbType, "urbtype");
        completeTable.appendColumn(parkRate, "parkrate");

        TableDataSet.logColumnFreqReport("Zonal Data", completeTable, completeTable.getColumnPosition("areaType"));
        TableDataSet.logColumnFreqReport("Zonal Data", completeTable, completeTable.getColumnPosition("urbtype"));

        int columnPosition = completeTable.getColumnPosition("parkrate");
        
        logger.info( "Frequency Report table: " + "Zonal Data");
        logger.info( "Listing for column " + columnPosition + ": " + completeTable.getColumnLabel(columnPosition) );
        logger.info( String.format("%8s", "Taz") + String.format("%11s", "Value") );
        
        float[] columnData = new float[completeTable.getRowCount()];
        for (int r = 1; r <= completeTable.getRowCount(); r++) {
            columnData[r - 1] = completeTable.getValueAt(r, columnPosition);
        }

        
        int zeroCount = 0;
        for (int i=0; i < NoOfZones; i++) {
            
            if ( columnData[i] != 0 )
                logger.info( String.format("%8d", i+1) + String.format("%11.5f", columnData[i]) );
            else
                zeroCount++;
            
        }
        logger.info( "The other " + zeroCount + " Tazs have values of 0.");
            
    }

    private float[] createAreaType() {
        float[] result = new float[NoOfZones];
        float[] gqpop = zsed.getColumnAsFloat(zsed.getColumnPosition("gqpop"));
        float[] hhpop = zsed.getColumnAsFloat(zsed.getColumnPosition("hhpop"));
        float[] empoff = zsed.getColumnAsFloat(zsed.getColumnPosition("empoff"));
        float[] empretgds = zsed.getColumnAsFloat(zsed.getColumnPosition(
                    "empretgds"));
        float[] empretsrv = zsed.getColumnAsFloat(zsed.getColumnPosition(
                    "empretsrv"));
        float[] empother = zsed.getColumnAsFloat(zsed.getColumnPosition(
                    "empother"));
        float[] area = zsed.getColumnAsFloat(zsed.getColumnPosition("area"));
        float popden;
        float empden;

        for (int i = 0; i < NoOfZones; i++) {
            popden = (gqpop[i] + hhpop[i]) / area[i];
            empden = (empoff[i] + empretgds[i] + empretsrv[i] + empother[i]) / area[i];

            if (empden >= 5000) {
                result[i] = 2;
            }

            if ((empden >= 2000) && (empden < 5000)) {
                result[i] = 3;
            }

            if ((popden >= 4000) && (empden < 2000)) {
                result[i] = 4;
            }

            if (((popden >= 1000) && (popden < 4000) && (empden < 2000)) ||
                    ((empden >= 1000) && (empden < 2000) && (popden < 4000))) {
                result[i] = 5;
            }

            if (((popden >= 500) && (popden < 1000) && (empden < 1000)) ||
                    ((empden >= 500) && (empden < 1000) && (popden < 1000))) {
                result[i] = 6;
            }

            if ((popden < 500) && (empden < 500)) {
                result[i] = 7;
            }
            
            // zonal arrays are 0 based, so index i is taz-1
            // original logic: if ((i <= 72) && (i != 35) && (i != 54))
            if ((i <= 71) && (i != 34) && (i != 53))
                result[i] = 1;

        }

        return result;
    }

    private float[] createUrbType() {
        for (int i = 0; i < NoOfZones; i++) {
            if ((areaType[i] >= 1) && (areaType[i] <= 3)) {
                urbType[i] = 1;
            }

            if ((areaType[i] >= 4) && (areaType[i] <= 5)) {
                urbType[i] = 2;
            }

            if ((areaType[i] >= 6) && (areaType[i] <= 7)) {
                urbType[i] = 3;
            }
        }

        return urbType;
    }

    private float[] createParkRate() {
        double[] result = new double[NoOfZones];
        float[] resultf = new float[NoOfZones];
        float[] work_wal = accessi.getColumnAsFloat(accessi.getColumnPosition(
                    "work_walk"));
        float[] parklng = zsed.getColumnAsFloat(zsed.getColumnPosition(
                    "parklng"));
        float[] cbdatype = zsed.getColumnAsFloat(zsed.getColumnPosition(
                    "cbdatype"));
        float[] propfree = zsed.getColumnAsFloat(zsed.getColumnPosition(
                    "propfree"));

        double lgwrkskm = 0.0;
        double lgnlngal = 0.0;
        double free75 = 0.0;

        for (int i = 0; i < NoOfZones; i++) {
            
            if (work_wal[i] == 0.0) {
                lgwrkskm = 0.0;
            }

            if (work_wal[i] > 0.0) {
                lgwrkskm = MathUtil.log((double) work_wal[i]);
            }

            if (parklng[i] == 0.0) {
                lgnlngal = 0.0;
            }

            if (parklng[i] > 0.0) {
                lgnlngal = MathUtil.log((double) parklng[i]);
            }

            if (propfree[i] < 0.75) {
                free75 = 0.0;
            }

            if (propfree[i] >= 0.75) {
                free75 = 1.0;
            }

            if (cbdatype[i] == 1.0) {
                result[i] = (-11.799 + (1.126 * lgwrkskm) + (0.059 * lgnlngal)) -
                    (0.182 * free75);
            } else if (cbdatype[i] > 1.0) {
                result[i] = (-0.676 + (0.137 * lgwrkskm)) - (0.040 * lgnlngal) -
                    (0.118 * free75);
            } else {
                result[i] = 0.0;
            }

            if ( result[i] > 0 )
                resultf[i] = (float) result[i];
            else
                resultf[i] = 0;
            
        }

        return resultf;
    }

    
    public static void main(String[] args) {
        
        // pass the name of the properties file (excluding ".properties")
        HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap( args[0] );

        Derived dr = new Derived(propertyMap);
        dr.logDerivedTableFreqs();
        dr.writeDerivedTable();
        
        logger.info ( "end of Derived.main()" );
    }
}
