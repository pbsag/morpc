package com.pb.morpc.synpop.pums;

import com.pb.morpc.synpop.HH;

import java.util.logging.Logger;

/**
 * The PUMSHH class is a HH with additional PUMS related attributes:
 *      puma
 *      weight
 *
 * Attributes are labeled by the actual PUMS variables.
 *
 */
public class PUMSHH extends HH implements Cloneable{

    protected static Logger logger = Logger.getLogger("com.pb.common.util");

    public static final int HHSIZES = 9;
    public static final int HHSIZES_BASE = 6;
    public static final int WORKERS = 6;
    public static final int WORKERS_BASE = 4;
    public static final int INCOMES = 3;
    public static final int INCOMES_BASE = 3;
    
    public static final String[] HHSIZE_BASE_Labels = { "1", "2", "3", "4", "5", "6+" };
//    public static final float[] HHSIZE_BASE_Categories = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.5f };
    public static final String[] HHSIZE_Labels = { "1", "2", "3", "4", "5", "6", "7", "8", "9+" };
    public static final float[] HHSIZE_Categories = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 10.1f };
    public static final String[] WORKER_BASE_Labels = { "0", "1", "2", "3+" };
//    public static final float[] WORKER_BASE_Categories = { 0.0f, 1.0f, 2.0f, 3.5f };
    public static final String[] WORKER_Labels = { "0", "1", "2", "3", "4", "5+" };
    public static final float[] WORKER_Categories = { 0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.2f };
    public static final String[] INCOME_BASE_Labels = { "LOW", "MEDIUM", "HIGH" };
//    public static final float[] INCOME_BASE_Categories = { 15000.0f, 52500.0f, 85000.0f };
    public static final String[] INCOME_Labels = { "LOW", "MEDIUM", "HIGH" };
    public static final float[] INCOME_Categories = { 15000.0f, 52500.0f, 85000.0f };


    
    public PUMSHH () {
        this.attribLabels = new String[5];
        attribLabels[0] = "PERSONS";
        attribLabels[1] = "RWRKR89";
        attribLabels[2] = "RHHINC";
        attribLabels[3] = "PUMA";
        attribLabels[4] = "HOUSWGT";

        this.attribs = new int[5];
    }

    public Object clone(){
        PUMSHH o = null;
 //       try{
            o = (PUMSHH) super.clone();
 //       }catch(CloneNotSupportedException e){
 //           logger.severe("Error: PUMSHH can't clone");
 //           System.exit(1);
 //       }
        return o;
     }
	/**
	This method takes a hh size value and returns one of the pre-defined
	hh size categories (0 to HHSIZES - 1) for the model.
	**/
	public static int getHHSizeCategory (int hhSize) {
		
		if (hhSize <= 0) {
            logger.severe("trying to set HH Size category for hhSize= " + hhSize + ".  HH Size should be >= 1.");
            logger.severe("exiting getHHSizeCategory() in PUMSHH.");
            logger.severe("exit (10)");
            System.exit (10);
		}
		else if (hhSize > HHSIZES - 1) {
			hhSize = HHSIZES;
		}
		
		return hhSize - 1;
	}


	/**
	This method takes a hh income value and returns one of the pre-defined
	hh income categories (0-2) for the model.
	**/
	public static int getIncomeCategory (int income) {
		
		if (income < 30000) {
			income = 0;
		}
		else if (income < 75000) {
			income = 1;
		}
		else {
			income = 2;
		}
		
		return income;
	}


	/**
	This method takes a hh workers value and returns one of the pre-defined
	hh worker categories (0 to WORKERS -1) for the model.
	**/
	public static int getWorkerCategory (int workers) {
		
		if (workers < 0) {
            logger.severe("trying to set HH Workers category for workers= " + workers + ".  HH Workers should be >= 0.");
            logger.severe("exiting getWorkerCategory() in PUMSHH.");
            logger.severe("exit (12)");
            System.exit (12);
		}
		else if (workers > WORKERS - 1) {
			workers = WORKERS - 1;
		}
		
		return workers;
	}


	/**
	This method takes a person number argument and returns one of the pre-defined
	person types (1-7) for the model.
	**/
	public int getPersonType (int personNumberInHH) {
		
		if (personNumberInHH < 0 || personNumberInHH >= HHSIZES) {
            logger.severe("trying to get person type for personNumberInHH= " + personNumberInHH + ".  HH person number should be >= 0 and < " + HHSIZES + ".");
            logger.severe("exiting getPersonType() in PUMSHH.");
            logger.severe("exit (13)");
            System.exit (13);
		}
		
		return this.personTypes[personNumberInHH];
	}
}

