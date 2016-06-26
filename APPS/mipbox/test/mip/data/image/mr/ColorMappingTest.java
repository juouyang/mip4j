/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mip.data.image.mr;

import java.io.File;
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
public class ColorMappingTest {

    ColorMapping instance;

    public ColorMappingTest() {
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
        instance = new ColorMapping(new BMRStudy(studyRoot.toPath()));
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of show method, of class ColorMapping.
     */
    @Test
    public void testShow() {
        instance.show();
    }

    /**
     * Test of render method, of class ColorMapping.
     */
    @Test
    public void testRender() {
        instance.render();
    }

    /**
     * Test of getColorMapping method, of class ColorMapping.
     */
    @Test
    public void testGetColorMapping() {
        int x = 0;
        int y = 0;
        int z = 0;
        int expResult = 6;
        int result = instance.getColorMapping(x, y, z);
        assertEquals(expResult, result);
    }

}
