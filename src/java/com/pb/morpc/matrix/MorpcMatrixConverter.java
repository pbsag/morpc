package com.pb.morpc.matrix;

/**
 * @author Jim Hicks
 *
 * Convert MORPC TP+ matrices to binary
 */

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.Runtime;
import java.util.HashMap;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import java.util.ResourceBundle;
import com.pb.common.util.ResourceUtil;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.matrix.MatrixType;
import com.pb.morpc.structures.MessageWindow;


public class MorpcMatrixConverter {
    
    static Logger logger = Logger.getLogger("com.pb.morpc.matrix");

	static PrintWriter stdoutLog;
	static PrintWriter stderrLog;

	MessageWindow mw;
	String formatString;

	ResourceBundle rb;

    
    public MorpcMatrixConverter () {
		this.mw = new MessageWindow ( "MORPC Matrix Format Conversion Utility" );

		this.rb = ResourceUtil.getResourceBundle( "morpc" );
    }


	public void convertBinaryToTpp () {

		String binaryDirectory =  ResourceUtil.getProperty(rb, "OutputDirectory.binary");
		String tpplusDirectory =  ResourceUtil.getProperty(rb, "TripsDirectory.tpplus");
		

		ArrayList man_hwyam = new ArrayList();
		man_hwyam.add ( binaryDirectory + "/man_sov_am.binary" );
		man_hwyam.add ( binaryDirectory + "/man_hov_am.binary" );
		man_hwyam.add ( binaryDirectory + "/man_sov_am_veh.binary" );
		man_hwyam.add ( binaryDirectory + "/man_hov_am_veh.binary" );
		convert ( man_hwyam, tpplusDirectory + "/man_hwyam.tpp" );

		ArrayList nonman_hwyam = new ArrayList();
		nonman_hwyam.add ( binaryDirectory + "/nonman_sov_am.binary" );
		nonman_hwyam.add ( binaryDirectory + "/nonman_hov_am.binary" );
		nonman_hwyam.add ( binaryDirectory + "/nonman_sov_am_veh.binary" );
		nonman_hwyam.add ( binaryDirectory + "/nonman_hov_am_veh.binary" );
		convert ( nonman_hwyam, tpplusDirectory + "/nonman_hwyam.tpp" );

		ArrayList man_hwypm = new ArrayList();
		man_hwypm.add ( binaryDirectory + "/man_sov_pm.binary" );
		man_hwypm.add ( binaryDirectory + "/man_hov_pm.binary" );
		man_hwypm.add ( binaryDirectory + "/man_sov_pm_veh.binary" );
		man_hwypm.add ( binaryDirectory + "/man_hov_pm_veh.binary" );
		convert ( man_hwypm, tpplusDirectory + "/man_hwypm.tpp" );

		ArrayList nonman_hwypm = new ArrayList();
		nonman_hwypm.add ( binaryDirectory + "/nonman_sov_pm.binary" );
		nonman_hwypm.add ( binaryDirectory + "/nonman_hov_pm.binary" );
		nonman_hwypm.add ( binaryDirectory + "/nonman_sov_pm_veh.binary" );
		nonman_hwypm.add ( binaryDirectory + "/nonman_hov_pm_veh.binary" );
		convert ( nonman_hwypm, tpplusDirectory + "/nonman_hwypm.tpp" );

		ArrayList man_hwymd = new ArrayList();
		man_hwymd.add ( binaryDirectory + "/man_sov_md.binary" );
		man_hwymd.add ( binaryDirectory + "/man_hov_md.binary" );
		man_hwymd.add ( binaryDirectory + "/man_sov_md_veh.binary" );
		man_hwymd.add ( binaryDirectory + "/man_hov_md_veh.binary" );
		convert ( man_hwymd, tpplusDirectory + "/man_hwymd.tpp" );

		ArrayList nonman_hwymd = new ArrayList();
		nonman_hwymd.add ( binaryDirectory + "/nonman_sov_md.binary" );
		nonman_hwymd.add ( binaryDirectory + "/nonman_hov_md.binary" );
		nonman_hwymd.add ( binaryDirectory + "/nonman_sov_md_veh.binary" );
		nonman_hwymd.add ( binaryDirectory + "/nonman_hov_md_veh.binary" );
		convert ( nonman_hwymd, tpplusDirectory + "/nonman_hwymd.tpp" );

		ArrayList man_hwynt = new ArrayList();
		man_hwynt.add ( binaryDirectory + "/man_sov_nt.binary" );
		man_hwynt.add ( binaryDirectory + "/man_hov_nt.binary" );
		man_hwynt.add ( binaryDirectory + "/man_sov_nt_veh.binary" );
		man_hwynt.add ( binaryDirectory + "/man_hov_nt_veh.binary" );
		convert ( man_hwynt, tpplusDirectory + "/man_hwynt.tpp" );

		ArrayList nonman_hwynt = new ArrayList();
		nonman_hwynt.add ( binaryDirectory + "/nonman_sov_nt.binary" );
		nonman_hwynt.add ( binaryDirectory + "/nonman_hov_nt.binary" );
		nonman_hwynt.add ( binaryDirectory + "/nonman_sov_nt_veh.binary" );
		nonman_hwynt.add ( binaryDirectory + "/nonman_hov_nt_veh.binary" );
		convert ( nonman_hwynt, tpplusDirectory + "/nonman_hwynt.tpp" );

		ArrayList man_transitam = new ArrayList();
		man_transitam.add ( binaryDirectory + "/man_walktran_lbs_am.binary");
		man_transitam.add ( binaryDirectory + "/man_walktran_ebs_am.binary");
		man_transitam.add ( binaryDirectory + "/man_walktran_brt_am.binary");
		man_transitam.add ( binaryDirectory + "/man_walktran_lrt_am.binary");
		man_transitam.add ( binaryDirectory + "/man_walktran_crl_am.binary");
		man_transitam.add ( binaryDirectory + "/man_drivtran_lbs_am.binary");
		man_transitam.add ( binaryDirectory + "/man_drivtran_ebs_am.binary");
		man_transitam.add ( binaryDirectory + "/man_drivtran_brt_am.binary");
		man_transitam.add ( binaryDirectory + "/man_drivtran_lrt_am.binary");
		man_transitam.add ( binaryDirectory + "/man_drivtran_crl_am.binary");
		man_transitam.add ( binaryDirectory + "/man_trandriv_lbs_am.binary");
		man_transitam.add ( binaryDirectory + "/man_trandriv_ebs_am.binary");
		man_transitam.add ( binaryDirectory + "/man_trandriv_brt_am.binary");
		man_transitam.add ( binaryDirectory + "/man_trandriv_lrt_am.binary");
		man_transitam.add ( binaryDirectory + "/man_trandriv_crl_am.binary");
		convert (man_transitam, tpplusDirectory + "/man_transitam.tpp");

		ArrayList nonman_transitam = new ArrayList();
		nonman_transitam.add ( binaryDirectory + "/nonman_walktran_lbs_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_walktran_ebs_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_walktran_brt_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_walktran_lrt_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_walktran_crl_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_drivtran_lbs_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_drivtran_ebs_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_drivtran_brt_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_drivtran_lrt_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_drivtran_crl_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_trandriv_lbs_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_trandriv_ebs_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_trandriv_brt_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_trandriv_lrt_am.binary");
		nonman_transitam.add ( binaryDirectory + "/nonman_trandriv_crl_am.binary");
		convert (nonman_transitam, tpplusDirectory + "/nonman_transitam.tpp");

		ArrayList man_transitpm = new ArrayList();
		man_transitpm.add ( binaryDirectory + "/man_walktran_lbs_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_walktran_ebs_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_walktran_brt_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_walktran_lrt_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_walktran_crl_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_drivtran_lbs_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_drivtran_ebs_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_drivtran_brt_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_drivtran_lrt_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_drivtran_crl_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_trandriv_lbs_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_trandriv_ebs_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_trandriv_brt_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_trandriv_lrt_pm.binary");
		man_transitpm.add ( binaryDirectory + "/man_trandriv_crl_pm.binary");
		convert (man_transitpm, tpplusDirectory + "/man_transitpm.tpp");

		ArrayList nonman_transitpm = new ArrayList();
		nonman_transitpm.add ( binaryDirectory + "/nonman_walktran_lbs_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_walktran_ebs_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_walktran_brt_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_walktran_lrt_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_walktran_crl_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_drivtran_lbs_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_drivtran_ebs_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_drivtran_brt_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_drivtran_lrt_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_drivtran_crl_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_trandriv_lbs_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_trandriv_ebs_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_trandriv_brt_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_trandriv_lrt_pm.binary");
		nonman_transitpm.add ( binaryDirectory + "/nonman_trandriv_crl_pm.binary");
		convert (nonman_transitpm, tpplusDirectory + "/nonman_transitpm.tpp");

		ArrayList man_transitmd = new ArrayList();
		man_transitmd.add ( binaryDirectory + "/man_walktran_lbs_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_walktran_ebs_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_walktran_brt_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_walktran_lrt_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_walktran_crl_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_drivtran_lbs_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_drivtran_ebs_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_drivtran_brt_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_drivtran_lrt_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_drivtran_crl_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_trandriv_lbs_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_trandriv_ebs_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_trandriv_brt_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_trandriv_lrt_md.binary");
		man_transitmd.add ( binaryDirectory + "/man_trandriv_crl_md.binary");
		convert (man_transitmd, tpplusDirectory + "/man_transitmd.tpp");

		ArrayList nonman_transitmd = new ArrayList();
		nonman_transitmd.add ( binaryDirectory + "/nonman_walktran_lbs_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_walktran_ebs_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_walktran_brt_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_walktran_lrt_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_walktran_crl_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_drivtran_lbs_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_drivtran_ebs_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_drivtran_brt_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_drivtran_lrt_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_drivtran_crl_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_trandriv_lbs_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_trandriv_ebs_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_trandriv_brt_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_trandriv_lrt_md.binary");
		nonman_transitmd.add ( binaryDirectory + "/nonman_trandriv_crl_md.binary");
		convert (nonman_transitmd, tpplusDirectory + "/nonman_transitmd.tpp");

		ArrayList man_transitnt = new ArrayList();
		man_transitnt.add ( binaryDirectory + "/man_walktran_lbs_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_walktran_ebs_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_walktran_brt_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_walktran_lrt_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_walktran_crl_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_drivtran_lbs_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_drivtran_ebs_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_drivtran_brt_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_drivtran_lrt_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_drivtran_crl_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_trandriv_lbs_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_trandriv_ebs_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_trandriv_brt_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_trandriv_lrt_nt.binary");
		man_transitnt.add ( binaryDirectory + "/man_trandriv_crl_nt.binary");
		convert (man_transitnt, tpplusDirectory + "/man_transitnt.tpp");

		ArrayList nonman_transitnt = new ArrayList();
		nonman_transitnt.add ( binaryDirectory + "/nonman_walktran_lbs_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_walktran_ebs_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_walktran_brt_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_walktran_lrt_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_walktran_crl_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_drivtran_lbs_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_drivtran_ebs_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_drivtran_brt_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_drivtran_lrt_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_drivtran_crl_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_trandriv_lbs_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_trandriv_ebs_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_trandriv_brt_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_trandriv_lrt_nt.binary");
		nonman_transitnt.add ( binaryDirectory + "/nonman_trandriv_crl_nt.binary");
		convert (nonman_transitnt, tpplusDirectory + "/nonman_transitnt.tpp");
		
		ArrayList man_nonmotoram = new ArrayList();
		man_nonmotoram.add ( binaryDirectory + "/man_nonmotor_am.binary");
		convert (man_nonmotoram, tpplusDirectory + "/man_nonmotoram.tpp");

		ArrayList nonman_nonmotoram = new ArrayList();
		nonman_nonmotoram.add ( binaryDirectory + "/nonman_nonmotor_am.binary");
		convert (nonman_nonmotoram, tpplusDirectory + "/nonman_nonmotoram.tpp");

		ArrayList man_nonmotorpm = new ArrayList();
		man_nonmotorpm.add ( binaryDirectory + "/man_nonmotor_pm.binary");
		convert (man_nonmotorpm, tpplusDirectory + "/man_nonmotorpm.tpp");

		ArrayList nonman_nonmotorpm = new ArrayList();
		nonman_nonmotorpm.add ( binaryDirectory + "/nonman_nonmotor_pm.binary");
		convert (nonman_nonmotorpm, tpplusDirectory + "/nonman_nonmotorpm.tpp");

		ArrayList man_nonmotormd = new ArrayList();
		man_nonmotormd.add ( binaryDirectory + "/man_nonmotor_md.binary");
		convert (man_nonmotormd, tpplusDirectory + "/man_nonmotormd.tpp");

		ArrayList nonman_nonmotormd = new ArrayList();
		nonman_nonmotormd.add ( binaryDirectory + "/nonman_nonmotor_md.binary");
		convert (nonman_nonmotormd, tpplusDirectory + "/nonman_nonmotormd.tpp");

		ArrayList man_nonmotornt = new ArrayList();
		man_nonmotornt.add ( binaryDirectory + "/man_nonmotor_nt.binary");
		convert (man_nonmotornt, tpplusDirectory + "/man_nonmotornt.tpp");

		ArrayList nonman_nonmotornt = new ArrayList();
		nonman_nonmotornt.add ( binaryDirectory + "/nonman_nonmotor_nt.binary");
		convert (nonman_nonmotornt, tpplusDirectory + "/nonman_nonmotornt.tpp");

	}


