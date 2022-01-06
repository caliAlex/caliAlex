package a4;

import java.awt.event.*;
import java.lang.Math;

import org.joml.*;

public class Camera implements KeyListener {
    //c is camera origin in space
    //u, v, n = camera orientation
    private final Vector3f u;
    private final Vector3f v;
    private final Vector3f n;
    private Vector3f c;

    //Vars for axes rendering
    private boolean axesOn;

    public Camera(float camX, float camY, float camZ){
        c = new Vector3f(camX, camY, camZ);       //Initialized to axes origin

        Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);

        n = new Vector3f(0.0f, 0.0f, 0.0f);
        c.sub(target, n);
        n.normalize();

        u = new Vector3f(0.0f, 1.0f, 0.0f);
        u.cross(n, u);
        u.normalize();

        v = new Vector3f(0.0f, 0.0f, 0.0f);
        u.cross(n, v);
        v.normalize();

    }

    public boolean getBool(){
        return axesOn;
    }

    public float getX(){
        return this.c.x;
    }

    public float getY(){
        return this.c.y;
    }

    public float getZ(){
        return this.c.z;
    }

    public Matrix4f setUpViewMat(){
        //Translation Matrix
        Matrix4f t = new Matrix4f(1.0f, 0.0f, 0.0f, -c.x,
                            0.0f, 1.0f, 0.0f, -c.y,
                       0.0f,0.0f, 1.0f, -c.z,
                0.0f, 0.0f, 0.0f, 1.0f);
        t.transpose();

        //Rotation Matrix (negative of rotation angles)
        Matrix4f r = new Matrix4f(u.x, -v.x, n.x, 0,
                                  u.y, -v.y, n.y, 0,
                                  u.z, -v.z, n.z, 0,
                            0, 0, 0, 1);

        return r.mul(t);
    }

    @Override
    public void keyTyped (KeyEvent e){
    }

    @Override
    public void keyPressed (KeyEvent e){
        int key = e.getKeyCode();

        float movementAmount = 3.0f;
        if (key == KeyEvent.VK_W) {
            c.sub(n.x*3, n.y*3, n.z*3);
            System.out.println("w pressed");
        }
        else if (key == KeyEvent.VK_A) {
            c.sub(u.x*3, u.y*3, u.z*3);
            System.out.println("a pressed");
        }
        else if (key == KeyEvent.VK_S) {
            c.add(n.x*3, n.y*3, n.z*3);
            System.out.println("s pressed");
        }
        else if (key == KeyEvent.VK_D) {
            c.add(u.x*3, u.y*3, u.z*3);
            System.out.println("d pressed");
        }
        else if (key == KeyEvent.VK_Q) {
            c.add(0.0f, movementAmount, 0.0f);
            System.out.println("q pressed");
        }
        else if (key == KeyEvent.VK_E) {
            c.sub(0.0f, movementAmount, 0.0f);
            System.out.println("e pressed");
        }
        else if (key == KeyEvent.VK_UP) {
            n.rotateAxis((float) Math.toRadians(1), u.x, u.y, u.z);
            v.rotateAxis((float) Math.toRadians(1), u.x, u.y, u.z);
            System.out.println("up pressed");
        }
        else if (key == KeyEvent.VK_DOWN) {
            n.rotateAxis((float) Math.toRadians(-1), u.x, u.y, u.z);
            v.rotateAxis((float) Math.toRadians(-1), u.x, u.y, u.z);
            System.out.println("down pressed");
        }
        else if (key == KeyEvent.VK_LEFT) {
            u.rotateAxis((float) Math.toRadians(1), 0.0f, movementAmount, 0.0f);
            n.rotateAxis((float) Math.toRadians(1), 0.0f, movementAmount, 0.0f);
            v.rotateAxis((float) Math.toRadians(1), 0.0f, movementAmount, 0.0f);
            System.out.println("left pressed");
        }
        else if (key == KeyEvent.VK_RIGHT) {
            u.rotateAxis((float) Math.toRadians(-1), 0.0f, movementAmount, 0.0f);
            n.rotateAxis((float) Math.toRadians(-1), 0.0f, movementAmount, 0.0f);
            v.rotateAxis((float) Math.toRadians(-1), 0.0f, movementAmount, 0.0f);
            System.out.println("right pressed");
        }
        //Toggled based on value in boolean
        else if (key == KeyEvent.VK_SPACE){
            if(axesOn) {
                axesOn = false;
                System.out.println("Axes Off");
            }
            else{
                axesOn = true;
                System.out.println("Axes On");
            }
        }
    }

    @Override
    public void keyReleased (KeyEvent e){}
};