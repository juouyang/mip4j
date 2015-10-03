/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.model.data.bmr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 *
 * @author ju
 */
enum Tag {

    pname("姓名"),
    id("身份證"),
    pid("體檢號"),
    date("檢查日期"),
    hos("就診地點"),
    sid("檢查號碼"),
    density("乳房組成形態"),
    lesion_no("病灶編號"),
    slice_no("軸狀切面張數"),
    birads("BIRADS分類");

    private final String t;

    private Tag(String s) {
        t = s;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : t.equals(otherName);
    }

    public static Tag getTag(String name) {
        for (Tag t : Tag.values()) {
            if (t.equalsName(name)) {
                return t;
            }
            if (name.endsWith(t.toString()) && (t == Tag.density || t == Tag.slice_no)) {
                return t;
            }
            if (name.contains("組成形態") && t == Tag.density) {
                return t;
            }
            if (t.toString().indexOf(name) == 1) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.t;
    }
}

enum Side {

    Left,
    Right;
}

class Lesion {

    public int number;
    public Side side;
    public int sliceStart;
    public int sliceEnd;
    public String birads;
    public String lesionType;

    public Lesion(int no, Side s) {
        number = no;
        side = s;
    }

    @Override
    public String toString() {
        return "Lesion {"
                + "number = [" + number + "]"
                + ", side = [" + side + "]"
                + ", sliceStart = [" + sliceStart + "]"
                + ", sliceEnd = [" + sliceEnd + "]"
                + ", birads = [" + birads + "]"
                + ", lesionType = [" + lesionType + "]"
                + "}";
    }

}

public class BMRReport {

    public static boolean DEBUG = false;

    private static final String COLON_DELIMITER = "：";
    private static final String LEFT_BREAST = "左乳";
    private static final String RIGHT_BREAST = "右乳";
    private static final String REPORT_START0 = "磁振造影";
    private static final String REPORT_START1 = "打顯影劑";
    private static final String REPORT_START2 = "三度空間";
    private static final String REPORT_END = "報告總結";
    private static final DateFormat df0 = new SimpleDateFormat("yyyy/MM/dd");
    private static final DateFormat df1 = new SimpleDateFormat("MM/dd/yyyy");
    private static final DateFormat df2 = new SimpleDateFormat("yyyyMM/dd");
    private static final DateFormat df3 = new SimpleDateFormat("yyyy/MM//dd");
    private static final DateFormat df4 = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat df5 = new SimpleDateFormat("yyyy.MM.dd");

    private final String pdfFilePath;

    public String patientName;
    public String personID;
    public String patientID;
    public Date studyDate;
    public String studyID;
    public String hospital;
    public String density;

    private int lesionID = 100; // for those doesn't have lesion No.
    private int lesionCount = 0;
    private int sliceStart = 0;
    private int sliceEnd = 0;
    private String birads_code;
    private String lesion_type;
    private final Map<Integer, Lesion> lesionList = new HashMap<>();

    private boolean isRightBreast = false;
    private boolean isReportStart = false;
    private String previousLine;

    public boolean hasParseError = false;
    private boolean skipThisLine = false;

    private String preProcess(String s) {
        return s.replaceAll(":", COLON_DELIMITER)
                .replaceAll("；", "\t")
                .replaceAll("<", " <\t")
                .replaceAll("/", " /")
                .replaceAll("--", COLON_DELIMITER)
                .replaceAll("TMHH", "TMUH")
                .replaceAll("STMH", "SMHT")
                .replaceAll("20009", "2009")
                .replaceAll("：軸狀切面張數", " 軸狀切面張數")
                .replaceAll("：乳房組成形態：", "\t乳房組成形態：")
                .replaceAll("：乳房組成形態：", "\t乳房組成形態：")
                .replaceAll("組成形態屬於", "組成形態屬於：")
                .replaceAll("BIRADS ?分類", "\tBIRADS分類：")
                .replaceAll("BIRADS", (s.contains(Tag.lesion_no.toString()) && s.contains(Tag.slice_no.toString()) && !s.contains("分類")) ? "BIRADS分類：" : "BIRADS")
                .replaceAll("BI-RADS", (s.contains(Tag.lesion_no.toString()) && s.contains(Tag.slice_no.toString()) && !s.contains("分類")) ? "BIRADS分類：" : "BI-RADS")
                .replaceAll("BI-RADS ?分類", "\tBIRADS分類")
                .replaceAll("：：", "：").trim();
    }

