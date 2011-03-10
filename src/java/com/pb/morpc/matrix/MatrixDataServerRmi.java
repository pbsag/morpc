package com.pb.morpc.matrix;

import java.io.Serializable;
import com.pb.common.calculator.DataEntry;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;

/**
 * @author Jim Hicks
 * 
 *         Class for managing matrix data in a remote process and accessed by UECs
 *         using RMI.
 */
public class MatrixDataServerRmi
        implements MatrixDataServerIf, Serializable
{

    // protected static Logger logger = Logger.getLogger(MatrixDataServerRmi.class);

    UtilRmi remote;
    String  connectString;

    public MatrixDataServerRmi(String hostname, int port, String className)
    {

        connectString = String.format("//%s:%d/%s", hostname, port, className);
        remote = new UtilRmi(connectString);

    }

    public void clear()
    {
        Object[] objArray = {};
        remote.method("clear", objArray);
    }

    public Matrix getMatrix(DataEntry dataEntry)
    {
        Object[] objArray = {dataEntry};
        return (Matrix) remote.method("getMatrix", objArray);
    }

    public Matrix readTppMatrix(String fileName, String tableName)
    {
        Object[] objArray = {fileName, tableName};
        return (Matrix) remote.method("readTppMatrix", objArray);
    }

    public void writeTpplusMatrices ( String fileName, float[][][] trips, String[] names, String[] descriptions ) {
        Object[] objArray = {fileName, trips, names, descriptions};
        remote.method("writeTpplusMatrices", objArray);
    }
    
    public void start32BitMatrixIoServer(MatrixType mType)
    {
        Object[] objArray = {mType};
        remote.method("start32BitMatrixIoServer", objArray);
    }

    public void stop32BitMatrixIoServer()
    {
        Object[] objArray = {};
        remote.method("stop32BitMatrixIoServer", objArray);
    }

    public String testRemote()
    {
        Object[] objArray = {};
        return (String) remote.method("testRemote", objArray);
    }

}