package com.pb.morpc.structures;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.logging.Logger;

import com.pb.common.util.ObjectUtil;

/**
 * @author Freedman
 *
 */
//public class Person implements java.io.Serializable {
public class Person implements java.io.Externalizable {

	protected static Logger logger = Logger.getLogger("com.pb.morpc.models");

	short ID;
    short personType;
	short patternType;
	short freeParking;

    //an array indicated whether the person is available
    //for each hour of the day.
    boolean[] available = new boolean[TemporalType.HOURS+1];

	short maxAdultOverlaps;
	short maxChildOverlaps;
	short availWindow;

	boolean[] jointTourParticipation;
	boolean[] individualTourParticipation;
	boolean[] mandatoryTourParticipation;

	short numMandTours;
	short numJointTours;
	short numIndNonMandTours;
	short numIndNonMandInceTours;
	short numIndNonMandShopTours;
	short numIndNonMandMaintTours;
	short numIndNonMandDiscrTours;
	short numIndNonMandEatTours;


    public Person () {
    	for (int i=0; i < available.length; i++)
    		if (i < 5 || i > 23)
				available[i] = false;
			else
				available[i] = true;
    }


	/**
	 * return number of mandatory tours for this person.
	 */
	public int getNumMandTours () {
		return this.numMandTours;
	}


	/**
	 * set number of mandatory tours for this person.
	 */
	public void setNumMandTours (int numMandTours) {
		this.numMandTours = (short)numMandTours;
	}


	/**
	 * return number of joint tours in which this person participates.
	 */
	public int getNumJointTours () {
		return this.numJointTours;
	}


	/**
	 * set number of joint tours in which this person participates.
	 */
	public void setNumJointTours (int numJointTours) {
		this.numJointTours = (short)numJointTours;
	}


	/**
	 * return number of individual non-mandatory tours in which this person participates.
	 */
	public int getNumIndNonMandTours () {
		return this.numIndNonMandTours;
	}


	/**
	 * set number of individual non-mandatory tours in which this person participates.
	 */
	public void setNumIndNonMandTours (int numIndNonMandTours) {
		this.numIndNonMandTours = (short)numIndNonMandTours;
	}


	/**
	 * return number of individual non-mandatory tours including escorting in which this person participates.
	 */
	public int getNumIndNonMandInceTours () {
		return this.numIndNonMandInceTours;
	}


	/**
	 * set number of individual non-mandatory tours including escorting in which this person participates.
	 */
	public void setNumIndNonMandInceTours (int numIndNonMandInceTours) {
		this.numIndNonMandInceTours = (short)numIndNonMandInceTours;
	}


	/**
	 * set number of individual non-mandatory shopping tours in which this person participates.
	 */
	public void setNumIndNonMandShopTours (int numIndNonMandShopTours) {
		this.numIndNonMandShopTours = (short)numIndNonMandShopTours;
	}


	/**
	 * return number of individual non-mandatory shopping tours in which this person participates.
	 */
	public int getNumIndNonMandShopTours () {
		return this.numIndNonMandShopTours;
	}


	/**
	 * set number of individual non-mandatory other maintenance tours in which this person participates.
	 */
	public void setNumIndNonMandMaintTours (int numIndNonMandMaintTours) {
		this.numIndNonMandMaintTours = (short)numIndNonMandMaintTours;
	}


	/**
	 * return number of individual non-mandatory other maintenance tours in which this person participates.
	 */
	public int getNumIndNonMandMaintTours () {
		return this.numIndNonMandMaintTours;
	}


	/**
	 * set number of individual non-mandatory discretionary tours in which this person participates.
	 */
	public void setNumIndNonMandDiscrTours (int numIndNonMandDiscrTours) {
		this.numIndNonMandDiscrTours = (short)numIndNonMandDiscrTours;
	}


	/**
	 * return number of individual non-mandatory discretionary tours in which this person participates.
	 */
	public int getNumIndNonMandDiscrTours () {
		return this.numIndNonMandDiscrTours;
	}


	/**
	 * set number of individual non-mandatory eating-out tours in which this person participates.
	 */
	public void setNumIndNonMandEatTours (int numIndNonMandEatTours) {
		this.numIndNonMandEatTours = (short)numIndNonMandEatTours;
	}


