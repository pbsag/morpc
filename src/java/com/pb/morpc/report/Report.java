package com.pb.morpc.report;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.NDimensionalMatrix;
import com.pb.common.util.ResourceUtil;

import java.io.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
//import java.util.logging.Level;
import org.apache.log4j.Logger;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Oct. 29, 2003
 */
public class Report {
    private Logger logger;
    TableDataSet HHTable;
    TableDataSet PersonTable;
    TableDataSet M5678Table;
    TableDataSet M1Table;
    TableDataSet M21Table;
    TableDataSet DistrictTable;
    HashMap propertyMap;
    String reportDirectory;

    public Report() {
        //get a logger object
        logger = Logger.getLogger("com.pb.morpc.report");
        propertyMap = ResourceUtil.getResourceBundleAsHashMap("morpc");
        reportDirectory = (String) propertyMap.get("report.directory");

        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            HHTable = reader.readFile(new File((String) propertyMap.get("SyntheticHousehold.file")));
        } catch (Exception e) {
			e.printStackTrace();
            System.out.println("can not open SynPopHH table");
        }
/*
        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            PersonTable = reader.readFile(new File((String) propertyMap.get("SyntheticPerson.file")));
        } catch (Exception e) {
			e.printStackTrace();
            System.out.println("can not open SynPopP table");
        }
*/
        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            M1Table = reader.readFile(new File((String) propertyMap.get("Model1.outputFile")));
        } catch (Exception e) {
			e.printStackTrace();
            System.out.println("can not open M1 table");
        }

        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            M21Table = reader.readFile(new File((String) propertyMap.get("Model21.outputFile")));
        } catch (Exception e) {
			e.printStackTrace();
            System.out.println("can not open M21 table");
        }

        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            M5678Table = reader.readFile(new File((String) propertyMap.get("Model567.outputFile")));
        } catch (Exception e) {
			e.printStackTrace();
            System.out.println("can not open M5678 table");
        }

        try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            DistrictTable = reader.readFile(new File((String) propertyMap.get("TAZEquivalency.file")));
        } catch (Exception e) {
			e.printStackTrace();
            System.out.println("can not open TAZEquivalency table");
        }

        addDistrictCol(HHTable, "hh_taz_id", DistrictTable, "taz", "suprdist", "suprdist");
    }

    private TableDataSet addDistrictCol(TableDataSet originalTable,
        String joinKey_o, TableDataSet districtTable, String joinKey_d,
        String districtCol, String newCol) {
        float[] joinCol_o = originalTable.getColumnAsFloat(originalTable.getColumnPosition(
                    joinKey_o));
        float[] district = new float[joinCol_o.length];
        float[] joinCol_d = districtTable.getColumnAsFloat(districtTable.getColumnPosition(
                    joinKey_d));
        float[] district_d = districtTable.getColumnAsFloat(districtTable.getColumnPosition(
                    districtCol));

        for (int i = 0; i < joinCol_o.length; i++) {
            for (int j = 0; j < joinCol_d.length; j++) {
                if (joinCol_o[i] == joinCol_d[j]) {
                    district[i] = district_d[j];
                }
            }
        }

        originalTable.appendColumn(district, newCol);

        return originalTable;
    }

    private TableDataSet straightAppendCol(TableDataSet t1, TableDataSet t2,
        String colName) {
        float[] col = t2.getColumnAsFloat(t2.getColumnPosition(colName));
        t1.appendColumn(col, colName);

        return t1;
    }

    public void generateReports() {
        //echo morpc properties
        //echoProperty();

        Vector properties = getReportProperties();

        for (int p = 0; p < properties.size(); p++) {
            ReportPropertyParser rpp = new ReportPropertyParser((HashMap) properties.get(
                        p));
            String[] dTitles = rpp.getDimensionTitles();
            Vector sTitles = rpp.getShapeTitles();
            Vector sValues = rpp.getShapeValues();
            int[] shape = rpp.getMatrixShape();
            int dimensions = rpp.getMatrixDimensions();
            String mName = rpp.getMatrixName();
            String rLocation = reportDirectory + rpp.getReportLocation();
            String format = rpp.getFormat();
            String type = rpp.getType();
            String formatLine = rpp.getFormatLine();

            //construct an empty NDimensionalMatrix
            NDimensionalMatrix emptyMatrix = new NDimensionalMatrix(mName,
                    dimensions, shape);

            //construct a matrix to hold populted matrix
            NDimensionalMatrix matrix = new NDimensionalMatrix();

            //populate matrix
            if (mName.equals("HH Income By Origin District")) {
                matrix = populateIncomeDistrictMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("HHSize By Origin District")) {
                matrix = populateHHSizeDistrictMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("District_orig By District_dest By Purpose")) {
                matrix = populateDistrict_oDistrict_dPurposeMatrix(emptyMatrix,
                        shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Autos By District")) {
                matrix = populateAutosDistrictMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Autos By Income")) {
                matrix = populateAutosIncomeMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Autos By HHSize")) {
                matrix = populateAutosHHSizeMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Households By Income")) {
                matrix = populateIncomeFrqMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Households By Size")) {
                matrix = populateHHSizeFrqMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Households By Autos")) {
                matrix = populateAutosFrqMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }
            /*
            if (mName.equals("Daily Activity Pattern -- all individuals")) {
                matrix = populateMandatoryTravelFrqMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }
            
            
            if(mName.equals("Mandatory Travel By District")){
                matrix = populateMandatoryTravelDistrictMatrix(emptyMatrix, shape, sValues);
                //create table from matrix
                Table table=new Table(matrix, mName, dTitles, sTitles);
                //write table to hard drive
                table.writeTable(rLocation, format, type,formatLine);
            }
            */
            if (mName.equals("Origin District By Destination District")) {
                matrix = populateDistrict_oDistrict_dMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Origin District By Destination District By Mode")) {
                matrix = populateDistrict_oDistrict_dM7Matrix(emptyMatrix,
                        shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Full Time Workers By Income")) {
                matrix = populateFtwIncomeMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Part Time Workers By Income")) {
                matrix = populatePtwIncomeMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Workers By Income")) {
                matrix = populateWorkerIncomeMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Purpose By Mode--Total Tours")) {
                matrix = populatePurposeModeTotMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Purpose By Mode--Individual Tours")) {
                matrix = populatePurposeModeIndiMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Purpose By Mode--Joint Tours")) {
                matrix = populatePurposeModeJointMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Person Type By Purpose--Total Tours")) {
                matrix = populatePersonTypePurposeTotMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Person Type By Purpose--Individual Tours")) {
                matrix = populatePersonTypePurposeIndiMatrix(emptyMatrix,
                        shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Person Type By Purpose--Joint Tours")) {
                matrix = populatePersonTypePurposeJointMatrix(emptyMatrix,
                        shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Party Size By Purpose--Joint Tours")) {
                matrix = populatePartySizePurposeJointMatrix(emptyMatrix,
                        shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Party Size Frequency--Joint Tours")) {
                matrix = populatePartySizeJointFrqMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Mode Frequency--Total Tours")) {
                matrix = populateModeFrqTotMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Mode Frequency--Individual Tours")) {
                matrix = populateModeFrqIndiMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Mode Frequency--Joint Tours")) {
                matrix = populateModeFrqJointMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Purpose Frequency--Total Tours")) {
                matrix = populatePurposeFrqTotMatrix(emptyMatrix, shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Purpose Frequency--Individual Tours")) {
                matrix = populatePurposeFrqIndiMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Purpose Frequency--Joint Tours")) {
                matrix = populatePurposeFrqJointMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals("Person Type Frequency--Joint Tours")) {
                matrix = populatePersontypeFrqJointMatrix(emptyMatrix, shape,
                        sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }

            if (mName.equals(
                        "Purpose By Person Type--Individuals in Joint Tours")) {
                matrix = populatePurposePersontypeJointMatrix(emptyMatrix,
                        shape, sValues);

                //create table from matrix
                Table table = new Table(matrix, mName, dTitles, sTitles);

                //write table to hard drive
                table.writeTable(rLocation, format, type, formatLine);
            }
        }
    }

    private NDimensionalMatrix populateIncomeDistrictMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = HHTable.getRowCount();
        float[] income = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "income"));
        float[] district = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "suprdist"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] hhincomeVals = (int[]) sValues.get(0);
                    int[] districtVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((hhincomeVals[j] == income[i]) &&
                            (districtVals[k] == district[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateFtwIncomeMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = HHTable.getRowCount();
        float[] income = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "income"));
        float[] ftw = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_f"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //truncate full time workers to 3+
            if (ftw[i] > 3) {
                ftw[i] = 3;
            }

            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] ftwVals = (int[]) sValues.get(0);
                    int[] incomeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((ftwVals[j] == ftw[i]) && (incomeVals[k] == income[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePtwIncomeMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = HHTable.getRowCount();
        float[] income = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "income"));
        float[] ptw = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_p"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //truncate part time workers to 3
            if (ptw[i] > 3) {
                ptw[i] = 3;
            }

            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] ptwVals = (int[]) sValues.get(0);
                    int[] incomeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((ptwVals[j] == ptw[i]) && (incomeVals[k] == income[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateWorkerIncomeMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = HHTable.getRowCount();
        float[] workers = new float[NoRows];
        float[] income = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "income"));
        float[] ptw = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_p"));
        float[] ftw = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_f"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            workers[i] = ptw[i] + ftw[i];

            //truncate workers to 3+
            if (workers[i] > 3) {
                workers[i] = 3;
            }

            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] workersVals = (int[]) sValues.get(0);
                    int[] incomeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((workersVals[j] == workers[i]) &&
                            (incomeVals[k] == income[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateAutosDistrictMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;

        TableDataSet joinedTable = addDistrictCol(M1Table, "hh_taz_id",
                DistrictTable, "taz", "suprdist", "suprdist");

        int NoRows = joinedTable.getRowCount();
        float[] autos = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "M1"));
        float[] district = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "suprdist"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] autosVals = (int[]) sValues.get(0);
                    int[] districtVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((autosVals[j] == autos[i]) &&
                            (districtVals[k] == district[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateAutosIncomeMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;

        TableDataSet joinedTable = straightAppendCol(HHTable, M1Table, "M1");

        int NoRows = joinedTable.getRowCount();
        float[] autos = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "M1"));
        float[] income = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "income"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] autosVals = (int[]) sValues.get(0);
                    int[] incomeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((autosVals[j] == autos[i]) &&
                            (incomeVals[k] == income[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateAutosHHSizeMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;

        TableDataSet joinedTable = straightAppendCol(HHTable, M1Table, "M1");

        int NoRows = joinedTable.getRowCount();
        float[] hhsize = new float[NoRows];
        float[] workers_f = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_f"));
        float[] workers_p = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_p"));
        float[] students = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "students"));
        float[] nonworkers = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "nonworkers"));
        float[] preschool = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "preschool"));
        float[] schoolpred = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "schoolpred"));
        float[] schooldriv = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "schooldriv"));

        float[] autos = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "M1"));

        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            hhsize[i] = workers_f[i] + workers_p[i] + students[i] +
                nonworkers[i] + preschool[i] + schoolpred[i] + schooldriv[i];

            //truncate household size to 6+
            if (hhsize[i] > 6) {
                hhsize[i] = 6;
            }

            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] autosVals = (int[]) sValues.get(0);
                    int[] hhsizeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((autosVals[j] == autos[i]) &&
                            (hhsizeVals[k] == hhsize[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateHHSizeDistrictMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = HHTable.getRowCount();
        float[] hhsize = new float[NoRows];
        float[] workers_f = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_f"));
        float[] workers_p = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_p"));
        float[] students = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "students"));
        float[] nonworkers = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "nonworkers"));
        float[] preschool = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "preschool"));
        float[] schoolpred = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "schoolpred"));
        float[] schooldriv = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "schooldriv"));
        float[] district = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "suprdist"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            hhsize[i] = workers_f[i] + workers_p[i] + students[i] +
                nonworkers[i] + preschool[i] + schoolpred[i] + schooldriv[i];

            //truncate household size to 6+
            if (hhsize[i] > 6) {
                hhsize[i] = 6;
            }

            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] hhsizeVals = (int[]) sValues.get(0);
                    int[] districtVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((hhsizeVals[j] == hhsize[i]) &&
                            (districtVals[k] == district[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    //for "tour cat" 4, make corresponding "purpose" 9
    //district_o, join equivalency and m5678, keys are taz and tour_orig
    //district_d, join equivalency and m5678, keys are taz and M5_DC_TAZid
    private NDimensionalMatrix populateDistrict_oDistrict_dPurposeMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        TableDataSet joinedTable = addDistrictCol(M5678Table, "tour_orig",
                DistrictTable, "taz", "suprdist", "dist_o");
        joinedTable = addDistrictCol(M5678Table, "M5_DC_TAZid", DistrictTable,
                "taz", "suprdist", "dist_d");

        float[] dist_o = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "dist_o"));
        float[] dist_d = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "dist_d"));
        float[] purpose = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "purpose"));
        float[] tourCat = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "tourCategory"));

        NDimensionalMatrix result = emptyMatrix;
        int NoRows = joinedTable.getRowCount();
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    for (int m = 0; m < shape[2]; m++) {
                        //cell position in matrix
                        int[] position = { j, k, m };
                        int[] dist_oVals = (int[]) sValues.get(0);
                        int[] dist_dVals = (int[]) sValues.get(1);
                        int[] purposeVals = (int[]) sValues.get(2);

                        if (tourCat[i] != 4.0) {
                            //increase 1 if dist_o val, dist_d val, and district val match those got from hh array at this position
                            if ((dist_dVals[j] == dist_d[i]) &&
                                    (dist_oVals[k] == dist_o[i]) &&
                                    (purposeVals[m] == purpose[i])) {
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        } else {
                            if ((dist_dVals[j] == dist_d[i]) &&
                                    (dist_oVals[k] == dist_o[i])) {
                                position[2] = 8;
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateIncomeFrqMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = HHTable.getRowCount();
        float[] income = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "income"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[1]; j++) {
                int[] position = { 0, j };
                int[] hhincomeVals = (int[]) sValues.get(1);

                if (hhincomeVals[j] == income[i]) {
                    m_cell = result.getValue(position);
                    result.setValue(m_cell + 1, position);
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateHHSizeFrqMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = HHTable.getRowCount();
        float[] hhsize = new float[NoRows];
        float[] workers_f = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_f"));
        float[] workers_p = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "workers_p"));
        float[] students = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "students"));
        float[] nonworkers = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "nonworkers"));
        float[] preschool = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "preschool"));
        float[] schoolpred = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "schoolpred"));
        float[] schooldriv = HHTable.getColumnAsFloat(HHTable.getColumnPosition(
                    "schooldriv"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            hhsize[i] = workers_f[i] + workers_p[i] + students[i] +
                nonworkers[i] + preschool[i] + schoolpred[i] + schooldriv[i];

            if (hhsize[i] > 6) {
                hhsize[i] = 6;
            }

            //populate each cell
            for (int j = 0; j < shape[1]; j++) {
                int[] position = { 0, j };
                int[] hhsizeVals = (int[]) sValues.get(1);

                if (hhsizeVals[j] == hhsize[i]) {
                    m_cell = result.getValue(position);
                    result.setValue(m_cell + 1, position);
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateAutosFrqMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M1Table.getRowCount();
        float[] autos = M1Table.getColumnAsFloat(M1Table.getColumnPosition("M1"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[1]; j++) {
                int[] position = { 0, j };
                int[] autosVals = (int[]) sValues.get(1);

                if (autosVals[j] == autos[i]) {
                    m_cell = result.getValue(position);
                    result.setValue(m_cell + 1, position);
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateMandatoryTravelFrqMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = PersonTable.getRowCount();
        float[] mandatoryTravel = PersonTable.getColumnAsFloat(PersonTable.getColumnPosition(
                    "M2"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[1]; j++) {
                int[] position = { 0, j };
                int[] mandatoryTravelVals = (int[]) sValues.get(1);

                if (mandatoryTravelVals[j] == mandatoryTravel[i]) {
                    m_cell = result.getValue(position);
                    result.setValue(m_cell + 1, position);
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateMandatoryTravelDistrictMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;

        TableDataSet joinedTable = straightAppendCol(HHTable, PersonTable, "M2");

        int NoRows = joinedTable.getRowCount();
        float[] mandatoryTravel = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "M2"));
        float[] district = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "suprdist"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] mandatoryTravelVals = (int[]) sValues.get(0);
                    int[] districtVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if ((mandatoryTravelVals[j] == mandatoryTravel[i]) &&
                            (districtVals[k] == district[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateDistrict_oDistrict_dMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        TableDataSet joinedTable = addDistrictCol(M5678Table, "tour_orig",
                DistrictTable, "taz", "suprdist", "dist_o");
        joinedTable = addDistrictCol(M5678Table, "M5_DC_TAZid", DistrictTable,
                "taz", "suprdist", "dist_d");

        float[] dist_o = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "dist_o"));
        float[] dist_d = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "dist_d"));

        NDimensionalMatrix result = emptyMatrix;
        int NoRows = joinedTable.getRowCount();
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] dist_oVals = (int[]) sValues.get(0);
                    int[] dist_dVals = (int[]) sValues.get(1);

                    if ((dist_dVals[j] == dist_d[i]) &&
                            (dist_oVals[k] == dist_o[i])) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateDistrict_oDistrict_dM7Matrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        TableDataSet joinedTable = addDistrictCol(M5678Table, "tour_orig",
                DistrictTable, "taz", "suprdist", "dist_o");
        joinedTable = addDistrictCol(M5678Table, "M5_DC_TAZid", DistrictTable,
                "taz", "suprdist", "dist_d");

        float[] dist_o = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "dist_o"));
        float[] dist_d = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "dist_d"));
        float[] M7 = joinedTable.getColumnAsFloat(joinedTable.getColumnPosition(
                    "M7_MC"));

        NDimensionalMatrix result = emptyMatrix;
        int NoRows = joinedTable.getRowCount();
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    for (int m = 0; m < shape[2]; m++) {
                        //cell position in matrix
                        int[] position = { j, k, m };
                        int[] dist_oVals = (int[]) sValues.get(0);
                        int[] dist_dVals = (int[]) sValues.get(1);
                        int[] M7Vals = (int[]) sValues.get(2);

                        //increase 1 if dist_o val, dist_d val, and district val match those got from hh array at this position
                        if ((dist_dVals[j] == dist_d[i]) &&
                                (dist_oVals[k] == dist_o[i]) &&
                                (M7Vals[m] == M7[i])) {
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + 1, position);
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePurposeModeTotMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] mode = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "M7_MC"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) { //mode

                for (int k = 0; k < shape[1]; k++) { //purpose

                    //cell position in matrix
                    int[] position = { j, k };
                    int[] modeVals = (int[]) sValues.get(0);
                    int[] purposeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if (tourCat[i] != 4.0) {
                        if ((purposeVals[k] == purpose[i]) &&
                                (modeVals[j] == mode[i])) {
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + 1, position);
                        }
                    } else {
                        if ((modeVals[j] == mode[i])) {
                            position[1] = 8;
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + 1, position);
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePurposeModeJointMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] mode = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "M7_MC"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] == 2.0) {
                //populate each cell
                for (int j = 0; j < shape[0]; j++) { //mode

                    for (int k = 0; k < shape[1]; k++) { //purpose

                        //cell position in matrix
                        int[] position = { j, k };
                        int[] modeVals = (int[]) sValues.get(0);
                        int[] purposeVals = (int[]) sValues.get(1);

                        //increase 1 if income val and district val match those got from hh array at this position
                        if (tourCat[i] != 4.0) {
                            if ((purposeVals[k] == purpose[i]) &&
                                    (modeVals[j] == mode[i])) {
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        } else {
                            if ((modeVals[j] == mode[i])) {
                                position[1] = 8;
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePurposeModeIndiMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] mode = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "M7_MC"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] != 2.0) {
                //populate each cell
                for (int j = 0; j < shape[0]; j++) { //mode

                    for (int k = 0; k < shape[1]; k++) { //purpose

                        //cell position in matrix
                        int[] position = { j, k };
                        int[] modeVals = (int[]) sValues.get(0);
                        int[] purposeVals = (int[]) sValues.get(1);

                        //increase 1 if income val and district val match those got from hh array at this position
                        if (tourCat[i] != 4.0) {
                            if ((purposeVals[k] == purpose[i]) &&
                                    (modeVals[j] == mode[i])) {
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        } else {
                            if ((modeVals[j] == mode[i])) {
                                position[1] = 8;
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePersonTypePurposeTotMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] personType = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "personType"));
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) { //purpose

                for (int k = 0; k < shape[1]; k++) { //person type

                    //cell position in matrix
                    int[] position = { j, k };
                    int[] purposeVals = (int[]) sValues.get(0);
                    int[] persontypeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if (tourCat[i] != 4.0) {
                        if ((persontypeVals[k] == personType[i]) &&
                                (purposeVals[j] == purpose[i])) {
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + 1, position);
                        }
                    } else {
                        if ((persontypeVals[k] == personType[i])) {
                            position[0] = 8;
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + 1, position);
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePersonTypePurposeIndiMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] personType = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "personType"));
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) { //purpose

                for (int k = 0; k < shape[1]; k++) { //person type

                    //cell position in matrix
                    int[] position = { j, k };
                    int[] purposeVals = (int[]) sValues.get(0);
                    int[] persontypeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if (tourCat[i] != 2.0) {
                        if (tourCat[i] != 4.0) {
                            if ((persontypeVals[k] == personType[i]) &&
                                    (purposeVals[j] == purpose[i])) {
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        } else {
                            if ((persontypeVals[k] == personType[i])) {
                                position[0] = 8;
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePersonTypePurposeJointMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] personType = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "personType"));
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[0]; j++) { //purpose

                for (int k = 0; k < shape[1]; k++) { //person type

                    //cell position in matrix
                    int[] position = { j, k };
                    int[] purposeVals = (int[]) sValues.get(0);
                    int[] persontypeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if (tourCat[i] == 2.0) {
                        if (tourCat[i] != 4.0) {
                            if ((persontypeVals[k] == personType[i]) &&
                                    (purposeVals[j] == purpose[i])) {
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        } else {
                            if ((persontypeVals[k] == personType[i])) {
                                position[0] = 8;
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePartySizePurposeJointMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] partySize = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "jt_party_size"));
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] == 2.0) {
                if (partySize[i] > 5) {
                    partySize[i] = 5;
                }

                //populate each cell
                for (int j = 0; j < shape[0]; j++) { //party size

                    for (int k = 0; k < shape[1]; k++) { //purpose

                        //cell position in matrix
                        int[] position = { j, k };
                        int[] partysizeVals = (int[]) sValues.get(0);
                        int[] purposeVals = (int[]) sValues.get(1);

                        //increase 1 if income val and district val match those got from hh array at this position
                        if (tourCat[i] != 4.0) {
                            if ((partysizeVals[j] == partySize[i]) &&
                                    (purposeVals[k] == purpose[i])) {
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        } else {
                            if ((partysizeVals[j] == partySize[i])) {
                                position[1] = 8;
                                m_cell = result.getValue(position);
                                result.setValue(m_cell + 1, position);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePartySizeJointFrqMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] partysize = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "jt_party_size"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] == 2.0) {
                if (partysize[i] > 5) {
                    partysize[i] = 5;
                }

                //populate each cell
                for (int j = 0; j < shape[1]; j++) {
                    int[] position = { 0, j };
                    int[] partysizeVals = (int[]) sValues.get(1);

                    if (partysizeVals[j] == partysize[i]) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateModeFrqTotMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] mode = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "M7_MC"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[1]; j++) {
                int[] position = { 0, j };
                int[] modeVals = (int[]) sValues.get(1);

                if (modeVals[j] == mode[i]) {
                    m_cell = result.getValue(position);
                    result.setValue(m_cell + 1, position);
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateModeFrqIndiMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] mode = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "M7_MC"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] != 2.0) {
                //populate each cell
                for (int j = 0; j < shape[1]; j++) {
                    int[] position = { 0, j };
                    int[] modeVals = (int[]) sValues.get(1);

                    if (modeVals[j] == mode[i]) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populateModeFrqJointMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] mode = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "M7_MC"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] == 2.0) {
                //populate each cell
                for (int j = 0; j < shape[1]; j++) {
                    int[] position = { 0, j };
                    int[] modeVals = (int[]) sValues.get(1);

                    if (modeVals[j] == mode[i]) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePurposeFrqTotMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            //populate each cell
            for (int j = 0; j < shape[1]; j++) {
                int[] position = { 0, j };
                int[] purposeVals = (int[]) sValues.get(1);

                if (tourCat[i] != 4.0) {
                    if (purposeVals[j] == purpose[i]) {
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                } else {
                    position[1] = 8;
                    m_cell = result.getValue(position);
                    result.setValue(m_cell + 1, position);
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePurposeFrqIndiMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] != 2.0) {
                //populate each cell
                for (int j = 0; j < shape[1]; j++) {
                    int[] position = { 0, j };
                    int[] purposeVals = (int[]) sValues.get(1);

                    if (tourCat[i] != 4.0) {
                        if (purposeVals[j] == purpose[i]) {
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + 1, position);
                        }
                    } else {
                        position[1] = 8;
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePurposeFrqJointMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        float[] purpose = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "purpose"));
        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] == 2.0) {
                //populate each cell
                for (int j = 0; j < shape[1]; j++) {
                    int[] position = { 0, j };
                    int[] purposeVals = (int[]) sValues.get(1);

                    if (tourCat[i] != 4.0) {
                        if (purposeVals[j] == purpose[i]) {
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + 1, position);
                        }
                    } else {
                        position[1] = 8;
                        m_cell = result.getValue(position);
                        result.setValue(m_cell + 1, position);
                    }
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePersontypeFrqJointMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        int[] p0 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_0_type"));
        int[] p1 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_1_type"));
        int[] p2 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_2_type"));
        int[] p3 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_3_type"));
        int[] p4 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_4_type"));
        int[] p5 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_5_type"));
        int[][] persontypeCount = new int[NoRows][shape[1]];

        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));
        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] == 2.0) {
                if (p0[i] != 0) {
                    persontypeCount[i][p0[i] - 1]++;
                }

                if (p1[i] != 0) {
                    persontypeCount[i][p1[i] - 1]++;
                }

                if (p2[i] != 0) {
                    persontypeCount[i][p2[i] - 1]++;
                }

                if (p3[i] != 0) {
                    persontypeCount[i][p3[i] - 1]++;
                }

                if (p4[i] != 0) {
                    persontypeCount[i][p4[i] - 1]++;
                }

                if (p5[i] != 0) {
                    persontypeCount[i][p5[i] - 1]++;
                }

                //populate each cell
                for (int j = 0; j < shape[1]; j++) {
                    int[] position = { 0, j };
                    m_cell = result.getValue(position);
                    result.setValue(m_cell + persontypeCount[i][j], position);
                }
            }
        }

        return result;
    }

    private NDimensionalMatrix populatePurposePersontypeJointMatrix(
        NDimensionalMatrix emptyMatrix, int[] shape, Vector sValues) {
        NDimensionalMatrix result = emptyMatrix;
        int NoRows = M5678Table.getRowCount();
        int[] p0 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_0_type"));
        int[] p1 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_1_type"));
        int[] p2 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_2_type"));
        int[] p3 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_3_type"));
        int[] p4 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_4_type"));
        int[] p5 = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "jt_person_5_type"));
        int[] purpose = M5678Table.getColumnAsInt(M5678Table.getColumnPosition(
                    "purpose"));
        int[][] persontypeCount = new int[NoRows][shape[1]];

        float[] tourCat = M5678Table.getColumnAsFloat(M5678Table.getColumnPosition(
                    "tourCategory"));

        float m_cell;

        //populate matrix
        for (int i = 0; i < NoRows; i++) {
            if (tourCat[i] == 2.0) {
                if (p0[i] != 0) {
                    persontypeCount[i][p0[i] - 1]++;
                }

                if (p1[i] != 0) {
                    persontypeCount[i][p1[i] - 1]++;
                }

                if (p2[i] != 0) {
                    persontypeCount[i][p2[i] - 1]++;
                }

                if (p3[i] != 0) {
                    persontypeCount[i][p3[i] - 1]++;
                }

                if (p4[i] != 0) {
                    persontypeCount[i][p4[i] - 1]++;
                }

                if (p5[i] != 0) {
                    persontypeCount[i][p5[i] - 1]++;
                }
            }

            //populate each cell
            for (int j = 0; j < shape[0]; j++) {
                for (int k = 0; k < shape[1]; k++) {
                    //cell position in matrix
                    int[] position = { j, k };
                    int[] purposeVals = (int[]) sValues.get(0);
                    int[] persontypeVals = (int[]) sValues.get(1);

                    //increase 1 if income val and district val match those got from hh array at this position
                    if (tourCat[i] != 4.0) {
                        if ((purposeVals[j] == purpose[i])) {
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + persontypeCount[i][k],
                                position);
                        }
                    } else {
                        position[0] = 8;

                        if ((purposeVals[j] == purpose[i])) {
                            m_cell = result.getValue(position);
                            result.setValue(m_cell + persontypeCount[i][k],
                                position);
                        }
                    }
                }
            }
        }

        return result;
    }

    private void echoProperty() {
        String propertyLocation = (String) propertyMap.get("property.location");
        File propertyFile = new File(propertyLocation + "morpc.properties");
        String lastModifiedDate = new Date(propertyFile.lastModified()).toString();
        long fileSize = propertyFile.length();

        //stream to read from property file
        FileInputStream stream = null;

        if (!propertyFile.exists()) {
            logger.info("No MORPC property file exists.");
            System.exit(0);
        } else {
            try {
                stream = new FileInputStream(propertyFile);

                //to hold file contents
                byte[] buffer = new byte[4096];

                //how many bytes in buffer
                int bytes_read;

                logger.info("Echo of morpc.properties for this run.");
                logger.info("Modified at:" + lastModifiedDate);
                logger.info("Size:" + fileSize + " bytes");

                //read a chunk of bytes intot he buffer, then write them out
                while ((bytes_read = stream.read(buffer)) != -1) {
                    String s = new String(buffer, 0, bytes_read);
                    logger.info(s);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    //this method is not currently used
    private Vector getReportProperties() {
        Vector result = new Vector();
        Set keys = propertyMap.keySet();
        Iterator itr = keys.iterator();
        String key;
        HashMap property;

        while (itr.hasNext()) {
            key = (String) itr.next();

            if (key.endsWith(".report")) {
                property = ResourceUtil.getResourceBundleAsHashMap((String) propertyMap.get(
                            key));
                result.add(property);
            }
        }

        return result;
    }
}
