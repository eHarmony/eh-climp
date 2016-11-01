package com.eharmony.climp.util.turbojpeg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;
import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJDecompressor;
import org.libjpegturbo.turbojpeg.TJException;

import com.eharmony.climp.util.WriteBuffer;

public class TurboJpegDecoder  implements JpegDecoder {
    public void decode(WriteBuffer writeBuffer, InputStream is) throws IOException {
        final byte[] unneccessaryCopy = IOUtils.toByteArray(is);
        decode(writeBuffer, unneccessaryCopy);
    }
    @Override
    public void decode(WriteBuffer writeBuffer, ByteBuffer srcImage) throws IOException {
        // TODO Auto-generated method stub
        final byte [] unneccessaryCopy;
        if (srcImage.hasArray()) {
            unneccessaryCopy = srcImage.array();
        } else {
            unneccessaryCopy = new byte[srcImage.limit()];
            srcImage.get(unneccessaryCopy);
        }
        decode(writeBuffer, unneccessaryCopy);
        
    }

    

    public void decode(WriteBuffer writeBuffer, byte[] unneccessaryCopy) throws IOException {
        try (TJDecompressor decompressor = new TJDecompressor(unneccessaryCopy)) {
            int width = decompressor.getWidth();
            int height = decompressor.getHeight();
            writeBuffer.ensurePixelBuffer(width, height);
            final byte[] unneccessaryCopy2 = new byte[width * height * 4];
            decompressor.decompress(unneccessaryCopy2, 0, 0, width, 0, height, TJ.PF_RGB, TJ.FLAG_FASTDCT);
            writeBuffer.getPixelBuffer().put(unneccessaryCopy2);
            writeBuffer.getPixelBuffer().rewind();
            writeBuffer.createTextureData(width, height);
        } catch (Exception ex) {
            throw new IOException("Error in turbojpeg comressor: " + ex.getMessage(), ex);
        }
    }


}