	/**
	 * return number of individual non-mandatory eating-out tours in which this person participates.
	 */
	public int getNumIndNonMandEatTours () {
		return this.numIndNonMandEatTours;
	}


	/**
	 * return maximum time window overlaps between this person and other adult persons in the household.
	 */
	public int getMaxAdultOverlaps () {
		return this.maxAdultOverlaps;
	}


	/**
	 * set maximum time window overlaps between this person and other adult persons in the household.
	 */
	public void setMaxAdultOverlaps (int overlaps) {
		this.maxAdultOverlaps = (short)overlaps;
	}


	/**
	 * return maximum time window overlaps between this person and other children in the household.
	 */
	public int getMaxChildOverlaps () {
		return this.maxChildOverlaps;
	}


	/**
	 * set maximum time window overlaps between this person and other children in the household.
	 */
	public void setMaxChildOverlaps (int overlaps) {
		this.maxChildOverlaps = (short)overlaps;
	}


	/**
	 * return available time window for this person in the household.
	 */
	public int getAvailableWindow () {
		return this.availWindow;
	}


	/**
	 * set available time window for this person in the household.
	 */
	public void setAvailableWindow (int availWindow) {
		this.availWindow = (short)availWindow;
	}


	/**
	 * return true/false that person participates in joint tour id
	 */
	public boolean getJointTourParticipation (int id) {
		return this.jointTourParticipation[id];
	}


	/**
	 * set true/false that person participates in joint tour id
	 */
	public void setJointTourParticipation (int id, boolean participation) {
		this.jointTourParticipation[id] = participation;
	}


	/**
	 * define the array which holds true/false that person participates in joint tour
	 */
	public void setJointTourParticipationArray (int numJointTours) {
		this.jointTourParticipation = new boolean[numJointTours];
	}


	/**
	 * return true/false that person participates in individual tour id
	 */
	public boolean getIndividualTourParticipation (int id) {
		return this.individualTourParticipation[id];
	}


	/**
	 * set true/false that person participates in individual tour
	 */
	public void setIndividualTourParticipation (int id, boolean participation) {
		this.individualTourParticipation[id] = participation;
	}


	/**
	 * return true/false that person participates in individual mandatory tour
	 */
	public boolean getMandatoryTourParticipation (int id) {
		return this.mandatoryTourParticipation[id];
	}


	/**
	 * set true/false that person participates in individual mandatory tour
	 */
	public void setMandatoryTourParticipation (int id, boolean participation) {
		this.mandatoryTourParticipation[id] = participation;
	}


	/**
	 * define the array which holds true/false that person participates in individual tour
	 */
	public void setIndividualTourParticipationArray (int numIndivTours) {
		this.individualTourParticipation = new boolean[numIndivTours];
	}


	/**
	 * define the array which holds true/false that person participates in individual mandatory tour
	 */
	public void setMandatoryTourParticipationArray (int numMandatoryTours) {
		this.mandatoryTourParticipation = new boolean[numMandatoryTours];
	}


	/**
	 * return number of mandatory tours in which person participates
	 */
	public int getMandatoryTourCount () {

		int count=0;
		if (this.mandatoryTourParticipation != null) {
			for (int i=0; i < this.mandatoryTourParticipation.length; i++) {
				if (this.mandatoryTourParticipation[i])
					count++;
			}
		}

		return count;
	}


	/**
	 * return number of non-mandatory tours in which person participates
	 */
	public int getNonMandatoryTourCount () {

		int count=0;

		if (this.individualTourParticipation != null) {
			for (int i=0; i < this.individualTourParticipation.length; i++) {
				if (this.individualTourParticipation[i])
					count++;
			}
		}

		return count;
	}


	/**
	 * return number of joint tours in which person participates
	 */
	public int getJointTourCount () {

		int count=0;
		if (this.jointTourParticipation != null) {
			for (int i=0; i < this.jointTourParticipation.length; i++) {
				if (this.jointTourParticipation[i])
					count++;
			}
		}

		return count;
	}


    /**
     * set id for person object
     */
    public void setID (int id) {
        this.ID = (short)id;
    }

