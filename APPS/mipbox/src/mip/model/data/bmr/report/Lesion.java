/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.model.data.bmr.report;

/**
 *
 * @author ju
 */
public class Lesion {

    public enum Side {

        Left,
        Right;
    }

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

    public String toJSONString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("\"number\" : \"").append(number).append("\",");
        sb.append("\"side\" : \"").append(side).append("\",");
        sb.append("\"sliceStart\" : \"").append(sliceStart).append("\",");
        sb.append("\"sliceEnd\" : \"").append(sliceEnd).append("\",");
        sb.append("\"birads\" : \"").append(birads).append("\",");
        sb.append("\"lesionType\" : \"").append(lesionType).append("\"");
        sb.append("}");

        return sb.toString();
    }

}
