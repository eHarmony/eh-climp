package com.eharmony.climp.util.turbojpeg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.libjpegturbo.turbojpeg.TJException;

import com.eharmony.climp.util.WriteBuffer;

public interface JpegDecoder {

    public abstract void decode(WriteBuffer writeBuffer, InputStream sourceImageInputStream) throws IOException;

    public abstract void decode(WriteBuffer writeBuffer, ByteBuffer srcImage) throws IOException;

}