/*
 * Created on Dec 7, 2004
 * This class represents a logsum record.
 */
package com.pb.morpc.structures;

import java.io.Serializable;

/**
 * @author Wu Sun
 * <sunw@pbworld.com>
 */
public class SummitAggregationRecord implements Serializable{
	//hh ID
    protected int householdID;
    //tour categor
	protected int tourCategory;
	//person ID
	protected int personID;
	//tour ID
	protected int tourID;
	//exponentiated utilities
	protected double [] expUtils;
	//probabilities
	protected double [] probs;
	
	/*
	private int subtourID;
	private double logsum;
	*/
	
	public SummitAggregationRecord(){		
	}
	
	public void setHouseholdID(int householdID){
		this.householdID=householdID;
	}
	
	public void setTourCategory(int tourCategory){
		this.tourCategory=tourCategory;
	}
	
	public void setPersonID(int personID){
		this.personID=personID;
	}
	
	public void setTourID(int tourID){
		this.tourID=tourID;
	}
	
	public void setExpUtils(double [] expUtils){
		this.expUtils=expUtils;
	}
	
	public void setProbs(double [] probs){
		this.probs=probs;
	}
	
	/*
	public void setSubtourID(int subtourID){
		this.subtourID=subtourID;
	}
	
	public void setLogsum(double logsum){
		this.logsum=logsum;
	}
	*/
	
	public int getHouseholdID(){
		return householdID;
	}
	
	public int getTourCategory(){
		return tourCategory;
	}
	
	public int getPersonID(){
		return personID;
	}
	
	public int getTourID(){
		return tourID;
	}
	
	/*
	public int getSubtourID(){
		return subtourID;
	}
	
	public double getLogsum(){
		return logsum;
	}
	*/
	
	public double [] getExpUtils(){
		return expUtils;
	}
	
	public double [] getProbs(){
		return probs;
	}
	
	public int getNoFields(){
		return 4+probs.length+expUtils.length;
	}

}
