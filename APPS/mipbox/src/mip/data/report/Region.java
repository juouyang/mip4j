/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.report;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ju
 */
public enum Region {
    DNA("DNA", new String[]{"KRAS", "mutation", "EGFR"}),
    Head("Head", new String[]{"cheek", "salivary gland", "skull", "nasal", "face", "nasopharynx", "neck"}),
    LUNG("Lung", new String[]{"lung"}),
    THYROID("Thyroid", new String[]{"thyroid"}),
    BONE("Bone", new String[]{"vetebra", "intervertebral", "iliac"}),
    ABDOMEN("Abdomen", new String[]{"stomach", "abdominal", "omentum", "appendix"}),
    ANUS("Anus", new String[]{"anus"}),
    ORAL("Oral", new String[]{"oral"}),
    INTESTINE("Intestine", new String[]{"intestine"}),
    ESOPHAGUS("Esophagus", new String[]{"esophagus"}),
    GALLBLADDER("Gallbladder", new String[]{"gallbladder"}),
    DUODENUM("Duodenum", new String[]{"duodenum"}),
    HAND("Hand", new String[]{"thumb", "wrist"}),
    PELVIC("Pelvic", new String[]{"pelvic"}),
    LEG("Leg", new String[]{"knee"}),
    FEMALE("Female", new String[]{"uterus", "endometrium", "fallopian tube", "falllopian", "ovary", "vagina", "cervix", "vaginal", "parametrium", "Vulva"}),
    BREAST("Breast", new String[]{"breast", "nipple", "Rotter", "chest", "sentinel node", "ER(+)", "ER positive", "HER2", "HER-2"}),
    LYMPH("Lymph", new String[]{"lymph node", "axilla"}),
    SKIN("Skin", new String[]{"skin"}),
    UNKNOWN("Unknown", null),
    IGNORED("Ignored", null),;

    private final String description;
    final String[] keywords;

    private Region(String s, String[] k) {
        description = s;
        keywords = k;
    }

    @Override
    public String toString() {
        return this.description;
    }

    public static Region fromString(String s) {
        for (Region r : values()) {
            if (r.keywords != null) {
                for (String k : r.keywords) {
                    if (StringUtils.containsIgnoreCase(s, k)) {
                        return r;
                    }
                }
            }
        }
        return UNKNOWN;
    }

}
