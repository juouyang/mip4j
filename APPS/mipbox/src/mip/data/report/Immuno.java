/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.report;

/**
 *
 * @author ju
 */
public class Immuno {

    final String immunoText;
    double ki67;

    Immuno(String s) {
        immunoText = s;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append(ki67 == Double.MIN_VALUE ? "-" : ki67).append(",");
        sb.append("\"").append(immunoText).append("\"");

        return sb.toString();
    }
}
