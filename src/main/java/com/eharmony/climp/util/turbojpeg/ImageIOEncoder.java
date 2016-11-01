package com.eharmony.climp.util.turbojpeg;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ImageIOEncoder implements JpegEncoder {

    @Override
    public void close() throws IOException {
        // nothing to do
    }

    @Override
    public InputStream encode(ByteBuffer buffer, Dimension dimension) throws IOException {
//        ImageIO.writ
//        IO
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void encode(FileChannel channel, ByteBuffer buffer, Dimension dimension) throws IOException {
        // TODO Auto-generated method stub

    }

}
