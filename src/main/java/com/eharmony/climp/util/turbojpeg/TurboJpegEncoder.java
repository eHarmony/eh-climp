package com.eharmony.climp.util.turbojpeg;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.apache.commons.io.IOUtils;
import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJCompressor;
import org.libjpegturbo.turbojpeg.TJDecompressor;
import org.libjpegturbo.turbojpeg.TJException;
import org.libjpegturbo.turbojpeg.bridj.TurbojpegLibrary.TJSAMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eharmony.climp.util.WriteBuffer;
import com.google.common.io.ByteStreams;

public class TurboJpegEncoder implements JpegEncoder {
    TJCompressor compressor = null;
    private final int pixelFormat;
    private final int jpegQuality;
    private final int subSampling;
    Logger log = LoggerFactory.getLogger(getClass());

    public TurboJpegEncoder(int pixelFormat, int jpegQuality, int subSampling) throws TJException {
        this.compressor = new TJCompressor();
        this.pixelFormat = pixelFormat;
        this.jpegQuality = jpegQuality;
        this.subSampling = subSampling;
    }

    public InputStream encode(ByteBuffer buffer, Dimension dimension) throws IOException {

        final byte[] unneccessaryCopy;
        if (buffer.hasArray()) {
            unneccessaryCopy = buffer.array();
        } else {
            unneccessaryCopy = new byte[buffer.remaining()];
            buffer.get(unneccessaryCopy);
        }
        // TODO Currently this only works with RGBX, I don't know why
        log.debug("Creating compressor");
                compressor.setSourceImage(unneccessaryCopy, 0, 0, dimension.width, 0, dimension.height, pixelFormat);
        // todo switch to http://bridj.googlecode.com/ so it lets you use pointers
        // https://searchcode.com/codesearch/view/38304416/
        // http://www.massapi.com/source/bitbucket/74/53/745331507/src/turbojpeg/test/LibJpegTurboTests.java.html

        compressor.setJPEGQuality(jpegQuality);
        compressor.setSubsamp(subSampling);

        byte[] compress = compressor.compress(0);
        log.debug("Finished compression");
        return new ByteArrayInputStream(compress, 0, compressor.getCompressedSize());
    }

    public void encode(FileChannel channel, ByteBuffer buffer, Dimension dimension) throws IOException {
        final byte[] unneccessaryCopy;
        if (buffer.hasArray()) {
            unneccessaryCopy = buffer.array();
        } else {
            unneccessaryCopy = new byte[buffer.remaining()];
            buffer.get(unneccessaryCopy);
        }
        // TODO Currently this only works with RGBX, I don't know why
        compressor.setSourceImage(unneccessaryCopy, 0, 0, dimension.width, 0, dimension.height, pixelFormat);
        // todo switch to http://bridj.googlecode.com/ so it lets you use pointers
        // https://searchcode.com/codesearch/view/38304416/
        // http://www.massapi.com/source/bitbucket/74/53/745331507/src/turbojpeg/test/LibJpegTurboTests.java.html

        compressor.setJPEGQuality(85);
        compressor.setSubsamp(subSampling);
        
        byte[] compress = compressor.compress(0);
        log.debug("Finished compression");
        channel.write(ByteBuffer.wrap(compress, 0, compressor.getCompressedSize()));
        log.debug("Completed writing compressed file");
        
    }

    @Override
    public void close() throws IOException {
        if (compressor != null) {
            compressor.close();
            compressor = null;
        }
        
    }

}
