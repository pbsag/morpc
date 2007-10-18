package com.pb.morpc.matrix;
/**
 * @author Jim Hicks
 *
 * Aggregate MORPC tpplus submode matrices for use in stop location choice models
 */

import java.io.File;
import java.util.HashMap;
import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.MatrixCalculator;


public class MorpcMatrixAggregaterTpp {
    
    static Logger logger = Logger.getLogger("com.pb.morpc.matrix");
    
    static final MatrixType INPUT_SKIM_FILE_FORMAT = MatrixType.TPPLUS; 
    static final MatrixType OUTPUT_SKIM_FILE_FORMAT = MatrixType.TPPLUS; 
    private String INPUT_SKIM_FILE_DIR; 
    private String OUTPUT_SKIM_FILE_DIR; 
    
	HashMap propertyMap;


	
    public MorpcMatrixAggregaterTpp ( HashMap propertyMap ) {
        
		this.propertyMap = propertyMap;
        INPUT_SKIM_FILE_DIR = (String)propertyMap.get("SkimsDirectory.tpplus");
        OUTPUT_SKIM_FILE_DIR = (String)propertyMap.get("SkimsDirectory.tpplus");
        
    }

    
    
    /**
     * read the submode skims for time period/access mode comibation and aggregate
     * for use in the stop location choice model.
     */
    public void aggregateSlcSkims () {
		bestWtIvtAmSkims();
		bestWtIvtMdSkims();
		bestDtIvtAmSkims();
		bestDtIvtMdSkims();
		wtLbsIvtAmSkims();
		wtEbsIvtAmSkims();
		wtBrtIvtAmSkims();
		wtLrtIvtAmSkims();
		wtCrlIvtAmSkims();
		wtLbsIvtMdSkims();
		wtEbsIvtMdSkims();
		wtBrtIvtMdSkims();
		wtLrtIvtMdSkims();
		wtCrlIvtMdSkims();
		dtLbsIvtAmSkims();
		dtEbsIvtAmSkims();
		dtBrtIvtAmSkims();
		dtLrtIvtAmSkims();
		dtCrlIvtAmSkims();
		dtLbsIvtMdSkims();
		dtEbsIvtMdSkims();
		dtBrtIvtMdSkims();
		dtLrtIvtMdSkims();
		dtCrlIvtMdSkims();

		logger.info ("finished aggregating submode ivt skims in tpplus format.");
	    
    }
    
    

