package com.pb.morpc.daf;

import java.io.Serializable;

/**
 * @author Freedman
 *
 * An enumeration of Pattern Types (day pattern results)
 */
public final class MessageID implements Serializable {

	public static final short SEND_PROPERTY_MAP_ID = 1;
	public static final String SEND_PROPERTY_MAP = "SendPropertyMap";

	public static final short PROPERTY_MAP_ID = 2;
	public static final String PROPERTY_MAP = "PropertyMap";

	public static final short PROPERTY_MAP_KEY_ID = 3;
	public static final String PROPERTY_MAP_KEY = "PropertyMapKey";

	public static final short READY_ID = 4;
    public static final String READY = "Ready";

	public static final short HOUSEHOLD_LIST_ID = 5;
	public static final String HOUSEHOLD_LIST = "HouseholdList";
    
	public static final short HOUSEHOLD_LIST_KEY_ID = 6;
	public static final String HOUSEHOLD_LIST_KEY = "HouseholdListKey";
    
	public static final short RESULTS_ID = 7;
	public static final String RESULTS = "Results";
    
	public static final short FINISHED_ID = 8;
	public static final String FINISHED = "Finished";
    
	public static final short EXIT_ID = 9;
	public static final String EXIT = "Exit";
    
	public static final short TOUR_CATEGORY_KEY_ID = 10;
	public static final String TOUR_CATEGORY_KEY = "TourCategory";

	public static final short TOUR_TYPES_KEY_ID = 11;
	public static final String TOUR_TYPES_KEY = "TourCategoryTypes";

	public static final short SEND_WORK_ID = 12;
	public static final String SEND_WORK = "SendWork";
    
	public static final short RESET_HHS_PROCESSED_ID = 13;
	public static final String RESET_HHS_PROCESSED = "ResetHHsProcessed";
    
	public static final short SEND_START_INFO_ID = 14;
	public static final String SEND_START_INFO = "SendStartInfo";

	public static final short START_INFO_ID = 15;
	public static final String START_INFO = "StartInfo";

	public static final short RESTART_ID = 16;
	public static final String RESTART = "Restart";

	public static final short HH_ARRAY_FINISHED_ID = 17;
	public static final String HH_ARRAY_FINISHED = "HhArrayFinished";
 
	public static final short ZONAL_DATA_MANAGER_KEY_ID = 18;
	public static final String ZONAL_DATA_MANAGER_KEY = "ZonalDataManager";
 
	public static final short HHS_ARRAY_KEY_ID = 19;
	public static final String HHS_ARRAY_KEY = "HouseholdsArray";
 
	public static final short SEND_HHS_ARRAY_ID = 20;
	public static final String SEND_HHS_ARRAY = "SendHouseholdsArray";
	
	public static final short STATIC_ZONAL_DATA_MAP_KEY_ID = 21;
	public static final String STATIC_ZONAL_DATA_MAP_KEY = "ZonalDataManagerStaticData";
		
	public static final short SHADOW_PRICE_ITER_KEY_ID = 22;
	public static final String SHADOW_PRICE_ITER_KEY = "ShadowPriceIter";

	public static final short CURRENT_MODEL_KEY_ID = 23;
	public static final String CURRENT_MODEL_KEY = "CurrentModel";
	
	public static final short UPDATE_HHS_ARRAY_ID = 24;
	public static final String UPDATE_HHS_ARRAY = "UpdateHouseholdsArray";
	
	public static final short UPDATED_HHS_ID = 25;
	public static final String UPDATED_HHS = "UpdatedHouseholds";
	
	public static final short TOD_DATA_MANAGER_KEY_ID = 26;
	public static final String TOD_DATA_MANAGER_KEY = "TodDataManager";
 
	public static final short STATIC_TOD_DATA_MAP_KEY_ID = 27;
	public static final String STATIC_TOD_DATA_MAP_KEY = "TodDataManagerStaticData";
		
	public static final short DTMHH_DATA_MANAGER_KEY_ID = 28;
	public static final String DTMHH_DATA_MANAGER_KEY = "DtmHhDataManager";
 
	public static final short STATIC_DTMHH_DATA_MAP_KEY_ID = 29;
	public static final String STATIC_DTMHH_DATA_MAP_KEY = "DtmHhDataManagerStaticData";
		
	public static final short PROCESSOR_ID_KEY_ID = 30;
	public static final String PROCESSOR_ID_KEY = "ProcessorID";
		
	public static final short MATRIX_LIST_ID = 31;
	public static final String MATRIX_LIST = "MatrixList";
		
	public static final short MATRIX_LIST_KEY_ID = 32;
	public static final String MATRIX_LIST_KEY = "MatrixListKey";
		
	public static final short MATRIX_NAMES_KEY_ID = 33;
	public static final String MATRIX_NAMES_KEY = "MatrixNamesKey";
		
	public static final short NUMBER_OF_ZONES_KEY_ID = 34;
	public static final String NUMBER_OF_ZONES_KEY = "NumberOfZonesKey";

	public static final short INPUT_LIST_KEY_ID = 35;
	public static final String INPUT_LIST_KEY = "InputMatrixListKey";
		
	public static final short OUTPUT_LIST_KEY_ID = 36;
	public static final String OUTPUT_LIST_KEY = "OutputMatrixListKey";
		
	public static final short CLEAR_HHS_ARRAY_ID = 37;
	public static final String CLEAR_HHS_ARRAY = "ClearHouseholdsArray";
	
	public static final short RELEASE_MEMORY_ID = 38;
	public static final String RELEASE_MEMORY = "ReleaseWorkerMemory";
		
	public static final short RELEASE_MATRIX_MEMORY_ID = 39;
	public static final String RELEASE_MATRIX_MEMORY = "ReleaseWorkerMatrixMemory";
		
}