    public BMRReport(String pdfFile) throws IOException {
        pdfFilePath = pdfFile;
        PDFParser parser = new PDFParser(new FileInputStream(new File(pdfFile)));
        parser.parse();
        COSDocument cosDoc = parser.getDocument();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        try (PDDocument pdDoc = new PDDocument(cosDoc)) {
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(10);
            String parsedText = pdfStripper.getText(pdDoc);
            String[] lines = parsedText.split(System.getProperty("line.separator"));
            int i = 0;

            for (String s : lines) {
                if (s.trim().length() == 0) {
                    continue;
                }

                if (DEBUG) {
                    System.out.println(++i + ":\t" + s);
                }

                s = preProcess(s);
                DBG_TAG("前處理", s);

                if (s.contains(REPORT_START1) && s.contains(REPORT_START2)) {
                    DBG_TAG("報告開始", s);
                    isReportStart = true;
                }

                if (s.contains(REPORT_START0) && s.contains("結果")) {
                    DBG_TAG("報告開始", s);
                    isReportStart = true;
                }

                if (s.contains(LEFT_BREAST) && isReportStart) {
                    s = s.substring(s.indexOf(LEFT_BREAST) + 2);
                    if (s.indexOf(COLON_DELIMITER) == 0) {
                        s = s.substring(1);
                    }
                    DBG_TAG("左乳", s);
                    lesion_type = (s.length() > 4 && !s.contains(Tag.slice_no.toString())) ? preCleanupLesionType(s) : null;
                    isRightBreast = false;
                }

                if (s.contains(RIGHT_BREAST) && isReportStart) {
                    s = s.substring(s.indexOf(RIGHT_BREAST) + 2);
                    if (s.indexOf(COLON_DELIMITER) == 0) {
                        s = s.substring(1);
                    }
                    DBG_TAG("右乳", s);
                    lesion_type = (s.length() > 4 && !s.contains(Tag.slice_no.toString())) ? preCleanupLesionType(s) : null;
                    isRightBreast = true;
                }

                if (s.contains(REPORT_END) || s.contains("國際乳房腫瘤通用的分級方法")) {
                    if (lesionCount != lesionList.size()) {
                        hasParseError = true;
                    }
                    DBG_TAG("報告結束", s);
                    isReportStart = false;
                    break;
                }

                parseTagData(s);
                parseLesionInfo(s);

                if (!skipThisLine) {
                    previousLine = s;
                }
                skipThisLine = false;
            }
        }
    }

    private String preCleanupLesionType(String s) {
        s = s.replaceAll("\\* ?", "*");
        if (s.contains("\t")) {
            s = s.substring(0, s.indexOf("\t"));
        }
        if (s.contains(" ")) {
            s = s.substring(0, s.indexOf(" "));
        }
        if (s.endsWith(COLON_DELIMITER)) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.contains("，")) {
            s = s.substring(0, s.indexOf("，"));
        }

        return (s.length() >= 1 && s.charAt(0) == '*') ? s.substring(1).replaceAll("：", "") : s;
    }

