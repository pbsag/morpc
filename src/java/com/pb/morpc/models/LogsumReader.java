/*
 * Created on Dec 7, 2004
 * Use this class to open a logsum table.
 */
package com.pb.morpc.models;

import com.pb.common.util.ResourceUtil;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import java.util.HashMap;
import org.apache.log4j.Logger;
import java.io.IOException;
;
/**
 * @author sunw
 * <sunw@pbworld.com>
 */
public class LogsumReader {
	
    protected static HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap("morpc1");
    protected static Logger logger = Logger.getLogger("com.pb.morpc.models");
    protected static String logsumFile=(String)propertyMap.get("logsumFile");
    protected static CSVFileReader reader;
    protected static TableDataSet logsumTable;

    static{
    	reader=new CSVFileReader();
    	try{
    		logsumTable=reader.readTable(logsumFile);
    	}catch(IOException e){
    		logger.error("failed opening logsum table:"+logsumFile);
    	}
    }
    
    public static TableDataSet getLogsumTable(){
    	return logsumTable;
    }

}
