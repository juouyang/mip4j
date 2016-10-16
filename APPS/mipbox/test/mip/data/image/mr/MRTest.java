/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import java.io.IOException;
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
public class MRTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    private MR instance;

    public MRTest() throws IOException {
    }

    @Before
    public void setUp() throws IOException {
        instance = MROpener.openMR();
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of getStudyID method, of class MR.
     */
    @Test
    public void testGetStudyID() {
        String expResult = "9527";
        String result = instance.getStudyID();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSeriesNumber method, of class MR.
     */
    @Test
    public void testGetSeriesNumber() {
        String expResult = "2";
        String result = instance.getSeriesNumber();
        assertEquals(expResult, result);
    }

    /**
     * Test of getInstanceNumber method, of class MR.
     */
    @Test
    public void testGetInstanceNumber() {
        String expResult = "80";
        String result = instance.getInstanceNumber();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPatientID method, of class MR.
     */
    @Test
    public void testGetPatientID() {
        String expResult = "001";
        String result = instance.getPatientID();
        assertEquals(expResult, result);
    }

    /**
     * Test of getWidth method, of class Image.
     */
    @Test
    public void testGetWidth() {
        int expResult = 512;
        int result = instance.getWidth();
        assertEquals(expResult, result);
    }

    /**
     * Test of getHeight method, of class Image.
     */
    @Test
    public void testGetHeight() {
        int expResult = 512;
        int result = instance.getHeight();
        assertEquals(expResult, result);
    }

    /**
     * Test of show method, of class MR.
     */
    @Test
    public void testShow() {
        instance.show();
    }

}