    private void parseLesionInfo(String s) {
        if (s.length() == 0) {
            return;
        }

        if (s.contains("*鑑別診斷")) {
            skipThisLine = true;
            return;
        }

        Lesion l = null;

        if (s.charAt(0) == '*') {
            if (!s.contains("非") && s.contains("塊狀病灶") && s.indexOf("塊狀病灶") != 1) {
                s = "*塊狀病灶";
            }
            if (s.contains("囊腫") && s.indexOf("囊腫") != 1) {
                s = "*囊腫";
            }

            String t = preCleanupLesionType(s);
            DBG_TAG("腫瘤型態1", t);

            if (previousLine.contains(Tag.slice_no.toString())) {

                if (lesionCount != 0 && previousLine.contains(Tag.lesion_no.toString())) {
                    l = getLesion(lesionCount);
                } else {
                    l = getLesion(lesionID++);
                }

                if (l.lesionType == null && l.birads == null) {
                    l.lesionType = l.lesionType == null ? t : l.lesionType + t;
                    l.birads = l.birads == null ? birads_code : l.birads + birads_code;
                    l.sliceStart = sliceStart;
                    l.sliceEnd = sliceEnd;

                    birads_code = null;
                    sliceStart = 0;
                    sliceEnd = 0;
                }
            }
        } else if (lesion_type != null && s.contains(Tag.lesion_no.toString())) {
            String t = lesion_type;

            if (s.contains(Tag.slice_no.toString())) {
                if (t.contains("囊腫") && t.indexOf("囊腫") != 0) {
                    t = "囊腫";
                }

                DBG_TAG("腫瘤型態2", t);

                if (lesionCount != 0 && previousLine.contains(Tag.lesion_no.toString())) {
                    l = getLesion(lesionCount);
                } else {
                    l = getLesion(lesionID++);
                }

                l.lesionType = l.lesionType == null ? t : l.lesionType + t;
                l.birads = l.birads == null ? birads_code : l.birads + birads_code;
                l.sliceStart = sliceStart;
                l.sliceEnd = sliceEnd;

                lesion_type = null;
                birads_code = null;
                sliceStart = 0;
                sliceEnd = 0;
            }
        } else if (previousLine != null && previousLine.contains(Tag.slice_no.toString()) && previousLine.contains(Tag.lesion_no.toString()) && previousLine.contains(Tag.birads.toString())) {
            if (lesionCount != 0 && previousLine.contains(Tag.lesion_no.toString())) {
                l = getLesion(lesionCount);
            } else {
                l = getLesion(lesionID++);
            }

            if (s.contains("囊腫") && s.indexOf("囊腫") != 0) {
                s = "囊腫";
            }

            int pageNumber = 0;
            try {
                pageNumber = Integer.parseInt(s.trim());
            } catch (Exception ignore) {
            }

            if (s.contains(Tag.pname.toString()) && s.contains(Tag.date.toString()) || pageNumber != 0 || s.contains("標示") || s.contains("大小") || s.contains("分布") || s.contains("下側") || s.contains("有")) {
                skipThisLine = true;
            } else if (l.lesionType == null) {
                String t = preCleanupLesionType(s);

                if (t.contains("非塊狀病灶") && !t.contains("*") && t.length() != 5) {
                    t = "非塊狀病灶";
                }

                DBG_TAG("腫瘤型態3", t);

                l.lesionType = l.lesionType == null ? t : l.lesionType + t;
                l.birads = l.birads == null ? birads_code : l.birads + birads_code;
                l.sliceStart = sliceStart;
                l.sliceEnd = sliceEnd;

                birads_code = null;
                sliceStart = 0;
                sliceEnd = 0;
            }
        } else {
            lesion_type = null;
        }

        if (l != null && l.birads == null && l.number >= 100) {
            l.birads = "#N/A";
        }
    }

    private Lesion getLesion(int lesionNumber) {
        if (!lesionList.containsKey(lesionNumber)) {
            lesionList.put(lesionNumber, new Lesion(lesionNumber, isRightBreast ? Side.Right : Side.Left));
        }

        return lesionList.get(lesionNumber);
    }

    private String preCleanup(String s) {
        return s.replaceAll("： +", "：")
                .replaceAll(" +：", "：")
                .replaceAll(" +/", "/")
                .replaceAll("/ +", "/")
                .replaceAll("_+", "")
                .replaceAll(" +", " ");
    }

    private static void DBG_TAG(String tag, String data) {
        if (DEBUG) {
            System.out.println("\t\t\t\t\t\t\t\t!!!!!!!!!!\t\t" + String.format("%-20s\t\t=\t%s", String.format("[%s]", tag), String.format("[%s]", data)));
        }
    }

