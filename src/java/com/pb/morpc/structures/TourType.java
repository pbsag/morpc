package com.pb.morpc.structures;

/**
 * @author Freedman
 *
 * An enumeration of tour types.
 */
public final class TourType {

	public static final short TYPES=9;
    public static final short WORK = 1;
    public static final short UNIVERSITY = 2;
    public static final short SCHOOL = 3;
    public static final short ESCORTING = 4;
    public static final short SHOP = 5;
    public static final short OTHER_MAINTENANCE = 6;
    public static final short DISCRETIONARY=7;
	public static final short EAT = 8;
	public static final short ATWORK = 9;
 
	public static final short CATEGORIES = 4;
	public static final short MANDATORY_CATEGORY = 1;
	public static final short JOINT_CATEGORY = 2;
	public static final short NON_MANDATORY_CATEGORY = 3;
	public static final short AT_WORK_CATEGORY = 4;

	public static final short[] MANDATORY_TYPES = { 1, 2, 3 };
	public static final short[] JOINT_TYPES = { 5, 6, 7, 8 };
	public static final short[] NON_MANDATORY_TYPES = { 4, 5, 6, 7, 8 };
	public static final short[] AT_WORK_TYPES = { 9 };

	public static final String[][] TYPE_LABELS = {
		{ "" },
		{ "WORK", "UNIVERSITY", "SCHOOL" },
		{ "SHOP", "OTHER_MAINT", "DISCR", "EAT" },
		{ "ESCORTING", "SHOP", "OTHER_MAINT", "DISCR", "EAT" },
		{ "AT WORK" }
	};

	public static final String[] TYPE_CATEGORY_LABELS = { "", "MANDATORY", "JOINT", "NON-MANDATORY", "AT WORK" };

}
