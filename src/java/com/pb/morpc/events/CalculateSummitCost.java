/*
 * Created on Oct 11, 2004
 *
 */
package com.pb.morpc.events;

import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.Matrix;
import java.util.HashMap;

/**
 * @author SunW
 * <sunw@pbworld.com>
 */
public class CalculateSummitCost {
	
	protected SpecialEventDataReader dataReader;
	protected HashMap propertyMap;
	protected MatrixReader reader;
	protected String logsumsFile;
	protected Matrix logsums;
	protected float beta;
	protected Matrix cost;
	
	public CalculateSummitCost(SpecialEventDataReader dataReader){
		this.dataReader=dataReader;
		propertyMap=dataReader.getPropertyMap();
		readBeta();
		calCost();
	}
	
	public Matrix getCost(){
		return cost;
	}
		
	private void readBeta(){
		beta=new Float((String)propertyMap.get("events.beta")).floatValue();
	}
	
	private void calCost(){
		int NoRows=logsums.getRowCount();
		int NoCols=logsums.getColumnCount();
		cost=logsums.multiply(beta);
	}
}
