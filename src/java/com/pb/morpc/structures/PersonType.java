package com.pb.morpc.structures;
import java.io.Serializable;

/**
 * @author Freedman
 *
 * An enumeration of person types.
 */
public final class PersonType implements Serializable{
    
    public static final short TYPES=7;
    public static final short WORKER_F = 1;
    public static final short WORKER_P = 2;
    public static final short STUDENT = 3;
    public static final short NONWORKER = 4;
    public static final short PRESCHOOL = 5;
    public static final short SCHOOL_PRED =6;
    public static final short SCHOOL_DRIV = 7;

}
