package com.pb.morpc.matrix;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.io.File;
import java.io.Serializable;

import com.pb.common.calculator.DataEntry;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;


/**
 * @author Jim Hicks
 *
 * Class for managing matrix data in a remote process and accessed by UECs using RMI.
 */

public class MatrixDataServer implements MatrixDataServerIf, Serializable
{

    protected static Logger logger = Logger.getLogger(MatrixDataServer.class);
    
    private static final String VERSION                    = "2.2.1";
    
    // These are used if the server is started manually by running this class's
    // main(). If so, these must be defined consistently with
    // any class that acts as a client to the server, i.e. the client must know the
    // address and port as well.
    private static final String        MATRIX_DATA_SERVER_ADDRESS = "127.0.0.1";
    private static final int           MATRIX_DATA_SERVER_PORT    = 1171;
    public static final String         MATRIX_DATA_SERVER_NAME    = MatrixDataServer.class.getCanonicalName();
    private static final String        MATRIX_DATA_SERVER_LABEL   = "matrix server";
    
    private MatrixIO32BitJvm           ioVm32Bit                  = null;
    
    private HashMap<String, DataEntry> matrixEntryMap;
    private HashMap<String, Matrix>    matrixMap;
    
    public MatrixDataServer()
    {
        // start the 32 bit JVM used specifically for running matrix io classes
        ioVm32Bit = MatrixIO32BitJvm.getInstance();
    }
    
    public String testRemote()
    {
        logger.info("testRemote() called by remote process.");
        return String.format("testRemote() method in %s called.", this.getClass().getCanonicalName());
    }
    
    /*
     * Read a matrix.
     * @param matrixEntry a DataEntry describing the matrix to read
     * @return a Matrix
     */
     public synchronized Matrix getMatrix(DataEntry matrixEntry)
     {
     
         Matrix matrix;
         String fileName = matrixEntry.fileName;
         
         if (matrixEntry.format.equalsIgnoreCase("emme2"))
         {
             MatrixReader mr = MatrixReader.createReader(MatrixType.EMME2, new File(fileName));
             matrix = mr.readMatrix(matrixEntry.matrixName);
         } else if (matrixEntry.format.equalsIgnoreCase("binary"))
         {
             MatrixReader mr = MatrixReader.createReader(MatrixType.BINARY, new File(fileName));
             matrix = mr.readMatrix();
         } else if (matrixEntry.format.equalsIgnoreCase("zip")
                 || matrixEntry.format.equalsIgnoreCase("zmx"))
         {
             MatrixReader mr = MatrixReader.createReader(MatrixType.ZIP, new File(fileName));
             matrix = mr.readMatrix();
         } else if (matrixEntry.format.equalsIgnoreCase("tpplus"))
         {
             MatrixReader mr = MatrixReader.createReader(MatrixType.TPPLUS, new File(fileName));
             matrix = mr.readMatrix(matrixEntry.matrixName);
         } else if (matrixEntry.format.equalsIgnoreCase("transcad"))
         {
             MatrixReader mr = MatrixReader.createReader(MatrixType.TRANSCAD, new File(fileName));
             matrix = mr.readMatrix(matrixEntry.matrixName);
         } else
         {
             throw new RuntimeException("unsupported matrix type: " + matrixEntry.format);
         }
         
         // Use token name from control file for matrix name (not name from underlying
         // matrix)
         matrix.setName(matrixEntry.name);
         
         return matrix;
     }
     
   /*
    * Read a matrix.
    * @param matrixEntry a DataEntry describing the matrix to read
    * @return a Matrix
    */
    public synchronized Matrix readTppMatrix( String fileName, String tableName )
    {
      
        MatrixReader mr = MatrixReader.createReader(MatrixType.TPPLUS, new File(fileName));
        Matrix matrix = mr.readMatrix(tableName);
          
        // Use table name for matrix name
        matrix.setName(tableName);
          
        return matrix;
    }
      
    public synchronized void writeTpplusMatrices ( String tppFileName, float[][][] trips, String[] names, String[] descriptions ) {

        MatrixWriter mw = MatrixWriter.createWriter (MatrixType.TPPLUS, new File( tppFileName ) );

        Matrix[] outputMatrices = new Matrix[trips.length-1];
        String[] newNames = new String[trips.length-1];
        
        logger.info( String.format("matrix totals for %d tables written to %s:", trips.length-1, tppFileName) );
        for (int i=1; i < trips.length; i++) {
            newNames[i-1] = names[i];
            outputMatrices[i-1] = new Matrix( names[i], descriptions[i], trips[i] );
            logger.info( String.format("    [%d] %-16s: %.0f", i-1, newNames[i-1], outputMatrices[i-1].getSum()) );
        }

        mw.writeMatrices(newNames, outputMatrices);

    }
    
    public void clear()
    {
        if (matrixMap != null) matrixMap.clear();
        if (matrixEntryMap != null) matrixEntryMap.clear();
    }
    
    public void start32BitMatrixIoServer(MatrixType mType)
    {
    
        // start the matrix I/O server process
        ioVm32Bit.startJVM32();
        
        // establish that matrix reader and writer classes will use the RMI versions
        // for TPPLUS format matrices
        ioVm32Bit.startMatrixDataServer(mType);
        logger.info("matrix data server 32 bit process started.");
    
    }
    
    public void stop32BitMatrixIoServer()
    {
    
        // stop the matrix I/O server process
        ioVm32Bit.stopMatrixDataServer();
        
        // close the JVM in which the RMI reader/writer classes were running
        ioVm32Bit.stopJVM32();
        logger.info("matrix data server 32 bit process stopped.");
    
    }
    
    public static void main(String args[]) throws Exception
    {
    
        String serverAddress = MATRIX_DATA_SERVER_ADDRESS;
        int serverPort = MATRIX_DATA_SERVER_PORT;
        String className = MATRIX_DATA_SERVER_NAME;
        String serverLabel = MATRIX_DATA_SERVER_LABEL;
        
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equalsIgnoreCase("-hostname")) serverAddress = args[i + 1];
            else if (args[i].equalsIgnoreCase("-port")) serverPort = Integer.parseInt(args[i + 1]);
            else if (args[i].equalsIgnoreCase("-label")) serverLabel = args[i + 1];
        }
        
        MatrixDataServer matrixServer = new MatrixDataServer();
        
        try
        {
        
            // create the concrete data server object
            matrixServer.start32BitMatrixIoServer(MatrixType.TPPLUS);
        
        } catch (RuntimeException e)
        {
            matrixServer.stop32BitMatrixIoServer();
            logger
                    .error(
                            "RuntimeException caught in com.pb.models.ctramp.MatrixDataServer.main() -- exiting.",
                            e);
        }
        
        // bind this concrete object with the cajo library objects for managing RMI
        Remote.config(serverAddress, serverPort, null, 0);
        ItemServer.bind(matrixServer, className);
        
        // log that the server started
        System.out.println(String.format("%s version %s started on: %s:%d", serverLabel, VERSION,
                serverAddress, serverPort));
    
    }

}