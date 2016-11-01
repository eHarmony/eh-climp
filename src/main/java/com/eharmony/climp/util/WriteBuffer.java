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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;

public class WriteBuffer {
    protected int writePixelSizeLast = 0;
    protected Buffer writePixelBuffer = null;
    protected TextureData writeTextureData = null;
    protected Texture writeTexture = new Texture(GL.GL_TEXTURE_2D);

    protected GL gl = null;
    public WriteBuffer(GL gl) {
        this.gl = gl;
    }   

    public ByteBuffer getPixelBuffer() { return (ByteBuffer) writePixelBuffer; }
    public void rewindPixelBuffer() { writePixelBuffer.rewind(); }
    public Texture getTexture() { return writeTexture;}
    public TextureData getTextureData() { return writeTextureData; }
    public GLProfile getGLProfile() { return gl.getGLProfile(); }
    boolean hasNewData = false;
    public boolean isValid() {
      return null!=writeTextureData && null!=writePixelBuffer ;
    }

    public void ensurePixelBuffer(int width, int height) {
        
        int writePixelSize = width * height * 4 ; // BGRA
//        if(writePixelSize>writePixelSizeLast) {
        //TODO: Reusing the same write pixel buffer has severe issues. Not 100% sure why. I think we have to pass in
        // the real texture coordinates to the resize method
        if (true) {
            writePixelBuffer = GLBuffers.newDirectGLBuffer(GL.GL_UNSIGNED_BYTE, writePixelSize);
            writePixelSizeLast = writePixelSize ;
            hasNewData = true;
        } else {
            writePixelBuffer.clear();
            writePixelBuffer.rewind();
            hasNewData = false;
        }
    }
    public void createTextureData(int width, int height) {
        writeTextureData = new TextureData(
                gl.getGLProfile(),
                // gl.isGL2GL3()?gl.GL_RGBA:gl.GL_RGB,
                GL.GL_RGB,
                width, height,
                0, // border
                GL.GL_RGB,
                GL.GL_UNSIGNED_BYTE,
                false, //mipmap
                false, //compressed
                false /* flip */,
                writePixelBuffer,
                null /* Flusher */);
        if (hasNewData) {
            writeTexture.updateImage(gl, writeTextureData);
        } else {
            writeTexture.updateSubImage(gl, writeTextureData, 0,
                                       0, 0, // src offset
                                       0, 0, // dst offset
                                       width, height);
        }
        
    }

}
