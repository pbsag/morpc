package com.pb.morpc.report;

import java.util.Vector;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Nov. 5 2003
 */
public class SumRow {
    Vector dataContent = new Vector();
    Vector rowHeader = new Vector();
    float total;
    float average;
    int dataCellsPerRow;
    int rowHeadersPerRow;
    int sumRowPosition;
    String type;

    /**
     * SumRow constructor
     * @param dataContent represnets data content of this row
     * @param rowHeadersPerRow represents number of header elements in a row
     * @param sumRowPosition represents this row's position in a table
     * @param type represents row type, either "Average" or "Total"
     */
    public SumRow(Vector dataContent, int rowHeadersPerRow, int sumRowPosition,
        String type) {
        this.dataContent = dataContent;
        this.rowHeadersPerRow = rowHeadersPerRow;
        this.sumRowPosition = sumRowPosition;
        this.type = type;
        dataCellsPerRow = dataContent.size();
        calculateTotAvg();
        makeRowHeader();
    }

    /**
     * get row headers
     * @return
     */
    public Vector getRowHeader() {
        return rowHeader;
    }

    /**
     * get data content
     * @return
     */
    public Vector getDataContent() {
        return dataContent;
    }

    /**
     * get average
     * @return
     */
    public float getAverage() {
        return average;
    }

    /**
     * get total
     * @return
     */
    public float getTotal() {
        return total;
    }

    /**
     * get sum row position in a table
     * @return
     */
    public int getSumRowPosition() {
        return sumRowPosition;
    }

    /**
     * get sum row type
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * calculate total and average for this sum row
     */
    private void calculateTotAvg() {
        total = 0;

        for (int i = 0; i < dataCellsPerRow; i++) {
            total = total + ((Float) dataContent.get(i)).floatValue();
        }

        average = total / dataCellsPerRow;
    }

    /**
     * make header in a row
     */
    private void makeRowHeader() {
        for (int i = 0; i < rowHeadersPerRow; i++) {
            //add row type to last row header element
            if (i == (rowHeadersPerRow - 1)) {
                rowHeader.add(type);
            }
            //add empty to other row header elements
            else {
                rowHeader.add("");
            }
        }
    }
}
