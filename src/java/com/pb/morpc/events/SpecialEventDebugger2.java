/*
 * Created on Dec 27, 2004
 * This class reads Summit Data File contents
 */


package com.pb.morpc.events;

import com.pb.common.summit.SummitFileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import com.pb.common.summit.SummitRecord;
import com.pb.common.summit.SummitHeader;
import com.pb.common.datafile.TableDataSet;
import java.io.File;
import com.pb.common.datafile.CSVFileWriter;
import java.util.Vector;

/**
 * @author SunW
 *
 */
public class SpecialEventDebugger2 {
	
	protected SummitFileReader reader;;
	protected Logger logger=Logger.getLogger("com.pb.morpc.events");
	protected CSVFileWriter writer=null;
	protected TableDataSet table=null;
	protected String summitFile=null;
	
	public SpecialEventDebugger2(String summitFile){
		this.summitFile=summitFile;
	}
	
	public void doWork(){
		
		TableDataSet table=new TableDataSet();
		reader=new SummitFileReader(summitFile);
		int NoRecords=reader.getRecordSize();
		int NoFields=10;
		Vector [] v=new Vector[NoFields];
		
		for(int i=0; i<NoFields; i++){
			v[i]=new Vector();
		}

		table=new TableDataSet();
		String [] labels=new String[NoFields];
		
		labels[0]="PTAZ";
		labels[1]="ATAZ";
		labels[2]="AutoExp";
		labels[3]="Trips";
		labels[4]="WlkTrnShare";
		labels[5]="WlkTrnAvail";
		labels[6]="DrvTrnShare";
		labels[7]="DrvTrnOnlyAvail";
		labels[8]="WlkTrnTrips";
		labels[9]="DrvTrnTrips";
		
		writer=new CSVFileWriter();
		
		try{
	        SummitHeader header = reader.getHeader();
	        
	        int zones = header.getZones();
	        int segments = header.getMarketSegments();
	        float tivt = header.getTransitInVehicleTime();
	        float aivt = header.getAutoInVehicleTime();
	        StringBuffer purp = header.getPurpose();
	        StringBuffer tod = header.getTimeOfDay();
	        StringBuffer title = header.getTitle();
		}catch(IOException e){
			logger.error("failed reading header.");
		}
		
		SummitRecord summitRecord;
		float []currentRecord=new float[NoFields];
		short ataz=-1;
		short ptaz=-1;
		float transitShareOfWalkTransit=-1;
		float transitShareOfDriveTransitOnly=-1;
		float walkTransitAvailableShare=-1;
		float driveTransitOnlyShare=-1;
		float expAuto=-1;
		float trips=-1;
		int currentA=-1;
		int currentP=-1;
		
		for(int i=0; i<3000000; i++){
			
			for(int j=0; j<NoFields; j++){
				currentRecord[j]=0;
			}
			
			try{
				summitRecord=reader.readRecord();
				currentRecord[3]=summitRecord.getTrips();
				
				if(currentRecord[3]>0){
					currentRecord[0]=summitRecord.getPtaz();
					currentRecord[1]=summitRecord.getAtaz();
					currentRecord[2]=summitRecord.getExpAuto();
					currentRecord[4]=summitRecord.getTransitShareOfWalkTransit();
					currentRecord[5]=summitRecord.getWalkTransitAvailableShare();
					currentRecord[6]=summitRecord.getTransitShareOfDriveTransitOnly();
					currentRecord[7]=summitRecord.getDriveTransitOnlyShare();
					currentRecord[8]=currentRecord[3]*currentRecord[4];
					currentRecord[9]=currentRecord[3]*currentRecord[6];
					
					for(int k=0; k<10; k++){
						v[k].add(new Float(currentRecord[k]));
					}
				}
			}catch(IOException e){
				logger.error("failed reading record:"+"origin-"+ptaz+" dest-"+ataz+" market-1");
			}
		}
		
		for(int k=0; k<10; k++){
			table.appendColumn(convertVectorToArray(v[k]),labels[k]);
		}
		
		try{
			writer.writeFile(table, new File("d:\\morpcEvent\\output\\records.csv"));
		}catch(IOException e){
			logger.error("failed writing out table.");
		}
	}
	
	private float [] convertVectorToArray(Vector v){
		float [] result=new float[v.size()];
		for(int i=0; i<result.length; i++){
			result[i]=((Float)v.get(i)).floatValue();
		}
		return result;
	}
		
	public static void main(String [] args){
		SpecialEventDebugger2 db=new SpecialEventDebugger2(args[0]);
		db.doWork();
	}

}
