/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import java.io.File;
import java.io.IOException;
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
public class BMRStudyTest {

    BMRStudy instance;

    public BMRStudyTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        File studyRoot = new File(getClass().getClassLoader().getResource("resources/bmr/").getFile());
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
     * Test of MRSeries member, of class BMRStudy.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testAllSereis() throws IOException {
        MRSeriesTest mrsTest = new MRSeriesTest();
        mrsTest.instance = instance.mrs2;
        mrsTest.testGetImageArrayXY();
        mrsTest.instance = instance.mrs3;
        mrsTest.testGetImageArrayXY();
        mrsTest.instance = instance.mrs4;
        mrsTest.testGetImageArrayXY();
    }

}
