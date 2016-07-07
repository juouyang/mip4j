/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image;

import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import mip.data.image.mr.MR;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ju
 */
public class ShortImageTest {

    MR instance;

    public ShortImageTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        File f = new File(getClass().getClassLoader().getResource("resources/bmr/1/001.dcm").getFile());
        instance = new MR(f.toPath());
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of setPixel method, of class ShortImage.
     */
    @Test
    public void testSetPixel() {
        int x = 0;
        int y = 0;
        int v = 0;
        instance.setPixel(x, y, v);
        assertEquals(instance.getPixel(x, y), v);
    }

    /**
     * Test of getPixel method, of class ShortImage.
     */
    @Test
    public void testGetPixel() {
        int x = 0;
        int y = 0;
        short expResult = 15;
        short result = instance.getPixel(x, y);
        assertEquals(expResult, result);
    }

    /**
     * Test of getMax method, of class ShortImage.
     */
    @Test
    public void testGetMax() {
        short expResult = 917;
        short result = instance.getMax();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMin method, of class ShortImage.
     */
    @Test
    public void testGetMin() {
        short expResult = 0;
        short result = instance.getMin();
        assertEquals(expResult, result);
    }

    /**
     * Test of getWindowCenter method, of class ShortImage.
     */
    @Test
    public void testGetWindowCenter() {
        int expResult = 365;
        int result = instance.getWindowCenter();
        assertEquals(expResult, result);
    }

    /**
     * Test of getWindowWidth method, of class ShortImage.
     */
    @Test
    public void testGetWindowWidth() {
        int expResult = 695;
        int result = instance.getWindowWidth();
        assertEquals(expResult, result);
    }

    /**
     * Test of toImagePlus method, of class ShortImage.
     */
    @Test
    public void testToImagePlus() {
        String title = "";
        ImagePlus result = instance.getImagePlus(title);
        assertEquals((int) result.getProcessor().getMin(), instance.getMin());
        assertEquals((int) result.getProcessor().getMax(), instance.getMax());
    }

    /**
     * Test of show method, of class ShortImage.
     */
    @Test
    public void testShow() {
        instance.show();
    }
}
