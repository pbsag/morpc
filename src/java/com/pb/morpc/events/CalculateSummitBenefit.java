/*
 * Created on Oct 11, 2004
 *
 */
package com.pb.morpc.events;

import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.Matrix;
import java.util.HashMap;
import java.io.File;

/**
 * @author SunW
 * <sunw@pbworld.com>
 */
public class CalculateSummitBenefit {
	
	protected SpecialEventDataReader dataReader;
	protected HashMap propertyMap;
	protected MatrixReader reader;
	protected String tripsFile;
	protected Matrix trips;
	protected CalculateSummitCost baselineCostCalculator;
	protected CalculateSummitCost buildCostCalculator;
	protected Matrix baselineCost;
	protected Matrix buildCost;
	protected String baselineLogsumsFile;
	protected String buildLogsumsFile;
	protected Matrix benefit;
	
	public CalculateSummitBenefit(SpecialEventDataReader dataReader, Matrix baselineCost, Matrix buildCost){;
		this.baselineCost=baselineCost;
		this.buildCost=buildCost;
		readTrips();
		calBenefit();
	}
	
	public Matrix getBenefit(){
		return benefit;
	}
	
	public float getTotBenefit(){
		return (float)benefit.getSum();
	}
	
	public float getBenefitAt(int row, int col){
		return benefit.getValueAt(row, col);
	}
	
	private void readTrips(){
		tripsFile=(String)propertyMap.get("tripTable.file");
		reader = MatrixReader.createReader ( MatrixType.BINARY, new File(tripsFile));
		trips=reader.readMatrix();
	}
	
	private void calBenefit(){
		Matrix costDiff=baselineCost.subtract(buildCost);
		int NoRows=costDiff.getRowCount();
		int NoCols=costDiff.getColumnCount();
		benefit=new Matrix(NoRows, NoCols);
		
		float temp=0f;
		for(int i=0; i<NoRows; i++){
			for(int j=0; j<NoCols; j++){
				temp=trips.getValueAt(i+1,j+1)*costDiff.getValueAt(i+1,j+1);
				benefit.setValueAt(i+1,j+1,temp);
			}
		}
	}
}