	/**
	 * read best wt ivt am skims matrices and aggregate
	 */
	private void bestWtIvtAmSkims () {

		logger.info ("aggregating best wt am ivt skims");
	    

		String[] tableNames = { "wtLbsAm", "wtEbsAm", "wtBrtAm", "wtLrtAm", "wtCrlAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsAm + wtEbsAm + wtBrtAm + wtLrtAm + wtCrlAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("bestWtIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}
	
	/**
	 * read best wt ivt md skims matrices and aggregate
	 */
	private void bestWtIvtMdSkims () {

		logger.info ("aggregating best wt md ivt skims");
	    
        String[] tableNames = { "wtLbsMd", "wtEbsMd", "wtBrtMd", "wtLrtMd", "wtCrlMd" };

        HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsMd + wtEbsMd + wtBrtMd + wtLrtMd + wtCrlMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("bestWtIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}
	
	/**
	 * read best dt ivt am skims matrices and aggregate
	 */
	private void bestDtIvtAmSkims () {

	    logger.info ("aggregating best dt am ivt skims");
	    
        String[] tableNames = { "dtLbsAm", "dtEbsAm", "dtBrtAm", "dtLrtAm", "dtCrlAm", "dtDriveAm" };
        
		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsAm + dtEbsAm + dtBrtAm + dtLrtAm + dtCrlAm + dtDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("bestDtIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}
	
	/**
	 * read best dt ivt md skims matrices and aggregate
	 */
	private void bestDtIvtMdSkims () {

		logger.info ("aggregating best dt md ivt skims");
	    
        String[] tableNames = { "dtLbsMd", "dtEbsMd", "dtBrtMd", "dtLrtMd", "dtCrlMd", "dtDriveMd" };
        
		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsMd + dtEbsMd + dtBrtMd + dtLrtMd + dtCrlMd + dtDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("bestDtIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}
	
	/**
	 * read wt lbs ivt am skims matrices and aggregate
	 */
	private void wtLbsIvtAmSkims () {

		logger.info ("aggregating wt am lbs ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtLbsLbsAm" };

        HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsLbsAm + (sovDistMd<3.01)*(wtLbsLbsAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtLbsIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt ebs ivt am skims matrices and aggregate
	 */
	private void wtEbsIvtAmSkims () {

		logger.info ("aggregating wt am ebs ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtEbsLbsAm", "wtEbsEbsAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtEbsLbsAm + wtEbsEbsAm + (sovDistMd<3.01)*(wtEbsEbsAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtEbsIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt brt ivt am skims matrices and aggregate
	 */
	private void wtBrtIvtAmSkims () {

		logger.info ("aggregating wt am brt ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtBrtLbsAm", "wtBrtEbsAm", "wtBrtBrtAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtBrtLbsAm + wtBrtEbsAm + wtBrtBrtAm + (sovDistMd<3.01)*(wtBrtBrtAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtBrtIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt lrt ivt am skims matrices and aggregate
	 */
	private void wtLrtIvtAmSkims () {

		logger.info ("aggregating wt am lrt ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtLrtLbsAm", "wtLrtEbsAm", "wtLrtBrtAm", "wtLrtLrtAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLrtLbsAm + wtLrtEbsAm + wtLrtBrtAm + wtLrtLrtAm + (sovDistMd<3.01)*(wtLrtLrtAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtLrtIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt crl ivt am skims matrices and aggregate
	 */
	private void wtCrlIvtAmSkims () {

		logger.info ("aggregating wt am crl ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtCrlLbsAm", "wtCrlEbsAm", "wtCrlBrtAm", "wtCrlLrtAm", "wtCrlCrlAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtCrlLbsAm + wtCrlEbsAm + wtCrlBrtAm + wtCrlLrtAm + wtCrlCrlAm + (sovDistMd<3.01)*(wtCrlCrlAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtCrlIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt lbs ivt md skims matrices and aggregate
	 */
	private void wtLbsIvtMdSkims () {

		logger.info ("aggregating wt md lbs ivt skims");
	    
		String[] tableNames = { "sovDistMd", "wtLbsLbsMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsLbsMd + (sovDistMd<3.01)*(wtLbsLbsMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtLbsIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt ebs ivt md skims matrices and aggregate
	 */
	private void wtEbsIvtMdSkims () {

		logger.info ("aggregating wt md ebs ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtEbsLbsMd", "wtEbsEbsMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtEbsLbsMd + wtEbsEbsMd + (sovDistMd<3.01)*(wtEbsEbsMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtEbsIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt brt ivt md skims matrices and aggregate
	 */
	private void wtBrtIvtMdSkims () {

		logger.info ("aggregating wt md brt ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtBrtLbsMd", "wtBrtEbsMd", "wtBrtBrtMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtBrtLbsMd + wtBrtEbsMd + wtBrtBrtMd + (sovDistMd<3.01)*(wtBrtBrtMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtBrtIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt lrt ivt md skims matrices and aggregate
	 */
	private void wtLrtIvtMdSkims () {

		logger.info ("aggregating wt md lrt ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtLrtLbsMd", "wtLrtEbsMd", "wtLrtBrtMd", "wtLrtLrtMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLrtLbsMd + wtLrtEbsMd + wtLrtBrtMd + wtLrtLrtMd + (sovDistMd<3.01)*(wtLrtLrtMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtLrtIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read wt crl ivt md skims matrices and aggregate
	 */
	private void wtCrlIvtMdSkims () {

		logger.info ("aggregating wt md crl ivt skims");
	    
        String[] tableNames = { "sovDistMd", "wtCrlLbsMd", "wtCrlEbsMd", "wtCrlBrtMd", "wtCrlLrtMd", "wtCrlCrlMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtCrlLbsMd + wtCrlEbsMd + wtCrlBrtMd + wtCrlLrtMd + wtCrlCrlMd + (sovDistMd<3.01)*(wtCrlCrlMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("wtCrlIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt lbs ivt am skims matrices and aggregate
	 */
	private void dtLbsIvtAmSkims () {

		logger.info ("aggregating dt am lbs ivt skims");
	    
        String[] tableNames = { "dtLbsLbsAm", "dtLbsDriveAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsLbsAm + dtLbsDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtLbsIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt ebs ivt am skims matrices and aggregate
	 */
	private void dtEbsIvtAmSkims () {

		logger.info ("aggregating dt am ebs ivt skims");
	    
        String[] tableNames = { "dtEbsLbsAm", "dtEbsEbsAm", "dtEbsDriveAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtEbsLbsAm + dtEbsEbsAm + dtEbsDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtEbsIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt brt ivt am skims matrices and aggregate
	 */
	private void dtBrtIvtAmSkims () {

		logger.info ("aggregating dt am brt ivt skims");
	    
        String[] tableNames = { "dtBrtLbsAm", "dtBrtEbsAm", "dtBrtBrtAm", "dtBrtDriveAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtBrtLbsAm + dtBrtEbsAm + dtBrtBrtAm + dtBrtDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtBrtIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt lrt ivt am skims matrices and aggregate
	 */
	private void dtLrtIvtAmSkims () {

		logger.info ("aggregating dt am lrt ivt skims");
	    
        String[] tableNames = { "dtLrtLbsAm", "dtLrtEbsAm", "dtLrtBrtAm", "dtLrtLrtAm", "dtLrtDriveAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLrtLbsAm + dtLrtEbsAm + dtLrtBrtAm + dtLrtLrtAm + dtLrtDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtLrtIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt crl ivt am skims matrices and aggregate
	 */
	private void dtCrlIvtAmSkims () {

		logger.info ("aggregating dt am crl ivt skims");
	    
        String[] tableNames = { "dtCrlLbsAm", "dtCrlEbsAm", "dtCrlBrtAm", "dtCrlLrtAm", "dtCrlCrlAm", "dtCrlDriveAm" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtCrlLbsAm + dtCrlEbsAm + dtCrlBrtAm + dtCrlLrtAm + dtCrlCrlAm + dtCrlDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtCrlIvtAm.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt lbs ivt md skims matrices and aggregate
	 */
	private void dtLbsIvtMdSkims () {

		logger.info ("aggregating dt md lbs ivt skims");
	    
        String[] tableNames = { "dtLbsLbsMd", "dtLbsDriveMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsLbsMd + dtLbsDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtLbsIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt ebs ivt md skims matrices and aggregate
	 */
	private void dtEbsIvtMdSkims () {

		logger.info ("aggregating dt md ebs ivt skims");
	    
        String[] tableNames = { "dtEbsLbsMd", "dtEbsEbsMd", "dtEbsDriveMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtEbsLbsMd + dtEbsEbsMd + dtEbsDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtEbsIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt brt ivt md skims matrices and aggregate
	 */
	private void dtBrtIvtMdSkims () {

		logger.info ("aggregating dt md brt ivt skims");
	    
        String[] tableNames = { "dtBrtLbsMd", "dtBrtEbsMd", "dtBrtBrtMd", "dtBrtDriveMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtBrtLbsMd + dtBrtEbsMd + dtBrtBrtMd + dtBrtDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtBrtIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt lrt ivt md skims matrices and aggregate
	 */
	private void dtLrtIvtMdSkims () {

		logger.info ("aggregating dt md lrt ivt skims");
	    
        String[] tableNames = { "dtLrtLbsMd", "dtLrtEbsMd", "dtLrtBrtMd", "dtLrtLrtMd", "dtLrtDriveMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLrtLbsMd + dtLrtEbsMd + dtLrtBrtMd + dtLrtLrtMd + dtLrtDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtLrtIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	/**
	 * read dt crl ivt md skims matrices and aggregate
	 */
	private void dtCrlIvtMdSkims () {

		logger.info ("aggregating dt md crl ivt skims");
	    
        String[] tableNames = { "dtCrlLbsMd", "dtCrlEbsMd", "dtCrlBrtMd", "dtCrlLrtMd", "dtCrlCrlMd", "dtCrlDriveMd" };

		HashMap matrixMap = saveMatricesInMap ( tableNames );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtCrlLbsMd + dtCrlEbsMd + dtCrlBrtMd + dtCrlLrtMd + dtCrlCrlMd + dtCrlDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = OUTPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get("dtCrlIvtMd.file");
		MatrixWriter writer = MatrixWriter.createWriter (OUTPUT_SKIM_FILE_FORMAT, new File( fileName ) );

        long startTime = System.currentTimeMillis();
		writer.writeMatrix ( resultMatrix );
        logger.info("wrote "  + fileName + ", " + resultMatrix.getRowCount()*resultMatrix.getColumnCount()*4 + " bytes, in " + 
                ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
	}

	

	
	
	private HashMap saveMatricesInMap ( String[] tableNames ) {

		MatrixReader reader;
		Matrix m;
		HashMap matrixMap = new HashMap();

		for (int i=0; i < tableNames.length; i++) {
			String partialName = tableNames[i];
            String tableNameProperty = partialName + ".file";
            String tableNumberProperty = partialName + ".table";
            String fullName = INPUT_SKIM_FILE_DIR + "/" + (String)propertyMap.get(tableNameProperty);
            String tableIndex = (String)propertyMap.get(tableNumberProperty);
            
            try {
    			reader = MatrixReader.createReader ( INPUT_SKIM_FILE_FORMAT, new File(fullName) );
    			m = reader.readMatrix(tableIndex);
    			matrixMap.put ( partialName, m );
            }
            catch (RuntimeException e) {
                logger.fatal( String.format("could not create %s matrix reader to read %s", INPUT_SKIM_FILE_FORMAT, fullName) );
                logger.fatal( String.format("partialName = %s", partialName) );
                logger.fatal( String.format("tableNameProperty = %s", tableNameProperty) );
                logger.fatal( String.format("tableNumberProperty = %s", tableNumberProperty) );
                logger.fatal( String.format("tableIndex = %s", tableIndex) );
                throw e;
            }
		}

		return matrixMap;
	}
	

	
	public static void main(String[] args) {

		HashMap propertyMap;
		
		// aggregate the original 129 matrices specified for Stop location choice into 25.
		if (args.length == 0) {
			propertyMap = ResourceUtil.getResourceBundleAsHashMap ("morpc" );
		}
		else {
			propertyMap = ResourceUtil.getResourceBundleAsHashMap ( args[0] );
		}
		
		MorpcMatrixAggregater ma = new MorpcMatrixAggregater(propertyMap);
		ma.aggregateSlcSkims();
                        
		logger.info("end of aggregating MORPC TP+ Matrices");

		System.exit (0);
        
	}
	
}