	private void convert ( ArrayList fileList, String tppFileName) {

		Matrix[] mArray = new Matrix[fileList.size()];
		
		
//		systemCommand ( "set path" );
		

		for (int i=0; i < fileList.size(); i++) {

			mw.setMessage2 ("converting " + fileList.get(i) + " to "  + tppFileName);
	
			MatrixReader binReader = MatrixReader.createReader ( MatrixType.BINARY, new File( (String)fileList.get(i) ) );
			mArray[i] = binReader.readMatrix();
			
			logger.info( "matrix total for table: " + (String)fileList.get(i) + " = " + mArray[i].getSum() );				
		}

		MatrixWriter tppWriter = MatrixWriter.createWriter (MatrixType.TPPLUS, new File( tppFileName ) );
		tppWriter.writeMatrices ( Integer.toString(fileList.size()), mArray );
	}


	public void convertTppToBinary () {

		String originalSkimsDirectory =  ResourceUtil.getProperty(rb, "originalSkimsDirectory.tpplus");
		String binaryDirectory =  ResourceUtil.getProperty(rb, "OutputDirectory.binary");

		ArrayList fileList = new ArrayList();
		
		fileList.add ( "hwyam.skm" );
		fileList.add ( "hwymd.skm" );
		fileList.add ( "bestdram.skm" );
		fileList.add ( "bestdrmd.skm" );
		fileList.add ( "bestwkam.skm" );
		fileList.add ( "bestwkmd.skm" );
		fileList.add ( "lbuswkam.skm" );
		fileList.add ( "lbuswkmd.skm" );
		fileList.add ( "lbusdram.skm" );
		fileList.add ( "lbusdrmd.skm" );
		fileList.add ( "ebuswkam.skm" );
		fileList.add ( "ebuswkmd.skm" );
		fileList.add ( "ebusdram.skm" );
		fileList.add ( "ebusdrmd.skm" );
		fileList.add ( "brtwkam.skm" );
		fileList.add ( "brtwkmd.skm" );
		fileList.add ( "brtdram.skm" );
		fileList.add ( "brtdrmd.skm" );
		fileList.add ( "lrtwkam.skm" );
		fileList.add ( "lrtwkmd.skm" );
		fileList.add ( "lrtdram.skm" );
		fileList.add ( "lrtdrmd.skm" );
		fileList.add ( "crlwkam.skm" );
		fileList.add ( "crlwkmd.skm" );
		fileList.add ( "crldram.skm" );
		fileList.add ( "crldrmd.skm" );
		
		for (int i=0; i < fileList.size(); i++) {
			mw.setMessage2 ("converting " + (String)fileList.get(i) + " to binary." );
			MatrixReader tppReader = MatrixReader.createReader ( MatrixType.TPPLUS, new File( originalSkimsDirectory + "/" + (String)fileList.get(i) ) );
			
			Matrix[] m = tppReader.readMatrices();
		
			for (int j=1; j < m.length; j++) {
				int lastIndex = ((String)fileList.get(i)).lastIndexOf('.');
				String name = binaryDirectory + "/" + ((String)fileList.get(i)).substring(0, lastIndex) + j + ".binary";
			
				MatrixWriter binWriter = MatrixWriter.createWriter (MatrixType.BINARY, new File(name) );
				binWriter.writeMatrix ( m[j] );
			}
		}
    }

	
	public void runDOSCommand (String command) throws InterruptedException {
		
		try {
			String s;
			Process proc = Runtime.getRuntime().exec( "c:\\windows\\system32\\cmd.exe /C " + command);
            
			BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
 
			stdoutLog.println("<<<<<  stdout log for: " + command);
			stdoutLog.flush();
			while ((s = stdout.readLine()) != null) {
				stdoutLog.println(s);
				stdoutLog.flush();
			}
			stdoutLog.println(">>>>>  end of stdout log for: " + command);
			stdoutLog.println("");
			stdoutLog.println("");
			stdoutLog.flush();
 
			stderrLog.println("<<<<<  stderr log for: " + command);
			stderrLog.flush();
			while ((s = stderr.readLine()) != null) {
				stderrLog.println(s);
				stderrLog.flush();
			}
			stdoutLog.println(">>>>>  end of stderr log for: " + command);
			stdoutLog.println("");
			stdoutLog.println("");
			stderrLog.flush();
		} 
		catch (IOException e) {
			System.err.println("exception: "+e);
		}
	}



