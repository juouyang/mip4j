/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.model.data.bmr.report;

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
import mip.model.data.bmr.report.Lesion;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 *
 * @author ju
 */
public class BMRReport {

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

        private final String desc;

        private Tag(String s) {
            desc = s;
        }

        public boolean equalsTag(String s) {
            return (s == null) ? false : desc.equals(s);
        }

        public static Tag getTag(String s) {
            for (Tag t : Tag.values()) {
                if (t.equalsTag(s)) {
                    return t;
                }
                if (s.endsWith(t.toString()) && (t == Tag.density || t == Tag.slice_no)) {
                    return t;
                }
                if (s.contains("組成形態") && t == Tag.density) {
                    return t;
                }
                if (t.toString().indexOf(s) == 1) {
                    return t;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    public static boolean DEBUG = false;

    private static final String COLON_DELIMITER = "：";

    private boolean isReportStart = false;
    private boolean isRightBreast = false;

    private final String pdfFilePath;

    public String patientName;
    public String personID;
    public String patientID;
    public Date studyDate;
    public String studyID;
    public String hospital;
    public String density;

    public final Map<Integer, Lesion> lesionList = new HashMap<>();
    public boolean hasBenignLesion = false;
    private int lesionID = 100; // for those doesn't have lesion No.
    private int lesionCount = 0;
    private int sliceStart = 0;
    private int sliceEnd = 0;
    private String birads_code;
    private String lesion_type = "";

    private String previousLine;
    private boolean skipThisLine = false;

    public BMRReport(String pdfFile) throws IOException {

        PDFParser parser = new PDFParser(new FileInputStream(new File(pdfFile)));
        parser.parse();
        COSDocument cosDoc = parser.getDocument();

        try (PDDocument pdDoc = new PDDocument(cosDoc)) {

            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(10);

            String parsedText = pdfStripper.getText(pdDoc);
            String[] lines = parsedText.split(System.getProperty("line.separator"));

            int i = 0;
            String original_line = "";
            for (String s : lines) {
                if (s.trim().length() == 0) {
                    continue;
                }

                DBG_LINE(++i, s);

                s = BMRReportUtils.fixTag(s);
                original_line = s;
                DBG_TAG("統一格式", s);

                if ((s.contains("磁振造影") && s.contains("結果")) || (s.contains("打顯影劑") && s.contains("三度空間"))) {
                    DBG_TAG("報告開始", s);
                    isReportStart = true;
                }

                if (isReportStart) {
                    boolean mayHasData = false;
                    if (s.contains(COLON_DELIMITER)) {
                        mayHasData = true;
                    }

                    final String LEFT_BREAST = "左乳";
                    if (s.contains(LEFT_BREAST)) {
                        s = s.substring(s.indexOf(LEFT_BREAST) + 2);
                        if (s.indexOf(COLON_DELIMITER) == 0) {
                            s = s.substring(1);
                        }
                        DBG_TAG("左乳", s);

                        isRightBreast = false;
                    }

                    final String RIGHT_BREAST = "右乳";
                    if (s.contains(RIGHT_BREAST)) {
                        s = s.substring(s.indexOf(RIGHT_BREAST) + 2);
                        if (s.indexOf(COLON_DELIMITER) == 0) {
                            s = s.substring(1);
                        }
                        DBG_TAG("右乳", s);
                        isRightBreast = true;
                    }

                    if (s.contains(Tag.lesion_no.toString()) && mayHasData && !s.contains(Tag.slice_no.toString()) && !s.contains(Tag.birads.toString())) {
                        s = BMRReportUtils.cleanupLesionType(s);
                        if (!s.contains("標示") && !s.contains(Tag.lesion_no.toString())) {
                            lesion_type = s;
                            DBG_TAG("腫瘤型態4", s);
                        }
                    }

                    if (s.contains("報告總結") || s.contains("國際乳房腫瘤通用的分級方法")) {
                        if (lesionCount != lesionList.size()) {
                            hasBenignLesion = true;
                        }
                        DBG_TAG("報告結束", s);
                        isReportStart = false;
                        break;
                    }
                }

                parseTagData(s);
                parseLesionInfo(s);

                if (!skipThisLine) {
                    previousLine = original_line;
                }
                skipThisLine = false;
            }
        }

        pdfFilePath = pdfFile;
    }

    private void parseTagData(String s) {
        if (s.contains(COLON_DELIMITER)) {
            String clean = BMRReportUtils.cleanup(s);
            Scanner scanner = new Scanner(clean);
            DBG_TAG("清理", clean);

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

                    if (foundInReport(tag)) {
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
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case date:
                            String dateStr = aScanner.next().trim();
                            try {
                                studyDate = BMRReportUtils.string2Date(dateStr);
                            } catch (IllegalArgumentException ignore) {
                            }
                            break;
                        case hos:
                            try {
                                hospital = aScanner.next().trim();
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case sid:
                            try {
                                studyID = aScanner.next().trim();
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case density:
                            try {
                                density = aScanner.next().replaceAll("。", "");
                            } catch (NoSuchElementException ignore) {
                            }
                            break;
                        case lesion_no:
                            lesionCount = Integer.parseInt(BMRReportUtils.transFullwidthNumber2Halfwidth(aScanner.next().trim()));
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
                                        .replaceAll("Ⅴ", "V")
                                        .replaceAll("\\.", "")
                                        .replaceAll("VI", "6")
                                        .replaceAll("IV", "4")
                                        .replaceAll("V", "5")
                                        .replaceAll("III", "3")
                                        .replaceAll("II", "2")
                                        .replaceAll("I", "1")
                                        .replaceAll("Ⅱ", "2")
                                        .replaceAll("Ⅲ", "3")
                                        .replaceAll("Ⅳ", "4")
                                        .replaceAll("V", "5")
                                        .replaceAll("Ⅵ", "6")
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
            s = s.replaceAll("塊狀顯影", "塊狀病灶");
            s = s.replaceAll("腫塊病灶", "塊狀病灶");

            if (!s.contains("非")) {
                if (s.contains("塊狀病灶") && (s.indexOf("塊狀病灶") != 1 || s.length() > "塊狀病灶".length() + 1)) {
                    s = "*塊狀病灶";
                }
                if (s.contains("腫塊") && (s.indexOf("腫塊") != 1 || s.length() > "腫塊".length() + 1)) {
                    s = "*塊狀病灶";
                }
                if (s.equals("*腫塊")) {
                    s = "*塊狀病灶";
                }
            }
            if (s.contains("非塊狀病灶") && (s.indexOf("非塊狀病灶") != 1 || s.length() > "非塊狀病灶".length() + 1)) {
                s = "*非塊狀病灶";
            }
            if (s.contains("焦點") && (s.indexOf("焦點") != 1 || s.length() > "焦點".length() + 1)) {
                s = "*焦點";
            }
            if (s.equals("*區域")) {
                s = "*焦點";
            }
            if (s.contains("囊腫") && (s.indexOf("囊腫") != 1 || s.length() > "囊腫".length() + 1)) {
                s = "*囊腫";
            }
            if (s.contains("淋巴結") && (s.indexOf("淋巴結") != 1 || s.length() > "淋巴結".length() + 1)) {
                s = "*淋巴結";
            }
            if (s.contains("結節") && (s.indexOf("結節") != 1 || s.length() > "結節".length() + 1)) {
                s = "*結節";
            }

            String t = BMRReportUtils.cleanupLesionType(s);
            DBG_TAG("腫瘤型態1", t);

            if (previousLine.contains(Tag.slice_no.toString())) {

                if (lesionCount != 0 && previousLine.contains(Tag.lesion_no.toString())) {
                    l = getLesion(lesionCount);
                } else {
                    l = getLesion(lesionID++);
                    hasBenignLesion = true;
                }

                l.lesionType = t;
                if (l.birads == null) {
                    l.birads = l.birads == null ? birads_code : l.birads + birads_code;
                    l.sliceStart = sliceStart;
                    l.sliceEnd = sliceEnd;

                    birads_code = null;
                    sliceStart = 0;
                    sliceEnd = 0;
                }
            }
        } else if (!"".equals(lesion_type) && s.contains(Tag.lesion_no.toString())) {
            String t = lesion_type;

            if (s.contains(Tag.slice_no.toString())) {
                if (t.contains("囊腫") && t.indexOf("囊腫") != 0) {
                    t = "囊腫";
                }

                DBG_TAG("腫瘤型態2", t);

                if (lesionCount != 0 && (previousLine.contains(Tag.lesion_no.toString()) || s.contains(Tag.lesion_no.toString()))) {
                    l = getLesion(lesionCount);
                } else {
                    l = getLesion(lesionID++);
                    hasBenignLesion = true;
                }

                l.lesionType = l.lesionType == null ? t : l.lesionType + t;
                l.birads = l.birads == null ? birads_code : l.birads + birads_code;
                l.sliceStart = sliceStart;
                l.sliceEnd = sliceEnd;

                lesion_type = "";
                birads_code = null;
                sliceStart = 0;
                sliceEnd = 0;
            }
        } else if (previousLine != null && previousLine.contains(Tag.slice_no.toString()) && previousLine.contains(Tag.lesion_no.toString()) && previousLine.contains(Tag.birads.toString())) {
            if (lesionCount != 0 && previousLine.contains(Tag.lesion_no.toString())) {
                l = getLesion(lesionCount);
            } else {
                l = getLesion(lesionID++);
                hasBenignLesion = true;
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
                String t = ("".equals(lesion_type)) ? BMRReportUtils.cleanupLesionType(s) : lesion_type;

                if (t.contains("非塊狀病灶") && !t.contains("*") && t.length() != 5) {
                    t = "非塊狀病灶";
                }

                DBG_TAG("腫瘤型態3", t);

                l.lesionType = l.lesionType == null ? t : l.lesionType + t;
                lesion_type = "";
                l.birads = l.birads == null ? birads_code : l.birads + birads_code;
                l.sliceStart = sliceStart;
                l.sliceEnd = sliceEnd;

                birads_code = null;
                sliceStart = 0;
                sliceEnd = 0;
            }
        }

        if (l != null && l.birads == null && l.number >= 100) {
            l.birads = "#N/A";
        }
    }

    private Lesion getLesion(int lesionNumber) {
        if (!lesionList.containsKey(lesionNumber)) {
            lesionList.put(lesionNumber, new Lesion(lesionNumber, isRightBreast ? Lesion.Side.Right : Lesion.Side.Left));
        }

        return lesionList.get(lesionNumber);
    }

    private boolean foundInReport(Tag t) {
        switch (t) {
            case pname:
                return patientName != null;
            case id:
                return personID != null;
            case pid:
                return patientID != null;
            case date:
                return studyDate != null;
            case hos:
                return hospital != null;
            case sid:
                return studyID != null;
            case density:
                return density != null;
            default:
                return !isReportStart;
        }
    }

    public boolean hasMissingData() {
        for (Tag t : Tag.values()) {
            if (!foundInReport(t)) {
                return true;
            }
        }

        for (Lesion l : lesionList.values()) {
            Field[] fields = l.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (field.get(l) == null) {
                        return true;
                    }
                } catch (IllegalArgumentException | IllegalAccessException ignore) {
                }
            }
        }

        return false;
    }

    private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd");

    public String toExcelRow() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Lesion l : lesionList.values()) {
            sb.append("***\t")
                    .append(patientID).append("\t")
                    .append(hospital).append("\t")
                    .append(studyID).append("\t")
                    .append(hasBenignLesion).append("\t")
                    .append(l.number).append("\t")
                    .append(l.birads).append("\t")
                    .append(l.lesionType).append("\t")
                    .append(pdfFilePath);
            if (++i != lesionList.size()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public String toJSONString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("\"BMRReport\" : {");

        sb.append("\"patientName\" : \"").append(patientName).append("\",");
        sb.append("\"patientID\" : \"").append(patientID).append("\",");
        sb.append("\"personID\" : \"").append(personID).append("\",");
        sb.append("\"studyDate\" : \"").append(df.format(studyDate)).append("\",");
        sb.append("\"hospital\" : \"").append(hospital).append("\",");
        sb.append("\"studyID\" : \"").append(studyID).append("\",");
        sb.append("\"density\" : \"").append(density).append("\",");
        sb.append("\"hasBenignLesion\" : \"").append(hasBenignLesion).append("\",");
        sb.append("\"lesionCount\" : \"").append(lesionCount).append("\",");
        sb.append("\"lesions\" : [");

        int i = 0;
        for (Lesion l : lesionList.values()) {
            sb.append(l.toJSONString());
            if (++i != lesionList.size()) {
                sb.append(",");
            }
        }

        sb.append("],");
        sb.append("\"pdfFilePath\" : \"").append(BMRReportUtils.replace4JSON(pdfFilePath)).append("\"");
        sb.append("}");
        sb.append("}");

        return sb.toString();
    }

    private static void DBG_LINE(int lineNo, String line) {
        if (DEBUG) {
            System.out.println(String.format("%5s : %s", String.format("Line %d", lineNo), String.format("[%s]", line)));
        }
    }

    private static void DBG_TAG(String tag, String data) {
        if (DEBUG) {
            System.out.println("\t\t\t\t\t\t\t\t!!!!!!!!!!\t\t" + String.format("%-20s\t\t=\t%s", String.format("[%s]", tag), String.format("[%s]", data)));
        }
    }
}

class BMRReportUtils {

    static String cleanup(String s) {
        return s.replaceAll("： +", "：")
                .replaceAll(" +：", "：")
                .replaceAll(" +/", "/")
                .replaceAll("/ +", "/")
                .replaceAll("_+", "")
                .replaceAll(" +", " ");
    }

    static String cleanupLesionType(String s) {
        s = s.replaceAll("\\* ?", "*");
        if (s.contains("\t")) {
            s = s.substring(0, s.indexOf("\t"));
        }
        if (s.contains(" ")) {
            s = s.substring(0, s.indexOf(" "));
        }
        if (s.endsWith("：")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.contains("，")) {
            s = s.substring(0, s.indexOf("，"));
        }

        return (s.length() >= 1 && s.charAt(0) == '*') ? s.substring(1).replaceAll("：", "") : s;
    }

    static String fixTag(String s) {
        return s.replaceAll(":", "：")
                .replaceAll("--", "：")
                .replaceAll("；", "\t")
                .replaceAll("<", " <\t")
                .replaceAll("/", " /") // for lesionType
                .replaceAll("TMHH", "TMUH")
                .replaceAll("STMH", "SMHT")
                .replaceAll("20009", "2009")
                .replaceAll("：淋巴節", " 淋巴結")
                .replaceAll("：軸狀切面張數", " 軸狀切面張數")
                .replaceAll("：乳房組成形態：", "\t乳房組成形態：")
                .replaceAll("：乳房組成形態：", "\t乳房組成形態：")
                .replaceAll("組成形態屬於", "組成形態屬於：")
                .replaceAll("BIRADS ?分類", "\tBIRADS分類：")
                .replaceAll("BIRADS", (s.contains(BMRReport.Tag.lesion_no.toString()) && s.contains(BMRReport.Tag.slice_no.toString()) && !s.contains("分類")) ? "BIRADS分類：" : "BIRADS")
                .replaceAll("BI-RADS", (s.contains(BMRReport.Tag.lesion_no.toString()) && s.contains(BMRReport.Tag.slice_no.toString()) && !s.contains("分類")) ? "BIRADS分類：" : "BI-RADS")
                .replaceAll("BI-RADS ?分類", "\tBIRADS分類")
                .replaceAll("：：", "：").trim();
    }

    static String transFullwidthNumber2Halfwidth(String org) {
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

    static String replace4JSON(String s) {
        return s.replaceAll("\\\\", "/");
    }

    private static Date transTWYear2CommonEra(Date org) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(org);
        int sy = cal.get(Calendar.YEAR);
        if (sy < 1900) {
            cal.set(Calendar.YEAR, sy + 1911);
            return cal.getTime();
        } else {
            return org;
        }
    }

    private static boolean isAfter2KYear(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2000);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        final Date startDate = cal.getTime();
        cal.set(Calendar.YEAR, 3000);
        final Date endDate = cal.getTime();
        return !(d.before(startDate) || d.after(endDate));
    }

    static Date string2Date(String s) {
        Date retDate = null;

        try {
            final DateFormat df0 = new SimpleDateFormat("yyyy/MM/dd");
            retDate = BMRReportUtils.transTWYear2CommonEra(df0.parse(s));

            if (!BMRReportUtils.isAfter2KYear(retDate)) {
                final DateFormat df1 = new SimpleDateFormat("MM/dd/yyyy");
                retDate = df1.parse(s);
            }
            return retDate;
        } catch (ParseException ignore) {
        }
        try {
            final DateFormat df2 = new SimpleDateFormat("yyyyMM/dd");
            retDate = df2.parse(s);
            return retDate;
        } catch (ParseException ignore) {
        }
        try {
            final DateFormat df3 = new SimpleDateFormat("yyyy/MM//dd");
            retDate = df3.parse(s);
            return retDate;
        } catch (ParseException ignore) {

        }
        try {
            final DateFormat df4 = new SimpleDateFormat("yyyy-MM-dd");
            retDate = df4.parse(s);
            return retDate;
        } catch (ParseException ignore) {
        }
        try {
            final DateFormat df5 = new SimpleDateFormat("yyyy.MM.dd");
            retDate = df5.parse(s);
            return retDate;
        } catch (ParseException ignore) {
        }

        return retDate;
    }
}
