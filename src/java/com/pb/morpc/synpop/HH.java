package com.pb.morpc.synpop;

import org.apache.log4j.Logger;

/**
 * The HH class defines HH objects for a population.
 *
 * A HH has:
 *      HH attributes
 *      HH attribute labels
 *
 */
public class HH implements Cloneable{

    public String[] attribLabels;
    public int[] attribs;
    public int[] categoryValues;
    public int[] personTypes;
    public float areaType;
    public boolean adjusted;
    private int hhNumber;
    private int serialno;
    protected static Logger logger = Logger.getLogger("com.pb.common.util");
    
    public HH () {
        this.attribLabels = new String[3];
        attribLabels[0] = "HHSIZE";
        attribLabels[1] = "WORKERS";
        attribLabels[2] = "INCOME";

        this.attribs = new int[3];
        this.categoryValues = new int[3];
    }

    
    public void setAdjusted (boolean setValue) {
        adjusted = setValue;                    
    }
    
    public void setHHNumber (int newNumber) {
        this.hhNumber = newNumber;
    }
    
    public int getHHNumber () {
        return this.hhNumber;
    }
    
    public void setHHSerialno (int newNumber) {
        this.serialno = newNumber;
    }
    
    public int getHHSerialno () {
        return this.serialno;
    }
    
    public Object clone(){
        HH o = null;
        try{
            o= (HH)super.clone();
            
            
        }catch(CloneNotSupportedException e){
            logger.fatal("Error: HH can't clone");
            System.exit(1);
        }
        //clone references
        if(this.attribLabels!=null){
            o.attribLabels = new String[this.attribLabels.length];
            System.arraycopy(this.attribLabels,0,o.attribLabels,0,this.attribLabels.length); 
        }

        if(this.attribs!=null){
            o.attribs = new int[this.attribs.length];
            System.arraycopy(this.attribs,0,o.attribs,0,this.attribs.length); 
        }
        if(this.categoryValues!=null){
            o.categoryValues = new int[this.categoryValues.length];
            System.arraycopy(this.categoryValues,0,o.categoryValues,0,this.categoryValues.length); 
        }
        if(this.personTypes!=null){
            o.personTypes = new int[this.personTypes.length];       
            System.arraycopy(this.personTypes,0,o.personTypes,0,this.personTypes.length); 
        }
        return o;
     
       
    }        
}
