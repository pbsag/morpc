package com.pb.morpc.models;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.BinaryMatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;

import java.io.*;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Nov. 14 2003
 */
public class AccessibilityIndices {
    private TableDataSet attraction;
    HashMap propertyMap;
    Logger logger;

    //binary files used to calculate accessibility indices
    private Matrix sovTimeAm;
    private Matrix sovTimeMd;
    private Matrix sovDistAm;
    private Matrix wtWalkAm;
    private Matrix wtWait1Am;
    private Matrix wtWait2Am;
    private Matrix wtLbsAm;
    private Matrix wtEbsAm;
    private Matrix wtBrtAm;
    private Matrix wtLrtAm;
    private Matrix wtCrlAm;
    private Matrix wtWalkMd;
    private Matrix wtWait1Md;
    private Matrix wtWait2Md;
    private Matrix wtLbsMd;
    private Matrix wtEbsMd;
    private Matrix wtBrtMd;
    private Matrix wtLrtMd;
    private Matrix wtCrlMd;

    //parameter in formula
    double[] negGama = {
        -0.05, -0.05, -0.05, -0.05, -1.0, -0.05, -0.05, -0.05, -0.05, -1.0
    };

    //column titles in accessibility indices .csv file
    String[] colTitles = {
        "work_au_pk", "work_au_op", "work_tr_pk", "work_tr_op", "work_walk",
        "nonw_au_pk", "nonw_au_op", "nonw_tr_pk", "nonw_tr_op", "nonw_walk"
    };

    //number of zones
    int NoOfZones;

    //number of indices
    int NoOfIndices;

    //array to hold indices
    double[][] indices;

    //intermediate arrays in formulat
    double[][] A;
    double[][] c;

