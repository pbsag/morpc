package com.pb.morpc.report;

import com.pb.common.matrix.NDimensionalMatrix;

import java.util.Vector;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Oct. 29, 2003
 */
public class Header {
    Vector header1 = new Vector();
    Vector header2 = new Vector();
    String tableName;
    int dataCellsPerRow;
    int rowHeaderCellsPerRow;
    int cellsPerRow;

    /**
     * Header constructor
     * @param tableName represents table name
     * @param matrix represents a matrix a table is built on
     * @param dTitles represents dimension titles of a matrix
     * @param shapeTitles represents titles of shapes of each dimension
     */
    public Header(String tableName, NDimensionalMatrix matrix,
        String[] dTitles, Vector shapeTitles) {
        this.tableName = tableName;
        dataCellsPerRow = matrix.getShape(0);
        rowHeaderCellsPerRow = matrix.getDimensions() - 1;

        //only data cells so far
        cellsPerRow = dataCellsPerRow + rowHeaderCellsPerRow;
        makeHeader1(dTitles);
        makeHeader2(shapeTitles);
    }

    /**
     * Update header.
     * A header is updated when a choice is make in writeTable() in Table class
     * @param choice represents print table choice
     * Note:
     * If choice is "Total", add "Total" to header 2, add 1 to cellsPerRow
     * If choice is "Average", add "Average" to header 2, add 1 to cellsPerRow
     * If choice is "Percent", add "Percent" to header 2, add 1 to cellsPerRow
     * If choice is "None", do nothing
     */
    public void updateHeader(String choice) {
        if (choice.equals("None")) {
            if (header2.size() == (rowHeaderCellsPerRow + dataCellsPerRow + 1)) {
                header2.remove(rowHeaderCellsPerRow + dataCellsPerRow);
            }
        } else {
            if (header2.size() == (rowHeaderCellsPerRow + dataCellsPerRow + 1)) {
                header2.set(rowHeaderCellsPerRow + dataCellsPerRow, choice);
            } else {
                header2.add(choice);
            }
        }

        cellsPerRow = header2.size();
    }

    public String getTableName() {
        return tableName;
    }

    public Vector getHeader1() {
        return header1;
    }

    public Vector getHeader2() {
        return header2;
    }

    public int getCellsPerRow() {
        return cellsPerRow;
    }

    public int getDataCellsPerRow() {
        return dataCellsPerRow;
    }

    public int getRowHeaderCellsPerRow() {
        return rowHeaderCellsPerRow;
    }

    /**
     * make header 1, which contains dimension titles
     * @param dTitles represents dimension titles
     */
    private void makeHeader1(String[] dTitles) {
        for (int i = 0; i < dTitles.length; i++) {
            header1.add(dTitles[dTitles.length - 1 - i]);
        }
    }

    /**
     * make header 2, which contains shape[0] titles
     * @param shapeTitles represents titles of shapes of each dimension
     */
    private void makeHeader2(Vector shapeTitles) {
        //only data cells, column cell will be updated from Table.writeTable()
        for (int i = 0; i < rowHeaderCellsPerRow; i++) {
            header2.add(" ");
        }

        String[] colTitles = (String[]) shapeTitles.get(0);

        for (int i = 0; i < dataCellsPerRow; i++) {
            header2.add(colTitles[i]);
        }
    }
}
