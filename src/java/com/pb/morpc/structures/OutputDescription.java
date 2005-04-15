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
            } else if (columnName.equals("M32")){
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
            } else if (columnName.equals("M33")){
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
            } else if (columnName.equals("M41")){
                switch(testCase){
                 case(0):
                   description="not applicable";
                   break;
                 case(1):
                   description="0S_0E_0M";
                   break;
                 case(2):
                    description="0S_0E_1M";
                    break;
                 case(3):
                    description="0S_0E_2M";
                    break;
                 case(4):
                    description="0S_0E_3M";
                    break;
                 case(5):
                    description="0S_1E_0M";
                    break;
                 case(6):
                    description="0S_1E_1M";
                    break;
                 case(7):
                    description="0S_1E_2M";
                    break;
                 case(8):
                    description="0S_1E_3M";
                    break;
                 case(9):
                    description="0S_2E_0M";
                    break;
                 case(10):
                    description="0S_2E_1M";
                    break;
                 case(11):
                    description="0S_2E_2M";
                    break;
                 case(12):
                    description="0S_2E_3M";
                    break;
                 case(13):
                    description="1S_0E_0M";
                    break;
                 case(14):
                    description="1S_0E_1M";
                    break;
                 case(15):
                    description="1S_0E_2M";
                    break;
                 case(16):
                    description="1S_0E_3M";
                    break;
                 case(17):
                    description="1S_1E_0M";
                    break;
                 case(18):
                    description="1S_1E_1M";
                    break;
                 case(19):
                    description="1S_1E_2M";
                    break;
                 case(20):
                    description="1S_1E_3M";
                    break;
                 case(21):
                    description="1S_2E_0M";
                    break;
                 case(22):
                    description="1S_2E_1M";
                    break;
                 case(23):
                    description="1S_2E_2M";
                    break;
                 case(24):
                    description="1S_2E_3M";
                    break;
                 case(25):
                    description="2S_0E_0M";
                    break;
                 case(26):
                    description="2S_0E_1M";
                    break;
                 case(27):
                    description="2S_0E_2M";
                    break;
                 case(28):
                    description="2S_0E_3M";
                    break;
                 case(29):
                    description="2S_1E_0M";
                    break;
                 case(30):
                    description="2S_1E_1M";
                    break;
                 case(31):
                    description="2S_1E_2M";
                    break;
                 case(32):
                    description="2S_1E_3M";
                    break;
                 case(33):
                    description="2S_2E_0M";
                    break;
                 case(34):
                    description="2S_2E_1M";
                    break;
                 case(35):
                    description="2S_2E_2M";
                    break;
                 case(36):
                    description="2S_2E_3M";
                    break;
                 default:
                   description="not avail";
                }
            } else if (columnName.equals("M42")){
                switch(testCase){
                 case(0):
                    description="not applicable";
                    break;
                 case(1):
                    description="Work_f1";
                    break;
                 case(2):
                    description="Work_f2";
                    break;
                 case(3):
                    description="Work_f3";
                    break;
                 case(4):
                    description="Work_f4";
                    break;
                 case(5):
                    description="Work_p1";
                    break;
                 case(6):
                    description="Work_p2";
                    break;
                 case(7):
                    description="Work_p3";
                    break;
                 case(8):
                    description="Work_p4";
                    break;
                 case(9):
                    description="Stud1";
                    break;
                 case(10):
                    description="Stud2";
                    break;
                 case(11):
                    description="Stud3";
                    break;
                 case(12):
                    description="Stud4";
                    break;
                 case(13):
                    description="Nonw1";
                    break;
                 case(14):
                    description="Nonw2";
                    break;
                 case(15):
                    description="Nonw3";
                    break;
                 case(16):
                    description="Nonw4";
                    break;
                 case(17):
                    description="Pred1";
                    break;
                 case(18):
                    description="Pred2";
                    break;
                 case(19):
                    description="Pred3";
                    break;
                 case(20):
                    description="Pred4";
                    break;
                 case(21):
                    description="Driv1";
                    break;
                 case(22):
                    description="Driv2";
                    break;
                 case(23):
                    description="Driv3";
                    break;
                 case(24):
                    description="Driv4";
                    break;
                 default:
                   description="not avail";
                }
            } else if (columnName.equals("M431") || columnName.equals("M432") || columnName.equals("M433")){
                switch(testCase){
                  case(0):
                    description="not applicable";
                    break;
                  case(1):
                    description="No_tours";
                    break;
                  case(2):
                    description="1_eat";
                    break;
                  case(3):
                    description="1_discr";
                    break;
                  case(4):
                    description="2_discr";
                    break;
                  case(5):
                    description="2_ED";
                    break;
                  default:
                    description="not avail";
                }
            } else if (columnName.equals("M44")){

                switch(testCase){
                  case(0):
                    description="not applicable";
                    break;
                  case(1):
                    description="No_subts";
                    break;
                  case(2):
                    description="1_eat";
                    break;
                  case(3):
                    description="1_work";
                    break;
                  case(4):
                    description="1_other";
                    break;
                  case(5):
                    description="2_work";
                    break;
                  case(6):
                    description="2_EW";
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
