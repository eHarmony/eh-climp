package com.eharmony.climp.util;

/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelBufferProvider;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;

public class ReadBuffer {
    private static final int PIXEL_FORMAT = GL.GL_RGBA;
    protected int readPixelSizeLast = 0;
    protected Buffer readPixelBuffer = null;
    protected TextureData readTextureData = null;
//    protected Texture readTexture = new Texture(GL.GL_TEXTURE_2D);
    protected final GL gl;
    Logger log = LoggerFactory.getLogger(getClass());
    private final GLPixelStorageModes psm;
    private final int tmp[] = new int[1];
    private final GLPixelBufferProvider pixelBufferProvider;
    private final GLPixelAttributes pixelAttribs;    
    public ReadBuffer(GL gl) {
        this.gl = gl;
        psm = new GLPixelStorageModes();
        pixelBufferProvider = GLPixelBuffer.defaultProviderNoRowStride;
        pixelAttribs = new GLPixelAttributes(PIXEL_FORMAT, GL.GL_UNSIGNED_BYTE);        
    }

    public ByteBuffer getPixelBuffer() { return (ByteBuffer) readPixelBuffer; }
    public void rewindPixelBuffer() { readPixelBuffer.rewind(); }

   public TextureData getTextureData() { return readTextureData; }
//    public Texture getTexture() { return readTexture; }

    public boolean isValid() {
      return null!=readPixelBuffer ;
    }

    public void fetchOffscreenTexture(GLDrawable drawable) {
        fetchOffscreenTexture(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    }
    public void fetchOffscreenTexture(int x, int y, int width, int height) {
        log.debug("Fetching offscreen texture");
        final int glerr0 = gl.glGetError();
        if (GL.GL_NO_ERROR != glerr0) {
            throw new IllegalArgumentException("Pre-exisiting GL error 0x" + Integer.toHexString(glerr0));
        }        
        final int readPixelSize = GLBuffers.sizeof(gl, tmp, pixelAttribs.pfmt.comp.bytesPerPixel(), width, height, 1, true);
        boolean newData = false;
        if(readPixelSize>readPixelSizeLast) {
            readPixelBuffer = GLBuffers.newDirectGLBuffer(GL.GL_UNSIGNED_BYTE, readPixelSize);
            readPixelSizeLast = readPixelSize ;
            try {
                readTextureData = new TextureData(gl.getGLProfile(), pixelAttribs.format, width, height, 0, pixelAttribs, false, false, false, readPixelBuffer, null);
            } catch (Exception e) {
                e.printStackTrace();
                readTextureData = null;
                readPixelBuffer = null;
                readPixelSizeLast = 0;
            }
            log.debug("New GLBuffer loaded");

        }
        if(null!=readPixelBuffer) {
            readPixelBuffer.clear();
            psm.setPackAlignment(gl, 1);
            if (gl.isGL2ES3()) {
                final GL2ES3 gl2es3 = gl.getGL2ES3();
                psm.setPackRowLength(gl2es3, width);
                gl2es3.glReadBuffer(gl2es3.getDefaultReadBuffer());
            }            
            gl.glReadPixels(x, y, width, height, pixelAttribs.format, pixelAttribs.type, readPixelBuffer);
            gl.glFlush();
            log.debug("Pixels finished reading");
            readPixelBuffer.rewind();
//            if(newData) {
//                readTexture.updateImage(gl, readTextureData);
//            } else {
//                readTexture.updateSubImage(gl, readTextureData, 0,
//                                           0, 0, // src offset
//                                           0, 0, // dst offset
//                                           width, height);
//            }
        }
    }

}
