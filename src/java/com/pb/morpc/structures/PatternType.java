package com.pb.morpc.structures;
import java.io.Serializable;

/**
 * @author Freedman
 *
 * An enumeration of Pattern Types (day pattern results)
 */
public final class PatternType implements Serializable {

    public static final short TYPES=11;
    public static final short WORK_1 = 1;
    public static final short WORK_2 =2;
    public static final short SCHOOL_1 = 3;
    public static final short SCHOOL_2 = 4;
    public static final short SCHOOL_WORK = 5;
    public static final short UNIV_1 = 6;
    public static final short UNIV_2 = 7;
    public static final short UNIV_WORK = 8;
    public static final short WORK_UNIV = 9;
    public static final short NON_MAND = 10;
    public static final short HOME =11;
        

}
