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
	//person ID
	protected int personID;
	//tour ID
	protected int tourID;
	//purpose, 1 to 8
	protected int purpose;
    //tour categor, 1 to 4
	protected int tourCategory;
	//party size, 1 to ?, if 1 then not a joint tour
	protected int partySize;
	//HH income group, 1 to 3
	protected int hhIncome;
	//HH car ownership, 1 to 5 (represents 0,1,2,3,4+)
	protected int auto;
	//number of workers, 0 to 4 (represents 0,1,2,3,4+)
	protected int workers;
	//origin zone
	protected int origin;
	//destination zone
	protected int destination;
	//origin transit access sub-zone, 0 to 2
	protected int origSubZone;
	//destination transit access sub-zone, 0 to 2
	protected int destSubZone;
	//outbound TOD period, 1 to 4
	protected int outTOD;
	//inbound TOD period, 1 to 4
	protected int inTOD;
	//chosen mode, 1 to 6
	protected int mode;
	//probabilities
	protected double [] probs;
	//exponentiated utilities
	protected double [] expUtils;
	//alternative names
	protected String [] altNames;
	
	public SummitAggregationRecord(){		
	}
	
	public void setHouseholdID(int householdID){
		this.householdID=householdID;
	}
	
	public void setPersonID(int personID){
		this.personID=personID;
	}
	
	public void setTourID(int tourID){
		this.tourID=tourID;
	}
	
	public void setPurpose(int purpose){
		this.purpose=purpose;
	}	
	
	public void setTourCategory(int tourCategory){
		this.tourCategory=tourCategory;
	}
	
	public void setPartySize(int partySize){
		this.partySize=partySize;
	}
	
	public void setHHIncome(int hhIncome){
		this.hhIncome=hhIncome;
	}
	
	public void setAuto(int auto){
		this.auto=auto;
	}
	
	public void setWorkers(int workers){
		this.workers=workers;
	}
	
	public void setOrigin(int origin){
		this.origin=origin;
	}
	
	public void setDestination(int destination){
		this.destination=destination;
	}
	
	public void setOrigSubZone(int origSubZone){
		this.origSubZone=origSubZone;
	}
	
	public void setDestSubZone(int destSubZone){
		this.destSubZone=destSubZone;
	}
	
	public void setOutTOD(int outTOD){
		this.outTOD=outTOD;
	}
	
	public void setInTOD(int inTOD){
		this.inTOD=inTOD;
	}
	
	public void setMode(int mode){
		this.mode=mode;
	}
	
	public void setProbs(double [] probs){
		this.probs=probs;
	}
	
	public void setUtils(double [] expUtils){
		this.expUtils=expUtils;
	}
	
	public void setAltNames(String [] altNames){
		this.altNames=altNames;
	}
		
	public int getHouseholdID(){
		return householdID;
	}
	
	public int getPersonID(){
		return personID;
	}
	
	public int getTourID(){
		return tourID;
	}
	
	public int getPurpose(){
		return purpose;
	}
	
	public int getTourCategory(){
		return tourCategory;
	}
	
	public int getPartySize(){
		return partySize;
	}
	
	public int getHHIncome(){
		return hhIncome;
	}
	
	public int getAuto(){
		return auto;
	}
	
	public int getWorkers(){
		return workers;
	}
	
	public int getOrigin(){
		return origin;
	}
	
	public int getDestination(){
		return destination;
	}
	
	public int getOrigSubZone(){
		return origSubZone;
	}
	
	public int getDestSubZone(){
		return destSubZone;
	}
	
	public int getOutTOD(){
		return outTOD;
	}
	
	public int getInTOD(){
		return inTOD;
	}
	
	public int getMode(){
		return mode;
	}
	
	public double [] getProbs(){
		return probs;
	}
	
	public double [] getExpUtils(){
		return expUtils;
	}
	
	public int getNoFields(){
		return 16+probs.length+expUtils.length;
	}
	
	public String [] getAltNames(){
		return altNames;
	}

}
