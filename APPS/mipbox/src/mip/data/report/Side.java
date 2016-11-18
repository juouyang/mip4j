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
public enum Side {
    UNKNOWN("Unknown"), LEFT("Left"), RIGHT("Right");
    private final String description;

    private Side(String s) {
        description = s;
    }

    @Override
    public String toString() {
        return this.description;
    }

}
