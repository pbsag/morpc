package com.pb.morpc.daf;

/**
 * @author Jim Hicks
 *
 * worker class for setting the same random number seed on all nodes in a distributed environment.
 */


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.SeededRandom;


import java.util.HashMap;
import org.apache.log4j.Logger;



public class RnWorker extends MessageProcessingTask implements java.io.Serializable {

	private static boolean LOGGING = true;
	private static Logger logger = Logger.getLogger("com.pb.morpc.daf");
	
    private static MatrixIO32BitJvm ioVm32Bit = null;

    private HashMap propertyMap = null; 

	private String modelServer = "RnServer";
	
	
    public RnWorker () {
    }
    


	public void onStart() {

		if (LOGGING)
		    logger.info( this.name +  " onStart().");

        // Added by Jim Hicks - 14 Mar 2008
        // The matrix i/o server and 32 bit JVM that are necessary for reading TPPLUS matrix data 
        // are started in this class, which is the first task run on worker nodes, so that the matrix i/o
		// classes needed will be ready when the UECs that read matrix data need them, on each node.
		// Once these following lines are executed, tpplus matrix data can be read using RMI to a 32 bit JVM.
        
        // start the 32 bit JVM used specifically for running matrix io classes
        ioVm32Bit = MatrixIO32BitJvm.getInstance();
        ioVm32Bit.startJVM32();
        
        // establish that matrix reader and writer classes will use the RMI versions for TPPLUS format matrices
        ioVm32Bit.startMatrixDataServer( MatrixType.TPPLUS );
        
        
        
		// ask the random number server for start info including the propertyMap
		if (LOGGING)
		    logger.info( this.name +  " asking for START_INFO from " + modelServer + "." );
		Message msg = createMessage();
		msg.setId( MessageID.SEND_START_INFO );
		sendTo( modelServer, msg );
		
	}



	public void onMessage(Message msg) {

		if (msg.getId().equals(MessageID.START_INFO)) {
		    
			propertyMap = (HashMap)msg.getValue( MessageID.PROPERTY_MAP_KEY );
			
			if (LOGGING)
			    logger.info (this.getName() + " reading propertyMap message.");
			
			// set the global random number generator seed read from the properties file
			SeededRandom.setSeed(Integer.parseInt((String) propertyMap.get("RandomSeed")));

			Message finishedMsg = createMessage();
			finishedMsg.setId( MessageID.FINISHED );
			sendTo( modelServer, finishedMsg );
			
		}
		else if ( msg.getId().equals( MessageID.RELEASE_MEMORY ) || msg.getId().equals( MessageID.EXIT ) ) {

			if (LOGGING)
				logger.info (this.getName() + " releasing memory after getting " + msg.getId() + " from " + msg.getSender() );
		    
			propertyMap = null;

		}
		
	}

}
