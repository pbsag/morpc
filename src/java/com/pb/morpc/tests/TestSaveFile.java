package com.pb.morpc.tests;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import org.apache.log4j.Logger;

public class TestSaveFile {

	static Logger logger = Logger.getLogger("com.pb.morpc.models");

	static long runningTime = 0;
	static long markTime = 0;
	
	public static void testPerformance() {
    
		markTime = System.currentTimeMillis();
    
        String fileName = "c:\\tmp\\savefiletest\\M5678.csv";
    
        logger.info("Started reading " + fileName);
    
		TableDataSet table = null;
		TableDataSet table1 = null;
		TableDataSet table2 = null;
    
        try {
			logger.info("reading file 1st time");
			showMemory ();
            
            CSVFileReader reader = new CSVFileReader();

			table = reader.readFile(new File(fileName));
			logger.info("reading file 2nd time");
			showMemory ();

            table1 = reader.readFile(new File(fileName));
			logger.info("reading file 3rd time");
			showMemory ();
            
			table2 = reader.readFile(new File(fileName));
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
    
    
		logger.info("writing file");
		showMemory ();
        try {
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(table, new File("c:\\tmp\\savefiletest\\M5678_new.csv"), new DecimalFormat("#.000000"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    
        logger.info("Finished writing new file");
		showMemory ();
    }
    
    
	public static void showMemory () {

		runningTime = System.currentTimeMillis() - markTime;
		logger.info ("total running minutes = " + (float)((runningTime/1000.0)/60.0));
		logger.info ("totalMemory()=" + Runtime.getRuntime().totalMemory() + " bytes.");
		logger.info ("freeMemory()=" + Runtime.getRuntime().freeMemory() + " bytes.");

	}

	
	public static void main(String[] args) {
    
		TestSaveFile.testPerformance();
       
    }

}