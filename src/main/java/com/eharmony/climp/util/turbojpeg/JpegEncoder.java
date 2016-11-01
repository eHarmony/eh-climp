package com.eharmony.climp.util.turbojpeg;

import java.awt.Dimension;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public interface JpegEncoder extends Closeable{
    public InputStream encode(ByteBuffer buffer, Dimension dimension) throws IOException;
    public void encode(FileChannel channel, ByteBuffer buffer, Dimension dimension) throws IOException;
}