    private void parseTagData(String s) {
        if (s.contains(COLON_DELIMITER)) {
            String clean = preCleanup(s);
            Scanner scanner = new Scanner(clean);
            DBG_TAG("清理後", clean);

            while (scanner.hasNext()) {
                String newScanner = scanner.next();
                Scanner aScanner = new Scanner(newScanner);
                aScanner.useDelimiter(COLON_DELIMITER);
                if (aScanner.hasNext()) {
                    Tag tag = Tag.getTag(aScanner.next());
                    if (DEBUG) {
                        String tagName = (tag == null) ? "忽略" : tag.toString();
                        String data = "";

                        try {
                            data = aScanner.next().trim();
                        } catch (Exception ignore) {
                        } finally {
                            aScanner = new Scanner(newScanner);
                            aScanner.useDelimiter(COLON_DELIMITER);
                            aScanner.next();
                        }

                        if (!"忽略".equals(tagName)) {
                            DBG_TAG(tagName, data);
                        }
                    }

                    if (tag == null) {
                        continue;
                    }

                    if (!isEmptyField(tag)) {
                        continue;
                    }

                    switch (tag) {
                        case pname:
                            try {
                                patientName = aScanner.next().trim();
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case id:
                            try {
                                personID = aScanner.next().trim();
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case pid:
                            try {
                                patientID = aScanner.next().trim();
                            } catch (NoSuchElementException ex) {
                                hasParseError = true;
                            }
                            break;
                        case date:
                            String dateStr = aScanner.next().trim();
                            try {
                                studyDate = parseDateString(dateStr);
                            } catch (IllegalArgumentException ex) {
                                hasParseError = true;
                            }
                            break;
                        case hos:
                            try {
                                hospital = aScanner.next().trim();
                            } catch (NoSuchElementException ex) {
                                hasParseError = true;
                            }
                            break;
                        case sid:
                            try {
                                studyID = aScanner.next().trim();
                            } catch (NoSuchElementException ex) {
                                hasParseError = true;
                            }
                            break;
                        case density:
                            try {
                                density = aScanner.next().replaceAll("。", "");
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case lesion_no:
                            lesionCount = Integer.parseInt(transferNumber(aScanner.next().trim()));
                            break;
                        case slice_no:
                            try {
                                String slice_info = aScanner.next().trim().replaceAll("~", "-").replaceAll("～", "-").replaceAll("\\.", "-").replaceAll("、", "-");
                                String[] splits = slice_info.split("-");
                                sliceStart = Integer.parseInt(splits[0].trim());
                                sliceEnd = (splits.length == 2) ? Integer.parseInt(splits[1].trim()) : sliceStart;
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case birads:
                            try {
                                birads_code = aScanner.next()
                                        .replaceAll("\\.", "")
                                        .replaceAll("1", "I")
                                        .replaceAll("2", "II")
                                        .replaceAll("3", "III")
                                        .replaceAll("4", "IV")
                                        .replaceAll("5", "V")
                                        .replaceAll("6", "VI")
                                        .replaceAll("Ⅵ", "VI")
                                        .replaceAll("Ⅴ", "V")
                                        .replaceAll("Ⅳ", "IV")
                                        .replaceAll("A", "a")
                                        .replaceAll("B", "b")
                                        .replaceAll("C", "c");
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                    }
                }
            }
        }
    }

    private String transferNumber(String org) {
        char[] chars = org.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int tranTemp = (int) chars[i];
            if (tranTemp - 65248 >= 48 && tranTemp - 65248 <= 57) {
                tranTemp -= 65248;
            }
            org += (char) tranTemp;
        }

        return new String(chars);
    }

    private boolean isEmptyField(Tag t) {
        switch (t) {
            case pname:
                return patientName == null;
            case id:
                return personID == null;
            case pid:
                return patientID == null;
            case date:
                return studyDate == null;
            case hos:
                return hospital == null;
            case sid:
                return studyID == null;
            case density:
                return density == null;
            default:
                return isReportStart;
        }
    }

    public boolean hasEmptyField() {
        for (Tag t : Tag.values()) {
            if (isEmptyField(t)) {
                return true;
            }
        }

        for (Lesion l : lesionList.values()) {
            Field[] fields = l.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (field.get(l) == null) {
                        //System.out.println("Field name: " + field.getName());
                        return true;
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                }
            }
        }

        return false;
    }

    private static Date fixTWDate(Date d) {
        Calendar sdc = Calendar.getInstance();
        sdc.setTime(d);
        int sy = sdc.get(Calendar.YEAR);
        if (sy < 2000) {
            sdc.set(Calendar.YEAR, sy + 1911);
            return sdc.getTime();
        } else {
            return d;
        }
    }

    private static Date parseDateString(String dateStr) {
        Date studyDate;
        try {
            studyDate = fixTWDate(df0.parse(dateStr));

            if (!isWithinRange(studyDate)) {
                studyDate = df1.parse(dateStr);
            }
            return studyDate;
        } catch (ParseException ignore) {
        }

        try {
            studyDate = df2.parse(dateStr);
            return studyDate;
        } catch (ParseException ignore) {
        }

        try {
            studyDate = df3.parse(dateStr);
            return studyDate;
        } catch (ParseException ignore) {

        }

        try {
            studyDate = df4.parse(dateStr);
            return studyDate;
        } catch (ParseException ignore) {
        }

        try {
            studyDate = df5.parse(dateStr);
            return studyDate;
        } catch (ParseException ignore) {
        }

        DBG_TAG("studyDate", dateStr);
        System.err.println("\t\t\t\t!!!!!!!!!! studyDate = " + dateStr);
        throw new IllegalArgumentException("pasrse studyDate fail");
    }

    private static boolean isWithinRange(Date testDate) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2000);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        final Date startDate = cal.getTime();
        cal.set(Calendar.YEAR, 3000);
        final Date endDate = cal.getTime();
        return !(testDate.before(startDate) || testDate.after(endDate));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(patientName).append("\t");
        sb.append(patientID).append("\t");
        sb.append(personID).append("\t");
        sb.append(df0.format(studyDate)).append("\t");
        sb.append(hospital).append("\t");
        sb.append(studyID).append("\t");
        sb.append(density).append("\t");
        sb.append(lesionCount).append("\t");
        sb.append(pdfFilePath);

        for (Lesion l : lesionList.values()) {
            sb.append("\n\t").append(l);//.append("\t").append(pdfFilePath);
        }
        return sb.toString();
    }
}
