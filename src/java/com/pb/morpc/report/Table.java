package com.pb.morpc.report;

import com.pb.common.matrix.NDimensionalMatrix;

import java.io.*;

import java.util.Vector;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Oct. 29, 2003
 *
 * Note:
 *
 * Table Parts:
 * 1) A table is divided into a header, regular rows, and summary rows.
 * 2) A header has three elements, table name, header 1 which contains dimension titles,
 * and header 2 which contains shape[0] titles.
 * 3) A regular row has a row header, a data content, a total field, an average filed and a percent field.
 * 4) A summary row contains either total information or average information.
 * A summary row has a row header, a data content, a total field, and an average field.
 *
 * Table Types:
 * 1) Total table: a table with total rows and a total column
 * 2) Average table: a table with average rows and an average column
 * 3) Percent table: a table with total rows and a percent column
 * 4) None table: a table with no summary rows, no summary column at the end
 *
 * Table Classes:
 * Corresponding classes for these table parts are:
 * Table, Header, Row, and SumRow
 * An assistant class ReportPropertyParser is designed to parse .properties file
 *
 * Report Property File:
 * For each report, there is a corresponding .properties file.
 * Refer to ReportPropertyParser for details.
 *
 * A Table Example:
 *
 *  (table name)                                  Autos By HHSize
 * =========================================================================================================
 *  (Header 1) HHSize                                   Autos                                  (Summary col.)
 * ---------------------------------------------------------------------------------------------------------
 *  (Header 2)             1              2              3              4              5          Total
 * =========================================================================================================
 *  (Row)      1        28407.0        52104.0        78235.0         4938.0           76.0       163760.0
 * ---------------------------------------------------------------------------------------------------------
 *             2        11906.0        70702.0        66788.0        32000.0         8697.0       190093.0
 *  ---------------------------------------------------------------------------------------------------------
 *             3         3164.0        33578.0        31682.0        13041.0        11766.0        93231.0
 * ---------------------------------------------------------------------------------------------------------
 *             4         1913.0        27061.0        27061.0        12200.0        10484.0        78719.0
 * ---------------------------------------------------------------------------------------------------------
 *             5          855.0        11107.0        12497.0         5232.0         4944.0        34635.0
 * ---------------------------------------------------------------------------------------------------------
 *            6+          311.0         4025.0         5467.0         1873.0         2347.0        14023.0
 * ---------------------------------------------------------------------------------------------------------
 *(Sum Row)Total        46556.0       198577.0       221730.0        69284.0        38314.0       574461.0
 * =========================================================================================================
 */
public class Table {
    private NDimensionalMatrix matrix;
    private Header header;
    private Vector rows = new Vector();
    private Vector totRows = new Vector();
    private Vector avgRows = new Vector();

    /**
     * Table Constructor
     * @param matrix represents a n-dimensional matrix to be converted to a 2-dimensional table
     * @param dTitles represents dimension titles
     * @param sTitles represents shape titles of each dimension
     */
    public Table(NDimensionalMatrix matrix, String tableName, String[] dTitles,
        Vector sTitles) {
        this.matrix = matrix;
        this.header = new Header(tableName, matrix, dTitles, sTitles);
        this.rows = makeRows(matrix, sTitles);
    }

    /**
     * get table headers
     * @return
     */
    public Header getHeader() {
        return header;
    }

    /**
     * get regular rows
     * @return
     */
    public Vector getRows() {
        return rows;
    }

    /**
     * get total rows
     * @return
     */
    public Vector getTotRows() {
        return totRows;
    }

    /**
     * get average rows
     * @return
     */
    public Vector getAvgRows() {
        return avgRows;
    }
    
