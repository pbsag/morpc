package com.pb.morpc.structures;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: Christi Willison
 * Date: May 7, 2003
 * Time: 4:06:08 PM
 */
public class OutputDescription implements Serializable {

    public static String getDescription (String columnName, float value){
        String description = null;
        int testCase = (int)value;

        if(columnName.equals("M1")){
            switch(testCase){
                case(1):
                    description="0_car";
                    break;
                case(2):
                    description="1_car";
                    break;
                case(3):
                    description="2_cars";
                    break;
                case(4):
                    description="3_cars";
                    break;
                case(5):
                    description="4+_cars";
                    break;
                default:
                    description="not avail";
            }
        }else if (columnName.equals("M2")) {
            switch(testCase){
                case(1):
                    description="work_1";
                    break;
                case(2):
                    description="work_2";
                    break;
                case(3):
                    description="school_1";
                    break;
                case(4):
                    description="school_2";
                    break;
                case(5):
                    description="school_work";
                    break;
                case(6):
                    description="univ_1";
                    break;
                case(7):
                    description="univ_2";
                    break;
                case(8):
                    description="univ_work";
                    break;
                 case(9):
                    description="work_univ";
                    break;
                 case(10):
                    description="non_mand";
                    break;
                 case(11):
                    description="home";
                    break;
                 default:
                    description="not avail";
              }
            } else if(columnName.equals("M31")){
                switch(testCase){
                case(1):
                    description="0_tours";
                    break;
                case(2):
                    description="1_Shop";
                    break;
                case(3):
                    description="1_Eat";
                    break;
                case(4):
                    description="1_Main";
                    break;
                case(5):
                    description="1_Disc";
                    break;
                case(6):
                    description="2_SS";
                    break;
                case(7):
                    description="2_SE";
                    break;
                case(8):
                    description="2_SM";
                    break;
                 case(9):
                    description="2_SD";
                    break;
                 case(10):
                    description="2_EE";
                    break;
                 case(11):
                    description="2_EM";
                    break;
                 case(12):
                    description="2_ED";
                    break;
                 case(13):
                    description="2_MM";
                    break;
                 case(14):
                    description="2_MD";
                    break;
                 case(15):
                    description="2_DD";
                    break;
                 case(199):
                    description="0_travelers";
                    break;
                 default:
                    description="not avail";
              }
            } else if (columnName.equals("joint_tour_comp")){
                switch(testCase){
                  case(1):
                    description="adults";
                    break;
                  case(2):
                    description="children";
                    break;
                  case(3):
                    description="mixed";
                    break;
                  case(0):
                    description="no HH tour";
                    break;
                  default:
                    description="not avail";
                  }
            } else if (columnName.equals("participation")){
                 switch(testCase){
                  case(1):
                    description="yes";
                    break;
                  case(2):
                    description="no";
                    break;
                  case(99):
                    description="not elig";
                    break;
                  default:
                    description="not avail";
                 }
            } else if (columnName.equals("person_type")){
                switch(testCase){
                  case(1):
                    description="workers_f";
                    break;
                  case(2):
                    description="workers_p";
                    break;
                  case(3):
                    description="students";
                    break;
                  case(4):
                    description="nonworkers";
                    break;
                  case(5):
                    description="preschool";
                    break;
                  case(6):
                    description="schoolpred";
                    break;
                  case(7):
                    description="schooldriv";
                    break;
                  default:
                    description="not avail";
                 }
            } else if (columnName.equals("income")){
                switch(testCase){
                  case(1):
                    description="low";
                    break;
                  case(2):
                    description="medium";
                    break;
                  case(3):
                    description="high";
                    break;
                  default:
                    description="not avail";
                }
            } else description="not avail";



        return description;
    }

}
