package com.eharmony.climp;

import static org.junit.Assert.*;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;




public class OpenclResizerTest {
    public static OpenclResizer resizer;
    @BeforeClass
    public static void setup() {
        // requires turbojpeg currently
//        resizer = new OpenclResizer("lanczoz3");
    }
    @AfterClass
    public static void tearDown() throws Exception {
//        resizer.close();
    }

    @Test
    public void testResizeInputStream() throws Exception {
        InputStream is = getClass().getResourceAsStream("/ocean.jpg");
//        BufferedImage image = resizer.resize(is, new Dimension(200,200));
//        assertEquals(200,image.getHeight());
//        // maintain aspect ratio
//        assertEquals(150,image.getWidth());
        
    }
    
    @Test
    public void testResizeInputStreamList() throws Exception {
        InputStream is = getClass().getResourceAsStream("/ocean.jpg");
        ArrayList<Dimension> dimensions = Lists.newArrayList(new Dimension(200,200), new Dimension(100,100));
//        List<BufferedImage> images = resizer.resize(is, dimensions);
//        BufferedImage image = images.get(0);
//        assertEquals(200,image.getHeight());
//        // maintain aspect ratio
//        assertEquals(150,image.getWidth());
//        image = images.get(1);
//        assertEquals(100,image.getHeight());
//        // maintain aspect ratio
//        assertEquals(75,image.getWidth());
        
    }
    

}
