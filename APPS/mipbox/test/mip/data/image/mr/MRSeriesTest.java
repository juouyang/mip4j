/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import ij.ImagePlus;
import java.io.File;
import java.io.IOException;
import mip.util.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ju
 */
public class MRSeriesTest {

    MRSeries instance;

    public MRSeriesTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws InterruptedException {
        File seriesRoot = new File(getClass().getClassLoader().getResource("resources/bmr/2/").getFile());
        instance = new MRSeries(IOUtils.listFiles(seriesRoot.getPath()));
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of getImageArrayXY method, of class MRSeries.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testGetImageArrayXY() throws IOException {
        File f = new File(getClass().getClassLoader().getResource("resources/bmr/2/001.dcm").getFile());
        String seriesNumber = new MR(f.getPath()).getSeriesNumber();
        MR[] result = instance.getImageArrayXY();
        assertEquals(seriesNumber, result[0].getSeriesNumber());
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
        String title = "";
        ImagePlus result = instance.toImagePlus(title);
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
        instance.show(1);
    }

}
