package com.pb.morpc.report;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * <p>Company: PB Consult, Parsons Brinckerhoff</p>
 * @author Wu Sun
 * @version 1.0, Nov. 3, 2003
 *
 * Note:
 * For each report there is a corresponding .properties file, which contains:
 * 1) report.location, e.g. c:/model/2_Alts/2000B/8_report/2_hadp/auto_income.txt
 * 2) report.format, e.g. %15s
 * 3) matrix.name, e.g. Autos By Income
 * 4) dimension.titles, e.g. Autos,Income (if is a frequency table, use  , )
 * 5) shape.titles, e.g. 1,2,3,4,5|low,medium,high (if is a frequency table, use Frequency|cat1,cat2,...)
 * 6) shape.values, e.g. 1,2,3,4,5|1,2,3 (if is a frequency talbe, use 1|cat1Val, cat2Val, ...)
 * 7) report.type, 4 types: Total, Average, Percent, and None (if is a frequency table, use Percent)
 * 8) format.line, Yes or No.
 *
 * Sample .properties file:
 * report.location=c:/model/2_Alts/2000B/8_report/2_hadp/auto_income.txt
 * report.format=%15s
 * matrix.name=Autos By Income
 * dimension.titles=Autos,Income
 * shape.titles=1,2,3,4,5|low,medium,high
 * shape.values=1,2,3,4,5|1,2,3
 * report.type=Total
 * format.line=Yes
 */
public class ReportPropertyParser {
    HashMap property;

    /**
     * Constructor
     * @param property represents a property hash map
     */
    public ReportPropertyParser(HashMap property) {
        this.property = property;

        if (!isPropertyValid()) {
            System.out.println("property not valid");
            System.exit(1);
        }
    }

    /**
     * Check if .properties is well constructed
     * @return
     */
    private boolean isPropertyValid() {
        boolean result = true;

        if (getMatrixDimensions() != (getShapeValues()).size()) {
            result = false;
            System.out.println("dimension and shape don't match");
        }

        if ((getShapeValues()).size() != (getShapeTitles()).size()) {
            result = false;
            System.out.println("shape values and titles don't match");
        }

        return result;
    }

    /**
     * get dimension titles
     * @return
     */
    public String[] getDimensionTitles() {
        StringTokenizer st = new StringTokenizer((String) property.get(
                    "dimension.titles"), ",");
        String[] result = new String[st.countTokens()];
        int index = 0;

        while (st.hasMoreTokens()) {
            result[index] = st.nextToken();
            index++;
        }

        return result;
    }

    /**
     * get matrix shape values
     * @return
     */
    public Vector getShapeValues() {
        Vector result = new Vector();
        StringTokenizer st1 = new StringTokenizer((String) property.get(
                    "shape.values"), "|");

        while (st1.hasMoreTokens()) {
            String token = st1.nextToken();
            StringTokenizer st2 = new StringTokenizer(token, ",");
            String[] temp = new String[st2.countTokens()];
            int[] temp_int = new int[st2.countTokens()];
            int index = 0;

            while (st2.hasMoreTokens()) {
                temp[index] = st2.nextToken();
                temp_int[index] = new Integer(temp[index]).intValue();
                index++;
            }

            result.add(temp_int);
        }

        return result;
    }

    /**
     * get matrix shape titles
     * @return
     */
    public Vector getShapeTitles() {
        Vector result = new Vector();
        StringTokenizer st1 = new StringTokenizer((String) property.get(
                    "shape.titles"), "|");

        while (st1.hasMoreTokens()) {
            String token = st1.nextToken();
            StringTokenizer st2 = new StringTokenizer(token, ",");
            String[] temp = new String[st2.countTokens()];
            int index = 0;

            while (st2.hasMoreTokens()) {
                temp[index] = st2.nextToken();
                index++;
            }

            result.add(temp);
        }

        return result;
    }

    /**
     * get matrix shape
     * @return
     */
    public int[] getMatrixShape() {
        Vector shapeTitles = getShapeTitles();
        int[] result = new int[shapeTitles.size()];

        for (int i = 0; i < shapeTitles.size(); i++) {
            result[i] = ((String[]) shapeTitles.get(i)).length;
        }

        return result;
    }

    /**
     * get matrix dimensions
     * @return
     */
    public int getMatrixDimensions() {
        StringTokenizer st = new StringTokenizer((String) property.get(
                    "dimension.titles"), ",");

        return st.countTokens();
    }

    /**
     * get matrix name
     * @return
     */
    public String getMatrixName() {
        return (String) property.get("matrix.name");
    }

    /**
     * get report location
     * @return
     */
    public String getReportLocation() {
        return (String) property.get("report.file");
    }

    /**
     * get table cell format in a report
     * @return
     */
    public String getFormat() {
        return (String) property.get("report.format");
    }

    /**
     * get format line choice in a report, "Yes" or "No"
     * @return
     */
    public String getFormatLine() {
        return (String) property.get("format.line");
    }

    /**
     * get report type
     * @return
     */
    public String getType() {
        return (String) property.get("report.type");
    }
}
