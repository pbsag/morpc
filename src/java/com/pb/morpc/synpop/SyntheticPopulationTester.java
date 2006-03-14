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
  
  public SyntheticPopulationTester() {
  }

  private void runTest() {
      
      HashMap propertyMap = ResourceUtil.getResourceBundleAsHashMap ("morpc" );

      TableDataSet ZoneTable = null;

      String PUMSData=(String)propertyMap.get("PUMSData.file");
      String PUMSDictionary=(String)propertyMap.get("PUMSDictionary.file");
      String ZoneFile=(String)propertyMap.get("TAZData.file");

      String OutPutHH=(String)propertyMap.get("SyntheticHousehold.file");
      String ZonalTargetHH=(String)propertyMap.get("ZonalTargets.file");

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

      
      SyntheticPopulation sp = new SyntheticPopulation(pums, ZoneTable);
      
      String zonalStudentsInputFileName = (String) propertyMap.get("UnivStudentsTaz.file");
      String zonalStudentsOutputFileName = (String) propertyMap.get("UnivStudentsTazOutput.file");

      sp.runSynPop(OutPutHH, ZonalTargetHH, zonalStudentsInputFileName, zonalStudentsOutputFileName);

  }
  
  public static void main(String [] args){

    SyntheticPopulationTester sptester=new SyntheticPopulationTester();
    sptester.runTest();
    
  }
  
}