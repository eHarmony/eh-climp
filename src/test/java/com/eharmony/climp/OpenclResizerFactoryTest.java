package com.eharmony.climp;

import static org.junit.Assert.*;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OpenclResizerFactoryTest {
    static GenericObjectPool<OpenclResizer> pool;
    OpenclResizer resizer;
    @BeforeClass
    public static void setupTests() {
        // requires TurboJpeg setup
//        OpenclResizerFactory factory = new OpenclResizerFactory("lanczoz3");
//        pool = new GenericObjectPool<OpenclResizer>(factory);
    }
    @AfterClass
    public static void afterTests() {
//        pool.close();
    }
    
    
    @Before
    public void setup () throws Exception{
//        resizer = pool.borrowObject();
        
    }
    @After
    public void tearDown() {
//        pool.returnObject(resizer);
    }

    @Test
    public void testResizer() throws Exception {
        InputStream is = getClass().getResourceAsStream("/ocean.jpg");
//        BufferedImage image = resizer.resize(is, new Dimension(200,200));
//        assertEquals(200,image.getHeight());
//        // maintain aspect ratio
//        assertEquals(150,image.getWidth());
    }

}
