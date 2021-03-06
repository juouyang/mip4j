/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.report;

import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ju
 */
public class Immuno {

    static final String[] HER2_KW = new String[]{"HER2", "HER-2", "HER-2/neu"};
    static final String[] ER_KW = new String[]{
        "Estrogen Receptor",
        "ESTROGEN RECEPTOR",
        "(ER)"};
    static final String[] ER_END_KW = new String[]{
        "Progesterone Receptor",
        "PROGESTERONE RECEPTOR",
        "(PgR)",
        "HER2",
        "HER-2",
        "HER-2/neu",
        "Ki-67"};
    static final String[] PR_KW = new String[]{
        "Progesterone Receptor",
        "PROGESTERONE RECEPTOR",
        "(PgR)"};
    static final String[] PR_END_KEYWORDS = new String[]{
        "Estrogen Receptor",
        "ESTROGEN RECEPTOR",
        "(ER)",
        "HER2",
        "HER-2",
        "HER-2/neu",
        "Ki-67"};
    static final int ER_SEARCHWIN = 3;
    static final int PR_SEARCHWIN = 3;

    public static int parseER(String erText) {
        int ret = Integer.MIN_VALUE;
        Scanner fi = new Scanner(erText);
        fi.useDelimiter("[^\\p{Alnum},\\.-]");
        while (true) {
            if (fi.hasNextInt()) {
                ret = fi.nextInt();
            } else if (fi.hasNextDouble()) {
                ret = (int) fi.nextDouble();
            } else if (fi.hasNext()) {
                String tmp = fi.next();
                if (StringUtils.containsIgnoreCase(tmp, "negative")) {
                    ret = 0;
                }
            } else {
                break;
            }
        }
        if (ret == 1) {
            if (erText.contains(">=1")) {
                ret = Integer.MIN_VALUE;
            }
            if (erText.contains("<1")) {
                ret = 0;
            }
        }

        return ret;
    }

    public static int parsePR(String prText) {
        int ret = Integer.MIN_VALUE;
        Scanner fi = new Scanner(prText);
        fi.useDelimiter("[^\\p{Alnum},\\.-]");
        while (true) {
            if (fi.hasNextInt()) {
                ret = fi.nextInt();
            } else if (fi.hasNextDouble()) {
                ret = (int) fi.nextDouble();
            } else if (fi.hasNext()) {
                String tmp = fi.next();
                if (StringUtils.containsIgnoreCase(tmp, "negative")) {
                    ret = 0;
                }
            } else {
                break;
            }
        }
        if (ret == 1) {
            if (prText.contains(">=1")) {
                ret = Integer.MIN_VALUE;
            }
            if (prText.contains("<1")) {
                ret = 0;
            }
        }

        return ret;
    }

    public static int parseHER2(String her2Text) {
        int ret = Integer.MIN_VALUE;
        Scanner fi = new Scanner(her2Text);
        fi.useDelimiter("[^\\p{Alnum},\\.-]");
        while (true) {
            if (fi.hasNextInt()) {
                ret = fi.nextInt();
            } else if (fi.hasNextDouble()) {
                ret = (int) fi.nextDouble();
            } else if (fi.hasNext()) {
                fi.next();
            } else {
                break;
            }
        }

        return ret;
    }

    public static double parseKi67(String ki67Text) {
        double ret = Double.MIN_VALUE;
        Scanner fi = new Scanner(ki67Text);
        fi.useDelimiter("[^\\p{Alnum},\\.-]");
        while (true) {
            if (fi.hasNextInt()) {
                ret = fi.nextInt();
            } else if (fi.hasNextDouble()) {
                ret = fi.nextDouble();
            } else if (fi.hasNext()) {
                fi.next();
            } else {
                break;
            }
        }

        return ret;
    }

    public int er = Integer.MIN_VALUE;
    public int pr = Integer.MIN_VALUE;
    public int her2 = Integer.MIN_VALUE;
    public double ki67 = Double.MIN_VALUE;
    public String text;

    void setER(int er) {
        this.er = er;
    }

    void setPR(int pr) {
        this.pr = pr;
    }

    void setHER2(int her2) {
        this.her2 = her2;
    }

    void setKi67(double ki67) {
        this.ki67 = ki67;
    }

    void setImmunoText(String immunoText) {
        this.text = immunoText;
    }

    public boolean hasValue() {
        return !(her2 == Integer.MIN_VALUE && ki67 == Double.MIN_VALUE && er == Integer.MIN_VALUE && pr == Integer.MIN_VALUE);
    }

    public String getHER2() {
        return her2 == Integer.MIN_VALUE ? "-" : "" + her2;
    }

    public String getKi67() {
        return ki67 == Double.MIN_VALUE ? "-" : "" + ki67;
    }

    public String getER() {
        return er == Integer.MIN_VALUE ? "-" : "" + er;
    }

    public String getPR() {
        return pr == Integer.MIN_VALUE ? "-" : "" + pr;
    }

    public String toCSVString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append(her2 == Integer.MIN_VALUE ? "-" : her2).append(",");
        sb.append(ki67 == Double.MIN_VALUE ? "-" : ki67).append(",");
        sb.append(er == Integer.MIN_VALUE ? "-" : er).append(",");
        sb.append(pr == Integer.MIN_VALUE ? "-" : pr);

        return sb.toString();
    }
}
