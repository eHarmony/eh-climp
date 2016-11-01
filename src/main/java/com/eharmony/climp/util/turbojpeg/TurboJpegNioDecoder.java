package com.eharmony.climp.util.turbojpeg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.io.IOUtils;
import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJDecompressor;
import org.libjpegturbo.turbojpeg.TJException;
import org.libjpegturbo.turbojpeg.bridj.TurbojpegLibrary.TJPF;
import org.libjpegturbo.turbojpeg.nio.TJNioDecompressor;

import com.eharmony.climp.util.WriteBuffer;

public class TurboJpegNioDecoder implements JpegDecoder {
    TJNioDecompressor decompressor;
    public TurboJpegNioDecoder() {
        try {
            decompressor = new TJNioDecompressor();
        } catch (TJException tje) {
            throw new IllegalStateException("Could not initialize TJNioDecompressor",tje);
        }
    }
    /* (non-Javadoc)
     * @see com.eharmony.climp.util.turbojpeg.JpegDecoder#decode(com.eharmony.climp.util.WriteBuffer, java.io.InputStream)
     */
    @Override
    public void decode(WriteBuffer writeBuffer, InputStream sourceImageInputStream) throws IOException {

            // TODO read the jpeg header and know what size of bytebuffer to allocate instead
        
            final byte[] unneccessaryCopy = IOUtils.toByteArray(sourceImageInputStream);
            ByteBuffer srcImage = ByteBuffer.allocateDirect(unneccessaryCopy.length);
            srcImage.put(unneccessaryCopy);
            decode(writeBuffer, srcImage);
    }
    /* (non-Javadoc)
     * @see com.eharmony.climp.util.turbojpeg.JpegDecoder#decode(com.eharmony.climp.util.WriteBuffer, java.nio.ByteBuffer)
     */
    @Override
    public void decode(WriteBuffer writeBuffer, ByteBuffer srcImage) throws IOException {
        try {
            decompressor.setSourceImage(srcImage, TJPF.TJPF_RGB);
            decompressor.decompress(TJ.FLAG_FASTDCT);
            int width = decompressor.getWidth();
            int height = decompressor.getHeight();
            writeBuffer.ensurePixelBuffer(width, height);
            writeBuffer.getPixelBuffer().put(decompressor.getDstBuf());
//            writeBuffer.getPixelBuffer().limit(decompressor.getDstBuf().limit());
            writeBuffer.rewindPixelBuffer();
            writeBuffer.createTextureData(width, height);
        } catch (TJException ex) {
            throw new IOException("Error in turbojpeg compressor", ex);
        }
    }
    public void close() throws IOException {
        try {
            if (this.decompressor != null) {
                this.decompressor.close();
            }
            this.decompressor = null;
        } catch (TJException e) {
            throw new IOException(e);

        }
    }

}
