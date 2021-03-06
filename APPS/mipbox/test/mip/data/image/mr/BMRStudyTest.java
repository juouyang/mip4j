/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import java.io.File;
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
public class BMRStudyTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }
    private BMRStudy instance;

    public BMRStudyTest() {
    }

    @Before
    public void setUp() {
        File studyRoot = new File(BMRStudy.SBMR);
        instance = new BMRStudy(studyRoot.toPath());
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of getPatientID method, of class BMRStudy.
     */
    @Test
    public void testGetPatientID() {
        String expResult = "001";
        String result = instance.getPatientID();
        assertEquals(expResult, result);
    }

    /**
     * Test of getStudyID method, of class BMRStudy.
     */
    @Test
    public void testGetStudyID() {
        String expResult = "9527";
        String result = instance.getStudyID();
        assertEquals(expResult, result);
    }

    /**
     * Test of getPixel method, of class BMRStudy.
     */
    @Test
    public void testGetPixel() {
        int x = 0;
        int y = 0;
        int z = 0;
        int t = 0;
        int expResult = 373;
        int result = instance.getPixel(x, y, z, t);
        assertEquals(expResult, result);
    }

}
