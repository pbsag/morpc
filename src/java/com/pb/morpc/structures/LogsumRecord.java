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
public class LogsumRecord implements Serializable{
	//hh ID
    private int householdID;
    //tour categor
	private int tourCategory;
	//person ID
	private int personID;
	//tour ID
	private int tourID;
	//subtour ID
	private int subtourID;
	//logsum
	private double logsum;
	
	public LogsumRecord(){		
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
	
	public void setSubtourID(int subtourID){
		this.subtourID=subtourID;
	}
	
	public void setLogsum(double logsum){
		this.logsum=logsum;
	}
	
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
	
	public int getSubtourID(){
		return subtourID;
	}
	
	public double getLogsum(){
		return logsum;
	}

}
