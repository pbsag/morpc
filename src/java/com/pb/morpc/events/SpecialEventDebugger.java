/*
 * Created on Sep 29, 2004
 *
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

/**
 * @author SunW
 *
 */
public class SpecialEventDebugger {
	
	protected SummitFileReader reader;;
	protected Logger logger=Logger.getLogger("com.pb.miami.events");
	protected CSVFileWriter writer=null;
	protected TableDataSet table=null;
	protected SpecialEventSkimReader [] skimReader;
	protected float [][] data;
	protected double beta;

	protected String summitFile;
		
	public SpecialEventDebugger(String summitFile){
		this.summitFile=summitFile;
		beta=-0.0212;
	}
	
	public void doWork(){
		reader=new SummitFileReader(summitFile);
		int NoRecords=1000;
		int NoFields=40;
		int NoSkims=25;

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
		labels[10]="auto_time"; 
		labels[11]="auto_dist"; 
		labels[12]="auto_toll";  
		labels[13]="wlk_trn_wlk"; 
		labels[14]="wlk_trn_drv"; 
		labels[15]="wlk_trn_w1";  
		labels[16]="wlk_trn_w2";		
		labels[17]="wlk_trn_lbivt";     
		labels[18]="wlk_trn_ebivt";   
		labels[19]="wlk_trn_brtivt";  
		labels[20]="wlk_trn_lrtivt";   
		labels[21]="wlk_trn_crlivt";    
		labels[22]="wlk_trn_xfr";
		labels[23]="wlk_trn_fare";
		labels[24]="drv_trn_wlk"; 
		labels[25]="drv_trn_drv"; 
		labels[26]="drv_trn_w1";  
		labels[27]="drv_trn_w2";		
		labels[28]="drv_trn_lbivt";     
		labels[29]="drv_trn_ebivt";   
		labels[30]="drv_trn_brtivt";  
		labels[31]="drv_trn_lrtivt";    
		labels[32]="drv_trn_crlivt";    
		labels[33]="drv_trn_xfr";
		labels[34]="drv_trn_fare";
		labels[35]="wlk_trn_TotIvt";	
		labels[36]="drv_trn_TotIvt";
		labels[37]="AutoUtil";   
		labels[38]="WlkTranUtil";	
		labels[39]="DrvTranUtil";
		
		skimReader=new SpecialEventSkimReader[NoSkims];
		//auto_time
		skimReader[0]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\hwymd1.binary");
		//auto_dist
		skimReader[1]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\hwymd2.binary");
		//auto_toll
		skimReader[2]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\hwymd3.binary");
		//wlk-trn-wlk
		skimReader[3]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd1.binary");
		//wlk-trn-drv
		skimReader[4]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd2.binary");
		//wlk-trn-w1
		skimReader[5]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd3.binary");
		//wlk-trn-w2
		skimReader[6]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd4.binary");
		//wlk-trn-lbivt
		skimReader[7]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd5.binary");
		//wlk-trn-ebivt
		skimReader[8]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd6.binary");		
		//wlk-trn-brtivt
		skimReader[9]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd7.binary");
		//wlk-trn-lrtivt
		skimReader[10]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd8.binary");
		//wlk-trn-crlivt
		skimReader[11]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd9.binary");
		//wlk-trn-xfr
		skimReader[12]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd10.binary");
		//wlk-trn-fare
		skimReader[13]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestwkmd11.binary");
		//drv-trn-wlk
		skimReader[14]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd1.binary");
		//drv-trn-drv
		skimReader[15]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd2.binary");
		//drv-trn-w1
		skimReader[16]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd3.binary");
		//drv-trn-w2
		skimReader[17]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd4.binary");
		//drv-trn-lbivt
		skimReader[18]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd5.binary");
		//drv-trn-ebivt
		skimReader[19]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd6.binary");
		//drv-trn-brtivt
		skimReader[20]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd7.binary");
		//drv-trn-lrtivt
		skimReader[21]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd8.binary");
		//drv-trn-crlivt
		skimReader[22]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd9.binary");
		//drv-trn-xfr
		skimReader[23]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd10.binary");
		//drv-trn-fare
		skimReader[24]=new SpecialEventSkimReader("C:\\morpcevent\\skims\\binary\\baseline\\bestdrmd11.binary");
	
		//initialize labels
		/********************for MIAMI special event**************
		labels[0]="PTAZ";
		labels[1]="ATAZ";
		labels[2]="AutoExp";
		labels[3]="Trips";
		labels[4]="WlkTrnShare";
		labels[5]="WlkTrnAvail";
		labels[6]="DrvTrnOnlyShare";
		labels[7]="DrvTrnOnlyAvail";
		labels[8]="WlkTrnTrips";
		labels[9]="DrvTrnTrips";
		labels[10]="hwy_dist";
		labels[11]="hwy_ivt";
		labels[12]="wlk_bus_wlk";
		labels[13]="wlk_bus_ivt";
		labels[14]="wlk_bus_xfr";
		labels[15]="wlk_bus_w1";
		labels[16]="wlk_bus_w2";
		labels[17]="wlk_pre_wlk";
		labels[18]="wlk_pre_busivt";		
		labels[19]="wlk_pre_ivt";
		labels[20]="wlk_pre_xfr";
		labels[21]="wlk_pre_w1";
		labels[22]="wlk_pre_w2";
		labels[23]="drv_pre_wlk";
		labels[24]="drv_pre_auto";
		labels[25]="drv_pre_busivt";
		labels[26]="drv_pre_ivt";
		labels[27]="drv_pre_xfr";
		labels[28]="drv_pre_w1";
		labels[29]="drv_pre_w2";
		labels[30]="AutoUtil";   
		labels[31]="WlkBusUtil";	
		labels[32]="WlkTrnUtil";
		labels[33]="DrvTranUtil";
		
		skimReader=new SpecialEventSkimReader[NoSkims];
		
		//initialze input skims
		//hwy_dist, 10
		skimReader[0]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\RHSKIM2.binary");
		//hwy_ivt 	11
		skimReader[1]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\RHSKIM3.binary");
		//wlk_bus_wlk 12
		skimReader[2]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM11.binary");
		//wlk_bus_ivt 13
		skimReader[3]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM13.binary");
		//wlk_bus_xfr 14
		skimReader[4]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM15.binary");	
		//wlk_bus_w1 15
		skimReader[5]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM16.binary");
		//wlk_bus_w2 16
		skimReader[6]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM17.binary");
		//wlk_pre_wlk 17
		skimReader[7]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM21.binary");
		//wlk_pre_busivt 18
		skimReader[8]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM23.binary");
		//wlk_pre_ivt 19
		skimReader[9]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM24.binary");
		//wlk_pre_xfr 20
		skimReader[10]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM25.binary");
		//wlk_pre_w1 21
		skimReader[11]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM26.binary");
		//wlk_pre_w2 22
		skimReader[12]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM27.binary");
		//drv_pre_wlk 23
		skimReader[13]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM31.binary");
		//drv_pre_auto 24
		skimReader[14]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM32.binary");
		//drv_pre_busivt 25
		skimReader[15]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM33.binary");
		//drv_pre_ivt  26
		skimReader[16]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM34.binary");
		//drv_pre_xfr 27
		skimReader[17]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM35.binary");
		//drv_pre_w1 28
		skimReader[18]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM36.binary");
		//drv_pre_w2 29
		skimReader[19]=new SpecialEventSkimReader("C:\\miami\\skims\\binary\\baseline\\non_peak\\TSKIM37.binary");
		*/
		
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
		float [][] record=new float[NoRecords][NoFields];
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
		for(int i=0; i<NoRecords; i++){
			
			for(int j=0; j<NoFields; j++){
				record[i][j]=0;
			}
			
			try{
				summitRecord=reader.readRecord();
				record[i][0]=summitRecord.getPtaz();
				currentP=(int)record[i][0]-1;
				record[i][1]=summitRecord.getAtaz();
				currentA=(int)record[i][1]-1;
				
				record[i][2]=summitRecord.getExpAuto();
				record[i][3]=summitRecord.getTrips();
				record[i][4]=summitRecord.getTransitShareOfWalkTransit();
				record[i][5]=summitRecord.getWalkTransitAvailableShare();
				record[i][6]=summitRecord.getTransitShareOfDriveTransitOnly();
				record[i][7]=summitRecord.getDriveTransitOnlyShare();
				record[i][8]=record[i][3]*record[i][4];
				record[i][9]=record[i][3]*record[i][6];
				
				//input skims
				
				for(int j=0; j<NoSkims; j++){
					if(currentA>=0&&currentP>=0){
						data=skimReader[j].getSkim();
						//1st dim currentP, 2nd dimension currentA
						record[i][10+j]=data[currentP][currentA];
					}else{
						record[i][10+j]=0f;
					}
				}
				
				/*
				// * ***********for MIAMI special event******************
				//auto utility
				if(currentA>=0&&currentP>=0){
					record[i][30]=-0.0212f*record[i][11]/100.0f-0.008f*18*record[i][10]/100.0f-0.008f*500f;
				}
					
				if(currentA>=0&&currentP>=0){
					if(record[i][13]>0){
						if(record[i][14]>0)
							record[i][31]=-4.38981f-0.0212f*record[i][13]/100f-0.0437f*record[i][12]/100f-0.0760f*(record[i][15]+record[i][16])/100f-0.008f*125.0f-6.0f;
						else
							record[i][31]=-4.38981f-0.0212f*record[i][13]/100f-0.0437f*record[i][12]/100f-0.0760f*(record[i][15]+record[i][16])/100f-0.008f*125.0f-1.0f;							
					}else{
						record[i][31]=-999f;
					}
				}
					
				if(currentA>=0&&currentP>=0){
					if(record[i][19]>0){
						if(record[i][20]>0)
							record[i][32]=-4.38981f-0.0212f*(record[i][18]+record[i][19])/100f-0.0437f*record[i][17]/100f-0.0760f*(record[i][21]+record[i][22])/100f-0.008f*150f-6.0f;
						else
							record[i][32]=-4.38981f-0.0212f*(record[i][18]+record[i][19])/100f-0.0437f*record[i][17]/100f-0.0760f*(record[i][21]+record[i][22])/100f-0.008f*150f-1.0f;
					}else{
						record[i][32]=-999f;
					}
				}
					
				if(currentA>=0&&currentP>=0){
					if(record[i][26]>0){
						if(record[i][27]>0)
							record[i][33]=-4.38981f-0.0212f*(record[i][24]+record[i][25])/100f-0.0437f*record[i][23]/100f-0.038f*record[i][24]/100f-0.0760f*(record[i][28]+record[i][29])/100f-0.008f*150f-8.0f;
						else
							record[i][33]=-4.38981f-0.0212f*(record[i][24]+record[i][25])/100f-0.0437f*record[i][23]/100f-0.038f*record[i][24]/100f-0.0760f*(record[i][28]+record[i][29])/100f-0.008f*150f-3.0f;
					}else{
						record[i][33]=-999f;
					}
				}
				*/
				
				record[i][35]=record[i][17]+record[i][18]+record[i][19]+record[i][20]+record[i][21];
				record[i][36]=record[i][28]+record[i][29]+record[i][30]+record[i][31]+record[i][32];				
				//auto utility
				if(currentA>=0&&currentP>=0){
					record[i][37]=-0.0212f*record[i][10]-0.008f*18*record[i][11]-0.008f*record[i][12]-0.008f*500f;
				}
				
				if(currentA>=0&&currentP>=0){
					if(record[i][35]>0){
						if(record[i][22]>0)
							record[i][38]=-1.5f-0.0212f*record[i][35]-0.04371f*record[i][13]-0.03848f*record[i][14]-0.07609f*(record[i][15]+record[i][16])-0.008f*record[i][23]-10.4414f;
						else
							record[i][38]=-1.5f-0.0212f*record[i][35]-0.04371f*record[i][13]-0.03848f*record[i][14]-0.07609f*(record[i][15]+record[i][16])-0.008f*record[i][23]-5.2287f;						
					}else{
						record[i][38]=-999f;
					}
				}
				
				if(currentA>=0&&currentP>=0){
					if(record[i][36]>0){
						if(record[i][33]>0)
							record[i][39]=-1.5f-0.0212f*record[i][36]-0.04371f*record[i][24]-0.03848f*record[i][25]-0.07609f*(record[i][26]+record[i][27])-0.008f*record[i][34]-10.4414f;
						else
							record[i][39]=-1.5f-0.0212f*record[i][36]-0.04371f*record[i][24]-0.03848f*record[i][25]-0.07609f*(record[i][26]+record[i][27])-0.008f*record[i][34]-5.2287f;
					}else{
						record[i][39]=-999f;
					}
				}
				
				table=TableDataSet.create(record);				
				table.setColumnLabels(labels);
			}catch(IOException e){
				logger.error("failed reading record:"+"origin-"+ptaz+" dest-"+ataz+" market-1");
			}
		}
		try{
			/*************** this is for MIAMI**********************************
			writer.writeFile(table, new File("c:\\miami\\output\\event\\table_baseline.csv"));
			*/
			writer.writeFile(table, new File("c:\\morpcEvent\\output\\table_baseline.csv"));
		}catch(IOException e){
			logger.error("failed writing out table.");
		}
	}
		
	public static void main(String [] args){
		/*************** this is for MIAMI**********************************
		SpecialEventDebugger test=new SpecialEventDebugger("c:\\miami\\output\\event\\summitfile_baseline_pps.dat");
  		*/

		SpecialEventDebugger test=new SpecialEventDebugger("c:\\morpcEvent\\output\\summitfile_baseline_mdstart.dat");
		test.doWork();
	}
}
