package com.pb.morpc.models;

/**
 * @author Jim Hicks
 *
 * Model runner class for running destination, time of day, and mode choice for
 * individual tours
 */
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import java.util.HashMap;
import org.apache.log4j.Logger;
import java.io.*;


public class TODDataManager implements java.io.Serializable {

	protected static Logger logger = Logger.getLogger("com.pb.morpc.models");


	protected static TableDataSet todAltsTable;

	protected static int startPosition;
	protected static int endPosition;


	protected HashMap propertyMap;

	public static float[][] logsumTcEAEA;
	public static float[][] logsumTcEAAM;
	public static float[][] logsumTcEAMD;
	public static float[][] logsumTcEAPM;
	public static float[][] logsumTcEANT;
	public static float[][] logsumTcAMAM;
	public static float[][] logsumTcAMMD;
	public static float[][] logsumTcAMPM;
	public static float[][] logsumTcAMNT;
	public static float[][] logsumTcMDMD;
	public static float[][] logsumTcMDPM;
	public static float[][] logsumTcMDNT;
	public static float[][] logsumTcPMPM;
	public static float[][] logsumTcPMNT;
	public static float[][] logsumTcNTNT;
	
	
    public TODDataManager ( HashMap propertyMap ) {

		this.propertyMap = propertyMap;


		// build the TOD data table from the TOD choice alternatives file
		String todFile = (String)propertyMap.get( "TODAlternatives.file" );

		try {
            CSVFileReader reader = new CSVFileReader();
			reader.setDelimSet( " ,\t\n\r\f\"");
            todAltsTable = reader.readFile(new File(todFile));
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}




		startPosition = todAltsTable.getColumnPosition( "start" );
		if (startPosition <= 0) {
			logger.fatal( "start was not a field in the tod choice alternatives TableDataSet.");
			System.exit(1);
		}

		endPosition = todAltsTable.getColumnPosition( "end" );
		if (endPosition <= 0) {
			logger.fatal( "end was not a field in the tod choice alternatives TableDataSet.");
			System.exit(1);
		}



		int numTcAlternatives = todAltsTable.getRowCount();

		logsumTcEAEA = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcEAAM = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcEAMD = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcEAPM = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcEANT = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcAMAM = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcAMMD = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcAMPM = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcAMNT = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcMDMD = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcMDPM = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcMDNT = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcPMPM = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcPMNT = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];
		logsumTcNTNT = new float[ZonalDataManager.MAX_DISTRIBUTED_PROCESSORES][numTcAlternatives+1];

    }



	public static int getTodPeriod (int hour) {
		int timePeriod=0;

		if ( hour <= 6 )
			timePeriod = 1;
		else if ( hour <= 9 )
			timePeriod = 2;
		else if ( hour <= 12 )
			timePeriod = 3;
		else if ( hour <= 15 )
			timePeriod = 4;
		else if ( hour <= 18 )
			timePeriod = 5;
		else if ( hour <= 21 )
			timePeriod = 6;
		else
			timePeriod = 7;

		return timePeriod;
	}


	public static int getTodSkimPeriod (int period) {
		int skimPeriod=0;

		switch (period) {
			// am skims
			case(2):
				skimPeriod=1;
				break;
			// pm skims
			case(5):
				skimPeriod=2;
				break;
			// md skims
			case(3):
			case(4):
				skimPeriod=3;
				break;
			// nt skims
			default:
				skimPeriod=4;
		}

		return skimPeriod;
	}




	public static int getTodStartHour ( int todAlt ) {
		return (int)todAltsTable.getValueAt( todAlt, startPosition );
	}


	public static int getTodEndHour ( int todAlt ) {
		return (int)todAltsTable.getValueAt( todAlt, endPosition );
	}


	public static int getTodStartPeriod ( int todAlt ) {
		return getTodPeriod ( getTodStartHour ( todAlt ) );
	}


	public static int getTodEndPeriod ( int todAlt ) {
		return getTodPeriod ( getTodEndHour ( todAlt ) );
	}


	public static int getTodStartSkimPeriod ( int todAlt ) {
		return getTodSkimPeriod ( getTodStartPeriod ( todAlt ) );
	}


	public static int getTodEndSkimPeriod ( int todAlt ) {
		return getTodSkimPeriod ( getTodEndPeriod ( todAlt ) );
	}


	/**
	 * copy the static members of this class into a HashMap.  The non-static HashMap
	 * will be serialized in a message and sent to client VMs in a distributed application.
	 * Static data itself is not serialized with the object, so the extra step is necessary.
	 * 
	 * @return
	 */
	public HashMap createStaticDataMap () {
	    
		HashMap staticDataMap = new HashMap();

		staticDataMap.put ( "todAltsTable", todAltsTable );
		staticDataMap.put ( "startPosition", Integer.toString(startPosition) );
		staticDataMap.put ( "endPosition", Integer.toString(endPosition) );
		staticDataMap.put ( "logsumTcEAEA", logsumTcEAEA );
		staticDataMap.put ( "logsumTcEAAM", logsumTcEAAM );
		staticDataMap.put ( "logsumTcEAMD", logsumTcEAMD );
		staticDataMap.put ( "logsumTcEAPM", logsumTcEAPM );
		staticDataMap.put ( "logsumTcEANT", logsumTcEANT );
		staticDataMap.put ( "logsumTcAMAM", logsumTcAMAM );
		staticDataMap.put ( "logsumTcAMMD", logsumTcAMMD );
		staticDataMap.put ( "logsumTcAMPM", logsumTcAMPM );
		staticDataMap.put ( "logsumTcAMNT", logsumTcAMNT );
		staticDataMap.put ( "logsumTcMDMD", logsumTcMDMD );
		staticDataMap.put ( "logsumTcMDPM", logsumTcMDPM );
		staticDataMap.put ( "logsumTcMDNT", logsumTcMDNT );
		staticDataMap.put ( "logsumTcPMPM", logsumTcPMPM );
		staticDataMap.put ( "logsumTcPMNT", logsumTcPMNT );
		staticDataMap.put ( "logsumTcNTNT", logsumTcNTNT );

	    
		return staticDataMap;
	    
	}

	
	
	/**
	 * set values in the static members of this class from data in a HashMap.
	 * 
	 * @return
	 */
	public void setStaticData ( HashMap staticDataMap ) {
	    
		todAltsTable     = (TableDataSet)staticDataMap.get ( "todAltsTable" );
		startPosition    = Integer.parseInt((String)staticDataMap.get ( "startPosition" ));
		endPosition      = Integer.parseInt((String)staticDataMap.get ( "endPosition" ));
		logsumTcEAEA     = (float[][])staticDataMap.get ( "logsumTcEAEA" );
		logsumTcEAAM     = (float[][])staticDataMap.get ( "logsumTcEAAM" );
		logsumTcEAMD     = (float[][])staticDataMap.get ( "logsumTcEAMD" );
		logsumTcEAPM     = (float[][])staticDataMap.get ( "logsumTcEAPM" );
		logsumTcEANT     = (float[][])staticDataMap.get ( "logsumTcEANT" );
		logsumTcAMAM     = (float[][])staticDataMap.get ( "logsumTcAMAM" );
		logsumTcAMMD     = (float[][])staticDataMap.get ( "logsumTcAMMD" );
		logsumTcAMPM     = (float[][])staticDataMap.get ( "logsumTcAMPM" );
		logsumTcAMNT     = (float[][])staticDataMap.get ( "logsumTcAMNT" );
		logsumTcMDMD     = (float[][])staticDataMap.get ( "logsumTcMDMD" );
		logsumTcMDPM     = (float[][])staticDataMap.get ( "logsumTcMDPM" );
		logsumTcMDNT     = (float[][])staticDataMap.get ( "logsumTcMDNT" );
		logsumTcPMPM     = (float[][])staticDataMap.get ( "logsumTcPMPM" );
		logsumTcPMNT     = (float[][])staticDataMap.get ( "logsumTcPMNT" );
		logsumTcNTNT     = (float[][])staticDataMap.get ( "logsumTcNTNT" );

	}
	
	
}