    public AccessibilityIndices(String propertiesName) {
        logger = Logger.getLogger("com.pb.morpc.report");
        propertyMap = ResourceUtil.getResourceBundleAsHashMap(propertiesName);

        String binaryDirectory = (String) propertyMap.get("SkimsDirectory.binary");
        
        
        //open binary matrix readers
        sovTimeAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("sovTimeAm.file") + ".binary")).readMatrix();
        sovTimeMd = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("sovTimeMd.file") + ".binary")).readMatrix();
        sovDistAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("sovDistAm.file") + ".binary")).readMatrix();
        wtWalkAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtWalkAm.file") + ".binary")).readMatrix();
        wtWait1Am = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtWait1Am.file") + ".binary")).readMatrix();
        wtWait2Am = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtWait2Am.file") + ".binary")).readMatrix();
        wtLbsAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtLbsAm.file") + ".binary")).readMatrix();
        wtEbsAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtEbsAm.file") + ".binary")).readMatrix();
        wtBrtAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtBrtAm.file") + ".binary")).readMatrix();
        wtLrtAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtLrtAm.file") + ".binary")).readMatrix();
        wtCrlAm = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtCrlAm.file") + ".binary")).readMatrix();
        wtWalkMd = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtWalkMd.file") + ".binary")).readMatrix();
        wtWait1Md = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtWait1Md.file") + ".binary")).readMatrix();
        wtWait2Md = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtWait2Md.file") + ".binary")).readMatrix();
        wtLbsMd = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtLbsMd.file") + ".binary")).readMatrix();
        wtEbsMd = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtEbsMd.file") + ".binary")).readMatrix();
        wtBrtMd = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtBrtMd.file") + ".binary")).readMatrix();
        wtLrtMd = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtLrtMd.file") + ".binary")).readMatrix();
        wtCrlMd = new BinaryMatrixReader(new File(
                        binaryDirectory + "/" + (String) propertyMap.get("wtCrlMd.file") + ".binary")).readMatrix();

        try {
            CSVFileReader reader = new CSVFileReader();
            attraction = reader.readFile(new File((String) propertyMap.get("TAZMainData.file")));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        NoOfZones = attraction.getRowCount();
        NoOfIndices = negGama.length;

        indices = new double[NoOfZones][NoOfIndices];
        A = new double[NoOfZones][NoOfIndices];
        c = new double[NoOfZones][NoOfZones];

        A = calcuA(attraction);
        indices = calcuIndices();
    }

    /**
     * Calculate accessibility indices.
     * @return
     */
    private double[][] calcuIndices() {
        double[][] result = new double[NoOfZones][NoOfIndices];
        double temp = 0.0;

        for (int k = 0; k < NoOfIndices; k++) {
            calcuC(k);

            for (int i = 0; i < NoOfZones; i++) {
                for (int j = 0; j < NoOfZones; j++) {
                    temp = A[j][k] * MathUtil.exp(negGama[k] * c[i][j]);
                    result[i][k] = result[i][k] + temp;
                }
            }
        }

        return result;
    }

    /**
     * Calculate A in formula.
     * @param attraction
     * @return
     */
    private double[][] calcuA(TableDataSet attraction) {
        double[][] result = new double[NoOfZones][negGama.length];
        float[] empoff = attraction.getColumnAsFloat(attraction.getColumnPosition(
                    "empoff"));
        float[] empretgds = attraction.getColumnAsFloat(attraction.getColumnPosition(
                    "empretgds"));
        float[] empretsrv = attraction.getColumnAsFloat(attraction.getColumnPosition(
                    "empretsrv"));
        float[] empother = attraction.getColumnAsFloat(attraction.getColumnPosition(
                    "empother"));

        for (int k = 0; k < negGama.length; k++) {
            for (int j = 0; j < NoOfZones; j++) {
                if (k <= 4) {
                    result[j][k] = (double) (empoff[j] + empretgds[j] +
                        empretsrv[j] + empother[j]);
                } else {
                    result[j][k] = (double) (empretgds[j] + empretsrv[j]);
                }
            }
        }

        return result;
    }

    /**
     * Calculate C in formula.
     * @param k represents accessibility index number.
     */
    private void calcuC(int k) {
        for (int i = 0; i < NoOfZones; i++) {
            for (int j = 0; j < NoOfZones; j++) {
                if ((k == 0) || (k == 5)) {
                    c[i][j] = (double) (sovTimeAm.getValueAt(i, j));
                }

                if ((k == 1) || (k == 6)) {
                    c[i][j] = (double) (sovTimeMd.getValueAt(i, j));
                }

                if ((k == 2) || (k == 7)) {
                    c[i][j] = (double) (wtLbsAm.getValueAt(i, j) +
                        wtEbsAm.getValueAt(i, j) + wtBrtAm.getValueAt(i, j) +
                        wtLrtAm.getValueAt(i, j) + wtCrlAm.getValueAt(i, j) +
                        wtWait1Am.getValueAt(i, j) +
                        wtWait2Am.getValueAt(i, j) + wtWalkAm.getValueAt(i, j));

                    if (c[i][j] == 0.0) {
                        c[i][j] = 999.0;
                    }
                }

                if ((k == 3) || (k == 8)) {
                    c[i][j] = (double) (wtLbsMd.getValueAt(i, j) +
                        wtEbsMd.getValueAt(i, j) + wtBrtMd.getValueAt(i, j) +
                        wtLrtMd.getValueAt(i, j) + wtCrlMd.getValueAt(i, j) +
                        wtWait1Md.getValueAt(i, j) +
                        wtWait2Md.getValueAt(i, j) + wtWalkMd.getValueAt(i, j));

                    if (c[i][j] == 0.0) {
                        c[i][j] = 999.0;
                    }
                }

                if ((k == 4) || (k == 9)) {
                    c[i][j] = (double) (sovDistAm.getValueAt(i, j));

                    if (c[i][j] > 3.0) {
                        c[i][j] = 999.0;
                    }
                }
            }
        }
    }

    public double[][] getIndices() {
        return indices;
    }

    /**
     * Save accessibility indices as a .csv file.
     */
    public void writeIndices() {
        float[][] fIndices = new float[NoOfZones][NoOfIndices];
        float[] taz = new float[NoOfZones];
        ArrayList colTitlesArrayList = new ArrayList();

        for (int i = 0; i < colTitles.length; i++) {
            colTitlesArrayList.add(colTitles[i]);
        }

        for (int i = 0; i < NoOfZones; i++) {
            taz[i] = i + 1;

            for (int j = 0; j < NoOfIndices; j++) {
                fIndices[i][j] = (float) indices[i][j];
            }
        }

        TableDataSet completeTable = new TableDataSet();
        completeTable.appendColumn(taz, "taz");

        TableDataSet dataTable = TableDataSet.create(fIndices,
                colTitlesArrayList);

        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            completeTable.appendColumn(dataTable.getColumnAsFloat(i + 1),
                colTitles[i]);
        }

        try {
            File AccessibilityIndicesFile = new File((String) propertyMap.get("TAZAccessibility.file"));
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(completeTable, AccessibilityIndicesFile, new DecimalFormat("#.00"));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    //for testing purpose only
    public static void main(String[] args) {
        AccessibilityIndices ind = new AccessibilityIndices("morpc");
        ind.writeIndices();
    }
}
