package com.pb.morpc.report;

import com.pb.common.util.ResourceUtil;

import java.util.HashMap;
import java.util.logging.Logger;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Nov. 3, 2003
 */
public class ReportTester {
    static Logger logger = Logger.getLogger("com.pb.morpc.report");
    private HashMap propertyMap = null;

    public ReportTester() {
        propertyMap = ResourceUtil.getResourceBundleAsHashMap("morpc");
    }

    public void writeReport() {
        Report report = new Report();
        report.generateReports();
    }

    public static void main(String[] args) {
        double markTime=System.currentTimeMillis();
        ReportTester rt = new ReportTester();
        rt.writeReport();
        double runningTime = System.currentTimeMillis() - markTime;
        logger.info ("total running minutes = " + (float)((runningTime/1000.0)/60.0));
    }
}
