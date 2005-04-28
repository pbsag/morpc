/*
 * Created on Apr 14, 2005
 * This class defines Mode Choice alternatives
 * 
 * !!!!!!!!!!!!!!!!!!!IMPORTANT!!!!!!!!!!!!!!!!!!!!!!!!!
 * The name and order of alternatives in this class must match those in MC UEC.
 */
package com.pb.morpc.structures;
import java.io.Serializable;

/**
 * @author Wu Sun
 * <sunw@pbworld.com>
 *
 */
public final class MCAlternatives implements Serializable {
	
    public static final String [] MCAlts={"SOV","HOV","Walk_trn","Drive_trn","Non_motor","Schl_bus"};
    
    public static int getNoMCAlternatives(){
    	return MCAlts.length;
    }
    
    /**
     * get MC alternative by index
     * @param index, importatn 1-based
     * @return
     */
    public static String getMCAlt(int index){
    	return MCAlts[index-1];
    }
    
    /**
     * given MC alternative name, get alternative index
     * @param altName
     * @return
     */
    public static int getMCAltIndex(String altName){
    	int index=-1;
    	for(int i=0; i<MCAlts.length; i++){
    		if(altName.equalsIgnoreCase(MCAlts[i])){
    			index=i+1;
    			break;
    		}
    	}
    	return index;
    }
}
