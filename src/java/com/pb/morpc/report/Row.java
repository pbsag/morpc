package com.pb.morpc.report;

import com.pb.common.matrix.NDimensionalMatrix;

import java.util.Vector;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Oct. 29, 2003
 *
 * Note:
 * A table row's position in matrix: position array dimension is 1 less than matrix array dimension.
 * eg. position={3,2} represents a table row contains matrix cells m(i,3,2), i=0,1...n
 */
public class Row {
    NDimensionalMatrix matrix;
    int dimension;
    int[] shape;

    //row position in matrix
    int[] position;

    //number of data cells per row
    int dataCellsPerRow;
    Vector dataContent = new Vector();
    Vector rowHeader = new Vector();

    //total, average, or percent column of this row, also is the end column
    float total;
    float average;
    float percent;

    /**
     * Row constructor
     * @param matrix represents a matrix which contains this row
     * @param position represents position of this row in the matrix
     * @param sTitles represents titles of shapes in each dimension
     */
    public Row(NDimensionalMatrix matrix, int[] position, Vector sTitles) {
        this.matrix = matrix;
        dataCellsPerRow = matrix.getShape(0);
        dimension = matrix.getDimensions();
        shape = matrix.getShape();
        this.position = position;

        //check if position is valid
        if (dimension != (position.length + 1)) {
            System.out.println(
                "position dimension and shape dimension don't match");
            System.exit(1);
        }

        //make data content, calculate total and average
        makeDataContent();

        //make row header
        makeRowHeader(sTitles);
    }

    /**
     * set percent field, percent is set from Table class, because to calculate it, we need sum of rows
     * @param percent represents the percentage to be set
     */
    public void setPercent(float percent) {
        this.percent = percent;
    }

    public Vector getDataContent() {
        return dataContent;
    }

    public Vector getRowHeader() {
        return rowHeader;
    }

    public float getTotal() {
        return total;
    }

    public float getAverage() {
        return average;
    }

    public float getPercent() {
        return percent;
    }

    public int[] getPosition() {
        return position;
    }

    /**
     * make data content, also calculate total and average
     */
    private void makeDataContent() {
        //array holds data cells in a row
        float[] dataCells = new float[dataCellsPerRow];

        //cell position in a matrix
        int[] cellPosition = new int[position.length + 1];

        //copy position to cellPosition
        for (int i = 0; i < position.length; i++) {
            cellPosition[i + 1] = position[i];
        }

        //set 1st dimension position to cellPosition
        //obtain cell value from matrix at this position
        //calculate total and average
        //add each data cell to dataContent object
        total = 0;
        average = 0;

        for (int i = 0; i < dataCellsPerRow; i++) {
            cellPosition[0] = i;
            dataCells[i] = matrix.getValue(cellPosition);
            total = total + dataCells[i];
            dataContent.add(new Float(dataCells[i]));
        }

        average = total / dataCellsPerRow;
    }

    /**
     * make row header
     * @param sTitles represents titles of shapes of each dimension
     */
    private void makeRowHeader(Vector sTitles) {
        //number of header cells in a row
        int headersPerRow = position.length;

        //if there are 3rd or higher dimensional shape titles, add all header items
        if (isRowWith3DPlusHeaders()) {
            //loop over headers titles (shape titles) in reverse order
            //skip shape(0), because shape(0) headers are listed as columns titles
            for (int i = headersPerRow - 1; i >= 0; i--) {
                //get ith dimension headers
                String[] tempHeader = (String[]) sTitles.get(i + 1);
                rowHeader.add(tempHeader[position[i]]);
            }

            //if only 2nd dimensional shape titles, only add one header item
        } else {
            for (int i = headersPerRow - 1; i >= 0; i--) {
                //get ith dimension headers
                String[] tempHeader = (String[]) sTitles.get(i + 1);

                if (i == 0) {
                    rowHeader.add(tempHeader[position[i]]);
                } else {
                    rowHeader.add(" ");
                }
            }
        }
    }

    private boolean isRowWith3DPlusHeaders() {
        boolean result = false;

        for (int i = 1; i < position.length; i++) {
            if (position[i - 1] == 0) {
                result = true;

                break;
            }
        }

        return result;
    }
}
