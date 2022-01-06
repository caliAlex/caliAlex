package a4;

import java.nio.*;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.common.nio.Buffers;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;

public class Utils
{	public Utils() {
}

    public static int createShaderProgram(String vS, String tCS, String tES, String gS, String fS)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int vShader  = prepareShader(GL_VERTEX_SHADER, vS);
        int tcShader = prepareShader(GL_TESS_CONTROL_SHADER, tCS);
        int teShader = prepareShader(GL_TESS_EVALUATION_SHADER, tES);
        int gShader = prepareShader(GL_GEOMETRY_SHADER, gS);
        int fShader  = prepareShader(GL_FRAGMENT_SHADER, fS);
        int vtgfprogram = gl.glCreateProgram();
        gl.glAttachShader(vtgfprogram, vShader);
        gl.glAttachShader(vtgfprogram, tcShader);
        gl.glAttachShader(vtgfprogram, teShader);
        gl.glAttachShader(vtgfprogram, gShader);
        gl.glAttachShader(vtgfprogram, fShader);
        finalizeProgram(vtgfprogram);
        return vtgfprogram;
    }

    public static int createShaderProgram(String vS, String tCS, String tES, String fS)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int vShader  = prepareShader(GL_VERTEX_SHADER, vS);
        int tcShader = prepareShader(GL_TESS_CONTROL_SHADER, tCS);
        int teShader = prepareShader(GL_TESS_EVALUATION_SHADER, tES);
        int fShader  = prepareShader(GL_FRAGMENT_SHADER, fS);
        int vtfprogram = gl.glCreateProgram();
        gl.glAttachShader(vtfprogram, vShader);
        gl.glAttachShader(vtfprogram, tcShader);
        gl.glAttachShader(vtfprogram, teShader);
        gl.glAttachShader(vtfprogram, fShader);
        finalizeProgram(vtfprogram);
        return vtfprogram;
    }

    public static int createShaderProgram(String vS, String gS, String fS)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int vShader  = prepareShader(GL_VERTEX_SHADER, vS);
        int gShader = prepareShader(GL_GEOMETRY_SHADER, gS);
        int fShader  = prepareShader(GL_FRAGMENT_SHADER, fS);
        int vgfprogram = gl.glCreateProgram();
        gl.glAttachShader(vgfprogram, vShader);
        gl.glAttachShader(vgfprogram, gShader);
        gl.glAttachShader(vgfprogram, fShader);
        finalizeProgram(vgfprogram);
        return vgfprogram;
    }

    public static int createShaderProgram(String vS, String fS)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int vShader  = prepareShader(GL_VERTEX_SHADER, vS);
        int fShader  = prepareShader(GL_FRAGMENT_SHADER, fS);
        int vfprogram = gl.glCreateProgram();
        gl.glAttachShader(vfprogram, vShader);
        gl.glAttachShader(vfprogram, fShader);
        finalizeProgram(vfprogram);
        return vfprogram;
    }

    public static int finalizeProgram(int sprogram)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int[] linked = new int[1];
        gl.glLinkProgram(sprogram);
        checkOpenGLError();
        gl.glGetProgramiv(sprogram, GL_LINK_STATUS, linked, 0);
        if (linked[0] != 1)
        {	System.out.println("linking failed");
            printProgramLog(sprogram);
        }
        return sprogram;
    }

    private static int prepareShader(int shaderTYPE, String shader)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int[] shaderCompiled = new int[1];
        String shaderSource[] = readShaderSource(shader);
        int shaderRef = gl.glCreateShader(shaderTYPE);
        gl.glShaderSource(shaderRef, shaderSource.length, shaderSource, null, 0);
        gl.glCompileShader(shaderRef);
        checkOpenGLError();
        gl.glGetShaderiv(shaderRef, GL_COMPILE_STATUS, shaderCompiled, 0);
        if (shaderCompiled[0] != 1)
        {	if (shaderTYPE == GL_VERTEX_SHADER) System.out.print("Vertex ");
            if (shaderTYPE == GL_TESS_CONTROL_SHADER) System.out.print("Tess Control ");
            if (shaderTYPE == GL_TESS_EVALUATION_SHADER) System.out.print("Tess Eval ");
            if (shaderTYPE == GL_GEOMETRY_SHADER) System.out.print("Geometry ");
            if (shaderTYPE == GL_FRAGMENT_SHADER) System.out.print("Fragment ");
            System.out.println("shader compilation error.");
            printShaderLog(shaderRef);
        }
        return shaderRef;
    }

    private static String[] readShaderSource(String filename)
    {	Vector<String> lines = new Vector<String>();
        Scanner sc;
        String[] program;
        try
        {	sc = new Scanner(new File(filename));
            while (sc.hasNext())
            {	lines.addElement(sc.nextLine());
            }
            program = new String[lines.size()];
            for (int i = 0; i < lines.size(); i++)
            {	program[i] = (String) lines.elementAt(i) + "\n";
            }
        }
        catch (IOException e)
        {	System.err.println("IOException reading file: " + e);
            return null;
        }
        return program;
    }

    private static void printShaderLog(int shader)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int[] len = new int[1];
        int[] chWrittn = new int[1];
        byte[] log = null;

        // determine the length of the shader compilation log
        gl.glGetShaderiv(shader, GL_INFO_LOG_LENGTH, len, 0);
        if (len[0] > 0)
        {	log = new byte[len[0]];
            gl.glGetShaderInfoLog(shader, len[0], chWrittn, 0, log, 0);
            System.out.println("Shader Info Log: ");
            for (int i = 0; i < log.length; i++)
            {	System.out.print((char) log[i]);
            }
        }
    }

    public static void printProgramLog(int prog)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int[] len = new int[1];
        int[] chWrittn = new int[1];
        byte[] log = null;

        // determine length of the program compilation log
        gl.glGetProgramiv(prog, GL_INFO_LOG_LENGTH, len, 0);
        if (len[0] > 0)
        {	log = new byte[len[0]];
            gl.glGetProgramInfoLog(prog, len[0], chWrittn, 0, log, 0);
            System.out.println("Program Info Log: ");
            for (int i = 0; i < log.length; i++)
            {	System.out.print((char) log[i]);
            }
        }
    }

    public static boolean checkOpenGLError()
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        boolean foundError = false;
        GLU glu = new GLU();
        int glErr = gl.glGetError();
        while (glErr != GL_NO_ERROR)
        {	System.err.println("glError: " + glu.gluErrorString(glErr));
            foundError = true;
            glErr = gl.glGetError();
        }
        return foundError;
    }

    public static int loadTexture(String textureFileName)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        int finalTextureRef;
        Texture tex = null;
        try { tex = TextureIO.newTexture(new File(textureFileName), false); }
        catch (Exception e) { e.printStackTrace(); }
        finalTextureRef = tex.getTextureObject();

        // building a mipmap and use anisotropic filtering
        gl.glBindTexture(GL_TEXTURE_2D, finalTextureRef);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
        if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic"))
        {	float anisoset[] = new float[1];
            gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, anisoset, 0);
            gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisoset[0]);
        }
        return finalTextureRef;
    }

    public static int loadTextureAWT(String textureFileName)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();
        BufferedImage textureImage = getBufferedImage(textureFileName);
        byte[ ] imgRGBA = getRGBAPixelData(textureImage, true);
        ByteBuffer rgbaBuffer = Buffers.newDirectByteBuffer(imgRGBA);

        int[ ] textureIDs = new int[1];				// array to hold generated texture IDs
        gl.glGenTextures(1, textureIDs, 0);
        int textureID = textureIDs[0];				// ID for the 0th texture object
        gl.glBindTexture(GL_TEXTURE_2D, textureID);	// specifies the currently active 2D texture
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,	// MIPMAP Level, number of color components
                textureImage.getWidth(), textureImage.getHeight(), 0,	// image size, border (ignored)
                GL_RGBA, GL_UNSIGNED_BYTE,				// pixel format and data type
                rgbaBuffer);						// buffer holding texture data

        // build a mipmap and use anisotropic filtering if available
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        gl.glGenerateMipmap(GL.GL_TEXTURE_2D);

        if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic"))
        {	float anisoset[] = new float[1];
            gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, anisoset, 0);
            gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisoset[0]);
        }
        return textureID;
    }

    public static int loadCubeMap(String dirName)
    {	GL4 gl = (GL4) GLContext.getCurrentGL();


        //First, create texture cube map
        //Texture tex = new Texture(GL_TEXTURE_CUBE_MAP);

        //Load each of the six (6) texture image files
        //TextureData topFile = TextureIO.newTextureData(glProfile, new File(dirName + File.separator + "yp.png"), false, "png");
        //TextureData leftFile = TextureIO.newTextureData(glProfile, new File(dirName + File.separator + "xn.png"), false, "png");
        //TextureData backFile = TextureIO.newTextureData(glProfile, new File(dirName + File.separator + "zn.png"), false, "png");
        //TextureData rightFile = TextureIO.newTextureData(glProfile, new File(dirName + File.separator + "xp.png"), false, "png");
        //TextureData frontFile = TextureIO.newTextureData(glProfile, new File(dirName + File.separator + "zp.png"), false, "png");
        //TextureData bottomFile = TextureIO.newTextureData(glProfile, new File(dirName + File.separator + "yn.png"), false, "png");

        //Each texture is assigned to a cube face
        //tex.updateImage(gl, topFile, GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
        //tex.updateImage(gl, leftFile, GL_TEXTURE_CUBE_MAP_NEGATIVE_X);
        //tex.updateImage(gl, backFile, GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
        //tex.updateImage(gl, rightFile, GL_TEXTURE_CUBE_MAP_POSITIVE_X);
        //tex.updateImage(gl, frontFile, GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
        //tex.updateImage(gl, bottomFile, GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);


        String topFile = dirName + File.separator + "yp.png";
        String leftFile = dirName + File.separator + "xn.png";
        String backFile = dirName + File.separator + "zn.png";
        String rightFile = dirName + File.separator + "xp.png";
        String frontFile = dirName + File.separator + "zp.png";
        String bottomFile = dirName + File.separator + "yn.png";

        BufferedImage topImage = getBufferedImage(topFile);
        BufferedImage leftImage = getBufferedImage(leftFile);
        BufferedImage backImage = getBufferedImage(backFile);
        BufferedImage rightImage = getBufferedImage(rightFile);
        BufferedImage frontImage = getBufferedImage(frontFile);
        BufferedImage bottomImage = getBufferedImage(bottomFile);

        byte[] topRGBA = getRGBAPixelData(topImage, false);
        byte[] leftRGBA = getRGBAPixelData(leftImage, false);
        byte[] backRGBA = getRGBAPixelData(backImage, false);
        byte[] rightRGBA = getRGBAPixelData(rightImage, false);
        byte[] frontRGBA = getRGBAPixelData(frontImage, false);
        byte[] bottomRGBA = getRGBAPixelData(bottomImage, false);

        ByteBuffer topWrappedRGBA = ByteBuffer.wrap(topRGBA);
        ByteBuffer leftWrappedRGBA = ByteBuffer.wrap(leftRGBA);
        ByteBuffer backWrappedRGBA = ByteBuffer.wrap(backRGBA);
        ByteBuffer rightWrappedRGBA = ByteBuffer.wrap(rightRGBA);
        ByteBuffer frontWrappedRGBA = ByteBuffer.wrap(frontRGBA);
        ByteBuffer bottomWrappedRGBA = ByteBuffer.wrap(bottomRGBA);

        int[] textureIDs = new int[1];
        gl.glGenTextures(1, textureIDs, 0);
        int textureID = textureIDs[0];

        checkOpenGLError();

        gl.glBindTexture(GL_TEXTURE_CUBE_MAP, textureID);
        gl.glTexStorage2D(GL_TEXTURE_CUBE_MAP, 1, GL_RGBA8, 320, 320);

        // attach the image texture to each face of the currently active OpenGL texture ID
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 0, 320, 320,
                GL_RGBA, GL.GL_UNSIGNED_BYTE, topWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 0, 0, 320, 320,
                GL_RGBA, GL.GL_UNSIGNED_BYTE, leftWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, 0, 0, 320, 320,
                GL_RGBA, GL.GL_UNSIGNED_BYTE, backWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, 0, 0, 320, 320,
                GL_RGBA, GL.GL_UNSIGNED_BYTE, rightWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, 0, 0, 320, 320,
                GL_RGBA, GL.GL_UNSIGNED_BYTE, frontWrappedRGBA);
        gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, 0, 320, 320,
                GL_RGBA, GL.GL_UNSIGNED_BYTE, bottomWrappedRGBA);

        gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        checkOpenGLError();
        return textureID;
    }

    private static BufferedImage getBufferedImage(String fileName)
    {	BufferedImage img;
        try {
            img = ImageIO.read(new File(fileName));	// assumes GIF, JPG, PNG, BMP
        } catch (IOException e) {
            System.err.println("Error reading '" + fileName + '"');
            throw new RuntimeException(e);
        }
        return img;
    }

    private static byte[] getRGBAPixelData(BufferedImage img, boolean flip)
    {	int height = img.getHeight(null);
        int width = img.getWidth(null);

        // create an (empty) BufferedImage with a suitable Raster and ColorModel
        WritableRaster raster = Raster.createInterleavedRaster(
                DataBuffer.TYPE_BYTE, width, height, 4, null);

        // convert to a color model that OpenGL understands
        ComponentColorModel colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 }, // bits
                true,  // hasAlpha
                false, // isAlphaPreMultiplied
                ComponentColorModel.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);

        BufferedImage newImage = new BufferedImage(colorModel, raster, false, null);
        Graphics2D g = newImage.createGraphics();

        if (flip)	// flip image vertically
        {	AffineTransform gt = new AffineTransform();
            gt.translate(0, height);
            gt.scale(1, -1d);
            g.transform(gt);
        }
        g.drawImage(img, null, null); // draw original image into new image
        g.dispose();

        // now retrieve the underlying byte array from the raster data buffer
        DataBufferByte dataBuf = (DataBufferByte) raster.getDataBuffer();
        return dataBuf.getData();
    }


    // GOLD material - ambient, diffuse, specular, and shininess
    public static float[] goldAmbient()  { return (new float [] {0.17f,  0.f, 0.0745f, 1} ); }
    public static float[] goldDiffuse()  { return (new float [] {0.7516f,  0.6065f, 0.2265f, 1} ); }
    public static float[] goldSpecular() { return (new float [] {0.6283f,  0.5559f, 0.3661f, 1} ); }
    public static float goldShininess()  { return 51.2f; }

    // SILVER material - ambient, diffuse, specular, and shininess
    public static float[] silverAmbient()  { return (new float [] {0.1923f,  0.1923f,  0.1923f, 1} ); }
    public static float[] silverDiffuse()  { return (new float [] {0.5075f,  0.5075f,  0.5075f, 1} ); }
    public static float[] silverSpecular() { return (new float [] {0.5083f,  0.5083f,  0.5083f, 1} ); }
    public static float silverShininess()  { return 51.2f; }


    // BRONZE material - ambient, diffuse, specular, and shininess
    public static float[] bronzeAmbient()  { return (new float [] {0.2125f,  0.1275f, 0.0540f, 1} ); }
    public static float[] bronzeDiffuse()  { return (new float [] {0.7140f,  0.4284f, 0.1814f, 1} ); }
    public static float[] bronzeSpecular() { return (new float [] {0.3936f,  0.2719f, 0.1667f, 1} ); }
    public static float bronzeShininess()  { return 25.6f; }

    // Personal material - ambient, diffuse, specular, and shininess
