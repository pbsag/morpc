package com.pb.morpc.models;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.UtilityExpressionCalculator;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.morpc.structures.Household;
import com.pb.morpc.structures.MessageWindow;
import com.pb.morpc.structures.OutputDescription;
import com.pb.morpc.structures.PatternType;
import com.pb.morpc.synpop.SyntheticPopulation;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class Model27 {

    static Logger logger = Logger.getLogger("com.pb.morpc.models");

	static final int MODEL_27_SHEET = 1;
	static final int DATA_27_SHEET = 0;

	HashMap propertyMap;
	boolean useMessageWindow = false;
	MessageWindow mw;
	
	private IndexValues index = new IndexValues();
	private int[] sample;
	
    public Model27 (HashMap propertyMap) {
        
        this.propertyMap = propertyMap;

		// get the indicator for whether to use the message window or not
		// from the properties file.
		String useMessageWindowString = (String)propertyMap.get( "MessageWindow");
		if (useMessageWindowString != null) {
			if (useMessageWindowString.equalsIgnoreCase("true")) {
				this.useMessageWindow = true;
				this.mw = new MessageWindow ( "MORPC Daily Activity Pattern (M27) Run Time Information" );
			}
		}
    }


    private TableDataSet createPersonDataTable(TableDataSet hhTable) {

		boolean debug = false;

	   int numOfHouseholds = hhTable.getRowCount();
	   ArrayList tableHeadings=new ArrayList();

	   tableHeadings.add("hh_id");
	   tableHeadings.add("person_id");
	   tableHeadings.add("person_type");
	   tableHeadings.add("M2");

	   int workers_f_col=hhTable.getColumnPosition(SyntheticPopulation.WORKERS_F_FIELD);
	   int workers_p_col=hhTable.getColumnPosition(SyntheticPopulation.WORKERS_P_FIELD);
	   int students_col=hhTable.getColumnPosition(SyntheticPopulation.STUDENTS_FIELD);
	   int nonworkers_col=hhTable.getColumnPosition(SyntheticPopulation.NONWORKERS_FIELD);
	   int preschool_col=hhTable.getColumnPosition(SyntheticPopulation.PRESCHOOL_FIELD);
	   int schoolpred_col=hhTable.getColumnPosition(SyntheticPopulation.SCHOOLPRED_FIELD);
	   int schooldriv_col=hhTable.getColumnPosition(SyntheticPopulation.SCHOOLDRIV_FIELD);


	   int school1_21_col=hhTable.getColumnPosition("school_21");
	   int non_mand_21_col=hhTable.getColumnPosition("non_mand_21");
	   int home_21_col=hhTable.getColumnPosition("home_21");

	   int work1_22_col=hhTable.getColumnPosition("work_1_22");
	   int school1_22_col=hhTable.getColumnPosition("school_1_22");
	   int school2_22_col=hhTable.getColumnPosition("school_2_22");
	   int schoolWork_22_col=hhTable.getColumnPosition("school_work_22");
	   int nonMand_22_col=hhTable.getColumnPosition("non_mand_22");
	   int home_22_col=hhTable.getColumnPosition("home_22");

	   int work1_23_col=hhTable.getColumnPosition("work_1_23");
	   int school1_23_col=hhTable.getColumnPosition("school_1_23");
	   int school2_23_col=hhTable.getColumnPosition("school_2_23");
	   int schoolWork_23_col=hhTable.getColumnPosition("school_work_23");
	   int nonMand_23_col=hhTable.getColumnPosition("non_mand_23");
	   int home_23_col=hhTable.getColumnPosition("home_23");

	   int work1_24_col=hhTable.getColumnPosition("work_1_24");
	   int work2_24_col=hhTable.getColumnPosition("work_2_24");
	   int univ1_24_col=hhTable.getColumnPosition("univ_1_24");
	   int univ2_24_col=hhTable.getColumnPosition("univ_2_24");
	   int univWork_24_col=hhTable.getColumnPosition("univ_work_24");
	   int nonMand_24_col=hhTable.getColumnPosition("non_mand_24");
	   int home_24_col=hhTable.getColumnPosition("home_24");

	   int work1_25_col=hhTable.getColumnPosition("work_1_25");
	   int work2_25_col=hhTable.getColumnPosition("work_2_25");
	   int workUniv_25_col=hhTable.getColumnPosition("work_univ_25");
	   int univ1_25_col=hhTable.getColumnPosition("univ_1_25");
	   int nonMand_25_col=hhTable.getColumnPosition("non_mand_25");
	   int home_25_col=hhTable.getColumnPosition("home_25");

	   int work1_26_col=hhTable.getColumnPosition("work_1_26");
	   int work2_26_col=hhTable.getColumnPosition("work_2_26");
	   int workUniv_26_col=hhTable.getColumnPosition("work_univ_26");
	   int univ1_26_col=hhTable.getColumnPosition("univ_1_26");
	   int nonMand_26_col=hhTable.getColumnPosition("non_mand_26");
	   int home_26_col=hhTable.getColumnPosition("home_26");

	   int work1_27_col=hhTable.getColumnPosition("work_1_27");
	   int work2_27_col=hhTable.getColumnPosition("work_2_27");
	   int univ1_27_col=hhTable.getColumnPosition("univ_1_27");
	   int nonMand_27_col=hhTable.getColumnPosition("non_mand_27");
	   int home_27_col=hhTable.getColumnPosition("home_27");



	   float numOfWorkers_f = hhTable.getColumnTotal(workers_f_col);
	   float numOfWorkers_p = hhTable.getColumnTotal(workers_p_col);
	   float numOfStudents = hhTable.getColumnTotal(students_col);
	   float numOfNonworkers = hhTable.getColumnTotal(nonworkers_col);
	   float numOfPreschoolers = hhTable.getColumnTotal(preschool_col);
	   float numOfSchoolpredrivers = hhTable.getColumnTotal(schoolpred_col);
	   float numOfSchooldrivers = hhTable.getColumnTotal(schooldriv_col);

	   int numOfPersons = (int)(numOfWorkers_f + numOfWorkers_p + numOfStudents + numOfNonworkers
	                        + numOfPreschoolers + numOfSchoolpredrivers + numOfSchooldrivers);



	   float[] personTypeSum = new float[7];
	   float[][] tableData = new float[(int)numOfPersons][11];
	   int rowPointer=0;

		PrintWriter out21=null, out22=null, out23=null, out24=null, out25=null, out26=null, out27=null;

		try {

			if (debug) {
				out21 = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("Model21_Persons.csv")));
				out22 = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("Model22_Persons.csv")));
				out23 = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("Model23_Persons.csv")));
				out24 = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("Model24_Persons.csv")));
				out25 = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("Model25_Persons.csv")));
				out26 = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("Model26_Persons.csv")));
				out27 = new PrintWriter (
					new BufferedWriter (
						new FileWriter ("Model27_Persons.csv")));
			}


       for(int i=1; i<=numOfHouseholds; i++) {

           int numSchool1_21 = (int)hhTable.getValueAt(i,school1_21_col);
           int numNonMand_21 = (int)hhTable.getValueAt(i,non_mand_21_col);
           int numHome_21 = (int)hhTable.getValueAt(i,home_21_col);

           int numWork1_22 = (int)hhTable.getValueAt(i,work1_22_col);
           int numSchool1_22 = (int)hhTable.getValueAt(i,school1_22_col);
           int numSchool2_22 = (int)hhTable.getValueAt(i,school2_22_col);
           int numSchoolWork_22 = (int)hhTable.getValueAt(i,schoolWork_22_col);
           int numNonMand_22 = (int)hhTable.getValueAt(i,nonMand_22_col);
           int numHome_22 = (int)hhTable.getValueAt(i,home_22_col);

           int numWork1_23 = (int)hhTable.getValueAt(i,work1_23_col);
           int numSchool1_23 = (int)hhTable.getValueAt(i,school1_23_col);
           int numSchool2_23 = (int)hhTable.getValueAt(i,school2_23_col);
           int numSchoolWork_23 = (int)hhTable.getValueAt(i,schoolWork_23_col);
           int numNonMand_23 = (int)hhTable.getValueAt(i,nonMand_23_col);
           int numHome_23 = (int)hhTable.getValueAt(i,home_23_col);

           int numWork1_24 = (int)hhTable.getValueAt(i,work1_24_col);
           int numWork2_24 = (int)hhTable.getValueAt(i,work2_24_col);
           int numUniv1_24 = (int)hhTable.getValueAt(i,univ1_24_col);
           int numUniv2_24 = (int)hhTable.getValueAt(i,univ2_24_col);
           int numUnivWork_24 = (int)hhTable.getValueAt(i,univWork_24_col);
           int numNonMand_24 = (int)hhTable.getValueAt(i,nonMand_24_col);
           int numHome_24 = (int)hhTable.getValueAt(i,home_24_col);

           int numWork1_25 = (int)hhTable.getValueAt(i,work1_25_col);
           int numWork2_25 = (int)hhTable.getValueAt(i,work2_25_col);
           int numWorkUniv_25 = (int)hhTable.getValueAt(i,workUniv_25_col);
           int numUniv1_25 = (int)hhTable.getValueAt(i,univ1_25_col);
           int numNonMand_25 = (int)hhTable.getValueAt(i,nonMand_25_col);
           int numHome_25 = (int)hhTable.getValueAt(i,home_25_col);

           int numWork1_26 = (int)hhTable.getValueAt(i,work1_26_col);
           int numWork2_26 = (int)hhTable.getValueAt(i,work2_26_col);
           int numWorkUniv_26 = (int)hhTable.getValueAt(i,workUniv_26_col);
           int numUniv1_26 = (int)hhTable.getValueAt(i,univ1_26_col);
           int numNonMand_26 = (int)hhTable.getValueAt(i,nonMand_26_col);
           int numHome_26 = (int)hhTable.getValueAt(i,home_26_col);

           int numWork1_27 = (int)hhTable.getValueAt(i,work1_27_col);
           int numWork2_27 = (int)hhTable.getValueAt(i,work2_27_col);
           int numUniv1_27 = (int)hhTable.getValueAt(i,univ1_27_col);
           int numNonMand_27 = (int)hhTable.getValueAt(i,nonMand_27_col);
           int numHome_27 = (int)hhTable.getValueAt(i,home_27_col);

           int numOfWorkers_fInHH=(int)hhTable.getValueAt(i,workers_f_col);
           int numOfWorkers_pInHH=(int)hhTable.getValueAt(i,workers_p_col);
           int numOfStudentsInHH=(int)hhTable.getValueAt(i,students_col);
           int numOfNonworkersInHH=(int)hhTable.getValueAt(i,nonworkers_col);
           int numOfPreschoolInHH=(int)hhTable.getValueAt(i,preschool_col);
           int numOfSchoolpredInHH=(int)hhTable.getValueAt(i,schoolpred_col);
           int numOfSchooldrivInHH=(int)hhTable.getValueAt(i,schooldriv_col);
           float hhID=hhTable.getValueAt(i,hhTable.getColumnPosition(SyntheticPopulation.HHID_FIELD));
           float personID=1;


           if(numOfWorkers_fInHH>0){
               float personType=1;
               personTypeSum[(int)personType-1]+=numOfWorkers_fInHH;
               if(numWork1_25 > 0){
                   for(int p=1; p<=numWork1_25; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out25.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_25 + "," + numWork2_25 + "," + numWorkUniv_25 + "," + numUniv1_25 + "," + numNonMand_25 + "," + numHome_25 + "," + numOfWorkers_fInHH);
                   }
               }
               if(numWork2_25 > 0){
                   for(int p=1; p<=numWork2_25; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_2};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out25.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_25 + "," + numWork2_25 + "," + numWorkUniv_25 + "," + numUniv1_25 + "," + numNonMand_25 + "," + numHome_25 + "," + numOfWorkers_fInHH);
                   }
               }
               if(numWorkUniv_25 > 0){
                   for(int p=1; p<=numWorkUniv_25; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_UNIV};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out25.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_25 + "," + numWork2_25 + "," + numWorkUniv_25 + "," + numUniv1_25 + "," + numNonMand_25 + "," + numHome_25 + "," + numOfWorkers_fInHH);
                   }
               }
               if(numUniv1_25 > 0){
                   for(int p=1; p<=numUniv1_25; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.UNIV_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out25.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_25 + "," + numWork2_25 + "," + numWorkUniv_25 + "," + numUniv1_25 + "," + numNonMand_25 + "," + numHome_25 + "," + numOfWorkers_fInHH);
                   }
               }
               if(numNonMand_25 > 0){
                   for(int p=1; p<=numNonMand_25; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.NON_MAND};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out25.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_25 + "," + numWork2_25 + "," + numWorkUniv_25 + "," + numUniv1_25 + "," + numNonMand_25 + "," + numHome_25 + "," + numOfWorkers_fInHH);
                   }
               }
               if(numHome_25 > 0){
                   for(int p=1; p<=numHome_25; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.HOME};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out25.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_25 + "," + numWork2_25 + "," + numWorkUniv_25 + "," + numUniv1_25 + "," + numNonMand_25 + "," + numHome_25 + "," + numOfWorkers_fInHH);
                   }
               }
	           if (debug) out25.flush ();
           }

           if(numOfWorkers_pInHH>0){
               float personType=2;
               personTypeSum[(int)personType-1]+=numOfWorkers_pInHH;
               if(numWork1_26 > 0){
                   for(int p=1; p<=numWork1_26; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out26.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_26 + "," + numWork2_26 + "," + numWorkUniv_26 + "," + numUniv1_26 + "," + numNonMand_26 + "," + numHome_26 + "," + numOfWorkers_pInHH);
                   }
               }
               if(numWork2_26 > 0){
                   for(int p=1; p<=numWork2_26; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_2};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out26.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_26 + "," + numWork2_26 + "," + numWorkUniv_26 + "," + numUniv1_26 + "," + numNonMand_26 + "," + numHome_26 + "," + numOfWorkers_pInHH);
                   }
               }
               if(numWorkUniv_26 > 0){
                   for(int p=1; p<=numWorkUniv_26; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_UNIV};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out26.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_26 + "," + numWork2_26 + "," + numWorkUniv_26 + "," + numUniv1_26 + "," + numNonMand_26 + "," + numHome_26 + "," + numOfWorkers_pInHH);
                   }
               }
               if(numUniv1_26 > 0){
                   for(int p=1; p<=numUniv1_26; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.UNIV_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out26.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_26 + "," + numWork2_26 + "," + numWorkUniv_26 + "," + numUniv1_26 + "," + numNonMand_26 + "," + numHome_26 + "," + numOfWorkers_pInHH);
                   }
               }
               if(numNonMand_26 > 0){
                   for(int p=1; p<=numNonMand_26; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.NON_MAND};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out26.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_26 + "," + numWork2_26 + "," + numWorkUniv_26 + "," + numUniv1_26 + "," + numNonMand_26 + "," + numHome_26 + "," + numOfWorkers_pInHH);
                   }
               }
               if(numHome_26 > 0){
                   for(int p=1; p<=numHome_26; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.HOME};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out26.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_26 + "," + numWork2_26 + "," + numWorkUniv_26 + "," + numUniv1_26 + "," + numNonMand_26 + "," + numHome_26 + "," + numOfWorkers_pInHH);
                   }
               }
	           if (debug) out26.flush ();
           }

           if(numOfStudentsInHH>0){
               float personType=3;
               personTypeSum[(int)personType-1]+=numOfStudentsInHH;
               if(numWork1_24 > 0){
                   for(int p=1; p<=numWork1_24; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out24.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_24 + "," + numWork2_24 + "," + numUniv1_24 + "," + numUniv2_24 + "," + numUnivWork_24 + "," + numNonMand_24 + "," + numHome_24 + "," + numOfStudentsInHH);
                   }
               }
               if(numWork2_24 > 0){
                   for(int p=1; p<=numWork2_24; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_2};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out24.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_24 + "," + numWork2_24 + "," + numUniv1_24 + "," + numUniv2_24 + "," + numUnivWork_24 + "," + numNonMand_24 + "," + numHome_24 + "," + numOfStudentsInHH);
                   }
               }
               if(numUniv1_24 > 0){
                   for(int p=1; p<=numUniv1_24; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.UNIV_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out24.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_24 + "," + numWork2_24 + "," + numUniv1_24 + "," + numUniv2_24 + "," + numUnivWork_24 + "," + numNonMand_24 + "," + numHome_24 + "," + numOfStudentsInHH);
                   }
               }
               if(numUniv2_24 > 0){
                   for(int p=1; p<=numUniv2_24; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.UNIV_2};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out24.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_24 + "," + numWork2_24 + "," + numUniv1_24 + "," + numUniv2_24 + "," + numUnivWork_24 + "," + numNonMand_24 + "," + numHome_24 + "," + numOfStudentsInHH);
                   }
               }
               if(numUnivWork_24 > 0){
                   for(int p=1; p<=numUnivWork_24; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.UNIV_WORK};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out24.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_24 + "," + numWork2_24 + "," + numUniv1_24 + "," + numUniv2_24 + "," + numUnivWork_24 + "," + numNonMand_24 + "," + numHome_24 + "," + numOfStudentsInHH);
                   }
               }
               if(numNonMand_24 > 0){
                   for(int p=1; p<=numNonMand_24; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.NON_MAND};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out24.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_24 + "," + numWork2_24 + "," + numUniv1_24 + "," + numUniv2_24 + "," + numUnivWork_24 + "," + numNonMand_24 + "," + numHome_24 + "," + numOfStudentsInHH);
                   }
               }
               if(numHome_24 > 0){
                   for(int p=1; p<=numHome_24; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.HOME};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out24.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_24 + "," + numWork2_24 + "," + numUniv1_24 + "," + numUniv2_24 + "," + numUnivWork_24 + "," + numNonMand_24 + "," + numHome_24 + "," + numOfStudentsInHH);
                   }
               }
		       if (debug) out24.flush ();
           }

           if(numOfNonworkersInHH>0){
               float personType=4;
               personTypeSum[(int)personType-1]+=numOfNonworkersInHH;
               if(numWork1_27 > 0){
                   for(int p=1; p<=numWork1_27; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out27.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_27 + "," + numWork2_27 + "," + numUniv1_27 + "," + numNonMand_27 + "," + numHome_27 + "," + numOfNonworkersInHH);
                   }
               }
               if(numWork2_27 > 0){
                   for(int p=1; p<=numWork2_27; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_2};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out27.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_27 + "," + numWork2_27 + "," + numUniv1_27 + "," + numNonMand_27 + "," + numHome_27 + "," + numOfNonworkersInHH);
                   }
               }
               if(numUniv1_27 > 0){
                   for(int p=1; p<=numUniv1_27; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.UNIV_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out27.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_27 + "," + numWork2_27 + "," + numUniv1_27 + "," + numNonMand_27 + "," + numHome_27 + "," + numOfNonworkersInHH);
                   }
               }
               if(numNonMand_27 > 0){
                   for(int p=1; p<=numNonMand_27; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.NON_MAND};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out27.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_27 + "," + numWork2_27 + "," + numUniv1_27 + "," + numNonMand_27 + "," + numHome_27 + "," + numOfNonworkersInHH);
                   }
               }
               if(numHome_27 > 0){
                   for(int p=1; p<=numHome_27; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.HOME};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out27.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_27 + "," + numWork2_27 + "," + numUniv1_27 + "," + numNonMand_27 + "," + numHome_27 + "," + numOfNonworkersInHH);
                   }
               }
               if (debug) out27.flush ();
           }

           if(numOfPreschoolInHH>0){
               float personType=5;
               personTypeSum[(int)personType-1]+=numOfPreschoolInHH;
               if(numSchool1_21 > 0){
                   for(int p=1; p<=numSchool1_21; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.SCHOOL_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out21.println (hhID + "," + personID + "," + rowPointer + "," + numSchool1_21 + "," + numNonMand_21 + "," + numHome_21 + "," + numOfPreschoolInHH);
                   }
               }
               if(numNonMand_21 > 0){
                   for(int p=1; p<=numNonMand_21; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.NON_MAND};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out21.println (hhID + "," + personID + "," + rowPointer + "," + numSchool1_21 + "," + numNonMand_21 + "," + numHome_21 + "," + numOfPreschoolInHH);
                   }
               }
               if(numHome_21 > 0){
                   for(int p=1; p<=numHome_21; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.HOME};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out21.println (hhID + "," + personID + "," + rowPointer + "," + numSchool1_21 + "," + numNonMand_21 + "," + numHome_21 + "," + numOfPreschoolInHH);
                   }
               }
               if (debug) out21.flush ();
           }

           if(numOfSchoolpredInHH>0){
               float personType=6;
               personTypeSum[(int)personType-1]+=numOfSchoolpredInHH;
               if(numWork1_22 > 0){
                   for(int p=1; p<=numWork1_22; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out22.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_22 + "," + numSchool1_22 + "," + numSchool2_22 + "," + numSchoolWork_22 + "," + numNonMand_22 + "," + numHome_22 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numSchool1_22 > 0){
                   for(int p=1; p<=numSchool1_22; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.SCHOOL_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out22.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_22 + "," + numSchool1_22 + "," + numSchool2_22 + "," + numSchoolWork_22 + "," + numNonMand_22 + "," + numHome_22 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numSchool2_22 > 0){
                   for(int p=1; p<=numSchool2_22; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.SCHOOL_2};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out22.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_22 + "," + numSchool1_22 + "," + numSchool2_22 + "," + numSchoolWork_22 + "," + numNonMand_22 + "," + numHome_22 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numSchoolWork_22 > 0){
                   for(int p=1; p<=numSchoolWork_22; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.SCHOOL_WORK};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out22.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_22 + "," + numSchool1_22 + "," + numSchool2_22 + "," + numSchoolWork_22 + "," + numNonMand_22 + "," + numHome_22 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numNonMand_22 > 0){
                   for(int p=1; p<=numNonMand_22; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.NON_MAND};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out22.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_22 + "," + numSchool1_22 + "," + numSchool2_22 + "," + numSchoolWork_22 + "," + numNonMand_22 + "," + numHome_22 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numHome_22 > 0){
                   for(int p=1; p<=numHome_22; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.HOME};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out22.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_22 + "," + numSchool1_22 + "," + numSchool2_22 + "," + numSchoolWork_22 + "," + numNonMand_22 + "," + numHome_22 + "," + numOfSchoolpredInHH);
                   }
               }
               if (debug) out22.flush ();
           }

           if(numOfSchooldrivInHH>0){
               float personType=7;
               personTypeSum[(int)personType-1]+=numOfSchooldrivInHH;
               if(numWork1_23 > 0){
                   for(int p=1; p<=numWork1_23; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.WORK_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out23.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_23 + "," + numSchool1_23 + "," + numSchool2_23 + "," + numSchoolWork_23 + "," + numNonMand_23 + "," + numHome_23 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numSchool1_23 > 0){
                   for(int p=1; p<=numSchool1_23; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.SCHOOL_1};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out23.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_23 + "," + numSchool1_23 + "," + numSchool2_23 + "," + numSchoolWork_23 + "," + numNonMand_23 + "," + numHome_23 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numSchool2_23 > 0){
                   for(int p=1; p<=numSchool2_23; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.SCHOOL_2};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out23.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_23 + "," + numSchool1_23 + "," + numSchool2_23 + "," + numSchoolWork_23 + "," + numNonMand_23 + "," + numHome_23 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numSchoolWork_23 > 0){
                   for(int p=1; p<=numSchoolWork_23; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.SCHOOL_WORK};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out23.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_23 + "," + numSchool1_23 + "," + numSchool2_23 + "," + numSchoolWork_23 + "," + numNonMand_23 + "," + numHome_23 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numNonMand_23 > 0){
                   for(int p=1; p<=numNonMand_23; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.NON_MAND};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out23.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_23 + "," + numSchool1_23 + "," + numSchool2_23 + "," + numSchoolWork_23 + "," + numNonMand_23 + "," + numHome_23 + "," + numOfSchoolpredInHH);
                   }
               }
               if(numHome_23 > 0){
                   for(int p=1; p<=numHome_23; p++){
                       float[] rowOfData={hhID,personID,personType,PatternType.HOME};
                       tableData[rowPointer]=rowOfData;
                       rowPointer++;
                       personID++;
		               if (debug) out23.println (hhID + "," + personID + "," + rowPointer + "," + numWork1_23 + "," + numSchool1_23 + "," + numSchool2_23 + "," + numSchoolWork_23 + "," + numNonMand_23 + "," + numHome_23 + "," + numOfSchoolpredInHH);
                   }
               }
               if (debug) out23.flush ();
           }
       }

		} catch (IOException e) {
			logger.error ("File could not be opened, or other IO exception ocurred");
		}


       TableDataSet personTable = TableDataSet.create(tableData,tableHeadings);

       return personTable;
    }



    public void runNonworkerDailyActivityPatternChoice() {

        int hh_id;
        int hh_taz_id;


        //open files
        String controlFile = (String)propertyMap.get( "Model27.controlFile" );
        String outputFile = (String)propertyMap.get( "Model27.outputFile" );
        String personFile = (String)propertyMap.get( "SyntheticPerson.file" );


        logger.debug(controlFile);
        logger.debug(outputFile);


        // create a new UEC to get utilties for this logit model
		UtilityExpressionCalculator uec = new UtilityExpressionCalculator ( new File(controlFile), MODEL_27_SHEET, DATA_27_SHEET, propertyMap, Household.class );
        int numberOfAlternatives=uec.getNumberOfAlternatives();
        String[] alternativeNames = uec.getAlternativeNames();

		sample = new int[uec.getNumberOfAlternatives()+1];
		
        // create and define a new LogitModel object
        LogitModel root= new LogitModel("root", numberOfAlternatives);
        ConcreteAlternative[] alts= new ConcreteAlternative[numberOfAlternatives];

        for(int i=0;i<numberOfAlternatives;i++){
            logger.debug("alternative "+(i+1)+" is "+alternativeNames[i] );
            alts[i]  = new ConcreteAlternative(alternativeNames[i], new Integer(i+1));
            root.addAlternative (alts[i]);
            logger.debug(alternativeNames[i]+" has been added to the root");
        }

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();

        // get the household data table from the UEC control file
        TableDataSet hhTable = uec.getHouseholdData();
        if (hhTable == null) {
            logger.debug ("Could not get householdData TableDataSet from UEC");
            System.exit(1);
        }

        int hh_idPosition = hhTable.getColumnPosition( SyntheticPopulation.HHID_FIELD );
        if (hh_idPosition <= 0) {
            logger.debug (SyntheticPopulation.HHID_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }
        int hh_taz_idPosition = hhTable.getColumnPosition( SyntheticPopulation.HHTAZID_FIELD );
        if (hh_taz_idPosition <= 0) {
            logger.debug (SyntheticPopulation.HHTAZID_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        int nonworkers_idPosition = hhTable.getColumnPosition( SyntheticPopulation.NONWORKERS_FIELD );
        if (nonworkers_idPosition <= 0) {
            logger.debug (SyntheticPopulation.NONWORKERS_FIELD + " was not a field in the householdData TableDataSet.");
            System.exit(1);
        }

        //define vectors which will hold results of linked attributes
        //which need to be appended to hhTable for use in next model
        float[] nonworkersWork1=new float[hhTable.getRowCount()];
        float[] nonworkersWork2=new float[hhTable.getRowCount()];
        float[] nonworkersUniv1=new float[hhTable.getRowCount()];
        float[] nonworkersNonMand=new float[hhTable.getRowCount()];
        float[] nonworkersAtHome=new float[hhTable.getRowCount()];

        // loop over all households and all nonworkers in household
        //to make alternative choice.
        for (int i=0; i < hhTable.getRowCount(); i++) {
			if (useMessageWindow) mw.setMessage1 ("Model27 Choice for hh " + (i+1) + " of " + hhTable.getRowCount() );

            hh_id = (int)hhTable.getValueAt( i+1, hh_idPosition );
            hh_taz_id = (int)hhTable.getValueAt( i+1, hh_taz_idPosition );
            // check for nonworkers in the household
            if ( (int)hhTable.getValueAt( i+1, nonworkers_idPosition ) > 0 ) {

				// get utilities for each alternative for this household
				index.setZoneIndex( hh_taz_id );
				index.setHHIndex( hh_id );
        
				Arrays.fill(sample, 1);
				double[] utilities = uec.solve( index, new Object(), sample );

				//set utility for each alternative
				for(int a=0;a < numberOfAlternatives;a++){
					alts[a].setAvailability( sample[a+1] == 1 );
					if (sample[a+1] == 1)
						alts[a].setAvailability( (utilities[a] > -99.0) );
					alts[a].setUtility(utilities[a]);
				}
				// set availabilities
				root.computeAvailabilities();


				root.getUtility();
				root.calculateProbabilities();


                //loop over number of nonworkers in household
                for (int m=0; m < (int)hhTable.getValueAt( i+1, nonworkers_idPosition ); m++){
                    ConcreteAlternative chosen = (ConcreteAlternative) root.chooseElementalAlternative();
                    String chosenAltName= chosen.getName();
                    if (chosenAltName.equals("Work_1"))
                        ++ nonworkersWork1[i];
                    else if (chosenAltName.equals("Work_2"))
                        ++ nonworkersWork2[i];
                    else if (chosenAltName.equals("Univ_1"))
                        ++ nonworkersUniv1[i];
                    else if (chosenAltName.equals("Non_mand"))
                        ++ nonworkersNonMand[i];
                    else if (chosenAltName.equals("Home"))
                        ++ nonworkersAtHome[i];
                }//next predriver in household
            }//end if
        }//next household
        //append nonworkers at home onto HH data file
        hhTable.appendColumn (nonworkersWork1, "work_1_27");
        hhTable.appendColumn (nonworkersWork2, "work_2_27");
        hhTable.appendColumn (nonworkersUniv1, "univ_1_27");
        hhTable.appendColumn (nonworkersNonMand, "non_mand_27");
        hhTable.appendColumn (nonworkersAtHome, "home_27");
        //write out updated HH file to disk


		if (outputFile != null) {
			if (useMessageWindow) mw.setMessage2 ("Writing results to: " + outputFile );
	        try {
                CSVFileWriter writer = new CSVFileWriter();
                writer.writeFile(hhTable, new File(outputFile), new DecimalFormat("#.000000"));
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	            System.exit(1);
	        }
		}
		

        //create a person data table.
		if (useMessageWindow) mw.setMessage2 ("creating person table");
        TableDataSet personTable = createPersonDataTable(hhTable);
        String[] descriptions = OutputDescription.getDescriptions("person_type");
        TableDataSet.logColumnFreqReport("SynPopP",personTable,personTable.getColumnPosition("person_type"),descriptions);
        descriptions = OutputDescription.getDescriptions("M2");
        TableDataSet.logColumnFreqReport("SynPopP",personTable,personTable.getColumnPosition("M2"),descriptions);


		if (personFile != null) {
			if (useMessageWindow) mw.setMessage2 ("Writing person table to: " + personFile );
			// write updated household table to new output file
			try {
                CSVFileWriter writer = new CSVFileWriter();
                writer.writeFile(personTable, new File(personFile), new DecimalFormat("#.000000"));
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		if (useMessageWindow) mw.setMessage3 ("end of Model 2.7");
		logger.info ("end of Model 2.7");


		if (useMessageWindow) mw.setVisible(false);
    }//end of main



}
