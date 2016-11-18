/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import ij.ImagePlus;
import mip.util.IJUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ju
 */
public class MRSeriesTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }
    private MRSeries instance;

    public MRSeriesTest() {
    }

    @Before
    public void setUp() throws InterruptedException {
        instance = MROpener.openMRSeries();
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of getWidth method, of class MRSeries.
     */
    @Test
    public void testGetWidth() {
        int expResult = 512;
        int result = instance.getWidth();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHeight method, of class MRSeries.
     */
    @Test
    public void testGetHeight() {
        int expResult = 512;
        int result = instance.getHeight();
        assertEquals(expResult, result);
    }

    /**
     * Test of getLength method, of class MRSeries.
     */
    @Test
    public void testGetLength() {
        int expResult = 160;
        int result = instance.getSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of toImagePlus method, of class MRSeries.
     */
    @Test
    public void testToImagePlus() {
        ImagePlus result = new ImagePlus("", IJUtils.toImageStack(instance.imageArrayXY));
        assertEquals(result.getImageStack().getWidth(), instance.getWidth());
        assertEquals(result.getImageStack().getHeight(), instance.getHeight());
        assertEquals(result.getImageStack().getSize(), instance.getSize());
    }

    /**
     * Test of mip method, of class MRSeries.
     */
    @Test
    public void testMip() {
        instance.mip();
    }

    /**
     * Test of show method, of class MRSeries.
     */
    @Test
    public void testShow() {
        instance.display(1);
    }

    /**
     * Test of getSize method, of class MRSeries.
     */
    @Test
    public void testGetSize() {
        int expResult = 160;
        int result = instance.getSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPixel method, of class MRSeries.
     */
    @Test
    public void testGetPixel() {
        int x = 0;
        int y = 0;
        int z = 0;
        int expResult = 373;
        int result = instance.getPixel(x, y, z);
        assertEquals(expResult, result);
    }

    /**
     * Test of render method, of class MRSeries.
     */
    @Test
    public void testRender() {
        instance.render(2);
    }

}
