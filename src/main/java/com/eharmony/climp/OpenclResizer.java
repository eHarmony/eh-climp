package com.eharmony.climp;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;

import org.libjpegturbo.turbojpeg.bridj.TurbojpegLibrary.TJPF;
import org.libjpegturbo.turbojpeg.bridj.TurbojpegLibrary.TJSAMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eharmony.climp.util.ReadBuffer;
import com.eharmony.climp.util.WriteBuffer;
import com.eharmony.climp.util.turbojpeg.JpegDecoder;
import com.eharmony.climp.util.turbojpeg.JpegEncoder;
import com.eharmony.climp.util.turbojpeg.TurboJpegDecoder;
import com.eharmony.climp.util.turbojpeg.TurboJpegNioDecoder;
import com.eharmony.climp.util.turbojpeg.TurboJpegNioEncoder;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class OpenclResizer implements Closeable {
    private final GLCapabilities caps;
    private final GLDrawableFactory factory;
    CLProgram program;
    private final static String GL_EXT_TEXTURE_FILTER_ANISOTROPIC_STRING = "GL_EXT_texture_filter_anisotropic";
    private final static boolean MIPMAP_FILTER_DISABLED = false;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ShaderCode rsVp;
    private ShaderCode rsFp;
    private JpegDecoder decoder = new TurboJpegNioDecoder();
    private final static boolean PERFORMANCE_TIMING = false;

    private String shaderBasename;

    float[] clearColor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    private static final float[] s_quadVertices = { -1f, -1f, 0f, // LB
            1f, -1f, 0f, // RB
            -1f, 1f, 0f, // LT
            1f, 1f, 0f // RT
    };
    private static final float[] s_quadColors = { 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f };
    private static final float[] s_quadTexCoords = { 0f, 0f, // LB
            1f, 0f, // RB
            0f, 1f, // LT
            1f, 1f // RT
    };

    private ShaderState st;
    private GLArrayDataServer interleavedVBO;
    private GLUniformData pmvMatrixUniform;
    private PMVMatrix pmvMatrix;
    ShaderProgram sp;
    GLOffscreenAutoDrawable.FBO permanentbuffer;
    GL2ES2 gl = null;
    private final static int MAX_DIMENSION = 2560;
    private AWTGLReadBufferUtil noAlphaBufferUtil;
    final ReadBuffer readBuffer;
    final WriteBuffer sourceBuffer; 
    public OpenclResizer(String shaderBasename) {

        this.shaderBasename = shaderBasename;
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
        caps = new GLCapabilities(glp);
        caps.setDoubleBuffered(false);
        caps.setOnscreen(false);
        caps.setAlphaBits(8);  
        caps.setRedBits(8);
        caps.setBlueBits(8);
        caps.setGreenBits(8);
        caps.setHardwareAccelerated(true);
        caps.setFBO(true);
        caps.setPBuffer(false);
        caps.setOnscreen(false);
        log.info("Using:{} caps:{}", caps.getGLProfile().getImplName(), caps.toString());


        // Make a pbuffer to get an offscreen context
        factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        if (!factory.canCreateFBO(null, caps.getGLProfile())) {
         throw new IllegalStateException("FBO support not available (required to run this resizer)");
         }

//		if (!factory.canCreateGLPbuffer(null, caps.getGLProfile())) {
//			throw new IllegalStateException("pbuffer support not available (required to run this demo)");
//		}
        initOpenGL();
        // create buffers for transfering in and out of opengl
        readBuffer = new ReadBuffer(gl);
        sourceBuffer = new WriteBuffer(gl);

    }

    void initOpenGL() {
        permanentbuffer = (GLOffscreenAutoDrawable.FBO) factory.createOffscreenAutoDrawable(null, caps, null, MAX_DIMENSION,
                MAX_DIMENSION);

        try {
            permanentbuffer.display(); // force init
            permanentbuffer.getContext().makeCurrent();
            gl = permanentbuffer.getGL().getGL2ES2();

            st = initShader(gl);
            log.info("Hardware:{} caps:{}", permanentbuffer.getContext().isHardwareRasterizer(), permanentbuffer
                    .getContext().toString());

            // setup mgl_PMVMatrix
            pmvMatrix = new PMVMatrix();
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 10.0f);
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();
            pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf()); // P,
            // Mv

            st.ownUniform(pmvMatrixUniform);
            if (!st.uniform(gl, pmvMatrixUniform)) {
                throw new GLException("Error setting PMVMatrix in shader: " + st);
            }
            if (!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", 0))) {
                throw new GLException("Error setting mgl_ActiveTexture in shader: " + st);
            }
            interleavedVBO = fillShaderState(st, gl);
            interleavedVBO.enableBuffer(gl, true);

            // OpenGL Render Settings
            gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            
            noAlphaBufferUtil = new AWTGLReadBufferUtil(caps.getGLProfile(), false);

        } catch (Exception e) {
            log.error("Unexpected exception for " + st, e);
        } finally {
            // st.release(gl, true, true, false);
            // sp.release(gl,false);
            // pbuffer.destroy();
        }
    }

    public ResizedImageContext resize(RenderedImage image, Dimension targetDimension) {
        log.debug("Resizing to {}x{} from rendered image", targetDimension.getWidth(), targetDimension.getHeight());

        BufferedImage imgToResize;
        if (image instanceof PlanarImage) {
            imgToResize = ((PlanarImage) image).getAsBufferedImage();
        } else if (image instanceof BufferedImage) {
            imgToResize = (BufferedImage) image;
        } else {
            throw new IllegalArgumentException("Don't know how resize image of type [" + image.getClass().getName()
                    + "].");
        }
        return resizeOpenGl(imgToResize, targetDimension);
    }

    public List<ResizedImageContext> resize(RenderedImage image, List<Dimension> targetDimensions) {
        log.debug("Resizing {} sizes from rendered image", targetDimensions.size());

        BufferedImage imgToResize;
        if (image instanceof PlanarImage) {
            imgToResize = ((PlanarImage) image).getAsBufferedImage();
        } else if (image instanceof BufferedImage) {
            imgToResize = (BufferedImage) image;
        } else {
            throw new IllegalArgumentException("Don't know how resize image of type [" + image.getClass().getName()
                    + "].");
        }
        Texture texture = null;
        try {
            // permanentbuffer.display(); // force init
            // permanentbuffer.getContext().makeCurrent();
            texture = AWTTextureIO.newTexture(GLProfile.getGL2ES2(), imgToResize, MIPMAP_FILTER_DISABLED);

            final Texture t = texture;
            log.debug("Texture size {}", t.getEstimatedMemorySize());
            // need to evaluate everything before we return so we can clean up
            // the texture properly
            return targetDimensions.stream()
                    .map(targetDimension -> { return resizeOpenGl(t, targetDimension);})
                    .collect(Collectors.toList());
            
        } finally {
            if (texture != null) {
                texture.destroy(gl);
            }
        }
    }
    public ResizedImageContext resize(InputStream is, Dimension targetDimension) throws IOException {
        log.debug("Reading texture into BufferedImage");
//        BufferedImage argbImg = ImageIO.read(is);
//        return resize(argbImg, targetDimension);
        Texture texture = null;
        try {
            permanentbuffer.display(); // force init
            permanentbuffer.getContext().makeCurrent();
            st.useProgram(gl, true);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            log.debug("Reading texturedata read");
            decoder.decode(sourceBuffer, is);
            texture = sourceBuffer.getTexture();
            log.debug("Reading textureData created");
            return resizeOpenGl(texture, targetDimension);
        } finally {
            log.info("Finished resize");

        }
        
    }
    
    public ResizedImageContext resize(ByteBuffer buffer, Dimension targetDimension) throws IOException {
        log.debug("Reading texture into BufferedImage");
//        BufferedImage argbImg = ImageIO.read(is);
//        return resize(argbImg, targetDimension);
        Texture texture = null;
        try {
            permanentbuffer.display(); // force init
            permanentbuffer.getContext().makeCurrent();
            st.useProgram(gl, true);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            log.debug("Reading texturedata read");
            decoder.decode(sourceBuffer, buffer);
            texture = sourceBuffer.getTexture();
            log.debug("Reading textureData created");
            return resizeOpenGl(texture, targetDimension);
        } finally {
            log.info("Finished resize");

        }
        
    }
    
    

    public List<ResizedImageContext> resize(InputStream is, List<Dimension> targetDimensions) throws IOException {
        log.debug("Reading texture into BufferedImages");
        BufferedImage argbImg = ImageIO.read(is);
        return resize(argbImg, targetDimensions);
    }


    public ResizedImageContext resizeOpenGl(BufferedImage imgToResize, Dimension dimension) {
        Texture texture = null;
        try {
            permanentbuffer.display(); // force init
            permanentbuffer.getContext().makeCurrent();
            st.useProgram(gl, true);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            texture = AWTTextureIO.newTexture(GLProfile.getGL2ES2(), imgToResize, MIPMAP_FILTER_DISABLED);
            return resizeOpenGl(texture, dimension);
        } finally {
            if (texture != null) {
                texture.destroy(gl);
            }
        }

    }

    synchronized public ResizedImageContext resizeOpenGl(Texture t, Dimension dimension) {
        Dimension targetDimension = getOptimalDimensionToResize(t, dimension);

        log.debug("Resizing to width:" + targetDimension.width + " height:" + targetDimension.height);
        if (targetDimension.height > MAX_DIMENSION || targetDimension.width > MAX_DIMENSION) {
            throw new IllegalArgumentException("Image size must be smaller than "+ MAX_DIMENSION);
            
        }

        gl.glViewport(0, 0, targetDimension.width, targetDimension.height);

        st.useProgram(gl, true);


//        if (null != t) {
//            // fetch the flipped texture coordinates
//            t.getImageTexCoords().getST_LB_RB_LT_RT(s_quadTexCoords, 0, 1f, 1f);
//        }

        // setup mgl_PMVMatrix
        if (!st.uniform(gl, new GLUniformData("destWidth", (float) targetDimension.width))) {
            throw new GLException("Error setting destWidth in shader: " + st);
        }
        if (!st.uniform(gl, new GLUniformData("destHeight", (float) targetDimension.height))) {
            throw new GLException("Error setting destHeight in shader: " + st);
        }

        gl.glActiveTexture(GL.GL_TEXTURE0);
        t.enable(gl);
        t.bind(gl);
        if (PERFORMANCE_TIMING) {
            gl.glFinish();
            log.debug("Real start");
        }
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        st.useProgram(gl, false);

        if (PERFORMANCE_TIMING) {
            gl.glFinish();
            log.debug("Finished resizing");
        }
        
//        BufferedImage retval = noAlphaBufferUtil.readPixelsToBufferedImage(gl, 0, 0, targetDimension.width,
//                targetDimension.height, false);
//        log.debug("Extracted BufferedImage");
        
//        boolean success = bufferUtil.readPixels(gl, 0, 0, targetDimension.width,
//              targetDimension.height, false);
//        if (success) {
//        } else {
//            return null
//        }
        readBuffer.fetchOffscreenTexture(0, 0, targetDimension.width, targetDimension.height);
        log.debug("Fetched offscreen");
        return new ResizedImageContext(readBuffer.getPixelBuffer(),targetDimension);

    }

    public GLArrayDataServer fillShaderState(ShaderState st, final GL2ES2 gl) {
        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3 + 4 + 2, GL.GL_FLOAT, false, 3 * 4,
                GL.GL_STATIC_DRAW);
        {
            interleavedVBO.addGLSLSubArray("mgl_Vertex", 3, GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_Color", 4, GL.GL_ARRAY_BUFFER);
            // interleavedVBO.addGLSLSubArray("mgl_Normal", 3,
            // GL.GL_ARRAY_BUFFER);
            interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);

            final FloatBuffer ib = (FloatBuffer) interleavedVBO.getBuffer();

            for (int i = 0; i < 4; i++) {
                ib.put(s_quadVertices, i * 3, 3);
                ib.put(s_quadColors, i * 4, 4);
                // ib.put(s_cubeNormals, i*3, 3);
                ib.put(s_quadTexCoords, i * 2, 2);
            }
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);
        st.ownAttribute(interleavedVBO, true);
        return interleavedVBO;
    }

    private ShaderState initShader(final GL2ES2 gl) {
        // load the shaders
        rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "/shaders", "shader/bin",
                shaderBasename, true);
        rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "/shaders", "shader/bin",
                shaderBasename, true);
        // Create & Compile the shader objects
        rsVp.defaultShaderCustomization(gl, true, true);
        rsFp.defaultShaderCustomization(gl, true, true);

        // create the shader program and link
        sp = new ShaderProgram();
        sp.init(gl);
        if (!sp.add(gl, rsVp, System.err)) {
            log.error("Could not link Vertex Shader");
        }
        ;
        if (!sp.add(gl, rsFp, System.err)) {
            log.error("Could not link Fragment Shader");
        }
        ;
        if (!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: " + sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, true);

        int loc = st.getUniformLocation(gl, "mgl_ActiveTexture");

        return st;
    }

    private Dimension getOptimalDimensionToResize(BufferedImage image, Dimension targetDimension) {

        if (targetDimension.getWidth() != targetDimension.getHeight()) {
            // requested for exact dimension
            return targetDimension;

        }

        final int width;
        final int height;
        double maximumDimension = targetDimension.getHeight();
        double ratio = (double) image.getHeight() / (double) image.getWidth();
        if (ratio > 1.0) {
            // height > width
            height = (int) maximumDimension;
            width = (int) Math.round((maximumDimension / (double) image.getHeight()) * (double) image.getWidth());
        } else {
            // width >= height
            width = (int) maximumDimension;
            height = (int) Math.round((maximumDimension / (double) image.getWidth()) * (double) image.getHeight());
        }
        return new Dimension(width, height);
    }

    private Dimension getOptimalDimensionToResize(Texture t, Dimension targetDimension) {

        if (targetDimension.getWidth() != targetDimension.getHeight()) {
            // requested for exact dimension
            return targetDimension;

        }

        final int width;
        final int height;
        double maximumDimension = targetDimension.getHeight();
        double ratio = (double) t.getHeight() / (double) t.getWidth();
        if (ratio > 1.0) {
            // height > width
            height = (int) maximumDimension;
            width = (int) Math.round((maximumDimension / (double) t.getHeight()) * (double) t.getWidth());
        } else {
            // width >= height
            width = (int) maximumDimension;
            height = (int) Math.round((maximumDimension / (double) t.getWidth()) * (double) t.getHeight());
        }
        return new Dimension(width, height);
    }
    public static class ResizedImageContext {
        public final ByteBuffer buffer;
        public final Dimension dimension;
        public ResizedImageContext(ByteBuffer buffer, Dimension dimension) {
            super();
            this.buffer = buffer;
            this.dimension = dimension;
        }
        public ByteBuffer getBuffer() {
            return buffer;
        }
        public Dimension getDimension() {
            return dimension;
        }
        
    }
    
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        OpenclResizer resizer = new OpenclResizer("lanczoz3");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        JpegEncoder encoder = new TurboJpegNioEncoder(TJPF.TJPF_RGBX,90,TJSAMP.TJSAMP_444);
//        JpegEncoder encoder= new TurboJpegEncoder(TJ.PF_RGBX,85,TJ.SAMP_420);
        try {
            while (true) {
                System.out.println("Enter file location:");
                String filename = null;
                try {
                    filename = reader.readLine().trim();
                    if (filename.length() > 0) {
                        long startNanos = System.nanoTime();

                        // BufferedImage image = ImageIO.read(new
                        // File(filename));
                        File f = new File(filename);
                        if (f.exists() || f.canRead()) { 
                            try (RandomAccessFile srcFile = new RandomAccessFile(f,"r")) {
                                ByteBuffer out = srcFile.getChannel().map(MapMode.READ_ONLY, 0, srcFile.length());
                                ResizedImageContext resizedImage = resizer.resize(out,new Dimension(600,600));
                                long midNanos = System.nanoTime();
                                File file = new File(filename + ".new.jpg");
                                if (file.exists()) {
                                    file.delete();
                                }
                                try (RandomAccessFile destFile = new RandomAccessFile(file,"rwd")) {                       
        //                        InputStream newImage = encoder.encode(resizedImage.getBuffer(), resizedImage.getDimension());
        //                        FileUtils.copyInputStreamToFile(newImage, file);
                                    FileChannel channel = destFile.getChannel();
                                    encoder.encode(channel, resizedImage.getBuffer(),resizedImage.getDimension());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                long endNanos = System.nanoTime();
                                System.out.println("Finished reading and resizing in " + (midNanos - startNanos) / 1000000
                                        + "ms, writing in " + (endNanos - midNanos) / 1000000 + "ms");
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not generate comparison for " + filename + " due to: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        } finally {
            resizer.close();
        }
    }

    @Override
    public void close() throws IOException {
        this.st.release(gl, true, true, true);
        this.permanentbuffer.destroy();
    }

}