package com.pb.morpc.models;


import com.pb.common.model.Alternative;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;

/**
 * Implements the logit model for auto ownership choice
 * 
 * @author    Jim Hicks
 * @version   1.0, 3/10/2003
 *
 */
public class AutoOwnershipLM {

	LogitModel root;
	
	
	public AutoOwnershipLM() {
		
		// define the structure of this simple multinomial logit model
		define();
		
	}
	
	
	public void attachUtilities (double[] utilities) {
		
		Alternative alt;
		
		for (int i=0; i < 5; i++) {
			alt = root.getAlternative(i);
			
            alt.setAvailability( utilities[i] > -99.0 );
			alt.setUtility (utilities[i]);
		}
	}
	
	
	
    public double[] getProportions () {
        
        //calculate the logsum at the root level
        root.writeUtilityHeader();
        double logsum = root.getUtility();

        //calculate probabilities
        root.writeProbabilityHeader();
        root.calculateProbabilities();
        
        double[] proportions = root.getProbabilities();
        double[] newProportions = new double[5];
        for (int i=0; i < 5; i++)
            newProportions[i] = proportions[i];
        
        return ( newProportions );
        
    }
    
    public int getAlternativeNumber(){
        ConcreteAlternative ca = (ConcreteAlternative)root.chooseElementalAlternative();
        return ((Integer)ca.getAlternativeObject()).intValue();
    }
    
    
	private void define () {

		// define root level
		this.root = new LogitModel ("root", 5);
//	    this.root.setDebug (true);

	    // define level 1 alternatives
	    ConcreteAlternative auto0  = new ConcreteAlternative ("0 auto", new Integer(1));
	    ConcreteAlternative auto1  = new ConcreteAlternative ("1 auto", new Integer(2));
	    ConcreteAlternative auto2  = new ConcreteAlternative ("2 auto", new Integer(3));
	    ConcreteAlternative auto3  = new ConcreteAlternative ("3 auto", new Integer(4));
	    ConcreteAlternative auto4p = new ConcreteAlternative ("4+ auto", new Integer(5));
	
		// add alternatives to root level nest to create simple multinomial logit model
	    root.addAlternative (auto0);
	    root.addAlternative (auto1);
	    root.addAlternative (auto2);
	    root.addAlternative (auto3);
	    root.addAlternative (auto4p);

        // set availabilities
        root.computeAvailabilities();
        root.writeAvailabilities();
	}        

}
