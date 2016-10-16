/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image;

public class RGB {

    public short R;
    public short G;
    public short B;

    @Override
    public String toString() {
        return String.format("(%04d,%04d,%04d)", R, G, B);
    }

}