    /**
     * return id for person object
     */
    public int getID () {
        return this.ID;
    }

    /**
     * set personType for person object
     */
    public void setPersonType (int personType) {
        this.personType = (short)personType;
    }

    /**
     * get personType for person object
     */
    public int getPersonType () {
        return this.personType;
    }

    /**
     * set patternType for person object
     */
    public void setPatternType (int patternType) {
        this.patternType = (short)patternType;
    }

	/**
	 * set patternType for person object
	 */
	public int getPatternType () {
		return this.patternType;
	}

	/**
	 * set freeParking for person object
	 */
	public void setFreeParking (int arg) {
		this.freeParking = (short)arg;
	}

	/**
	 * get freeParking for person object
	 */
	public int getFreeParking () {
		return this.freeParking;
	}

	/**
	 * set the hour passed in as available for travel for the person object
	 */
	public void setHourAvailable (int arg) {
		this.available[arg] = true;
	}

	/**
	 * set the hour passed in as unavailable for travel for the person object
	 */
	public void setHourUnavailable (int arg) {
		this.available[arg] = false;
	}

	/**
	 * return true/false that the hour passed in is available for travel
	 */
	public boolean getHourIsAvailable (int arg) {
		return this.available[arg];
	}

	/**
	 * return available array
	 */
	public boolean[] getAvailable () {
		return this.available;
	}

	/*
	 * print the value of all the attributes in this object to the logger
	 */
	public void printPersonState () {

		logger.info( "");
		logger.info( "Person object information for person ID = " + ID);
		logger.info( "---------------------------------------------");

		logger.info( "(short) personType = " + personType);
		logger.info( "(short) patternType = " + patternType);
		logger.info( "(short) freeParking = " + freeParking);

		logger.info( "(short) maxAdultOverlaps = " + maxAdultOverlaps);
		logger.info( "(short) maxChildOverlaps = " + maxChildOverlaps);
		logger.info( "(short) availWindow = " + availWindow);

		logger.info( "(short) numMandTours = " + numMandTours);
		logger.info( "(short) numJointTours = " + numJointTours);
		logger.info( "(short) numIndNonMandTours = " + numIndNonMandTours);
		logger.info( "(short) numIndNonMandInceTours = " + numIndNonMandInceTours);
		logger.info( "(short) numIndNonMandShopTours = " + numIndNonMandShopTours);
		logger.info( "(short) numIndNonMandMaintTours = " + numIndNonMandMaintTours);
		logger.info( "(short) numIndNonMandDiscrTours = " + numIndNonMandDiscrTours);
		logger.info( "(short) numIndNonMandEatTours = " + numIndNonMandEatTours);

		logger.info( "(boolean[]) available = ");
		if (available != null) {
			for (int i=0; i < available.length; i++)
				logger.info( "available[" + i + "] =" + available[i]);
		}
		else {
			logger.info( "available = null");
		}

		logger.info( "(boolean[]) jointTourParticipation = ");
		if (jointTourParticipation != null) {
			for (int i=0; i < jointTourParticipation.length; i++)
				logger.info( "jointTourParticipation[" + i + "] =" + jointTourParticipation[i]);
		}
		else {
			logger.info( "jointTourParticipation = null");
		}

		logger.info( "(boolean[]) individualTourParticipation = ");
		if (individualTourParticipation != null) {
			for (int i=0; i < individualTourParticipation.length; i++)
				logger.info( "individualTourParticipation[" + i + "] =" + individualTourParticipation[i]);
		}
		else {
			logger.info( "individualTourParticipation = null");
		}

		logger.info( "(boolean[]) mandatoryTourParticipation = ");
		if (mandatoryTourParticipation != null) {
			for (int i=0; i < mandatoryTourParticipation.length; i++)
				logger.info( "mandatoryTourParticipation[" + i + "] =" + mandatoryTourParticipation[i]);
		}
		else {
			logger.info( "mandatoryTourParticipation = null");
		}

	}