    public void writeLogging(String file, String logging){
        PrintWriter outStream;
        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(file,true)));

            //print logging infor
            outStream.print(logging);
            outStream.println("\n");
            //close outStream
            outStream.close();
        } catch (IOException e) {
        	System.out.println("can not write logging information");
        	System.exit(1);
        }
    }

    /**
     * write talbe to hard disk
     * @param file represents location to write table to
     * @param format represents cell format in this table, eg. "%10s", "%8.2f"
     * @param choice
     */
    public void writeTable(String file, String format, String choice,
        String formatline) {
        //update header using choice
        header.updateHeader(choice);

        int[] shape = matrix.getShape();
        PrintWriter outStream;
        String tableName = header.getTableName();
        Vector header1 = header.getHeader1();
        Vector header2 = header.getHeader2();
        Vector sumRows = new Vector();
        int cellsPerRow = header.getCellsPerRow();
        int dataCellsPerRow = header.getDataCellsPerRow();
        int rowHeaderCellsPerRow = header.getRowHeaderCellsPerRow();
        int formatLen = parseFormatLength(format);
        int tableNameFormatLen = ((formatLen * cellsPerRow) +
            tableName.length()) / 2;

        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(file,true)));

            //print table name
            outStream.println("\n");
            //comment out if table name not formatted
            //outStream.print(String.format("%" + tableNameFormatLen + "s",tableName));
            outStream.print(tableName);
            outStream.println("\n");

            if (formatline.equals("Yes")) {
                outStream.println(makeDivLine(format, cellsPerRow, '='));
            }

            //If not frequency table, print header 1
            //If frequency table (only 1 data cell per row), don't print header 1
            if (dataCellsPerRow > 1) {
                //make new format for header elements covering data cells
                for (int i = 0; i < header1.size(); i++) {
                    if (i >= rowHeaderCellsPerRow) {
                        //center header element
                        formatLen = ((dataCellsPerRow + 1) / 2) * formatLen;

                        //make new format
                        String tempformat = "%" + formatLen + "s";
                        outStream.print(String.format(tempformat,
                                (String) header1.get(i)));
                    } else {
                        outStream.print(String.format(format,
                                (String) header1.get(i)));
                    }
                }

                outStream.println("\n");

                if (formatline.equals("Yes")) {
                    outStream.println(makeDivLine(format, cellsPerRow, '-'));
                }
            }

            //Print header 2
            for (int i = 0; i < header2.size(); i++) {
                outStream.print(String.format(format, (String) header2.get(i)));
            }

            outStream.println("\n");

            if (formatline.equals("Yes")) {
                outStream.println(makeDivLine(format, cellsPerRow, '='));
            }

            //Print rows
            for (int i = 0; i < rows.size(); i++) {
                //print regular rows
                Row row = (Row) rows.get(i);
                Vector rowHeader = row.getRowHeader();
                Vector dataContent = row.getDataContent();

                //print row headers
                for (int j = 0; j < rowHeader.size(); j++) {
                    outStream.print(String.format(format,
                            (String) rowHeader.get(j)));
                }

                //print data content
                for (int j = 0; j < dataContent.size(); j++) {
                    outStream.print(String.format(format,
                            new Integer(
                                (((Float) dataContent.get(j)).intValue())).toString()));
                }

                //if type is "Percent" print percent field
                //if type is "Average" print average field
                //if type is "Total" print total field
                //else do nothing
                if (choice.equals("Percent")) {
                    outStream.print(String.format(format,
                            (new Float(row.getPercent())).toString()));
                } else if (choice.equals("Average")) {
                    outStream.print(String.format(format,
                            (new Integer(
                                (new Float(row.getAverage())).intValue())).toString()));
                } else if (choice.equals("Total")) {
                    outStream.print(String.format(format,
                            (new Integer((new Float(row.getTotal())).intValue())).toString()));
                }

                outStream.println("\n");

                if (formatline.equals("Yes")) {
                    if ((i == (rows.size() - 1)) && choice.equals("None")) {
                        outStream.println(makeDivLine(format, cellsPerRow, '='));
                    } else {
                        outStream.println(makeDivLine(format, cellsPerRow, '-'));
                    }
                }

                //If current row is multplier of 2nd dimension shape
                //and current row is not 0
                //and choice is either "Total" or "Average"
                //Print sum row (either total row or average row)
                //If choice is "Percent", print percent column, and total sum rows
                //If choice is "None", not print sum column and sum rows
                if ((i != 0) && ((((i + 1) / shape[1]) * shape[1]) == (i + 1)) &&
                        (!choice.equals("None"))) {
                    if (choice.equals("Total") || choice.equals("Percent")) {
                        sumRows = totRows;
                    } else {
                        sumRows = avgRows;
                    }

                    //loop over sumRows to find the one to print
                    for (int j = 0; j < sumRows.size(); j++) {
                        SumRow srow = (SumRow) sumRows.get(j);

                        //if current sum row is the one to be printed
                        if (srow.getSumRowPosition() == i) {
                            Vector sumRowHeader = srow.getRowHeader();
                            Vector sumRowDataContent = srow.getDataContent();
                            float average = srow.getAverage();
                            float total = srow.getTotal();

                            //print sum row header
                            for (int m = 0; m < sumRowHeader.size(); m++) {
                                outStream.print(String.format(format,
                                        (String) sumRowHeader.get(m)));
                            }

                            //print sum row data content
                            for (int m = 0; m < sumRowDataContent.size();
                                    m++) {
                                outStream.print(String.format(format,
                                        new Integer(
                                            ((Float) sumRowDataContent.get(m)).intValue()).toString()));
                            }

                            //print sum row end column
                            if (choice.equals("Percent")) {
                                outStream.print(String.format(format, "100.00"));
                            } else if (choice.equals("Average")) {
                                outStream.print(String.format(format,
                                        (new Integer(
                                            (new Float(average)).intValue())).toString()));
                            } else {
                                outStream.print(String.format(format,
                                        (new Integer(
                                            (new Float(total)).intValue())).toString()));
                            }

                            outStream.println("\n");

                            if (formatline.equals("Yes")) {
                                outStream.println(makeDivLine(format,
                                        cellsPerRow, '='));
                            }
                        }
                    }
                }
            }

            //close outStream
            outStream.close();
        } catch (IOException e) {
            System.out.println("can not write table to hard drive");
            System.exit(1);
        }
    }

    /**
     * Make table rows, including regular, total, and average rows.
     * @param matrix represents n-dimensional matrix
     * @param sTitles represents shape titles of each dimension
     * @return a vector of regular rows in a table
     */
    private Vector makeRows(NDimensionalMatrix matrix, Vector sTitles) {
        //matrix dimension
        int dimension = matrix.getDimensions();

        //matrix shape
        int[] shape = matrix.getShape();

        //total number of cells in matrix
        int NoOfCells = shapeProduct(shape, dimension - 1);

        //rows in a table
        Vector rows = new Vector();

        //rows for one summary operation
        Row[] rowsToSum = new Row[shape[1]];

        //number of table rows processed so far
        int rowsProcessed = 0;

        //number of matrix cells processed so far
        int cellsProcessed = 0;

        for (int m = 0; m < NoOfCells; m++) {
            //index of cell being processed
            int cellIndex = m;

            //row position of current cell
            int[] rowPosition = new int[dimension - 1];

            //p=shape[dimension-2]*shape[dimension-3]...shape[0]
            int p = shapeProduct(shape, dimension - 2);

            //increasing index for updating p
            int index = 0;

            cellsProcessed++;

            //calculate row position of current cell, row position is calculated in index decreasing order
            for (int i = dimension - 2; i >= 0; i--) {
                rowPosition[i] = cellIndex / p;
                index++;

                //if cells processed is multiplier of 1st dimension shape
                //and if cells processed is not 0
                //and if rowPosition array of current cell is complete
                //make a new row, add it to a vector, increase rowProcessed by 1
                if ((((cellsProcessed / shape[0]) * shape[0]) == cellsProcessed) &&
                        (cellsProcessed != 0) && (i == 0)) {
                    Row temp = new Row(matrix, rowPosition, sTitles);
                    rows.add(temp);

                    rowsToSum[rowsProcessed -
                    ((rowsProcessed / shape[1]) * shape[1])] = temp;
                    rowsProcessed++;

                    //if rows processed is multiplier of 2nd dimension shape
                    //and if rows processed is not 0
                    //make a sum row, and it to sumRows vector
                    //calculate percentage of each row contained in this summary,
                    //make a avg row and add it to AvgRows vector
                    if ((((rowsProcessed / shape[1]) * shape[1]) == rowsProcessed) &&
                            (rowsProcessed != 0)) {
                        SumRow tempTot = makeTotRow(rowsToSum, dimension,
                                rowsProcessed - 1, shape[0]);

                        for (int k = 0; k < rowsToSum.length; k++) {
                            Float tempf = new Float(Math.round(
                                        (rowsToSum[k].getTotal() / tempTot.getTotal()) * 10000));
                            float percent = tempf.floatValue() / 100;
                            rowsToSum[k].setPercent(percent);
                        }

                        totRows.add(tempTot);

                        SumRow tempAvg = makeAvgRow(rowsToSum, dimension,
                                rowsProcessed - 1, shape[0]);
                        avgRows.add(tempAvg);
                    }
                }

                //update cellIndex and p for calculating rowPosition
                cellIndex = cellIndex - (rowPosition[i] * p);
                p = shapeProduct(shape, dimension - 2 - index);
            }
        }

        return rows;
    }

    /**
     * Caculate product of shape []: p=shape[n]*shape[n-1]*...*shape[0]
     * @param shape represents matrix shape
     * @param n represents starting dimension
     * @return product
     */
    private int shapeProduct(int[] shape, int n) {
        int result = 1;

        for (int i = n; i >= 0; i--) {
            result = result * shape[i];
        }

        return result;
    }

    /**
     * Make a total row
     * @param rowsToSum represents rows included in this summary
     * @param dimensions represents matrix dimensions
     * @param sumRowPosition represents position of this sum row
     * @param noOfDataCells represents number of data cells in this sum row
     * @return a SumRow object
     */
    private SumRow makeTotRow(Row[] rowsToSum, int dimensions,
        int sumRowPosition, int noOfDataCells) {
        Vector dataContent = new Vector();
        float[] dataCells = new float[noOfDataCells];

        //populate data cells of this row
        for (int i = 0; i < rowsToSum.length; i++) {
            Vector temp = rowsToSum[i].getDataContent();

            for (int j = 0; j < noOfDataCells; j++) {
                dataCells[j] = dataCells[j] +
                    ((Float) temp.get(j)).floatValue();
            }
        }

        //add data cells to a dataContent object
        for (int i = 0; i < noOfDataCells; i++) {
            dataContent.add(new Float(dataCells[i]));
        }

        //make a sum row
        SumRow sr = new SumRow(dataContent, dimensions - 1, sumRowPosition,
                "total");

        return sr;
    }

    /**
     * Caculate an average row
     * @param rowsToAvg represents rows included in this summary
     * @param dimensions represents matrix dimensions
     * @param avgRowPosition represents position of this sum row
     * @param noOfDataCells represents number of data cells in this sum row
     * @return a SumRow object
     */
    private SumRow makeAvgRow(Row[] rowsToAvg, int dimensions,
        int avgRowPosition, int noOfDataCells) {
        Vector dataContent = new Vector();
        float[] dataCells = new float[noOfDataCells];

        //populate data cells in this row
        for (int i = 0; i < rowsToAvg.length; i++) {
            Vector temp = rowsToAvg[i].getDataContent();

            for (int j = 0; j < noOfDataCells; j++) {
                dataCells[j] = dataCells[j] +
                    ((Float) temp.get(j)).floatValue();
            }
        }

        //add data cells to a dataContent object
        for (int i = 0; i < noOfDataCells; i++) {
            Float tempf = new Float(Math.round(
                        (dataCells[i] / rowsToAvg.length) * 100));
            dataCells[i] = tempf.floatValue() / 100;
            dataContent.add(new Float(dataCells[i]));
        }

        //make a sum row
        SumRow ar = new SumRow(dataContent, dimensions - 1, avgRowPosition,
                "Average");

        return ar;
    }

    /**
     * Parse format length
     * @param format represents a format string
     * @return format length
     */
    private int parseFormatLength(String format) {
        int result = 0;
        int numEndIndex = format.indexOf("s") - 1;
        String length = format.substring(1, numEndIndex + 1);
        result = (new Integer(length)).intValue();

        return result;
    }

    /**
     * Make a dividen line
     * @param format represent a format string
     * @param cellsPerRow represents number of cells per row
     * @param c represents a character which is the basic unit of a dividen line
     * @return a dividen line
     */
    private String makeDivLine(String format, int cellsPerRow, char c) {
        StringBuffer buffer = new StringBuffer();
        int NoChars = cellsPerRow * parseFormatLength(format);

        for (int i = 0; i < NoChars; i++) {
            buffer.append(c);
        }

        return buffer.toString();
    }
}
