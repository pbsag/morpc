/*
 * Created on Jun 25, 2004
 *
 *Assistant Class for TOD indexing
 */
package com.pb.morpc.events;

import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * @author Wu Sun <sunw@pbworld.com>
 *
 */
public class TODIndex {
	
	private static Logger logger = Logger.getLogger("com.pb.morpc.events");
	private static HashMap indexMap;
	private static HashMap indexMapR;
	
	static{
		//Assume data sheet orders in UEC control file are: AM, MD, PM, NT
		indexMap=new HashMap();
		indexMap.put(new Integer(1),"AM");
		indexMap.put(new Integer(2),"MD");
		indexMap.put(new Integer(3),"PM");
		indexMap.put(new Integer(4),"NT");
		
		indexMapR=new HashMap();
		indexMapR.put("AM",new Integer(1));
		indexMapR.put("MD",new Integer(2));
		indexMapR.put("PM",new Integer(3));
		indexMapR.put("NT",new Integer(4));
	}
	
	static String getNameByIndex(int index){
		
		String result=null;
		
		if(indexMap.containsKey(new Integer(index)))
			result=(String)indexMap.get(new Integer(index));
		else{
			logger.fatal("key "+index+" not found in TOD index map.");
			System.exit(-1);
		}
		return result;
	}
	
	static int getIndexByName(String name){
		int result=-1;
		
		if(indexMapR.containsKey(name))
			result=((Integer)indexMapR.get(name)).intValue();
		else{
			logger.fatal("key "+name+" not found in TOD index map.");
			System.exit(-1);
		}
		return result;
	}
	
	static int getNoTODs(){
		return indexMap.size();
	}
}