//    public static float[] personalAmbient()  { return (new float [] {0.23125f, 0.23125f, 0.23125f, 1.0f} ); }
//    public static float[] personalDiffuse()  { return (new float [] {0.2775f, 0.2775f, 0.2775f, 1.0f} ); }
//    public static float[] personalSpecular() { return (new float [] {0.773911f, 0.773911f, 0.773911f, 1.0f} ); }
//    public static float personalShininess()  { return 250.0f; }

    public static float[] personalAmbient()  { return (new float [] {0.6f, 0.6f, 0.6f, 1.0f} ); }
    public static float[] personalDiffuse()  { return (new float [] {0.9f, 0.9f, 0.9f, 0.9f} ); }
    public static float[] personalSpecular() { return (new float [] {1f, 1f, 1f, 1.0f} ); }
    public static float personalShininess()  { return 5.0f; }




    // Pearl material - ambient, diffuse, specular, and shininess
    public static float[] pearlAmbient()  { return (new float [] {0.135f,  0.20725f, 0.20725f, 0.922f} ); }
    public static float[] pearlDiffuse()  { return (new float [] {1.00f,  0.829f, 0.829f, 0.922f} ); }
    public static float[] pearlSpecular() { return (new float [] {0.2966f,  0.2966f, 0.2966f, 0.922f} ); }
    public static float pearlShininess()  { return 11.264f; }

    // JADE material - ambient, diffuse, specular, and shininess
    public static float[] jadeAmbient() { return (new float[] {0.135f, 0.2225f, 0.1575f});}
    public static float[] jadeDiffuse() { return (new float[] {0.54f, 0.89f, 0.63f});}
    public static float[] jadeSpecular() { return (new float[] {0.316228f, 0.316228f, 0.316228f});}
    public static float jadeShininess() { return 0.1f; }

}