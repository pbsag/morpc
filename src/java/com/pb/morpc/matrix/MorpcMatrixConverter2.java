package com.pb.morpc.matrix;

/**
 * @author Jim Hicks
 *
 * Convert MORPC TP+ matrices to binary
 */

import java.awt.Container;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;


public class MorpcMatrixConverter2 extends JFrame {
    
    private static JLabel jMsg1 = new JLabel();
    private static JLabel jMsg2 = new JLabel();
    private static JLabel jMsg3 = new JLabel();
    static Logger logger = Logger.getLogger("com.pb.morpc.matrix");

    
    public MorpcMatrixConverter2 () {
        super ("MORPC TP+ to Binary Matrix Converter");
        
        Container cp = getContentPane();
        cp.setLayout (new GridLayout(3,1));
        jMsg1.setText ("");
        cp.add (jMsg1);
        jMsg2.setText ("");
        cp.add (jMsg2);
        jMsg3.setText ("");
        cp.add (jMsg3);

		// create a message box to report which macro file is running
		this.addWindowListener (
			new WindowAdapter() {
				public void windowClosing (WindowEvent e) {
					System.exit(0);
				}
			} );

		this.setSize (700,100);
		this.setVisible(true);

    }


    public static void setMessage1 (String message) {
        jMsg1.setText (message);
    }


    public static void setMessage2 (String message) {
        jMsg2.setText ("        " + message);
    }


    public static void setMessage3 (String message) {
        jMsg3.setText ("        " + message);
    }


    public void convertMatrices () {

		ArrayList fileList = new ArrayList();
		
		fileList.add ("c:/morpc_module/tppMatrices/hwyam1.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam2.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam3.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam4.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam5.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam6.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam7.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam8.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam9.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd1.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd2.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd3.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd4.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd5.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd6.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd7.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd8.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwymd9.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy1.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy2.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy3.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy4.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy5.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy6.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy7.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy8.binary");
		fileList.add ("c:/morpc_module/tppMatrices/hwyam_copy9.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam1.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam2.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam3.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam4.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam5.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam6.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam7.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam8.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam9.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam10.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkam11.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd1.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd2.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd3.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd4.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd5.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd6.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd7.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd8.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd9.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd10.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestwkmd11.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram1.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram2.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram3.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram4.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram5.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram6.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram7.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram8.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram9.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram10.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram11.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdram12.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd1.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd2.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd3.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd4.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd5.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd6.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd7.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd8.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd9.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd10.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd11.binary");
		fileList.add ("c:/morpc_module/tppMatrices/bestdrmd12.binary");

		
		Matrix matrixData = null;
		float[] rowBuf = null;
		float[] rowBuf2 = null;

		for (int i=0; i < fileList.size(); i++) {
			setMessage2 ("converting " + (String)fileList.get(i) );
			MatrixReader binReader = MatrixReader.createReader ( MatrixType.BINARY, new File( (String)fileList.get(i) ) );
			
			Matrix m = binReader.readMatrix();

			if (i == 0) {
				matrixData = new Matrix(m.getRowCount(), m.getRowCount());
				rowBuf = new float[m.getRowCount()];
				rowBuf2 = new float[m.getRowCount()];
			}


			int lastIndex = ((String)fileList.get(i)).lastIndexOf('.');
			String name = ((String)fileList.get(i)).substring(0, lastIndex) + ".binary2";
		
			MatrixWriter binWriter = MatrixWriter.createWriter (MatrixType.BINARY, new File(name) );
			
			for (int k=2; k < m.getRowCount(); k++) {
				m.getRow (k, rowBuf);
				for (int n=1; n < rowBuf.length; n++)
					rowBuf2[n-1] = rowBuf[n];
				matrixData.setRow (rowBuf2, k-1);
			}
			
			binWriter.writeMatrix ( matrixData );
		}
    }

	
    public static void main(String[] args) {

        // create a model runner object and run models
		MorpcMatrixConverter2 mx = new MorpcMatrixConverter2 ();
        mx.convertMatrices();
                        
        logger.debug("end of converting MORPC TP+ Matrices");
        System.exit (0);
        
    }
    
}
