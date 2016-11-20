/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.report;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import mip.util.LogUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author ju
 */
public class BMR implements Comparable<BMR> {

    public static final String MR_ROOT = "W:/_BREAST_MRI/";
    private static final Logger LOG = LogUtils.LOGGER;

    public static BMR getBMR(Diagnosis d, Set<BMR> set, LocalDate targetDate) {
        assert (d.side != Side.MIXED);
        if (set == null || d.cancerType == CancerType.UNKNOWN || d.region == Region.UNKNOWN || d.side == Side.UNKNOWN) {
            return null;
        }
        List<LocalDate> dates = new ArrayList<>(10);
        set.stream().forEach((bmr) -> {
            dates.add(bmr.scanDate);
        });
        // get the Closest BMR BEFORE BiopsyDate
        LocalDate beforeAndEqual = null;
        long min = Long.MAX_VALUE;
        for (LocalDate date : dates) {
            long monthsBetween = ChronoUnit.MONTHS.between(targetDate, date);
            long daysBetween = ChronoUnit.DAYS.between(targetDate, date);
            if (Math.abs(monthsBetween) > 6 || daysBetween > 0) {
                continue;
            }
            if (min == Long.MAX_VALUE || min > Math.abs(daysBetween)) {
                beforeAndEqual = date;
                min = Math.abs(daysBetween);
            }
        }
        for (BMR bmr : set) {
            if (bmr.scanDate.equals(beforeAndEqual)) {
                return bmr;
            }
        }
        return null;
    }

    String hospital;
    String studyID;
    LocalDate scanDate;
    final List<Diagnosis> leftDiagnosisList = new ArrayList<>(8);
    final List<Diagnosis> rightDiagnosisList = new ArrayList<>(8);
    final List<Diagnosis> leftMixedDiagnosisList = new ArrayList<>(8);
    final List<Diagnosis> rightMixedDiagnosisList = new ArrayList<>(8);
    final List<Diagnosis> duplicateTypeDiagnosisList = new ArrayList<>(8);

    private void _merge(Diagnosis keep, boolean ignoredBenign) {
        assert (keep != null && keep.side != Side.UNKNOWN);
        boolean isSingleType = true;
        List<Diagnosis> list = (keep.side == Side.LEFT) ? leftDiagnosisList : rightDiagnosisList;
        for (Diagnosis d : list) {
            if (ignoredBenign && d.cancerType == CancerType.BENIGN) {
                continue;
            }
            if (d != keep && d.cancerType != keep.cancerType) {
                isSingleType = false;
                break;
            }
        }
        if (isSingleType) {
            list.stream().filter((d) -> (d != keep)).forEach((d) -> {
                duplicateTypeDiagnosisList.add(d);
            });
            duplicateTypeDiagnosisList.stream().forEach((Diagnosis d) -> {
                list.remove(d);
            });
        }
    }

    public void mergeByCancerType() {
        if (leftDiagnosisList.size() > 1) {
            Diagnosis malignant = null;
            Diagnosis benign = null;
            for (Diagnosis d : leftDiagnosisList) {
                if (d.cancerType != CancerType.BENIGN) {
                    malignant = d;
                } else {
                    benign = d;
                }
            }
            assert (malignant != null || benign != null);
            if (malignant != null && benign != null) {
                _merge(malignant, true);
            } else {
                _merge(malignant != null ? malignant : benign, false);
            }
        }
        if (rightDiagnosisList.size() > 1) {
            Diagnosis malignant = null;
            Diagnosis benign = null;
            for (Diagnosis d : rightDiagnosisList) {
                if (d.cancerType != CancerType.BENIGN) {
                    malignant = d;
                } else {
                    benign = d;
                }
            }
            assert (malignant != null || benign != null);
            if (malignant != null && benign != null) {
                _merge(malignant, true); // ignore benign
            } else {
                _merge(malignant != null ? malignant : benign, false);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(hospital).append("\t").append(studyID).append("\n");
        sb.append(ArrayUtils.toString(leftDiagnosisList.toArray())).append("\n");
        sb.append(ArrayUtils.toString(rightDiagnosisList.toArray())).append("\n");
        sb.append(ArrayUtils.toString(leftMixedDiagnosisList.toArray())).append("\n");
        sb.append(ArrayUtils.toString(rightMixedDiagnosisList.toArray())).append("\n");
        return sb.toString();
    }

    public int getTotalDiagnosesCount() {
        return leftDiagnosisList.size() + rightDiagnosisList.size() + leftMixedDiagnosisList.size() + rightMixedDiagnosisList.size();
    }

    @Override
    public int compareTo(BMR bmr) {
        int hos = hospital.compareTo(bmr.hospital);
        int id = Integer.parseInt(studyID) - Integer.parseInt(bmr.studyID);
        return (hos != 0) ? hos : id;
    }

}