    /**
     * Each variable is read in the order that it written out in writeExternal().
     * When reading arrays, the length of the array is read first so the for-loop
     * can be initialized with the correct length.
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ID = in.readShort();
        personType = in.readShort();
        patternType = in.readShort();
        freeParking = in.readShort();

        available = ObjectUtil.readBooleanArray(in);

        maxAdultOverlaps = in.readShort();
        maxChildOverlaps = in.readShort();
        availWindow = in.readShort();

        jointTourParticipation = ObjectUtil.readBooleanArray(in);
        individualTourParticipation = ObjectUtil.readBooleanArray(in);
        mandatoryTourParticipation = ObjectUtil.readBooleanArray(in);

        numMandTours = in.readShort();
        numJointTours = in.readShort();
        numIndNonMandTours = in.readShort();
        numIndNonMandInceTours = in.readShort();
        numIndNonMandShopTours = in.readShort();
        numIndNonMandMaintTours = in.readShort();
        numIndNonMandDiscrTours = in.readShort();
        numIndNonMandEatTours = in.readShort();
    }


    /**
     * Each variable is written out in the order that it was declared in the class.
     * When writing out arrays, the length of the array is written first so the
     * readExternal method knows how many elements to read.
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(ID);
        out.writeShort(personType);
        out.writeShort(patternType);
        out.writeShort(freeParking);

        ObjectUtil.writeBooleanArray(out, available);

        out.writeShort(maxAdultOverlaps);
        out.writeShort(maxChildOverlaps);
        out.writeShort(availWindow);

        ObjectUtil.writeBooleanArray(out, jointTourParticipation);
        ObjectUtil.writeBooleanArray(out, individualTourParticipation);
        ObjectUtil.writeBooleanArray(out, mandatoryTourParticipation);

        out.writeShort(numMandTours);
        out.writeShort(numJointTours);
        out.writeShort(numIndNonMandTours);
        out.writeShort(numIndNonMandInceTours);
        out.writeShort(numIndNonMandShopTours);
        out.writeShort(numIndNonMandMaintTours);
        out.writeShort(numIndNonMandDiscrTours);
        out.writeShort(numIndNonMandEatTours);
    }

    public void writeContentToLogger(Logger logger){
      logger.severe("Contents of Person object with index:"+ID);
      logger.severe("personType:"+personType);
      logger.severe("patternType:"+patternType);
      logger.severe("freeParking:"+freeParking);

	  if (available != null) {
	      for(int i=0; i<available.length; i++){
	          logger.severe("available["+i+"]="+available[i]);
	      }
	  }
	  else {
		  logger.severe("available[]=null");
	  }

      logger.severe("maxAdultOverlaps:"+maxAdultOverlaps);
      logger.severe("maxChildOverlaps:"+maxChildOverlaps);
      logger.severe("availWindow:"+availWindow);

      if (jointTourParticipation != null) {
          for(int i=0; i<jointTourParticipation.length; i++){
              logger.severe("jointTourParticipation["+i+"]="+jointTourParticipation[i]);
          }
      }
      else {
          logger.severe("jointTourParticipation[]=null");
      }

	  if (individualTourParticipation != null) {
	      for(int i=0; i<individualTourParticipation.length; i++){
	        logger.severe("individualTourParticipation["+i+"]="+individualTourParticipation[i]);
	      }
	  }
	  else {
		  logger.severe("individualTourParticipation[]=null");
	  }

	  if (mandatoryTourParticipation != null) {
		  for(int i=0; i<mandatoryTourParticipation.length; i++){
			logger.severe("mandatoryTourParticipation["+i+"]="+mandatoryTourParticipation[i]);
		  }
	  }
	  else {
		  logger.severe("mandatoryTourParticipation[]=null");
	  }

	  
      logger.severe("numMandTours:"+numMandTours);
      logger.severe("numJointTours:"+numJointTours);
      logger.severe("numIndNonMandTours:"+numIndNonMandTours);
      logger.severe("numIndNonMandInceTours:"+numIndNonMandInceTours);
      logger.severe("numIndNonMandShopTours:"+numIndNonMandShopTours);
      logger.severe("numIndNonMandMaintTours:"+numIndNonMandMaintTours);
      logger.severe("numIndNonMandDiscrTours:"+numIndNonMandDiscrTours);
      logger.severe("numIndNonMandEatTours:"+numIndNonMandEatTours);
    }
}
