package com.pb.morpc.synpop;



/**
 * ZonalData class for containing zonal data table.
 *
 */
public class ZonalData {

	int countyID;
	int puma;
	float areaType;
	float avgHHSize;
	float avgWorkers;
	float avgIncome;
	float hhs;
   
    public ZonalData () {
    }
    
   
   	public float getHHs() {
   		return hhs;
   	} 

   	public int getPuma() {
   		return puma;
   	} 

   	public float getAreaType() {
   		return areaType;
   	} 

   	public float getAvgHHSize() {
   		return avgHHSize;
   	} 

   	public float getAvgWorkers() {
   		return avgWorkers;
   	} 

   	public float getAvgIncome() {
   		return avgIncome;
   	} 



   	public void setHHs(float hhs) {
   		this.hhs = hhs;
   	} 

   	public void setPuma(float puma) {
   		this.puma = (int)puma;
   	} 

   	public void setAreaType(float areaType) {
   		this.areaType = areaType;
   	} 

   	public void setAvgHHSize(float avgHHSize) {
   		this.avgHHSize = avgHHSize;
   	} 

   	public void setAvgWorkers(float avgWorkers) {
   		this.avgWorkers = avgWorkers;
   	} 

   	public void setAvgIncome(float avgIncome) {
   		this.avgIncome = avgIncome;
   	} 



	public boolean isFranklinCounty() {
		if (countyID == 49)
			return true;
		else
			return false;
	}
}

