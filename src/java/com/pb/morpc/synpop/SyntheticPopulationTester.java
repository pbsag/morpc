package com.pb.morpc.synpop;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.morpc.synpop.pums2000.PUMSData;
import com.pb.morpc.synpop.SyntheticPopulation;
import java.util.HashMap;
import java.io.*;

/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0
 */

public class SyntheticPopulationTester {
  private String PUMSData;
  private String PUMSDictionary;
  private String ZoneFile;
  private TableDataSet ZoneTable;
  private PUMSData pums;
  private static SyntheticPopulation sp;
  private static String OutPutHH;
  private static String ZonalTargetHH;
  private HashMap propertyMap = null;

  public SyntheticPopulationTester() {

    propertyMap = ResourceUtil.getResourceBundleAsHashMap ("morpc" );

    PUMSData=(String)propertyMap.get("PUMSData.file");
    PUMSDictionary=(String)propertyMap.get("PUMSDictionary.file");
    ZoneFile=(String)propertyMap.get("TAZData.file");

    OutPutHH=(String)propertyMap.get("SyntheticHousehold.file");
    ZonalTargetHH=(String)propertyMap.get("ZonalTargets.file");

    try {
        CSVFileReader reader = new CSVFileReader();
        ZoneTable = reader.readFile(new File(ZoneFile));
    }
    catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
    }

    PUMSData pums = new PUMSData(PUMSData, PUMSDictionary);
    pums.readData (PUMSData);
    pums.createPumaIndices();
    sp = new SyntheticPopulation(pums, ZoneTable);
  }

  public static void main(String [] args){
    SyntheticPopulationTester sptester=new SyntheticPopulationTester();
    sp.runSynPop(OutPutHH, ZonalTargetHH);
  }
}