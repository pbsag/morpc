package com.pb.morpc.report;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import org.apache.log4j.Logger;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Nov. 3, 2003
 */
public class ReportTester {
    static Logger logger = Logger.getLogger("com.pb.morpc.report");

    public ReportTester() {
    }

    public void writeReport(String logging, HashMap propertyMap) {
        Report report = new Report(propertyMap);
        report.setLoggingInfo(logging);
        report.generateReports();
    }

    public static void main(String[] args) {
        
        String loggingString;
        double markTime=System.currentTimeMillis();
        
        HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap("morpc_bench");
        ReportTester rt = new ReportTester();
        
        if ( args.length > 0 )
            loggingString = args[0];
        else
            loggingString = "";
        
        rt.writeReport(loggingString, propertyMap);
        
        double runningTime = System.currentTimeMillis() - markTime;
        logger.info ("total running minutes = " + (float)((runningTime/1000.0)/60.0));
    }
}
