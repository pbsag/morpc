package com.pb.morpc.matrix;
/**
 * @author Jim Hicks
 *
 * Aggregate MORPC binary submode matrices for use in stop location choice models
 */

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.MatrixCalculator;


public class MorpcMatrixAggregater {
    
    static Logger logger = Logger.getLogger("com.pb.morpc.matrix");
	HashMap propertyMap;


	String binaryDirectory;

	
    public MorpcMatrixAggregater ( HashMap propertyMap ) {
		this.propertyMap = propertyMap;
		
		this.binaryDirectory = (String)propertyMap.get("SkimsDirectory.binary");
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

		logger.info ("finished aggregating submode ivt skims.");
	    
    }
    
    

	/**
	 * read binary best wt ivt am skims matrices and aggregate
	 */
	private void bestWtIvtAmSkims () {

		logger.info ("aggregating best wt am ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "wtLbsAm" );
		tableList.add ( "wtEbsAm" );
		tableList.add ( "wtBrtAm" );
		tableList.add ( "wtLrtAm" );
		tableList.add ( "wtCrlAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsAm + wtEbsAm + wtBrtAm + wtLrtAm + wtCrlAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("bestWtIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}
	
	/**
	 * read binary best wt ivt md skims matrices and aggregate
	 */
	private void bestWtIvtMdSkims () {

		logger.info ("aggregating best wt md ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "wtLbsMd" );
		tableList.add ( "wtEbsMd" );
		tableList.add ( "wtBrtMd" );
		tableList.add ( "wtLrtMd" );
		tableList.add ( "wtCrlMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsMd + wtEbsMd + wtBrtMd + wtLrtMd + wtCrlMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("bestWtIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}
	
	/**
	 * read binary best dt ivt am skims matrices and aggregate
	 */
	private void bestDtIvtAmSkims () {

	    logger.info ("aggregating best dt am ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtLbsAm" );
		tableList.add ( "dtEbsAm" );
		tableList.add ( "dtBrtAm" );
		tableList.add ( "dtLrtAm" );
		tableList.add ( "dtCrlAm" );
		tableList.add ( "dtDriveAm" );
		
		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsAm + dtEbsAm + dtBrtAm + dtLrtAm + dtCrlAm + dtDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("bestDtIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}
	
	/**
	 * read binary best dt ivt md skims matrices and aggregate
	 */
	private void bestDtIvtMdSkims () {

		logger.info ("aggregating best dt md ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtLbsMd" );
		tableList.add ( "dtEbsMd" );
		tableList.add ( "dtBrtMd" );
		tableList.add ( "dtLrtMd" );
		tableList.add ( "dtCrlMd" );
		tableList.add ( "dtDriveMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsMd + dtEbsMd + dtBrtMd + dtLrtMd + dtCrlMd + dtDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("bestDtIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}
	
	/**
	 * read binary wt lbs ivt am skims matrices and aggregate
	 */
	private void wtLbsIvtAmSkims () {

		logger.info ("aggregating wt am lbs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtLbsLbsAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsLbsAm + (sovDistMd<3.01)*(wtLbsLbsAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtLbsIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt ebs ivt am skims matrices and aggregate
	 */
	private void wtEbsIvtAmSkims () {

		logger.info ("aggregating wt am ebs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtEbsLbsAm" );
		tableList.add ( "wtEbsEbsAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtEbsLbsAm + wtEbsEbsAm + (sovDistMd<3.01)*(wtEbsEbsAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtEbsIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt brt ivt am skims matrices and aggregate
	 */
	private void wtBrtIvtAmSkims () {

		logger.info ("aggregating wt am brt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtBrtLbsAm" );
		tableList.add ( "wtBrtEbsAm" );
		tableList.add ( "wtBrtBrtAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtBrtLbsAm + wtBrtEbsAm + wtBrtBrtAm + (sovDistMd<3.01)*(wtBrtBrtAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtBrtIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt lrt ivt am skims matrices and aggregate
	 */
	private void wtLrtIvtAmSkims () {

		logger.info ("aggregating wt am lrt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtLrtLbsAm" );
		tableList.add ( "wtLrtEbsAm" );
		tableList.add ( "wtLrtBrtAm" );
		tableList.add ( "wtLrtLrtAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLrtLbsAm + wtLrtEbsAm + wtLrtBrtAm + wtLrtLrtAm + (sovDistMd<3.01)*(wtLrtLrtAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtLrtIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt crl ivt am skims matrices and aggregate
	 */
	private void wtCrlIvtAmSkims () {

		logger.info ("aggregating wt am crl ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtCrlLbsAm" );
		tableList.add ( "wtCrlEbsAm" );
		tableList.add ( "wtCrlBrtAm" );
		tableList.add ( "wtCrlLrtAm" );
		tableList.add ( "wtCrlCrlAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtCrlLbsAm + wtCrlEbsAm + wtCrlBrtAm + wtCrlLrtAm + wtCrlCrlAm + (sovDistMd<3.01)*(wtCrlCrlAm==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtCrlIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt lbs ivt md skims matrices and aggregate
	 */
	private void wtLbsIvtMdSkims () {

		logger.info ("aggregating wt md lbs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtLbsLbsMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLbsLbsMd + (sovDistMd<3.01)*(wtLbsLbsMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtLbsIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt ebs ivt md skims matrices and aggregate
	 */
	private void wtEbsIvtMdSkims () {

		logger.info ("aggregating wt md ebs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtEbsLbsMd" );
		tableList.add ( "wtEbsEbsMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtEbsLbsMd + wtEbsEbsMd + (sovDistMd<3.01)*(wtEbsEbsMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtEbsIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt brt ivt md skims matrices and aggregate
	 */
	private void wtBrtIvtMdSkims () {

		logger.info ("aggregating wt md brt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtBrtLbsMd" );
		tableList.add ( "wtBrtEbsMd" );
		tableList.add ( "wtBrtBrtMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtBrtLbsMd + wtBrtEbsMd + wtBrtBrtMd + (sovDistMd<3.01)*(wtBrtBrtMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtBrtIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt lrt ivt md skims matrices and aggregate
	 */
	private void wtLrtIvtMdSkims () {

		logger.info ("aggregating wt md lrt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtLrtLbsMd" );
		tableList.add ( "wtLrtEbsMd" );
		tableList.add ( "wtLrtBrtMd" );
		tableList.add ( "wtLrtLrtMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtLrtLbsMd + wtLrtEbsMd + wtLrtBrtMd + wtLrtLrtMd + (sovDistMd<3.01)*(wtLrtLrtMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtLrtIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary wt crl ivt md skims matrices and aggregate
	 */
	private void wtCrlIvtMdSkims () {

		logger.info ("aggregating wt md crl ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "sovDistMd" );
		tableList.add ( "wtCrlLbsMd" );
		tableList.add ( "wtCrlEbsMd" );
		tableList.add ( "wtCrlBrtMd" );
		tableList.add ( "wtCrlLrtMd" );
		tableList.add ( "wtCrlCrlMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "wtCrlLbsMd + wtCrlEbsMd + wtCrlBrtMd + wtCrlLrtMd + wtCrlCrlMd + (sovDistMd<3.01)*(wtCrlCrlMd==0)*sovDistMd*20", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("wtCrlIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt lbs ivt am skims matrices and aggregate
	 */
	private void dtLbsIvtAmSkims () {

		logger.info ("aggregating dt am lbs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtLbsLbsAm" );
		tableList.add ( "dtLbsDriveAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsLbsAm + dtLbsDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtLbsIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt ebs ivt am skims matrices and aggregate
	 */
	private void dtEbsIvtAmSkims () {

		logger.info ("aggregating dt am ebs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtEbsLbsAm" );
		tableList.add ( "dtEbsEbsAm" );
		tableList.add ( "dtEbsDriveAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtEbsLbsAm + dtEbsEbsAm + dtEbsDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtEbsIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt brt ivt am skims matrices and aggregate
	 */
	private void dtBrtIvtAmSkims () {

		logger.info ("aggregating dt am brt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtBrtLbsAm" );
		tableList.add ( "dtBrtEbsAm" );
		tableList.add ( "dtBrtBrtAm" );
		tableList.add ( "dtBrtDriveAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtBrtLbsAm + dtBrtEbsAm + dtBrtBrtAm + dtBrtDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtBrtIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt lrt ivt am skims matrices and aggregate
	 */
	private void dtLrtIvtAmSkims () {

		logger.info ("aggregating dt am lrt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtLrtLbsAm" );
		tableList.add ( "dtLrtEbsAm" );
		tableList.add ( "dtLrtBrtAm" );
		tableList.add ( "dtLrtLrtAm" );
		tableList.add ( "dtLrtDriveAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLrtLbsAm + dtLrtEbsAm + dtLrtBrtAm + dtLrtLrtAm + dtLrtDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtLrtIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt crl ivt am skims matrices and aggregate
	 */
	private void dtCrlIvtAmSkims () {

		logger.info ("aggregating dt am crl ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtCrlLbsAm" );
		tableList.add ( "dtCrlEbsAm" );
		tableList.add ( "dtCrlBrtAm" );
		tableList.add ( "dtCrlLrtAm" );
		tableList.add ( "dtCrlCrlAm" );
		tableList.add ( "dtCrlDriveAm" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtCrlLbsAm + dtCrlEbsAm + dtCrlBrtAm + dtCrlLrtAm + dtCrlCrlAm + dtCrlDriveAm", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtCrlIvtAm.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt lbs ivt md skims matrices and aggregate
	 */
	private void dtLbsIvtMdSkims () {

		logger.info ("aggregating dt md lbs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtLbsLbsMd" );
		tableList.add ( "dtLbsDriveMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLbsLbsMd + dtLbsDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtLbsIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt ebs ivt md skims matrices and aggregate
	 */
	private void dtEbsIvtMdSkims () {

		logger.info ("aggregating dt md ebs ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtEbsLbsMd" );
		tableList.add ( "dtEbsEbsMd" );
		tableList.add ( "dtEbsDriveMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtEbsLbsMd + dtEbsEbsMd + dtEbsDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtEbsIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt brt ivt md skims matrices and aggregate
	 */
	private void dtBrtIvtMdSkims () {

		logger.info ("aggregating dt md brt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtBrtLbsMd" );
		tableList.add ( "dtBrtEbsMd" );
		tableList.add ( "dtBrtBrtMd" );
		tableList.add ( "dtBrtDriveMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtBrtLbsMd + dtBrtEbsMd + dtBrtBrtMd + dtBrtDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtBrtIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt lrt ivt md skims matrices and aggregate
	 */
	private void dtLrtIvtMdSkims () {

		logger.info ("aggregating dt md lrt ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtLrtLbsMd" );
		tableList.add ( "dtLrtEbsMd" );
		tableList.add ( "dtLrtBrtMd" );
		tableList.add ( "dtLrtLrtMd" );
		tableList.add ( "dtLrtDriveMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtLrtLbsMd + dtLrtEbsMd + dtLrtBrtMd + dtLrtLrtMd + dtLrtDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtLrtIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	/**
	 * read binary dt crl ivt md skims matrices and aggregate
	 */
	private void dtCrlIvtMdSkims () {

		logger.info ("aggregating dt md crl ivt skims");
	    
		ArrayList tableList = new ArrayList();

		tableList.add ( "dtCrlLbsMd" );
		tableList.add ( "dtCrlEbsMd" );
		tableList.add ( "dtCrlBrtMd" );
		tableList.add ( "dtCrlLrtMd" );
		tableList.add ( "dtCrlCrlMd" );
		tableList.add ( "dtCrlDriveMd" );

		HashMap matrixMap = saveMatricesInMap ( tableList );
		
		MatrixCalculator mc = new MatrixCalculator ( "dtCrlLbsMd + dtCrlEbsMd + dtCrlBrtMd + dtCrlLrtMd + dtCrlCrlMd + dtCrlDriveMd", matrixMap );
		Matrix resultMatrix = mc.solve();
 
		String fileName = binaryDirectory + "/" + (String)propertyMap.get("dtCrlIvtMd.file") + ".binary";
		MatrixWriter writer = MatrixWriter.createWriter (MatrixType.BINARY, new File( fileName ) );
		writer.writeMatrix ( resultMatrix );
		
	}

	

	
	
	private HashMap saveMatricesInMap ( ArrayList tableList ) {

		String tableName;
		String partialName;
		String fullName;
		MatrixReader reader;
		Matrix m;
		HashMap matrixMap = new HashMap();

		for (int i=0; i < tableList.size(); i++) {
			partialName = (String)tableList.get(i);
			tableName = partialName + ".file";
			fullName = binaryDirectory + "/" + (String)propertyMap.get(tableName) + ".binary";
			reader = MatrixReader.createReader ( MatrixType.BINARY, new File(fullName) );
			m = reader.readMatrix();
			matrixMap.put ( partialName, m );
		}

		return matrixMap;
	}
	

	
	public static void main(String[] args) {

		// aggregate the original 129 matrices specified for Stop location choice into 25.
		HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap ("morpc" );
		MorpcMatrixAggregater ma = new MorpcMatrixAggregater(propertyMap);
		ma.aggregateSlcSkims();
                        
		logger.fine("end of aggregating MORPC TP+ Matrices");

		System.exit (0);
        
	}
	
}
