package com.pb.morpc.matrix;

/**
 * @author Jim Hicks
 *
 * Convert MORPC TP+ matrices to binary
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


public class MorpcMatrixZipper {
    
    static Logger logger = Logger.getLogger("com.pb.morpc.matrix");

	HashMap propertyMap;

    
    public MorpcMatrixZipper ( HashMap propertyMap ) {
		this.propertyMap = propertyMap;
    }


	public void convertHwyBinaryToZip () {

		ArrayList binMatrices = new ArrayList();
		
		binMatrices.add ( (String)propertyMap.get("sovTimeAm.file") );
		binMatrices.add ( (String)propertyMap.get("sovDistAm.file") );
		binMatrices.add ( (String)propertyMap.get("sovTollAm.file") );
		binMatrices.add ( (String)propertyMap.get("hovTimeAm.file") );
		binMatrices.add ( (String)propertyMap.get("hovDistAm.file") );
		binMatrices.add ( (String)propertyMap.get("hovTollAm.file") );
		binMatrices.add ( (String)propertyMap.get("sovTimeMd.file") );
		binMatrices.add ( (String)propertyMap.get("sovDistMd.file") );
		binMatrices.add ( (String)propertyMap.get("sovTollMd.file") );
		binMatrices.add ( (String)propertyMap.get("hovTimeMd.file") );
		binMatrices.add ( (String)propertyMap.get("hovDistMd.file") );
		binMatrices.add ( (String)propertyMap.get("hovTollMd.file") );
		
		convert ( binMatrices, (String)propertyMap.get("SkimsDirectory.binary"), (String)propertyMap.get("SkimsDirectory.zip") );

	}

	

	public void convertWTBinaryToZip () {

		ArrayList binMatrices = new ArrayList();
		
		binMatrices.add ( (String)propertyMap.get("wtWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtWait1Am.file") );
		binMatrices.add ( (String)propertyMap.get("wtWait2Am.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtXfersAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtFareAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtWait1Md.file") );
		binMatrices.add ( (String)propertyMap.get("wtWait2Md.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtXfersMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtFareMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtLrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlLrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlCrlAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtLrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlLrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlCrlMd.file") );
		binMatrices.add ( (String)propertyMap.get("bestWtIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("bestWtIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("wtLbsIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtEbsIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtBrtIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtLrtIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("wtCrlIvtMd.file") );
		
		convert ( binMatrices, (String)propertyMap.get("SkimsDirectory.binary"), (String)propertyMap.get("SkimsDirectory.zip") );

	}

	

	public void convertDTBinaryToZip () {

		ArrayList binMatrices = new ArrayList();
		
		binMatrices.add ( (String)propertyMap.get("dtWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtWait1Am.file") );
		binMatrices.add ( (String)propertyMap.get("dtWait2Am.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtXfersAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtFareAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtWait1Md.file") );
		binMatrices.add ( (String)propertyMap.get("dtWait2Md.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtXfersMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtFareMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtLrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlWalkAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlDriveAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlLbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlEbsAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlBrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlLrtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlCrlAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtLrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlWalkMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlDriveMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlLbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlEbsMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlBrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlLrtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlCrlMd.file") );
		binMatrices.add ( (String)propertyMap.get("bestDtIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("bestDtIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlIvtAm.file") );
		binMatrices.add ( (String)propertyMap.get("dtLbsIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtEbsIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtBrtIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtLrtIvtMd.file") );
		binMatrices.add ( (String)propertyMap.get("dtCrlIvtMd.file") );
		
		convert ( binMatrices, (String)propertyMap.get("SkimsDirectory.binary"), (String)propertyMap.get("SkimsDirectory.zip") );

	}

	

	private void convert ( ArrayList fileList, String binDirectoryName, String zipDirectoryName) {

		int slashIndex;
		int dotIndex;

		for (int i=0; i < fileList.size(); i++) {

			String binFileName = binDirectoryName + "/" + (String)fileList.get(i) + ".binary";

			MatrixReader binReader = MatrixReader.createReader ( MatrixType.BINARY, new File( binFileName ) );
			Matrix mx = binReader.readMatrix();

			String zipFileName = zipDirectoryName + "/" + (String)fileList.get(i) + ".zip";

			logger.info( "converting " + binFileName + " to " + zipFileName );				
			
			MatrixWriter zipWriter = MatrixWriter.createWriter (MatrixType.ZIP, new File( zipFileName ) );
			zipWriter.writeMatrix ( (String)fileList.get(i), mx );
		}

	}


	
    public static void main(String[] args) {

        // create a model runner object and run models
		HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap ("morpc" );
		MorpcMatrixZipper mx = new MorpcMatrixZipper (propertyMap);
		
		mx.convertHwyBinaryToZip();	
		mx.convertWTBinaryToZip();	
		mx.convertDTBinaryToZip();	

		logger.info( "done converting MORPC Binary matrices to Zip matrices" );				

        System.exit (0);
        
    }
    
}