	void systemCommand (String command) {

		try {
			
			writeToErrors ("          " + command);
			runDOSCommand (command);

		} catch (InterruptedException e) {

			System.out.println ("Interrupted exception ocurred for command: " + command);

		}
	}



	void writeToErrors (String message) {
		try {
			// open current errors file to append message
			PrintWriter out = new PrintWriter (
				new BufferedWriter (
					new FileWriter ("errors", true)));

			// write message to errors
			out.println(message);
				
			// close errors file
			out.close();
		} catch (IOException e) {
			System.out.println ("errors file could not be opened for appending, or other IO exception ocurred.");
			System.out.println ("attempted to write: " + message);
		}
	}



    public static void main(String[] args) {

        // create a model runner object and run models
		MorpcMatrixConverter mx = new MorpcMatrixConverter ();
		

		// open log files for capturing screen output
		try {
			stdoutLog = new PrintWriter (
				new BufferedWriter (
					new FileWriter ("stdout.log")));

			try {

				stderrLog = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("stderr.log")));

			} catch (IOException e) {

				System.out.println ("Stderr log file could not be opened for writing, or other IO exception ocurred.");

			}

		} catch (IOException e) {

			System.out.println ("Stdout log file could not be opened for writing, or other IO exception ocurred.");

		}


		if ( args[0].equalsIgnoreCase("tpplusbinary") ) {

		    mx.convertTppToBinary();	

			// aggregate the original 129 matrices specified for Stop location choice into 25.
			HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap ("morpc" );
			MorpcMatrixAggregater ma = new MorpcMatrixAggregater(propertyMap);
			ma.aggregateSlcSkims();

		}
		else if ( args[0].equalsIgnoreCase("binarytpplus") ) {

		    mx.convertBinaryToTpp();	

		}
                        
        logger.debug("end of converting MORPC TP+ Matrices");
        mx.mw.setMessage3 ("end of converting MORPC TP+ Matrices");
        
		logger.info( "end of converting MORPC TP+ Matrices" );				

        System.exit (0);
        
    }
    
}
