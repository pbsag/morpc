/*
 * Created on May 28, 2004
 */
package com.pb.morpc.structures;

import com.pb.morpc.models.TODDataManager;
import com.pb.morpc.models.ZonalDataManager;
import java.util.HashMap;

/**
 * @author Wu Sun
 *
 * This is a structure to hold a ZonalDataManager object, a TodDataManager object, 
 * a static ZDM map, and a static TOD map.  
 * These objects are saved to hard drive in DiskObjectZDMTDM.file.
 */

public class ZDMTDM {
	
	private ZonalDataManager zdm;
	private TODDataManager tdm;
	private HashMap staticZonalDataMap;
	private HashMap staticTodDataMap;
	
	public void setZDM(ZonalDataManager zdm){
		this.zdm=zdm;
	}
	
	public void setTDM(TODDataManager tdm){
		this.tdm=tdm;
	}
	
	public void setStaticZonalDataMap(HashMap staticZonalDataMap){
		this.staticZonalDataMap=staticZonalDataMap;
	}
	
	public void setStaticTodDataMap(HashMap staticTodDataMap){
		this.staticTodDataMap=staticTodDataMap;
	}
	
	public ZonalDataManager getZDM(){
		return zdm;
	}
	
	public TODDataManager getTDM(){
		return tdm;
	}
	
	public HashMap getStaticZonalDataMap(){
		return staticZonalDataMap;
	}
	
	public HashMap getStaticTodDataMap(){
		return staticTodDataMap;
	}
}
