/*
 * Created on Mar 8, 2005
 *
 */
package com.pb.morpc.util;
import java.util.Vector;
import com.pb.morpc.structures.SummitAggregationRecord;

/**
 * @author SunW
 * <sunw@pbworld.com>
 */
public class VectorToArrayConvertor {
	
	protected Vector vector;
	protected int [] intArray;
	protected float [] floatArray;
	protected double [] doubleArray;
	protected String [] stringArray;
	protected int size;
	
	public VectorToArrayConvertor(Vector vector){
		this.vector=vector;
		size=vector.size();
	}
	
	public int [] getIntArray(){
		int [] result=new int[size];
		for(int i=0; i<size; i++){
			result[i]=((Integer)vector.get(i)).intValue();
		}
		return result;
	}
	
	public float [] getFloatArray(){
		float [] result=new float[size];
		for(int i=0; i<size; i++){
			result[i]=((Float)vector.get(i)).floatValue();
		}
		return result;
	}
	
	public double [] getDoubleArray(){
		double [] result=new double[size];
		for(int i=0; i<size; i++){
			result[i]=((Double)vector.get(i)).doubleValue();
		}
		return result;
	}
	
	public String [] getStringArray(){
		String [] result=new String[size];
		for(int i=0; i<size; i++){
			result[i]=(String)vector.get(i);
		}
		return result;
	}
	
	public SummitAggregationRecord [] getSummitAggregationArray(){
		SummitAggregationRecord [] result=new SummitAggregationRecord[size];
		for(int i=0; i<size; i++){
			result[i]=(SummitAggregationRecord)vector.get(i);
		}
		return result;		
	}

}
