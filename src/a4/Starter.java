package a4;

import java.awt.*;
import java.awt.event.*;
import java.nio.*;
import java.lang.Math;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLContext;
import org.joml.*;

import javax.swing.JFrame;


public class Starter extends JFrame implements GLEventListener, KeyListener,
        MouseListener, MouseWheelListener, MouseMotionListener {
    private final GLCanvas myCanvas;
    private int renderingProgram, shadowProgram, axesProgram, cubeMapProgram, envMapProgram, geometryProgram;
    private final int[] vao = new int[1];
    private final int[] vbo = new int[22];
    private double startTime = 0.0;
    private double tf;
    private int heightMap;

    /**** Camera Declaration ****/
    private Camera mainCam;
    private final Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

    /**** OBJ & Texture vars ****/
    private ImportedModel myModel1, myModel2, myModel3, myModel4, myModel5, myModel6, spiderObj;
    private int churchTexture, gridTexture, flTexture, shipTexture, bulldogTexture, spiderTexture;
    private int bgTex;

    private int alphaLoc, flipLoc;
    //OBJ translations, rotations, & scales
    private final Vector3f mountainLoc = new Vector3f(0.0f, -14.0f, 0.0f);
    private final Vector3f mountainScale = new Vector3f(15.0f, 15.0f, 15.0f);

    private final Vector3f churchLoc = new Vector3f(-26.0f, 45.5f, -10.0f);
    private final float churchRotation = (float) Math.toRadians(90);
    private final Vector3f churchScale = new Vector3f(2.7f, 2.7f, 2.7f);

    private final Vector3f lightOBJScale = new Vector3f(0.7f, 0.7f, 0.7f);

    private final Vector3f gridLoc = new Vector3f(0.0f, -15.0f, 0.0f);
    private final Vector3f gridScale = new Vector3f(400.0f, 400.0f, 400.0f);

    private final Vector3f shipScale = new Vector3f(10.0f, 10.0f, 10.0f);
    private final Vector3f shipLocation = new Vector3f((float) Math.sin(tf) * 250.0f, 170.0f, (float) Math.cos(tf) * 250.0f);

    private final Vector3f spiderLoc = new Vector3f(-53.0f, 25.0f, 10.0f);
    private final Vector3f spiderRotation = new Vector3f((float) Math.toRadians(30), (float) Math.toRadians(45), 0.0f);
    private final Vector3f spiderScale = new Vector3f(1.0f, 1.0f, 1.0f);

    private final Vector3f bulldogLoc = new Vector3f(33.0f, 80.0f, -2.0f);
    private final float bulldogRotation = (float) Math.toRadians(90);
    private final Vector3f bulldogScale = new Vector3f(4.0f, 4.0f, 4.0f);

    // Allocate variables for display() function
    private final FloatBuffer vals = Buffers.newDirectFloatBuffer(16);

    private Matrix4f pMat = new Matrix4f();     //Perspective Matrix
    private Matrix4f vMat = new Matrix4f();     //View Matrix
    private Matrix4f mMat = new Matrix4f();     //Model Matrix
    private Matrix4f mvMat = new Matrix4f();    //Model-View Matrix

    private int mvLoc, projLoc, nLoc, sLoc, vLoc, conditional;

    //Explosion Variables
    private int explosionLoc, boomConditional, makeGoBoomLoc;
    private int boomFactor=0;

    private float aspect, explosionTimeFactorShader;

    private final CurrentDragLocation mouseDrag = new CurrentDragLocation();

    private int cubeMapTexture;
    private final SkyBox skyBox = new SkyBox();

    /**** End OBJ & Texture vars ****/

    /**** Noise Begin ****/
    private int noiseTexture;
    private int noiseHeight= 300;
    private int noiseWidth = 300;
    private int noiseDepth = 300;
    private double[][][] noise = new double[noiseWidth][noiseHeight][noiseDepth];
    private java.util.Random random = new java.util.Random();
    /**** Noise End ****/

    /**** Light Declaration ****/
    private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
    private Vector3f currentLightPos = new Vector3f(0.0f, 200.0f, 0.0f);
    private Vector3f deltaLightPos = new Vector3f(currentLightPos);
    private float[] lightPos = new float[3];
    private boolean lightsOn = true;

    Matrix4f b = new Matrix4f();

    // white light properties
    float[] globalAmbient = new float[]{0.3f, 0.3f, 0.3f, 1.0f};

    float[] lightAmbient = new float[]{0.4f, 0.4f, 0.4f, 1.0f};
    float[] lightDiffuse = new float[]{0.4f, 0.4f, 0.4f, 1.0f};
    float[] lightSpecular = new float[]{0.8f, 0.8f, 0.8f, 1.0f};

    /**** End Light Declaration ****/

    /**** Material Declaration ****/
    private float[] gMatAmb = Utils.goldAmbient();
    private float[] gMatDif = Utils.goldDiffuse();
    private float[] gMatSpec = Utils.goldSpecular();
    private float gMatShi = Utils.goldShininess();

    private float[] pMatAmb = Utils.personalAmbient();
    private float[] pMatDif = Utils.personalDiffuse();
    private float[] pMatSpec = Utils.personalSpecular();
    private float pMatShi = Utils.personalShininess();

    private float[] pearlMatAmb = Utils.pearlAmbient();
    private float[] pearlMatDif = Utils.pearlDiffuse();
    private float[] pearlMatSpec = Utils.pearlSpecular();
    private float pearlMatShi = Utils.pearlShininess();

    private float[] jadeMatAmb = Utils.jadeAmbient();
    private float[] jadeMatDif = Utils.jadeDiffuse();
    private float[] jadeMatSpec = Utils.jadeSpecular();
    private float jadeMatShi = Utils.jadeShininess();

    float[] matAmb = gMatAmb;
    float[] matDif = gMatDif;
    float[] matSpe = gMatSpec;
    float matShi = gMatShi;

    /**** End Material Declaration ****/

    /**** Shadow Declarations ****/
    private int scSizeX, scSizeY;
    private int[] shadowTex = new int[1];
    private int[] shadowBuffer = new int[1];
    private Matrix4f lightVmat = new Matrix4f();
    private Matrix4f lightPmat = new Matrix4f();
    private Matrix4f shadowMVP1 = new Matrix4f();
    private Matrix4f shadowMVP2 = new Matrix4f();

    /**** End Shadow Declarations ****/

    /**** Menu Declarations ****/

    /**** End Menu Declarations ****/


//---------------------------------------------------------------------------------------------------------------------

    /**** Starter Begin ****/
    public Starter() {
        setTitle("Press 'M' for Menu");
        setSize(1100, 800);
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        myCanvas.addKeyListener(this);
        myCanvas.addMouseListener(this);
        myCanvas.addMouseMotionListener(this);
        myCanvas.addMouseWheelListener(this);
        this.add(myCanvas);
        this.setVisible(true);
        Animator animator = new Animator(myCanvas);
        animator.start();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   //End run upon closing screen
        myCanvas.setFocusable(true);
        addMenu();                          //Method to add button menu upon startup
        myCanvas.requestFocus();
    }
    /***** STARTER END *****/

    /**** DISPLAY START ****/

    // Display manages the setup of the custom frame buffer and the shadow texture
    // in preparation for passOne and passTwo.

    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glClearColor(0.7f, 0.8f, 0.9f, 1.0f); // background fog color is bluish-grey
        gl.glClear(GL_COLOR_BUFFER_BIT);
        double elapsedTime = System.currentTimeMillis() - startTime;

        //Setup view and perspective matrix from the light point of view for pass 1
        lightVmat.identity().setLookAt(deltaLightPos, origin, up);	// vector from light to origin
        lightPmat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		// Disable drawing colors, but enable the depth computation
		gl.glEnable(GL_DEPTH_TEST);

        // Time Factor
		tf = elapsedTime/1000.0;

        /**** Begin cubeMap ****/
        {
            gl.glUseProgram(cubeMapProgram);
            vLoc = gl.glGetUniformLocation(cubeMapProgram, "sky_v_matrix");
            projLoc = gl.glGetUniformLocation(cubeMapProgram, "sky_proj_matrix");

            gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));

            mMat.identity();
            mMat.translate(mainCam.getX(), mainCam.getY(), mainCam.getZ());
            mMat.rotateXYZ(0.0f, (float) Math.toRadians(90), 0.0f);

            currentLightPos.set(deltaLightPos);

            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);

            gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glActiveTexture(GL_TEXTURE0);
            gl.glBindTexture(GL_TEXTURE_CUBE_MAP, cubeMapTexture);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);         // cube is CW, but we are viewing the inside

            gl.glDisable(GL_DEPTH_TEST);
            gl.glDrawArrays(GL_TRIANGLES, 0, 36);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glEnable(GL_PROGRAM_POINT_SIZE);
        }
        /**** End CubeMap ****/


        /**** Program Pass Calls ****/
        {
            gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
            gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);
            gl.glDrawBuffer(GL_NONE);

            // For reducing shadow artifacts
            gl.glEnable(GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(2.0f, 4.0f);

            passOne();
            // More artifact reduction
            gl.glDisable(GL_POLYGON_OFFSET_FILL);

            //Restore the default display buffer, and re-enable drawing
            gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
            gl.glActiveTexture(GL_TEXTURE1);
            gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);

            gl.glActiveTexture(GL_TEXTURE9);
            gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
            gl.glDrawBuffer(GL_FRONT);

            passTwo();
        }
        /**** End Program Pass Calls ****/

    }
    /*************** PASS ONE *******************/
    public void passOne() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glUseProgram(shadowProgram);
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        sLoc = gl.glGetUniformLocation(shadowProgram, "shadowMVP");

        //VBO indices:
        {
//             0 = Mountain Positions
//             2 = Mountain Normals
//             3 = Church positions
//             4 = Church Texture Positions
//             5 = Church Normals
//             6 = Grid Positions
//             7 = Grid Texture Positions
//             8 = Grid Normals
//             9 = Lamp Vertices
//            10 = Lamp Textures
//            11 = Lamp Normals
//            12 = Cubemap Vertices
//            13 = Ship Vertices
//             1 = Ship Texture Positions
//            14 = Ship Normals
//            15 = Bulldog Vertices
//            16 = Bulldog Texture
//            17 = Bulldog Normals
//            18 = Spider Vertices
//            19 = Spider Texture
//            20 = Spider Normals
//            21 = indicies
        }


        /** Mountain Shadow Begin**/
        {
            mMat.identity();
            mMat.translate(mountainLoc);
            mMat.scale(mountainScale);

            //It's drawing from the light's POV, so need to use the light's P & V matrices
            shadowMVP1.identity();
            shadowMVP1.mul(lightPmat);
            shadowMVP1.mul(lightVmat);
            shadowMVP1.mul(mMat);

            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

            //Mountain Vertices
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            //Draw the Mountain shadow
            gl.glDrawArrays(GL_TRIANGLES, 0, myModel1.getNumVertices());
        }
        /*** Mountain Shadow End***/

        /**** Church Shadow Begin ****/
        {
            mMat.identity();
            mMat.translate(churchLoc);
            mMat.rotateY(churchRotation);
            mMat.scale(churchScale);

            shadowMVP1.identity();
            shadowMVP1.mul(lightPmat);
            shadowMVP1.mul(lightVmat);
            shadowMVP1.mul(mMat);

            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myModel2.getNumVertices());
        }
        /**** Church Shadow End ****/

        /**** Ship Shadow Begin ****/
        {
            mMat.identity();
            mMat.translate((float) Math.sin(tf) * 250.0f, 150.0f, (float) Math.cos(tf) * 250.0f);
            mMat.rotateY((float) tf);
            mMat.rotateY((float) -179.0);
            mMat.rotateX((float) 0.49);
            mMat.rotateZ((float) 0.33);
            mMat.scale(shipScale);

            shadowMVP1.identity();
            shadowMVP1.mul(lightPmat);
            shadowMVP1.mul(lightVmat);
            shadowMVP1.mul(mMat);

            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myModel5.getNumVertices());
        }
        /**** Ship Shadow End ****/

        /**** Bulldog Shadow Begin ****/
        {
            mMat.identity();
            mMat.translate(bulldogLoc);
            mMat.rotateY(bulldogRotation);
            mMat.scale(bulldogScale);

            shadowMVP1.identity();
            shadowMVP1.mul(lightPmat);
            shadowMVP1.mul(lightVmat);
            shadowMVP1.mul(mMat);

            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myModel6.getNumVertices());
        }
        /**** Bulldog Shadow End ****/

        /**** SPIDER M8 SHADOW bott'm ****/
        {
            mMat.identity();
            mMat.translate(spiderLoc);
            mMat.rotateXYZ(spiderRotation);
            mMat.scale(spiderScale);

            shadowMVP1.identity();
            shadowMVP1.mul(lightPmat);
            shadowMVP1.mul(lightVmat);
            shadowMVP1.mul(mMat);

            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[18]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, spiderObj.getNumVertices());
        }
        /**** SPIDER END M8 ****/

        /**** Grid Shadow ****/
        {
            mMat.identity();
            mMat.translate(gridLoc);
            mMat.scale(gridScale);

            //It's drawing from the light's POV, so need to use the light's P & V matrices
            shadowMVP1.identity();
            shadowMVP1.mul(lightPmat);
            shadowMVP1.mul(lightVmat);
            shadowMVP1.mul(mMat);

            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

            //Mountain Vertices
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glEnable(GL_CULL_FACE);
            gl.glFrontFace(GL_CCW);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            //Draw the Mountain shadow
            gl.glDrawArrays(GL_TRIANGLES, 0, myModel3.getNumVertices());
        }
        /**** End Grid Shadow ****/
    }
    /*************** End PASS ONE ***************/


    /*************** PASS TWO ***************/
    /*
            VBO indices:
             0 = Mountain Positions
             2 = Mountain Normals
             3 = Church positions
             4 = Church Texture Positions
             5 = Church Normals
             6 = Grid Positions
             7 = Grid Texture Positions
             8 = Grid Normals
             9 = Lamp Vertices
            10 = Lamp Textures
            11 = Lamp Normals
            12 = Cubemap Vertices
            13 = Ship Vertices
             1 = Ship Texture Positions
            14 = Ship Normals
            15 = Bulldog Vertices
            16 = Bulldog Texture
            17 = Bulldog Normals
            18 = Spider Vertices
            19 = Spider Texture
            20 = Spider Normals
         */

    public void passTwo()
    {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glUseProgram(renderingProgram);

        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));

        /**** Camera Install ****/
        {
            //Setup View, model, and model-view matrices
            //Note that the view matrix is done within Camera class
            vMat.identity();
            vMat = mainCam.setUpViewMat();
            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);
        }
        /**** End Camera Install ****/


        /**** BEGIN MOUNTAIN ****/
        {
            gl.glUseProgram(envMapProgram);

            mMat.identity();
            mMat.translate(mountainLoc);
            mMat.scale(mountainScale);

            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);

            //Build MVP matrix for object from the lights POV
            shadowMVP2.identity();
            shadowMVP2.mul(b);
            shadowMVP2.mul(lightPmat);
            shadowMVP2.mul(lightVmat);
            shadowMVP2.mul(mMat);

            mvMat.invert(invTrMat);
            invTrMat.transpose(invTrMat);

            //make a non-color material
            matAmb = pMatAmb;
            matDif = pMatDif;
            matSpe = pMatSpec;
            matShi = pMatShi;

            installLights(envMapProgram, vMat);

            mvLoc = gl.glGetUniformLocation(envMapProgram, "mv_matrix");
            projLoc = gl.glGetUniformLocation(envMapProgram, "proj_matrix");
            nLoc = gl.glGetUniformLocation(envMapProgram, "norm_matrix");
            sLoc = gl.glGetUniformLocation(envMapProgram, "shadowMVP");

            //Copies respective matrices to corresponding uniform vars
            gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
            gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

            //Activate mountain vertices buffer
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            //Activate mountain-normals buffer
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
            gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(2);

            gl.glActiveTexture(GL_TEXTURE0);
            gl.glBindTexture(GL_TEXTURE_CUBE_MAP, cubeMapTexture);

            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myModel1.getNumVertices());
        }
        /**** END MOUNTAIN ****/

        /**** BEGIN CHURCH ****/
        {
            gl.glUseProgram(renderingProgram);

            matAmb = pearlMatAmb; //jade
            matDif = pearlMatDif;
            matSpe = pearlMatSpec;
            matShi = pearlMatShi;
            //Change material f

            installLights(renderingProgram, vMat);

            mMat.identity();
            mMat.translate(churchLoc);
            mMat.rotateY(churchRotation);
            mMat.scale(churchScale);

            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);

            shadowMVP2.identity();
            shadowMVP2.mul(b);
            shadowMVP2.mul(lightPmat);
            shadowMVP2.mul(lightVmat);
            shadowMVP2.mul(mMat);

            mvMat.invert(invTrMat);
            invTrMat.transpose(invTrMat);


            //Get uniform location for bump mapped objects
            conditional = gl.glGetUniformLocation(renderingProgram, "shaderConditional");
            mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
            projLoc = gl.glGetUniformLocation(renderingProgram, "proj_matrix");
            nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");
            sLoc = gl.glGetUniformLocation(renderingProgram, "shadowMVP");

            //No bumps
            gl.glProgramUniform1i(renderingProgram, conditional, 1); //Turning on bumps
            //gl.glUniform1i(renderingProgram, conditional); //Turning on bumps


            gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
            gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

            //3 = Church positions
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            //4 = Church Texture Positions
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
            gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(1);

            //5 = Church Normals
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
            gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(2);

            gl.glActiveTexture(GL_TEXTURE0);
            gl.glBindTexture(GL_TEXTURE_2D, churchTexture);

            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myModel2.getNumVertices());
        }
        /**** END CHURCH ****/

        /**** BEGIN SPIDER ****/
        {
            //TODO - spider madness crazy stuff
            //int gShader = gl.glCreateShader(GL_GEOMETRY_SHADER);
            gl.glUseProgram(geometryProgram);

            mvLoc = gl.glGetUniformLocation(geometryProgram, "mv_matrix");
            projLoc = gl.glGetUniformLocation(geometryProgram, "proj_matrix");
            nLoc = gl.glGetUniformLocation(geometryProgram, "norm_matrix");
            sLoc = gl.glGetUniformLocation(geometryProgram, "shadowMVP");
            explosionLoc = gl.glGetUniformLocation(geometryProgram, "explosionFactor");
            makeGoBoomLoc = gl.glGetUniformLocation(geometryProgram, "boomConditional");

            installLights(geometryProgram, vMat);

            mMat.identity();
            mMat.translate(spiderLoc);
            mMat.rotateXYZ(spiderRotation);
            mMat.scale(spiderScale);

            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);

            shadowMVP2.identity();
            shadowMVP2.mul(b);
            shadowMVP2.mul(lightPmat);
            shadowMVP2.mul(lightVmat);
            shadowMVP2.mul(mMat);

            mvMat.invert(invTrMat);
            invTrMat.transpose(invTrMat);

            //No bumps
            gl.glProgramUniform1i(renderingProgram, conditional, 1);

            //System.out.println(tf % 10.0);
            explosionTimeFactorShader = (float)(tf % 10.0);


            //Copies respective matrices to corresponding uniform vars
            gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
            gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
            //gl.glUniform1f(explosionLoc, explosionTimeFactorShader);
            gl.glUniform1f(explosionLoc, (float)boomFactor);
            gl.glUniform1i(makeGoBoomLoc, boomConditional);

            gl.glActiveTexture(GL_TEXTURE0);
            gl.glBindTexture(GL_TEXTURE_2D, spiderTexture);

            //Activate ship vertices buffer
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[18]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            //Activate ship texture buffer
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[19]);
            gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(1);

            //Activate ship-normals buffer
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[20]);
            gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(2);

            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, spiderObj.getNumVertices());
        }
        /**** END SPIDER ****/

        /**** BEGIN SHIP ****/
        {
        gl.glUseProgram(renderingProgram);

        mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
        projLoc = gl.glGetUniformLocation(renderingProgram, "proj_matrix");
        nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");
        sLoc = gl.glGetUniformLocation(renderingProgram, "shadowMVP");

        installLights(renderingProgram, vMat);

        mMat.identity();
        mMat.translate((float) Math.sin(tf) * 250.0f, 150.0f, (float) Math.cos(tf) * 250.0f);
        mMat.rotateY((float) tf);
        mMat.rotateY((float) -179.0);
        mMat.rotateX((float) 0.49);
        mMat.rotateZ((float) 0.33);
        mMat.scale(shipScale);

        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        mvMat.invert(invTrMat);
        invTrMat.transpose(invTrMat);

        shadowMVP2.identity();
        shadowMVP2.mul(b);
        shadowMVP2.mul(lightPmat);
        shadowMVP2.mul(lightVmat);
        shadowMVP2.mul(mMat);

        mvMat.invert(invTrMat);
        invTrMat.transpose(invTrMat);


        //Copies respective matrices to corresponding uniform vars
        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
        gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

        //Activate ship vertices buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        //Activate ship texture buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        //Activate ship-normals buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);


        //Nooise
        gl.glProgramUniform1i(renderingProgram, conditional, 3);

        gl.glActiveTexture(GL_TEXTURE3);
        gl.glBindTexture(GL_TEXTURE_3D, bgTex);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, myModel5.getNumVertices());
    }
        /**** END SHIP ****/

        /**** BEGIN BULLDOG ****/
        {
        alphaLoc = gl.glGetUniformLocation(renderingProgram, "alpha");
        flipLoc = gl.glGetUniformLocation(renderingProgram, "flipNormal");

        matAmb = gMatAmb;
        matDif = jadeMatDif;
        matSpe = gMatSpec;
        matShi = pMatShi;

        installLights(renderingProgram, vMat);

        mMat.identity();
        mMat.translate(bulldogLoc);
        mMat.rotateY(bulldogRotation);
        mMat.scale(bulldogScale);

        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        shadowMVP2.identity();
        shadowMVP2.mul(b);
        shadowMVP2.mul(lightPmat);
        shadowMVP2.mul(lightVmat);
        shadowMVP2.mul(mMat);

        mvMat.invert(invTrMat);
        invTrMat.transpose(invTrMat);

        // Uniform var makes obj transparent in shader conditional
        gl.glProgramUniform1i(renderingProgram, conditional, 2);

        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
        gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
        gl.glProgramUniform1f(renderingProgram, alphaLoc, 1.0f); //This for transparency
        gl.glProgramUniform1f(renderingProgram, flipLoc, 1.0f);
        gl.glProgramUniform1i(renderingProgram, conditional, 2);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[17]);
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);

        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, bulldogTexture);

		// 2-pass rendering a transparent version of the pyramid

		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL_FUNC_ADD);

		gl.glEnable(GL_CULL_FACE);

		gl.glCullFace(GL_FRONT);
		gl.glProgramUniform1f(renderingProgram, alphaLoc, 0.3f);
		gl.glProgramUniform1f(renderingProgram, flipLoc, -1.0f);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel6.getNumVertices());

		gl.glCullFace(GL_BACK);
		gl.glProgramUniform1f(renderingProgram, alphaLoc, 0.4f);
		gl.glProgramUniform1f(renderingProgram, flipLoc, 1.0f);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel6.getNumVertices());

		gl.glDisable(GL_BLEND);

    }
        /**** END BULLDOG ****/


        /**** BEGIN LIGHT OBJECT ****/
        {
            mMat.identity();
            mMat.translate(deltaLightPos.x, deltaLightPos.y, deltaLightPos.z);
            mMat.scale(lightOBJScale);

            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);
            mvMat.invert(invTrMat);
            invTrMat.transpose(invTrMat);

            shadowMVP2.identity();
            shadowMVP2.mul(b);
            shadowMVP2.mul(lightPmat);
            shadowMVP2.mul(lightVmat);
            shadowMVP2.mul(mMat);



            gl.glProgramUniform1i(renderingProgram, conditional, 1);        // Bump

            gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
            gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
            gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(1);

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
            gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(2);

            gl.glActiveTexture(GL_TEXTURE0);
            gl.glBindTexture(GL_TEXTURE_2D, flTexture);

            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myModel4.getNumVertices());
        }
        /**** END LIGHT OBJECT ****/

        /**** BEGIN GRID ****/
        {
            //Change material for Grid
            matAmb = gMatAmb;
            matDif = gMatDif;
            matSpe = gMatSpec;
            matShi = gMatShi;

            mMat.identity();
            mMat.translate(gridLoc);
            mMat.scale(gridScale);

            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);

            gl.glProgramUniform1i(renderingProgram, conditional, 1); //Turning on bumps

            gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
            gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(0);

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
            gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(1);

            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
            gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(2);

            // height map
            gl.glActiveTexture(GL_TEXTURE2);
            gl.glBindTexture(GL_TEXTURE_2D, heightMap);

            gl.glActiveTexture(GL_TEXTURE0);
            gl.glBindTexture(GL_TEXTURE_2D, gridTexture);

            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);

            gl.glDrawArrays(GL_TRIANGLES, 0, myModel3.getNumVertices());
        }
        /**** END GRID ****/


        /**** Begin Light Install ****/
        {
            mMat.identity();
            //mMat.translate(currentLightPos.x(), currentLightPos.y(), currentLightPos.z());
            mMat.translate(deltaLightPos.x(), deltaLightPos.y(), deltaLightPos.z());

            mvMat.identity();
            mvMat.mul(vMat);
            mvMat.mul(mMat);

            //Build the inverse-transpose of the MV matrix for transforming normal vectors
            mvMat.invert(invTrMat);
            invTrMat.transpose(invTrMat);

            //Put the MV, PROJ, and Inverse-Transpose(normal) matrices into the corresponding uniforms
            gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
            gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
            gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
            gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
        }
        /**** End Light Installation ****/

        /**** Render Axes ****/
        {
            gl.glUseProgram(axesProgram);

            int axesMvLoc = gl.glGetUniformLocation(axesProgram, "axes_mv_matrix");
            int axesProjLoc = gl.glGetUniformLocation(axesProgram, "axes_proj_matrix");


            //mvMat.identity();
            //mvMat.mul(vMat);
            //mvMat.mul(mMat);

            if (mainCam.getBool()) {
                gl.glUniformMatrix4fv(axesProjLoc, 1, false, pMat.get(vals));
                gl.glUniformMatrix4fv(axesMvLoc, 1, false, mvMat.get(vals));
                gl.glDrawArrays(GL_LINES, 0, 6);
            }
        }
        /**** End Render Axes ****/

        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        //installLights(renderingProgram, vMat);

    }

    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        startTime = System.currentTimeMillis();

        //Assign shaders to appropriate corresponding rendering program
        shadowProgram = Utils.createShaderProgram("a4/Shaders/pass1Vert.glsl", "a4/Shaders/pass1Frag.glsl");
        cubeMapProgram = Utils.createShaderProgram("a4/Shaders/skyVertShader.glsl", "a4/Shaders/skyFragShader.glsl");
        renderingProgram = Utils.createShaderProgram("a4/Shaders/vertShader.glsl", "a4/Shaders/fragShader.glsl");
        axesProgram = Utils.createShaderProgram("a4/Shaders/axVertShader.glsl", "a4/Shaders/axFragShader.glsl");
        envMapProgram = Utils.createShaderProgram("a4/Shaders/vertShader.glsl", "a4/Shaders/envMapFrag.glsl");
        geometryProgram = Utils.createShaderProgram("a4/Shaders/geoVert.glsl", "a4/Shaders/geoShader.glsl", "a4/Shaders/geoFrag.glsl");

        /**** Starting Camera Position ****/
        float cameraX = 0.0f;
        float cameraY = 175.0f;
        float cameraZ = 325.0f;


        myModel1 = new ImportedModel("../Mountains.obj");   //Mountain OBJ
        myModel2 = new ImportedModel("../church1.obj");     //Church Building OBJ
        myModel3 = new ImportedModel("../grid.obj");        //Ground OBJ
        myModel4 = new ImportedModel("../untitled.obj");    //Light OBJ
        myModel5 = new ImportedModel("../ship.obj");        //SpaceShip OBJ
        myModel6 = new ImportedModel("../bulldog.obj");     //Dog OBJ
        spiderObj = new ImportedModel("../spider.obj");


        //deltaLightPos.add(new Vector3f(0.0f, -1.0f, 0.0f));

        //Noise Texture call & assignment for ship
        generateNoise();
        bgTex = buildNoiseTexture();

        mainCam = new Camera(cameraX, cameraY, cameraZ);
        myCanvas.addKeyListener(mainCam);

        aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

        setupVertices();
        setupShadowBuffers();

        //Import 2D textures to place on 3D objects
        churchTexture = Utils.loadTexture("Church_Texture.png");
        gridTexture = Utils.loadTexture("gridTexture.jpg");
        flTexture = Utils.loadTexture("LaptTexture.png");
        shipTexture = Utils.loadTexture("shipText.png");
        heightMap = Utils.loadTexture("height.jpg");
        bulldogTexture = Utils.loadTexture("dog.jpg");
        spiderTexture = Utils.loadTexture("spiderTex.jpg");
        cubeMapTexture = Utils.loadCubeMap("cubeMap");
        gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

        b.set(
                0.5f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.5f, 0.0f,
                0.5f, 0.5f, 0.5f, 1.0f);
    }

    private void setupShadowBuffers()
    {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        scSizeX = myCanvas.getWidth();
        scSizeY = myCanvas.getHeight();

        //Create custom frame buffer
        gl.glGenFramebuffers(1, shadowBuffer, 0);

        //Create Shadow texture and configure it to hold depth info
        gl.glGenTextures(1, shadowTex, 0);
        gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
                scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

        // may reduce shadow border artifacts
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }


    private void setupVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
         /*
            VBO indices:
            0 = Mountain Positions
            2 = Mountain Normals
            3 = Church positions
            4 = Church Texture Positions
            5 = Church Normals
            6 = Grid Positions
            7 = Grid Texture Positions
            8 = Grid Normals
            9 = Lamp Vertices
            10 = Lamp Textures
            11 = Lamp Normals
            12 = Cubemap Vertices
            13 = Ship Vertices
             1 = Ship Texture Positions
            14 = Ship Normals
            15 = Dog Vertices
            16 = Dog Textures
            17 = Dog Normals
            18 = Spider Vertices
            19 = Spider Texture
            20 = Spider Normals
         */

        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vbo.length, vbo, 0);


        /**** BEGIN IMPORTED OBJs ****/

        /**** Mountain Begin ****/
        {
            ModelVertices mountain = new ModelVertices(myModel1);

            //VBO for mountain .obj vertex Locations
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
            FloatBuffer vertBufMnt = Buffers.newDirectFloatBuffer(mountain.getPValues());
            gl.glBufferData(GL_ARRAY_BUFFER, vertBufMnt.limit() * 4, vertBufMnt, GL_STATIC_DRAW);

            //VBO for normal vectors
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
            FloatBuffer norBufMnt = Buffers.newDirectFloatBuffer(mountain.getNValues());
            gl.glBufferData(GL_ARRAY_BUFFER, norBufMnt.limit() * 4, norBufMnt, GL_STATIC_DRAW);
    }
        /**** Mountain End ****/

        /**** Church Begin ****/
        {
            ModelVertices church = new ModelVertices(myModel2);

            //VBO for church .obj vertex Locations
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
            FloatBuffer vertBufChurch = Buffers.newDirectFloatBuffer(church.getPValues());
            gl.glBufferData(GL_ARRAY_BUFFER, vertBufChurch.limit() * 4, vertBufChurch, GL_STATIC_DRAW);

            //VBO for texture coordinates
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
            FloatBuffer texBufChurch = Buffers.newDirectFloatBuffer(church.getTValues());
            gl.glBufferData(GL_ARRAY_BUFFER, texBufChurch.limit() * 4, texBufChurch, GL_STATIC_DRAW);

            //VBO for normal vectors
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
            FloatBuffer norBufChurch = Buffers.newDirectFloatBuffer(church.getNValues());
            gl.glBufferData(GL_ARRAY_BUFFER, norBufChurch.limit() * 4, norBufChurch, GL_STATIC_DRAW);
        }
        /**** Church End ****/

        /**** Spider Begin ****/
        {
            ModelVertices spider = new ModelVertices(spiderObj);

            //VBO for .obj vertex Locations
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[18]);
            FloatBuffer vertBufSpider = Buffers.newDirectFloatBuffer(spider.getPValues());
            gl.glBufferData(GL_ARRAY_BUFFER, vertBufSpider.limit() * 4, vertBufSpider, GL_STATIC_DRAW);

            //VBO for texture coordinates
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[19]);
            FloatBuffer texBufSpider = Buffers.newDirectFloatBuffer(spider.getTValues());
            gl.glBufferData(GL_ARRAY_BUFFER, texBufSpider.limit() * 4, texBufSpider, GL_STATIC_DRAW);

            //VBO for ship normal vectors
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[20]);
            FloatBuffer norBufSpider = Buffers.newDirectFloatBuffer(spider.getNValues());
            gl.glBufferData(GL_ARRAY_BUFFER, norBufSpider.limit() * 4, norBufSpider, GL_STATIC_DRAW);
        }
        /**** Spider End ****/

        /**** Ship Begin ****/
        {
            ModelVertices ship = new ModelVertices(myModel5);


            //VBO for .obj vertex Locations
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
            FloatBuffer vertBufShip = Buffers.newDirectFloatBuffer(ship.getPValues());
            gl.glBufferData(GL_ARRAY_BUFFER, vertBufShip.limit() * 4, vertBufShip, GL_STATIC_DRAW);

            //VBO for texture coordinates
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
            FloatBuffer texBufShip = Buffers.newDirectFloatBuffer(ship.getTValues());
            gl.glBufferData(GL_ARRAY_BUFFER, texBufShip.limit() * 4, texBufShip, GL_STATIC_DRAW);

            //VBO for ship normal vectors
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
            FloatBuffer norBufShip = Buffers.newDirectFloatBuffer(ship.getNValues());
            gl.glBufferData(GL_ARRAY_BUFFER, norBufShip.limit() * 4, norBufShip, GL_STATIC_DRAW);
        }
        /**** Ship End ****/

        /**** Dog Begin ****/
        {
            ModelVertices dog = new ModelVertices(myModel6);

            //VBO for .obj vertex Locations
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
            FloatBuffer vertBufDog = Buffers.newDirectFloatBuffer(dog.getPValues());
            gl.glBufferData(GL_ARRAY_BUFFER, vertBufDog.limit() * 4, vertBufDog, GL_STATIC_DRAW);

            //VBO for dog texture coordinates
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
            FloatBuffer texBufDog = Buffers.newDirectFloatBuffer(dog.getTValues());
            gl.glBufferData(GL_ARRAY_BUFFER, texBufDog.limit() * 4, texBufDog, GL_STATIC_DRAW);

            //VBO for dog normal vectors
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[17]);
            FloatBuffer norBufDog = Buffers.newDirectFloatBuffer(dog.getNValues());
            gl.glBufferData(GL_ARRAY_BUFFER, norBufDog.limit() * 4, norBufDog, GL_STATIC_DRAW);
        }
        /**** Bulldog End ****/

        /**** Grid Begin ****/
        {
            ModelVertices grid = new ModelVertices(myModel3);

            //VBO for Grid vertex Locations
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
            FloatBuffer vertBufGrid = Buffers.newDirectFloatBuffer(grid.getPValues());
            gl.glBufferData(GL_ARRAY_BUFFER, vertBufGrid.limit() * 4, vertBufGrid, GL_STATIC_DRAW);

            //VBO for grid texture coordinates
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
            FloatBuffer texBufGrid = Buffers.newDirectFloatBuffer(grid.getTValues());
            gl.glBufferData(GL_ARRAY_BUFFER, texBufGrid.limit() * 4, texBufGrid, GL_STATIC_DRAW);

            //VBO for grid normal vectors
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
            FloatBuffer norBufGrid = Buffers.newDirectFloatBuffer(grid.getNValues());
            gl.glBufferData(GL_ARRAY_BUFFER, norBufGrid.limit() * 4, norBufGrid, GL_STATIC_DRAW);
        }
        /**** Grid End ****/

        /**** Light Model Begin ****/
        {
            ModelVertices light = new ModelVertices(myModel4);

            //VBO for Lamp vertex Locations
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
            FloatBuffer vertBufFL = Buffers.newDirectFloatBuffer(light.getPValues());
            gl.glBufferData(GL_ARRAY_BUFFER, vertBufFL.limit() * 4, vertBufFL, GL_STATIC_DRAW);

            //VBO for Lamp texture coordinates
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
            FloatBuffer texBufFL = Buffers.newDirectFloatBuffer(light.getTValues());
            gl.glBufferData(GL_ARRAY_BUFFER, texBufFL.limit() * 4, texBufFL, GL_STATIC_DRAW);

            //VBO for Lamp normal vectors
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
            FloatBuffer norBufFL = Buffers.newDirectFloatBuffer(light.getNValues());
            gl.glBufferData(GL_ARRAY_BUFFER, norBufFL.limit() * 4, norBufFL, GL_STATIC_DRAW);
        }
        /**** Light Model End ****/

        /**** Cubemap Begin ****/
        {
            //VBO for cubemap vertices
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
            FloatBuffer cubeVertBuf = Buffers.newDirectFloatBuffer(skyBox.getVertices());
            gl.glBufferData(GL_ARRAY_BUFFER, cubeVertBuf.limit() * 4, cubeVertBuf, GL_STATIC_DRAW);
        }
        /**** CubeMap End ****/
    }

    private void installLights(int renderingProgramPar, Matrix4f vMatrix) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        //Convert light's postition to view space, and save it in a float array
        currentLightPos.mulPosition(vMatrix);
        lightPos[0] = currentLightPos.x();
        lightPos[1] = currentLightPos.y();
        lightPos[2] = currentLightPos.z();


        // get the locations of the light and material fields in the shader
        int globalAmbLoc = gl.glGetUniformLocation(renderingProgramPar, "globalAmbient");
        int ambLoc = gl.glGetUniformLocation(renderingProgramPar, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(renderingProgramPar, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(renderingProgramPar, "light.specular");
        int posLoc = gl.glGetUniformLocation(renderingProgramPar, "light.position");
        int mambLoc = gl.glGetUniformLocation(renderingProgramPar, "material.ambient");
        int mdiffLoc = gl.glGetUniformLocation(renderingProgramPar, "material.diffuse");
        int mspecLoc = gl.glGetUniformLocation(renderingProgramPar, "material.specular");
        int mshiLoc = gl.glGetUniformLocation(renderingProgramPar, "material.shininess");

        //  set the uniform light and material values in the shader
        gl.glProgramUniform4fv(renderingProgramPar, globalAmbLoc, 1, globalAmbient, 0);
        gl.glProgramUniform4fv(renderingProgramPar, ambLoc, 1, lightAmbient, 0);
        gl.glProgramUniform4fv(renderingProgramPar, diffLoc, 1, lightDiffuse, 0);
        gl.glProgramUniform4fv(renderingProgramPar, specLoc, 1, lightSpecular, 0);
        gl.glProgramUniform3fv(renderingProgramPar, posLoc, 1, lightPos, 0);
        gl.glProgramUniform4fv(renderingProgramPar, mambLoc, 1, matAmb, 0);
        gl.glProgramUniform4fv(renderingProgramPar, mdiffLoc, 1, matDif, 0);
        gl.glProgramUniform4fv(renderingProgramPar, mspecLoc, 1, matSpe, 0);
        gl.glProgramUniform1f(renderingProgramPar, mshiLoc, matShi);
    }

    /*************Panel Button Method**************/
    //Method that adds a panel to the top-North side of the frame
    public void addMenu() {
        JFrame menuFrame = new JFrame();
        JPanel menuPanel = new JPanel();
        JLabel menuLabel = new JLabel();

        menuLabel.setIcon(new ImageIcon("keyMenu.JPG"));    //Ensure image stored at correct (path) location
        menuPanel.add(menuLabel);
        add(menuPanel);
        validate();

        menuPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        //menuPanel.setLayout(new GridLayout(1, 10));
        //menuPanel.add(menuLabel);

        menuFrame.add(menuPanel, BorderLayout.CENTER);
        menuFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        menuFrame.setTitle("Key Menu");
        menuFrame.setSize(500, 500);
        menuFrame.pack();
        menuFrame.setVisible(true);
    }
    // 3D Texture section

    private void fillDataArray(byte data[])
    { double veinFrequency = 1.75;
        double turbPower = 3.0;
        double turbSize =  32.0;
        for (int i=0; i<noiseWidth; i++)
        { for (int j=0; j<noiseHeight; j++)
        { for (int k=0; k<noiseDepth; k++)
        {	double xyzValue = (float)i/noiseWidth + (float)j/noiseHeight + (float)k/noiseDepth
                + turbPower * turbulence(i,j,k,turbSize)/256.0;

            double sineValue = logistic(Math.abs(Math.sin(xyzValue * 3.14159 * veinFrequency)));
            sineValue = Math.max(-1.0, Math.min(sineValue*1.25-0.20, 1.0));

            Color c = new Color((float)sineValue,
                    (float)Math.min(sineValue*1.5-0.25, 1.0),
                    (float)sineValue);

            data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+0] = (byte) c.getRed();
            data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+1] = (byte) c.getGreen();
            data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+2] = (byte) c.getBlue();
            data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+3] = (byte) 255;
        } } } }

    private int buildNoiseTexture()
    {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        byte[] data = new byte[noiseHeight*noiseWidth*noiseDepth*4];

        fillDataArray(data);

        ByteBuffer bb = Buffers.newDirectByteBuffer(data);

        int[] textureIDs = new int[1];
        gl.glGenTextures(1, textureIDs, 0);
        int textureID = textureIDs[0];

        gl.glBindTexture(GL_TEXTURE_3D, textureID);

        gl.glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA8, noiseWidth, noiseHeight, noiseDepth);
        gl.glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0,
                noiseWidth, noiseHeight, noiseDepth, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, bb);

        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        return textureID;
    }

    void generateNoise()
    {	for (int x=0; x<noiseWidth; x++)
    {	for (int y=0; y<noiseHeight; y++)
    {	for (int z=0; z<noiseDepth; z++)
    {	noise[x][y][z] = random.nextDouble();
    }	}	}	}

    double smoothNoise(double x1, double y1, double z1)
    {	//get fractional part of x, y, and z
        double fractX = x1 - (int) x1;
        double fractY = y1 - (int) y1;
        double fractZ = z1 - (int) z1;

        //neighbor values
        int x2 = ((int)x1 + noiseWidth + 1) % noiseWidth;
        int y2 = ((int)y1 + noiseHeight+ 1) % noiseHeight;
        int z2 = ((int)z1 + noiseDepth + 1) % noiseDepth;

        //smooth the noise by interpolating
        double value = 0.0;
        value += (1-fractX) * (1-fractY) * (1-fractZ) * noise[(int)x1][(int)y1][(int)z1];
        value += (1-fractX) * fractY     * (1-fractZ) * noise[(int)x1][(int)y2][(int)z1];
        value += fractX     * (1-fractY) * (1-fractZ) * noise[(int)x2][(int)y1][(int)z1];
        value += fractX     * fractY     * (1-fractZ) * noise[(int)x2][(int)y2][(int)z1];

        value += (1-fractX) * (1-fractY) * fractZ     * noise[(int)x1][(int)y1][(int)z2];
        value += (1-fractX) * fractY     * fractZ     * noise[(int)x1][(int)y2][(int)z2];
        value += fractX     * (1-fractY) * fractZ     * noise[(int)x2][(int)y1][(int)z2];
        value += fractX     * fractY     * fractZ     * noise[(int)x2][(int)y2][(int)z2];

        return value;
    }

    private double turbulence(double x, double y, double z, double size)
    {	double value = 0.0, initialSize = size;
        while(size >= 0.9)
        {	value = value + smoothNoise(x/size, y/size, z/size) * size;
            size = size / 2.0;
        }
        value = 128.0 * value / initialSize;
        return value;
    }

    private double logistic(double x)
    {	double k = 3.0;
        return (1.0/(1.0+Math.pow(2.718,-k*x)));
    }

    public static void main(String[] args) {
        new Starter();
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

        setupShadowBuffers();
    }

    public void dispose(GLAutoDrawable drawable) { }

    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) {
        mouseDrag.dragBegin(e.getX(), e.getY());
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        mouseDrag.dragEnd();
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseDrag.getChangeInPos(new Vector2f(e.getX(), e.getY()));
        deltaLightPos.add(mouseDrag.changeInPos.x * 0.3f, 0, mouseDrag.changeInPos.y * 0.3f);
    }

    @Override
    public void mouseMoved(MouseEvent e) { }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() > 0) {                    //if scrolling in the + direction
            //System.out.println("Current y light position: " + deltaLightPos.y());
            deltaLightPos.y -= 15;
        } else {
            //System.out.println("Current y light position: " + deltaLightPos.y());
            deltaLightPos.y += 15;

        }
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_R)
        {
            if(lightsOn)
            {
                globalAmbient = new float[]{0.0f, 0.0f, 0.0f};
                lightAmbient = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
                lightDiffuse = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
                lightSpecular = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
                lightsOn = false;
            }
            else
            {
                globalAmbient = new float[]{0.3f, 0.4f, 0.5f};
                lightAmbient = new float[]{0.6f, 0.6f, 0.6f, 1.0f};
                lightDiffuse = new float[]{0.4f, 0.4f, 0.4f, 1.0f};
                lightSpecular = new float[]{0.5f, 0.5f, 0.5f, 1.0f};
                lightsOn = true;
            }
        }
        //Make the Spider Explode
        else if(key == KeyEvent.VK_B) {
            System.out.println("Spider go BOOM!");
            if(boomFactor < 50)
                boomFactor++;
            boomConditional = 1;
        }
        //Bring the Spider back to original form
        else if(key == KeyEvent.VK_N){
            System.out.println("Spider go BACK TO NORMAL!");
            if(boomFactor > 0)
                boomFactor--;
            boomConditional = 0;
        }

        /** Display Key Menu **/
        else if(key == KeyEvent.VK_M){
            System.out.println("Show key Menu");
            addMenu();
        }
    }
    @Override
    public void keyReleased(KeyEvent e) { }
}

