package com.pb.morpc.matrix;

import com.pb.common.math.MathUtil;
import com.pb.common.matrix.NDimensionalMatrix;
import com.pb.common.matrix.NDimensionalMatrixDouble;
import com.pb.common.matrix.RowVector;


/**
 * The MatrixUtil class holds static public methods for use on Matrix objects.
 *
 */
public class MatrixUtil {

    
    private MatrixUtil () {
    }

    
    /**
     * get an NDimensionalMatrix object tha represents the double[][]
     */
    public static NDimensionalMatrix getNDimensionalMatrix (double[][] array) {

		int[] loc = new int[2];
		int[] shape = { array.length, array[0].length };
    	NDimensionalMatrix m2d = new NDimensionalMatrix ("", 2, shape);
    	float fValue;
    	double dValue;
    	double dsum1=0.0, dsum2=0.0, dsum3=0.0;
    
        // Set matrix values
        for (int r=0; r < array.length; r++) {
        	for (int c=0; c < array[r].length; c++) {
        		dValue = array[r][c] + Float.MIN_VALUE/2.0;
        		
        		if (dValue < Float.MIN_VALUE)
        			fValue = Float.MIN_VALUE;
        		else
        			fValue = (float)dValue;
        			
        		dsum1 += fValue;
        		dsum2 += dValue;
        		
        		loc[0] = r;
        		loc[1] = c;
        		m2d.setValue ( fValue, loc );
        		
        		dsum3 += m2d.getValue (loc);
        	}
        }
        		
		return m2d;        
    }


    /**
     * get an NDimensionalMatrixDouble object tha represents the double[][]
     */
    public static NDimensionalMatrixDouble getNDimensionalMatrixDouble (double[][] array) {

		int[] loc = new int[2];
		int[] shape = { array.length, array[0].length };
    	NDimensionalMatrixDouble m2d = new NDimensionalMatrixDouble ("", 2, shape);
    	float fValue;
    	double dValue;
    	double dsum1=0.0, dsum2=0.0, dsum3=0.0;
    
        // Set matrix values
        for (int r=0; r < array.length; r++) {
        	for (int c=0; c < array[r].length; c++) {
        		dValue = array[r][c] + Float.MIN_VALUE/2.0;
        		
        		if (dValue < Float.MIN_VALUE)
        			fValue = Float.MIN_VALUE;
        		else
        			fValue = (float)dValue;
        			
        		dsum1 += fValue;
        		dsum2 += dValue;
        		
        		loc[0] = r;
        		loc[1] = c;
        		m2d.setValue ( fValue, loc );
        		
        		dsum3 += m2d.getValue (loc);
        	}
        }
        		
		return m2d;        
    }


    public static RowVector adjustAverageToControl (RowVector seedProportions, RowVector seedCategoryValues, float controlAvg) {
        
        float epsilon = 0.000001f;
        float[] proportions = seedProportions.copyValues1D();
        float[] newProportions = seedProportions.copyValues1D();
        float[] categories = seedCategoryValues.copyValues1D();
        float lambda = 0.0f;
        float gap = 100.0f;
        float newAvg;
		int zeroCategory = 0;
    
            
		if (controlAvg == 0.0f) {
			// if the control average is 0, handle the special case
			for (int i=0; i < newProportions.length; i++) {
				newProportions[i] = 0.0f;
				
				if (categories[i] == 0.0f)
					zeroCategory = i;
			}
			newProportions[zeroCategory] = 1.0f;
		}
		else if (controlAvg > categories[categories.length-1]) {
			// if the control average is greater than the largest category value, handle the special case
			for (int i=0; i < newProportions.length; i++)
				newProportions[i] = 0.0f;

			newProportions[categories.length-1] = 1.0f;
		}
		else {
			// otherwise, apply the adjustment algorithm
	        while (gap > epsilon) {
	            
	            newProportions = calculateNewProportions (proportions, categories, lambda);
	            
	            newAvg = calculateNewAverage (newProportions, categories);
	
	            gap = calculateAbsoluteGap (newAvg, controlAvg);
	            
	            lambda = calculateNewLambda (lambda, newAvg, controlAvg);
	        }
		}
    
        
        return ( new RowVector(newProportions) );
    }        
    
    
    private static float[] calculateNewProportions (float[] oldProportions, float[] categories, float lambda) {
        
        double proportion;
        double category;
        float newProportions[] = new float[oldProportions.length];
        
        
        double denom = 0.0f;
        for (int i=0; i < oldProportions.length; i++)
            denom += oldProportions[i]*MathUtil.exp(lambda*categories[i]);

    
        for (int i=0; i < oldProportions.length; i++)
            newProportions[i] = (float)(oldProportions[i]*MathUtil.exp(lambda*categories[i])/denom);

        
        return newProportions;
    }        
   
   
    private static float calculateNewAverage (float[] newProportions, float[] categories) {

        float average = 0.0f;
        for (int i=0; i < newProportions.length; i++)
            average += newProportions[i]*categories[i];
        
        return average;
    }
    
    
    private static float calculateAbsoluteGap (float newAvg, float controlAvg) {
        
        return ( Math.abs( newAvg - controlAvg ) );
    }
    
    
    private static float calculateNewLambda (float lambda, float newAvg, float controlAvg) {
        
        return ( lambda + (controlAvg - newAvg)/controlAvg );
    }
}
