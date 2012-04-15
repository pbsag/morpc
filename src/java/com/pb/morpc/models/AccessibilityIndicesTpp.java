package com.pb.morpc.models;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.morpc.matrix.MatrixDataServer;
import com.pb.morpc.matrix.MatrixDataServerRmi;

import java.io.*;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun - tpplus version implemented by Jim Hicks, 10/3/07 
 * @version 1.0, Nov. 14 2003
 */
public class AccessibilityIndicesTpp {
    private TableDataSet attraction;
    HashMap propertyMap;
    Logger logger;

    //skims tables used to calculate accessibility indices
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

    private String matrixSeverAddress;
    private String matrixSeverPort;

    
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

    public AccessibilityIndicesTpp(String propertiesName) {
        logger = Logger.getLogger(AccessibilityIndicesTpp.class);
        propertyMap = ResourceUtil.getResourceBundleAsHashMap(propertiesName);

        matrixSeverAddress = (String) propertyMap.get("MatrixServerAddress");
        matrixSeverPort = (String) propertyMap.get("MatrixServerPort");

        readTppSkimFiles();
        

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


    
    private void readTppSkimFiles() {
        
        String[] skimsMatrixNames = { "sovTimeAm", "sovTimeMd", "sovDistAm",
                "wtWalkAm", "wtWait1Am", "wtWait2Am", "wtLbsAm", "wtEbsAm", "wtBrtAm", "wtLrtAm", "wtCrlAm",
                "wtWalkMd", "wtWait1Md", "wtWait2Md", "wtLbsMd", "wtEbsMd", "wtBrtMd", "wtLrtMd", "wtCrlMd" };

        HashMap matrixMap = new HashMap();
        for ( String tableIdentifier: skimsMatrixNames )
            matrixMap = readTppTableFromFile(tableIdentifier, matrixMap);
        

        sovTimeAm = (Matrix)matrixMap.get("sovTimeAm");
        sovTimeMd = (Matrix)matrixMap.get("sovTimeMd");
        sovDistAm = (Matrix)matrixMap.get("sovDistAm");
        wtWalkAm = (Matrix)matrixMap.get("wtWalkAm");
        wtWait1Am = (Matrix)matrixMap.get("wtWait1Am");
        wtWait2Am = (Matrix)matrixMap.get("wtWait2Am");
        wtLbsAm = (Matrix)matrixMap.get("wtLbsAm");
        wtEbsAm = (Matrix)matrixMap.get("wtEbsAm");
        wtBrtAm = (Matrix)matrixMap.get("wtBrtAm");
        wtLrtAm = (Matrix)matrixMap.get("wtLrtAm");
        wtCrlAm = (Matrix)matrixMap.get("wtCrlAm");
        wtWalkMd = (Matrix)matrixMap.get("wtWalkMd");
        wtWait1Md = (Matrix)matrixMap.get("wtWait1Md");
        wtWait2Md = (Matrix)matrixMap.get("wtWait2Md");
        wtLbsMd = (Matrix)matrixMap.get("wtLbsMd");
        wtEbsMd = (Matrix)matrixMap.get("wtEbsMd");
        wtBrtMd = (Matrix)matrixMap.get("wtBrtMd");
        wtLrtMd = (Matrix)matrixMap.get("wtLrtMd");
        wtCrlMd = (Matrix)matrixMap.get("wtCrlMd");
        
    }


    
    private HashMap readTppTableFromFile(String tableIdentifier, HashMap matrixMap) {
        
        String skimsDirectory = (String) propertyMap.get("SkimsDirectory.tpplus");
        
        String tableNameProperty = tableIdentifier + ".file";
        String skimFileName = (String)propertyMap.get(tableNameProperty);

        String tableNumberProperty = tableIdentifier + ".table";
        String skimTableIndex = (String)propertyMap.get(tableNumberProperty);
        
        String fullName = skimsDirectory + "/" + skimFileName;
        
        try {
            if ( matrixSeverAddress == null ){
                MatrixReader reader = MatrixReader.createReader ( MatrixType.TPPLUS, new File(fullName) );
                Matrix m = reader.readMatrix(skimTableIndex);
                matrixMap.put ( tableIdentifier, m );
            }
            else{
                //These lines will remote any matrix reader call
                MatrixDataServerRmi ms = new MatrixDataServerRmi( matrixSeverAddress, Integer.parseInt(matrixSeverPort), MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                Matrix m = ms.readTppMatrix( fullName, skimTableIndex );
                matrixMap.put ( tableIdentifier, m );
            }
        }        
        catch (RuntimeException e) {
            logger.fatal( String.format("could not create %s matrix reader to read %s", MatrixType.TPPLUS, fullName) );
            logger.fatal( String.format("tableIdentifier = %s", tableIdentifier) );
            logger.fatal( String.format("tableNameProperty = %s", tableNameProperty) );
            logger.fatal( String.format("skimFileName = %s", skimFileName) );
            logger.fatal( String.format("tableNumberProperty = %s", tableNumberProperty) );
            logger.fatal( String.format("skimTableIndex = %s", skimTableIndex) );
            logger.fatal( String.format("skimsDirectory = %s", skimsDirectory) );
            logger.fatal( String.format("fullName = %s", fullName) );
            throw e;
        }
    
        return matrixMap;
        
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
                    c[i][j] = (double) (sovTimeAm.getValueAt(i+1, j+1));
                }

                if ((k == 1) || (k == 6)) {
                    c[i][j] = (double) (sovTimeMd.getValueAt(i+1, j+1));
                }

                if ((k == 2) || (k == 7)) {
                    c[i][j] = (double) (wtLbsAm.getValueAt(i+1, j+1) +
                        wtEbsAm.getValueAt(i+1, j+1) + wtBrtAm.getValueAt(i+1, j+1) +
                        wtLrtAm.getValueAt(i+1, j+1) + wtCrlAm.getValueAt(i+1, j+1) +
                        wtWait1Am.getValueAt(i+1, j+1) +
                        wtWait2Am.getValueAt(i+1, j+1) + wtWalkAm.getValueAt(i+1, j+1));

                    if (c[i][j] == 0.0) {
                        c[i][j] = 999.0;
                    }
                }

                if ((k == 3) || (k == 8)) {
                    c[i][j] = (double) (wtLbsMd.getValueAt(i+1, j+1) +
                        wtEbsMd.getValueAt(i+1, j+1) + wtBrtMd.getValueAt(i+1, j+1) +
                        wtLrtMd.getValueAt(i+1, j+1) + wtCrlMd.getValueAt(i+1, j+1) +
                        wtWait1Md.getValueAt(i+1, j+1) +
                        wtWait2Md.getValueAt(i+1, j+1) + wtWalkMd.getValueAt(i+1, j+1));

                    if (c[i][j] == 0.0) {
                        c[i][j] = 999.0;
                    }
                }

                if ((k == 4) || (k == 9)) {
                    c[i][j] = (double) (sovDistAm.getValueAt(i+1, j+1));

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
