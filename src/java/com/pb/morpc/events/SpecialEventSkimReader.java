/*
 * Created on Sep 30, 2004
 *
 */
package com.pb.morpc.events;

import java.io.File;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.Matrix;

/**
 * @author SunW
 *
 */
public class SpecialEventSkimReader {
	
	protected MatrixReader reader;
	protected float [][] data;
	
	public SpecialEventSkimReader(String file){
		File binFile=new File(file);
		reader = MatrixReader.createReader ( MatrixType.BINARY, binFile);
		Matrix binMatrix=reader.readMatrix();
		data=binMatrix.getValues();
	}
	
	public float [][] getSkim(){
		return data;
	}
}
