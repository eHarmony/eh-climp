package com.eharmony.climp.util.turbojpeg;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.libjpegturbo.turbojpeg.TJException;
import org.libjpegturbo.turbojpeg.bridj.TurbojpegLibrary.TJPF;
import org.libjpegturbo.turbojpeg.bridj.TurbojpegLibrary.TJSAMP;
import org.libjpegturbo.turbojpeg.nio.TJNioCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eharmony.climp.util.ByteBufferBackedInputStream;

public class TurboJpegNioEncoder implements JpegEncoder {
    private final TJPF pixelFormat;
    private TJNioCompressor compressor = new TJNioCompressor();
    private int jpegQuality;
    private TJSAMP subSampling;

    public TurboJpegNioEncoder(TJPF pixelFormat, int jpegQuality, TJSAMP subSampling) throws TJException {
        this.pixelFormat = pixelFormat;
        this.jpegQuality = jpegQuality;
        this.subSampling = subSampling;
    }

    Logger log = LoggerFactory.getLogger(getClass());

    public InputStream encode(ByteBuffer buffer, Dimension dimension) throws IOException {

        log.debug("Copying bytes");
        final byte[] unneccessaryCopy;
        if (buffer.hasArray()) {
            unneccessaryCopy = buffer.array();
        } else {
            unneccessaryCopy = new byte[buffer.remaining()];
            buffer.get(unneccessaryCopy);
        }
        log.debug("Creating compressor");
        compressor.setSourceImage(buffer, dimension.width, 0 // pitch
                , dimension.height, pixelFormat, subSampling);
        // This uses http://bridj.googlecode.com/ so it lets you use pointers
        // https://searchcode.com/codesearch/view/38304416/
        // http://www.massapi.com/source/bitbucket/74/53/745331507/src/turbojpeg/test/LibJpegTurboTests.java.html

        compressor.setJPEGQuality(jpegQuality);
        ByteBuffer jpegBuf = compressor.compress(0);
        log.debug("Finished compression");
        return new ByteBufferBackedInputStream(jpegBuf);
    }

    public void encode(FileChannel channel, ByteBuffer buffer, Dimension dimension) throws IOException {

        log.debug("Creating compressor");
        compressor.setSourceImage(buffer, dimension.width, 0 // pitch
                , dimension.height, pixelFormat, subSampling);

        compressor.setJPEGQuality(jpegQuality);
        ByteBuffer jpegBuf = compressor.compress(0);
        jpegBuf.rewind();
        log.debug("Finished compression");
        channel.write(jpegBuf);

        log.debug("Completed writing compressed file");
    }

    public void close() throws IOException {
        try {
            if (this.compressor != null) {
                this.compressor.close();
            }
            this.compressor = null;
        } catch (TJException e) {
            throw new IOException(e);

        }
    }

}
