/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.model.data.bmr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    pid("體檢號"),
    date("檢查日期"),
    hos("就診地點"),
    sid("檢查號碼"),
    density("乳房組成形態"),
    lesion_no("病灶編號");

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
    public String location;
    public double toNipple;
    public double toChestWall;

    public Lesion(int no, Side s) {
        number = no;
        side = s;
    }

    @Override
    public String toString() {
        return "Lesion{" + "number=" + number
                + ", side=" + side
                + ", sliceStart=" + sliceStart
                + ", sliceEnd=" + sliceEnd
                + ", birads=" + birads
                + ", lesionType=" + lesionType
                + ", location=" + location
                + ", toNipple=" + toNipple
                + ", toChestWall=" + toChestWall + '}';
    }

}

public class BMRReport {

    private static final String COLON_DELIMITER = "：";
    private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd");

    private String patientName;
    private String patientID;
    private Date studyDate;
    private String studyID;
    private String hospital;
    private String density;
    private int lesionCount;
    private Map<Integer, Lesion> lesionList = new HashMap<>();

    private boolean isRight = false;

    public BMRReport(String pdfFile) {
        try {
            PDFParser parser = new PDFParser(new FileInputStream(new File(pdfFile)));
            parser.parse();
            COSDocument cosDoc = parser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(10);
            String parsedText = pdfStripper.getText(pdDoc);
            String[] lines = parsedText.split(System.getProperty("line.separator"));
            int i = 0;

            for (String s : lines) {
                System.out.println(++i + "\t" + s);

                if (s.contains("右乳")) {
                    isRight = true;
                }

                delimiterFields(s);
                parseLesionInfo(s);
            }
        } catch (IOException e) {
        }

        System.out.println();
    }

    private void parseLesionInfo(String s) {
        if (s.contains(Tag.lesion_no.toString())) {
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!parseLesionInfo");
            System.out.println(s);
            if (lesionCount == 0) {
                return;
            }

            Lesion l = getLesion(lesionCount);
        }
    }

    private Lesion getLesion(int lesionNumber) {
        if (!lesionList.containsKey(lesionNumber)) {
            lesionList.put(lesionNumber, new Lesion(lesionNumber, isRight ? Side.Right : Side.Left));
        }

        return lesionList.get(lesionNumber);
    }

    private void delimiterFields(String s) {
        if (s.contains(COLON_DELIMITER)) {
            Scanner scanner = new Scanner(s.replaceAll("： +", "："));

            while (scanner.hasNext()) {
                String newScanner = scanner.next();
                Scanner aScanner = new Scanner(newScanner);
                aScanner.useDelimiter(COLON_DELIMITER);
                if (aScanner.hasNext()) {
                    Tag tag = Tag.getTag(aScanner.next());
                    if (tag == null) {
                        continue;
                    }
                    String token = aScanner.next().trim();
                    switch (tag) {
                        case pname:
                            patientName = token;
                            break;
                        case pid:
                            patientID = token;
                            break;
                        case date:
                            try {
                                studyDate = df.parse(token);
                            } catch (ParseException ex) {
                            }
                            break;
                        case hos:
                            hospital = token;
                            break;
                        case sid:
                            studyID = token;
                            break;
                        case density:
                            density = token;
                            break;
                        case lesion_no:
                            lesionCount = Integer.parseInt(token);
                            break;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PatientName \t= ").append(patientName).append("\n");
        sb.append("PatientID \t= ").append(patientID).append("\n");
        sb.append("StudyDate \t= ").append(df.format(studyDate)).append("\n");
        sb.append("Hospital \t= ").append(hospital).append("\n");
        sb.append("StudyID \t= ").append(studyID).append("\n");
        sb.append("Density \t= ").append(density).append("\n");
        sb.append("LesionCount \t= ").append(lesionCount).append("\n");

        for (Lesion l : lesionList.values()) {
            sb.append(l).append("\n");
        }
        return sb.toString();
    }
}
